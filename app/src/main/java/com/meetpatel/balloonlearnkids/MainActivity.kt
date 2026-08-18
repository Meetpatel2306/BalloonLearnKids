package com.meetpatel.balloonlearnkids

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.meetpatel.balloonlearnkids.audio.Ambience
import com.meetpatel.balloonlearnkids.audio.AnimalVoices
import com.meetpatel.balloonlearnkids.audio.Music
import com.meetpatel.balloonlearnkids.audio.Speaker
import com.meetpatel.balloonlearnkids.audio.ToneEngine
import com.meetpatel.balloonlearnkids.game.GameMode
import com.meetpatel.balloonlearnkids.ui.GameScreen
import com.meetpatel.balloonlearnkids.ui.HomeScreen
import com.meetpatel.balloonlearnkids.ui.SplashScreen

private sealed interface Screen {
    data object Splash : Screen
    data object Home : Screen
    data class Play(val mode: GameMode) : Screen
}

class MainActivity : ComponentActivity() {

    private lateinit var tones: ToneEngine
    private lateinit var ambience: Ambience
    private lateinit var music: Music
    private lateinit var speaker: Speaker
    private lateinit var animals: AnimalVoices
    private lateinit var haptics: Haptics
    private lateinit var prefs: Prefs

    /**
     * The game draws itself in fixed proportions — balloons, labels and cards are
     * all sized from the screen — so it uses its own text sizes rather than the
     * phone's. Someone with Android's text set to 2x would otherwise push the
     * second row of game balloons clean off the bottom.
     *
     * Nothing readable is lost: the welcome notice, the privacy policy and the
     * terms are all scrolling text, and Settings stays legible at these sizes.
     */
    override fun attachBaseContext(newBase: Context) {
        val config = Configuration(newBase.resources.configuration)
        config.fontScale = 1f
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tones = ToneEngine(applicationContext)
        ambience = Ambience(applicationContext)
        music = Music(applicationContext)
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

                // The menu strolls, a game bounces along. Switching screens
                // crossfades between the two rather than cutting.
                LaunchedEffect(screen) {
                    music.setEnabled(prefs.musicEnabled)
                    when (screen) {
                        is Screen.Splash -> Unit
                        is Screen.Home -> music.play(Music.Track.DAY)
                        is Screen.Play -> music.play(Music.Track.PLAY)
                    }
                }

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
                            music = music,
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
                            music = music,
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
        music.pause()
        super.onStop()
    }

    override fun onStart() {
        super.onStart()
        ambience.resume()
        music.resume()
    }

    override fun onDestroy() {
        tones.release()
        ambience.release()
        music.release()
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
