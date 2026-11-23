import Foundation

/// Keys for the recitation extras. Same names on Android.
enum PlaybackPrefKey {
    static let speed = "audio.speed"
    static let showQuranTransliteration = "reader.showQuranTransliteration"
}

/// Recitation speeds she can pick.
enum PlaybackSpeed {
    static let choices: [Float] = [0.75, 1, 1.25, 1.5]

    /// The saved speed, or normal speed.
    static var saved: Float {
        let value = Float(UserDefaults.standard.double(forKey: PlaybackPrefKey.speed))
        return choices.contains(value) ? value : 1
    }

    static func label(_ value: Float) -> String {
        switch value {
        case 0.75: "0.75x"
        case 1: "1x"
        case 1.25: "1.25x"
        case 1.5: "1.5x"
        default: "\(value)x"
        }
    }
}

/// Pauses recitation after a while, or once the surah ends.
enum SleepTimer: Hashable {
    case minutes(Int)
    case endOfSurah

    static let choices: [SleepTimer] = [.minutes(15), .minutes(30), .minutes(45), .minutes(60), .endOfSurah]

    var title: String {
        switch self {
        case .minutes(let m): m == 60 ? "1 hour" : "\(m) minutes"
        case .endOfSurah: "End of surah"
        }
    }
}
