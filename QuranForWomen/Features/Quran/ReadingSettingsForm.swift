import SwiftUI

/// Text size, translation visibility and reciter, with a live preview.
/// Kept out of the reading surface on purpose.
/// Motion: the preview verse inks in, then grows and shrinks smoothly as she moves
/// a slider. The translation fades in and out with its toggle, and the chosen
/// reciter's checkmark pops into place.
struct ReadingSettingsForm: View {
    var showsLanguageLink = true
    /// The surah open in the reader, so its recitation can be downloaded from here.
    var surahID: Int? = nil

    @Environment(QuranStore.self) private var quran
    @Environment(AudioPlayer.self) private var player
    @Environment(TranslationStore.self) private var translations
    @Environment(TransliterationStore.self) private var transliteration
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    @AppStorage(PrefKey.arabicSize) private var arabicSize = ReaderDefaults.arabicSize
    @AppStorage(PrefKey.translationSize) private var translationSize = ReaderDefaults.translationSize
    @AppStorage(PrefKey.showTranslation) private var showTranslation = true
    @AppStorage(PrefKey.haptics) private var hapticsOn = true

    private let sample = VerseRef(surah: 1, ayah: 2)

    var body: some View {
        Form {
            Section {
                VStack(alignment: .leading, spacing: Space.m) {
                    SmoothSize(size: arabicSize) { size in
                        ArabicText(text: "\(quran.text(sample)) ﴿\(sample.ayah.arabicIndic)﴾", size: size)
                    }
                    .inkReveal(delay: 0.2, duration: 0.9)
                    if transliteration.show {
                        SmoothSize(size: translationSize) { size in
                            TransliterationLine(ref: sample, size: size)
                        }
                        .transition(reduceMotion ? AnyTransition.opacity : AnyTransition.opacity.combined(with: .blurred))
                    }
                    if showTranslation {
                        SmoothSize(size: translationSize) { size in
                            TranslationBody(ref: sample, size: size)
                        }
                        .transition(reduceMotion ? AnyTransition.opacity : AnyTransition.opacity.combined(with: .blurred))
                    }
                }
                .padding(.vertical, Space.xs)
                .animation(reduceMotion ? nil : .smooth(duration: 0.3), value: arabicSize)
                .animation(reduceMotion ? nil : .smooth(duration: 0.3), value: translationSize)
                .animation(Motion.gentle, value: showTranslation)
                .animation(Motion.gentle, value: transliteration.show)
            } header: {
                Text("Preview")
            }
            .listRowBackground(Palette.surface)

            Section("Arabic text size") {
                SizeSlider(value: $arabicSize, range: ReaderDefaults.arabicRange, label: "Arabic text size")
            }
            .listRowBackground(Palette.surface)

            Section {
                Toggle("Show translation", isOn: $showTranslation.animation(reduceMotion ? nil : Motion.gentle))
                if showTranslation {
                    SizeSlider(value: $translationSize, range: ReaderDefaults.translationRange, label: "Translation text size")
                }
                if showsLanguageLink {
                    NavigationLink(value: Route.language) {
                        LabeledContent("Language", value: translations.selected.nativeName)
                    }
                }
                TransliterationToggleRows()
            } header: {
                Text("Translation")
            }
            .listRowBackground(Palette.surface)

            Section {
                ForEach(Reciter.all) { reciter in
                    ReciterRow(name: reciter.name, isSelected: player.reciterID == reciter.id) {
                        guard player.reciterID != reciter.id else { return }
                        withAnimation(reduceMotion ? nil : Motion.reward) {
                            player.reciterID = reciter.id
                        }
                    }
                }
                PlaybackSpeedRow()
                if let surahID {
                    SurahRecitationDownloadRow(surahID: surahID)
                }
            } header: {
                Text("Recitation")
            }
            .listRowBackground(Palette.surface)

            Section {
                Button("Reset to default sizes") {
                    withAnimation(reduceMotion ? nil : Motion.smooth) {
                        arabicSize = ReaderDefaults.arabicSize
                        translationSize = ReaderDefaults.translationSize
                    }
                }
                .disabled(arabicSize == ReaderDefaults.arabicSize && translationSize == ReaderDefaults.translationSize)
            }
            .listRowBackground(Palette.surface)
        }
        .scrollContentBackground(.hidden)
        .pageBackground()
        .sensoryFeedback(.selection, trigger: player.reciterID) { _, _ in hapticsOn }
    }
}

/// Lets a text size glide between values instead of jumping, by animating the number
/// the content is drawn at.
private struct SmoothSize<Content: View>: View, Animatable {
    var size: Double
    let content: (CGFloat) -> Content

    init(size: Double, @ViewBuilder content: @escaping (CGFloat) -> Content) {
        self.size = size
        self.content = content
    }

    var animatableData: Double {
        get { size }
        set { size = newValue }
    }

    var body: some View {
        content(CGFloat(size))
    }
}

/// One reciter. The checkmark pops in with a small bounce when she picks this voice.
private struct ReciterRow: View {
    let name: String
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack {
                Text(name)
                    .foregroundStyle(Palette.textPrimary)
                Spacer()
                if isSelected {
                    Image(systemName: "checkmark")
                        .font(.body.weight(.semibold))
                        .foregroundStyle(Palette.accent)
                        .transition(.scale(scale: 0.3).combined(with: .opacity))
                        .accessibilityHidden(true)
                }
            }
            .contentShape(Rectangle())
        }
        .accessibilityLabel(name)
        .accessibilityAddTraits(isSelected ? .isSelected : [])
        .accessibilityHint(isSelected ? "" : "Uses this reciter")
    }
}

/// A size slider with the current size beside it. The number rolls as she slides.
private struct SizeSlider: View {
    @Binding var value: Double
    let range: ClosedRange<Double>
    let label: String

    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @AppStorage(PrefKey.haptics) private var hapticsOn = true

    var body: some View {
        HStack(spacing: Space.m) {
            Image(systemName: "textformat.size.smaller")
                .foregroundStyle(Palette.textSecondary)
                .accessibilityHidden(true)
            Slider(value: $value, in: range, step: 1)
                .accessibilityLabel(label)
                .accessibilityValue("\(Int(value)) points")
            Image(systemName: "textformat.size.larger")
                .foregroundStyle(Palette.textSecondary)
                .accessibilityHidden(true)
            Text("\(Int(value))")
                .font(.subheadline.weight(.medium))
                .monospacedDigit()
                .foregroundStyle(Palette.accent)
                .contentTransition(reduceMotion ? .identity : .numericText(value: value))
                .animation(reduceMotion ? nil : Motion.press, value: value)
                .frame(minWidth: 24, alignment: .trailing)
                .accessibilityHidden(true)
        }
        .sensoryFeedback(.selection, trigger: value) { _, _ in hapticsOn }
    }
}

/// The same form, as a pushed screen in You. It carries the form's motion as is.
struct ReadingSettingsView: View {
    var body: some View {
        ReadingSettingsForm()
            .navigationTitle("Text Size & Recitation")
            .navigationBarTitleDisplayMode(.inline)
    }
}
