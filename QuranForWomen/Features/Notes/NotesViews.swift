import SwiftUI

/// Identifies which note sheet is open.
private enum NoteSheet: Identifiable {
    case record
    case edit(LessonNote, isNew: Bool)

    var id: String {
        switch self {
        case .record: "record"
        case .edit(let note, _): note.id.uuidString
        }
    }
}

// MARK: - Notes home

/// Lesson notes. The empty state comes into focus with a small bounce of the waveform; saved
/// notes are dealt in one after another, settle as they scroll, and reflow softly while searching.
struct NotesHome: View {
    @Environment(JournalStore.self) private var journal
    @Environment(AppRouter.self) private var router
    @Environment(QuranStore.self) private var quran
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var sheet: NoteSheet?
    @State private var query = ""

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: Space.xl) {
                let notes = filtered
                if journal.notes.isEmpty {
                    VStack(spacing: Space.m) {
                        Image(systemName: "waveform")
                            .font(.system(size: 36, weight: .light))
                            .foregroundStyle(BookInk.rule)
                            .modifier(NotesBounceOnAppear(delay: 0.4))
                        Text("No notes yet")
                            .font(.system(.title3, design: .serif, weight: .semibold))
                        Button {
                            sheet = .record
                        } label: {
                            Label("Record a Lesson", systemImage: "mic.fill")
                        }
                        .buttonStyle(.primary)
                        .padding(.top, Space.xs)
                        Button("Write Notes Instead") { sheet = .edit(LessonNote(), isNew: true) }
                            .font(.subheadline.weight(.medium))
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.top, Space.xl)
                    .focusIn(delay: 0.05)
                } else if notes.isEmpty {
                    ContentUnavailableView.search(text: query)
                } else {
                    LazyVStack(spacing: Space.s) {
                        ForEach(Array(notes.enumerated()), id: \.element.id) { index, note in
                            NavigationLink(value: Route.lessonNote(note.id)) {
                                NoteCard(note: note)
                                    .zoomSource(ZoomID.note(note.id))
                            }
                            .buttonStyle(.pressable)
                            .dealIn(min(index, 8), baseDelay: 0.12, step: 0.05)
                            .scrollSettle()
                        }
                    }
                    .animation(reduceMotion ? nil : Motion.gentle, value: notes.map(\.id))
                }
            }
            .padding(.horizontal, Space.page)
            .padding(.bottom, Space.xxl)
        }
        .searchable(text: $query, prompt: "Search notes")
        .toolbar(journal.isAccessible ? .automatic : .hidden, for: .navigationBar)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Menu {
                    Button("Record a Lesson", systemImage: "mic") { sheet = .record }
                    Button("Write Notes", systemImage: "square.and.pencil") { sheet = .edit(LessonNote(), isNew: true) }
                    Divider()
                    Button("Journal Settings", systemImage: "gearshape") { router.open(.journalSettings, in: .journal) }
                } label: {
                    Label("New note", systemImage: "plus")
                } primaryAction: {
                    sheet = .record
                }
            }
        }
        .fullScreenCover(item: $sheet) { sheet in
            switch sheet {
            case .record:
                LessonRecordView()
            case .edit(let note, let isNew):
                LessonNoteEditor(note: note, isNew: isNew)
            }
        }
    }

    private var filtered: [LessonNote] {
        let q = query.trimmingCharacters(in: .whitespaces)
        guard !q.isEmpty else { return journal.sortedNotes }
        return journal.sortedNotes.filter { n in
            ([n.title, n.speaker, n.takeaway, n.notes, n.transcript, n.kind.title] + n.points + n.quotes)
                .contains { $0.localizedCaseInsensitiveContains(q) }
        }
    }

}

private struct NoteCard: View {
    let note: LessonNote

    var body: some View {
        VStack(alignment: .leading, spacing: Space.xs) {
            HStack(spacing: 6) {
                Image(systemName: note.kind.symbol)
                Text(note.kind.title)
                if note.pinned { Image(systemName: "pin.fill") }
                Spacer()
                Text(note.date.formatted(date: .abbreviated, time: .omitted))
            }
            .font(.caption)
            .foregroundStyle(BookInk.rule)

            Text(note.displayTitle)
                .font(.headline)
                .foregroundStyle(BookInk.ink)
                .multilineTextAlignment(.leading)

            let meta = metaLine
            if !meta.isEmpty {
                Text(meta)
                    .font(.footnote)
                    .foregroundStyle(BookInk.caption)
            }
            if !note.takeaway.isEmpty {
                Text(note.takeaway)
                    .font(AppFont.serif(.subheadline))
                    .foregroundStyle(BookInk.caption)
                    .lineLimit(2)
                    .multilineTextAlignment(.leading)
            }
        }
        .cardSurface(padding: Space.m)
    }

    private var metaLine: String {
        var parts: [String] = []
        if !note.speaker.isEmpty { parts.append(note.speaker) }
        if note.minutes > 0 { parts.append("\(note.minutes) min") }
        if !note.verses.isEmpty { parts.append(note.verses.count == 1 ? "1 verse" : "\(note.verses.count) verses") }
        return parts.joined(separator: " · ")
    }
}

// MARK: - Recording

/// Recording a lesson. Each stage crossfades through a soft blur; while listening a ring pulses
/// out from the mic, the timer rolls its digits and the verse count ticks up as verses are heard.
struct LessonRecordView: View {
    private enum Stage { case ready, listening, arranging }

    @Environment(QuranStore.self) private var quran
    @Environment(AudioPlayer.self) private var player
    @Environment(\.dismiss) private var dismiss
    @Environment(\.openURL) private var openURL
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @AppStorage(PrefKey.haptics) private var hapticsOn = true
    @State private var recorder = LessonRecorder()
    @State private var stage: Stage = .ready
    @State private var kind: LessonNote.Kind = .khutbah
    @State private var speaker = ""
    @State private var result: LessonNote?

    var body: some View {
        NavigationStack {
            ZStack {
                Group {
                    switch stage {
                    case .ready: ready
                    case .listening: listening
                    case .arranging: arranging
                    }
                }
                .id(stage)
                .transition(reduceMotion ? AnyTransition.opacity : .opacity.combined(with: .blurred))
            }
            .animation(reduceMotion ? nil : Motion.smooth, value: stage)
            .sensoryFeedback(.impact(flexibility: .soft, intensity: 0.7), trigger: stage) { _, _ in hapticsOn }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .bookPage()
            .journalPrivacyCover()
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(stage == .listening ? "Discard" : "Cancel") {
                        recorder.stop()
                        dismiss()
                    }
                }
            }
        }
        .onDisappear { if recorder.state == .recording { recorder.stop() } }
        .fullScreenCover(item: $result, onDismiss: { dismiss() }) { note in
            LessonNoteEditor(note: note, isNew: true)
        }
    }

    private var ready: some View {
        ScrollView {
            VStack(spacing: Space.xl) {
                Image(systemName: "waveform")
                    .font(.system(size: 44, weight: .light))
                    .foregroundStyle(BookInk.rule)
                    .modifier(NotesBounceOnAppear(delay: 0.35))
                    .padding(.top, Space.xl)
                    .focusIn(delay: 0.05)
                Text("Record a lesson")
                    .font(.system(.title, design: .serif, weight: .semibold))
                    .focusIn(delay: 0.1)

                VStack(alignment: .leading, spacing: Space.m) {
                    Picker("Type", selection: $kind) {
                        ForEach(LessonNote.Kind.allCases) { Label($0.title, systemImage: $0.symbol).tag($0) }
                    }
                    .pickerStyle(.menu)
                    TextField("Speaker (optional)", text: $speaker)
                        .textInputAutocapitalization(.words)
                }
                .cardSurface(padding: Space.m)
                .dealIn(0, baseDelay: 0.2)
                .scrollSettle()

                if recorder.state == .denied {
                    VStack(spacing: Space.s) {
                        Text("Microphone or speech access is off.")
                            .font(.footnote)
                            .foregroundStyle(BookInk.ribbon)
                            .multilineTextAlignment(.center)
                        Button("Open Settings") {
                            if let url = URL(string: UIApplication.openSettingsURLString) { openURL(url) }
                        }
                        .buttonStyle(.tintedCompact)
                    }
                } else if recorder.state == .unavailable {
                    Text("Speech recognition is unavailable.")
                        .font(.footnote)
                        .foregroundStyle(BookInk.ribbon)
                        .multilineTextAlignment(.center)
                }

                Button {
                    // Recording takes over the audio session, so stop any recitation first.
                    player.stop()
                    Task {
                        await recorder.start()
                        if recorder.state == .recording { stage = .listening }
                    }
                } label: {
                    Label("Start Recording", systemImage: "mic.fill")
                }
                .buttonStyle(.primary)
                .dealIn(1, baseDelay: 0.2)
            }
            .padding(.horizontal, Space.xl)
        }
    }

    private var listening: some View {
        VStack(spacing: 0) {
            ScrollViewReader { proxy in
                ScrollView {
                    Text(recorder.transcript.isEmpty ? "Listening…" : recorder.transcript)
                        .font(AppFont.serif(.title3))
                        .foregroundStyle(recorder.transcript.isEmpty ? BookInk.caption : BookInk.ink)
                        .lineSpacing(6)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(Space.xl)
                    Color.clear.frame(height: 1).id("end")
                }
                .onChange(of: recorder.transcript) { _, _ in
                    withAnimation(.easeOut(duration: 0.2)) { proxy.scrollTo("end", anchor: .bottom) }
                }
            }

            TimelineView(.periodic(from: .now, by: 1)) { context in
                let verses = VerseDetector.find(in: recorder.transcript, quran: quran).count
                HStack(spacing: Space.m) {
                    RecordingPulse()
                    Text(elapsed(at: context.date))
                        .font(.headline.monospacedDigit())
                        .contentTransition(.numericText(value: Double(elapsedSeconds(at: context.date))))
                        .animation(reduceMotion ? nil : Animation.snappy, value: elapsed(at: context.date))
                    LiveBars(time: context.date.timeIntervalSinceReferenceDate)
                    if verses > 0 {
                        Text(verses == 1 ? "· 1 verse" : "· \(verses) verses")
                            .font(.subheadline)
                            .foregroundStyle(BookInk.rule)
                            .contentTransition(.numericText(value: Double(verses)))
                            .transition(.scale(scale: 0.8).combined(with: .opacity))
                    }
                    Spacer()
                    Button("Stop") { finish() }
                        .buttonStyle(.tintedCompact)
                }
                .animation(reduceMotion ? nil : Motion.reward, value: verses)
                .padding(.horizontal, Space.xl)
                .padding(.vertical, Space.m)
                .background(.bar)
            }
        }
    }

    private var arranging: some View {
        VStack(spacing: Space.m) {
            ProgressView()
            Text("Arranging your notes")
                .foregroundStyle(BookInk.caption)
        }
    }

    private func elapsed(at date: Date) -> String {
        guard let start = recorder.startedAt else { return "0:00" }
        let seconds = Int(date.timeIntervalSince(start))
        return String(format: "%d:%02d", seconds / 60, seconds % 60)
    }

    /// Whole seconds since recording began, so the timer digits roll upward.
    private func elapsedSeconds(at date: Date) -> Int {
        guard let start = recorder.startedAt else { return 0 }
        return max(0, Int(date.timeIntervalSince(start)))
    }

    private func finish() {
        let minutes = recorder.elapsedMinutes
        let transcript = recorder.stop()
        stage = .arranging
        Task {
            try? await Task.sleep(for: .milliseconds(900))
            let verses = VerseDetector.find(in: transcript, quran: quran)
            let arranged = LessonArranger.arrange(transcript, verses: verses) { quran.surah($0).name }
            result = LessonNote(
                kind: kind,
                title: arranged.title,
                speaker: speaker.trimmingCharacters(in: .whitespaces),
                date: .now,
                verses: verses,
                points: arranged.points,
                quotes: arranged.quotes,
                takeaway: arranged.takeaway,
                transcript: transcript,
                minutes: minutes
            )
        }
    }
}

// MARK: - Note view

/// One lesson note. The heading comes into focus, verses, points and lines are dealt in, the
/// takeaway card rises last, and Save to Journal turns into a checkmark with a bounce.
struct LessonNoteView: View {
    let noteID: UUID

    @Environment(JournalStore.self) private var journal
    @Environment(QuranStore.self) private var quran
    @Environment(\.dismiss) private var dismiss
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @AppStorage(PrefKey.haptics) private var hapticsOn = true
    @State private var editing: LessonNote?
    @State private var showTranscript = false
    @State private var confirmDelete = false
    @State private var savedToJournal = false

    var body: some View {
        if let note = journal.note(id: noteID) {
            ScrollView {
                VStack(alignment: .leading, spacing: Space.xl) {
                    VStack(alignment: .leading, spacing: Space.xs) {
                        Label(note.kind.title, systemImage: note.kind.symbol)
                            .font(.footnote.weight(.semibold))
                            .foregroundStyle(BookInk.rule)
                        Text(note.displayTitle)
                            .font(.system(.title, design: .serif, weight: .semibold))
                        Text(Self.meta(note))
                            .font(.subheadline)
                            .foregroundStyle(BookInk.caption)
                    }
                    .focusIn(delay: 0.05)

                    if !note.verses.isEmpty {
                        section("Verses mentioned") {
                            VStack(spacing: Space.s) {
                                ForEach(Array(note.verses.enumerated()), id: \.element.id) { index, ref in
                                    VerseCard(ref: ref, arabicSize: 22)
                                        .dealIn(min(index, 8), baseDelay: 0.15, step: 0.07)
                                        .scrollSettle()
                                }
                            }
                        }
                    }

                    if !note.points.isEmpty {
                        section("The main points") {
                            VStack(alignment: .leading, spacing: Space.m) {
                                ForEach(Array(note.points.enumerated()), id: \.offset) { index, point in
                                    HStack(alignment: .firstTextBaseline, spacing: Space.s) {
                                        Text("\(index + 1)")
                                            .font(.headline.monospacedDigit())
                                            .foregroundStyle(Color(hex: 0xB8902F))
                                        Text(point)
                                            .font(AppFont.serif(.body))
                                    }
                                    .dealIn(min(index, 8), baseDelay: 0.2, step: 0.05)
                                }
                            }
                        }
                        .scrollSettle()
                    }

                    if !note.quotes.isEmpty {
                        section("Lines worth keeping") {
                            VStack(alignment: .leading, spacing: Space.m) {
                                ForEach(Array(note.quotes.enumerated()), id: \.offset) { index, quote in
                                    HStack(spacing: Space.s) {
                                        Rectangle()
                                            .fill(Color(hex: 0xD3A94A))
                                            .frame(width: 3)
                                        Text(quote)
                                            .font(AppFont.serif(.body).italic())
                                    }
                                    .fixedSize(horizontal: false, vertical: true)
                                    .dealIn(min(index, 8), baseDelay: 0.25, step: 0.05)
                                }
                            }
                        }
                        .scrollSettle()
                    }

                    if !note.takeaway.isEmpty {
                        VStack(alignment: .leading, spacing: Space.xs) {
                            Text("Carry this into my week")
                                .font(.subheadline.weight(.semibold))
                                .foregroundStyle(BookInk.pageLit.opacity(0.85))
                            Text(note.takeaway)
                                .font(AppFont.serif(.title3))
                                .foregroundStyle(BookInk.pageLit)
                        }
                        .padding(Space.l)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(RoundedRectangle(cornerRadius: Radius.card, style: .continuous).fill(BookInk.rule))
                        .dealIn(0, baseDelay: 0.3)
                        .scrollSettle()
                    }

                    if !note.notes.isEmpty {
                        section("My notes") {
                            MarkdownBody(text: note.notes)
                        }
                        .focusIn(delay: 0.35)
                    }

                    if !note.transcript.isEmpty {
                        DisclosureGroup("Transcript", isExpanded: $showTranscript) {
                            Text(note.transcript)
                                .font(.callout)
                                .foregroundStyle(BookInk.caption)
                                .textSelection(.enabled)
                                .padding(.top, Space.s)
                        }
                        .tint(BookInk.rule)
                    }

                    VStack(spacing: Space.s) {
                        Button {
                            saveToJournal(note)
                        } label: {
                            Label(savedToJournal ? "Saved to Journal" : "Save to Journal",
                                  systemImage: savedToJournal ? "checkmark" : "book.closed")
                                .contentTransition(.symbolEffect(.replace))
                                .symbolEffect(.bounce, value: reduceMotion ? false : savedToJournal)
                        }
                        .buttonStyle(.tinted)
                        .disabled(savedToJournal)
                        .sensoryFeedback(.success, trigger: savedToJournal) { _, saved in saved && hapticsOn }
                    }
                }
                .padding(.horizontal, Space.page)
                .readableWidth()
                .padding(.vertical, Space.l)
            }
            .bookPage()
            .journalPrivacyCover()
            .navigationBarTitleDisplayMode(.inline)
            .minimizesNavigationBarOnScroll()
            .toolbar {
                ToolbarItemGroup(placement: .topBarTrailing) {
                    // Nothing in the note can be shared or changed while the journal is locked.
                    if journal.isAccessible {
                    Button {
                        var updated = note
                        updated.pinned.toggle()
                        journal.save(updated)
                    } label: {
                        Label(note.pinned ? "Unpin" : "Pin", systemImage: note.pinned ? "pin.fill" : "pin")
                            .contentTransition(.symbolEffect(.replace))
                            .symbolEffect(.bounce, value: reduceMotion ? false : note.pinned)
                    }
                    ShareLink(item: note.plainText) {
                        Label("Share", systemImage: "square.and.arrow.up")
                    }
                    Menu {
                        Button("Edit", systemImage: "pencil") { editing = note }
                        Button("Delete", systemImage: "trash", role: .destructive) { confirmDelete = true }
                    } label: {
                        Label("More", systemImage: "ellipsis.circle")
                    }
                    }
                }
            }
            .fullScreenCover(item: $editing) { LessonNoteEditor(note: $0, isNew: false) }
            .confirmationDialog("Delete this note?", isPresented: $confirmDelete, titleVisibility: .visible) {
                Button("Delete Note", role: .destructive) {
                    journal.delete(note)
                    dismiss()
                }
            }
        } else {
            ContentUnavailableView("Note not found", systemImage: "doc")
        }
    }

    private static func meta(_ note: LessonNote) -> String {
        var parts: [String] = []
        if !note.speaker.isEmpty { parts.append(note.speaker) }
        parts.append(note.date.formatted(date: .long, time: .omitted))
        if note.minutes > 0 { parts.append("\(note.minutes) min") }
        return parts.joined(separator: " · ")
    }

    private func section<Content: View>(_ title: String, @ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: Space.s) {
            Text(title)
                .font(.headline)
                .foregroundStyle(BookInk.ink)
                .accessibilityAddTraits(.isHeader)
            content()
        }
    }

    private func saveToJournal(_ note: LessonNote) {
        var body = ""
        if !note.points.isEmpty {
            body += "## Main points\n" + note.points.map { "- \($0)" }.joined(separator: "\n")
        }
        if !note.takeaway.isEmpty {
            body += (body.isEmpty ? "" : "\n\n") + "## Carry this into my week\n\(note.takeaway)"
        }
        journal.save(JournalEntry(
            date: note.date,
            title: note.displayTitle,
            body: body,
            verse: note.verses.first,
            origin: "From a \(note.kind.title.lowercased())"
        ))
        withAnimation(reduceMotion ? nil : Motion.reward) { savedToJournal = true }
    }
}

// MARK: - Note editor

/// Editing a lesson note. The form comes into focus, and each verse, whether found in the talk
/// or added by hand, pops in as a small chip with a book that bounces once.
struct LessonNoteEditor: View {
    @State var note: LessonNote
    let isNew: Bool

    @Environment(JournalStore.self) private var journal
    @Environment(QuranStore.self) private var quran
    @Environment(\.dismiss) private var dismiss
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @AppStorage(PrefKey.haptics) private var hapticsOn = true
    @State private var verseInput = ""

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    Picker("Type", selection: $note.kind) {
                        ForEach(LessonNote.Kind.allCases) { Text($0.title).tag($0) }
                    }
                    TextField("Title", text: $note.title)
                    TextField("Speaker", text: $note.speaker)
                        .textInputAutocapitalization(.words)
                    DatePicker("Date", selection: $note.date, displayedComponents: .date)
                }
                .listRowBackground(BookInk.pageLit)

                Section {
                    ForEach(Array(note.verses.enumerated()), id: \.element.id) { index, ref in
                        HStack(spacing: Space.xs) {
                            Image(systemName: "book.closed")
                                .font(.footnote.weight(.semibold))
                                .foregroundStyle(BookInk.rule)
                                .modifier(NotesBounceOnAppear(delay: 0.3 + Double(min(index, 8)) * 0.06))
                                .accessibilityHidden(true)
                            Text("\(quran.surah(ref.surah).name) · \(ref.id)")
                        }
                        .padding(.horizontal, Space.s)
                        .padding(.vertical, 6)
                        .background(Capsule().fill(BookInk.gild.opacity(0.2)))
                        .modifier(PopIn(delay: 0.15 + Double(min(index, 8)) * 0.06))
                    }
                    .onDelete { note.verses.remove(atOffsets: $0) }
                    HStack {
                        TextField("Add a verse (2:255)", text: $verseInput)
                            .keyboardType(.numbersAndPunctuation)
                            .onSubmit(addVerse)
                        Button("Add", action: addVerse)
                            .disabled(!isValidVerse)
                    }
                } header: {
                    Text("Verses mentioned")
                }
                .listRowBackground(BookInk.pageLit)

                EditableList(title: "The main points", items: $note.points, placeholder: "A point")
                EditableList(title: "Lines worth keeping", items: $note.quotes, placeholder: "A line")

                Section("Carry this into my week") {
                    TextField("One thing to act on", text: $note.takeaway, axis: .vertical)
                }
                .listRowBackground(BookInk.pageLit)

                Section("My notes") {
                    TextField("Anything else", text: $note.notes, axis: .vertical)
                        .lineLimit(4...)
                }
                .listRowBackground(BookInk.pageLit)
            }
            .focusIn(delay: 0.05)
            .scrollContentBackground(.hidden)
            .bookPage()
            .journalPrivacyCover()
            .sensoryFeedback(.selection, trigger: note.verses.count) { old, new in new > old && hapticsOn }
            .navigationTitle(isNew ? "New Note" : "Edit Note")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancel") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        note.points = note.points.filter { !$0.trimmingCharacters(in: .whitespaces).isEmpty }
                        note.quotes = note.quotes.filter { !$0.trimmingCharacters(in: .whitespaces).isEmpty }
                        journal.save(note)
                        dismiss()
                    }
                    .fontWeight(.semibold)
                    .disabled(!journal.isAccessible)
                }
            }
        }
    }

    private var isValidVerse: Bool {
        guard let ref = VerseRef(verseInput.trimmingCharacters(in: .whitespaces)) else { return false }
        return quran.contains(ref)
    }

    private func addVerse() {
        guard let ref = VerseRef(verseInput.trimmingCharacters(in: .whitespaces)), quran.contains(ref) else { return }
        if !note.verses.contains(ref) {
            withAnimation(reduceMotion ? nil : Motion.gentle) { note.verses.append(ref) }
        }
        verseInput = ""
    }
}

private struct EditableList: View {
    let title: String
    @Binding var items: [String]
    let placeholder: String

    var body: some View {
        Section {
            ForEach(items.indices, id: \.self) { index in
                TextField(placeholder, text: Binding(
                    get: { items.indices.contains(index) ? items[index] : "" },
                    set: { if items.indices.contains(index) { items[index] = $0 } }
                ), axis: .vertical)
            }
            .onDelete { items.remove(atOffsets: $0) }
            Button {
                items.append("")
            } label: {
                Label("Add", systemImage: "plus")
            }
        } header: {
            Text(title)
        }
        .listRowBackground(BookInk.pageLit)
    }
}

/// Gently moving bars while a lesson is being written out.
private struct LiveBars: View {
    let time: Double
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        TimelineView(.animation(minimumInterval: 1.0 / 20, paused: reduceMotion)) { context in
            let t = reduceMotion ? 0 : context.date.timeIntervalSinceReferenceDate
            HStack(spacing: 3) {
                ForEach(0..<5, id: \.self) { i in
                    Capsule()
                        .fill(BookInk.rule)
                        .frame(width: 3, height: 6 + 12 * abs(sin(t * 3.1 + Double(i) * 0.9)))
                }
            }
            .frame(height: 20)
        }
        .accessibilityHidden(true)
    }
}

// MARK: - Notes motion

/// A red recording dot with a soft ring that keeps pulsing outward. Still under Reduce Motion.
private struct RecordingPulse: View {
    @State private var pulsing = false
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        ZStack {
            Circle()
                .stroke(Color.red.opacity(0.6), lineWidth: 2)
                .frame(width: 12, height: 12)
                .scaleEffect(pulsing ? 2.2 : 1)
                .opacity(reduceMotion ? 0.35 : (pulsing ? 0 : 0.8))
            Image(systemName: "mic.fill")
                .font(.system(size: 9, weight: .bold))
                .foregroundStyle(.white)
                .frame(width: 18, height: 18)
                .background(Circle().fill(Color.red))
        }
        .frame(width: 30, height: 30)
        .accessibilityHidden(true)
        .onAppear {
            guard !reduceMotion, !pulsing else { return }
            withAnimation(.easeOut(duration: 1.4).repeatForever(autoreverses: false)) {
                pulsing = true
            }
        }
    }
}

/// Plays one SF Symbol bounce shortly after the view appears. Still under Reduce Motion.
private struct NotesBounceOnAppear: ViewModifier {
    var delay: Double = 0.3
    @State private var fired = false
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    func body(content: Content) -> some View {
        content
            .symbolEffect(.bounce, value: fired)
            .task {
                guard !reduceMotion, !fired else { return }
                try? await Task.sleep(for: .seconds(delay))
                fired = true
            }
    }
}

/// A small chip pops in from a little smaller with a light overshoot. Only fades under Reduce Motion.
private struct PopIn: ViewModifier {
    var delay: Double = 0
    @State private var shown = false
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    func body(content: Content) -> some View {
        content
            .opacity(shown ? 1 : 0)
            .scaleEffect(shown || reduceMotion ? 1 : 0.6, anchor: .leading)
            .onAppear {
                guard !shown else { return }
                withAnimation(reduceMotion ? .easeOut(duration: 0.2) : Motion.reward.delay(delay)) {
                    shown = true
                }
            }
    }
}
