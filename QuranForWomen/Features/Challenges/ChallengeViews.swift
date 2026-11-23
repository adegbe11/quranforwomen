import SwiftUI

// MARK: - Copy

extension ChallengeStep {
    /// What the day asks for, in plain words.
    var actionText: String {
        switch self {
        case .dhikr(let phrase):
            return "Say this \(phrase.target) times"
        case .pages(let pages):
            return pages.count == 1 ? "Read page \(pages.lowerBound)" : "Read pages \(pages.lowerBound) to \(pages.upperBound)"
        case .dua:
            return "Read this dua"
        case .gratitude:
            return "Read this verse, then write"
        }
    }
}

// MARK: - List

/// Every challenge in one quiet list. Rows deal in and each icon lands with a small bounce.
struct ChallengesView: View {
    @Environment(ContentStore.self) private var content
    @Environment(ChallengeStore.self) private var challenges

    var body: some View {
        let list = ChallengeCatalog.available(content: content)
        ScrollView {
            VStack(alignment: .leading, spacing: Space.xl) {
                GroupedSection {
                    ForEach(Array(list.enumerated()), id: \.element.id) { index, challenge in
                        if index > 0 { RowDivider() }
                        NavigationLink(value: Route.challenge(challenge.id)) {
                            ListRow(
                                symbol: challenge.symbol,
                                title: challenge.title,
                                value: progress(challenge),
                                landDelay: 0.3 + Double(min(index, 8)) * 0.05
                            )
                            .zoomSource("challenge-\(challenge.id)")
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
        .navigationTitle("Challenges")
    }

    /// Nothing until she starts; then where she is. The title already says how many days.
    private func progress(_ challenge: Challenge) -> String? {
        guard challenges.state(of: challenge) != nil else { return nil }
        if challenges.isComplete(challenge) { return "Done" }
        return "Day \(challenges.nextDay(in: challenge) + 1)"
    }
}

// MARK: - One challenge

/// One challenge, day by day. She can open any day; the next day not yet done comes first.
/// Marking a day done fills its tile with a spring and sends out a few petals, then the
/// next day slides in. Missing a day changes nothing: she continues where she is.
struct ChallengeView: View {
    let challengeID: String

    @Environment(ContentStore.self) private var content
    @Environment(QuranStore.self) private var quran
    @Environment(ChallengeStore.self) private var challenges
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @AppStorage(PrefKey.haptics) private var hapticsOn = true

    @State private var selected: Int?
    @State private var markTick = 0
    @State private var confirmLeave = false
    @State private var confirmRestart = false
    @State private var showMushaf = false
    @State private var opening: JournalStart?

    private var challenge: Challenge? { ChallengeCatalog.challenge(id: challengeID) }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: Space.xl) {
                if let challenge {
                    if let state = challenges.state(of: challenge) {
                        active(challenge, state: state)
                            .transition(.opacity.combined(with: .scale(scale: 0.97)))
                    } else {
                        intro(challenge)
                            .transition(.opacity)
                    }
                }
            }
            .padding(.horizontal, Space.page)
            .padding(.vertical, Space.s)
            .readableWidth()
            .animation(reduceMotion ? nil : Motion.smooth, value: challenge.map { challenges.state(of: $0) == nil } ?? true)
        }
        .pageBackground()
        .navigationTitle(challenge?.title ?? "")
        .navigationBarTitleDisplayMode(.inline)
        .confirmationDialog("Leave this challenge?", isPresented: $confirmLeave, titleVisibility: .visible) {
            Button("Leave Challenge", role: .destructive) {
                guard let challenge else { return }
                withAnimation(reduceMotion ? nil : Motion.smooth) {
                    challenges.leave(challenge)
                    selected = nil
                }
            }
        }
        .confirmationDialog("Start this challenge again?", isPresented: $confirmRestart, titleVisibility: .visible) {
            Button("Start Over", role: .destructive) { restart() }
        }
        .sensoryFeedback(.success, trigger: markTick) { _, _ in hapticsOn }
        .fullScreenCover(isPresented: $showMushaf) { MushafView() }
        .fullScreenCover(item: $opening) { JournalStartView(start: $0) }
    }

    // MARK: Before starting

    @ViewBuilder
    private func intro(_ challenge: Challenge) -> some View {
        let first = ChallengeCatalog.step(challenge, day: 0, content: content, startPage: savedPage)
        VStack(alignment: .leading, spacing: Space.s) {
            Image(systemName: challenge.symbol)
                .font(.system(size: 22, weight: .regular))
                .foregroundStyle(Palette.accent)
                .frame(width: 44, height: 44)
                .background(Circle().fill(Palette.accentWash))
                .modifier(ExploreIconLand(delay: 0.35))
                .accessibilityHidden(true)
            HStack(alignment: .firstTextBaseline, spacing: Space.xs) {
                RollingNumber(value: challenge.days, delay: 0.15)
                    .font(.system(size: 56, weight: .light, design: .rounded))
                    .foregroundStyle(Palette.textPrimary)
                Text("days")
                    .font(.headline)
                    .foregroundStyle(Palette.textSecondary)
            }
            if let first {
                RowDivider(inset: 0)
                    .padding(.vertical, Space.xs)
                Text("Day 1 · \(first.actionText)")
                    .font(.subheadline.weight(.medium))
                    .foregroundStyle(Palette.textPrimary)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .cardSurface(padding: Space.xl)
        .accessibilityElement(children: .combine)
        .dealIn(0, baseDelay: 0.1)

        Button("Start Challenge") {
            withAnimation(reduceMotion ? nil : Motion.smooth) {
                challenges.start(challenge)
                selected = nil
            }
        }
        .buttonStyle(.primary)
        .dealIn(1, baseDelay: 0.1)
    }

    // MARK: Started

    @ViewBuilder
    private func active(_ challenge: Challenge, state: ChallengeProgress) -> some View {
        let next = challenges.nextDay(in: challenge)
        let day = min(max(selected ?? next, 0), challenge.days - 1)
        let doneCount = state.done.count
        let complete = doneCount >= challenge.days
        let step = ChallengeCatalog.step(challenge, day: day, content: content, startPage: state.startPage)

        HStack(spacing: Space.l) {
            ChallengeRing(done: doneCount, days: challenge.days)
                .frame(width: 112, height: 112)
            VStack(alignment: .leading, spacing: 4) {
                Eyebrow(text: "Day \(day + 1)")
                    .contentTransition(.numericText(value: Double(day)))
                if let step {
                    Text(step.actionText)
                        .font(.system(.headline, design: .serif))
                        .foregroundStyle(Palette.textPrimary)
                        .fixedSize(horizontal: false, vertical: true)
                        .contentTransition(.opacity)
                }
            }
            .animation(reduceMotion ? nil : .snappy, value: day)
            .focusIn(delay: 0.2)
        }
        .cardSurface(padding: Space.xl)
        .accessibilityElement(children: .combine)

        if let step {
            ChallengeStepView(step: step, day: day, isDone: state.done.contains(day), onCountReached: {
                if !state.done.contains(day) { mark(day, in: challenge, done: true) }
            }, openMushaf: { page in
                UserDefaults.standard.set(page, forKey: "mushaf.page")
                showMushaf = true
            }, write: { prompt in
                opening = .prompt(prompt, isDua: false)
            })
            .id("\(challenge.id)-\(day)")
            .transition(reduceMotion ? .opacity : .asymmetric(
                insertion: .move(edge: .trailing).combined(with: .opacity),
                removal: .move(edge: .leading).combined(with: .opacity)
            ))
        }

        ChallengeMarkButton(done: state.done.contains(day)) {
            mark(day, in: challenge, done: !state.done.contains(day))
        }

        if complete {
            ChallengeComplete(days: challenge.days) { confirmRestart = true }
                .transition(.scale(scale: 0.95).combined(with: .opacity))
        }

        VStack(alignment: .leading, spacing: Space.s) {
            SectionHeader(title: "All days")
                .focusIn(delay: 0.3)
            LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 6), count: 7), spacing: 6) {
                ForEach(0..<challenge.days, id: \.self) { index in
                    ChallengeDayCell(day: index, isDone: state.done.contains(index), isSelected: index == day) {
                        withAnimation(reduceMotion ? nil : .spring(response: 0.45, dampingFraction: 0.86)) {
                            selected = index
                        }
                    }
                    .dealIn(min(index / 7 + index % 7, 14), baseDelay: 0.35, step: 0.03)
                }
            }
        }

        VStack(spacing: Space.xs) {
            if !complete {
                Button("Start Over") { confirmRestart = true }
                    .frame(maxWidth: .infinity, minHeight: 44)
            }
            Button("Leave Challenge", role: .destructive) { confirmLeave = true }
                .frame(maxWidth: .infinity, minHeight: 44)
        }
    }

    private var savedPage: Int { max(UserDefaults.standard.integer(forKey: "mushaf.page"), 1) }

    private func mark(_ day: Int, in challenge: Challenge, done: Bool) {
        withAnimation(reduceMotion ? nil : .spring(response: 0.45, dampingFraction: 0.7)) {
            // Keep this day on screen while it fills in.
            selected = day
            challenges.setDone(done, day: day, in: challenge)
        }
        guard done else { return }
        markTick += 1
        guard !challenges.isComplete(challenge) else { return }
        // Then the next day not yet done slides in.
        Task {
            try? await Task.sleep(for: .seconds(reduceMotion ? 0.3 : 0.9))
            guard selected == day else { return }
            withAnimation(reduceMotion ? nil : .spring(response: 0.5, dampingFraction: 0.86)) { selected = nil }
        }
    }

    private func restart() {
        guard let challenge else { return }
        withAnimation(reduceMotion ? nil : Motion.smooth) {
            challenges.restart(challenge)
            selected = nil
        }
    }
}

// MARK: - A day's content

/// The bundled text for one day, exactly as it is in the app's verified data.
private struct ChallengeStepView: View {
    let step: ChallengeStep
    let day: Int
    let isDone: Bool
    let onCountReached: () -> Void
    let openMushaf: (Int) -> Void
    let write: (String) -> Void

    @Environment(QuranStore.self) private var quran

    var body: some View {
        switch step {
        case .dhikr(let phrase):
            VStack(alignment: .leading, spacing: Space.m) {
                ArabicText(text: phrase.arabic, size: 30, placement: .center)
                    .inkReveal(delay: 0.2, duration: 1.0)
                Text(phrase.transliteration)
                    .font(.subheadline.italic())
                    .foregroundStyle(Palette.textSecondary)
                    .frame(maxWidth: .infinity)
                    .multilineTextAlignment(.center)
                TranslationText(text: phrase.translation)
                    .multilineTextAlignment(.center)
                SourceLabel(kind: .hadith, reference: phrase.source)
                    .frame(maxWidth: .infinity)
                ChallengeCounter(target: phrase.target, isDone: isDone, reached: onCountReached)
                    .frame(maxWidth: .infinity)
                    .padding(.top, Space.xs)
            }
            .cardSurface(padding: Space.xl)

        case .dua(let dua):
            VStack(alignment: .leading, spacing: Space.m) {
                Text(dua.title)
                    .font(.system(.headline, design: .serif))
                    .foregroundStyle(Palette.textPrimary)
                ArabicText(text: dua.arabic, size: 26)
                    .inkReveal(delay: 0.2, duration: 1.1)
                if let transliteration = dua.transliteration {
                    Text(transliteration)
                        .font(.subheadline.italic())
                        .foregroundStyle(Palette.textSecondary)
                        .fixedSize(horizontal: false, vertical: true)
                }
                TranslationText(text: dua.translation)
                if let note = dua.note {
                    Text(note)
                        .font(.footnote)
                        .foregroundStyle(Palette.textSecondary)
                        .fixedSize(horizontal: false, vertical: true)
                }
                SourceLabel(kind: dua.sourceKind, reference: dua.source)
            }
            .cardSurface(padding: Space.xl)

        case .pages(let pages):
            VStack(alignment: .leading, spacing: Space.m) {
                if let range = quran.verses(onPages: pages) {
                    Text("\(quran.surah(range.first.surah).name) \(range.first.id) to \(quran.surah(range.last.surah).name) \(range.last.id)")
                        .font(.system(.title3, design: .serif, weight: .semibold))
                        .foregroundStyle(Palette.textPrimary)
                        .fixedSize(horizontal: false, vertical: true)
                }
                Text(pages.count == 1 ? "Page \(pages.lowerBound)" : "Pages \(pages.lowerBound) to \(pages.upperBound)")
                    .font(.subheadline)
                    .foregroundStyle(Palette.textSecondary)
                Button("Open the Mushaf") { openMushaf(pages.lowerBound) }
                    .buttonStyle(.primary)
            }
            .cardSurface(padding: Space.xl)

        case .gratitude(let verse, let prompt):
            VStack(alignment: .leading, spacing: Space.s) {
                VerseCard(ref: verse, inkDelay: 0.25)
                VStack(alignment: .leading, spacing: Space.m) {
                    Eyebrow(text: "Write")
                    Text(prompt)
                        .font(AppFont.serif(.title3, weight: .semibold))
                        .foregroundStyle(Palette.textPrimary)
                        .fixedSize(horizontal: false, vertical: true)
                    Button("Write") { write(prompt) }
                        .buttonStyle(.tinted)
                }
                .cardSurface(padding: Space.xl)
            }
        }
    }
}

/// A tap counter for the day's dhikr. Each tap ticks; reaching the target marks the day done.
private struct ChallengeCounter: View {
    let target: Int
    let isDone: Bool
    let reached: () -> Void

    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @AppStorage(PrefKey.haptics) private var hapticsOn = true
    @State private var count = 0
    @State private var pulse = false

    private var fraction: Double { target == 0 ? 0 : min(1, Double(count) / Double(target)) }

    var body: some View {
        VStack(spacing: Space.s) {
            Button {
                count += 1
                if !reduceMotion { pulse.toggle() }
                if count == target { reached() }
            } label: {
                ZStack {
                    Circle().stroke(Palette.accentWash, lineWidth: 8)
                    Circle()
                        .trim(from: 0, to: max(fraction, 0.001))
                        .stroke(Palette.accentFill, style: StrokeStyle(lineWidth: 8, lineCap: .round))
                        .rotationEffect(.degrees(-90))
                        .animation(reduceMotion ? nil : .spring(response: 0.4, dampingFraction: 0.8), value: fraction)
                    VStack(spacing: 0) {
                        Text("\(count)")
                            .font(.system(size: 40, weight: .light, design: .rounded))
                            .monospacedDigit()
                            .foregroundStyle(Palette.textPrimary)
                            .contentTransition(reduceMotion ? .identity : .numericText(value: Double(count)))
                            .animation(reduceMotion ? nil : .snappy, value: count)
                        Text("of \(target)")
                            .font(.caption)
                            .foregroundStyle(Palette.textSecondary)
                    }
                }
                .frame(width: 140, height: 140)
                .contentShape(Circle())
                .keyframeAnimator(initialValue: 1.0, trigger: pulse) { [still = reduceMotion] view, scale in
                    view.scaleEffect(still ? 1 : scale)
                } keyframes: { _ in
                    SpringKeyframe(0.95, duration: 0.08)
                    SpringKeyframe(1.0, duration: 0.25, spring: .bouncy)
                }
            }
            .buttonStyle(.plain)
            .accessibilityLabel("Count")
            .accessibilityValue("\(count) of \(target)")
            .sensoryFeedback(.selection, trigger: count) { _, new in hapticsOn && new > 0 && new != target }

            if count > 0 {
                Button("Reset") {
                    withAnimation(reduceMotion ? nil : .snappy) { count = 0 }
                }
                .font(.subheadline.weight(.medium))
                .frame(minWidth: 44, minHeight: 44)
                .transition(.opacity)
            }
        }
        .animation(reduceMotion ? nil : Motion.gentle, value: count > 0)
    }
}

// MARK: - Pieces

/// Marks a day done. The ring fills with a spring, the check pops in, and a few petals drift out.
private struct ChallengeMarkButton: View {
    let done: Bool
    let action: () -> Void

    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        Button(action: action) {
            HStack(spacing: Space.s) {
                ZStack {
                    Circle().strokeBorder(Palette.accent, lineWidth: 2).opacity(done ? 0 : 1)
                    Circle().fill(Palette.accentFill).scaleEffect(done ? 1 : 0.01).opacity(done ? 1 : 0)
                    Image(systemName: "checkmark")
                        .font(.system(size: 11, weight: .bold))
                        .foregroundStyle(Palette.onAccentFill)
                        .scaleEffect(done ? 1 : 0.3)
                        .opacity(done ? 1 : 0)
                        .symbolEffect(.bounce, value: reduceMotion ? false : done)
                }
                .frame(width: 22, height: 22)
                .overlay { PetalBurst(trigger: done, active: done) }
                Text(done ? "Done" : "Mark as done")
                    .contentTransition(.opacity)
            }
            .animation(reduceMotion ? nil : .spring(response: 0.35, dampingFraction: 0.6), value: done)
        }
        .buttonStyle(.tinted)
        .accessibilityValue(done ? "Done" : "Not done")
    }
}

/// How many days are done, drawn as a ring that fills from the top.
private struct ChallengeRing: View {
    let done: Int
    let days: Int

    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var drawn = false

    private var fraction: Double { days == 0 ? 0 : min(1, Double(done) / Double(days)) }

    var body: some View {
        ZStack {
            Circle()
                .stroke(Palette.accentWash, lineWidth: 10)
            Circle()
                .trim(from: 0, to: drawn || reduceMotion ? max(fraction, 0.001) : 0.001)
                .stroke(Palette.accentFill, style: StrokeStyle(lineWidth: 10, lineCap: .round))
                .rotationEffect(.degrees(-90))
                .animation(reduceMotion ? nil : .spring(response: 0.9, dampingFraction: 0.8), value: fraction)
            VStack(spacing: 0) {
                RollingNumber(value: done, delay: 0.2)
                    .font(.system(.title2, design: .serif, weight: .semibold))
                    .foregroundStyle(Palette.textPrimary)
                Text("of \(days) done")
                    .font(.caption)
                    .foregroundStyle(Palette.textSecondary)
            }
        }
        .onAppear {
            guard !drawn else { return }
            withAnimation(.spring(response: 1.2, dampingFraction: 0.85).delay(0.25)) { drawn = true }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("\(done) of \(days) days done")
    }
}

/// One day. Done days fill; the open day wears a soft ring.
private struct ChallengeDayCell: View {
    let day: Int
    let isDone: Bool
    let isSelected: Bool
    let select: () -> Void

    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        Button(action: select) {
            Text("\(day + 1)")
                .font(.caption.weight(.medium).monospacedDigit())
                .frame(maxWidth: .infinity, minHeight: 44)
                .foregroundStyle(isDone ? Palette.onAccentFill : Palette.textPrimary)
                .background {
                    ZStack {
                        RoundedRectangle(cornerRadius: 9, style: .continuous)
                            .fill(Palette.surface)
                        RoundedRectangle(cornerRadius: 9, style: .continuous)
                            .fill(Palette.accentFill)
                            .scaleEffect(isDone ? 1 : 0.2)
                            .opacity(isDone ? 1 : 0)
                    }
                }
                .overlay {
                    RoundedRectangle(cornerRadius: 9, style: .continuous)
                        .strokeBorder(Palette.blushStrong, lineWidth: 1.5)
                        .opacity(isSelected ? 1 : 0)
                }
                .keyframeAnimator(initialValue: 1.0, trigger: isDone) { [still = reduceMotion] view, scale in
                    view.scaleEffect(still ? 1 : scale)
                } keyframes: { _ in
                    SpringKeyframe(0.86, duration: 0.1)
                    SpringKeyframe(1.1, duration: 0.18, spring: .bouncy)
                    SpringKeyframe(1.0, duration: 0.3, spring: .smooth)
                }
                .animation(reduceMotion ? nil : .spring(response: 0.35, dampingFraction: 0.65), value: isDone)
                .animation(reduceMotion ? nil : .spring(response: 0.3, dampingFraction: 0.8), value: isSelected)
        }
        .buttonStyle(PressableStyle(scale: 0.97))
        .accessibilityLabel("Day \(day + 1)")
        .accessibilityValue(isDone ? "Done" : "Not done")
        .accessibilityAddTraits(isSelected ? .isSelected : [])
    }
}

/// All days done: a lotus draws itself in, with a few gold sparks.
private struct ChallengeComplete: View {
    let days: Int
    let again: () -> Void

    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var drawn = false
    @State private var fire = 0

    var body: some View {
        VStack(spacing: Space.m) {
            ZStack {
                ForEach(0..<12, id: \.self) { i in
                    let angle = Double(i) / 12 * 2 * .pi
                    Circle()
                        .fill(i.isMultiple(of: 2) ? Color(hex: 0xD3A94A) : Palette.accentFill)
                        .frame(width: 5, height: 5)
                        .keyframeAnimator(initialValue: PetalState(), trigger: fire) { spark, value in
                            spark
                                .offset(x: cos(angle) * value.distance, y: sin(angle) * value.distance)
                                .opacity(reduceMotion ? 0 : value.opacity)
                                .scaleEffect(value.scale)
                        } keyframes: { _ in
                            KeyframeTrack(\.distance) {
                                CubicKeyframe(10, duration: 0.01)
                                SpringKeyframe(64, duration: 0.7, spring: .snappy)
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
                LotusMark()
                    .trim(from: 0, to: drawn || reduceMotion ? 1 : 0)
                    .stroke(Palette.accent, style: StrokeStyle(lineWidth: 1.8, lineCap: .round, lineJoin: .round))
                    .frame(width: 64, height: 46)
            }
            .frame(height: 70)
            .accessibilityHidden(true)
            Text("You finished all \(days) days.")
                .font(AppFont.serif(.body, weight: .semibold))
                .foregroundStyle(Palette.accent)
                .multilineTextAlignment(.center)
            Button("Start Again", action: again)
                .buttonStyle(.tintedCompact)
        }
        .frame(maxWidth: .infinity)
        .padding(Space.xl)
        .background(
            RoundedRectangle(cornerRadius: Radius.card, style: .continuous)
                .fill(LinearGradient(colors: [Color(hex: 0xC9A24A).opacity(0.16), Palette.accentWash.opacity(0.6)],
                                     startPoint: .topLeading, endPoint: .bottomTrailing))
        )
        .onAppear {
            withAnimation(reduceMotion ? nil : .easeInOut(duration: 1.4).delay(0.2)) { drawn = true }
            Task {
                try? await Task.sleep(for: .seconds(0.9))
                fire += 1
            }
        }
    }
}
