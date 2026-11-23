import SwiftUI

/// The duas she opened last, newest first, as a row of small leaves she can scroll through.
/// Each leaf is dealt in, and zooms open into the dua.
struct RecentDuasRow: View {
    let duas: [Dua]

    var body: some View {
        VStack(alignment: .leading, spacing: Space.xs) {
            Text("Recent")
                .font(.title3.weight(.semibold))
                .foregroundStyle(BookInk.ink)
                .padding(.bottom, Space.xxs)
                .accessibilityAddTraits(.isHeader)

            ScrollView(.horizontal, showsIndicators: false) {
                LazyHStack(spacing: Space.s) {
                    ForEach(Array(duas.enumerated()), id: \.element.id) { index, dua in
                        NavigationLink(value: Route.dua(dua.id)) {
                            RecentDuaLeaf(dua: dua)
                                .zoomSource(ZoomID.dua(dua.id))
                        }
                        .buttonStyle(.pressable)
                        .dealIn(min(index, 6), baseDelay: 0.15, step: 0.05)
                    }
                }
                .padding(.horizontal, Space.page)
                .padding(.vertical, 2)
            }
            .padding(.horizontal, -Space.page)
            .scrollClipDisabled()
        }
    }
}

private struct RecentDuaLeaf: View {
    let dua: Dua

    var body: some View {
        VStack(alignment: .leading, spacing: Space.xs) {
            Text(dua.title)
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(BookInk.ink)
                .lineLimit(2)
                .multilineTextAlignment(.leading)
                .fixedSize(horizontal: false, vertical: true)
            Spacer(minLength: 0)
            Text(dua.source)
                .font(.caption)
                .foregroundStyle(BookInk.caption)
                .lineLimit(1)
        }
        .frame(width: 150, height: 92, alignment: .topLeading)
        .cardSurface(padding: Space.s)
        .accessibilityElement(children: .combine)
    }
}
