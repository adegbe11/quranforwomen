import SwiftUI

@main
struct QuranForWomenApp: App {
    @State private var quran: QuranStore
    @State private var content = ContentStore()
    @State private var library: LibraryStore
    @State private var translations = TranslationStore()
    @State private var player: AudioPlayer
    @State private var transliteration = TransliterationStore()
    @State private var recitations: RecitationDownloads
    @State private var prayer = PrayerStore()
    @State private var fasting = FastingStore()
    @State private var missedPrayers = MissedPrayersStore()
    @State private var router = AppRouter()
    @State private var journal = JournalStore()
    @State private var planHistory = PlanHistoryStore()
    @State private var dhikrStore = DhikrStore()
    @State private var challenges = ChallengeStore()

    @AppStorage(PrefKey.appearance) private var appearance: Appearance = .system
    /// The launch lotus stays over the app for one bloom on a cold start, then lets go.
    @State private var launching = true

    init() {
        let quran = QuranStore()
        _quran = State(initialValue: quran)
        _player = State(initialValue: AudioPlayer(quran: quran))
        _recitations = State(initialValue: RecitationDownloads(quran: quran))
        let library = LibraryStore()
        _library = State(initialValue: library)
        NotificationHandler.shared.library = library
        NotificationHandler.shared.register()
    }

    var body: some Scene {
        WindowGroup {
            RootView()
                .environment(quran)
                .environment(content)
                .environment(library)
                .environment(translations)
                .environment(player)
                .environment(transliteration)
                .environment(recitations)
                .environment(prayer)
                .environment(fasting)
                .environment(missedPrayers)
                .environment(router)
                .environment(journal)
                .environment(planHistory)
                .environment(dhikrStore)
                .environment(challenges)
                .tint(Palette.accent)
                .overlay {
                    if launching {
                        LaunchBloom { launching = false }
                            .transition(.opacity)
                    }
                }
                .preferredColorScheme(appearance.colorScheme)
        }
    }
}

/// The brand splash. The launch screen is plain emerald; the lotus is then drawn live, petal by
/// petal, a light fills it and a gold shimmer passes once, the name draws together beneath it,
/// and a circle opens from the lotus to reveal the app.
private struct LaunchBloom: View {
    let done: () -> Void

    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @AppStorage(PrefKey.haptics) private var hapticsOn = true
    @State private var center: CGFloat = 0
    @State private var sides: CGFloat = 0
    @State private var water: CGFloat = 0
    @State private var lit = false
    @State private var shimmer: CGFloat = -1
    @State private var named = false
    @State private var reveal: CGFloat = 0
    @State private var leaving = false

    private let cream = Color(hex: 0xFAF7F0)
    private let gold = Color(hex: 0xE9CF8F)

    var body: some View {
        GeometryReader { geo in
            let cover = hypot(geo.size.width, geo.size.height) * 1.1
            ZStack {
                ZStack {
                    Color("LaunchBackground")
                    LinearGradient(colors: [Color(hex: 0x032A20), Color(hex: 0x064E3B), Color(hex: 0x0B6A50)],
                                   startPoint: .top, endPoint: .bottom)
                        .opacity(lit ? 1 : 0)
                    RadialGradient(colors: [cream.opacity(0.16), .clear], center: .center, startRadius: 0, endRadius: 220)
                        .scaleEffect(lit ? 1 : 0.4)
                        .opacity(lit ? 1 : 0)
                }
                // The circle that opens from the lotus to show the app underneath.
                .mask {
                    ZStack {
                        Rectangle()
                        Circle()
                            .frame(width: cover * reveal, height: cover * reveal)
                            .blendMode(.destinationOut)
                    }
                    .compositingGroup()
                }

                mark
                    .scaleEffect(leaving ? 1.35 : (lit ? 1.04 : 1))
                    .opacity(leaving ? 0 : 1)
                    .blur(radius: leaving ? 8 : 0)
            }
            .frame(width: geo.size.width, height: geo.size.height)
        }
        .ignoresSafeArea()
        .allowsHitTesting(reveal < 0.5)
        .accessibilityHidden(true)
        .sensoryFeedback(.impact(flexibility: .soft, intensity: 0.5), trigger: lit) { _, new in hapticsOn && new }
        .task { await play() }
    }

    private var mark: some View {
        VStack(spacing: 22) {
            ZStack {
                LotusPiece(part: .all)
                    .fill(cream.opacity(lit ? 0.12 : 0))
                LotusPiece(part: .center)
                    .trim(from: 0, to: center)
                    .stroke(cream, style: stroke)
                LotusPiece(part: .left)
                    .trim(from: 0, to: sides)
                    .stroke(cream, style: stroke)
                LotusPiece(part: .right)
                    .trim(from: 0, to: sides)
                    .stroke(cream, style: stroke)
                LotusPiece(part: .water)
                    .trim(from: 0.5 - water / 2, to: 0.5 + water / 2)
                    .stroke(cream, style: stroke)
                // One pass of gold light across the lotus.
                ZStack {
                    LinearGradient(colors: [.clear, gold.opacity(0.9), .clear], startPoint: .leading, endPoint: .trailing)
                        .frame(width: 40)
                        .offset(x: shimmer * 80)
                }
                .frame(width: 96, height: 69)
                .mask { LotusPiece(part: .all).stroke(style: stroke) }
                .opacity(shimmer > -1 && shimmer < 1 ? 1 : 0)
            }
            .frame(width: 96, height: 69)
            .shadow(color: cream.opacity(lit ? 0.35 : 0), radius: lit ? 20 : 0)

            Text("Quran for Women")
                .font(.system(size: 30, weight: .semibold, design: .serif))
                .tracking(named ? 0.4 : 9)
                .foregroundStyle(cream)
                .fixedSize()
                .opacity(named ? 1 : 0)
                .blur(radius: named ? 0 : 8)
        }
        // Keeps the lotus at the exact center of the screen while the name sits below it.
        .offset(y: 29)
    }

    private var stroke: StrokeStyle {
        StrokeStyle(lineWidth: 2.6, lineCap: .round, lineJoin: .round)
    }

    @MainActor
    private func play() async {
        if reduceMotion {
            center = 1; sides = 1; water = 1; lit = true; named = true
            try? await Task.sleep(for: .milliseconds(700))
            withAnimation(.easeOut(duration: 0.35)) { leaving = true; reveal = 1 }
            try? await Task.sleep(for: .milliseconds(350))
            done()
            return
        }
        try? await Task.sleep(for: .milliseconds(100))
        withAnimation(.easeInOut(duration: 0.6)) { center = 1 }
        withAnimation(.easeInOut(duration: 0.55).delay(0.15)) { sides = 1 }
        withAnimation(.easeOut(duration: 0.5).delay(0.3)) { water = 1 }
        try? await Task.sleep(for: .milliseconds(650))
        withAnimation(.easeOut(duration: 0.6)) { lit = true }
        withAnimation(.easeInOut(duration: 0.6)) { shimmer = 1 }
        try? await Task.sleep(for: .milliseconds(100))
        withAnimation(.spring(response: 0.6, dampingFraction: 0.85)) { named = true }
        try? await Task.sleep(for: .milliseconds(800))
        withAnimation(.easeIn(duration: 0.55)) {
            reveal = 1
            leaving = true
        }
        try? await Task.sleep(for: .milliseconds(560))
        done()
    }
}

/// The parts of the lotus, with the same geometry as LotusMark, so each can be drawn on its own.
struct LotusPiece: Shape {
    enum Part { case center, left, right, water, all }
    let part: Part

    func path(in rect: CGRect) -> Path {
        let w = rect.width, h = rect.height
        let base = CGPoint(x: rect.midX, y: rect.maxY - h * 0.08)
        var p = Path()
        if part == .center || part == .all {
            p.move(to: base)
            p.addQuadCurve(to: CGPoint(x: rect.midX, y: rect.minY), control: CGPoint(x: rect.midX - w * 0.2, y: rect.minY + h * 0.45))
            p.addQuadCurve(to: base, control: CGPoint(x: rect.midX + w * 0.2, y: rect.minY + h * 0.45))
        }
        if part == .left || part == .all {
            p.move(to: base)
            p.addQuadCurve(to: CGPoint(x: rect.minX + w * 0.06, y: rect.minY + h * 0.38), control: CGPoint(x: rect.minX + w * 0.12, y: rect.maxY - h * 0.08))
            p.addQuadCurve(to: base, control: CGPoint(x: rect.midX - w * 0.14, y: rect.minY + h * 0.42))
        }
        if part == .right || part == .all {
            p.move(to: base)
            p.addQuadCurve(to: CGPoint(x: rect.maxX - w * 0.06, y: rect.minY + h * 0.38), control: CGPoint(x: rect.maxX - w * 0.12, y: rect.maxY - h * 0.08))
            p.addQuadCurve(to: base, control: CGPoint(x: rect.midX + w * 0.14, y: rect.minY + h * 0.42))
        }
        if part == .water || part == .all {
            p.move(to: CGPoint(x: rect.minX + w * 0.18, y: rect.maxY))
            p.addQuadCurve(to: CGPoint(x: rect.maxX - w * 0.18, y: rect.maxY), control: CGPoint(x: rect.midX, y: rect.maxY - h * 0.1))
        }
        return p
    }
}

struct RootView: View {
    @AppStorage(PrefKey.hasOnboarded) private var hasOnboarded = false
    @Environment(TranslationStore.self) private var translations
    @Environment(ContentStore.self) private var content
    @Environment(LibraryStore.self) private var library
    @Environment(PrayerStore.self) private var prayer
    @Environment(FastingStore.self) private var fasting
    @Environment(AppRouter.self) private var router
    @Environment(JournalStore.self) private var journal
    @Environment(\.scenePhase) private var scenePhase
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        ZStack {
            if hasOnboarded {
                MainTabView()
                    .transition(.opacity)
            } else {
                OnboardingFlow {
                    withAnimation(reduceMotion ? nil : .easeInOut(duration: 0.5)) {
                        hasOnboarded = true
                    }
                }
                .transition(.opacity)
            }
        }
        .pageBackground()
        .widgetSync()
        .task(id: hasOnboarded) {
            // The translation language is chosen during onboarding, so fetch it after that.
            guard hasOnboarded else { return }
            await translations.prepareSelected()
            WidgetBridge.publish(content: content, translations: translations)
        }
        .onChange(of: translations.selectedState) { _, state in
            if state == .ready { WidgetBridge.publish(content: content, translations: translations) }
        }
        .onChange(of: library.isResting, initial: true) { _, resting in
            prayer.isResting = resting
            fasting.refreshReminders(prayer: prayer, resting: resting)
        }
        .onChange(of: scenePhase) { _, phase in
            // Keep the next few days of prayer notifications topped up.
            if phase == .active { prayer.refreshNotifications() }
            if phase == .active { fasting.refreshReminders(prayer: prayer, resting: library.isResting) }
            if phase == .background && journal.lockEnabled {
                journal.relock()
                // Close every private page, wherever it was opened from.
                router.paths[.journal] = []
                router.paths[.search] = (router.paths[.search] ?? []).filter {
                    if case .journalEntry = $0 { return false }
                    if case .lessonNote = $0 { return false }
                    return true
                }
            }
        }
        .onOpenURL { url in
            hasOnboarded = true
            router.handle(url)
        }
    }
}

enum AppTab: Hashable {
    case today, quran, duas, journal, you, search
}

struct MainTabView: View {
    @Environment(AppRouter.self) private var router

    var body: some View {
        if #available(iOS 18.0, *) {
            ModernTabs()
        } else {
            legacyTabs
        }
    }

    /// iOS 17: the classic tab bar. Search lives inside each tab.
    private var legacyTabs: some View {
        @Bindable var router = router
        return TabView(selection: $router.tab) {
            TodayView()
                .tabItem { Label("Today", systemImage: "sun.max") }
                .tag(AppTab.today)
            QuranLibraryView()
                .tabItem { Label("Quran", systemImage: "book") }
                .tag(AppTab.quran)
            DuasView()
                .tabItem { Label("Duas", systemImage: "hands.and.sparkles") }
                .tag(AppTab.duas)
            JournalTab()
                .tabItem { Label("Journal", systemImage: "square.and.pencil") }
                .tag(AppTab.journal)
            YouView()
                .tabItem { Label("You", systemImage: "person.crop.circle") }
                .tag(AppTab.you)
        }
    }
}

/// iOS 18 and later: the native Tab API with a search tab. On iOS 26 and 27 the bar is
/// Liquid Glass, shrinks while she scrolls, and the mini player becomes its bottom accessory.
@available(iOS 18.0, *)
private struct ModernTabs: View {
    @Environment(AppRouter.self) private var router

    var body: some View {
        @Bindable var router = router
        TabView(selection: $router.tab) {
            Tab("Today", systemImage: "sun.max", value: AppTab.today) { TodayView() }
            Tab("Quran", systemImage: "book", value: AppTab.quran) { QuranLibraryView() }
            Tab("Duas", systemImage: "hands.and.sparkles", value: AppTab.duas) { DuasView() }
            Tab("Journal", systemImage: "square.and.pencil", value: AppTab.journal) { JournalTab() }
            Tab("You", systemImage: "person.crop.circle", value: AppTab.you) { YouView() }
            Tab(value: AppTab.search, role: .search) { SearchTab() }
        }
        .modifier(LiquidTabBar())
    }
}

/// The iOS 26 and later tab bar behaviors, each only where the system supports it.
private struct LiquidTabBar: ViewModifier {
    @Environment(AudioPlayer.self) private var player

    func body(content: Content) -> some View {
        if #available(iOS 26.1, *) {
            content
                .tabBarMinimizeBehavior(.onScrollDown)
                .tabViewSearchActivation(.searchTabSelection)
                .tabViewBottomAccessory(isEnabled: player.current != nil) {
                    MiniPlayerAccessory()
                }
        } else {
            if #available(iOS 26.0, *) {
                content
                    .tabBarMinimizeBehavior(.onScrollDown)
                    .tabViewSearchActivation(.searchTabSelection)
            } else {
                content
            }
        }
    }
}
