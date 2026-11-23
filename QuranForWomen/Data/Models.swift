import Foundation

/// A reference to one verse, for example 2:255.
struct VerseRef: Codable, Hashable, Identifiable, Comparable {
    let surah: Int
    let ayah: Int

    var id: String { "\(surah):\(ayah)" }

    static func < (lhs: VerseRef, rhs: VerseRef) -> Bool {
        (lhs.surah, lhs.ayah) < (rhs.surah, rhs.ayah)
    }

    /// Parses "2:255".
    init?(_ string: String) {
        let parts = string.split(separator: ":").compactMap { Int($0) }
        guard parts.count == 2 else { return nil }
        self.init(surah: parts[0], ayah: parts[1])
    }

    init(surah: Int, ayah: Int) {
        self.surah = surah
        self.ayah = ayah
    }
}

struct Surah: Identifiable, Decodable {
    let id: Int
    let arabicName: String
    /// Transliterated name, for example "Al-Baqarah".
    let name: String
    /// English meaning, for example "The Cow".
    let meaning: String
    let revelation: String
    let sajdas: [Int]
    /// Madani mushaf page for each verse.
    let pages: [Int]
    /// Verified Uthmani text, one entry per verse. Never edited.
    let ayahs: [String]

    var verseCount: Int { ayahs.count }
    /// Al-Fatihah counts the Basmala as verse 1, and At-Tawbah has none.
    var showsBasmala: Bool { id != 1 && id != 9 }
    var revelationPlace: String { revelation == "Meccan" ? "Makkah" : "Madinah" }
}

extension Surah: Hashable {
    static func == (lhs: Surah, rhs: Surah) -> Bool { lhs.id == rhs.id }
    func hash(into hasher: inout Hasher) { hasher.combine(id) }
}

/// A curated set of whole verses, for a feeling or a topic.
struct VerseCollection: Codable, Identifiable, Hashable {
    let id: String
    let title: String
    let symbol: String
    let intro: String?
    let verses: [String]

    var refs: [VerseRef] { verses.compactMap(VerseRef.init) }
}

enum HighlightColor: String, Codable, CaseIterable, Identifiable {
    case emerald, blush, gold, sky

    var id: String { rawValue }
    var name: String { rawValue.capitalized }
}

struct VerseNote: Codable, Hashable {
    var text: String
    var updated = Date()
}

struct DuaCategory: Codable, Identifiable, Hashable {
    let id: String
    let title: String
    let symbol: String
}

struct Dua: Codable, Identifiable, Hashable {
    enum Kind: String, Codable { case quran, hadith }

    struct VerseRange: Codable, Hashable {
        let surah: Int
        let from: Int
        let to: Int
    }

    let id: String
    let category: String
    let title: String
    let kind: Kind
    /// "Quran 2:250" or a hadith collection and number.
    let source: String
    let arabic: String
    let transliteration: String?
    let translation: String
    let note: String?
    let verses: VerseRange?
    let wholeVerses: Bool?

    /// Recitation is only offered when the dua is made of complete verses.
    var playableRange: VerseRange? { wholeVerses == true ? verses : nil }
    var sourceKind: SourceLabel.Kind { kind == .quran ? .quran : .hadith }
    var kindTitle: String { kind == .quran ? "From the Quran" : "From the Sunnah" }
}

struct DhikrPhrase: Codable, Identifiable, Hashable {
    let id: String
    let arabic: String
    let transliteration: String
    let translation: String
    let target: Int
    let source: String
}

/// A prayer the user writes in her own words. Kept apart from sourced duas.
struct PersonalDua: Codable, Identifiable, Hashable {
    var id = UUID()
    var text: String
    var created = Date()
}
