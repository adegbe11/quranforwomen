import SwiftUI
import WidgetKit

/// Keys the app writes through WidgetBridge (App/WidgetSync.swift). Keep the two in step.
extension WidgetShared {
    static let latitudeKey = "widget.prayer.latitude"
    static let longitudeKey = "widget.prayer.longitude"
    static let methodKey = "widget.prayer.method"
    static let asrKey = "widget.prayer.asr"
    static let placeKey = "widget.prayer.place"
    static let surahsFinishedKey = "widget.progress.surahs"
    static let planDaysKey = "widget.plan.days"
    static let planStartKey = "widget.plan.start"
    static let planFinishedKey = "widget.plan.finished"
    static let dhikrDayKey = "widget.dhikr.day"
    static let dhikrTodayKey = "widget.dhikr.today"

    static var defaults: UserDefaults? { UserDefaults(suiteName: suite) }

    /// Same as the app's daily index (ContentStore.dayIndex).
    static func dayNumber(_ date: Date) -> Int {
        Calendar.current.ordinality(of: .day, in: .era, for: date) ?? 0
    }

    /// Same format as LibraryStore.dayKey in the app.
    static func dayKey(_ date: Date) -> String {
        let c = Calendar.current.dateComponents([.year, .month, .day], from: date)
        return String(format: "%04d-%02d-%02d", c.year ?? 0, c.month ?? 0, c.day ?? 0)
    }

    /// A minute after the start of the next day.
    static func nextMidnight(after date: Date) -> Date {
        Calendar.current.startOfDay(for: date).addingTimeInterval(24 * 3600 + 60)
    }
}

/// The small capitals label at the top of each widget.
struct WidgetEyebrow: View {
    let text: String

    var body: some View {
        Text(text)
            .font(.system(size: 10, weight: .semibold))
            .tracking(0.6)
            .foregroundStyle(WidgetPalette.accent)
            .lineLimit(1)
    }
}
