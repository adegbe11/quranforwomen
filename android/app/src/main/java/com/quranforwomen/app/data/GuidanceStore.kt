package com.quranforwomen.app.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray

/**
 * One article of guidance or a devotional, written by a named author and checked by a named reviewer.
 * The app never writes these. They come only from the bundled guidance.json.
 */
@Serializable
data class GuidanceArticle(
    val id: String,
    val section: String,
    val title: String,
    /** Paragraphs, in order. */
    val body: List<String>,
    /** Quran verses as "surah:ayah", for example "2:222". */
    val quranRefs: List<String>? = null,
    /** Hadith citations as written, for example "Sahih Muslim 332". */
    val hadithRefs: List<String>? = null,
    val author: String? = null,
    val reviewer: String? = null,
    /** "2026-10-01". */
    val reviewedOn: String? = null,
    /** "draft" or "approved". */
    val status: String,
) {
    val verses: List<VerseRef> get() = quranRefs.orEmpty().mapNotNull(VerseRef::parse)
    val hadith: List<String> get() = hadithRefs.orEmpty().filter { it.isNotBlank() }

    /** Only approved articles with a named reviewer are ever shown. */
    val isPublishable: Boolean
        get() = status == "approved" && !reviewer.isNullOrBlank() && title.isNotBlank() && body.isNotEmpty()
}

/**
 * The bundled guidance and devotionals. Empty until reviewed articles are added,
 * and every screen for it stays hidden until then.
 */
class GuidanceStore(context: Context) {
    val articles: List<GuidanceArticle> = runCatching {
        val text = context.assets.open("guidance.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
        val items = (AppJson.parseToJsonElement(text) as? JsonObject)?.get("items") as? JsonArray ?: JsonArray(emptyList())
        // One bad item never hides the others.
        items.jsonArray.mapNotNull { item ->
            runCatching { AppJson.decodeFromJsonElement(GuidanceArticle.serializer(), item) }.getOrNull()
        }.filter { it.isPublishable }
    }.getOrDefault(emptyList())

    /** Section names in the order they first appear. */
    val sections: List<String> get() = articles.map { it.section }.distinct()

    val isEmpty: Boolean get() = articles.isEmpty()

    fun articles(section: String): List<GuidanceArticle> = articles.filter { it.section == section }

    fun article(id: String): GuidanceArticle? = articles.firstOrNull { it.id == id }
}
