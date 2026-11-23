import Foundation
import Observation

/// A reading plan she finished, kept so she can look back on it.
struct FinishedPlan: Codable, Hashable, Identifiable {
    var title: String
    var start: Date
    var finish: Date

    var id: Date { start }
}

extension ReadingPlan {
    /// A short name for the plan, used on the plan screen and in her finished plans.
    var title: String {
        if let first = firstSurah {
            if first == 1 && (lastSurah ?? 114) >= 114 { return "One surah at a time" }
            if first == 78 { return "Short surahs" }
            return "Surahs \(first) to \(lastSurah ?? 114)"
        }
        switch days {
        case 120: return "4 months"
        case 365: return "One year"
        default: return "\(days) days"
        }
    }
}

/// Finished reading plans, newest first. Stored on this iPhone under its own key.
@Observable
@MainActor
final class PlanHistoryStore {
    private static let storageKey = "planHistory.v1"

    private(set) var finished: [FinishedPlan] = []

    @ObservationIgnored private let defaults: UserDefaults

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
        if let data = defaults.data(forKey: Self.storageKey),
           let saved = try? JSONDecoder().decode([FinishedPlan].self, from: data) {
            finished = saved
        }
    }

    /// Keeps the list in step with the current plan: a complete plan is listed,
    /// and a plan that is no longer complete (a day unmarked) is taken off again.
    func sync(with plan: ReadingPlan?) {
        guard let plan else { return }
        let index = finished.firstIndex { Self.same($0.start, plan.start) }
        if plan.isComplete {
            guard index == nil else { return }
            finished.insert(FinishedPlan(title: plan.title, start: plan.start, finish: .now), at: 0)
            save()
        } else if let index {
            finished.remove(at: index)
            save()
        }
    }

    func eraseAll() {
        finished = []
        defaults.removeObject(forKey: Self.storageKey)
    }

    private static func same(_ a: Date, _ b: Date) -> Bool { abs(a.timeIntervalSince(b)) < 1 }

    private func save() {
        guard let data = try? JSONEncoder().encode(finished) else { return }
        defaults.set(data, forKey: Self.storageKey)
    }
}
