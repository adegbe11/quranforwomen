import SwiftUI
import UIKit

// MARK: - Pages

/// One page of the Madani mushaf: the verses printed on it.
struct MushafPage: Identifiable, Hashable {
    let number: Int
    let verses: [VerseRef]
    var id: Int { number }
}

extension QuranStore {
    /// The 604 pages of the Madani mushaf, built from the bundled page number of every verse.
    /// The text is the verified Uthmani text; only the grouping into pages comes from the mushaf.
    func mushafPages() -> [MushafPage] {
        var pages: [Int: [VerseRef]] = [:]
        for surah in surahs {
            for (index, page) in surah.pages.enumerated() {
                pages[page, default: []].append(VerseRef(surah: surah.id, ayah: index + 1))
            }
        }
        return pages.keys.sorted().map { MushafPage(number: $0, verses: pages[$0] ?? []) }
    }
}

private enum MushafKey {
    static let page = "mushaf.page"
    static let ribbon = "mushaf.ribbon"
}

/// The colours of a leather bound book, the same edition as PrayerKey's classic Bible:
/// red brown leather tooled in real gold, cream leaves, a red silk ribbon, on a dark desk.
private enum MushafInk {
    static let desk = Color(hex: 0x17130D)
    static let deskLit = Color(hex: 0x2A2016)
    static let paper = Color(hex: 0xF6EFDC)
    static let paperLit = Color(hex: 0xFDFAF0)
    static let paperEdge = Color(hex: 0xE6D9B6)
    static let ink = Color(hex: 0x221C12)
    static let caption = Color(hex: 0x5A4828)
    static let rule = Color(hex: 0x8C6E32)
    static let gild = Color(hex: 0xC9A26D)
    static let gold = Color(hex: 0xD4AF37)
    static let goldLight = Color(hex: 0xF3E5AB)
    static let goldDeep = Color(hex: 0x7A5A15)
    static let board = Color(hex: 0x5A1E18)
    static let boardDeep = Color(hex: 0x1B0807)
    static let boardLit = Color(hex: 0x87382A)
    static let ribbon = Color(hex: 0x9A1B2B)
    static let ribbonDeep = Color(hex: 0x8C1F2B)
}

// MARK: - The book

/// The Quran as a book. The closed cover comes first, a band of light passing over its gold.
/// It opens like a real mushaf, hinged on the right, with a warm light rising from the page beneath,
/// and the pages turn right to left with a real page curl. A ribbon drops in to keep her place,
/// the gilt edge riffles the actual pages under her finger, and closing swings the cover shut first.
struct MushafView: View {
    @Environment(QuranStore.self) private var quran
    @Environment(\.dismiss) private var dismiss
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @AppStorage(PrefKey.haptics) private var hapticsOn = true
    @AppStorage(MushafKey.page) private var savedPage = 1
    @AppStorage(MushafKey.ribbon) private var ribbonPage = 0

    @State private var pages: [MushafPage] = []
    @State private var index = 0
    @State private var opened = false
    @State private var coverAngle: Double = 0
    @State private var coverGone = false
    @State private var chrome = true
    @State private var scrubbing: Int?
    @State private var bloom = false
    @State private var closing = false

    var body: some View {
        ZStack {
            RadialGradient(colors: [MushafInk.deskLit, MushafInk.desk], center: .center, startRadius: 0, endRadius: 520)
                .ignoresSafeArea()

            if !pages.isEmpty {
                PageCurlView(pageCount: pages.count, index: $index, hapticsOn: hapticsOn, animatesTurns: scrubbing == nil) { i in
                    AnyView(
                        MushafPageView(page: pages[i], quran: quran, hasRibbon: ribbonPage == pages[i].number)
                            .onTapGesture { withAnimation(.smooth(duration: 0.3)) { chrome.toggle() } }
                    )
                }
                .padding(.horizontal, Space.xs)
                // The page keeps still and leaves room for the bar and the gilt edge,
                // which fade in and out in the margins around it, as in Apple Books.
                .padding(.top, 52)
                .padding(.bottom, 56)
                .ignoresSafeArea(edges: .bottom)
                .opacity(opened ? 1 : 0)

                // The light that rises from the page as the book opens.
                OpeningLight(active: bloom)
                    .allowsHitTesting(false)
            }

            if !coverGone {
                MushafCover(shining: !opened && !reduceMotion)
                    // The cover is exactly the size of the pages it closes over.
                    .padding(.horizontal, Space.xs)
                    .padding(.top, 52)
                    .padding(.bottom, 56)
                    .ignoresSafeArea(edges: .bottom)
                    // Hinged on the right, like a mushaf, so the cover swings open to the right.
                    .rotation3DEffect(.degrees(coverAngle), axis: (x: 0, y: 1, z: 0), anchor: .trailing, perspective: 0.55)
                    .onTapGesture(perform: openBook)
                    .gesture(DragGesture(minimumDistance: 20).onEnded { value in
                        if value.translation.width > 40 { openBook() }
                    })
                    .accessibilityElement(children: .ignore)
                    .accessibilityLabel("The Holy Quran")
                    .accessibilityHint("Opens the mushaf")
                    .accessibilityAddTraits(.isButton)
                    .accessibilityAction { openBook() }
            }

            // The bar slides down from the top and the gilt edge rises from the bottom.
            VStack {
                if opened && chrome && !closing {
                    topBar
                        .transition(.move(edge: .top).combined(with: .opacity))
                }
                Spacer()
                if opened && chrome && !closing {
                    GiltEdge(count: pages.count, index: $index, scrubbing: $scrubbing, pages: pages, quran: quran, hapticsOn: hapticsOn)
                        .padding(.horizontal, Space.page)
                        .padding(.bottom, Space.s)
                        .transition(.move(edge: .bottom).combined(with: .opacity))
                }
            }
        }
        .statusBarHidden(opened && !chrome)
        .preferredColorScheme(.dark)
        .onAppear {
            if pages.isEmpty {
                pages = quran.mushafPages()
                index = pages.lastIndex { $0.number <= savedPage } ?? 0
            }
        }
        .onChange(of: index) { _, new in
            if pages.indices.contains(new) { savedPage = pages[new].number }
        }
        // Riffling: the page under her finger is the page she sees.
        .onChange(of: scrubbing) { _, new in
            if let new, new != index { index = new }
        }
        .sensoryFeedback(.impact(flexibility: .soft, intensity: 0.7), trigger: opened) { _, new in hapticsOn && new }
    }

    private var current: MushafPage? { pages.indices.contains(index) ? pages[index] : nil }

    private var topBar: some View {
        HStack(spacing: Space.s) {
            Button { closeBook() } label: {
                Image(systemName: "xmark")
                    .font(.system(size: 15, weight: .semibold))
                    .frame(width: 40, height: 40)
            }
            .readableGlass(Circle(), onDark: true)
            .accessibilityLabel("Close")

            Spacer()

            if let current, let first = current.verses.first {
                VStack(spacing: 0) {
                    Text(quran.surah(first.surah).name)
                        .font(.subheadline.weight(.semibold))
                    Text("Juz \(quran.juz(of: first)) · Page \(current.number)")
                        .font(.caption)
                        .monospacedDigit()
                        .contentTransition(.numericText(value: Double(current.number)))
                        .opacity(0.75)
                }
                .foregroundStyle(.white)
                .animation(.snappy, value: current.number)
            }

            Spacer()

            Menu {
                if let current {
                    if ribbonPage == current.number {
                        Button("Remove Ribbon", systemImage: "bookmark.slash") { ribbonPage = 0 }
                    } else {
                        Button("Place Ribbon Here", systemImage: "bookmark") { ribbonPage = current.number }
                    }
                }
                if ribbonPage > 0, ribbonPage != current?.number,
                   let target = pages.firstIndex(where: { $0.number == ribbonPage }) {
                    Button("Go to Ribbon, Page \(ribbonPage)", systemImage: "arrow.uturn.right") { index = target }
                }
            } label: {
                Image(systemName: ribbonPage == current?.number ? "bookmark.fill" : "bookmark")
                    .font(.system(size: 15, weight: .semibold))
                    .contentTransition(.symbolEffect(.replace))
                    .symbolEffect(.bounce, value: ribbonPage)
                    .frame(width: 40, height: 40)
            }
            .readableGlass(Circle(), onDark: true)
            .accessibilityLabel("Ribbon")
        }
        .foregroundStyle(.white)
        .padding(.horizontal, Space.page)
        .padding(.top, Space.xs)
        .sensoryFeedback(.selection, trigger: ribbonPage) { _, _ in hapticsOn }
    }

    private func openBook() {
        guard !opened else { return }
        if reduceMotion {
            withAnimation(.easeInOut(duration: 0.3)) { opened = true; coverGone = true }
            return
        }
        withAnimation(.easeOut(duration: 0.25)) { opened = true }
        withAnimation(.spring(response: 0.9, dampingFraction: 0.86)) { coverAngle = -100 }
        withAnimation(.easeOut(duration: 0.6).delay(0.15)) { bloom = true }
        withAnimation(.easeIn(duration: 0.9).delay(0.9)) { bloom = false }
        // Take the cover away once it has swung past edge on.
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.7) { if !closing { coverGone = true } }
    }

    /// The cover swings back over the pages, then the book closes.
    private func closeBook() {
        guard !closing else { return }
        if reduceMotion {
            dismiss()
            return
        }
        withAnimation(.smooth(duration: 0.25)) { closing = true }
        // Put the cover back open, without animation, so it is drawn at -100 first.
        var still = Transaction()
        still.disablesAnimations = true
        withTransaction(still) {
            coverAngle = -100
            coverGone = false
        }
        // Swing it shut on the next pass, once it has been drawn open.
        DispatchQueue.main.async {
            withAnimation(.spring(response: 0.6, dampingFraction: 0.9)) { coverAngle = 0 }
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.65) { dismiss() }
    }
}

// MARK: - Page turning

/// UIKit's real page curl. The spine is on the right, so pages lift from the left edge and
/// turn right to left, the way a mushaf is read.
private struct PageCurlView: UIViewControllerRepresentable {
    let pageCount: Int
    @Binding var index: Int
    let hapticsOn: Bool
    var animatesTurns = true
    let page: (Int) -> AnyView

    func makeCoordinator() -> Coordinator { Coordinator(self) }

    func makeUIViewController(context: Context) -> UIPageViewController {
        let controller = UIPageViewController(
            transitionStyle: .pageCurl,
            navigationOrientation: .horizontal,
            options: [.spineLocation: NSNumber(value: UIPageViewController.SpineLocation.max.rawValue)]
        )
        controller.isDoubleSided = false
        controller.dataSource = context.coordinator
        controller.delegate = context.coordinator
        controller.view.backgroundColor = .clear
        controller.setViewControllers([context.coordinator.host(index)], direction: .forward, animated: false)
        return controller
    }

    func updateUIViewController(_ controller: UIPageViewController, context: Context) {
        context.coordinator.parent = self
        guard let shown = controller.viewControllers?.first as? PageHost else { return }
        if shown.pageIndex == index {
            // Redraw the page on show, so a ribbon placed or removed appears at once.
            shown.rootView = page(index)
        } else {
            controller.setViewControllers(
                [context.coordinator.host(index)],
                direction: index > shown.pageIndex ? .forward : .reverse,
                animated: animatesTurns && abs(index - shown.pageIndex) == 1
            )
        }
    }

    final class Coordinator: NSObject, UIPageViewControllerDataSource, UIPageViewControllerDelegate {
        var parent: PageCurlView
        private let feedback = UIImpactFeedbackGenerator(style: .light)

        init(_ parent: PageCurlView) { self.parent = parent }

        func host(_ index: Int) -> PageHost {
            PageHost(index: index, root: parent.page(index))
        }

        func pageViewController(_ controller: UIPageViewController, viewControllerBefore viewController: UIViewController) -> UIViewController? {
            guard let host = viewController as? PageHost, host.pageIndex > 0 else { return nil }
            return self.host(host.pageIndex - 1)
        }

        func pageViewController(_ controller: UIPageViewController, viewControllerAfter viewController: UIViewController) -> UIViewController? {
            guard let host = viewController as? PageHost, host.pageIndex < parent.pageCount - 1 else { return nil }
            return self.host(host.pageIndex + 1)
        }

        func pageViewController(_ controller: UIPageViewController, didFinishAnimating finished: Bool, previousViewControllers: [UIViewController], transitionCompleted completed: Bool) {
            guard completed, let host = controller.viewControllers?.first as? PageHost else { return }
            parent.index = host.pageIndex
            if parent.hapticsOn { feedback.impactOccurred(intensity: 0.5) }
        }
    }
}

private final class PageHost: UIHostingController<AnyView> {
    let pageIndex: Int

    init(index: Int, root: AnyView) {
        pageIndex = index
        super.init(rootView: root)
        // The page is laid out by SwiftUI around it; keep UIKit safe area insets out of the leaf.
        safeAreaRegions = []
        view.backgroundColor = .clear
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) { fatalError("init(coder:) is not used") }
}

// MARK: - A page

/// One leaf: cream paper, a gold double frame with corner stars, the surah and juz at the top,
/// the verses justified right to left with their verse marks, and the page number in a medallion.
private struct MushafPageView: View {
    let page: MushafPage
    let quran: QuranStore
    let hasRibbon: Bool

    private enum Block: Hashable {
        case header(Int)
        case basmala
        case text(String)
    }

    /// Verses run together the way they are printed; a new surah starts with its band and the Basmala.
    private var blocks: [Block] {
        var result: [Block] = []
        var running = ""
        for ref in page.verses {
            if ref.ayah == 1 {
                if !running.isEmpty { result.append(.text(running)); running = "" }
                result.append(.header(ref.surah))
                if quran.surah(ref.surah).showsBasmala { result.append(.basmala) }
            }
            running += quran.text(ref) + " \u{FD3F}" + ref.ayah.arabicIndic + "\u{FD3E} "
        }
        if !running.isEmpty { result.append(.text(running)) }
        return result
    }

    var body: some View {
        let first = page.verses.first ?? VerseRef(surah: 1, ayah: 1)
        ZStack {
            RoundedRectangle(cornerRadius: 6, style: .continuous)
                .fill(LinearGradient(colors: [MushafInk.paperLit, MushafInk.paper, MushafInk.paperEdge], startPoint: .trailing, endPoint: .leading))
                .shadow(color: .black.opacity(0.35), radius: 10, x: -2, y: 4)

            VStack(spacing: 6) {
                HStack {
                    Text("الجزء \(quran.juz(of: first).arabicIndic)")
                    Spacer()
                    Text(quran.surah(first.surah).arabicName)
                }
                .font(AppFont.arabic(15))
                .foregroundStyle(MushafInk.caption)
                .environment(\.layoutDirection, .rightToLeft)
                .padding(.horizontal, 18)
                .padding(.top, 14)

                ZStack {
                    PageFrame()
                    ViewThatFits(in: .vertical) {
                        ForEach([25.0, 23, 21, 19.5, 18, 16.5, 15, 13.5, 12], id: \.self) { size in
                            layout(size: size)
                        }
                    }
                    .padding(.horizontal, 22)
                    .padding(.vertical, 16)
                }
                .padding(.horizontal, 10)

                Text(page.number.arabicIndic)
                    .font(AppFont.arabic(15))
                    .foregroundStyle(MushafInk.caption)
                    .frame(minWidth: 34, minHeight: 26)
                    .background(Capsule().strokeBorder(MushafInk.gild, lineWidth: 1))
                    .padding(.bottom, 12)
            }

            if hasRibbon {
                Ribbon()
                    .frame(width: 18, height: 90)
                    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
                    .padding(.leading, 34)
                    .transition(.move(edge: .top))
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("Page \(page.number), \(quran.surah(first.surah).name), from verse \(first.ayah)")
    }

    private func layout(size: CGFloat) -> some View {
        VStack(spacing: size * 0.45) {
            ForEach(Array(blocks.enumerated()), id: \.offset) { _, block in
                switch block {
                case .header(let id):
                    SurahBand(name: quran.surah(id).arabicName, size: size)
                case .basmala:
                    Text(quran.basmala)
                        // Fixed sizes on the page, so it stays in proportion like print.
                        .font(.custom(AppFont.arabicFamily, fixedSize: size))
                        .foregroundStyle(MushafInk.ink)
                        .frame(maxWidth: .infinity)
                case .text(let text):
                    JustifiedArabic(text: text, size: size)
                }
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
    }
}

/// Arabic set justified, right to left, like the lines of a printed mushaf.
private struct JustifiedArabic: UIViewRepresentable {
    let text: String
    let size: CGFloat

    private var attributed: NSAttributedString {
        let style = NSMutableParagraphStyle()
        style.alignment = .justified
        style.baseWritingDirection = .rightToLeft
        style.lineSpacing = size * 0.3
        let descriptor = UIFontDescriptor(fontAttributes: [.family: AppFont.arabicFamily])
        let font = UIFont(descriptor: descriptor, size: size)
        return NSAttributedString(string: text, attributes: [
            .font: font,
            .foregroundColor: UIColor(red: 0x22 / 255.0, green: 0x1C / 255.0, blue: 0x12 / 255.0, alpha: 1),
            .paragraphStyle: style,
        ])
    }

    func makeUIView(context: Context) -> UILabel {
        let label = UILabel()
        label.numberOfLines = 0
        label.semanticContentAttribute = .forceRightToLeft
        label.setContentCompressionResistancePriority(.defaultLow, for: .horizontal)
        label.isAccessibilityElement = false
        return label
    }

    func updateUIView(_ label: UILabel, context: Context) {
        label.attributedText = attributed
    }

    func sizeThatFits(_ proposal: ProposedViewSize, uiView: UILabel, context: Context) -> CGSize? {
        let width = proposal.width.flatMap { $0.isFinite && $0 > 0 ? $0 : nil } ?? 320
        let rect = attributed.boundingRect(
            with: CGSize(width: width, height: .greatestFiniteMagnitude),
            options: [.usesLineFragmentOrigin, .usesFontLeading],
            context: nil
        )
        return CGSize(width: width, height: ceil(rect.height) + 2)
    }
}

/// The band that opens a surah, in gold.
private struct SurahBand: View {
    let name: String
    let size: CGFloat

    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var lit = false

    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 4, style: .continuous)
                .fill(MushafInk.goldLight.opacity(0.4))
            RoundedRectangle(cornerRadius: 4, style: .continuous)
                .strokeBorder(MushafInk.gild, lineWidth: 1.2)
            RoundedRectangle(cornerRadius: 3, style: .continuous)
                .strokeBorder(MushafInk.rule.opacity(0.6), lineWidth: 0.6)
                .padding(3)
            HStack {
                StarMark().fill(MushafInk.gild).frame(width: size * 0.55, height: size * 0.55)
                Spacer()
                Text("سورة \(name)")
                    .font(.custom(AppFont.arabicFamily, fixedSize: size * 0.9))
                    .foregroundStyle(MushafInk.caption)
                Spacer()
                StarMark().fill(MushafInk.gild).frame(width: size * 0.55, height: size * 0.55)
            }
            .padding(.horizontal, 10)
        }
        .frame(height: size * 1.9)
        .overlay {
            GeometryReader { geo in
                LinearGradient(colors: [.clear, Color.white.opacity(0.55), .clear], startPoint: .leading, endPoint: .trailing)
                    .frame(width: geo.size.width * 0.3)
                    .offset(x: lit ? geo.size.width : -geo.size.width * 0.3)
            }
            .clipShape(RoundedRectangle(cornerRadius: 4, style: .continuous))
            .blendMode(.plusLighter)
            .allowsHitTesting(false)
            .opacity(reduceMotion ? 0 : 1)
        }
        .onAppear {
            guard !lit, !reduceMotion else { return }
            withAnimation(.easeInOut(duration: 1.1).delay(0.35)) { lit = true }
        }
        .accessibilityLabel("Surah \(name)")
    }
}

/// The gold double rule around the text, with a star in each corner.
private struct PageFrame: View {
    var body: some View {
        ZStack {
            Rectangle().strokeBorder(MushafInk.gild, lineWidth: 1.4)
            Rectangle().strokeBorder(MushafInk.rule.opacity(0.6), lineWidth: 0.7).padding(4)
            GeometryReader { geo in
                ForEach(0..<4, id: \.self) { corner in
                    StarMark()
                        .fill(MushafInk.gild)
                        .frame(width: 12, height: 12)
                        .position(
                            x: corner % 2 == 0 ? 2 : geo.size.width - 2,
                            y: corner < 2 ? 2 : geo.size.height - 2
                        )
                }
            }
        }
        .accessibilityHidden(true)
    }
}

/// An eight pointed star, the common ornament of mushaf borders.
struct StarMark: Shape {
    func path(in rect: CGRect) -> Path {
        let center = CGPoint(x: rect.midX, y: rect.midY)
        let outer = min(rect.width, rect.height) / 2
        let inner = outer * 0.48
        var path = Path()
        for i in 0..<16 {
            let angle = Double(i) * .pi / 8 - .pi / 2
            let r = i.isMultiple(of: 2) ? outer : inner
            let point = CGPoint(x: center.x + CGFloat(cos(angle)) * r, y: center.y + CGFloat(sin(angle)) * r)
            if i == 0 { path.move(to: point) } else { path.addLine(to: point) }
        }
        path.closeSubpath()
        return path
    }
}

/// The silk ribbon that keeps her place.
private struct Ribbon: View {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var dropped = false

    var body: some View {
        Path { p in
            p.move(to: .zero)
            p.addLine(to: CGPoint(x: 18, y: 0))
            p.addLine(to: CGPoint(x: 18, y: 90))
            p.addLine(to: CGPoint(x: 9, y: 80))
            p.addLine(to: CGPoint(x: 0, y: 90))
            p.closeSubpath()
        }
        .fill(LinearGradient(colors: [MushafInk.ribbon, MushafInk.ribbonDeep], startPoint: .leading, endPoint: .trailing))
        .shadow(color: .black.opacity(0.25), radius: 2, y: 1)
        .offset(y: dropped || reduceMotion ? 0 : -96)
        .rotationEffect(.degrees(dropped || reduceMotion ? 0 : -6), anchor: .top)
        .onAppear {
            guard !dropped else { return }
            withAnimation(.spring(response: 0.55, dampingFraction: 0.55)) { dropped = true }
        }
        .accessibilityHidden(true)
    }
}

/// A warm light that rises from the first page as the book opens, with a few gold specks.
private struct OpeningLight: View {
    let active: Bool

    var body: some View {
        ZStack {
            RadialGradient(colors: [MushafInk.goldLight.opacity(0.55), .clear], center: .center, startRadius: 0, endRadius: 260)
                .scaleEffect(active ? 1.15 : 0.6)
            ForEach(0..<10, id: \.self) { i in
                let x = CGFloat((i * 37) % 100) / 100 - 0.5
                Circle()
                    .fill(MushafInk.goldLight)
                    .frame(width: CGFloat(2 + i % 3), height: CGFloat(2 + i % 3))
                    .offset(x: x * 260, y: active ? -140 - CGFloat(i % 4) * 30 : 60)
                    .blur(radius: 0.5)
            }
        }
        .opacity(active ? 1 : 0)
        .blendMode(.plusLighter)
        .accessibilityHidden(true)
    }
}

// MARK: - The gilt edge

/// Drag along the gilt edge to riffle through the book. Page 1 is on the right, as in a mushaf.
/// A tick for every page passed, and the surah under her finger shown above it.
private struct GiltEdge: View {
    let count: Int
    @Binding var index: Int
    @Binding var scrubbing: Int?
    let pages: [MushafPage]
    let quran: QuranStore
    let hapticsOn: Bool

    var body: some View {
        GeometryReader { geo in
            let width = geo.size.width
            let shown = scrubbing ?? index
            let fraction = count > 1 ? CGFloat(shown) / CGFloat(count - 1) : 0
            // Right to left: page 1 sits at the right end.
            let x = width * (1 - fraction)
            ZStack(alignment: .leading) {
                Capsule()
                    .fill(LinearGradient(colors: [Color(hex: 0xB49B5F), Color(hex: 0xFFF0C8), Color(hex: 0xDED0A8)], startPoint: .top, endPoint: .bottom))
                    .frame(height: 6)
                Circle()
                    .fill(MushafInk.goldLight)
                    .frame(width: scrubbing == nil ? 16 : 22, height: scrubbing == nil ? 16 : 22)
                    .shadow(color: .black.opacity(0.4), radius: 3)
                    .offset(x: x - (scrubbing == nil ? 8 : 11))
                if let scrubbing, pages.indices.contains(scrubbing), let first = pages[scrubbing].verses.first {
                    VStack(spacing: 0) {
                        Text(quran.surah(first.surah).name).font(.footnote.weight(.semibold))
                        Text("Page \(pages[scrubbing].number)")
                            .font(.caption2)
                            .monospacedDigit()
                            .contentTransition(.numericText(value: Double(pages[scrubbing].number)))
                    }
                    .foregroundStyle(MushafInk.boardDeep)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 5)
                    .background(Capsule().fill(MushafInk.goldLight))
                    .fixedSize()
                    .offset(x: min(max(x - 50, 0), width - 100), y: -38)
                    .transition(.scale(scale: 0.8).combined(with: .opacity))
                }
            }
            .frame(height: 30)
            .contentShape(Rectangle())
            .gesture(
                DragGesture(minimumDistance: 0)
                    .onChanged { value in
                        guard count > 0 else { return }
                        let f = 1 - min(max(value.location.x / max(width, 1), 0), 1)
                        let target = Int((f * CGFloat(count - 1)).rounded())
                        withAnimation(.snappy(duration: 0.15)) { scrubbing = target }
                    }
                    .onEnded { _ in
                        if let scrubbing { index = scrubbing }
                        withAnimation(.smooth(duration: 0.25)) { scrubbing = nil }
                    }
            )
        }
        .frame(height: 30)
        .sensoryFeedback(.selection, trigger: scrubbing) { old, new in hapticsOn && old != nil && new != nil }
        .accessibilityElement()
        .accessibilityLabel("Page")
        .accessibilityValue("\(pages.indices.contains(index) ? pages[index].number : 1) of \(count)")
        .accessibilityAdjustableAction { direction in
            switch direction {
            case .increment: index = min(index + 1, count - 1)
            case .decrement: index = max(index - 1, 0)
            @unknown default: break
            }
        }
    }
}

// MARK: - The cover

/// The closed mushaf: a deep green board with tooled gold. Every gold line is drawn three times,
/// a shadow below, the metal, and a highlight above, so it reads as pressed into the board.
/// Drawn rather than a photograph: crisp at any size, and nothing to license.
struct MushafCover: View {
    /// When true, a soft band of light passes across the cover every few seconds.
    var shining = false

    var body: some View {
        GeometryReader { geo in
            let w = geo.size.width, h = geo.size.height
            ZStack {
                RoundedRectangle(cornerRadius: 10, style: .continuous)
                    .fill(RadialGradient(colors: [MushafInk.boardLit, MushafInk.board, MushafInk.boardDeep],
                                         center: .center, startRadius: 0, endRadius: max(w, h) * 0.75))
                    .shadow(color: .black.opacity(0.5), radius: 16, y: 8)

                // Border band: two rules with a running line of stars between them.
                Emboss {
                    ZStack {
                        Rectangle().stroke(lineWidth: 2).padding(w * 0.045)
                        Rectangle().stroke(lineWidth: 1).padding(w * 0.105)
                    }
                }
                StarRun(inset: w * 0.075, spacing: w * 0.075)
                ForEach(0..<4, id: \.self) { corner in
                    Emboss { StarMark().frame(width: w * 0.12, height: w * 0.12) }
                        .position(x: corner % 2 == 0 ? w * 0.075 : w - w * 0.075,
                                  y: corner < 2 ? w * 0.075 : h - w * 0.075)
                }

                // The arch panel, like a mihrab.
                Emboss { ArchPanel().stroke(lineWidth: 1.6).padding(w * 0.17) }
                Emboss { ArchPanel().stroke(lineWidth: 0.8).padding(w * 0.2) }

                Emboss { StarMark().frame(width: w * 0.09, height: w * 0.09) }
                    .position(x: w / 2, y: h * 0.27)
                Emboss { StarMark().frame(width: w * 0.09, height: w * 0.09) }
                    .position(x: w / 2, y: h * 0.8)

                VStack(spacing: h * 0.018) {
                    Text("القرآن الكريم")
                        .font(AppFont.arabic(w * 0.13))
                    Text("The Holy Quran")
                        .font(.system(size: w * 0.07, weight: .medium, design: .serif))
                }
                .foregroundStyle(LinearGradient(colors: [MushafInk.goldLight, MushafInk.gold, MushafInk.goldDeep], startPoint: .top, endPoint: .bottom))
                .shadow(color: .black.opacity(0.6), radius: 1, y: 1.5)
                .multilineTextAlignment(.center)
                .position(x: w / 2, y: h * 0.53)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .overlay {
            if shining {
                CoverShine()
                    .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                    .allowsHitTesting(false)
            }
        }
    }
}

/// Light catching the tooled gold, as if the book were turned in her hands.
private struct CoverShine: View {
    var body: some View {
        GeometryReader { geo in
            LinearGradient(colors: [.clear, MushafInk.goldLight.opacity(0.28), .clear], startPoint: .leading, endPoint: .trailing)
                .frame(width: geo.size.width * 0.45)
                .rotationEffect(.degrees(18))
                .phaseAnimator([false, true]) { band, across in
                    band.offset(x: across ? geo.size.width * 1.2 : -geo.size.width * 0.6)
                } animation: { across in
                    across ? .easeInOut(duration: 1.6).delay(1.2) : .linear(duration: 0.01)
                }
        }
        .blendMode(.plusLighter)
        .accessibilityHidden(true)
    }
}

/// Draws its content three times: a dark groove, gold, and a thin highlight.
private struct Emboss<Content: View>: View {
    @ViewBuilder var content: Content

    var body: some View {
        ZStack {
            content.foregroundStyle(Color.black.opacity(0.55)).offset(y: 1.2)
            content.foregroundStyle(LinearGradient(colors: [MushafInk.goldLight, MushafInk.gold, MushafInk.goldDeep], startPoint: .topLeading, endPoint: .bottomTrailing))
            content.foregroundStyle(Color.white.opacity(0.25)).offset(y: -0.6).blendMode(.plusLighter)
        }
    }
}

/// A line of small stars around the border band.
private struct StarRun: View {
    let inset: CGFloat
    let spacing: CGFloat

    var body: some View {
        Canvas { context, size in
            let gold = GraphicsContext.Shading.color(MushafInk.gold)
            let star = StarMark()
            let s: CGFloat = spacing * 0.32
            func draw(_ x: CGFloat, _ y: CGFloat) {
                context.fill(star.path(in: CGRect(x: x - s / 2, y: y - s / 2, width: s, height: s)), with: gold)
            }
            var x = inset + spacing
            while x < size.width - inset - spacing / 2 {
                draw(x, inset); draw(x, size.height - inset)
                x += spacing
            }
            var y = inset + spacing
            while y < size.height - inset - spacing / 2 {
                draw(inset, y); draw(size.width - inset, y)
                y += spacing
            }
        }
        .accessibilityHidden(true)
    }
}

/// A pointed arch at the top and bottom, the shape of the central panel on a mushaf cover.
private struct ArchPanel: Shape {
    func path(in rect: CGRect) -> Path {
        let w = rect.width, h = rect.height
        let shoulder = h * 0.16
        var p = Path()
        p.move(to: CGPoint(x: rect.minX, y: rect.minY + shoulder))
        p.addQuadCurve(to: CGPoint(x: rect.midX, y: rect.minY), control: CGPoint(x: rect.minX + w * 0.28, y: rect.minY + shoulder * 0.15))
        p.addQuadCurve(to: CGPoint(x: rect.maxX, y: rect.minY + shoulder), control: CGPoint(x: rect.maxX - w * 0.28, y: rect.minY + shoulder * 0.15))
        p.addLine(to: CGPoint(x: rect.maxX, y: rect.maxY - shoulder))
        p.addQuadCurve(to: CGPoint(x: rect.midX, y: rect.maxY), control: CGPoint(x: rect.maxX - w * 0.28, y: rect.maxY - shoulder * 0.15))
        p.addQuadCurve(to: CGPoint(x: rect.minX, y: rect.maxY - shoulder), control: CGPoint(x: rect.minX + w * 0.28, y: rect.maxY - shoulder * 0.15))
        p.closeSubpath()
        return p
    }
}
