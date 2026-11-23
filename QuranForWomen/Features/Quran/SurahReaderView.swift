import SwiftUI

/// The reading surface. Motion here only ever serves the reading: it shows where she is,
/// follows the recitation, and marks the end. Nothing moves while she is simply reading.
struct SurahReaderView: View {
    private let surahID: Int
    let startVerse: Int?

    init(surahID: Int, startVerse: Int?) {
        // Never trust an out-of-range number from a link.
        self.surahID = min(max(surahID, 1), 114)
        self.startVerse = startVerse
    }

    @Environment(QuranStore.self) private var quran
    @Environment(LibraryStore.self) private var library
    @Environment(AudioPlayer.self) private var player
    @Environment(TranslationStore.self) private var translations
    @Environment(TransliterationStore.self) private var transliteration
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    @AppStorage(PrefKey.haptics) private var hapticsOn = true
    @AppStorage(PrefKey.arabicSize) private var arabicSize = ReaderDefaults.arabicSize
    @AppStorage(PrefKey.translationSize) private var translationSize = ReaderDefaults.translationSize
    @AppStorage(PrefKey.showTranslation) private var showTranslation = true

    @State private var visible: Set<Int> = []
    @State private var selected: VerseRef?
    @State private var showSettings = false
    @State private var isReady = false
    @State private var recordedVisit = false
    @Namespace private var playingGlide

    private var surah: Surah { quran.surah(surahID) }

    /// How far through the surah she is, from the first verse on screen.
    private var position: Double {
        guard let first = visible.min(), surah.verseCount > 1 else { return visible.isEmpty ? 0 : 1 }
        return Double(first - 1) / Double(surah.verseCount - 1)
    }

    var body: some View {
        ScrollViewReader { proxy in
            ScrollView {
                LazyVStack(alignment: .leading, spacing: 0) {
                    SurahHeader(surah: surah)
                        .padding(.top, Space.l)
                        .padding(.bottom, Space.xl)

                    if surah.showsBasmala {
                        ArabicText(text: quran.basmala, size: arabicSize * 0.92, placement: .center, color: BookInk.ink)
                            .inkReveal(delay: 0.55, duration: 1.0)
                            .background { BloomGlow(delay: 0.6) }
                            .padding(.bottom, Space.xl)
                            .accessibilityLabel("Bismillah ir-Rahman ir-Rahim")
                    }

                    ForEach(1...surah.verseCount, id: \.self) { ayah in
                        let ref = VerseRef(surah: surahID, ayah: ayah)
                        VerseRow(
                            ref: ref,
                            text: surah.ayahs[ayah - 1],
                            hasSajdah: surah.sajdas.contains(ayah),
                            isPlaying: player.isCurrent(ref),
                            isSelected: selected == ref,
                            isBookmarked: library.isBookmarked(ref),
                            highlight: library.highlight(ref),
                            note: library.note(ref)?.text,
                            arabicSize: arabicSize,
                            translationSize: translationSize,
                            showTranslation: showTranslation,
                            showTransliteration: transliteration.show,
                            glide: playingGlide
                        )
                        .id(ayah)
                        .onTapGesture { selected = ref }
                        .onAppear { visible.insert(ayah) }
                        .onDisappear { visible.remove(ayah) }
                        // Each verse rises gently into place as it enters the screen.
                        .scrollTransition(.animated(.smooth(duration: 0.45))) { [lift = reduceMotion ? CGFloat(0) : 18] view, phase in
                            view
                                .opacity(phase == .bottomTrailing ? 0.35 : 1)
                                .offset(y: phase == .bottomTrailing ? lift : 0)
                        }
                    }

                    NextSurahFooter(current: surah, reachedEnd: visible.contains(surah.verseCount))
                        .padding(.top, Space.xxl)
                }
                .padding(.horizontal, Space.page)
                .readableWidth()
                .padding(.bottom, Space.xxxl)
            }
            .task {
                if !recordedVisit {
                    recordedVisit = true
                    let opened = min(max(startVerse ?? 1, 1), surah.verseCount)
                    library.recordVisit(VerseRef(surah: surahID, ayah: opened))
                }
                if let startVerse, startVerse > 1 {
                    try? await Task.sleep(for: .milliseconds(60))
                    proxy.scrollTo(startVerse, anchor: .top)
                }
                // Only start saving the reading position once the first scroll settled.
                try? await Task.sleep(for: .milliseconds(400))
                isReady = true
            }
            .onChange(of: player.current) { _, current in
                // Follow the recitation, verse by verse.
                guard let current, current.surah == surahID else { return }
                withAnimation(reduceMotion ? nil : .smooth(duration: 0.6)) {
                    proxy.scrollTo(current.ayah, anchor: .center)
                }
            }
        }
        .overlay(alignment: .top) {
            ReadingLine(progress: position)
        }
        .safeAreaInset(edge: .bottom, spacing: 0) {
            // The tab bar, and its mini player, hide while reading. Keep the controls here.
            if MiniPlayerBar.usesSystemAccessory { MiniPlayerBar() }
        }
        // The reader is set like a book: the lit leaf of PrayerKey's Bible.
        .bookPage()
        .task(id: transliteration.show) {
            // Load the transliteration once if she reads with it.
            if transliteration.show { await transliteration.prepare() }
        }
        .onChange(of: visible) { _, verses in
            guard isReady, let first = verses.min() else { return }
            library.setLastRead(VerseRef(surah: surahID, ayah: first))
            if verses.contains(surah.verseCount) { library.markCompleted(surahID) }
        }
        .navigationTitle(surah.name)
        .navigationBarTitleDisplayMode(.inline)
        .minimizesNavigationBarOnScroll()
        .toolbar(.hidden, for: .tabBar)
        .toolbar {
            ToolbarItemGroup(placement: .topBarTrailing) {
                Button {
                    if player.current?.surah == surahID {
                        player.togglePlayPause()
                    } else {
                        let from = visible.min() ?? 1
                        player.repeatMode = .off
                        player.play(VerseRef(surah: surahID, ayah: from))
                    }
                } label: {
                    let playingHere = player.current?.surah == surahID && player.isPlaying
                    Label(playingHere ? "Pause" : "Listen", systemImage: playingHere ? "pause.fill" : "headphones")
                        .contentTransition(.symbolEffect(.replace))
                }
                Button {
                    showSettings = true
                } label: {
                    Label("Reading settings", systemImage: "textformat.size")
                }
                Menu {
                    let favourite = library.isFavourite(surahID)
                    Button {
                        withAnimation(reduceMotion ? nil : Motion.reward) {
                            library.toggleFavourite(surahID)
                        }
                    } label: {
                        Label(favourite ? "Remove from Favourites" : "Add to Favourites",
                              systemImage: favourite ? "heart.slash" : "heart")
                    }
                    ShareLink(
                        item: SurahShareText(surah: surah, quran: quran, translations: translations),
                        preview: SharePreview(surah.name)
                    ) {
                        Label("Share Surah", systemImage: "square.and.arrow.up")
                    }
                } label: {
                    Label("More", systemImage: "ellipsis.circle")
                }
                .sensoryFeedback(.selection, trigger: library.isFavourite(surahID)) { _, _ in hapticsOn }
            }
        }
        .sheet(item: $selected) { ref in
            VerseActionsSheet(ref: ref)
                .presentationDetents([.medium, .large])
                .presentationDragIndicator(.visible)
                .presentationBackground(Palette.background)
                .presentationCornerRadius(28)
        }
        .sheet(isPresented: $showSettings) {
            NavigationStack {
                ReadingSettingsForm(showsLanguageLink: false, surahID: surahID)
                    .navigationTitle("Reading")
                    .navigationBarTitleDisplayMode(.inline)
                    .toolbar {
                        ToolbarItem(placement: .confirmationAction) {
                            Button("Done") { showSettings = false }
                        }
                    }
            }
            .presentationDetents([.medium, .large])
            .presentationDragIndicator(.visible)
        }
    }
}

// MARK: - Basmala glow

/// A soft light that blooms once behind the Basmala, then settles to a faint warmth.
private struct BloomGlow: View {
    let delay: Double
    @State private var phase = 0
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        Ellipse()
            .fill(BookInk.glow)
            .blur(radius: 24)
            .scaleEffect(phase == 0 ? 0.4 : 1)
            .opacity(phase == 0 ? 0 : (phase == 1 ? 0.9 : 0.35))
            .accessibilityHidden(true)
            .onAppear {
                guard phase == 0 else { return }
                if reduceMotion { phase = 2; return }
                withAnimation(.smooth(duration: 1.2).delay(delay)) { phase = 1 }
                withAnimation(.smooth(duration: 1.6).delay(delay + 1.4)) { phase = 2 }
            }
    }
}

// MARK: - Reading line

/// A hairline under the navigation bar that fills as she moves through the surah.
private struct ReadingLine: View {
    let progress: Double
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        GeometryReader { geo in
            Capsule()
                .fill(LinearGradient(colors: [BookInk.rule, BookInk.gild], startPoint: .leading, endPoint: .trailing))
                .frame(width: max(6, geo.size.width * CGFloat(progress)), height: 2.5)
                .animation(reduceMotion ? nil : .spring(response: 0.6, dampingFraction: 0.85), value: progress)
        }
        .frame(height: 2.5)
        .accessibilityElement()
        .accessibilityLabel("Reading position")
        .accessibilityValue("\(Int((progress * 100).rounded())) percent")
    }
}

// MARK: - Header

private struct SurahHeader: View {
    let surah: Surah

    var body: some View {
        VStack(spacing: Space.s) {
            ZStack {
                RoundedRectangle(cornerRadius: 6, style: .continuous)
                    .fill(BookInk.gild.opacity(0.14))
                RoundedRectangle(cornerRadius: 6, style: .continuous)
                    .strokeBorder(BookInk.gild, lineWidth: 1.2)
                RoundedRectangle(cornerRadius: 4, style: .continuous)
                    .strokeBorder(BookInk.rule.opacity(0.5), lineWidth: 0.6)
                    .padding(3)
                HStack {
                    StarMark().fill(BookInk.gild).frame(width: 14, height: 14)
                    Spacer()
                    Text(surah.arabicName)
                        .font(AppFont.arabic(34, relativeTo: .largeTitle))
                        .foregroundStyle(BookInk.caption)
                        .inkReveal(delay: 0.1, duration: 0.9)
                    Spacer()
                    StarMark().fill(BookInk.gild).frame(width: 14, height: 14)
                }
                .padding(.horizontal, 14)
            }
            .frame(height: 66)
            .accessibilityHidden(true)
            Text(surah.name)
                .font(.system(.title2, design: .serif, weight: .semibold))
                .foregroundStyle(BookInk.ink)
                .focusIn(delay: 0.3)
            Text("\(surah.meaning) · \(surah.verseCount) verses · \(surah.revelationPlace)")
                .font(.subheadline)
                .foregroundStyle(BookInk.caption)
                .multilineTextAlignment(.center)
                .focusIn(delay: 0.42)
        }
        .frame(maxWidth: .infinity)
        .accessibilityElement(children: .combine)
        .accessibilityAddTraits(.isHeader)
    }
}

// MARK: - Verse

private struct VerseRow: View {
    let ref: VerseRef
    let text: String
    let hasSajdah: Bool
    let isPlaying: Bool
    let isSelected: Bool
    let isBookmarked: Bool
    let highlight: HighlightColor?
    let note: String?
    let arabicSize: Double
    let translationSize: Double
    let showTranslation: Bool
    let showTransliteration: Bool
    let glide: Namespace.ID

    @AppStorage(PrefKey.haptics) private var hapticsOn = true

    var body: some View {
        VStack(alignment: .leading, spacing: Space.m) {
            HStack(spacing: Space.xs) {
                Text("\(ref.surah):\(ref.ayah)")
                    .font(.caption.weight(.semibold).monospacedDigit())
                    .foregroundStyle(BookInk.rubric)
                if hasSajdah {
                    Label("Sajdah", systemImage: "arrow.down.to.line")
                        .font(.caption.weight(.medium))
                        .foregroundStyle(BookInk.rubric)
                }
                Spacer()
                if isBookmarked {
                    // A ribbon that drops in when she saves the verse.
                    Image(systemName: "bookmark.fill")
                        .font(.footnote)
                        .foregroundStyle(BookInk.ribbon)
                        .transition(.asymmetric(
                            insertion: .move(edge: .top).combined(with: .opacity),
                            removal: .scale(scale: 0.4).combined(with: .opacity)
                        ))
                        .accessibilityLabel("Saved")
                }
                if isPlaying {
                    Image(systemName: "waveform")
                        .font(.caption)
                        .foregroundStyle(BookInk.gild)
                        .symbolEffect(.variableColor.iterative, options: .repeating)
                        .transition(.scale.combined(with: .opacity))
                        .accessibilityLabel("Playing")
                }
            }

            ArabicText(text: "\(text) ﴿\(ref.ayah.arabicIndic)﴾", size: arabicSize, color: BookInk.ink)

            if showTransliteration {
                TransliterationLine(ref: ref, size: CGFloat(translationSize))
            }

            if isPlaying {
                RecitationLine()
                    .transition(.opacity)
            }

            if showTranslation {
                TranslationBody(ref: ref, size: CGFloat(translationSize))
            }

            if let note {
                Label {
                    Text(note)
                        .font(AppFont.serif(.subheadline))
                        .lineLimit(3)
                } icon: {
                    Image(systemName: "note.text")
                }
                .foregroundStyle(BookInk.caption)
                .padding(Space.s)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(RoundedRectangle(cornerRadius: Radius.control, style: .continuous).fill(BookInk.pageEdge.opacity(0.7)))
                .accessibilityLabel("Your note: \(note)")
            }
        }
        .padding(.vertical, Space.l)
        .padding(.horizontal, Space.s)
        .background {
            ZStack {
                RoundedRectangle(cornerRadius: Radius.tile, style: .continuous)
                    .fill(staticFill)
                // The recitation highlight glides from one verse to the next.
                if isPlaying {
                    RoundedRectangle(cornerRadius: Radius.tile, style: .continuous)
                        .fill(BookInk.gild.opacity(0.16))
                        .matchedGeometryEffect(id: "playing", in: glide)
                }
            }
            .padding(.horizontal, -Space.xxs)
        }
        .overlay(alignment: .bottom) {
            Rectangle()
                .fill(BookInk.rule.opacity(0.22))
                .frame(height: 1)
                .padding(.horizontal, Space.xxxl)
                .opacity(isPlaying || isSelected || highlight != nil ? 0 : 1)
        }
        .contentShape(Rectangle())
        .animation(.smooth(duration: 0.55), value: isPlaying)
        .animation(.spring(response: 0.4, dampingFraction: 0.55), value: isBookmarked)
        .animation(Motion.gentle, value: isSelected)
        .animation(Motion.gentle, value: highlight)
        .sensoryFeedback(.impact(flexibility: .soft, intensity: 0.5), trigger: isBookmarked) { _, new in hapticsOn && new }
        .accessibilityElement(children: .combine)
        .accessibilityAddTraits(.isButton)
        .accessibilityHint("Shows play, repeat, save and share")
    }

    private var staticFill: Color {
        if isSelected { return BookInk.pageEdge }
        return highlight?.fill ?? .clear
    }
}

/// A fine line under the verse being recited, filling right to left with the audio,
/// the direction the Arabic is read.
struct RecitationLine: View {
    @Environment(AudioPlayer.self) private var player

    var body: some View {
        GeometryReader { geo in
            ZStack(alignment: .trailing) {
                Capsule().fill(BookInk.gild.opacity(0.2))
                Capsule()
                    .fill(BookInk.gild)
                    .frame(width: geo.size.width * CGFloat(player.itemProgress))
                    .animation(.linear(duration: 0.1), value: player.itemProgress)
            }
        }
        .frame(height: 2)
        .accessibilityHidden(true)
    }
}

// MARK: - Footer

/// The end of a surah: a quiet moment, then the way forward.
private struct NextSurahFooter: View {
    let current: Surah
    let reachedEnd: Bool

    @Environment(QuranStore.self) private var quran
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @AppStorage(PrefKey.haptics) private var hapticsOn = true
    @State private var drawn: CGFloat = 0
    @State private var bloomed = false

    var body: some View {
        VStack(spacing: Space.m) {
            ZStack {
                Circle()
                    .fill(RadialGradient(colors: [BookInk.glow, BookInk.glow.opacity(0)], center: .center, startRadius: 0, endRadius: 60))
                    .frame(width: 120, height: 120)
                    .scaleEffect(bloomed ? 1 : 0.3)
                    .opacity(bloomed ? 1 : 0)
                LotusMark()
                    .trim(from: 0, to: drawn)
                    .stroke(BookInk.gild, style: StrokeStyle(lineWidth: 1.6, lineCap: .round, lineJoin: .round))
                    .frame(width: 52, height: 37)
            }
            .accessibilityHidden(true)

            VStack(spacing: 4) {
                Text("Alhamdulillah")
                    .font(.system(.title3, design: .serif, weight: .semibold))
                    .foregroundStyle(BookInk.ink)
                Text("You finished \(current.name).")
                    .font(.subheadline)
                    .foregroundStyle(BookInk.caption)
            }
            .opacity(bloomed ? 1 : 0)
            .blur(radius: bloomed || reduceMotion ? 0 : 6)

            if current.id < 114 {
                let next = quran.surah(current.id + 1)
                NavigationLink(value: Route.reader(surah: next.id, verse: nil)) {
                    HStack(spacing: Space.s) {
                        Text("Next: \(next.name)")
                        Image(systemName: "arrow.right")
                    }
                    .zoomSource(ZoomID.reader(next.id, nil))
                }
                .buttonStyle(.tinted)
                .padding(.top, Space.s)
            }
        }
        .frame(maxWidth: .infinity)
        .onChange(of: reachedEnd, initial: true) { _, reached in
            guard reached, !bloomed else { return }
            if reduceMotion {
                drawn = 1
                bloomed = true
                return
            }
            withAnimation(.easeInOut(duration: 1.1)) { drawn = 1 }
            withAnimation(.spring(response: 0.6, dampingFraction: 0.6).delay(0.9)) { bloomed = true }
        }
        .sensoryFeedback(.success, trigger: bloomed) { _, new in hapticsOn && new }
    }
}
