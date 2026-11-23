import SwiftUI
import UIKit

// MARK: - Color tokens

extension UIColor {
    convenience init(hex: UInt32, alpha: CGFloat = 1) {
        self.init(
            red: CGFloat((hex >> 16) & 0xFF) / 255,
            green: CGFloat((hex >> 8) & 0xFF) / 255,
            blue: CGFloat(hex & 0xFF) / 255,
            alpha: alpha
        )
    }
}

extension Color {
    /// A color that resolves differently in light and dark appearance.
    init(light: UInt32, dark: UInt32) {
        self.init(uiColor: UIColor { traits in
            UIColor(hex: traits.userInterfaceStyle == .dark ? dark : light)
        })
    }
}

/// Semantic colors. Views use these names, never raw hex values.
///
/// Brand: Deep Emerald #064E3B, Blush #F5DCE3, Warm Cream #FAF7F0.
/// Dark values are tuned so text keeps at least 4.5:1 contrast.
enum Palette {
    /// Emerald for icons, links and selected states drawn on the background.
    static let accent = Color(light: 0x064E3B, dark: 0x7FD1AE)
    /// Emerald for filled buttons.
    static let accentFill = Color(light: 0x064E3B, dark: 0x0F6B50)
    static let onAccentFill = Color.white
    /// A quiet emerald wash for tinted buttons and selected rows.
    static let accentWash = Color(light: 0xE4EEE9, dark: 0x16271F)

    /// Blush, used sparingly for warmth and highlights.
    static let blush = Color(light: 0xF5DCE3, dark: 0x3D2830)
    static let blushStrong = Color(light: 0xB24D6B, dark: 0xE9A9BC)

    static let background = Color(light: 0xFAF7F0, dark: 0x0E1311)
    static let surface = Color(light: 0xFFFFFF, dark: 0x171E1B)
    static let surfaceSunken = Color(light: 0xF3EFE5, dark: 0x121816)

    static let textPrimary = Color(light: 0x17251F, dark: 0xEDEAE2)
    static let textSecondary = Color(light: 0x5F6D66, dark: 0xA3ADA7)
    static let border = Color(light: 0xE7E5DD, dark: 0x27302C)

    /// Paper for the welcome illustration. Stays warm in both appearances.
    static let paper = Color(light: 0xFFFDF7, dark: 0xE9E3D4)
    static let paperShade = Color(light: 0xEDE5D3, dark: 0xCFC6B2)
    static let ink = Color(hex: 0x1F2A24)
}

extension Color {
    init(hex: UInt32) { self.init(uiColor: UIColor(hex: hex)) }
}

// MARK: - Spacing and shape

/// The book: the colours of PrayerKey's Bible, used wherever she reads the Quran itself.
/// Light is the classic edition (cream leaves, brown ink, gilt, a red rubric for verse numbers).
/// Dark is the black edition (near black leaves, soft white ink, silver), so reading at night stays calm.
enum BookInk {
    static let page = Color(light: 0xF6EFDC, dark: 0x1B1B1D)
    static let pageLit = Color(light: 0xFDFAF0, dark: 0x232325)
    static let pageEdge = Color(light: 0xE6D9B6, dark: 0x111113)
    static let ink = Color(light: 0x221C12, dark: 0xEDEDED)
    static let caption = Color(light: 0x5A4828, dark: 0xAEAEB2)
    static let gild = Color(light: 0xC9A26D, dark: 0xBDBDC2)
    static let rule = Color(light: 0x8C6E32, dark: 0x8E8E93)
    static let rubric = Color(light: 0x9A1B2B, dark: 0xB9B9BE)
    static let ribbon = Color(light: 0x8C1F2B, dark: 0xD1D1D6)
    static let glow = Color(light: 0xF6ECCF, dark: 0x3A3A3C)

    /// The lit leaf, brighter at the spine and deeper at the edge.
    static var leaf: LinearGradient {
        LinearGradient(colors: [pageLit, page, pageEdge], startPoint: .trailing, endPoint: .leading)
    }
}

enum Space {
    static let xxs: CGFloat = 4
    static let xs: CGFloat = 8
    static let s: CGFloat = 12
    static let m: CGFloat = 16
    static let l: CGFloat = 20
    static let xl: CGFloat = 24
    static let xxl: CGFloat = 32
    static let xxxl: CGFloat = 48
    /// Standard horizontal page margin.
    static let page: CGFloat = 20
}

enum Radius {
    // Slightly tighter radii, in line with iOS 27 reining in overly round corners.
    static let card: CGFloat = 20
    static let tile: CGFloat = 16
    static let control: CGFloat = 12
}

// MARK: - Typography

enum AppFont {
    /// Amiri Quran (SIL Open Font License), made for Quranic text with full diacritics.
    static let arabicFamily = "Amiri Quran"

    static func arabic(_ size: CGFloat, relativeTo style: Font.TextStyle = .title) -> Font {
        .custom(arabicFamily, size: size, relativeTo: style)
    }

    /// Editorial serif (New York) for translations and a few headlines.
    static func serif(_ style: Font.TextStyle, weight: Font.Weight = .regular) -> Font {
        .system(style, design: .serif, weight: weight)
    }

    static let eyebrow = Font.system(.caption, design: .default, weight: .semibold)
}

// MARK: - Motion

/// Motion rules, from Apple's fluid interface guidance:
/// springs for anything she touches (interruptible, carries her momentum),
/// easing only for the app announcing something, entrances under 0.6 seconds.
enum Motion {
    /// Press feedback: quick, no bounce.
    static let press = Animation.snappy(duration: 0.22)
    /// Things she toggles or moves.
    static let gentle = Animation.smooth(duration: 0.4)
    static let smooth = Animation.smooth(duration: 0.5)
    /// A small, earned overshoot, for moments of completion.
    static let reward = Animation.spring(response: 0.45, dampingFraction: 0.68)
    /// Entrances: fast start, long soft landing (expo out).
    static func entrance(delay: Double = 0) -> Animation {
        .timingCurve(0.16, 1, 0.3, 1, duration: 0.6).delay(delay)
    }
}

/// Zoom transitions between a tapped item and the screen it opens (iOS 18 and later).
private struct ZoomNamespaceKey: EnvironmentKey {
    static let defaultValue: Namespace.ID? = nil
}

extension EnvironmentValues {
    var zoomNamespace: Namespace.ID? {
        get { self[ZoomNamespaceKey.self] }
        set { self[ZoomNamespaceKey.self] = newValue }
    }
}

private struct ZoomSource: ViewModifier {
    let id: String
    @Environment(\.zoomNamespace) private var namespace

    func body(content: Content) -> some View {
        if #available(iOS 18.0, *) {
            if let namespace {
                content.matchedTransitionSource(id: id, in: namespace)
            } else {
                content
            }
        } else {
            content
        }
    }
}

private struct ZoomDestination: ViewModifier {
    let id: String
    @Environment(\.zoomNamespace) private var namespace

    func body(content: Content) -> some View {
        if #available(iOS 18.0, *) {
            if let namespace {
                content.navigationTransition(.zoom(sourceID: id, in: namespace))
            } else {
                content
            }
        } else {
            content
        }
    }
}

extension View {
    /// Marks the view that a screen zooms out of.
    func zoomSource(_ id: String) -> some View { modifier(ZoomSource(id: id)) }
    /// Makes a pushed screen zoom from its source.
    func zoomDestination(_ id: String) -> some View { modifier(ZoomDestination(id: id)) }
}

enum ZoomID {
    static func reader(_ surah: Int, _ verse: Int?) -> String { "reader-\(surah)-\(verse ?? 0)" }
    static func dua(_ id: String) -> String { "dua-\(id)" }
    static func entry(_ id: UUID) -> String { "entry-\(id.uuidString)" }
    static func note(_ id: UUID) -> String { "note-\(id.uuidString)" }
    static func collection(_ id: String) -> String { "collection-\(id)" }
    static let dhikr = "dhikr"
    static let personalDuas = "personalDuas"
    static let savedAyahs = "savedAyahs"
    static let savedDuas = "savedDuas"
    static let annotations = "annotations"
    static let readingProgress = "readingProgress"
    static let reminder = "reminder"
    static let language = "language"
    static let readingSettings = "readingSettings"
    static let downloads = "downloads"
    static let about = "about"
    static let quiz = "quiz"
    static let readingPlan = "readingPlan"
    static let prayerTimes = "prayerTimes"
    static let qibla = "qibla"
    static let prayerSettings = "prayerSettings"
    static let sync = "sync"
    static let journalTemplates = "journalTemplates"
    static let journalPrompts = "journalPrompts"
    static let journalSettings = "journalSettings"
    static let feelings = "feelings"
    static let explore = "explore"
    static let settings = "settings"
    static let mushaf = "mushaf"
    static func duaCategory(_ id: String) -> String { "duaCategory-\(id)" }
    static func templateCollection(_ id: String) -> String { "templateCollection-\(id)" }
    static func promptPack(_ id: String) -> String { "promptPack-\(id)" }
}

// MARK: - Helpers

extension Int {
    /// Arabic-Indic digits, used for verse numbers inside Arabic text.
    var arabicIndic: String {
        String(String(self).map { ch -> Character in
            guard let d = ch.wholeNumberValue, let scalar = Unicode.Scalar(UInt32(0x0660 + d)) else { return ch }
            return Character(scalar)
        })
    }
}
