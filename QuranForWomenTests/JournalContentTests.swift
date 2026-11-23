import XCTest
@testable import QuranForWomen

final class JournalContentTests: XCTestCase {
    private let template = JournalTemplate(
        id: "test",
        collection: "daily",
        title: "Test page",
        summary: "A page for tests.",
        prompts: [
            TemplatePrompt(heading: "First", question: "First question?"),
            TemplatePrompt(heading: "Second", question: "Second question?", style: .field),
            TemplatePrompt(heading: "Third", question: "Third question?", style: .list),
        ]
    )

    func testEmptyAnswersGiveEmptyString() {
        XCTAssertEqual(JournalContent.compose(template, answers: []), "")
        XCTAssertEqual(JournalContent.compose(template, answers: ["", "", ""]), "")
        XCTAssertEqual(JournalContent.compose(template, answers: ["  ", "\n", " \n\t "]), "")
    }

    func testAnsweredSectionsAppearInOrder() {
        let text = JournalContent.compose(template, answers: ["One", "Two", "Three"])
        XCTAssertEqual(text, "## First\nOne\n\n## Second\nTwo\n\n## Third\n- Three")
    }

    func testEmptySectionsAreLeftOut() {
        let text = JournalContent.compose(template, answers: ["One", "   ", "Three"])
        XCTAssertEqual(text, "## First\nOne\n\n## Third\n- Three")
        XCTAssertFalse(text.contains("Second"))
    }

    func testAnswersAreTrimmed() {
        let text = JournalContent.compose(template, answers: ["  One  \n", "", ""])
        XCTAssertEqual(text, "## First\nOne")
    }

    func testListAnswersBecomeBullets() {
        let text = JournalContent.compose(template, answers: ["", "", "apples\n  bread \nmilk"])
        XCTAssertEqual(text, "## Third\n- apples\n- bread\n- milk")
    }

    func testFewerAnswersThanPrompts() {
        let text = JournalContent.compose(template, answers: ["Only the first"])
        XCTAssertEqual(text, "## First\nOnly the first")
    }

    func testBundledTemplatesAreWellFormed() {
        let collectionIDs = Set(JournalContent.collections.map(\.id))
        XCTAssertEqual(Set(JournalContent.templates.map(\.id)).count, JournalContent.templates.count)
        for template in JournalContent.templates {
            XCTAssertFalse(template.prompts.isEmpty, template.id)
            XCTAssertTrue(collectionIDs.contains(template.collection), template.id)
            XCTAssertEqual(JournalContent.template(id: template.id), template)
        }
    }
}
