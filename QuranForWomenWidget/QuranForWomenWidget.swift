import SwiftUI
import WidgetKit

/// Shared with the app through the App Group.
enum WidgetShared {
    static let suite = "group.com.quranforwomen.app"
    static let translationsKey = "widget.translations"
    static let rtlKey = "widget.rtl"
}

private struct WidgetVerse: Decodable {
    let ref: String
    let surahName: String
    let arabic: String
}

struct VerseEntry: TimelineEntry {
    let date: Date
    let ref: String
    let surahName: String
    let arabic: String
    let translation: String?
    let translationIsRTL: Bool
}

struct VerseProvider: TimelineProvider {
    func placeholder(in context: Context) -> VerseEntry { entry(for: .now) }

    func getSnapshot(in context: Context, completion: @escaping (VerseEntry) -> Void) {
        completion(entry(for: .now))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<VerseEntry>) -> Void) {
        let now = Date.now
        let tomorrow = Calendar.current.startOfDay(for: now).addingTimeInterval(24 * 3600 + 60)
        completion(Timeline(entries: [entry(for: now)], policy: .after(tomorrow)))
    }

    /// Same daily pick as the app's Today screen.
    private func entry(for date: Date) -> VerseEntry {
        let verses = Self.verses
        guard !verses.isEmpty else {
            return VerseEntry(date: date, ref: "", surahName: "", arabic: "", translation: nil, translationIsRTL: false)
        }
        let day = Calendar.current.ordinality(of: .day, in: .era, for: date) ?? 0
        let verse = verses[day % verses.count]
        let shared = UserDefaults(suiteName: WidgetShared.suite)
        let translations = shared?.dictionary(forKey: WidgetShared.translationsKey) as? [String: String]
        return VerseEntry(
            date: date,
            ref: verse.ref,
            surahName: verse.surahName,
            arabic: verse.arabic,
            translation: translations?[verse.ref],
            translationIsRTL: shared?.bool(forKey: WidgetShared.rtlKey) ?? false
        )
    }

    private static let verses: [WidgetVerse] = {
        guard let url = Bundle.main.url(forResource: "widget-verses", withExtension: "json"),
              let data = try? Data(contentsOf: url),
              let list = try? JSONDecoder().decode([WidgetVerse].self, from: data)
        else { return [] }
        return list
    }()
}

enum WidgetPalette {
    static let accent = Color(light: 0x064E3B, dark: 0x7FD1AE)
    static let gold = Color(light: 0x8C6E32, dark: 0xC9A26D)
    static let track = Color(light: 0xE8E2D4, dark: 0x22302A)
    static let background = Color(light: 0xFAF7F0, dark: 0x0E1311)
    static let text = Color(light: 0x17251F, dark: 0xEDEAE2)
    static let secondary = Color(light: 0x5F6D66, dark: 0xA3ADA7)
}

extension Color {
    init(light: UInt32, dark: UInt32) {
        self.init(uiColor: UIColor { traits in
            let hex = traits.userInterfaceStyle == .dark ? dark : light
            return UIColor(
                red: CGFloat((hex >> 16) & 0xFF) / 255,
                green: CGFloat((hex >> 8) & 0xFF) / 255,
                blue: CGFloat(hex & 0xFF) / 255,
                alpha: 1
            )
        })
    }
}

struct VerseWidgetView: View {
    let entry: VerseEntry
    @Environment(\.widgetFamily) private var family

    var body: some View {
        switch family {
        case .accessoryInline:
            Text(entry.translation ?? "\(entry.surahName) \(entry.ref)")
        case .accessoryRectangular:
            VStack(alignment: .leading, spacing: 2) {
                Text("\(entry.surahName) \(entry.ref)")
                    .font(.caption2.weight(.semibold))
                Text(entry.translation ?? entry.arabic)
                    .font(.caption)
                    .lineLimit(3)
            }
        case .systemSmall:
            VStack(alignment: .leading, spacing: 6) {
                header
                Spacer(minLength: 0)
                arabic(size: 17, lines: 4)
            }
        default:
            VStack(alignment: .leading, spacing: 8) {
                header
                arabic(size: family == .systemLarge ? 24 : 19, lines: family == .systemLarge ? 6 : 2)
                if let translation = entry.translation {
                    Text(translation)
                        .font(.system(family == .systemLarge ? .body : .footnote, design: .serif))
                        .foregroundStyle(WidgetPalette.text)
                        .lineLimit(family == .systemLarge ? 8 : 3)
                        .multilineTextAlignment(entry.translationIsRTL ? .trailing : .leading)
                        .frame(maxWidth: .infinity, alignment: entry.translationIsRTL ? .trailing : .leading)
                }
                Spacer(minLength: 0)
            }
        }
    }

    private var header: some View {
        HStack {
            Text("VERSE OF THE DAY")
                .font(.system(size: 10, weight: .semibold))
                .tracking(0.6)
                .foregroundStyle(WidgetPalette.accent)
            Spacer(minLength: 4)
            Text("\(entry.surahName) \(entry.ref)")
                .font(.system(size: 10, weight: .medium))
                .foregroundStyle(WidgetPalette.secondary)
                .lineLimit(1)
        }
    }

    private func arabic(size: CGFloat, lines: Int) -> some View {
        Text(entry.arabic)
            .font(.custom("Amiri Quran", size: size))
            .foregroundStyle(WidgetPalette.text)
            .lineLimit(lines)
            .multilineTextAlignment(.leading)
            .frame(maxWidth: .infinity, alignment: .leading)
            .environment(\.layoutDirection, .rightToLeft)
    }
}

struct VerseOfTheDayWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "VerseOfTheDay", provider: VerseProvider()) { entry in
            VerseWidgetView(entry: entry)
                .containerBackground(for: .widget) { WidgetPalette.background }
                .widgetURL(URL(string: "quranforwomen://verse/\(entry.ref)"))
        }
        .configurationDisplayName("Verse of the Day")
        .description("A verse from the Quran each day.")
        .supportedFamilies([.systemSmall, .systemMedium, .systemLarge, .accessoryRectangular, .accessoryInline])
    }
}

@main
struct QuranForWomenWidgets: WidgetBundle {
    var body: some Widget {
        VerseOfTheDayWidget()
        DuaOfTheDayWidget()
        NextPrayerWidget()
        ReadingProgressWidget()
        DhikrTodayWidget()
    }
}
