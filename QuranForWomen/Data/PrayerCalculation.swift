import Foundation

// MARK: - Calculation
// Foundation only: this file is also built into the widget extension (see project.yml).

/// Standard astronomical prayer time calculation (the widely used PrayTimes method).
/// Runs fully on device. Location is never sent to us; iOS looks up the city name once.
enum Prayer: String, CaseIterable, Identifiable, Codable {
    case fajr, sunrise, dhuhr, asr, maghrib, isha

    var id: String { rawValue }

    var name: String {
        switch self {
        case .fajr: "Fajr"
        case .sunrise: "Sunrise"
        case .dhuhr: "Dhuhr"
        case .asr: "Asr"
        case .maghrib: "Maghrib"
        case .isha: "Isha"
        }
    }

    /// Sunrise is shown for reference but is not a prayer.
    var isPrayer: Bool { self != .sunrise }

    static var prayers: [Prayer] { allCases.filter(\.isPrayer) }
}

enum CalculationMethod: String, CaseIterable, Identifiable, Codable {
    case muslimWorldLeague, isna, egypt, ummAlQura, karachi, dubai, singapore, turkey

    var id: String { rawValue }

    var title: String {
        switch self {
        case .muslimWorldLeague: "Muslim World League"
        case .isna: "North America (ISNA)"
        case .egypt: "Egyptian General Authority"
        case .ummAlQura: "Umm al-Qura, Makkah"
        case .karachi: "University of Islamic Sciences, Karachi"
        case .dubai: "Dubai"
        case .singapore: "Singapore, Malaysia, Indonesia"
        case .turkey: "Diyanet, Turkey"
        }
    }

    var fajrAngle: Double {
        switch self {
        case .muslimWorldLeague: 18
        case .isna: 15
        case .egypt: 19.5
        case .ummAlQura: 18.5
        case .karachi: 18
        case .dubai: 18.2
        case .singapore: 20
        case .turkey: 18
        }
    }

    /// Isha as a sun angle, or as minutes after Maghrib.
    var isha: (angle: Double?, minutes: Double?) {
        switch self {
        case .muslimWorldLeague: (17, nil)
        case .isna: (15, nil)
        case .egypt: (17.5, nil)
        case .ummAlQura: (nil, 90)
        case .karachi: (18, nil)
        case .dubai: (18.2, nil)
        case .singapore: (18, nil)
        case .turkey: (17, nil)
        }
    }

    /// A sensible default for the region of a country code.
    static func suggested(countryCode: String?) -> CalculationMethod {
        switch countryCode?.uppercased() {
        case "US", "CA": .isna
        case "SA", "YE", "OM", "BH", "KW", "QA": .ummAlQura
        case "AE": .dubai
        case "EG", "SD", "LY", "DZ", "MA", "TN", "SY", "LB", "JO", "IQ", "PS": .egypt
        case "PK", "IN", "BD", "AF": .karachi
        case "MY", "SG", "ID", "BN": .singapore
        case "TR": .turkey
        default: .muslimWorldLeague
        }
    }
}

enum AsrMethod: String, CaseIterable, Identifiable, Codable {
    case standard, hanafi

    var id: String { rawValue }
    var title: String { self == .standard ? "Standard (Shafi'i, Maliki, Hanbali)" : "Hanafi" }
    var shadowFactor: Double { self == .standard ? 1 : 2 }
}

struct PrayerSchedule {
    let date: Date
    let times: [Prayer: Date]

    func time(_ prayer: Prayer) -> Date? { times[prayer] }

    /// The next prayer after a moment, if any remain today.
    func next(after now: Date = .now) -> (Prayer, Date)? {
        Prayer.prayers.compactMap { p in times[p].map { (p, $0) } }
            .first { $0.1 > now }
    }
}

enum PrayerCalculator {
    static func schedule(
        for date: Date,
        latitude: Double,
        longitude: Double,
        method: CalculationMethod,
        asr: AsrMethod,
        calendar: Calendar = .current
    ) -> PrayerSchedule {
        let day = calendar.startOfDay(for: date)
        let c = calendar.dateComponents([.year, .month, .day], from: day)
        let tzHours = Double(calendar.timeZone.secondsFromGMT(for: day.addingTimeInterval(12 * 3600))) / 3600
        let jDate = julian(year: c.year ?? 2000, month: c.month ?? 1, day: c.day ?? 1) - longitude / (15 * 24)

        func sunPosition(_ jd: Double) -> (declination: Double, equation: Double) {
            let d = jd - 2451545.0
            let g = fixAngle(357.529 + 0.98560028 * d)
            let q = fixAngle(280.459 + 0.98564736 * d)
            let l = fixAngle(q + 1.915 * dsin(g) + 0.020 * dsin(2 * g))
            let e = 23.439 - 0.00000036 * d
            let ra = darctan2(dcos(e) * dsin(l), dcos(l)) / 15
            let eqt = q / 15 - fixHour(ra)
            let decl = darcsin(dsin(e) * dsin(l))
            return (decl, eqt)
        }

        func midDay(_ t: Double) -> Double {
            fixHour(12 - sunPosition(jDate + t).equation)
        }

        func sunAngleTime(_ angle: Double, _ t: Double, counterClockwise: Bool = false) -> Double {
            let decl = sunPosition(jDate + t).declination
            let noon = midDay(t)
            let ratio = (-dsin(angle) - dsin(decl) * dsin(latitude)) / (dcos(decl) * dcos(latitude))
            let span = darccos(min(max(ratio, -1), 1)) / 15
            return noon + (counterClockwise ? -span : span)
        }

        func asrTime(_ factor: Double, _ t: Double) -> Double {
            let decl = sunPosition(jDate + t).declination
            let angle = -darccot(factor + dtan(abs(latitude - decl)))
            return sunAngleTime(angle, t)
        }

        // Two passes: the first gives approximate times, the second refines them.
        var fajr = 5.0, sunrise = 6.0, dhuhr = 12.0, asrT = 13.0, sunset = 18.0, isha = 18.0
        for _ in 0..<2 {
            let f = sunAngleTime(method.fajrAngle, fajr / 24, counterClockwise: true)
            let r = sunAngleTime(0.833, sunrise / 24, counterClockwise: true)
            let d = midDay(dhuhr / 24)
            let a = asrTime(asr.shadowFactor, asrT / 24)
            let s = sunAngleTime(0.833, sunset / 24)
            let i = method.isha.angle.map { sunAngleTime($0, isha / 24) } ?? s
            fajr = f; sunrise = r; dhuhr = d; asrT = a; sunset = s; isha = i
        }
        if let minutes = method.isha.minutes { isha = sunset + minutes / 60 }

        // High latitudes: keep Fajr and Isha within a share of the night (angle based rule).
        let night = fixHour(sunrise - sunset)
        let fajrLimit = method.fajrAngle / 60 * night
        if fajr.isNaN || fixHour(sunrise - fajr) > fajrLimit { fajr = sunrise - fajrLimit }
        if let angle = method.isha.angle {
            let ishaLimit = angle / 60 * night
            if isha.isNaN || fixHour(isha - sunset) > ishaLimit { isha = sunset + ishaLimit }
        }

        let shift = tzHours - longitude / 15
        func at(_ hours: Double) -> Date? {
            guard hours.isFinite else { return nil }
            return day.addingTimeInterval((hours + shift) * 3600)
        }
        var times: [Prayer: Date] = [:]
        times[.fajr] = at(fajr)
        times[.sunrise] = at(sunrise)
        // A minute after the sun passes its height, as most timetables do.
        times[.dhuhr] = at(dhuhr + 1.0 / 60)
        times[.asr] = at(asrT)
        times[.maghrib] = at(sunset)
        times[.isha] = at(isha)
        return PrayerSchedule(date: day, times: times)
    }

    /// Direction of the Kaaba from a location, in degrees clockwise from true north.
    static func qiblaBearing(latitude: Double, longitude: Double) -> Double {
        let kaabaLat = 21.4225, kaabaLon = 39.8262
        let dLon = kaabaLon - longitude
        let y = dsin(dLon)
        let x = dcos(latitude) * dtan(kaabaLat) - dsin(latitude) * dcos(dLon)
        return fixAngle(darctan2(y, x))
    }

    // MARK: Math helpers (degrees)

    private static func julian(year: Int, month: Int, day: Int) -> Double {
        var y = Double(year), m = Double(month)
        if m <= 2 { y -= 1; m += 12 }
        let a = floor(y / 100)
        let b = 2 - a + floor(a / 4)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + Double(day) + b - 1524.5
    }

    private static func dsin(_ d: Double) -> Double { sin(d * .pi / 180) }
    private static func dcos(_ d: Double) -> Double { cos(d * .pi / 180) }
    private static func dtan(_ d: Double) -> Double { tan(d * .pi / 180) }
    private static func darcsin(_ x: Double) -> Double { asin(x) * 180 / .pi }
    private static func darccos(_ x: Double) -> Double { acos(x) * 180 / .pi }
    private static func darctan2(_ y: Double, _ x: Double) -> Double { atan2(y, x) * 180 / .pi }
    private static func darccot(_ x: Double) -> Double { atan(1 / x) * 180 / .pi }
    private static func fixAngle(_ a: Double) -> Double { let r = a.truncatingRemainder(dividingBy: 360); return r < 0 ? r + 360 : r }
    private static func fixHour(_ h: Double) -> Double { let r = h.truncatingRemainder(dividingBy: 24); return r < 0 ? r + 24 : r }
}
