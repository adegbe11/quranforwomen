import SwiftUI

/// The first screen: a quiet photograph of a woman reading the Quran, one verse,
/// the name, and one clear action. One orchestrated entrance, then stillness.
/// Get Started leaves in one move: the photo drifts up and fades, the words fade together,
/// and the button glides to where the next screen's Continue button sits, so it never jumps.
struct WelcomeView: View {
    var onGetStarted: () -> Void
    var onContinue: () -> Void

    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var showPhoto = false
    @State private var showVerse = false
    @State private var showTitle = false
    @State private var showActions = false
    @State private var leaving = false

    /// From the Get Started button down to the next step's footer button: the link below it
    /// (44), the gap between them (Space.m) and the padding difference (Space.s vs Space.m).
    private let glideToFooter: CGFloat = 44 + Space.m + Space.s - Space.m

    var body: some View {
        GeometryReader { geo in
            ZStack(alignment: .top) {
                Palette.background.ignoresSafeArea()

                photo(height: geo.size.height * 0.64 + geo.safeAreaInsets.top)

                VStack(spacing: 0) {
                    HStack(alignment: .top) {
                        verse
                            .opacity(showVerse ? 1 : 0)
                            .blur(radius: showVerse || reduceMotion ? 0 : 8)
                        Spacer(minLength: Space.m)
                    }
                    .padding(.horizontal, Space.xl)
                    .padding(.top, Space.xxl)
                    .opacity(leaving ? 0 : 1)

                    Spacer(minLength: 0)

                    VStack(spacing: Space.s) {
                        LotusMark()
                            .stroke(Palette.accentFill, style: StrokeStyle(lineWidth: 1.6, lineCap: .round, lineJoin: .round))
                            .frame(width: 48, height: 34)
                            .accessibilityHidden(true)
                        Text("Quran for Women")
                            .font(.system(size: 40, weight: .medium, design: .serif))
                            .foregroundStyle(Palette.accentFill)
                            .minimumScaleFactor(0.7)
                            .lineLimit(1)
                            .accessibilityAddTraits(.isHeader)
                        Text("Your Daily Companion to the Quran,\nDua & a Peaceful Heart")
                            .font(.system(.title3, design: .serif))
                            .foregroundStyle(Palette.textSecondary)
                            .multilineTextAlignment(.center)
                            .lineSpacing(2)
                    }
                    .padding(.horizontal, Space.xl)
                    .opacity(showTitle && !leaving ? 1 : 0)
                    .blur(radius: showTitle || reduceMotion ? 0 : 10)
                    .offset(y: showTitle || reduceMotion ? (leaving && !reduceMotion ? -10 : 0) : 8)

                    VStack(spacing: Space.m) {
                        Button(action: getStarted) {
                            HStack(spacing: Space.s) {
                                Text("Get Started")
                                Image(systemName: "arrow.right")
                                    .font(.headline)
                            }
                        }
                        .buttonStyle(.primary)
                        .offset(y: leaving && !reduceMotion ? glideToFooter : 0)
                        .disabled(leaving)

                        Button(action: onContinue) {
                            HStack(spacing: 4) {
                                Text("Already started?")
                                    .foregroundStyle(Palette.textSecondary)
                                Text("Continue")
                                    .foregroundStyle(Palette.accentFill)
                                    .fontWeight(.semibold)
                                    .underline()
                            }
                            .font(.subheadline)
                            .frame(minHeight: 44)
                        }
                        .buttonStyle(PressableStyle(scale: 0.97))
                        .accessibilityLabel("Already started? Continue")
                        .opacity(leaving ? 0 : 1)
                    }
                    .padding(.horizontal, Space.xl)
                    .padding(.top, Space.xxl)
                    .padding(.bottom, Space.s)
                    .opacity(showActions ? 1 : 0)
                    .offset(y: showActions || reduceMotion ? 0 : 12)
                }
            }
        }
        // The photograph is a warm, light scene, so this screen stays light.
        .environment(\.colorScheme, .light)
        .onAppear(perform: animateIn)
    }

    // MARK: Parts

    private func photo(height: CGFloat) -> some View {
        Image("WelcomePhoto")
            .resizable()
            .scaledToFill()
            .frame(maxWidth: .infinity)
            .frame(height: height, alignment: .top)
            .clipped()
            // A gentle settle, like a camera finding focus.
            .scaleEffect(showPhoto || reduceMotion ? 1 : 1.06)
            // A long, eased fade into the cream, so there is no edge on tall screens.
            .overlay(alignment: .bottom) {
                LinearGradient(
                    stops: [
                        .init(color: Palette.background.opacity(0), location: 0),
                        .init(color: Palette.background.opacity(0.35), location: 0.4),
                        .init(color: Palette.background.opacity(0.8), location: 0.75),
                        .init(color: Palette.background, location: 1),
                    ],
                    startPoint: .top, endPoint: .bottom
                )
                .frame(height: height * 0.34)
            }
            .opacity(showPhoto && !leaving ? 1 : 0)
            .offset(y: leaving && !reduceMotion ? -30 : 0)
            .ignoresSafeArea(edges: .top)
            .accessibilityHidden(true)
    }

    private var verse: some View {
        VStack(alignment: .leading, spacing: Space.s) {
            Text("\u{201C}Verily in the remembrance\nof Allah do hearts\nfind rest!\u{201D}")
                .font(.system(.title3, design: .serif, weight: .medium))
                .foregroundStyle(Color(hex: 0x12382B))
                .lineSpacing(3)
                .fixedSize(horizontal: false, vertical: true)
            Rectangle()
                .fill(Color(hex: 0x1F4A39).opacity(0.35))
                .frame(width: 40, height: 1)
            Text("Ar-Ra'd 13:28")
                .font(.footnote.weight(.medium))
                .foregroundStyle(Color(hex: 0x4F5B55))
        }
        // A soft light behind the words keeps them readable over any part of the photo.
        .background {
            RadialGradient(colors: [Palette.background.opacity(0.75), Palette.background.opacity(0)],
                           center: .leading, startRadius: 0, endRadius: 190)
                .padding(-28)
                .blur(radius: 14)
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel("Verily in the remembrance of Allah do hearts find rest. Quran, Ar-Ra'd, 13:28, Pickthall translation.")
    }

    /// Everything but the button leaves together, then the next step takes over.
    private func getStarted() {
        guard !leaving else { return }
        withAnimation(.easeInOut(duration: reduceMotion ? 0.2 : 0.35)) { leaving = true }
        DispatchQueue.main.asyncAfter(deadline: .now() + (reduceMotion ? 0.2 : 0.35)) { onGetStarted() }
    }

    /// Photo settles, the verse comes into focus, then the name, then the action.
    private func animateIn() {
        guard !showActions else { return }
        if reduceMotion {
            withAnimation(.easeOut(duration: 0.4)) {
                showPhoto = true; showVerse = true; showTitle = true; showActions = true
            }
            return
        }
        withAnimation(.easeOut(duration: 1.1)) { showPhoto = true }
        withAnimation(Motion.entrance(delay: 0.3)) { showVerse = true }
        withAnimation(Motion.entrance(delay: 0.5)) { showTitle = true }
        withAnimation(Motion.entrance(delay: 0.75)) { showActions = true }
    }
}


/// A simple three petal lotus, drawn as a line mark.
struct LotusMark: Shape {
    func path(in rect: CGRect) -> Path {
        let w = rect.width, h = rect.height
        let base = CGPoint(x: rect.midX, y: rect.maxY - h * 0.08)
        var p = Path()
        // Centre petal
        p.move(to: base)
        p.addQuadCurve(to: CGPoint(x: rect.midX, y: rect.minY), control: CGPoint(x: rect.midX - w * 0.2, y: rect.minY + h * 0.45))
        p.addQuadCurve(to: base, control: CGPoint(x: rect.midX + w * 0.2, y: rect.minY + h * 0.45))
        // Left petal
        p.move(to: base)
        p.addQuadCurve(to: CGPoint(x: rect.minX + w * 0.06, y: rect.minY + h * 0.38), control: CGPoint(x: rect.minX + w * 0.12, y: rect.maxY - h * 0.08))
        p.addQuadCurve(to: base, control: CGPoint(x: rect.midX - w * 0.14, y: rect.minY + h * 0.42))
        // Right petal
        p.move(to: base)
        p.addQuadCurve(to: CGPoint(x: rect.maxX - w * 0.06, y: rect.minY + h * 0.38), control: CGPoint(x: rect.maxX - w * 0.12, y: rect.maxY - h * 0.08))
        p.addQuadCurve(to: base, control: CGPoint(x: rect.midX + w * 0.14, y: rect.minY + h * 0.42))
        // Water line
        p.move(to: CGPoint(x: rect.minX + w * 0.18, y: rect.maxY))
        p.addQuadCurve(to: CGPoint(x: rect.maxX - w * 0.18, y: rect.maxY), control: CGPoint(x: rect.midX, y: rect.maxY - h * 0.1))
        return p
    }
}
