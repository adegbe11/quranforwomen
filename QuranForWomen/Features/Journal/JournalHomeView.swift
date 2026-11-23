import LocalAuthentication
import SwiftUI

enum JournalPrefKey {
    static let section = "journal.section"
    static let lock = "journal.lock"
    static let hidePreviews = "journal.hidePreviews"
}

/// What opens the editor: a blank page, a prompt, a template, or a verse.
enum JournalStart: Identifiable {
    case blank
    case prompt(String, isDua: Bool)
    case template(JournalTemplate)
    case verse(VerseRef)
    case edit(JournalEntry)
    case dua

    var id: String {
        switch self {
        case .blank: "blank"
        case .prompt(let text, _): "prompt-\(text)"
        case .template(let t): "template-\(t.id)"
        case .verse(let ref): "verse-\(ref.id)"
        case .edit(let entry): "edit-\(entry.id)"
        case .dua: "dua"
        }
    }
}

struct JournalTab: View {
    var body: some View {
        TabStack(tab: .journal) {
            JournalRoot()
        }
    }
}

private struct JournalRoot: View {
    enum JournalSection: String, CaseIterable { case journal = "Journal", notes = "Notes" }

    @AppStorage(JournalPrefKey.section) private var section: JournalSection = .journal
    private var lockEnabled: Bool { journal.lockEnabled }
    @Environment(\.scenePhase) private var scenePhase
    @Environment(JournalStore.self) private var journal
    @Environment(AppRouter.self) private var router
    private var unlocked: Bool { journal.isUnlocked }

    var body: some View {
        // The content stays mounted under the lock, so an open page or a recording
        // in progress is never thrown away when the app goes to the background.
        ZStack {
            VStack(spacing: 0) {
                Picker("Section", selection: $section) {
                    ForEach(JournalSection.allCases, id: \.self) { Text($0.rawValue).tag($0) }
                }
                .pickerStyle(.segmented)
                .padding(.horizontal, Space.page)
                .padding(.bottom, Space.xs)

                ZStack {
                    Group {
                        switch section {
                        case .journal: JournalHome()
                        case .notes: NotesHome()
                        }
                    }
                    .id(section)
                    .transition(.opacity.combined(with: .blurred))
                }
                .frame(maxHeight: .infinity)
            }
            .animation(.smooth(duration: 0.4), value: section)
            .accessibilityHidden(lockEnabled && !unlocked)

            if lockEnabled && !unlocked {
                LockedJournal {
                    // The lock dissolves away to reveal her pages.
                    withAnimation(.smooth(duration: 0.55)) { journal.isUnlocked = true }
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .background(BookInk.page)
                .transition(.opacity.combined(with: .scale(scale: 1.04)).combined(with: .blurred))
            }
        }
        .bookPage()
        .navigationTitle(section == .journal ? "Journal" : "Notes")
        .onChange(of: scenePhase) { _, phase in
            // Lock again whenever the app leaves the screen.
            // Relocking happens app-wide in RootView.
            _ = phase
        }
    }
}

// MARK: - Lock

private struct LockedJournal: View {
    let onUnlock: () -> Void
    @State private var failed = false
    @Environment(\.scenePhase) private var scenePhase

    var body: some View {
        VStack(spacing: Space.l) {
            Spacer()
            Image(systemName: "lock")
                .font(.system(size: 40, weight: .light))
                .foregroundStyle(BookInk.rule)
                .symbolEffect(.bounce, value: failed)
            Text("Your journal is private")
                .font(.system(.title2, design: .serif, weight: .semibold))
            Button("Unlock Journal") { authenticate() }
                .buttonStyle(.primary)
                .padding(.horizontal, Space.xxl)
            if failed {
                Text("Could not unlock. Try again.")
                    .font(.footnote)
                    .foregroundStyle(BookInk.ribbon)
            }
            Spacer()
        }
        .frame(maxWidth: .infinity)
        .onAppear { if scenePhase == .active { authenticate() } }
        .onChange(of: scenePhase) { _, phase in
            if phase == .active { authenticate() }
        }
    }

    private func authenticate() {
        let context = LAContext()
        var error: NSError?
        guard context.canEvaluatePolicy(.deviceOwnerAuthentication, error: &error) else {
            onUnlock()
            return
        }
        context.evaluatePolicy(.deviceOwnerAuthentication, localizedReason: "Unlock your journal") { success, _ in
            Task { @MainActor in
                if success { onUnlock() } else { failed = true }
            }
        }
    }
}

// MARK: - Home

/// A calm list of pages, newest first. One + menu, like Apple's Journal.
private struct JournalHome: View {
    enum Filter: String, CaseIterable, Identifiable {
        case all, duas, answered, gratitude, quran, favorites

        var id: String { rawValue }
        var title: String {
            switch self {
            case .all: "All pages"
            case .duas: "My duas"
            case .answered: "Answered duas"
            case .gratitude: "Gratitude"
            case .quran: "Quran reflections"
            case .favorites: "Favorites"
            }
        }
        var symbol: String {
            switch self {
            case .all: "book.pages"
            case .duas: "hands.and.sparkles"
            case .answered: "sparkles"
            case .gratitude: "heart"
            case .quran: "book"
            case .favorites: "star"
            }
        }

        func matches(_ e: JournalEntry) -> Bool {
            switch self {
            case .all: true
            case .duas: e.isDua
            case .answered: e.isDua && e.duaStage.isAnswered
            case .gratitude: !e.gratitude.isEmpty || e.mood == .grateful || (e.origin ?? "").localizedCaseInsensitiveContains("grat") || (e.origin ?? "").localizedCaseInsensitiveContains("bless")
            case .quran: e.verse != nil
            case .favorites: e.favorite
            }
        }
    }

    @Environment(JournalStore.self) private var journal
    @Environment(AppRouter.self) private var router
    @State private var start: JournalStart?
    @State private var filter: Filter = .all
    @State private var showCalendar = false
    @State private var query = ""

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: Space.xl) {
                if query.isEmpty && filter == .all {
                    suggestion
                        .focusIn(delay: 0.05)
                        .scrollSettle()
                }

                if filter != .all {
                    Button {
                        withAnimation(Motion.gentle) { filter = .all }
                    } label: {
                        Label(filter.title, systemImage: "xmark.circle.fill")
                            .font(.subheadline.weight(.medium))
                    }
                    .buttonStyle(.tintedCompact)
                }

                if journal.entries.isEmpty {
                    emptyState
                } else if showCalendar {
                    JournalCalendar(entries: filtered) { router.open(.journalEntry($0.id), in: .journal) }
                        .scrollSettle()
                } else {
                    entryList
                }
            }
            .padding(.horizontal, Space.page)
            .padding(.top, Space.s)
            .padding(.bottom, Space.xxl)
        }
        .searchable(text: $query, prompt: "Search your journal")
        .toolbar(journal.isAccessible ? .automatic : .hidden, for: .navigationBar)
        .toolbar {
            ToolbarItemGroup(placement: .topBarTrailing) {
                Menu {
                    Picker("Show", selection: $filter) {
                        ForEach(Filter.allCases) { Label($0.title, systemImage: $0.symbol).tag($0) }
                    }
                    Toggle(isOn: $showCalendar) {
                        Label("Calendar view", systemImage: "calendar")
                    }
                    Divider()
                    Button("Journal Settings", systemImage: "gearshape") {
                        router.open(.journalSettings, in: .journal)
                    }
                } label: {
                    Label("View options", systemImage: "line.3.horizontal.decrease.circle")
                }

                Menu {
                    Button("New Page", systemImage: "square.and.pencil") { start = .blank }
                    Button("New Dua", systemImage: "hands.and.sparkles") { start = .dua }
                    Divider()
                    Button("From a Template", systemImage: "doc.text") {
                        router.open(.journalTemplates, in: .journal)
                    }
                    Button("From a Prompt", systemImage: "text.bubble") {
                        router.open(.journalPrompts, in: .journal)
                    }
                } label: {
                    Label("New page", systemImage: "plus")
                } primaryAction: {
                    start = .blank
                }
            }
        }
        .fullScreenCover(item: $start) { start in
            JournalStartView(start: start)
        }
    }

    // MARK: Parts

    /// One quiet suggestion, not a banner.
    private var suggestion: some View {
        Button {
            start = .prompt(JournalContent.question(), isDua: false)
        } label: {
            VStack(alignment: .leading, spacing: Space.xs) {
                Text("Today's question")
                    .font(.subheadline)
                    .foregroundStyle(BookInk.caption)
                Text(JournalContent.question())
                    .font(.system(.title3, design: .serif, weight: .semibold))
                    .foregroundStyle(BookInk.ink)
                    .multilineTextAlignment(.leading)
                Text("Write")
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(BookInk.rule)
                    .padding(.top, 2)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .cardSurface()
        }
        .buttonStyle(.pressable)
    }

    private var filtered: [JournalEntry] {
        let base = journal.sortedEntries.filter(filter.matches)
        let q = query.trimmingCharacters(in: .whitespaces)
        guard !q.isEmpty else { return base }
        return base.filter { e in
            [e.title, e.body, e.gratitude, e.tags.joined(separator: " "), e.origin ?? "", e.verse?.id ?? "", e.answeredNote]
                .contains { $0.localizedCaseInsensitiveContains(q) }
        }
    }

    private var entryList: some View {
        let groups = Dictionary(grouping: filtered) { Calendar.current.startOfDay(for: $0.date) }
            .sorted { $0.key > $1.key }
        // Position of each page in the list, so they are dealt in top to bottom.
        let order = Dictionary(filtered.enumerated().map { ($0.element.id, $0.offset) }, uniquingKeysWith: { first, _ in first })
        return LazyVStack(alignment: .leading, spacing: Space.s) {
            if groups.isEmpty {
                ContentUnavailableView.search(text: query)
            }
            ForEach(groups, id: \.key) { day, items in
                Text(dayLabel(day))
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(BookInk.caption)
                    .padding(.top, Space.s)
                    .focusIn(delay: 0.12 + Double(min(order[items.first?.id ?? UUID()] ?? 0, 8)) * 0.05)
                ForEach(items) { entry in
                    NavigationLink(value: Route.journalEntry(entry.id)) {
                        EntryCard(entry: entry)
                            .zoomSource(ZoomID.entry(entry.id))
                            .overlay {
                                if journal.justSaved == entry.id { ArrivalGlow() }
                            }
                    }
                    .buttonStyle(.pressable)
                    .dealIn(min(order[entry.id] ?? 0, 8), baseDelay: 0.15, step: 0.05)
                    .scrollSettle()
                }
            }
        }
    }

    private var emptyState: some View {
        VStack(spacing: Space.s) {
            Text("No pages yet")
                .font(.system(.title3, design: .serif, weight: .semibold))
            Button("New Page") { start = .blank }
                .buttonStyle(.tintedCompact)
                .padding(.top, Space.xs)
        }
        .frame(maxWidth: .infinity)
        .padding(.top, Space.l)
    }

    private func dayLabel(_ day: Date) -> String {
        if Calendar.current.isDateInToday(day) { return "Today" }
        if Calendar.current.isDateInYesterday(day) { return "Yesterday" }
        return day.formatted(.dateTime.weekday(.wide).day().month(.wide))
    }
}

// MARK: - Entry card

struct EntryCard: View {
    /// Without a title, the first line already shows as the title, so the preview starts after it.
    static func preview(for entry: JournalEntry) -> String {
        let body = entry.body.replacingOccurrences(of: "## ", with: "")
        guard entry.title.isEmpty else { return body }
        var lines = body.components(separatedBy: "
")
        while let first = lines.first, first.trimmingCharacters(in: .whitespaces).isEmpty { lines.removeFirst() }
        if !lines.isEmpty { lines.removeFirst() }
        return lines.joined(separator: "
").trimmingCharacters(in: .whitespacesAndNewlines)
    }

    let entry: JournalEntry

    @AppStorage(JournalPrefKey.hidePreviews) private var hidePreviews = false

    var body: some View {
        VStack(alignment: .leading, spacing: Space.xs) {
            HStack(spacing: 6) {
                Circle()
                    .fill(entry.isDua && entry.duaStage.isAnswered ? Color(hex: 0xD3A94A) : BookInk.rule)
                    .frame(width: 7, height: 7)
                if let mood = entry.mood {
                    Text(mood.title)
                }
                if let origin = entry.origin {
                    Text("· \(origin)").lineLimit(1)
                }
                Spacer()
                if entry.favorite {
                    Image(systemName: "star.fill").foregroundStyle(Color(hex: 0xD3A94A))
                }
                Text(entry.date.formatted(date: .omitted, time: .shortened))
            }
            .font(.caption)
            .foregroundStyle(BookInk.caption)

            if hidePreviews {
                Text("Private page")
                    .font(.subheadline)
                    .foregroundStyle(BookInk.caption)
            } else {
                Text(entry.displayTitle)
                    .font(.headline)
                    .foregroundStyle(BookInk.ink)
                    .multilineTextAlignment(.leading)
                let preview = Self.preview(for: entry)
                if !preview.isEmpty {
                    Text(preview)
                        .font(AppFont.serif(.subheadline))
                        .foregroundStyle(BookInk.caption)
                        .lineLimit(4)
                        .multilineTextAlignment(.leading)
                }
                HStack(spacing: Space.xs) {
                    if let verse = entry.verse {
                        SourceLabel(kind: .quran, reference: "Quran \(verse.id)")
                    }
                    if entry.isDua {
                        Text(entry.duaStage.title)
                            .font(.caption.weight(.semibold))
                            .foregroundStyle(entry.duaStage.isAnswered ? Color(hex: 0x9A7420) : BookInk.ribbon)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 4)
                            .background(Capsule().fill(entry.duaStage.isAnswered ? Color(hex: 0xF6ECCF) : BookInk.ribbon.opacity(0.12)))
                    }
                }
            }
        }
        .cardSurface(padding: Space.m)
    }
}

// MARK: - Calendar

/// A month of her journal. Days with pages carry a soft dot; tapping one lists that day's pages below.
private struct JournalCalendar: View {
    let entries: [JournalEntry]
    let open: (JournalEntry) -> Void

    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @AppStorage(PrefKey.haptics) private var hapticsOn = true
    @State private var month = Calendar.current.date(from: Calendar.current.dateComponents([.year, .month], from: .now)) ?? .now
    @State private var selected = Calendar.current.startOfDay(for: .now)

    var body: some View {
        let calendar = Calendar.current
        let days = daysInGrid()
        let byDay = Dictionary(grouping: entries) { calendar.startOfDay(for: $0.date) }
        let count = entries.filter { calendar.isDate($0.date, equalTo: month, toGranularity: .month) }.count
        let chosen = (byDay[selected] ?? []).sorted { $0.date > $1.date }

        VStack(alignment: .leading, spacing: Space.l) {
            VStack(spacing: Space.m) {
                HStack {
                    Button { shift(-1) } label: {
                        Image(systemName: "chevron.left").frame(width: 44, height: 44)
                    }
                    .buttonStyle(PressableStyle(scale: 0.9))
                    .accessibilityLabel("Previous month")
                    Spacer()
                    Text(month.formatted(.dateTime.month(.wide).year()))
                        .font(.headline)
                        .contentTransition(.numericText())
                        .animation(reduceMotion ? nil : Motion.gentle, value: month)
                    Spacer()
                    Button { shift(1) } label: {
                        Image(systemName: "chevron.right").frame(width: 44, height: 44)
                    }
                    .buttonStyle(PressableStyle(scale: 0.9))
                    .accessibilityLabel("Next month")
                }
                .foregroundStyle(BookInk.rule)

                LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 4), count: 7), spacing: 6) {
                    ForEach(calendar.veryShortWeekdaySymbols.indices, id: \.self) { i in
                        let index = (i + calendar.firstWeekday - 1) % 7
                        Text(calendar.veryShortWeekdaySymbols[index])
                            .font(.caption2.weight(.semibold))
                            .foregroundStyle(BookInk.caption)
                            .accessibilityHidden(true)
                    }
                    ForEach(days.indices, id: \.self) { i in
                        if let day = days[i] {
                            let items = byDay[calendar.startOfDay(for: day)] ?? []
                            let isSelected = calendar.isDate(day, inSameDayAs: selected)
                            Button {
                                withAnimation(reduceMotion ? nil : .spring(response: 0.35, dampingFraction: 0.75)) {
                                    selected = calendar.startOfDay(for: day)
                                }
                            } label: {
                                VStack(spacing: 3) {
                                    Text("\(calendar.component(.day, from: day))")
                                        .font(.subheadline.monospacedDigit().weight(isSelected ? .semibold : .regular))
                                        .foregroundStyle(BookInk.ink)
                                    Circle()
                                        .fill(items.isEmpty ? Color.clear : BookInk.rule.opacity(0.7))
                                        .frame(width: 5, height: 5)
                                }
                                .frame(maxWidth: .infinity, minHeight: 44)
                                .background(
                                    RoundedRectangle(cornerRadius: 10, style: .continuous)
                                        .fill(BookInk.gild.opacity(isSelected ? 0.32 : 0))
                                        .scaleEffect(isSelected || reduceMotion ? 1 : 0.7)
                                )
                                .overlay(
                                    RoundedRectangle(cornerRadius: 10, style: .continuous)
                                        .strokeBorder(calendar.isDateInToday(day) ? BookInk.rule : .clear)
                                )
                                .contentShape(Rectangle())
                            }
                            .buttonStyle(PressableStyle(scale: 0.95))
                            .disabled(items.isEmpty)
                            .accessibilityLabel("\(day.formatted(date: .long, time: .omitted)), \(items.count == 1 ? "1 page" : "\(items.count) pages")")
                            .accessibilityAddTraits(isSelected ? .isSelected : [])
                        } else {
                            Color.clear.frame(height: 44)
                        }
                    }
                }

                Text(count == 1 ? "1 page this month" : "\(count) pages this month")
                    .font(.footnote.monospacedDigit())
                    .foregroundStyle(BookInk.caption)
                    .contentTransition(.numericText(value: Double(count)))
                    .animation(reduceMotion ? nil : Motion.gentle, value: count)
            }
            .cardSurface()

            if !chosen.isEmpty {
                VStack(alignment: .leading, spacing: Space.s) {
                    Text(dayTitle(selected))
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(BookInk.caption)
                        .accessibilityAddTraits(.isHeader)
                        .focusIn(delay: 0.02)
                    ForEach(Array(chosen.enumerated()), id: \.element.id) { index, entry in
                        Button { open(entry) } label: {
                            EntryCard(entry: entry)
                        }
                        .buttonStyle(.pressable)
                        .dealIn(min(index, 6), baseDelay: 0.05, step: 0.05)
                    }
                }
                .id(selected)
                .transition(.opacity)
            }
        }
        .sensoryFeedback(.selection, trigger: selected) { _, _ in hapticsOn }
    }

    private func dayTitle(_ day: Date) -> String {
        if Calendar.current.isDateInToday(day) { return "Today" }
        if Calendar.current.isDateInYesterday(day) { return "Yesterday" }
        return day.formatted(.dateTime.weekday(.wide).day().month(.wide))
    }

    private func shift(_ months: Int) {
        withAnimation(reduceMotion ? nil : Motion.gentle) {
            month = Calendar.current.date(byAdding: .month, value: months, to: month) ?? month
        }
    }

    private func daysInGrid() -> [Date?] {
        let calendar = Calendar.current
        guard let range = calendar.range(of: .day, in: .month, for: month) else { return [] }
        let firstWeekday = calendar.component(.weekday, from: month)
        let leading = (firstWeekday - calendar.firstWeekday + 7) % 7
        var days: [Date?] = Array(repeating: nil, count: leading)
        for day in range {
            days.append(calendar.date(byAdding: .day, value: day - 1, to: month))
        }
        return days
    }
}

// MARK: - Entry detail

/// One journal page. The page comes into focus piece by piece, the attached verse's Arabic is
/// revealed right to left like ink, and favoriting swaps the star with a bounce and a burst of petals.
struct JournalEntryView: View {
    let entryID: UUID

    @Environment(JournalStore.self) private var journal
    @Environment(QuranStore.self) private var quran
    @Environment(\.dismiss) private var dismiss
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @AppStorage(PrefKey.haptics) private var hapticsOn = true
    @State private var editing: JournalStart?
    @State private var updatingDua = false
    @State private var confirmDelete = false

    var body: some View {
        if let entry = journal.entry(id: entryID) {
            ScrollView {
                VStack(alignment: .leading, spacing: Space.l) {
                    VStack(alignment: .leading, spacing: Space.xs) {
                        HStack(spacing: 6) {
                            if let mood = entry.mood {
                                Label(mood.title, systemImage: mood.symbol)
                            }
                            if let origin = entry.origin { Text("· \(origin)") }
                        }
                        .font(.footnote.weight(.medium))
                        .foregroundStyle(BookInk.rule)
                        Text(entry.displayTitle)
                            .font(.system(.title, design: .serif, weight: .semibold))
                            .foregroundStyle(BookInk.ink)
                        Text(entry.date.formatted(date: .complete, time: .shortened))
                            .font(.subheadline)
                            .foregroundStyle(BookInk.caption)
                    }
                    .focusIn(delay: 0.05)

                    if let verse = entry.verse {
                        NavigationLink(value: Route.reader(surah: verse.surah, verse: verse.ayah)) {
                            VStack(alignment: .leading, spacing: Space.s) {
                                ArabicText(text: "\(quran.text(verse)) ﴿\(verse.ayah.arabicIndic)﴾", size: 22)
                                    .inkReveal(delay: 0.3)
                                TranslationBody(ref: verse)
                                Text(quran.displayName(verse))
                                    .font(.footnote.weight(.semibold))
                                    .foregroundStyle(BookInk.rule)
                            }
                            .cardSurface(fill: BookInk.gild.opacity(0.2).opacity(0.6))
                            .zoomSource(ZoomID.reader(verse.surah, verse.ayah))
                        }
                        .buttonStyle(.pressable)
                        .focusIn(delay: 0.15)
                        .scrollSettle()
                    }

                    MarkdownBody(text: entry.body)
                        .focusIn(delay: 0.22)

                    if !entry.gratitude.isEmpty {
                        VStack(alignment: .leading, spacing: 4) {
                            Text("Grateful for")
                                .font(.footnote.weight(.semibold))
                                .foregroundStyle(BookInk.rule)
                            Text(entry.gratitude)
                                .font(AppFont.serif(.body))
                        }
                        .cardSurface(padding: Space.m, fill: BookInk.gild.opacity(0.2).opacity(0.5))
                        .dealIn(0, baseDelay: 0.3)
                        .scrollSettle()
                    }

                    if entry.isDua {
                        duaJourney(entry)
                            .dealIn(1, baseDelay: 0.3)
                            .scrollSettle()
                    }

                    if !entry.tags.isEmpty {
                        Text(entry.tags.map { "#\($0)" }.joined(separator: "  "))
                            .font(.footnote)
                            .foregroundStyle(BookInk.caption)
                            .focusIn(delay: 0.4)
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
                    // Nothing on the page can be shared or changed while the journal is locked.
                    if journal.isAccessible {
                    Button {
                        withAnimation(reduceMotion ? nil : Motion.reward) {
                            journal.toggleFavorite(entry)
                        }
                    } label: {
                        Label(entry.favorite ? "Remove from favorites" : "Favorite", systemImage: entry.favorite ? "star.fill" : "star")
                            .contentTransition(.symbolEffect(.replace))
                            .symbolEffect(.bounce, value: reduceMotion ? false : entry.favorite)
                            .overlay { PetalBurst(trigger: entry.favorite, active: entry.favorite) }
                    }
                    .sensoryFeedback(.selection, trigger: entry.favorite) { _, _ in hapticsOn }
                    ShareLink(item: entry.plainText) {
                        Label("Share", systemImage: "square.and.arrow.up")
                    }
                    Menu {
                        Button("Edit", systemImage: "pencil") { editing = .edit(entry) }
                        Button("Delete", systemImage: "trash", role: .destructive) { confirmDelete = true }
                    } label: {
                        Label("More", systemImage: "ellipsis.circle")
                    }
                    }
                }
            }
            .fullScreenCover(item: $editing) { JournalStartView(start: $0) }
            .sheet(isPresented: $updatingDua) { DuaJourneySheet(entry: entry) }
            .confirmationDialog("Delete this page?", isPresented: $confirmDelete, titleVisibility: .visible) {
                Button("Delete Page", role: .destructive) {
                    journal.delete(entry)
                    dismiss()
                }
            } message: {
                Text("This cannot be undone.")
            }
        } else {
            ContentUnavailableView("Page not found", systemImage: "doc")
        }
    }

    private func duaJourney(_ entry: JournalEntry) -> some View {
        VStack(alignment: .leading, spacing: Space.s) {
            HStack {
                Text("Dua journey")
                    .font(.headline)
                Spacer()
                HStack(spacing: 6) {
                    if entry.duaStage.isAnswered {
                        Image(systemName: "sparkles")
                            .symbolEffect(.bounce, value: entry.duaStage)
                            .transition(.scale.combined(with: .opacity))
                    }
                    Text(entry.duaStage.title)
                        .contentTransition(.opacity)
                }
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(entry.duaStage.isAnswered ? Color(hex: 0x9A7420) : BookInk.rule)
                .overlay { AnsweredBurst(trigger: entry.duaStage, active: entry.duaStage.isAnswered) }
            }
            if !entry.nextStep.isEmpty {
                Text("Next step: \(entry.nextStep)")
                    .font(.subheadline)
            }
            if entry.duaStage.isAnswered {
                if !entry.answeredNote.isEmpty {
                    Text(entry.answeredNote)
                        .font(AppFont.serif(.body))
                }
                if let answeredAt = entry.answeredAt {
                    Text("Asked \(entry.date.formatted(date: .abbreviated, time: .omitted)). Answered \(answeredAt.formatted(date: .abbreviated, time: .omitted)).")
                        .font(.caption)
                        .foregroundStyle(BookInk.caption)
                }
            }
            Button("Update This Dua") { updatingDua = true }
                .buttonStyle(.tinted)
        }
        .cardSurface(fill: entry.duaStage.isAnswered ? Color(light: 0xFBF5E6, dark: 0x2A2519) : BookInk.pageLit)
        .background {
            if entry.duaStage.isAnswered { GoldenBloom(trigger: entry.duaStage) }
        }
        .animation(.spring(response: 0.5, dampingFraction: 0.7), value: entry.duaStage)
    }
}

/// "Where is this dua now?" The stages sit on a vertical path. The current step glows and the
/// path fills up to it; moving to an answered stage blooms gold with a few sparks.
struct DuaJourneySheet: View {
    let entry: JournalEntry

    @Environment(JournalStore.self) private var journal
    @Environment(\.dismiss) private var dismiss
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @AppStorage(PrefKey.haptics) private var hapticsOn = true
    @State private var stage: DuaStage = .asking
    @State private var nextStep = ""
    @State private var answeredNote = ""
    /// True once the saved stage is loaded, so opening the sheet does not fire a burst.
    @State private var loaded = false

    var body: some View {
        NavigationStack {
            Form {
                Section("Where is this dua now?") {
                    stagePath
                        .padding(.vertical, Space.xs)
                }
                .listRowBackground(BookInk.pageLit)

                Section("Next step") {
                    TextField("Optional", text: $nextStep, axis: .vertical)
                }
                .listRowBackground(BookInk.pageLit)

                if stage.isAnswered {
                    Section {
                        TextField("How did Allah answer?", text: $answeredNote, axis: .vertical)
                            .lineLimit(3...8)
                    } header: {
                        Text("What happened")
                    }
                    .listRowBackground(BookInk.pageLit)
                }

                Section {
                    Text("The Prophet ﷺ taught that every sincere dua is answered: it is given, saved for the Hereafter, or a harm of the same size is turned away. (Musnad Ahmad)")
                        .font(.footnote)
                        .foregroundStyle(BookInk.caption)
                }
                .listRowBackground(Color.clear)
            }
            .scrollContentBackground(.hidden)
            .bookPage()
            .journalPrivacyCover()
            .navigationTitle("Dua Journey")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancel") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        var updated = entry
                        updated.duaStage = stage
                        updated.nextStep = nextStep.trimmingCharacters(in: .whitespacesAndNewlines)
                        updated.answeredNote = stage.isAnswered ? answeredNote.trimmingCharacters(in: .whitespacesAndNewlines) : ""
                        journal.save(updated)
                        dismiss()
                    }
                    .disabled(!journal.isAccessible)
                }
            }
            .sensoryFeedback(.success, trigger: stage.isAnswered) { _, new in new && hapticsOn && loaded }
            .sensoryFeedback(.selection, trigger: stage) { _, new in hapticsOn && loaded && !new.isAnswered }
        }
        .onAppear {
            stage = entry.duaStage
            nextStep = entry.nextStep
            answeredNote = entry.answeredNote
            // Wait one turn so loading the saved stage is not treated as a change.
            DispatchQueue.main.async { loaded = true }
        }
        .presentationDetents([.medium, .large])
    }

    private var stagePath: some View {
        let stages = DuaStage.allCases
        let current = stages.firstIndex(of: stage) ?? 0
        return VStack(alignment: .leading, spacing: 0) {
            ForEach(Array(stages.enumerated()), id: \.element) { index, step in
                Button {
                    withAnimation(reduceMotion ? nil : (step.isAnswered ? Motion.reward : Motion.gentle)) {
                        stage = step
                    }
                } label: {
                    HStack(alignment: .center, spacing: Space.m) {
                        node(step, index: index, current: current)
                        Text(step.title)
                            .font(.body.weight(step == stage ? .semibold : .regular))
                            .foregroundStyle(step == stage ? (step.isAnswered ? Color(hex: 0x9A7420) : BookInk.ink) : BookInk.caption)
                            .multilineTextAlignment(.leading)
                        Spacer(minLength: 0)
                    }
                    .frame(minHeight: 44)
                    .contentShape(Rectangle())
                }
                .buttonStyle(PressableStyle(scale: 0.97))
                .accessibilityLabel(step.title)
                .accessibilityAddTraits(step == stage ? .isSelected : [])
            }
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel("Stage")
    }

    /// A dot on the path, with the line leading down to the next one.
    private func node(_ step: DuaStage, index: Int, current: Int) -> some View {
        let isCurrent = step == stage
        let reached = index <= current
        let gold = Color(hex: 0xD3A94A)
        let tint = stage.isAnswered ? gold : BookInk.rule
        let isLast = index == DuaStage.allCases.count - 1
        return ZStack {
            // The line to the next step, filled once she has passed this one.
            if !isLast {
                Capsule()
                    .fill(index < current ? tint : BookInk.rule.opacity(0.25))
                    .frame(width: 2, height: 28)
                    .offset(y: 22)
            }
            if isCurrent && step.isAnswered {
                GoldenBloom(trigger: stage)
                    .frame(width: 44, height: 44)
                    .clipShape(Circle())
            }
            Circle()
                .fill(isCurrent ? tint.opacity(0.22) : Color.clear)
                .frame(width: 30, height: 30)
                .scaleEffect(isCurrent ? 1 : 0.4)
                .blur(radius: isCurrent ? 2 : 0)
            Circle()
                .fill(reached ? tint : BookInk.pageLit)
                .overlay(Circle().strokeBorder(reached ? tint : BookInk.rule.opacity(0.25), lineWidth: 2))
                .frame(width: isCurrent ? 16 : 12, height: isCurrent ? 16 : 12)
                .shadow(color: isCurrent ? tint.opacity(0.55) : .clear, radius: 6)
            AnsweredBurst(trigger: stage, active: loaded && isCurrent && step.isAnswered)
        }
        .frame(width: 30, height: 30)
        .accessibilityHidden(true)
    }
}

/// Renders the light markdown used by templates: headings, bullets, checklists.
struct MarkdownBody: View {
    let text: String

    var body: some View {
        VStack(alignment: .leading, spacing: Space.s) {
            ForEach(Array(text.components(separatedBy: "\n").enumerated()), id: \.offset) { _, raw in
                let line = raw.trimmingCharacters(in: .whitespaces)
                if line.hasPrefix("## ") {
                    Text(String(line.dropFirst(3)).uppercased())
                        .font(.caption.weight(.semibold))
                        .tracking(0.6)
                        .foregroundStyle(BookInk.rule)
                        .padding(.top, Space.xs)
                } else if line.hasPrefix("- [x] ") || line.hasPrefix("- [ ] ") {
                    Label(String(line.dropFirst(6)), systemImage: line.hasPrefix("- [x]") ? "checkmark.square" : "square")
                        .font(AppFont.serif(.body))
                } else if line.hasPrefix("- ") {
                    HStack(alignment: .firstTextBaseline, spacing: Space.xs) {
                        Text("•").foregroundStyle(BookInk.rule)
                        Text(String(line.dropFirst(2)))
                    }
                    .font(AppFont.serif(.body))
                } else if !line.isEmpty {
                    Text(line)
                        .font(AppFont.serif(.body))
                        .lineSpacing(4)
                }
            }
        }
        .foregroundStyle(BookInk.ink)
        .frame(maxWidth: .infinity, alignment: .leading)
        .textSelection(.enabled)
    }
}

// MARK: - Journal motion

/// A soft blush ring around a page she just saved, fading as it settles.
private struct ArrivalGlow: View {
    @State private var faded = false
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        RoundedRectangle(cornerRadius: Radius.card, style: .continuous)
            .strokeBorder(BookInk.ribbon.opacity(faded ? 0 : 0.6), lineWidth: 2)
            .background(
                RoundedRectangle(cornerRadius: Radius.card, style: .continuous)
                    .fill(BookInk.ribbon.opacity(0.12).opacity(faded ? 0 : 0.35))
            )
            .allowsHitTesting(false)
            .onAppear {
                withAnimation(.easeOut(duration: reduceMotion ? 0.3 : 1.8).delay(0.4)) { faded = true }
            }
    }
}

/// A warm golden light that blooms behind an answered dua, then settles.
private struct GoldenBloom: View {
    let trigger: DuaStage
    @State private var phase = 0
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        RoundedRectangle(cornerRadius: Radius.card, style: .continuous)
            .fill(Color(hex: 0xF6ECCF))
            .blur(radius: 26)
            .scaleEffect(phase == 1 ? 1.08 : 1)
            .opacity(phase == 0 ? 0 : (phase == 1 ? 0.95 : 0.4))
            .allowsHitTesting(false)
            .onAppear(perform: play)
            .onChange(of: trigger) { _, _ in phase = 0; play() }
    }

    private func play() {
        if reduceMotion { phase = 2; return }
        withAnimation(.spring(response: 0.6, dampingFraction: 0.6)) { phase = 1 }
        withAnimation(.smooth(duration: 1.4).delay(0.9)) { phase = 2 }
    }
}

/// Sparks of gold that fly out when a dua is marked answered.
private struct AnsweredBurst: View {
    let trigger: DuaStage
    let active: Bool
    @State private var fire = 0
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        ZStack {
            ForEach(0..<10, id: \.self) { i in
                let angle = Double(i) / 10 * 2 * .pi
                Circle()
                    .fill(Color(hex: i.isMultiple(of: 2) ? 0xD3A94A : 0xF2C66D))
                    .frame(width: 5, height: 5)
                    .keyframeAnimator(initialValue: PetalState(), trigger: fire) { spark, value in
                        spark
                            .offset(x: cos(angle) * value.distance, y: sin(angle) * value.distance)
                            .opacity(value.opacity)
                            .scaleEffect(value.scale)
                    } keyframes: { _ in
                        KeyframeTrack(\.distance) {
                            CubicKeyframe(0, duration: 0.01)
                            SpringKeyframe(54, duration: 0.6, spring: .snappy)
                        }
                        KeyframeTrack(\.opacity) {
                            LinearKeyframe(1, duration: 0.05)
                            LinearKeyframe(1, duration: 0.3)
                            LinearKeyframe(0, duration: 0.4)
                        }
                        KeyframeTrack(\.scale) {
                            CubicKeyframe(1.2, duration: 0.25)
                            CubicKeyframe(0.5, duration: 0.5)
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
