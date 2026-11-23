import CoreTransferable
import SwiftUI

/// A whole surah as plain text for the share sheet: its name, the Basmala where it is
/// shown, every verse in Arabic with its number, the downloaded translation under each
/// verse when there is one, and the reference. The text is only built when she shares.
struct SurahShareText: Transferable {
    let number: Int
    let name: String
    let meaning: String
    let arabicName: String
    let basmala: String?
    let ayahs: [String]
    let translation: [String]?
    let translator: String?

    @MainActor
    init(surah: Surah, quran: QuranStore, translations: TranslationStore) {
        number = surah.id
        name = surah.name
        meaning = surah.meaning
        arabicName = surah.arabicName
        basmala = surah.showsBasmala ? quran.basmala : nil
        ayahs = surah.ayahs
        if let all = translations.selectedVerses(), all.indices.contains(surah.id - 1),
           all[surah.id - 1].count == surah.ayahs.count {
            translation = all[surah.id - 1]
            translator = translations.selected.translator
        } else {
            translation = nil
            translator = nil
        }
    }

    var text: String {
        var parts = ["\(name) · \(meaning)\n\(arabicName)"]
        if let basmala { parts.append(basmala) }
        for (index, ayah) in ayahs.enumerated() {
            var verse = "\(ayah) ﴿\((index + 1).arabicIndic)﴾"
            if let translation { verse += "\n\(index + 1). \(translation[index])" }
            parts.append(verse)
        }
        var reference = "Quran \(number):1-\(ayahs.count) · \(name)"
        if let translator { reference += "\nTranslation: \(translator)" }
        parts.append(reference)
        return parts.joined(separator: "\n\n")
    }

    static var transferRepresentation: some TransferRepresentation {
        ProxyRepresentation(exporting: { (item: SurahShareText) in item.text })
    }
}
