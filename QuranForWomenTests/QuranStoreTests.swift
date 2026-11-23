import XCTest
@testable import QuranForWomen

@MainActor
final class QuranStoreTests: XCTestCase {
    private var quran: QuranStore { Fixtures.quran }

    func testLoads114Surahs() {
        XCTAssertEqual(quran.surahs.count, 114)
        XCTAssertEqual(quran.surahs.map(\.id), Array(1...114))
        XCTAssertEqual(quran.surah(1).verseCount, 7)
        XCTAssertEqual(quran.surah(2).verseCount, 286)
        XCTAssertEqual(quran.surah(114).verseCount, 6)
    }

    func testTotalVerses() {
        XCTAssertEqual(quran.totalVerses, 6236)
        XCTAssertEqual(quran.surahs.reduce(0) { $0 + $1.verseCount }, 6236)
    }

    func testEveryVerseHasAPage() {
        for surah in quran.surahs {
            XCTAssertEqual(surah.pages.count, surah.verseCount, "Surah \(surah.id)")
        }
    }

    func testGlobalNumberFirstMiddleLast() {
        XCTAssertEqual(quran.globalNumber(VerseRef(surah: 1, ayah: 1)), 1)
        // Al-Fatihah has 7 verses, so 2:255 is 7 + 255.
        XCTAssertEqual(quran.globalNumber(VerseRef(surah: 2, ayah: 255)), 262)
        XCTAssertEqual(quran.globalNumber(VerseRef(surah: 114, ayah: 6)), 6236)
    }

    func testRefAtGlobalFirstMiddleLast() {
        XCTAssertEqual(quran.ref(atGlobal: 1), VerseRef(surah: 1, ayah: 1))
        XCTAssertEqual(quran.ref(atGlobal: 262), VerseRef(surah: 2, ayah: 255))
        XCTAssertEqual(quran.ref(atGlobal: 6236), VerseRef(surah: 114, ayah: 6))
    }

    func testGlobalNumberRoundTripsForEveryVerse() {
        for n in 1...quran.totalVerses {
            let ref = quran.ref(atGlobal: n)
            XCTAssertTrue(quran.contains(ref), "Position \(n)")
            XCTAssertEqual(quran.globalNumber(ref), n, "Position \(n)")
        }
    }

    func testRefAtGlobalClampsOutOfRange() {
        XCTAssertEqual(quran.ref(atGlobal: 0), VerseRef(surah: 1, ayah: 1))
        XCTAssertEqual(quran.ref(atGlobal: 99_999), VerseRef(surah: 114, ayah: 6))
    }

    func testJuz() {
        XCTAssertEqual(quran.juzStarts.count, 30)
        XCTAssertEqual(quran.juz(of: VerseRef(surah: 1, ayah: 1)), 1)
        XCTAssertEqual(quran.juz(of: VerseRef(surah: 2, ayah: 141)), 1)
        XCTAssertEqual(quran.juz(of: VerseRef(surah: 2, ayah: 142)), 2)
        XCTAssertEqual(quran.juz(of: VerseRef(surah: 114, ayah: 6)), 30)
    }

    func testJuzNeverDecreases() {
        var last = 1
        for ref in Fixtures.allVerses {
            let juz = quran.juz(of: ref)
            XCTAssertTrue((1...30).contains(juz), "\(ref.id)")
            XCTAssertGreaterThanOrEqual(juz, last, "\(ref.id)")
            last = juz
        }
        XCTAssertEqual(last, 30)
    }

    func testContains() {
        XCTAssertTrue(quran.contains(VerseRef(surah: 1, ayah: 1)))
        XCTAssertTrue(quran.contains(VerseRef(surah: 1, ayah: 7)))
        XCTAssertTrue(quran.contains(VerseRef(surah: 2, ayah: 286)))
        XCTAssertTrue(quran.contains(VerseRef(surah: 114, ayah: 6)))

        XCTAssertFalse(quran.contains(VerseRef(surah: 1, ayah: 8)))
        XCTAssertFalse(quran.contains(VerseRef(surah: 1, ayah: 0)))
        XCTAssertFalse(quran.contains(VerseRef(surah: 0, ayah: 1)))
        XCTAssertFalse(quran.contains(VerseRef(surah: 115, ayah: 1)))
        XCTAssertFalse(quran.contains(VerseRef(surah: 114, ayah: 7)))
    }

    func testTextForValidRef() {
        XCTAssertFalse(quran.text(VerseRef(surah: 1, ayah: 1)).isEmpty)
        XCTAssertFalse(quran.text(VerseRef(surah: 2, ayah: 255)).isEmpty)
        XCTAssertFalse(quran.text(VerseRef(surah: 114, ayah: 6)).isEmpty)
    }

    func testTextIsEmptyForInvalidRef() {
        XCTAssertEqual(quran.text(VerseRef(surah: 1, ayah: 8)), "")
        XCTAssertEqual(quran.text(VerseRef(surah: 0, ayah: 1)), "")
        XCTAssertEqual(quran.text(VerseRef(surah: 115, ayah: 1)), "")
        XCTAssertEqual(quran.text(VerseRef(surah: 2, ayah: 0)), "")
    }

    func testVerseRefOrdering() {
        XCTAssertLessThan(VerseRef(surah: 1, ayah: 7), VerseRef(surah: 2, ayah: 1))
        XCTAssertLessThan(VerseRef(surah: 2, ayah: 9), VerseRef(surah: 2, ayah: 10))
        XCTAssertFalse(VerseRef(surah: 3, ayah: 1) < VerseRef(surah: 3, ayah: 1))
    }
}
