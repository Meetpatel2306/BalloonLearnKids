package com.meetpatel.bubblelearnkids

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
import com.meetpatel.bubblelearnkids.audio.Ambience
import com.meetpatel.bubblelearnkids.audio.AnimalVoices
import com.meetpatel.bubblelearnkids.audio.Speaker
import com.meetpatel.bubblelearnkids.audio.ToneEngine
import com.meetpatel.bubblelearnkids.game.GameMode
import com.meetpatel.bubblelearnkids.ui.GameScreen
import com.meetpatel.bubblelearnkids.ui.HomeScreen
import com.meetpatel.bubblelearnkids.ui.SplashScreen

private sealed interface Screen {
    data object Splash : Screen
    data object Home : Screen
    data class Play(val mode: GameMode) : Screen
}

class MainActivity : ComponentActivity() {

    private lateinit var tones: ToneEngine
    private lateinit var ambience: Ambience
    private lateinit var speaker: Speaker
    private lateinit var animals: AnimalVoices
    private lateinit var haptics: Haptics
    private lateinit var prefs: Prefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tones = ToneEngine(applicationContext)
        ambience = Ambience(applicationContext)
        speaker = Speaker(applicationContext)
        animals = AnimalVoices(applicationContext)
        haptics = Haptics(applicationContext)
        prefs = Prefs(applicationContext)

        // A child will not understand a screen that goes dark mid-game.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        goImmersive()

        setContent {
            MaterialTheme {
                var screen: Screen by remember { mutableStateOf<Screen>(Screen.Splash) }

                // The system back gesture leaves a game but never leaves the app
                // from the menu, so a stray swipe cannot dump a child onto the
                // home screen mid-play.
                BackHandler(enabled = screen is Screen.Play) { screen = Screen.Home }

                Surface(Modifier.fillMaxSize()) {
                    when (val s = screen) {
                        is Screen.Splash -> SplashScreen(
                            onDone = { screen = Screen.Home },
                            tones = tones,
                            soundEnabled = prefs.soundEnabled,
                        )

                        is Screen.Home -> HomeScreen(
                            prefs = prefs,
                            tones = tones,
                            haptics = haptics,
                            onStart = { mode -> screen = Screen.Play(mode) },
                        )

                        is Screen.Play -> GameScreen(
                            mode = s.mode,
                            tones = tones,
                            ambience = ambience,
                            speaker = speaker,
                            animals = animals,
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

    // Silence the background soundscape whenever the app is not on screen, and
    // bring it back (only if a game still wants it) when the app returns.
    override fun onStop() {
        ambience.pause()
        super.onStop()
    }

    override fun onStart() {
        super.onStart()
        ambience.resume()
    }

    override fun onDestroy() {
        tones.release()
        ambience.release()
        speaker.shutdown()
        animals.release()
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
