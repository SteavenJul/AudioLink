package com.audiolink

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import java.io.DataOutputStream
import java.net.ServerSocket
import java.net.Socket

class SenderService : Service() {

    companion object {
        const val EXTRA_RESULT_CODE = "RESULT_CODE"
        const val EXTRA_DATA = "DATA"
        private const val CHANNEL_ID = "sender_channel"
        private const val NOTIF_ID = 1
        const val SAMPLE_RATE = 44100
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_STEREO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        const val PORT = 9999
    }

    private var isRunning = false
    private var serverSocket: ServerSocket? = null
    private var audioRecord: AudioRecord? = null
    private var mediaProjection: MediaProjection? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Build notification FIRST before anything else
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification())

        if (intent == null) {
            LogManager.logSender("ERROR: Intent is null")
            stopSelf()
            return START_NOT_STICKY
        }

        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Int.MIN_VALUE)
        val data = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra<Intent>(EXTRA_DATA)
        }

        LogManager.logSender("resultCode=$resultCode data=${data != null}")

        if (resultCode == Int.MIN_VALUE || data == null) {
            LogManager.logSender("ERROR: Missing projection data (code=$resultCode)")
            stopSelf()
            return START_NOT_STICKY
        }

        try {
            val projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = projectionManager.getMediaProjection(resultCode, data)
            LogManager.logSender("MediaProjection created successfully")
        } catch (e: Exception) {
            LogManager.logSender("ERROR creating MediaProjection: ${e.message}")
            stopSelf()
            return START_NOT_STICKY
        }

        isRunning = true
        startServer()
        return START_STICKY
    }

    private fun startServer() {
        Thread {
            try {
                serverSocket = ServerSocket(PORT)
                LogManager.logSender("Server listening on port $PORT")
                LogManager.logSender("Waiting for Oppo A52 to connect...")
                while (isRunning) {
                    val client = serverSocket!!.accept()
                    LogManager.logSender("Receiver connected: ${client.inetAddress.hostAddress}")
                    handleClient(client)
                }
            } catch (e: Exception) {
                if (isRunning) LogManager.logSender("Server error: ${e.message}")
            }
        }.start()
    }

    private fun handleClient(socket: Socket) {
        Thread {
            try {
                socket.tcpNoDelay = true
                socket.setSendBufferSize(16384)
                val out = DataOutputStream(socket.getOutputStream())

                val config = AudioPlaybackCaptureConfiguration.Builder(mediaProjection!!)
                    .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                    .addMatchingUsage(AudioAttributes.USAGE_GAME)
                    .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                    .build()

                val bufferSize = AudioRecord.getMinBufferSize(
                    SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT
                ) * 2

                audioRecord = AudioRecord.Builder()
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setSampleRate(SAMPLE_RATE)
                            .setEncoding(AUDIO_FORMAT)
                            .setChannelMask(CHANNEL_CONFIG)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .setAudioPlaybackCaptureConfig(config)
                    .build()

                val buffer = ByteArray(bufferSize)
                audioRecord?.startRecording()
                LogManager.logSender("Audio capture started! Streaming...")

                while (isRunning && socket.isConnected) {
                    val bytesRead = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (bytesRead > 0) {
                        out.writeInt(bytesRead)
                        out.write(buffer, 0, bytesRead)
                        out.flush()
                    }
                }
            } catch (e: Exception) {
                LogManager.logSender("Stream error: ${e.message}")
            } finally {
                audioRecord?.stop()
                audioRecord?.release()
                audioRecord = null
                socket.close()
                LogManager.logSender("Client disconnected")
            }
        }.start()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Audio Sender", NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AudioLink — Sending")
            .setContentText("Streaming phone audio...")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        audioRecord?.stop()
        audioRecord?.release()
        mediaProjection?.stop()
        serverSocket?.close()
        LogManager.logSender("Sender service stopped")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
