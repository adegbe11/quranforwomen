import SwiftUI

struct YouView: View {
    var body: some View {
        TabStack(tab: .you) {
            YouHome()
        }
    }
}

/// Only her own things. Every setting lives behind one row.
private struct YouHome: View {
    @Environment(LibraryStore.self) private var library
    @Environment(JournalStore.self) private var journal

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: Space.xl) {
                YouHeader(
                    saved: library.bookmarks.count,
                    pages: journal.entries.count,
                    finished: library.completedSurahs.count
                )
                .dealIn(0, baseDelay: 0.05)

                GroupedSection(title: "Saved") {
                    linkRow(.savedAyahs, "Saved Ayahs", "bookmark", count: library.bookmarks.count, index: 0)
                    RowDivider()
                    linkRow(.annotations, "Highlights & Notes", "highlighter", count: library.annotatedVerses.count, index: 1)
                    RowDivider()
                    linkRow(.savedDuas, "Saved Duas", "heart", count: library.savedDuaIDs.count, index: 2)
                    RowDivider()
                    linkRow(.personalDuas, "Your Own Words", "pencil", count: library.personalDuas.count, index: 3)
                }
                .focusIn(delay: 0.2)

                GroupedSection(title: "Reading") {
                    linkRow(.readingProgress, "Reading Progress", "book", index: 4)
                    RowDivider()
                    linkRow(.readingPlan, "Reading Plan", "calendar",
                            value: library.plan.map { "Day \($0.dayIndex() + 1)" }, index: 5)
                    RowDivider()
                    linkRow(.readingHistory, "History", "clock.arrow.circlepath", count: library.history.count, index: 6)
                }
                .focusIn(delay: 0.4)

                GroupedSection {
                    linkRow(.islamicCalendar, "Islamic Calendar", "calendar", index: 6)
                    RowDivider()
                    linkRow(.settings, "Settings", "gearshape", index: 6)
                }
                .focusIn(delay: 0.55)
            }
            .padding(.horizontal, Space.page)
            .padding(.vertical, Space.s)
        }
        .pageBackground()
        .navigationTitle("You")
    }

    private func linkRow(_ route: Route, _ title: String, _ symbol: String, count: Int = 0, value: String? = nil, index: Int) -> some View {
        NavigationLink(value: route) {
            HStack(spacing: Space.m) {
                Image(systemName: symbol)
                    .font(.system(size: 17))
                    .foregroundStyle(Palette.accent)
                    .frame(width: 26)
                    .modifier(IconLand(delay: 0.25 + Double(index) * 0.06))
                    .accessibilityHidden(true)
                Text(title)
                    .foregroundStyle(Palette.textPrimary)
                Spacer()
                if count > 0 {
                    RollingNumber(value: count, delay: 0.3 + Double(index) * 0.06)
                        .foregroundStyle(Palette.textSecondary)
                } else if let value {
                    Text(value)
                        .foregroundStyle(Palette.textSecondary)
                        .monospacedDigit()
                        .contentTransition(.numericText())
                        .animation(.snappy, value: value)
                }
                Image(systemName: "chevron.right")
                    .font(.footnote.weight(.semibold))
                    .foregroundStyle(Palette.textSecondary.opacity(0.5))
                    .accessibilityHidden(true)
            }
            .padding(.horizontal, Space.m)
            .padding(.vertical, 12)
            .frame(minHeight: 48)
            .contentShape(Rectangle())
            .zoomSource(route.rowZoomID)
        }
        .dealIn(index, baseDelay: 0.2, step: 0.06)
    }
}

/// Her own milestones, counted up gently. Not scores, never compared.
private struct YouHeader: View {
    let saved: Int
    let pages: Int
    let finished: Int

    var body: some View {
        VStack(alignment: .leading, spacing: Space.l) {
            HStack(spacing: Space.s) {
                LotusMark()
                    .stroke(Palette.accentFill, style: StrokeStyle(lineWidth: 1.5, lineCap: .round, lineJoin: .round))
                    .frame(width: 34, height: 24)
                    .accessibilityHidden(true)
                Text("Your space")
                    .font(.system(.title2, design: .serif, weight: .semibold))
                    .foregroundStyle(Palette.accentFill)
                    .accessibilityAddTraits(.isHeader)
            }
            HStack(spacing: 0) {
                milestone(saved, saved == 1 ? "verse saved" : "verses saved", delay: 0.35)
                Divider().frame(height: 34)
                milestone(pages, pages == 1 ? "page written" : "pages written", delay: 0.45)
                Divider().frame(height: 34)
                milestone(finished, finished == 1 ? "surah finished" : "surahs finished", delay: 0.55)
            }
        }
        .cardSurface(padding: Space.xl)
    }

    private func milestone(_ value: Int, _ label: String, delay: Double) -> some View {
        VStack(spacing: 2) {
            RollingNumber(value: value, delay: delay)
                .font(.system(size: 28, weight: .light, design: .serif))
                .foregroundStyle(Palette.textPrimary)
            Text(label)
                .font(.caption)
                .foregroundStyle(Palette.textSecondary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .accessibilityElement(children: .combine)
    }
}

/// A number that rolls up from zero when it first appears, and rolls on change.
struct RollingNumber: View {
    let value: Int
    var delay: Double = 0
    @State private var shown = 0
    @State private var started = false
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        Text("\(shown)")
            .monospacedDigit()
            .contentTransition(.numericText(value: Double(shown)))
            .onAppear {
                guard !started else { return }
                started = true
                if reduceMotion || value == 0 { shown = value; return }
                withAnimation(.snappy(duration: 0.6).delay(delay)) { shown = value }
            }
            .onChange(of: value) { _, new in
                withAnimation(.snappy) { shown = new }
            }
            .accessibilityLabel("\(value)")
    }
}

/// The row's icon gives one small bounce as the row lands.
private struct IconLand: ViewModifier {
    let delay: Double
    @State private var bounce = false
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    func body(content: Content) -> some View {
        content
            .symbolEffect(.bounce, value: bounce)
            .task {
                guard !reduceMotion else { return }
                try? await Task.sleep(for: .seconds(delay))
                bounce = true
            }
    }
}

/// Every preference in one place, grouped the way iOS Settings is.
/// Motion here answers her changes: she sees and feels each setting take effect.
struct SettingsView: View {
    @Environment(LibraryStore.self) private var library
    @Environment(TranslationStore.self) private var translations
    @Environment(PrayerStore.self) private var prayer
    @AppStorage(PrefKey.appearance) private var appearance: Appearance = .system
    @AppStorage(PrefKey.reminderEnabled) private var reminderEnabled = false
    @AppStorage(PrefKey.eveningReminderEnabled) private var eveningEnabled = false
    @AppStorage(PrefKey.haptics) private var hapticsOn = true
    @Namespace private var appearanceGlide

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: Space.xl) {
                GroupedSection(title: "Appearance") {
                    HStack(spacing: Space.s) {
                        ForEach(Array(Appearance.allCases.enumerated()), id: \.element) { index, option in
                            AppearanceTile(option: option, isSelected: appearance == option, namespace: appearanceGlide) {
                                withAnimation(.smooth(duration: 0.5)) { appearance = option }
                            }
                            .dealIn(index, baseDelay: 0.1, step: 0.06)
                        }
                    }
                    .padding(Space.m)
                }
                .focusIn(delay: 0.05)

                GroupedSection(title: "Reading") {
                    settingRow(.language, "Language & Translation", "character.book.closed", value: translations.selected.nativeName, index: 0)
                    RowDivider()
                    settingRow(.readingSettings, "Text Size & Recitation", "textformat.size", index: 1)
                    RowDivider()
                    settingRow(.downloads, "Downloads", "arrow.down.circle", index: 2)
                }
                .focusIn(delay: 0.2)

                GroupedSection(title: "Daily life") {
                    settingRow(.reminder, "Reminders", "bell", value: reminderEnabled || eveningEnabled ? "On" : "Off", index: 3)
                    RowDivider()
                    TimelineView(.everyMinute) { context in
                        settingRow(.prayerTimes, "Prayer Times", "clock", value: nextPrayerText(at: context.date), index: 4)
                    }
                    RowDivider()
                    settingRow(.qibla, "Qibla", "location.north.line", index: 5)
                }
                .focusIn(delay: 0.32)

                GroupedSection(title: "App") {
                    HapticsRow(isOn: $hapticsOn)
                        .dealIn(6, baseDelay: 0.2, step: 0.05)
                    RowDivider()
                    settingRow(.journalSettings, "Journal", "lock", index: 7)
                    RowDivider()
                    settingRow(.sync, "iCloud Sync", "icloud", value: library.syncEnabled ? "On" : "Off",
                               index: 8, pulse: library.syncEnabled)
                }
                .focusIn(delay: 0.44)

                GroupedSection {
                    settingRow(.about, "Privacy & About", "hand.raised", index: 9)
                }
                .focusIn(delay: 0.55)
            }
            .padding(.horizontal, Space.page)
            .padding(.vertical, Space.s)
        }
        .pageBackground()
        .navigationTitle("Settings")
    }

    private func nextPrayerText(at date: Date) -> String? {
        guard let next = prayer.nextPrayer(after: date) else { return nil }
        return "\(next.0.name) \(next.1.formatted(date: .omitted, time: .shortened))"
    }

    private func settingRow(_ route: Route, _ title: String, _ symbol: String, value: String? = nil, index: Int, pulse: Bool = false) -> some View {
        NavigationLink(value: route) {
            HStack(spacing: Space.m) {
                Image(systemName: symbol)
                    .font(.system(size: 17))
                    .foregroundStyle(Palette.accent)
                    .frame(width: 26)
                    .symbolEffect(.pulse, options: .repeating, isActive: pulse)
                    .modifier(IconLand(delay: 0.25 + Double(index) * 0.05))
                    .accessibilityHidden(true)
                Text(title)
                    .foregroundStyle(Palette.textPrimary)
                Spacer()
                if let value {
                    Text(value)
                        .foregroundStyle(Palette.textSecondary)
                        .lineLimit(1)
                        .contentTransition(.opacity)
                        .animation(.smooth, value: value)
                }
                Image(systemName: "chevron.right")
                    .font(.footnote.weight(.semibold))
                    .foregroundStyle(Palette.textSecondary.opacity(0.5))
                    .accessibilityHidden(true)
            }
            .padding(.horizontal, Space.m)
            .padding(.vertical, 12)
            .frame(minHeight: 48)
            .contentShape(Rectangle())
            .zoomSource(route.rowZoomID)
        }
        .dealIn(index, baseDelay: 0.2, step: 0.05)
    }
}

/// A small phone preview of each appearance. The selection ring glides between them.
private struct AppearanceTile: View {
    let option: Appearance
    let isSelected: Bool
    let namespace: Namespace.ID
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(spacing: Space.xs) {
                ZStack {
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .fill(background)
                    VStack(alignment: .leading, spacing: 4) {
                        Capsule().fill(Palette.accentFill).frame(width: 26, height: 5)
                        RoundedRectangle(cornerRadius: 3).fill(line).frame(height: 14)
                        RoundedRectangle(cornerRadius: 3).fill(line.opacity(0.6)).frame(width: 34, height: 6)
                        Spacer(minLength: 0)
                    }
                    .padding(8)
                }
                .frame(height: 78)
                .overlay {
                    if isSelected {
                        RoundedRectangle(cornerRadius: 14, style: .continuous)
                            .strokeBorder(Palette.accent, lineWidth: 2.5)
                            .padding(-4)
                            .matchedGeometryEffect(id: "appearance", in: namespace)
                    }
                }
                Text(option.title)
                    .font(.footnote.weight(isSelected ? .semibold : .regular))
                    .foregroundStyle(isSelected ? Palette.accent : Palette.textSecondary)
            }
            .frame(maxWidth: .infinity)
            .contentShape(Rectangle())
        }
        .buttonStyle(PressableStyle(scale: 0.95))
        .accessibilityLabel("\(option.title) appearance")
        .accessibilityAddTraits(isSelected ? .isSelected : [])
        .sensoryFeedback(.selection, trigger: isSelected) { _, new in new }
    }

    /// Automatic shows half light, half dark.
    private var background: AnyShapeStyle {
        switch option {
        case .light: AnyShapeStyle(Color(hex: 0xFAF7F0))
        case .dark: AnyShapeStyle(Color(hex: 0x0E1311))
        case .system: AnyShapeStyle(LinearGradient(
            stops: [.init(color: Color(hex: 0xFAF7F0), location: 0.5), .init(color: Color(hex: 0x0E1311), location: 0.5)],
            startPoint: .leading, endPoint: .trailing))
        }
    }

    private var line: Color {
        option == .dark ? Color(hex: 0x27302C) : Color(hex: 0xE7E5DD)
    }
}

/// Turning haptics on plays a sample tap, so she feels what she enabled.
private struct HapticsRow: View {
    @Binding var isOn: Bool
    @State private var sample = 0

    var body: some View {
        Toggle(isOn: Binding(
            get: { isOn },
            set: { new in
                isOn = new
                if new { sample += 1 }
            }
        )) {
            HStack(spacing: Space.m) {
                Image(systemName: "hand.tap")
                    .font(.system(size: 17))
                    .foregroundStyle(Palette.accent)
                    .frame(width: 26)
                    .symbolEffect(.bounce, value: sample)
                    .accessibilityHidden(true)
                Text("Haptics")
                    .foregroundStyle(Palette.textPrimary)
            }
        }
        .tint(Palette.accentFill)
        .padding(.horizontal, Space.m)
        .padding(.vertical, 8)
        .frame(minHeight: 48)
        .sensoryFeedback(.impact(flexibility: .soft, intensity: 0.9), trigger: sample)
    }
}

// MARK: - Saved

/// Her saved verses. One is featured and can be turned over to another,
/// the rest deal in, and a removed verse can be brought back with Undo.
struct SavedAyahsView: View {
    @Environment(QuranStore.self) private var quran
    @Environment(LibraryStore.self) private var library
    @Environment(AppRouter.self) private var router
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @AppStorage(PrefKey.haptics) private var hapticsOn = true

    @State private var quranOrder = false
    @State private var featured: VerseRef?
    @State private var flips = 0
    @State private var removed: VerseRef?
    @State private var undoToken = 0
    @Namespace private var sortGlide

    private var ordered: [VerseRef] {
        let list = Array(library.bookmarks.reversed())
        return quranOrder ? list.sorted { ($0.surah, $0.ayah) < ($1.surah, $1.ayah) } : list
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: Space.l) {
                if library.bookmarks.isEmpty {
                    SavedEmpty(symbol: "bookmark", title: "No saved ayahs",
                               action: "Open the Quran") { router.tab = .quran }
                        .transition(.opacity)
                } else {
                    header
                    if let featured, library.isBookmarked(featured) {
                        FeaturedVerse(ref: featured, flips: flips, canTurn: library.bookmarks.count > 1) { turn() }
                            .dealIn(0, baseDelay: 0.1)
                    }
                    sortSwitch
                        .dealIn(1, baseDelay: 0.1)
                    LazyVStack(spacing: Space.s) {
                        ForEach(Array(ordered.enumerated()), id: \.element) { index, ref in
                            SavedVerseRow(ref: ref) { remove(ref) }
                                .dealIn(min(index, 8), baseDelay: 0.2, step: 0.05)
                                .scrollSettle()
                                .transition(.asymmetric(insertion: .opacity, removal: .scale(scale: 0.9).combined(with: .opacity)))
                        }
                    }
                }
            }
            .padding(.horizontal, Space.page)
            .padding(.vertical, Space.s)
            .animation(reduceMotion ? nil : .spring(response: 0.45, dampingFraction: 0.85), value: library.bookmarks)
            .animation(reduceMotion ? nil : .spring(response: 0.45, dampingFraction: 0.85), value: quranOrder)
        }
        .pageBackground()
        .navigationTitle("Saved Ayahs")
        .overlay(alignment: .bottom) {
            if let removed {
                UndoBar(text: "Removed \(quran.displayName(removed))") {
                    withAnimation(reduceMotion ? nil : Motion.gentle) {
                        library.toggleBookmark(removed)
                        self.removed = nil
                    }
                }
                .padding(.horizontal, Space.page)
                .padding(.bottom, Space.m)
                .transition(.move(edge: .bottom).combined(with: .opacity))
            }
        }
        .animation(reduceMotion ? nil : .spring(response: 0.4, dampingFraction: 0.8), value: removed)
        .onAppear { if featured == nil { featured = library.bookmarks.randomElement() } }
        .sensoryFeedback(.selection, trigger: flips) { _, _ in hapticsOn }
        .sensoryFeedback(.impact(flexibility: .soft, intensity: 0.6), trigger: undoToken) { _, _ in hapticsOn }
    }

    private var header: some View {
        HStack(alignment: .firstTextBaseline, spacing: 6) {
            RollingNumber(value: library.bookmarks.count, delay: 0.1)
                .font(.system(size: 34, weight: .light, design: .rounded))
                .foregroundStyle(Palette.textPrimary)
            Text(library.bookmarks.count == 1 ? "verse saved" : "verses saved")
                .font(.subheadline)
                .foregroundStyle(Palette.textSecondary)
        }
        .focusIn(delay: 0.05)
    }

    private var sortSwitch: some View {
        HStack(spacing: 4) {
            ForEach([false, true], id: \.self) { byQuran in
                let isOn = quranOrder == byQuran
                Button {
                    withAnimation(reduceMotion ? nil : .spring(response: 0.38, dampingFraction: 0.78)) { quranOrder = byQuran }
                } label: {
                    Text(byQuran ? "Quran order" : "Newest first")
                        .font(.subheadline.weight(isOn ? .semibold : .medium))
                        .foregroundStyle(isOn ? Palette.onAccentFill : Palette.accent)
                        .frame(maxWidth: .infinity, minHeight: 36)
                        .background {
                            if isOn {
                                Capsule().fill(Palette.accentFill).matchedGeometryEffect(id: "sort", in: sortGlide)
                            }
                        }
                        .contentShape(Capsule())
                }
                .buttonStyle(PressableStyle(scale: 0.97))
                .accessibilityAddTraits(isOn ? .isSelected : [])
            }
        }
        .padding(4)
        .background(Capsule().fill(Palette.accentWash))
    }

    private func turn() {
        let others = library.bookmarks.filter { $0 != featured }
        guard let next = others.randomElement() else { return }
        withAnimation(reduceMotion ? nil : .spring(response: 0.6, dampingFraction: 0.8)) {
            flips += 1
            featured = next
        }
    }

    private func remove(_ ref: VerseRef) {
        withAnimation(reduceMotion ? nil : .spring(response: 0.45, dampingFraction: 0.85)) {
            library.toggleBookmark(ref)
            removed = ref
            if featured == ref { featured = library.bookmarks.randomElement() }
        }
        undoToken += 1
        let token = undoToken
        Task {
            try? await Task.sleep(for: .seconds(4))
            if token == undoToken { removed = nil }
        }
    }
}

/// One saved verse, chosen to show first. Turning it flips the card to another.
private struct FeaturedVerse: View {
    let ref: VerseRef
    let flips: Int
    let canTurn: Bool
    let turn: () -> Void

    @Environment(QuranStore.self) private var quran
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        VStack(alignment: .leading, spacing: Space.m) {
            HStack {
                Eyebrow(text: "From your saved verses")
                Spacer()
                if canTurn {
                    Button(action: turn) {
                        Label("Another", systemImage: "arrow.triangle.2.circlepath")
                            .font(.subheadline.weight(.medium))
                            .symbolEffect(.bounce, value: flips)
                    }
                    .foregroundStyle(Palette.accent)
                }
            }
            NavigationLink(value: Route.reader(surah: ref.surah, verse: ref.ayah)) {
                VStack(alignment: .leading, spacing: Space.s) {
                    ArabicText(text: "\(quran.text(ref)) ﴿\(ref.ayah.arabicIndic)﴾", size: 26)
                        .inkReveal(delay: 0.15, duration: 1.0)
                    TranslationBody(ref: ref)
                    Text(quran.displayName(ref))
                        .font(.footnote.weight(.semibold))
                        .foregroundStyle(Palette.accent)
                }
                .id(ref)
                .transition(.asymmetric(insertion: .scale(scale: 0.94).combined(with: .opacity), removal: .opacity))
            }
            .buttonStyle(PressableStyle(scale: 0.97))
        }
        .cardSurface(padding: Space.xl, fill: Palette.blush.opacity(0.45))
        // The card tips a little as it turns to a new verse.
        .phaseAnimator([0.0, 1.0, 0.0], trigger: flips) { card, tilt in
            card.rotation3DEffect(.degrees(reduceMotion ? 0 : tilt * 14), axis: (x: 0, y: 1, z: 0), perspective: 0.6)
        } animation: { _ in .spring(response: 0.35, dampingFraction: 0.7) }
    }
}

/// A saved verse in the list. Hold it to remove it.
private struct SavedVerseRow: View {
    let ref: VerseRef
    let remove: () -> Void

    @Environment(QuranStore.self) private var quran

    var body: some View {
        NavigationLink(value: Route.reader(surah: ref.surah, verse: ref.ayah)) {
            VStack(alignment: .leading, spacing: Space.xs) {
                Text(quran.displayName(ref))
                    .font(.footnote.weight(.semibold))
                    .foregroundStyle(Palette.accent)
                ArabicText(text: quran.text(ref), size: 22, lineLimit: 3)
                TranslationPreview(ref: ref)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .cardSurface(padding: Space.m)
            .zoomSource(ZoomID.reader(ref.surah, ref.ayah))
        }
        .buttonStyle(.pressable)
        .contextMenu {
            Button("Remove from Saved", systemImage: "bookmark.slash", role: .destructive, action: remove)
        }
        .accessibilityAction(named: "Remove from Saved", remove)
    }
}

private struct TranslationPreview: View {
    let ref: VerseRef
    @Environment(TranslationStore.self) private var translations

    var body: some View {
        if let text = translations.text(ref) {
            Text(text)
                .font(AppFont.serif(.subheadline))
                .foregroundStyle(Palette.textSecondary)
                .lineLimit(2)
                .multilineTextAlignment(.leading)
        }
    }
}

/// A short bar after removing something, with Undo.
struct UndoBar: View {
    let text: String
    let undo: () -> Void

    var body: some View {
        HStack {
            Text(text)
                .font(.subheadline)
                .lineLimit(1)
            Spacer()
            Button("Undo", action: undo)
                .font(.subheadline.weight(.semibold))
        }
        .foregroundStyle(Palette.textPrimary)
        .padding(.horizontal, Space.m)
        .frame(minHeight: 48)
        .readableGlass(Capsule())
        .shadow(color: .black.opacity(0.1), radius: 12, y: 4)
    }
}

/// An empty list: the symbol bounces in, with a plain line and one way forward.
struct SavedEmpty: View {
    let symbol: String
    let title: String
    var detail: String? = nil
    let action: String
    let perform: () -> Void

    @State private var shown = false

    var body: some View {
        VStack(spacing: Space.m) {
            Image(systemName: symbol)
                .font(.system(size: 44, weight: .light))
                .foregroundStyle(Palette.accent)
                .symbolEffect(.bounce, value: shown)
            Text(title)
                .font(.system(.title3, design: .serif, weight: .semibold))
                .foregroundStyle(Palette.textPrimary)
            if let detail {
                Text(detail)
                    .font(.subheadline)
                    .foregroundStyle(Palette.textSecondary)
                    .multilineTextAlignment(.center)
            }
            Button(action, action: perform)
                .buttonStyle(.tintedCompact)
                .padding(.top, Space.xs)
        }
        .frame(maxWidth: .infinity)
        .padding(.top, Space.xxxl)
        .focusIn(delay: 0.05)
        .onAppear { shown = true }
    }
}

/// Duas she saved. They deal in, and a removed dua can be brought back with Undo.
struct SavedDuasView: View {
    @Environment(ContentStore.self) private var content
    @Environment(LibraryStore.self) private var library
    @Environment(AppRouter.self) private var router
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @AppStorage(PrefKey.haptics) private var hapticsOn = true

    @State private var removed: Dua?
    @State private var undoToken = 0

    private var duas: [Dua] { library.savedDuaIDs.reversed().compactMap { content.dua(id: $0) } }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: Space.l) {
                if duas.isEmpty {
                    SavedEmpty(symbol: "heart", title: "No saved duas",
                               action: "Browse Duas") { router.tab = .duas }
                        .transition(.opacity)
                } else {
                    HStack(alignment: .firstTextBaseline, spacing: 6) {
                        RollingNumber(value: duas.count, delay: 0.1)
                            .font(.system(size: 34, weight: .light, design: .rounded))
                            .foregroundStyle(Palette.textPrimary)
                        Text(duas.count == 1 ? "dua saved" : "duas saved")
                            .font(.subheadline)
                            .foregroundStyle(Palette.textSecondary)
                    }
                    .focusIn(delay: 0.05)

                    LazyVStack(spacing: Space.s) {
                        ForEach(Array(duas.enumerated()), id: \.element.id) { index, dua in
                            NavigationLink(value: Route.dua(dua.id)) {
                                DuaCard(dua: dua)
                                    .zoomSource(ZoomID.dua(dua.id))
                            }
                            .buttonStyle(.pressable)
                            .contextMenu {
                                Button("Remove from Saved", systemImage: "bookmark.slash", role: .destructive) { remove(dua) }
                            }
                            .accessibilityAction(named: "Remove from Saved") { remove(dua) }
                            .dealIn(min(index, 8), baseDelay: 0.12, step: 0.05)
                            .scrollSettle()
                            .transition(.asymmetric(insertion: .opacity, removal: .scale(scale: 0.9).combined(with: .opacity)))
                        }
                    }
                }
            }
            .padding(.horizontal, Space.page)
            .padding(.vertical, Space.s)
            .animation(reduceMotion ? nil : .spring(response: 0.45, dampingFraction: 0.85), value: library.savedDuaIDs)
        }
        .pageBackground()
        .navigationTitle("Saved Duas")
        .overlay(alignment: .bottom) {
            if let removed {
                UndoBar(text: "Removed \(removed.title)") {
                    withAnimation(reduceMotion ? nil : Motion.gentle) {
                        library.toggleSaved(removed)
                        self.removed = nil
                    }
                }
                .padding(.horizontal, Space.page)
                .padding(.bottom, Space.m)
                .transition(.move(edge: .bottom).combined(with: .opacity))
            }
        }
        .animation(reduceMotion ? nil : .spring(response: 0.4, dampingFraction: 0.8), value: removed?.id)
        .sensoryFeedback(.impact(flexibility: .soft, intensity: 0.6), trigger: undoToken) { _, _ in hapticsOn }
    }

    private func remove(_ dua: Dua) {
        withAnimation(reduceMotion ? nil : .spring(response: 0.45, dampingFraction: 0.85)) {
            library.toggleSaved(dua)
            removed = dua
        }
        undoToken += 1
        let token = undoToken
        Task {
            try? await Task.sleep(for: .seconds(4))
            if token == undoToken { removed = nil }
        }
    }
}

// MARK: - Reading progress

/// Calm and factual. No streaks, no scores.
struct ReadingProgressView: View {
    @Environment(QuranStore.self) private var quran
    @Environment(LibraryStore.self) private var library

    var body: some View {
        List {
            Section {
                if let last = library.lastRead {
                    let position = quran.globalNumber(last)
                    VStack(alignment: .leading, spacing: Space.s) {
                        Text("You are reading")
                            .font(.subheadline)
                            .foregroundStyle(Palette.textSecondary)
                        Text("\(quran.surah(last.surah).name), verse \(last.ayah)")
                            .font(.system(.title3, design: .serif, weight: .semibold))
                        GrowBar(value: Double(position) / Double(max(quran.totalVerses, 1)))
                        Text("Verse \(position) of \(quran.totalVerses) in the Quran")
                            .font(.footnote)
                            .foregroundStyle(Palette.textSecondary)
                    }
                    .padding(.vertical, Space.xs)
                    NavigationLink(value: Route.reader(surah: last.surah, verse: last.ayah)) {
                        Text("Continue Reading")
                            .foregroundStyle(Palette.accent)
                    }
                } else {
                    Text("Not started")
                        .foregroundStyle(Palette.textSecondary)
                }
            }
            .listRowBackground(Palette.surface)

            Section {
                SurahMosaic(completed: library.completedSurahs, current: library.lastRead?.surah)
                    .padding(.vertical, Space.s)
            } header: {
                Text("All 114 surahs")
            }
            .listRowBackground(Palette.surface)

            Section {
                LabeledContent("Surahs read to the end", value: "\(library.completedSurahs.count) of 114")
                LabeledContent("Saved ayahs", value: "\(library.bookmarks.count)")
            }
            .listRowBackground(Palette.surface)

            if !library.completedSurahs.isEmpty {
                Section("Read to the end") {
                    ForEach(library.completedSurahs.sorted(), id: \.self) { id in
                        NavigationLink(value: Route.reader(surah: id, verse: nil)) {
                            SurahRow(surah: quran.surah(id), isComplete: true)
                        }
                    }
                }
                .listRowBackground(Palette.surface)
            }
        }
        .scrollContentBackground(.hidden)
        .pageBackground()
        .navigationTitle("Reading Progress")
    }
}

private extension Route {
    /// The zoom id for a screen opened from a row on You or Settings.
    var rowZoomID: String {
        switch self {
        case .savedAyahs: return ZoomID.savedAyahs
        case .annotations: return ZoomID.annotations
        case .savedDuas: return ZoomID.savedDuas
        case .personalDuas: return ZoomID.personalDuas
        case .readingProgress: return ZoomID.readingProgress
        case .readingHistory: return ZoomID.readingHistory
        case .readingPlan: return ZoomID.readingPlan
        case .settings: return ZoomID.settings
        case .language: return ZoomID.language
        case .readingSettings: return ZoomID.readingSettings
        case .downloads: return ZoomID.downloads
        case .reminder: return ZoomID.reminder
        case .prayerSettings: return ZoomID.prayerSettings
        case .qibla: return ZoomID.qibla
        case .journalSettings: return ZoomID.journalSettings
        case .sync: return ZoomID.sync
        case .about: return ZoomID.about
        default: return "row"
        }
    }
}

// MARK: - Progress pieces

/// 114 dots, one per surah. They ripple in as a quick wave; her current surah gently pulses.
private struct SurahMosaic: View {
    let completed: Set<Int>
    let current: Int?

    @Environment(QuranStore.self) private var quran
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    private let columns = Array(repeating: GridItem(.flexible(), spacing: 6), count: 12)

    var body: some View {
        LazyVGrid(columns: columns, spacing: 6) {
            ForEach(1...114, id: \.self) { id in
                NavigationLink(value: Route.reader(surah: id, verse: nil)) {
                    MosaicDot(isDone: completed.contains(id), isCurrent: current == id)
                        .modifier(WaveIn(index: id))
                        .zoomSource(ZoomID.reader(id, nil))
                }
                .buttonStyle(PressableStyle(scale: 0.8))
                .accessibilityLabel("Surah \(id), \(quran.surah(id).name)")
                .accessibilityValue(completed.contains(id) ? "Read to the end" : (current == id ? "Reading now" : "Not yet"))
            }
        }
    }
}

private struct MosaicDot: View {
    let isDone: Bool
    let isCurrent: Bool
    @State private var pulse = false
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        Circle()
            .fill(isDone ? Palette.accentFill : Palette.accentWash)
            .aspectRatio(1, contentMode: .fit)
            .overlay {
                if isCurrent {
                    Circle()
                        .strokeBorder(Palette.blushStrong, lineWidth: 2)
                        .scaleEffect(pulse ? 1.35 : 1)
                        .opacity(pulse ? 0.2 : 1)
                }
            }
            .onAppear {
                guard isCurrent, !reduceMotion else { return }
                withAnimation(.easeInOut(duration: 1.4).repeatForever(autoreverses: true)) { pulse = true }
            }
    }
}

/// A quick wave across the grid: each dot pops in a moment after the one before.
private struct WaveIn: ViewModifier {
    let index: Int
    @State private var shown = false
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    func body(content: Content) -> some View {
        content
            .scaleEffect(shown || reduceMotion ? 1 : 0.2)
            .opacity(shown || reduceMotion ? 1 : 0)
            .onAppear {
                guard !shown else { return }
                withAnimation(.spring(response: 0.4, dampingFraction: 0.6).delay(0.15 + Double(index) * 0.008)) { shown = true }
            }
    }
}

/// A bar that grows to its value when it appears.
private struct GrowBar: View {
    let value: Double
    @State private var shown = false

    var body: some View {
        GeometryReader { geo in
            ZStack(alignment: .leading) {
                Capsule().fill(Palette.accentWash)
                Capsule()
                    .fill(LinearGradient(colors: [Palette.accentFill, Palette.accent], startPoint: .leading, endPoint: .trailing))
                    .frame(width: geo.size.width * CGFloat(shown ? min(max(value, 0.01), 1) : 0))
            }
        }
        .frame(height: 6)
        .onAppear { withAnimation(.spring(response: 1.0, dampingFraction: 0.85).delay(0.3)) { shown = true } }
        .accessibilityHidden(true)
    }
}
