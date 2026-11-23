import SwiftUI
import WidgetKit

struct PlanDay {
    /// One based day of the plan.
    let day: Int
    let days: Int
    let pages: ClosedRange<Int>
    let finished: Int
}

struct ProgressEntry: TimelineEntry {
    let date: Date
    let surahsFinished: Int
    let plan: PlanDay?
}

struct ProgressProvider: TimelineProvider {
    func placeholder(in context: Context) -> ProgressEntry {
        ProgressEntry(date: .now, surahsFinished: 12, plan: nil)
    }

    func getSnapshot(in context: Context, completion: @escaping (ProgressEntry) -> Void) {
        completion(entry(for: .now))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<ProgressEntry>) -> Void) {
        let now = Date.now
        completion(Timeline(entries: [entry(for: now)], policy: .after(WidgetShared.nextMidnight(after: now))))
    }

    private func entry(for date: Date) -> ProgressEntry {
        let d = WidgetShared.defaults
        let surahs = d?.integer(forKey: WidgetShared.surahsFinishedKey) ?? 0
        var plan: PlanDay?
        if let d, let days = d.object(forKey: WidgetShared.planDaysKey) as? Int, days > 0 {
            let start = Date(timeIntervalSince1970: d.double(forKey: WidgetShared.planStartKey))
            // Same as ReadingPlan.dayIndex and ReadingPlan.pages in the app.
            let calendar = Calendar.current
            let elapsed = calendar.dateComponents([.day], from: calendar.startOfDay(for: start), to: calendar.startOfDay(for: date)).day ?? 0
            let index = min(max(elapsed, 0), days - 1)
            let total = 604
            let first = index * total / days + 1
            let last = max(first, (index + 1) * total / days)
            plan = PlanDay(day: index + 1, days: days, pages: first...last, finished: d.integer(forKey: WidgetShared.planFinishedKey))
        }
        return ProgressEntry(date: date, surahsFinished: min(max(surahs, 0), 114), plan: plan)
    }
}

struct ProgressWidgetView: View {
    let entry: ProgressEntry
    @Environment(\.widgetFamily) private var family

    var body: some View {
        switch family {
        case .systemMedium:
            HStack(alignment: .top, spacing: 16) {
                surahs
                VStack(alignment: .leading, spacing: 6) {
                    Spacer(minLength: 0)
                    if let plan = entry.plan {
                        Text("Day \(plan.day) of \(plan.days)")
                            .font(.system(.subheadline, design: .serif).weight(.semibold))
                            .foregroundStyle(WidgetPalette.text)
                        Text(plan.pages.count == 1 ? "Page \(plan.pages.lowerBound)" : "Pages \(plan.pages.lowerBound) to \(plan.pages.upperBound)")
                            .font(.footnote)
                            .foregroundStyle(WidgetPalette.secondary)
                        bar(Double(plan.finished) / Double(plan.days))
                            .accessibilityLabel("\(plan.finished) of \(plan.days) days read")
                    } else {
                        Text("All 114 surahs")
                            .font(.footnote)
                            .foregroundStyle(WidgetPalette.secondary)
                        bar(Double(entry.surahsFinished) / 114)
                            .accessibilityLabel("\(entry.surahsFinished) of 114 surahs finished")
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
        default:
            surahs
        }
    }

    private var surahs: some View {
        VStack(alignment: .leading, spacing: 2) {
            WidgetEyebrow(text: "QURAN")
            Spacer(minLength: 0)
            HStack(alignment: .firstTextBaseline, spacing: 4) {
                Text("\(entry.surahsFinished)")
                    .font(.system(size: 34, weight: .semibold, design: .serif))
                    .foregroundStyle(WidgetPalette.text)
                    .contentTransition(.numericText(value: Double(entry.surahsFinished)))
                Text("of 114")
                    .font(.footnote)
                    .foregroundStyle(WidgetPalette.secondary)
            }
            Text("surahs finished")
                .font(.footnote)
                .foregroundStyle(WidgetPalette.secondary)
            if family == .systemSmall, let plan = entry.plan {
                Text("Day \(plan.day) of \(plan.days)")
                    .font(.system(size: 12, weight: .medium))
                    .foregroundStyle(WidgetPalette.gold)
                    .padding(.top, 2)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .combine)
    }

    private func bar(_ fraction: Double) -> some View {
        GeometryReader { proxy in
            ZStack(alignment: .leading) {
                Capsule().fill(WidgetPalette.track)
                Capsule()
                    .fill(WidgetPalette.accent)
                    .frame(width: proxy.size.width * min(max(fraction, 0), 1))
            }
        }
        .frame(height: 6)
    }
}

struct ReadingProgressWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "ReadingProgress", provider: ProgressProvider()) { entry in
            ProgressWidgetView(entry: entry)
                .containerBackground(for: .widget) { WidgetPalette.background }
                .widgetURL(URL(string: entry.plan == nil ? "quranforwomen://progress" : "quranforwomen://plan"))
        }
        .configurationDisplayName("Reading Progress")
        .description("Surahs finished and your reading plan.")
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}
