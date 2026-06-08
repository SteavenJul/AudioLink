package com.audiolink

import android.content.Context
import android.content.SharedPreferences

object SettingsManager {

    private const val PREFS_NAME = "audiolink_settings"

    const val KEY_AUDIO_PORT = "audio_port"
    const val KEY_DISCOVERY_PORT = "discovery_port"
    const val KEY_SCAN_TIMEOUT = "scan_timeout"
    const val KEY_BUFFER_MULTIPLIER = "buffer_multiplier"
    const val KEY_RETRY_COUNT = "retry_count"
    const val KEY_RETRY_DELAY = "retry_delay"

    const val DEFAULT_AUDIO_PORT = 9999
    const val DEFAULT_DISCOVERY_PORT = 9998
    const val DEFAULT_SCAN_TIMEOUT = 8
    const val DEFAULT_BUFFER_MULTIPLIER = 2
    const val DEFAULT_RETRY_COUNT = 10
    const val DEFAULT_RETRY_DELAY = 2

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getAudioPort(context: Context): Int =
        prefs(context).getInt(KEY_AUDIO_PORT, DEFAULT_AUDIO_PORT)

    fun getDiscoveryPort(context: Context): Int =
        prefs(context).getInt(KEY_DISCOVERY_PORT, DEFAULT_DISCOVERY_PORT)

    fun getScanTimeoutMs(context: Context): Long =
        prefs(context).getInt(KEY_SCAN_TIMEOUT, DEFAULT_SCAN_TIMEOUT) * 1000L

    fun getBufferMultiplier(context: Context): Int =
        prefs(context).getInt(KEY_BUFFER_MULTIPLIER, DEFAULT_BUFFER_MULTIPLIER)

    fun getRetryCount(context: Context): Int =
        prefs(context).getInt(KEY_RETRY_COUNT, DEFAULT_RETRY_COUNT)

    fun getRetryDelayMs(context: Context): Long =
        prefs(context).getInt(KEY_RETRY_DELAY, DEFAULT_RETRY_DELAY) * 1000L

    fun setAudioPort(context: Context, value: Int) =
        prefs(context).edit().putInt(KEY_AUDIO_PORT, value).apply()

    fun setDiscoveryPort(context: Context, value: Int) =
        prefs(context).edit().putInt(KEY_DISCOVERY_PORT, value).apply()

    fun setScanTimeout(context: Context, seconds: Int) =
        prefs(context).edit().putInt(KEY_SCAN_TIMEOUT, seconds).apply()

    fun setBufferMultiplier(context: Context, value: Int) =
        prefs(context).edit().putInt(KEY_BUFFER_MULTIPLIER, value).apply()

    fun setRetryCount(context: Context, value: Int) =
        prefs(context).edit().putInt(KEY_RETRY_COUNT, value).apply()

    fun setRetryDelay(context: Context, seconds: Int) =
        prefs(context).edit().putInt(KEY_RETRY_DELAY, seconds).apply()

    fun resetAll(context: Context) =
        prefs(context).edit().clear().apply()
}
