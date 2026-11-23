import Foundation
import Observation
import SwiftUI

/// Keys for simple preferences stored with @AppStorage.
enum PrefKey {
    static let hasOnboarded = "hasOnboarded"
    static let arabicSize = "reader.arabicSize"
    static let translationSize = "reader.translationSize"
    static let showTranslation = "reader.showTranslation"
    static let showTransliteration = "duas.showTransliteration"
    static let appearance = "appearance"
    static let haptics = "haptics"
    static let reciter = "audio.reciter"
    static let translation = "translation.edition"
    static let reminderEnabled = "reminder.enabled"
    static let reminderMinutes = "reminder.minutes"
    static let eveningReminderEnabled = "reminder.evening.enabled"
    static let eveningReminderMinutes = "reminder.evening.minutes"
    static let dhikrSelection = "dhikr.selection"
    static let dhikrTarget = "dhikr.target"
    static let iCloudSync = "sync.icloud"
}

enum ReaderDefaults {
    static let arabicSize: Double = 30
    static let translationSize: Double = 17
    static let arabicRange: ClosedRange<Double> = 22...48
    static let translationRange: ClosedRange<Double> = 13...26
}

enum Appearance: String, CaseIterable, Identifiable {
    case system, light, dark

    var id: String { rawValue }
    var title: String {
        switch self {
        case .system: "Automatic"
        case .light: "Light"
        case .dark: "Dark"
        }
    }
    var colorScheme: ColorScheme? {
        switch self {
        case .system: nil
        case .light: .light
        case .dark: .dark
        }
    }
}

/// A plan to read the whole Quran over a number of days, split by mushaf pages.
struct ReadingPlan: Codable, Hashable {
    var days: Int
    var start: Date
    var finishedDays: Set<Int> = []
    // Added later. Optional so older saved plans still decode. Nil means a page plan.
    var firstSurah: Int? = nil
    var lastSurah: Int? = nil

    static let totalPages = 604

    /// True for plans that read one surah a day instead of a share of pages.
    var isSurahPlan: Bool { firstSurah != nil }

    /// The surah read on a day of a surah plan.
    func surah(forDay day: Int) -> Int { (firstSurah ?? 1) + day }

    /// True when finishing the plan means the whole Quran was read.
    var coversWholeQuran: Bool { !isSurahPlan || (firstSurah == 1 && (lastSurah ?? 114) >= 114) }

    /// How much of the plan is read, from 0 to 1.
    var progress: Double {
        guard days > 0 else { return 0 }
        if isSurahPlan || days > Self.totalPages {
            return min(1, Double(finishedDays.count) / Double(days))
        }
        let pagesRead = finishedDays.reduce(0) { $0 + pages(forDay: $1).count }
        return min(1, Double(pagesRead) / Double(Self.totalPages))
    }

    /// Zero based day of the plan for a date, clamped to the plan length.
    func dayIndex(on date: Date = .now) -> Int {
        let calendar = Calendar.current
        let elapsed = calendar.dateComponents([.day], from: calendar.startOfDay(for: start), to: calendar.startOfDay(for: date)).day ?? 0
        return min(max(elapsed, 0), days - 1)
    }

    func pages(forDay day: Int) -> ClosedRange<Int> {
        let first = day * Self.totalPages / days + 1
        let last = max(first, (day + 1) * Self.totalPages / days)
        return first...last
    }

    var isComplete: Bool { finishedDays.count >= days }
}

/// Everything personal: bookmarks, highlights, notes, saved duas, reading place,
/// prayer log and dhikr counts. Stored on this iPhone, and in her own iCloud only if she turns sync on.
@Observable
@MainActor
final class LibraryStore {
    private struct Snapshot: Codable {
        var bookmarks: [VerseRef] = []
        var savedDuaIDs: [String] = []
        var lastRead: VerseRef?
        var completedSurahs: Set<Int> = []
        var personalDuas: [PersonalDua] = []
        var dhikrCounts: [String: Int] = [:]
        // Added later. Optional so older saved data still decodes.
        var highlights: [String: HighlightColor]?
        var notes: [String: VerseNote]?
        var prayerLog: [String: [String]]?
        var restSince: Date?
        var plan: ReadingPlan?
        var updatedAt: Date?
        var favouriteSurahs: [Int]?
        var history: [ReadingVisit]?
    }

    private static let storageKey = "library.v1"

    private(set) var bookmarks: [VerseRef] = []
    private(set) var savedDuaIDs: [String] = []
    private(set) var lastRead: VerseRef?
    private(set) var completedSurahs: Set<Int> = []
    private(set) var personalDuas: [PersonalDua] = []
    private(set) var dhikrCounts: [String: Int] = [:]
    private(set) var highlights: [String: HighlightColor] = [:]
    private(set) var notes: [String: VerseNote] = [:]
    /// "yyyy-MM-dd" to the prayers marked as prayed that day.
    private(set) var prayerLog: [String: [String]] = [:]
    /// Set while she is on rest days (her period). Prayer reminders pause.
    private(set) var restSince: Date?
    private(set) var plan: ReadingPlan?
    /// Surah numbers she marked with a heart, in Quran order.
    private(set) var favouriteSurahs: [Int] = []
    /// The last places opened in the reader, newest first.
    private(set) var history: [ReadingVisit] = []
    private(set) var syncEnabled = false
    private var updatedAt = Date.distantPast

    @ObservationIgnored private let defaults: UserDefaults
    @ObservationIgnored private var cloudObserver: NSObjectProtocol?

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
        syncEnabled = defaults.bool(forKey: PrefKey.iCloudSync)
        if let data = defaults.data(forKey: Self.storageKey),
           let snapshot = try? JSONDecoder().decode(Snapshot.self, from: data) {
            apply(snapshot)
        }
        if syncEnabled { startCloudSync() }
    }

    // MARK: Verses

    func isBookmarked(_ ref: VerseRef) -> Bool { bookmarks.contains(ref) }

    func toggleBookmark(_ ref: VerseRef) {
        if let index = bookmarks.firstIndex(of: ref) {
            bookmarks.remove(at: index)
        } else {
            bookmarks.insert(ref, at: 0)
        }
        save()
    }

    func removeBookmarks(at offsets: IndexSet) {
        bookmarks.remove(atOffsets: offsets)
        save()
    }

    func highlight(_ ref: VerseRef) -> HighlightColor? { highlights[ref.id] }

    func setHighlight(_ color: HighlightColor?, for ref: VerseRef) {
        highlights[ref.id] = color
        save()
    }

    func note(_ ref: VerseRef) -> VerseNote? { notes[ref.id] }

    func setNote(_ text: String, for ref: VerseRef) {
        let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
        notes[ref.id] = trimmed.isEmpty ? nil : VerseNote(text: trimmed)
        save()
    }

    /// Verses with a highlight or a note, newest note first, then in Quran order.
    var annotatedVerses: [VerseRef] {
        let ids = Set(highlights.keys).union(notes.keys)
        return ids.compactMap(VerseRef.init).sorted { a, b in
            let da = notes[a.id]?.updated ?? .distantPast
            let db = notes[b.id]?.updated ?? .distantPast
            return da == db ? a < b : da > db
        }
    }

    func setLastRead(_ ref: VerseRef) {
        guard ref != lastRead else { return }
        lastRead = ref
        // The newest history entry follows her place in the surah she has open.
        if let first = history.first, first.ref.surah == ref.surah { history[0].ref = ref }
        save()
    }

    // MARK: Favourite surahs

    func isFavourite(_ surah: Int) -> Bool { favouriteSurahs.contains(surah) }

    func toggleFavourite(_ surah: Int) {
        if let index = favouriteSurahs.firstIndex(of: surah) {
            favouriteSurahs.remove(at: index)
        } else {
            favouriteSurahs.append(surah)
            favouriteSurahs.sort()
        }
        save()
    }

    // MARK: Reading history

    static let historyLimit = 20

    /// Called when the reader opens. One entry per surah, newest first.
    func recordVisit(_ ref: VerseRef) {
        history.removeAll { $0.ref.surah == ref.surah }
        history.insert(ReadingVisit(ref: ref, date: .now), at: 0)
        if history.count > Self.historyLimit { history.removeLast(history.count - Self.historyLimit) }
        save()
    }

    func clearHistory() {
        history = []
        save()
    }

    func markCompleted(_ surah: Int) {
        guard !completedSurahs.contains(surah) else { return }
        completedSurahs.insert(surah)
        save()
    }

    // MARK: Duas

    func isSaved(_ dua: Dua) -> Bool { savedDuaIDs.contains(dua.id) }

    func toggleSaved(_ dua: Dua) {
        if let index = savedDuaIDs.firstIndex(of: dua.id) {
            savedDuaIDs.remove(at: index)
        } else {
            savedDuaIDs.insert(dua.id, at: 0)
        }
        save()
    }

    func addPersonalDua(_ text: String) {
        let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        personalDuas.insert(PersonalDua(text: trimmed), at: 0)
        save()
    }

    func updatePersonalDua(_ dua: PersonalDua, text: String) {
        guard let index = personalDuas.firstIndex(where: { $0.id == dua.id }) else { return }
        let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmed.isEmpty {
            personalDuas.remove(at: index)
        } else {
            personalDuas[index].text = trimmed
        }
        save()
    }

    func removePersonalDuas(at offsets: IndexSet) {
        personalDuas.remove(atOffsets: offsets)
        save()
    }

    // MARK: Dhikr

    func dhikrCount(_ id: String) -> Int { dhikrCounts[id] ?? 0 }

    func setDhikrCount(_ id: String, _ value: Int) {
        dhikrCounts[id] = max(0, value)
        save()
    }

    // MARK: Prayer

    static func dayKey(_ date: Date) -> String {
        let c = Calendar.current.dateComponents([.year, .month, .day], from: date)
        return String(format: "%04d-%02d-%02d", c.year ?? 0, c.month ?? 0, c.day ?? 0)
    }

    func prayed(_ prayer: String, on date: Date = .now) -> Bool {
        prayerLog[Self.dayKey(date)]?.contains(prayer) ?? false
    }

    func togglePrayed(_ prayer: String, on date: Date = .now) {
        let key = Self.dayKey(date)
        var done = prayerLog[key] ?? []
        if let index = done.firstIndex(of: prayer) { done.remove(at: index) } else { done.append(prayer) }
        prayerLog[key] = done.isEmpty ? nil : done
        save()
    }

    func setPrayed(_ prayer: String, on date: Date = .now) {
        guard !prayed(prayer, on: date) else { return }
        togglePrayed(prayer, on: date)
    }

    var isResting: Bool { restSince != nil }

    func setResting(_ on: Bool) {
        restSince = on ? (restSince ?? .now) : nil
        save()
    }

    // MARK: Reading plan

    func startPlan(days: Int) {
        plan = ReadingPlan(days: days, start: .now)
        save()
    }

    /// One surah a day, from the first surah to the last, in order.
    func startSurahPlan(from first: Int, to last: Int) {
        let low = min(max(first, 1), 114)
        let high = min(max(last, low), 114)
        plan = ReadingPlan(days: high - low + 1, start: .now, firstSurah: low, lastSurah: high)
        save()
    }

    func toggleFinished(day: Int) {
        guard var current = plan else { return }
        if current.finishedDays.contains(day) { current.finishedDays.remove(day) } else { current.finishedDays.insert(day) }
        plan = current
        save()
    }

    func endPlan() {
        plan = nil
        save()
    }

    // MARK: iCloud sync

    func setSyncEnabled(_ on: Bool) {
        syncEnabled = on
        defaults.set(on, forKey: PrefKey.iCloudSync)
        if on {
            startCloudSync()
            save()
        } else if let cloudObserver {
            NotificationCenter.default.removeObserver(cloudObserver)
            self.cloudObserver = nil
        }
    }

    private func startCloudSync() {
        guard cloudObserver == nil else { return }
        let store = NSUbiquitousKeyValueStore.default
        cloudObserver = NotificationCenter.default.addObserver(
            forName: NSUbiquitousKeyValueStore.didChangeExternallyNotification, object: store, queue: .main
        ) { [weak self] _ in
            Task { @MainActor in self?.pullFromCloud() }
        }
        store.synchronize()
        pullFromCloud()
    }

    /// Last writer wins. Simple and predictable for one person's devices.
    private func pullFromCloud() {
        guard let data = NSUbiquitousKeyValueStore.default.data(forKey: Self.storageKey),
              let remote = try? JSONDecoder().decode(Snapshot.self, from: data),
              let remoteDate = remote.updatedAt, remoteDate > updatedAt
        else { return }
        apply(remote)
        defaults.set(data, forKey: Self.storageKey)
    }

    // MARK: Data controls

    func eraseAll() {
        apply(Snapshot())
        updatedAt = .now
        defaults.removeObject(forKey: Self.storageKey)
        if syncEnabled { NSUbiquitousKeyValueStore.default.removeObject(forKey: Self.storageKey) }
    }

    // MARK: Persistence

    private func apply(_ s: Snapshot) {
        bookmarks = s.bookmarks
        savedDuaIDs = s.savedDuaIDs
        lastRead = s.lastRead
        completedSurahs = s.completedSurahs
        personalDuas = s.personalDuas
        dhikrCounts = s.dhikrCounts
        highlights = s.highlights ?? [:]
        notes = s.notes ?? [:]
        prayerLog = s.prayerLog ?? [:]
        restSince = s.restSince
        plan = s.plan
        updatedAt = s.updatedAt ?? .distantPast
        favouriteSurahs = s.favouriteSurahs ?? []
        history = s.history ?? []
    }

    private func save() {
        updatedAt = .now
        let snapshot = Snapshot(
            bookmarks: bookmarks,
            savedDuaIDs: savedDuaIDs,
            lastRead: lastRead,
            completedSurahs: completedSurahs,
            personalDuas: personalDuas,
            dhikrCounts: dhikrCounts,
            highlights: highlights,
            notes: notes,
            prayerLog: prayerLog,
            restSince: restSince,
            plan: plan,
            updatedAt: updatedAt,
            favouriteSurahs: favouriteSurahs,
            history: history
        )
        guard let data = try? JSONEncoder().encode(snapshot) else { return }
        defaults.set(data, forKey: Self.storageKey)
        if syncEnabled {
            NSUbiquitousKeyValueStore.default.set(data, forKey: Self.storageKey)
        }
    }
}
