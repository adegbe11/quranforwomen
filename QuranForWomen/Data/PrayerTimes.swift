import CoreLocation
import Foundation
import Observation
import UserNotifications

// MARK: - Prayer sound

/// The sound played with each prayer notification.
enum PrayerSound: String, CaseIterable, Identifiable {
    /// A full adhan recording, only offered when one is bundled as Sounds/adhan.wav.
    case adhan
    /// Original bell made for this app. Loud and clear.
    case bell
    /// The standard iPhone notification sound.
    case system

    var id: String { rawValue }

    var title: String {
        switch self {
        case .adhan: "Adhan"
        case .bell: "Prayer bell"
        case .system: "iPhone default"
        }
    }

    var fileName: String? {
        switch self {
        case .adhan: "adhan.wav"
        case .bell: "prayer-bell.wav"
        case .system: nil
        }
    }

    var isAvailable: Bool {
        guard let fileName else { return true }
        return Bundle.main.url(forResource: fileName, withExtension: nil) != nil
    }

    static var available: [PrayerSound] { allCases.filter(\.isAvailable) }

    /// The adhan if it is bundled, otherwise the bell.
    static var preferred: PrayerSound { PrayerSound.adhan.isAvailable ? .adhan : .bell }

    var notificationSound: UNNotificationSound {
        guard let fileName, isAvailable else { return .default }
        return UNNotificationSound(named: UNNotificationSoundName(fileName))
    }
}

// MARK: - Store

/// Location, settings and today's schedule. Location is kept only on this iPhone.
@Observable
@MainActor
final class PrayerStore: NSObject, CLLocationManagerDelegate {
    private enum Key {
        static let latitude = "prayer.latitude"
        static let longitude = "prayer.longitude"
        static let place = "prayer.place"
        static let method = "prayer.method"
        static let asr = "prayer.asr"
        static let notify = "prayer.notify"
        static let sound = "prayer.sound"
        static let enabled = "prayer.enabledPrayers"
    }

    private(set) var latitude: Double? = nil
    private(set) var longitude: Double? = nil
    private(set) var placeName: String? = nil
    private(set) var isLocating = false
    private(set) var locationDenied = false
    /// Compass heading in degrees, while the Qibla screen is open.
    private(set) var heading: Double?

    var calculationMethod: CalculationMethod {
        didSet { UserDefaults.standard.set(calculationMethod.rawValue, forKey: Key.method); refreshNotifications() }
    }
    var asr: AsrMethod {
        didSet { UserDefaults.standard.set(asr.rawValue, forKey: Key.asr); refreshNotifications() }
    }
    var notifyAtPrayerTimes: Bool {
        didSet { UserDefaults.standard.set(notifyAtPrayerTimes, forKey: Key.notify); refreshNotifications() }
    }
    var sound: PrayerSound {
        didSet { UserDefaults.standard.set(sound.rawValue, forKey: Key.sound); refreshNotifications() }
    }
    /// Which of the five prayers send a notification. All five by default.
    var enabledPrayers: Set<Prayer> {
        didSet {
            UserDefaults.standard.set(enabledPrayers.map(\.rawValue), forKey: Key.enabled)
            refreshNotifications()
        }
    }
    /// Set by the app so notifications pause on rest days.
    @ObservationIgnored var isResting = false {
        didSet { if oldValue != isResting { refreshNotifications() } }
    }

    @ObservationIgnored private let manager = CLLocationManager()
    @ObservationIgnored private var refreshTask: Task<Void, Never>?

    override init() {
        let d = UserDefaults.standard
        calculationMethod = CalculationMethod(rawValue: d.string(forKey: Key.method) ?? "") ?? .muslimWorldLeague
        asr = AsrMethod(rawValue: d.string(forKey: Key.asr) ?? "") ?? .standard
        notifyAtPrayerTimes = d.bool(forKey: Key.notify)
        let savedSound = PrayerSound(rawValue: d.string(forKey: Key.sound) ?? "")
        sound = (savedSound?.isAvailable ?? false) ? savedSound! : PrayerSound.preferred
        if let saved = d.stringArray(forKey: Key.enabled) {
            enabledPrayers = Set(saved.compactMap(Prayer.init(rawValue:)))
        } else {
            enabledPrayers = Set(Prayer.prayers)
        }
        super.init()
        if d.object(forKey: Key.latitude) != nil {
            latitude = d.double(forKey: Key.latitude)
            longitude = d.double(forKey: Key.longitude)
        }
        placeName = d.string(forKey: Key.place)
        manager.delegate = self
        manager.desiredAccuracy = kCLLocationAccuracyKilometer
    }

    var hasLocation: Bool { latitude != nil && longitude != nil }

    func schedule(for date: Date = .now) -> PrayerSchedule? {
        guard let latitude, let longitude else { return nil }
        return PrayerCalculator.schedule(for: date, latitude: latitude, longitude: longitude, method: calculationMethod, asr: asr)
    }

    /// The next prayer, looking into tomorrow after Isha.
    func nextPrayer(after now: Date = .now) -> (Prayer, Date)? {
        if let next = schedule(for: now)?.next(after: now) { return next }
        guard let tomorrow = Calendar.current.date(byAdding: .day, value: 1, to: now),
              let fajr = schedule(for: tomorrow)?.time(.fajr) else { return nil }
        return (.fajr, fajr)
    }

    var qiblaBearing: Double? {
        guard let latitude, let longitude else { return nil }
        return PrayerCalculator.qiblaBearing(latitude: latitude, longitude: longitude)
    }

    // MARK: Location

    func requestLocation() {
        locationDenied = false
        switch manager.authorizationStatus {
        case .notDetermined:
            isLocating = true
            manager.requestWhenInUseAuthorization()
        case .denied, .restricted:
            locationDenied = true
        default:
            isLocating = true
            manager.requestLocation()
        }
    }

    func startHeading() {
        guard CLLocationManager.headingAvailable() else { return }
        manager.startUpdatingHeading()
    }

    func stopHeading() {
        manager.stopUpdatingHeading()
        heading = nil
    }

    nonisolated func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        let status = manager.authorizationStatus
        Task { @MainActor in
            switch status {
            case .authorizedWhenInUse, .authorizedAlways:
                if self.isLocating { self.manager.requestLocation() }
            case .denied, .restricted:
                self.isLocating = false
                self.locationDenied = true
            default:
                break
            }
        }
    }

    nonisolated func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        guard let location = locations.last else { return }
        Task { @MainActor in self.save(location) }
    }

    nonisolated func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        Task { @MainActor in self.isLocating = false }
    }

    nonisolated func locationManager(_ manager: CLLocationManager, didUpdateHeading newHeading: CLHeading) {
        let value = newHeading.trueHeading >= 0 ? newHeading.trueHeading : newHeading.magneticHeading
        Task { @MainActor in self.heading = value }
    }

    private func save(_ location: CLLocation) {
        isLocating = false
        let isFirst = !hasLocation
        latitude = location.coordinate.latitude
        longitude = location.coordinate.longitude
        UserDefaults.standard.set(latitude, forKey: Key.latitude)
        UserDefaults.standard.set(longitude, forKey: Key.longitude)
        CLGeocoder().reverseGeocodeLocation(location) { [weak self] placemarks, _ in
            let mark = placemarks?.first
            let name = [mark?.locality, mark?.country].compactMap { $0 }.joined(separator: ", ")
            let country = mark?.isoCountryCode
            Task { @MainActor in
                guard let self else { return }
                self.placeName = name.isEmpty ? nil : name
                UserDefaults.standard.set(self.placeName, forKey: Key.place)
                if isFirst { self.calculationMethod = CalculationMethod.suggested(countryCode: country) }
            }
        }
        refreshNotifications()
    }

    // MARK: Notifications

    private static let notificationPrefix = "prayer-"

    nonisolated static let categoryID = "prayer"
    nonisolated static let prayedActionID = "prayed"

    /// Schedules a notification at each chosen prayer for the next 6 days
    /// (up to 30, well under the system limit of 64 pending notifications).
    /// It is refreshed every time the app opens.
    func refreshNotifications() {
        let center = UNUserNotificationCenter.current()
        let enabled = notifyAtPrayerTimes && !isResting
        let chosen = Prayer.prayers.filter { enabledPrayers.contains($0) }
        let alertSound = sound.notificationSound
        let schedules = enabled ? (0..<6).compactMap { offset in
            Calendar.current.date(byAdding: .day, value: offset, to: .now).flatMap { schedule(for: $0) }
        } : []
        let previous = refreshTask
        previous?.cancel()
        refreshTask = Task {
            await previous?.value
            let pending = await center.pendingNotificationRequests()
            guard !Task.isCancelled else { return }
            let old = pending.map(\.identifier).filter { $0.hasPrefix(Self.notificationPrefix) }
            center.removePendingNotificationRequests(withIdentifiers: old)
            guard enabled else { return }
            let now = Date.now
            for schedule in schedules {
                for prayer in chosen {
                    guard !Task.isCancelled else { return }
                    guard let time = schedule.time(prayer), time > now else { continue }
                    let content = UNMutableNotificationContent()
                    content.title = "Time for \(prayer.name)"
                    content.body = "Hayya 'ala as-salah. Come to prayer."
                    content.sound = alertSound
                    // Shows through Focus modes, like an alarm she chose.
                    content.interruptionLevel = .timeSensitive
                    content.categoryIdentifier = Self.categoryID
                    content.userInfo = ["prayer": prayer.rawValue, "time": time.timeIntervalSince1970]
                    let parts = Calendar.current.dateComponents([.year, .month, .day, .hour, .minute], from: time)
                    let id = "\(Self.notificationPrefix)\(LibraryStore.dayKey(time))-\(prayer.rawValue)"
                    try? await center.add(UNNotificationRequest(
                        identifier: id, content: content,
                        trigger: UNCalendarNotificationTrigger(dateMatching: parts, repeats: false)
                    ))
                }
            }
        }
    }

    func enableNotifications() async -> Bool {
        let center = UNUserNotificationCenter.current()
        let status = await center.notificationSettings().authorizationStatus
        var allowed = status == .authorized || status == .provisional
        if status == .notDetermined {
            allowed = (try? await center.requestAuthorization(options: [.alert, .sound, .badge])) ?? false
        }
        notifyAtPrayerTimes = allowed
        return allowed
    }
}
