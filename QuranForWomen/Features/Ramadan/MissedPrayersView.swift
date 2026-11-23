import SwiftUI

/// Her own count of prayers to make up. She sets the numbers; each one she marks counts down. Private.
struct MissedPrayersView: View {
    @Environment(MissedPrayersStore.self) private var store
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @AppStorage(PrefKey.haptics) private var hapticsOn = true

    @State private var editing = false
    @State private var markTick = 0
    @State private var marked: Prayer?
    @State private var undoToken = 0

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: Space.xl) {
                summary
                    .dealIn(0, baseDelay: 0.02)
                    .scrollSettle()

                GroupedSection {
                    ForEach(Array(Prayer.prayers.enumerated()), id: \.element) { i, prayer in
                        if i > 0 { RowDivider() }
                        MissedPrayerRow(prayer: prayer, editing: editing) { mark(prayer) }
                            .dealIn(i, baseDelay: 0.26, step: 0.05)
                    }
                }
                .dealIn(1, baseDelay: 0.2)
                .scrollSettle()
            }
            .padding(.horizontal, Space.page)
            .padding(.vertical, Space.s)
            .animation(reduceMotion ? nil : Motion.gentle, value: editing)
        }
        .scrollDismissesKeyboard(.interactively)
        .pageBackground()
        .navigationTitle("Make-up Prayers")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button(editing ? "Done" : "Edit") { editing.toggle() }
                    .fontWeight(editing ? .semibold : .regular)
            }
        }
        .overlay(alignment: .bottom) {
            if let marked {
                UndoBar(text: "\(marked.name) marked") {
                    withAnimation(reduceMotion ? nil : Motion.gentle) {
                        store.undoOne(marked)
                        self.marked = nil
                    }
                    undoToken += 1
                }
                .padding(.horizontal, Space.page)
                .padding(.bottom, Space.m)
                .transition(.move(edge: .bottom).combined(with: .opacity))
            }
        }
        .animation(reduceMotion ? nil : .spring(response: 0.4, dampingFraction: 0.8), value: marked)
        .sensoryFeedback(.success, trigger: markTick) { _, _ in hapticsOn }
        .sensoryFeedback(.selection, trigger: editing) { _, _ in hapticsOn }
    }

    private var summary: some View {
        HStack(spacing: 0) {
            figure(store.totalRemaining, "to make up", delay: 0.3)
            Divider().frame(height: 34)
            figure(store.madeUp, "made up", delay: 0.4)
        }
        .cardSurface(padding: Space.xl)
    }

    private func figure(_ value: Int, _ label: String, delay: Double) -> some View {
        VStack(spacing: 2) {
            RollingNumber(value: value, delay: delay)
                .font(.system(size: 28, weight: .light, design: .serif))
                .foregroundStyle(Palette.textPrimary)
            Text(label)
                .font(.caption)
                .foregroundStyle(Palette.textSecondary)
        }
        .frame(maxWidth: .infinity)
        .accessibilityElement(children: .combine)
        .focusIn(delay: delay - 0.15)
    }

    private func mark(_ prayer: Prayer) {
        var done = false
        withAnimation(reduceMotion ? nil : .spring(response: 0.35, dampingFraction: 0.62)) {
            done = store.markOne(prayer)
        }
        guard done else { return }
        markTick += 1
        marked = prayer
        undoToken += 1
        let token = undoToken
        Task {
            try? await Task.sleep(for: .seconds(4))
            if token == undoToken { marked = nil }
        }
    }
}

private extension Prayer {
    var missedSymbol: String {
        switch self {
        case .fajr: "sun.horizon"
        case .sunrise: "sunrise"
        case .dhuhr: "sun.max"
        case .asr: "sun.min"
        case .maghrib: "sunset"
        case .isha: "moon.stars"
        }
    }
}

private struct MissedPrayerRow: View {
    let prayer: Prayer
    let editing: Bool
    let mark: () -> Void

    @Environment(MissedPrayersStore.self) private var store
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var burst = false

    var body: some View {
        let left = store.remaining(prayer)
        HStack(spacing: Space.m) {
            Image(systemName: prayer.missedSymbol)
                .font(.system(size: 17))
                .foregroundStyle(Palette.accent)
                .frame(width: 26)
                .accessibilityHidden(true)
            Text(prayer.name)
                .foregroundStyle(Palette.textPrimary)
            Spacer(minLength: Space.xs)
            if editing {
                TextField("0", value: Binding(
                    get: { store.remaining(prayer) },
                    set: { store.setRemaining(prayer, $0) }
                ), format: .number)
                .keyboardType(.numberPad)
                .multilineTextAlignment(.trailing)
                .monospacedDigit()
                .padding(.horizontal, Space.s)
                .frame(width: 96, height: 44)
                .background(RoundedRectangle(cornerRadius: Radius.control, style: .continuous).fill(Palette.accentWash))
                .accessibilityLabel("\(prayer.name) prayers to make up")
                .transition(.scale(scale: 0.9).combined(with: .opacity))
            } else {
                RollingNumber(value: left)
                    .foregroundStyle(left > 0 ? Palette.textPrimary : Palette.textSecondary)
                    .transition(.opacity)
                Button {
                    burst.toggle()
                    mark()
                } label: {
                    Image(systemName: "checkmark")
                        .font(.system(size: 13, weight: .bold))
                        .foregroundStyle(Palette.onAccentFill)
                        .frame(width: 30, height: 30)
                        .background(Circle().fill(Palette.accentFill))
                        .keyframeAnimator(initialValue: 1.0, trigger: burst) { [still = reduceMotion] view, scale in
                            view.scaleEffect(still ? 1 : scale)
                        } keyframes: { _ in
                            SpringKeyframe(0.82, duration: 0.1)
                            SpringKeyframe(1.12, duration: 0.18, spring: .bouncy)
                            SpringKeyframe(1.0, duration: 0.3, spring: .smooth)
                        }
                        .overlay { PetalBurst(trigger: burst, active: true) }
                        .frame(width: 44, height: 44)
                        .contentShape(Rectangle())
                }
                .buttonStyle(PressableStyle(scale: 0.9))
                .disabled(left == 0)
                .opacity(left == 0 ? 0.35 : 1)
                .accessibilityLabel("Mark one \(prayer.name) prayed")
                .transition(.scale.combined(with: .opacity))
            }
        }
        .padding(.leading, Space.m)
        .padding(.trailing, Space.xs)
        .frame(minHeight: 56)
    }
}
