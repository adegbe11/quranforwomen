import SwiftUI

// Guidance and devotionals. Every word here comes from guidance.json, written by a named
// author and approved by a named reviewer. While there are none, these screens are never reached.

/// The articles in one section, in one quiet list.
struct GuidanceSectionView: View {
    let section: String

    var body: some View {
        let articles = GuidanceLibrary.articles(in: section)
        ScrollView {
            VStack(alignment: .leading, spacing: Space.xl) {
                GroupedSection {
                    ForEach(Array(articles.enumerated()), id: \.element.id) { index, article in
                        if index > 0 { RowDivider(inset: Space.m) }
                        NavigationLink(value: Route.guidanceArticle(article.id)) {
                            ListRow(title: article.title)
                                .zoomSource("guidanceArticle-\(article.id)")
                        }
                        .dealIn(min(index, 8), baseDelay: 0.1, step: 0.05)
                    }
                }
                .focusIn(delay: 0.05)
            }
            .padding(.horizontal, Space.page)
            .padding(.vertical, Space.s)
        }
        .pageBackground()
        .navigationTitle(section)
    }
}

/// One article, set like a book. Its verses open the reader.
struct GuidanceArticleView: View {
    let articleID: String

    @Environment(QuranStore.self) private var quran

    var body: some View {
        ScrollView {
            if let article = GuidanceLibrary.article(id: articleID) {
                VStack(alignment: .leading, spacing: Space.l) {
                    VStack(alignment: .leading, spacing: Space.xs) {
                        Eyebrow(text: article.section)
                        Text(article.title)
                            .font(AppFont.serif(.largeTitle, weight: .semibold))
                            .foregroundStyle(BookInk.ink)
                            .fixedSize(horizontal: false, vertical: true)
                            .accessibilityAddTraits(.isHeader)
                    }
                    .focusIn(delay: 0.05)

                    ForEach(Array(article.body.enumerated()), id: \.offset) { index, paragraph in
                        TranslationText(text: paragraph)
                            .dealIn(min(index, 8), baseDelay: 0.15, step: 0.06)
                    }

                    let verses = article.verses.filter { quran.contains($0) }
                    if !verses.isEmpty {
                        SectionHeader(title: "From the Quran")
                            .padding(.top, Space.s)
                            .focusIn(delay: 0.3)
                        ForEach(Array(verses.enumerated()), id: \.element.id) { index, ref in
                            GuidanceVerseCard(ref: ref, inkDelay: 0.35 + Double(min(index, 6)) * 0.08)
                                .dealIn(min(index, 8), baseDelay: 0.3, step: 0.07)
                                .scrollSettle()
                        }
                    }

                    if !article.hadith.isEmpty {
                        SectionHeader(title: "From the Sunnah")
                            .padding(.top, Space.s)
                        VStack(alignment: .leading, spacing: Space.xs) {
                            ForEach(Array(article.hadith.enumerated()), id: \.offset) { index, citation in
                                SourceLabel(kind: .hadith, reference: citation)
                                    .dealIn(min(index, 8), baseDelay: 0.35, step: 0.05)
                            }
                        }
                    }

                    GuidanceCredits(article: article)
                        .padding(.top, Space.s)
                }
                .padding(.horizontal, Space.page)
                .readableWidth()
                .padding(.vertical, Space.s)
            }
        }
        .bookPage()
        .navigationTitle("")
        .navigationBarTitleDisplayMode(.inline)
        .minimizesNavigationBarOnScroll()
    }
}

/// A verse with its translation on the book page, opening the reader.
private struct GuidanceVerseCard: View {
    let ref: VerseRef
    let inkDelay: Double

    @Environment(QuranStore.self) private var quran

    var body: some View {
        NavigationLink(value: Route.reader(surah: ref.surah, verse: ref.ayah)) {
            VStack(alignment: .leading, spacing: Space.m) {
                ArabicText(text: "\(quran.text(ref)) ﴿\(ref.ayah.arabicIndic)﴾", size: 24)
                    .inkReveal(delay: inkDelay, duration: 1.0)
                TranslationBody(ref: ref)
                Text(quran.displayName(ref))
                    .font(.footnote.weight(.semibold))
                    .foregroundStyle(BookInk.rule)
            }
            .cardSurface()
        }
        .buttonStyle(.pressable)
    }
}

/// Who wrote it and who reviewed it.
private struct GuidanceCredits: View {
    let article: GuidanceArticle

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            if let author = article.author?.trimmingCharacters(in: .whitespaces), !author.isEmpty {
                Text("Written by \(author)")
            }
            if let reviewer = article.reviewer {
                Text(reviewedLine(reviewer))
            }
        }
        .font(.footnote)
        .foregroundStyle(BookInk.caption)
        .accessibilityElement(children: .combine)
    }

    private func reviewedLine(_ reviewer: String) -> String {
        guard let raw = article.reviewedOn, !raw.isEmpty else { return "Reviewed by \(reviewer)" }
        let parser = DateFormatter()
        parser.locale = Locale(identifier: "en_US_POSIX")
        parser.dateFormat = "yyyy-MM-dd"
        let date = parser.date(from: raw).map { $0.formatted(date: .long, time: .omitted) } ?? raw
        return "Reviewed by \(reviewer) · \(date)"
    }
}
