import SwiftUI

/// Compact player that sits above the tab bar while recitation is active (before iOS 26.1).
struct MiniPlayerBar: View {
    static var usesSystemAccessory: Bool {
        if #available(iOS 26.1, *) { return true }
        return false
    }

    @Environment(QuranStore.self) private var quran
    @Environment(AudioPlayer.self) private var player
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var showFull = false

    var body: some View {
        Group {
            if let current = player.current {
                HStack(spacing: Space.s) {
                    Button {
                        showFull = true
                    } label: {
                        HStack(spacing: Space.s) {
                            Image(systemName: "waveform")
                                .font(.system(size: 15, weight: .semibold))
                                .foregroundStyle(Palette.accent)
                                .symbolEffect(.variableColor.iterative, options: .repeating, isActive: player.isPlaying && !reduceMotion)
                                .frame(width: 36, height: 36)
                                .background(Circle().fill(Palette.accentWash))
                            VStack(alignment: .leading, spacing: 1) {
                                Text(quran.surah(current.surah).name)
                                    .font(.subheadline.weight(.semibold))
                                    .foregroundStyle(Palette.textPrimary)
                                Text(player.didFail ? "Could not load audio" : "Verse \(current.ayah) · \(player.reciter.name)")
                                    .font(.caption)
                                    .foregroundStyle(player.didFail ? Palette.blushStrong : Palette.textSecondary)
                                    .lineLimit(1)
                                    .contentTransition(reduceMotion ? .identity : .numericText(value: Double(current.ayah)))
                                    .animation(reduceMotion ? nil : .snappy, value: current.ayah)
                            }
                            Spacer(minLength: 0)
                        }
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(PressableStyle(scale: 0.97))
                    .accessibilityLabel("Now playing \(quran.surah(current.surah).name), verse \(current.ayah)")
                    .accessibilityHint("Opens the player")

                    PlayPauseButton(size: 36)

                    Button {
                        player.stop()
                    } label: {
                        Image(systemName: "xmark")
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundStyle(Palette.textSecondary)
                            .frame(width: 36, height: 36)
                            .contentShape(Rectangle())
                    }
                    .buttonStyle(PressableStyle(scale: 0.9))
                    .accessibilityLabel("Stop recitation")
                }
                .padding(.leading, Space.xs)
                .padding(.trailing, Space.xs)
                .padding(.vertical, Space.xs)
                .background(.regularMaterial, in: Capsule())
                .overlay(Capsule().strokeBorder(Palette.border.opacity(0.6), lineWidth: 0.5))
                .shadow(color: .black.opacity(0.08), radius: 12, y: 4)
                .padding(.horizontal, Space.m)
                .padding(.bottom, Space.xs)
                .transition(.move(edge: .bottom).combined(with: .opacity))
            }
        }
        .animation(reduceMotion ? nil : Motion.smooth, value: player.current == nil)
        .sheet(isPresented: $showFull) {
            FullPlayerView()
                .presentationDragIndicator(.visible)
                .presentationBackground(Palette.background)
        }
    }
}

struct PlayPauseButton: View {
    var size: CGFloat = 36

    @Environment(AudioPlayer.self) private var player

    var body: some View {
        Button {
            player.togglePlayPause()
        } label: {
            ZStack {
                Circle().fill(Palette.accentFill)
                if player.isLoading {
                    ProgressView()
                        .tint(Palette.onAccentFill)
                        .controlSize(size > 50 ? .regular : .small)
                } else {
                    Image(systemName: player.isPlaying ? "pause.fill" : "play.fill")
                        .font(.system(size: size * 0.38, weight: .semibold))
                        .foregroundStyle(Palette.onAccentFill)
                        .contentTransition(.symbolEffect(.replace))
                }
            }
            .frame(width: size, height: size)
        }
        .buttonStyle(PressableStyle(scale: 0.92))
        .accessibilityLabel(player.isPlaying ? "Pause" : "Play")
    }
}

/// Focused playback: the verse being recited, with simple controls.
/// Motion: each new verse rises in softly out of a blur while the last one lifts away,
/// and its Arabic inks in right to left. The verse number rolls, a slow glow breathes
/// behind the play button while recitation plays, and the skip, repeat and reciter
/// symbols give a small bounce when used.
struct FullPlayerView: View {
    @Environment(QuranStore.self) private var quran
    @Environment(AudioPlayer.self) private var player
    @Environment(\.dismiss) private var dismiss
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    @AppStorage(PrefKey.arabicSize) private var arabicSize = ReaderDefaults.arabicSize
    @AppStorage(PrefKey.haptics) private var hapticsOn = true

    @State private var backTaps = 0
    @State private var nextTaps = 0
    @State private var showRange = false

    var body: some View {
        VStack(spacing: 0) {
            if let current = player.current {
                ScrollView {
                    VStack(spacing: Space.xl) {
                        VStack(spacing: 4) {
                            Text(quran.surah(current.surah).name)
                                .font(.system(.title3, design: .serif, weight: .semibold))
                            Text("Verse \(current.ayah) of \(quran.surah(current.surah).verseCount)")
                                .font(.subheadline)
                                .monospacedDigit()
                                .foregroundStyle(Palette.textSecondary)
                                .contentTransition(reduceMotion ? .identity : .numericText(value: Double(current.ayah)))
                        }
                        .accessibilityElement(children: .combine)

                        // Old and new verse overlap while they cross, so nothing jumps.
                        ZStack {
                            VStack(spacing: Space.xl) {
                                ArabicText(text: "\(quran.text(current)) ﴿\(current.ayah.arabicIndic)﴾", size: max(arabicSize, 30), placement: .center)
                                    .inkReveal(delay: 0.12, duration: 0.95)
                                TranslationBody(ref: current)
                            }
                            .id(current)
                            .transition(verseTransition)
                        }
                    }
                    .padding(.horizontal, Space.xl)
                    .padding(.top, Space.xxl)
                    .padding(.bottom, Space.xl)
                }
                .animation(reduceMotion ? Motion.gentle : Motion.smooth, value: current)

                controls
            } else {
                ContentUnavailableView("Nothing playing", systemImage: "headphones")
                    .onAppear { dismiss() }
            }
        }
        .sheet(isPresented: $showRange) {
            if let current = player.current {
                RangeRepeatSheet(surahID: current.surah, startAyah: current.ayah)
            }
        }
    }

    /// The new verse rises from a soft blur, the old one lifts away. A plain fade with Reduce Motion.
    private var verseTransition: AnyTransition {
        if reduceMotion { return .opacity }
        return .asymmetric(
            insertion: .opacity.combined(with: .offset(y: 18)).combined(with: .blurred),
            removal: .opacity.combined(with: .offset(y: -18)).combined(with: .blurred)
        )
    }

    private var controls: some View {
        @Bindable var player = player

        return VStack(spacing: Space.l) {
            if player.didFail {
                Label("Could not load audio.", systemImage: "wifi.exclamationmark")
                    .font(.footnote)
                    .foregroundStyle(Palette.blushStrong)
                    .multilineTextAlignment(.center)
                    .transition(.opacity)
            }

            HStack(spacing: Space.xxl) {
                Button {
                    if !reduceMotion { backTaps += 1 }
                    player.previous()
                } label: {
                    Image(systemName: "backward.fill")
                        .font(.title2)
                        .symbolEffect(.bounce, value: backTaps)
                }
                .accessibilityLabel("Previous verse")

                PlayPauseButton(size: 72)
                    .background {
                        BreathingGlow(active: player.isPlaying)
                    }

                Button {
                    if !reduceMotion { nextTaps += 1 }
                    player.next()
                } label: {
                    Image(systemName: "forward.fill")
                        .font(.title2)
                        .symbolEffect(.bounce, value: nextTaps)
                }
                .accessibilityLabel("Next verse")
            }
            .foregroundStyle(Palette.accent)
            .buttonStyle(PressableStyle(scale: 0.9))

            HStack {
                Menu {
                    Section("Repeat each verse") {
                        ForEach([AudioPlayer.RepeatMode.off, .times(3), .times(5), .forever], id: \.self) { mode in
                            Button {
                                if player.loopRange != nil { player.clearLoop() }
                                player.repeatMode = mode
                            } label: {
                                if player.loopRange == nil && player.repeatMode == mode {
                                    Label(mode.title, systemImage: "checkmark")
                                } else {
                                    Text(mode.title)
                                }
                            }
                        }
                    }
                    Button {
                        showRange = true
                    } label: {
                        if player.loopRange != nil {
                            Label("A range of verses", systemImage: "checkmark")
                        } else {
                            Text("A range of verses")
                        }
                    }
                } label: {
                    Label("Repeat: \(player.loopTitle ?? player.repeatMode.title)", systemImage: "repeat")
                        .font(.subheadline.weight(.medium))
                        .lineLimit(1)
                        .contentTransition(.opacity)
                        .symbolEffect(.bounce, value: reduceMotion ? "" : (player.loopTitle ?? player.repeatMode.title))
                }
                .accessibilityLabel("Repeat, \(player.loopTitle ?? player.repeatMode.title)")
                Spacer()
                Menu {
                    Picker("Reciter", selection: $player.reciterID) {
                        ForEach(Reciter.all) { Text($0.name).tag($0.id) }
                    }
                } label: {
                    Label(player.reciter.name, systemImage: "person.wave.2")
                        .font(.subheadline.weight(.medium))
                        .lineLimit(1)
                        .contentTransition(.opacity)
                        .symbolEffect(.bounce, value: reduceMotion ? "" : player.reciterID)
                }
                .accessibilityLabel("Reciter, \(player.reciter.name)")
            }
            .foregroundStyle(Palette.accent)
            .animation(reduceMotion ? nil : Motion.gentle, value: player.repeatMode)
            .animation(reduceMotion ? nil : Motion.gentle, value: player.reciterID)
            .sensoryFeedback(.selection, trigger: player.repeatMode) { _, _ in hapticsOn }
            .sensoryFeedback(.selection, trigger: player.reciterID) { _, _ in hapticsOn }

            PlayerExtrasRow()
        }
        .padding(.horizontal, Space.xl)
        .padding(.top, Space.l)
        .padding(.bottom, Space.xl)
        .background(Palette.surface.opacity(0.6))
        .animation(reduceMotion ? nil : Motion.gentle, value: player.didFail)
    }
}

/// A soft emerald light behind the play button that slowly swells and settles while
/// recitation plays, and fades away on pause. Still and faint with Reduce Motion.
private struct BreathingGlow: View {
    let active: Bool

    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        ZStack {
            if active {
                if reduceMotion {
                    glow
                        .opacity(0.5)
                        .transition(.opacity)
                } else {
                    glow
                        .phaseAnimator([false, true]) { view, swelled in
                            view
                                .scaleEffect(swelled ? 1.4 : 1.05)
                                .opacity(swelled ? 0.85 : 0.4)
                        } animation: { _ in
                            .easeInOut(duration: 2.2)
                        }
                        .transition(.opacity)
                }
            }
        }
        .animation(Motion.smooth, value: active)
        .allowsHitTesting(false)
        .accessibilityHidden(true)
    }

    private var glow: some View {
        Circle()
            .fill(Palette.accent.opacity(0.4))
            .blur(radius: 16)
    }
}

/// The mini player as the tab bar's Liquid Glass bottom accessory (iOS 26.1 and later).
/// It adapts when the tab bar shrinks into a single line.
@available(iOS 26.0, *)
struct MiniPlayerAccessory: View {
    @Environment(QuranStore.self) private var quran
    @Environment(AudioPlayer.self) private var player
    @Environment(\.tabViewBottomAccessoryPlacement) private var placement
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var showFull = false
    @State private var nextTaps = 0

    var body: some View {
        if let current = player.current {
            HStack(spacing: Space.s) {
                Button {
                    showFull = true
                } label: {
                    HStack(spacing: Space.s) {
                        Image(systemName: "waveform")
                            .foregroundStyle(Palette.accent)
                            .symbolEffect(.variableColor.iterative, options: .repeating, isActive: player.isPlaying && !reduceMotion)
                        VStack(alignment: .leading, spacing: 0) {
                            Text(quran.surah(current.surah).name)
                                .font(.subheadline.weight(.semibold))
                            if placement != .inline {
                                Text(player.didFail ? "Could not load audio" : "Verse \(current.ayah) · \(player.reciter.name)")
                                    .font(.caption)
                                    .foregroundStyle(player.didFail ? AnyShapeStyle(Palette.blushStrong) : AnyShapeStyle(.secondary))
                                    .lineLimit(1)
                                    .contentTransition(reduceMotion ? .identity : .numericText(value: Double(current.ayah)))
                                    .animation(reduceMotion ? nil : .snappy, value: current.ayah)
                            }
                        }
                        Spacer(minLength: 0)
                    }
                    .contentShape(Rectangle())
                }
                .buttonStyle(PressableStyle(scale: 0.97))
                .accessibilityLabel("Now playing \(quran.surah(current.surah).name), verse \(current.ayah)")

                Button {
                    player.togglePlayPause()
                } label: {
                    Group {
                        if player.isLoading {
                            ProgressView().controlSize(.small)
                        } else {
                            Image(systemName: player.isPlaying ? "pause.fill" : "play.fill")
                                .contentTransition(.symbolEffect(.replace))
                        }
                    }
                    .frame(width: 32, height: 32)
                }
                .accessibilityLabel(player.isLoading ? "Loading" : (player.isPlaying ? "Pause" : "Play"))

                if placement != .inline {
                    Button {
                        if !reduceMotion { nextTaps += 1 }
                        player.next()
                    } label: {
                        Image(systemName: "forward.fill")
                            .symbolEffect(.bounce, value: nextTaps)
                            .frame(width: 32, height: 32)
                    }
                    .accessibilityLabel("Next verse")
                }
            }
            .padding(.horizontal, Space.m)
            .sheet(isPresented: $showFull) {
                FullPlayerView()
                    .presentationDragIndicator(.visible)
                    .presentationBackground(Palette.background)
            }
        }
    }
}
