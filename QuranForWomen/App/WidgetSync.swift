import SwiftUI
import WidgetKit

/// What the home screen widgets need from the app, written to the App Group.
/// The widget extension cannot read UserDefaults.standard, so the app copies it here.
extension WidgetBridge {
    enum Key {
        static let latitude = "widget.prayer.latitude"
        static let longitude = "widget.prayer.longitude"
        static let method = "widget.prayer.method"
        static let asr = "widget.prayer.asr"
        static let place = "widget.prayer.place"
        static let surahsFinished = "widget.progress.surahs"
        static let planDays = "widget.plan.days"
        static let planStart = "widget.plan.start"
        static let planFinished = "widget.plan.finished"
        static let dhikrDay = "widget.dhikr.day"
        static let dhikrToday = "widget.dhikr.today"
    }

    /// Widget kinds, matching the widget extension.
    enum Kind {
        static let prayer = "NextPrayer"
        static let progress = "ReadingProgress"
        static let dhikr = "DhikrToday"
    }

    @MainActor
    static func publishPrayer(_ prayer: PrayerStore) {
        guard let shared = UserDefaults(suiteName: suite) else { return }
        if let latitude = prayer.latitude, let longitude = prayer.longitude {
            shared.set(latitude, forKey: Key.latitude)
            shared.set(longitude, forKey: Key.longitude)
        } else {
            shared.removeObject(forKey: Key.latitude)
            shared.removeObject(forKey: Key.longitude)
        }
        shared.set(prayer.calculationMethod.rawValue, forKey: Key.method)
        shared.set(prayer.asr.rawValue, forKey: Key.asr)
        shared.set(prayer.placeName, forKey: Key.place)
        WidgetCenter.shared.reloadTimelines(ofKind: Kind.prayer)
    }

    @MainActor
    static func publishProgress(_ library: LibraryStore) {
        guard let shared = UserDefaults(suiteName: suite) else { return }
        shared.set(library.completedSurahs.count, forKey: Key.surahsFinished)
        if let plan = library.plan {
            shared.set(plan.days, forKey: Key.planDays)
            shared.set(plan.start.timeIntervalSince1970, forKey: Key.planStart)
            shared.set(plan.finishedDays.count, forKey: Key.planFinished)
        } else {
            shared.removeObject(forKey: Key.planDays)
            shared.removeObject(forKey: Key.planStart)
            shared.removeObject(forKey: Key.planFinished)
        }
        WidgetCenter.shared.reloadTimelines(ofKind: Kind.progress)
    }

    /// Today's dhikr total, from the same daily count the dhikr screen shows.
    @MainActor
    static func publishDhikr(_ extras: DhikrStore) {
        guard let shared = UserDefaults(suiteName: suite) else { return }
        shared.set(LibraryStore.dayKey(.now), forKey: Key.dhikrDay)
        shared.set(extras.total(), forKey: Key.dhikrToday)
        WidgetCenter.shared.reloadTimelines(ofKind: Kind.dhikr)
    }
}

extension AppRouter {
    /// quranforwomen://dhikr, ://dua/<id>, ://prayer and ://progress (used by the widgets).
    static func widgetRoute(_ url: URL) -> Route? {
        guard url.scheme == "quranforwomen" else { return nil }
        switch url.host {
        case "dhikr": return .dhikr
        case "prayer": return .prayerTimes
        case "progress": return .readingProgress
        case "plan": return .readingPlan
        case "dua":
            let id = url.lastPathComponent
            return id.isEmpty || id == "/" ? nil : .dua(id)
        default: return nil
        }
    }
}

/// Keeps the widgets in step with the app while it is open.
private struct WidgetSync: ViewModifier {
    @Environment(PrayerStore.self) private var prayer
    @Environment(LibraryStore.self) private var library
    @Environment(DhikrStore.self) private var extras

    private struct PrayerInputs: Equatable {
        var latitude: Double?
        var longitude: Double?
        var method: CalculationMethod
        var asr: AsrMethod
        var place: String?
    }

    private struct ProgressInputs: Equatable {
        var surahs: Int
        var plan: ReadingPlan?
    }

    func body(content: Content) -> some View {
        content
            .task(id: PrayerInputs(
                latitude: prayer.latitude, longitude: prayer.longitude,
                method: prayer.calculationMethod, asr: prayer.asr, place: prayer.placeName
            )) {
                WidgetBridge.publishPrayer(prayer)
            }
            .task(id: ProgressInputs(surahs: library.completedSurahs.count, plan: library.plan)) {
                WidgetBridge.publishProgress(library)
            }
            .task(id: extras.total()) {
                // Wait for a pause in counting before asking the widget to redraw.
                try? await Task.sleep(for: .seconds(2))
                guard !Task.isCancelled else { return }
                WidgetBridge.publishDhikr(extras)
            }
    }
}

extension View {
    /// Publishes prayer settings, reading progress and today's dhikr to the widgets.
    func widgetSync() -> some View {
        modifier(WidgetSync())
    }
}
