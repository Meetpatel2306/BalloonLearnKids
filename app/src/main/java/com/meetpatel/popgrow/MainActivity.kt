package com.meetpatel.popgrow

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.meetpatel.popgrow.audio.ToneEngine
import com.meetpatel.popgrow.ui.GameScreen
import com.meetpatel.popgrow.ui.HomeScreen

private sealed interface Screen {
    data object Home : Screen
    data class Play(val twoPlayer: Boolean) : Screen
}

class MainActivity : ComponentActivity() {

    private lateinit var tones: ToneEngine
    private lateinit var haptics: Haptics
    private lateinit var prefs: Prefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tones = ToneEngine(applicationContext)
        haptics = Haptics(applicationContext)
        prefs = Prefs(applicationContext)

        // A child will not understand a screen that goes dark mid-game.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        goImmersive()

        setContent {
            MaterialTheme {
                var screen: Screen by remember { mutableStateOf(Screen.Home) }

                // The system back gesture leaves a game but never leaves the app
                // from the menu, so a stray swipe cannot dump a child onto the
                // home screen mid-play.
                BackHandler(enabled = screen != Screen.Home) { screen = Screen.Home }

                Surface(Modifier.fillMaxSize()) {
                    when (val s = screen) {
                        is Screen.Home -> HomeScreen(
                            prefs = prefs,
                            onStart = { twoPlayer -> screen = Screen.Play(twoPlayer) },
                        )

                        is Screen.Play -> GameScreen(
                            twoPlayer = s.twoPlayer,
                            tones = tones,
                            haptics = haptics,
                            prefs = prefs,
                            onExit = { screen = Screen.Home },
                        )
                    }
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) goImmersive()
    }

    override fun onDestroy() {
        tones.release()
        super.onDestroy()
    }

    /**
     * Full-screen with the bars hidden until a deliberate swipe. On a tablet
     * handed to a toddler, a visible navigation bar is just three more buttons
     * to press by accident.
     */
    private fun goImmersive() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }
}
