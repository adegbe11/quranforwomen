import Foundation
import Observation

struct TranslationOption: Identifiable, Hashable {
    /// Edition identifier on api.alquran.cloud.
    let id: String
    let language: String
    let nativeName: String
    let translator: String
    let isRightToLeft: Bool
    /// ISO language code used to suggest a default.
    let code: String

    static let all: [TranslationOption] = [
        .init(id: "en.sahih", language: "English", nativeName: "English", translator: "Saheeh International", isRightToLeft: false, code: "en"),
        .init(id: "ur.junagarhi", language: "Urdu", nativeName: "اردو", translator: "Muhammad Junagarhi", isRightToLeft: true, code: "ur"),
        .init(id: "id.indonesian", language: "Indonesian", nativeName: "Bahasa Indonesia", translator: "Indonesian Ministry of Religious Affairs", isRightToLeft: false, code: "id"),
        .init(id: "ms.basmeih", language: "Malay", nativeName: "Bahasa Melayu", translator: "Abdullah Muhammad Basmeih", isRightToLeft: false, code: "ms"),
        .init(id: "tr.diyanet", language: "Turkish", nativeName: "Türkçe", translator: "Diyanet İşleri", isRightToLeft: false, code: "tr"),
        .init(id: "fr.hamidullah", language: "French", nativeName: "Français", translator: "Muhammad Hamidullah", isRightToLeft: false, code: "fr"),
        .init(id: "bn.bengali", language: "Bengali", nativeName: "বাংলা", translator: "Muhiuddin Khan", isRightToLeft: false, code: "bn"),
        .init(id: "es.cortes", language: "Spanish", nativeName: "Español", translator: "Julio Cortés", isRightToLeft: false, code: "es"),
        .init(id: "de.bubenheim", language: "German", nativeName: "Deutsch", translator: "Bubenheim and Elyas", isRightToLeft: false, code: "de"),
        .init(id: "ru.kuliev", language: "Russian", nativeName: "Русский", translator: "Elmir Kuliev", isRightToLeft: false, code: "ru"),
        .init(id: "ha.gumi", language: "Hausa", nativeName: "Hausa", translator: "Abubakar Mahmoud Gumi", isRightToLeft: false, code: "ha"),
        .init(id: "sw.barwani", language: "Swahili", nativeName: "Kiswahili", translator: "Ali Muhsin Al-Barwani", isRightToLeft: false, code: "sw"),
        .init(id: "so.abduh", language: "Somali", nativeName: "Soomaali", translator: "Mahmud Muhammad Abduh", isRightToLeft: false, code: "so"),
    ]

    static func suggested(for locale: Locale = .current) -> TranslationOption {
        let code = locale.language.languageCode?.identifier ?? "en"
        return all.first { $0.code == code } ?? all[0]
    }
}

/// Downloads a full translation once, checks it, and keeps it for offline reading.
@Observable
@MainActor
final class TranslationStore {
    enum State: Equatable { case notDownloaded, downloading, ready, failed }

    private(set) var states: [String: State] = [:]
    var selectedID: String {
        didSet {
            UserDefaults.standard.set(selectedID, forKey: PrefKey.translation)
            Task { await prepareSelected() }
        }
    }

    @ObservationIgnored private var cache: [String: [[String]]] = [:]

    init() {
        selectedID = UserDefaults.standard.string(forKey: PrefKey.translation) ?? TranslationOption.suggested().id
        for option in TranslationOption.all {
            let exists = FileManager.default.fileExists(atPath: Self.fileURL(option.id).path)
            states[option.id] = exists ? .ready : .notDownloaded
        }
    }

    var selected: TranslationOption {
        TranslationOption.all.first { $0.id == selectedID } ?? TranslationOption.all[0]
    }

    var selectedState: State { state(selectedID) }

    func state(_ id: String) -> State { states[id] ?? .notDownloaded }

    /// Translation of one verse in the selected language, if downloaded.
    func text(_ ref: VerseRef) -> String? {
        guard let verses = verses(for: selectedID),
              verses.indices.contains(ref.surah - 1),
              verses[ref.surah - 1].indices.contains(ref.ayah - 1)
        else { return nil }
        return verses[ref.surah - 1][ref.ayah - 1]
    }

    /// The whole selected translation, used by search.
    func selectedVerses() -> [[String]]? { verses(for: selectedID) }

    /// Loads the selected translation into memory, downloading it if needed.
    func prepareSelected() async {
        let id = selectedID
        switch state(id) {
        case .ready:
            if cache[id] == nil {
                let url = Self.fileURL(id)
                let loaded = await Task.detached(priority: .userInitiated) {
                    try? JSONDecoder().decode([[String]].self, from: Data(contentsOf: url))
                }.value
                if let loaded {
                    cache[id] = loaded
                    // Touch observed state so views reading text refresh.
                    states[id] = .ready
                } else {
                    states[id] = .notDownloaded
                    await download(id)
                }
            }
        case .notDownloaded, .failed:
            await download(id)
        case .downloading:
            break
        }
    }

    func download(_ id: String) async {
        guard state(id) != .downloading else { return }
        states[id] = .downloading
        do {
            guard let url = URL(string: "https://api.alquran.cloud/v1/quran/\(id)") else { throw URLError(.badURL) }
            var request = URLRequest(url: url)
            request.timeoutInterval = 60
            let (data, response) = try await URLSession.shared.data(for: request)
            guard (response as? HTTPURLResponse)?.statusCode == 200 else { throw URLError(.badServerResponse) }
            let verses = try await Task.detached(priority: .utility) { try Self.parse(data) }.value
            let fileURL = Self.fileURL(id)
            try FileManager.default.createDirectory(at: fileURL.deletingLastPathComponent(), withIntermediateDirectories: true)
            try JSONEncoder().encode(verses).write(to: fileURL, options: .atomic)
            var values = URLResourceValues()
            values.isExcludedFromBackup = true
            var mutableURL = fileURL
            try? mutableURL.setResourceValues(values)
            cache[id] = verses
            states[id] = .ready
        } catch {
            states[id] = .failed
        }
    }

    func delete(_ id: String) {
        try? FileManager.default.removeItem(at: Self.fileURL(id))
        cache[id] = nil
        states[id] = .notDownloaded
    }

    func sizeOnDisk(_ id: String) -> Int64? {
        let attributes = try? FileManager.default.attributesOfItem(atPath: Self.fileURL(id).path)
        return (attributes?[.size] as? NSNumber)?.int64Value
    }

    private func verses(for id: String) -> [[String]]? {
        guard state(id) == .ready else { return nil }
        if let cached = cache[id] { return cached }
        guard let data = try? Data(contentsOf: Self.fileURL(id)),
              let loaded = try? JSONDecoder().decode([[String]].self, from: data)
        else { return nil }
        cache[id] = loaded
        return loaded
    }

    nonisolated static func fileURL(_ id: String) -> URL {
        let base = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
        return base.appendingPathComponent("Translations", isDirectory: true).appendingPathComponent("\(id).json")
    }

    /// Accepts a download only if it has all 114 surahs and all 6236 verses.
    nonisolated static func parse(_ data: Data) throws -> [[String]] {
        struct Response: Decodable {
            struct Body: Decodable {
                struct SurahText: Decodable {
                    struct Ayah: Decodable { let text: String }
                    let ayahs: [Ayah]
                }
                let surahs: [SurahText]
            }
            let data: Body
        }
        let response = try JSONDecoder().decode(Response.self, from: data)
        let verses = response.data.surahs.map { $0.ayahs.map { $0.text.trimmingCharacters(in: .whitespacesAndNewlines) } }
        guard verses.count == 114, verses.reduce(0, { $0 + $1.count }) == 6236 else {
            throw CocoaError(.fileReadCorruptFile)
        }
        return verses
    }
}
