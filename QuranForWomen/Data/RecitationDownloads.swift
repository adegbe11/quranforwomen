import Foundation
import Observation

/// Keeps a surah's recitation on this iPhone for offline listening, one reciter at a time.
/// Files come from the same audio CDN the player streams from and are left out of backups.
@Observable
@MainActor
final class RecitationDownloads {
    enum State: Equatable { case notDownloaded, downloading, ready, failed }

    struct Item: Hashable, Identifiable {
        let reciterID: String
        let surah: Int
        var id: String { "\(reciterID)/\(surah)" }
    }

    private(set) var states: [String: State] = [:]
    private(set) var progresses: [String: Double] = [:]

    @ObservationIgnored private let quran: QuranStore
    @ObservationIgnored private var tasks: [String: Task<Void, Never>] = [:]
    @ObservationIgnored private var sizes: [String: Int64] = [:]

    init(quran: QuranStore) {
        self.quran = quran
        // A surah counts as downloaded once every verse arrived and the marker was written.
        let fm = FileManager.default
        let reciters = (try? fm.contentsOfDirectory(at: Self.root, includingPropertiesForKeys: nil)) ?? []
        for reciterDir in reciters {
            let surahs = (try? fm.contentsOfDirectory(at: reciterDir, includingPropertiesForKeys: nil)) ?? []
            for surahDir in surahs {
                guard let surah = Int(surahDir.lastPathComponent),
                      fm.fileExists(atPath: surahDir.appendingPathComponent(Self.marker).path)
                else { continue }
                states[Self.key(reciterDir.lastPathComponent, surah)] = .ready
            }
        }
    }

    func state(reciterID: String, surah: Int) -> State {
        states[Self.key(reciterID, surah)] ?? .notDownloaded
    }

    /// How much of a download in progress has arrived, 0 to 1.
    func progress(reciterID: String, surah: Int) -> Double {
        progresses[Self.key(reciterID, surah)] ?? 0
    }

    /// Every downloaded surah, grouped by reciter in the app's reciter order.
    var downloaded: [Item] {
        states.compactMap { key, state -> Item? in
            guard state == .ready else { return nil }
            let parts = key.split(separator: "/")
            guard parts.count == 2, let surah = Int(parts[1]) else { return nil }
            return Item(reciterID: String(parts[0]), surah: surah)
        }
        .sorted { a, b in
            let ra = Reciter.all.firstIndex { $0.id == a.reciterID } ?? 99
            let rb = Reciter.all.firstIndex { $0.id == b.reciterID } ?? 99
            return ra == rb ? a.surah < b.surah : ra < rb
        }
    }

    func download(reciterID: String, surah: Int) {
        let key = Self.key(reciterID, surah)
        let current = state(reciterID: reciterID, surah: surah)
        guard current != .downloading, current != .ready else { return }
        states[key] = .downloading
        progresses[key] = 0

        let total = quran.surah(surah).verseCount
        let first = quran.globalNumber(VerseRef(surah: surah, ayah: 1))
        let dir = Self.surahDirectory(reciterID, surah)

        tasks[key] = Task { [weak self] in
            do {
                try FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
                Self.excludeFromBackup(Self.root)
                var done = 0
                var start = 1
                // Four verses at a time.
                while start <= total {
                    try Task.checkCancellation()
                    let batch = Array(start...min(start + 3, total))
                    try await withThrowingTaskGroup(of: Void.self) { group in
                        for ayah in batch {
                            let target = dir.appendingPathComponent("\(ayah).mp3")
                            guard let remote = URL(string: "https://cdn.islamic.network/quran/audio/128/\(reciterID)/\(first + ayah - 1).mp3") else {
                                throw URLError(.badURL)
                            }
                            group.addTask { try await Self.fetch(remote, to: target) }
                        }
                        try await group.waitForAll()
                    }
                    done += batch.count
                    start += batch.count
                    self?.progresses[key] = Double(done) / Double(total)
                }
                try Data("ok".utf8).write(to: dir.appendingPathComponent(Self.marker), options: .atomic)
                self?.sizes[key] = Self.folderSize(dir)
                self?.states[key] = .ready
            } catch {
                if Task.isCancelled {
                    try? FileManager.default.removeItem(at: dir)
                    self?.states[key] = .notDownloaded
                } else {
                    self?.states[key] = .failed
                }
            }
            self?.progresses[key] = nil
            self?.tasks[key] = nil
        }
    }

    func cancel(reciterID: String, surah: Int) {
        tasks[Self.key(reciterID, surah)]?.cancel()
    }

    func delete(reciterID: String, surah: Int) {
        let key = Self.key(reciterID, surah)
        tasks[key]?.cancel()
        try? FileManager.default.removeItem(at: Self.surahDirectory(reciterID, surah))
        states[key] = .notDownloaded
        sizes[key] = nil
    }

    func sizeOnDisk(reciterID: String, surah: Int) -> Int64? {
        let key = Self.key(reciterID, surah)
        guard state(reciterID: reciterID, surah: surah) == .ready else { return nil }
        if let size = sizes[key] { return size }
        let size = Self.folderSize(Self.surahDirectory(reciterID, surah))
        sizes[key] = size
        return size
    }

    // MARK: Files

    private static let marker = ".complete"

    private nonisolated static func key(_ reciterID: String, _ surah: Int) -> String { "\(reciterID)/\(surah)" }

    private nonisolated static var root: URL {
        let base = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
        return base.appendingPathComponent("Recitations", isDirectory: true)
    }

    private nonisolated static func surahDirectory(_ reciterID: String, _ surah: Int) -> URL {
        root.appendingPathComponent(reciterID, isDirectory: true).appendingPathComponent("\(surah)", isDirectory: true)
    }

    /// The saved recitation of one verse. It may not exist.
    nonisolated static func localURL(reciterID: String, surah: Int, ayah: Int) -> URL {
        surahDirectory(reciterID, surah).appendingPathComponent("\(ayah).mp3")
    }

    private nonisolated static func fetch(_ url: URL, to target: URL) async throws {
        if let size = try? target.resourceValues(forKeys: [.fileSizeKey]).fileSize, size > 0 { return }
        var request = URLRequest(url: url)
        request.timeoutInterval = 60
        let (temporary, response) = try await URLSession.shared.download(for: request)
        guard (response as? HTTPURLResponse)?.statusCode == 200 else { throw URLError(.badServerResponse) }
        try? FileManager.default.removeItem(at: target)
        try FileManager.default.moveItem(at: temporary, to: target)
        excludeFromBackup(target)
    }

    private nonisolated static func excludeFromBackup(_ url: URL) {
        var values = URLResourceValues()
        values.isExcludedFromBackup = true
        var mutableURL = url
        try? mutableURL.setResourceValues(values)
    }

    private nonisolated static func folderSize(_ dir: URL) -> Int64 {
        let files = (try? FileManager.default.contentsOfDirectory(at: dir, includingPropertiesForKeys: [.fileSizeKey])) ?? []
        return files.reduce(Int64(0)) { sum, file in
            sum + Int64((try? file.resourceValues(forKeys: [.fileSizeKey]).fileSize) ?? 0)
        }
    }
}
