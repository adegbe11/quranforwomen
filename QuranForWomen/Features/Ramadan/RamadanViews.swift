import SwiftUI

/// One quiet card on Today during Ramadan: the day, when suhoor ends and when iftar is.
/// Shows nothing on other days.
struct RamadanTodayCard: View {
    let today: Date

    @Environment(PrayerStore.self) private var prayer

    var body: some View {
        let h = Hijri.of(today)
        if h.month == Hijri.ramadan {
            let schedule = prayer.schedule(for: today)
            NavigationLink(value: Route.islamicCalendar) {
                VStack(alignment: .leading, spacing: Space.l) {
                    VStack(alignment: .leading, spacing: Space.xs) {
                        Eyebrow(text: "Ramadan")
                            .focusIn(delay: 0.4)
                        Text("Day \(h.day) of Ramadan")
                            .font(.system(.title2, design: .serif, weight: .semibold))
                            .foregroundStyle(Palette.textPrimary)
                            .focusIn(delay: 0.5)
                    }
                    if let fajr = schedule?.time(.fajr), let maghrib = schedule?.time(.maghrib) {
                        HStack(alignment: .top, spacing: Space.m) {
                            time("Suhoor ends", fajr)
                            time("Iftar", maghrib)
                        }
                        .focusIn(delay: 0.6)
                    }
                    HStack(spacing: Space.s) {
                        Image(systemName: "calendar")
                            .accessibilityHidden(true)
                        Text("Fasting tracker")
                            .font(.headline)
                        Spacer()
                        Image(systemName: "chevron.right")
                            .font(.footnote.weight(.semibold))
                            .accessibilityHidden(true)
                    }
                    .foregroundStyle(Palette.accent)
                    .frame(minHeight: 44)
                    .focusIn(delay: 0.7)
                }
                .cardSurface(padding: Space.xl)
            }
            .buttonStyle(.pressable)
            .accessibilityHint("Opens the fasting tracker")
        }
    }

    private func time(_ label: String, _ date: Date) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(label)
                .font(.subheadline)
                .foregroundStyle(Palette.textSecondary)
            Text(date.formatted(date: .omitted, time: .shortened))
                .font(.title3.weight(.semibold).monospacedDigit())
                .foregroundStyle(Palette.textPrimary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .combine)
    }
}

/// Suhoor and iftar reminders, in Prayer Settings.
struct RamadanReminderSection: View {
    @Environment(FastingStore.self) private var fasting
    @Environment(PrayerStore.self) private var prayer
    @Environment(LibraryStore.self) private var library
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @AppStorage(PrefKey.haptics) private var hapticsOn = true
    @State private var denied = false

    private let minuteChoices = [15, 30, 45, 60]

    var body: some View {
        GroupedSection(title: "Ramadan") {
            toggle("Suhoor reminder", symbol: "moon.stars", isOn: fasting.suhoorReminder, moment: .suhoor)

            if fasting.suhoorReminder {
                VStack(alignment: .leading, spacing: Space.xs) {
                    Text("Before Fajr")
                        .font(.subheadline)
                        .foregroundStyle(Palette.textSecondary)
                    Picker("Before Fajr", selection: Binding(
                        get: { fasting.suhoorMinutes },
                        set: { new in
                            withAnimation(reduceMotion ? nil : Motion.gentle) { fasting.setSuhoorMinutes(new) }
                            refresh()
                        }
                    )) {
                        ForEach(minuteChoices, id: \.self) { Text("\($0) min").tag($0) }
                    }
                    .pickerStyle(.segmented)
                }
                .padding(.leading, 58)
                .padding(.trailing, Space.m)
                .padding(.bottom, Space.s)
                .transition(.opacity)
            }

            RowDivider()
            toggle("Iftar reminder", symbol: "sunset", isOn: fasting.iftarReminder, moment: .iftar)

            if denied {
                Text("Notifications are off in Settings.")
                    .font(.footnote)
                    .foregroundStyle(Palette.blushStrong)
                    .padding(.horizontal, Space.m)
                    .padding(.bottom, Space.s)
                    .transition(.opacity)
            }
        }
        .animation(reduceMotion ? nil : Motion.smooth, value: fasting.suhoorReminder)
        .animation(reduceMotion ? nil : Motion.smooth, value: denied)
        .sensoryFeedback(.selection, trigger: fasting.suhoorMinutes) { _, _ in hapticsOn }
        // Prayer times move with her settings, so the reminders follow them.
        .onChange(of: prayer.calculationMethod) { _, _ in refresh() }
        .onChange(of: prayer.asr) { _, _ in refresh() }
        .onChange(of: prayer.latitude) { _, _ in refresh() }
    }

    private func toggle(_ title: String, symbol: String, isOn: Bool, moment: RamadanMoment) -> some View {
        Toggle(isOn: Binding(
            get: { isOn },
            set: { on in
                if on {
                    Task {
                        let allowed = await fasting.requestPermission()
                        denied = !allowed
                        if allowed {
                            withAnimation(reduceMotion ? nil : Motion.smooth) { fasting.setReminder(moment, true) }
                            refresh()
                        }
                    }
                } else {
                    withAnimation(reduceMotion ? nil : Motion.smooth) { fasting.setReminder(moment, false) }
                    refresh()
                }
            }
        )) {
            HStack(spacing: Space.m) {
                Image(systemName: symbol)
                    .font(.system(size: 17))
                    .foregroundStyle(Palette.accent)
                    .frame(width: 26)
                    .symbolEffect(.bounce, value: isOn)
                    .accessibilityHidden(true)
                Text(title)
                    .foregroundStyle(Palette.textPrimary)
            }
        }
        .tint(Palette.accentFill)
        .padding(.horizontal, Space.m)
        .frame(minHeight: 48)
    }

    private func refresh() {
        fasting.refreshReminders(prayer: prayer, resting: library.isResting)
    }
}
