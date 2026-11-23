package com.quranforwomen.app.nav

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.staticCompositionLocalOf
import com.quranforwomen.app.data.VerseRef
import java.util.concurrent.atomic.AtomicLong

/** How a journal page is started. */
sealed interface JournalStart {
    data object Blank : JournalStart
    data class Prompt(val text: String, val isDua: Boolean = false) : JournalStart
    data class Template(val templateId: String) : JournalStart
    data class Verse(val ref: VerseRef) : JournalStart
    data class Edit(val entryId: String) : JournalStart
    data object Dua : JournalStart
}

/** Every screen that can be pushed onto a tab's stack. */
sealed interface Route {
    /** Full screen routes cover the tab bar, like an iOS full screen cover. */
    val fullScreen: Boolean get() = false
    /** Screens she reads in hide the tab bar and slide in from the side, as on iPhone. */
    val hidesTabBar: Boolean get() = fullScreen

    data class Reader(val surah: Int, val verse: Int? = null) : Route { override val hidesTabBar get() = true }
    data class DuaCategory(val id: String) : Route
    data class DuaDetail(val id: String) : Route
    data object Dhikr : Route { override val hidesTabBar get() = true }
    data object DhikrHistory : Route
    data object CustomDhikr : Route
    data object PersonalDuas : Route
    data object SavedAyahs : Route
    data object SavedDuas : Route
    data object Annotations : Route
    data object ReadingProgress : Route
    data object ReadingHistory : Route
    data object Reminder : Route
    data object Language : Route
    data object ReadingSettings : Route
    data object Downloads : Route
    data object About : Route
    data class Collection(val id: String) : Route
    data object Quiz : Route
    data object ReadingPlan : Route
    data object PrayerTimes : Route
    data object Qibla : Route
    data object PrayerSettings : Route
    data object IslamicCalendar : Route
    data object MissedPrayers : Route
    data class JournalEntry(val id: String) : Route
    data object JournalTemplates : Route
    data class TemplateCollection(val id: String) : Route
    data object JournalPrompts : Route
    data class PromptPack(val id: String) : Route
    data object JournalSettings : Route
    data class LessonNote(val id: String) : Route
    data object Feelings : Route
    data object Explore : Route
    data object Settings : Route
    data object Search : Route
    data object Challenges : Route
    data class Challenge(val id: String) : Route
    data class GuidanceSection(val section: String) : Route
    data class GuidanceArticle(val id: String) : Route

    /** The flippable Mushaf. Opens at a page, or where she left off. */
    data class Mushaf(val page: Int? = null) : Route { override val fullScreen get() = true }
    /** The journal editor or a template, started in one of several ways. */
    data class JournalEditor(val start: JournalStart) : Route { override val fullScreen get() = true }
    /** Recording or typing a new lesson note. */
    data class LessonCapture(val record: Boolean) : Route { override val fullScreen get() = true }
    /** Editing a lesson note. */
    data class LessonEdit(val id: String, val isNew: Boolean = false) : Route { override val fullScreen get() = true }
    /** The full recitation player. */
    data object Player : Route { override val fullScreen get() = true }
}

enum class AppTab(val title: String) {
    today("Today"), quran("Quran"), duas("Duas"), journal("Journal"), you("You");
}

/** One entry on a stack. The id keeps each screen's saved state apart, even for the same route twice. */
data class StackEntry(val id: Long, val route: Route)

/** Selected tab and one stack per tab, so any screen can be opened from anywhere. */
class AppRouter {
    var tab by mutableStateOf(AppTab.today)
    private val stacks: Map<AppTab, SnapshotStateList<StackEntry>> = AppTab.entries.associateWith { mutableStateListOf() }
    private val counter = AtomicLong(1)

    /** Bumped when a tab is tapped again while open, so its root can scroll to the top. */
    var reselect by mutableStateOf(0 to AppTab.today); private set

    fun stack(tab: AppTab): SnapshotStateList<StackEntry> = stacks.getValue(tab)

    val current: StackEntry? get() = stack(tab).lastOrNull()

    fun push(route: Route) { stack(tab).add(StackEntry(counter.getAndIncrement(), route)) }

    fun open(route: Route, in_: AppTab) {
        tab = in_
        stack(in_).add(StackEntry(counter.getAndIncrement(), route))
    }

    /** Returns false when already at the root. */
    fun pop(): Boolean {
        val s = stack(tab)
        if (s.isEmpty()) return false
        s.removeAt(s.lastIndex)
        return true
    }

    fun popToRoot(t: AppTab = tab) { stack(t).clear() }

    fun select(t: AppTab) {
        if (t == tab) {
            if (stack(t).isNotEmpty()) popToRoot(t) else reselect = (reselect.first + 1) to t
        } else {
            tab = t
        }
    }

    /** Removes private journal pages from every stack, when the journal locks. */
    fun closePrivatePages() {
        stacks.values.forEach { s ->
            s.removeAll {
                it.route is Route.JournalEntry || it.route is Route.LessonNote ||
                    it.route is Route.JournalEditor || it.route is Route.LessonEdit
            }
        }
        // A recording in progress stays open: losing a khutbah to a dimmed screen would be worse.
        val journal = stack(AppTab.journal)
        journal.removeAll { it.route !is Route.LessonCapture }
    }

    /** quranforwomen://verse/2:255 from the widget, and the reminder links. */
    fun handle(uri: Uri) {
        if (uri.scheme != "quranforwomen") return
        when (uri.host) {
            "verse" -> {
                val ref = VerseRef.parse(uri.lastPathSegment ?: return) ?: return
                if (ref.surah !in 1..114 || ref.ayah < 1) return
                stack(AppTab.today).clear()
                open(Route.Reader(ref.surah, ref.ayah), AppTab.today)
            }
            "today" -> { popToRoot(AppTab.today); tab = AppTab.today }
            "dhikr" -> { stack(AppTab.today).clear(); open(Route.Dhikr, AppTab.today) }
            // From the home screen widgets.
            "dua" -> { val id = uri.lastPathSegment ?: return; stack(AppTab.today).clear(); open(Route.DuaDetail(id), AppTab.today) }
            "prayer" -> { stack(AppTab.today).clear(); open(Route.PrayerTimes, AppTab.today) }
            "progress" -> { stack(AppTab.today).clear(); open(Route.ReadingProgress, AppTab.today) }
            "plan" -> { stack(AppTab.today).clear(); open(Route.ReadingPlan, AppTab.today) }
        }
    }
}

val LocalRouter = staticCompositionLocalOf<AppRouter> { error("No router") }
