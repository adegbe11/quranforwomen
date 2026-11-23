import Foundation
import UserNotifications

/// Daily reminders added after the first two: one for the reading plan, one to write in the journal.
/// Each has its own identifier, so turning one off never touches another.
enum GentleReminder: String {
    case plan = "reading-plan"
    case journal = "journal-writing"

    var title: String {
        switch self {
        case .plan: "Reading plan"
        case .journal: "Journal"
        }
    }

    var body: String {
        switch self {
        case .plan: "Today's reading is ready whenever you are."
        case .journal: "A few minutes to write, whenever you are ready."
        }
    }

    var enabledKey: String { "reminder.\(rawValue).enabled" }
    var minutesKey: String { "reminder.\(rawValue).minutes" }

    /// Minutes after midnight.
    var defaultMinutes: Int {
        switch self {
        case .plan: 7 * 60
        case .journal: 21 * 60
        }
    }

    /// Asks permission if needed and schedules the reminder.
    /// Returns false when notifications are not allowed.
    func enable(atMinutes minutes: Int) async -> Bool {
        let center = UNUserNotificationCenter.current()
        let settings = await center.notificationSettings()
        var allowed = settings.authorizationStatus == .authorized || settings.authorizationStatus == .provisional
        if settings.authorizationStatus == .notDetermined {
            allowed = (try? await center.requestAuthorization(options: [.alert, .sound])) ?? false
        }
        guard allowed else { return false }

        center.removePendingNotificationRequests(withIdentifiers: [rawValue])
        let content = UNMutableNotificationContent()
        content.title = title
        content.body = body
        content.sound = .default

        var components = DateComponents()
        components.hour = minutes / 60
        components.minute = minutes % 60
        let trigger = UNCalendarNotificationTrigger(dateMatching: components, repeats: true)
        do {
            try await center.add(UNNotificationRequest(identifier: rawValue, content: content, trigger: trigger))
            return true
        } catch {
            return false
        }
    }

    func disable() {
        UNUserNotificationCenter.current().removePendingNotificationRequests(withIdentifiers: [rawValue])
    }

    /// Removes the reminder and switches its setting off, for example when the plan ends.
    func turnOff(defaults: UserDefaults = .standard) {
        disable()
        defaults.set(false, forKey: enabledKey)
    }
}
