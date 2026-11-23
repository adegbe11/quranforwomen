import Foundation
import Observation

/// The Quran in Latin letters, shown under the Arabic when she turns it on.
/// Downloaded once from AlQuran Cloud, checked like a translation, and kept for offline reading.
@Observable
@MainActor
final class TransliterationStore {
    static let edition = "en.transliteration"

    private(set) var state: TranslationStore.State
    /// Off by default. Turning it on fetches the text the first time.
    var show: Bool {
        didSet {
            UserDefaults.standard.set(show, forKey: PlaybackPrefKey.showQuranTransliteration)
            if show { Task { await prepare() } }
        }
    }

    @ObservationIgnored private var cache: [[String]]?

    init() {
        let exists = FileManager.default.fileExists(atPath: TranslationStore.fileURL(Self.edition).path)
        state = exists ? .ready : .notDownloaded
        show = UserDefaults.standard.bool(forKey: PlaybackPrefKey.showQuranTransliteration)
    }

    /// Transliteration of one verse, if downloaded.
    func text(_ ref: VerseRef) -> String? {
        guard let verses = verses(),
              verses.indices.contains(ref.surah - 1),
              verses[ref.surah - 1].indices.contains(ref.ayah - 1)
        else { return nil }
        return verses[ref.surah - 1][ref.ayah - 1]
    }

    /// Loads the text into memory, downloading it if needed.
    func prepare() async {
        switch state {
        case .ready:
            if cache == nil {
                let url = TranslationStore.fileURL(Self.edition)
                let loaded = await Task.detached(priority: .userInitiated) {
                    try? JSONDecoder().decode([[String]].self, from: Data(contentsOf: url))
                }.value
                if let loaded {
                    cache = loaded
                    // Touch observed state so views reading text refresh.
                    state = .ready
                } else {
                    state = .notDownloaded
                    await download()
                }
            }
        case .notDownloaded, .failed:
            await download()
        case .downloading:
            break
        }
    }

    func download() async {
        guard state != .downloading else { return }
        state = .downloading
        do {
            guard let url = URL(string: "https://api.alquran.cloud/v1/quran/\(Self.edition)") else { throw URLError(.badURL) }
            var request = URLRequest(url: url)
            request.timeoutInterval = 60
            let (data, response) = try await URLSession.shared.data(for: request)
            guard (response as? HTTPURLResponse)?.statusCode == 200 else { throw URLError(.badServerResponse) }
            // The same check as translations: all 114 surahs and all 6236 verses.
            let verses = try await Task.detached(priority: .utility) { try TranslationStore.parse(data) }.value
            let fileURL = TranslationStore.fileURL(Self.edition)
            try FileManager.default.createDirectory(at: fileURL.deletingLastPathComponent(), withIntermediateDirectories: true)
            try JSONEncoder().encode(verses).write(to: fileURL, options: .atomic)
            var values = URLResourceValues()
            values.isExcludedFromBackup = true
            var mutableURL = fileURL
            try? mutableURL.setResourceValues(values)
            cache = verses
            state = .ready
        } catch {
            state = .failed
        }
    }

    func delete() {
        try? FileManager.default.removeItem(at: TranslationStore.fileURL(Self.edition))
        cache = nil
        state = .notDownloaded
    }

    func sizeOnDisk() -> Int64? {
        let attributes = try? FileManager.default.attributesOfItem(atPath: TranslationStore.fileURL(Self.edition).path)
        return (attributes?[.size] as? NSNumber)?.int64Value
    }

    private func verses() -> [[String]]? {
        guard state == .ready else { return nil }
        if let cache { return cache }
        guard let data = try? Data(contentsOf: TranslationStore.fileURL(Self.edition)),
              let loaded = try? JSONDecoder().decode([[String]].self, from: data)
        else { return nil }
        cache = loaded
        return loaded
    }
}
