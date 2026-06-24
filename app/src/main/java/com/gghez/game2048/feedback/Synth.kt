package com.gghez.game2048.feedback

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/** A single note: a [freqHz] tone held for [durationMs]. */
data class Note(val freqHz: Double, val durationMs: Int)

/**
 * Parameters for synthesizing a short feedback tone.
 *
 * A spec is one or more [notes] played back-to-back. A single note makes a simple
 * tick; two ascending notes make a "blip". Every note is shaped by the same envelope
 * and timbre so the whole buffer sounds like one cue.
 */
data class ToneSpec(
    /** Notes played in sequence. */
    val notes: List<Note>,
    /** Peak output amplitude, in 0..1. The rendered waveform never exceeds this. */
    val amplitude: Double,
    /** Linear fade-in at the start of each note, in ms — kills the onset click. */
    val attackMs: Double = 4.0,
    /** Linear fade-out at the end of each note, in ms — kills the tail click. */
    val releaseMs: Double = 9.0,
    /** Exponential body-decay rate; higher = the note dies away faster. */
    val decay: Double = 24.0,
    /** Amplitudes of the upper harmonics (2f, 3f, …) relative to the fundamental. */
    val harmonics: List<Double> = emptyList(),
)

/**
 * Pure, Android-free PCM synthesizer: turns a [ToneSpec] into a 16-bit mono buffer.
 *
 * The waveform is additive (fundamental + optional [ToneSpec.harmonics]) and is
 * normalized so the peak stays within [ToneSpec.amplitude] — no clipping. Each note is
 * wrapped in a linear attack/release ramp, so the buffer and every note boundary start
 * and end at exactly 0 amplitude — no audible clicks or pops. Keeping this logic free
 * of Android imports lets it be unit-tested on the JVM, per the project's pure-core stance.
 */
object Synth {

    /** Renders [spec] to a signed 16-bit PCM mono [ShortArray] at [sampleRate] Hz. */
    fun render(spec: ToneSpec, sampleRate: Int): ShortArray {
        val total = spec.notes.sumOf { sampleCount(it.durationMs, sampleRate) }
        val out = ShortArray(total)
        // Conservative normalization: the additive peak is at most the sum of the
        // component amplitudes, so dividing by it guarantees |signal| <= amplitude.
        val norm = 1.0 + spec.harmonics.sum()
        val attack = (spec.attackMs / 1000.0 * sampleRate).toInt().coerceAtLeast(1)
        val release = (spec.releaseMs / 1000.0 * sampleRate).toInt().coerceAtLeast(1)

        var offset = 0
        for (note in spec.notes) {
            val count = sampleCount(note.durationMs, sampleRate)
            for (i in 0 until count) {
                val t = i.toDouble() / sampleRate
                var wave = sin(2.0 * PI * note.freqHz * t)
                var harmonic = 2
                for (amp in spec.harmonics) {
                    wave += amp * sin(2.0 * PI * note.freqHz * harmonic * t)
                    harmonic++
                }
                val env = exp(-t * spec.decay) * ramp(i, count, attack, release)
                val value = wave / norm * spec.amplitude * env
                out[offset + i] = (value * Short.MAX_VALUE).toInt().toShort()
            }
            offset += count
        }
        return out
    }

    private fun sampleCount(durationMs: Int, sampleRate: Int): Int = sampleRate * durationMs / 1000

    /**
     * Linear attack/release window for one note: rises 0→1 over the first [attack]
     * samples and falls 1→0 over the last [release], so sample 0 and the final sample
     * are exactly 0. Whichever ramp is lower wins, so very short notes still close cleanly.
     */
    private fun ramp(i: Int, count: Int, attack: Int, release: Int): Double {
        val rise = if (i < attack) i.toDouble() / attack else 1.0
        val fall = if (i >= count - release) (count - 1 - i).toDouble() / release else 1.0
        return minOf(rise, fall).coerceAtLeast(0.0)
    }
}
