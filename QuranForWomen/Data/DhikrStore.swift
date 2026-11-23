import AVFoundation
import Foundation
import Observation

/// A dhikr phrase she writes in her own words, with her own target.
/// Always shown as personal, never as Quran or Sunnah, and kept apart from the sourced list.
struct CustomDhikr: Codable, Identifiable, Hashable {
    var id = UUID()
    var text: String
    /// 0 means no target.
    var target: Int
    var created = Date()

    /// The key her count is kept under, apart from the sourced phrase ids.
    var countID: String { CustomDhikr.prefix + id.uuidString }

    static let prefix = "custom:"
}

/// Dhikr extras and recently opened duas: her own phrases, how much she counted each day,
/// an optional daily goal, the counting sound, and the last duas she opened.
/// Kept only on this iPhone.
@Observable
@MainActor
final class DhikrStore {
    private struct Snapshot: Codable {
        var custom: [CustomDhikr] = []
        /// "yyyy-MM-dd" to the total she counted that day.
        var daily: [String: Int] = [:]
        var goal: Int?
        var soundOn: Bool?
        var recentDuaIDs: [String]?
    }

    private static let storageKey = "dhikr.extras.v1"
    static let recentLimit = 10
    static let historyDays = 30

    private(set) var custom: [CustomDhikr] = []
    private(set) var daily: [String: Int] = [:]
    private(set) var goal: Int?
    private(set) var soundOn = false
    private(set) var recentDuaIDs: [String] = []

    @ObservationIgnored private let defaults: UserDefaults
    @ObservationIgnored private var clickPlayer: AVAudioPlayer?

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
        if let data = defaults.data(forKey: Self.storageKey),
           let s = try? JSONDecoder().decode(Snapshot.self, from: data) {
            custom = s.custom
            daily = s.daily
            goal = s.goal
            soundOn = s.soundOn ?? false
            recentDuaIDs = s.recentDuaIDs ?? []
        }
    }

    // MARK: Her own dhikr

    func customDhikr(countID: String) -> CustomDhikr? {
        custom.first { $0.countID == countID }
    }

    @discardableResult
    func addCustom(text: String, target: Int) -> CustomDhikr? {
        let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return nil }
        let item = CustomDhikr(text: trimmed, target: max(0, target))
        custom.insert(item, at: 0)
        save()
        return item
    }

    func updateCustom(_ item: CustomDhikr, text: String, target: Int) {
        guard let index = custom.firstIndex(where: { $0.id == item.id }) else { return }
        let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        custom[index].text = trimmed
        custom[index].target = max(0, target)
        save()
    }

    func removeCustom(_ item: CustomDhikr) {
        custom.removeAll { $0.id == item.id }
        save()
    }

    func restoreCustom(_ item: CustomDhikr, at index: Int) {
        guard !custom.contains(where: { $0.id == item.id }) else { return }
        custom.insert(item, at: min(max(index, 0), custom.count))
        save()
    }

    // MARK: Daily totals

    /// Adds one count to today. Old days beyond the history window are dropped.
    func recordCount(on date: Date = .now) {
        let key = LibraryStore.dayKey(date)
        daily[key, default: 0] += 1
        if daily.count > Self.historyDays + 5 { prune(before: date) }
        save()
    }

    func total(on date: Date = .now) -> Int { daily[LibraryStore.dayKey(date)] ?? 0 }

    /// The last 30 days, oldest first, ending today.
    func history(endingOn date: Date = .now) -> [(date: Date, total: Int)] {
        let calendar = Calendar.current
        let today = calendar.startOfDay(for: date)
        return (0..<Self.historyDays).reversed().compactMap { offset in
            guard let day = calendar.date(byAdding: .day, value: -offset, to: today) else { return nil }
            return (date: day, total: self.total(on: day))
        }
    }

    func setGoal(_ value: Int?) {
        goal = (value ?? 0) > 0 ? value : nil
        save()
    }

    private func prune(before date: Date) {
        guard let cutoff = Calendar.current.date(byAdding: .day, value: -Self.historyDays, to: date) else { return }
        let oldest = LibraryStore.dayKey(cutoff)
        daily = daily.filter { $0.key >= oldest }
    }

    // MARK: Counting sound

    func setSoundOn(_ on: Bool) {
        soundOn = on
        save()
        if on { prepareClick() }
    }

    /// A soft click, only when she turned the sound on.
    func playClick() {
        guard soundOn else { return }
        prepareClick()
        guard let clickPlayer else { return }
        // Mix with her music and respect the silent switch, unless recitation already owns the session.
        let session = AVAudioSession.sharedInstance()
        if session.category == .soloAmbient {
            try? session.setCategory(.ambient)
        }
        clickPlayer.currentTime = 0
        clickPlayer.play()
    }

    private func prepareClick() {
        guard clickPlayer == nil,
              let url = Bundle.main.url(forResource: "dhikr-click", withExtension: "wav") else { return }
        clickPlayer = try? AVAudioPlayer(contentsOf: url)
        clickPlayer?.volume = 0.7
        clickPlayer?.prepareToPlay()
    }

    // MARK: Recently opened duas

    func noteOpened(duaID: String) {
        guard recentDuaIDs.first != duaID else { return }
        recentDuaIDs.removeAll { $0 == duaID }
        recentDuaIDs.insert(duaID, at: 0)
        if recentDuaIDs.count > Self.recentLimit {
            recentDuaIDs = Array(recentDuaIDs.prefix(Self.recentLimit))
        }
        save()
    }

    func clearRecent() {
        recentDuaIDs = []
        save()
    }

    // MARK: Data controls

    func eraseAll() {
        custom = []
        daily = [:]
        goal = nil
        soundOn = false
        recentDuaIDs = []
        defaults.removeObject(forKey: Self.storageKey)
    }

    private func save() {
        let snapshot = Snapshot(custom: custom, daily: daily, goal: goal, soundOn: soundOn, recentDuaIDs: recentDuaIDs)
        guard let data = try? JSONEncoder().encode(snapshot) else { return }
        defaults.set(data, forKey: Self.storageKey)
    }
}
