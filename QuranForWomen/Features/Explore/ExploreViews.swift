import SwiftUI

// MARK: - Shared verse card

/// A verse with its translation, opening the reader in place.
/// When asked, the Arabic inks in right to left. A saved verse's bookmark gives one
/// small bounce as the card lands, and a new highlight colour washes in softly.
struct VerseCard: View {
    let ref: VerseRef
    var arabicSize: CGFloat = 24
    /// When set, the Arabic inks in right to left after this many seconds.
    var inkDelay: Double?

    @Environment(QuranStore.self) private var quran
    @Environment(LibraryStore.self) private var library
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var landed = false

    var body: some View {
        let saved = library.isBookmarked(ref)
        NavigationLink(value: Route.reader(surah: ref.surah, verse: ref.ayah)) {
            VStack(alignment: .leading, spacing: Space.m) {
                ArabicText(text: "\(quran.text(ref)) ﴿\(ref.ayah.arabicIndic)﴾", size: arabicSize)
                    .modifier(OptionalInkReveal(delay: inkDelay))
                TranslationBody(ref: ref)
                HStack {
                    Text(quran.displayName(ref))
                        .font(.footnote.weight(.semibold))
                        .foregroundStyle(Palette.accent)
                    Spacer()
                    if saved {
                        Image(systemName: "bookmark.fill")
                            .font(.footnote)
                            .foregroundStyle(Palette.blushStrong)
                            .symbolEffect(.bounce, value: landed)
                            .transition(.scale(scale: 0.4).combined(with: .opacity))
                            .accessibilityLabel("Saved")
                    }
                }
                .animation(reduceMotion ? nil : Motion.reward, value: saved)
            }
            .cardSurface(fill: library.highlight(ref)?.fill ?? Palette.surface)
            .animation(reduceMotion ? nil : Motion.gentle, value: library.highlight(ref))
            .zoomSource(ZoomID.reader(ref.surah, ref.ayah))
        }
        .buttonStyle(.pressable)
        .task {
            guard !reduceMotion, !landed, saved else { return }
            try? await Task.sleep(for: .seconds(0.45 + (inkDelay ?? 0)))
            guard !Task.isCancelled else { return }
            landed = true
        }
    }
}

/// Inks Arabic in only when a delay is given, so other screens keep a still card.
private struct OptionalInkReveal: ViewModifier {
    let delay: Double?

    func body(content: Content) -> some View {
        if let delay {
            content.inkReveal(delay: delay, duration: 1.0)
        } else {
            content
        }
    }
}

// MARK: - Feelings and topics

/// "How is your heart today?" A row of gentle choices.
/// The chips deal in one after another. A tapped chip dips, springs back with a
/// small overshoot and gives a light tick.
struct FeelingsRow: View {
    @Environment(ContentStore.self) private var content
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @AppStorage(PrefKey.haptics) private var hapticsOn = true

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: Space.xs) {
                ForEach(Array(content.feelings.enumerated()), id: \.element.id) { index, feeling in
                    NavigationLink(value: Route.collection(feeling.id)) {
                        Label(feeling.title, systemImage: feeling.symbol)
                            .font(.subheadline.weight(.medium))
                            .foregroundStyle(Palette.accent)
                            .padding(.horizontal, Space.m)
                            .frame(minHeight: 44)
                            .background(Capsule().fill(Palette.accentWash))
                            .zoomSource(ZoomID.collection(feeling.id))
                    }
                    .buttonStyle(ChipBounceStyle(still: reduceMotion, haptics: hapticsOn))
                    .accessibilityHint("Opens verses for this feeling")
                    .dealIn(min(index, 8), baseDelay: 0.1, step: 0.05)
                }
            }
            .padding(.horizontal, Space.page)
        }
        .padding(.horizontal, -Space.page)
    }
}

/// A chip that dips while held and springs back with a small overshoot when let go,
/// with a light tick on release.
private struct ChipBounceStyle: ButtonStyle {
    var still = false
    var haptics = true

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed && !still ? 0.93 : 1)
            .opacity(configuration.isPressed ? 0.9 : 1)
            .animation(
                still ? nil : (configuration.isPressed ? Motion.press : Animation.spring(response: 0.32, dampingFraction: 0.5)),
                value: configuration.isPressed
            )
            .sensoryFeedback(.selection, trigger: configuration.isPressed) { old, new in
                haptics && old && !new
            }
    }
}

/// A feeling or topic's verses. The verses deal in one after another, ink in right
/// to left, and settle as she scrolls.
struct CollectionView: View {
    let collectionID: String

    @Environment(ContentStore.self) private var content

    var body: some View {
        ScrollView {
            if let collection = content.collection(id: collectionID) {
                LazyVStack(spacing: Space.s) {
                    ForEach(Array(collection.refs.enumerated()), id: \.element.id) { index, ref in
                        // Cards further down ink in soon after they scroll into view.
                        VerseCard(ref: ref, inkDelay: index < 8 ? 0.3 + Double(index) * 0.08 : 0.2)
                            .dealIn(min(index, 8), baseDelay: 0.15, step: 0.07)
                            .scrollSettle()
                    }
                }
                .padding(.horizontal, Space.page)
                .readableWidth()
                .padding(.vertical, Space.s)
            }
        }
        .pageBackground()
        .navigationTitle(content.collection(id: collectionID)?.title ?? "")
        .minimizesNavigationBarOnScroll()
    }
}

// MARK: - Highlights and notes

/// Her highlights and notes. The count rolls up, the verses deal in and settle as she
/// scrolls, each highlight's colour swatch pops in, and a removed one folds away with Undo.
struct AnnotationsView: View {
    @Environment(QuranStore.self) private var quran
    @Environment(LibraryStore.self) private var library
    @Environment(AppRouter.self) private var router
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @AppStorage(PrefKey.haptics) private var hapticsOn = true

    /// What was removed, kept for a few seconds so Undo can bring it back.
    private struct Removed: Equatable {
        let ref: VerseRef
        let color: HighlightColor?
        let note: VerseNote?
    }

    @State private var removed: Removed?
    @State private var undoToken = 0

    var body: some View {
        ScrollView {
            let refs = library.annotatedVerses
            VStack(alignment: .leading, spacing: Space.l) {
                if refs.isEmpty {
                    SavedEmpty(symbol: "highlighter", title: "No highlights or notes yet",
                               action: "Open the Quran") { router.tab = .quran }
                        .transition(.opacity)
                } else {
                    header(count: refs.count)
                    LazyVStack(spacing: Space.s) {
                        ForEach(Array(refs.enumerated()), id: \.element) { index, ref in
                            annotationCard(ref, index: index)
                                .dealIn(min(index, 8), baseDelay: 0.15, step: 0.05)
                                .scrollSettle()
                                .transition(.asymmetric(insertion: .opacity, removal: .scale(scale: 0.9).combined(with: .opacity)))
                        }
                    }
                }
            }
            .padding(.horizontal, Space.page)
            .padding(.vertical, Space.s)
            .animation(reduceMotion ? nil : .spring(response: 0.45, dampingFraction: 0.85), value: refs)
        }
        .pageBackground()
        .navigationTitle("Highlights & Notes")
        .overlay(alignment: .bottom) {
            if let removed {
                UndoBar(text: "Removed \(quran.displayName(removed.ref))") {
                    withAnimation(reduceMotion ? nil : Motion.gentle) {
                        if let color = removed.color { library.setHighlight(color, for: removed.ref) }
                        if let note = removed.note { library.setNote(note.text, for: removed.ref) }
                        self.removed = nil
                    }
                }
                .padding(.horizontal, Space.page)
                .padding(.bottom, Space.m)
                .transition(.move(edge: .bottom).combined(with: .opacity))
            }
        }
        .animation(reduceMotion ? nil : .spring(response: 0.4, dampingFraction: 0.8), value: removed?.ref)
        .sensoryFeedback(.impact(flexibility: .soft, intensity: 0.6), trigger: undoToken) { _, _ in hapticsOn }
    }

    private func header(count: Int) -> some View {
        HStack(alignment: .firstTextBaseline, spacing: 6) {
            RollingNumber(value: count, delay: 0.1)
                .font(.system(size: 34, weight: .light, design: .rounded))
                .foregroundStyle(Palette.textPrimary)
            Text(count == 1 ? "verse with a highlight or note" : "verses with a highlight or note")
                .font(.subheadline)
                .foregroundStyle(Palette.textSecondary)
        }
        .focusIn(delay: 0.05)
    }

    private func annotationCard(_ ref: VerseRef, index: Int) -> some View {
        let step = Double(min(index, 8))
        return VStack(alignment: .leading, spacing: Space.xs) {
            VerseCard(ref: ref, arabicSize: 22, inkDelay: index < 8 ? 0.3 + step * 0.06 : 0.2)
            if let color = library.highlight(ref) {
                HighlightTag(color: color, delay: 0.45 + step * 0.05)
                    .padding(.horizontal, Space.s)
            }
            if let note = library.note(ref) {
                Label {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(note.text)
                            .font(AppFont.serif(.body))
                            .foregroundStyle(Palette.textPrimary)
                        Text(note.updated.formatted(date: .abbreviated, time: .omitted))
                            .font(.caption)
                            .foregroundStyle(Palette.textSecondary)
                    }
                } icon: {
                    Image(systemName: "note.text").foregroundStyle(Palette.accent)
                }
                .padding(.horizontal, Space.s)
            }
        }
        .contextMenu {
            Button(removeTitle(ref), systemImage: "trash", role: .destructive) { remove(ref) }
        }
        .accessibilityAction(named: removeTitle(ref)) { remove(ref) }
    }

    private func removeTitle(_ ref: VerseRef) -> String {
        switch (library.highlight(ref) != nil, library.note(ref) != nil) {
        case (true, true): "Remove Highlight and Note"
        case (true, false): "Remove Highlight"
        default: "Remove Note"
        }
    }

    private func remove(_ ref: VerseRef) {
        let snapshot = Removed(ref: ref, color: library.highlight(ref), note: library.note(ref))
        withAnimation(reduceMotion ? nil : .spring(response: 0.45, dampingFraction: 0.85)) {
            if snapshot.color != nil { library.setHighlight(nil, for: ref) }
            if snapshot.note != nil { library.setNote("", for: ref) }
            removed = snapshot
        }
        undoToken += 1
        let token = undoToken
        Task {
            try? await Task.sleep(for: .seconds(4))
            if token == undoToken { removed = nil }
        }
    }
}

/// A highlight's colour as a small swatch that pops in with a little overshoot.
private struct HighlightTag: View {
    let color: HighlightColor
    let delay: Double

    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var shown = false

    var body: some View {
        HStack(spacing: 6) {
            Circle()
                .fill(color.swatch)
                .frame(width: 10, height: 10)
                .scaleEffect(shown || reduceMotion ? 1 : 0.01)
            Text("\(color.name) highlight")
                .font(.caption.weight(.medium))
                .foregroundStyle(Palette.textSecondary)
        }
        .animation(reduceMotion ? nil : Motion.gentle, value: color)
        .accessibilityElement(children: .combine)
        .onAppear {
            guard !shown else { return }
            withAnimation(reduceMotion ? nil : Motion.reward.delay(delay)) { shown = true }
        }
    }
}

// MARK: - Reading plan

extension QuranStore {
    /// First and last verse that fall on a range of mushaf pages.
    func verses(onPages pages: ClosedRange<Int>) -> (first: VerseRef, last: VerseRef)? {
        var first: VerseRef?
        var last: VerseRef?
        for surah in surahs {
            for (index, page) in surah.pages.enumerated() where pages.contains(page) {
                let ref = VerseRef(surah: surah.id, ayah: index + 1)
                if first == nil { first = ref }
                last = ref
            }
        }
        guard let first, let last else { return nil }
        return (first, last)
    }
}

/// Read the Quran at her own pace. Choosing shows what each plan means,
/// a ring fills as she reads, and the days she has read fill in. Missed days are never counted against her.
struct ReadingPlanView: View {
    @Environment(QuranStore.self) private var quran
    @Environment(LibraryStore.self) private var library
    @Environment(PlanHistoryStore.self) private var history
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @AppStorage(PrefKey.haptics) private var hapticsOn = true
    @State private var confirmEnd = false
    @State private var choice: PlanChoice = .pages(30)
    @State private var customDays = 45
    @State private var markTick = 0

    private enum PlanChoice: Hashable {
        case pages(Int)
        case custom
        case shortSurahs
        case allSurahs
    }

    private struct PlanOption: Identifiable {
        let choice: PlanChoice
        let title: String
        let detail: String
        var id: PlanChoice { choice }
    }

    private let pageOptions = [
        PlanOption(choice: .pages(30), title: "30 days", detail: "One juz a day."),
        PlanOption(choice: .pages(60), title: "60 days", detail: "Half a juz a day."),
        PlanOption(choice: .pages(90), title: "90 days", detail: "A third of a juz a day."),
        PlanOption(choice: .pages(120), title: "4 months", detail: "A quarter juz a day."),
        PlanOption(choice: .pages(365), title: "One year", detail: ""),
        PlanOption(choice: .custom, title: "Custom", detail: ""),
    ]

    private let surahOptions = [
        PlanOption(choice: .shortSurahs, title: "Short surahs", detail: "Juz 30, surah 78 to 114."),
        PlanOption(choice: .allSurahs, title: "One surah at a time", detail: "All 114 surahs in order."),
    ]

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: Space.xl) {
                if let plan = library.plan {
                    activePlan(plan)
                        .transition(.opacity.combined(with: .scale(scale: 0.97)))
                } else {
                    choosePlan
                        .transition(.opacity)
                }
            }
            .padding(.horizontal, Space.page)
            .padding(.vertical, Space.s)
            .animation(reduceMotion ? nil : Motion.smooth, value: library.plan == nil)
        }
        .pageBackground()
        .navigationTitle("Reading Plan")
        .confirmationDialog("End this reading plan?", isPresented: $confirmEnd, titleVisibility: .visible) {
            Button("End Plan", role: .destructive) {
                GentleReminder.plan.turnOff()
                library.endPlan()
            }
        }
        .sensoryFeedback(.success, trigger: markTick) { _, _ in hapticsOn }
        .onAppear { history.sync(with: library.plan) }
    }

    // MARK: Choose

    private var selectedOption: PlanOption {
        (pageOptions + surahOptions).first { $0.choice == choice } ?? pageOptions[0]
    }

    private var isSurahChoice: Bool { choice == .shortSurahs || choice == .allSurahs }

    private var selectedDays: Int {
        switch choice {
        case .pages(let days): days
        case .custom: customDays
        case .shortSurahs: 114 - 78 + 1
        case .allSurahs: 114
        }
    }

    private var choosePlan: some View {
        VStack(alignment: .leading, spacing: Space.l) {
            PlanPreview(
                days: selectedDays,
                title: choice == .custom ? "\(customDays) days" : selectedOption.title,
                detail: selectedOption.detail,
                bySurah: isSurahChoice
            )
            .dealIn(0, baseDelay: 0.1)

            GroupedSection(title: "Whole Quran") {
                ForEach(Array(pageOptions.enumerated()), id: \.element.id) { index, option in
                    if index > 0 { RowDivider(inset: Space.m) }
                    choiceRow(option)
                }
                if choice == .custom {
                    RowDivider(inset: Space.m)
                    customDaysRows
                        .transition(.opacity)
                }
            }
            .dealIn(1, baseDelay: 0.1)

            GroupedSection(title: "By surah") {
                ForEach(Array(surahOptions.enumerated()), id: \.element.id) { index, option in
                    if index > 0 { RowDivider(inset: Space.m) }
                    choiceRow(option)
                }
            }
            .dealIn(2, baseDelay: 0.1)

            Button("Start Plan") {
                withAnimation(reduceMotion ? nil : Motion.smooth) { startChosenPlan() }
            }
            .buttonStyle(.primary)
            .dealIn(3, baseDelay: 0.1)

            if !history.finished.isEmpty {
                finishedPlans
                    .dealIn(4, baseDelay: 0.1)
            }
        }
        .animation(reduceMotion ? nil : Motion.gentle, value: choice)
        .sensoryFeedback(.selection, trigger: choice) { _, _ in hapticsOn }
    }

    private func choiceRow(_ option: PlanOption) -> some View {
        let isOn = choice == option.choice
        return Button {
            withAnimation(reduceMotion ? nil : .spring(response: 0.38, dampingFraction: 0.78)) { choice = option.choice }
        } label: {
            HStack(spacing: Space.m) {
                Text(option.title)
                    .font(.body.weight(isOn ? .semibold : .regular))
                    .foregroundStyle(Palette.textPrimary)
                Spacer(minLength: Space.xs)
                Image(systemName: "checkmark")
                    .font(.body.weight(.semibold))
                    .foregroundStyle(Palette.accent)
                    .scaleEffect(isOn || reduceMotion ? 1 : 0.4)
                    .opacity(isOn ? 1 : 0)
                    .accessibilityHidden(true)
            }
            .padding(.horizontal, Space.m)
            .padding(.vertical, 12)
            .frame(minHeight: 48)
            .contentShape(Rectangle())
        }
        .accessibilityLabel(option.title)
        .accessibilityAddTraits(isOn ? .isSelected : [])
    }

    /// Any number of days from a week to two years. The slider moves fast, the stepper fine tunes.
    private var customDaysRows: some View {
        VStack(spacing: 0) {
            Stepper(value: $customDays, in: 7...730) {
                Text("\(customDays) days")
                    .font(.body.monospacedDigit())
                    .foregroundStyle(Palette.textPrimary)
                    .contentTransition(reduceMotion ? .identity : .numericText(value: Double(customDays)))
                    .animation(reduceMotion ? nil : .snappy, value: customDays)
            }
            .padding(.horizontal, Space.m)
            .padding(.vertical, 6)
            .frame(minHeight: 48)

            Slider(
                value: Binding(
                    get: { Double(customDays) },
                    set: { customDays = Int($0.rounded()) }
                ),
                in: 7...730,
                step: 1
            )
            .tint(Palette.accentFill)
            .padding(.horizontal, Space.m)
            .padding(.bottom, Space.s)
            .frame(minHeight: 44)
            .accessibilityLabel("Days")
            .accessibilityValue("\(customDays)")
        }
        .sensoryFeedback(.selection, trigger: customDays) { _, _ in hapticsOn }
    }

    private func startChosenPlan() {
        switch choice {
        case .pages(let days): library.startPlan(days: days)
        case .custom: library.startPlan(days: customDays)
        case .shortSurahs: library.startSurahPlan(from: 78, to: 114)
        case .allSurahs: library.startSurahPlan(from: 1, to: 114)
        }
    }

    // MARK: Active

    @ViewBuilder
    private func activePlan(_ plan: ReadingPlan) -> some View {
        let today = plan.dayIndex()
        let fraction = plan.progress
        let todayRead = plan.finishedDays.contains(today)
        let percent = Int((fraction * 100).rounded())

        VStack(alignment: .leading, spacing: Space.l) {
            HStack(spacing: Space.l) {
                PlanRing(fraction: fraction, day: today + 1, days: plan.days, wholeQuran: !plan.isSurahPlan)
                    .frame(width: 112, height: 112)
                VStack(alignment: .leading, spacing: 4) {
                    Eyebrow(text: "Today")
                    portion(plan, day: today)
                    Text(plan.isSurahPlan ? "\(percent)% of the plan read" : "\(percent)% of the Quran read")
                        .font(.caption.weight(.medium))
                        .foregroundStyle(Palette.accent)
                        .monospacedDigit()
                        .contentTransition(.numericText(value: fraction))
                        .animation(.snappy, value: fraction)
                }
                .focusIn(delay: 0.2)
            }

            if let target = readTarget(plan, day: today) {
                NavigationLink(value: target) {
                    Text("Read Today's Portion")
                }
                .buttonStyle(.primary)
            }

            Button {
                mark(today, in: plan)
            } label: {
                HStack(spacing: Space.s) {
                    ZStack {
                        Circle().strokeBorder(Palette.accent, lineWidth: 2).opacity(todayRead ? 0 : 1)
                        Circle().fill(Palette.accentFill).scaleEffect(todayRead ? 1 : 0.01).opacity(todayRead ? 1 : 0)
                        Image(systemName: "checkmark")
                            .font(.system(size: 11, weight: .bold))
                            .foregroundStyle(Palette.onAccentFill)
                            .scaleEffect(todayRead ? 1 : 0.3)
                            .opacity(todayRead ? 1 : 0)
                            .symbolEffect(.bounce, value: reduceMotion ? false : todayRead)
                    }
                    .frame(width: 22, height: 22)
                    .overlay { PetalBurst(trigger: todayRead, active: todayRead) }
                    Text(todayRead ? "Read today" : "Mark today as read")
                        .contentTransition(.opacity)
                }
                .animation(.spring(response: 0.35, dampingFraction: 0.6), value: todayRead)
            }
            .buttonStyle(.tinted)
        }
        .cardSurface(padding: Space.xl)

        if plan.isComplete {
            PlanComplete(wholeQuran: plan.coversWholeQuran)
                .transition(.scale(scale: 0.95).combined(with: .opacity))
        }

        GentleReminderSection(.plan, title: "Daily reminder")
            .dealIn(1, baseDelay: 0.3)

        VStack(alignment: .leading, spacing: Space.s) {
            SectionHeader(title: "All days")
                .focusIn(delay: 0.3)
            LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 6), count: 7), spacing: 6) {
                ForEach(0..<plan.days, id: \.self) { day in
                    PlanDayCell(day: day, isRead: plan.finishedDays.contains(day), isToday: day == today) {
                        mark(day, in: plan)
                    }
                    // A diagonal ripple across the first weeks.
                    .dealIn(min(day / 7 + day % 7, 14), baseDelay: 0.35, step: 0.03)
                }
            }
        }

        if !history.finished.isEmpty {
            finishedPlans
                .dealIn(2, baseDelay: 0.4)
        }

        Button("End Plan", role: .destructive) { confirmEnd = true }
            .frame(maxWidth: .infinity, minHeight: 44)
    }

    /// What to read today: a surah, or the verses on the day's pages.
    @ViewBuilder
    private func portion(_ plan: ReadingPlan, day: Int) -> some View {
        if plan.isSurahPlan {
            let surah = quran.surah(plan.surah(forDay: day))
            Text(surah.name)
                .font(.system(.headline, design: .serif))
                .foregroundStyle(Palette.textPrimary)
                .fixedSize(horizontal: false, vertical: true)
            Text("Surah \(surah.id) · \(surah.verseCount) verses")
                .font(.subheadline)
                .foregroundStyle(Palette.textSecondary)
        } else {
            let pages = plan.pages(forDay: day)
            if let range = quran.verses(onPages: pages) {
                Text("\(quran.surah(range.first.surah).name) \(range.first.id) to \(quran.surah(range.last.surah).name) \(range.last.id)")
                    .font(.system(.headline, design: .serif))
                    .foregroundStyle(Palette.textPrimary)
                    .fixedSize(horizontal: false, vertical: true)
            }
            Text(pages.count == 1 ? "Page \(pages.lowerBound)" : "Pages \(pages.lowerBound) to \(pages.upperBound)")
                .font(.subheadline)
                .foregroundStyle(Palette.textSecondary)
        }
    }

    /// Opens the reader where today's portion starts.
    private func readTarget(_ plan: ReadingPlan, day: Int) -> Route? {
        if plan.isSurahPlan {
            return .reader(surah: plan.surah(forDay: day), verse: nil)
        }
        guard let range = quran.verses(onPages: plan.pages(forDay: day)) else { return nil }
        return .reader(surah: range.first.surah, verse: range.first.ayah)
    }

    /// Plans she has finished, with when each began and ended.
    private var finishedPlans: some View {
        GroupedSection(title: "Finished plans") {
            ForEach(Array(history.finished.enumerated()), id: \.element.id) { index, done in
                if index > 0 { RowDivider() }
                ListRow(
                    symbol: "checkmark.circle",
                    title: done.title,
                    subtitle: "\(done.start.formatted(date: .abbreviated, time: .omitted)) to \(done.finish.formatted(date: .abbreviated, time: .omitted))",
                    showsChevron: false
                )
            }
        }
    }

    private func mark(_ day: Int, in plan: ReadingPlan) {
        let wasRead = plan.finishedDays.contains(day)
        withAnimation(reduceMotion ? nil : .spring(response: 0.45, dampingFraction: 0.7)) {
            library.toggleFinished(day: day)
            history.sync(with: library.plan)
        }
        if !wasRead { markTick += 1 }
    }
}

/// What a pace means: pages a day, rolling as she changes it, and the day she would finish.
private struct PlanPreview: View {
    let days: Int
    let title: String
    let detail: String
    var bySurah = false

    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    private var pagesPerDay: Int { bySurah ? 1 : max(1, Int((Double(ReadingPlan.totalPages) / Double(days)).rounded())) }
    private var finish: Date { Calendar.current.date(byAdding: .day, value: days - 1, to: .now) ?? .now }

    var body: some View {
        VStack(alignment: .leading, spacing: Space.s) {
            Eyebrow(text: title)
                .contentTransition(.opacity)
            HStack(alignment: .firstTextBaseline, spacing: Space.xs) {
                Text("\(pagesPerDay)")
                    .font(.system(size: 56, weight: .light, design: .rounded))
                    .monospacedDigit()
                    .foregroundStyle(Palette.textPrimary)
                    .contentTransition(reduceMotion ? .identity : .numericText(value: Double(pagesPerDay)))
                Text(bySurah ? "surah a day" : (pagesPerDay == 1 ? "page a day" : "pages a day"))
                    .font(.headline)
                    .foregroundStyle(Palette.textSecondary)
            }
            if !detail.isEmpty {
                Text(detail)
                    .font(.subheadline)
                    .foregroundStyle(Palette.textSecondary)
                    .contentTransition(.opacity)
            }
            RowDivider(inset: 0)
                .padding(.vertical, Space.xs)
            Label {
                Text("Finish by \(finish.formatted(.dateTime.day().month(.wide).year()))")
                    .contentTransition(.opacity)
            } icon: {
                Image(systemName: "calendar")
                    .foregroundStyle(Palette.accent)
                    .symbolEffect(.bounce, value: days)
            }
            .font(.subheadline.weight(.medium))
            .foregroundStyle(Palette.textPrimary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .cardSurface(padding: Space.xl)
        .animation(reduceMotion ? nil : .snappy, value: days)
        .accessibilityElement(children: .combine)
    }
}

/// How much of the Quran she has read, drawn as a ring that fills from the top.
private struct PlanRing: View {
    let fraction: Double
    let day: Int
    let days: Int
    var wholeQuran = true

    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var drawn = false

    var body: some View {
        ZStack {
            Circle()
                .stroke(Palette.accentWash, lineWidth: 10)
            Circle()
                .trim(from: 0, to: drawn || reduceMotion ? max(fraction, 0.001) : 0.001)
                .stroke(Palette.accentFill, style: StrokeStyle(lineWidth: 10, lineCap: .round))
                .rotationEffect(.degrees(-90))
                .animation(reduceMotion ? nil : .spring(response: 0.9, dampingFraction: 0.8), value: fraction)
            VStack(spacing: 0) {
                Text("Day \(day)")
                    .font(.system(.title3, design: .serif, weight: .semibold))
                    .foregroundStyle(Palette.textPrimary)
                    .monospacedDigit()
                    .contentTransition(.numericText(value: Double(day)))
                    .animation(reduceMotion ? nil : .snappy, value: day)
                Text("of \(days)")
                    .font(.caption)
                    .foregroundStyle(Palette.textSecondary)
            }
        }
        .onAppear {
            guard !drawn else { return }
            withAnimation(.spring(response: 1.2, dampingFraction: 0.85).delay(0.25)) { drawn = true }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("Day \(day) of \(days). \(Int((fraction * 100).rounded())) percent of the \(wholeQuran ? "Quran" : "plan") read.")
    }
}

/// One day of the plan. Read days fill, today's tile breathes softly until it is read.
private struct PlanDayCell: View {
    let day: Int
    let isRead: Bool
    let isToday: Bool
    let toggle: () -> Void

    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        Button(action: toggle) {
            Text("\(day + 1)")
                .font(.caption.weight(.medium).monospacedDigit())
                .frame(maxWidth: .infinity, minHeight: 36)
                .foregroundStyle(isRead ? Palette.onAccentFill : Palette.textPrimary)
                .background {
                    ZStack {
                        RoundedRectangle(cornerRadius: 9, style: .continuous)
                            .fill(isToday ? Palette.blush : Palette.surface)
                        RoundedRectangle(cornerRadius: 9, style: .continuous)
                            .fill(Palette.accentFill)
                            .scaleEffect(isRead ? 1 : 0.2)
                            .opacity(isRead ? 1 : 0)
                    }
                }
                .overlay {
                    if isToday && !isRead && !reduceMotion {
                        RoundedRectangle(cornerRadius: 9, style: .continuous)
                            .strokeBorder(Palette.blushStrong, lineWidth: 1.5)
                            .phaseAnimator([false, true]) { ring, on in
                                ring.opacity(on ? 0.9 : 0.2)
                            } animation: { _ in .easeInOut(duration: 1.4) }
                    }
                }
                .keyframeAnimator(initialValue: 1.0, trigger: isRead) { [still = reduceMotion] view, scale in
                    view.scaleEffect(still ? 1 : scale)
                } keyframes: { _ in
                    SpringKeyframe(0.86, duration: 0.1)
                    SpringKeyframe(1.1, duration: 0.18, spring: .bouncy)
                    SpringKeyframe(1.0, duration: 0.3, spring: .smooth)
                }
                .animation(.spring(response: 0.35, dampingFraction: 0.65), value: isRead)
        }
        .buttonStyle(PressableStyle(scale: 0.97))
        .accessibilityLabel(isToday ? "Day \(day + 1), today" : "Day \(day + 1)")
        .accessibilityValue(isRead ? "Read" : "Not read")
    }
}

/// When the whole Quran is read: a lotus draws itself in, with a few gold sparks.
private struct PlanComplete: View {
    var wholeQuran = true
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var drawn = false
    @State private var fire = 0

    var body: some View {
        VStack(spacing: Space.m) {
            ZStack {
                ForEach(0..<12, id: \.self) { i in
                    let angle = Double(i) / 12 * 2 * .pi
                    Circle()
                        .fill(i.isMultiple(of: 2) ? Color(hex: 0xD3A94A) : Palette.accentFill)
                        .frame(width: 5, height: 5)
                        .keyframeAnimator(initialValue: PetalState(), trigger: fire) { spark, value in
                            spark
                                .offset(x: cos(angle) * value.distance, y: sin(angle) * value.distance)
                                .opacity(reduceMotion ? 0 : value.opacity)
                                .scaleEffect(value.scale)
                        } keyframes: { _ in
                            KeyframeTrack(\.distance) {
                                CubicKeyframe(10, duration: 0.01)
                                SpringKeyframe(64, duration: 0.7, spring: .snappy)
                            }
                            KeyframeTrack(\.opacity) {
                                LinearKeyframe(1, duration: 0.05)
                                LinearKeyframe(1, duration: 0.35)
                                LinearKeyframe(0, duration: 0.35)
                            }
                            KeyframeTrack(\.scale) {
                                CubicKeyframe(1.3, duration: 0.2)
                                CubicKeyframe(0.5, duration: 0.5)
                            }
                        }
                }
                LotusMark()
                    .trim(from: 0, to: drawn || reduceMotion ? 1 : 0)
                    .stroke(Palette.accent, style: StrokeStyle(lineWidth: 1.8, lineCap: .round, lineJoin: .round))
                    .frame(width: 64, height: 46)
            }
            .frame(height: 70)
            Text(wholeQuran ? "You have read the whole Quran. May Allah accept it from you." : "You finished this plan.")
                .font(AppFont.serif(.body, weight: .semibold))
                .foregroundStyle(Palette.accent)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .padding(Space.xl)
        .background(
            RoundedRectangle(cornerRadius: Radius.card, style: .continuous)
                .fill(LinearGradient(colors: [Color(hex: 0xC9A24A).opacity(0.16), Palette.accentWash.opacity(0.6)],
                                     startPoint: .topLeading, endPoint: .bottomTrailing))
        )
        .onAppear {
            withAnimation(.easeInOut(duration: 1.4).delay(0.2)) { drawn = true }
            Task {
                try? await Task.sleep(for: .seconds(0.9))
                fire += 1
            }
        }
        .accessibilityElement(children: .combine)
    }
}

// MARK: - Quiz

struct QuizQuestion: Identifiable {
    let id = UUID()
    let prompt: String
    let arabic: String?
    let options: [String]
    let answer: Int
    let explanation: String
}

/// Builds questions only from verified data: surah names, meanings, verse counts,
/// place of revelation, order, and the source of well-known verses.
enum QuizMaker {
    @MainActor
    static func make(quran: QuranStore, content: ContentStore, count: Int = 10) -> [QuizQuestion] {
        let famous = Array(Set(content.dailyVerses + content.feelings.flatMap(\.refs) + content.topics.flatMap(\.refs)))
        var questions: [QuizQuestion] = []
        for i in 0..<count {
            switch i % 5 {
            case 0:
                guard let ref = famous.randomElement() else { continue }
                let surah = quran.surah(ref.surah)
                let others = quran.surahs.filter { $0.id != surah.id }.shuffled().prefix(3).map(\.name)
                questions.append(build(
                    prompt: "Which surah is this verse from?",
                    arabic: quran.text(ref),
                    correct: surah.name, wrong: others,
                    explanation: "It is from \(surah.name), verse \(ref.ayah)."
                ))
            case 1:
                let surah = quran.surahs.filter { $0.verseCount >= 5 }.randomElement() ?? quran.surah(1)
                let counts = Set(quran.surahs.map(\.verseCount)).subtracting([surah.verseCount])
                let wrong = counts.sorted { abs($0 - surah.verseCount) < abs($1 - surah.verseCount) }.prefix(6).shuffled().prefix(3).map(String.init)
                questions.append(build(
                    prompt: "How many verses are in \(surah.name)?",
                    arabic: nil, correct: String(surah.verseCount), wrong: Array(wrong),
                    explanation: "\(surah.name) has \(surah.verseCount) verses."
                ))
            case 2:
                let surah = quran.surahs.randomElement() ?? quran.surah(1)
                let other = surah.revelationPlace == "Makkah" ? "Madinah" : "Makkah"
                questions.append(build(
                    prompt: "Where was \(surah.name) revealed?",
                    arabic: nil, correct: surah.revelationPlace, wrong: [other],
                    explanation: "\(surah.name) is a \(surah.revelationPlace == "Makkah" ? "Makkan" : "Madinan") surah."
                ))
            case 3:
                let surah = quran.surahs.randomElement() ?? quran.surah(1)
                let wrong = quran.surahs.filter { $0.meaning != surah.meaning }.shuffled().prefix(3).map(\.meaning)
                questions.append(build(
                    prompt: "What does the name \(surah.name) mean?",
                    arabic: nil, correct: surah.meaning, wrong: Array(wrong),
                    explanation: "\(surah.name) means \(surah.meaning)."
                ))
            default:
                let surah = quran.surahs.filter { $0.id < 114 }.randomElement() ?? quran.surah(1)
                let next = quran.surah(surah.id + 1)
                let wrong = quran.surahs.filter { $0.id != next.id && $0.id != surah.id }.shuffled().prefix(3).map(\.name)
                questions.append(build(
                    prompt: "Which surah comes right after \(surah.name)?",
                    arabic: nil, correct: next.name, wrong: Array(wrong),
                    explanation: "\(next.name) is surah \(next.id), right after \(surah.name)."
                ))
            }
        }
        return questions.shuffled()
    }

    private static func build(prompt: String, arabic: String?, correct: String, wrong: [String], explanation: String) -> QuizQuestion {
        let options = ([correct] + wrong).shuffled()
        return QuizQuestion(
            prompt: prompt, arabic: arabic, options: options,
            answer: options.firstIndex(of: correct) ?? 0, explanation: explanation
        )
    }
}

/// A calm quiz for learning. No timers, no streaks, no leaderboard.
/// Each answer is shown kindly: a right answer sweeps in emerald, a wrong one gives
/// a small shake and the right answer is shown so she learns it.
struct QuizView: View {
    @Environment(QuranStore.self) private var quran
    @Environment(ContentStore.self) private var content
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @AppStorage(PrefKey.haptics) private var hapticsOn = true

    @State private var questions: [QuizQuestion] = []
    @State private var index = 0
    @State private var chosen: Int?
    @State private var correct = 0
    /// Which questions she answered rightly, for the progress segments.
    @State private var results: [Bool] = []
    /// Starts a fresh round of entrance motion after "New Questions".
    @State private var round = 0

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: Space.xl) {
                if questions.isEmpty {
                    ProgressView().frame(maxWidth: .infinity)
                } else {
                    QuizProgress(total: questions.count, index: index, results: results)
                        .focusIn(delay: 0.05)
                    ZStack(alignment: .topLeading) {
                        if index >= questions.count {
                            QuizFinished(correct: correct, total: questions.count) { restart() }
                                .transition(slide)
                        } else {
                            questionView(questions[index])
                                .id("\(round)-\(index)")
                                .transition(slide)
                        }
                    }
                }
            }
            .padding(.horizontal, Space.page)
            .padding(.vertical, Space.l)
        }
        .pageBackground()
        .navigationTitle("Quran Quiz")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear { if questions.isEmpty { restart() } }
        .sensoryFeedback(trigger: chosen) { _, new in
            guard hapticsOn, let new, index < questions.count else { return nil }
            return new == questions[index].answer ? .success : .impact(flexibility: .soft, intensity: 0.5)
        }
    }

    private var slide: AnyTransition {
        reduceMotion ? .opacity : .asymmetric(
            insertion: .push(from: .trailing).combined(with: .opacity),
            removal: .push(from: .trailing).combined(with: .opacity)
        )
    }

    private func questionView(_ q: QuizQuestion) -> some View {
        VStack(alignment: .leading, spacing: Space.l) {
            Text(q.prompt)
                .font(.system(.title2, design: .serif, weight: .semibold))
                .foregroundStyle(Palette.textPrimary)
                .fixedSize(horizontal: false, vertical: true)
                .focusIn(delay: 0.1)
            if let arabic = q.arabic {
                ArabicText(text: arabic, size: 24)
                    .inkReveal(delay: 0.25, duration: 1.0)
                    .cardSurface()
                    .focusIn(delay: 0.15)
            }
            VStack(spacing: Space.xs) {
                ForEach(q.options.indices, id: \.self) { i in
                    QuizOption(
                        text: q.options[i],
                        state: optionState(i, answer: q.answer),
                        revealDelay: chosen != nil && chosen != q.answer && i == q.answer ? 0.25 : 0
                    ) {
                        guard chosen == nil else { return }
                        withAnimation(reduceMotion ? nil : .spring(response: 0.4, dampingFraction: 0.8)) {
                            chosen = i
                        }
                        if i == q.answer { correct += 1 }
                        results.append(i == q.answer)
                    }
                    .dealIn(i, baseDelay: 0.25, step: 0.06)
                }
            }
            if chosen != nil {
                VStack(alignment: .leading, spacing: Space.m) {
                    Label {
                        Text(q.explanation)
                            .foregroundStyle(Palette.textPrimary)
                    } icon: {
                        Image(systemName: "lightbulb")
                            .foregroundStyle(Color(hex: 0xC9A24A))
                            .symbolEffect(.bounce, value: chosen != nil)
                    }
                    .font(.subheadline)
                    .padding(Space.m)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(RoundedRectangle(cornerRadius: Radius.tile, style: .continuous).fill(Palette.surface))

                    Button(index + 1 == questions.count ? "See How You Did" : "Next Question") {
                        withAnimation(reduceMotion ? nil : .spring(response: 0.5, dampingFraction: 0.86)) {
                            index += 1
                            chosen = nil
                        }
                    }
                    .buttonStyle(.primary)
                }
                .transition(.move(edge: .bottom).combined(with: .opacity))
            }
        }
    }

    private func optionState(_ i: Int, answer: Int) -> QuizOption.State {
        guard let chosen else { return .open }
        if i == answer { return chosen == answer ? .right : .revealed }
        if i == chosen { return .wrong }
        return .dimmed
    }

    private func restart() {
        withAnimation(reduceMotion ? nil : .spring(response: 0.5, dampingFraction: 0.86)) {
            questions = QuizMaker.make(quran: quran, content: content)
            index = 0
            chosen = nil
            correct = 0
            results = []
            round += 1
        }
    }
}

/// Ten small segments. The current one stretches wide; answered ones fill.
private struct QuizProgress: View {
    let total: Int
    let index: Int
    let results: [Bool]

    var body: some View {
        VStack(alignment: .leading, spacing: Space.s) {
            Text(index < total ? "Question \(index + 1) of \(total)" : "All done")
                .font(.subheadline.weight(.medium))
                .foregroundStyle(Palette.accent)
                .monospacedDigit()
                .contentTransition(.numericText(value: Double(index)))
                .animation(.snappy, value: index)
            HStack(spacing: 5) {
                ForEach(0..<total, id: \.self) { i in
                    Capsule()
                        .fill(color(i))
                        .frame(width: i == index ? 28 : nil, height: 6)
                        .frame(maxWidth: i == index ? 28 : .infinity)
                }
            }
            .animation(.spring(response: 0.45, dampingFraction: 0.75), value: index)
            .animation(.smooth, value: results.count)
        }
        .accessibilityElement(children: .combine)
    }

    private func color(_ i: Int) -> Color {
        if i < results.count { return results[i] ? Palette.accentFill : Palette.blushStrong.opacity(0.55) }
        if i == index { return Palette.accent.opacity(0.45) }
        return Palette.border
    }
}

/// One answer. It sweeps in emerald when right, shakes softly and turns blush when wrong.
private struct QuizOption: View {
    enum State { case open, right, revealed, wrong, dimmed }

    let text: String
    let state: State
    var revealDelay: Double = 0
    let choose: () -> Void

    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @SwiftUI.State private var swept = false

    var body: some View {
        Button(action: choose) {
            HStack {
                Text(text)
                    .font(.headline)
                    .foregroundStyle(Palette.textPrimary)
                    .multilineTextAlignment(.leading)
                Spacer()
                ZStack {
                    if state == .right || state == .revealed {
                        Image(systemName: "checkmark.circle.fill")
                            .foregroundStyle(Palette.accent)
                            .transition(.scale.combined(with: .opacity))
                            .overlay { PetalBurst(trigger: state == .right, active: state == .right) }
                    } else if state == .wrong {
                        Image(systemName: "xmark.circle")
                            .foregroundStyle(Palette.blushStrong)
                            .transition(.scale.combined(with: .opacity))
                    }
                }
                .font(.title3)
            }
            .padding(Space.m)
            .frame(maxWidth: .infinity, minHeight: 56, alignment: .leading)
            .background {
                ZStack {
                    RoundedRectangle(cornerRadius: Radius.tile, style: .continuous)
                        .fill(Palette.surface)
                    // The answer colour sweeps in from the leading edge.
                    RoundedRectangle(cornerRadius: Radius.tile, style: .continuous)
                        .fill(sweepColor)
                        .scaleEffect(x: swept ? 1 : 0, anchor: .leading)
                }
                .clipShape(RoundedRectangle(cornerRadius: Radius.tile, style: .continuous))
            }
            .overlay(
                RoundedRectangle(cornerRadius: Radius.tile, style: .continuous)
                    .strokeBorder(state == .right || state == .revealed ? Palette.accent.opacity(0.5) : Palette.border)
            )
            .opacity(state == .dimmed ? 0.5 : 1)
            .keyframeAnimator(initialValue: 0.0, trigger: state == .wrong) { [still = reduceMotion] view, x in
                view.offset(x: still ? 0 : x)
            } keyframes: { _ in
                KeyframeTrack {
                    LinearKeyframe(-7, duration: 0.06)
                    LinearKeyframe(6, duration: 0.08)
                    LinearKeyframe(-4, duration: 0.08)
                    LinearKeyframe(2, duration: 0.07)
                    LinearKeyframe(0, duration: 0.07)
                }
            }
            .keyframeAnimator(initialValue: 1.0, trigger: state == .right) { [still = reduceMotion] view, scale in
                view.scaleEffect(still ? 1 : scale)
            } keyframes: { _ in
                SpringKeyframe(1.03, duration: 0.15, spring: .bouncy)
                SpringKeyframe(1.0, duration: 0.3, spring: .smooth)
            }
        }
        .buttonStyle(.pressable)
        .disabled(state != .open)
        .onChange(of: state) { _, new in
            let sweep = new == .right || new == .revealed || new == .wrong
            guard sweep else { swept = false; return }
            withAnimation(reduceMotion ? nil : .easeOut(duration: 0.45).delay(revealDelay)) { swept = true }
        }
        .accessibilityValue(accessibilityState)
    }

    private var sweepColor: Color {
        switch state {
        case .right, .revealed: Palette.accentWash
        case .wrong: Palette.blush
        default: .clear
        }
    }

    private var accessibilityState: String {
        switch state {
        case .right: "Correct"
        case .revealed: "The right answer"
        case .wrong: "Not quite"
        default: ""
        }
    }
}

/// The end of a round: a ring draws to her score, a lotus draws itself, a few sparks.
private struct QuizFinished: View {
    let correct: Int
    let total: Int
    let again: () -> Void

    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var drawn = false
    @State private var fire = 0

    private var fraction: Double { total == 0 ? 0 : Double(correct) / Double(total) }

    var body: some View {
        VStack(spacing: Space.l) {
            ZStack {
                ForEach(0..<12, id: \.self) { i in
                    let angle = Double(i) / 12 * 2 * .pi
                    Circle()
                        .fill(i.isMultiple(of: 2) ? Color(hex: 0xD3A94A) : Palette.accentFill)
                        .frame(width: 5, height: 5)
                        .keyframeAnimator(initialValue: PetalState(), trigger: fire) { spark, value in
                            spark
                                .offset(x: cos(angle) * value.distance, y: sin(angle) * value.distance)
                                .opacity(reduceMotion ? 0 : value.opacity)
                                .scaleEffect(value.scale)
                        } keyframes: { _ in
                            KeyframeTrack(\.distance) {
                                CubicKeyframe(70, duration: 0.01)
                                SpringKeyframe(120, duration: 0.7, spring: .snappy)
                            }
                            KeyframeTrack(\.opacity) {
                                LinearKeyframe(1, duration: 0.05)
                                LinearKeyframe(1, duration: 0.35)
                                LinearKeyframe(0, duration: 0.35)
                            }
                            KeyframeTrack(\.scale) {
                                CubicKeyframe(1.3, duration: 0.2)
                                CubicKeyframe(0.5, duration: 0.5)
                            }
                        }
                }
                Circle()
                    .stroke(Palette.accentWash, lineWidth: 12)
                Circle()
                    .trim(from: 0, to: drawn || reduceMotion ? max(fraction, 0.001) : 0.001)
                    .stroke(Palette.accentFill, style: StrokeStyle(lineWidth: 12, lineCap: .round))
                    .rotationEffect(.degrees(-90))
                VStack(spacing: 2) {
                    HStack(alignment: .firstTextBaseline, spacing: 2) {
                        RollingNumber(value: correct, delay: 0.3)
                            .font(.system(size: 48, weight: .light, design: .rounded))
                        Text("/\(total)")
                            .font(.title3)
                            .foregroundStyle(Palette.textSecondary)
                    }
                    Text("answered")
                        .font(.caption)
                        .foregroundStyle(Palette.textSecondary)
                }
            }
            .frame(width: 170, height: 170)
            .padding(.top, Space.l)
            .accessibilityElement(children: .ignore)
            .accessibilityLabel("You answered \(correct) of \(total)")

            LotusMark()
                .trim(from: 0, to: drawn || reduceMotion ? 1 : 0)
                .stroke(Palette.accent, style: StrokeStyle(lineWidth: 1.6, lineCap: .round, lineJoin: .round))
                .frame(width: 44, height: 32)
                .accessibilityHidden(true)

            Text(headline)
                .font(.system(.title2, design: .serif, weight: .semibold))
                .foregroundStyle(Palette.textPrimary)
                .multilineTextAlignment(.center)
                .focusIn(delay: 0.6)

            Button("New Questions", action: again)
                .buttonStyle(.primary)
                .dealIn(0, baseDelay: 0.8)
        }
        .frame(maxWidth: .infinity)
        .onAppear {
            withAnimation(.spring(response: 1.2, dampingFraction: 0.85).delay(0.2)) { drawn = true }
            Task {
                try? await Task.sleep(for: .seconds(1.0))
                fire += 1
            }
        }
    }

    private var headline: String {
        switch fraction {
        case 0.8...: "Masha'Allah"
        case 0.5..<0.8: "Well done"
        default: "Good start"
        }
    }
}

// MARK: - Explore

/// Topics, the reading plan and the quiz, in one quiet list.
/// Rows deal in one after another and each icon lands with a small bounce.
struct ExploreView: View {
    @Environment(ContentStore.self) private var content
    @Environment(LibraryStore.self) private var library
    @Environment(ChallengeStore.self) private var challenges

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: Space.xl) {
                GroupedSection(title: "Topics") {
                    ForEach(Array(content.topics.enumerated()), id: \.element.id) { index, topic in
                        if index > 0 { RowDivider() }
                        NavigationLink(value: Route.collection(topic.id)) {
                            ExploreRow(
                                symbol: topic.symbol,
                                title: topic.title,
                                subtitle: "\(topic.verses.count) verses",
                                landDelay: 0.3 + Double(min(index, 8)) * 0.05
                            )
                            .zoomSource(ZoomID.collection(topic.id))
                        }
                        .dealIn(min(index, 8), baseDelay: 0.1, step: 0.05)
                    }
                }
                .focusIn(delay: 0.05)
                GroupedSection(title: "Practice") {
                    NavigationLink(value: Route.readingPlan) {
                        ExploreRow(
                            symbol: "calendar",
                            title: "Read the whole Quran",
                            subtitle: library.plan.map { "Day \($0.dayIndex() + 1) of \($0.days)" },
                            landDelay: 0.55
                        )
                        .zoomSource(ZoomID.readingPlan)
                    }
                    .dealIn(0, baseDelay: 0.35, step: 0.06)
                    RowDivider()
                    NavigationLink(value: Route.quiz) {
                        ExploreRow(symbol: "questionmark.circle", title: "Quran quiz", landDelay: 0.61)
                            .zoomSource(ZoomID.quiz)
                    }
                    .dealIn(1, baseDelay: 0.35, step: 0.06)
                    RowDivider()
                    NavigationLink(value: Route.challenges) {
                        ExploreRow(
                            symbol: "target",
                            title: "Challenges",
                            subtitle: challenges.activeCount > 0 ? "\(challenges.activeCount) in progress" : nil,
                            landDelay: 0.67
                        )
                        .zoomSource("challenges")
                    }
                    .dealIn(2, baseDelay: 0.35, step: 0.06)
                }
                .focusIn(delay: 0.3)
                // Shown only once reviewed articles are bundled.
                if !GuidanceLibrary.isEmpty {
                    GroupedSection(title: "Guidance") {
                        ForEach(Array(GuidanceLibrary.sections.enumerated()), id: \.element) { index, section in
                            if index > 0 { RowDivider() }
                            NavigationLink(value: Route.guidanceSection(section)) {
                                ExploreRow(
                                    symbol: "book.pages",
                                    title: section,
                                    landDelay: 0.75 + Double(min(index, 8)) * 0.05
                                )
                                .zoomSource("guidance-\(section)")
                            }
                            .dealIn(min(index, 8), baseDelay: 0.45, step: 0.05)
                        }
                    }
                    .focusIn(delay: 0.4)
                }
            }
            .padding(.horizontal, Space.page)
            .padding(.vertical, Space.s)
        }
        .pageBackground()
        .navigationTitle("Explore")
    }
}

/// A list row like ListRow, whose icon lands with one small bounce as the row arrives.
/// Only the icon bounces, never the chevron.
private struct ExploreRow: View {
    let symbol: String
    let title: String
    var subtitle: String?
    var landDelay: Double = 0.3

    var body: some View {
        HStack(spacing: Space.m) {
            Image(systemName: symbol)
                .font(.system(size: 17, weight: .regular))
                .foregroundStyle(Palette.accent)
                .frame(width: 26)
                .modifier(ExploreIconLand(delay: landDelay))
                .accessibilityHidden(true)
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.body)
                    .foregroundStyle(Palette.textPrimary)
                if let subtitle {
                    Text(subtitle)
                        .font(.subheadline)
                        .foregroundStyle(Palette.textSecondary)
                        .lineLimit(2)
                        .contentTransition(.opacity)
                }
            }
            .multilineTextAlignment(.leading)
            Spacer(minLength: Space.xs)
            Image(systemName: "chevron.right")
                .font(.footnote.weight(.semibold))
                .foregroundStyle(Palette.textSecondary.opacity(0.5))
                .accessibilityHidden(true)
        }
        .padding(.horizontal, Space.m)
        .padding(.vertical, 12)
        .frame(minHeight: 48)
        .contentShape(Rectangle())
        .accessibilityElement(children: .combine)
    }
}

/// An icon gives one small bounce after a delay, as its row or tile lands.
/// Still when Reduce Motion is on. Shared with the feelings screen.
struct ExploreIconLand: ViewModifier {
    let delay: Double
    @State private var bounce = false
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    func body(content: Content) -> some View {
        content
            .symbolEffect(.bounce, value: bounce)
            .task {
                guard !reduceMotion, !bounce else { return }
                try? await Task.sleep(for: .seconds(delay))
                guard !Task.isCancelled else { return }
                bounce = true
            }
    }
}
