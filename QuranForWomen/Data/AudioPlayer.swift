import AVFoundation
import MediaPlayer
import Observation

struct Reciter: Identifiable, Hashable {
    /// Edition identifier on the Islamic Network audio CDN.
    let id: String
    let name: String

    static let all: [Reciter] = [
        Reciter(id: "ar.alafasy", name: "Mishary Rashid Alafasy"),
        Reciter(id: "ar.husary", name: "Mahmoud Khalil Al-Husary"),
        Reciter(id: "ar.minshawi", name: "Mohamed Siddiq Al-Minshawi"),
    ]

    static func named(_ id: String) -> Reciter { all.first { $0.id == id } ?? all[0] }
}

/// Verse by verse recitation with repeat, follow along, and lock screen controls.
@Observable
@MainActor
final class AudioPlayer {
    enum RepeatMode: Hashable {
        case off
        case times(Int)
        case forever

        var title: String {
            switch self {
            case .off: "Off"
            case .times(let n): "\(n) times"
            case .forever: "Until I stop"
            }
        }
    }

    private(set) var current: VerseRef?
    private(set) var isPlaying = false
    private(set) var isLoading = false
    private(set) var didFail = false
    /// How far through the current verse the recitation is, 0 to 1.
    private(set) var itemProgress: Double = 0
    /// Last verse to play. nil means the end of the surah.
    private(set) var stopAfter: Int?
    var repeatMode: RepeatMode = .off {
        didSet { playsRemaining = Self.plays(for: repeatMode) }
    }
    var reciterID: String {
        didSet {
            UserDefaults.standard.set(reciterID, forKey: PrefKey.reciter)
            if let current { load(current, autoplay: isPlaying) }
        }
    }

    var reciter: Reciter { Reciter.named(reciterID) }

    /// Recitation speed, remembered.
    var rate: Float = PlaybackSpeed.saved {
        didSet {
            UserDefaults.standard.set(Double(rate), forKey: PlaybackPrefKey.speed)
            player.defaultRate = rate
            if player.timeControlStatus != .paused { player.rate = rate }
            updateNowPlaying()
        }
    }

    /// The sleep timer, if one is set.
    private(set) var sleepTimer: SleepTimer?
    /// When a timed sleep timer ends.
    private(set) var sleepEndsAt: Date?
    /// A range of verses that plays again from its start.
    private(set) var loopRange: ClosedRange<Int>?
    private(set) var loopMode: RepeatMode = .off

    @ObservationIgnored private var sleepTask: Task<Void, Never>?
    @ObservationIgnored private var loopsRemaining = 1

    @ObservationIgnored private let quran: QuranStore
    @ObservationIgnored private let player = AVPlayer()
    @ObservationIgnored private var playsRemaining = 1
    @ObservationIgnored private var observers: [NSObjectProtocol] = []
    @ObservationIgnored private var statusObservation: NSKeyValueObservation?
    @ObservationIgnored private var itemObservation: NSKeyValueObservation?
    @ObservationIgnored private var timeObserver: Any?

    init(quran: QuranStore) {
        self.quran = quran
        reciterID = UserDefaults.standard.string(forKey: PrefKey.reciter) ?? Reciter.all[0].id
        player.automaticallyWaitsToMinimizeStalling = true
        player.defaultRate = PlaybackSpeed.saved

        statusObservation = player.observe(\.timeControlStatus, options: [.initial, .new]) { [weak self] player, _ in
            let status = player.timeControlStatus
            Task { @MainActor in
                self?.isLoading = status == .waitingToPlayAtSpecifiedRate
                self?.isPlaying = status != .paused
            }
        }
        observers.append(NotificationCenter.default.addObserver(
            forName: .AVPlayerItemDidPlayToEndTime, object: nil, queue: .main
        ) { [weak self] note in
            let item = note.object as? AVPlayerItem
            Task { @MainActor in self?.itemFinished(item) }
        })
        observers.append(NotificationCenter.default.addObserver(
            forName: AVAudioSession.interruptionNotification, object: nil, queue: .main
        ) { [weak self] note in
            let type = (note.userInfo?[AVAudioSessionInterruptionTypeKey] as? UInt)
                .flatMap(AVAudioSession.InterruptionType.init(rawValue:))
            Task { @MainActor in
                if type == .began { self?.pause() }
            }
        })
        configureRemoteCommands()
        timeObserver = player.addPeriodicTimeObserver(
            forInterval: CMTime(seconds: 0.1, preferredTimescale: 600), queue: .main
        ) { [weak self] _ in
            Task { @MainActor in self?.updateProgress() }
        }
    }

    private func updateProgress() {
        guard let item = player.currentItem else { itemProgress = 0; return }
        let duration = item.duration.seconds
        let now = player.currentTime().seconds
        guard duration.isFinite, duration > 0, now.isFinite else { return }
        itemProgress = min(max(now / duration, 0), 1)
    }

    // MARK: Public controls

    /// Plays a verse. By default continues through the rest of the surah.
    func play(_ ref: VerseRef, through lastVerse: Int? = nil) {
        guard quran.contains(ref) else { return }
        loopRange = nil
        stopAfter = lastVerse
        playsRemaining = Self.plays(for: repeatMode)
        load(ref, autoplay: true)
    }

    /// Plays a range of whole verses, for example a dua, then stops.
    func play(range: Dua.VerseRange) {
        repeatMode = .off
        play(VerseRef(surah: range.surah, ayah: range.from), through: range.to)
    }

    /// Plays a single verse a number of times.
    func repeatVerse(_ ref: VerseRef, mode: RepeatMode) {
        repeatMode = mode
        play(ref, through: ref.ayah)
    }

    /// Plays verses from one to another within a surah, then the whole range again.
    func repeatRange(surah: Int, from: Int, to: Int, mode: RepeatMode) {
        let start = min(from, to)
        let end = max(from, to)
        repeatMode = .off
        play(VerseRef(surah: surah, ayah: start), through: end)
        guard current == VerseRef(surah: surah, ayah: start) else { return }
        loopRange = start...end
        loopMode = mode
        loopsRemaining = Self.plays(for: mode)
    }

    /// Stops repeating the range and carries on to the end of the surah.
    func clearLoop() {
        loopRange = nil
        stopAfter = nil
    }

    /// Sets or clears the sleep timer. A timed one fades the voice out over three seconds, then pauses.
    func setSleep(_ timer: SleepTimer?) {
        sleepTask?.cancel()
        player.volume = 1
        sleepTimer = timer
        sleepEndsAt = nil
        guard case .minutes(let minutes) = timer else { return }
        let ends = Date().addingTimeInterval(TimeInterval(minutes * 60))
        sleepEndsAt = ends
        sleepTask = Task { [weak self] in
            while !Task.isCancelled, Date() < ends {
                let left = max(0.05, min(1, ends.timeIntervalSinceNow))
                try? await Task.sleep(for: .seconds(left))
            }
            guard !Task.isCancelled else { return }
            await self?.fadeOutAndPause()
        }
    }

    private func fadeOutAndPause() async {
        for step in 1...30 {
            if Task.isCancelled {
                player.volume = 1
                return
            }
            player.volume = 1 - Float(step) / 30
            try? await Task.sleep(for: .milliseconds(100))
        }
        pause()
        player.volume = 1
        sleepTimer = nil
        sleepEndsAt = nil
    }

    func togglePlayPause() {
        if isPlaying { pause() } else { resume() }
    }

    func pause() {
        player.pause()
        updateNowPlaying()
    }

    func resume() {
        guard let current else { return }
        if player.currentItem == nil || didFail {
            load(current, autoplay: true)
        } else {
            activateSession()
            player.play()
        }
        updateNowPlaying()
    }

    func next() {
        guard let current else { return }
        let next = VerseRef(surah: current.surah, ayah: current.ayah + 1)
        guard quran.contains(next) else { return }
        playsRemaining = Self.plays(for: repeatMode)
        if let stopAfter, next.ayah > stopAfter { self.stopAfter = nil }
        if let loopRange, next.ayah > loopRange.upperBound { self.loopRange = nil }
        load(next, autoplay: true)
    }

    func previous() {
        guard let current else { return }
        let previous = VerseRef(surah: current.surah, ayah: current.ayah - 1)
        guard quran.contains(previous) else {
            player.seek(to: .zero)
            return
        }
        playsRemaining = Self.plays(for: repeatMode)
        load(previous, autoplay: true)
    }

    func stop() {
        player.pause()
        player.replaceCurrentItem(with: nil)
        current = nil
        stopAfter = nil
        didFail = false
        loopRange = nil
        sleepTask?.cancel()
        sleepTimer = nil
        sleepEndsAt = nil
        player.volume = 1
        MPNowPlayingInfoCenter.default().nowPlayingInfo = nil
        try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
    }

    func isCurrent(_ ref: VerseRef) -> Bool { current == ref }

    // MARK: Internals

    private static func plays(for mode: RepeatMode) -> Int {
        switch mode {
        case .off: 1
        case .times(let n): max(1, n)
        case .forever: .max
        }
    }

    private func audioURL(_ ref: VerseRef) -> URL? {
        // A downloaded surah plays from the iPhone.
        let local = RecitationDownloads.localURL(reciterID: reciterID, surah: ref.surah, ayah: ref.ayah)
        if FileManager.default.fileExists(atPath: local.path) { return local }
        return URL(string: "https://cdn.islamic.network/quran/audio/128/\(reciterID)/\(quran.globalNumber(ref)).mp3")
    }

    private func load(_ ref: VerseRef, autoplay: Bool) {
        guard let url = audioURL(ref) else { return }
        current = ref
        didFail = false
        itemProgress = 0
        let item = AVPlayerItem(url: url)
        // Keeps the voice natural when it plays faster or slower.
        item.audioTimePitchAlgorithm = .spectral
        player.defaultRate = rate
        itemObservation = item.observe(\.status, options: [.new]) { [weak self] item, _ in
            let failed = item.status == .failed
            Task { @MainActor in
                if failed {
                    self?.didFail = true
                    self?.isLoading = false
                }
            }
        }
        player.replaceCurrentItem(with: item)
        if autoplay {
            activateSession()
            player.play()
        }
        updateNowPlaying()
    }

    private func itemFinished(_ item: AVPlayerItem?) {
        guard let item, item === player.currentItem, let current else { return }
        let last = stopAfter ?? quran.surah(current.surah).verseCount
        // The sleep timer set to the end of the surah stops once it has played through.
        if sleepTimer == .endOfSurah, current.ayah >= last {
            stop()
            return
        }
        if playsRemaining > 1 {
            if playsRemaining != .max { playsRemaining -= 1 }
            player.seek(to: .zero)
            player.play()
            return
        }
        if current.ayah < last {
            playsRemaining = Self.plays(for: repeatMode)
            load(VerseRef(surah: current.surah, ayah: current.ayah + 1), autoplay: true)
        } else if let loopRange, loopsRemaining > 1 {
            if loopsRemaining != .max { loopsRemaining -= 1 }
            playsRemaining = Self.plays(for: repeatMode)
            load(VerseRef(surah: current.surah, ayah: loopRange.lowerBound), autoplay: true)
        } else {
            stop()
        }
    }

    private func activateSession() {
        let session = AVAudioSession.sharedInstance()
        try? session.setCategory(.playback, mode: .spokenAudio)
        try? session.setActive(true)
    }

    private func updateNowPlaying() {
        guard let current else { return }
        let surah = quran.surah(current.surah)
        MPNowPlayingInfoCenter.default().nowPlayingInfo = [
            MPMediaItemPropertyTitle: "\(surah.name) · Verse \(current.ayah)",
            MPMediaItemPropertyArtist: reciter.name,
            MPMediaItemPropertyAlbumTitle: "Quran for Women",
            MPNowPlayingInfoPropertyPlaybackRate: isPlaying ? Double(rate) : 0.0,
        ]
    }

    private func configureRemoteCommands() {
        let center = MPRemoteCommandCenter.shared()
        center.playCommand.addTarget { [weak self] _ in
            Task { @MainActor in self?.resume() }
            return .success
        }
        center.pauseCommand.addTarget { [weak self] _ in
            Task { @MainActor in self?.pause() }
            return .success
        }
        center.togglePlayPauseCommand.addTarget { [weak self] _ in
            Task { @MainActor in self?.togglePlayPause() }
            return .success
        }
        center.nextTrackCommand.addTarget { [weak self] _ in
            Task { @MainActor in self?.next() }
            return .success
        }
        center.previousTrackCommand.addTarget { [weak self] _ in
            Task { @MainActor in self?.previous() }
            return .success
        }
    }
}
