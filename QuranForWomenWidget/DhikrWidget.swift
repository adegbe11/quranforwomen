import SwiftUI
import WidgetKit

struct DhikrEntry: TimelineEntry {
    let date: Date
    let count: Int
}

struct DhikrProvider: TimelineProvider {
    func placeholder(in context: Context) -> DhikrEntry { DhikrEntry(date: .now, count: 33) }

    func getSnapshot(in context: Context, completion: @escaping (DhikrEntry) -> Void) {
        completion(entry(for: .now))
    }

    /// Starts again from zero after midnight.
    func getTimeline(in context: Context, completion: @escaping (Timeline<DhikrEntry>) -> Void) {
        let now = Date.now
        let midnight = WidgetShared.nextMidnight(after: now)
        let entries = [entry(for: now), DhikrEntry(date: midnight, count: 0)]
        completion(Timeline(entries: entries, policy: .after(midnight)))
    }

    private func entry(for date: Date) -> DhikrEntry {
        let d = WidgetShared.defaults
        let isToday = d?.string(forKey: WidgetShared.dhikrDayKey) == WidgetShared.dayKey(date)
        return DhikrEntry(date: date, count: isToday ? (d?.integer(forKey: WidgetShared.dhikrTodayKey) ?? 0) : 0)
    }
}

struct DhikrWidgetView: View {
    let entry: DhikrEntry
    @Environment(\.widgetFamily) private var family

    var body: some View {
        switch family {
        case .accessoryCircular:
            VStack(spacing: 0) {
                Text("\(entry.count)")
                    .font(.system(size: 18, weight: .semibold, design: .rounded))
                    .minimumScaleFactor(0.5)
                    .widgetAccentable()
                Text("today")
                    .font(.system(size: 9))
            }
            .accessibilityElement(children: .combine)
        default:
            VStack(alignment: .leading, spacing: 2) {
                WidgetEyebrow(text: "DHIKR")
                Spacer(minLength: 0)
                Text("\(entry.count)")
                    .font(.system(size: 38, weight: .semibold, design: .serif))
                    .foregroundStyle(WidgetPalette.text)
                    .minimumScaleFactor(0.6)
                    .lineLimit(1)
                    .contentTransition(.numericText(value: Double(entry.count)))
                Text("today")
                    .font(.footnote)
                    .foregroundStyle(WidgetPalette.gold)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .accessibilityElement(children: .combine)
        }
    }
}

struct DhikrTodayWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "DhikrToday", provider: DhikrProvider()) { entry in
            DhikrWidgetView(entry: entry)
                .containerBackground(for: .widget) { WidgetPalette.background }
                .widgetURL(URL(string: "quranforwomen://dhikr"))
        }
        .configurationDisplayName("Dhikr")
        .description("Your dhikr count for today.")
        .supportedFamilies([.systemSmall, .accessoryCircular])
    }
}
