import SwiftUI

extension ZoomID {
    static let readingHistory = "readingHistory"
}

/// The last places she opened in the reader, newest first. Rows deal in,
/// and clearing folds them away.
struct ReadingHistoryView: View {
    @Environment(QuranStore.self) private var quran
    @Environment(LibraryStore.self) private var library
    @Environment(AppRouter.self) private var router
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @AppStorage(PrefKey.haptics) private var hapticsOn = true

    @State private var confirmClear = false
    @State private var cleared = 0

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: Space.l) {
                if library.history.isEmpty {
                    SavedEmpty(symbol: "clock.arrow.circlepath", title: "No history",
                               action: "Open the Quran") { router.tab = .quran }
                        .transition(.opacity)
                } else {
                    GroupedSection {
                        ForEach(Array(library.history.enumerated()), id: \.element.id) { index, visit in
                            if index > 0 { RowDivider(inset: 70) }
                            NavigationLink(value: Route.reader(surah: visit.ref.surah, verse: visit.ref.ayah)) {
                                HistoryRow(visit: visit)
                                    .zoomSource(ZoomID.reader(visit.ref.surah, visit.ref.ayah))
                            }
                            .dealIn(min(index, 8), baseDelay: 0.08, step: 0.05)
                            .transition(.opacity)
                        }
                    }
                    .scrollSettle()
                    .transition(.opacity)
                }
            }
            .padding(.horizontal, Space.page)
            .padding(.vertical, Space.s)
            .animation(reduceMotion ? nil : .spring(response: 0.45, dampingFraction: 0.85), value: library.history)
        }
        .pageBackground()
        .navigationTitle("History")
        .toolbar {
            if !library.history.isEmpty {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Clear") { confirmClear = true }
                }
            }
        }
        .confirmationDialog("Clear history?", isPresented: $confirmClear, titleVisibility: .visible) {
            Button("Clear History", role: .destructive) {
                withAnimation(reduceMotion ? nil : .spring(response: 0.45, dampingFraction: 0.85)) {
                    library.clearHistory()
                }
                cleared += 1
            }
        }
        .sensoryFeedback(.impact(flexibility: .soft, intensity: 0.6), trigger: cleared) { _, _ in hapticsOn }
    }
}

/// One place: the surah, the verse, and when she opened it.
private struct HistoryRow: View {
    let visit: ReadingVisit

    @Environment(QuranStore.self) private var quran

    var body: some View {
        let surah = quran.surah(visit.ref.surah)
        HStack(spacing: Space.m) {
            Text("\(surah.id)")
                .font(.subheadline.weight(.semibold).monospacedDigit())
                .foregroundStyle(Palette.accent)
                .frame(width: 38, height: 38)
                .background(RoundedRectangle(cornerRadius: 11, style: .continuous).fill(Palette.accentWash))
                .accessibilityHidden(true)
            VStack(alignment: .leading, spacing: 3) {
                Text(surah.name)
                    .font(.headline)
                    .foregroundStyle(Palette.textPrimary)
                Text("Verse \(visit.ref.ayah) of \(surah.verseCount)")
                    .font(.subheadline.monospacedDigit())
                    .foregroundStyle(Palette.textSecondary)
            }
            Spacer(minLength: Space.xs)
            Text(timeText)
                .font(.subheadline)
                .foregroundStyle(Palette.textSecondary)
                .lineLimit(1)
        }
        .padding(.horizontal, Space.m)
        .padding(.vertical, 10)
        .frame(minHeight: 56)
        .contentShape(Rectangle())
        .accessibilityElement(children: .combine)
        .accessibilityLabel("\(surah.name), verse \(visit.ref.ayah), \(timeText)")
    }

    private var timeText: String {
        let isToday = Calendar.current.isDateInToday(visit.date)
        return visit.date.formatted(date: isToday ? .omitted : .abbreviated, time: .shortened)
    }
}
