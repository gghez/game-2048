package com.gghez.game2048.feedback

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/** Haptic + audio feedback for board events. Each call is gated by user settings. */
interface GameFeedback {
    fun onMove(vibrate: Boolean, sound: Boolean)
    fun onMerge(vibrate: Boolean, sound: Boolean)
    fun release()
}

/**
 * Android implementation.
 *
 * Sound is synthesized in-process by the pure [Synth] (no audio assets) and played via
 * [AudioTrack]. Tracks are created lazily on first use, so nothing is allocated when
 * sound stays off. Vibration uses the platform Vibrator with a one-shot effect (with a
 * pre-API-26 fallback).
 */
class AndroidGameFeedback(context: Context) : GameFeedback {

    private val appContext = context.applicationContext

    private val vibrator: Vibrator? = run {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = appContext.getSystemService(VibratorManager::class.java)
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    private var moveTrack: AudioTrack? = null
    private var mergeTrack: AudioTrack? = null

    override fun onMove(vibrate: Boolean, sound: Boolean) {
        if (vibrate) vibrate(MOVE_VIBRATION_MS)
        if (sound) play(moveTrack ?: buildTrack(MOVE_SPEC).also { moveTrack = it })
    }

    override fun onMerge(vibrate: Boolean, sound: Boolean) {
        if (vibrate) vibrate(MERGE_VIBRATION_MS)
        if (sound) play(mergeTrack ?: buildTrack(MERGE_SPEC).also { mergeTrack = it })
    }

    override fun release() {
        runCatching { moveTrack?.release() }
        runCatching { mergeTrack?.release() }
        moveTrack = null
        mergeTrack = null
    }

    private fun vibrate(ms: Long) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(ms)
        }
    }

    private fun play(track: AudioTrack) {
        runCatching { track.stop() }
        runCatching { track.reloadStaticData() }
        runCatching { track.play() }
    }

    /** Wraps a [Synth]-rendered PCM buffer in a static [AudioTrack] ready to replay. */
    private fun buildTrack(spec: ToneSpec): AudioTrack {
        val samples = Synth.render(spec, SAMPLE_RATE)
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setBufferSizeInBytes(samples.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        track.write(samples, 0, samples.size)
        return track
    }

    private companion object {
        const val SAMPLE_RATE = 44100

        /**
         * Move: a soft, low-mid tick. Low amplitude and a touch of 2nd harmonic keep it
         * warm but unobtrusive, so it doesn't fatigue when it fires on every swipe.
         */
        val MOVE_SPEC = ToneSpec(
            notes = listOf(Note(freqHz = 330.0, durationMs = 50)),
            amplitude = 0.22,
            attackMs = 4.0,
            releaseMs = 10.0,
            decay = 30.0,
            harmonics = listOf(0.20),
        )

        /**
         * Merge: a short ascending two-note blip (a perfect fifth, C5 → G5). It is louder
         * and warmer than the move (richer harmonics, slower decay) and the upward step
         * reads as "combine / level up" — clearly the reward cue.
         */
        val MERGE_SPEC = ToneSpec(
            notes = listOf(
                Note(freqHz = 523.25, durationMs = 55), // C5
                Note(freqHz = 783.99, durationMs = 70), // G5
            ),
            amplitude = 0.40,
            attackMs = 4.0,
            releaseMs = 9.0,
            decay = 18.0,
            harmonics = listOf(0.25, 0.12),
        )

        const val MOVE_VIBRATION_MS = 12L
        const val MERGE_VIBRATION_MS = 35L
    }
}
