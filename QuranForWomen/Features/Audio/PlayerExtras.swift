import SwiftUI

extension AudioPlayer {
    /// What the repeat label says while a range of verses is repeating.
    var loopTitle: String? {
        guard let loopRange else { return nil }
        let verses = loopRange.lowerBound == loopRange.upperBound
            ? "Verse \(loopRange.lowerBound)"
            : "Verses \(loopRange.lowerBound) to \(loopRange.upperBound)"
        return loopMode == .off ? verses : "\(verses) · \(loopMode.title)"
    }
}

// MARK: - Full player row

/// Speed, sleep timer and offline download, under the repeat and reciter menus.
/// Each symbol gives a small bounce when its value changes.
struct PlayerExtrasRow: View {
    @Environment(AudioPlayer.self) private var player
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @AppStorage(PrefKey.haptics) private var hapticsOn = true

    var body: some View {
        if let current = player.current {
            HStack {
                Menu {
                    ForEach(PlaybackSpeed.choices, id: \.self) { value in
                        Button {
                            player.rate = value
                        } label: {
                            if player.rate == value {
                                Label(PlaybackSpeed.label(value), systemImage: "checkmark")
                            } else {
                                Text(PlaybackSpeed.label(value))
                            }
                        }
                    }
                } label: {
                    Label(PlaybackSpeed.label(player.rate), systemImage: "gauge.with.dots.needle.50percent")
                        .font(.subheadline.weight(.medium))
                        .monospacedDigit()
                        .contentTransition(.opacity)
                        .symbolEffect(.bounce, value: reduceMotion ? Float(0) : player.rate)
                        .frame(minHeight: 44)
                }
                .accessibilityLabel("Playback speed, \(PlaybackSpeed.label(player.rate))")

                Spacer(minLength: Space.xs)

                Menu {
                    ForEach(SleepTimer.choices, id: \.self) { timer in
                        Button {
                            player.setSleep(timer)
                        } label: {
                            if player.sleepTimer == timer {
                                Label(timer.title, systemImage: "checkmark")
                            } else {
                                Text(timer.title)
                            }
                        }
                    }
                    if player.sleepTimer != nil {
                        Button("Turn off") { player.setSleep(nil) }
                    }
                } label: {
                    TimelineView(.periodic(from: .now, by: 1)) { context in
                        Label(sleepText(at: context.date), systemImage: "moon.zzz")
                            .font(.subheadline.weight(.medium))
                            .monospacedDigit()
                            .lineLimit(1)
                            .contentTransition(.opacity)
                            .symbolEffect(.bounce, value: reduceMotion ? nil : player.sleepTimer)
                            .frame(minHeight: 44)
                    }
                }
                .accessibilityLabel(sleepAccessibility)

                Spacer(minLength: Space.xs)

                RecitationDownloadButton(reciterID: player.reciterID, surah: current.surah)
            }
            .foregroundStyle(Palette.accent)
            .animation(reduceMotion ? nil : Motion.gentle, value: player.rate)
            .animation(reduceMotion ? nil : Motion.gentle, value: player.sleepTimer)
            .sensoryFeedback(.selection, trigger: player.rate) { _, _ in hapticsOn }
            .sensoryFeedback(.selection, trigger: player.sleepTimer) { _, _ in hapticsOn }
        }
    }

    private func minutesLeft(at date: Date) -> Int? {
        guard let ends = player.sleepEndsAt else { return nil }
        return Int((max(0, ends.timeIntervalSince(date)) / 60).rounded(.up))
    }

    private func sleepText(at date: Date) -> String {
        switch player.sleepTimer {
        case nil: "Sleep"
        case .endOfSurah: "End of surah"
        case .minutes(let m): "\(minutesLeft(at: date) ?? m) min"
        }
    }

    private var sleepAccessibility: String {
        switch player.sleepTimer {
        case nil: "Sleep timer, off"
        case .endOfSurah: "Sleep timer, end of surah"
        case .minutes: "Sleep timer, \(minutesLeft(at: .now) ?? 0) minutes left"
        }
    }
}

/// Downloads the surah playing now for the reciter playing it. A ring fills while it
/// downloads, and a finished download lands with a few petals and a gentle tap.
struct RecitationDownloadButton: View {
    let reciterID: String
    let surah: Int

    @Environment(RecitationDownloads.self) private var downloads
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @AppStorage(PrefKey.haptics) private var hapticsOn = true

    private var state: RecitationDownloads.State { downloads.state(reciterID: reciterID, surah: surah) }
    private var percent: Int { Int((downloads.progress(reciterID: reciterID, surah: surah) * 100).rounded()) }

    var body: some View {
        Button {
            switch state {
            case .downloading: downloads.cancel(reciterID: reciterID, surah: surah)
            case .ready: break
            case .notDownloaded, .failed: downloads.download(reciterID: reciterID, surah: surah)
            }
        } label: {
            HStack(spacing: 6) {
                ZStack {
                    if state == .downloading {
                        DownloadRing(progress: downloads.progress(reciterID: reciterID, surah: surah))
                            .frame(width: 16, height: 16)
                    } else {
                        Image(systemName: symbol)
                            .contentTransition(.symbolEffect(.replace))
                    }
                }
                .frame(width: 20, height: 20)
                Text(title)
                    .monospacedDigit()
                    .lineLimit(1)
                    .contentTransition(reduceMotion ? .opacity : .numericText(value: Double(percent)))
            }
            .font(.subheadline.weight(.medium))
            .frame(minHeight: 44)
            .contentShape(Rectangle())
        }
        .buttonStyle(PressableStyle(scale: 0.95))
        .overlay {
            PetalBurst(trigger: state == .ready, active: state == .ready)
                .frame(width: 22, height: 22)
                .allowsHitTesting(false)
        }
        .animation(reduceMotion ? nil : Motion.reward, value: state)
        .animation(reduceMotion ? nil : Motion.gentle, value: percent)
        .sensoryFeedback(.impact(flexibility: .soft, intensity: 0.6), trigger: state) { _, new in hapticsOn && new == .downloading }
        .sensoryFeedback(.success, trigger: state) { old, new in hapticsOn && old == .downloading && new == .ready }
        .accessibilityLabel(accessibility)
    }

    private var symbol: String {
        switch state {
        case .ready: "checkmark.circle.fill"
        case .failed: "wifi.exclamationmark"
        case .notDownloaded, .downloading: "arrow.down.circle"
        }
    }

    private var title: String {
        switch state {
        case .notDownloaded: "Download"
        case .downloading: "\(percent)%"
        case .ready: "Downloaded"
        case .failed: "Retry"
        }
    }

    private var accessibility: String {
        switch state {
        case .notDownloaded: "Download this surah"
        case .downloading: "Downloading, \(percent) percent. Cancel"
        case .ready: "Downloaded"
        case .failed: "Download failed. Retry"
        }
    }
}

/// A thin ring that fills with a download.
private struct DownloadRing: View {
    let progress: Double

    var body: some View {
        ZStack {
            Circle().stroke(Palette.accent.opacity(0.2), lineWidth: 2)
            Circle()
                .trim(from: 0, to: progress)
                .stroke(Palette.accent, style: StrokeStyle(lineWidth: 2, lineCap: .round))
                .rotationEffect(.degrees(-90))
                .animation(Motion.gentle, value: progress)
        }
        .accessibilityHidden(true)
    }
}

// MARK: - Repeat a range

/// Pick a first and last verse in the surah and how many times to play them.
/// The numbers roll as she steps them, and the rows deal in one after another.
struct RangeRepeatSheet: View {
    let surahID: Int
    /// When set, closes the screen that opened this sheet instead of only the sheet.
    var onStarted: (() -> Void)?

    @Environment(QuranStore.self) private var quran
    @Environment(AudioPlayer.self) private var player
    @Environment(\.dismiss) private var dismiss
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @AppStorage(PrefKey.haptics) private var hapticsOn = true

    @State private var from: Int
    @State private var to: Int
    @State private var mode: AudioPlayer.RepeatMode = .times(3)
    @State private var didSetUp = false

    private static let modes: [AudioPlayer.RepeatMode] = [.off, .times(3), .times(5), .times(10), .forever]

    init(surahID: Int, startAyah: Int, onStarted: (() -> Void)? = nil) {
        self.surahID = surahID
        self.onStarted = onStarted
        _from = State(initialValue: max(startAyah, 1))
        _to = State(initialValue: max(startAyah, 1))
    }

    var body: some View {
        let surah = quran.surah(surahID)
        NavigationStack {
            Form {
                Section {
                    Stepper(value: $from, in: 1...surah.verseCount) {
                        HStack {
                            Text("From verse")
                            Spacer()
                            RollingNumber(value: from)
                                .font(.headline)
                                .foregroundStyle(Palette.accent)
                        }
                    }
                    .accessibilityValue("\(from)")
                    .dealIn(0, baseDelay: 0.1, step: 0.06)
                    Stepper(value: $to, in: 1...surah.verseCount) {
                        HStack {
                            Text("To verse")
                            Spacer()
                            RollingNumber(value: to)
                                .font(.headline)
                                .foregroundStyle(Palette.accent)
                        }
                    }
                    .accessibilityValue("\(to)")
                    .dealIn(1, baseDelay: 0.1, step: 0.06)
                } header: {
                    Text(surah.name)
                }
                .listRowBackground(Palette.surface)

                Section {
                    ForEach(Array(Self.modes.enumerated()), id: \.element) { index, choice in
                        Button {
                            withAnimation(reduceMotion ? nil : Motion.reward) { mode = choice }
                        } label: {
                            HStack {
                                Text(choice == .off ? "Once" : choice.title)
                                    .foregroundStyle(Palette.textPrimary)
                                Spacer()
                                if mode == choice {
                                    Image(systemName: "checkmark")
                                        .font(.body.weight(.semibold))
                                        .foregroundStyle(Palette.accent)
                                        .transition(.scale(scale: 0.3).combined(with: .opacity))
                                        .accessibilityHidden(true)
                                }
                            }
                            .contentShape(Rectangle())
                        }
                        .accessibilityAddTraits(mode == choice ? .isSelected : [])
                        .dealIn(index + 2, baseDelay: 0.1, step: 0.06)
                    }
                } header: {
                    Text("Repeat")
                }
                .listRowBackground(Palette.surface)

                Section {
                    Button {
                        player.repeatRange(surah: surahID, from: from, to: to, mode: mode)
                        if let onStarted { onStarted() } else { dismiss() }
                    } label: {
                        Label("Play", systemImage: "play.fill")
                            .font(.headline)
                            .frame(maxWidth: .infinity)
                    }
                    .dealIn(Self.modes.count + 2, baseDelay: 0.1, step: 0.06)
                }
                .listRowBackground(Palette.surface)
            }
            .scrollContentBackground(.hidden)
            .pageBackground()
            .navigationTitle("Repeat a range")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
            }
            .onChange(of: from) { _, new in if to < new { to = new } }
            .onChange(of: to) { _, new in if from > new { from = new } }
            .sensoryFeedback(.selection, trigger: from) { _, _ in hapticsOn }
            .sensoryFeedback(.selection, trigger: to) { _, _ in hapticsOn }
            .sensoryFeedback(.selection, trigger: mode) { _, _ in hapticsOn }
            .onAppear {
                guard !didSetUp else { return }
                didSetUp = true
                if let range = player.loopRange, player.current?.surah == surahID {
                    from = range.lowerBound
                    to = range.upperBound
                    mode = player.loopMode
                } else {
                    to = min(from + 4, surah.verseCount)
                }
            }
        }
        .presentationDetents([.medium, .large])
        .presentationDragIndicator(.visible)
    }
}

// MARK: - Reading settings

/// Recitation speed as a segmented control, for the reading settings.
struct PlaybackSpeedRow: View {
    @Environment(AudioPlayer.self) private var player
    @AppStorage(PrefKey.haptics) private var hapticsOn = true

    var body: some View {
        @Bindable var player = player
        VStack(alignment: .leading, spacing: Space.xs) {
            Text("Speed")
            Picker("Speed", selection: $player.rate) {
                ForEach(PlaybackSpeed.choices, id: \.self) { value in
                    Text(PlaybackSpeed.label(value)).tag(value)
                }
            }
            .pickerStyle(.segmented)
            .labelsHidden()
            .accessibilityLabel("Playback speed")
        }
        .padding(.vertical, Space.xxs)
        .sensoryFeedback(.selection, trigger: player.rate) { _, _ in hapticsOn }
    }
}

/// Downloads one surah for the current reciter, from the reader's settings.
struct SurahRecitationDownloadRow: View {
    let surahID: Int

    @Environment(QuranStore.self) private var quran
    @Environment(AudioPlayer.self) private var player
    @Environment(RecitationDownloads.self) private var downloads
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @AppStorage(PrefKey.haptics) private var hapticsOn = true

    var body: some View {
        let reciterID = player.reciterID
        let state = downloads.state(reciterID: reciterID, surah: surahID)
        Button {
            switch state {
            case .downloading: downloads.cancel(reciterID: reciterID, surah: surahID)
            case .ready: break
            case .notDownloaded, .failed: downloads.download(reciterID: reciterID, surah: surahID)
            }
        } label: {
            HStack {
                Text("Download \(quran.surah(surahID).name)")
                    .foregroundStyle(Palette.textPrimary)
                Spacer()
                Group {
                    switch state {
                    case .ready:
                        Image(systemName: "checkmark.circle.fill")
                            .foregroundStyle(Palette.accent)
                    case .downloading:
                        DownloadRing(progress: downloads.progress(reciterID: reciterID, surah: surahID))
                            .frame(width: 20, height: 20)
                    case .failed:
                        Text("Retry")
                            .foregroundStyle(Palette.accent)
                    case .notDownloaded:
                        Image(systemName: "arrow.down.circle")
                            .font(.title3)
                            .foregroundStyle(Palette.accent)
                    }
                }
                .transition(reduceMotion ? .opacity : .scale(scale: 0.5).combined(with: .opacity))
                .overlay(alignment: .topTrailing) {
                    PetalBurst(trigger: state == .ready, active: state == .ready)
                        .frame(width: 22, height: 22)
                }
            }
            .contentShape(Rectangle())
        }
        .animation(reduceMotion ? nil : Motion.reward, value: state)
        .sensoryFeedback(.success, trigger: state) { old, new in hapticsOn && old == .downloading && new == .ready }
        .accessibilityValue(accessibilityState(state))
    }

    private func accessibilityState(_ state: RecitationDownloads.State) -> String {
        switch state {
        case .ready: "Downloaded"
        case .downloading: "Downloading"
        case .failed: "Failed"
        case .notDownloaded: "Not downloaded"
        }
    }
}

// MARK: - Transliteration

/// The verse in Latin letters, set a little smaller than the translation, in italic.
struct TransliterationLine: View {
    let ref: VerseRef
    var size: CGFloat = 17

    @Environment(TransliterationStore.self) private var transliteration
    @Environment(\.inBook) private var inBook
    @ScaledMetric(relativeTo: .body) private var scale: CGFloat = 1

    var body: some View {
        if let text = transliteration.text(ref) {
            Text(text)
                .font(.system(size: size * 0.92 * scale, design: .serif).italic())
                .foregroundStyle(inBook ? BookInk.caption : Palette.textSecondary)
                .lineSpacing(3)
                .multilineTextAlignment(.leading)
                .frame(maxWidth: .infinity, alignment: .leading)
                .fixedSize(horizontal: false, vertical: true)
                .environment(\.layoutDirection, .leftToRight)
        }
    }
}

/// The Transliteration switch for the reading settings. While it downloads a small spinner
/// shows under it, and if it could not download a Retry appears.
struct TransliterationToggleRows: View {
    @Environment(TransliterationStore.self) private var transliteration
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        @Bindable var transliteration = transliteration
        Toggle("Transliteration", isOn: $transliteration.show.animation(reduceMotion ? nil : Motion.gentle))
        if transliteration.show {
            switch transliteration.state {
            case .downloading:
                HStack(spacing: Space.xs) {
                    ProgressView()
                    Text("Downloading")
                        .foregroundStyle(Palette.textSecondary)
                }
                .font(.subheadline)
                .transition(.opacity)
            case .failed:
                HStack {
                    Text("Could not download")
                        .foregroundStyle(Palette.textSecondary)
                    Spacer()
                    Button("Retry") { Task { await transliteration.download() } }
                        .buttonStyle(.borderless)
                }
                .font(.subheadline)
                .transition(.opacity)
            case .ready, .notDownloaded:
                EmptyView()
            }
        }
    }
}

// MARK: - Downloads

/// The Quran transliteration in Downloads, kept like a translation.
struct TransliterationDownloadSection: View {
    @Environment(TransliterationStore.self) private var transliteration
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @AppStorage(PrefKey.haptics) private var hapticsOn = true

    private var pop: AnyTransition {
        reduceMotion ? .opacity : .scale(scale: 0.5).combined(with: .opacity)
    }

    var body: some View {
        let state = transliteration.state
        Section {
            HStack {
                Text("English transliteration")
                Spacer()
                Group {
                    switch state {
                    case .ready:
                        VStack(alignment: .trailing, spacing: 2) {
                            Image(systemName: "checkmark.circle.fill")
                                .foregroundStyle(Palette.accent)
                                .accessibilityLabel("Downloaded")
                            if let size = transliteration.sizeOnDisk() {
                                Text(ByteCountFormatter.string(fromByteCount: size, countStyle: .file))
                                    .font(.caption)
                                    .foregroundStyle(Palette.textSecondary)
                            }
                        }
                    case .downloading:
                        ProgressView()
                            .accessibilityLabel("Downloading")
                    case .failed:
                        Button("Retry") { Task { await transliteration.download() } }
                            .buttonStyle(.borderless)
                    case .notDownloaded:
                        Button {
                            Task { await transliteration.download() }
                        } label: {
                            Image(systemName: "arrow.down.circle")
                                .font(.title3)
                        }
                        .buttonStyle(.borderless)
                        .accessibilityLabel("Download transliteration")
                    }
                }
                .transition(pop)
                .overlay(alignment: .topTrailing) {
                    PetalBurst(trigger: state == .ready, active: state == .ready)
                        .frame(width: 22, height: 22)
                }
            }
            .animation(reduceMotion ? nil : Motion.reward, value: state)
            .sensoryFeedback(.success, trigger: state) { old, new in hapticsOn && old == .downloading && new == .ready }
            .dealIn(0, baseDelay: 0.4, step: 0.05)
            .swipeActions {
                if state == .ready {
                    Button("Delete", role: .destructive) { transliteration.delete() }
                }
            }
        } header: {
            Text("Transliteration")
        }
        .listRowBackground(Palette.surface)
    }
}

/// Surahs saved for offline listening, with their size. Swipe to delete one.
struct RecitationDownloadsSection: View {
    @Environment(QuranStore.self) private var quran
    @Environment(RecitationDownloads.self) private var downloads

    var body: some View {
        let items = downloads.downloaded
        if !items.isEmpty {
            Section {
                ForEach(Array(items.enumerated()), id: \.element.id) { index, item in
                    HStack {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(quran.surah(item.surah).name)
                            Text(Reciter.named(item.reciterID).name)
                                .font(.subheadline)
                                .foregroundStyle(Palette.textSecondary)
                        }
                        Spacer()
                        if let size = downloads.sizeOnDisk(reciterID: item.reciterID, surah: item.surah) {
                            Text(ByteCountFormatter.string(fromByteCount: size, countStyle: .file))
                                .font(.subheadline)
                                .foregroundStyle(Palette.textSecondary)
                        }
                    }
                    .accessibilityElement(children: .combine)
                    .dealIn(min(index, 8), baseDelay: 0.45, step: 0.05)
                    .swipeActions {
                        Button("Delete", role: .destructive) {
                            downloads.delete(reciterID: item.reciterID, surah: item.surah)
                        }
                    }
                }
            } header: {
                Text("Recitations")
            }
            .listRowBackground(Palette.surface)
        }
    }
}
