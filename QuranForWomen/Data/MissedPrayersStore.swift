import Foundation
import Observation

/// Prayers she wants to make up, set by her, counting down as she marks them. Kept only on this iPhone.
@Observable
@MainActor
final class MissedPrayersStore {
    static let maximum = 99999

    private(set) var counts: [Prayer: Int]
    private(set) var madeUp: Int

    init() {
        let d = UserDefaults.standard
        var saved: [Prayer: Int] = [:]
        for prayer in Prayer.prayers {
            saved[prayer] = d.integer(forKey: Self.key(prayer))
        }
        counts = saved
        madeUp = d.integer(forKey: "missed.madeUp")
    }

    func remaining(_ prayer: Prayer) -> Int { counts[prayer] ?? 0 }

    var totalRemaining: Int { counts.values.reduce(0, +) }

    func setRemaining(_ prayer: Prayer, _ value: Int) {
        let v = min(max(value, 0), Self.maximum)
        counts[prayer] = v
        UserDefaults.standard.set(v, forKey: Self.key(prayer))
    }

    /// Marks one made up. Returns false when none are left.
    @discardableResult
    func markOne(_ prayer: Prayer) -> Bool {
        let current = remaining(prayer)
        guard current > 0 else { return false }
        setRemaining(prayer, current - 1)
        madeUp += 1
        UserDefaults.standard.set(madeUp, forKey: "missed.madeUp")
        return true
    }

    func undoOne(_ prayer: Prayer) {
        setRemaining(prayer, remaining(prayer) + 1)
        madeUp = max(0, madeUp - 1)
        UserDefaults.standard.set(madeUp, forKey: "missed.madeUp")
    }

    /// Erase My Data.
    func eraseAll() {
        for prayer in Prayer.prayers {
            counts[prayer] = 0
            UserDefaults.standard.removeObject(forKey: Self.key(prayer))
        }
        madeUp = 0
        UserDefaults.standard.removeObject(forKey: "missed.madeUp")
    }

    private static func key(_ prayer: Prayer) -> String { "missed.\(prayer.rawValue)" }
}
