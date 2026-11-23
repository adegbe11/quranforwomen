import Foundation
@testable import QuranForWomen

/// Stores loaded once and shared by every test. They only read the bundled JSON,
/// so sharing them keeps the suite fast without making tests depend on each other.
/// The tests run inside the app, so Bundle.main is the app bundle with its resources.
@MainActor
enum Fixtures {
    static let quran = QuranStore()
    static let content = ContentStore()

    /// Every verse of the Quran in mushaf order, 1:1 to 114:6.
    static var allVerses: [VerseRef] {
        quran.surahs.flatMap { surah in
            (1...surah.verseCount).map { VerseRef(surah: surah.id, ayah: $0) }
        }
    }

    /// A Gregorian calendar fixed to one time zone, so results never depend on the test machine.
    nonisolated static func calendar(_ timeZoneID: String) -> Calendar {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(identifier: timeZoneID)!
        return calendar
    }

    /// Noon on a given day in a given calendar.
    nonisolated static func noon(_ year: Int, _ month: Int, _ day: Int, in calendar: Calendar) -> Date {
        calendar.date(from: DateComponents(year: year, month: month, day: day, hour: 12))!
    }
}
