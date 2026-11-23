import SwiftUI
import UIKit

// MARK: - Reminders

/// Her two gentle reminders. Each section deals in, and when notifications are off
/// in Settings, the note about it slides down into place.
struct ReminderSettingsView: View {
    @AppStorage(PrefKey.reminderEnabled) private var quranOn = false
    @AppStorage(PrefKey.reminderMinutes) private var quranMinutes = 7 * 60
    @AppStorage(PrefKey.eveningReminderEnabled) private var eveningOn = false
    @AppStorage(PrefKey.eveningReminderMinutes) private var eveningMinutes = 20 * 60
    @Environment(\.openURL) private var openURL
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var denied = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: Space.xl) {
                ReminderSection(
                    title: "Quran moment",
                    isOn: $quranOn, minutes: $quranMinutes, kind: .quran, denied: $denied
                )
                .dealIn(0, baseDelay: 0.12)

                ReminderSection(
                    title: "Evening remembrance",
                    isOn: $eveningOn, minutes: $eveningMinutes, kind: .evening, denied: $denied
                )
                .dealIn(1, baseDelay: 0.12)

                if denied {
                    GroupedSection {
                        VStack(alignment: .leading, spacing: Space.s) {
                            Text("Notifications are off.")
                                .font(.subheadline)
                                .foregroundStyle(Palette.textSecondary)
                                .fixedSize(horizontal: false, vertical: true)
                            Button("Open Settings") {
                                if let url = URL(string: UIApplication.openSettingsURLString) { openURL(url) }
                            }
                            .buttonStyle(.tintedCompact)
                        }
                        .padding(Space.m)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    }
                    .transition(reduceMotion ? AnyTransition.opacity : AnyTransition.move(edge: .top).combined(with: .opacity))
                }
            }
            .padding(.horizontal, Space.page)
            .padding(.vertical, Space.s)
            .animation(reduceMotion ? nil : Motion.smooth, value: denied)
        }
        .pageBackground()
        .navigationTitle("Reminders")
        .navigationBarTitleDisplayMode(.inline)
        .task { denied = await ReminderScheduler.isDenied() }
    }
}

/// One reminder. Its icon bounces as she turns it on, the time row opens, and a sample
/// notification drops in. Changing the time rolls the time shown on the sample.
private struct ReminderSection: View {
    let title: String
    @Binding var isOn: Bool
    @Binding var minutes: Int
    let kind: ReminderKind
    @Binding var denied: Bool

    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @AppStorage(PrefKey.haptics) private var hapticsOn = true

    var body: some View {
        GroupedSection {
            Toggle(isOn: Binding(
                get: { isOn },
                set: { newValue in Task { await setEnabled(newValue) } }
            )) {
                HStack(spacing: Space.m) {
                    Image(systemName: symbol)
                        .font(.system(size: 17))
                        .foregroundStyle(Palette.accent)
                        .frame(width: 26)
                        .contentTransition(.symbolEffect(.replace))
                        .symbolEffect(.bounce, value: isOn)
                        .accessibilityHidden(true)
                    Text(title)
                        .foregroundStyle(Palette.textPrimary)
                }
            }
            .tint(Palette.accentFill)
            .padding(.horizontal, Space.m)
            .padding(.vertical, 8)
            .frame(minHeight: 48)

            if isOn {
                RowDivider()
                DatePicker(selection: timeBinding, displayedComponents: .hourAndMinute) {
                    HStack(spacing: Space.m) {
                        Image(systemName: "clock")
                            .font(.system(size: 17))
                            .foregroundStyle(Palette.accent)
                            .frame(width: 26)
                            .accessibilityHidden(true)
                        Text("Time")
                            .foregroundStyle(Palette.textPrimary)
                    }
                }
                .tint(Palette.accentFill)
                .padding(.horizontal, Space.m)
                .padding(.vertical, 6)
                .frame(minHeight: 48)
                .transition(.opacity)

                ReminderSample(kind: kind, time: time, minutes: minutes)
                    .padding(Space.m)
                    .background(Palette.accentWash.opacity(0.5))
                    .transition(.opacity)
            }
        }
        .animation(reduceMotion ? nil : Motion.gentle, value: isOn)
        .sensoryFeedback(.impact(flexibility: .soft, intensity: 0.7), trigger: isOn) { _, new in hapticsOn && new }
    }

    private var symbol: String {
        switch kind {
        case .quran: isOn ? "book.fill" : "book"
        case .evening: isOn ? "moon.stars.fill" : "moon.stars"
        }
    }

    private var time: Date {
        Calendar.current.date(bySettingHour: minutes / 60, minute: minutes % 60, second: 0, of: .now) ?? .now
    }

    private var timeBinding: Binding<Date> {
        Binding(
            get: { time },
            set: { date in
                let parts = Calendar.current.dateComponents([.hour, .minute], from: date)
                minutes = (parts.hour ?? 7) * 60 + (parts.minute ?? 0)
                Task { await setEnabled(true) }
            }
        )
    }

    private func setEnabled(_ on: Bool) async {
        if on {
            let ok = await ReminderScheduler.enable(atMinutes: minutes, kind: kind)
            isOn = ok
            denied = !ok
        } else {
            ReminderScheduler.disable(kind)
            isOn = false
        }
    }
}

/// What the reminder will look like. It drops in with a small settle,
/// and gives a tiny nudge with the time rolling when she changes the time.
private struct ReminderSample: View {
    let kind: ReminderKind
    let time: Date
    let minutes: Int
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var shown = false

    private var timeText: String { time.formatted(date: .omitted, time: .shortened) }

    var body: some View {
        HStack(alignment: .top, spacing: Space.s) {
            RoundedRectangle(cornerRadius: 8, style: .continuous)
                .fill(Palette.accentFill)
                .frame(width: 34, height: 34)
                .overlay(
                    LotusMark()
                        .stroke(.white, style: StrokeStyle(lineWidth: 1.2, lineCap: .round))
                        .padding(7)
                )
            VStack(alignment: .leading, spacing: 1) {
                HStack {
                    Text(kind.title)
                        .font(.footnote.weight(.semibold))
                        .lineLimit(1)
                    Spacer(minLength: Space.xs)
                    Text(timeText)
                        .font(.caption2.monospacedDigit())
                        .foregroundStyle(.secondary)
                        .contentTransition(reduceMotion ? .identity : .numericText(value: Double(minutes)))
                        .animation(reduceMotion ? nil : .snappy, value: minutes)
                }
                Text(kind.body)
                    .font(.footnote)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        .foregroundStyle(Palette.textPrimary)
        .padding(10)
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 18, style: .continuous))
        .shadow(color: .black.opacity(0.12), radius: 10, y: 4)
        .keyframeAnimator(initialValue: 1.0, trigger: minutes) { [still = reduceMotion] view, scale in
            view.scaleEffect(still ? 1 : scale)
        } keyframes: { _ in
            SpringKeyframe(1.025, duration: 0.12)
            SpringKeyframe(1.0, duration: 0.3, spring: .smooth)
        }
        .offset(y: shown || reduceMotion ? 0 : -24)
        .opacity(shown || reduceMotion ? 1 : 0)
        .scaleEffect(shown || reduceMotion ? 1 : 0.96, anchor: .top)
        .onAppear {
            withAnimation(.spring(response: 0.5, dampingFraction: 0.68).delay(0.15)) { shown = true }
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel("Sample reminder at \(timeText): \(kind.title). \(kind.body)")
    }
}

// MARK: - iCloud sync

/// iCloud Sync. The picture comes into focus, and while sync is on the cloud pulses
/// and dots flow between iPhone and iPad.
struct SyncSettingsView: View {
    @Environment(LibraryStore.self) private var library
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @AppStorage(PrefKey.haptics) private var hapticsOn = true

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: Space.xl) {
                SyncPicture(isOn: library.syncEnabled)
                    .focusIn(delay: 0.05)

                GroupedSection {
                    Toggle(isOn: Binding(
                        get: { library.syncEnabled },
                        set: { on in withAnimation(reduceMotion ? nil : Motion.gentle) { library.setSyncEnabled(on) } }
                    )) {
                        HStack(spacing: Space.m) {
                            Image(systemName: library.syncEnabled ? "icloud.fill" : "icloud")
                                .font(.system(size: 17))
                                .foregroundStyle(Palette.accent)
                                .frame(width: 26)
                                .contentTransition(.symbolEffect(.replace))
                                .symbolEffect(.bounce, value: library.syncEnabled)
                                .accessibilityHidden(true)
                            Text("Sync with iCloud")
                                .foregroundStyle(Palette.textPrimary)
                        }
                    }
                    .tint(Palette.accentFill)
                    .padding(.horizontal, Space.m)
                    .padding(.vertical, 8)
                    .frame(minHeight: 48)
                }
                .dealIn(0, baseDelay: 0.18)
            }
            .padding(.horizontal, Space.page)
            .padding(.vertical, Space.s)
        }
        .pageBackground()
        .navigationTitle("iCloud Sync")
        .navigationBarTitleDisplayMode(.inline)
        .sensoryFeedback(.impact(flexibility: .soft, intensity: 0.7), trigger: library.syncEnabled) { _, _ in hapticsOn }
    }
}

/// iPhone and iPad with a cloud between them. While sync is on, the cloud pulses
/// and the dots light up in turn, toward the cloud.
private struct SyncPicture: View {
    let isOn: Bool
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        HStack(spacing: Space.s) {
            device("iphone")
            link(reversed: false)
            Image(systemName: isOn ? "icloud.fill" : "icloud")
                .font(.system(size: 34, weight: .light))
                .foregroundStyle(isOn ? Palette.accent : Palette.textSecondary)
                .contentTransition(.symbolEffect(.replace))
                .symbolEffect(.pulse, options: .repeating, isActive: isOn && !reduceMotion)
            link(reversed: true)
            device("ipad")
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, Space.xl)
        .background(RoundedRectangle(cornerRadius: Radius.card, style: .continuous).fill(Palette.surface))
        .animation(reduceMotion ? nil : Motion.gentle, value: isOn)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(isOn ? "iCloud Sync is on" : "iCloud Sync is off")
    }

    private func device(_ symbol: String) -> some View {
        Image(systemName: symbol)
            .font(.system(size: 28, weight: .light))
            .foregroundStyle(isOn ? Palette.accent : Palette.textSecondary)
    }

    @ViewBuilder
    private func link(reversed: Bool) -> some View {
        if isOn && !reduceMotion {
            PhaseAnimator([0, 1, 2]) { phase in
                dots(lit: reversed ? 2 - phase : phase)
            } animation: { _ in .easeInOut(duration: 0.35) }
        } else {
            dots(lit: nil)
        }
    }

    private func dots(lit: Int?) -> some View {
        HStack(spacing: 5) {
            ForEach(0..<3, id: \.self) { i in
                Circle()
                    .fill(Palette.accent)
                    .frame(width: 5, height: 5)
                    .opacity(lit.map { $0 == i ? 1 : 0.3 } ?? (isOn ? 0.6 : 0.2))
            }
        }
    }
}

// MARK: - Language

/// Translation choices deal in one after another. Choosing one moves the check mark
/// to it, and a download in progress fades in and out beside it.
struct LanguageSettingsView: View {
    @Environment(TranslationStore.self) private var translations
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: Space.l) {
                VStack(spacing: Space.xs) {
                    ForEach(Array(TranslationOption.all.enumerated()), id: \.element.id) { index, option in
                        SelectableRow(isSelected: translations.selectedID == option.id) {
                            withAnimation(reduceMotion ? nil : Motion.gentle) { translations.selectedID = option.id }
                        } label: {
                            HStack {
                                TranslationOptionLabel(option: option)
                                Spacer()
                                if translations.state(option.id) == .downloading {
                                    ProgressView()
                                        .transition(.scale.combined(with: .opacity))
                                        .accessibilityLabel("Downloading")
                                }
                            }
                            .animation(reduceMotion ? nil : Motion.gentle, value: translations.state(option.id))
                        }
                        .dealIn(min(index, 8), baseDelay: 0.12, step: 0.05)
                    }
                }
            }
            .padding(.horizontal, Space.page)
            .padding(.vertical, Space.s)
        }
        .pageBackground()
        .navigationTitle("Language & Translation")
        .navigationBarTitleDisplayMode(.inline)
    }
}

// MARK: - Downloads

/// Translations she can keep on this iPhone. Rows deal in, and each status
/// changes with a soft pop.
struct DownloadsView: View {
    @Environment(TranslationStore.self) private var translations

    var body: some View {
        List {
            Section {
                LabeledContent("Arabic Quran", value: "Included")
                    .focusIn(delay: 0.05)
            }
            .listRowBackground(Palette.surface)

            Section {
                ForEach(Array(TranslationOption.all.enumerated()), id: \.element.id) { index, option in
                    DownloadRow(option: option)
                        .dealIn(min(index, 8), baseDelay: 0.15, step: 0.05)
                        .swipeActions {
                            if translations.state(option.id) == .ready && option.id != translations.selectedID {
                                Button("Delete", role: .destructive) { translations.delete(option.id) }
                            }
                        }
                }
            } header: {
                Text("Translations")
            }
            .listRowBackground(Palette.surface)

            TransliterationDownloadSection()
            RecitationDownloadsSection()
        }
        .scrollContentBackground(.hidden)
        .pageBackground()
        .navigationTitle("Downloads")
        .navigationBarTitleDisplayMode(.inline)
    }
}

/// One translation. A finished download lands with a check mark that pops in,
/// a small burst of petals and a gentle tap.
private struct DownloadRow: View {
    let option: TranslationOption
    @Environment(TranslationStore.self) private var translations
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @AppStorage(PrefKey.haptics) private var hapticsOn = true

    private var state: TranslationStore.State { translations.state(option.id) }

    private var pop: AnyTransition {
        reduceMotion ? .opacity : .scale(scale: 0.5).combined(with: .opacity)
    }

    var body: some View {
        HStack {
            TranslationOptionLabel(option: option)
            Spacer()
            status
                .overlay(alignment: .topTrailing) {
                    PetalBurst(trigger: state == .ready, active: state == .ready)
                        .frame(width: 22, height: 22)
                }
        }
        .animation(reduceMotion ? nil : Motion.reward, value: state)
        .sensoryFeedback(.impact(flexibility: .soft, intensity: 0.6), trigger: state) { _, new in hapticsOn && new == .downloading }
        .sensoryFeedback(.success, trigger: state) { old, new in hapticsOn && old == .downloading && new == .ready }
    }

    @ViewBuilder
    private var status: some View {
        switch state {
        case .ready:
            VStack(alignment: .trailing, spacing: 2) {
                Image(systemName: "checkmark.circle.fill")
                    .foregroundStyle(Palette.accent)
                    .accessibilityLabel("Downloaded")
                if let size = translations.sizeOnDisk(option.id) {
                    Text(ByteCountFormatter.string(fromByteCount: size, countStyle: .file))
                        .font(.caption)
                        .foregroundStyle(Palette.textSecondary)
                }
            }
            .transition(pop)
        case .downloading:
            ProgressView()
                .accessibilityLabel("Downloading")
                .transition(pop)
        case .failed:
            Button("Retry") { Task { await translations.download(option.id) } }
                .buttonStyle(.borderless)
                .transition(pop)
        case .notDownloaded:
            Button {
                Task { await translations.download(option.id) }
            } label: {
                Image(systemName: "arrow.down.circle")
                    .font(.title3)
            }
            .buttonStyle(.borderless)
            .accessibilityLabel("Download \(option.language)")
            .transition(pop)
        }
    }
}

// MARK: - Privacy and about

/// Privacy and sources. The lotus draws itself, the name comes into focus,
/// and each point deals in after it.
struct AboutView: View {
    @Environment(LibraryStore.self) private var library
    @Environment(JournalStore.self) private var journal
    @Environment(DhikrStore.self) private var dhikrStore
    @Environment(PlanHistoryStore.self) private var planHistory
    @Environment(ChallengeStore.self) private var challenges
    @Environment(FastingStore.self) private var fasting
    @Environment(MissedPrayersStore.self) private var missedPrayers
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @AppStorage(PrefKey.haptics) private var hapticsOn = true
    @State private var confirmErase = false
    @State private var erased = false

    private static let privacyPoints: [(symbol: String, text: String)] = [
        ("iphone", "Your data stays on this iPhone"),
        ("location", "Location is only used for prayer times and the Qibla"),
        ("person.crop.circle.badge.xmark", "No account"),
        ("chart.bar.xaxis", "No ads or tracking"),
    ]

    private static let sources: [(title: String, detail: String)] = [
        ("Arabic Quran text", "Tanzil Project, Uthmani"),
        ("Translations", "AlQuran Cloud"),
        ("Recitation", "Islamic Network"),
        ("Duas and dhikr", "Quran and hadith"),
        ("Arabic font", "Amiri Quran, SIL OFL"),
        ("Prayer times", "Calculated on device"),
    ]

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: Space.xl) {
                AboutHeader()

                GroupedSection(title: "Your privacy") {
                    ForEach(Array(Self.privacyPoints.enumerated()), id: \.offset) { index, point in
                        if index > 0 { RowDivider() }
                        PrivacyPoint(symbol: point.symbol, text: point.text)
                            .dealIn(index, baseDelay: 0.35, step: 0.06)
                    }
                }
                .focusIn(delay: 0.3)

                GroupedSection(title: "Sources") {
                    ForEach(Array(Self.sources.enumerated()), id: \.offset) { index, source in
                        if index > 0 { RowDivider(inset: Space.m) }
                        SourceRow(title: source.title, detail: source.detail)
                            .dealIn(index, baseDelay: 0.5, step: 0.05)
                    }
                }
                .focusIn(delay: 0.45)

                GroupedSection {
                    Button(role: .destructive) { confirmErase = true } label: {
                        HStack(spacing: Space.m) {
                            Image(systemName: "trash")
                                .font(.system(size: 17))
                                .frame(width: 26)
                                .symbolEffect(.bounce, value: erased)
                                .accessibilityHidden(true)
                            Text("Erase My Data")
                            Spacer()
                        }
                        .foregroundStyle(Color.red)
                        .padding(.horizontal, Space.m)
                        .frame(minHeight: 48)
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(RowPressStyle())

                    if erased {
                        RowDivider()
                        Label("Data erased", systemImage: "checkmark.circle.fill")
                            .font(.subheadline)
                            .foregroundStyle(Palette.accent)
                            .padding(.horizontal, Space.m)
                            .padding(.vertical, Space.s)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .transition(.opacity)
                    }
                }
                .focusIn(delay: 0.55)
            }
            .padding(.horizontal, Space.page)
            .padding(.vertical, Space.s)
            .animation(reduceMotion ? nil : Motion.gentle, value: erased)
        }
        .pageBackground()
        .navigationTitle("Privacy & About")
        .navigationBarTitleDisplayMode(.inline)
        .sensoryFeedback(.success, trigger: erased) { _, new in hapticsOn && new }
        .confirmationDialog("Erase all your data on this iPhone?", isPresented: $confirmErase, titleVisibility: .visible) {
            Button("Erase My Data", role: .destructive) {
                library.eraseAll()
                journal.eraseAll()
                dhikrStore.eraseAll()
                planHistory.eraseAll()
                challenges.eraseAll()
                fasting.eraseAll()
                missedPrayers.eraseAll()
                erased = true
            }
        } message: {
            Text("This cannot be undone.")
        }
    }
}

/// The lotus draws itself line by line, then the name and version come into focus.
private struct AboutHeader: View {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var drawn = false

    private static var version: String {
        Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "1.0"
    }

    var body: some View {
        VStack(spacing: Space.s) {
            LotusMark()
                .trim(from: 0, to: drawn ? 1 : 0)
                .stroke(Palette.accent, style: StrokeStyle(lineWidth: 1.6, lineCap: .round, lineJoin: .round))
                .frame(width: 54, height: 50)
                .padding(Space.m)
                .background(Circle().fill(Palette.accentWash))
                .scaleEffect(drawn || reduceMotion ? 1 : 0.92)
                .accessibilityHidden(true)
            Text("Quran for Women")
                .font(.system(.title3, design: .serif, weight: .semibold))
                .foregroundStyle(Palette.textPrimary)
                .focusIn(delay: 0.45)
            Text("Version \(Self.version)")
                .font(.subheadline)
                .foregroundStyle(Palette.textSecondary)
                .multilineTextAlignment(.center)
                .focusIn(delay: 0.6)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, Space.l)
        .padding(.horizontal, Space.m)
        .background(RoundedRectangle(cornerRadius: Radius.card, style: .continuous).fill(Palette.surface))
        .accessibilityElement(children: .combine)
        .onAppear {
            guard !drawn else { return }
            if reduceMotion {
                drawn = true
            } else {
                withAnimation(.easeInOut(duration: 1.4).delay(0.1)) { drawn = true }
            }
        }
    }
}

private struct PrivacyPoint: View {
    let symbol: String
    let text: String

    var body: some View {
        HStack(alignment: .top, spacing: Space.s) {
            RowIcon(systemImage: symbol)
            Text(text)
                .font(.subheadline)
                .foregroundStyle(Palette.textPrimary)
                .fixedSize(horizontal: false, vertical: true)
            Spacer(minLength: 0)
        }
        .padding(.horizontal, Space.m)
        .padding(.vertical, Space.s)
    }
}

private struct SourceRow: View {
    let title: String
    let detail: String

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(title)
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(Palette.textPrimary)
            Text(detail)
                .font(.subheadline)
                .foregroundStyle(Palette.textSecondary)
                .fixedSize(horizontal: false, vertical: true)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, Space.m)
        .padding(.vertical, Space.s)
        .accessibilityElement(children: .combine)
    }
}
