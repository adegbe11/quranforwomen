package com.quranforwomen.app.feature.journal

import androidx.compose.ui.graphics.vector.ImageVector
import com.quranforwomen.app.data.VerseRef
import com.quranforwomen.app.ui.components.symbol
import java.time.LocalDate

/** A question on a template page. */
internal data class TemplatePrompt(
    val heading: String,
    val question: String,
    val style: Style = Style.lines,
) {
    enum class Style {
        /** A few writing lines. */
        lines,
        /** One short line, such as a verse or a name. */
        field,
        /** A short numbered list. */
        list,
    }
}

internal data class JournalTemplate(
    val id: String,
    val collection: String,
    val title: String,
    val summary: String,
    val prompts: List<TemplatePrompt>,
    val isDua: Boolean = false,
    /** A verse shown at the top of the page (from the bundled Quran text). */
    val verse: VerseRef? = null,
)

internal data class TemplateCollection(
    val id: String,
    val title: String,
    val summary: String,
    val symbolName: String,
) {
    val icon: ImageVector get() = symbol(symbolName)
}

internal data class PromptPack(
    val id: String,
    val title: String,
    val symbolName: String,
    val prompts: List<String>,
    val isDua: Boolean = false,
) {
    val icon: ImageVector get() = symbol(symbolName)
}

private typealias P = TemplatePrompt
private val FIELD = TemplatePrompt.Style.field
private val LIST = TemplatePrompt.Style.list

/** Written for this app. Plain, honest, never promising outcomes on Allah's behalf. */
internal object JournalContent {
    /** One rotating question on the Journal home, by day. */
    val dailyQuestions = listOf(
        "What is on your heart today?",
        "Where did you notice Allah's mercy this week?",
        "What are you asking Allah for right now?",
        "What blessing have you not thanked Allah for yet?",
        "What do you want to remember about today?",
        "What are you carrying that you can leave with Allah?",
        "Which verse has stayed with you lately, and why?",
    )

    fun question(date: LocalDate = LocalDate.now()): String =
        dailyQuestions[Math.floorMod(date.toEpochDay(), dailyQuestions.size.toLong()).toInt()]

    // Templates

    val collections = listOf(
        TemplateCollection("dua", "Dua", "Bring it to Allah in words", "hands.and.sparkles"),
        TemplateCollection("daily", "Daily walk", "Mornings, evenings and muhasabah", "sunrise"),
        TemplateCollection("quran", "Quran reflection", "Tadabbur and surah notes", "book"),
        TemplateCollection("gratitude", "Gratitude", "Shukr for big and small things", "heart"),
        TemplateCollection("answered", "Answered duas", "Remember how Allah answered", "sparkles"),
        TemplateCollection("hard", "Hard days", "Honest pages for heavy seasons", "cloud.rain"),
        TemplateCollection("family", "Family & motherhood", "Children, marriage, parents", "house"),
        TemplateCollection("ramadan", "Ramadan", "Fasting, Quran and the last ten nights", "moon.stars"),
        TemplateCollection("decisions", "Decisions & growth", "Istikhara, habits, who you are becoming", "signpost.right"),
    )

    val templates = listOf(
        // Dua
        JournalTemplate("dua-today", "dua", "Making dua today", "Put what you are asking into words.", listOf(
            P("On my heart", "What is on my heart today?"),
            P("I am asking Allah for", "What am I asking Allah for?"),
            P("People in my duas", "Who do I want to make dua for?", LIST),
            P("I leave with Allah", "What am I leaving in Allah's hands?"),
        ), isDua = true, verse = VerseRef(2, 186)),
        JournalTemplate("dua-someone", "dua", "Dua for someone I love", "Carry another person to Allah.", listOf(
            P("Who", "Who am I making dua for?", FIELD),
            P("What they are facing", "What are they going through?"),
            P("My dua for them", "What do I ask Allah to do for them?"),
        ), isDua = true),
        JournalTemplate("dua-family", "dua", "Dua for my family", "Your home, in your own words.", listOf(
            P("My home", "What does my home need right now?"),
            P("My children or family", "What do I ask for each of them?", LIST),
            P("My dua", "My dua for my family is"),
        ), isDua = true, verse = VerseRef(25, 74)),
        JournalTemplate("dua-update", "dua", "Dua update", "Return to something you are still asking for.", listOf(
            P("Still asking for", "What am I still asking for?"),
            P("What has changed", "What has changed since I started asking?"),
            P("While I wait", "What am I learning while I wait?"),
            P("Next step", "Is there one step I can take?", FIELD),
        ), isDua = true),

        // Daily walk
        JournalTemplate("morning", "daily", "Morning intention", "Begin the day with Allah.", listOf(
            P("Today I want to", "What do I hope for today?"),
            P("For Allah", "One thing I will do for the sake of Allah today", FIELD),
            P("I need", "What do I need from Allah today?"),
        ), verse = VerseRef(2, 152)),
        JournalTemplate("muhasabah", "daily", "Evening muhasabah", "A gentle look back at your day.", listOf(
            P("Alhamdulillah", "What went well today?", LIST),
            P("Where I fell short", "Where did I fall short today?"),
            P("Forgiveness", "What do I want to ask Allah to forgive?"),
            P("Tomorrow", "What will I do a little better tomorrow?", FIELD),
        )),
        JournalTemplate("before-sleep", "daily", "Before I sleep", "Put the day down.", listOf(
            P("Still turning over", "What is still on my mind?"),
            P("I leave with Allah", "What am I handing to Allah tonight?"),
        )),
        JournalTemplate("close", "daily", "A moment I felt close to Allah", "Keep it so you can return to it.", listOf(
            P("The moment", "What happened?"),
            P("What I felt", "What did I feel?"),
            P("Remember", "What do I want to remember from it?"),
        )),

        // Quran reflection
        JournalTemplate("tadabbur", "quran", "Tadabbur: reflect on a verse", "Slow down with one verse.", listOf(
            P("The verse", "Which verse? For example 2:286", FIELD),
            P("What it says", "In my own words, what does it say?"),
            P("About Allah", "What does it teach me about Allah?"),
            P("What it asks of me", "What does it ask of me?"),
            P("My dua", "A dua that comes from this verse"),
        ), verse = VerseRef(38, 29)),
        JournalTemplate("surah-notes", "quran", "Surah notes", "Notes as you read a surah.", listOf(
            P("Surah", "Which surah?", FIELD),
            P("Themes", "Themes I noticed", LIST),
            P("Verses that stood out", "Which verses stood out?", LIST),
            P("Questions", "Questions I still have"),
        )),
        JournalTemplate("carry-verse", "quran", "A verse to carry today", "Take one verse into your day.", listOf(
            P("The verse", "Which verse?", FIELD),
            P("Why it spoke to me", "Why did it speak to me?"),
            P("Living it", "How will I live it today?"),
        )),
        JournalTemplate("hifz", "quran", "Memorization log", "Track what you are learning by heart.", listOf(
            P("Memorizing", "What am I memorizing?", FIELD),
            P("How it went", "How did it go today?"),
            P("To review", "Verses to review", LIST),
        )),

        // Gratitude
        JournalTemplate("three-blessings", "gratitude", "Three blessings", "Three things, today.", listOf(
            P("Alhamdulillah for", "Three blessings from today", LIST),
        ), verse = VerseRef(14, 7)),
        JournalTemplate("small-things", "gratitude", "The small things", "Notice what is easy to miss.", listOf(
            P("Small blessings", "Small things I am thankful for", LIST),
            P("Easy to forget", "A blessing I usually take for granted"),
        )),
        JournalTemplate("shukr-hard", "gratitude", "Shukr in a hard season", "Gratitude without pretending.", listOf(
            P("What is hard", "What is hard right now?"),
            P("What is still good", "What is still good?", LIST),
            P("Protected from", "What might Allah have protected me from?"),
        )),
        JournalTemplate("people-thankful", "gratitude", "People I am thankful for", "The people Allah placed in your life.", listOf(
            P("Who", "Who am I thankful for?", LIST),
            P("Why", "Why?"),
            P("Tell them", "Will I tell them?", FIELD),
        )),

        // Answered duas
        JournalTemplate("answered", "answered", "Allah answered", "Write it down so you never forget.", listOf(
            P("What I asked", "What did I ask for?"),
            P("How it was answered", "How did Allah answer?"),
            P("Remember", "What do I want to remember about this?"),
        )),
        JournalTemplate("waiting", "answered", "Still waiting with sabr", "Patience is also worship.", listOf(
            P("Waiting for", "What am I waiting for?"),
            P("How long", "How long have I been asking?", FIELD),
            P("What helps", "What helps me wait?"),
        ), isDua = true, verse = VerseRef(2, 153)),
        JournalTemplate("different", "answered", "When the answer was different", "When it did not go as you asked.", listOf(
            P("What I asked", "What did I ask for?"),
            P("What happened", "What happened instead?"),
            P("What I might learn", "What might I be learning?"),
        ), verse = VerseRef(2, 216)),

        // Hard days
        JournalTemplate("anxious", "hard", "When I am anxious", "Separate what you hold from what Allah holds.", listOf(
            P("What I am anxious about", "What is making me anxious?"),
            P("In my control", "What is in my control?", LIST),
            P("With Allah", "What do I leave with Allah?"),
        ), verse = VerseRef(13, 28)),
        JournalTemplate("far", "hard", "When I feel far from Allah", "This is allowed to be written down.", listOf(
            P("Honestly", "How do I honestly feel?"),
            P("One small step", "One small step back toward Allah", FIELD),
        ), verse = VerseRef(50, 16)),
        JournalTemplate("grief", "hard", "When I am grieving", "No stages, no timeline.", listOf(
            P("Who or what I lost", "Who or what did I lose?"),
            P("To Allah", "What do I want to say to Allah?"),
            P("A memory", "A memory I want to keep"),
        ), verse = VerseRef(2, 156)),
        JournalTemplate("tired", "hard", "When I am tired", "Rest is not failure.", listOf(
            P("What drains me", "What is draining me?"),
            P("What would help", "What would give me rest?"),
            P("What can wait", "What can wait?", LIST),
        ), verse = VerseRef(2, 286)),

        // Family and motherhood
        JournalTemplate("children", "family", "My children", "Who they are right now.", listOf(
            P("About them today", "What do I want to remember about them today?"),
            P("My dua for them", "My dua for my children"),
        ), isDua = true, verse = VerseRef(14, 40)),
        JournalTemplate("marriage", "family", "My marriage", "Tranquility, honestly.", listOf(
            P("Grateful for", "What am I grateful for in my spouse?"),
            P("What is hard", "What is hard right now?"),
            P("My dua for us", "My dua for us"),
        ), isDua = true, verse = VerseRef(30, 21)),
        JournalTemplate("parents", "family", "My parents", "Honor and mercy.", listOf(
            P("About them", "What do I want to say about my parents?"),
            P("My dua for them", "My dua for my parents"),
        ), isDua = true, verse = VerseRef(17, 24)),
        JournalTemplate("motherhood", "family", "Pregnancy and motherhood", "A page for this season.", listOf(
            P("How I feel", "How am I feeling, in body and heart?"),
            P("My hopes", "What do I hope for my child?"),
            P("My dua", "My dua for my child"),
        ), isDua = true, verse = VerseRef(3, 38)),
        JournalTemplate("letter-child", "family", "A letter to my child", "Something for them to read one day.", listOf(
            P("Dear", "Write to your child"),
        )),

        // Ramadan
        JournalTemplate("ramadan-day", "ramadan", "Ramadan day", "A short page for each day.", listOf(
            P("My fast", "How did my fast go today?"),
            P("Quran today", "What did I read today?", FIELD),
            P("Dua at iftar", "My dua at iftar"),
            P("One good deed", "One good deed today", FIELD),
        ), verse = VerseRef(2, 183)),
        JournalTemplate("last-ten", "ramadan", "The last ten nights", "Prepare your heart and your list.", listOf(
            P("What I want", "What do I want from these nights?"),
            P("My dua list", "My duas", LIST),
        ), isDua = true, verse = VerseRef(97, 3)),

        // Decisions and growth
        JournalTemplate("istikhara", "decisions", "Istikhara reflection", "Think it through after praying istikhara.", listOf(
            P("Deciding", "What am I deciding?"),
            P("Istikhara", "Have I prayed istikhara about it?", FIELD),
            P("Advice", "What did people I trust advise?"),
            P("My heart now", "How does my heart feel about it now?"),
            P("Next", "What will I do next?", FIELD),
        ), verse = VerseRef(3, 159)),
        JournalTemplate("becoming", "decisions", "Who I want to become", "The woman you are growing into.", listOf(
            P("Becoming", "Who do I want to become?"),
            P("Already", "Where am I already growing?"),
            P("One habit", "One habit that would help", FIELD),
        )),
        JournalTemplate("honest", "decisions", "An honest page", "Just you, and what is true.", listOf(
            P("Honestly", "What is true for me right now?"),
        )),
        JournalTemplate("letter-self", "decisions", "A letter to myself, later", "Read it in a year.", listOf(
            P("Dear me", "Write to yourself a year from now"),
        )),
    )

    fun templates(collection: TemplateCollection): List<JournalTemplate> = templates.filter { it.collection == collection.id }

    fun template(id: String): JournalTemplate? = templates.firstOrNull { it.id == id }

    fun collection(id: String): TemplateCollection? = collections.firstOrNull { it.id == id }

    fun pack(id: String): PromptPack? = packs.firstOrNull { it.id == id }

    /** Turns filled answers into the entry body. Empty sections are left out. */
    fun compose(template: JournalTemplate, answers: List<String>): String =
        template.prompts.zip(answers).mapNotNull { (prompt, answer) ->
            val text = answer.trim()
            if (text.isEmpty()) return@mapNotNull null
            if (prompt.style == TemplatePrompt.Style.list) {
                val items = text.split("\n").filter { it.isNotEmpty() }.map { "- ${it.trim()}" }
                "## ${prompt.heading}\n" + items.joinToString("\n")
            } else {
                "## ${prompt.heading}\n$text"
            }
        }.joinToString("\n\n")

    // Prompt packs

    val packs = listOf(
        PromptPack("morning", "Morning", "sunrise", listOf(
            "What do I want to give Allah before this day begins?",
            "What is one intention I can set for today?",
            "Who can I be kind to today?",
            "What am I worried about today, and can I leave it with Allah?",
            "Which dhikr do I want to keep on my tongue today?",
            "What would make today count, in Allah's sight?",
        )),
        PromptPack("evening", "Evening", "moon", listOf(
            "What can I leave in Allah's hands until morning?",
            "Where did I see Allah's care today?",
            "Is there anyone I need to forgive tonight?",
            "What do I want to thank Allah for before I sleep?",
            "What did today teach me?",
            "What do I want to do differently tomorrow?",
        )),
        PromptPack("shukr", "Shukr", "heart", listOf(
            "What do I have now that I once made dua for?",
            "Which blessing do I see every day but rarely thank Allah for?",
            "Who has been a mercy to me lately?",
            "What hardship turned out to be a blessing?",
            "What about my body am I grateful for today?",
            "What is one small joy from this week?",
        )),
        PromptPack("tawbah", "Tawbah", "drop", listOf(
            "What do I want to turn back to Allah from?",
            "What usually leads me there, and what could I change?",
            "Who might I need to make things right with?",
            "What does Allah's mercy mean to me right now?",
            "What good deed could follow this mistake?",
        )),
        PromptPack("my-duas", "My duas", "hands.and.sparkles", listOf(
            "What would I ask for if I were not afraid to ask?",
            "What have I stopped asking for, and why?",
            "What do I want most for my akhirah?",
            "Who do I want to include in my duas this week?",
            "What do I want Allah to change in me?",
        ), isDua = true),
        PromptPack("sabr", "Sabr", "leaf", listOf(
            "What am I being patient with right now?",
            "What helps me stay patient?",
            "Who in the Quran was patient the way I am trying to be?",
            "What would I tell a sister going through this?",
            "What is one thing I can control today?",
        )),
        PromptPack("tawakkul", "Tawakkul", "shield", listOf(
            "What am I trying to control that belongs to Allah?",
            "When has Allah taken care of something I was afraid of?",
            "What would trusting Allah look like today?",
            "What step can I take, and what can I leave?",
        )),
        PromptPack("hard-days", "Hard days", "cloud.rain", listOf(
            "What do I want to tell Allah, without tidying it up?",
            "What do I need right now: rest, help, or time?",
            "Who could I reach out to today?",
            "What is one gentle thing I can do for myself?",
            "What has carried me through hard days before?",
        )),
        PromptPack("quran", "Quran reflection", "book", listOf(
            "Which verse has stayed with me this week?",
            "What is one verse I want to understand better?",
            "What did I read today that I want to act on?",
            "Which story in the Quran do I relate to right now?",
            "Which name of Allah do I want to know more deeply?",
        )),
        PromptPack("family", "Family", "house", listOf(
            "What do I love about my home right now?",
            "What does each person in my family need from me this week?",
            "What tradition do I want to start in my home?",
            "What do I want my children to remember about me?",
            "Where do I need more patience at home?",
        )),
        PromptPack("rest", "Rest & self-care", "moon.zzz", listOf(
            "What does my body need today?",
            "Where do I need to say no?",
            "What gives me peace that I have been skipping?",
            "How can I care for myself as an amanah from Allah?",
        )),
    )
}
