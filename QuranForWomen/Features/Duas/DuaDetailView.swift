import SwiftUI
import UIKit

/// One dua, unfolding in reading order: the title comes into focus, the Arabic inks in
/// right to left, then the transliteration and meaning follow. Saving pops the bookmark
/// and lets a few petals fly. Listen and Pause morph into each other.
struct DuaDetailView: View {
    let duaID: String

    @Environment(ContentStore.self) private var content
    @Environment(LibraryStore.self) private var library
    @Environment(AudioPlayer.self) private var player
    @Environment(DhikrStore.self) private var extras
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @AppStorage(PrefKey.showTransliteration) private var showTransliteration = true
    @AppStorage(PrefKey.arabicSize) private var arabicSize = ReaderDefaults.arabicSize
    @AppStorage(PrefKey.haptics) private var hapticsOn = true
    @State private var savePops = 0
    @State private var shareImage: UIImage?
    @State private var copied = false
    @State private var copyToken = 0

    var body: some View {
        if let dua = content.dua(id: duaID) {
            ScrollView {
                VStack(alignment: .leading, spacing: Space.xl) {
                    // The dua unfolds in reading order: title, Arabic, transliteration, meaning, source.
                    VStack(alignment: .leading, spacing: Space.xs) {
                        Eyebrow(text: dua.kindTitle)
                        Text(dua.title)
                            .font(.system(.title, design: .serif, weight: .semibold))
                            .foregroundStyle(BookInk.ink)
                            .accessibilityAddTraits(.isHeader)
                    }
                    .focusIn(delay: 0.05)

                    VStack(alignment: .leading, spacing: Space.s) {
                        ArabicText(text: dua.arabic, size: max(arabicSize, 28))
                            .inkReveal(delay: 0.3, duration: 1.3)
                        if isPlaying(dua) {
                            RecitationLine()
                                .transition(.opacity)
                        }
                    }
                    .padding(.vertical, Space.xs)
                    .animation(.smooth, value: isPlaying(dua))

                    if showTransliteration, let transliteration = dua.transliteration {
                        Text(transliteration)
                            .font(.body.italic())
                            .foregroundStyle(BookInk.caption)
                            .fixedSize(horizontal: false, vertical: true)
                            .focusIn(delay: 1.2)
                    }

                    VStack(alignment: .leading, spacing: Space.xs) {
                        Text("Meaning")
                            .font(.footnote.weight(.semibold))
                            .foregroundStyle(BookInk.caption)
                        TranslationText(text: dua.translation, style: .title3)
                    }
                    .focusIn(delay: 1.4)

                    if let note = dua.note {
                        Text(note)
                            .font(.subheadline)
                            .foregroundStyle(BookInk.caption)
                            .padding(Space.m)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .background(RoundedRectangle(cornerRadius: Radius.control, style: .continuous).fill(BookInk.pageEdge.opacity(0.7)))
                            .dealIn(0, baseDelay: 1.55)
                    }

                    SourceLabel(kind: dua.sourceKind, reference: dua.source)
                        .dealIn(1, baseDelay: 1.55, step: 0.08)

                    HStack(spacing: Space.s) {
                        if let range = dua.playableRange {
                            let first = VerseRef(surah: range.surah, ayah: range.from)
                            let isThis = (range.from...range.to).contains(player.current?.ayah ?? -1)
                                && player.current?.surah == range.surah
                            Button {
                                if isThis { player.togglePlayPause() } else { player.play(range: range) }
                            } label: {
                                Label(isThis && player.isPlaying ? "Pause" : "Listen",
                                      systemImage: isThis && player.isPlaying ? "pause.fill" : "play.fill")
                                    .contentTransition(.symbolEffect(.replace))
                            }
                            .buttonStyle(.tinted)
                            .accessibilityHint("Plays the recitation of \(first.id)")
                        }
                        let saved = library.isSaved(dua)
                        Button {
                            withAnimation(reduceMotion ? nil : Motion.reward) {
                                library.toggleSaved(dua)
                            }
                            // One small pop when she saves. Nothing when she removes it.
                            if !saved && !reduceMotion { savePops += 1 }
                        } label: {
                            Label(saved ? "Saved" : "Save",
                                  systemImage: saved ? "bookmark.fill" : "bookmark")
                                .contentTransition(.symbolEffect(.replace))
                                .symbolEffect(.bounce, value: savePops)
                        }
                        .buttonStyle(.tinted)
                        .overlay { PetalBurst(trigger: saved, active: saved) }
                        .sensoryFeedback(.impact(flexibility: .soft, intensity: 0.8), trigger: saved) { _, new in new && hapticsOn }
                        .accessibilityLabel(saved ? "Saved" : "Save")
                        .accessibilityHint(saved ? "Removes this dua from Saved" : "Keeps this dua in Saved")
                    }
                    .animation(reduceMotion ? nil : Motion.press, value: player.isPlaying)
                    .dealIn(2, baseDelay: 1.55, step: 0.08)
                }
                .padding(.horizontal, Space.page)
                .readableWidth()
                .padding(.vertical, Space.l)
            }
            .bookPage()
            .navigationBarTitleDisplayMode(.inline)
            .minimizesNavigationBarOnScroll()
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    shareMenu(dua)
                }
            }
            .sensoryFeedback(.success, trigger: copyToken) { _, _ in hapticsOn }
            .task(id: dua.id) {
                extras.noteOpened(duaID: dua.id)
                renderShareImage(dua)
            }
        } else {
            ContentUnavailableView("Dua not found", systemImage: "questionmark")
        }
    }

    private func isPlaying(_ dua: Dua) -> Bool {
        guard let range = dua.playableRange, let current = player.current else { return false }
        return current.surah == range.surah && (range.from...range.to).contains(current.ayah)
    }

    private func shareText(_ dua: Dua) -> String {
        [dua.arabic, dua.transliteration, dua.translation, dua.source]
            .compactMap { $0 }
            .joined(separator: "\n\n")
    }

    /// Share as text or as an image card, and copy each part on its own.
    /// The share symbol turns into a check for a moment after a copy.
    private func shareMenu(_ dua: Dua) -> some View {
        Menu {
            ShareLink(item: shareText(dua)) {
                Label("Share as text", systemImage: "text.alignleft")
            }
            if let shareImage {
                ShareLink(
                    item: Image(uiImage: shareImage),
                    preview: SharePreview(dua.title, image: Image(uiImage: shareImage))
                ) {
                    Label("Share as image", systemImage: "photo")
                }
            }
            Section {
                Button {
                    copy(dua.arabic)
                } label: {
                    Label("Copy Arabic", systemImage: "doc.on.doc")
                }
                Button {
                    copy(dua.translation)
                } label: {
                    Label("Copy translation", systemImage: "doc.on.doc")
                }
                if let transliteration = dua.transliteration {
                    Button {
                        copy(transliteration)
                    } label: {
                        Label("Copy transliteration", systemImage: "doc.on.doc")
                    }
                }
            }
        } label: {
            Label(copied ? "Copied" : "Share", systemImage: copied ? "checkmark" : "square.and.arrow.up")
                .contentTransition(.symbolEffect(.replace))
        }
    }

    private func copy(_ text: String) {
        UIPasteboard.general.string = text
        copyToken += 1
        let token = copyToken
        withAnimation(reduceMotion ? nil : Motion.reward) { copied = true }
        Task {
            try? await Task.sleep(for: .seconds(1.6))
            guard token == copyToken else { return }
            withAnimation(reduceMotion ? nil : Motion.gentle) { copied = false }
        }
    }

    /// The same calm card used for verses: the Arabic, the meaning and the source.
    private func renderShareImage(_ dua: Dua) {
        let card = VerseShareCard(arabic: dua.arabic, translation: dua.translation, reference: dua.source)
        let renderer = ImageRenderer(content: card)
        renderer.scale = 3
        shareImage = renderer.uiImage
    }
}

/// Personal prayers in her own words. Clearly marked as personal, never as Quran or Hadith.
/// Her duas are dealt in. A new one slides in at the top with a soft glow, an edited one
/// glows gently, and a deleted one folds away with a short Undo before it is really gone.
struct PersonalDuasView: View {
    @Environment(LibraryStore.self) private var library
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @AppStorage(PrefKey.haptics) private var hapticsOn = true

    @State private var editing: PersonalDua?
    @State private var isAdding = false
    /// The dua that should glow for a moment, after she adds or edits it.
    @State private var glowing: UUID?
    @State private var glowToken = 0
    /// A deleted dua waits here for a few seconds so Undo can bring it back.
    @State private var pendingDelete: PersonalDua?
    @State private var undoToken = 0
    @State private var addedToken = 0

    private var visibleDuas: [PersonalDua] {
        library.personalDuas.filter { $0.id != pendingDelete?.id }
    }

    var body: some View {
        List {
            if visibleDuas.isEmpty {
                SavedEmpty(symbol: "pencil.and.scribble", title: "No personal duas yet",
                           action: "Write a Dua") { isAdding = true }
                    .listRowBackground(Color.clear)
                    .listRowSeparator(.hidden)
            } else {
                Section {
                    ForEach(Array(visibleDuas.enumerated()), id: \.element.id) { index, dua in
                        Button {
                            editing = dua
                        } label: {
                            VStack(alignment: .leading, spacing: Space.xs) {
                                Text(dua.text)
                                    .font(AppFont.serif(.body))
                                    .foregroundStyle(BookInk.ink)
                                    .lineLimit(4)
                                    .contentTransition(.opacity)
                                Text(dua.created.formatted(date: .abbreviated, time: .omitted))
                                    .font(.caption)
                                    .foregroundStyle(BookInk.caption)
                            }
                            .padding(.vertical, 4)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .contentShape(Rectangle())
                            .dealIn(min(index, 8), baseDelay: 0.15, step: 0.05)
                        }
                        .accessibilityHint("Opens this dua to edit")
                        .listRowBackground(rowBackground(for: dua))
                    }
                    .onDelete { offsets in
                        if let index = offsets.first, visibleDuas.indices.contains(index) {
                            delete(visibleDuas[index])
                        }
                    }
                }
            }
        }
        .scrollContentBackground(.hidden)
        .bookPage()
        .animation(reduceMotion ? nil : .spring(response: 0.45, dampingFraction: 0.85), value: visibleDuas)
        .navigationTitle("Your Own Words")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button { isAdding = true } label: {
                    Label("Write a Dua", systemImage: "plus")
                        .symbolEffect(.bounce, value: reduceMotion ? 0 : addedToken)
                }
            }
        }
        .overlay(alignment: .bottom) {
            if pendingDelete != nil {
                UndoBar(text: "Dua deleted") { undoDelete() }
                    .padding(.horizontal, Space.page)
                    .padding(.bottom, Space.m)
                    .transition(.move(edge: .bottom).combined(with: .opacity))
            }
        }
        .animation(reduceMotion ? nil : .spring(response: 0.4, dampingFraction: 0.8), value: pendingDelete?.id)
        .sensoryFeedback(.success, trigger: addedToken) { _, _ in hapticsOn }
        .sensoryFeedback(.impact(flexibility: .soft, intensity: 0.6), trigger: undoToken) { _, _ in hapticsOn }
        .onDisappear { commitDelete() }
        .sheet(isPresented: $isAdding) {
            PersonalDuaEditor(initialText: "") { text in
                let before = library.personalDuas.first?.id
                withAnimation(reduceMotion ? nil : .spring(response: 0.5, dampingFraction: 0.82)) {
                    library.addPersonalDua(text)
                }
                // Only celebrate when a dua was really added (empty text is ignored by the store).
                if let added = library.personalDuas.first?.id, added != before {
                    addedToken += 1
                    glow(added)
                }
            }
        }
        .sheet(item: $editing) { dua in
            PersonalDuaEditor(initialText: dua.text) { text in
                withAnimation(reduceMotion ? nil : Motion.gentle) {
                    library.updatePersonalDua(dua, text: text)
                }
                if library.personalDuas.contains(where: { $0.id == dua.id }) { glow(dua.id) }
            }
        }
    }

    /// The row's card. It warms to a soft blush when the dua is new or just edited, then fades back.
    private func rowBackground(for dua: PersonalDua) -> some View {
        ZStack {
            BookInk.pageLit
            BookInk.gild.opacity(0.28)
                .opacity(glowing == dua.id ? 1 : 0)
        }
    }

    private func glow(_ id: UUID) {
        glowToken += 1
        let token = glowToken
        if reduceMotion {
            glowing = id
        } else {
            withAnimation(.easeOut(duration: 0.35).delay(0.3)) { glowing = id }
        }
        Task {
            try? await Task.sleep(for: .seconds(1.6))
            guard token == glowToken else { return }
            withAnimation(reduceMotion ? nil : .easeInOut(duration: 1.0)) { glowing = nil }
        }
    }

    private func delete(_ dua: PersonalDua) {
        // Only one Undo at a time: an earlier delete is made final now.
        commitDelete()
        withAnimation(reduceMotion ? nil : .spring(response: 0.45, dampingFraction: 0.85)) {
            pendingDelete = dua
        }
        undoToken += 1
        let token = undoToken
        Task {
            try? await Task.sleep(for: .seconds(4))
            if token == undoToken { commitDelete() }
        }
    }

    private func undoDelete() {
        guard let restored = pendingDelete else { return }
        withAnimation(reduceMotion ? nil : Motion.gentle) {
            pendingDelete = nil
        }
        undoToken += 1
        glow(restored.id)
    }

    /// Removes the waiting dua from her list for good.
    private func commitDelete() {
        guard let dua = pendingDelete else { return }
        pendingDelete = nil
        if let index = library.personalDuas.firstIndex(where: { $0.id == dua.id }) {
            library.removePersonalDuas(at: IndexSet(integer: index))
        }
    }
}

/// Writing or editing a personal dua. The words come softly into focus as the sheet opens.
private struct PersonalDuaEditor: View {
    let initialText: String
    let onSave: (String) -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var text = ""
    @FocusState private var focused: Bool

    var body: some View {
        NavigationStack {
            TextEditor(text: $text)
                .font(AppFont.serif(.body))
                .scrollContentBackground(.hidden)
                .padding(Space.m)
                .focused($focused)
                .focusIn(delay: 0.1)
                .bookPage()
                .navigationTitle(initialText.isEmpty ? "New Dua" : "Edit Dua")
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) {
                        Button("Cancel") { dismiss() }
                    }
                    ToolbarItem(placement: .confirmationAction) {
                        Button("Save") {
                            onSave(text)
                            dismiss()
                        }
                        .disabled(text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && initialText.isEmpty)
                    }
                }
        }
        .onAppear {
            text = initialText
            focused = true
        }
    }
}

/// A small burst of petals from the Save button, only when she saves.
struct PetalBurst: View {
    let trigger: Bool
    /// Only burst when saving, not when removing.
    let active: Bool

    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Environment(\.inBook) private var inBook
    @State private var fire = 0

    var body: some View {
        ZStack {
            ForEach(0..<8, id: \.self) { i in
                let angle = Double(i) / 8 * 2 * .pi
                Capsule()
                    // In a book the petals are gilt and red silk.
                    .fill(i.isMultiple(of: 2) ? (inBook ? BookInk.gild : Palette.accentFill) : (inBook ? BookInk.ribbon : Palette.blushStrong))
                    .frame(width: 4, height: 9)
                    .rotationEffect(.radians(angle + .pi / 2))
                    .keyframeAnimator(initialValue: PetalState(), trigger: fire) { petal, value in
                        petal
                            .offset(x: cos(angle) * value.distance, y: sin(angle) * value.distance)
                            .opacity(value.opacity)
                            .scaleEffect(value.scale)
                    } keyframes: { _ in
                        KeyframeTrack(\.distance) {
                            CubicKeyframe(0, duration: 0.01)
                            SpringKeyframe(42, duration: 0.45, spring: .snappy)
                        }
                        KeyframeTrack(\.opacity) {
                            LinearKeyframe(1, duration: 0.05)
                            LinearKeyframe(1, duration: 0.25)
                            LinearKeyframe(0, duration: 0.3)
                        }
                        KeyframeTrack(\.scale) {
                            CubicKeyframe(1.1, duration: 0.2)
                            CubicKeyframe(0.6, duration: 0.4)
                        }
                    }
            }
        }
        .allowsHitTesting(false)
        .accessibilityHidden(true)
        .onChange(of: trigger) { _, _ in
            if active && !reduceMotion { fire += 1 }
        }
    }
}

struct PetalState {
    var distance: CGFloat = 0
    var opacity: Double = 0
    var scale: CGFloat = 1
}
