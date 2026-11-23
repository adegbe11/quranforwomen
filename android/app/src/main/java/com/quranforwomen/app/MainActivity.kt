package com.quranforwomen.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.quranforwomen.app.data.TranslationStore
import com.quranforwomen.app.feature.onboarding.OnboardingFlow
import com.quranforwomen.app.feature.splash.LaunchBloom
import com.quranforwomen.app.nav.LocalRouter
import com.quranforwomen.app.nav.MainScaffold
import com.quranforwomen.app.ui.components.LocalUndo
import com.quranforwomen.app.ui.components.UndoState
import com.quranforwomen.app.ui.theme.LocalDark
import com.quranforwomen.app.ui.theme.QuranTheme
import com.quranforwomen.app.widget.VerseWidget

/** AppCompat is used only so the system biometric prompt can attach for the journal lock. */
class MainActivity : AppCompatActivity() {
    private val graph by lazy { AppGraph.get(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        // The emerald launch screen stays until the Quran text is loaded, so the main thread never waits on it.
        installSplashScreen().setKeepOnScreenCondition { !graph.contentReady.isCompleted }
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        // A cold start plays the splash once. Rotations and returning to the app do not.
        val coldStart = savedInstanceState == null
        handle(intent)
        if (graph.contentReady.isCompleted) show(coldStart)
        else lifecycleScope.launch { graph.contentReady.await(); show(coldStart) }
    }

    private fun show(coldStart: Boolean) {
        setContent {
            QuranTheme(graph.prefs.appearance) {
                CompositionLocalProvider(
                    LocalGraph provides graph,
                    LocalRouter provides graph.router,
                    LocalUndo provides remember { UndoState() },
                ) {
                    AppRoot(coldStart)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    private fun handle(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme == "quranforwomen") {
            graph.prefs.hasOnboarded = true
            graph.router.handle(data)
        }
    }
}

@Composable
private fun AppRoot(coldStart: Boolean) {
    val graph = LocalGraph.current
    val prefs = graph.prefs
    var launching by rememberSaveable { mutableStateOf(coldStart) }
    val context = androidx.compose.ui.platform.LocalContext.current

    SystemBarIcons(light = launching || (LocalDark.current && prefs.hasOnboarded))

    Box(Modifier.fillMaxSize()) {
        Crossfade(prefs.hasOnboarded, animationSpec = tween(500), label = "root") { onboarded ->
            if (onboarded) MainScaffold() else OnboardingFlow(onDone = { prefs.hasOnboarded = true })
        }
        if (launching) LaunchBloom { launching = false }
    }

    // The translation language is chosen during onboarding, so fetch it after that.
    LaunchedEffect(prefs.hasOnboarded) {
        if (prefs.hasOnboarded) {
            graph.translations.prepareSelected()
            VerseWidget.refresh(context)
        }
    }
    LaunchedEffect(graph.translations.selectedState) {
        if (graph.translations.selectedState == TranslationStore.State.Ready) VerseWidget.refresh(context)
    }
    com.quranforwomen.app.widget.WidgetSync()

    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                // Keep the next prayer notification set.
                Lifecycle.Event.ON_RESUME -> graph.prayer.refreshNotifications()
                Lifecycle.Event.ON_STOP -> if (graph.journal.lockEnabled) {
                    graph.journal.relock()
                    // Close every private page, wherever it was opened from.
                    graph.router.closePrivatePages()
                }
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
}

/** Light (white) status and navigation bar icons while on a dark surface, such as the night sky. */
@Composable
fun SystemBarIcons(light: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    val dark = LocalDark.current
    DisposableEffect(light, dark) {
        val window = (view.context as? android.app.Activity)?.window ?: return@DisposableEffect onDispose { }
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = !light
        controller.isAppearanceLightNavigationBars = !light
        // Leaving a dark screen hands the icons back to the app's normal colour for the theme.
        onDispose {
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
    }
}
