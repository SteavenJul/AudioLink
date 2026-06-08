package com.audiolink

import android.content.Context
import android.content.SharedPreferences

object SettingsManager {

    private const val PREFS_NAME = "audiolink_settings"

    // Keys
    const val KEY_SAMPLE_RATE     = "sample_rate"
    const val KEY_BUFFER_MS       = "buffer_ms"
    const val KEY_AUDIO_PORT      = "audio_port"
    const val KEY_DISCOVERY_PORT  = "discovery_port"
    const val KEY_SCAN_TIMEOUT    = "scan_timeout"
    const val KEY_RETRY_COUNT     = "retry_count"
    const val KEY_RETRY_DELAY     = "retry_delay"

    // Defaults
    const val DEFAULT_SAMPLE_RATE    = 48000
    const val DEFAULT_BUFFER_MS      = 80
    const val DEFAULT_AUDIO_PORT     = 9999
    const val DEFAULT_DISCOVERY_PORT = 9998
    const val DEFAULT_SCAN_TIMEOUT   = 8
    const val DEFAULT_RETRY_COUNT    = 10
    const val DEFAULT_RETRY_DELAY    = 2

    // Sample rate options (sender only)
    val SAMPLE_RATES = listOf(
        8000  to "8000 Hz — Phone Call",
        11025 to "11025 Hz — Voice",
        16000 to "16000 Hz — Low",
        22050 to "22050 Hz — Game ⚡",
        44100 to "44100 Hz — Standard 🎬",
        48000 to "48000 Hz — Studio 🎵"
    )

    // Buffer options in ms (receiver only)
    val BUFFER_OPTIONS_MS = listOf(
        0    to "0ms — No buffer (choppy)",
        5    to "5ms — Experimental",
        10   to "10ms — Ultra Low ⚡",
        20   to "20ms — Game",
        40   to "40ms — Low",
        80   to "80ms — Movie 🎬",
        150  to "150ms — Music 🎵",
        200  to "200ms — Stable",
        500  to "500ms — Maximum"
    )

    // Presets
    data class Preset(val name: String, val sampleRate: Int, val bufferMs: Int)

    val PRESET_GAME  = Preset("Game",  22050, 10)
    val PRESET_MOVIE = Preset("Movie", 44100, 80)
    val PRESET_MUSIC = Preset("Music", 48000, 150)

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getSampleRate(context: Context): Int =
        prefs(context).getInt(KEY_SAMPLE_RATE, DEFAULT_SAMPLE_RATE)

    fun getBufferMs(context: Context): Int =
        prefs(context).getInt(KEY_BUFFER_MS, DEFAULT_BUFFER_MS)

    // Convert ms to bytes based on sample rate
    fun getBufferBytes(context: Context, sampleRate: Int): Int {
        val ms = getBufferMs(context)
        if (ms == 0) return 0
        return (sampleRate * 2 * 2 * ms) / 1000
    }

    fun getAudioPort(context: Context): Int =
        prefs(context).getInt(KEY_AUDIO_PORT, DEFAULT_AUDIO_PORT)

    fun getDiscoveryPort(context: Context): Int =
        prefs(context).getInt(KEY_DISCOVERY_PORT, DEFAULT_DISCOVERY_PORT)

    fun getScanTimeoutMs(context: Context): Long =
        prefs(context).getInt(KEY_SCAN_TIMEOUT, DEFAULT_SCAN_TIMEOUT) * 1000L

    fun getRetryCount(context: Context): Int =
        prefs(context).getInt(KEY_RETRY_COUNT, DEFAULT_RETRY_COUNT)

    fun getRetryDelayMs(context: Context): Long =
        prefs(context).getInt(KEY_RETRY_DELAY, DEFAULT_RETRY_DELAY) * 1000L

    fun setSampleRate(context: Context, value: Int) =
        prefs(context).edit().putInt(KEY_SAMPLE_RATE, value).apply()

    fun setBufferMs(context: Context, value: Int) =
        prefs(context).edit().putInt(KEY_BUFFER_MS, value).apply()

    fun setAudioPort(context: Context, value: Int) =
        prefs(context).edit().putInt(KEY_AUDIO_PORT, value).apply()

    fun setDiscoveryPort(context: Context, value: Int) =
        prefs(context).edit().putInt(KEY_DISCOVERY_PORT, value).apply()

    fun setScanTimeout(context: Context, seconds: Int) =
        prefs(context).edit().putInt(KEY_SCAN_TIMEOUT, seconds).apply()

    fun setRetryCount(context: Context, value: Int) =
        prefs(context).edit().putInt(KEY_RETRY_COUNT, value).apply()

    fun setRetryDelay(context: Context, seconds: Int) =
        prefs(context).edit().putInt(KEY_RETRY_DELAY, seconds).apply()

    fun applyPreset(context: Context, preset: Preset) {
        prefs(context).edit()
            .putInt(KEY_SAMPLE_RATE, preset.sampleRate)
            .putInt(KEY_BUFFER_MS, preset.bufferMs)
            .apply()
    }

    fun resetAll(context: Context) =
        prefs(context).edit().clear().apply()
}
