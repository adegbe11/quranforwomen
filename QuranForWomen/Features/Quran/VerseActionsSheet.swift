import SwiftUI
import UIKit

extension HighlightColor {
    /// Soft fills that keep text readable in both appearances.
    var fill: Color {
        switch self {
        case .emerald: Color(light: 0xDCEDE5, dark: 0x1C3329)
        case .blush: Color(light: 0xF7E1E8, dark: 0x3D2830)
        case .gold: Color(light: 0xF6ECCF, dark: 0x3A3220)
        case .sky: Color(light: 0xDFEAF4, dark: 0x1E2B38)
        }
    }

    var swatch: Color {
        switch self {
        case .emerald: Color(hex: 0x3E9C7A)
        case .blush: Color(hex: 0xD98AA2)
        case .gold: Color(hex: 0xD3A94A)
        case .sky: Color(hex: 0x6E9CC6)
        }
    }
}

/// The small set of actions that appears when a verse is tapped.
/// Motion: the verse card comes into focus with its Arabic inked in, and tints live as
/// she picks a highlight. The action tiles are dealt in one by one, Save sends up a few
/// petals, and Share turns into a check once the verse is copied.
struct VerseActionsSheet: View {
    let ref: VerseRef

    @Environment(QuranStore.self) private var quran
    @Environment(TranslationStore.self) private var translations
    @Environment(LibraryStore.self) private var library
    @Environment(AudioPlayer.self) private var player
    @Environment(\.dismiss) private var dismiss
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @AppStorage(PrefKey.haptics) private var hapticsOn = true

    @State private var copied = false
    @State private var editingNote = false
    @State private var shareImage: UIImage?
    @State private var journaling: JournalStart?
    @State private var showRange = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: Space.l) {
                VStack(alignment: .leading, spacing: Space.s) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("\(quran.surah(ref.surah).name) · Verse \(ref.ayah)")
                            .font(.headline)
                            .foregroundStyle(Palette.textPrimary)
                        Text("\(quran.surah(ref.surah).meaning) · Juz \(quran.juz(of: ref)) · Page \(quran.page(of: ref))")
                            .font(.subheadline)
                            .foregroundStyle(Palette.textSecondary)
                    }
                    .accessibilityElement(children: .combine)

                    ArabicText(text: quran.text(ref), size: 22, lineLimit: 2)
                        .inkReveal(delay: 0.2, duration: 0.9)
                }
                .padding(Space.m)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(
                    RoundedRectangle(cornerRadius: Radius.tile, style: .continuous)
                        .fill(library.highlight(ref)?.fill ?? Palette.surface)
                )
                .animation(reduceMotion ? nil : Motion.gentle, value: library.highlight(ref))
                .focusIn()
                .scrollSettle()

                HStack(alignment: .top, spacing: Space.xs) {
                    ActionTile(title: "Play", systemImage: "play.fill") {
                        player.repeatMode = .off
                        player.play(ref)
                        dismiss()
                    }
                    .dealIn(0, baseDelay: 0.08, step: 0.05)

                    Menu {
                        Section("Repeat this verse") {
                            Button("3 times") { repeatVerse(.times(3)) }
                            Button("5 times") { repeatVerse(.times(5)) }
                            Button("10 times") { repeatVerse(.times(10)) }
                            Button("Until I stop") { repeatVerse(.forever) }
                        }
                        Section("Repeat a range") {
                            Button {
                                showRange = true
                            } label: {
                                Label("Choose verses", systemImage: "repeat")
                            }
                        }
                    } label: {
                        ActionTileLabel(title: "Repeat", systemImage: "repeat")
                    }
                    .buttonStyle(.pressable)
                    .dealIn(1, baseDelay: 0.08, step: 0.05)

                    let saved = library.isBookmarked(ref)
                    ActionTile(
                        title: saved ? "Saved" : "Save",
                        systemImage: saved ? "bookmark.fill" : "bookmark",
                        isActive: saved
                    ) {
                        withAnimation(reduceMotion ? nil : Motion.reward) {
                            library.toggleBookmark(ref)
                        }
                    }
                    .overlay { PetalBurst(trigger: saved, active: saved) }
                    .sensoryFeedback(.selection, trigger: saved) { _, _ in hapticsOn }
                    .accessibilityHint(saved ? "Removes this verse from Saved" : "Keeps this verse in Saved")
                    .dealIn(2, baseDelay: 0.08, step: 0.05)

                    ActionTile(
                        title: library.note(ref) == nil ? "Note" : "Edit note",
                        systemImage: library.note(ref) == nil ? "square.and.pencil" : "note.text",
                        isActive: library.note(ref) != nil
                    ) {
                        editingNote = true
                    }
                    .dealIn(3, baseDelay: 0.08, step: 0.05)

                    Menu {
                        ShareLink(item: shareText) {
                            Label("Share as text", systemImage: "text.alignleft")
                        }
                        if let shareImage {
                            ShareLink(
                                item: Image(uiImage: shareImage),
                                preview: SharePreview(quran.displayName(ref), image: Image(uiImage: shareImage))
                            ) {
                                Label("Share as image", systemImage: "photo")
                            }
                        }
                        Button {
                            UIPasteboard.general.string = shareText
                            withAnimation(reduceMotion ? nil : Motion.reward) {
                                copied = true
                            }
                        } label: {
                            Label("Copy", systemImage: "doc.on.doc")
                        }
                    } label: {
                        ActionTileLabel(title: copied ? "Copied" : "Share", systemImage: copied ? "checkmark" : "square.and.arrow.up", isActive: copied)
                    }
                    .buttonStyle(.pressable)
                    .sensoryFeedback(.success, trigger: copied) { _, new in new && hapticsOn }
                    .dealIn(4, baseDelay: 0.08, step: 0.05)
                }

                Button {
                    journaling = .verse(ref)
                } label: {
                    Label("Reflect in my journal", systemImage: "square.and.pencil")
                }
                .buttonStyle(.tinted)
                .dealIn(5, baseDelay: 0.08, step: 0.05)

                HighlightPicker(selection: library.highlight(ref)) { color in
                    withAnimation(reduceMotion ? nil : Motion.gentle) {
                        library.setHighlight(color, for: ref)
                    }
                }
                .dealIn(6, baseDelay: 0.08, step: 0.05)

                if let note = library.note(ref) {
                    Button { editingNote = true } label: {
                        VStack(alignment: .leading, spacing: Space.xxs) {
                            Text("Your note")
                                .font(.footnote.weight(.semibold))
                                .foregroundStyle(Palette.textSecondary)
                            Text(note.text)
                                .font(AppFont.serif(.body))
                                .foregroundStyle(Palette.textPrimary)
                                .multilineTextAlignment(.leading)
                        }
                        .cardSurface(padding: Space.m)
                    }
                    .buttonStyle(.pressable)
                    .transition(reduceMotion ? AnyTransition.opacity : AnyTransition.opacity.combined(with: .scale(scale: 0.96, anchor: .top)))
                    .scrollSettle()
                }
            }
            .padding(.horizontal, Space.page)
            .padding(.top, Space.xl)
            .padding(.bottom, Space.l)
            .animation(reduceMotion ? nil : Motion.gentle, value: library.note(ref))
        }
        .scrollBounceBehavior(.basedOnSize)
        .sheet(isPresented: $editingNote) {
            VerseNoteEditor(ref: ref)
        }
        .sheet(isPresented: $showRange) {
            RangeRepeatSheet(surahID: ref.surah, startAyah: ref.ayah) { dismiss() }
        }
        .fullScreenCover(item: $journaling) { JournalStartView(start: $0) }
        .task { renderShareImage() }
    }

    private func repeatVerse(_ mode: AudioPlayer.RepeatMode) {
        player.repeatVerse(ref, mode: mode)
        dismiss()
    }

    private var shareText: String {
        var parts = ["\(quran.text(ref)) ﴿\(ref.ayah.arabicIndic)﴾"]
        if let translation = translations.text(ref) { parts.append(translation) }
        parts.append("Quran \(ref.surah):\(ref.ayah) · \(quran.surah(ref.surah).name)")
        return parts.joined(separator: "\n\n")
    }

    private func renderShareImage() {
        let card = VerseShareCard(
            arabic: "\(quran.text(ref)) ﴿\(ref.ayah.arabicIndic)﴾",
            translation: translations.text(ref),
            reference: "\(quran.surah(ref.surah).name) · \(ref.surah):\(ref.ayah)"
        )
        let renderer = ImageRenderer(content: card)
        renderer.scale = 3
        shareImage = renderer.uiImage
    }
}

/// Four soft colors and a clear option. The selection ring glides from one swatch
/// to the next, and the chosen swatch swells a little.
private struct HighlightPicker: View {
    let selection: HighlightColor?
    let onSelect: (HighlightColor?) -> Void

    @Namespace private var ring
    @AppStorage(PrefKey.haptics) private var hapticsOn = true

    var body: some View {
        HStack(spacing: Space.s) {
            Text("Highlight")
                .font(.subheadline.weight(.medium))
                .foregroundStyle(Palette.textSecondary)
            Spacer()
            ForEach(HighlightColor.allCases) { color in
                Button {
                    onSelect(selection == color ? nil : color)
                } label: {
                    Circle()
                        .fill(color.swatch)
                        .frame(width: 30, height: 30)
                        .scaleEffect(selection == color ? 1.08 : 1)
                        .overlay {
                            if selection == color {
                                Circle()
                                    .strokeBorder(Palette.textPrimary, lineWidth: 2)
                                    .padding(-4)
                                    .matchedGeometryEffect(id: "ring", in: ring)
                            }
                        }
                        .frame(width: 44, height: 44)
                        .contentShape(Rectangle())
                }
                .buttonStyle(PressableStyle(scale: 0.9))
                .accessibilityLabel("\(color.name) highlight")
                .accessibilityAddTraits(selection == color ? .isSelected : [])
            }
        }
        .sensoryFeedback(.selection, trigger: selection) { _, _ in hapticsOn }
    }
}

/// A calm, branded card used when sharing a verse as an image.
/// It stays still on purpose: it is drawn straight to an image, and any entrance
/// motion here could leave the shared picture blank or half faded.
struct VerseShareCard: View {
    let arabic: String
    let translation: String?
    let reference: String

    var body: some View {
        VStack(spacing: 24) {
            Text(arabic)
                .font(.custom(AppFont.arabicFamily, size: 30))
                .foregroundStyle(Color(hex: 0x17251F))
                .multilineTextAlignment(.center)
                .lineSpacing(10)
                .environment(\.layoutDirection, .rightToLeft)
            if let translation {
                Text(translation)
                    .font(.system(size: 17, design: .serif))
                    .foregroundStyle(Color(hex: 0x17251F))
                    .multilineTextAlignment(.center)
                    .lineSpacing(4)
            }
            VStack(spacing: 4) {
                Text(reference)
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(Color(hex: 0x064E3B))
                Text("Quran for Women")
                    .font(.system(size: 12))
                    .foregroundStyle(Color(hex: 0x5F6D66))
            }
        }
        .padding(40)
        .frame(width: 400)
        .background(Color(hex: 0xFAF7F0))
        .overlay(alignment: .top) {
            Rectangle().fill(Color(hex: 0x064E3B)).frame(height: 6)
        }
    }
}

/// Writing a note on a verse. The Arabic inks in, the writing box comes into focus
/// and its border warms to emerald while she types, and the character count rolls.
struct VerseNoteEditor: View {
    let ref: VerseRef

    @Environment(QuranStore.self) private var quran
    @Environment(LibraryStore.self) private var library
    @Environment(\.dismiss) private var dismiss
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var text = ""
    @FocusState private var focused: Bool

    var body: some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: Space.m) {
                ArabicText(text: quran.text(ref), size: 22, lineLimit: 3)
                    .inkReveal(delay: 0.1, duration: 0.9)
                TextEditor(text: $text)
                    .font(AppFont.serif(.body))
                    .scrollContentBackground(.hidden)
                    .padding(Space.s)
                    .background(RoundedRectangle(cornerRadius: Radius.control, style: .continuous).fill(Palette.surface))
                    .overlay(
                        RoundedRectangle(cornerRadius: Radius.control, style: .continuous)
                            .strokeBorder(focused ? Palette.accent.opacity(0.55) : Palette.border, lineWidth: focused ? 1.5 : 1)
                    )
                    .animation(reduceMotion ? nil : Motion.gentle, value: focused)
                    .focused($focused)
                    .focusIn(delay: 0.12)
                Text(text.count == 1 ? "1 character" : "\(text.count) characters")
                    .monospacedDigit()
                    .contentTransition(reduceMotion ? .identity : .numericText(value: Double(text.count)))
                    .animation(reduceMotion ? nil : Motion.press, value: text.count)
                    .font(.caption)
                    .foregroundStyle(Palette.textSecondary)
                    .frame(maxWidth: .infinity, alignment: .trailing)
            }
            .padding(Space.page)
            .pageBackground()
            .navigationTitle("Note on \(ref.id)")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        library.setNote(text, for: ref)
                        dismiss()
                    }
                }
            }
        }
        .onAppear {
            text = library.note(ref)?.text ?? ""
            focused = true
        }
    }
}

/// A tappable action tile.
private struct ActionTile: View {
    let title: String
    let systemImage: String
    var isActive = false
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            ActionTileLabel(title: title, systemImage: systemImage, isActive: isActive)
        }
        .buttonStyle(.pressable)
    }
}

/// The look of an action tile. Its symbol morphs and gives one small bounce
/// when the tile turns on or off, and its fill blushes softly.
private struct ActionTileLabel: View {
    let title: String
    let systemImage: String
    var isActive = false

    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        VStack(spacing: Space.xs) {
            Image(systemName: systemImage)
                .font(.system(size: 19, weight: .medium))
                .contentTransition(.symbolEffect(.replace))
                .symbolEffect(.bounce, value: reduceMotion ? false : isActive)
                .frame(height: 24)
            Text(title)
                .font(.caption.weight(.medium))
                .lineLimit(1)
                .minimumScaleFactor(0.7)
        }
        .foregroundStyle(Palette.accent)
        .frame(maxWidth: .infinity, minHeight: 72)
        .background(
            RoundedRectangle(cornerRadius: Radius.control, style: .continuous)
                .fill(isActive ? Palette.blush : Palette.surface)
        )
        .overlay(
            RoundedRectangle(cornerRadius: Radius.control, style: .continuous)
                .strokeBorder(Palette.border, lineWidth: 1)
        )
        .animation(reduceMotion ? nil : Motion.gentle, value: isActive)
        .accessibilityElement(children: .combine)
    }
}
