import SwiftUI

struct TodayView: View {
    var body: some View {
        TabStack(tab: .today) {
            TodayContent()
        }
    }
}

/// One quiet hero (the sky, with her five prayers), a verse, where she left off,
/// and one calm list for everything else.
private struct TodayContent: View {
    @Environment(ContentStore.self) private var content
    @Environment(\.scenePhase) private var scenePhase
    @State private var today = Date.now
    @State private var journalStart: JournalStart?

    var body: some View {
        ScrollView {
            VStack(spacing: 0) {
                PrayerSkyHeader()
                    // Stretches when pulled down, drifts at half speed when scrolled.
                    .visualEffect { view, proxy in
                        let minY = proxy.frame(in: .scrollView).minY
                        return view
                            .scaleEffect(minY > 0 ? 1 + minY / 430 : 1, anchor: .bottom)
                            .offset(y: minY > 0 ? -minY : -minY * 0.5)
                    }

                VStack(alignment: .leading, spacing: Space.xxl) {
                    // Dealt in after the sky wakes up, then each settles as it scrolls into view.
                    // On a new day the old verse softens away and the new one comes into focus.
                    if Hijri.isRamadan(today) {
                        RamadanTodayCard(today: today)
                            .dealIn(0, baseDelay: 0.3, step: 0.08)
                            .scrollSettle()
                    }
                    ZStack {
                        DailyVerseCard(ref: content.verseOfTheDay(today))
                            .id(today.formatted(date: .numeric, time: .omitted))
                            .transition(.opacity.combined(with: .blurred))
                    }
                    .animation(.smooth(duration: 0.6), value: today)
                    .dealIn(0, baseDelay: 0.35, step: 0.08)
                    .scrollSettle()
                    ContinueReadingSection()
                        .dealIn(1, baseDelay: 0.35, step: 0.08)
                        .scrollSettle()
                    moreForToday
                        .dealIn(2, baseDelay: 0.35, step: 0.08)
                        .scrollSettle()
                }
                .padding(.horizontal, Space.page)
                .padding(.top, Space.xl)
                .padding(.bottom, Space.xxl)
                .background(
                    UnevenRoundedRectangle(topLeadingRadius: 26, topTrailingRadius: 26, style: .continuous)
                        .fill(Palette.background)
                        .shadow(color: .black.opacity(0.08), radius: 16, y: -4)
                )
                .padding(.top, -26)
            }
        }
        .ignoresSafeArea(edges: .top)
        .background(Palette.background.ignoresSafeArea())
        .toolbar(.hidden, for: .navigationBar)
        .fullScreenCover(item: $journalStart) { JournalStartView(start: $0) }
        .onChange(of: scenePhase) { _, phase in
            // Refresh the daily content when she returns on a new day.
            if phase == .active, !Calendar.current.isDate(today, inSameDayAs: .now) {
                today = .now
            }
        }
    }

    private var moreForToday: some View {
        let dua = content.duaOfTheDay(today)
        return GroupedSection(title: "More for today") {
            NavigationLink(value: Route.dua(dua.id)) {
                ListRow(symbol: "hands.and.sparkles", title: "Today's dua", subtitle: dua.title, landDelay: 0.75)
                    .zoomSource(ZoomID.dua(dua.id))
            }
            RowDivider()
            NavigationLink(value: Route.dhikr) {
                ListRow(symbol: "circle.dotted", title: "A moment of dhikr", landDelay: 0.82)
            }
            RowDivider()
            NavigationLink(value: Route.feelings) {
                ListRow(symbol: "heart", title: "How is your heart?", landDelay: 0.89)
            }
            RowDivider()
            Button {
                journalStart = .prompt(JournalContent.question(for: today), isDua: false)
            } label: {
                ListRow(symbol: "square.and.pencil", title: "Today's question", subtitle: JournalContent.question(for: today), landDelay: 0.96)
            }
        }
    }
}

// MARK: - Verse of the day

/// The verse of the day. While it is recited, a soft emerald light breathes around the card.
private struct DailyVerseCard: View {
    let ref: VerseRef

    @Environment(QuranStore.self) private var quran
    @Environment(LibraryStore.self) private var library
    @Environment(AudioPlayer.self) private var player
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    private var reciting: Bool { player.isCurrent(ref) && player.isPlaying }

    var body: some View {
        VStack(alignment: .leading, spacing: Space.l) {
            Text("Verse of the day")
                .font(.subheadline.weight(.medium))
                .foregroundStyle(Palette.accent)

            NavigationLink(value: Route.reader(surah: ref.surah, verse: ref.ayah)) {
                VStack(alignment: .leading, spacing: Space.l) {
                    ArabicText(text: "\(quran.text(ref)) ﴿\(ref.ayah.arabicIndic)﴾", size: 27)
                        .modifier(InkReveal(delay: 0.5))
                    TranslationBody(ref: ref)
                        .focusIn(delay: 1.3)
                }
                .zoomSource(ZoomID.reader(ref.surah, ref.ayah))
            }
            .buttonStyle(.pressable)
            .accessibilityHint("Opens this verse in the reader")

            HStack(spacing: Space.s) {
                Text("\(quran.surah(ref.surah).name) · \(ref.surah):\(ref.ayah)")
                    .font(.footnote)
                    .foregroundStyle(Palette.textSecondary)
                Spacer()
                CircleIconButton(
                    systemImage: player.isCurrent(ref) && player.isPlaying ? "pause.fill" : "play.fill",
                    label: player.isCurrent(ref) && player.isPlaying ? "Pause" : "Listen",
                    isActive: reciting
                ) {
                    if player.isCurrent(ref) {
                        player.togglePlayPause()
                    } else {
                        player.play(ref, through: ref.ayah)
                    }
                }
                .symbolEffect(.bounce, value: reduceMotion ? false : reciting)
                SaveButton(isSaved: library.isBookmarked(ref)) {
                    library.toggleBookmark(ref)
                }
            }
        }
        .cardSurface(padding: Space.xl)
        .overlay {
            if reciting && !reduceMotion {
                RoundedRectangle(cornerRadius: Radius.card, style: .continuous)
                    .strokeBorder(Palette.accent.opacity(0.5), lineWidth: 1.5)
                    .phaseAnimator([false, true]) { ring, on in
                        ring.opacity(on ? 0.9 : 0.25)
                    } animation: { _ in .easeInOut(duration: 1.4) }
                    .allowsHitTesting(false)
                    .transition(.opacity)
            }
        }
        .animation(.smooth(duration: 0.4), value: reciting)
    }
}

/// Translation for a verse, or a calm note while it downloads.
struct TranslationBody: View {
    let ref: VerseRef
    var size: CGFloat?

    @Environment(TranslationStore.self) private var translations

    var body: some View {
        if let text = translations.text(ref) {
            TranslationText(text: text, size: size, rightToLeft: translations.selected.isRightToLeft)
        } else {
            switch translations.selectedState {
            case .downloading:
                HStack(spacing: Space.xs) {
                    ProgressView()
                    Text("Preparing the translation")
                }
                .font(.footnote)
                .foregroundStyle(Palette.textSecondary)
            case .failed, .notDownloaded:
                Button {
                    Task { await translations.prepareSelected() }
                } label: {
                    Label("Download translation", systemImage: "arrow.down.circle")
                        .font(.footnote.weight(.medium))
                }
                .buttonStyle(.borderless)
            case .ready:
                EmptyView()
            }
        }
    }
}

// MARK: - Continue reading

private struct ContinueReadingSection: View {
    @Environment(QuranStore.self) private var quran
    @Environment(LibraryStore.self) private var library

    var body: some View {
        let ref = library.lastRead ?? VerseRef(surah: 1, ayah: 1)
        let surah = quran.surah(ref.surah)

        VStack(alignment: .leading, spacing: Space.l) {
            HStack(alignment: .firstTextBaseline) {
                VStack(alignment: .leading, spacing: 4) {
                    Text(library.lastRead == nil ? "Begin reading" : "Continue reading")
                        .font(.subheadline)
                        .foregroundStyle(Palette.textSecondary)
                    Text(surah.name)
                        .font(.title3.weight(.semibold))
                        .foregroundStyle(Palette.textPrimary)
                    Text(library.lastRead == nil ? "The Opening" : "Verse \(ref.ayah) of \(surah.verseCount)")
                        .font(.subheadline)
                        .monospacedDigit()
                        .foregroundStyle(Palette.textSecondary)
                        .contentTransition(.numericText(value: Double(ref.ayah)))
                        .animation(.snappy, value: ref.ayah)
                }
                Spacer()
                Text(surah.arabicName)
                    .font(AppFont.arabic(24))
                    .foregroundStyle(Palette.accent)
                    .inkReveal(delay: 0.6, duration: 0.8)
                    .accessibilityHidden(true)
            }

            if library.lastRead != nil {
                FillBar(value: Double(ref.ayah) / Double(max(surah.verseCount, 1)))
                    .accessibilityElement()
                    .accessibilityLabel("Position in surah")
                    .accessibilityValue("Verse \(ref.ayah) of \(surah.verseCount)")
            }

            NavigationLink(value: Route.reader(surah: ref.surah, verse: ref.ayah)) {
                Text(library.lastRead == nil ? "Start Reading" : "Continue")
            }
            .buttonStyle(.primary)
            .zoomSource(ZoomID.reader(ref.surah, ref.ayah))
        }
        .cardSurface(padding: Space.xl)
    }
}

// MARK: - Feelings

/// "How is your heart?" as soft tiles. The words come into focus, the tiles deal in
/// and each icon lands with a small bounce. When she picks one, a gentle blush wash
/// spreads across it with a light tick, and its verses open from that tile.
/// The wash fades away when she comes back.
struct FeelingsView: View {
    @Environment(ContentStore.self) private var content
    @Environment(AppRouter.self) private var router
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @AppStorage(PrefKey.haptics) private var hapticsOn = true

    @State private var picked: String?
    @State private var pickTick = 0

    private let columns = [GridItem(.flexible(), spacing: Space.s), GridItem(.flexible(), spacing: Space.s)]

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: Space.l) {
                LazyVGrid(columns: columns, spacing: Space.s) {
                    ForEach(Array(content.feelings.enumerated()), id: \.element.id) { index, feeling in
                        Button {
                            open(feeling)
                        } label: {
                            FeelingTile(
                                feeling: feeling,
                                picked: picked == feeling.id,
                                landDelay: 0.3 + Double(min(index, 8)) * 0.05
                            )
                            .zoomSource(ZoomID.collection(feeling.id))
                        }
                        .buttonStyle(.pressable)
                        .accessibilityHint("Opens verses for this feeling")
                        .dealIn(min(index, 8), baseDelay: 0.12, step: 0.05)
                        .scrollSettle()
                    }
                }
            }
            .padding(.horizontal, Space.page)
            .padding(.vertical, Space.s)
        }
        .pageBackground()
        .navigationTitle("How is your heart?")
        .onAppear {
            // Back from the verses: let the wash fade.
            guard picked != nil else { return }
            withAnimation(reduceMotion ? nil : Motion.smooth.delay(0.35)) { picked = nil }
        }
        .sensoryFeedback(.selection, trigger: pickTick) { _, _ in hapticsOn }
    }

    /// Same as tapping a link here: pushes the feeling's verses onto this tab's stack.
    private func open(_ feeling: VerseCollection) {
        withAnimation(reduceMotion ? nil : .easeOut(duration: 0.4)) { picked = feeling.id }
        pickTick += 1
        router.open(.collection(feeling.id), in: router.tab)
    }
}

/// One feeling as a soft tile. Its icon lands with a bounce, and when picked a blush
/// wash spreads from the top corner while the icon bounces once more.
private struct FeelingTile: View {
    let feeling: VerseCollection
    let picked: Bool
    let landDelay: Double

    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        let shape = RoundedRectangle(cornerRadius: Radius.tile, style: .continuous)
        VStack(alignment: .leading, spacing: Space.s) {
            Image(systemName: feeling.symbol)
                .font(.system(size: 19, weight: .medium))
                .foregroundStyle(picked ? Palette.blushStrong : Palette.accent)
                .frame(width: 40, height: 40)
                .background(Circle().fill(picked ? Palette.surface.opacity(0.6) : Palette.accentWash))
                .symbolEffect(.bounce, value: reduceMotion ? false : picked)
                .modifier(ExploreIconLand(delay: landDelay))
                .accessibilityHidden(true)
            Spacer(minLength: 0)
            Text(feeling.title)
                .font(.headline)
                .foregroundStyle(Palette.textPrimary)
                .multilineTextAlignment(.leading)
                .lineLimit(2)
                .minimumScaleFactor(0.85)
        }
        .padding(Space.m)
        .frame(maxWidth: .infinity, minHeight: 112, alignment: .topLeading)
        .background {
            ZStack {
                shape.fill(Palette.surface)
                // The wash grows from the icon's corner when she picks this feeling.
                shape
                    .fill(Palette.blush)
                    .scaleEffect(picked || reduceMotion ? 1 : 0.2, anchor: .topLeading)
                    .opacity(picked ? 1 : 0)
            }
            .clipShape(shape)
        }
        .contentShape(shape)
        .accessibilityElement(children: .combine)
        .accessibilityLabel(feeling.title)
    }
}

// MARK: - Motion pieces

/// A slim progress bar that fills to its value when it appears.
private struct FillBar: View {
    let value: Double
    @State private var shown = false
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        GeometryReader { geo in
            ZStack(alignment: .leading) {
                Capsule().fill(Palette.accentWash)
                Capsule()
                    .fill(LinearGradient(colors: [Palette.accentFill, Palette.accent], startPoint: .leading, endPoint: .trailing))
                    .frame(width: geo.size.width * CGFloat(shown ? min(max(value, 0.02), 1) : 0))
            }
        }
        .frame(height: 6)
        .onAppear {
            guard !shown else { return }
            withAnimation(reduceMotion ? nil : .spring(response: 0.9, dampingFraction: 0.85).delay(0.7)) { shown = true }
        }
    }
}
