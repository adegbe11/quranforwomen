import XCTest
@testable import QuranForWomen

final class ReadingPlanTests: XCTestCase {
    // dayIndex uses Calendar.current, so the dates here are built with it too.
    private let start = Calendar.current.date(from: DateComponents(year: 2025, month: 3, day: 1, hour: 12))!

    private func date(daysAfterStart days: Int) -> Date {
        Calendar.current.date(byAdding: .day, value: days, to: start)!
    }

    func testDayRangesCoverEveryPageOnce() {
        for days in [30, 60, 120, 365] {
            let plan = ReadingPlan(days: days, start: start)
            var expectedNext = 1
            for day in 0..<days {
                let range = plan.pages(forDay: day)
                XCTAssertEqual(range.lowerBound, expectedNext, "\(days) day plan, day \(day) leaves a gap or overlaps")
                XCTAssertLessThanOrEqual(range.upperBound, ReadingPlan.totalPages, "\(days) day plan, day \(day)")
                expectedNext = range.upperBound + 1
            }
            XCTAssertEqual(expectedNext, ReadingPlan.totalPages + 1, "\(days) day plan does not end on page 604")
        }
    }

    func testDayRangesAddUpTo604() {
        for days in [30, 60, 120, 365] {
            let plan = ReadingPlan(days: days, start: start)
            let total = (0..<days).reduce(0) { $0 + plan.pages(forDay: $1).count }
            XCTAssertEqual(total, 604, "\(days) day plan")
        }
    }

    func testThirtyDayPlanStartsAndEndsCorrectly() {
        let plan = ReadingPlan(days: 30, start: start)
        XCTAssertEqual(plan.pages(forDay: 0).lowerBound, 1)
        XCTAssertEqual(plan.pages(forDay: 29).upperBound, 604)
    }

    func testDayIndexOnStartDay() {
        let plan = ReadingPlan(days: 30, start: start)
        XCTAssertEqual(plan.dayIndex(on: start), 0)
        XCTAssertEqual(plan.dayIndex(on: date(daysAfterStart: 10)), 10)
    }

    func testDayIndexClampsBeforeStart() {
        let plan = ReadingPlan(days: 30, start: start)
        XCTAssertEqual(plan.dayIndex(on: date(daysAfterStart: -1)), 0)
        XCTAssertEqual(plan.dayIndex(on: date(daysAfterStart: -100)), 0)
    }

    func testDayIndexClampsAfterEnd() {
        let plan = ReadingPlan(days: 30, start: start)
        XCTAssertEqual(plan.dayIndex(on: date(daysAfterStart: 29)), 29)
        XCTAssertEqual(plan.dayIndex(on: date(daysAfterStart: 30)), 29)
        XCTAssertEqual(plan.dayIndex(on: date(daysAfterStart: 400)), 29)
    }

    func testIsComplete() {
        var plan = ReadingPlan(days: 30, start: start)
        XCTAssertFalse(plan.isComplete)
        plan.finishedDays = Set(0..<29)
        XCTAssertFalse(plan.isComplete)
        plan.finishedDays.insert(29)
        XCTAssertTrue(plan.isComplete)
    }
}
