import SwiftUI
import WidgetKit

private struct WidgetDua: Decodable {
    let id: String
    let title: String
    let source: String
    let arabic: String
    let translation: String
}

struct DuaEntry: TimelineEntry {
    let date: Date
    let id: String
    let title: String
    let arabic: String
    let translation: String
    let source: String
}

struct DuaProvider: TimelineProvider {
    func placeholder(in context: Context) -> DuaEntry { entry(for: .now) }

    func getSnapshot(in context: Context, completion: @escaping (DuaEntry) -> Void) {
        completion(entry(for: .now))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<DuaEntry>) -> Void) {
        let now = Date.now
        completion(Timeline(entries: [entry(for: now)], policy: .after(WidgetShared.nextMidnight(after: now))))
    }

    /// Same daily pick as the app's Today screen (ContentStore.duaOfTheDay).
    private func entry(for date: Date) -> DuaEntry {
        let duas = Self.duas
        guard !duas.isEmpty else {
            return DuaEntry(date: date, id: "", title: "", arabic: "", translation: "", source: "")
        }
        let dua = duas[(WidgetShared.dayNumber(date) * 7) % duas.count]
        return DuaEntry(
            date: date, id: dua.id, title: dua.title,
            arabic: dua.arabic, translation: dua.translation, source: dua.source
        )
    }

    private static let duas: [WidgetDua] = {
        struct Payload: Decodable { let duas: [WidgetDua] }
        guard let url = Bundle.main.url(forResource: "content", withExtension: "json"),
              let data = try? Data(contentsOf: url),
              let payload = try? JSONDecoder().decode(Payload.self, from: data)
        else { return [] }
        return payload.duas
    }()
}

struct DuaWidgetView: View {
    let entry: DuaEntry
    @Environment(\.widgetFamily) private var family

    var body: some View {
        switch family {
        case .systemSmall:
            VStack(alignment: .leading, spacing: 6) {
                WidgetEyebrow(text: "DUA OF THE DAY")
                Spacer(minLength: 0)
                title(lines: 2)
                arabic(size: 16, lines: 2)
            }
        case .systemLarge:
            VStack(alignment: .leading, spacing: 10) {
                header
                title(lines: 2)
                arabic(size: 22, lines: 6)
                translation(lines: 8, large: true)
                Spacer(minLength: 0)
            }
        default:
            VStack(alignment: .leading, spacing: 6) {
                header
                title(lines: 1)
                arabic(size: 17, lines: 2)
                translation(lines: 2, large: false)
                Spacer(minLength: 0)
            }
        }
    }

    private var header: some View {
        HStack {
            WidgetEyebrow(text: "DUA OF THE DAY")
            Spacer(minLength: 4)
            Text(entry.source)
                .font(.system(size: 10, weight: .medium))
                .foregroundStyle(WidgetPalette.gold)
                .lineLimit(1)
        }
    }

    private func title(lines: Int) -> some View {
        Text(entry.title)
            .font(.system(.subheadline, design: .serif).weight(.semibold))
            .foregroundStyle(WidgetPalette.text)
            .lineLimit(lines)
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

    private func translation(lines: Int, large: Bool) -> some View {
        Text(entry.translation)
            .font(.system(large ? .body : .footnote, design: .serif))
            .foregroundStyle(WidgetPalette.secondary)
            .lineLimit(lines)
    }
}

struct DuaOfTheDayWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "DuaOfTheDay", provider: DuaProvider()) { entry in
            DuaWidgetView(entry: entry)
                .containerBackground(for: .widget) { WidgetPalette.background }
                .widgetURL(URL(string: entry.id.isEmpty ? "quranforwomen://today" : "quranforwomen://dua/\(entry.id)"))
        }
        .configurationDisplayName("Dua of the Day")
        .description("A dua from the Quran or Sunnah each day.")
        .supportedFamilies([.systemSmall, .systemMedium, .systemLarge])
    }
}
