import Foundation

/// One place opened in the reader, and when. Kept in LibraryStore's reading history.
struct ReadingVisit: Codable, Hashable, Identifiable {
    var ref: VerseRef
    var date: Date

    var id: Int { ref.surah }
}

extension QuranStore {
    /// The 60 hizbs, each as its four quarter starts. Empty if the bundled data has no hizb index.
    var hizbs: [[VerseRef]] {
        guard hizbQuarterStarts.count == 240 else { return [] }
        return (0..<60).map { Array(hizbQuarterStarts[($0 * 4)..<($0 * 4 + 4)]) }
    }
}
