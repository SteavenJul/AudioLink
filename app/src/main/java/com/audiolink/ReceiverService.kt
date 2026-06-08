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
import android.os.Process
import androidx.core.app.NotificationCompat
import java.io.DataInputStream
import java.net.Socket

class ReceiverService : Service() {

    companion object {
        const val EXTRA_HOST = "HOST"
        const val EXTRA_PORT = "PORT"
        private const val CHANNEL_ID = "receiver_channel"
        private const val NOTIF_ID = 2
        const val CONFIG_HEADER = 0xABCD1234.toInt()
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
        val maxRetries = SettingsManager.getRetryCount(this)
        val retryDelay = SettingsManager.getRetryDelayMs(this)

        Thread {
            var retries = 0
            while (isRunning && retries < maxRetries) {
                try {
                    LogManager.logReceiver("Connecting to $host:$port (attempt ${retries + 1})...")
                    socket = Socket(host, port)
                    socket?.tcpNoDelay = true
                    socket?.setReceiveBufferSize(65536)
                    LogManager.logReceiver("Connected!")
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

    private fun receiveAndPlay() {
        // Boost thread priority for audio
        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
        try {
            val inp = DataInputStream(socket!!.getInputStream())

            // Read config from sender
            val header     = inp.readInt()
            val sampleRate = inp.readInt()
            val chunkSize  = inp.readInt()

            if (header != CONFIG_HEADER) {
                LogManager.logReceiver("ERROR: Bad config header")
                return
            }

            LogManager.logReceiver("Config: ${sampleRate}Hz, ${chunkSize}B chunks")
            setupAudioTrack(sampleRate, chunkSize)

            while (isRunning && socket?.isConnected == true) {
                val size = inp.readInt()
                if (size <= 0 || size > 65536) continue
                val buffer = ByteArray(size)
                inp.readFully(buffer)
                // Write immediately — no extra buffering
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

    private fun setupAudioTrack(sampleRate: Int, chunkSize: Int) {
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
        // Use minimum possible buffer — just enough to not crash
        val actualBuffer = maxOf(chunkSize * 2, minBuffer)
        LogManager.logReceiver("AudioTrack: ${sampleRate}Hz, ${actualBuffer}B buffer")

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
        LogManager.logReceiver("Audio ready!")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Audio Receiver", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AudioLink — Receiving")
            .setContentText("Playing audio from sender")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true).build()
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
