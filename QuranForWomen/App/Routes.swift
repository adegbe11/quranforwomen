import SwiftUI
import WidgetKit

/// Every screen that can be pushed onto a tab's navigation stack.
enum Route: Hashable {
    case reader(surah: Int, verse: Int?)
    case duaCategory(String)
    case dua(String)
    case dhikr
    case dhikrHistory
    case customDhikr
    case personalDuas
    case savedAyahs
    case savedDuas
    case annotations
    case readingProgress
    case readingHistory
    case reminder
    case language
    case readingSettings
    case downloads
    case about
    case collection(String)
    case quiz
    case readingPlan
    case prayerTimes
    case qibla
    case prayerSettings
    case islamicCalendar
    case missedPrayers
    case sync
    case journalEntry(UUID)
    case journalTemplates
    case templateCollection(String)
    case journalPrompts
    case promptPack(String)
    case journalSettings
    case lessonNote(UUID)
    case feelings
    case explore
    case settings
    case challenges
    case challenge(String)
    case guidanceSection(String)
    case guidanceArticle(String)
}

/// Selected tab and one navigation path per tab, so any screen can be opened from anywhere.
@Observable
@MainActor
final class AppRouter {
    var tab: AppTab = .today
    var paths: [AppTab: [Route]] = [.today: [], .quran: [], .duas: [], .journal: [], .you: [], .search: []]

    func open(_ route: Route, in tab: AppTab) {
        self.tab = tab
        paths[tab, default: []].append(route)
    }

    func path(for tab: AppTab) -> Binding<[Route]> {
        Binding(
            get: { self.paths[tab] ?? [] },
            set: { self.paths[tab] = $0 }
        )
    }

    /// quranforwomen://verse/2:255 (used by the widget).
    func handle(_ url: URL) {
        if let route = Self.widgetRoute(url) {
            paths[.today] = [route]
            tab = .today
            return
        }
        guard url.scheme == "quranforwomen", url.host == "verse",
              let ref = VerseRef(url.lastPathComponent),
              (1...114).contains(ref.surah), ref.ayah >= 1 else { return }
        paths[.today] = [.reader(surah: ref.surah, verse: ref.ayah)]
        tab = .today
    }
}

private struct RouteDestinations: ViewModifier {
    func body(content: Content) -> some View {
        content.navigationDestination(for: Route.self) { route in
            switch route {
            case let .reader(surah, verse): SurahReaderView(surahID: surah, startVerse: verse).zoomDestination(ZoomID.reader(surah, verse))
            case .duaCategory(let id): DuaCategoryView(categoryID: id).zoomDestination(ZoomID.duaCategory(id))
            case .dua(let id): DuaDetailView(duaID: id).zoomDestination(ZoomID.dua(id))
            case .dhikr: DhikrView().zoomDestination(ZoomID.dhikr)
            case .dhikrHistory: DhikrHistoryView()
            case .customDhikr: CustomDhikrView()
            case .personalDuas: PersonalDuasView().zoomDestination(ZoomID.personalDuas)
            case .savedAyahs: SavedAyahsView().zoomDestination(ZoomID.savedAyahs)
            case .savedDuas: SavedDuasView().zoomDestination(ZoomID.savedDuas)
            case .annotations: AnnotationsView().zoomDestination(ZoomID.annotations)
            case .readingProgress: ReadingProgressView().zoomDestination(ZoomID.readingProgress)
            case .readingHistory: ReadingHistoryView().zoomDestination(ZoomID.readingHistory)
            case .reminder: ReminderSettingsView().zoomDestination(ZoomID.reminder)
            case .language: LanguageSettingsView().zoomDestination(ZoomID.language)
            case .readingSettings: ReadingSettingsView().zoomDestination(ZoomID.readingSettings)
            case .downloads: DownloadsView().zoomDestination(ZoomID.downloads)
            case .about: AboutView().zoomDestination(ZoomID.about)
            case .collection(let id): CollectionView(collectionID: id).zoomDestination(ZoomID.collection(id))
            case .quiz: QuizView().zoomDestination(ZoomID.quiz)
            case .readingPlan: ReadingPlanView().zoomDestination(ZoomID.readingPlan)
            case .prayerTimes: PrayerTimesView().zoomDestination(ZoomID.prayerTimes)
            case .qibla: QiblaView().zoomDestination(ZoomID.qibla)
            case .prayerSettings: PrayerSettingsView().zoomDestination(ZoomID.prayerSettings)
            case .islamicCalendar: IslamicCalendarView()
            case .missedPrayers: MissedPrayersView()
            case .sync: SyncSettingsView().zoomDestination(ZoomID.sync)
            case .journalEntry(let id): JournalEntryView(entryID: id).zoomDestination(ZoomID.entry(id))
            case .journalTemplates: JournalTemplatesView().zoomDestination(ZoomID.journalTemplates)
            case .templateCollection(let id): TemplateCollectionView(collectionID: id).zoomDestination(ZoomID.templateCollection(id))
            case .journalPrompts: JournalPromptsView().zoomDestination(ZoomID.journalPrompts)
            case .promptPack(let id): PromptPackView(packID: id).zoomDestination(ZoomID.promptPack(id))
            case .journalSettings: JournalSettingsView().zoomDestination(ZoomID.journalSettings)
            case .lessonNote(let id): LessonNoteView(noteID: id).zoomDestination(ZoomID.note(id))
            case .feelings: FeelingsView().zoomDestination(ZoomID.feelings)
            case .explore: ExploreView().zoomDestination(ZoomID.explore)
            case .settings: SettingsView().zoomDestination(ZoomID.settings)
            case .challenges: ChallengesView().zoomDestination("challenges")
            case .challenge(let id): ChallengeView(challengeID: id).zoomDestination("challenge-\(id)")
            case .guidanceSection(let section): GuidanceSectionView(section: section).zoomDestination("guidance-\(section)")
            case .guidanceArticle(let id): GuidanceArticleView(articleID: id).zoomDestination("guidanceArticle-\(id)")
            }
        }
    }
}

extension View {
    /// Registers all routes for a tab's stack.
    func appNavigation() -> some View {
        modifier(RouteDestinations())
    }
}

/// A navigation stack for one tab, with shared destinations and the mini player.
struct TabStack<Root: View>: View {
    let tab: AppTab
    @ViewBuilder var root: Root

    @Environment(AppRouter.self) private var router
    @Namespace private var zoom

    var body: some View {
        NavigationStack(path: router.path(for: tab)) {
            root.appNavigation()
        }
        .environment(\.zoomNamespace, zoom)
        .safeAreaInset(edge: .bottom, spacing: 0) {
            // On iOS 26.1 and later the mini player is the tab bar's own bottom accessory.
            if !MiniPlayerBar.usesSystemAccessory {
                MiniPlayerBar()
            }
        }
    }
}

/// Hands today's verse translations to the home screen widget.
enum WidgetBridge {
    static let suite = "group.com.quranforwomen.app"

    @MainActor
    static func publish(content: ContentStore, translations: TranslationStore) {
        guard let shared = UserDefaults(suiteName: suite) else { return }
        var map: [String: String] = [:]
        for ref in content.dailyVerses {
            if let text = translations.text(ref) { map[ref.id] = text }
        }
        shared.set(map, forKey: "widget.translations")
        shared.set(translations.selected.isRightToLeft, forKey: "widget.rtl")
        WidgetCenter.shared.reloadAllTimelines()
    }
}
