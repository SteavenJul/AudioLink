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
        startForeground(NOTIF_ID, buildNotification())

        val host = intent?.getStringExtra(EXTRA_HOST) ?: "192.168.43.1"
        val port = intent?.getIntExtra(EXTRA_PORT, 9999) ?: 9999

        isRunning = true
        startReceiving(host, port)
        return START_STICKY
    }

    private fun startReceiving(host: String, port: Int) {
        Thread {
            var retries = 0
            while (isRunning && retries < 10) {
                try {
                    LogManager.logReceiver("Connecting to $host:$port (attempt ${retries + 1})...")
                    socket = Socket(host, port)
                    socket?.tcpNoDelay = true
                    socket?.setReceiveBufferSize(16384)
                    LogManager.logReceiver("Connected! Setting up audio output...")
                    setupAudioTrack()
                    receiveAndPlay()
                    retries = 0 // reset on successful connection
                } catch (e: Exception) {
                    LogManager.logReceiver("Connection failed: ${e.message}")
                    retries++
                    if (isRunning) Thread.sleep(2000)
                }
            }
            if (retries >= 10) {
                LogManager.logReceiver("ERROR: Could not connect after 10 attempts")
                LogManager.logReceiver("Make sure Sender app is running on Poco X7 Pro")
                stopSelf()
            }
        }.start()
    }

    private fun setupAudioTrack() {
        val bufferSize = AudioTrack.getMinBufferSize(
            SenderService.SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        ) * 4

        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        audioManager.mode = AudioManager.MODE_NORMAL
        audioManager.isSpeakerphoneOn = false

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SenderService.SAMPLE_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack?.setVolume(AudioTrack.getMaxVolume())
        audioTrack?.play()
        LogManager.logReceiver("Audio output ready — playing to earbuds")
    }

    private fun receiveAndPlay() {
        try {
            val inp = DataInputStream(socket!!.getInputStream())
            LogManager.logReceiver("Receiving audio stream...")

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

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Audio Receiver", NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AudioLink — Receiving")
            .setContentText("Playing audio from Poco X7 Pro")
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
        LogManager.logReceiver("Receiver service stopped")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
