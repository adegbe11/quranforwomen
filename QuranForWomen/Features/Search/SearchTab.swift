import SwiftUI

/// One search for everything: surahs, verses, duas and her own journal pages.
/// Lives in the tab bar's search tab on iOS 18 and later.
struct SearchTab: View {
    var body: some View {
        TabStack(tab: .search) {
            UniversalSearch()
        }
    }
}

private enum SearchPrefKey {
    static let recent = "search.recent"
}

/// Results deal in as they arrive, the words she typed light up inside each result,
/// and soft placeholder rows stand in while the Quran is searched.
private struct UniversalSearch: View {
    @Environment(QuranStore.self) private var quran
    @Environment(ContentStore.self) private var content
    @Environment(TranslationStore.self) private var translations
    @Environment(JournalStore.self) private var journal
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @AppStorage(PrefKey.haptics) private var hapticsOn = true
    /// Recent searches, newest first, kept only on this iPhone.
    @AppStorage(SearchPrefKey.recent) private var recentStore = ""
    @AppStorage(PrefKey.dhikrSelection) private var dhikrSelection = "subhanallah"
    @Environment(AppRouter.self) private var router

    @State private var query = ""
    @State private var quranResults = QuranSearch.Results()
    @State private var searching = false
    @State private var chipTick = 0

    private let suggestions = ["Ayat al-Kursi", "2:255", "Patience", "Mercy", "Forgiveness", "Maryam"]

    private var recent: [String] { recentStore.split(separator: "\n").map(String.init) }

    var body: some View {
        List {
            if trimmed.isEmpty {
                idle
            } else {
                results
            }
        }
        .listStyle(.insetGrouped)
        .scrollContentBackground(.hidden)
        .pageBackground()
        .navigationTitle("Search")
        .searchable(text: $query, prompt: "Verses, surahs, duas, your journal")
        .onSubmit(of: .search) { remember(trimmed) }
        .task(id: trimmed) { await run() }
        .animation(reduceMotion ? nil : Motion.gentle, value: trimmed.isEmpty)
        .animation(reduceMotion ? nil : Motion.gentle, value: searching)
        .sensoryFeedback(.selection, trigger: chipTick) { _, _ in hapticsOn }
    }

    private var trimmed: String { query.trimmingCharacters(in: .whitespacesAndNewlines) }

    // MARK: Before she types

    @ViewBuilder
    private var idle: some View {
        if !recent.isEmpty {
            Section {
                ForEach(Array(recent.enumerated()), id: \.element) { index, item in
                    Button { pick(item) } label: {
                        Label(item, systemImage: "clock.arrow.circlepath")
                            .foregroundStyle(Palette.textPrimary)
                    }
                    .dealIn(index, baseDelay: 0.05, step: 0.04)
                }
            } header: {
                HStack {
                    Text("Recent")
                    Spacer()
                    Button("Clear") {
                        withAnimation(reduceMotion ? nil : Motion.gentle) { recentStore = "" }
                    }
                    .font(.footnote.weight(.semibold))
                    .textCase(nil)
                }
            }
            .listRowBackground(Palette.surface)
        }

        Section {
            FlowChips(items: suggestions) { pick($0) }
                .listRowBackground(Color.clear)
                .listRowInsets(EdgeInsets(top: Space.xs, leading: 0, bottom: Space.xs, trailing: 0))
        } header: {
            Text("Try")
        }
    }

    private func pick(_ text: String) {
        chipTick += 1
        query = text
        remember(text)
    }

    private func remember(_ text: String) {
        let clean = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !clean.isEmpty else { return }
        let list = [clean] + recent.filter { $0.caseInsensitiveCompare(clean) != .orderedSame }
        recentStore = list.prefix(6).joined(separator: "\n")
    }

    // MARK: Results

    private var duaMatches: [Dua] {
        let q = trimmed
        let isArabic = ArabicSearch.containsArabic(q)
        let needle = isArabic ? ArabicSearch.normalize(q) : q
        return content.duas.filter { dua in
            if isArabic { return ArabicSearch.normalize(dua.arabic).contains(needle) }
            return [dua.title, dua.translation, dua.transliteration ?? "", dua.source]
                .contains { $0.range(of: needle, options: [.caseInsensitive, .diacriticInsensitive]) != nil }
        }
    }

    private var dhikrMatches: [DhikrPhrase] {
        let q = trimmed
        let isArabic = ArabicSearch.containsArabic(q)
        let needle = isArabic ? ArabicSearch.normalize(q) : q
        return content.dhikr.filter { item in
            if isArabic { return ArabicSearch.normalize(item.arabic).contains(needle) }
            return [item.transliteration, item.translation, item.source]
                .contains { $0.range(of: needle, options: [.caseInsensitive, .diacriticInsensitive]) != nil }
        }
    }

    private var journalMatches: [JournalEntry] {
        guard journal.isAccessible else { return [] }
        let q = trimmed
        return journal.sortedEntries.filter { e in
            [e.title, e.body, e.gratitude, e.answeredNote].contains { $0.localizedCaseInsensitiveContains(q) }
        }
    }

    @ViewBuilder
    private var results: some View {
        let isKursi = trimmed.localizedCaseInsensitiveContains("kursi")
        // "Ayat al-Kursi" is a name people search for, so map it to 2:255.
        if isKursi {
            Section {
                JumpRow(title: "Ayat al-Kursi", detail: "Al-Baqarah 2:255", route: .reader(surah: 2, verse: 255))
            }
            .listRowBackground(Palette.accentWash.opacity(0.6))
        }
        if let jump = quranResults.jump {
            Section {
                JumpRow(title: "Go to \(quran.surah(jump.surah).name)", detail: "Verse \(jump.ayah)",
                        route: .reader(surah: jump.surah, verse: jump.ayah))
                    .id("\(jump.surah):\(jump.ayah)")
            }
            .listRowBackground(Palette.accentWash.opacity(0.6))
        }
        if !quranResults.surahs.isEmpty {
            Section("Surahs") {
                ForEach(Array(quranResults.surahs.enumerated()), id: \.element) { index, id in
                    NavigationLink(value: Route.reader(surah: id, verse: nil)) {
                        SurahRow(surah: quran.surah(id))
                            .zoomSource(ZoomID.reader(id, nil))
                    }
                    .dealIn(min(index, 8), baseDelay: 0.02, step: 0.035)
                }
            }
            .listRowBackground(Palette.surface)
        }
        let duas = duaMatches
        if !duas.isEmpty {
            Section("Duas") {
                ForEach(Array(duas.prefix(12).enumerated()), id: \.element.id) { index, dua in
                    NavigationLink(value: Route.dua(dua.id)) {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(highlight(dua.title))
                                .foregroundStyle(Palette.textPrimary)
                            Text(dua.source).font(.footnote).foregroundStyle(Palette.textSecondary)
                        }
                        .zoomSource(ZoomID.dua(dua.id))
                    }
                    .dealIn(min(index, 8), baseDelay: 0.04, step: 0.035)
                }
            }
            .listRowBackground(Palette.surface)
        }
        let dhikr = dhikrMatches
        if !dhikr.isEmpty {
            Section("Dhikr") {
                ForEach(Array(dhikr.prefix(8).enumerated()), id: \.element.id) { index, item in
                    // Opens the counter on this phrase.
                    Button {
                        dhikrSelection = item.id
                        router.paths[router.tab, default: []].append(.dhikr)
                    } label: {
                        HStack(spacing: Space.s) {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(highlight(item.transliteration))
                                    .foregroundStyle(Palette.textPrimary)
                                Text(highlight(item.translation))
                                    .font(.footnote)
                                    .foregroundStyle(Palette.textSecondary)
                                    .lineLimit(2)
                            }
                            .multilineTextAlignment(.leading)
                            Spacer(minLength: 0)
                            Image(systemName: "chevron.right")
                                .font(.footnote.weight(.semibold))
                                .foregroundStyle(Palette.textSecondary.opacity(0.5))
                                .accessibilityHidden(true)
                        }
                        .contentShape(Rectangle())
                        .zoomSource(ZoomID.dhikr)
                    }
                    .accessibilityHint("Opens the dhikr counter")
                    .dealIn(min(index, 8), baseDelay: 0.05, step: 0.035)
                }
            }
            .listRowBackground(Palette.surface)
        }
        let pages = journalMatches
        if !pages.isEmpty {
            Section("Your journal") {
                ForEach(Array(pages.prefix(12).enumerated()), id: \.element.id) { index, entry in
                    NavigationLink(value: Route.journalEntry(entry.id)) {
                        VStack(alignment: .leading, spacing: 2) {
                            if UserDefaults.standard.bool(forKey: JournalPrefKey.hidePreviews) {
                                Text("Private page").foregroundStyle(Palette.textPrimary)
                            } else {
                                Text(highlight(entry.displayTitle)).foregroundStyle(Palette.textPrimary)
                            }
                            Text(entry.date.formatted(date: .abbreviated, time: .omitted))
                                .font(.footnote).foregroundStyle(Palette.textSecondary)
                        }
                        .zoomSource(ZoomID.entry(entry.id))
                    }
                    .dealIn(min(index, 8), baseDelay: 0.06, step: 0.035)
                }
            }
            .listRowBackground(Palette.surface)
        }
        if !quranResults.verses.isEmpty {
            Section("Verses") {
                ForEach(Array(quranResults.verses.enumerated()), id: \.element) { index, ref in
                    NavigationLink(value: Route.reader(surah: ref.surah, verse: ref.ayah)) {
                        VStack(alignment: .leading, spacing: Space.xs) {
                            Text(quran.displayName(ref))
                                .font(.footnote.weight(.semibold))
                                .foregroundStyle(Palette.accent)
                            ArabicText(text: quran.text(ref), size: 20, lineLimit: 2)
                                .inkReveal(delay: 0.1 + Double(min(index, 6)) * 0.05, duration: 0.8)
                            if let t = translations.text(ref) {
                                Text(highlight(t))
                                    .font(AppFont.serif(.subheadline))
                                    .foregroundStyle(Palette.textSecondary)
                                    .lineLimit(2)
                            }
                        }
                        .padding(.vertical, 4)
                        .zoomSource(ZoomID.reader(ref.surah, ref.ayah))
                    }
                    .dealIn(min(index, 8), baseDelay: 0.06, step: 0.035)
                }
            }
            .listRowBackground(Palette.surface)
        }
        if searching && quranResults.isEmpty && duas.isEmpty && dhikr.isEmpty && pages.isEmpty {
            Section {
                ForEach(0..<3, id: \.self) { i in
                    PlaceholderRow(index: i)
                }
            }
            .listRowBackground(Palette.surface)
        } else if !searching && quranResults.isEmpty && duas.isEmpty && dhikr.isEmpty && pages.isEmpty && !isKursi {
            NoResults(text: trimmed)
                .listRowBackground(Color.clear)
        }
    }

    /// The words she typed, lit up in emerald inside a result.
    private func highlight(_ text: String) -> AttributedString {
        var attributed = AttributedString(text)
        let needle = trimmed
        guard !needle.isEmpty, !ArabicSearch.containsArabic(needle),
              let range = attributed.range(of: needle, options: [.caseInsensitive, .diacriticInsensitive]) else {
            return attributed
        }
        attributed[range].foregroundColor = Palette.accent
        attributed[range].inlinePresentationIntent = .stronglyEmphasized
        return attributed
    }

    private func run() async {
        let text = trimmed
        guard !text.isEmpty else {
            quranResults = .init()
            searching = false
            return
        }
        searching = true
        try? await Task.sleep(for: .milliseconds(220))
        guard !Task.isCancelled else { return }
        let found = await QuranSearch.run(query: text, quran: quran, translation: translations.selectedVerses())
        guard !Task.isCancelled else { return }
        withAnimation(reduceMotion ? nil : Motion.gentle) {
            quranResults = found
            searching = false
        }
    }
}

/// A direct jump to a verse. It pops in with a small spring.
private struct JumpRow: View {
    let title: String
    let detail: String
    let route: Route

    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var shown = false

    var body: some View {
        NavigationLink(value: route) {
            HStack(spacing: Space.m) {
                Image(systemName: "arrow.right.circle.fill")
                    .font(.title2)
                    .foregroundStyle(Palette.accent)
                    .symbolEffect(.bounce, value: shown)
                VStack(alignment: .leading, spacing: 1) {
                    Text(title)
                        .font(.headline)
                        .foregroundStyle(Palette.textPrimary)
                    Text(detail)
                        .font(.subheadline)
                        .foregroundStyle(Palette.textSecondary)
                }
            }
            .padding(.vertical, 4)
            .zoomSource(zoomID)
        }
        .scaleEffect(shown || reduceMotion ? 1 : 0.94)
        .opacity(shown || reduceMotion ? 1 : 0)
        .onAppear {
            withAnimation(.spring(response: 0.4, dampingFraction: 0.62)) { shown = true }
        }
    }

    private var zoomID: String {
        if case let .reader(surah, verse) = route { return ZoomID.reader(surah, verse) }
        return "jump"
    }
}

/// A soft stand-in row while results load.
private struct PlaceholderRow: View {
    let index: Int
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            RoundedRectangle(cornerRadius: 4).frame(width: 90, height: 10)
            RoundedRectangle(cornerRadius: 4).frame(height: 14)
            RoundedRectangle(cornerRadius: 4).frame(width: CGFloat(160 + index * 40), height: 10)
        }
        .foregroundStyle(Palette.border)
        .padding(.vertical, 6)
        .phaseAnimator([false, true]) { row, dim in
            row.opacity(dim && !reduceMotion ? 0.45 : 1)
        } animation: { _ in .easeInOut(duration: 0.8).delay(Double(index) * 0.12) }
        .accessibilityLabel("Searching")
    }
}

/// Nothing found: a small wiggle of the glass and a plain line.
private struct NoResults: View {
    let text: String
    @State private var shown = false

    var body: some View {
        VStack(spacing: Space.s) {
            Image(systemName: "magnifyingglass")
                .font(.system(size: 34, weight: .light))
                .foregroundStyle(Palette.textSecondary)
                .symbolEffect(.bounce, value: shown)
            Text("No results for \u{201C}\(text)\u{201D}")
                .font(.headline)
                .foregroundStyle(Palette.textPrimary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, Space.xl)
        .focusIn(delay: 0.05)
        .onAppear { shown = true }
    }
}

/// Suggestion chips that wrap onto new lines. They deal in with a small bounce.
private struct FlowChips: View {
    let items: [String]
    let onTap: (String) -> Void

    var body: some View {
        FlowLayout(spacing: Space.xs) {
            ForEach(Array(items.enumerated()), id: \.element) { index, item in
                Button(item) { onTap(item) }
                    .font(.subheadline.weight(.medium))
                    .foregroundStyle(Palette.accent)
                    .padding(.horizontal, Space.m)
                    .frame(minHeight: 36)
                    .background(Capsule().fill(Palette.accentWash))
                    .buttonStyle(PressableStyle(scale: 0.94))
                    .dealIn(index, baseDelay: 0.12, step: 0.045)
            }
        }
    }
}

/// A simple wrapping layout.
private struct FlowLayout: Layout {
    var spacing: CGFloat = 8

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let width = proposal.width ?? .infinity
        var x: CGFloat = 0, y: CGFloat = 0, rowHeight: CGFloat = 0, maxX: CGFloat = 0
        for view in subviews {
            let size = view.sizeThatFits(.unspecified)
            if x > 0 && x + size.width > width {
                x = 0
                y += rowHeight + spacing
                rowHeight = 0
            }
            x += size.width + spacing
            maxX = max(maxX, x - spacing)
            rowHeight = max(rowHeight, size.height)
        }
        return CGSize(width: maxX, height: y + rowHeight)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        var x = bounds.minX, y = bounds.minY, rowHeight: CGFloat = 0
        for view in subviews {
            let size = view.sizeThatFits(.unspecified)
            if x > bounds.minX && x + size.width > bounds.maxX {
                x = bounds.minX
                y += rowHeight + spacing
                rowHeight = 0
            }
            view.place(at: CGPoint(x: x, y: y), proposal: ProposedViewSize(size))
            x += size.width + spacing
            rowHeight = max(rowHeight, size.height)
        }
    }
}
