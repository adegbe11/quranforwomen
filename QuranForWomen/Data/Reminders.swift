import Foundation
import UserNotifications

/// The three gentle times offered during onboarding.
enum ReminderMoment: String, CaseIterable, Identifiable {
    case morning, afternoon, evening, later

    var id: String { rawValue }

    var title: String {
        switch self {
        case .morning: "Morning"
        case .afternoon: "Afternoon"
        case .evening: "Evening"
        case .later: "I'll decide later"
        }
    }

    var detail: String {
        switch self {
        case .morning: "Around 7:00 AM"
        case .afternoon: "Around 1:00 PM"
        case .evening: "Around 8:00 PM"
        case .later: "No reminder for now"
        }
    }

    var symbol: String {
        switch self {
        case .morning: "sunrise"
        case .afternoon: "sun.max"
        case .evening: "moon"
        case .later: "clock"
        }
    }

    /// Minutes after midnight.
    var minutes: Int? {
        switch self {
        case .morning: 7 * 60
        case .afternoon: 13 * 60
        case .evening: 20 * 60
        case .later: nil
        }
    }
}

/// Two optional gentle reminders: a Quran moment, and evening remembrance. No streaks, no guilt.
enum ReminderKind: String {
    case quran = "daily-quran-moment"
    case evening = "evening-remembrance"

    var title: String {
        switch self {
        case .quran: "A moment with the Quran"
        case .evening: "Evening remembrance"
        }
    }

    var body: String {
        switch self {
        case .quran: "Your verse for today is here whenever you are ready."
        case .evening: "A few calm minutes of evening duas and dhikr."
        }
    }
}

enum ReminderScheduler {
    /// Asks permission if needed and schedules the reminder.
    /// Returns false when the user has not allowed notifications.
    static func enable(atMinutes minutes: Int, kind: ReminderKind = .quran) async -> Bool {
        let center = UNUserNotificationCenter.current()
        let settings = await center.notificationSettings()
        var allowed = settings.authorizationStatus == .authorized || settings.authorizationStatus == .provisional
        if settings.authorizationStatus == .notDetermined {
            allowed = (try? await center.requestAuthorization(options: [.alert, .sound])) ?? false
        }
        guard allowed else { return false }

        center.removePendingNotificationRequests(withIdentifiers: [kind.rawValue])
        let content = UNMutableNotificationContent()
        content.title = kind.title
        content.body = kind.body
        content.sound = .default

        var components = DateComponents()
        components.hour = minutes / 60
        components.minute = minutes % 60
        let trigger = UNCalendarNotificationTrigger(dateMatching: components, repeats: true)
        do {
            try await center.add(UNNotificationRequest(identifier: kind.rawValue, content: content, trigger: trigger))
            return true
        } catch {
            return false
        }
    }

    static func disable(_ kind: ReminderKind = .quran) {
        UNUserNotificationCenter.current().removePendingNotificationRequests(withIdentifiers: [kind.rawValue])
    }

    static func isDenied() async -> Bool {
        await UNUserNotificationCenter.current().notificationSettings().authorizationStatus == .denied
    }
}
