import XCTest
@testable import QuranForWomen

final class PrayerTimesTests: XCTestCase {
    private let order: [Prayer] = [.fajr, .sunrise, .dhuhr, .asr, .maghrib, .isha]

    private let london = (latitude: 51.5074, longitude: -0.1278)
    private let makkah = (latitude: 21.4225, longitude: 39.8262)

    private func schedule(
        _ place: (latitude: Double, longitude: Double),
        timeZone: String,
        year: Int, month: Int, day: Int,
        method: CalculationMethod,
        asr: AsrMethod = .standard
    ) -> (PrayerSchedule, Calendar) {
        let calendar = Fixtures.calendar(timeZone)
        let date = Fixtures.noon(year, month, day, in: calendar)
        let schedule = PrayerCalculator.schedule(
            for: date,
            latitude: place.latitude,
            longitude: place.longitude,
            method: method,
            asr: asr,
            calendar: calendar
        )
        return (schedule, calendar)
    }

    /// Minutes after local midnight on the schedule's day.
    private func minutes(_ date: Date, from schedule: PrayerSchedule) -> Double {
        date.timeIntervalSince(schedule.date) / 60
    }

    private func assertInOrder(_ schedule: PrayerSchedule, _ label: String, file: StaticString = #filePath, line: UInt = #line) {
        let times = order.compactMap { schedule.time($0) }
        XCTAssertEqual(times.count, order.count, "\(label): a time is missing", file: file, line: line)
        for (earlier, later) in zip(times, times.dropFirst()) {
            XCTAssertLessThan(earlier, later, "\(label): times are out of order", file: file, line: line)
        }
    }

    func testMakkahTimesAreInOrder() {
        for method in [CalculationMethod.ummAlQura, .muslimWorldLeague] {
            let (s, _) = schedule(makkah, timeZone: "Asia/Riyadh", year: 2025, month: 6, day: 21, method: method)
            assertInOrder(s, "Makkah \(method.rawValue)")
        }
        let (winter, _) = schedule(makkah, timeZone: "Asia/Riyadh", year: 2025, month: 12, day: 21, method: .ummAlQura)
        assertInOrder(winter, "Makkah winter")
    }

    func testLondonTimesAreInOrder() {
        // June tests the high latitude rule, where the sun never reaches the Fajr and Isha angles.
        let (summer, _) = schedule(london, timeZone: "Europe/London", year: 2025, month: 6, day: 21, method: .muslimWorldLeague)
        assertInOrder(summer, "London summer")
        let (winter, _) = schedule(london, timeZone: "Europe/London", year: 2025, month: 12, day: 21, method: .muslimWorldLeague)
        assertInOrder(winter, "London winter")
    }

    func testScheduleDateIsStartOfLocalDay() {
        let (s, calendar) = schedule(london, timeZone: "Europe/London", year: 2025, month: 6, day: 21, method: .muslimWorldLeague)
        XCTAssertEqual(s.date, calendar.startOfDay(for: Fixtures.noon(2025, 6, 21, in: calendar)))
    }

    func testLondonSummerDhuhrIsAroundOnePM() {
        // Solar noon in London in June is a little after 13:00 summer time.
        let (s, _) = schedule(london, timeZone: "Europe/London", year: 2025, month: 6, day: 21, method: .muslimWorldLeague)
        let dhuhr = minutes(s.time(.dhuhr)!, from: s)
        XCTAssertGreaterThanOrEqual(dhuhr, 12 * 60 + 50)
        XCTAssertLessThanOrEqual(dhuhr, 13 * 60 + 15)
    }

    func testLondonWinterDhuhrIsAroundNoon() {
        let (s, _) = schedule(london, timeZone: "Europe/London", year: 2025, month: 12, day: 21, method: .muslimWorldLeague)
        let dhuhr = minutes(s.time(.dhuhr)!, from: s)
        XCTAssertGreaterThanOrEqual(dhuhr, 11 * 60 + 45)
        XCTAssertLessThanOrEqual(dhuhr, 12 * 60 + 15)
    }

    func testMakkahDhuhrIsAroundMidday() {
        let (s, _) = schedule(makkah, timeZone: "Asia/Riyadh", year: 2025, month: 6, day: 21, method: .ummAlQura)
        let dhuhr = minutes(s.time(.dhuhr)!, from: s)
        XCTAssertGreaterThanOrEqual(dhuhr, 12 * 60 + 5)
        XCTAssertLessThanOrEqual(dhuhr, 12 * 60 + 40)
    }

    func testAllTimesFallOnTheRightDay() {
        // In Makkah every time, Isha included, falls between local midnight and the next midnight.
        let (s, _) = schedule(makkah, timeZone: "Asia/Riyadh", year: 2025, month: 6, day: 21, method: .ummAlQura)
        for prayer in order {
            let m = minutes(s.time(prayer)!, from: s)
            XCTAssertGreaterThan(m, 0, prayer.name)
            XCTAssertLessThan(m, 24 * 60, prayer.name)
        }
    }

    func testUmmAlQuraIshaIsNinetyMinutesAfterMaghrib() {
        let (s, _) = schedule(makkah, timeZone: "Asia/Riyadh", year: 2025, month: 6, day: 21, method: .ummAlQura)
        let gap = s.time(.isha)!.timeIntervalSince(s.time(.maghrib)!) / 60
        XCTAssertEqual(gap, 90, accuracy: 0.01)
    }

    func testHanafiAsrIsLaterThanStandard() {
        let places: [((latitude: Double, longitude: Double), String)] = [(london, "Europe/London"), (makkah, "Asia/Riyadh")]
        for (place, zone) in places {
            for month in [3, 6, 12] {
                let (standard, _) = schedule(place, timeZone: zone, year: 2025, month: month, day: 15, method: .muslimWorldLeague, asr: .standard)
                let (hanafi, _) = schedule(place, timeZone: zone, year: 2025, month: month, day: 15, method: .muslimWorldLeague, asr: .hanafi)
                XCTAssertLessThan(standard.time(.asr)!, hanafi.time(.asr)!, "\(zone) month \(month)")
                // The other times do not depend on the Asr method.
                XCTAssertEqual(standard.time(.dhuhr), hanafi.time(.dhuhr))
                XCTAssertEqual(standard.time(.maghrib), hanafi.time(.maghrib))
                assertInOrder(hanafi, "Hanafi \(zone) month \(month)")
            }
        }
    }

    func testNextPrayerSkipsSunrise() {
        let (s, _) = schedule(makkah, timeZone: "Asia/Riyadh", year: 2025, month: 6, day: 21, method: .ummAlQura)
        // Just after Fajr, the next prayer is Dhuhr, not sunrise.
        let afterFajr = s.time(.fajr)!.addingTimeInterval(60)
        XCTAssertEqual(s.next(after: afterFajr)?.0, Prayer.dhuhr)
        XCTAssertNil(s.next(after: s.time(.isha)!.addingTimeInterval(60)))
    }

    func testQiblaFromLondon() {
        let bearing = PrayerCalculator.qiblaBearing(latitude: london.latitude, longitude: london.longitude)
        XCTAssertEqual(bearing, 119, accuracy: 2)
    }

    func testQiblaFromMadinahPointsSouth() {
        // Makkah is almost due south of Madinah.
        let bearing = PrayerCalculator.qiblaBearing(latitude: 24.4672, longitude: 39.6111)
        XCTAssertGreaterThan(bearing, 170)
        XCTAssertLessThan(bearing, 185)
    }

    func testQiblaNearMakkahIsValid() {
        let points = [(21.4225, 39.8262), (21.43, 39.83), (21.40, 39.80), (21.5, 39.9)]
        for (lat, lon) in points {
            let bearing = PrayerCalculator.qiblaBearing(latitude: lat, longitude: lon)
            XCTAssertTrue(bearing.isFinite, "\(lat), \(lon)")
            XCTAssertGreaterThanOrEqual(bearing, 0, "\(lat), \(lon)")
            XCTAssertLessThan(bearing, 360, "\(lat), \(lon)")
        }
    }
}
