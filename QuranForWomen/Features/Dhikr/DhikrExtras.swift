import SwiftUI

// MARK: - Daily goal ring

/// A small ring for today's total against her daily goal. It fills gently with each count
/// and warms to gold, with one soft bloom, when the goal is reached.
struct DhikrGoalRing: View {
    let today: Int
    let goal: Int

    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var bloom = 0

    private let gold = Color(hex: 0xC9A24A)
    private var progress: Double { goal > 0 ? min(Double(today) / Double(goal), 1) : 0 }
    private var reached: Bool { goal > 0 && today >= goal }

    var body: some View {
        HStack(spacing: Space.xs) {
            ZStack {
                Circle()
                    .stroke(Palette.border, lineWidth: 4)
                Circle()
                    .trim(from: 0, to: progress)
                    .stroke(reached ? gold : Palette.accent, style: StrokeStyle(lineWidth: 4, lineCap: .round))
                    .rotationEffect(.degrees(-90))
                    .animation(reduceMotion ? nil : Motion.gentle, value: progress)
                    .animation(reduceMotion ? nil : .smooth(duration: 0.5), value: reached)
                if reached {
                    Image(systemName: "checkmark")
                        .font(.system(size: 11, weight: .bold))
                        .foregroundStyle(gold)
                        .transition(.scale.combined(with: .opacity))
                }
            }
            .frame(width: 28, height: 28)
            .keyframeAnimator(initialValue: 1.0, trigger: bloom) { ring, scale in
                ring.scaleEffect(reduceMotion ? 1 : scale)
            } keyframes: { _ in
                KeyframeTrack {
                    CubicKeyframe(1.25, duration: 0.14)
                    SpringKeyframe(1, duration: 0.45, spring: .bouncy)
                }
            }

            HStack(spacing: 3) {
                RollingNumber(value: today)
                Text("of \(goal)")
                    .monospacedDigit()
            }
            .font(.footnote.weight(.medium))
            .foregroundStyle(reached ? Palette.accent : Palette.textSecondary)
        }
        .padding(.horizontal, Space.s)
        .padding(.vertical, 6)
        .background(Capsule().fill(Palette.surface.opacity(0.85)))
        .animation(reduceMotion ? nil : Motion.reward, value: reached)
        .onChange(of: reached) { _, new in
            if new { bloom += 1 }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("Daily goal")
        .accessibilityValue("\(today) of \(goal) today")
    }
}

// MARK: - History

/// Her total dhikr counts for each of the last 30 days, as quiet bars. Nothing is marked
/// as missed. The bars rise in one after another, and tapping a bar shows that day.
struct DhikrHistoryView: View {
    @Environment(DhikrStore.self) private var store
    @Environment(\.dismiss) private var dismiss
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @AppStorage(PrefKey.haptics) private var hapticsOn = true

    @State private var selected: Date?
    @State private var risen = false

    var body: some View {
        let days = store.history()
        let last7 = days.suffix(7).reduce(0) { $0 + $1.total }
        let last30 = days.reduce(0) { $0 + $1.total }
        ScrollView {
            VStack(alignment: .leading, spacing: Space.xl) {
                if last30 == 0 {
                    SavedEmpty(symbol: "chart.bar", title: "No dhikr counted yet", action: "Count Dhikr") {
                        dismiss()
                    }
                } else {
                    chart(days)
                        .dealIn(0, baseDelay: 0.1)

                    GroupedSection {
                        ListRow(title: "Today", value: "\(store.total())", showsChevron: false)
                        RowDivider(inset: Space.m)
                        ListRow(title: "Last 7 days", value: "\(last7)", showsChevron: false)
                        RowDivider(inset: Space.m)
                        ListRow(title: "Last 30 days", value: "\(last30)", showsChevron: false)
                    }
                    .dealIn(1, baseDelay: 0.25)
                }
            }
            .padding(.horizontal, Space.page)
            .padding(.vertical, Space.s)
        }
        .pageBackground()
        .navigationTitle("History")
        .navigationBarTitleDisplayMode(.inline)
        .sensoryFeedback(.selection, trigger: selected) { _, _ in hapticsOn }
        .onAppear {
            guard !risen else { return }
            if reduceMotion {
                risen = true
            } else {
                withAnimation(.spring(response: 0.6, dampingFraction: 0.8).delay(0.2)) { risen = true }
            }
        }
    }

    private func chart(_ days: [(date: Date, total: Int)]) -> some View {
        let peak = max(days.map(\.total).max() ?? 0, 1)
        let shown = days.first { Calendar.current.isDate($0.date, inSameDayAs: selected ?? .now) } ?? days[days.count - 1]
        let isToday = Calendar.current.isDateInToday(shown.date)
        return VStack(alignment: .leading, spacing: Space.m) {
            VStack(alignment: .leading, spacing: 2) {
                Text(isToday ? "Today" : shown.date.formatted(.dateTime.weekday(.wide).month(.abbreviated).day()))
                    .font(.subheadline.weight(.medium))
                    .foregroundStyle(Palette.textSecondary)
                    .contentTransition(.opacity)
                RollingNumber(value: shown.total)
                    .font(.system(size: 34, weight: .light, design: .rounded))
                    .foregroundStyle(Palette.textPrimary)
            }
            .animation(reduceMotion ? nil : Motion.gentle, value: shown.date)
            .accessibilityElement(children: .combine)
            .focusIn(delay: 0.05)

            HStack(alignment: .bottom, spacing: 3) {
                ForEach(Array(days.enumerated()), id: \.offset) { index, day in
                    bar(day, index: index, peak: peak, isShown: Calendar.current.isDate(day.date, inSameDayAs: shown.date))
                }
            }
            .frame(height: 150, alignment: .bottom)

            HStack {
                Text(days[0].date.formatted(.dateTime.month(.abbreviated).day()))
                Spacer()
                Text("Today")
            }
            .font(.caption)
            .foregroundStyle(Palette.textSecondary)
            .accessibilityHidden(true)
        }
        .cardSurface(padding: Space.m)
    }

    private func bar(_ day: (date: Date, total: Int), index: Int, peak: Int, isShown: Bool) -> some View {
        let fraction = CGFloat(day.total) / CGFloat(peak)
        let height = day.total == 0 ? 3 : max(6, 150 * fraction)
        return Button {
            selected = day.date
        } label: {
            RoundedRectangle(cornerRadius: 3, style: .continuous)
                .fill(day.total == 0 ? Palette.border : (isShown ? Palette.accent : Palette.accent.opacity(0.35)))
                .frame(height: risen ? height : 3)
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottom)
                .contentShape(Rectangle())
                .animation(
                    reduceMotion ? nil : .spring(response: 0.55, dampingFraction: 0.78).delay(Double(index) * 0.018),
                    value: risen
                )
                .animation(reduceMotion ? nil : Motion.press, value: isShown)
        }
        .buttonStyle(.plain)
        .accessibilityLabel(day.date.formatted(date: .abbreviated, time: .omitted))
        .accessibilityValue("\(day.total)")
        .accessibilityAddTraits(isShown ? .isSelected : [])
    }
}

// MARK: - Her own dhikr

/// Dhikr phrases in her own words. Marked as personal, kept apart from the sourced list.
/// A new phrase glows for a moment, and a deleted one can be brought back with Undo.
struct CustomDhikrView: View {
    @Environment(DhikrStore.self) private var store
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @AppStorage(PrefKey.dhikrSelection) private var selectionID = "subhanallah"
    @AppStorage(PrefKey.haptics) private var hapticsOn = true

    @State private var editing: CustomDhikr?
    @State private var isAdding = false
    @State private var glowing: UUID?
    @State private var glowToken = 0
    @State private var undoItem: (item: CustomDhikr, index: Int)?
    @State private var undoToken = 0
    @State private var addedToken = 0

    var body: some View {
        List {
            if store.custom.isEmpty {
                SavedEmpty(symbol: "pencil.and.scribble", title: "No dhikr of your own yet",
                           action: "Add Dhikr") { isAdding = true }
                    .listRowBackground(Color.clear)
                    .listRowSeparator(.hidden)
            } else {
                Section {
                    ForEach(Array(store.custom.enumerated()), id: \.element.id) { index, item in
                        Button {
                            editing = item
                        } label: {
                            CustomDhikrRow(item: item)
                                .dealIn(min(index, 8), baseDelay: 0.15, step: 0.05)
                        }
                        .accessibilityHint("Opens this dhikr to edit")
                        .listRowBackground(
                            ZStack {
                                Palette.surface
                                Palette.blush.opacity(glowing == item.id ? 0.9 : 0)
                            }
                        )
                    }
                    .onDelete { offsets in
                        if let index = offsets.first, store.custom.indices.contains(index) {
                            delete(store.custom[index], at: index)
                        }
                    }
                }
            }
        }
        .scrollContentBackground(.hidden)
        .pageBackground()
        .animation(reduceMotion ? nil : .spring(response: 0.45, dampingFraction: 0.85), value: store.custom)
        .navigationTitle("Your Own Dhikr")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button { isAdding = true } label: {
                    Label("Add Dhikr", systemImage: "plus")
                        .symbolEffect(.bounce, value: reduceMotion ? 0 : addedToken)
                }
            }
        }
        .overlay(alignment: .bottom) {
            if undoItem != nil {
                UndoBar(text: "Dhikr deleted") { undoDelete() }
                    .padding(.horizontal, Space.page)
                    .padding(.bottom, Space.m)
                    .transition(.move(edge: .bottom).combined(with: .opacity))
            }
        }
        .animation(reduceMotion ? nil : .spring(response: 0.4, dampingFraction: 0.8), value: undoItem?.item.id)
        .sensoryFeedback(.success, trigger: addedToken) { _, _ in hapticsOn }
        .sensoryFeedback(.impact(flexibility: .soft, intensity: 0.6), trigger: undoToken) { _, _ in hapticsOn }
        .sheet(isPresented: $isAdding) {
            CustomDhikrEditor(initialText: "", initialTarget: 33) { text, target in
                var added: CustomDhikr?
                withAnimation(reduceMotion ? nil : .spring(response: 0.5, dampingFraction: 0.82)) {
                    added = store.addCustom(text: text, target: target)
                }
                if let added {
                    addedToken += 1
                    selectionID = added.countID
                    glow(added.id)
                }
            }
        }
        .sheet(item: $editing) { item in
            CustomDhikrEditor(initialText: item.text, initialTarget: item.target) { text, target in
                withAnimation(reduceMotion ? nil : Motion.gentle) {
                    store.updateCustom(item, text: text, target: target)
                }
                glow(item.id)
            }
        }
    }

    private func glow(_ id: UUID) {
        glowToken += 1
        let token = glowToken
        if reduceMotion {
            glowing = id
        } else {
            withAnimation(.easeOut(duration: 0.35).delay(0.3)) { glowing = id }
        }
        Task {
            try? await Task.sleep(for: .seconds(1.6))
            guard token == glowToken else { return }
            withAnimation(reduceMotion ? nil : .easeInOut(duration: 1.0)) { glowing = nil }
        }
    }

    private func delete(_ item: CustomDhikr, at index: Int) {
        withAnimation(reduceMotion ? nil : .spring(response: 0.45, dampingFraction: 0.85)) {
            store.removeCustom(item)
            undoItem = (item: item, index: index)
        }
        undoToken += 1
        let token = undoToken
        Task {
            try? await Task.sleep(for: .seconds(4))
            if token == undoToken {
                withAnimation(reduceMotion ? nil : Motion.gentle) { undoItem = nil }
            }
        }
    }

    private func undoDelete() {
        guard let pending = undoItem else { return }
        withAnimation(reduceMotion ? nil : Motion.gentle) {
            store.restoreCustom(pending.item, at: pending.index)
            undoItem = nil
        }
        undoToken += 1
        glow(pending.item.id)
    }
}

private struct CustomDhikrRow: View {
    let item: CustomDhikr

    var body: some View {
        HStack(alignment: .center, spacing: Space.m) {
            VStack(alignment: .leading, spacing: Space.xs) {
                if ArabicSearch.containsArabic(item.text) {
                    ArabicText(text: item.text, size: 22, lineLimit: 2)
                } else {
                    Text(item.text)
                        .font(AppFont.serif(.body))
                        .foregroundStyle(Palette.textPrimary)
                        .lineLimit(3)
                        .multilineTextAlignment(.leading)
                }
                SourceLabel(kind: .personal, reference: "Your own words")
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            Text(item.target > 0 ? "\(item.target)" : "No target")
                .font(.subheadline)
                .monospacedDigit()
                .foregroundStyle(Palette.textSecondary)
        }
        .padding(.vertical, 4)
        .contentShape(Rectangle())
        .accessibilityElement(children: .combine)
    }
}

/// Writing or editing her own dhikr phrase and its target.
private struct CustomDhikrEditor: View {
    let initialText: String
    let initialTarget: Int
    let onSave: (String, Int) -> Void

    @Environment(\.dismiss) private var dismiss
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var text = ""
    @State private var target = 33
    @FocusState private var focused: Bool

    private let targets = [33, 99, 100, 0]

    var body: some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: Space.l) {
                SourceLabel(kind: .personal, reference: "Your own words")
                    .focusIn(delay: 0.05)

                TextField("Your words", text: $text, axis: .vertical)
                    .font(AppFont.serif(.title3))
                    .lineLimit(2...6)
                    .padding(Space.m)
                    .background(RoundedRectangle(cornerRadius: Radius.control, style: .continuous).fill(Palette.surface))
                    .overlay(
                        RoundedRectangle(cornerRadius: Radius.control, style: .continuous)
                            .strokeBorder(focused ? Palette.accent.opacity(0.55) : Palette.border, lineWidth: focused ? 1.5 : 1)
                    )
                    .animation(reduceMotion ? nil : Motion.gentle, value: focused)
                    .focused($focused)
                    .focusIn(delay: 0.1)

                VStack(alignment: .leading, spacing: Space.xs) {
                    Text("Target")
                        .font(.footnote.weight(.semibold))
                        .foregroundStyle(Palette.textSecondary)
                    Picker("Target", selection: $target) {
                        ForEach(targets, id: \.self) { value in
                            Text(value == 0 ? "None" : "\(value)").tag(value)
                        }
                    }
                    .pickerStyle(.segmented)
                }
                .dealIn(0, baseDelay: 0.2)

                Spacer(minLength: 0)
            }
            .padding(Space.page)
            .pageBackground()
            .navigationTitle(initialText.isEmpty ? "New Dhikr" : "Edit Dhikr")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        onSave(text, target)
                        dismiss()
                    }
                    .disabled(text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                }
            }
        }
        .presentationDetents([.medium, .large])
        .onAppear {
            text = initialText
            target = targets.contains(initialTarget) ? initialTarget : 33
            focused = true
        }
    }
}
