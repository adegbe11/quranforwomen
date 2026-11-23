import SwiftUI

struct DuasView: View {
    var body: some View {
        TabStack(tab: .duas) {
            DuasHome()
        }
    }
}

private struct DuasHome: View {
    @Environment(ContentStore.self) private var content
    @Environment(DhikrStore.self) private var extras
    @State private var query = ""

    var body: some View {
        ScrollView {
            if trimmed.isEmpty {
                VStack(alignment: .leading, spacing: Space.xl) {
                    GroupedSection {
                        NavigationLink(value: Route.dhikr) {
                            ListRow(symbol: "circle.dotted", title: "Dhikr")
                                .zoomSource(ZoomID.dhikr)
                        }
                    }
                    .dealIn(0)

                    let recent = extras.recentDuaIDs.compactMap { content.dua(id: $0) }
                    if !recent.isEmpty {
                        RecentDuasRow(duas: recent)
                            .dealIn(1, baseDelay: 0.1)
                            .transition(.opacity)
                    }

                    GroupedSection(title: "Categories") {
                        ForEach(Array(content.categories.enumerated()), id: \.element.id) { index, category in
                            if index > 0 { RowDivider() }
                            NavigationLink(value: Route.duaCategory(category.id)) {
                                ListRow(
                                    symbol: category.symbol,
                                    title: category.title,
                                    value: "\(content.duas(in: category).count)"
                                )
                                .modifier(LandingBounce(delay: 0.25 + Double(index) * 0.05))
                                .zoomSource(ZoomID.duaCategory(category.id))
                            }
                            .dealIn(index, baseDelay: 0.2, step: 0.05)
                        }
                    }

                    GroupedSection {
                        NavigationLink(value: Route.personalDuas) {
                            ListRow(symbol: "pencil", title: "Your Own Words")
                                .zoomSource(ZoomID.personalDuas)
                        }
                    }
                    .dealIn(2, baseDelay: 0.5)
                }
                .padding(.horizontal, Space.page)
                .padding(.vertical, Space.s)
            } else {
                searchResults
                    .padding(.horizontal, Space.page)
            }
        }
        .bookPage()
        .navigationTitle("Duas")
        .searchable(text: $query, prompt: "Search duas")
    }

    private var trimmed: String { query.trimmingCharacters(in: .whitespacesAndNewlines) }

    private var matches: [Dua] {
        let isArabic = ArabicSearch.containsArabic(trimmed)
        let needle = isArabic ? ArabicSearch.normalize(trimmed) : trimmed
        return content.duas.filter { dua in
            if isArabic { return ArabicSearch.normalize(dua.arabic).contains(needle) }
            let fields = [dua.title, dua.translation, dua.transliteration ?? "", dua.source,
                          content.category(id: dua.category)?.title ?? ""]
            return fields.contains { $0.range(of: needle, options: [.caseInsensitive, .diacriticInsensitive]) != nil }
        }
    }

    @ViewBuilder
    private var searchResults: some View {
        let found = matches
        if found.isEmpty {
            ContentUnavailableView.search(text: trimmed)
                .padding(.top, Space.xxxl)
        } else {
            LazyVStack(spacing: Space.s) {
                ForEach(found) { dua in
                    NavigationLink(value: Route.dua(dua.id)) {
                        DuaCard(dua: dua)
                            .zoomSource(ZoomID.dua(dua.id))
                    }
                    .buttonStyle(.pressable)
                    .scrollSettle()
                }
            }
            .padding(.vertical, Space.s)
        }
    }
}

private struct CategoryTile: View {
    let category: DuaCategory
    let count: Int

    var body: some View {
        VStack(alignment: .leading, spacing: Space.m) {
            Image(systemName: category.symbol)
                .font(.system(size: 20, weight: .medium))
                .foregroundStyle(BookInk.rule)
                .frame(width: 40, height: 40)
                .background(Circle().fill(BookInk.gild.opacity(0.2)))
            VStack(alignment: .leading, spacing: 2) {
                Text(category.title)
                    .font(.headline)
                    .foregroundStyle(BookInk.ink)
                    .multilineTextAlignment(.leading)
                    .fixedSize(horizontal: false, vertical: true)
                Text(count == 1 ? "1 dua" : "\(count) duas")
                    .font(.subheadline)
                    .foregroundStyle(BookInk.caption)
            }
        }
        .frame(maxWidth: .infinity, minHeight: 120, alignment: .topLeading)
        .cardSurface(padding: Space.m)
        .accessibilityElement(children: .combine)
    }
}

/// A dua preview used in lists. Its Arabic line inks in right to left, a beat
/// after the card lands, so the card reads in the order the dua is said.
struct DuaCard: View {
    let dua: Dua
    /// When the Arabic starts to ink in. Lists that deal cards in pass a later value.
    var revealDelay: Double = 0.2

    var body: some View {
        VStack(alignment: .leading, spacing: Space.s) {
            Text(dua.title)
                .font(.headline)
                .foregroundStyle(BookInk.ink)
                .multilineTextAlignment(.leading)
            ArabicText(text: dua.arabic, size: 22, lineLimit: 2)
                .inkReveal(delay: revealDelay, duration: 0.8)
            Text(dua.translation)
                .font(AppFont.serif(.subheadline))
                .foregroundStyle(BookInk.caption)
                .lineLimit(2)
                .multilineTextAlignment(.leading)
            SourceLabel(kind: dua.sourceKind, reference: dua.source)
        }
        .cardSurface()
    }
}

/// One category of duas. A small header comes into focus and its symbol gives one
/// bounce, then the cards are dealt in and ease as they scroll. Each card zooms open.
struct DuaCategoryView: View {
    let categoryID: String

    @Environment(ContentStore.self) private var content

    var body: some View {
        ScrollView {
            LazyVStack(spacing: Space.s) {
                if let category = content.category(id: categoryID) {
                    let duas = content.duas(in: category)
                    categoryHeader(category, count: duas.count)
                        .padding(.bottom, Space.xs)

                    ForEach(Array(duas.enumerated()), id: \.element.id) { index, dua in
                        let slot = min(index, 8)
                        NavigationLink(value: Route.dua(dua.id)) {
                            // The Arabic inks in just after this card has been dealt.
                            DuaCard(dua: dua, revealDelay: 0.3 + Double(slot) * 0.06)
                                .zoomSource(ZoomID.dua(dua.id))
                        }
                        .buttonStyle(.pressable)
                        .dealIn(slot, baseDelay: 0.15, step: 0.06)
                        .scrollSettle()
                    }
                }
            }
            .padding(.horizontal, Space.page)
            .padding(.vertical, Space.s)
        }
        .bookPage()
        .navigationTitle(content.category(id: categoryID)?.title ?? "Duas")
    }

    private func categoryHeader(_ category: DuaCategory, count: Int) -> some View {
        HStack(spacing: Space.s) {
            Image(systemName: category.symbol)
                .font(.system(size: 17, weight: .medium))
                .foregroundStyle(BookInk.rule)
                .frame(width: 36, height: 36)
                .background(Circle().fill(BookInk.gild.opacity(0.2)))
                .modifier(LandingBounce(delay: 0.2))
                .accessibilityHidden(true)
            Text(count == 1 ? "1 dua" : "\(count) duas")
                .font(.subheadline)
                .foregroundStyle(BookInk.caption)
            Spacer(minLength: 0)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .focusIn(delay: 0.05)
    }
}

/// The row's symbol gives one small bounce as the row lands.
private struct LandingBounce: ViewModifier {
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
