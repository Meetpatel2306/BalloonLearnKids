package com.meetpatel.balloonlearnkids.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import com.meetpatel.balloonlearnkids.R

/**
 * The background music.
 *
 * Three looping tunes — a strolling daytime one, a slow lullaby for the night
 * scenes and a bouncier one for busy play — plus a short fanfare for finishing a
 * whole set. They are real stereo recordings bundled with the app, so there is
 * nothing to download and it all still works with the phone in flight mode.
 *
 * Everything fades: swapping tracks or turning the music off never cuts a note
 * dead, which matters when a small child is holding the phone.
 */
class Music(private val context: Context) {

    enum class Track(val res: Int) {
        DAY(R.raw.music_day),
        NIGHT(R.raw.music_night),
        PLAY(R.raw.music_play),
        MEADOW(R.raw.music_meadow),
    }

    private val handler = Handler(Looper.getMainLooper())

    private var player: MediaPlayer? = null
    private var fanfare: MediaPlayer? = null

    /** What the caller has asked for, kept so pause/resume can restore it. */
    private var wanted: Track? = null
    private var enabled = true
    private var volume = 0f

    /** The level the music settles at — deliberately well under the voice. */
    private val target = 0.38f

    fun setEnabled(value: Boolean) {
        if (enabled == value) return
        enabled = value
        if (!value) fadeOutAndStop() else wanted?.let { play(it) }
    }

    /** Starts [track], crossfading if something else is already playing. */
    fun play(track: Track) {
        wanted = track
        if (!enabled) return
        if (player != null && currentTrack == track) return

        val old = player
        currentTrack = track

        val mp = MediaPlayer.create(context, track.res) ?: return
        mp.isLooping = true
        mp.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
        )
        mp.setVolume(0f, 0f)
        runCatching { mp.start() }
        player = mp
        volume = 0f
        fadeIn()

        // Let the outgoing tune die away behind the new one.
        old?.let { fadeOut(it, thenRelease = true) }
    }

    /** The little fanfare when a whole set is finished. Plays over the music. */
    fun playFanfare() {
        if (!enabled) return
        oneShot(R.raw.music_win, 0.7f)
    }

    /** A room full of children clapping. Used for the end-of-set party. */
    fun playApplause() = oneShot(R.raw.sfx_applause, 0.75f)

    /** A rising shower of bells, played as the confetti goes up. */
    fun playCheer() = oneShot(R.raw.sfx_cheer, 0.6f)

    /** The warm three-note chime for a correct answer. */
    fun playCorrect() = oneShot(R.raw.sfx_correct, 0.55f)

    /**
     * Fires a bundled clip once and lets it clean itself up. These are the
     * moments worth a real recording, so they are not synthesised live.
     */
    private fun oneShot(res: Int, volume: Float) {
        val mp = MediaPlayer.create(context, res) ?: return
        mp.setVolume(volume, volume)
        mp.setOnCompletionListener { runCatching { it.release() } }
        runCatching { mp.start() }
        fanfare = mp
    }

    fun pause() {
        player?.let { p -> runCatching { if (p.isPlaying) p.pause() } }
        fanfare?.let { p -> runCatching { if (p.isPlaying) p.pause() } }
    }

    fun resume() {
        if (!enabled) return
        player?.let { p -> runCatching { if (!p.isPlaying) p.start() } }
    }

    fun release() {
        handler.removeCallbacksAndMessages(null)
        player?.runCatching { release() }
        fanfare?.runCatching { release() }
        player = null
        fanfare = null
        currentTrack = null
    }

    // ------------------------------------------------------------- internals

    private var currentTrack: Track? = null

    private fun fadeIn() {
        handler.removeCallbacksAndMessages(null)
        val step = object : Runnable {
            override fun run() {
                val p = player ?: return
                volume = (volume + 0.02f).coerceAtMost(target)
                runCatching { p.setVolume(volume, volume) }
                if (volume < target) handler.postDelayed(this, 40)
            }
        }
        handler.post(step)
    }

    private fun fadeOut(mp: MediaPlayer, thenRelease: Boolean) {
        var v = target
        val step = object : Runnable {
            override fun run() {
                v -= 0.03f
                if (v <= 0f) {
                    runCatching { mp.stop() }
                    if (thenRelease) runCatching { mp.release() }
                    return
                }
                runCatching { mp.setVolume(v, v) }
                handler.postDelayed(this, 40)
            }
        }
        handler.post(step)
    }

    private fun fadeOutAndStop() {
        val p = player ?: return
        player = null
        currentTrack = null
        fadeOut(p, thenRelease = true)
    }
}
