import SwiftUI

/// The Quran tab. Motion: the place she left off comes into focus and its line fills
/// to where she stopped, the first surahs are dealt in, and rows settle as they scroll.
struct QuranLibraryView: View {
    var body: some View {
        TabStack(tab: .quran) {
            QuranLibraryContent()
        }
    }
}

/// Browse and search share one scrolling page. Switching between them crossfades,
/// and search results ease in as they arrive.
private struct QuranLibraryContent: View {
    @Environment(QuranStore.self) private var quran
    @Environment(LibraryStore.self) private var library
    @Environment(TranslationStore.self) private var translations
    @Environment(ContentStore.self) private var content
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    @State private var query = ""
    @State private var results = QuranSearch.Results()
    @State private var isSearching = false
    @State private var showMushaf = false
    @AppStorage("quran.browse") private var browseBy: LibraryBrowse = .surah
    @AppStorage(PrefKey.haptics) private var hapticsOn = true

    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: Space.xl) {
                if trimmedQuery.isEmpty {
                    browse
                } else {
                    searchResults
                }
            }
            .padding(.horizontal, Space.page)
            .padding(.top, Space.s)
            .padding(.bottom, Space.xxl)
            .animation(reduceMotion ? nil : Motion.gentle, value: trimmedQuery.isEmpty)
            .animation(reduceMotion ? nil : Motion.gentle, value: results.surahs)
            .animation(reduceMotion ? nil : Motion.gentle, value: results.verses)
            .animation(reduceMotion ? nil : Motion.gentle, value: isSearching)
            .animation(reduceMotion ? nil : Motion.gentle, value: browseBy)
            .animation(reduceMotion ? nil : Motion.gentle, value: library.favouriteSurahs)
        }
        .pageBackground()
        .navigationTitle("Quran")
        .searchable(text: $query, prompt: "Surah, verse, or words")
        .task(id: trimmedQuery) {
            await runSearch()
        }
        .fullScreenCover(isPresented: $showMushaf) { MushafView().zoomDestination(ZoomID.mushaf) }
    }

    private var trimmedQuery: String {
        query.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    @ViewBuilder
    private var browse: some View {
        MushafCard { showMushaf = true }
            .focusIn(delay: 0.02)
            .scrollSettle()

        if let last = library.lastRead {
            ContinueCard(ref: last)
                .focusIn(delay: 0.05)
                .scrollSettle()
        }

        if !library.favouriteSurahs.isEmpty {
            RowGroup(title: "Favourites") {
                ForEach(Array(library.favouriteSurahs.enumerated()), id: \.element) { index, id in
                    NavigationLink(value: Route.reader(surah: id, verse: nil)) {
                        SurahRow(surah: quran.surah(id), isComplete: library.completedSurahs.contains(id), isFavourite: true)
                            .padding(.horizontal, Space.m)
                            .padding(.vertical, Space.xs)
                            .contentShape(Rectangle())
                    }
                    .overlay(alignment: .top) {
                        if index > 0 { RowDivider(inset: 70) }
                    }
                    .modifier(FavouriteMenu(surah: id))
                    .transition(.opacity)
                }
            }
            .dealIn(0, baseDelay: 0.08)
            .scrollSettle()
            .transition(.opacity)
        }

        GroupedSection {
            NavigationLink(value: Route.explore) {
                ListRow(symbol: "sparkles", title: "Topics, plans and quiz")
            }
            RowDivider()
            NavigationLink(value: Route.annotations) {
                ListRow(symbol: "highlighter", title: "Highlights and notes")
            }
        }
        .dealIn(0, baseDelay: 0.12)
        .scrollSettle()

        Picker("Browse by", selection: $browseBy) {
            ForEach(LibraryBrowse.allCases) { Text($0.title).tag($0) }
        }
        .pickerStyle(.segmented)
        .frame(minHeight: 44)
        .dealIn(1, baseDelay: 0.12)
        .sensoryFeedback(.selection, trigger: browseBy) { _, _ in hapticsOn }

        switch browseBy {
        case .surah:
            surahList
                .transition(.opacity)
        case .juz:
            JuzList()
                .transition(.opacity)
        case .hizb:
            HizbList()
                .transition(.opacity)
        }
    }

    private var surahList: some View {
        RowGroup(title: "114 Surahs") {
            ForEach(quran.surahs) { surah in
                let index = surah.id - 1
                NavigationLink(value: Route.reader(surah: surah.id, verse: nil)) {
                    SurahRow(
                        surah: surah,
                        isComplete: library.completedSurahs.contains(surah.id),
                        popDelay: index < OpeningDeal.count ? 0.4 + Double(min(index, 8)) * 0.05 : 0.1,
                        isFavourite: library.isFavourite(surah.id)
                    )
                    .padding(.horizontal, Space.m)
                    .padding(.vertical, Space.xs)
                    .contentShape(Rectangle())
                    .zoomSource(ZoomID.reader(surah.id, nil))
                }
                .overlay(alignment: .top) {
                    if index > 0 { RowDivider(inset: 70) }
                }
                .modifier(FavouriteMenu(surah: surah.id))
                .modifier(OpeningDeal(index: index))
                .scrollSettle()
            }
        }
    }

    @ViewBuilder
    private var searchResults: some View {
        if let jump = results.jump {
            GroupedSection {
                NavigationLink(value: Route.reader(surah: jump.surah, verse: jump.ayah)) {
                    ListRow(symbol: "arrow.right.circle", title: "Go to \(quran.surah(jump.surah).name), verse \(jump.ayah)")
                        .zoomSource(ZoomID.reader(jump.surah, jump.ayah))
                }
            }
            .transition(.opacity)
            .scrollSettle()
        }
        if !results.surahs.isEmpty {
            RowGroup(title: "Surahs") {
                ForEach(Array(results.surahs.enumerated()), id: \.element) { index, id in
                    NavigationLink(value: Route.reader(surah: id, verse: nil)) {
                        SurahRow(surah: quran.surah(id), isComplete: library.completedSurahs.contains(id),
                                 isFavourite: library.isFavourite(id))
                            .padding(.horizontal, Space.m)
                            .padding(.vertical, Space.xs)
                            .contentShape(Rectangle())
                            .zoomSource(ZoomID.reader(id, nil))
                    }
                    .overlay(alignment: .top) {
                        if index > 0 { RowDivider(inset: 70) }
                    }
                    .modifier(FavouriteMenu(surah: id))
                    .scrollSettle()
                }
            }
            .transition(.opacity)
        }
        if !results.verses.isEmpty {
            RowGroup(title: results.verses.count >= QuranSearch.limit ? "First \(QuranSearch.limit) verses" : "Verses") {
                ForEach(Array(results.verses.enumerated()), id: \.element) { index, ref in
                    NavigationLink(value: Route.reader(surah: ref.surah, verse: ref.ayah)) {
                        SearchVerseRow(ref: ref)
                            .padding(.horizontal, Space.m)
                            .padding(.vertical, Space.xs)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .contentShape(Rectangle())
                            .zoomSource(ZoomID.reader(ref.surah, ref.ayah))
                    }
                    .overlay(alignment: .top) {
                        if index > 0 { RowDivider(inset: Space.m) }
                    }
                    .scrollSettle()
                }
            }
            .transition(.opacity)
        }
        if isSearching && results.isEmpty {
            ProgressView()
                .frame(maxWidth: .infinity)
                .padding(.top, Space.xxl)
                .transition(.opacity)
        } else if results.isEmpty {
            ContentUnavailableView.search(text: trimmedQuery)
                .padding(.top, Space.xl)
                .transition(.opacity)
        }
    }

    private func runSearch() async {
        let text = trimmedQuery
        guard !text.isEmpty else {
            results = .init()
            isSearching = false
            return
        }
        isSearching = true
        try? await Task.sleep(for: .milliseconds(220))
        guard !Task.isCancelled else { return }
        let found = await QuranSearch.run(
            query: text,
            quran: quran,
            translation: translations.selectedVerses()
        )
        guard !Task.isCancelled else { return }
        results = found
        isSearching = false
    }
}

/// Where she left off, as a card. It comes into focus, and a fine line fills
/// to the verse she reached.
private struct ContinueCard: View {
    let ref: VerseRef

    @Environment(QuranStore.self) private var quran

    var body: some View {
        let surah = quran.surah(ref.surah)
        NavigationLink(value: Route.reader(surah: ref.surah, verse: ref.ayah)) {
            VStack(alignment: .leading, spacing: Space.m) {
                HStack(alignment: .firstTextBaseline, spacing: Space.s) {
                    VStack(alignment: .leading, spacing: 4) {
                        Label("Continue reading", systemImage: "book")
                            .font(.subheadline)
                            .foregroundStyle(Palette.textSecondary)
                        Text(surah.name)
                            .font(.title3.weight(.semibold))
                            .foregroundStyle(Palette.textPrimary)
                        Text("Verse \(ref.ayah) of \(surah.verseCount)")
                            .font(.subheadline.monospacedDigit())
                            .foregroundStyle(Palette.textSecondary)
                            .contentTransition(.numericText(value: Double(ref.ayah)))
                            .animation(.snappy, value: ref.ayah)
                    }
                    Spacer(minLength: Space.xs)
                    Text(surah.arabicName)
                        .font(AppFont.arabic(24))
                        .foregroundStyle(Palette.accent)
                        .accessibilityHidden(true)
                }
                ProgressLine(value: Double(ref.ayah) / Double(max(surah.verseCount, 1)))
            }
            .cardSurface(padding: Space.l)
            .zoomSource(ZoomID.reader(ref.surah, ref.ayah))
        }
        .buttonStyle(.pressable)
        .accessibilityElement(children: .combine)
        .accessibilityLabel("Continue \(surah.name), verse \(ref.ayah) of \(surah.verseCount)")
        .accessibilityAddTraits(.isButton)
    }
}

/// A slim line that fills to her place in the surah when it appears,
/// and glides to a new place when it changes.
private struct ProgressLine: View {
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
        .frame(height: 5)
        .animation(reduceMotion ? nil : .spring(response: 0.6, dampingFraction: 0.85), value: value)
        .accessibilityHidden(true)
        .onAppear {
            guard !shown else { return }
            withAnimation(reduceMotion ? nil : .spring(response: 0.9, dampingFraction: 0.85).delay(0.45)) {
                shown = true
            }
        }
    }
}

/// A titled group of rows on one surface. Built lazily, so the full surah list stays light.
private struct RowGroup<Content: View>: View {
    let title: String
    @ViewBuilder var content: Content

    var body: some View {
        VStack(alignment: .leading, spacing: Space.xs) {
            Text(title)
                .font(.title3.weight(.semibold))
                .foregroundStyle(Palette.textPrimary)
                .padding(.bottom, Space.xxs)
                .accessibilityAddTraits(.isHeader)
            LazyVStack(spacing: 0) {
                content
            }
            .buttonStyle(RowPressStyle())
            .background(RoundedRectangle(cornerRadius: Radius.tile, style: .continuous).fill(Palette.surface))
            .clipShape(RoundedRectangle(cornerRadius: Radius.tile, style: .continuous))
        }
    }
}

/// Deals in only the rows on the first screen, so a long scroll never waits on them.
private struct OpeningDeal: ViewModifier {
    static let count = 12
    let index: Int

    func body(content: Content) -> some View {
        if index < Self.count {
            content.dealIn(min(index, 8), baseDelay: 0.2, step: 0.05)
        } else {
            content
        }
    }
}

/// One surah in a list. When given a pop delay, its number badge pops in as the row
/// lands. The read mark scales in and gives one small bounce when it appears.
struct SurahRow: View {
    let surah: Surah
    var isComplete = false
    /// Seconds before the number badge pops in. nil keeps the badge still.
    var popDelay: Double?
    var isFavourite = false

    @State private var landed = false
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        let badgeShown = landed || popDelay == nil
        HStack(spacing: Space.m) {
            Text("\(surah.id)")
                .font(.subheadline.weight(.semibold).monospacedDigit())
                .foregroundStyle(Palette.accent)
                .frame(width: 38, height: 38)
                .background(RoundedRectangle(cornerRadius: 11, style: .continuous).fill(Palette.accentWash))
                .scaleEffect(badgeShown ? 1 : 0.55)
                .opacity(badgeShown ? 1 : 0)
                .accessibilityHidden(true)

            VStack(alignment: .leading, spacing: 3) {
                HStack(spacing: 6) {
                    Text(surah.name)
                        .font(.headline)
                        .foregroundStyle(Palette.textPrimary)
                    if isComplete {
                        Image(systemName: "checkmark.circle.fill")
                            .font(.caption)
                            .foregroundStyle(Palette.accent)
                            .symbolEffect(.bounce, value: isComplete)
                            .transition(.scale(scale: 0.4).combined(with: .opacity))
                            .accessibilityLabel("Read")
                    }
                    if isFavourite {
                        Image(systemName: "heart.fill")
                            .font(.caption)
                            .foregroundStyle(Palette.blushStrong)
                            .symbolEffect(.bounce, value: isFavourite)
                            .transition(.scale(scale: 0.4).combined(with: .opacity))
                            .accessibilityHidden(true)
                    }
                }
                Text("\(surah.meaning) · \(surah.verseCount) verses")
                    .font(.subheadline)
                    .foregroundStyle(Palette.textSecondary)
            }
            .multilineTextAlignment(.leading)

            Spacer(minLength: Space.xs)

            Text(surah.arabicName)
                .font(AppFont.arabic(21, relativeTo: .headline))
                .foregroundStyle(Palette.textPrimary)
                .accessibilityHidden(true)
        }
        .padding(.vertical, 4)
        .animation(reduceMotion ? nil : Motion.reward, value: isComplete)
        .animation(reduceMotion ? nil : Motion.reward, value: isFavourite)
        .accessibilityElement(children: .combine)
        .accessibilityLabel("Surah \(surah.id), \(surah.name), \(surah.meaning), \(surah.verseCount) verses\(isFavourite ? ", favourite" : "")")
        .onAppear {
            guard let popDelay, !landed else { return }
            if reduceMotion {
                landed = true
            } else {
                withAnimation(Motion.reward.delay(popDelay)) { landed = true }
            }
        }
    }
}

/// A verse found by search: reference, two lines of Arabic and of translation.
private struct SearchVerseRow: View {
    let ref: VerseRef

    @Environment(QuranStore.self) private var quran
    @Environment(TranslationStore.self) private var translations

    var body: some View {
        VStack(alignment: .leading, spacing: Space.xs) {
            Text(quran.displayName(ref))
                .font(.footnote.weight(.semibold))
                .foregroundStyle(Palette.accent)
            ArabicText(text: quran.text(ref), size: 21, lineLimit: 2)
            if let translation = translations.text(ref) {
                Text(translation)
                    .font(AppFont.serif(.subheadline))
                    .foregroundStyle(Palette.textSecondary)
                    .multilineTextAlignment(.leading)
                    .lineLimit(2)
            }
        }
        .padding(.vertical, 4)
    }
}

/// Finds surahs by name or number, jumps to "2:255", and searches verse text
/// in Arabic (ignoring diacritics) or in the downloaded translation.
enum QuranSearch {
    static let limit = 60

    struct Results {
        var jump: VerseRef?
        var surahs: [Int] = []
        var verses: [VerseRef] = []
        var isEmpty: Bool { jump == nil && surahs.isEmpty && verses.isEmpty }
    }

    @MainActor
    static func run(query: String, quran: QuranStore, translation: [[String]]?) async -> Results {
        var results = Results()

        // "2:255" or "2 255"
        let numbers = query.split(whereSeparator: { $0 == ":" || $0 == " " || $0 == "." }).compactMap { Int($0) }
        if numbers.count == 2, quran.contains(VerseRef(surah: numbers[0], ayah: numbers[1])) {
            results.jump = VerseRef(surah: numbers[0], ayah: numbers[1])
        }

        let folded = fold(query)
        let isArabic = ArabicSearch.containsArabic(query)
        let normalizedQuery = ArabicSearch.normalize(query)
        results.surahs = quran.surahs.filter { surah in
            if let n = Int(query), n == surah.id { return true }
            if isArabic { return !normalizedQuery.isEmpty && ArabicSearch.normalize(surah.arabicName).contains(normalizedQuery) }
            guard !folded.isEmpty else { return false }
            return fold(surah.name).contains(folded) || fold(surah.meaning).contains(folded)
        }.map(\.id)

        guard query.count >= 2, Int(query) == nil, results.jump == nil else { return results }

        let maybeSource: [[String]]?
        if isArabic {
            maybeSource = await quran.normalizedVerses()
        } else {
            maybeSource = translation
        }
        guard let source = maybeSource else { return results }

        let needle = isArabic ? normalizedQuery : query
        results.verses = await Task.detached(priority: .userInitiated) {
            var found: [VerseRef] = []
            outer: for (s, verses) in source.enumerated() {
                for (a, text) in verses.enumerated() {
                    let match = isArabic
                        ? text.contains(needle)
                        : text.range(of: needle, options: [.caseInsensitive, .diacriticInsensitive]) != nil
                    if match {
                        found.append(VerseRef(surah: s + 1, ayah: a + 1))
                        if found.count >= limit { break outer }
                    }
                }
            }
            return found
        }.value
        return results
    }

    /// Lowercase and drop punctuation, so "al baqara" matches "Al-Baqarah".
    private static func fold(_ text: String) -> String {
        text.folding(options: [.caseInsensitive, .diacriticInsensitive], locale: nil)
            .filter { $0.isLetter || $0.isNumber }
    }
}


/// The way into the mushaf: the closed book, tilted a little, with the page she is on.
private struct MushafCard: View {
    let open: () -> Void
    @AppStorage("mushaf.page") private var page = 1
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var lifted = false

    var body: some View {
        Button(action: open) {
            HStack(spacing: Space.l) {
                MushafCover()
                    .frame(width: 58, height: 88)
                    .zoomSource(ZoomID.mushaf)
                    .rotation3DEffect(.degrees(lifted || reduceMotion ? -12 : 0), axis: (x: 0, y: 1, z: 0), anchor: .trailing, perspective: 0.6)
                    .shadow(color: .black.opacity(0.18), radius: 6, x: -3, y: 4)
                VStack(alignment: .leading, spacing: 4) {
                    Text("The Mushaf")
                        .font(.system(.title3, design: .serif, weight: .semibold))
                        .foregroundStyle(Palette.textPrimary)
                    Text("Page \(page)")
                        .font(.subheadline)
                        .foregroundStyle(Palette.textSecondary)
                        .monospacedDigit()
                        .contentTransition(.numericText(value: Double(page)))
                }
                Spacer()
                Image(systemName: "book.pages")
                    .font(.title3)
                    .foregroundStyle(Palette.accent)
                    .accessibilityHidden(true)
            }
            .padding(Space.l)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(RoundedRectangle(cornerRadius: Radius.card, style: .continuous).fill(Palette.surface))
        }
        .buttonStyle(.pressable)
        .accessibilityLabel("The Mushaf, page \(page)")
        .accessibilityHint("Opens the Quran as a book")
        .onAppear {
            guard !lifted, !reduceMotion else { return }
            withAnimation(.spring(response: 0.8, dampingFraction: 0.7).delay(0.35)) { lifted = true }
        }
    }
}

// MARK: - Browse by surah, juz or hizb

enum LibraryBrowse: String, CaseIterable, Identifiable {
    case surah, juz, hizb

    var id: String { rawValue }
    var title: String {
        switch self {
        case .surah: "Surah"
        case .juz: "Juz"
        case .hizb: "Hizb"
        }
    }
}

/// Hold a surah row to add it to Favourites or take it out.
private struct FavouriteMenu: ViewModifier {
    let surah: Int

    @Environment(LibraryStore.self) private var library
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @AppStorage(PrefKey.haptics) private var hapticsOn = true

    func body(content: Content) -> some View {
        let favourite = library.isFavourite(surah)
        let title = favourite ? "Remove from Favourites" : "Add to Favourites"
        content
            .contextMenu {
                Button(title, systemImage: favourite ? "heart.slash" : "heart") { toggle() }
            }
            .accessibilityAction(named: title) { toggle() }
            .sensoryFeedback(.selection, trigger: favourite) { _, _ in hapticsOn }
    }

    private func toggle() {
        withAnimation(reduceMotion ? nil : Motion.reward) {
            library.toggleFavourite(surah)
        }
    }
}

/// The 30 juz, each opening the reader at its first verse.
private struct JuzList: View {
    @Environment(QuranStore.self) private var quran

    var body: some View {
        RowGroup(title: "30 Juz") {
            ForEach(Array(quran.juzStarts.enumerated()), id: \.offset) { index, start in
                NavigationLink(value: Route.reader(surah: start.surah, verse: start.ayah)) {
                    PartRow(number: index + 1, title: "Juz \(index + 1)", start: start,
                            popDelay: index < OpeningDeal.count ? 0.4 + Double(min(index, 8)) * 0.05 : nil)
                        .padding(.horizontal, Space.m)
                        .padding(.vertical, Space.xs)
                        .contentShape(Rectangle())
                        .zoomSource(ZoomID.reader(start.surah, start.ayah))
                }
                .overlay(alignment: .top) {
                    if index > 0 { RowDivider(inset: 70) }
                }
                .modifier(OpeningDeal(index: index))
                .scrollSettle()
            }
        }
    }
}

/// The 60 hizbs. Each row opens the hizb, and its three quarter marks open the reader there.
private struct HizbList: View {
    @Environment(QuranStore.self) private var quran

    var body: some View {
        RowGroup(title: "60 Hizbs") {
            ForEach(Array(quran.hizbs.enumerated()), id: \.offset) { index, quarters in
                VStack(alignment: .leading, spacing: 0) {
                    NavigationLink(value: Route.reader(surah: quarters[0].surah, verse: quarters[0].ayah)) {
                        PartRow(number: index + 1, title: "Hizb \(index + 1)", start: quarters[0],
                                popDelay: index < OpeningDeal.count ? 0.4 + Double(min(index, 8)) * 0.05 : nil)
                            .padding(.horizontal, Space.m)
                            .padding(.top, Space.xs)
                            .contentShape(Rectangle())
                            .zoomSource(ZoomID.reader(quarters[0].surah, quarters[0].ayah))
                    }
                    HStack(spacing: Space.xs) {
                        ForEach(1..<4, id: \.self) { quarter in
                            QuarterLink(hizb: index + 1, quarter: quarter, start: quarters[quarter])
                        }
                    }
                    .padding(.leading, 70)
                    .padding(.trailing, Space.m)
                    .padding(.bottom, Space.s)
                }
                .overlay(alignment: .top) {
                    if index > 0 { RowDivider(inset: 70) }
                }
                .modifier(OpeningDeal(index: index))
                .scrollSettle()
            }
        }
    }
}

/// One quarter mark of a hizb: the fraction and the verse it starts at.
private struct QuarterLink: View {
    let hizb: Int
    let quarter: Int
    let start: VerseRef

    @Environment(QuranStore.self) private var quran

    private static let marks = ["", "¼", "½", "¾"]
    private static let spoken = ["", "one quarter", "half", "three quarters"]

    var body: some View {
        NavigationLink(value: Route.reader(surah: start.surah, verse: start.ayah)) {
            Text("\(Self.marks[quarter]) · \(start.surah):\(start.ayah)")
                .font(.subheadline.weight(.medium).monospacedDigit())
                .foregroundStyle(Palette.accent)
                .lineLimit(1)
                .minimumScaleFactor(0.8)
                .frame(maxWidth: .infinity, minHeight: 44)
                .background(Capsule().fill(Palette.accentWash))
                .contentShape(Capsule())
                .zoomSource(ZoomID.reader(start.surah, start.ayah))
        }
        .buttonStyle(PressableStyle(scale: 0.94))
        .accessibilityLabel("Hizb \(hizb), \(Self.spoken[quarter]), \(quran.surah(start.surah).name) verse \(start.ayah)")
    }
}

/// A juz or hizb row: its number, where it starts, and that surah's Arabic name.
private struct PartRow: View {
    let number: Int
    let title: String
    let start: VerseRef
    var popDelay: Double?

    @Environment(QuranStore.self) private var quran
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var landed = false

    var body: some View {
        let surah = quran.surah(start.surah)
        let badgeShown = landed || popDelay == nil
        HStack(spacing: Space.m) {
            Text("\(number)")
                .font(.subheadline.weight(.semibold).monospacedDigit())
                .foregroundStyle(Palette.accent)
                .frame(width: 38, height: 38)
                .background(RoundedRectangle(cornerRadius: 11, style: .continuous).fill(Palette.accentWash))
                .scaleEffect(badgeShown ? 1 : 0.55)
                .opacity(badgeShown ? 1 : 0)
                .accessibilityHidden(true)
            VStack(alignment: .leading, spacing: 3) {
                Text(title)
                    .font(.headline)
                    .foregroundStyle(Palette.textPrimary)
                Text("\(surah.name) · \(start.surah):\(start.ayah)")
                    .font(.subheadline.monospacedDigit())
                    .foregroundStyle(Palette.textSecondary)
            }
            Spacer(minLength: Space.xs)
            Text(surah.arabicName)
                .font(AppFont.arabic(21, relativeTo: .headline))
                .foregroundStyle(Palette.textPrimary)
                .accessibilityHidden(true)
        }
        .padding(.vertical, 4)
        .frame(minHeight: 44)
        .accessibilityElement(children: .combine)
        .accessibilityLabel("\(title), starts at \(surah.name) verse \(start.ayah)")
        .onAppear {
            guard let popDelay, !landed else { return }
            if reduceMotion {
                landed = true
            } else {
                withAnimation(Motion.reward.delay(popDelay)) { landed = true }
            }
        }
    }
}
