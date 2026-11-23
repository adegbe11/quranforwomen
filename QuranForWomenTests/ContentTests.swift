import XCTest
@testable import QuranForWomen

@MainActor
final class ContentTests: XCTestCase {
    private var quran: QuranStore { Fixtures.quran }
    private var content: ContentStore { Fixtures.content }

    func testLibraryIsNotEmpty() {
        XCTAssertFalse(content.categories.isEmpty)
        XCTAssertFalse(content.duas.isEmpty)
        XCTAssertFalse(content.dhikr.isEmpty)
        XCTAssertFalse(content.dailyVerses.isEmpty)
        XCTAssertFalse(content.feelings.isEmpty)
        XCTAssertFalse(content.topics.isEmpty)
    }

    func testEveryDuaHasASource() {
        for dua in content.duas {
            XCTAssertFalse(dua.source.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty, dua.id)
        }
    }

    func testEveryDuaHasTextAndAKnownCategory() {
        let categoryIDs = Set(content.categories.map(\.id))
        for dua in content.duas {
            XCTAssertFalse(dua.arabic.isEmpty, dua.id)
            XCTAssertFalse(dua.translation.isEmpty, dua.id)
            XCTAssertTrue(categoryIDs.contains(dua.category), "\(dua.id) has unknown category \(dua.category)")
        }
    }

    func testIDsAreUnique() {
        XCTAssertEqual(Set(content.duas.map(\.id)).count, content.duas.count)
        XCTAssertEqual(Set(content.dhikr.map(\.id)).count, content.dhikr.count)
        XCTAssertEqual(Set(content.categories.map(\.id)).count, content.categories.count)
        let collections = content.feelings + content.topics
        XCTAssertEqual(Set(collections.map(\.id)).count, collections.count)
    }

    func testQuranicDuaVersesExist() {
        for dua in content.duas {
            guard let range = dua.verses else { continue }
            XCTAssertLessThanOrEqual(range.from, range.to, dua.id)
            XCTAssertTrue(quran.contains(VerseRef(surah: range.surah, ayah: range.from)), dua.id)
            XCTAssertTrue(quran.contains(VerseRef(surah: range.surah, ayah: range.to)), dua.id)
        }
    }

    func testEveryDailyVerseIsValid() {
        for ref in content.dailyVerses {
            XCTAssertTrue(quran.contains(ref), ref.id)
        }
    }

    func testEveryFeelingVerseIsValid() {
        for feeling in content.feelings {
            XCTAssertFalse(feeling.verses.isEmpty, feeling.id)
            // Every string must parse, not just the ones that happen to be well formed.
            XCTAssertEqual(feeling.refs.count, feeling.verses.count, feeling.id)
            for ref in feeling.refs {
                XCTAssertTrue(quran.contains(ref), "\(feeling.id) \(ref.id)")
            }
        }
    }

    func testEveryTopicVerseIsValid() {
        for topic in content.topics {
            XCTAssertFalse(topic.verses.isEmpty, topic.id)
            XCTAssertEqual(topic.refs.count, topic.verses.count, topic.id)
            for ref in topic.refs {
                XCTAssertTrue(quran.contains(ref), "\(topic.id) \(ref.id)")
            }
        }
    }

    func testDhikrTargetsArePositive() {
        for phrase in content.dhikr {
            XCTAssertGreaterThan(phrase.target, 0, phrase.id)
            XCTAssertFalse(phrase.source.isEmpty, phrase.id)
        }
    }

    func testDailyPicksAreValid() {
        // A fixed date, so the pick is the same on every run.
        let date = Fixtures.noon(2025, 6, 21, in: Fixtures.calendar("UTC"))
        XCTAssertTrue(quran.contains(content.verseOfTheDay(date)))
        XCTAssertNotNil(content.dua(id: content.duaOfTheDay(date).id))
    }

    func testLookups() {
        let dua = content.duas[0]
        XCTAssertEqual(content.dua(id: dua.id), dua)
        XCTAssertNil(content.dua(id: "no-such-dua"))
        let collection = content.feelings[0]
        XCTAssertEqual(content.collection(id: collection.id), collection)
        XCTAssertNil(content.collection(id: "no-such-collection"))
    }
}
