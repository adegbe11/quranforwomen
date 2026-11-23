import SwiftUI
import UIKit

/// One optional daily reminder as a grouped section. The bell bounces as she turns it on,
/// the time row opens beneath it, and if notifications are off a short note slides in.
struct GentleReminderSection: View {
    let reminder: GentleReminder
    let title: String

    @AppStorage private var isOn: Bool
    @AppStorage private var minutes: Int
    @AppStorage(PrefKey.haptics) private var hapticsOn = true
    @Environment(\.openURL) private var openURL
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Environment(\.inBook) private var inBook
    @State private var denied = false

    init(_ reminder: GentleReminder, title: String) {
        self.reminder = reminder
        self.title = title
        _isOn = AppStorage(wrappedValue: false, reminder.enabledKey)
        _minutes = AppStorage(wrappedValue: reminder.defaultMinutes, reminder.minutesKey)
    }

    private var accent: Color { inBook ? BookInk.rule : Palette.accent }
    private var ink: Color { inBook ? BookInk.ink : Palette.textPrimary }

    var body: some View {
        GroupedSection {
            Toggle(isOn: Binding(
                get: { isOn },
                set: { newValue in Task { await setEnabled(newValue) } }
            )) {
                HStack(spacing: Space.m) {
                    Image(systemName: isOn ? "bell.fill" : "bell")
                        .font(.system(size: 17))
                        .foregroundStyle(accent)
                        .frame(width: 26)
                        .contentTransition(.symbolEffect(.replace))
                        .symbolEffect(.bounce, value: reduceMotion ? false : isOn)
                        .accessibilityHidden(true)
                    Text(title)
                        .foregroundStyle(ink)
                }
            }
            .tint(inBook ? BookInk.rule : Palette.accentFill)
            .padding(.horizontal, Space.m)
            .padding(.vertical, 8)
            .frame(minHeight: 48)

            if isOn {
                RowDivider()
                DatePicker(selection: timeBinding, displayedComponents: .hourAndMinute) {
                    HStack(spacing: Space.m) {
                        Image(systemName: "clock")
                            .font(.system(size: 17))
                            .foregroundStyle(accent)
                            .frame(width: 26)
                            .accessibilityHidden(true)
                        Text("Time")
                            .foregroundStyle(ink)
                    }
                }
                .tint(inBook ? BookInk.rule : Palette.accentFill)
                .padding(.horizontal, Space.m)
                .padding(.vertical, 6)
                .frame(minHeight: 48)
                .transition(.opacity)
            }

            if denied {
                RowDivider()
                VStack(alignment: .leading, spacing: Space.s) {
                    Text("Notifications are off.")
                        .font(.subheadline)
                        .foregroundStyle(inBook ? BookInk.caption : Palette.textSecondary)
                    Button("Open Settings") {
                        if let url = URL(string: UIApplication.openSettingsURLString) { openURL(url) }
                    }
                    .buttonStyle(.tintedCompact)
                }
                .padding(Space.m)
                .frame(maxWidth: .infinity, alignment: .leading)
                .transition(reduceMotion ? AnyTransition.opacity : AnyTransition.move(edge: .top).combined(with: .opacity))
            }
        }
        .animation(reduceMotion ? nil : Motion.gentle, value: isOn)
        .animation(reduceMotion ? nil : Motion.smooth, value: denied)
        .sensoryFeedback(.impact(flexibility: .soft, intensity: 0.7), trigger: isOn) { _, new in hapticsOn && new }
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
            let ok = await reminder.enable(atMinutes: minutes)
            isOn = ok
            denied = !ok
        } else {
            reminder.disable()
            isOn = false
            denied = false
        }
    }
}

/// The same reminder as rows in a settings form, for screens built with Form.
struct GentleReminderFormSection: View {
    let reminder: GentleReminder
    let title: String

    @AppStorage private var isOn: Bool
    @AppStorage private var minutes: Int
    @AppStorage(PrefKey.haptics) private var hapticsOn = true
    @Environment(\.openURL) private var openURL
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var denied = false

    init(_ reminder: GentleReminder, title: String) {
        self.reminder = reminder
        self.title = title
        _isOn = AppStorage(wrappedValue: false, reminder.enabledKey)
        _minutes = AppStorage(wrappedValue: reminder.defaultMinutes, reminder.minutesKey)
    }

    var body: some View {
        Section {
            Toggle(isOn: Binding(
                get: { isOn },
                set: { newValue in Task { await setEnabled(newValue) } }
            )) {
                Label {
                    Text(title)
                } icon: {
                    Image(systemName: isOn ? "bell.fill" : "bell")
                        .foregroundStyle(BookInk.rule)
                        .contentTransition(.symbolEffect(.replace))
                        .symbolEffect(.bounce, value: reduceMotion ? false : isOn)
                        .accessibilityHidden(true)
                }
            }
            .sensoryFeedback(.impact(flexibility: .soft, intensity: 0.7), trigger: isOn) { _, new in hapticsOn && new }

            if isOn {
                DatePicker(selection: timeBinding, displayedComponents: .hourAndMinute) {
                    Label("Time", systemImage: "clock")
                }
            }

            if denied {
                VStack(alignment: .leading, spacing: Space.s) {
                    Text("Notifications are off.")
                        .font(.subheadline)
                        .foregroundStyle(BookInk.caption)
                    Button("Open Settings") {
                        if let url = URL(string: UIApplication.openSettingsURLString) { openURL(url) }
                    }
                    .buttonStyle(.tintedCompact)
                }
                .padding(.vertical, Space.xs)
            }
        }
        .listRowBackground(BookInk.pageLit)
    }

    private var time: Date {
        Calendar.current.date(bySettingHour: minutes / 60, minute: minutes % 60, second: 0, of: .now) ?? .now
    }

    private var timeBinding: Binding<Date> {
        Binding(
            get: { time },
            set: { date in
                let parts = Calendar.current.dateComponents([.hour, .minute], from: date)
                minutes = (parts.hour ?? 21) * 60 + (parts.minute ?? 0)
                Task { await setEnabled(true) }
            }
        )
    }

    private func setEnabled(_ on: Bool) async {
        if on {
            let ok = await reminder.enable(atMinutes: minutes)
            withAnimation(reduceMotion ? nil : Motion.gentle) {
                isOn = ok
                denied = !ok
            }
        } else {
            reminder.disable()
            withAnimation(reduceMotion ? nil : Motion.gentle) {
                isOn = false
                denied = false
            }
        }
    }
}
