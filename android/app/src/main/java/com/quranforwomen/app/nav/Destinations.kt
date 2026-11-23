package com.quranforwomen.app.nav

import androidx.compose.runtime.Composable
import com.quranforwomen.app.feature.audio.FullPlayerScreen
import com.quranforwomen.app.feature.dhikr.DhikrScreen
import com.quranforwomen.app.feature.duas.DuaCategoryScreen
import com.quranforwomen.app.feature.duas.DuaDetailScreen
import com.quranforwomen.app.feature.duas.DuasScreen
import com.quranforwomen.app.feature.duas.PersonalDuasScreen
import com.quranforwomen.app.feature.duas.SavedDuasScreen
import com.quranforwomen.app.feature.explore.CollectionScreen
import com.quranforwomen.app.feature.explore.ExploreScreen
import com.quranforwomen.app.feature.explore.FeelingsScreen
import com.quranforwomen.app.feature.explore.QuizScreen
import com.quranforwomen.app.feature.explore.ReadingPlanScreen
import com.quranforwomen.app.feature.journal.JournalEditorScreen
import com.quranforwomen.app.feature.journal.JournalEntryScreen
import com.quranforwomen.app.feature.journal.JournalPromptsScreen
import com.quranforwomen.app.feature.journal.JournalSettingsScreen
import com.quranforwomen.app.feature.journal.JournalTabScreen
import com.quranforwomen.app.feature.journal.JournalTemplatesScreen
import com.quranforwomen.app.feature.journal.PromptPackScreen
import com.quranforwomen.app.feature.journal.TemplateCollectionScreen
import com.quranforwomen.app.feature.notes.LessonCaptureScreen
import com.quranforwomen.app.feature.notes.LessonEditScreen
import com.quranforwomen.app.feature.notes.LessonNoteScreen
import com.quranforwomen.app.feature.prayer.PrayerSettingsScreen
import com.quranforwomen.app.feature.prayer.PrayerTimesScreen
import com.quranforwomen.app.feature.prayer.QiblaScreen
import com.quranforwomen.app.feature.quran.MushafScreen
import com.quranforwomen.app.feature.quran.QuranLibraryScreen
import com.quranforwomen.app.feature.quran.ReadingSettingsScreen
import com.quranforwomen.app.feature.quran.SurahReaderScreen
import com.quranforwomen.app.feature.search.SearchScreen
import com.quranforwomen.app.feature.today.TodayScreen
import com.quranforwomen.app.feature.you.AboutScreen
import com.quranforwomen.app.feature.you.AnnotationsScreen
import com.quranforwomen.app.feature.you.DownloadsScreen
import com.quranforwomen.app.feature.you.LanguageScreen
import com.quranforwomen.app.feature.you.ReadingProgressScreen
import com.quranforwomen.app.feature.you.ReminderScreen
import com.quranforwomen.app.feature.you.SavedAyahsScreen
import com.quranforwomen.app.feature.you.SettingsScreen
import com.quranforwomen.app.feature.you.YouScreen

/** The first screen of each tab. */
@Composable
fun TabRoot(tab: AppTab) = when (tab) {
    AppTab.today -> TodayScreen()
    AppTab.quran -> QuranLibraryScreen()
    AppTab.duas -> DuasScreen()
    AppTab.journal -> JournalTabScreen()
    AppTab.you -> YouScreen()
}

/** Every pushed screen. */
@Composable
fun RouteScreen(route: Route) = when (route) {
    is Route.Reader -> SurahReaderScreen(route.surah, route.verse)
    is Route.DuaCategory -> DuaCategoryScreen(route.id)
    is Route.DuaDetail -> DuaDetailScreen(route.id)
    Route.Dhikr -> DhikrScreen()
    Route.DhikrHistory -> com.quranforwomen.app.feature.dhikr.DhikrHistoryScreen()
    Route.CustomDhikr -> com.quranforwomen.app.feature.dhikr.CustomDhikrScreen()
    Route.PersonalDuas -> PersonalDuasScreen()
    Route.SavedAyahs -> SavedAyahsScreen()
    Route.SavedDuas -> SavedDuasScreen()
    Route.Annotations -> AnnotationsScreen()
    Route.ReadingProgress -> ReadingProgressScreen()
    Route.ReadingHistory -> com.quranforwomen.app.feature.quran.ReadingHistoryScreen()
    Route.Reminder -> ReminderScreen()
    Route.Language -> LanguageScreen()
    Route.ReadingSettings -> ReadingSettingsScreen()
    Route.Downloads -> DownloadsScreen()
    Route.About -> AboutScreen()
    is Route.Collection -> CollectionScreen(route.id)
    Route.Quiz -> QuizScreen()
    Route.ReadingPlan -> ReadingPlanScreen()
    Route.PrayerTimes -> PrayerTimesScreen()
    Route.Qibla -> QiblaScreen()
    Route.PrayerSettings -> PrayerSettingsScreen()
    Route.IslamicCalendar -> com.quranforwomen.app.feature.ramadan.IslamicCalendarScreen()
    Route.MissedPrayers -> com.quranforwomen.app.feature.ramadan.MissedPrayersScreen()
    is Route.JournalEntry -> JournalEntryScreen(route.id)
    Route.JournalTemplates -> JournalTemplatesScreen()
    is Route.TemplateCollection -> TemplateCollectionScreen(route.id)
    Route.JournalPrompts -> JournalPromptsScreen()
    is Route.PromptPack -> PromptPackScreen(route.id)
    Route.JournalSettings -> JournalSettingsScreen()
    is Route.LessonNote -> LessonNoteScreen(route.id)
    Route.Feelings -> FeelingsScreen()
    Route.Explore -> ExploreScreen()
    Route.Settings -> SettingsScreen()
    Route.Search -> SearchScreen()
    is Route.Mushaf -> MushafScreen(route.page)
    is Route.JournalEditor -> JournalEditorScreen(route.start)
    is Route.LessonCapture -> LessonCaptureScreen(route.record)
    is Route.LessonEdit -> LessonEditScreen(route.id, route.isNew)
    Route.Player -> FullPlayerScreen()
    Route.Challenges -> com.quranforwomen.app.feature.challenges.ChallengesScreen()
    is Route.Challenge -> com.quranforwomen.app.feature.challenges.ChallengeScreen(route.id)
    is Route.GuidanceSection -> com.quranforwomen.app.feature.guidance.GuidanceSectionScreen(route.section)
    is Route.GuidanceArticle -> com.quranforwomen.app.feature.guidance.GuidanceArticleScreen(route.id)
}
