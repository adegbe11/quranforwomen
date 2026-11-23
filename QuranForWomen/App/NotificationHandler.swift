import Foundation
import UserNotifications

/// Shows prayer notifications even while the app is open, and handles the
/// "I prayed" button on the notification, which ticks that prayer in her tracker.
final class NotificationHandler: NSObject, UNUserNotificationCenterDelegate {
    static let shared = NotificationHandler()

    @MainActor weak var library: LibraryStore?

    /// Call once at launch, before any notification can arrive.
    func register() {
        let center = UNUserNotificationCenter.current()
        center.delegate = self
        let prayed = UNNotificationAction(
            identifier: PrayerStore.prayedActionID,
            title: "I prayed",
            options: [.authenticationRequired]
        )
        let category = UNNotificationCategory(
            identifier: PrayerStore.categoryID,
            actions: [prayed],
            intentIdentifiers: [],
            options: []
        )
        center.setNotificationCategories([category])
    }

    // Play the sound and show the banner even when the app is in front.
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification
    ) async -> UNNotificationPresentationOptions {
        [.banner, .list, .sound]
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse
    ) async {
        guard response.actionIdentifier == PrayerStore.prayedActionID else { return }
        let info = response.notification.request.content.userInfo
        guard let prayer = info["prayer"] as? String else { return }
        let time = (info["time"] as? TimeInterval).map(Date.init(timeIntervalSince1970:)) ?? .now
        await MainActor.run {
            self.library?.setPrayed(prayer, on: time)
        }
    }
}
