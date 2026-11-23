import Foundation
import Observation
import UIKit

// MARK: - Models

enum JournalMood: String, Codable, CaseIterable, Identifiable {
    case grateful, peaceful, hopeful, makingDua, heavy, anxious

    var id: String { rawValue }

    var title: String {
        switch self {
        case .grateful: "Grateful"
        case .peaceful: "Peaceful"
        case .hopeful: "Hopeful"
        case .makingDua: "Making dua"
        case .heavy: "Heavy"
        case .anxious: "Anxious"
        }
    }

    var symbol: String {
        switch self {
        case .grateful: "heart"
        case .peaceful: "leaf"
        case .hopeful: "sunrise"
        case .makingDua: "hands.and.sparkles"
        case .heavy: "cloud.rain"
        case .anxious: "wind"
        }
    }

    /// The editor softens to a cooler tone on heavy days.
    var isHeavy: Bool { self == .heavy || self == .anxious }
}

/// Where a dua is now. Every sincere dua is heard.
enum DuaStage: String, Codable, CaseIterable, Identifiable {
    case asking, waiting, takingAStep, answered, answeredDifferently

    var id: String { rawValue }

    var title: String {
        switch self {
        case .asking: "Making dua"
        case .waiting: "Waiting with patience"
        case .takingAStep: "Taking a step"
        case .answered: "Answered"
        case .answeredDifferently: "Answered differently"
        }
    }

    var isAnswered: Bool { self == .answered || self == .answeredDifferently }
}

struct JournalEntry: Codable, Identifiable, Hashable {
    var id = UUID()
    var date = Date()
    var updated = Date()
    var title = ""
    /// Light markdown: "## Heading" lines, "- " bullets, "- [ ]" checklists.
    var body = ""
    var mood: JournalMood?
    var gratitude = ""
    var verse: VerseRef?
    var tags: [String] = []
    var favorite = false
    /// The template or prompt that started this page, shown as a small label.
    var origin: String?
    // Dua journey
    var isDua = false
    var duaStage: DuaStage = .asking
    var nextStep = ""
    var answeredNote = ""
    var answeredAt: Date?

    var isEmpty: Bool {
        title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            && body.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            && gratitude.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    var displayTitle: String {
        if !title.isEmpty { return title }
        if let firstLine = body.split(separator: "\n").first(where: { !$0.hasPrefix("##") && !$0.isEmpty }) {
            return String(firstLine.prefix(60))
        }
        return mood?.title ?? "Journal page"
    }

    /// Plain text for sharing and export.
    var plainText: String {
        var parts: [String] = []
        parts.append(date.formatted(date: .complete, time: .omitted))
        if !title.isEmpty { parts.append(title) }
        if !body.isEmpty { parts.append(body.replacingOccurrences(of: "## ", with: "")) }
        if !gratitude.isEmpty { parts.append("Grateful for: \(gratitude)") }
        if let verse { parts.append("Verse: Quran \(verse.id)") }
        if isDua && duaStage.isAnswered && !answeredNote.isEmpty { parts.append("How Allah answered: \(answeredNote)") }
        return parts.joined(separator: "\n\n")
    }
}

extension JournalEntry {
    /// Decodes pages saved by any version of the app. A field a page does not have yet takes
    /// its default, so an update can never make her journal look empty.
    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        self.init()
        id = try c.decodeIfPresent(UUID.self, forKey: .id) ?? id
        date = try c.decodeIfPresent(Date.self, forKey: .date) ?? date
        updated = try c.decodeIfPresent(Date.self, forKey: .updated) ?? updated
        title = try c.decodeIfPresent(String.self, forKey: .title) ?? title
        body = try c.decodeIfPresent(String.self, forKey: .body) ?? body
        mood = try? c.decodeIfPresent(JournalMood.self, forKey: .mood)
        gratitude = try c.decodeIfPresent(String.self, forKey: .gratitude) ?? gratitude
        verse = try? c.decodeIfPresent(VerseRef.self, forKey: .verse)
        tags = try c.decodeIfPresent([String].self, forKey: .tags) ?? tags
        favorite = try c.decodeIfPresent(Bool.self, forKey: .favorite) ?? favorite
        origin = try c.decodeIfPresent(String.self, forKey: .origin)
        isDua = try c.decodeIfPresent(Bool.self, forKey: .isDua) ?? isDua
        duaStage = (try? c.decodeIfPresent(DuaStage.self, forKey: .duaStage)) ?? duaStage
        nextStep = try c.decodeIfPresent(String.self, forKey: .nextStep) ?? nextStep
        answeredNote = try c.decodeIfPresent(String.self, forKey: .answeredNote) ?? answeredNote
        answeredAt = try c.decodeIfPresent(Date.self, forKey: .answeredAt)
    }
}

extension LessonNote {
    /// Same promise for lesson notes: older saves always open.
    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        self.init()
        id = try c.decodeIfPresent(UUID.self, forKey: .id) ?? id
        kind = (try? c.decodeIfPresent(Kind.self, forKey: .kind)) ?? kind
        title = try c.decodeIfPresent(String.self, forKey: .title) ?? title
        speaker = try c.decodeIfPresent(String.self, forKey: .speaker) ?? speaker
        date = try c.decodeIfPresent(Date.self, forKey: .date) ?? date
        verses = (try? c.decodeIfPresent([VerseRef].self, forKey: .verses)) ?? verses
        points = try c.decodeIfPresent([String].self, forKey: .points) ?? points
        quotes = try c.decodeIfPresent([String].self, forKey: .quotes) ?? quotes
        takeaway = try c.decodeIfPresent(String.self, forKey: .takeaway) ?? takeaway
        notes = try c.decodeIfPresent(String.self, forKey: .notes) ?? notes
        transcript = try c.decodeIfPresent(String.self, forKey: .transcript) ?? transcript
        minutes = try c.decodeIfPresent(Int.self, forKey: .minutes) ?? minutes
        pinned = try c.decodeIfPresent(Bool.self, forKey: .pinned) ?? pinned
        tags = try c.decodeIfPresent([String].self, forKey: .tags) ?? tags
    }
}

/// Notes from a khutbah, halaqa, class or lecture.
struct LessonNote: Codable, Identifiable, Hashable {
    enum Kind: String, Codable, CaseIterable, Identifiable {
        case khutbah, halaqa, classNote, lecture, study

        var id: String { rawValue }
        var title: String {
            switch self {
            case .khutbah: "Khutbah"
            case .halaqa: "Halaqa"
            case .classNote: "Class"
            case .lecture: "Lecture"
            case .study: "Personal study"
            }
        }
        var symbol: String {
            switch self {
            case .khutbah: "building.columns"
            case .halaqa: "person.3"
            case .classNote: "graduationcap"
            case .lecture: "mic"
            case .study: "book"
            }
        }
    }

    var id = UUID()
    var kind: Kind = .khutbah
    var title = ""
    var speaker = ""
    var date = Date()
    var verses: [VerseRef] = []
    var points: [String] = []
    var quotes: [String] = []
    var takeaway = ""
    var notes = ""
    var transcript = ""
    var minutes = 0
    var pinned = false
    var tags: [String] = []

    var displayTitle: String { title.isEmpty ? "\(kind.title) notes" : title }

    var plainText: String {
        var parts = [displayTitle]
        let meta = [kind.title, speaker.isEmpty ? nil : speaker, date.formatted(date: .abbreviated, time: .omitted)]
            .compactMap { $0 }.joined(separator: " · ")
        parts.append(meta)
        if !verses.isEmpty { parts.append("Verses: " + verses.map { "Quran \($0.id)" }.joined(separator: ", ")) }
        if !points.isEmpty { parts.append("Main points:\n" + points.enumerated().map { "\($0.offset + 1). \($0.element)" }.joined(separator: "\n")) }
        if !quotes.isEmpty { parts.append("Lines worth keeping:\n" + quotes.map { "\"\($0)\"" }.joined(separator: "\n")) }
        if !takeaway.isEmpty { parts.append("Carry this into my week: \(takeaway)") }
        if !notes.isEmpty { parts.append(notes) }
        return parts.joined(separator: "\n\n")
    }
}

// MARK: - Store

/// The journal and lesson notes. Saved as a file on this iPhone only, never synced or uploaded.
@Observable
@MainActor
final class JournalStore {
    private struct Snapshot: Codable {
        var entries: [JournalEntry] = []
        var notes: [LessonNote] = []
        var recentTemplates: [String] = []
    }

    private(set) var entries: [JournalEntry] = []
    private(set) var notes: [LessonNote] = []
    private(set) var recentTemplates: [String] = []
    /// The page saved a moment ago. The list gives it a soft arrival glow.
    private(set) var justSaved: UUID?
    /// True after Face ID or passcode in this session. Never saved, so it resets on launch.
    var isUnlocked = false
    /// Whether she turned the journal lock on. Saved.
    var lockEnabled = UserDefaults.standard.bool(forKey: JournalPrefKey.lock) {
        didSet { UserDefaults.standard.set(lockEnabled, forKey: JournalPrefKey.lock) }
    }

    /// Whether journal content may be shown right now.
    var isAccessible: Bool { isUnlocked || !lockEnabled }

    /// Lock again, for example when the app leaves the screen.
    func relock() {
        guard lockEnabled else { return }
        isUnlocked = false
    }

    @ObservationIgnored private let fileURL: URL
    /// True while the saved file exists but could not be read (for example before first unlock).
    /// Nothing is written until it loads, so a bad read can never overwrite the journal.
    @ObservationIgnored private var loadPending = false
    @ObservationIgnored private var unlockObserver: NSObjectProtocol?

    init() {
        let base = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
        fileURL = base.appendingPathComponent("Journal", isDirectory: true).appendingPathComponent("journal.json")
        load()
        unlockObserver = NotificationCenter.default.addObserver(
            forName: UIApplication.protectedDataDidBecomeAvailableNotification, object: nil, queue: .main
        ) { [weak self] _ in
            Task { @MainActor in
                guard let self, self.loadPending else { return }
                self.load()
            }
        }
    }

    private func load() {
        guard FileManager.default.fileExists(atPath: fileURL.path) else {
            loadPending = false
            return
        }
        guard let data = try? Data(contentsOf: fileURL) else {
            loadPending = true
            return
        }
        if let snapshot = try? JSONDecoder().decode(Snapshot.self, from: data) {
            entries = snapshot.entries
            notes = snapshot.notes
            recentTemplates = snapshot.recentTemplates
        } else {
            // Unreadable format: keep a copy instead of overwriting it.
            let backup = fileURL.deletingLastPathComponent()
                .appendingPathComponent("journal-backup-\(Int(Date.now.timeIntervalSince1970)).json")
            try? FileManager.default.moveItem(at: fileURL, to: backup)
        }
        loadPending = false
    }

    // MARK: Entries

    /// Newest first.
    var sortedEntries: [JournalEntry] { entries.sorted { $0.date > $1.date } }

    func entry(id: UUID) -> JournalEntry? { entries.first { $0.id == id } }

    func save(_ entry: JournalEntry) {
        var entry = entry
        entry.updated = .now
        if entry.isDua && entry.duaStage.isAnswered {
            if entry.answeredAt == nil { entry.answeredAt = .now }
        } else {
            entry.answeredAt = nil
        }
        if let index = entries.firstIndex(where: { $0.id == entry.id }) {
            entries[index] = entry
        } else {
            entries.append(entry)
        }
        persist()
        justSaved = entry.id
        let saved = entry.id
        Task {
            try? await Task.sleep(for: .seconds(2.5))
            if justSaved == saved { justSaved = nil }
        }
    }

    func delete(_ entry: JournalEntry) {
        entries.removeAll { $0.id == entry.id }
        persist()
    }

    func toggleFavorite(_ entry: JournalEntry) {
        guard var current = self.entry(id: entry.id) else { return }
        current.favorite.toggle()
        save(current)
    }

    var lastEdited: JournalEntry? { entries.max { $0.updated < $1.updated } }

    /// A page from a year ago, or else a month ago.
    var onThisDay: JournalEntry? {
        let calendar = Calendar.current
        for months in [12, 1] {
            guard let past = calendar.date(byAdding: .month, value: -months, to: .now) else { continue }
            if let match = sortedEntries.first(where: { calendar.isDate($0.date, inSameDayAs: past) }) { return match }
        }
        return nil
    }

    var allTags: [String] {
        var counts: [String: Int] = [:]
        for tag in entries.flatMap(\.tags) { counts[tag, default: 0] += 1 }
        return counts.sorted { $0.value > $1.value }.map(\.key)
    }

    func markTemplateUsed(_ id: String) {
        recentTemplates.removeAll { $0 == id }
        recentTemplates.insert(id, at: 0)
        recentTemplates = Array(recentTemplates.prefix(6))
        persist()
    }

    // MARK: Notes

    func note(id: UUID) -> LessonNote? { notes.first { $0.id == id } }

    func save(_ note: LessonNote) {
        if let index = notes.firstIndex(where: { $0.id == note.id }) {
            notes[index] = note
        } else {
            notes.append(note)
        }
        persist()
    }

    func delete(_ note: LessonNote) {
        notes.removeAll { $0.id == note.id }
        persist()
    }

    /// Pinned first, then newest.
    var sortedNotes: [LessonNote] {
        notes.sorted { a, b in a.pinned != b.pinned ? a.pinned : a.date > b.date }
    }

    // MARK: Export and erase

    /// The whole journal and notes as one plain text file.
    func exportFile() -> URL? {
        var text = "Quran for Women · Journal\nExported \(Date.now.formatted(date: .long, time: .shortened))\n\n"
        for entry in sortedEntries { text += entry.plainText + "\n\n----------\n\n" }
        if !notes.isEmpty {
            text += "LESSON NOTES\n\n"
            for note in sortedNotes { text += note.plainText + "\n\n----------\n\n" }
        }
        let url = FileManager.default.temporaryDirectory.appendingPathComponent("My Journal.txt")
        do {
            try Data(text.utf8).write(to: url, options: [.atomic, .completeFileProtection])
            return url
        } catch {
            return nil
        }
    }

    func eraseAll() {
        entries = []
        notes = []
        recentTemplates = []
        try? FileManager.default.removeItem(at: fileURL)
    }

    private func persist() {
        guard !loadPending else { return }
        let snapshot = Snapshot(entries: entries, notes: notes, recentTemplates: recentTemplates)
        guard let data = try? JSONEncoder().encode(snapshot) else { return }
        try? FileManager.default.createDirectory(at: fileURL.deletingLastPathComponent(), withIntermediateDirectories: true)
        // Encrypted at rest while the iPhone is locked.
        try? data.write(to: fileURL, options: [.atomic, .completeFileProtectionUnlessOpen])
    }
}
