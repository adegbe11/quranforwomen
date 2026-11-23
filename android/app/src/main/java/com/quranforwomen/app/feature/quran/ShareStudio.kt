package com.quranforwomen.app.feature.quran

import android.content.Context
import android.graphics.Bitmap
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import com.quranforwomen.app.R
import com.quranforwomen.app.ui.components.AppSheet
import com.quranforwomen.app.ui.components.Arabesque
import com.quranforwomen.app.ui.components.PrimaryButton
import com.quranforwomen.app.ui.components.pressable
import com.quranforwomen.app.ui.components.rememberHaptics
import com.quranforwomen.app.ui.components.shareFile
import com.quranforwomen.app.ui.components.sharedCacheFile
import com.quranforwomen.app.ui.theme.Palette
import com.quranforwomen.app.ui.theme.Space
import com.quranforwomen.app.ui.theme.TextStyles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.cos
import kotlin.math.sin

/** The looks a shared verse or dua can take. Deep jewel grounds with gold, or ivory with gold. */
enum class ShareStyle(
    val title: String,
    val top: Int,
    val bottom: Int,
    val arabic: Int,
    val text: Int,
    val reference: Int,
    val lattice: Float,
    val stars: Boolean = false,
) {
    Emerald("Emerald", 0xFF0F5B45.toInt(), 0xFF06281E.toInt(), 0xFFF7EEDC.toInt(), 0xFFE9DFC9.toInt(), 0xFFE2C27D.toInt(), 0.13f),
    Midnight("Midnight", 0xFF16244A.toInt(), 0xFF070D19.toInt(), 0xFFF7EEDC.toInt(), 0xFFDCD6E6.toInt(), 0xFFE2C27D.toInt(), 0.09f, stars = true),
    Ivory("Ivory", 0xFFFBF6EA.toInt(), 0xFFEFE2C6.toInt(), 0xFF1F1A10.toInt(), 0xFF3A3122.toInt(), 0xFF8A6A2B.toInt(), 0.16f),
    Rose("Rose", 0xFF8E2D4C.toInt(), 0xFF3F0F21.toInt(), 0xFFF7EEDC.toInt(), 0xFFF1DCE2.toInt(), 0xFFE9CF8F.toInt(), 0.12f),
}

private val GOLD = intArrayOf(0xFF8A6A2B.toInt(), 0xFFE9CF8F.toInt(), 0xFFB8913F.toInt(), 0xFFF3E2B0.toInt(), 0xFF9C7A34.toInt())

/**
 * Draws a verse or dua as a luxurious card: a jewel or ivory ground, the gold star lattice,
 * a double gold frame with star corners, and the text centred in a calm pool of colour.
 */
internal fun renderShareCard(
    context: Context,
    arabic: String,
    translation: String?,
    reference: String,
    look: ShareStyle,
    scale: Float = 3f,
): Bitmap {
    val width = (400 * scale).toInt()
    val pad = 52 * scale
    val contentWidth = (width - 2 * pad).toInt()

    fun layout(text: String, paint: TextPaint, spacing: Float, rtl: Boolean = false): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, contentWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setTextDirection(if (rtl) TextDirectionHeuristics.RTL else TextDirectionHeuristics.FIRSTSTRONG_LTR)
            .setLineSpacing(spacing, 1f)
            .setIncludePad(true)
            .build()

    val arabicPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = runCatching { ResourcesCompat.getFont(context, R.font.amiri_quran) }.getOrNull() ?: Typeface.SERIF
        textSize = 29 * scale
        color = look.arabic
    }
    val translationPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
        textSize = 16.5f * scale
        color = look.text
    }
    val referencePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        textSize = 13.5f * scale
        letterSpacing = 0.06f
        color = look.reference
    }
    val brandPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 10.5f * scale
        letterSpacing = 0.28f
        color = look.reference
        alpha = 210
    }

    val arabicLayout = layout(arabic, arabicPaint, 12 * scale, rtl = true)
    val translationLayout = translation?.let { layout(it, translationPaint, 5 * scale) }
    val referenceLayout = layout(reference, referencePaint, 0f)
    val brandLayout = layout("QURAN FOR WOMEN", brandPaint, 0f)

    val gap = 22 * scale
    val ornament = 16 * scale
    var height = pad + ornament + gap + arabicLayout.height
    if (translationLayout != null) height += gap + translationLayout.height
    height += gap + ornament + gap * 0.6f + referenceLayout.height + 10 * scale + brandLayout.height + pad
    val h = height.toInt()

    val bitmap = Bitmap.createBitmap(width, h, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val w = width.toFloat()
    val hf = h.toFloat()

    // Ground.
    canvas.drawRect(0f, 0f, w, hf, Paint().apply { shader = LinearGradient(0f, 0f, w * 0.3f, hf, look.top, look.bottom, Shader.TileMode.CLAMP) })
    if (look.stars) {
        val rnd = java.util.Random(7)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        repeat(120) {
            p.color = 0xFFFFFFFF.toInt()
            p.alpha = 40 + rnd.nextInt(120)
            canvas.drawCircle(rnd.nextFloat() * w, rnd.nextFloat() * hf, (0.6f + rnd.nextFloat() * 1.3f) * scale, p)
        }
    }
    // The gold star lattice.
    val goldShader = LinearGradient(0f, 0f, w, w * 0.35f, GOLD, null, Shader.TileMode.MIRROR)
    val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = goldShader
        strokeWidth = 0.8f * scale
        strokeCap = Paint.Cap.ROUND
        alpha = (255 * look.lattice).toInt()
    }
    val cell = 46 * scale
    for (s in Arabesque.lattice(w, hf, cell, cell / 2, cell / 2)) canvas.drawLine(s[0], s[1], s[2], s[3], line)
    // A calm pool behind the words, so they read cleanly.
    canvas.drawRect(0f, 0f, w, hf, Paint().apply {
        shader = RadialGradient(w / 2, hf / 2, maxOf(w, hf) * 0.62f, intArrayOf(look.bottom and 0x00FFFFFF or (0xD0 shl 24), look.bottom and 0x00FFFFFF), floatArrayOf(0.35f, 1f), Shader.TileMode.CLAMP)
        alpha = if (look == ShareStyle.Ivory) 0 else 150
    })
    if (look == ShareStyle.Ivory) {
        canvas.drawRect(0f, 0f, w, hf, Paint().apply {
            shader = RadialGradient(w / 2, hf / 2, maxOf(w, hf) * 0.6f, intArrayOf(0xEEFBF6EA.toInt(), 0x00FBF6EA), floatArrayOf(0.3f, 1f), Shader.TileMode.CLAMP)
        })
    }

    // Double gold frame with star corners.
    val goldFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = goldShader }
    val frame = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = goldShader; style = Paint.Style.STROKE }
    val inset = 18 * scale
    frame.strokeWidth = 1.6f * scale
    canvas.drawRect(inset, inset, w - inset, hf - inset, frame)
    frame.strokeWidth = 0.7f * scale
    val i2 = inset + 5 * scale
    canvas.drawRect(i2, i2, w - i2, hf - i2, frame)
    for ((cx, cy) in listOf(inset to inset, w - inset to inset, inset to hf - inset, w - inset to hf - inset)) {
        canvas.drawPath(starPath(cx, cy, 8 * scale), goldFill)
    }

    var y = pad
    fun ornamentRow() {
        val cy = y + ornament / 2
        val lp = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = goldShader; strokeWidth = 0.9f * scale }
        canvas.drawLine(w / 2 - 70 * scale, cy, w / 2 - 14 * scale, cy, lp)
        canvas.drawLine(w / 2 + 14 * scale, cy, w / 2 + 70 * scale, cy, lp)
        canvas.drawPath(starPath(w / 2, cy, 8 * scale), goldFill)
        y += ornament
    }
    fun draw(l: StaticLayout) {
        canvas.save()
        canvas.translate(pad, y)
        l.draw(canvas)
        canvas.restore()
        y += l.height
    }
    ornamentRow()
    y += gap
    draw(arabicLayout)
    translationLayout?.let { y += gap; draw(it) }
    y += gap
    ornamentRow()
    y += gap * 0.6f
    draw(referenceLayout)
    y += 10 * scale
    draw(brandLayout)
    return bitmap
}

private fun starPath(cx: Float, cy: Float, r: Float, inner: Float = 0.6f): Path = Path().apply {
    for (k in 0 until 16) {
        val a = Math.toRadians(k * 22.5 - 90.0)
        val rr = if (k % 2 == 0) r else r * inner
        val x = cx + (rr * cos(a)).toFloat()
        val y = cy + (rr * sin(a)).toFloat()
        if (k == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

internal fun saveShareCard(context: Context, bitmap: Bitmap): File {
    val file = context.sharedCacheFile("verse.png")
    FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    bitmap.recycle()
    return file
}

/** The last style she chose, kept for the next share. */
private var lastStyle = ShareStyle.Emerald

/**
 * Pick how the verse looks before sharing it: four styles side by side, drawn live with her verse.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ShareStudioSheet(
    arabic: String,
    translation: String?,
    reference: String,
    shareTitle: String,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptics = rememberHaptics()
    var chosen by remember { mutableStateOf(lastStyle) }
    val previews = remember { mutableStateMapOf<ShareStyle, androidx.compose.ui.graphics.ImageBitmap>() }
    LaunchedEffect(arabic, translation) {
        for (style in ShareStyle.entries) {
            val bmp = withContext(Dispatchers.Default) { renderShareCard(context, arabic, translation, reference, style, scale = 1f) }
            previews[style] = bmp.asImageBitmap()
        }
    }

    AppSheet(onDismiss = onDismiss) {
        Text(
            "Share as image",
            style = TextStyles.headline,
            color = Palette.textPrimary,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = Space.s, bottom = Space.l).semantics { heading() },
        )
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = Space.page),
            horizontalArrangement = Arrangement.spacedBy(Space.m),
            verticalAlignment = Alignment.Top,
        ) {
            ShareStyle.entries.forEach { style ->
                val isChosen = style == chosen
                Column(
                    Modifier
                        .width(150.dp)
                        .pressable(scale = 0.97f) { if (!isChosen) haptics.tick(); chosen = style }
                        .semantics(mergeDescendants = true) {
                            role = Role.RadioButton
                            selected = isChosen
                            contentDescription = "${style.title} style"
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Space.xs),
                ) {
                    val shape = RoundedCornerShape(14.dp)
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp)
                            .clip(shape)
                            .border(if (isChosen) 3.dp else 1.dp, if (isChosen) Arabesque.gold else Palette.border, shape),
                        contentAlignment = Alignment.Center,
                    ) {
                        previews[style]?.let {
                            Image(it, null, contentScale = ContentScale.FillWidth, modifier = Modifier.fillMaxWidth())
                        }
                    }
                    Text(
                        style.title,
                        style = TextStyles.footnote.copy(fontWeight = if (isChosen) FontWeight.SemiBold else FontWeight.Normal),
                        color = if (isChosen) Palette.textPrimary else Palette.textSecondary,
                    )
                }
            }
        }
        PrimaryButton(
            "Share",
            onClick = {
                lastStyle = chosen
                scope.launch {
                    val file = withContext(Dispatchers.IO) {
                        saveShareCard(context, renderShareCard(context, arabic, translation, reference, chosen))
                    }
                    context.shareFile(file, "image/png", shareTitle)
                    onDismiss()
                }
            },
            modifier = Modifier.padding(horizontal = Space.page).padding(top = Space.xl),
        )
    }
}
