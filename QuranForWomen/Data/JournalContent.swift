import Foundation

/// A question on a template page.
struct TemplatePrompt: Hashable {
    enum Style: Hashable {
        /// A few writing lines.
        case lines
        /// One short line, such as a verse or a name.
        case field
        /// A short numbered list.
        case list
    }

    let heading: String
    let question: String
    var style: Style = .lines
}

struct JournalTemplate: Identifiable, Hashable {
    let id: String
    let collection: String
    let title: String
    let summary: String
    let prompts: [TemplatePrompt]
    var isDua = false
    /// A verse shown at the top of the page (from the bundled Quran text).
    var verse: VerseRef?
}

struct TemplateCollection: Identifiable, Hashable {
    let id: String
    let title: String
    let summary: String
    let symbol: String
}

struct PromptPack: Identifiable, Hashable {
    let id: String
    let title: String
    let symbol: String
    let prompts: [String]
    var isDua = false
}

/// Written for this app. Plain, honest, never promising outcomes on Allah's behalf.
enum JournalContent {
    /// One rotating question on the Journal home, by day.
    static let dailyQuestions = [
        "What is on your heart today?",
        "Where did you notice Allah's mercy this week?",
        "What are you asking Allah for right now?",
        "What blessing have you not thanked Allah for yet?",
        "What do you want to remember about today?",
        "What are you carrying that you can leave with Allah?",
        "Which verse has stayed with you lately, and why?",
    ]

    static func question(for date: Date = .now) -> String {
        let day = Calendar.current.ordinality(of: .day, in: .era, for: date) ?? 0
        return dailyQuestions[day % dailyQuestions.count]
    }

    // MARK: Templates

    static let collections: [TemplateCollection] = [
        .init(id: "dua", title: "Dua", summary: "Bring it to Allah in words", symbol: "hands.and.sparkles"),
        .init(id: "daily", title: "Daily walk", summary: "Mornings, evenings and muhasabah", symbol: "sunrise"),
        .init(id: "quran", title: "Quran reflection", summary: "Tadabbur and surah notes", symbol: "book"),
        .init(id: "gratitude", title: "Gratitude", summary: "Shukr for big and small things", symbol: "heart"),
        .init(id: "answered", title: "Answered duas", summary: "Remember how Allah answered", symbol: "sparkles"),
        .init(id: "hard", title: "Hard days", summary: "Honest pages for heavy seasons", symbol: "cloud.rain"),
        .init(id: "family", title: "Family & motherhood", summary: "Children, marriage, parents", symbol: "house"),
        .init(id: "ramadan", title: "Ramadan", summary: "Fasting, Quran and the last ten nights", symbol: "moon.stars"),
        .init(id: "decisions", title: "Decisions & growth", summary: "Istikhara, habits, who you are becoming", symbol: "signpost.right"),
    ]

    static let templates: [JournalTemplate] = [
        // Dua
        .init(id: "dua-today", collection: "dua", title: "Making dua today", summary: "Put what you are asking into words.", prompts: [
            .init(heading: "On my heart", question: "What is on my heart today?"),
            .init(heading: "I am asking Allah for", question: "What am I asking Allah for?"),
            .init(heading: "People in my duas", question: "Who do I want to make dua for?", style: .list),
            .init(heading: "I leave with Allah", question: "What am I leaving in Allah's hands?"),
        ], isDua: true, verse: VerseRef(surah: 2, ayah: 186)),
        .init(id: "dua-someone", collection: "dua", title: "Dua for someone I love", summary: "Carry another person to Allah.", prompts: [
            .init(heading: "Who", question: "Who am I making dua for?", style: .field),
            .init(heading: "What they are facing", question: "What are they going through?"),
            .init(heading: "My dua for them", question: "What do I ask Allah to do for them?"),
        ], isDua: true),
        .init(id: "dua-family", collection: "dua", title: "Dua for my family", summary: "Your home, in your own words.", prompts: [
            .init(heading: "My home", question: "What does my home need right now?"),
            .init(heading: "My children or family", question: "What do I ask for each of them?", style: .list),
            .init(heading: "My dua", question: "My dua for my family is"),
        ], isDua: true, verse: VerseRef(surah: 25, ayah: 74)),
        .init(id: "dua-update", collection: "dua", title: "Dua update", summary: "Return to something you are still asking for.", prompts: [
            .init(heading: "Still asking for", question: "What am I still asking for?"),
            .init(heading: "What has changed", question: "What has changed since I started asking?"),
            .init(heading: "While I wait", question: "What am I learning while I wait?"),
            .init(heading: "Next step", question: "Is there one step I can take?", style: .field),
        ], isDua: true),

        // Daily walk
        .init(id: "morning", collection: "daily", title: "Morning intention", summary: "Begin the day with Allah.", prompts: [
            .init(heading: "Today I want to", question: "What do I hope for today?"),
            .init(heading: "For Allah", question: "One thing I will do for the sake of Allah today", style: .field),
            .init(heading: "I need", question: "What do I need from Allah today?"),
        ], verse: VerseRef(surah: 2, ayah: 152)),
        .init(id: "muhasabah", collection: "daily", title: "Evening muhasabah", summary: "A gentle look back at your day.", prompts: [
            .init(heading: "Alhamdulillah", question: "What went well today?", style: .list),
            .init(heading: "Where I fell short", question: "Where did I fall short today?"),
            .init(heading: "Forgiveness", question: "What do I want to ask Allah to forgive?"),
            .init(heading: "Tomorrow", question: "What will I do a little better tomorrow?", style: .field),
        ]),
        .init(id: "before-sleep", collection: "daily", title: "Before I sleep", summary: "Put the day down.", prompts: [
            .init(heading: "Still turning over", question: "What is still on my mind?"),
            .init(heading: "I leave with Allah", question: "What am I handing to Allah tonight?"),
        ]),
        .init(id: "close", collection: "daily", title: "A moment I felt close to Allah", summary: "Keep it so you can return to it.", prompts: [
            .init(heading: "The moment", question: "What happened?"),
            .init(heading: "What I felt", question: "What did I feel?"),
            .init(heading: "Remember", question: "What do I want to remember from it?"),
        ]),

        // Quran reflection
        .init(id: "tadabbur", collection: "quran", title: "Tadabbur: reflect on a verse", summary: "Slow down with one verse.", prompts: [
            .init(heading: "The verse", question: "Which verse? For example 2:286", style: .field),
            .init(heading: "What it says", question: "In my own words, what does it say?"),
            .init(heading: "About Allah", question: "What does it teach me about Allah?"),
            .init(heading: "What it asks of me", question: "What does it ask of me?"),
            .init(heading: "My dua", question: "A dua that comes from this verse"),
        ], verse: VerseRef(surah: 38, ayah: 29)),
        .init(id: "surah-notes", collection: "quran", title: "Surah notes", summary: "Notes as you read a surah.", prompts: [
            .init(heading: "Surah", question: "Which surah?", style: .field),
            .init(heading: "Themes", question: "Themes I noticed", style: .list),
            .init(heading: "Verses that stood out", question: "Which verses stood out?", style: .list),
            .init(heading: "Questions", question: "Questions I still have"),
        ]),
        .init(id: "carry-verse", collection: "quran", title: "A verse to carry today", summary: "Take one verse into your day.", prompts: [
            .init(heading: "The verse", question: "Which verse?", style: .field),
            .init(heading: "Why it spoke to me", question: "Why did it speak to me?"),
            .init(heading: "Living it", question: "How will I live it today?"),
        ]),
        .init(id: "hifz", collection: "quran", title: "Memorization log", summary: "Track what you are learning by heart.", prompts: [
            .init(heading: "Memorizing", question: "What am I memorizing?", style: .field),
            .init(heading: "How it went", question: "How did it go today?"),
            .init(heading: "To review", question: "Verses to review", style: .list),
        ]),

        // Gratitude
        .init(id: "three-blessings", collection: "gratitude", title: "Three blessings", summary: "Three things, today.", prompts: [
            .init(heading: "Alhamdulillah for", question: "Three blessings from today", style: .list),
        ], verse: VerseRef(surah: 14, ayah: 7)),
        .init(id: "small-things", collection: "gratitude", title: "The small things", summary: "Notice what is easy to miss.", prompts: [
            .init(heading: "Small blessings", question: "Small things I am thankful for", style: .list),
            .init(heading: "Easy to forget", question: "A blessing I usually take for granted"),
        ]),
        .init(id: "shukr-hard", collection: "gratitude", title: "Shukr in a hard season", summary: "Gratitude without pretending.", prompts: [
            .init(heading: "What is hard", question: "What is hard right now?"),
            .init(heading: "What is still good", question: "What is still good?", style: .list),
            .init(heading: "Protected from", question: "What might Allah have protected me from?"),
        ]),
        .init(id: "people-thankful", collection: "gratitude", title: "People I am thankful for", summary: "The people Allah placed in your life.", prompts: [
            .init(heading: "Who", question: "Who am I thankful for?", style: .list),
            .init(heading: "Why", question: "Why?"),
            .init(heading: "Tell them", question: "Will I tell them?", style: .field),
        ]),

        // Answered duas
        .init(id: "answered", collection: "answered", title: "Allah answered", summary: "Write it down so you never forget.", prompts: [
            .init(heading: "What I asked", question: "What did I ask for?"),
            .init(heading: "How it was answered", question: "How did Allah answer?"),
            .init(heading: "Remember", question: "What do I want to remember about this?"),
        ]),
        .init(id: "waiting", collection: "answered", title: "Still waiting with sabr", summary: "Patience is also worship.", prompts: [
            .init(heading: "Waiting for", question: "What am I waiting for?"),
            .init(heading: "How long", question: "How long have I been asking?", style: .field),
            .init(heading: "What helps", question: "What helps me wait?"),
        ], isDua: true, verse: VerseRef(surah: 2, ayah: 153)),
        .init(id: "different", collection: "answered", title: "When the answer was different", summary: "When it did not go as you asked.", prompts: [
            .init(heading: "What I asked", question: "What did I ask for?"),
            .init(heading: "What happened", question: "What happened instead?"),
            .init(heading: "What I might learn", question: "What might I be learning?"),
        ], verse: VerseRef(surah: 2, ayah: 216)),

        // Hard days
        .init(id: "anxious", collection: "hard", title: "When I am anxious", summary: "Separate what you hold from what Allah holds.", prompts: [
            .init(heading: "What I am anxious about", question: "What is making me anxious?"),
            .init(heading: "In my control", question: "What is in my control?", style: .list),
            .init(heading: "With Allah", question: "What do I leave with Allah?"),
        ], verse: VerseRef(surah: 13, ayah: 28)),
        .init(id: "far", collection: "hard", title: "When I feel far from Allah", summary: "This is allowed to be written down.", prompts: [
            .init(heading: "Honestly", question: "How do I honestly feel?"),
            .init(heading: "One small step", question: "One small step back toward Allah", style: .field),
        ], verse: VerseRef(surah: 50, ayah: 16)),
        .init(id: "grief", collection: "hard", title: "When I am grieving", summary: "No stages, no timeline.", prompts: [
            .init(heading: "Who or what I lost", question: "Who or what did I lose?"),
            .init(heading: "To Allah", question: "What do I want to say to Allah?"),
            .init(heading: "A memory", question: "A memory I want to keep"),
        ], verse: VerseRef(surah: 2, ayah: 156)),
        .init(id: "tired", collection: "hard", title: "When I am tired", summary: "Rest is not failure.", prompts: [
            .init(heading: "What drains me", question: "What is draining me?"),
            .init(heading: "What would help", question: "What would give me rest?"),
            .init(heading: "What can wait", question: "What can wait?", style: .list),
        ], verse: VerseRef(surah: 2, ayah: 286)),

        // Family and motherhood
        .init(id: "children", collection: "family", title: "My children", summary: "Who they are right now.", prompts: [
            .init(heading: "About them today", question: "What do I want to remember about them today?"),
            .init(heading: "My dua for them", question: "My dua for my children"),
        ], isDua: true, verse: VerseRef(surah: 14, ayah: 40)),
        .init(id: "marriage", collection: "family", title: "My marriage", summary: "Tranquility, honestly.", prompts: [
            .init(heading: "Grateful for", question: "What am I grateful for in my spouse?"),
            .init(heading: "What is hard", question: "What is hard right now?"),
            .init(heading: "My dua for us", question: "My dua for us"),
        ], isDua: true, verse: VerseRef(surah: 30, ayah: 21)),
        .init(id: "parents", collection: "family", title: "My parents", summary: "Honor and mercy.", prompts: [
            .init(heading: "About them", question: "What do I want to say about my parents?"),
            .init(heading: "My dua for them", question: "My dua for my parents"),
        ], isDua: true, verse: VerseRef(surah: 17, ayah: 24)),
        .init(id: "motherhood", collection: "family", title: "Pregnancy and motherhood", summary: "A page for this season.", prompts: [
            .init(heading: "How I feel", question: "How am I feeling, in body and heart?"),
            .init(heading: "My hopes", question: "What do I hope for my child?"),
            .init(heading: "My dua", question: "My dua for my child"),
        ], isDua: true, verse: VerseRef(surah: 3, ayah: 38)),
        .init(id: "letter-child", collection: "family", title: "A letter to my child", summary: "Something for them to read one day.", prompts: [
            .init(heading: "Dear", question: "Write to your child"),
        ]),

        // Ramadan
        .init(id: "ramadan-day", collection: "ramadan", title: "Ramadan day", summary: "A short page for each day.", prompts: [
            .init(heading: "My fast", question: "How did my fast go today?"),
            .init(heading: "Quran today", question: "What did I read today?", style: .field),
            .init(heading: "Dua at iftar", question: "My dua at iftar"),
            .init(heading: "One good deed", question: "One good deed today", style: .field),
        ], verse: VerseRef(surah: 2, ayah: 183)),
        .init(id: "last-ten", collection: "ramadan", title: "The last ten nights", summary: "Prepare your heart and your list.", prompts: [
            .init(heading: "What I want", question: "What do I want from these nights?"),
            .init(heading: "My dua list", question: "My duas", style: .list),
        ], isDua: true, verse: VerseRef(surah: 97, ayah: 3)),

        // Decisions and growth
        .init(id: "istikhara", collection: "decisions", title: "Istikhara reflection", summary: "Think it through after praying istikhara.", prompts: [
            .init(heading: "Deciding", question: "What am I deciding?"),
            .init(heading: "Istikhara", question: "Have I prayed istikhara about it?", style: .field),
            .init(heading: "Advice", question: "What did people I trust advise?"),
            .init(heading: "My heart now", question: "How does my heart feel about it now?"),
            .init(heading: "Next", question: "What will I do next?", style: .field),
        ], verse: VerseRef(surah: 3, ayah: 159)),
        .init(id: "becoming", collection: "decisions", title: "Who I want to become", summary: "The woman you are growing into.", prompts: [
            .init(heading: "Becoming", question: "Who do I want to become?"),
            .init(heading: "Already", question: "Where am I already growing?"),
            .init(heading: "One habit", question: "One habit that would help", style: .field),
        ]),
        .init(id: "honest", collection: "decisions", title: "An honest page", summary: "Just you, and what is true.", prompts: [
            .init(heading: "Honestly", question: "What is true for me right now?"),
        ]),
        .init(id: "letter-self", collection: "decisions", title: "A letter to myself, later", summary: "Read it in a year.", prompts: [
            .init(heading: "Dear me", question: "Write to yourself a year from now"),
        ]),
    ]

    static func templates(in collection: TemplateCollection) -> [JournalTemplate] {
        templates.filter { $0.collection == collection.id }
    }

    static func template(id: String) -> JournalTemplate? { templates.first { $0.id == id } }

    /// Turns filled answers into the entry body. Empty sections are left out.
    static func compose(_ template: JournalTemplate, answers: [String]) -> String {
        zip(template.prompts, answers).compactMap { prompt, answer in
            let text = answer.trimmingCharacters(in: .whitespacesAndNewlines)
            guard !text.isEmpty else { return nil }
            if prompt.style == .list {
                let items = text.split(separator: "\n").map { "- \($0.trimmingCharacters(in: .whitespaces))" }
                return "## \(prompt.heading)\n" + items.joined(separator: "\n")
            }
            return "## \(prompt.heading)\n\(text)"
        }
        .joined(separator: "\n\n")
    }

    // MARK: Prompt packs

    static let packs: [PromptPack] = [
        .init(id: "morning", title: "Morning", symbol: "sunrise", prompts: [
            "What do I want to give Allah before this day begins?",
            "What is one intention I can set for today?",
            "Who can I be kind to today?",
            "What am I worried about today, and can I leave it with Allah?",
            "Which dhikr do I want to keep on my tongue today?",
            "What would make today count, in Allah's sight?",
        ]),
        .init(id: "evening", title: "Evening", symbol: "moon", prompts: [
            "What can I leave in Allah's hands until morning?",
            "Where did I see Allah's care today?",
            "Is there anyone I need to forgive tonight?",
            "What do I want to thank Allah for before I sleep?",
            "What did today teach me?",
            "What do I want to do differently tomorrow?",
        ]),
        .init(id: "shukr", title: "Shukr", symbol: "heart", prompts: [
            "What do I have now that I once made dua for?",
            "Which blessing do I see every day but rarely thank Allah for?",
            "Who has been a mercy to me lately?",
            "What hardship turned out to be a blessing?",
            "What about my body am I grateful for today?",
            "What is one small joy from this week?",
        ]),
        .init(id: "tawbah", title: "Tawbah", symbol: "drop", prompts: [
            "What do I want to turn back to Allah from?",
            "What usually leads me there, and what could I change?",
            "Who might I need to make things right with?",
            "What does Allah's mercy mean to me right now?",
            "What good deed could follow this mistake?",
        ]),
        .init(id: "my-duas", title: "My duas", symbol: "hands.and.sparkles", prompts: [
            "What would I ask for if I were not afraid to ask?",
            "What have I stopped asking for, and why?",
            "What do I want most for my akhirah?",
            "Who do I want to include in my duas this week?",
            "What do I want Allah to change in me?",
        ], isDua: true),
        .init(id: "sabr", title: "Sabr", symbol: "leaf", prompts: [
            "What am I being patient with right now?",
            "What helps me stay patient?",
            "Who in the Quran was patient the way I am trying to be?",
            "What would I tell a sister going through this?",
            "What is one thing I can control today?",
        ]),
        .init(id: "tawakkul", title: "Tawakkul", symbol: "shield", prompts: [
            "What am I trying to control that belongs to Allah?",
            "When has Allah taken care of something I was afraid of?",
            "What would trusting Allah look like today?",
            "What step can I take, and what can I leave?",
        ]),
        .init(id: "hard-days", title: "Hard days", symbol: "cloud.rain", prompts: [
            "What do I want to tell Allah, without tidying it up?",
            "What do I need right now: rest, help, or time?",
            "Who could I reach out to today?",
            "What is one gentle thing I can do for myself?",
            "What has carried me through hard days before?",
        ]),
        .init(id: "quran", title: "Quran reflection", symbol: "book", prompts: [
            "Which verse has stayed with me this week?",
            "What is one verse I want to understand better?",
            "What did I read today that I want to act on?",
            "Which story in the Quran do I relate to right now?",
            "Which name of Allah do I want to know more deeply?",
        ]),
        .init(id: "family", title: "Family", symbol: "house", prompts: [
            "What do I love about my home right now?",
            "What does each person in my family need from me this week?",
            "What tradition do I want to start in my home?",
            "What do I want my children to remember about me?",
            "Where do I need more patience at home?",
        ]),
        .init(id: "rest", title: "Rest & self-care", symbol: "moon.zzz", prompts: [
            "What does my body need today?",
            "Where do I need to say no?",
            "What gives me peace that I have been skipping?",
            "How can I care for myself as an amanah from Allah?",
        ]),
    ]
}
