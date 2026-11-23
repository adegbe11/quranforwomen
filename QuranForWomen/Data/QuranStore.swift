import Foundation
import Observation

/// The bundled, verified Quran text (Tanzil Uthmani). Read only. Works fully offline.
@Observable
@MainActor
final class QuranStore {
    let surahs: [Surah]
    let basmala: String
    /// First verse of each of the 30 juz.
    let juzStarts: [VerseRef]
    /// First verse of each of the 240 hizb quarters (4 per hizb, 60 hizbs).
    let hizbQuarterStarts: [VerseRef]
    let totalVerses: Int
    private let offsets: [Int]

    @ObservationIgnored private var normalizedCache: [[String]]?

    init(bundle: Bundle = .main) {
        struct Payload: Decodable {
            let basmala: String
            let juzStarts: [VerseRef]
            let hizbQuarterStarts: [VerseRef]?
            let surahs: [Surah]
        }
        guard
            let url = bundle.url(forResource: "quran-uthmani", withExtension: "json"),
            let data = try? Data(contentsOf: url),
            let payload = try? JSONDecoder().decode(Payload.self, from: data),
            payload.surahs.count == 114
        else {
            fatalError("The bundled Quran text is missing or damaged.")
        }
        surahs = payload.surahs
        basmala = payload.basmala
        juzStarts = payload.juzStarts
        hizbQuarterStarts = payload.hizbQuarterStarts ?? []
        var running = 0
        offsets = payload.surahs.map { surah in
            defer { running += surah.verseCount }
            return running
        }
        totalVerses = running
    }

    func surah(_ id: Int) -> Surah {
        surahs[min(max(id, 1), 114) - 1]
    }

    func contains(_ ref: VerseRef) -> Bool {
        (1...114).contains(ref.surah) && (1...surah(ref.surah).verseCount).contains(ref.ayah)
    }

    func text(_ ref: VerseRef) -> String {
        guard contains(ref) else { return "" }
        return surah(ref.surah).ayahs[ref.ayah - 1]
    }

    /// Position of a verse in the whole Quran, 1 to 6236. Used for audio files.
    func globalNumber(_ ref: VerseRef) -> Int {
        let surah = min(max(ref.surah, 1), 114)
        return offsets[surah - 1] + min(max(ref.ayah, 1), self.surah(surah).verseCount)
    }

    /// The verse at a position in the whole Quran, 1 to 6236.
    func ref(atGlobal number: Int) -> VerseRef {
        let n = min(max(number, 1), totalVerses)
        var surah = 1
        while surah < 114 && offsets[surah] < n { surah += 1 }
        return VerseRef(surah: surah, ayah: n - offsets[surah - 1])
    }

    /// Juz number (1 to 30) that contains a verse.
    func juz(of ref: VerseRef) -> Int {
        (juzStarts.lastIndex { $0 <= ref } ?? 0) + 1
    }

    func page(of ref: VerseRef) -> Int {
        surah(ref.surah).pages[ref.ayah - 1]
    }

    func displayName(_ ref: VerseRef) -> String {
        "\(surah(ref.surah).name) · \(ref.surah):\(ref.ayah)"
    }

    /// Diacritic-free text for search. Built once, off the main thread.
    func normalizedVerses() async -> [[String]] {
        if let normalizedCache { return normalizedCache }
        let ayahs = surahs.map(\.ayahs)
        let result = await Task.detached(priority: .userInitiated) {
            ayahs.map { $0.map(ArabicSearch.normalize) }
        }.value
        normalizedCache = result
        return result
    }
}

/// Duas, dhikr phrases and the daily verse list. All bundled and sourced.
@Observable
@MainActor
final class ContentStore {
    let categories: [DuaCategory]
    let duas: [Dua]
    let dhikr: [DhikrPhrase]
    let dailyVerses: [VerseRef]
    let feelings: [VerseCollection]
    let topics: [VerseCollection]

    init(bundle: Bundle = .main) {
        struct Payload: Decodable {
            let categories: [DuaCategory]
            let duas: [Dua]
            let dhikr: [DhikrPhrase]
            let dailyVerses: [String]
            let feelings: [VerseCollection]
            let topics: [VerseCollection]
        }
        guard
            let url = bundle.url(forResource: "content", withExtension: "json"),
            let data = try? Data(contentsOf: url),
            let payload = try? JSONDecoder().decode(Payload.self, from: data)
        else {
            fatalError("The bundled dua library is missing or damaged.")
        }
        categories = payload.categories
        duas = payload.duas
        dhikr = payload.dhikr
        dailyVerses = payload.dailyVerses.compactMap(VerseRef.init)
        feelings = payload.feelings
        topics = payload.topics
    }

    func collection(id: String) -> VerseCollection? {
        (feelings + topics).first { $0.id == id }
    }

    /// Index used by both the app and the widget to pick today's verse.
    static func dayIndex(_ date: Date = .now) -> Int { dayNumber(date) }

    func duas(in category: DuaCategory) -> [Dua] {
        duas.filter { $0.category == category.id }
    }

    func dua(id: String) -> Dua? { duas.first { $0.id == id } }
    func category(id: String) -> DuaCategory? { categories.first { $0.id == id } }
    func dhikr(id: String) -> DhikrPhrase? { dhikr.first { $0.id == id } }

    func verseOfTheDay(_ date: Date = .now) -> VerseRef {
        dailyVerses[Self.dayNumber(date) % dailyVerses.count]
    }

    func duaOfTheDay(_ date: Date = .now) -> Dua {
        duas[(Self.dayNumber(date) * 7) % duas.count]
    }

    private static func dayNumber(_ date: Date) -> Int {
        Calendar.current.ordinality(of: .day, in: .era, for: date) ?? 0
    }
}

enum ArabicSearch {
    /// Removes diacritics and Quranic marks and folds letter variants, so a plain
    /// search like "الرحمن" finds "ٱلرَّحْمَٰنِ".
    static func normalize(_ text: String) -> String {
        var out = String.UnicodeScalarView()
        for scalar in text.unicodeScalars {
            let v = scalar.value
            if (0x0610...0x061A).contains(v) || (0x064B...0x065F).contains(v) || v == 0x0670
                || (0x06D6...0x06ED).contains(v) || v == 0x0640 {
                continue
            }
            switch v {
            case 0x0671, 0x0623, 0x0625, 0x0622: out.append("\u{0627}" as Unicode.Scalar)
            case 0x0649: out.append("\u{064A}" as Unicode.Scalar)
            case 0x0629: out.append("\u{0647}" as Unicode.Scalar)
            default: out.append(scalar)
            }
        }
        return String(out)
    }

    static func containsArabic(_ text: String) -> Bool {
        text.unicodeScalars.contains { (0x0600...0x06FF).contains($0.value) }
    }
}
