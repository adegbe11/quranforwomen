import SwiftUI

/// A quiet digital tasbih. One phrase, one large count, one generous tap surface.
/// Motion follows a tasbih in her hand: beads gather, a marker moves bead to bead,
/// each round of beads releases with a wave, and her target ends in a small golden bloom.
struct DhikrView: View {
    @Environment(ContentStore.self) private var content
    @Environment(LibraryStore.self) private var library
    @Environment(DhikrStore.self) private var extras
    @Environment(AppRouter.self) private var router
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    @AppStorage(PrefKey.dhikrSelection) private var selectionID = "subhanallah"
    /// 0 means no target. -1 means use the phrase's suggested target.
    @AppStorage(PrefKey.dhikrTarget) private var targetSetting = -1
    @AppStorage(PrefKey.haptics) private var hapticsOn = true

    @State private var confirmReset = false
    @State private var completions = 0
    @State private var rounds = 0
    @State private var ripples: [Ripple] = []
    @State private var pressed = false
    /// Which way the phrase slides when she changes it.
    @State private var forward = true

    @State private var goalBlooms = 0

    private var phrase: DhikrPhrase {
        content.dhikr(id: selectionID) ?? content.dhikr[0]
    }

    /// Her own phrase, when she chose one. Never mixed into the sourced list.
    private var personal: CustomDhikr? { extras.customDhikr(countID: selectionID) }

    /// The id her count is kept under: the sourced phrase id, or her own phrase's key.
    private var countID: String { personal?.countID ?? phrase.id }

    private var suggestedTarget: Int { personal?.target ?? phrase.target }

    private var target: Int? {
        switch targetSetting {
        case -1: return suggestedTarget > 0 ? suggestedTarget : nil
        case 0: return nil
        default: return targetSetting
        }
    }

    private var count: Int { library.dhikrCount(countID) }
    private var beads: Int { min(target ?? 33, 33) }

    var body: some View {
        VStack(spacing: 0) {
            phraseHeader
                .padding(.top, Space.l)
                .padding(.horizontal, Space.xl)

            Spacer(minLength: Space.l)

            counter

            Spacer(minLength: Space.l)

            tapSurface
                .overlay(alignment: .topLeading) {
                    if let goal = extras.goal {
                        DhikrGoalRing(today: extras.total(), goal: goal)
                            .padding(Space.s)
                            .allowsHitTesting(false)
                            .transition(.scale(scale: 0.8, anchor: .topLeading).combined(with: .opacity))
                    }
                }
                .animation(reduceMotion ? nil : Motion.reward, value: extras.goal)
                .padding(.horizontal, Space.xl)
                .padding(.bottom, Space.xl)
                .dealIn(0, baseDelay: 0.35)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .pageBackground()
        .navigationTitle("Dhikr")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar(.hidden, for: .tabBar)
        .safeAreaInset(edge: .bottom, spacing: 0) {
            if MiniPlayerBar.usesSystemAccessory { MiniPlayerBar() }
        }
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Menu {
                    Picker("Target", selection: $targetSetting) {
                        if personal != nil {
                            Text(suggestedTarget > 0 ? "Your target (\(suggestedTarget))" : "Your target (none)").tag(-1)
                        } else {
                            Text("Suggested (\(phrase.target))").tag(-1)
                        }
                        Text("33").tag(33)
                        Text("99").tag(99)
                        Text("100").tag(100)
                        Text("No target").tag(0)
                    }
                } label: {
                    Label("Target", systemImage: "target")
                        .symbolEffect(.bounce, value: reduceMotion ? 0 : targetSetting)
                }
            }
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    confirmReset = true
                } label: {
                    Label("Reset", systemImage: "arrow.counterclockwise")
                }
                .disabled(count == 0)
            }
            ToolbarItem(placement: .topBarTrailing) {
                moreMenu
            }
        }
        .confirmationDialog("Reset the count to zero?", isPresented: $confirmReset, titleVisibility: .visible) {
            Button("Reset", role: .destructive) {
                withAnimation(reduceMotion ? nil : Motion.gentle) {
                    library.setDhikrCount(countID, 0)
                }
            }
        }
        .sensoryFeedback(.success, trigger: goalBlooms) { _, _ in hapticsOn }
        // A soft tap for each count, a firmer one at the end of each round, success at her target.
        .sensoryFeedback(.impact(flexibility: .soft, intensity: 0.6), trigger: count) { old, new in
            hapticsOn && new > old && new % beads != 0
        }
        .sensoryFeedback(.impact(weight: .medium, intensity: 0.9), trigger: rounds) { _, _ in hapticsOn }
        .sensoryFeedback(.success, trigger: completions) { _, _ in hapticsOn }
    }

    // MARK: Parts

    private var phraseHeader: some View {
        VStack(spacing: Space.s) {
            Menu {
                Picker("Dhikr", selection: Binding(
                    get: { selectionID },
                    set: { select($0) }
                )) {
                    ForEach(content.dhikr) { item in
                        Text(item.transliteration).tag(item.id)
                    }
                    if !extras.custom.isEmpty {
                        Section("Your Own Words") {
                            ForEach(extras.custom) { item in
                                Text(item.text).tag(item.countID)
                            }
                        }
                    }
                }
                Divider()
                Button {
                    open(.customDhikr)
                } label: {
                    Label("Your Own Dhikr", systemImage: "pencil")
                }
            } label: {
                HStack(spacing: 6) {
                    Text(displayName)
                        .lineLimit(1)
                        .font(.headline)
                        .contentTransition(.interpolate)
                    Image(systemName: "chevron.up.chevron.down")
                        .font(.caption.weight(.semibold))
                }
                .foregroundStyle(Palette.accent)
                .padding(.horizontal, Space.m)
                .padding(.vertical, Space.xs)
                .background(Capsule().fill(Palette.accentWash))
            }
            .accessibilityLabel("Choose dhikr. Current: \(displayName)")
            .focusIn(delay: 0.05)

            // The phrase slides in the direction she swiped, and the Arabic writes itself in.
            ZStack {
                VStack(spacing: Space.s) {
                    if let personal {
                        // Her own words: shown as written, and marked as personal.
                        if ArabicSearch.containsArabic(personal.text) {
                            ArabicText(text: personal.text, size: 34, placement: .center, color: Palette.textPrimary)
                                .padding(.top, Space.xs)
                                .inkReveal(delay: 0.15, duration: 0.9)
                        } else {
                            Text(personal.text)
                                .font(AppFont.serif(.title2))
                                .foregroundStyle(Palette.textPrimary)
                                .multilineTextAlignment(.center)
                                .padding(.top, Space.xs)
                                .focusIn(delay: 0.15)
                        }
                        SourceLabel(kind: .personal, reference: "Your own words")
                            .focusIn(delay: 0.45)
                    } else {
                        ArabicText(text: phrase.arabic, size: 38, placement: .center, color: Palette.textPrimary)
                            .padding(.top, Space.xs)
                            .inkReveal(delay: 0.15, duration: 0.9)

                        Text(phrase.translation)
                            .font(AppFont.serif(.body))
                            .foregroundStyle(Palette.textSecondary)
                            .multilineTextAlignment(.center)
                            .focusIn(delay: 0.45)

                        Text(phrase.source)
                            .font(.caption)
                            .foregroundStyle(Palette.textSecondary.opacity(0.8))
                            .focusIn(delay: 0.55)
                    }
                }
                .id(countID)
                .transition(reduceMotion ? AnyTransition.opacity : AnyTransition.push(from: forward ? .trailing : .leading).combined(with: .opacity))
            }
            .frame(maxWidth: .infinity)
            .contentShape(Rectangle())
            .gesture(
                DragGesture(minimumDistance: 24)
                    .onEnded { value in
                        guard abs(value.translation.width) > abs(value.translation.height) else { return }
                        step(value.translation.width < 0 ? 1 : -1)
                    }
            )
            .accessibilityElement(children: .combine)
            .accessibilityHint("Swipe up or down to change the dhikr")
            .accessibilityAdjustableAction { direction in
                step(direction == .increment ? 1 : -1)
            }
        }
        .sensoryFeedback(.selection, trigger: selectionID) { _, _ in hapticsOn }
    }

    private var counter: some View {
        let filled = count == 0 ? 0 : ((count - 1) % beads) + 1
        let complete = target.map(isComplete) ?? false
        return ZStack {
            BeadRing(total: beads, filled: filled, count: count, round: rounds, complete: complete)
                .frame(width: 264, height: 264)
                .accessibilityHidden(true)

            CompletionSparks(trigger: completions)

            VStack(spacing: Space.xxs) {
                Text("\(count)")
                    .font(.system(size: 76, weight: .light, design: .rounded))
                    .monospacedDigit()
                    .foregroundStyle(Palette.textPrimary)
                    .contentTransition(reduceMotion ? .identity : .numericText(value: Double(count)))
                    .minimumScaleFactor(0.5)
                    .lineLimit(1)
                    // A small lift with every count, a bigger one at her target.
                    .keyframeAnimator(initialValue: 1.0, trigger: count) { view, scale in
                        view.scaleEffect(reduceMotion ? 1 : scale)
                    } keyframes: { _ in
                        KeyframeTrack {
                            CubicKeyframe(complete ? 1.16 : 1.06, duration: 0.08)
                            SpringKeyframe(1, duration: 0.4, spring: .bouncy)
                        }
                    }
                if let target {
                    HStack(spacing: 4) {
                        if complete {
                            Image(systemName: "checkmark")
                                .font(.subheadline.weight(.bold))
                                .transition(.scale.combined(with: .opacity))
                                .symbolEffect(.bounce, value: completions)
                        }
                        Text(complete ? "Complete" : "of \(target)")
                            .monospacedDigit()
                            .contentTransition(reduceMotion ? .opacity : .numericText(value: Double(target)))
                    }
                    .font(.subheadline.weight(.medium))
                    .foregroundStyle(complete ? Palette.accent : Palette.textSecondary)
                    .animation(reduceMotion ? nil : Motion.reward, value: complete)
                    .animation(reduceMotion ? nil : .snappy, value: target)
                }
            }
            .frame(width: 180)
        }
        // The glow breathes slowly while she counts, and blooms when she reaches her target.
        .background {
            Circle()
                .fill(Palette.accentWash)
                .frame(width: 200, height: 200)
                .blur(radius: 30)
                .phaseAnimator([false, true, false], trigger: completions) { glow, phase in
                    glow.scaleEffect(phase ? 1.5 : 0.9).opacity(phase ? 1 : 0.4)
                } animation: { _ in .smooth(duration: 0.8) }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(target.map { "\(count) of \($0)" } ?? "\(count)")
    }

    private var tapSurface: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 28, style: .continuous)
                .fill(Palette.accentWash)
            ForEach(ripples) { ripple in
                RippleView(origin: ripple.origin)
            }
            Image(systemName: "hand.tap")
                .font(.system(size: 30, weight: .regular))
                // Invites the first tap, then answers each one.
                .symbolEffect(.pulse, options: .repeating, isActive: count == 0 && !reduceMotion)
                .symbolEffect(.bounce, value: count)
                .foregroundStyle(Palette.accent)
            .allowsHitTesting(false)
        }
        .frame(maxWidth: .infinity)
        .frame(height: 190)
        .clipShape(RoundedRectangle(cornerRadius: 28, style: .continuous))
        .contentShape(RoundedRectangle(cornerRadius: 28, style: .continuous))
        .scaleEffect(pressed ? 0.98 : 1)
        .animation(Motion.press, value: pressed)
        .onTapGesture { location in
            if !reduceMotion {
                let ripple = Ripple(origin: location)
                ripples.append(ripple)
                Task {
                    try? await Task.sleep(for: .milliseconds(700))
                    ripples.removeAll { $0.id == ripple.id }
                }
            }
            pressed = true
            Task {
                try? await Task.sleep(for: .milliseconds(90))
                pressed = false
            }
            increment()
        }
        .accessibilityElement()
        .accessibilityAddTraits(.isButton)
        .accessibilityLabel("Count")
        .accessibilityValue(target.map { "\(count) of \($0)" } ?? "\(count)")
        .accessibilityHint("Adds one")
        .accessibilityAction { increment() }
    }

    private func isComplete(_ target: Int) -> Bool { count >= target && count > 0 }

    private func increment() {
        let next = count + 1
        let goalBefore = extras.goal.map { extras.total() >= $0 } ?? true
        withAnimation(reduceMotion ? nil : .snappy(duration: 0.22)) {
            library.setDhikrCount(countID, next)
            extras.recordCount()
        }
        extras.playClick()
        if let target, next == target {
            completions += 1
        } else if next % beads == 0 {
            rounds += 1
        }
        if !goalBefore, let goal = extras.goal, extras.total() >= goal {
            goalBlooms += 1
        }
    }

    private var displayName: String { personal?.text ?? phrase.transliteration }

    /// Every phrase in picker order: the sourced list, then her own.
    private var allIDs: [String] { content.dhikr.map(\.id) + extras.custom.map(\.countID) }

    /// Moves to the next or previous phrase, wrapping around. Her own phrases and the
    /// sourced list each wrap within themselves, so the two are never mixed.
    private func step(_ offset: Int) {
        let group = personal != nil ? extras.custom.map(\.countID) : content.dhikr.map(\.id)
        guard !group.isEmpty, let index = group.firstIndex(of: countID) else { return }
        select(group[(index + offset + group.count) % group.count], forward: offset > 0)
    }

    private func open(_ route: Route) {
        router.paths[router.tab, default: []].append(route)
    }

    /// Sound, daily goal, history and her own phrases.
    private var moreMenu: some View {
        Menu {
            Toggle(isOn: Binding(
                get: { extras.soundOn },
                set: { extras.setSoundOn($0) }
            )) {
                Label("Counting Sound", systemImage: "speaker.wave.2")
            }
            Picker(selection: Binding(
                get: { extras.goal ?? 0 },
                set: { extras.setGoal($0) }
            )) {
                Text("Off").tag(0)
                ForEach([100, 300, 500, 1000], id: \.self) { value in
                    Text("\(value)").tag(value)
                }
            } label: {
                Label("Daily Goal", systemImage: "circle.circle")
            }
            .pickerStyle(.menu)
            Divider()
            Button {
                open(.dhikrHistory)
            } label: {
                Label("History", systemImage: "chart.bar")
            }
            Button {
                open(.customDhikr)
            } label: {
                Label("Your Own Dhikr", systemImage: "pencil")
            }
        } label: {
            Label("More", systemImage: "ellipsis.circle")
        }
    }

    private func select(_ id: String, forward: Bool? = nil) {
        guard id != selectionID else { return }
        let all = allIDs
        let from = all.firstIndex(of: countID) ?? 0
        let to = all.firstIndex(of: id) ?? 0
        self.forward = forward ?? (to > from)
        withAnimation(reduceMotion ? nil : .spring(response: 0.45, dampingFraction: 0.86)) {
            selectionID = id
        }
    }
}

// MARK: - Tasbih beads

/// Beads around the count, like a tasbih in her hand.
/// They gather one by one on arrival, a marker moves to each new bead along the ring,
/// a wave runs through them at the end of each round, and they release one after another.
private struct BeadRing: View {
    let total: Int
    let filled: Int
    let count: Int
    let round: Int
    let complete: Bool

    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var gathered = false
    @State private var lastFilled = 0

    private let gold = Color(hex: 0xC9A24A)

    var body: some View {
        GeometryReader { geo in
            let size = min(geo.size.width, geo.size.height)
            let radius = size / 2 - 10
            let bead = max(6, min(11, (2 * .pi * radius / CGFloat(total)) * 0.55))
            // Beads release in turn, last to first, when a round ends or she resets.
            let releasing = filled < lastFilled
            ZStack {
                ForEach(0..<total, id: \.self) { i in
                    let angle = Double(i) / Double(total) * 2 * .pi - .pi / 2
                    Circle()
                        .fill(i < filled ? (complete ? gold : Palette.accentFill) : Palette.border)
                        .frame(width: bead, height: bead)
                        .scaleEffect(i == filled - 1 && !reduceMotion ? 1.45 : 1)
                        .keyframeAnimator(initialValue: 1.0, trigger: round) { dot, scale in
                            dot.scaleEffect(reduceMotion ? 1 : scale)
                        } keyframes: { _ in
                            KeyframeTrack {
                                LinearKeyframe(1, duration: Double(i) * 0.012)
                                CubicKeyframe(1.6, duration: 0.12)
                                SpringKeyframe(1, duration: 0.35, spring: .bouncy)
                            }
                        }
                        .scaleEffect(gathered || reduceMotion ? 1 : 0.01)
                        .opacity(gathered || reduceMotion ? 1 : 0)
                        .offset(x: cos(angle) * radius, y: sin(angle) * radius)
                        .animation(
                            .spring(response: 0.35, dampingFraction: 0.55)
                                .delay(releasing && !reduceMotion ? Double(total - i) * 0.012 : 0),
                            value: filled
                        )
                        .animation(.spring(response: 0.5, dampingFraction: 0.62).delay(0.1 + Double(i) * 0.016), value: gathered)
                        .animation(.smooth(duration: 0.5).delay(Double(i) * 0.015), value: complete)
                }

                // The marker: a soft light that travels along the ring to the newest bead.
                if count > 0 && !reduceMotion {
                    Circle()
                        .fill((complete ? gold : Palette.accent).opacity(0.25))
                        .frame(width: bead * 3, height: bead * 3)
                        .blur(radius: 3)
                        .modifier(OrbitOffset(
                            angle: Double(count - 1) / Double(total) * 2 * .pi - .pi / 2,
                            radius: radius
                        ))
                        .animation(.spring(response: 0.38, dampingFraction: 0.72), value: count)
                        .transition(.opacity)
                }
            }
            .frame(width: geo.size.width, height: geo.size.height)
        }
        .onAppear {
            lastFilled = filled
            gathered = true
        }
        .onChange(of: filled) { _, new in lastFilled = new }
    }
}

/// Places a view on a circle by angle, so animating the angle moves it along the arc,
/// not straight across.
private struct OrbitOffset: ViewModifier, Animatable {
    var angle: Double
    let radius: CGFloat

    var animatableData: Double {
        get { angle }
        set { angle = newValue }
    }

    func body(content: Content) -> some View {
        content.offset(x: cos(angle) * radius, y: sin(angle) * radius)
    }
}

/// Gold and emerald sparks that fly out from the count when she reaches her target.
private struct CompletionSparks: View {
    let trigger: Int
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        ZStack {
            ForEach(0..<14, id: \.self) { i in
                let angle = Double(i) / 14 * 2 * .pi
                Circle()
                    .fill(i.isMultiple(of: 2) ? Color(hex: 0xD3A94A) : Palette.accentFill)
                    .frame(width: 6, height: 6)
                    .keyframeAnimator(initialValue: PetalState(), trigger: trigger) { spark, value in
                        spark
                            .offset(x: cos(angle) * value.distance, y: sin(angle) * value.distance)
                            .opacity(reduceMotion ? 0 : value.opacity)
                            .scaleEffect(value.scale)
                    } keyframes: { _ in
                        KeyframeTrack(\.distance) {
                            CubicKeyframe(40, duration: 0.01)
                            SpringKeyframe(150, duration: 0.7, spring: .snappy)
                        }
                        KeyframeTrack(\.opacity) {
                            LinearKeyframe(1, duration: 0.05)
                            LinearKeyframe(1, duration: 0.35)
                            LinearKeyframe(0, duration: 0.35)
                        }
                        KeyframeTrack(\.scale) {
                            CubicKeyframe(1.3, duration: 0.2)
                            CubicKeyframe(0.5, duration: 0.5)
                        }
                    }
            }
        }
        .allowsHitTesting(false)
        .accessibilityHidden(true)
    }
}

private struct Ripple: Identifiable {
    let id = UUID()
    let origin: CGPoint
}

/// A ring of light that spreads from where her finger touched.
private struct RippleView: View {
    let origin: CGPoint
    @State private var expanded = false

    var body: some View {
        Circle()
            .fill(Palette.accent.opacity(0.18))
            .frame(width: 60, height: 60)
            .scaleEffect(expanded ? 6 : 0.3)
            .opacity(expanded ? 0 : 1)
            .position(origin)
            .allowsHitTesting(false)
            .onAppear {
                withAnimation(.easeOut(duration: 0.65)) { expanded = true }
            }
    }
}
