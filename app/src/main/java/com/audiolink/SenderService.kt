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
import androidx.annotation.RequiresApi
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

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotification())

        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, -1) ?: -1
        val data = intent?.getParcelableExtra<Intent>(EXTRA_DATA)

        if (resultCode == -1 || data == null) {
            LogManager.logSender("ERROR: No MediaProjection data received")
            stopSelf()
            return START_NOT_STICKY
        }

        val projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = projectionManager.getMediaProjection(resultCode, data)

        isRunning = true
        startServer()
        return START_STICKY
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun startServer() {
        Thread {
            try {
                serverSocket = ServerSocket(PORT)
                LogManager.logSender("Server started on port $PORT")
                LogManager.logSender("Waiting for receiver to connect...")

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

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun handleClient(socket: Socket) {
        Thread {
            try {
                socket.tcpNoDelay = true
                socket.setSendBufferSize(16384)

                val out = DataOutputStream(socket.getOutputStream())

                // Capture ALL phone audio using MediaProjection
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
                LogManager.logSender("Capturing phone audio — streaming started!")

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
                LogManager.logSender("Receiver disconnected")
            }
        }.start()
    }

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Audio Sender", NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
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
