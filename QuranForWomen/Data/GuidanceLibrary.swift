import Foundation

/// One article of guidance or a devotional, written by a named author and checked by a named reviewer.
/// The app never writes these. They come only from the bundled guidance.json.
struct GuidanceArticle: Decodable, Identifiable, Hashable {
    let id: String
    let section: String
    let title: String
    /// Paragraphs, in order.
    let body: [String]
    /// Quran verses as "surah:ayah", for example "2:222".
    let quranRefs: [String]?
    /// Hadith citations as written, for example "Sahih Muslim 332".
    let hadithRefs: [String]?
    let author: String?
    let reviewer: String?
    /// "2026-10-01".
    let reviewedOn: String?
    /// "draft" or "approved".
    let status: String

    var verses: [VerseRef] { (quranRefs ?? []).compactMap(VerseRef.init) }
    var hadith: [String] { (hadithRefs ?? []).filter { !$0.trimmingCharacters(in: .whitespaces).isEmpty } }

    /// Only approved articles with a named reviewer are ever shown.
    var isPublishable: Bool {
        status == "approved"
            && !(reviewer ?? "").trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            && !title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            && !body.isEmpty
    }
}

/// The bundled guidance and devotionals. Empty until reviewed articles are added,
/// and every screen for it stays hidden until then.
enum GuidanceLibrary {
    private struct Payload: Decodable {
        let items: [Lossy]
    }

    /// One bad item never hides the others.
    private struct Lossy: Decodable {
        let value: GuidanceArticle?
        init(from decoder: Decoder) throws {
            value = try? GuidanceArticle(from: decoder)
        }
    }

    static let articles: [GuidanceArticle] = {
        guard
            let url = Bundle.main.url(forResource: "guidance", withExtension: "json"),
            let data = try? Data(contentsOf: url),
            let payload = try? JSONDecoder().decode(Payload.self, from: data)
        else { return [] }
        return payload.items.compactMap(\.value).filter(\.isPublishable)
    }()

    /// Section names in the order they first appear.
    static var sections: [String] {
        var seen: [String] = []
        for article in articles where !seen.contains(article.section) {
            seen.append(article.section)
        }
        return seen
    }

    static var isEmpty: Bool { articles.isEmpty }

    static func articles(in section: String) -> [GuidanceArticle] {
        articles.filter { $0.section == section }
    }

    static func article(id: String) -> GuidanceArticle? {
        articles.first { $0.id == id }
    }
}
