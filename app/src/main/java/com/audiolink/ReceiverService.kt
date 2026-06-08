package com.audiolink

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import java.io.DataInputStream
import java.net.Socket

class ReceiverService : Service() {

    companion object {
        const val EXTRA_HOST = "HOST"
        const val EXTRA_PORT = "PORT"
        private const val CHANNEL_ID = "receiver_channel"
        private const val NOTIF_ID = 2
    }

    private var isRunning = false
    private var socket: Socket? = null
    private var audioTrack: AudioTrack? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification())

        val host = intent?.getStringExtra(EXTRA_HOST)
        val port = intent?.getIntExtra(EXTRA_PORT, SettingsManager.getAudioPort(this))
            ?: SettingsManager.getAudioPort(this)

        if (host.isNullOrBlank()) {
            LogManager.logReceiver("ERROR: No host IP")
            stopSelf()
            return START_NOT_STICKY
        }

        isRunning = true
        startReceiving(host, port)
        return START_STICKY
    }

    private fun startReceiving(host: String, port: Int) {
        val maxRetries  = SettingsManager.getRetryCount(this)
        val retryDelay  = SettingsManager.getRetryDelayMs(this)
        val sampleRate  = SettingsManager.getSampleRate(this)
        val bufferSize  = SettingsManager.getBufferSize(this)

        LogManager.logReceiver("Config: ${sampleRate}Hz, ${bufferSize}B buffer")

        Thread {
            var retries = 0
            while (isRunning && retries < maxRetries) {
                try {
                    LogManager.logReceiver("Connecting to $host:$port (attempt ${retries + 1})...")
                    socket = Socket(host, port)
                    socket?.tcpNoDelay = true
                    socket?.setReceiveBufferSize(bufferSize * 2)
                    LogManager.logReceiver("Connected!")
                    setupAudioTrack(sampleRate, bufferSize)
                    receiveAndPlay()
                    retries = 0
                } catch (e: Exception) {
                    LogManager.logReceiver("Failed: ${e.message}")
                    retries++
                    if (isRunning) Thread.sleep(retryDelay)
                }
            }
            if (retries >= maxRetries) {
                LogManager.logReceiver("ERROR: Could not connect after $maxRetries attempts")
                stopSelf()
            }
        }.start()
    }

    private fun setupAudioTrack(sampleRate: Int, bufferSize: Int) {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        audioManager.mode = AudioManager.MODE_NORMAL
        audioManager.isSpeakerphoneOn = false
        audioManager.isBluetoothScoOn = false
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVol, 0)

        val minBuffer = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val actualBuffer = maxOf(bufferSize, minBuffer)

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build()
            )
            .setBufferSizeInBytes(actualBuffer)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack?.setVolume(AudioTrack.getMaxVolume())
        audioTrack?.play()
        LogManager.logReceiver("Playback ready: ${sampleRate}Hz, ${actualBuffer}B buffer")
    }

    private fun receiveAndPlay() {
        try {
            val inp = DataInputStream(socket!!.getInputStream())
            LogManager.logReceiver("Receiving stream...")
            while (isRunning && socket?.isConnected == true) {
                val size = inp.readInt()
                if (size <= 0 || size > 65536) continue
                val buffer = ByteArray(size)
                inp.readFully(buffer)
                audioTrack?.write(buffer, 0, size)
            }
        } catch (e: Exception) {
            LogManager.logReceiver("Stream ended: ${e.message}")
        } finally {
            audioTrack?.stop()
            audioTrack?.release()
            audioTrack = null
            socket?.close()
            LogManager.logReceiver("Playback stopped")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Audio Receiver", NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AudioLink — Receiving")
            .setContentText("Playing audio from sender")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        audioTrack?.stop()
        audioTrack?.release()
        socket?.close()
        LogManager.logReceiver("Receiver stopped")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
