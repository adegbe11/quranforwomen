import AVFoundation
import Foundation
import Observation
import Speech

// MARK: - Verse detection

/// Finds Quran references in free text: "2:255", "Surah Al-Baqarah verse 255",
/// "Baqarah ayah 255", "chapter 2 verse 255". Only returns verses that exist.
enum VerseDetector {
    @MainActor
    static func find(in text: String, quran: QuranStore, limit: Int = 12) -> [VerseRef] {
        var found: [VerseRef] = []
        func add(_ ref: VerseRef) {
            if quran.contains(ref) && !found.contains(ref) { found.append(ref) }
        }

        // 2:255
        for match in text.matches(of: #/\b(\d{1,3})\s*:\s*(\d{1,3})\b(?!\s*(?:am|pm|a\.m|p\.m))/#.ignoresCase()) {
            if let s = Int(match.1), let a = Int(match.2) { add(VerseRef(surah: s, ayah: a)) }
        }

        // chapter 2 verse 255
        for match in text.matches(of: #/(?:chapter|surah|sura)\s+(\d{1,3}),?\s+(?:verse|ayah|ayat|aya)\s+(\d{1,3})/#.ignoresCase()) {
            if let s = Int(match.1), let a = Int(match.2) { add(VerseRef(surah: s, ayah: a)) }
        }

        // Surah Al-Baqarah verse 255, Baqarah ayah 255
        let folded = fold(text)
        let names = quran.surahs.map { (id: $0.id, key: fold($0.name).replacingOccurrences(of: "al", with: "", options: .anchored)) }
        for match in folded.matches(of: #/([a-z]+(?: [a-z]+)?) (?:verse|ayah|ayat|aya) (\d{1,3})/#) {
            let words = String(match.1).replacingOccurrences(of: " ", with: "")
            guard let a = Int(match.2) else { continue }
            let candidate = words.hasPrefix("al") ? String(words.dropFirst(2)) : words
            if let surah = names.first(where: { !$0.key.isEmpty && (candidate.hasSuffix($0.key) || $0.key == candidate) }) {
                add(VerseRef(surah: surah.id, ayah: a))
            }
        }
        return Array(found.prefix(limit))
    }

    /// Lowercase letters, digits and single spaces only.
    private static func fold(_ text: String) -> String {
        let lowered = text.folding(options: [.caseInsensitive, .diacriticInsensitive], locale: nil)
        var out = ""
        var lastWasSpace = false
        for ch in lowered {
            if ch.isLetter || ch.isNumber {
                if ch == "'" { continue }
                out.append(ch)
                lastWasSpace = false
            } else if ch == "-" || ch == "'" || ch == "\u{2019}" {
                continue
            } else if !lastWasSpace {
                out.append(" ")
                lastWasSpace = true
            }
        }
        return out
    }
}

// MARK: - Arranging a talk into notes

/// Picks the speaker's own sentences. It never rewrites or summarizes in new words.
enum LessonArranger {
    struct Result {
        var title: String
        var points: [String]
        var quotes: [String]
        var takeaway: String
    }

    private static let stopWords: Set<String> = [
        "the", "and", "that", "this", "with", "for", "you", "your", "are", "was", "were", "have", "has", "had",
        "but", "not", "they", "them", "their", "what", "when", "will", "would", "there", "from", "about",
        "which", "who", "his", "her", "him", "she", "our", "out", "can", "all", "just", "like", "one", "because",
        "into", "then", "than", "also", "some", "very", "more", "know", "said", "says", "say", "going", "really",
        "people", "thing", "things", "said", "even", "only", "want", "get", "him", "how", "why", "any",
    ]

    private static let pointCues = [
        "the first", "the second", "the third", "number one", "number two", "the point is", "the lesson",
        "remember", "never forget", "the key", "most important", "allah tells us", "allah says",
        "the prophet", "we learn", "this teaches",
    ]

    private static let quoteCues = ["allah says", "allah tells us", "the prophet said", "he said", "she said", "narrated"]

    private static let actionCues = [
        "this week", "go home", "tonight", "tomorrow", "starting today", "try to", "make sure", "let us",
        "let's", "i want you to", "make dua", "ask yourselves", "ask yourself", "start",
    ]

    static func arrange(_ transcript: String, verses: [VerseRef], surahName: (Int) -> String) -> Result {
        let sentences = split(transcript)
        guard !sentences.isEmpty else { return Result(title: "", points: [], quotes: [], takeaway: "") }

        // How often each meaningful word appears across the whole talk.
        var frequency: [String: Int] = [:]
        for word in sentences.flatMap(words) where word.count > 3 && !stopWords.contains(word) {
            frequency[word, default: 0] += 1
        }

        func score(_ sentence: String) -> Double {
            let lower = sentence.lowercased()
            let ws = words(sentence)
            let themeScore = ws.reduce(0.0) { $0 + Double(frequency[$1] ?? 0) } / Double(max(ws.count, 1))
            let cueScore = pointCues.contains { lower.contains($0) } ? 3.0 : 0
            return themeScore + cueScore
        }

        let usable = sentences.enumerated().filter { (8...45).contains(words($0.element).count) }
        let ranked = usable.sorted { score($0.element) > score($1.element) }

        let pointIndexes = Set(ranked.prefix(4).map { $0.offset })
        let points = sentences.enumerated().filter { pointIndexes.contains($0.offset) }.map { $0.element }

        var quotes = usable.filter { item in quoteCues.contains { item.element.lowercased().contains($0) } }
            .map { $0.element }.filter { !points.contains($0) }
        if quotes.count < 2 {
            quotes += ranked.dropFirst(4).map { $0.element }.filter { !quotes.contains($0) && !points.contains($0) }
        }
        quotes = Array(quotes.prefix(2))

        let lastFifth = sentences.count * 4 / 5
        let takeaway = sentences.enumerated().reversed().first { item in
            actionCues.contains { item.element.lowercased().contains($0) } && words(item.element).count >= 5
        }?.element ?? sentences.dropFirst(lastFifth).max { score($0) < score($1) } ?? ""

        let themes = frequency.sorted { $0.value > $1.value }.prefix(2).map { $0.key.capitalized }
        let title: String
        if let verse = verses.first {
            title = "Reflections on \(surahName(verse.surah))"
        } else if themes.count == 2 {
            title = "\(themes[0]) and \(themes[1])"
        } else {
            title = ""
        }
        return Result(title: title, points: points, quotes: quotes, takeaway: takeaway)
    }

    private static func split(_ text: String) -> [String] {
        var result: [String] = []
        text.enumerateSubstrings(in: text.startIndex..., options: .bySentences) { sub, _, _, _ in
            if let s = sub?.trimmingCharacters(in: .whitespacesAndNewlines), !s.isEmpty { result.append(s) }
        }
        return result
    }

    private static func words(_ sentence: String) -> [String] {
        sentence.lowercased().split { !$0.isLetter && $0 != "'" }.map(String.init)
    }
}

// MARK: - Recorder

/// Live, on-device transcription of a khutbah or class. Audio is not saved.
@Observable
@MainActor
final class LessonRecorder {
    enum State: Equatable { case idle, needsPermission, denied, recording, unavailable }

    private(set) var state: State = .idle
    private(set) var transcript = ""
    private(set) var startedAt: Date?

    @ObservationIgnored private let engine = AVAudioEngine()
    @ObservationIgnored private let sink = BufferSink()
    @ObservationIgnored private var recognizer: SFSpeechRecognizer?
    @ObservationIgnored private var request: SFSpeechAudioBufferRecognitionRequest?
    @ObservationIgnored private var task: SFSpeechRecognitionTask?
    /// Text from earlier recognition segments. Recognition restarts quietly for long talks.
    @ObservationIgnored private var committed = ""
    /// Segments that ended with no new words, in a row. Stops a silent restart loop.
    @ObservationIgnored private var emptyRestarts = 0

    var elapsedMinutes: Int {
        guard let startedAt else { return 0 }
        return max(1, Int(Date.now.timeIntervalSince(startedAt) / 60))
    }

    func start() async {
        let speech = await Self.requestSpeechAuthorization()
        let mic = await AVAudioApplication.requestRecordPermission()
        guard speech == .authorized, mic else {
            state = .denied
            return
        }
        let recognizer = SFSpeechRecognizer(locale: Locale.current) ?? SFSpeechRecognizer(locale: Locale(identifier: "en-US"))
        guard let recognizer, recognizer.isAvailable else {
            state = .unavailable
            return
        }
        self.recognizer = recognizer
        transcript = ""
        committed = ""
        emptyRestarts = 0
        startedAt = .now
        do {
            let session = AVAudioSession.sharedInstance()
            try session.setCategory(.record, mode: .measurement)
            try session.setActive(true, options: .notifyOthersOnDeactivation)
            let input = engine.inputNode
            let format = input.outputFormat(forBus: 0)
            guard format.sampleRate > 0, format.channelCount > 0 else {
                state = .unavailable
                return
            }
            input.removeTap(onBus: 0)
            Self.installTap(on: input, format: format, sink: sink)
            engine.prepare()
            try engine.start()
            beginSegment()
            state = .recording
        } catch {
            stopEngine()
            state = .unavailable
        }
    }

    /// Stops and returns the full transcript.
    @discardableResult
    func stop() -> String {
        stopEngine()
        state = .idle
        return transcript
    }

    private func beginSegment() {
        guard let recognizer else { return }
        let request = SFSpeechAudioBufferRecognitionRequest()
        request.shouldReportPartialResults = true
        request.addsPunctuation = true
        if recognizer.supportsOnDeviceRecognition { request.requiresOnDeviceRecognition = true }
        self.request = request
        sink.request = request
        task = Self.startTask(recognizer: recognizer, request: request) { [weak self] text, done in
            Task { @MainActor in self?.handle(text: text, done: done) }
        }
    }

    private func handle(text: String?, done: Bool) {
        guard state == .recording else { return }
        if let text {
            transcript = committed.isEmpty ? text : committed + " " + text
        }
        if done {
            emptyRestarts = transcript == committed ? emptyRestarts + 1 : 0
            guard emptyRestarts < 3 else {
                // Recognition keeps failing straight away. Stop, and keep what we have.
                stopEngine()
                state = .unavailable
                return
            }
            // Keep listening: commit what we have and start a fresh segment.
            committed = transcript
            request?.endAudio()
            beginSegment()
        }
    }

    // These run on background audio and speech threads, so they stay off the main actor.

    nonisolated private static func requestSpeechAuthorization() async -> SFSpeechRecognizerAuthorizationStatus {
        await withCheckedContinuation { continuation in
            SFSpeechRecognizer.requestAuthorization { continuation.resume(returning: $0) }
        }
    }

    nonisolated private static func installTap(on input: AVAudioInputNode, format: AVAudioFormat, sink: BufferSink) {
        input.installTap(onBus: 0, bufferSize: 1024, format: format) { buffer, _ in
            sink.append(buffer)
        }
    }

    nonisolated private static func startTask(
        recognizer: SFSpeechRecognizer,
        request: SFSpeechAudioBufferRecognitionRequest,
        onUpdate: @escaping @Sendable (String?, Bool) -> Void
    ) -> SFSpeechRecognitionTask {
        recognizer.recognitionTask(with: request) { result, error in
            onUpdate(result?.bestTranscription.formattedString, (result?.isFinal ?? false) || error != nil)
        }
    }

    private func stopEngine() {
        if engine.isRunning { engine.stop() }
        engine.inputNode.removeTap(onBus: 0)
        request?.endAudio()
        task?.cancel()
        task = nil
        request = nil
        sink.request = nil
        try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
    }
}

/// Hands audio buffers from the audio thread to the current recognition request.
private final class BufferSink: @unchecked Sendable {
    private let lock = NSLock()
    private var _request: SFSpeechAudioBufferRecognitionRequest?

    var request: SFSpeechAudioBufferRecognitionRequest? {
        get { lock.lock(); defer { lock.unlock() }; return _request }
        set { lock.lock(); _request = newValue; lock.unlock() }
    }

    func append(_ buffer: AVAudioPCMBuffer) {
        request?.append(buffer)
    }
}
