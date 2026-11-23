import SwiftUI
import WidgetKit

/// Her saved location and calculation settings, published by the app.
private struct WidgetPrayerSettings {
    let latitude: Double
    let longitude: Double
    let method: CalculationMethod
    let asr: AsrMethod
    let place: String?

    static func load() -> WidgetPrayerSettings? {
        guard let d = WidgetShared.defaults, d.object(forKey: WidgetShared.latitudeKey) != nil,
              d.object(forKey: WidgetShared.longitudeKey) != nil else { return nil }
        return WidgetPrayerSettings(
            latitude: d.double(forKey: WidgetShared.latitudeKey),
            longitude: d.double(forKey: WidgetShared.longitudeKey),
            method: CalculationMethod(rawValue: d.string(forKey: WidgetShared.methodKey) ?? "") ?? .muslimWorldLeague,
            asr: AsrMethod(rawValue: d.string(forKey: WidgetShared.asrKey) ?? "") ?? .standard,
            place: d.string(forKey: WidgetShared.placeKey)
        )
    }

    func schedule(for date: Date) -> PrayerSchedule {
        PrayerCalculator.schedule(for: date, latitude: latitude, longitude: longitude, method: method, asr: asr)
    }
}

struct PrayerTimeItem {
    let prayer: Prayer
    let time: Date
}

struct PrayerEntry: TimelineEntry {
    let date: Date
    /// The next prayer and its time. Nil until she sets her location in the app.
    let next: PrayerTimeItem?
    /// The five prayers of the day the next prayer falls on.
    let day: [PrayerTimeItem]
    let place: String?

    static func empty(_ date: Date) -> PrayerEntry {
        PrayerEntry(date: date, next: nil, day: [], place: nil)
    }
}

struct PrayerProvider: TimelineProvider {
    func placeholder(in context: Context) -> PrayerEntry {
        let now = Date.now
        let times = Prayer.prayers.enumerated().map { index, prayer in
            PrayerTimeItem(prayer: prayer, time: Calendar.current.startOfDay(for: now).addingTimeInterval(Double(5 + index * 3) * 3600))
        }
        return PrayerEntry(date: now, next: times[2], day: times, place: nil)
    }

    func getSnapshot(in context: Context, completion: @escaping (PrayerEntry) -> Void) {
        guard let settings = WidgetPrayerSettings.load() else {
            completion(context.isPreview ? placeholder(in: context) : .empty(.now))
            return
        }
        completion(entry(at: .now, settings))
    }

    /// One entry now and one at each prayer time over the next day, so the widget moves on by itself.
    func getTimeline(in context: Context, completion: @escaping (Timeline<PrayerEntry>) -> Void) {
        let now = Date.now
        guard let settings = WidgetPrayerSettings.load() else {
            // The app reloads this widget when she sets her location.
            completion(Timeline(entries: [.empty(now)], policy: .never))
            return
        }
        var moments = [now]
        for offset in 0...1 {
            guard let day = Calendar.current.date(byAdding: .day, value: offset, to: now) else { continue }
            let schedule = settings.schedule(for: day)
            for prayer in Prayer.prayers {
                if let time = schedule.time(prayer), time > now { moments.append(time) }
            }
        }
        // Also redraw at midnight, so the times of the new day show after Isha.
        moments.append(Calendar.current.startOfDay(for: now).addingTimeInterval(24 * 3600 + 1))
        moments = Array(Set(moments)).sorted()
        let entries = moments.map { entry(at: $0, settings) }
        let last = moments.last ?? now.addingTimeInterval(6 * 3600)
        completion(Timeline(entries: entries, policy: .after(last)))
    }

    private func entry(at moment: Date, _ settings: WidgetPrayerSettings) -> PrayerEntry {
        var schedule = settings.schedule(for: moment)
        var next = schedule.next(after: moment)
        if next == nil, let tomorrow = Calendar.current.date(byAdding: .day, value: 1, to: moment) {
            schedule = settings.schedule(for: tomorrow)
            if let fajr = schedule.time(.fajr) { next = (Prayer.fajr, fajr) }
        }
        let day = Prayer.prayers.compactMap { prayer in
            schedule.time(prayer).map { PrayerTimeItem(prayer: prayer, time: $0) }
        }
        return PrayerEntry(
            date: moment,
            next: next.map { PrayerTimeItem(prayer: $0.0, time: $0.1) },
            day: day,
            place: settings.place
        )
    }
}

struct PrayerWidgetView: View {
    let entry: PrayerEntry
    @Environment(\.widgetFamily) private var family

    var body: some View {
        if let next = entry.next {
            content(next)
        } else {
            noLocation
        }
    }

    @ViewBuilder
    private func content(_ next: PrayerTimeItem) -> some View {
        switch family {
        case .accessoryInline:
            Text("\(next.prayer.name) \(timeText(next.time))")
        case .accessoryRectangular:
            VStack(alignment: .leading, spacing: 1) {
                Text("Next prayer")
                    .font(.caption2.weight(.semibold))
                Text(next.prayer.name)
                    .font(.headline)
                    .widgetAccentable()
                Text(next.time, format: .dateTime.hour().minute())
                    .font(.caption)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .accessibilityElement(children: .combine)
        case .systemMedium:
            HStack(alignment: .top, spacing: 16) {
                summary(next)
                VStack(spacing: 5) {
                    ForEach(entry.day, id: \.prayer) { item in
                        row(item, isNext: item.prayer == next.prayer && item.time == next.time)
                    }
                }
                .frame(maxWidth: .infinity)
            }
        default:
            summary(next)
        }
    }

    private func summary(_ next: PrayerTimeItem) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            WidgetEyebrow(text: "NEXT PRAYER")
            Spacer(minLength: 0)
            Text(next.prayer.name)
                .font(.system(size: 26, weight: .semibold, design: .serif))
                .foregroundStyle(WidgetPalette.text)
                .lineLimit(1)
                .minimumScaleFactor(0.7)
            Text(next.time, format: .dateTime.hour().minute())
                .font(.system(.title3, design: .rounded).weight(.medium))
                .foregroundStyle(WidgetPalette.gold)
                .contentTransition(.numericText())
            if let place = entry.place {
                Text(place)
                    .font(.system(size: 11))
                    .foregroundStyle(WidgetPalette.secondary)
                    .lineLimit(1)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .combine)
    }

    private func row(_ item: PrayerTimeItem, isNext: Bool) -> some View {
        HStack {
            Text(item.prayer.name)
                .font(.system(size: 13, weight: isNext ? .semibold : .regular))
            Spacer(minLength: 4)
            Text(item.time, format: .dateTime.hour().minute())
                .font(.system(size: 13, weight: isNext ? .semibold : .regular, design: .rounded))
                .monospacedDigit()
        }
        .foregroundStyle(isNext ? WidgetPalette.accent : WidgetPalette.secondary)
        .accessibilityElement(children: .combine)
    }

    @ViewBuilder
    private var noLocation: some View {
        switch family {
        case .accessoryInline, .accessoryRectangular:
            Text("Prayer times")
        default:
            VStack(alignment: .leading, spacing: 4) {
                WidgetEyebrow(text: "NEXT PRAYER")
                Spacer(minLength: 0)
                Text("Add your location")
                    .font(.system(.subheadline, design: .serif).weight(.semibold))
                    .foregroundStyle(WidgetPalette.text)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
    }

    private func timeText(_ date: Date) -> String {
        date.formatted(date: .omitted, time: .shortened)
    }
}

struct NextPrayerWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "NextPrayer", provider: PrayerProvider()) { entry in
            PrayerWidgetView(entry: entry)
                .containerBackground(for: .widget) { WidgetPalette.background }
                .widgetURL(URL(string: "quranforwomen://prayer"))
        }
        .configurationDisplayName("Next Prayer")
        .description("The next prayer and today's times.")
        .supportedFamilies([.systemSmall, .systemMedium, .accessoryRectangular, .accessoryInline])
    }
}
