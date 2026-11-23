import Foundation
import Observation
import UserNotifications

/// The kinds of fast she can mark on the calendar.
enum FastKind: String, CaseIterable, Identifiable, Codable {
    case ramadan, voluntary, makeUp

    var id: String { rawValue }

    var title: String {
        switch self {
        case .ramadan: "Ramadan fast"
        case .voluntary: "Voluntary fast"
        case .makeUp: "Making up a fast"
        }
    }

    var symbol: String {
        switch self {
        case .ramadan: "moon.stars"
        case .voluntary: "sparkles"
        case .makeUp: "arrow.counterclockwise"
        }
    }
}

/// The two Ramadan reminders.
enum RamadanMoment: String {
    case suhoor, iftar

    var title: String { self == .suhoor ? "Suhoor" : "Iftar" }
}

/// Her fasts, the number of fasts she has to make up, and the suhoor and iftar reminders.
/// Kept only on this iPhone. Days she did not fast are simply left blank.
@Observable
@MainActor
final class FastingStore {
    private enum Key {
        static let fasts = "fasting.days"
        static let owed = "fasting.makeUpOwed"
        static let suhoor = "fasting.suhoorReminder"
        static let iftar = "fasting.iftarReminder"
        static let suhoorMinutes = "fasting.suhoorMinutes"
    }

    /// "yyyy-MM-dd" to the kind of fast marked that day.
    private(set) var fasts: [String: FastKind]
    /// Fasts to make up in total, including the ones she has since made up.
    private var makeUpOwed: Int

    private(set) var suhoorReminder: Bool
    private(set) var iftarReminder: Bool
    /// How long before Fajr the suhoor reminder comes.
    private(set) var suhoorMinutes: Int

    @ObservationIgnored private var refreshTask: Task<Void, Never>?

    init() {
        let d = UserDefaults.standard
        let saved = (d.dictionary(forKey: Key.fasts) as? [String: String]) ?? [:]
        fasts = saved.compactMapValues { FastKind(rawValue: $0) }
        makeUpOwed = d.integer(forKey: Key.owed)
        suhoorReminder = d.bool(forKey: Key.suhoor)
        iftarReminder = d.bool(forKey: Key.iftar)
        let minutes = d.integer(forKey: Key.suhoorMinutes)
        suhoorMinutes = minutes > 0 ? minutes : 30
    }

    // MARK: Fasts

    func kind(on date: Date) -> FastKind? { fasts[LibraryStore.dayKey(date)] }

    func setFast(_ kind: FastKind?, on date: Date) {
        let key = LibraryStore.dayKey(date)
        guard fasts[key] != kind else { return }
        fasts[key] = kind
        UserDefaults.standard.set(fasts.mapValues(\.rawValue), forKey: Key.fasts)
    }

    private var madeUp: Int { fasts.values.filter { $0 == .makeUp }.count }

    /// Erase My Data: marked fasts and the make-up count. Reminder choices are settings and stay.
    func eraseAll() {
        fasts = [:]
        makeUpOwed = 0
        UserDefaults.standard.removeObject(forKey: Key.fasts)
        UserDefaults.standard.removeObject(forKey: Key.owed)
    }

    /// Counts down as she marks make-up fasts.
    var makeUpLeft: Int { max(0, makeUpOwed - madeUp) }

    func setMakeUpLeft(_ value: Int) {
        makeUpOwed = min(max(value, 0), 9999) + madeUp
        UserDefaults.standard.set(makeUpOwed, forKey: Key.owed)
    }

    /// Ramadan fasts in a Hijri year.
    func ramadanCount(hijriYear: Int) -> Int {
        fasts.filter { key, kind in
            guard kind == .ramadan, let date = Self.date(key) else { return false }
            return Hijri.of(date).year == hijriYear
        }.count
    }

    /// Voluntary fasts in a Gregorian year.
    func voluntaryCount(year: Int) -> Int {
        let prefix = String(format: "%04d-", year)
        return fasts.filter { $0.value == .voluntary && $0.key.hasPrefix(prefix) }.count
    }

    private static func date(_ key: String) -> Date? {
        let parts = key.split(separator: "-").compactMap { Int($0) }
        guard parts.count == 3 else { return nil }
        return Calendar.current.date(from: DateComponents(year: parts[0], month: parts[1], day: parts[2], hour: 12))
    }

    // MARK: Reminders

    func setReminder(_ moment: RamadanMoment, _ on: Bool) {
        switch moment {
        case .suhoor:
            suhoorReminder = on
            UserDefaults.standard.set(on, forKey: Key.suhoor)
        case .iftar:
            iftarReminder = on
            UserDefaults.standard.set(on, forKey: Key.iftar)
        }
    }

    func setSuhoorMinutes(_ minutes: Int) {
        suhoorMinutes = minutes
        UserDefaults.standard.set(minutes, forKey: Key.suhoorMinutes)
    }

    func requestPermission() async -> Bool {
        let center = UNUserNotificationCenter.current()
        let status = await center.notificationSettings().authorizationStatus
        if status == .authorized || status == .provisional { return true }
        guard status == .notDetermined else { return false }
        return (try? await center.requestAuthorization(options: [.alert, .sound, .badge])) ?? false
    }

    private static let notificationPrefix = "ramadan-"

    /// Schedules suhoor and iftar for the days of Ramadan in the next 10 days (up to 20,
    /// which with the prayer notifications stays under the system limit of 64).
    /// Paused on rest days. Refreshed every time the app opens.
    func refreshReminders(prayer: PrayerStore, resting: Bool) {
        let center = UNUserNotificationCenter.current()
        let now = Date.now
        var planned: [(RamadanMoment, Date, String)] = []
        if (suhoorReminder || iftarReminder) && !resting {
            for offset in 0..<10 {
                guard let day = Calendar.current.date(byAdding: .day, value: offset, to: now),
                      Hijri.of(day).month == Hijri.ramadan,
                      let schedule = prayer.schedule(for: day) else { continue }
                if suhoorReminder, let fajr = schedule.time(.fajr) {
                    let at = fajr.addingTimeInterval(-Double(suhoorMinutes) * 60)
                    if at > now {
                        planned.append((.suhoor, at, "Fajr is at \(fajr.formatted(date: .omitted, time: .shortened))."))
                    }
                }
                if iftarReminder, let maghrib = schedule.time(.maghrib), maghrib > now {
                    planned.append((.iftar, maghrib, "It is Maghrib time."))
                }
            }
        }
        let reminders = planned
        let previous = refreshTask
        previous?.cancel()
        refreshTask = Task {
            await previous?.value
            let pending = await center.pendingNotificationRequests()
            guard !Task.isCancelled else { return }
            let old = pending.map(\.identifier).filter { $0.hasPrefix(Self.notificationPrefix) }
            center.removePendingNotificationRequests(withIdentifiers: old)
            for (moment, time, body) in reminders {
                guard !Task.isCancelled else { return }
                let content = UNMutableNotificationContent()
                content.title = moment.title
                content.body = body
                content.sound = .default
                let parts = Calendar.current.dateComponents([.year, .month, .day, .hour, .minute], from: time)
                let id = "\(Self.notificationPrefix)\(LibraryStore.dayKey(time))-\(moment.rawValue)"
                try? await center.add(UNNotificationRequest(
                    identifier: id, content: content,
                    trigger: UNCalendarNotificationTrigger(dateMatching: parts, repeats: false)
                ))
            }
        }
    }
}
