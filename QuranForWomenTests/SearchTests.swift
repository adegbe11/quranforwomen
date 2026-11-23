import XCTest
@testable import QuranForWomen

@MainActor
final class SearchTests: XCTestCase {
    private var quran: QuranStore { Fixtures.quran }

    /// "الرحمن" written plainly, with no diacritics.
    private let arRahman = "\u{0627}\u{0644}\u{0631}\u{062D}\u{0645}\u{0646}"

    // MARK: VerseRef parsing

    func testVerseRefParsesSurahAndAyah() {
        XCTAssertEqual(VerseRef("2:255"), VerseRef(surah: 2, ayah: 255))
        XCTAssertEqual(VerseRef("1:1"), VerseRef(surah: 1, ayah: 1))
        XCTAssertEqual(VerseRef("114:6")?.id, "114:6")
    }

    func testVerseRefRejectsBadInput() {
        XCTAssertNil(VerseRef("2"))
        XCTAssertNil(VerseRef("2:255:1"))
        XCTAssertNil(VerseRef("a:b"))
        XCTAssertNil(VerseRef(""))
    }

    func testVerseRefIdRoundTrips() {
        for ref in [VerseRef(surah: 1, ayah: 1), VerseRef(surah: 2, ayah: 255), VerseRef(surah: 114, ayah: 6)] {
            XCTAssertEqual(VerseRef(ref.id), ref)
        }
    }

    // MARK: Deep links

    func testRouterOpensVerseFromWidgetLink() {
        let router = AppRouter()
        router.tab = .quran
        router.handle(URL(string: "quranforwomen://verse/2:255")!)
        XCTAssertEqual(router.tab, .today)
        XCTAssertEqual(router.paths[.today] ?? [], [Route.reader(surah: 2, verse: 255)])
    }

    func testRouterIgnoresOtherLinks() {
        let router = AppRouter()
        router.tab = .quran
        router.handle(URL(string: "https://example.com/verse/2:255")!)
        router.handle(URL(string: "quranforwomen://verse/115:1")!)
        router.handle(URL(string: "quranforwomen://dua/2:255")!)
        XCTAssertEqual(router.tab, .quran)
        XCTAssertEqual(router.paths[.today] ?? [], [])
    }

    // MARK: QuranSearch

    func testVerseNumberGivesJump() async {
        let results = await QuranSearch.run(query: "2:255", quran: quran, translation: nil)
        XCTAssertEqual(results.jump, VerseRef(surah: 2, ayah: 255))
        XCTAssertTrue(results.verses.isEmpty)
    }

    func testVerseNumberWithSpaceGivesJump() async {
        let results = await QuranSearch.run(query: "2 255", quran: quran, translation: nil)
        XCTAssertEqual(results.jump, VerseRef(surah: 2, ayah: 255))
    }

    func testInvalidVerseNumberGivesNoJump() async {
        let results = await QuranSearch.run(query: "1:8", quran: quran, translation: nil)
        XCTAssertNil(results.jump)
    }

    func testSurahNumberFindsSurah() async {
        let results = await QuranSearch.run(query: "18", quran: quran, translation: nil)
        XCTAssertEqual(results.surahs, [18])
        XCTAssertNil(results.jump)
    }

    func testEnglishSurahNameFindsSurah() async {
        let results = await QuranSearch.run(query: "al baqarah", quran: quran, translation: nil)
        XCTAssertTrue(results.surahs.contains(2))
    }

    func testArabicQueryMatchesVerses() async {
        let results = await QuranSearch.run(query: arRahman, quran: quran, translation: nil)
        XCTAssertNil(results.jump)
        XCTAssertFalse(results.verses.isEmpty)
        // The Basmala of Al-Fatihah is the first verse that contains it.
        XCTAssertEqual(results.verses.first, VerseRef(surah: 1, ayah: 1))
        XCTAssertLessThanOrEqual(results.verses.count, QuranSearch.limit)
        // Surah 55 is named Ar-Rahman.
        XCTAssertTrue(results.surahs.contains(55))
    }

    func testArabicQueryWithDiacriticsMatchesTheSame() async {
        // The query as it is written in the mushaf finds the same verses as the plain spelling.
        let marked = quran.text(VerseRef(surah: 1, ayah: 1)).split(separator: " ").map(String.init)[2]
        let plain = await QuranSearch.run(query: arRahman, quran: quran, translation: nil)
        let withMarks = await QuranSearch.run(query: marked, quran: quran, translation: nil)
        XCTAssertEqual(withMarks.verses, plain.verses)
    }

    func testTranslationSearchUsesTheGivenText() async {
        // A tiny fake translation: only 2:255 says "throne".
        var translation = quran.surahs.map { Array(repeating: "", count: $0.verseCount) }
        translation[1][254] = "His Throne extends over the heavens and the earth."
        let results = await QuranSearch.run(query: "throne", quran: quran, translation: translation)
        XCTAssertEqual(results.verses, [VerseRef(surah: 2, ayah: 255)])
    }

    func testEnglishQueryWithoutTranslationFindsNoVerses() async {
        let results = await QuranSearch.run(query: "mercy", quran: quran, translation: nil)
        XCTAssertTrue(results.verses.isEmpty)
    }

    // MARK: ArabicSearch

    func testContainsArabic() {
        XCTAssertTrue(ArabicSearch.containsArabic(arRahman))
        XCTAssertTrue(ArabicSearch.containsArabic("Surah \(arRahman)"))
        XCTAssertFalse(ArabicSearch.containsArabic("2:255"))
        XCTAssertFalse(ArabicSearch.containsArabic("Ar-Rahman"))
        XCTAssertFalse(ArabicSearch.containsArabic(""))
    }

    func testNormalizeStripsMarksFromBasmalaWord() {
        // The third word of 1:1 is Ar-Rahman with its full marks.
        let word = quran.text(VerseRef(surah: 1, ayah: 1)).split(separator: " ").map(String.init)[2]
        XCTAssertNotEqual(word, arRahman)
        XCTAssertEqual(ArabicSearch.normalize(word), arRahman)
    }

    func testNormalizeLeavesPlainTextAlone() {
        XCTAssertEqual(ArabicSearch.normalize(arRahman), arRahman)
        XCTAssertEqual(ArabicSearch.normalize("2:255 Ar-Rahman"), "2:255 Ar-Rahman")
    }

    func testNormalizeFoldsLetterVariants() {
        // Alif wasla, alif with hamza above and below, and alif with madda all become a plain alif.
        XCTAssertEqual(ArabicSearch.normalize("\u{0671}\u{0623}\u{0625}\u{0622}"), "\u{0627}\u{0627}\u{0627}\u{0627}")
        // Alif maqsura becomes ya, ta marbuta becomes ha.
        XCTAssertEqual(ArabicSearch.normalize("\u{0649}\u{0629}"), "\u{064A}\u{0647}")
    }

    func testNormalizeIsStableForEveryVerse() {
        for ref in Fixtures.allVerses {
            let once = ArabicSearch.normalize(quran.text(ref))
            XCTAssertEqual(ArabicSearch.normalize(once), once, ref.id)
            XCTAssertFalse(once.isEmpty, ref.id)
            let leftover = once.unicodeScalars.contains { (0x064B...0x065F).contains($0.value) || $0.value == 0x0670 }
            XCTAssertFalse(leftover, "\(ref.id) still has diacritics")
        }
    }

    func testNormalizedVersesMatchNormalize() async {
        let normalized = await quran.normalizedVerses()
        XCTAssertEqual(normalized.count, 114)
        XCTAssertEqual(normalized.reduce(0) { $0 + $1.count }, 6236)
        XCTAssertEqual(normalized[1][254], ArabicSearch.normalize(quran.text(VerseRef(surah: 2, ayah: 255))))
    }
}
