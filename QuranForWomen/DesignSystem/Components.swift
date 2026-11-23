import SwiftUI

// MARK: - Button styles

/// The one main action on a screen. Large, calm, emerald.
struct PrimaryButtonStyle: ButtonStyle {
    @Environment(\.isEnabled) private var isEnabled
    @Environment(\.inBook) private var inBook

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.headline)
            .foregroundStyle(inBook ? BookInk.pageLit : Palette.onAccentFill)
            .frame(maxWidth: .infinity, minHeight: 56)
            .padding(.horizontal, Space.l)
            .background(Capsule().fill(inBook ? BookInk.rule : Palette.accentFill))
            .contentShape(Capsule())
            .opacity(isEnabled ? (configuration.isPressed ? 0.88 : 1) : 0.4)
            .scaleEffect(configuration.isPressed ? 0.98 : 1)
            .animation(Motion.press, value: configuration.isPressed)
    }
}

/// A softer action that sits beside or below a primary action.
struct TintedButtonStyle: ButtonStyle {
    var fullWidth = true
    @Environment(\.inBook) private var inBook

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.headline)
            .foregroundStyle(inBook ? BookInk.caption : Palette.accent)
            .frame(maxWidth: fullWidth ? .infinity : nil, minHeight: 50)
            .padding(.horizontal, Space.l)
            .background(Capsule().fill(inBook ? BookInk.gild.opacity(0.2) : Palette.accentWash))
            .contentShape(Capsule())
            .opacity(configuration.isPressed ? 0.8 : 1)
            .scaleEffect(configuration.isPressed ? 0.98 : 1)
            .animation(Motion.press, value: configuration.isPressed)
    }
}

/// For whole tappable cards and tiles: a quiet press response, nothing more.
struct PressableStyle: ButtonStyle {
    var scale: CGFloat = 0.985

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed ? scale : 1)
            .opacity(configuration.isPressed ? 0.92 : 1)
            .animation(Motion.press, value: configuration.isPressed)
    }
}

extension ButtonStyle where Self == PrimaryButtonStyle {
    static var primary: PrimaryButtonStyle { PrimaryButtonStyle() }
}

extension ButtonStyle where Self == TintedButtonStyle {
    static var tinted: TintedButtonStyle { TintedButtonStyle() }
    static var tintedCompact: TintedButtonStyle { TintedButtonStyle(fullWidth: false) }
}

extension ButtonStyle where Self == PressableStyle {
    static var pressable: PressableStyle { PressableStyle() }
}

// MARK: - Surfaces

struct CardSurface: ViewModifier {
    var padding: CGFloat = Space.l
    /// Nil means the surface of the page it sits on.
    var fill: Color?
    @Environment(\.inBook) private var inBook

    func body(content: Content) -> some View {
        content
            .padding(padding)
            .frame(maxWidth: .infinity, alignment: .leading)
            // No border: like iOS, surfaces separate by tone and space alone.
            // In a book, a fine gilt rule edges the leaf, as on a printed page.
            .background(RoundedRectangle(cornerRadius: Radius.card, style: .continuous).fill(fill ?? (inBook ? BookInk.pageLit : Palette.surface)))
            .overlay {
                if inBook {
                    RoundedRectangle(cornerRadius: Radius.card, style: .continuous)
                        .strokeBorder(BookInk.gild.opacity(0.35), lineWidth: 0.8)
                }
            }
            .contentShape(RoundedRectangle(cornerRadius: Radius.card, style: .continuous))
    }
}

extension View {
    func cardSurface(padding: CGFloat = Space.l, fill: Color? = nil) -> some View {
        modifier(CardSurface(padding: padding, fill: fill))
    }

    /// Warm cream page background that also fills behind lists.
    func pageBackground() -> some View {
        background(Palette.background.ignoresSafeArea())
    }

    /// A screen set like a book, in the colours of PrayerKey's Bible: the lit leaf behind it,
    /// and every card, list, row, label and button on it drawn in book colours.
    /// Light is the classic edition; dark is the black edition.
    func bookPage() -> some View {
        self
            .environment(\.inBook, true)
            .tint(BookInk.rule)
            .background(BookInk.leaf.ignoresSafeArea())
    }
}

private struct InBookKey: EnvironmentKey {
    static let defaultValue = false
}

extension EnvironmentValues {
    /// True on screens set like a book, see `bookPage()`.
    var inBook: Bool {
        get { self[InBookKey.self] }
        set { self[InBookKey.self] = newValue }
    }
}

// MARK: - Text building blocks

/// A small uppercase label above a card's content.
struct Eyebrow: View {
    let text: String
    var color: Color?
    @Environment(\.inBook) private var inBook

    var body: some View {
        Text(text.uppercased())
            .font(AppFont.eyebrow)
            .tracking(0.8)
            .foregroundStyle(color ?? (inBook ? BookInk.rubric : Palette.accent))
            .accessibilityAddTraits(.isHeader)
    }
}

struct SectionHeader: View {
    let title: String
    var subtitle: String?
    @Environment(\.inBook) private var inBook

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(title)
                .font(.title3.weight(.semibold))
                .foregroundStyle(inBook ? BookInk.ink : Palette.textPrimary)
            if let subtitle {
                Text(subtitle)
                    .font(.subheadline)
                    .foregroundStyle(inBook ? BookInk.caption : Palette.textSecondary)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .combine)
        .accessibilityAddTraits(.isHeader)
    }
}

/// Arabic scripture or supplication. Always right to left, always the Quran font.
struct ArabicText: View {
    enum Placement { case start, center }

    let text: String
    var size: CGFloat = 28
    var placement: Placement = .start
    var color: Color?
    var lineLimit: Int?

    @Environment(\.inBook) private var inBook

    var body: some View {
        Text(attributed)
            .font(AppFont.arabic(size))
            .foregroundStyle(color ?? (inBook ? BookInk.ink : Palette.textPrimary))
            .lineSpacing(size * 0.32)
            .lineLimit(lineLimit)
            .multilineTextAlignment(placement == .center ? .center : .leading)
            .frame(maxWidth: .infinity, alignment: placement == .center ? .center : .leading)
            .fixedSize(horizontal: false, vertical: true)
            // In a right to left environment, "leading" means the right edge.
            .environment(\.layoutDirection, .rightToLeft)
    }

    private var attributed: AttributedString {
        var value = AttributedString(text)
        value.languageIdentifier = "ar"
        return value
    }
}

/// Translation text in the editorial serif.
struct TranslationText: View {
    let text: String
    var style: Font.TextStyle = .body
    var size: CGFloat?
    var color: Color?
    var rightToLeft = false

    /// Keeps a chosen reading size responsive to Dynamic Type.
    @ScaledMetric(relativeTo: .body) private var scale: CGFloat = 1
    @Environment(\.inBook) private var inBook

    var body: some View {
        Text(text)
            .font(size.map { Font.system(size: $0 * scale, design: .serif) } ?? AppFont.serif(style))
            .foregroundStyle(color ?? (inBook ? BookInk.ink : Palette.textPrimary))
            .lineSpacing(4)
            .multilineTextAlignment(.leading)
            .frame(maxWidth: .infinity, alignment: .leading)
            .fixedSize(horizontal: false, vertical: true)
            .environment(\.layoutDirection, rightToLeft ? .rightToLeft : .leftToRight)
    }
}

/// Shows where a text comes from. Quran and Sunnah look different on purpose.
struct SourceLabel: View {
    enum Kind { case quran, hadith, personal }

    let kind: Kind
    let reference: String
    @Environment(\.inBook) private var inBook

    var body: some View {
        HStack(spacing: 6) {
            Image(systemName: symbol)
                .imageScale(.small)
            Text(reference)
        }
        .font(.footnote.weight(.medium))
        .foregroundStyle(foreground)
        .padding(.horizontal, 10)
        .padding(.vertical, 5)
        .background(Capsule().fill(background))
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("Source: \(reference)")
    }

    private var foreground: Color {
        if inBook { return kind == .personal ? BookInk.ribbon : BookInk.caption }
        return kind == .personal ? Palette.blushStrong : Palette.accent
    }

    private var background: Color {
        if inBook { return kind == .personal ? BookInk.ribbon.opacity(0.12) : BookInk.gild.opacity(0.2) }
        return kind == .personal ? Palette.blush : Palette.accentWash
    }

    private var symbol: String {
        switch kind {
        case .quran: "book.closed"
        case .hadith: "text.quote"
        case .personal: "pencil"
        }
    }
}

// MARK: - Small controls

/// A round icon button used for Listen and Save style actions.
struct CircleIconButton: View {
    let systemImage: String
    let label: String
    var isActive = false
    let action: () -> Void

    @Environment(\.inBook) private var inBook

    var body: some View {
        Button(action: action) {
            Image(systemName: systemImage)
                .font(.system(size: 17, weight: .medium))
                .contentTransition(.symbolEffect(.replace))
                .foregroundStyle(inBook ? (isActive ? BookInk.ribbon : BookInk.rule) : Palette.accent)
                .frame(width: 44, height: 44)
                .background(Circle().fill(inBook
                    ? (isActive ? BookInk.ribbon.opacity(0.14) : BookInk.gild.opacity(0.2))
                    : (isActive ? Palette.blush : Palette.accentWash)))
        }
        .buttonStyle(PressableStyle(scale: 0.92))
        .accessibilityLabel(label)
    }
}

/// Bookmark toggle. Saving bounces the bookmark and sends out a few petals; removing is quiet.
struct SaveButton: View {
    let isSaved: Bool
    var savedLabel = "Saved"
    var unsavedLabel = "Save"
    let action: () -> Void

    @AppStorage(PrefKey.haptics) private var hapticsOn = true

    var body: some View {
        CircleIconButton(
            systemImage: isSaved ? "bookmark.fill" : "bookmark",
            label: isSaved ? savedLabel : unsavedLabel,
            isActive: isSaved,
            action: action
        )
        .symbolEffect(.bounce, value: isSaved)
        .overlay { PetalBurst(trigger: isSaved, active: isSaved) }
        .sensoryFeedback(.selection, trigger: isSaved) { _, _ in hapticsOn }
    }
}

/// A list row icon in brand emerald.
struct RowIcon: View {
    let systemImage: String
    @Environment(\.inBook) private var inBook

    var body: some View {
        Image(systemName: systemImage)
            .font(.system(size: 15, weight: .medium))
            .foregroundStyle(inBook ? BookInk.rule : Palette.accent)
            .frame(width: 30, height: 30)
            .background(RoundedRectangle(cornerRadius: 8, style: .continuous).fill(inBook ? BookInk.gild.opacity(0.2) : Palette.accentWash))
            .accessibilityHidden(true)
    }
}

// MARK: - Grouped lists

/// An inset grouped list on the cream page, the way iOS Settings and Health do it.
struct GroupedSection<Content: View>: View {
    var title: String?
    var footer: String?
    @ViewBuilder var content: Content

    @Environment(\.inBook) private var inBook

    var body: some View {
        VStack(alignment: .leading, spacing: Space.xs) {
            if let title {
                Text(title)
                    .font(.title3.weight(.semibold))
                    .foregroundStyle(inBook ? BookInk.ink : Palette.textPrimary)
                    .padding(.bottom, Space.xxs)
                    .accessibilityAddTraits(.isHeader)
            }
            VStack(spacing: 0) {
                content
            }
            .buttonStyle(RowPressStyle())
            .background(RoundedRectangle(cornerRadius: Radius.tile, style: .continuous).fill(inBook ? BookInk.pageLit : Palette.surface))
            .overlay {
                if inBook {
                    RoundedRectangle(cornerRadius: Radius.tile, style: .continuous)
                        .strokeBorder(BookInk.gild.opacity(0.35), lineWidth: 0.8)
                }
            }
            .clipShape(RoundedRectangle(cornerRadius: Radius.tile, style: .continuous))
            if let footer {
                Text(footer)
                    .font(.footnote)
                    .foregroundStyle(inBook ? BookInk.caption : Palette.textSecondary)
                    .padding(.horizontal, Space.m)
            }
        }
    }
}

/// A list row: optional symbol, title, optional subtitle and value, and a chevron.
struct ListRow: View {
    var symbol: String?
    let title: String
    var subtitle: String?
    var value: String?
    var showsChevron = true
    /// When set, the icon gives one small bounce this long after the row appears.
    var landDelay: Double?

    @Environment(\.inBook) private var inBook

    var body: some View {
        HStack(spacing: Space.m) {
            if let symbol {
                Image(systemName: symbol)
                    .font(.system(size: 17, weight: .regular))
                    .foregroundStyle(inBook ? BookInk.rule : Palette.accent)
                    .frame(width: 26)
                    .modifier(RowIconLand(delay: landDelay))
                    .accessibilityHidden(true)
            }
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.body)
                    .foregroundStyle(inBook ? BookInk.ink : Palette.textPrimary)
                if let subtitle {
                    Text(subtitle)
                        .font(.subheadline)
                        .foregroundStyle(inBook ? BookInk.caption : Palette.textSecondary)
                        .lineLimit(2)
                }
            }
            .multilineTextAlignment(.leading)
            Spacer(minLength: Space.xs)
            if let value {
                Text(value)
                    .font(.body)
                    .foregroundStyle(inBook ? BookInk.caption : Palette.textSecondary)
            }
            if showsChevron {
                Image(systemName: "chevron.right")
                    .font(.footnote.weight(.semibold))
                    .foregroundStyle((inBook ? BookInk.caption : Palette.textSecondary).opacity(0.5))
                    .accessibilityHidden(true)
            }
        }
        .padding(.horizontal, Space.m)
        .padding(.vertical, 12)
        .frame(minHeight: 48)
        .contentShape(Rectangle())
        .accessibilityElement(children: .combine)
    }
}

// MARK: - iOS 27

extension View {
    /// On iOS 27 and later the navigation bar shrinks while she scrolls down a long page,
    /// like Apple's own reading apps. The guard keeps the project building with Xcode 26,
    /// whose SDK does not have this call; earlier systems keep the bar as it is.
    @ViewBuilder
    func minimizesNavigationBarOnScroll() -> some View {
        #if compiler(>=6.4)
        if #available(iOS 27.0, *) {
            self.toolbarMinimizeBehavior(.onScrollDown, for: .navigationBar)
        } else {
            self
        }
        #else
        self
        #endif
    }

    /// Keeps reading text at a comfortable line length when the window is wide, such as an
    /// unfolded iPhone Duo or a resized iPhone app on iOS 27. On a normal iPhone it changes nothing.
    func readableWidth(_ maxWidth: CGFloat = 640) -> some View {
        frame(maxWidth: maxWidth).frame(maxWidth: .infinity)
    }
}

/// One small bounce of a row icon after a delay. Does nothing without a delay or with Reduce Motion.
private struct RowIconLand: ViewModifier {
    let delay: Double?
    @State private var landed = false
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    func body(content: Content) -> some View {
        content
            .symbolEffect(.bounce, value: landed)
            .task {
                guard let delay, !reduceMotion, !landed else { return }
                try? await Task.sleep(for: .seconds(delay))
                guard !Task.isCancelled else { return }
                landed = true
            }
    }
}

/// Hairline between rows, inset like iOS.
struct RowDivider: View {
    var inset: CGFloat = 58
    @Environment(\.inBook) private var inBook

    var body: some View {
        Rectangle()
            .fill(inBook ? BookInk.rule.opacity(0.25) : Palette.border)
            .frame(height: 0.5)
            .padding(.leading, inset)
    }
}

/// Rows highlight softly while pressed, like a native list cell.
struct RowPressStyle: ButtonStyle {
    @Environment(\.inBook) private var inBook

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .background(configuration.isPressed ? (inBook ? BookInk.gild.opacity(0.18) : Palette.accentWash.opacity(0.7)) : Color.clear)
    }
}

// MARK: - Entrance motion

/// Content arrives one piece after another, like cards being dealt, then settles.
struct DealIn: ViewModifier {
    let index: Int
    var baseDelay: Double = 0.15
    var step: Double = 0.07
    @State private var shown = false
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    func body(content: Content) -> some View {
        content
            .opacity(shown ? 1 : 0)
            .offset(y: shown || reduceMotion ? 0 : 24)
            .scaleEffect(shown || reduceMotion ? 1 : 0.97, anchor: .top)
            .onAppear {
                guard !shown else { return }
                withAnimation(.spring(response: 0.6, dampingFraction: 0.82).delay(baseDelay + Double(index) * step)) {
                    shown = true
                }
            }
    }
}

/// Comes into focus: fades in from a soft blur.
struct FocusIn: ViewModifier {
    var delay: Double = 0
    @State private var shown = false
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    func body(content: Content) -> some View {
        content
            .opacity(shown ? 1 : 0)
            .blur(radius: shown || reduceMotion ? 0 : 10)
            .offset(y: shown || reduceMotion ? 0 : 6)
            .onAppear {
                guard !shown else { return }
                withAnimation(Motion.entrance(delay: delay)) { shown = true }
            }
    }
}

extension View {
    func dealIn(_ index: Int, baseDelay: Double = 0.15, step: Double = 0.07) -> some View {
        modifier(DealIn(index: index, baseDelay: baseDelay, step: step))
    }

    func focusIn(delay: Double = 0) -> some View {
        modifier(FocusIn(delay: delay))
    }

    /// Cards ease in as they scroll into view, and soften as they leave.
    func scrollSettle() -> some View {
        scrollTransition(.interactive, axis: .vertical) { view, phase in
            view
                .scaleEffect(phase.isIdentity ? 1 : 0.95)
                .opacity(phase.isIdentity ? 1 : 0.55)
        }
    }
}

// MARK: - Glass (iOS 26 and later), readable at any glass setting

/// Liquid Glass with a light tint behind the content, so text stays readable whether
/// she sets glass to fully clear or fully tinted (iOS 27). With Reduce Transparency on,
/// it becomes a solid surface.
struct ReadableGlass<S: Shape>: ViewModifier {
    let shape: S
    /// True when the glass sits on something dark, such as the night sky.
    var onDark = false
    var tint: Color?
    var interactive = true

    @Environment(\.accessibilityReduceTransparency) private var reduceTransparency

    func body(content: Content) -> some View {
        if reduceTransparency {
            content.background(shape.fill(tint ?? (onDark ? Color(hex: 0x1B2A3A) : Color.white)))
        } else {
            if #available(iOS 26.0, *) {
                content.glassEffect(glass, in: shape)
            } else {
                content.background(shape.fill(tint.map { AnyShapeStyle($0) } ?? AnyShapeStyle(.ultraThinMaterial)))
            }
        }
    }

    @available(iOS 26.0, *)
    private var glass: Glass {
        let base = Glass.regular.tint(tint ?? (onDark ? Color.black.opacity(0.18) : Color.white.opacity(0.35)))
        return interactive ? base.interactive() : base
    }
}

extension View {
    func readableGlass<S: Shape>(_ shape: S, onDark: Bool = false, tint: Color? = nil, interactive: Bool = true) -> some View {
        modifier(ReadableGlass(shape: shape, onDark: onDark, tint: tint, interactive: interactive))
    }
}

// MARK: - Arabic reveal

/// Reveals Arabic from right to left, the direction it is written, with a soft leading edge.
struct InkReveal: ViewModifier {
    var delay: Double = 0
    var duration: Double = 1.1
    @State private var progress: CGFloat = 0
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    func body(content: Content) -> some View {
        content
            .mask {
                GeometryReader { geo in
                    let width = geo.size.width
                    let edge: CGFloat = 60
                    LinearGradient(
                        stops: [
                            .init(color: .clear, location: 0),
                            .init(color: .black, location: edge / (width + edge)),
                            .init(color: .black, location: 1),
                        ],
                        startPoint: .leading, endPoint: .trailing
                    )
                    .frame(width: width + edge)
                    // Slides in from the right edge, uncovering the line as Arabic is read.
                    .offset(x: width - (width + edge) * progress)
                }
            }
            .onAppear {
                guard progress == 0 else { return }
                if reduceMotion {
                    progress = 1
                } else {
                    withAnimation(.easeInOut(duration: duration).delay(delay)) { progress = 1 }
                }
            }
    }
}

extension View {
    /// Reveals Arabic right to left, as it is written.
    func inkReveal(delay: Double = 0, duration: Double = 1.1) -> some View {
        modifier(InkReveal(delay: delay, duration: duration))
    }
}
