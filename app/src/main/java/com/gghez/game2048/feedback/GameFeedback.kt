package com.gghez.game2048.feedback

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/** Haptic + audio feedback for board events. Each call is gated by user settings. */
interface GameFeedback {
    fun onMove(vibrate: Boolean, sound: Boolean)
    fun onMerge(vibrate: Boolean, sound: Boolean)
    fun release()
}

/**
 * Android implementation.
 *
 * Sound is synthesized in-process (a short decaying sine "click") and played via
 * [AudioTrack], so the app ships no audio assets. Tracks are created lazily on first
 * use, so nothing is allocated when sound stays off. Vibration uses the platform
 * Vibrator with a one-shot effect (with a pre-API-26 fallback).
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
        if (sound) play(moveTrack ?: buildClick(MOVE_FREQ, MOVE_DURATION_MS).also { moveTrack = it })
    }

    override fun onMerge(vibrate: Boolean, sound: Boolean) {
        if (vibrate) vibrate(MERGE_VIBRATION_MS)
        if (sound) play(mergeTrack ?: buildClick(MERGE_FREQ, MERGE_DURATION_MS).also { mergeTrack = it })
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

    /** Builds a static AudioTrack holding a short decaying-sine click. */
    private fun buildClick(freq: Double, durationMs: Int): AudioTrack {
        val count = SAMPLE_RATE * durationMs / 1000
        val samples = ShortArray(count)
        for (i in 0 until count) {
            val t = i.toDouble() / SAMPLE_RATE
            val envelope = exp(-t * DECAY)
            val value = sin(2.0 * PI * freq * t) * envelope * AMPLITUDE
            samples[i] = (value * Short.MAX_VALUE).toInt().toShort()
        }
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
        const val AMPLITUDE = 0.45
        const val DECAY = 32.0
        const val MOVE_FREQ = 660.0
        const val MERGE_FREQ = 392.0
        const val MOVE_DURATION_MS = 60
        const val MERGE_DURATION_MS = 95
        const val MOVE_VIBRATION_MS = 12L
        const val MERGE_VIBRATION_MS = 35L
    }
}
