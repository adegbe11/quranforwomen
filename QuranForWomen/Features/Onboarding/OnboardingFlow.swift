import SwiftUI

/// Welcome, language, daily moment, then a small celebration. No sign up, no survey.
/// Each step leaves with a soft blur and the next one arrives with staggered springs.
struct OnboardingFlow: View {
    var onFinish: () -> Void

    enum Step: Int, Comparable {
        case welcome, language, moment, ready
        static func < (a: Step, b: Step) -> Bool { a.rawValue < b.rawValue }
    }

    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var step: Step = .welcome
    @State private var forward = true
    /// Welcome to Language is a crossfade, so the button that glided down stays put.
    @State private var handoff = false
    @State private var language = TranslationOption.suggested().id
    @State private var moment: ReminderMoment?
    @State private var reminderOn = false

    var body: some View {
        ZStack {
            AmbientBackground()

            switch step {
            case .welcome:
                WelcomeView(onGetStarted: { go(.language) }, onContinue: onFinish)
                    .transition(stepTransition)
            case .language:
                StepScaffold(step: 1, onBack: { go(.welcome) }) {
                    LanguageStep(selection: $language)
                } footer: {
                    Button { go(.moment) } label: {
                        HStack(spacing: Space.s) { Text("Continue"); Image(systemName: "arrow.right") }
                    }
                    .buttonStyle(.primary)
                }
                .transition(stepTransition)
            case .moment:
                StepScaffold(step: 2, onBack: { go(.language) }) {
                    MomentStep(choice: $moment)
                } footer: {
                    MomentButton(choice: moment) { enabled in
                        reminderOn = enabled
                        go(.ready)
                    }
                }
                .transition(stepTransition)
            case .ready:
                ReadyStep(languageID: language, moment: moment, reminderOn: reminderOn, onBegin: onFinish)
                    .transition(stepTransition)
            }
        }
        .environment(\.colorScheme, .light)
    }

    private var stepTransition: AnyTransition {
        if reduceMotion || handoff { return .opacity }
        return .asymmetric(
            insertion: .move(edge: forward ? .trailing : .leading).combined(with: .opacity).combined(with: .blurred),
            removal: .move(edge: forward ? .leading : .trailing).combined(with: .opacity).combined(with: .blurred)
        )
    }

    private func go(_ next: Step) {
        // Set the direction first, so the leaving step also slides the right way.
        forward = next > step
        handoff = (step == .welcome && next == .language) || (step == .language && next == .welcome)
        DispatchQueue.main.async {
            withAnimation(reduceMotion ? .easeInOut(duration: 0.25) : .spring(response: 0.55, dampingFraction: 0.86)) {
                step = next
            }
        }
    }
}

// MARK: - Shared pieces

private struct BlurModifier: ViewModifier {
    let radius: CGFloat
    func body(content: Content) -> some View { content.blur(radius: radius) }
}

extension AnyTransition {
    /// Comes into focus on the way in, softens on the way out.
    static var blurred: AnyTransition {
        .modifier(active: BlurModifier(radius: 14), identity: BlurModifier(radius: 0))
    }
}

/// A warm cream page with a very slow blush and emerald light drifting across it.
private struct AmbientBackground: View {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        TimelineView(.animation(minimumInterval: 1.0 / 20, paused: reduceMotion)) { context in
            let t = reduceMotion ? 0 : context.date.timeIntervalSinceReferenceDate
            ZStack {
                Palette.background
                Circle()
                    .fill(Color(hex: 0xF5DCE3).opacity(0.55))
                    .frame(width: 420, height: 420)
                    .blur(radius: 90)
                    .offset(x: 120 + 30 * sin(t / 7), y: -260 + 25 * cos(t / 9))
                Circle()
                    .fill(Color(hex: 0xDCEDE5).opacity(0.6))
                    .frame(width: 380, height: 380)
                    .blur(radius: 90)
                    .offset(x: -150 + 25 * cos(t / 8), y: 300 + 30 * sin(t / 6))
            }
            .ignoresSafeArea()
        }
    }
}

/// Back button, progress, a scrolling body and a pinned footer.
private struct StepScaffold<Content: View, Footer: View>: View {
    let step: Int
    let onBack: () -> Void
    @ViewBuilder var content: Content
    @ViewBuilder var footer: Footer

    var body: some View {
        VStack(spacing: 0) {
            HStack(spacing: Space.m) {
                Button(action: onBack) {
                    Image(systemName: "chevron.left")
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundStyle(Palette.accentFill)
                        .frame(width: 40, height: 40)
                        .glassCircle()
                }
                .buttonStyle(PressableStyle(scale: 0.92))
                .accessibilityLabel("Back")
                ProgressSegments(current: step, total: 2)
            }
            .padding(.horizontal, Space.xl)
            .padding(.top, Space.s)

            ScrollView {
                content
                    .padding(.horizontal, Space.xl)
                    .padding(.top, Space.xl)
                    .padding(.bottom, Space.xl)
            }
            .scrollBounceBehavior(.basedOnSize)

            footer
                .padding(.horizontal, Space.xl)
                .padding(.top, Space.s)
                .padding(.bottom, Space.m)
        }
    }
}

private struct ProgressSegments: View {
    let current: Int
    let total: Int
    @State private var filled = false

    var body: some View {
        HStack(spacing: 6) {
            ForEach(1...total, id: \.self) { i in
                GeometryReader { geo in
                    ZStack(alignment: .leading) {
                        Capsule().fill(Palette.accentFill.opacity(0.12))
                        Capsule()
                            .fill(Palette.accentFill)
                            .frame(width: i < current ? geo.size.width : (i == current && filled ? geo.size.width : 0))
                    }
                }
                .frame(height: 4)
            }
        }
        .onAppear {
            withAnimation(.timingCurve(0.16, 1, 0.3, 1, duration: 0.8).delay(0.25)) { filled = true }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("Step \(current) of \(total)")
    }
}

/// Large serif title, lotus above. Comes into focus on arrival.
private struct StepHeader: View {
    let title: String
    @State private var shown = false
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        VStack(alignment: .leading, spacing: Space.s) {
            LotusMark()
                .stroke(Palette.accentFill, style: StrokeStyle(lineWidth: 1.5, lineCap: .round, lineJoin: .round))
                .frame(width: 34, height: 24)
                .accessibilityHidden(true)
            Text(title)
                .font(.system(size: 36, weight: .medium, design: .serif))
                .foregroundStyle(Palette.accentFill)
                .accessibilityAddTraits(.isHeader)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .opacity(shown ? 1 : 0)
        .blur(radius: shown || reduceMotion ? 0 : 8)
        .offset(y: shown || reduceMotion ? 0 : 10)
        .onAppear { withAnimation(Motion.entrance(delay: 0.12)) { shown = true } }
    }
}

private extension View {
    func staggered(_ index: Int) -> some View { dealIn(index, baseDelay: 0.22, step: 0.045) }

    func glassCircle() -> some View {
        self.readableGlass(Circle(), tint: Color.white.opacity(0.6))
    }
}

/// A checkmark that draws itself.
private struct CheckDraw: View {
    let isOn: Bool

    var body: some View {
        ZStack {
            Circle()
                .strokeBorder(isOn ? Palette.accentFill : Palette.border, lineWidth: 1.5)
            Circle()
                .fill(Palette.accentFill)
                .scaleEffect(isOn ? 1 : 0.2)
                .opacity(isOn ? 1 : 0)
            CheckShape()
                .trim(from: 0, to: isOn ? 1 : 0)
                .stroke(.white, style: StrokeStyle(lineWidth: 2.2, lineCap: .round, lineJoin: .round))
                .padding(7)
        }
        .frame(width: 26, height: 26)
        .animation(.spring(response: 0.35, dampingFraction: 0.65), value: isOn)
    }
}

private struct CheckShape: Shape {
    func path(in rect: CGRect) -> Path {
        var p = Path()
        p.move(to: CGPoint(x: rect.minX, y: rect.midY))
        p.addLine(to: CGPoint(x: rect.minX + rect.width * 0.38, y: rect.maxY - rect.height * 0.1))
        p.addLine(to: CGPoint(x: rect.maxX, y: rect.minY + rect.height * 0.1))
        return p
    }
}

/// A row whose selection highlight glides to it from the previous choice.
private struct GlideRow<Label: View>: View {
    let isSelected: Bool
    let namespace: Namespace.ID
    let action: () -> Void
    @ViewBuilder var label: Label

    @AppStorage(PrefKey.haptics) private var hapticsOn = true

    var body: some View {
        Button(action: action) {
            HStack(spacing: Space.m) {
                label.frame(maxWidth: .infinity, alignment: .leading)
                CheckDraw(isOn: isSelected)
            }
            .padding(.horizontal, Space.m)
            .padding(.vertical, 14)
            .background {
                ZStack {
                    RoundedRectangle(cornerRadius: Radius.tile, style: .continuous)
                        .fill(.white.opacity(0.7))
                    if isSelected {
                        RoundedRectangle(cornerRadius: Radius.tile, style: .continuous)
                            .fill(Palette.accentWash)
                            .overlay(
                                RoundedRectangle(cornerRadius: Radius.tile, style: .continuous)
                                    .strokeBorder(Palette.accentFill.opacity(0.35), lineWidth: 1)
                            )
                            .matchedGeometryEffect(id: "selection", in: namespace)
                    }
                }
            }
            .contentShape(RoundedRectangle(cornerRadius: Radius.tile, style: .continuous))
        }
        .buttonStyle(PressableStyle(scale: 0.97))
        .accessibilityAddTraits(isSelected ? .isSelected : [])
        .sensoryFeedback(.selection, trigger: isSelected) { _, new in hapticsOn && new }
    }
}

// MARK: - Language

private struct LanguageStep: View {
    @Binding var selection: String
    @Environment(TranslationStore.self) private var translations
    @Namespace private var glide

    var body: some View {
        VStack(alignment: .leading, spacing: Space.xl) {
            StepHeader(title: "Your Language")
            LanguagePreviewCard()
                .staggered(0)
            VStack(spacing: Space.xs) {
                ForEach(Array(TranslationOption.all.enumerated()), id: \.element.id) { index, option in
                    GlideRow(isSelected: selection == option.id, namespace: glide) {
                        withAnimation(.spring(response: 0.42, dampingFraction: 0.78)) { selection = option.id }
                        translations.selectedID = option.id
                    } label: {
                        TranslationOptionLabel(option: option)
                    }
                    .staggered(index + 1)
                }
            }
        }
        .onAppear { selection = translations.selectedID }
    }
}

/// Her choice made visible: 13:28 in Arabic, with its meaning in the language she picks.
/// The old meaning blurs away, soft lines hold the place while it downloads, then the new
/// meaning comes into focus. The text is the downloaded translation, never typed in.
private struct LanguagePreviewCard: View {
    @Environment(TranslationStore.self) private var translations
    @Environment(QuranStore.self) private var quran
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    private let ref = VerseRef(surah: 13, ayah: 28)

    var body: some View {
        let option = translations.selected
        let meaning = translations.text(ref)
        let alignment: Alignment = option.isRightToLeft ? .topTrailing : .topLeading
        VStack(alignment: .leading, spacing: Space.s) {
            ArabicText(text: quran.text(ref), size: 22, lineLimit: 3)
                .inkReveal(delay: 0.35, duration: 1.0)
            ZStack(alignment: alignment) {
                if let meaning {
                    Text(meaning)
                        .font(AppFont.serif(.subheadline))
                        .foregroundStyle(Palette.textSecondary)
                        .multilineTextAlignment(option.isRightToLeft ? .trailing : .leading)
                        .lineLimit(4)
                        .frame(maxWidth: .infinity, alignment: alignment)
                        .id(option.id)
                        .transition(reduceMotion ? AnyTransition.opacity : AnyTransition.opacity.combined(with: .blurred))
                } else if translations.selectedState == .downloading {
                    ShimmerLines()
                        .transition(.opacity)
                }
            }
            .frame(maxWidth: .infinity, minHeight: 60, alignment: alignment)
            .animation(reduceMotion ? nil : .easeInOut(duration: 0.45), value: meaning)
            .animation(reduceMotion ? nil : .easeInOut(duration: 0.45), value: option.id)
            Text("Ar-Ra'd 13:28")
                .font(.footnote.weight(.medium))
                .foregroundStyle(Palette.accentFill)
        }
        .padding(Space.l)
        .background(RoundedRectangle(cornerRadius: Radius.card, style: .continuous).fill(.white.opacity(0.7)))
        .accessibilityElement(children: .combine)
    }
}

/// Three soft lines that breathe while a translation downloads.
private struct ShimmerLines: View {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            ForEach([1.0, 0.85, 0.55], id: \.self) { fraction in
                GeometryReader { geo in
                    Capsule()
                        .fill(Palette.accentFill.opacity(0.1))
                        .frame(width: geo.size.width * fraction)
                }
                .frame(height: 9)
            }
        }
        .phaseAnimator([false, true]) { lines, dim in
            lines.opacity(dim && !reduceMotion ? 0.45 : 1)
        } animation: { _ in .easeInOut(duration: 0.8) }
        .accessibilityLabel("Loading")
    }
}

// MARK: - Daily moment

private struct MomentStep: View {
    @Binding var choice: ReminderMoment?
    @Namespace private var glide
    @State private var bannerShown = false

    var body: some View {
        VStack(alignment: .leading, spacing: Space.xl) {
            StepHeader(title: "Your Daily Moment")

            MomentSky(choice: choice)
                .frame(height: 150)
                .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
                .overlay(alignment: .top) {
                    if bannerShown, let choice, choice != .later {
                        NotificationPreview(detail: choice.detail)
                            .padding(10)
                            .transition(.move(edge: .top).combined(with: .opacity).combined(with: .scale(scale: 0.9, anchor: .top)))
                    }
                }
                .staggered(0)

            VStack(spacing: Space.xs) {
                ForEach(Array(ReminderMoment.allCases.enumerated()), id: \.element.id) { index, item in
                    GlideRow(isSelected: choice == item, namespace: glide) {
                        withAnimation(.spring(response: 0.42, dampingFraction: 0.78)) { choice = item }
                        bannerShown = false
                        if item != .later {
                            withAnimation(.spring(response: 0.5, dampingFraction: 0.7).delay(0.25)) { bannerShown = true }
                        }
                    } label: {
                        HStack(spacing: Space.m) {
                            Image(systemName: item.symbol)
                                .font(.system(size: 18, weight: .medium))
                                .foregroundStyle(Palette.accentFill)
                                .symbolEffect(.bounce, value: choice == item)
                                .frame(width: 28)
                            VStack(alignment: .leading, spacing: 2) {
                                Text(item.title).font(.headline).foregroundStyle(Palette.textPrimary)
                                if item != .later {
                                    Text(item.detail).font(.subheadline).foregroundStyle(Palette.textSecondary)
                                }
                            }
                        }
                    }
                    .staggered(index + 1)
                }
            }
        }
        .animation(.smooth(duration: 0.4), value: choice)
    }
}

/// The sky at the time she picks. The day moves to it, always forward: the light changes
/// through the hours in between, the sun travels its arc, at night the moon rises and the
/// stars come out, and dawn breaks again. The chosen time rolls in the corner.
private struct MomentSky: View {
    let choice: ReminderMoment?
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    /// Grows without wrapping, so the day only ever turns forward.
    @State private var dayTime: Double?

    /// One full turn of the day. 0 is dawn; it comes round again at 1.4.
    static let cycle = 1.4

    /// Position in the day: 0.14 morning, 0.5 midday, 0.9 sunset, 1.2 night.
    private static func position(_ choice: ReminderMoment?) -> Double {
        switch choice {
        case .morning: 0.14
        case .afternoon: 0.5
        case .evening: 0.9
        case .later, .none: 1.2
        }
    }

    var body: some View {
        MomentSkyFrame(time: dayTime ?? Self.position(choice), minutes: choice?.minutes)
            .onChange(of: choice) { old, new in
                let current = dayTime ?? Self.position(old)
                var next = Self.position(new)
                while next < current - 0.0001 { next += Self.cycle }
                let distance = next - current
                withAnimation(reduceMotion ? .easeInOut(duration: 0.3) : .smooth(duration: min(1.7, 0.8 + distance * 0.6))) {
                    dayTime = next
                }
            }
            .accessibilityHidden(true)
    }
}

/// One frame of the moving day. SwiftUI animates `time`, so every hour between is drawn.
private struct MomentSkyFrame: View, Animatable {
    var time: Double
    let minutes: Int?

    var animatableData: Double {
        get { time }
        set { time = newValue }
    }

    private static let stops: [(Double, SkyPalette)] = [
        (0.0, .fajr), (0.14, .sunrise), (0.5, .dhuhr), (0.72, .asr), (0.9, .maghrib), (1.2, .night), (1.4, .fajr),
    ]

    /// Where in the day this frame is, between 0 and one full turn.
    private var dayPosition: Double {
        let t = time.truncatingRemainder(dividingBy: MomentSky.cycle)
        return t < 0 ? t + MomentSky.cycle : t
    }

    private func palette(at time: Double) -> SkyPalette {
        let stops = Self.stops
        guard time > stops[0].0 else { return stops[0].1 }
        for i in 0..<(stops.count - 1) where time <= stops[i + 1].0 {
            let t = (time - stops[i].0) / (stops[i + 1].0 - stops[i].0)
            return stops[i].1.mixed(with: stops[i + 1].1, t)
        }
        return stops[stops.count - 1].1
    }

    var body: some View {
        let time = dayPosition
        let sky = palette(at: time)
        let night = time > 1.0
        let ink: Color = sky.darkness > 0.5 ? .white : Palette.ink
        // The sun crosses by day; by night the moon crosses, rising after sunset and setting before dawn.
        let arc = night ? (time - 1.0) / (MomentSky.cycle - 1.0) : min(max(time, 0), 1)
        ZStack(alignment: .bottomLeading) {
            SkyBackground(palette: sky, isNight: night)
            CelestialArc(progress: arc, isNight: night, ink: ink)
                .padding(.top, 40)
            if let minutes {
                Text(Self.clock(minutes))
                    .font(.system(size: 26, weight: .light, design: .rounded))
                    .monospacedDigit()
                    .foregroundStyle(ink)
                    .contentTransition(.numericText(value: Double(minutes)))
                    .padding(.leading, Space.l)
                    .padding(.bottom, Space.m)
                    .transition(.opacity)
            }
        }
    }

    private static func clock(_ minutes: Int) -> String {
        let date = Calendar.current.date(bySettingHour: minutes / 60, minute: minutes % 60, second: 0, of: .now) ?? .now
        return date.formatted(date: .omitted, time: .shortened)
    }
}

/// A preview of the real notification she will get.
private struct NotificationPreview: View {
    let detail: String

    var body: some View {
        HStack(spacing: Space.s) {
            RoundedRectangle(cornerRadius: 8, style: .continuous)
                .fill(Palette.accentFill)
                .frame(width: 34, height: 34)
                .overlay(
                    LotusMark()
                        .stroke(.white, style: StrokeStyle(lineWidth: 1.2, lineCap: .round))
                        .padding(7)
                )
            VStack(alignment: .leading, spacing: 1) {
                HStack {
                    Text("A moment with the Quran").font(.footnote.weight(.semibold))
                    Spacer()
                    Text(detail.replacingOccurrences(of: "Around ", with: "")).font(.caption2).foregroundStyle(.secondary)
                }
                Text("Your verse for today is here whenever you are ready.")
                    .font(.footnote)
                    .lineLimit(1)
            }
        }
        .foregroundStyle(Palette.textPrimary)
        .padding(10)
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 18, style: .continuous))
        .shadow(color: .black.opacity(0.12), radius: 10, y: 4)
        .accessibilityElement(children: .combine)
        .accessibilityLabel("Preview of the reminder notification")
    }
}

/// The footer button for the moment step. Asks permission only here, after she chose.
private struct MomentButton: View {
    let choice: ReminderMoment?
    let onDone: (Bool) -> Void

    @AppStorage(PrefKey.reminderEnabled) private var reminderEnabled = false
    @AppStorage(PrefKey.reminderMinutes) private var reminderMinutes = 7 * 60
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var working = false

    var body: some View {
        Button {
            Task { await finish() }
        } label: {
            HStack(spacing: Space.s) {
                if working {
                    ProgressView().tint(Palette.onAccentFill)
                } else {
                    Text(choice == nil || choice == .later ? "Continue" : "Turn On Reminder")
                        .contentTransition(.opacity)
                    Image(systemName: choice == nil || choice == .later ? "arrow.right" : "bell.fill")
                        .contentTransition(.symbolEffect(.replace))
                        .symbolEffect(.bounce, value: reduceMotion ? false : !(choice == nil || choice == .later))
                }
            }
            .animation(.smooth(duration: 0.3), value: choice)
        }
        .buttonStyle(.primary)
        .disabled(choice == nil || working)
        // A small spring the first time she chooses, as the button comes awake.
        .keyframeAnimator(initialValue: 1.0, trigger: choice == nil) { [still = reduceMotion] view, scale in
            view.scaleEffect(still ? 1 : scale)
        } keyframes: { _ in
            SpringKeyframe(1.04, duration: 0.15, spring: .bouncy)
            SpringKeyframe(1.0, duration: 0.3, spring: .smooth)
        }
    }

    private func finish() async {
        var enabled = false
        if let minutes = choice?.minutes {
            working = true
            enabled = await ReminderScheduler.enable(atMinutes: minutes)
            reminderEnabled = enabled
            reminderMinutes = minutes
            working = false
        }
        onDone(enabled)
    }
}

// MARK: - Ready

/// The one celebratory moment, in the same language as the splash: the lotus is drawn petal
/// by petal, fills with light and a gold shimmer passes once, the Bismillah writes itself in,
/// her two choices land, and Begin dissolves the lotus as the app opens.
private struct ReadyStep: View {
    let languageID: String
    let moment: ReminderMoment?
    let reminderOn: Bool
    let onBegin: () -> Void

    @Environment(QuranStore.self) private var quran
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @AppStorage(PrefKey.haptics) private var hapticsOn = true
    @State private var center: CGFloat = 0
    @State private var sides: CGFloat = 0
    @State private var water: CGFloat = 0
    @State private var bloomed = false
    @State private var shimmer: CGFloat = -1
    @State private var showText = false
    @State private var showChoices = false
    @State private var showButton = false
    @State private var leaving = false

    private let gold = Color(hex: 0xD3A94A)

    private var languageName: String {
        TranslationOption.all.first { $0.id == languageID }?.nativeName ?? "English"
    }

    var body: some View {
        ZStack {
            RisingLight(active: bloomed && !reduceMotion && !leaving)
                .allowsHitTesting(false)

            VStack(spacing: Space.xl) {
                Spacer()

                lotus
                    .scaleEffect(leaving ? 1.4 : 1)
                    .opacity(leaving ? 0 : 1)
                    .blur(radius: leaving ? 8 : 0)
                    .accessibilityHidden(true)

                VStack(spacing: Space.s) {
                    ArabicText(text: quran.basmala, size: 26, placement: .center, color: Palette.accentFill)
                        .inkReveal(delay: 0.1, duration: 1.0)
                    Text("Bismillah")
                        .font(.system(size: 40, weight: .medium, design: .serif))
                        .foregroundStyle(Palette.accentFill)
                }
                .multilineTextAlignment(.center)
                .opacity(showText && !leaving ? 1 : 0)
                .blur(radius: showText || reduceMotion ? 0 : 10)
                .offset(y: showText || reduceMotion ? 0 : 10)
                .id(showText)

                VStack(spacing: Space.xs) {
                    ChoiceLine(symbol: "globe", text: languageName, shown: showChoices, index: 0)
                    ChoiceLine(symbol: reminderOn ? "bell.fill" : "bell.slash",
                               text: reminderOn ? (moment?.detail ?? "") : "No reminder",
                               shown: showChoices, index: 1)
                }
                .opacity(leaving ? 0 : 1)

                Spacer()

                Button(action: begin) {
                    HStack(spacing: Space.s) { Text("Begin"); Image(systemName: "arrow.right") }
                }
                .buttonStyle(.primary)
                .disabled(leaving)
                .padding(.horizontal, Space.xl)
                .padding(.bottom, Space.m)
                .opacity(showButton && !leaving ? 1 : 0)
                .offset(y: showButton || reduceMotion ? 0 : 14)
            }
        }
        .sensoryFeedback(.success, trigger: bloomed) { _, new in hapticsOn && new }
        .onAppear(perform: play)
    }

    private var lotus: some View {
        let stroke = StrokeStyle(lineWidth: 2.4, lineCap: .round, lineJoin: .round)
        return ZStack {
            Circle()
                .fill(RadialGradient(colors: [Color(hex: 0xF6ECCF), Color(hex: 0xF6ECCF).opacity(0)], center: .center, startRadius: 0, endRadius: 130))
                .frame(width: 260, height: 260)
                .scaleEffect(bloomed ? 1 : 0.3)
                .opacity(bloomed ? 1 : 0)
            ZStack {
                LotusPiece(part: .all)
                    .fill(Palette.accentFill.opacity(bloomed ? 0.08 : 0))
                LotusPiece(part: .center).trim(from: 0, to: center).stroke(Palette.accentFill, style: stroke)
                LotusPiece(part: .left).trim(from: 0, to: sides).stroke(Palette.accentFill, style: stroke)
                LotusPiece(part: .right).trim(from: 0, to: sides).stroke(Palette.accentFill, style: stroke)
                LotusPiece(part: .water).trim(from: 0.5 - water / 2, to: 0.5 + water / 2).stroke(Palette.accentFill, style: stroke)
                // One pass of gold light across the lotus.
                ZStack {
                    LinearGradient(colors: [.clear, gold, .clear], startPoint: .leading, endPoint: .trailing)
                        .frame(width: 46)
                        .offset(x: shimmer * 100)
                }
                .frame(width: 120, height: 86)
                .mask { LotusPiece(part: .all).stroke(style: stroke) }
                .opacity(shimmer > -1 && shimmer < 1 ? 1 : 0)
            }
            .frame(width: 120, height: 86)
            .scaleEffect(bloomed ? 1.06 : 1)
        }
    }

    private func play() {
        if reduceMotion {
            center = 1; sides = 1; water = 1; bloomed = true; showText = true; showChoices = true; showButton = true
            return
        }
        withAnimation(.easeInOut(duration: 0.75).delay(0.2)) { center = 1 }
        withAnimation(.easeInOut(duration: 0.7).delay(0.4)) { sides = 1 }
        withAnimation(.easeOut(duration: 0.6).delay(0.6)) { water = 1 }
        withAnimation(.spring(response: 0.7, dampingFraction: 0.6).delay(1.15)) { bloomed = true }
        withAnimation(.easeInOut(duration: 0.7).delay(1.2)) { shimmer = 1 }
        withAnimation(Motion.entrance(delay: 1.45)) { showText = true }
        withAnimation(.spring(response: 0.5, dampingFraction: 0.75).delay(2.0)) { showChoices = true }
        withAnimation(Motion.entrance(delay: 2.4)) { showButton = true }
    }

    /// The lotus grows and dissolves, the rest falls away, then the app opens.
    private func begin() {
        guard !leaving else { return }
        withAnimation(.easeIn(duration: reduceMotion ? 0.2 : 0.45)) { leaving = true }
        DispatchQueue.main.asyncAfter(deadline: .now() + (reduceMotion ? 0.2 : 0.45)) { onBegin() }
    }
}

/// One of her choices, landing with a small check.
private struct ChoiceLine: View {
    let symbol: String
    let text: String
    let shown: Bool
    let index: Int

    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        HStack(spacing: Space.s) {
            Image(systemName: symbol)
                .foregroundStyle(Palette.accentFill)
                .frame(width: 22)
                .symbolEffect(.bounce, value: shown)
            Text(text)
                .foregroundStyle(Palette.textPrimary)
            Spacer(minLength: Space.m)
            Image(systemName: "checkmark.circle.fill")
                .foregroundStyle(Palette.accentFill)
                .scaleEffect(shown ? 1 : 0.3)
                .opacity(shown ? 1 : 0)
                .animation(.spring(response: 0.4, dampingFraction: 0.55).delay(reduceMotion ? 0 : 0.15 + Double(index) * 0.12), value: shown)
        }
        .font(.subheadline.weight(.medium))
        .padding(.horizontal, Space.m)
        .frame(width: 260, height: 44)
        .background(Capsule().fill(.white.opacity(0.7)))
        .opacity(shown ? 1 : 0)
        .offset(y: shown || reduceMotion ? 0 : 12)
        .animation(.spring(response: 0.5, dampingFraction: 0.78).delay(reduceMotion ? 0 : Double(index) * 0.12), value: shown)
        .accessibilityElement(children: .combine)
    }
}

/// Soft specks of light that drift upward and fade. Drawn in one Canvas.
private struct RisingLight: View {
    let active: Bool

    private static let specks: [(x: Double, speed: Double, size: Double, phase: Double)] = (0..<36).map { i in
        let a = Double(i)
        return (x: (a * 0.618).truncatingRemainder(dividingBy: 1),
                speed: 0.05 + (a * 0.37).truncatingRemainder(dividingBy: 1) * 0.08,
                size: 2 + (a * 0.71).truncatingRemainder(dividingBy: 1) * 3,
                phase: (a * 0.29).truncatingRemainder(dividingBy: 1))
    }

    var body: some View {
        TimelineView(.animation(minimumInterval: 1.0 / 30, paused: !active)) { context in
            let t = context.date.timeIntervalSinceReferenceDate
            Canvas { canvas, size in
                guard active else { return }
                for s in Self.specks {
                    let progress = (t * s.speed + s.phase).truncatingRemainder(dividingBy: 1)
                    let y = size.height * (1 - progress)
                    let x = Double(size.width) * s.x + 12 * sin(t * 0.8 + s.phase * 6)
                    let alpha = sin(progress * .pi) * 0.7
                    let rect = CGRect(x: x, y: y, width: s.size, height: s.size)
                    canvas.fill(Path(ellipseIn: rect), with: .color(Color(hex: 0xD3A94A).opacity(alpha)))
                }
            }
        }
        .ignoresSafeArea()
    }
}

// MARK: - Shared rows (also used in Settings)

/// A full width choice row with a check mark. Used in settings.
struct SelectableRow<Label: View>: View {
    let isSelected: Bool
    let action: () -> Void
    @ViewBuilder var label: Label

    @AppStorage(PrefKey.haptics) private var hapticsOn = true
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        Button(action: action) {
            HStack(spacing: Space.m) {
                label
                    .frame(maxWidth: .infinity, alignment: .leading)
                Image(systemName: isSelected ? "checkmark.circle.fill" : "circle")
                    .font(.title3)
                    .foregroundStyle(isSelected ? Palette.accent : Palette.border)
                    .contentTransition(.symbolEffect(.replace))
                    .symbolEffect(.bounce, value: reduceMotion ? false : isSelected)
            }
            .foregroundStyle(Palette.textPrimary)
            .padding(.horizontal, Space.m)
            .padding(.vertical, 14)
            .background(
                RoundedRectangle(cornerRadius: Radius.tile, style: .continuous)
                    .fill(isSelected ? Palette.accentWash : Palette.surface)
            )
        }
        .buttonStyle(.pressable)
        .accessibilityAddTraits(isSelected ? .isSelected : [])
        // Only the row being chosen taps, so one choice is one tap.
        .sensoryFeedback(.selection, trigger: isSelected) { _, new in hapticsOn && new }
    }
}

struct TranslationOptionLabel: View {
    let option: TranslationOption

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(option.nativeName)
                .font(.headline)
                .foregroundStyle(Palette.textPrimary)
            Text(option.language == option.nativeName ? option.translator : "\(option.language) · \(option.translator)")
                .font(.subheadline)
                .foregroundStyle(Palette.textSecondary)
        }
        .accessibilityElement(children: .combine)
    }
}
