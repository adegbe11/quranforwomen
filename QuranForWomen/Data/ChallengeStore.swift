import Foundation
import Observation

/// One challenge. Every day is built from the bundled, verified content only.
struct Challenge: Identifiable, Hashable {
    enum Kind: Hashable {
        /// One bundled dhikr phrase a day, with its own target.
        case dhikr
        /// A few pages of the mushaf a day, from where she is reading.
        case quranPages(perDay: Int)
        /// The bundled morning duas, one a day.
        case morning
        /// The bundled evening duas, one a day.
        case evening
        /// A gratitude verse and a journal prompt a day.
        case gratitude
        /// The 30 day page plan.
        case quranMonth
    }

    let id: String
    let title: String
    let symbol: String
    let days: Int
    let kind: Kind
}

/// What one day of a challenge asks for.
enum ChallengeStep: Hashable {
    case dhikr(DhikrPhrase)
    case pages(ClosedRange<Int>)
    case dua(Dua)
    case gratitude(verse: VerseRef, prompt: String)
}

enum ChallengeCatalog {
    static let all: [Challenge] = [
        Challenge(id: "dhikr-7", title: "7 days of dhikr", symbol: "circle.dotted", days: 7, kind: .dhikr),
        Challenge(id: "quran-7", title: "7 days of Quran", symbol: "book", days: 7, kind: .quranPages(perDay: 4)),
        Challenge(id: "morning-7", title: "Morning adhkar for 7 days", symbol: "sunrise", days: 7, kind: .morning),
        Challenge(id: "evening-7", title: "Evening adhkar for 7 days", symbol: "moon", days: 7, kind: .evening),
        Challenge(id: "gratitude-7", title: "7 days of gratitude", symbol: "heart", days: 7, kind: .gratitude),
        Challenge(id: "quran-30", title: "30 days with the Quran", symbol: "calendar", days: 30, kind: .quranMonth),
    ]

    static func challenge(id: String) -> Challenge? { all.first { $0.id == id } }

    /// Challenges whose bundled content is present. A challenge with nothing to show is left out.
    @MainActor
    static func available(content: ContentStore) -> [Challenge] {
        all.filter { step($0, day: 0, content: content, startPage: 1) != nil }
    }

    /// The bundled morning and evening duas. Chosen by their own titles and notes, never by guesswork.
    @MainActor
    static func adhkar(morning: Bool, content: ContentStore) -> [Dua] {
        let word = morning ? "morning" : "evening"
        return content.duas.filter { dua in
            guard dua.category == "morning-evening" else { return false }
            let words = (dua.title + " " + (dua.note ?? "")).lowercased()
            return words.contains(word) || (morning && words.contains("waking"))
        }
    }

    /// The step for a zero based day.
    @MainActor
    static func step(_ challenge: Challenge, day: Int, content: ContentStore, startPage: Int) -> ChallengeStep? {
        switch challenge.kind {
        case .dhikr:
            guard !content.dhikr.isEmpty else { return nil }
            return .dhikr(content.dhikr[day % content.dhikr.count])
        case .quranPages(let perDay):
            let first = min(max(startPage, 1), ReadingPlan.totalPages - perDay * challenge.days + 1) + day * perDay
            return .pages(first...(first + perDay - 1))
        case .morning, .evening:
            let duas = adhkar(morning: challenge.kind == .morning, content: content)
            guard !duas.isEmpty else { return nil }
            return .dua(duas[day % duas.count])
        case .gratitude:
            let verses = content.collection(id: "grateful")?.refs ?? []
            let prompts = JournalContent.packs.first { $0.id == "shukr" }?.prompts ?? []
            guard !verses.isEmpty, !prompts.isEmpty else { return nil }
            return .gratitude(verse: verses[day % verses.count], prompt: prompts[day % prompts.count])
        case .quranMonth:
            return .pages(ReadingPlan(days: challenge.days, start: .now).pages(forDay: day))
        }
    }
}

/// Her place in a challenge.
struct ChallengeProgress: Codable, Hashable {
    var started: Date
    var done: Set<Int> = []
    /// The mushaf page a page challenge began from.
    var startPage: Int = 1
}

/// Challenges she has started. Missing a day never breaks anything: she simply continues
/// from the next day not yet done. Stored only on this iPhone.
@Observable
@MainActor
final class ChallengeStore {
    private static let key = "challenges.progress"

    private(set) var progress: [String: ChallengeProgress] = [:]

    init() {
        if let data = UserDefaults.standard.data(forKey: Self.key),
           let saved = try? JSONDecoder().decode([String: ChallengeProgress].self, from: data) {
            progress = saved
        }
    }

    func state(of challenge: Challenge) -> ChallengeProgress? { progress[challenge.id] }

    /// Challenges started and not yet finished.
    var activeCount: Int {
        ChallengeCatalog.all.filter { challenge in
            guard let value = progress[challenge.id] else { return false }
            return value.done.count < challenge.days
        }.count
    }

    func start(_ challenge: Challenge) {
        let saved = UserDefaults.standard.integer(forKey: "mushaf.page")
        progress[challenge.id] = ChallengeProgress(started: .now, startPage: max(saved, 1))
        save()
    }

    func isDone(day: Int, in challenge: Challenge) -> Bool {
        progress[challenge.id]?.done.contains(day) ?? false
    }

    func setDone(_ done: Bool, day: Int, in challenge: Challenge) {
        guard var value = progress[challenge.id], (0..<challenge.days).contains(day) else { return }
        if done { value.done.insert(day) } else { value.done.remove(day) }
        progress[challenge.id] = value
        save()
    }

    /// The first day not yet done, or the last day once all are done.
    func nextDay(in challenge: Challenge) -> Int {
        let done = progress[challenge.id]?.done ?? []
        return (0..<challenge.days).first { !done.contains($0) } ?? challenge.days - 1
    }

    func isComplete(_ challenge: Challenge) -> Bool {
        (progress[challenge.id]?.done.count ?? 0) >= challenge.days
    }

    func restart(_ challenge: Challenge) {
        start(challenge)
    }

    func leave(_ challenge: Challenge) {
        progress[challenge.id] = nil
        save()
    }

    /// Erase My Data.
    func eraseAll() {
        progress = [:]
        UserDefaults.standard.removeObject(forKey: Self.key)
    }

    private func save() {
        if let data = try? JSONEncoder().encode(progress) {
            UserDefaults.standard.set(data, forKey: Self.key)
        }
    }
}
