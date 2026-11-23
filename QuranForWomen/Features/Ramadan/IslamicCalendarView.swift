import SwiftUI

/// The Islamic calendar: Hijri months with their Gregorian dates, the widely known days marked,
/// and her fasts. Swipe or use the arrows to move between months. Tap a day to mark a fast.
struct IslamicCalendarView: View {
    @Environment(FastingStore.self) private var fasting
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @AppStorage(PrefKey.haptics) private var hapticsOn = true

    @State private var today = Date.now
    @State private var index = Hijri.index(of: .now)
    /// Which way the month slides when she changes it.
    @State private var forward = true
    @State private var picked: PickedDay?

    private var current: Int { Hijri.index(of: today) }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: Space.xl) {
                monthCard
                    .dealIn(0, baseDelay: 0.02)
                    .scrollSettle()

                if !specials.isEmpty {
                    GroupedSection(title: "This month") {
                        ForEach(Array(specials.enumerated()), id: \.element) { i, special in
                            if i > 0 { RowDivider(inset: 40) }
                            CalendarSpecialRow(special: special, monthName: Hijri.monthName(Hijri.monthOf(index)))
                        }
                    }
                    .dealIn(1, baseDelay: 0.25)
                    .scrollSettle()
                }

                fastingSection
                    .dealIn(2, baseDelay: 0.25)
                    .scrollSettle()

                Text("Dates can differ by a day where you live.")
                    .font(.footnote)
                    .foregroundStyle(Palette.textSecondary)
                    .padding(.horizontal, Space.m)
                    .focusIn(delay: 0.5)
            }
            .padding(.horizontal, Space.page)
            .padding(.vertical, Space.s)
        }
        .pageBackground()
        .navigationTitle("Islamic Calendar")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                if index != current {
                    Button("Today") { go(to: current) }
                        .transition(.opacity)
                }
            }
        }
        .sheet(item: $picked) { day in
            CalendarFastSheet(date: day.date)
                .presentationDetents([.medium, .large])
                .presentationDragIndicator(.visible)
        }
        .sensoryFeedback(.selection, trigger: index) { _, _ in hapticsOn }
        .sensoryFeedback(.success, trigger: fasting.fasts) { old, new in hapticsOn && new.count >= old.count && new != old }
        .onAppear { today = .now }
    }

    // MARK: Month

    private var slide: AnyTransition {
        reduceMotion ? .opacity : .push(from: forward ? .trailing : .leading).combined(with: .opacity)
    }

    private func shift(_ by: Int) { go(to: index + by) }

    private func go(to target: Int) {
        forward = target > index
        withAnimation(reduceMotion ? nil : .spring(response: 0.45, dampingFraction: 0.86)) { index = target }
    }

    private var monthCard: some View {
        VStack(spacing: Space.xs) {
            HStack {
                Button { shift(-1) } label: {
                    Image(systemName: "chevron.left").frame(width: 44, height: 44)
                }
                .accessibilityLabel("Previous month")
                Spacer()
                ZStack {
                    VStack(spacing: 2) {
                        Text(Self.title(index))
                            .font(.headline)
                            .foregroundStyle(Palette.textPrimary)
                        Text(Self.gregorianRange(index))
                            .font(.footnote)
                            .foregroundStyle(Palette.textSecondary)
                    }
                    .id(index)
                    .transition(slide)
                }
                .clipped()
                .accessibilityElement(children: .combine)
                .accessibilityAddTraits(.isHeader)
                Spacer()
                Button { shift(1) } label: {
                    Image(systemName: "chevron.right").frame(width: 44, height: 44)
                }
                .accessibilityLabel("Next month")
            }
            .foregroundStyle(Palette.accent)
            .buttonStyle(PressableStyle(scale: 0.9))
            .padding(.horizontal, Space.xs)
            .focusIn(delay: 0.1)

            weekdayHeader

            ZStack {
                HijriMonthGrid(index: index, today: today) { date in
                    picked = PickedDay(date: date)
                }
                .id(index)
                .transition(slide)
            }
            .clipped()
        }
        .padding(.vertical, Space.s)
        .background(RoundedRectangle(cornerRadius: Radius.tile, style: .continuous).fill(Palette.surface))
        .contentShape(Rectangle())
        // Swipe the card to move between months. Vertical scrolling is left alone.
        .simultaneousGesture(
            DragGesture(minimumDistance: 30)
                .onEnded { value in
                    guard abs(value.translation.width) > abs(value.translation.height) * 1.5 else { return }
                    shift(value.translation.width < 0 ? 1 : -1)
                }
        )
    }

    private var weekdayHeader: some View {
        let cal = Calendar.current
        let symbols = cal.veryShortStandaloneWeekdaySymbols
        let first = cal.firstWeekday - 1
        return HStack(spacing: 0) {
            ForEach(0..<7, id: \.self) { i in
                Text(symbols[(first + i) % 7])
                    .font(.caption)
                    .foregroundStyle(Palette.textSecondary)
                    .frame(maxWidth: .infinity)
            }
        }
        .padding(.horizontal, Space.s)
        .accessibilityHidden(true)
    }

    // MARK: Known days

    private var specials: [CalendarSpecial] { Self.specialDays(index) }

    /// This month's widely known days. The last ten nights are listed once, where they begin.
    static func specialDays(_ index: Int) -> [CalendarSpecial] {
        let month = Hijri.monthOf(index)
        let start = Hijri.start(index)
        return (1...Hijri.length(index)).compactMap { day in
            guard let title = Hijri.specialDay(month: month, day: day) else { return nil }
            if month == Hijri.ramadan && day > 21 { return nil }
            guard let date = Calendar.current.date(byAdding: .day, value: day - 1, to: start) else { return nil }
            return CalendarSpecial(title: title, day: day, date: date)
        }
    }

    static func title(_ index: Int) -> String {
        "\(Hijri.monthName(Hijri.monthOf(index))) \(Hijri.yearOf(index))"
    }

    /// "Feb · Mar 2027", the Gregorian months a Hijri month covers.
    static func gregorianRange(_ index: Int) -> String {
        let cal = Calendar.current
        let start = Hijri.start(index)
        let end = cal.date(byAdding: .day, value: Hijri.length(index) - 1, to: start) ?? start
        let m1 = start.formatted(.dateTime.month(.abbreviated))
        let m2 = end.formatted(.dateTime.month(.abbreviated))
        let y1 = cal.component(.year, from: start)
        let y2 = cal.component(.year, from: end)
        if y1 != y2 { return "\(m1) \(y1) · \(m2) \(y2)" }
        if cal.component(.month, from: start) == cal.component(.month, from: end) { return "\(m1) \(y1)" }
        return "\(m1) · \(m2) \(y2)"
    }

    // MARK: Fasting

    private var fastingSection: some View {
        let hijriYear = Hijri.of(today).year
        let year = Calendar.current.component(.year, from: today)
        return GroupedSection(title: "Fasting") {
            countRow("This Ramadan", symbol: "moon.stars", value: fasting.ramadanCount(hijriYear: hijriYear), index: 0)
            RowDivider()
            countRow("Voluntary this year", symbol: "sparkles", value: fasting.voluntaryCount(year: year), index: 1)
            RowDivider()
            HStack(spacing: Space.m) {
                Image(systemName: "arrow.counterclockwise")
                    .font(.system(size: 17))
                    .foregroundStyle(Palette.accent)
                    .frame(width: 26)
                    .accessibilityHidden(true)
                Stepper(value: Binding(
                    get: { fasting.makeUpLeft },
                    set: { fasting.setMakeUpLeft($0) }
                ), in: 0...999) {
                    HStack {
                        Text("Fasts to make up")
                            .foregroundStyle(Palette.textPrimary)
                        Spacer(minLength: Space.xs)
                        RollingNumber(value: fasting.makeUpLeft, delay: 0.4)
                            .foregroundStyle(Palette.textSecondary)
                    }
                }
                .sensoryFeedback(.selection, trigger: fasting.makeUpLeft) { _, _ in hapticsOn }
            }
            .padding(.horizontal, Space.m)
            .frame(minHeight: 52)
        }
    }

    private func countRow(_ title: String, symbol: String, value: Int, index: Int) -> some View {
        HStack(spacing: Space.m) {
            Image(systemName: symbol)
                .font(.system(size: 17))
                .foregroundStyle(Palette.accent)
                .frame(width: 26)
                .accessibilityHidden(true)
            Text(title)
                .foregroundStyle(Palette.textPrimary)
            Spacer()
            RollingNumber(value: value, delay: 0.35 + Double(index) * 0.06)
                .foregroundStyle(Palette.textSecondary)
        }
        .padding(.horizontal, Space.m)
        .frame(minHeight: 48)
        .accessibilityElement(children: .combine)
    }
}

/// A day she tapped, for the sheet.
private struct PickedDay: Identifiable {
    let date: Date
    var id: Date { date }
}

struct CalendarSpecial: Hashable {
    let title: String
    let day: Int
    let date: Date
}

private let calendarGold = Color(hex: 0xC9A24A)

private struct CalendarSpecialRow: View {
    let special: CalendarSpecial
    let monthName: String

    var body: some View {
        HStack(spacing: Space.m) {
            Circle()
                .fill(calendarGold)
                .frame(width: 8, height: 8)
                .accessibilityHidden(true)
            VStack(alignment: .leading, spacing: 2) {
                Text(special.title)
                    .foregroundStyle(Palette.textPrimary)
                Text("\(special.day) \(monthName) · \(special.date.formatted(.dateTime.weekday(.abbreviated).month(.abbreviated).day()))")
                    .font(.subheadline)
                    .foregroundStyle(Palette.textSecondary)
            }
            Spacer(minLength: 0)
        }
        .padding(.horizontal, Space.m)
        .padding(.vertical, 10)
        .frame(minHeight: 48)
        .accessibilityElement(children: .combine)
    }
}

// MARK: - Grid

private struct HijriMonthGrid: View {
    let index: Int
    let today: Date
    let pick: (Date) -> Void

    var body: some View {
        let cal = Calendar.current
        let start = Hijri.start(index)
        let length = Hijri.length(index)
        let month = Hijri.monthOf(index)
        let lead = (cal.component(.weekday, from: start) - cal.firstWeekday + 7) % 7
        let rows = (lead + length + 6) / 7
        VStack(spacing: 0) {
            ForEach(0..<rows, id: \.self) { row in
                HStack(spacing: 0) {
                    ForEach(0..<7, id: \.self) { column in
                        cell(row * 7 + column, lead: lead, length: length, start: start, month: month)
                            .frame(maxWidth: .infinity)
                            .frame(height: 54)
                    }
                }
            }
        }
        .padding(.horizontal, Space.s)
    }

    @ViewBuilder
    private func cell(_ slot: Int, lead: Int, length: Int, start: Date, month: Int) -> some View {
        let n = slot - lead
        if n >= 0 && n < length, let date = Calendar.current.date(byAdding: .day, value: n, to: start) {
            HijriDayCell(date: date, hijriDay: n + 1, month: month, today: today, slot: slot, pick: pick)
        } else {
            Color.clear
        }
    }
}

/// One day: the Hijri number, the Gregorian number under it, a gold dot on a widely known day,
/// and her fast as a filled circle (Ramadan), a ring (voluntary) or a soft wash (making up).
/// Rest days carry a soft blush. Days without a fast stay plain.
private struct HijriDayCell: View {
    let date: Date
    let hijriDay: Int
    let month: Int
    let today: Date
    let slot: Int
    let pick: (Date) -> Void

    @Environment(FastingStore.self) private var fasting
    @Environment(LibraryStore.self) private var library
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        let cal = Calendar.current
        let kind = fasting.kind(on: date)
        let day = cal.startOfDay(for: date)
        let isToday = cal.isDate(date, inSameDayAs: today)
        let canMark = day <= cal.startOfDay(for: today)
        let special = Hijri.specialDay(month: month, day: hijriDay)
        let resting = library.restSince.map { day >= cal.startOfDay(for: $0) && canMark } ?? false
        let shape = RoundedRectangle(cornerRadius: 12, style: .continuous)

        Button { pick(date) } label: {
            ZStack {
                if resting {
                    shape.fill(Palette.blush.opacity(0.6))
                }
                ZStack {
                    if let kind {
                        mark(kind)
                            .transition(reduceMotion ? .opacity : .scale(scale: 0.6).combined(with: .opacity))
                    }
                }
                .frame(width: 40, height: 40)
                .animation(reduceMotion ? nil : .spring(response: 0.35, dampingFraction: 0.6), value: kind)

                VStack(spacing: 0) {
                    Text("\(hijriDay)")
                        .font(.callout.weight(isToday ? .bold : .medium))
                        .foregroundStyle(kind == .ramadan ? Palette.onAccentFill : (isToday ? Palette.accent : Palette.textPrimary))
                    Text("\(cal.component(.day, from: date))")
                        .font(.system(size: 10))
                        .foregroundStyle(kind == .ramadan ? Palette.onAccentFill.opacity(0.8) : Palette.textSecondary)
                }
                .monospacedDigit()

                if special != nil {
                    Circle()
                        .fill(calendarGold)
                        .frame(width: 4, height: 4)
                        .frame(maxHeight: .infinity, alignment: .bottom)
                        .padding(.bottom, 2)
                }
                if isToday {
                    shape.strokeBorder(Palette.accent, lineWidth: 1.5)
                }
            }
            .frame(width: 48, height: 48)
            .contentShape(shape)
            .overlay { PetalBurst(trigger: kind != nil, active: kind != nil) }
        }
        .buttonStyle(PressableStyle(scale: 0.9))
        .disabled(!canMark)
        .dealIn(min(slot, 41), baseDelay: 0.12, step: 0.008)
        .accessibilityLabel(label(kind: kind, special: special, isToday: isToday, resting: resting))
        .accessibilityHint(canMark ? "Mark a fast" : "")
    }

    @ViewBuilder
    private func mark(_ kind: FastKind) -> some View {
        switch kind {
        case .ramadan: Circle().fill(Palette.accentFill)
        case .voluntary: Circle().strokeBorder(Palette.accent, lineWidth: 1.5)
        case .makeUp: Circle().fill(Palette.accentWash)
        }
    }

    private func label(kind: FastKind?, special: String?, isToday: Bool, resting: Bool) -> String {
        [
            "\(hijriDay) \(Hijri.monthName(month)), \(date.formatted(date: .complete, time: .omitted))",
            kind?.title,
            special,
            isToday ? "Today" : nil,
            resting ? "Rest days" : nil,
        ]
        .compactMap { $0 }
        .joined(separator: ". ")
    }
}

// MARK: - Mark a fast

private struct CalendarFastSheet: View {
    let date: Date

    @Environment(FastingStore.self) private var fasting
    @Environment(\.dismiss) private var dismiss
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        let h = Hijri.of(date)
        let current = fasting.kind(on: date)
        let options: [FastKind] = h.month == Hijri.ramadan ? FastKind.allCases : [.voluntary, .makeUp]

        ScrollView {
            VStack(alignment: .leading, spacing: Space.l) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(date.formatted(date: .complete, time: .omitted))
                        .font(.title3.weight(.semibold))
                        .foregroundStyle(Palette.textPrimary)
                        .accessibilityAddTraits(.isHeader)
                    Text("\(h.day) \(Hijri.monthName(h.month)) \(h.year)")
                        .font(.subheadline)
                        .foregroundStyle(Palette.textSecondary)
                }
                .focusIn(delay: 0.05)

                GroupedSection {
                    ForEach(Array(options.enumerated()), id: \.element) { i, kind in
                        if i > 0 { RowDivider() }
                        choice(kind.title, symbol: kind.symbol, selected: current == kind) { choose(kind) }
                            .dealIn(i, baseDelay: 0.08, step: 0.05)
                    }
                    RowDivider()
                    choice("No fast", symbol: "circle.dotted", selected: current == nil) { choose(nil) }
                        .dealIn(options.count, baseDelay: 0.08, step: 0.05)
                }
            }
            .padding(Space.page)
        }
        .pageBackground()
    }

    private func choice(_ title: String, symbol: String, selected: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: Space.m) {
                Image(systemName: symbol)
                    .font(.system(size: 17))
                    .foregroundStyle(Palette.accent)
                    .frame(width: 26)
                    .accessibilityHidden(true)
                Text(title)
                    .foregroundStyle(Palette.textPrimary)
                Spacer()
                if selected {
                    Image(systemName: "checkmark")
                        .font(.body.weight(.semibold))
                        .foregroundStyle(Palette.accent)
                        .accessibilityHidden(true)
                }
            }
            .padding(.horizontal, Space.m)
            .frame(minHeight: 52)
            .contentShape(Rectangle())
        }
        .accessibilityAddTraits(selected ? .isSelected : [])
    }

    private func choose(_ kind: FastKind?) {
        withAnimation(reduceMotion ? nil : .spring(response: 0.35, dampingFraction: 0.62)) {
            fasting.setFast(kind, on: date)
        }
        dismiss()
    }
}
