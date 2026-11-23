package com.quranforwomen.app.feature.onboarding

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranforwomen.app.R
import com.quranforwomen.app.ui.components.LotusMark
import com.quranforwomen.app.ui.components.pressable
import com.quranforwomen.app.ui.components.symbol
import com.quranforwomen.app.ui.theme.AppFont
import com.quranforwomen.app.ui.theme.LocalReduceMotion
import com.quranforwomen.app.ui.theme.Motion
import com.quranforwomen.app.ui.theme.Palette
import com.quranforwomen.app.ui.theme.Space
import com.quranforwomen.app.ui.theme.TextStyles
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The first screen: a quiet photograph of a woman reading the Quran, one verse,
 * the name, and one clear action. One orchestrated entrance, then stillness.
 * Get Started leaves in one move: the photo drifts up and fades, the words fade together,
 * and the button glides to where the next screen's Continue button sits, so it never jumps.
 */
@Composable
internal fun OnboardingWelcome(onGetStarted: () -> Unit, onContinue: () -> Unit) {
    val reduce = LocalReduceMotion.current
    val scope = rememberCoroutineScope()
    val photo = remember { Animatable(0f) }
    val verse = remember { Animatable(0f) }
    val title = remember { Animatable(0f) }
    val actions = remember { Animatable(0f) }
    var leaving by remember { mutableStateOf(false) }
    val leave by animateFloatAsState(
        if (leaving) 1f else 0f,
        tween(if (reduce) 200 else 350, easing = FastOutSlowInEasing),
        label = "leave",
    )

    // Photo settles, the verse comes into focus, then the name, then the action.
    LaunchedEffect(Unit) {
        if (reduce) {
            val spec = tween<Float>(400, easing = LinearOutSlowInEasing)
            launch { photo.animateTo(1f, spec) }
            launch { verse.animateTo(1f, spec) }
            launch { title.animateTo(1f, spec) }
            launch { actions.animateTo(1f, spec) }
            return@LaunchedEffect
        }
        launch { photo.animateTo(1f, tween(1100, easing = LinearOutSlowInEasing)) }
        launch { verse.animateTo(1f, Motion.entrance(300)) }
        launch { title.animateTo(1f, Motion.entrance(500)) }
        launch { actions.animateTo(1f, Motion.entrance(750)) }
    }

    /** Everything but the button leaves together, then the next step takes over. */
    fun getStarted() {
        if (leaving) return
        leaving = true
        scope.launch {
            delay(if (reduce) 200 else 350)
            onGetStarted()
        }
    }

    val background = Palette.background
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val bars = WindowInsets.systemBars.asPaddingValues()
        val topInset = bars.calculateTopPadding()
        val bottomInset = bars.calculateBottomPadding()
        val photoHeight = (maxHeight - topInset - bottomInset) * 0.64f + topInset

        // The photograph, with a long eased fade into the cream so there is no edge on tall screens.
        Box(
            Modifier
                .fillMaxWidth()
                .height(photoHeight)
                .graphicsLayer {
                    alpha = photo.value * (1f - leave)
                    translationY = if (reduce) 0f else -30.dp.toPx() * leave
                }
                .clipToBounds()
                .clearAndSetSemantics { },
        ) {
            Image(
                painterResource(R.drawable.welcome_photo),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // A gentle settle, like a camera finding focus.
                        val s = if (reduce) 1f else 1.06f - 0.06f * photo.value
                        scaleX = s; scaleY = s
                    },
            )
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(photoHeight * 0.34f)
                    .background(
                        Brush.verticalGradient(
                            0f to background.copy(alpha = 0f),
                            0.4f to background.copy(alpha = 0.35f),
                            0.75f to background.copy(alpha = 0.8f),
                            1f to background,
                        )
                    ),
            )
        }

        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Space.xl)
                    .padding(top = topInset + Space.xxl)
                    .graphicsLayer { alpha = 1f - leave },
            ) {
                WelcomeVerse(
                    Modifier
                        .graphicsLayer { alpha = verse.value }
                        .softBlur { if (reduce) 0f else (1f - verse.value) * 8f },
                )
                Spacer(Modifier.width(Space.m).weight(1f))
            }

            Spacer(Modifier.weight(1f))

            BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = Space.xl)) {
                // Shrinks a little on narrow phones so the name stays on one line.
                val nameSize = minOf(40f, maxWidth.value / 7.8f)
                Column(
                    Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = title.value * (1f - leave)
                            translationY = if (reduce) 0f else ((1f - title.value) * 8f - 10f * leave).dp.toPx()
                        }
                        .softBlur { if (reduce) 0f else (1f - title.value) * 10f },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Space.s),
                ) {
                    LotusMark(Modifier.size(48.dp, 34.dp), color = Palette.accentFill, strokeWidth = 1.6.dp)
                    Text(
                        "Quran for Women",
                        style = TextStyle(fontFamily = AppFont.serif, fontSize = nameSize.sp, fontWeight = FontWeight.Medium),
                        color = Palette.accentFill,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        "Your Daily Companion to the Quran,\nDua & a Peaceful Heart",
                        style = TextStyle(fontFamily = AppFont.serif, fontSize = 20.sp, lineHeight = 27.sp, textAlign = TextAlign.Center),
                        color = Palette.textSecondary,
                    )
                }
            }

            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Space.xl)
                    .padding(top = Space.xxl, bottom = Space.s + bottomInset)
                    .graphicsLayer {
                        alpha = actions.value
                        translationY = if (reduce) 0f else (1f - actions.value) * 12.dp.toPx()
                    },
                verticalArrangement = Arrangement.spacedBy(Space.m),
            ) {
                OnboardingButton(
                    text = "Get Started",
                    icon = symbol("arrow.right"),
                    enabled = !leaving,
                    dimWhenDisabled = false,
                    modifier = Modifier.graphicsLayer {
                        translationY = if (reduce) 0f else GlideToFooter.toPx() * leave
                    },
                ) { getStarted() }

                Row(
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .heightIn(min = 44.dp)
                        .graphicsLayer { alpha = 1f - leave }
                        .pressable(scale = 0.97f, enabled = !leaving, onClick = onContinue)
                        .clearAndSetSemantics {
                            contentDescription = "Already started? Continue"
                            role = Role.Button
                            onClick { onContinue(); true }
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text("Already started?", style = TextStyles.subheadline, color = Palette.textSecondary)
                    Text(
                        "Continue",
                        style = TextStyles.subheadline.copy(fontWeight = FontWeight.SemiBold, textDecoration = TextDecoration.Underline),
                        color = Palette.accentFill,
                    )
                }
            }
        }
    }
}

/**
 * From the Get Started button down to the next step's footer button: the link below it (44),
 * the gap between them (Space.m) and the padding difference (Space.s against Space.m).
 */
private val GlideToFooter = 44.dp + Space.m + Space.s - Space.m

@Composable
private fun WelcomeVerse(modifier: Modifier = Modifier) {
    val glow = Palette.background
    Column(
        modifier
            // A soft light behind the words keeps them readable over any part of the photo.
            .drawBehind {
                val r = 218.dp.toPx()
                val c = Offset(0f, size.height / 2)
                drawCircle(
                    Brush.radialGradient(listOf(glow.copy(alpha = 0.75f), glow.copy(alpha = 0f)), center = c, radius = r),
                    radius = r,
                    center = c,
                )
            }
            .clearAndSetSemantics {
                contentDescription = "Verily in the remembrance of Allah do hearts find rest. Quran, Ar-Ra'd, 13:28, Pickthall translation."
            },
        verticalArrangement = Arrangement.spacedBy(Space.s),
    ) {
        Text(
            "“Verily in the remembrance\nof Allah do hearts\nfind rest!”",
            style = TextStyle(fontFamily = AppFont.serif, fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.Medium),
            color = Color(0xFF12382B),
        )
        Box(Modifier.size(40.dp, 1.dp).background(Color(0xFF1F4A39).copy(alpha = 0.35f)))
        Text(
            "Ar-Ra'd 13:28",
            style = TextStyles.footnote.copy(fontWeight = FontWeight.Medium),
            color = Color(0xFF4F5B55),
        )
    }
}
