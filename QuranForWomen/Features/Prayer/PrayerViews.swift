import AVFoundation
import AudioToolbox
import SwiftUI

// MARK: - Today card

/// Next prayer on Today, or a quiet invitation to set up prayer times.
struct PrayerCard: View {
    @Environment(PrayerStore.self) private var prayer
    @Environment(LibraryStore.self) private var library

    var body: some View {
        NavigationLink(value: Route.prayerTimes) {
            TimelineView(.everyMinute) { context in
                HStack(spacing: Space.m) {
                    RowIcon(systemImage: library.isResting ? "moon.zzz" : "clock")
                    VStack(alignment: .leading, spacing: 2) {
                        if library.isResting {
                            Text("Rest days")
                                .font(.headline)
                                .foregroundStyle(Palette.textPrimary)
                        } else if let upcoming = prayer.nextPrayer(after: context.date) {
                            Text("\(upcoming.0.name) at \(upcoming.1.formatted(date: .omitted, time: .shortened))")
                                .font(.headline)
                                .foregroundStyle(Palette.textPrimary)
                            Text("In \(Self.relative(from: context.date, to: upcoming.1))\(prayer.placeName.map { " · \($0)" } ?? "")")
                                .font(.subheadline.monospacedDigit())
                                .foregroundStyle(Palette.textSecondary)
                                .contentTransition(.numericText(countsDown: true))
                                .animation(.snappy, value: Self.relative(from: context.date, to: upcoming.1))
                        } else {
                            Text("Prayer times")
                                .font(.headline)
                                .foregroundStyle(Palette.textPrimary)
                        }
                    }
                    Spacer()
                    Image(systemName: "chevron.right")
                        .font(.footnote.weight(.semibold))
                        .foregroundStyle(Palette.textSecondary)
                }
                .cardSurface()
            }
        }
        .buttonStyle(.pressable)
    }

    static func relative(from: Date, to: Date) -> String {
        let minutes = max(0, Int(to.timeIntervalSince(from) / 60))
        if minutes < 60 { return minutes <= 1 ? "a minute" : "\(minutes) minutes" }
        let h = minutes / 60, m = minutes % 60
        let hours = h == 1 ? "1 hour" : "\(h) hours"
        return m == 0 ? hours : "\(hours) \(m) min"
    }
}

// MARK: - Prayer times

/// Today's prayers on a thread under a live sky. The sky matches the real hour,
/// the countdown ticks to the next prayer, and each prayer she marks answers her.
struct PrayerTimesView: View {
    @Environment(PrayerStore.self) private var prayer
    @Environment(LibraryStore.self) private var library
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @AppStorage(PrefKey.haptics) private var hapticsOn = true

    @State private var day = Date.now
    /// Which way the day slides when she changes it.
    @State private var forward = true
    @State private var markTick = 0
    @State private var allFiveTick = 0

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: Space.xl) {
                PrayerHero()
                    .dealIn(0, baseDelay: 0.02)
                    .scrollSettle()

                if prayer.hasLocation, let schedule = prayer.schedule(for: day) {
                    daySection(schedule)
                        .scrollSettle()

                    GroupedSection {
                        NavigationLink(value: Route.qibla) {
                            ListRow(symbol: "location.north.line", title: "Qibla direction")
                        }
                        RowDivider()
                        NavigationLink(value: Route.prayerSettings) {
                            ListRow(symbol: "slider.horizontal.3", title: "Prayer settings",
                                    value: prayer.notifyAtPrayerTimes ? "Reminders on" : nil)
                        }
                        RowDivider()
                        NavigationLink(value: Route.islamicCalendar) {
                            ListRow(symbol: "calendar", title: "Islamic calendar")
                        }
                        RowDivider()
                        NavigationLink(value: Route.missedPrayers) {
                            ListRow(symbol: "arrow.counterclockwise", title: "Make-up prayers")
                        }
                    }
                    .dealIn(1, baseDelay: 0.45)
                    .scrollSettle()
                } else {
                    LocationSetup()
                        .padding(Space.m)
                        .background(RoundedRectangle(cornerRadius: Radius.tile, style: .continuous).fill(Palette.surface))
                        .dealIn(1, baseDelay: 0.25)
                        .scrollSettle()
                }

                restDays
                    .dealIn(2, baseDelay: 0.45)
                    .scrollSettle()
            }
            .padding(.horizontal, Space.page)
            .padding(.vertical, Space.s)
        }
        .pageBackground()
        .navigationTitle("Prayer Times")
        .navigationBarTitleDisplayMode(.inline)
        .sensoryFeedback(.impact(flexibility: .soft, intensity: 0.7), trigger: markTick) { _, _ in hapticsOn }
        .sensoryFeedback(.success, trigger: allFiveTick) { _, _ in hapticsOn }
    }

    // MARK: Day

    private var isToday: Bool { Calendar.current.isDateInToday(day) }

    private var dayTitle: String {
        let calendar = Calendar.current
        if calendar.isDateInToday(day) { return "Today" }
        if calendar.isDateInYesterday(day) { return "Yesterday" }
        if calendar.isDateInTomorrow(day) { return "Tomorrow" }
        return day.formatted(.dateTime.weekday(.wide).day().month())
    }

    private var dayKey: Int { Calendar.current.ordinality(of: .day, in: .era, for: day) ?? 0 }

    private var slide: AnyTransition {
        reduceMotion ? .opacity : .push(from: forward ? .trailing : .leading).combined(with: .opacity)
    }

    private func daySection(_ schedule: PrayerSchedule) -> some View {
        let canMark = !library.isResting && day <= .now
        let prayedCount = Prayer.prayers.filter { library.prayed($0.rawValue, on: day) }.count
        let next = isToday ? schedule.next()?.0 : nil
        let items = Prayer.allCases

        return VStack(alignment: .leading, spacing: Space.s) {
            HStack {
                Button { shift(-1) } label: {
                    Image(systemName: "chevron.left").frame(width: 44, height: 44)
                }
                .accessibilityLabel("Previous day")
                Spacer()
                ZStack {
                    Text(dayTitle)
                        .font(.headline)
                        .id(dayKey)
                        .transition(slide)
                }
                .clipped()
                Spacer()
                Button { shift(1) } label: {
                    Image(systemName: "chevron.right").frame(width: 44, height: 44)
                }
                .accessibilityLabel("Next day")
            }
            .foregroundStyle(Palette.accent)
            .buttonStyle(PressableStyle(scale: 0.9))
            .focusIn(delay: 0.15)

            ZStack {
                VStack(spacing: 0) {
                    ForEach(Array(items.enumerated()), id: \.element) { index, item in
                        let time = schedule.time(item)
                        PrayerTimelineRow(
                            prayer: item,
                            time: time,
                            state: nodeState(item, time: time, next: next),
                            isFirst: index == 0,
                            isLast: index == items.count - 1,
                            isPrayed: library.prayed(item.rawValue, on: day),
                            showsCheck: item.isPrayer && canMark,
                            index: index
                        ) {
                            mark(item)
                        }
                    }
                    if canMark && prayedCount == 5 {
                        AllFiveBanner()
                            .transition(.move(edge: .bottom).combined(with: .opacity))
                    }
                }
                .id(dayKey)
                .transition(slide)
            }
            .background(RoundedRectangle(cornerRadius: Radius.tile, style: .continuous).fill(Palette.surface))
            .clipShape(RoundedRectangle(cornerRadius: Radius.tile, style: .continuous))
            .animation(reduceMotion ? nil : Motion.reward, value: prayedCount == 5)

            HStack {
                Text("\(prayer.placeName ?? "Your location") · \(prayer.calculationMethod.title)")
                    .lineLimit(1)
                Spacer()
                if canMark {
                    Text("\(prayedCount) of 5 prayed")
                        .monospacedDigit()
                        .contentTransition(.numericText(value: Double(prayedCount)))
                        .animation(.snappy, value: prayedCount)
                }
            }
            .font(.footnote)
            .foregroundStyle(Palette.textSecondary)
            .padding(.horizontal, Space.m)
        }
        .contentShape(Rectangle())
        // Swipe the card to move between days. Vertical scrolling is left alone.
        .simultaneousGesture(
            DragGesture(minimumDistance: 30)
                .onEnded { value in
                    guard abs(value.translation.width) > abs(value.translation.height) * 1.5 else { return }
                    shift(value.translation.width < 0 ? 1 : -1)
                }
        )
        .sensoryFeedback(.selection, trigger: dayKey) { _, _ in hapticsOn }
    }

    private func nodeState(_ item: Prayer, time: Date?, next: Prayer?) -> NodeState {
        if item == next { return .next }
        if let time, time <= .now { return .past }
        return .upcoming
    }

    private func shift(_ days: Int) {
        forward = days > 0
        let target = Calendar.current.date(byAdding: .day, value: days, to: day) ?? day
        withAnimation(reduceMotion ? nil : .spring(response: 0.45, dampingFraction: 0.86)) { day = target }
    }

    private func mark(_ item: Prayer) {
        let wasPrayed = library.prayed(item.rawValue, on: day)
        withAnimation(reduceMotion ? nil : .spring(response: 0.35, dampingFraction: 0.62)) {
            library.togglePrayed(item.rawValue, on: day)
        }
        guard !wasPrayed else { return }
        let count = Prayer.prayers.filter { library.prayed($0.rawValue, on: day) }.count
        if count == 5 { allFiveTick += 1 } else { markTick += 1 }
    }

    // MARK: Rest days

    private var restDays: some View {
        VStack(alignment: .leading, spacing: Space.xs) {
            Toggle(isOn: Binding(
                get: { library.isResting },
                set: { on in withAnimation(reduceMotion ? nil : Motion.smooth) { library.setResting(on) } }
            )) {
                HStack(spacing: Space.m) {
                    Image(systemName: "moon.zzz")
                        .font(.system(size: 17))
                        .foregroundStyle(Palette.blushStrong)
                        .frame(width: 26)
                        .symbolEffect(.bounce, value: library.isResting)
                        .accessibilityHidden(true)
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Rest days")
                            .foregroundStyle(Palette.textPrimary)
                        if let since = library.restSince {
                            Text("Since \(since.formatted(date: .abbreviated, time: .omitted))")
                                .font(.caption)
                                .foregroundStyle(Palette.textSecondary)
                                .transition(.opacity)
                        }
                    }
                }
            }
            .tint(Palette.accentFill)
            .padding(.horizontal, Space.m)
            .padding(.vertical, 10)
            .background(RoundedRectangle(cornerRadius: Radius.tile, style: .continuous).fill(Palette.blush.opacity(0.5)))

            Text("Prayer pauses during your period and is not made up.")
                .font(.footnote)
                .foregroundStyle(Palette.textSecondary)
                .padding(.horizontal, Space.m)
        }
    }
}

// MARK: - Live sky

/// A window of sky for the real hour, the sun or moon on its arc,
/// and a countdown to the next prayer that ticks each second.
private struct PrayerHero: View {
    @Environment(PrayerStore.self) private var prayer
    @Environment(LibraryStore.self) private var library
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var risen = false

    var body: some View {
        TimelineView(.periodic(from: .now, by: 1)) { context in
            let now = context.date
            let schedule = prayer.schedule(for: now)
            let sky = SkyState(date: now, schedule: schedule)
            let ink: Color = sky.isDark ? .white : Palette.ink
            let next = library.isResting ? nil : prayer.nextPrayer(after: now)

            ZStack(alignment: .topLeading) {
                SkyBackground(palette: sky.palette, isNight: sky.isNight)
                CelestialArc(progress: risen || reduceMotion ? sky.progress : 0, isNight: sky.isNight, ink: ink)
                    .padding(.top, 96)
                    .frame(height: 250)
                    .accessibilityHidden(true)
                // A soft light washes over the sky the moment a new prayer time begins.
                Rectangle()
                    .fill(.white)
                    .phaseAnimator([false, true, false], trigger: next?.0) { light, on in
                        light.opacity(on && !reduceMotion ? 0.35 : 0)
                    } animation: { on in on ? .easeOut(duration: 0.4) : .smooth(duration: 1.4) }
                    .allowsHitTesting(false)

                content(now: now, next: next, schedule: schedule, ink: ink)
                    .padding(Space.l)
            }
            .frame(height: 250)
            .clipShape(RoundedRectangle(cornerRadius: Radius.card, style: .continuous))
            .animation(reduceMotion ? nil : .smooth(duration: 1.2), value: sky.palette.darkness)
        }
        .onAppear {
            guard !risen else { return }
            withAnimation(.spring(response: 1.4, dampingFraction: 0.9).delay(0.2)) { risen = true }
        }
    }

    @ViewBuilder
    private func content(now: Date, next: (Prayer, Date)?, schedule: PrayerSchedule?, ink: Color) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            if library.isResting {
                Label("Rest days", systemImage: "moon.zzz")
                    .font(.system(.title, design: .serif, weight: .semibold))
                Spacer(minLength: 0)
            } else if let next {
                Text("Next prayer")
                    .font(.subheadline.weight(.medium))
                    .opacity(0.75)
                    .focusIn(delay: 0.15)
                ZStack(alignment: .leading) {
                    Text(next.0.name)
                        .font(.system(size: 40, weight: .semibold, design: .serif))
                        .id(next.0)
                        .transition(reduceMotion ? AnyTransition.opacity : AnyTransition.push(from: .bottom).combined(with: .opacity))
                }
                .animation(reduceMotion ? nil : .spring(response: 0.6, dampingFraction: 0.8), value: next.0)
                .focusIn(delay: 0.25)
                Text("at \(next.1.formatted(date: .omitted, time: .shortened))")
                    .font(.subheadline)
                    .opacity(0.8)
                    .focusIn(delay: 0.35)

                Spacer(minLength: 0)

                Text(Self.countdown(from: now, to: next.1))
                    .font(.system(size: 34, weight: .light, design: .rounded))
                    .monospacedDigit()
                    .contentTransition(reduceMotion ? .identity : .numericText(countsDown: true))
                    .animation(.snappy(duration: 0.3), value: Int(next.1.timeIntervalSince(now)))
                    .focusIn(delay: 0.45)
                progressBar(now: now, next: next, schedule: schedule, ink: ink)
                    .padding(.top, 6)
                    .focusIn(delay: 0.5)
            } else {
                Text("Prayer times")
                    .font(.system(size: 34, weight: .semibold, design: .serif))
                Spacer(minLength: 0)
            }
        }
        .foregroundStyle(ink)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        .accessibilityElement(children: .combine)
        .accessibilityAddTraits(.updatesFrequently)
        .accessibilityLabel(next.map { "Next prayer, \($0.0.name) at \($0.1.formatted(date: .omitted, time: .shortened)), in \(PrayerCard.relative(from: now, to: $0.1))" } ?? "")
    }

    /// How far she is from the last prayer to the next one.
    private func progressBar(now: Date, next: (Prayer, Date), schedule: PrayerSchedule?, ink: Color) -> some View {
        let previous = previousTime(before: now, schedule: schedule)
        let span = previous.map { next.1.timeIntervalSince($0) } ?? 0
        let fraction = span > 0 ? min(1, max(0, now.timeIntervalSince(previous!) / span)) : 0
        return Capsule()
            .fill(ink.opacity(0.18))
            .frame(height: 4)
            .overlay(alignment: .leading) {
                GeometryReader { geo in
                    Capsule()
                        .fill(ink.opacity(0.85))
                        .frame(width: max(4, geo.size.width * fraction))
                        .animation(.linear(duration: 1), value: fraction)
                }
            }
            .accessibilityHidden(true)
    }

    private func previousTime(before now: Date, schedule: PrayerSchedule?) -> Date? {
        if let today = Prayer.prayers.compactMap({ schedule?.time($0) }).filter({ $0 <= now }).max() {
            return today
        }
        guard let yesterday = Calendar.current.date(byAdding: .day, value: -1, to: now) else { return nil }
        return prayer.schedule(for: yesterday)?.time(.isha)
    }

    static func countdown(from now: Date, to target: Date) -> String {
        let total = max(0, Int(target.timeIntervalSince(now)))
        let h = total / 3600, m = (total % 3600) / 60, s = total % 60
        return h > 0 ? String(format: "%d:%02d:%02d", h, m, s) : String(format: "%d:%02d", m, s)
    }
}

// MARK: - Timeline

private enum NodeState { case past, next, upcoming }

private extension Prayer {
    var timeSymbol: String {
        switch self {
        case .fajr: "sun.horizon"
        case .sunrise: "sunrise"
        case .dhuhr: "sun.max"
        case .asr: "sun.min"
        case .maghrib: "sunset"
        case .isha: "moon.stars"
        }
    }
}

/// One prayer on the day's thread. The thread draws itself down the card,
/// the next prayer's node breathes, and her check answers with a small burst.
private struct PrayerTimelineRow: View {
    let prayer: Prayer
    let time: Date?
    let state: NodeState
    let isFirst: Bool
    let isLast: Bool
    let isPrayed: Bool
    let showsCheck: Bool
    let index: Int
    let toggle: () -> Void

    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var drawn = false
    @State private var landed = false

    var body: some View {
        HStack(spacing: Space.m) {
            Color.clear.frame(width: 14)
            Image(systemName: prayer.timeSymbol)
                .font(.system(size: 17))
                .foregroundStyle(prayer.isPrayer ? Palette.accent : Palette.textSecondary)
                .frame(width: 24)
                .symbolEffect(.bounce, value: landed)
                .accessibilityHidden(true)
            HStack(spacing: Space.xs) {
                Text(prayer.name)
                    .font(state == .next ? .headline : .body)
                    .foregroundStyle(prayer.isPrayer ? Palette.textPrimary : Palette.textSecondary)
                if state == .next {
                    Text("Next")
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(Palette.accent)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 2)
                        .background(Capsule().fill(Palette.accentWash))
                        .transition(.scale.combined(with: .opacity))
                }
            }
            Spacer(minLength: Space.xs)
            Text(time?.formatted(date: .omitted, time: .shortened) ?? "--")
                .font(.body.monospacedDigit())
                .foregroundStyle(state == .next ? Palette.accent : Palette.textPrimary)
            if showsCheck {
                PrayedCheck(name: prayer.name, isPrayed: isPrayed, toggle: toggle)
                    .transition(.scale.combined(with: .opacity))
            } else {
                Color.clear.frame(width: 8)
            }
        }
        .padding(.leading, Space.m)
        .padding(.trailing, Space.xs)
        .frame(minHeight: 56)
        .background(state == .next ? Palette.accentWash.opacity(0.45) : Color.clear)
        .background(alignment: .leading) {
            rail
                .frame(width: 14)
                .padding(.leading, Space.m)
        }
        .dealIn(index, baseDelay: 0.2, step: 0.05)
        .task {
            guard !reduceMotion else { drawn = true; return }
            withAnimation(.easeOut(duration: 0.3).delay(0.25 + Double(index) * 0.07)) { drawn = true }
            try? await Task.sleep(for: .seconds(0.35 + Double(index) * 0.07))
            landed = true
        }
        .accessibilityElement(children: .contain)
    }

    /// The thread: emerald up to now, quiet after it.
    private var rail: some View {
        ZStack {
            VStack(spacing: 0) {
                Rectangle()
                    .fill(isFirst ? Color.clear : (state == .upcoming ? Palette.border : Palette.accentFill))
                    .frame(width: 2)
                Rectangle()
                    .fill(isLast ? Color.clear : (state == .past ? Palette.accentFill : Palette.border))
                    .frame(width: 2)
            }
            .scaleEffect(x: 1, y: drawn ? 1 : 0, anchor: .top)
            node
        }
    }

    @ViewBuilder
    private var node: some View {
        switch state {
        case .past:
            Circle().fill(Palette.accentFill).frame(width: 10, height: 10)
        case .next:
            ZStack {
                if !reduceMotion {
                    Circle()
                        .stroke(Palette.accent, lineWidth: 1.5)
                        .frame(width: 12, height: 12)
                        .phaseAnimator([false, true]) { ring, out in
                            ring.scaleEffect(out ? 2.4 : 1).opacity(out ? 0 : 0.8)
                        } animation: { out in out ? .easeOut(duration: 1.6) : .linear(duration: 0.01) }
                }
                Circle().fill(Palette.accentFill).frame(width: 12, height: 12)
            }
        case .upcoming:
            Circle()
                .strokeBorder(Palette.border, lineWidth: 2)
                .background(Circle().fill(Palette.surface))
                .frame(width: 10, height: 10)
        }
    }
}

/// The prayed check: it presses in, fills with a soft overshoot, and sends out a few petals.
private struct PrayedCheck: View {
    let name: String
    let isPrayed: Bool
    let toggle: () -> Void

    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        Button(action: toggle) {
            ZStack {
                Circle()
                    .strokeBorder(Palette.border, lineWidth: 2)
                    .opacity(isPrayed ? 0 : 1)
                Circle()
                    .fill(Palette.accentFill)
                    .scaleEffect(isPrayed ? 1 : 0.01)
                    .opacity(isPrayed ? 1 : 0)
                Image(systemName: "checkmark")
                    .font(.system(size: 13, weight: .bold))
                    .foregroundStyle(Palette.onAccentFill)
                    .scaleEffect(isPrayed ? 1 : 0.3)
                    .opacity(isPrayed ? 1 : 0)
                    .symbolEffect(.bounce, value: reduceMotion ? false : isPrayed)
            }
            .frame(width: 28, height: 28)
            .keyframeAnimator(initialValue: 1.0, trigger: isPrayed) { [still = reduceMotion] view, scale in
                view.scaleEffect(still ? 1 : scale)
            } keyframes: { _ in
                SpringKeyframe(0.82, duration: 0.1)
                SpringKeyframe(1.12, duration: 0.18, spring: .bouncy)
                SpringKeyframe(1.0, duration: 0.3, spring: .smooth)
            }
            .overlay { PetalBurst(trigger: isPrayed, active: isPrayed) }
            .animation(.spring(response: 0.35, dampingFraction: 0.6), value: isPrayed)
            .frame(width: 44, height: 44)
            .contentShape(Rectangle())
        }
        .buttonStyle(PressableStyle(scale: 0.9))
        .accessibilityLabel(isPrayed ? "\(name) prayed" : "Mark \(name) as prayed")
    }
}

/// Shown when all five are marked: a warm light and a quiet word of thanks.
private struct AllFiveBanner: View {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var shown = false

    var body: some View {
        HStack(spacing: Space.s) {
            Image(systemName: "sparkles")
                .foregroundStyle(Color(hex: 0xC9A24A))
                .symbolEffect(.bounce, value: shown)
            Text("Alhamdulillah, all five prayed.")
                .font(AppFont.serif(.subheadline, weight: .semibold))
                .foregroundStyle(Palette.textPrimary)
            Spacer(minLength: 0)
        }
        .padding(.horizontal, Space.m)
        .frame(minHeight: 52)
        .background {
            LinearGradient(colors: [Color(hex: 0xC9A24A).opacity(0.18), Color(hex: 0xC9A24A).opacity(0.04)],
                           startPoint: .leading, endPoint: .trailing)
                .phaseAnimator([false, true, false], trigger: shown) { glow, on in
                    glow.opacity(on && !reduceMotion ? 1 : 0.7)
                } animation: { _ in .smooth(duration: 0.8) }
        }
        .overlay(alignment: .top) { RowDivider(inset: 0) }
        .onAppear { shown = true }
    }
}

private struct LocationSetup: View {
    @Environment(PrayerStore.self) private var prayer
    @Environment(\.openURL) private var openURL

    var body: some View {
        VStack(alignment: .leading, spacing: Space.m) {
            Text("Location")
                .font(.headline)
            if prayer.locationDenied {
                Text("Location is off in Settings.")
                    .font(.subheadline)
                    .foregroundStyle(Palette.blushStrong)
                Button("Open Settings") {
                    if let url = URL(string: UIApplication.openSettingsURLString) { openURL(url) }
                }
                .buttonStyle(.tinted)
            } else {
                Button {
                    prayer.requestLocation()
                } label: {
                    if prayer.isLocating { ProgressView().tint(Palette.onAccentFill) } else { Text("Use My Location") }
                }
                .buttonStyle(.primary)
            }
        }
        .padding(.vertical, Space.xs)
    }
}

// MARK: - Qibla

/// A compass that turns with her. A small Kaaba rides the rim; when it reaches the pointer
/// at the top she is facing the Qibla. The closer she gets, the more the dial answers.
struct QiblaView: View {
    @Environment(PrayerStore.self) private var prayer
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @AppStorage(PrefKey.haptics) private var hapticsOn = true

    /// Heading without the jump at north, so the dial always turns the short way.
    @State private var dial: Double = 0
    @State private var hasDial = false
    @State private var aligned = false
    /// The dial swings in and settles on first appearance, like a real compass.
    @State private var settled = false

    var body: some View {
        VStack(spacing: Space.xl) {
            if let bearing = prayer.qiblaBearing {
                let heading = prayer.heading
                // Degrees she still needs to turn. Positive means turn right.
                let offset = heading.map { Self.normalized(bearing - $0) }

                Spacer(minLength: 0)
                QiblaDial(
                    bearing: bearing,
                    dial: heading == nil ? 0 : dial,
                    swing: settled || reduceMotion ? 0 : -150,
                    closeness: offset.map { max(0, 1 - abs($0) / 45) } ?? 0,
                    aligned: aligned
                )
                .frame(width: 290, height: 290)
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(heading == nil
                    ? "The Qibla is \(Int(bearing.rounded())) degrees from north"
                    : (aligned ? "You are facing the Qibla" : turnText(offset ?? 0)))
                .accessibilityAddTraits(.updatesFrequently)
                .accessibilityHint(heading == nil ? "" : "Hold your iPhone flat and turn slowly until the Kaaba reaches the top")

                guidance(bearing: bearing, offset: offset)
                    .padding(.horizontal, Space.xl)
                    .focusIn(delay: 0.6)

                if let km = distanceKm {
                    Label(km, systemImage: "location")
                        .font(.footnote)
                        .foregroundStyle(Palette.textSecondary)
                        .focusIn(delay: 0.8)
                }
                Spacer(minLength: 0)
            } else {
                LocationSetup().padding(Space.page)
                Spacer()
            }
        }
        .frame(maxWidth: .infinity)
        .pageBackground()
        .navigationTitle("Qibla")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear {
            prayer.startHeading()
            withAnimation(.spring(response: 1.1, dampingFraction: 0.42).delay(0.25)) { settled = true }
        }
        .onDisappear { prayer.stopHeading() }
        .onChange(of: prayer.heading) { _, new in
            guard let new else { return }
            if !hasDial {
                dial = new
                hasDial = true
            } else {
                // Add only the short way round, so 359 to 1 is a 2 degree turn.
                withAnimation(reduceMotion ? nil : .interpolatingSpring(stiffness: 120, damping: 18)) {
                    dial += Self.normalized(new - dial)
                }
            }
            guard let bearing = prayer.qiblaBearing else { return }
            let off = abs(Self.normalized(bearing - new))
            // Lock in under 4 degrees, let go past 7, so it does not flicker at the edge.
            let nowAligned = aligned ? off < 7 : off < 4
            if nowAligned != aligned {
                withAnimation(reduceMotion ? nil : .spring(response: 0.4, dampingFraction: 0.6)) { aligned = nowAligned }
            }
        }
        // Gentle ticks every 5 degrees in the last 30, then success when she faces it.
        .sensoryFeedback(.selection, trigger: tickStep) { _, new in hapticsOn && new != nil && !aligned }
        .sensoryFeedback(.success, trigger: aligned) { _, new in hapticsOn && new }
    }

    private var tickStep: Int? {
        guard let heading = prayer.heading, let bearing = prayer.qiblaBearing else { return nil }
        let off = Self.normalized(bearing - heading)
        return abs(off) < 30 ? Int(off / 5) : nil
    }

    @ViewBuilder
    private func guidance(bearing: Double, offset: Double?) -> some View {
        VStack(spacing: Space.xs) {
            ZStack {
                if prayer.heading == nil {
                    Text("\(Int(bearing.rounded()))° from north")
                        .transition(.opacity)
                } else if aligned {
                    Label("You are facing the Qibla", systemImage: "checkmark.circle.fill")
                        .foregroundStyle(Palette.accent)
                        .symbolEffect(.bounce, value: aligned)
                        .transition(.scale(scale: 0.9).combined(with: .opacity))
                } else if let offset {
                    HStack(spacing: 6) {
                        Image(systemName: offset > 0 ? "arrow.turn.up.right" : "arrow.turn.up.left")
                            .foregroundStyle(Palette.accent)
                            .contentTransition(.symbolEffect(.replace))
                        Text(turnText(offset))
                            .monospacedDigit()
                            .contentTransition(.numericText(value: abs(offset.rounded())))
                            .animation(.snappy, value: Int(offset.rounded()))
                    }
                    .transition(.opacity)
                }
            }
            .font(.system(.title3, design: .serif, weight: .semibold))

            if prayer.heading == nil {
                Text("Compass not available")
                    .font(.subheadline)
                    .foregroundStyle(Palette.textSecondary)
            }
        }
    }

    private func turnText(_ offset: Double) -> String {
        let degrees = Int(abs(offset).rounded())
        return offset > 0 ? "Turn right \(degrees)°" : "Turn left \(degrees)°"
    }

    /// Straight-line distance to the Kaaba, in her local units.
    private var distanceKm: String? {
        guard let lat = prayer.latitude, let lon = prayer.longitude else { return nil }
        let r = Double.pi / 180
        let dLat = (21.4225 - lat) * r, dLon = (39.8262 - lon) * r
        let a = sin(dLat / 2) * sin(dLat / 2) + cos(lat * r) * cos(21.4225 * r) * sin(dLon / 2) * sin(dLon / 2)
        let km = 6371 * 2 * atan2(sqrt(a), sqrt(1 - a))
        let distance = Measurement(value: km.rounded(), unit: UnitLength.kilometers)
        return "\(distance.formatted(.measurement(width: .abbreviated, usage: .road))) to Makkah"
    }

    static func normalized(_ angle: Double) -> Double {
        var a = angle.truncatingRemainder(dividingBy: 360)
        if a > 180 { a -= 360 }
        if a < -180 { a += 360 }
        return a
    }
}

/// The compass face. Ticks gather on arrival, the face turns against her heading,
/// and the Kaaba on the rim glows brighter as she gets closer.
private struct QiblaDial: View {
    let bearing: Double
    /// Her heading, unwrapped.
    let dial: Double
    /// Extra turn for the settling swing on arrival.
    let swing: Double
    /// 0 when far, 1 when facing the Qibla.
    let closeness: Double
    let aligned: Bool

    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var gathered = false

    private let gold = Color(hex: 0xC9A24A)

    var body: some View {
        GeometryReader { geo in
            let size = min(geo.size.width, geo.size.height)
            let radius = size / 2
            ZStack {
                // Light that grows as she nears the Qibla, and pulses once she faces it.
                Circle()
                    .fill(Palette.accent.opacity(0.10 + 0.25 * closeness))
                    .blur(radius: 30)
                    .scaleEffect(0.8 + 0.25 * closeness)
                if aligned && !reduceMotion {
                    ForEach(0..<2, id: \.self) { i in
                        Circle()
                            .stroke(Palette.accent.opacity(0.5), lineWidth: 1.5)
                            .phaseAnimator([false, true]) { ring, out in
                                ring.scaleEffect(out ? 1.35 : 0.92).opacity(out ? 0 : 0.8)
                            } animation: { out in
                                out ? .easeOut(duration: 1.8).delay(Double(i) * 0.9) : .linear(duration: 0.01)
                            }
                    }
                }

                Circle()
                    .fill(aligned ? Palette.accentWash : Palette.surface)
                    .padding(14)
                    .shadow(color: .black.opacity(0.06), radius: 16, y: 6)

                // The turning face.
                ZStack {
                    ForEach(0..<72, id: \.self) { i in
                        let major = i % 18 == 0
                        let mid = i % 6 == 0
                        Capsule()
                            .fill(major ? Palette.textPrimary : Palette.textSecondary.opacity(mid ? 0.6 : 0.3))
                            .frame(width: major ? 2.5 : 1.5, height: major ? 14 : (mid ? 10 : 6))
                            .offset(y: -(radius - 26))
                            .rotationEffect(.degrees(Double(i) * 5))
                            .opacity(gathered || reduceMotion ? 1 : 0)
                            .scaleEffect(gathered || reduceMotion ? 1 : 0.6)
                            .animation(.spring(response: 0.5, dampingFraction: 0.7).delay(Double(i) * 0.006), value: gathered)
                    }
                    ForEach(0..<4, id: \.self) { i in
                        Text(["N", "E", "S", "W"][i])
                            .font(.footnote.weight(i == 0 ? .bold : .semibold))
                            .foregroundStyle(i == 0 ? Palette.blushStrong : Palette.textSecondary)
                            .rotationEffect(.degrees(-(Double(i) * 90) + dial - swing))
                            .offset(y: -(radius - 50))
                            .rotationEffect(.degrees(Double(i) * 90))
                            .focusIn(delay: 0.3 + Double(i) * 0.06)
                    }
                    // The Kaaba on the rim, at the Qibla bearing, kept upright.
                    KaabaMark(glow: closeness, aligned: aligned, gold: gold)
                        .rotationEffect(.degrees(-bearing + dial - swing))
                        // Sits on the edge of the face.
                        .offset(y: -(radius - 14))
                        .rotationEffect(.degrees(bearing))
                        .scaleEffect(gathered || reduceMotion ? 1 : 0.2)
                        .opacity(gathered || reduceMotion ? 1 : 0)
                        .animation(.spring(response: 0.5, dampingFraction: 0.55).delay(0.5), value: gathered)
                    // A thin line from the center to the Kaaba.
                    Capsule()
                        .fill(LinearGradient(colors: [Palette.accent.opacity(0), Palette.accent.opacity(0.35 + 0.5 * closeness)],
                                             startPoint: .bottom, endPoint: .top))
                        .frame(width: 2, height: radius - 40)
                        .offset(y: -(radius - 40) / 2)
                        .rotationEffect(.degrees(bearing))
                        .opacity(gathered || reduceMotion ? 1 : 0)
                        .animation(.easeOut(duration: 0.5).delay(0.6), value: gathered)
                }
                .rotationEffect(.degrees(-dial + swing))

                // The phone's own direction: a fixed pointer at the top.
                Image(systemName: "arrowtriangle.down.fill")
                    .font(.system(size: 14))
                    .foregroundStyle(aligned ? Palette.accent : Palette.textSecondary)
                    .symbolEffect(.bounce, value: reduceMotion ? false : aligned)
                    .scaleEffect(aligned ? 1.3 : 1)
                    .offset(y: -radius - 14)
                    .accessibilityHidden(true)

                Circle()
                    .fill(Palette.accentFill)
                    .frame(width: 8, height: 8)
            }
            .frame(width: geo.size.width, height: geo.size.height)
        }
        .onAppear { gathered = true }
    }
}

/// A small Kaaba: a dark cube with a gold band.
private struct KaabaMark: View {
    let glow: Double
    let aligned: Bool
    let gold: Color

    var body: some View {
        ZStack(alignment: .top) {
            RoundedRectangle(cornerRadius: 4, style: .continuous)
                .fill(Color(hex: 0x1E1E1E))
            Rectangle()
                .fill(gold)
                .frame(height: 3)
                .padding(.top, 6)
        }
        .frame(width: 24, height: 24)
        .clipShape(RoundedRectangle(cornerRadius: 4, style: .continuous))
        .shadow(color: gold.opacity(0.3 + 0.6 * glow), radius: 4 + 10 * glow)
        .scaleEffect(aligned ? 1.25 : 1)
        .accessibilityHidden(true)
    }
}

// MARK: - Settings

/// Every prayer setting, each answering as she changes it: times roll to their new values,
/// the bell rings, the chosen sound plays, and a sample notification shows what she will get.
struct PrayerSettingsView: View {
    @Environment(PrayerStore.self) private var prayer
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @AppStorage(PrefKey.haptics) private var hapticsOn = true
    @State private var denied = false
    @State private var playing: PrayerSound?
    @State private var playToken = 0
    @Namespace private var asrGlide

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: Space.xl) {
                if prayer.hasLocation {
                    TimesPreview()
                        .focusIn(delay: 0.05)
                        .scrollSettle()
                }

                calculation
                    .dealIn(0, baseDelay: 0.15)
                    .scrollSettle()

                notifications
                    .dealIn(1, baseDelay: 0.15)
                    .scrollSettle()

                if prayer.notifyAtPrayerTimes {
                    soundSection
                        .dealIn(0, baseDelay: 0.1)
                        .scrollSettle()
                    remindFor
                        .dealIn(1, baseDelay: 0.1)
                        .scrollSettle()
                }

                RamadanReminderSection()
                    .dealIn(2, baseDelay: 0.15)
                    .scrollSettle()

                locationSection
                    .dealIn(2, baseDelay: 0.15)
                    .scrollSettle()
            }
            .padding(.horizontal, Space.page)
            .padding(.vertical, Space.s)
            .animation(reduceMotion ? nil : Motion.smooth, value: prayer.notifyAtPrayerTimes)
        }
        .pageBackground()
        .navigationTitle("Prayer Settings")
        .navigationBarTitleDisplayMode(.inline)
        .onDisappear { SoundPreview.shared.stop() }
        .sensoryFeedback(.selection, trigger: prayer.calculationMethod) { _, _ in hapticsOn }
        .sensoryFeedback(.selection, trigger: prayer.asr) { _, _ in hapticsOn }
        .sensoryFeedback(.selection, trigger: prayer.enabledPrayers) { _, _ in hapticsOn }
    }

    // MARK: Calculation

    private var calculation: some View {
        GroupedSection(title: "Calculation") {
            Menu {
                Picker("Calculation method", selection: Binding(
                    get: { prayer.calculationMethod },
                    set: { new in withAnimation(reduceMotion ? nil : Motion.gentle) { prayer.calculationMethod = new } }
                )) {
                    ForEach(CalculationMethod.allCases) { Text($0.title).tag($0) }
                }
            } label: {
                HStack(spacing: Space.m) {
                    Image(systemName: "globe")
                        .font(.system(size: 17))
                        .foregroundStyle(Palette.accent)
                        .frame(width: 26)
                        .symbolEffect(.bounce, value: prayer.calculationMethod)
                        .accessibilityHidden(true)
                    Text("Method")
                        .foregroundStyle(Palette.textPrimary)
                    Spacer(minLength: Space.xs)
                    Text(prayer.calculationMethod.title)
                        .foregroundStyle(Palette.textSecondary)
                        .lineLimit(1)
                        .contentTransition(.opacity)
                    Image(systemName: "chevron.up.chevron.down")
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(Palette.textSecondary.opacity(0.6))
                        .accessibilityHidden(true)
                }
                .padding(.horizontal, Space.m)
                .frame(minHeight: 48)
                .contentShape(Rectangle())
            }
            RowDivider()
            VStack(alignment: .leading, spacing: Space.s) {
                HStack(spacing: Space.m) {
                    Image(systemName: "sun.min")
                        .font(.system(size: 17))
                        .foregroundStyle(Palette.accent)
                        .frame(width: 26)
                        .accessibilityHidden(true)
                    Text("Asr time")
                        .foregroundStyle(Palette.textPrimary)
                }
                asrSwitch
            }
            .padding(Space.m)
        }
    }

    /// Two choices with a selection that glides between them.
    private var asrSwitch: some View {
        HStack(spacing: 4) {
            ForEach(AsrMethod.allCases) { method in
                let selected = prayer.asr == method
                Button {
                    withAnimation(reduceMotion ? nil : .spring(response: 0.38, dampingFraction: 0.78)) { prayer.asr = method }
                } label: {
                    Text(method == .standard ? "Standard" : "Hanafi")
                        .font(.subheadline.weight(selected ? .semibold : .medium))
                        .foregroundStyle(selected ? Palette.onAccentFill : Palette.accent)
                        .frame(maxWidth: .infinity, minHeight: 38)
                        .background {
                            if selected {
                                Capsule()
                                    .fill(Palette.accentFill)
                                    .matchedGeometryEffect(id: "asr", in: asrGlide)
                            }
                        }
                        .contentShape(Capsule())
                }
                .buttonStyle(PressableStyle(scale: 0.97))
                .accessibilityAddTraits(selected ? .isSelected : [])
                .accessibilityHint(method == .standard ? "Shafi'i, Maliki and Hanbali" : "Asr comes a little later")
            }
        }
        .padding(4)
        .background(Capsule().fill(Palette.accentWash))
    }

    // MARK: Notifications

    private var notifications: some View {
        GroupedSection(title: "Notifications") {
            Toggle(isOn: Binding(
                get: { prayer.notifyAtPrayerTimes },
                set: { on in
                    if on {
                        Task { denied = !(await prayer.enableNotifications()) }
                    } else {
                        prayer.notifyAtPrayerTimes = false
                    }
                }
            )) {
                HStack(spacing: Space.m) {
                    Image(systemName: prayer.notifyAtPrayerTimes ? "bell.fill" : "bell")
                        .font(.system(size: 17))
                        .foregroundStyle(Palette.accent)
                        .frame(width: 26)
                        .contentTransition(.symbolEffect(.replace))
                        .symbolEffect(.bounce, options: .repeat(2), value: prayer.notifyAtPrayerTimes)
                        .accessibilityHidden(true)
                    Text("Notify at prayer times")
                        .foregroundStyle(Palette.textPrimary)
                }
            }
            .tint(Palette.accentFill)
            .padding(.horizontal, Space.m)
            .frame(minHeight: 48)

            if denied {
                Text("Notifications are off in Settings.")
                    .font(.footnote)
                    .foregroundStyle(Palette.blushStrong)
                    .padding(.horizontal, Space.m)
                    .padding(.bottom, Space.s)
                    .transition(.opacity)
            }

            if prayer.notifyAtPrayerTimes {
                PrayerNotificationSample(prayerName: prayer.nextPrayer()?.0.name ?? "Asr")
                    .padding(Space.m)
                    .background(Palette.accentWash.opacity(0.5))
                    .transition(.opacity)
            }
        }
    }

    // MARK: Sound

    private var soundSection: some View {
        GroupedSection(title: "Sound") {
            ForEach(Array(PrayerSound.available.enumerated()), id: \.element) { index, sound in
                if index > 0 { RowDivider() }
                Button { play(sound) } label: {
                    HStack(spacing: Space.m) {
                        Image(systemName: playing == sound ? "speaker.wave.3.fill" : "speaker.wave.2")
                            .font(.system(size: 17))
                            .foregroundStyle(Palette.accent)
                            .frame(width: 26)
                            .contentTransition(.symbolEffect(.replace))
                            .symbolEffect(.variableColor.iterative, options: .repeating, isActive: playing == sound && !reduceMotion)
                            .accessibilityHidden(true)
                        VStack(alignment: .leading, spacing: 2) {
                            Text(sound.title)
                                .foregroundStyle(Palette.textPrimary)
                            if playing == sound {
                                Text("Playing")
                                    .font(.caption)
                                    .foregroundStyle(Palette.accent)
                                    .transition(.opacity)
                            }
                        }
                        Spacer()
                        if prayer.sound == sound {
                            Image(systemName: "checkmark")
                                .font(.body.weight(.semibold))
                                .foregroundStyle(Palette.accent)
                                .transition(.scale.combined(with: .opacity))
                        }
                    }
                    .padding(.horizontal, Space.m)
                    .frame(minHeight: 52)
                    .contentShape(Rectangle())
                }
                .accessibilityLabel(sound.title)
                .accessibilityHint("Plays the sound and chooses it")
                .accessibilityAddTraits(prayer.sound == sound ? .isSelected : [])
            }
        }
        .sensoryFeedback(.selection, trigger: prayer.sound) { _, _ in hapticsOn }
    }

    private func play(_ sound: PrayerSound) {
        withAnimation(reduceMotion ? nil : Motion.gentle) {
            prayer.sound = sound
            playing = sound
        }
        let duration = SoundPreview.shared.play(sound)
        playToken += 1
        let token = playToken
        Task {
            try? await Task.sleep(for: .seconds(min(max(duration, 1.2), 8)))
            if token == playToken {
                withAnimation(reduceMotion ? nil : Motion.gentle) { playing = nil }
            }
        }
    }

    // MARK: Which prayers

    private var remindFor: some View {
        let count = Prayer.prayers.filter { prayer.enabledPrayers.contains($0) }.count
        return GroupedSection(title: "Remind me for") {
            VStack(spacing: Space.s) {
                HStack(spacing: 0) {
                    ForEach(Array(Prayer.prayers.enumerated()), id: \.element) { index, item in
                        ReminderChip(prayer: item, isOn: prayer.enabledPrayers.contains(item)) {
                            withAnimation(reduceMotion ? nil : .spring(response: 0.35, dampingFraction: 0.62)) {
                                if prayer.enabledPrayers.contains(item) {
                                    prayer.enabledPrayers.remove(item)
                                } else {
                                    prayer.enabledPrayers.insert(item)
                                }
                            }
                        }
                        .dealIn(index, baseDelay: 0.2, step: 0.05)
                    }
                }
                Text(count == 0 ? "No prayer reminders" : "\(count) of 5 prayers")
                    .font(.footnote)
                    .foregroundStyle(Palette.textSecondary)
                    .monospacedDigit()
                    .contentTransition(.numericText(value: Double(count)))
                    .animation(.snappy, value: count)
            }
            .padding(.vertical, Space.m)
        }
    }

    // MARK: Location

    private var locationSection: some View {
        GroupedSection(title: "Location") {
            HStack(spacing: Space.m) {
                Image(systemName: prayer.hasLocation ? "location.fill" : "location")
                    .font(.system(size: 17))
                    .foregroundStyle(Palette.accent)
                    .frame(width: 26)
                    .symbolEffect(.pulse, options: .repeating, isActive: prayer.isLocating && !reduceMotion)
                    .accessibilityHidden(true)
                Text(prayer.isLocating ? "Finding your location" : (prayer.placeName ?? "Not set"))
                    .foregroundStyle(Palette.textPrimary)
                    .lineLimit(1)
                    .contentTransition(.opacity)
                    .animation(.smooth, value: prayer.placeName)
                    .animation(.smooth, value: prayer.isLocating)
                Spacer(minLength: Space.xs)
                if prayer.isLocating {
                    ProgressView()
                        .transition(.opacity)
                } else {
                    Button(prayer.hasLocation ? "Update" : "Use My Location") { prayer.requestLocation() }
                        .buttonStyle(.tintedCompact)
                        .transition(.opacity)
                }
            }
            .padding(.horizontal, Space.m)
            .frame(minHeight: 56)
            .animation(reduceMotion ? nil : Motion.gentle, value: prayer.isLocating)
        }
    }
}

/// Today's five times. They roll to new values and glow briefly when a setting changes them.
private struct TimesPreview: View {
    @Environment(PrayerStore.self) private var prayer

    var body: some View {
        let schedule = prayer.schedule(for: .now)
        VStack(alignment: .leading, spacing: Space.s) {
            Text("Today's times")
                .font(.footnote.weight(.semibold))
                .foregroundStyle(Palette.textSecondary)
            HStack(spacing: 4) {
                ForEach(Array(Prayer.prayers.enumerated()), id: \.element) { index, item in
                    TimeCell(prayer: item, time: schedule?.time(item))
                        .dealIn(index, baseDelay: 0.1, step: 0.04)
                }
            }
        }
        .padding(Space.m)
        .background(RoundedRectangle(cornerRadius: Radius.tile, style: .continuous).fill(Palette.surface))
    }
}

private struct TimeCell: View {
    let prayer: Prayer
    let time: Date?

    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    private var minutes: Int {
        guard let time else { return 0 }
        let parts = Calendar.current.dateComponents([.hour, .minute], from: time)
        return (parts.hour ?? 0) * 60 + (parts.minute ?? 0)
    }

    var body: some View {
        VStack(spacing: 4) {
            Image(systemName: prayer.timeSymbol)
                .font(.footnote)
                .foregroundStyle(Palette.accent)
                .accessibilityHidden(true)
            Text(prayer.name)
                .font(.caption.weight(.semibold))
                .foregroundStyle(Palette.textPrimary)
            Text(time?.formatted(date: .omitted, time: .shortened) ?? "--")
                .font(.caption.monospacedDigit())
                .foregroundStyle(Palette.textSecondary)
                .lineLimit(1)
                .minimumScaleFactor(0.75)
                .contentTransition(reduceMotion ? .identity : .numericText(value: Double(minutes)))
                .animation(.snappy, value: minutes)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, Space.xs)
        .background {
            RoundedRectangle(cornerRadius: 10, style: .continuous)
                .fill(Palette.accentWash)
                .phaseAnimator([false, true, false], trigger: minutes) { glow, on in
                    glow.opacity(on && !reduceMotion ? 1 : 0)
                } animation: { on in on ? .easeOut(duration: 0.2) : .smooth(duration: 0.9) }
        }
        .accessibilityElement(children: .combine)
    }
}

/// What a prayer notification looks like. It drops in with a small settle.
private struct PrayerNotificationSample: View {
    let prayerName: String
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var shown = false

    var body: some View {
        VStack(alignment: .leading, spacing: Space.s) {
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
                        Text("Time for \(prayerName)").font(.footnote.weight(.semibold))
                        Spacer()
                        Text("now").font(.caption2).foregroundStyle(.secondary)
                    }
                    Text("Hayya 'ala as-salah. Come to prayer.")
                        .font(.footnote)
                        .lineLimit(1)
                }
            }
            Text("I prayed")
                .font(.footnote.weight(.semibold))
                .foregroundStyle(Palette.accent)
                .frame(maxWidth: .infinity, minHeight: 32)
                .background(RoundedRectangle(cornerRadius: 10, style: .continuous).fill(Palette.accentWash))
        }
        .foregroundStyle(Palette.textPrimary)
        .padding(10)
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 18, style: .continuous))
        .shadow(color: .black.opacity(0.12), radius: 10, y: 4)
        .offset(y: shown || reduceMotion ? 0 : -24)
        .opacity(shown || reduceMotion ? 1 : 0)
        .scaleEffect(shown || reduceMotion ? 1 : 0.96, anchor: .top)
        .onAppear {
            withAnimation(.spring(response: 0.5, dampingFraction: 0.68).delay(0.15)) { shown = true }
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel("Sample notification: Time for \(prayerName). Come to prayer. With an I prayed button.")
    }
}

/// A prayer she can include in reminders. It fills with a soft bounce when turned on.
private struct ReminderChip: View {
    let prayer: Prayer
    let isOn: Bool
    let toggle: () -> Void

    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        Button(action: toggle) {
            VStack(spacing: 6) {
                ZStack {
                    Circle().fill(Palette.accentWash)
                    Circle()
                        .fill(Palette.accentFill)
                        .scaleEffect(isOn ? 1 : 0.01)
                        .opacity(isOn ? 1 : 0)
                    Image(systemName: prayer.timeSymbol)
                        .font(.system(size: 17))
                        .foregroundStyle(isOn ? Palette.onAccentFill : Palette.accent)
                        .symbolEffect(.bounce, value: isOn)
                }
                .frame(width: 46, height: 46)
                .keyframeAnimator(initialValue: 1.0, trigger: isOn) { [still = reduceMotion] view, scale in
                    view.scaleEffect(still ? 1 : scale)
                } keyframes: { _ in
                    SpringKeyframe(0.86, duration: 0.1)
                    SpringKeyframe(1.08, duration: 0.18, spring: .bouncy)
                    SpringKeyframe(1.0, duration: 0.3, spring: .smooth)
                }
                Text(prayer.name)
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(isOn ? Palette.textPrimary : Palette.textSecondary)
            }
            .frame(maxWidth: .infinity)
            .contentShape(Rectangle())
        }
        .buttonStyle(PressableStyle(scale: 0.97))
        .accessibilityLabel(prayer.name)
        .accessibilityValue(isOn ? "Reminder on" : "Reminder off")
        .accessibilityAddTraits(isOn ? .isSelected : [])
    }
}

/// Plays a prayer sound so she can hear it before choosing.
@MainActor
final class SoundPreview {
    static let shared = SoundPreview()
    private var player: AVAudioPlayer?

    /// Plays the sound and returns roughly how long it lasts, in seconds.
    @discardableResult
    func play(_ sound: PrayerSound) -> TimeInterval {
        player?.stop()
        guard let name = sound.fileName,
              let url = Bundle.main.url(forResource: name, withExtension: nil) else {
            AudioServicesPlaySystemSound(1007)
            return 1.2
        }
        try? AVAudioSession.sharedInstance().setCategory(.playback, mode: .default)
        try? AVAudioSession.sharedInstance().setActive(true)
        player = try? AVAudioPlayer(contentsOf: url)
        player?.play()
        return player?.duration ?? 1.2
    }

    func stop() {
        player?.stop()
        player = nil
    }
}
