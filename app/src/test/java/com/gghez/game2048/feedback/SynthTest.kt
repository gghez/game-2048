package com.gghez.game2048.feedback

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SynthTest {

    private val sampleRate = 44100

    @Test fun bufferLengthMatchesTotalDuration() {
        val spec = ToneSpec(
            notes = listOf(Note(440.0, 50), Note(660.0, 70)),
            amplitude = 0.4,
        )
        val buf = Synth.render(spec, sampleRate)
        val expected = sampleRate * 50 / 1000 + sampleRate * 70 / 1000
        assertEquals(expected, buf.size)
    }

    @Test fun neverClipsEvenAtFullAmplitude() {
        val spec = ToneSpec(
            notes = listOf(Note(440.0, 60)),
            amplitude = 1.0,
            harmonics = listOf(0.25, 0.12),
        )
        val buf = Synth.render(spec, sampleRate)
        assertTrue(buf.all { it >= -Short.MAX_VALUE && it <= Short.MAX_VALUE })
    }

    @Test fun startsAndEndsAtSilence() {
        val spec = ToneSpec(
            notes = listOf(Note(523.25, 55), Note(783.99, 70)),
            amplitude = 0.4,
            harmonics = listOf(0.25, 0.12),
        )
        val buf = Synth.render(spec, sampleRate)
        // Attack/release ramps force the very first and very last samples to exactly 0,
        // so there is no onset/tail click.
        assertEquals(0, buf.first().toInt())
        assertEquals(0, buf.last().toInt())
    }

    @Test fun rampEdgesAreNearSilence() {
        val spec = ToneSpec(
            notes = listOf(Note(440.0, 80)),
            amplitude = 0.5,
            attackMs = 5.0,
            releaseMs = 10.0,
        )
        val buf = Synth.render(spec, sampleRate)
        // Within the first few samples the signal is still ramping up, well below peak.
        val onset = (0 until 10).maxOf { abs(buf[it].toInt()) }
        val peak = buf.maxOf { abs(it.toInt()) }
        assertTrue(onset < peak / 2, "onset $onset should be well below peak $peak")
    }

    @Test fun peakStaysWithinRequestedAmplitude() {
        val amplitude = 0.4
        val spec = ToneSpec(
            notes = listOf(Note(440.0, 80)),
            amplitude = amplitude,
            harmonics = listOf(0.25, 0.12),
        )
        val buf = Synth.render(spec, sampleRate)
        val peak = buf.maxOf { abs(it.toInt()) }
        // Conservative normalization guarantees the peak never exceeds amplitude * MAX
        // (+1 for integer rounding).
        assertTrue(
            peak <= (amplitude * Short.MAX_VALUE).toInt() + 1,
            "peak $peak exceeded amplitude bound",
        )
    }

    @Test fun pureSineReachesNearRequestedAmplitude() {
        val amplitude = 0.5
        val spec = ToneSpec(
            notes = listOf(Note(440.0, 100)),
            amplitude = amplitude,
            attackMs = 1.0,
            releaseMs = 1.0,
            decay = 0.0,
        )
        val buf = Synth.render(spec, sampleRate)
        val peak = buf.maxOf { abs(it.toInt()) }
        // With no decay and tiny ramps, a pure sine should swing close to amplitude * MAX.
        assertTrue(peak > 0.45 * Short.MAX_VALUE, "peak $peak fell short of amplitude")
    }
}
