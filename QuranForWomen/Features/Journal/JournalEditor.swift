import LocalAuthentication
import SwiftUI

/// Opens the right editor for how the page was started.
struct JournalStartView: View {
    let start: JournalStart

    @Environment(JournalStore.self) private var journal

    var body: some View {
        switch start {
        case .template(let template):
            TemplateFillView(template: template)
        case .edit(let entry):
            JournalEditorView(entry: entry, prompt: nil, isNew: false)
        case .prompt(let text, let isDua):
            JournalEditorView(entry: JournalEntry(origin: "Prompt", isDua: isDua), prompt: text, isNew: true)
        case .verse(let ref):
            JournalEditorView(entry: JournalEntry(verse: ref, origin: "From a verse"), prompt: "What does this verse say to you today?", isNew: true)
        case .dua:
            JournalEditorView(entry: JournalEntry(mood: .makingDua, isDua: true), prompt: "What are you asking Allah for?", isNew: true)
        case .blank:
            JournalEditorView(entry: JournalEntry(), prompt: nil, isNew: true)
        }
    }
}

// MARK: - Free writing

struct JournalEditorView: View {
    @State var entry: JournalEntry
    let prompt: String?
    let isNew: Bool

    @Environment(JournalStore.self) private var journal
    @Environment(QuranStore.self) private var quran
    @Environment(ContentStore.self) private var content
    @Environment(\.dismiss) private var dismiss
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    @State private var showDetails = false
    @State private var pickingDate = false
    @State private var tagsText = ""
    @FocusState private var bodyFocused: Bool
    @Namespace private var moodGlide

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: Space.l) {
                    masthead
                        .focusIn(delay: 0.05)
                    moodRow

                    if let prompt {
                        Text(prompt)
                            .font(AppFont.serif(.headline))
                            .foregroundStyle(BookInk.rule)
                            .padding(.horizontal, Space.s)
                            .padding(.vertical, Space.xs)
                            .background(RoundedRectangle(cornerRadius: 10, style: .continuous).fill(BookInk.gild.opacity(0.2)))
                            .focusIn(delay: 0.2)
                    }

                    TextField("Title", text: $entry.title, axis: .vertical)
                        .font(.system(.title2, design: .serif, weight: .semibold))
                        .submitLabel(.next)
                        .onSubmit { bodyFocused = true }

                    TextField("Write", text: $entry.body, axis: .vertical)
                        .font(AppFont.serif(.body))
                        .lineSpacing(5)
                        .lineLimit(10...)
                        .focused($bodyFocused)

                    if let verse = entry.verse {
                        attachedVerse(verse)
                            .scrollSettle()
                    }

                    let mentioned = VerseDetector.find(in: entry.body, quran: quran, limit: 3).filter { $0 != entry.verse }
                    if !mentioned.isEmpty {
                        VStack(alignment: .leading, spacing: Space.xs) {
                            Text("Verses you mentioned")
                                .font(.footnote.weight(.semibold))
                                .foregroundStyle(BookInk.caption)
                            HStack {
                                ForEach(mentioned) { ref in
                                    Button("+ \(quran.surah(ref.surah).name) \(ref.id)") {
                                        withAnimation(Motion.gentle) { entry.verse = ref }
                                    }
                                    .buttonStyle(.tintedCompact)
                                }
                            }
                        }
                    }

                    if showDetails { details }
                }
                .padding(.horizontal, Space.page)
                .padding(.vertical, Space.l)
                .animation(reduceMotion ? nil : Motion.gentle, value: showDetails)
            }
            .scrollDismissesKeyboard(.interactively)
            .background(background.ignoresSafeArea())
            .environment(\.inBook, true)
            .tint(BookInk.rule)
            .journalPrivacyCover()
            .animation(.easeInOut(duration: 0.6), value: entry.mood)
            .navigationBarTitleDisplayMode(.inline)
            .navigationTitle(isNew ? "New Page" : "Edit Page")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") { save() }
                        .fontWeight(.semibold)
                        // Nothing can be saved while the journal is locked.
                        .disabled(entry.isEmpty || !journal.isAccessible)
                }
            }
            .safeAreaInset(edge: .bottom) {
                if journal.isAccessible {
                    HStack { toolbarButtons }
                        .padding(.horizontal, Space.page)
                        .padding(.vertical, Space.s)
                        .background(.bar)
                }
            }
            .sheet(isPresented: $pickingDate) {
                DatePicker("Date", selection: $entry.date, in: ...Date.now)
                    .datePickerStyle(.graphical)
                    .padding()
                    .presentationDetents([.medium])
            }
        }
        .onAppear {
            tagsText = entry.tags.joined(separator: ", ")
            if isNew && prompt != nil { bodyFocused = true }
        }
    }

    // MARK: Parts

    private var masthead: some View {
        Button { pickingDate = true } label: {
            HStack(alignment: .lastTextBaseline, spacing: Space.s) {
                Text(entry.date.formatted(.dateTime.day(.twoDigits)))
                    .font(.system(size: 44, weight: .light, design: .serif))
                    .contentTransition(.numericText(value: Double(Calendar.current.component(.day, from: entry.date))))
                    .animation(.snappy, value: entry.date)
                VStack(alignment: .leading, spacing: 0) {
                    Text(entry.date.formatted(.dateTime.weekday(.wide)))
                    Text(entry.date.formatted(.dateTime.month(.wide).year()))
                }
                .font(.subheadline)
                .foregroundStyle(BookInk.caption)
                Image(systemName: "calendar")
                    .font(.footnote)
                    .foregroundStyle(BookInk.rule)
            }
            .foregroundStyle(BookInk.ink)
        }
        .buttonStyle(PressableStyle(scale: 0.97))
        .accessibilityLabel("Date:\(entry.date.formatted(date: .complete, time: .omitted)). Change date")
    }

    private var moodRow: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: Space.xs) {
                ForEach(Array(JournalMood.allCases.enumerated()), id: \.element) { index, mood in
                    Button {
                        withAnimation(.spring(response: 0.4, dampingFraction: 0.75)) {
                            entry.mood = entry.mood == mood ? nil : mood
                        }
                        if mood == .makingDua { entry.isDua = true }
                    } label: {
                        Label(mood.title, systemImage: mood.symbol)
                            .font(.footnote.weight(.medium))
                            .padding(.horizontal, Space.s)
                            .frame(minHeight: 36)
                            .foregroundStyle(entry.mood == mood ? BookInk.pageLit : BookInk.rule)
                            .symbolEffect(.bounce, value: entry.mood == mood)
                            .background {
                                ZStack {
                                    Capsule().fill(BookInk.gild.opacity(0.2))
                                    if entry.mood == mood {
                                        Capsule()
                                            .fill(BookInk.rule)
                                            .matchedGeometryEffect(id: "mood", in: moodGlide)
                                    }
                                }
                            }
                    }
                    .buttonStyle(.pressable)
                    .accessibilityAddTraits(entry.mood == mood ? .isSelected : [])
                    .dealIn(index, baseDelay: 0.12, step: 0.04)
                }
            }
        }
        .sensoryFeedback(.selection, trigger: entry.mood)
    }

    private func attachedVerse(_ verse: VerseRef) -> some View {
        VStack(alignment: .leading, spacing: Space.xs) {
            HStack {
                Text(quran.displayName(verse))
                    .font(.footnote.weight(.semibold))
                    .foregroundStyle(BookInk.rule)
                Spacer()
                Button {
                    withAnimation(Motion.gentle) { entry.verse = nil }
                } label: {
                    Image(systemName: "xmark.circle.fill").foregroundStyle(BookInk.caption)
                }
                .buttonStyle(PressableStyle(scale: 0.9))
                .accessibilityLabel("Remove verse")
            }
            ArabicText(text: quran.text(verse), size: 22, lineLimit: 4)
            TranslationBody(ref: verse)
        }
        .cardSurface(padding: Space.m, fill: BookInk.gild.opacity(0.2).opacity(0.5))
    }

    private var details: some View {
        VStack(alignment: .leading, spacing: Space.m) {
            field("Grateful for", text: $entry.gratitude, prompt: "One thing, big or small")
            field("Tags", text: $tagsText, prompt: "family, sabr, work")
            Toggle("Add to my duas", isOn: $entry.isDua)
            Toggle("Favorite", isOn: $entry.favorite)
        }
        .tint(BookInk.rule)
        .cardSurface(padding: Space.m)
        .scrollSettle()
        .transition(.opacity)
    }

    private func field(_ title: String, text: Binding<String>, prompt: String) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(title)
                .font(.footnote.weight(.semibold))
                .foregroundStyle(BookInk.caption)
            TextField(prompt, text: text, axis: .vertical)
        }
    }

    @ViewBuilder
    private var toolbarButtons: some View {
        Button {
            entry.verse = content.verseOfTheDay()
        } label: {
            Label("Attach today's verse", systemImage: "book")
        }
        Spacer()
        Button {
            withAnimation(reduceMotion ? nil : Motion.gentle) { entry.isDua.toggle() }
        } label: {
            Label(entry.isDua ? "In my duas" : "Add to my duas", systemImage: entry.isDua ? "hands.and.sparkles.fill" : "hands.and.sparkles")
                .contentTransition(.symbolEffect(.replace))
                .symbolEffect(.bounce, value: reduceMotion ? false : entry.isDua)
        }
        Spacer()
        Button {
            withAnimation(reduceMotion ? nil : Motion.gentle) { showDetails.toggle() }
        } label: {
            Label("More details", systemImage: showDetails ? "ellipsis.circle.fill" : "ellipsis.circle")
                .contentTransition(.symbolEffect(.replace))
        }
    }

    private var background: Color {
        entry.mood?.isHeavy == true ? BookInk.pageEdge : BookInk.page
    }

    private func save() {
        entry.tags = tagsText.split(separator: ",").map { $0.trimmingCharacters(in: .whitespaces) }.filter { !$0.isEmpty }
        journal.save(entry)
        dismiss()
    }
}

// MARK: - Template page

/// A printed-journal page: one section per question. Empty sections are not saved.
struct TemplateFillView: View {
    let template: JournalTemplate

    @Environment(JournalStore.self) private var journal
    @Environment(QuranStore.self) private var quran
    @Environment(\.dismiss) private var dismiss
    @State private var answers: [String] = []

    private let paper = BookInk.pageLit
    private let rule = BookInk.rule.opacity(0.35)

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: Space.xl) {
                    VStack(spacing: Space.xs) {
                        Text("✦ \(template.title) ✦")
                            .font(.system(.title2, design: .serif, weight: .semibold).italic())
                            .multilineTextAlignment(.center)
                        Text(Date.now.formatted(date: .long, time: .omitted))
                            .font(.subheadline)
                            .foregroundStyle(BookInk.caption)
                    }
                    .frame(maxWidth: .infinity)

                    if let verse = template.verse {
                        VStack(spacing: Space.xs) {
                            ArabicText(text: quran.text(verse), size: 21, placement: .center)
                            TranslationBody(ref: verse)
                            Text(quran.displayName(verse))
                                .font(.caption.weight(.semibold))
                                .foregroundStyle(BookInk.rule)
                        }
                    }

                    ForEach(template.prompts.indices, id: \.self) { i in
                        section(template.prompts[i], index: i)
                            .dealIn(i, baseDelay: 0.35, step: 0.08)
                    }
                }
                .padding(Space.xl)
                .background(
                    RoundedRectangle(cornerRadius: 8, style: .continuous)
                        .fill(paper)
                        .shadow(color: .black.opacity(0.06), radius: 10, y: 4)
                )
                .padding(Space.m)
                .dealIn(0, baseDelay: 0.05)
            }
            .scrollDismissesKeyboard(.interactively)
            .bookPage()
            .journalPrivacyCover()
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancel") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") { save() }
                        .fontWeight(.semibold)
                        .disabled(!journal.isAccessible)
                }
            }
        }
        .onAppear {
            if answers.count != template.prompts.count {
                answers = Array(repeating: "", count: template.prompts.count)
            }
        }
    }

    private func section(_ prompt: TemplatePrompt, index: Int) -> some View {
        VStack(alignment: .leading, spacing: Space.xs) {
            Text(prompt.heading.uppercased())
                .font(.caption.weight(.semibold))
                .tracking(0.8)
                .foregroundStyle(BookInk.rule)
            Text(prompt.question)
                .font(AppFont.serif(.subheadline))
                .foregroundStyle(BookInk.caption)
            TextField(prompt.style == .list ? "One per line" : "", text: binding(index), axis: .vertical)
                .font(AppFont.serif(.body))
                .lineLimit(lineRange(prompt.style))
                .padding(.vertical, Space.xs)
                .overlay(alignment: .bottom) {
                    Rectangle().fill(rule).frame(height: 1)
                }
        }
    }

    private func lineRange(_ style: TemplatePrompt.Style) -> ClosedRange<Int> {
        switch style {
        case .field: 1...3
        case .list: 3...12
        case .lines: 3...20
        }
    }

    private func binding(_ index: Int) -> Binding<String> {
        Binding(
            get: { answers.indices.contains(index) ? answers[index] : "" },
            set: { if answers.indices.contains(index) { answers[index] = $0 } }
        )
    }

    private func save() {
        let body = JournalContent.compose(template, answers: answers)
        guard !body.isEmpty else {
            dismiss()
            return
        }
        journal.save(JournalEntry(
            title: template.title,
            body: body,
            mood: template.isDua ? .makingDua : nil,
            verse: template.verse,
            origin: template.title,
            isDua: template.isDua
        ))
        journal.markTemplateUsed(template.id)
        dismiss()
    }
}

// MARK: - Template gallery

/// Template gallery. The featured card comes into focus, the collection tiles are dealt in
/// with a small bounce of each icon, and search results crossfade in and out as she types.
struct JournalTemplatesView: View {
    @Environment(JournalStore.self) private var journal
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var query = ""
    @State private var opening: JournalStart?

    private let columns = [GridItem(.flexible(), spacing: Space.s), GridItem(.flexible(), spacing: Space.s)]

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: Space.xl) {
                if query.isEmpty {
                    VStack(alignment: .leading, spacing: Space.xl) {
                        gallery
                    }
                    .transition(reduceMotion ? AnyTransition.opacity : .opacity.combined(with: .blurred))
                } else {
                    VStack(alignment: .leading, spacing: Space.xl) {
                        results
                    }
                    .transition(reduceMotion ? AnyTransition.opacity : .opacity.combined(with: .blurred))
                }
            }
            .padding(.horizontal, Space.page)
            .padding(.vertical, Space.s)
            .animation(reduceMotion ? nil : Motion.gentle, value: query)
        }
        .bookPage()
        .navigationTitle("Templates")
        .searchable(text: $query, prompt: "Dua, gratitude, Ramadan")
        .fullScreenCover(item: $opening) { JournalStartView(start: $0) }
    }

    @ViewBuilder
    private var gallery: some View {
        let recent = journal.recentTemplates.compactMap(JournalContent.template(id:))
        let featured = recent.first ?? JournalContent.template(id: "muhasabah")
        if let featured {
            Button { opening = .template(featured) } label: {
                VStack(alignment: .leading, spacing: Space.xs) {
                    Eyebrow(text: recent.isEmpty ? "Try tonight" : "Recently used", color: BookInk.pageLit.opacity(0.85))
                    Text(featured.title)
                        .font(.system(.title2, design: .serif, weight: .semibold))
                    Text(featured.summary)
                        .font(.subheadline)
                        .opacity(0.85)
                    Text("Begin")
                        .font(.subheadline.weight(.semibold))
                        .padding(.horizontal, Space.m)
                        .padding(.vertical, Space.xs)
                        .background(Capsule().fill(BookInk.pageLit.opacity(0.2)))
                        .padding(.top, Space.xs)
                }
                .foregroundStyle(BookInk.pageLit)
                .padding(Space.xl)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(RoundedRectangle(cornerRadius: Radius.card, style: .continuous).fill(BookInk.rule))
            }
            .buttonStyle(.pressable)
            .focusIn(delay: 0.05)
            .scrollSettle()
        }

        LazyVGrid(columns: columns, spacing: Space.s) {
            ForEach(Array(JournalContent.collections.enumerated()), id: \.element.id) { index, collection in
                NavigationLink(value: Route.templateCollection(collection.id)) {
                    VStack(alignment: .leading, spacing: Space.s) {
                        Image(systemName: collection.symbol)
                            .font(.system(size: 20, weight: .medium))
                            .foregroundStyle(BookInk.rule)
                            .modifier(BounceOnAppear(delay: 0.45 + Double(min(index, 8)) * 0.06))
                        Spacer(minLength: 0)
                        Text(collection.title)
                            .font(.headline)
                            .foregroundStyle(BookInk.ink)
                            .multilineTextAlignment(.leading)
                        Text("\(JournalContent.templates(in: collection).count) templates")
                            .font(.caption)
                            .foregroundStyle(BookInk.caption)
                    }
                    .frame(maxWidth: .infinity, minHeight: 120, alignment: .leading)
                    .cardSurface(padding: Space.m)
                    .zoomSource(ZoomID.templateCollection(collection.id))
                }
                .buttonStyle(.pressable)
                .dealIn(min(index, 8), baseDelay: 0.15, step: 0.06)
                .scrollSettle()
            }
        }
    }

    @ViewBuilder
    private var results: some View {
        let found = JournalContent.templates.filter { t in
            ([t.title, t.summary] + t.prompts.map(\.question)).contains { $0.localizedCaseInsensitiveContains(query) }
        }
        if found.isEmpty {
            ContentUnavailableView.search(text: query)
                .transition(.opacity)
        }
        ForEach(found) { template in
            TemplateRow(template: template) { opening = .template(template) }
                .transition(.opacity)
                .scrollSettle()
        }
    }
}

/// Plays one SF Symbol bounce shortly after the view appears. Still under Reduce Motion.
private struct BounceOnAppear: ViewModifier {
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

/// One collection of templates. The rows are dealt in and settle softly as they scroll.
struct TemplateCollectionView: View {
    let collectionID: String
    @State private var opening: JournalStart?

    var body: some View {
        let collection = JournalContent.collections.first { $0.id == collectionID }
        ScrollView {
            VStack(alignment: .leading, spacing: Space.s) {
                if let collection {
                    ForEach(Array(JournalContent.templates(in: collection).enumerated()), id: \.element.id) { index, template in
                        TemplateRow(template: template) { opening = .template(template) }
                            .dealIn(min(index, 8), baseDelay: 0.12, step: 0.05)
                            .scrollSettle()
                    }
                }
            }
            .padding(.horizontal, Space.page)
            .padding(.vertical, Space.s)
        }
        .bookPage()
        .navigationTitle(collection?.title ?? "Templates")
        .fullScreenCover(item: $opening) { JournalStartView(start: $0) }
    }
}

/// A template card. It presses in softly, and the dua mark gives one small bounce
/// when the row arrives. The list around it handles the deal in.
private struct TemplateRow: View {
    let template: JournalTemplate
    let open: () -> Void

    var body: some View {
        Button(action: open) {
            VStack(alignment: .leading, spacing: Space.xs) {
                HStack {
                    Text(template.title)
                        .font(.headline)
                        .foregroundStyle(BookInk.ink)
                    Spacer()
                    if template.isDua {
                        Image(systemName: "hands.and.sparkles")
                            .foregroundStyle(BookInk.ribbon)
                            .modifier(BounceOnAppear(delay: 0.5))
                            .accessibilityLabel("Adds to your duas")
                    }
                }
                Text(template.summary)
                    .font(.subheadline)
                    .foregroundStyle(BookInk.caption)
                Text(template.prompts.map(\.heading).joined(separator: " · "))
                    .font(.caption)
                    .foregroundStyle(BookInk.rule)
                    .lineLimit(2)
            }
            .multilineTextAlignment(.leading)
            .cardSurface(padding: Space.m)
        }
        .buttonStyle(.pressable)
    }
}

// MARK: - Prompts

/// Prompts. The prompt card comes into focus, "Another" crossfades the prompt through a soft
/// blur while its arrows bounce, and the prompt packs are dealt in below.
struct JournalPromptsView: View {
    @State private var opening: JournalStart?
    @State private var shuffle = 0
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @AppStorage(PrefKey.haptics) private var hapticsOn = true

    /// Morning prompts before noon, evening prompts after six, shukr in between.
    private var recommended: PromptPack {
        let hour = Calendar.current.component(.hour, from: .now)
        let id = (4..<12).contains(hour) ? "morning" : (hour >= 18 || hour < 4 ? "evening" : "shukr")
        return JournalContent.packs.first { $0.id == id } ?? JournalContent.packs[0]
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: Space.xl) {
                let pack = recommended
                let day = Calendar.current.ordinality(of: .day, in: .era, for: .now) ?? 0
                let prompt = pack.prompts[(day + shuffle) % pack.prompts.count]

                VStack(alignment: .leading, spacing: Space.m) {
                    Eyebrow(text: "For now · \(pack.title)")
                    // A ZStack keeps the height steady while the old and new prompt cross.
                    ZStack(alignment: .topLeading) {
                        Text(prompt)
                            .font(.system(.title2, design: .serif, weight: .semibold))
                            .foregroundStyle(BookInk.ink)
                            .fixedSize(horizontal: false, vertical: true)
                            .id(prompt)
                            .transition(reduceMotion ? AnyTransition.opacity : .opacity.combined(with: .blurred))
                    }
                    HStack {
                        Button("Write") { opening = .prompt(prompt, isDua: pack.isDua) }
                            .buttonStyle(.tintedCompact)
                        Button {
                            withAnimation(Motion.gentle) { shuffle += 1 }
                        } label: {
                            Label("Another", systemImage: "arrow.triangle.2.circlepath")
                                .symbolEffect(.bounce, value: reduceMotion ? 0 : shuffle)
                        }
                        .font(.subheadline.weight(.medium))
                        .accessibilityLabel("Another prompt")
                        .sensoryFeedback(.selection, trigger: shuffle) { _, _ in hapticsOn }
                    }
                }
                .cardSurface(padding: Space.xl, fill: BookInk.ribbon.opacity(0.12).opacity(0.5))
                .focusIn(delay: 0.05)
                .scrollSettle()

                VStack(alignment: .leading, spacing: Space.s) {
                    SectionHeader(title: "Prompt packs")
                        .focusIn(delay: 0.15)
                    ForEach(Array(JournalContent.packs.enumerated()), id: \.element.id) { index, pack in
                        NavigationLink(value: Route.promptPack(pack.id)) {
                            HStack(spacing: Space.m) {
                                RowIcon(systemImage: pack.symbol)
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(pack.title).font(.headline).foregroundStyle(BookInk.ink)
                                    Text("\(pack.prompts.count) prompts").font(.caption).foregroundStyle(BookInk.caption)
                                }
                                Spacer()
                                Image(systemName: "chevron.right")
                                    .font(.footnote.weight(.semibold))
                                    .foregroundStyle(BookInk.caption)
                                    .accessibilityHidden(true)
                            }
                            .cardSurface(padding: Space.m)
                            .zoomSource(ZoomID.promptPack(pack.id))
                        }
                        .buttonStyle(.pressable)
                        .dealIn(min(index, 8), baseDelay: 0.2, step: 0.05)
                        .scrollSettle()
                    }
                }
            }
            .padding(.horizontal, Space.page)
            .padding(.vertical, Space.s)
        }
        .bookPage()
        .navigationTitle("Prompts")
        .fullScreenCover(item: $opening) { JournalStartView(start: $0) }
    }
}

/// The prompts in one pack, dealt in one after another and settling as they scroll.
struct PromptPackView: View {
    let packID: String
    @State private var opening: JournalStart?

    var body: some View {
        let pack = JournalContent.packs.first { $0.id == packID }
        ScrollView {
            VStack(spacing: Space.s) {
                ForEach(Array((pack?.prompts ?? []).enumerated()), id: \.element) { index, prompt in
                    Button {
                        opening = .prompt(prompt, isDua: pack?.isDua ?? false)
                    } label: {
                        Text(prompt)
                            .font(AppFont.serif(.body))
                            .foregroundStyle(BookInk.ink)
                            .multilineTextAlignment(.leading)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .cardSurface(padding: Space.m)
                    }
                    .buttonStyle(.pressable)
                    .dealIn(min(index, 8), baseDelay: 0.1, step: 0.05)
                    .scrollSettle()
                }
            }
            .padding(.horizontal, Space.page)
            .padding(.vertical, Space.s)
        }
        .bookPage()
        .navigationTitle(pack?.title ?? "Prompts")
        .fullScreenCover(item: $opening) { JournalStartView(start: $0) }
    }
}

// MARK: - Settings

enum JournalLock {
    /// Face ID, Touch ID or passcode. Calls back on the main actor.
    static func authenticate(reason: String, completion: @escaping @MainActor (Bool) -> Void) {
        let context = LAContext()
        var error: NSError?
        guard context.canEvaluatePolicy(.deviceOwnerAuthentication, error: &error) else {
            Task { @MainActor in completion(true) }
            return
        }
        context.evaluatePolicy(.deviceOwnerAuthentication, localizedReason: reason) { success, _ in
            Task { @MainActor in completion(success) }
        }
    }
}

/// Journal settings. Turning the lock on or off swaps an open and closed lock with one bounce,
/// and Export morphs into Share with a small checkmark. The unlock cover appears at once and
/// only fades on the way out, so nothing shows while it locks.
struct JournalSettingsView: View {
    @Environment(JournalStore.self) private var journal
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @AppStorage(JournalPrefKey.hidePreviews) private var hidePreviews = false
    @AppStorage(PrefKey.haptics) private var hapticsOn = true
    @State private var exportURL: URL?

    var body: some View {
        Form {
            Section {
                Toggle(isOn: Binding(
                    get: { journal.lockEnabled },
                    set: { on in
                        if on {
                            // She is here already, so the journal stays open after locking it.
                            journal.isUnlocked = true
                            journal.lockEnabled = true
                        } else {
                            JournalLock.authenticate(reason: "Turn off the journal lock") { ok in
                                if ok { journal.lockEnabled = false }
                            }
                        }
                    }
                )) {
                    Label {
                        Text("Lock with Face ID or passcode")
                    } icon: {
                        Image(systemName: journal.lockEnabled ? "lock" : "lock.open")
                            .foregroundStyle(BookInk.rule)
                            .contentTransition(.symbolEffect(.replace))
                            .symbolEffect(.bounce, value: reduceMotion ? false : journal.lockEnabled)
                            .accessibilityHidden(true)
                    }
                    .animation(reduceMotion ? nil : Motion.gentle, value: journal.lockEnabled)
                }
                .sensoryFeedback(.impact(flexibility: .soft, intensity: 0.7), trigger: journal.lockEnabled) { _, _ in hapticsOn }
                Toggle("Hide page previews", isOn: $hidePreviews)
            }
            .listRowBackground(BookInk.pageLit)

            GentleReminderFormSection(.journal, title: "Reminder to write")

            Section {
                if let exportURL {
                    ShareLink(item: exportURL) {
                        HStack {
                            Label("Share Export File", systemImage: "square.and.arrow.up")
                            Spacer()
                            Image(systemName: "checkmark.circle.fill")
                                .foregroundStyle(BookInk.rule)
                                .modifier(BounceOnAppear(delay: 0.15))
                                .accessibilityHidden(true)
                        }
                    }
                    .accessibilityLabel("Share Export File. Export is ready")
                    .transition(reduceMotion ? AnyTransition.opacity : .opacity.combined(with: .blurred))
                } else {
                    Button {
                        withAnimation(reduceMotion ? nil : Motion.reward) {
                            exportURL = journal.exportFile()
                        }
                    } label: {
                        Label("Export Journal and Notes", systemImage: "doc.text")
                    }
                    .transition(reduceMotion ? AnyTransition.opacity : .opacity.combined(with: .blurred))
                }
                LabeledContent("Pages") {
                    Text("\(journal.entries.count)")
                        .monospacedDigit()
                        .contentTransition(.numericText(value: Double(journal.entries.count)))
                        .animation(reduceMotion ? nil : Motion.gentle, value: journal.entries.count)
                }
                LabeledContent("Notes") {
                    Text("\(journal.notes.count)")
                        .monospacedDigit()
                        .contentTransition(.numericText(value: Double(journal.notes.count)))
                        .animation(reduceMotion ? nil : Motion.gentle, value: journal.notes.count)
                }
            }
            .listRowBackground(BookInk.pageLit)
        }
        .tint(BookInk.rule)
        .scrollContentBackground(.hidden)
        .bookPage()
        .navigationTitle("Journal Settings")
        .navigationBarTitleDisplayMode(.inline)
        .sensoryFeedback(.success, trigger: exportURL != nil) { _, ready in ready && hapticsOn }
        .overlay {
            ZStack {
                if !journal.isAccessible {
                    VStack(spacing: Space.m) {
                        Image(systemName: "lock").font(.largeTitle).foregroundStyle(BookInk.rule)
                        Button("Unlock to Change") {
                            JournalLock.authenticate(reason: "Open journal settings") { ok in
                                if ok { journal.isUnlocked = true }
                            }
                        }
                        .buttonStyle(.tinted)
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .background(BookInk.page)
                    // Appears at once when locking; only fades away once she has unlocked.
                    .transition(.asymmetric(insertion: .identity, removal: .opacity))
                }
            }
            .animation(reduceMotion ? nil : Motion.smooth, value: journal.isAccessible)
        }
        .onDisappear {
            // Remove the plain text copy once she leaves this screen.
            if let exportURL { try? FileManager.default.removeItem(at: exportURL) }
            exportURL = nil
        }
    }
}

// MARK: - Privacy cover

/// Covers an open journal page or note while the journal is locked, without closing it,
/// so nothing she was writing is lost and nothing private shows in the app switcher.
struct JournalPrivacyCover: ViewModifier {
    @Environment(JournalStore.self) private var journal

    func body(content: Content) -> some View {
        content.overlay {
            if !journal.isAccessible {
                VStack(spacing: Space.m) {
                    Image(systemName: "lock")
                        .font(.system(size: 34, weight: .light))
                        .foregroundStyle(BookInk.rule)
                    Text("Your journal is private")
                        .font(.system(.title3, design: .serif, weight: .semibold))
                    Button("Unlock") {
                        JournalLock.authenticate(reason: "Unlock your journal") { ok in
                            if ok { withAnimation(.smooth(duration: 0.5)) { journal.isUnlocked = true } }
                        }
                    }
                    .buttonStyle(.tintedCompact)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .background(BookInk.page.ignoresSafeArea())
                .transition(.opacity)
            }
        }
    }
}

extension View {
    func journalPrivacyCover() -> some View { modifier(JournalPrivacyCover()) }
}
