package com.audiolink

import android.content.Context
import android.content.SharedPreferences

object SettingsManager {

    private const val PREFS_NAME = "audiolink_settings"

    const val KEY_SAMPLE_RATE = "sample_rate"
    const val KEY_BUFFER_SIZE = "buffer_size"
    const val KEY_AUDIO_PORT = "audio_port"
    const val KEY_DISCOVERY_PORT = "discovery_port"
    const val KEY_SCAN_TIMEOUT = "scan_timeout"
    const val KEY_RETRY_COUNT = "retry_count"
    const val KEY_RETRY_DELAY = "retry_delay"

    // Defaults — Movie preset
    const val DEFAULT_SAMPLE_RATE = 44100
    const val DEFAULT_BUFFER_SIZE = 2048
    const val DEFAULT_AUDIO_PORT = 9999
    const val DEFAULT_DISCOVERY_PORT = 9998
    const val DEFAULT_SCAN_TIMEOUT = 8
    const val DEFAULT_RETRY_COUNT = 10
    const val DEFAULT_RETRY_DELAY = 2

    // Sample rate options
    val SAMPLE_RATES = listOf(
        8000  to "8000 Hz — Phone Call",
        11025 to "11025 Hz — Voice",
        16000 to "16000 Hz — Low",
        22050 to "22050 Hz — Game",
        44100 to "44100 Hz — Standard",
        48000 to "48000 Hz — Studio"
    )

    // Buffer size options
    val BUFFER_SIZES = listOf(
        256   to "256 B — Ultra Low",
        512   to "512 B — Minimum",
        1024  to "1024 B — Low",
        2048  to "2048 B — Balanced",
        4096  to "4096 B — Medium",
        8192  to "8192 B — High",
        16384 to "16384 B — Maximum"
    )

    // Presets
    data class Preset(
        val name: String,
        val sampleRate: Int,
        val bufferSize: Int
    )

    val PRESET_GAME  = Preset("Game",  22050, 512)
    val PRESET_MOVIE = Preset("Movie", 44100, 2048)
    val PRESET_MUSIC = Preset("Music", 48000, 4096)

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getSampleRate(context: Context): Int =
        prefs(context).getInt(KEY_SAMPLE_RATE, DEFAULT_SAMPLE_RATE)

    fun getBufferSize(context: Context): Int =
        prefs(context).getInt(KEY_BUFFER_SIZE, DEFAULT_BUFFER_SIZE)

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

    fun setBufferSize(context: Context, value: Int) =
        prefs(context).edit().putInt(KEY_BUFFER_SIZE, value).apply()

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
            .putInt(KEY_BUFFER_SIZE, preset.bufferSize)
            .apply()
    }

    fun resetAll(context: Context) =
        prefs(context).edit().clear().apply()
}
