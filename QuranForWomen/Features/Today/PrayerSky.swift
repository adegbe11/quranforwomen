import SwiftUI
import UIKit

// MARK: - Sky model

/// A color as plain numbers, so skies can blend smoothly between prayer times.
struct SkyRGB {
    var r: Double, g: Double, b: Double

    init(_ hex: UInt32) {
        r = Double((hex >> 16) & 0xFF) / 255
        g = Double((hex >> 8) & 0xFF) / 255
        b = Double(hex & 0xFF) / 255
    }

    init(r: Double, g: Double, b: Double) { self.r = r; self.g = g; self.b = b }

    func mixed(with other: SkyRGB, _ t: Double) -> SkyRGB {
        SkyRGB(r: r + (other.r - r) * t, g: g + (other.g - g) * t, b: b + (other.b - b) * t)
    }

    var color: Color { Color(red: r, green: g, blue: b) }
}

/// Top, middle and horizon colors of the sky at one moment.
struct SkyPalette {
    var top: SkyRGB, middle: SkyRGB, horizon: SkyRGB
    /// How dark the sky is, 0 to 1. Above 0.5, text turns white.
    var darkness: Double

    func mixed(with other: SkyPalette, _ t: Double) -> SkyPalette {
        SkyPalette(
            top: top.mixed(with: other.top, t),
            middle: middle.mixed(with: other.middle, t),
            horizon: horizon.mixed(with: other.horizon, t),
            darkness: darkness + (other.darkness - darkness) * t
        )
    }

    static let night = SkyPalette(top: SkyRGB(0x071321), middle: SkyRGB(0x0F2236), horizon: SkyRGB(0x1D3550), darkness: 1)
    static let fajr = SkyPalette(top: SkyRGB(0x16223F), middle: SkyRGB(0x4B4F7E), horizon: SkyRGB(0xE3A3B4), darkness: 0.85)
    static let sunrise = SkyPalette(top: SkyRGB(0x8FC3DF), middle: SkyRGB(0xE9D3C9), horizon: SkyRGB(0xF8D7A8), darkness: 0.2)
    static let dhuhr = SkyPalette(top: SkyRGB(0x7DBEDF), middle: SkyRGB(0xB9DDEA), horizon: SkyRGB(0xF4F1E6), darkness: 0.1)
    static let asr = SkyPalette(top: SkyRGB(0x9CC4D8), middle: SkyRGB(0xF0DDC0), horizon: SkyRGB(0xF5DCE3), darkness: 0.15)
    static let maghrib = SkyPalette(top: SkyRGB(0x2A3460), middle: SkyRGB(0xB86A87), horizon: SkyRGB(0xF2A779), darkness: 0.75)
}

/// One prayer time on the sky arc, at its place from 0 to 1.
struct SkyMark: Hashable {
    let name: String
    let at: Double
}

/// Where the sky is for a given moment and prayer schedule.
struct SkyState {
    let palette: SkyPalette
    /// Position of the sun (day) or moon (night) along its arc, 0 to 1.
    let progress: Double
    let isNight: Bool
    /// Prayer times along the current arc, so the sky reads as a prayer clock. Empty without a location.
    let marks: [SkyMark]

    var isDark: Bool { palette.darkness > 0.5 }

    init(date: Date, schedule: PrayerSchedule?) {
        let calendar = Calendar.current
        let day = calendar.startOfDay(for: date)
        func time(_ p: Prayer, _ fallbackHour: Double) -> Date {
            schedule?.time(p) ?? day.addingTimeInterval(fallbackHour * 3600)
        }
        let fajr = time(.fajr, 5), sunrise = time(.sunrise, 6.5), dhuhr = time(.dhuhr, 12.5)
        let asr = time(.asr, 15.75), maghrib = time(.maghrib, 18.5), isha = time(.isha, 20)

        // Anchor colors at each prayer time and blend between them.
        let anchors: [(Date, SkyPalette)] = [
            (fajr.addingTimeInterval(-40 * 60), .night),
            (fajr, .fajr),
            (sunrise.addingTimeInterval(20 * 60), .sunrise),
            (dhuhr, .dhuhr),
            (asr, .asr),
            (maghrib.addingTimeInterval(-25 * 60), .asr.mixed(with: .maghrib, 0.4)),
            (maghrib, .maghrib),
            (isha, .night),
        ]
        var palette = SkyPalette.night
        if date >= anchors[0].0 && date < anchors[anchors.count - 1].0 {
            for i in 0..<(anchors.count - 1) where date >= anchors[i].0 && date < anchors[i + 1].0 {
                let span = anchors[i + 1].0.timeIntervalSince(anchors[i].0)
                let raw = span > 0 ? date.timeIntervalSince(anchors[i].0) / span : 0
                let eased = raw * raw * (3 - 2 * raw)
                palette = anchors[i].1.mixed(with: anchors[i + 1].1, eased)
            }
        }
        self.palette = palette

        func along(_ at: Date, _ start: Date, _ end: Date) -> Double {
            at.timeIntervalSince(start) / end.timeIntervalSince(start)
        }
        let hasSchedule = schedule != nil
        if date >= sunrise && date < maghrib {
            isNight = false
            progress = along(date, sunrise, maghrib)
            marks = hasSchedule ? [
                SkyMark(name: "Sunrise", at: 0),
                SkyMark(name: "Dhuhr", at: along(dhuhr, sunrise, maghrib)),
                SkyMark(name: "Asr", at: along(asr, sunrise, maghrib)),
                SkyMark(name: "Maghrib", at: 1),
            ] : []
        } else {
            isNight = true
            let start = date >= maghrib ? maghrib : maghrib.addingTimeInterval(-24 * 3600)
            let end = date >= maghrib ? fajr.addingTimeInterval(24 * 3600) : fajr
            let ishaAt = date >= maghrib ? isha : isha.addingTimeInterval(-24 * 3600)
            progress = min(max(along(date, start, end), 0), 1)
            marks = hasSchedule ? [
                SkyMark(name: "Maghrib", at: 0),
                SkyMark(name: "Isha", at: along(ishaAt, start, end)),
                SkyMark(name: "Fajr", at: 1),
            ] : []
        }
    }
}

// MARK: - Sky header

/// The top of Today: a sky that matches the real time of day, the sun or moon
/// moving along its arc, and the five prayers she can tick.
struct PrayerSkyHeader: View {
    var height: CGFloat = 430

    @Environment(PrayerStore.self) private var prayer
    @Environment(LibraryStore.self) private var library
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @AppStorage(PrefKey.haptics) private var hapticsOn = true
    @State private var denied = false
    /// The sun or moon starts on the horizon and rises to its real place on first appearance.
    @State private var risen = false

    var body: some View {
        TimelineView(.periodic(from: .now, by: 60)) { context in
            let schedule = prayer.schedule(for: context.date)
            let sky = SkyState(date: context.date, schedule: schedule)
            let ink: Color = sky.isDark ? .white : Palette.ink

            ZStack(alignment: .top) {
                SkyBackground(palette: sky.palette, isNight: sky.isNight)
                CelestialArc(progress: risen || reduceMotion ? sky.progress : 0, isNight: sky.isNight, ink: ink, marks: sky.marks)
                    .padding(.top, 104)
                    .frame(height: 304)
                    .accessibilityHidden(true)

                VStack(alignment: .leading, spacing: 0) {
                    Text(Self.dateLine(context.date))
                        .font(.subheadline)
                        .foregroundStyle(ink.opacity(0.75))
                        .focusIn(delay: 0.1)
                    Text("Assalamu Alaikum")
                        .font(.system(.largeTitle, design: .serif, weight: .semibold))
                        .foregroundStyle(ink)
                        .padding(.top, 4)
                        .accessibilityAddTraits(.isHeader)
                        .focusIn(delay: 0.2)
                    nextPrayerChip(at: context.date, ink: ink)
                        .padding(.top, Space.s)
                        .focusIn(delay: 0.45)

                    Spacer(minLength: 0)

                    if library.isResting {
                        Text("Rest days. Prayer is paused.")
                            .font(.subheadline)
                            .foregroundStyle(ink.opacity(0.85))
                            .padding(.bottom, 40)
                    } else {
                        PrayerDots(schedule: schedule, ink: ink)
                            .padding(.bottom, 44)
                    }
                }
                .padding(.horizontal, Space.page)
                .padding(.top, 64)
            }
            .frame(height: height)
            .animation(reduceMotion ? nil : .smooth(duration: 1.2), value: sky.palette.darkness)
        }
        .onAppear {
            guard !risen else { return }
            withAnimation(.spring(response: 1.4, dampingFraction: 0.9).delay(0.15)) { risen = true }
        }
    }

    @ViewBuilder
    private func nextPrayerChip(at date: Date, ink: Color) -> some View {
        if !prayer.hasLocation {
            Button {
                prayer.requestLocation()
            } label: {
                Label(prayer.isLocating ? "Finding your location" : "Set up prayer times", systemImage: "location")
                    .font(.subheadline.weight(.semibold))
                    .symbolEffect(.pulse, options: .repeating, isActive: prayer.isLocating && !reduceMotion)
                    .padding(.horizontal, Space.m)
                    .frame(minHeight: 36)
            }
            .skyGlass(ink: ink)
        } else if !prayer.notifyAtPrayerTimes && !library.isResting {
            Button {
                Task { denied = !(await prayer.enableNotifications()) }
            } label: {
                Label(denied ? "Allow notifications in Settings" : "Remind me at each prayer", systemImage: "bell.badge")
                    .font(.subheadline.weight(.semibold))
                    .padding(.horizontal, Space.m)
                    .frame(minHeight: 36)
            }
            .skyGlass(ink: ink)
        } else if let next = prayer.nextPrayer(after: date), !library.isResting {
            // The next prayer opens Prayer Times, with rest days, the Qibla and prayer settings.
            NavigationLink(value: Route.prayerTimes) {
                Label("\(next.0.name) in \(PrayerCard.relative(from: date, to: next.1))", systemImage: "clock")
                    .font(.subheadline.weight(.semibold).monospacedDigit())
                    .contentTransition(.numericText(countsDown: true))
                    .animation(.snappy, value: PrayerCard.relative(from: date, to: next.1))
                    .padding(.horizontal, Space.m)
                    .frame(minHeight: 36)
                    .skyGlass(ink: ink)
            }
            .buttonStyle(PressableStyle(scale: 0.96))
            .accessibilityLabel("Next prayer: \(next.0.name) at \(next.1.formatted(date: .omitted, time: .shortened))")
            .accessibilityHint("Opens prayer times")
        }
    }

    static func dateLine(_ date: Date) -> String {
        let hijri = DateFormatter()
        hijri.calendar = Calendar(identifier: .islamicUmmAlQura)
        hijri.locale = Locale(identifier: "en")
        hijri.dateFormat = "d MMMM y"
        return "\(date.formatted(.dateTime.weekday(.wide))) · \(hijri.string(from: date))"
    }
}

// MARK: - Background

struct SkyBackground: View {
    let palette: SkyPalette
    let isNight: Bool

    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        ZStack {
            gradient
            // A soft glow where the light meets the horizon.
            RadialGradient(
                colors: [palette.horizon.color.opacity(0.9), palette.horizon.color.opacity(0)],
                center: UnitPoint(x: 0.5, y: 1.05),
                startRadius: 0,
                endRadius: 320
            )
            if palette.darkness > 0.6 {
                StarField(twinkles: !reduceMotion)
                    .opacity((palette.darkness - 0.6) / 0.4)
                    .mask {
                        LinearGradient(colors: [.white, .white.opacity(0)], startPoint: .top, endPoint: .bottom)
                    }
            }
        }
        .ignoresSafeArea(edges: .top)
    }

    @ViewBuilder
    private var gradient: some View {
        if #available(iOS 18.0, *) {
            MeshGradient(
                width: 3, height: 3,
                points: [
                    [0, 0], [0.5, 0], [1, 0],
                    [0, 0.55], [0.55, 0.5], [1, 0.6],
                    [0, 1], [0.5, 1], [1, 1],
                ],
                colors: [
                    palette.top.color, palette.top.color, palette.top.color.opacity(0.95),
                    palette.middle.color, palette.middle.color, palette.middle.color,
                    palette.horizon.color, palette.horizon.color, palette.horizon.color,
                ]
            )
        } else {
            LinearGradient(
                colors: [palette.top.color, palette.middle.color, palette.horizon.color],
                startPoint: .top, endPoint: .bottom
            )
        }
    }
}

/// Fixed star positions that twinkle slowly. Drawn once, very light on battery.
private struct StarField: View {
    let twinkles: Bool

    private static let stars: [(x: Double, y: Double, size: Double, phase: Double)] = {
        var generator = SeededGenerator(seed: 7)
        return (0..<55).map { _ in
            (Double.random(in: 0...1, using: &generator),
             Double.random(in: 0...0.75, using: &generator),
             Double.random(in: 0.8...2.2, using: &generator),
             Double.random(in: 0...(2 * .pi), using: &generator))
        }
    }()

    var body: some View {
        TimelineView(.animation(minimumInterval: 1.0 / 12, paused: !twinkles)) { context in
            let t = context.date.timeIntervalSinceReferenceDate
            Canvas { canvas, size in
                for star in Self.stars {
                    let glow = twinkles ? 0.55 + 0.45 * sin(t * 0.8 + star.phase) : 0.8
                    let rect = CGRect(x: star.x * size.width, y: star.y * size.height, width: star.size, height: star.size)
                    canvas.fill(Path(ellipseIn: rect), with: .color(.white.opacity(glow)))
                }
            }
        }
    }
}

/// A tiny repeatable random generator so the stars never jump between launches.
private struct SeededGenerator: RandomNumberGenerator {
    var state: UInt64
    init(seed: UInt64) { state = seed &* 0x9E3779B97F4A7C15 }
    mutating func next() -> UInt64 {
        state &+= 0x9E3779B97F4A7C15
        var z = state
        z = (z ^ (z >> 30)) &* 0xBF58476D1CE4E5B9
        z = (z ^ (z >> 27)) &* 0x94D049BB133111EB
        return z ^ (z >> 31)
    }
}

// MARK: - Arc

/// The path of the sun or moon across the sky, with the body at its real position.
struct CelestialArc: View {
    let progress: Double
    let isNight: Bool
    let ink: Color
    /// Prayer times along the arc. Each is a small dot; only the next one is named.
    var marks: [SkyMark] = []

    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        GeometryReader { geo in
            let w = geo.size.width, h = geo.size.height
            let start = CGPoint(x: 28, y: h * 0.62)
            let end = CGPoint(x: w - 28, y: h * 0.62)
            let control = CGPoint(x: w / 2, y: -h * 0.45)
            let along: (Double) -> CGPoint = { at in
                let k = CGFloat(min(max(at, 0), 1))
                let v = 1 - k
                return CGPoint(
                    x: v * v * start.x + 2 * v * k * control.x + k * k * end.x,
                    y: v * v * start.y + 2 * v * k * control.y + k * k * end.y
                )
            }
            let clamped = min(max(progress, 0), 1)
            let t = CGFloat(clamped)
            let point = along(clamped)
            // Only the next prayer is named, so the sky reads as "the sun is heading for Asr"
            // without crowding the greeting above it.
            let next = marks.first { $0.at > clamped + 0.01 }?.name

            ZStack {
                Path { path in
                    path.move(to: start)
                    path.addQuadCurve(to: end, control: control)
                }
                .stroke(ink.opacity(0.22), style: StrokeStyle(lineWidth: 1, dash: [2, 5]))

                ForEach(marks, id: \.name) { mark in
                    let m = along(mark.at)
                    Circle()
                        .fill(ink.opacity(0.55))
                        .frame(width: 5, height: 5)
                        .position(m)
                    Text(mark.name)
                        .font(.system(size: 10))
                        .foregroundStyle(ink.opacity(0.6))
                        .fixedSize()
                        .opacity(mark.name == next && mark.at - clamped > 0.08 ? 1 : 0)
                        .position(x: m.x, y: m.y + 12)
                }
                .animation(reduceMotion ? nil : .smooth(duration: 0.6), value: next)

                Group {
                    if isNight {
                        Image(systemName: "moon.fill")
                            .font(.system(size: 24))
                            .foregroundStyle(Color(hex: 0xF4EBCF))
                            .shadow(color: Color(hex: 0xF4EBCF).opacity(0.7), radius: 14)
                    } else {
                        Circle()
                            .fill(RadialGradient(colors: [Color(hex: 0xFFF6DA), Color(hex: 0xFFD98A)], center: .center, startRadius: 0, endRadius: 14))
                            .frame(width: 26, height: 26)
                            .shadow(color: Color(hex: 0xFFD98A).opacity(0.9), radius: 22)
                    }
                }
                .position(point)
                .animation(reduceMotion ? nil : .smooth(duration: 1.6), value: t)
            }
        }
    }
}

// MARK: - Prayer dots

private struct PrayerDots: View {
    let schedule: PrayerSchedule?
    let ink: Color

    @Environment(LibraryStore.self) private var library
    @AppStorage(PrefKey.haptics) private var hapticsOn = true

    private var doneCount: Int { Prayer.prayers.filter { library.prayed($0.rawValue) }.count }

    var body: some View {
        VStack(alignment: .leading, spacing: Space.s) {
            content
            if doneCount == 5 {
                Text("Alhamdulillah, all five today.")
                    .font(.footnote)
                    .foregroundStyle(ink.opacity(0.75))
                    .transition(.opacity)
            }
        }
        .animation(.smooth, value: doneCount == 5)
        .sensoryFeedback(.impact(flexibility: .soft, intensity: 0.7), trigger: doneCount) { old, new in
            hapticsOn && new > old && new < 5
        }
        .sensoryFeedback(.success, trigger: doneCount) { _, new in hapticsOn && new == 5 }
    }

    @ViewBuilder
    private var content: some View {
        if #available(iOS 26.0, *) {
            GlassEffectContainer(spacing: 8) { row }
        } else {
            row
        }
    }

    private var row: some View {
        HStack(spacing: 0) {
            ForEach(Array(Prayer.prayers.enumerated()), id: \.element) { index, item in
                PrayerDotButton(
                    prayer: item,
                    time: schedule?.time(item),
                    isDone: library.prayed(item.rawValue),
                    ink: ink
                ) {
                    library.togglePrayed(item.rawValue)
                }
                .dealIn(index, baseDelay: 0.4, step: 0.05)
            }
        }
    }
}

private struct PrayerDotButton: View {
    let prayer: Prayer
    let time: Date?
    let isDone: Bool
    let ink: Color
    let toggle: () -> Void

    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        Button(action: toggle) {
            VStack(spacing: 6) {
                ZStack {
                    Image(systemName: "checkmark")
                        .font(.system(size: 16, weight: .bold))
                        .foregroundStyle(.white)
                        .opacity(isDone ? 1 : 0)
                        .scaleEffect(isDone ? 1 : 0.4)
                        .symbolEffect(.bounce, value: reduceMotion ? false : isDone)
                }
                .frame(width: 46, height: 46)
                .dotSurface(isDone: isDone)
                // Pressed in, then a soft overshoot, like a bead settling.
                .keyframeAnimator(initialValue: 1.0, trigger: isDone) { [still = reduceMotion] content, scale in
                    content.scaleEffect(still ? 1 : scale)
                } keyframes: { _ in
                    SpringKeyframe(0.84, duration: 0.1)
                    SpringKeyframe(1.1, duration: 0.18, spring: .bouncy)
                    SpringKeyframe(1.0, duration: 0.32, spring: .smooth)
                }
                // A ring of light that opens outward when she marks it.
                .overlay {
                    Circle()
                        .stroke(Color.white.opacity(0.7), lineWidth: 1.5)
                        .phaseAnimator([0, 1, 2], trigger: isDone) { ring, phase in
                            ring
                                .scaleEffect(phase == 2 ? 1.7 : 1)
                                .opacity(isDone && !reduceMotion && phase == 1 ? 0.8 : 0)
                        } animation: { phase in
                            phase == 2 ? .easeOut(duration: 0.6) : .linear(duration: 0.01)
                        }
                        .allowsHitTesting(false)
                }
                .animation(.snappy(duration: 0.25), value: isDone)

                Text(prayer.name)
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(ink)
                if let time {
                    Text(time.formatted(date: .omitted, time: .shortened))
                        .font(.caption2.monospacedDigit())
                        .foregroundStyle(ink.opacity(0.7))
                        .contentTransition(.numericText())
                        .animation(reduceMotion ? nil : .snappy, value: time)
                }
            }
            .frame(maxWidth: .infinity)
            .contentShape(Rectangle())
        }
        .buttonStyle(PressableStyle(scale: 0.97))
        .accessibilityLabel(prayer.name)
        .accessibilityValue([time?.formatted(date: .omitted, time: .shortened), isDone ? "prayed" : "not yet"].compactMap { $0 }.joined(separator: ", "))
        .accessibilityHint(isDone ? "Double tap to unmark" : "Double tap to mark as prayed")
    }
}

// MARK: - Glass helpers

private extension View {
    /// Liquid Glass on iOS 26, a soft material before that.
    func skyGlass(ink: Color, interactive: Bool = true) -> some View {
        self.foregroundStyle(ink)
            .readableGlass(Capsule(), onDark: ink == .white, interactive: interactive)
    }

    func dotSurface(isDone: Bool) -> some View {
        self.readableGlass(Circle(), tint: isDone ? Palette.accentFill : nil)
    }
}
