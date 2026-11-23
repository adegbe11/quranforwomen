import Foundation

/// A day in the Islamic (Hijri) calendar. Month is 1 to 12.
struct HijriDay: Hashable {
    let year: Int
    let month: Int
    let day: Int
}

/// The Umm al-Qura Islamic calendar, worked out on this iPhone (same tables as the Android app).
/// A Hijri month is identified by an index: year * 12 + month - 1.
enum Hijri {
    static let ramadan = 9

    static let monthNames = [
        "Muharram", "Safar", "Rabi al-Awwal", "Rabi al-Thani", "Jumada al-Awwal", "Jumada al-Thani",
        "Rajab", "Sha'ban", "Ramadan", "Shawwal", "Dhul Qa'dah", "Dhul Hijjah",
    ]

    static var calendar: Calendar {
        var c = Calendar(identifier: .islamicUmmAlQura)
        c.timeZone = .current
        return c
    }

    /// The Hijri day for a date, read at midday so the hour never tips it into the next day.
    static func of(_ date: Date) -> HijriDay {
        let noon = Calendar.current.date(bySettingHour: 12, minute: 0, second: 0, of: date) ?? date
        let c = calendar.dateComponents([.year, .month, .day], from: noon)
        return HijriDay(year: c.year ?? 0, month: c.month ?? 1, day: c.day ?? 1)
    }

    static func index(year: Int, month: Int) -> Int { year * 12 + month - 1 }
    static func index(of date: Date) -> Int {
        let h = of(date)
        return index(year: h.year, month: h.month)
    }
    static func yearOf(_ index: Int) -> Int { index / 12 }
    static func monthOf(_ index: Int) -> Int { index % 12 + 1 }

    /// The start of the Gregorian day on which a Hijri month begins.
    static func start(_ index: Int) -> Date {
        let cal = calendar
        var parts = DateComponents()
        parts.era = cal.component(.era, from: .now)
        parts.year = yearOf(index)
        parts.month = monthOf(index)
        parts.day = 1
        parts.hour = 12
        let date = cal.date(from: parts) ?? .now
        return Calendar.current.startOfDay(for: date)
    }

    /// 29 or 30.
    static func length(_ index: Int) -> Int {
        let days = Calendar.current.dateComponents([.day], from: start(index), to: start(index + 1)).day ?? 30
        return min(max(days, 29), 30)
    }

    static func monthName(_ month: Int) -> String { monthNames[min(max(month - 1, 0), 11)] }

    static func isRamadan(_ date: Date) -> Bool { of(date).month == ramadan }

    /// The widely known days, and nothing more.
    static func specialDay(month: Int, day: Int) -> String? {
        switch (month, day) {
        case (1, 1): return "Islamic New Year"
        case (1, 10): return "Ashura"
        case (9, 1): return "First day of Ramadan"
        case (9, 21...): return "Last ten nights"
        case (10, 1): return "Eid al-Fitr"
        case (12, 9): return "Day of Arafah"
        case (12, 10): return "Eid al-Adha"
        default: return nil
        }
    }
}
