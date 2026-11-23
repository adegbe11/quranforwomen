package com.quranforwomen.app

import com.quranforwomen.app.data.HideLevel
import com.quranforwomen.app.data.HifzStore
import com.quranforwomen.app.data.TafsirStore
import com.quranforwomen.app.data.WordStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StudyTests {
    @Test
    fun pauseMarksStayWithTheirWord() {
        // Al-Baqarah 2:2 as bundled: the pause marks are separate tokens.
        val verse = "ذَٰلِكَ ٱلْكِتَٰبُ لَا رَيْبَ ۛ فِيهِ ۛ هُدًۭى لِّلْمُتَّقِينَ"
        val words = HifzStore.splitWords(verse)
        assertEquals(7, words.size)
        assertEquals("رَيْبَ ۛ", words[3])
        assertEquals("فِيهِ ۛ", words[4])
    }

    @Test
    fun aLeadingStarJoinsTheFirstWord() {
        val words = HifzStore.splitWords("۞ إِنَّ ٱللَّهَ")
        assertEquals(2, words.size)
        assertTrue(words[0].startsWith("۞"))
    }

    @Test
    fun hideLevels() {
        assertEquals(listOf(false, true, false, true), (0..3).map { HideLevel.Some.hides(it) })
        assertEquals(listOf(false, true, true), (0..2).map { HideLevel.FirstWord.hides(it) })
        assertTrue((0..5).none { HideLevel.Off.hides(it) })
        assertTrue((0..5).all { HideLevel.All.hides(it) })
    }

    @Test
    fun tafsirGroupsPointToTheirOpeningVerse() {
        // Verses 2 and 3 are explained with verse 1; verse 4 stands alone.
        val texts = listOf("one", "", "", "four")
        val e = TafsirStore.group(texts, 3)!!
        assertEquals("one", e.html)
        assertEquals(1, e.firstAyah)
        assertEquals(3, e.lastAyah)
        val f = TafsirStore.group(texts, 4)!!
        assertEquals(4, f.firstAyah)
        assertEquals(4, f.lastAyah)
        assertNull(TafsirStore.group(emptyList(), 1))
    }

    @Test
    fun tafsirParseKeepsVerseOrder() {
        val body = """{"tafsirs":[{"verse_key":"9:2","text":""},{"verse_key":"9:1","text":"<p>a</p>"},{"verse_key":"9:3","text":"<p>c</p>"}]}"""
        assertEquals(listOf("<p>a</p>", "", "<p>c</p>"), TafsirStore.parse(body))
    }

    @Test
    fun wordParseDropsTheVerseNumberMarker() {
        val body = """{"verses":[
          {"verse_number":2,"words":[{"char_type_name":"word","text_uthmani":"ٱللَّهُ","translation":{"text":"Allah"},"transliteration":{"text":"allahu"},"audio_url":"wbw/112_002_001.mp3"},{"char_type_name":"end","text_uthmani":"٢"}]},
          {"verse_number":1,"words":[{"char_type_name":"word","text_uthmani":"قُلْ","translation":{"text":"Say"},"transliteration":{"text":"qul"},"audio_url":"wbw/112_001_001.mp3"},{"char_type_name":"end","text_uthmani":"١"}]}
        ]}"""
        val grid = WordStore.parse(body)
        assertEquals(2, grid.size)
        assertEquals("قُلْ", grid[0][0].arabic)
        assertEquals("Say", grid[0][0].meaning)
        assertEquals(1, grid[1].size)
        assertEquals("wbw/112_002_001.mp3", grid[1][0].audio)
    }
}
