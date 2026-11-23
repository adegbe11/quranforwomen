import XCTest
@testable import QuranForWomen

@MainActor
final class MushafPagesTests: XCTestCase {
    private var pages: [MushafPage] { Fixtures.quran.mushafPages() }

    func testExactly604PagesWithNoGaps() {
        let pages = pages
        XCTAssertEqual(pages.count, 604)
        XCTAssertEqual(pages.map(\.number), Array(1...604))
    }

    func testNoEmptyPage() {
        for page in pages {
            XCTAssertFalse(page.verses.isEmpty, "Page \(page.number)")
        }
    }

    func testEveryVerseAppearsOnceAndInOrder() {
        let flattened = pages.flatMap(\.verses)
        XCTAssertEqual(flattened.count, 6236)
        XCTAssertEqual(Set(flattened).count, 6236)
        XCTAssertEqual(flattened, Fixtures.allVerses)
    }

    func testFirstAndLastPages() {
        let pages = pages
        XCTAssertEqual(pages.first?.verses.first, VerseRef(surah: 1, ayah: 1))
        XCTAssertEqual(pages.first?.verses.last, VerseRef(surah: 1, ayah: 7))
        XCTAssertEqual(pages[1].number, 2)
        XCTAssertEqual(pages[1].verses.first, VerseRef(surah: 2, ayah: 1))
        XCTAssertEqual(pages.last?.verses.last, VerseRef(surah: 114, ayah: 6))
    }

    func testPageOfMatchesGrouping() {
        let quran = Fixtures.quran
        for page in pages {
            for ref in page.verses {
                XCTAssertEqual(quran.page(of: ref), page.number, "\(ref.id)")
            }
        }
    }
}
