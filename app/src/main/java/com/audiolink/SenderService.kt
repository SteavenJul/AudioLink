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
import android.os.Process
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
        val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_STEREO
        val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    private var isRunning = false
    private var serverSocket: ServerSocket? = null
    private var audioRecord: AudioRecord? = null
    private var mediaProjection: MediaProjection? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification())

        if (intent == null) { stopSelf(); return START_NOT_STICKY }

        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Int.MIN_VALUE)
        val data = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra<Intent>(EXTRA_DATA)
        }

        if (resultCode == Int.MIN_VALUE || data == null) {
            LogManager.logSender("ERROR: Missing projection data")
            stopSelf()
            return START_NOT_STICKY
        }

        try {
            val mgr = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = mgr.getMediaProjection(resultCode, data)
            LogManager.logSender("MediaProjection ready")
        } catch (e: Exception) {
            LogManager.logSender("ERROR: ${e.message}")
            stopSelf()
            return START_NOT_STICKY
        }

        isRunning = true
        startServer()
        return START_STICKY
    }

    private fun startServer() {
        val port = SettingsManager.getAudioPort(this)
        val sampleRate = SettingsManager.getSampleRate(this)

        // Absolute minimum buffer — just enough for AudioRecord to work
        val minBuffer = AudioRecord.getMinBufferSize(
            sampleRate, CHANNEL_CONFIG, AUDIO_FORMAT
        )
        // Read in tiny chunks — 10ms worth of audio at a time
        val chunkSize = (sampleRate * 2 * 2 * 10) / 1000
        LogManager.logSender("Sample rate: ${sampleRate}Hz")
        LogManager.logSender("Min buffer: ${minBuffer}B")
        LogManager.logSender("Chunk size: ${chunkSize}B (10ms)")

        Thread {
            try {
                serverSocket = ServerSocket(port)
                LogManager.logSender("Listening on port $port...")
                while (isRunning) {
                    val client = serverSocket!!.accept()
                    LogManager.logSender("Receiver connected: ${client.inetAddress.hostAddress}")
                    handleClient(client, sampleRate, minBuffer, chunkSize)
                }
            } catch (e: Exception) {
                if (isRunning) LogManager.logSender("Server error: ${e.message}")
            }
        }.start()
    }

    private fun handleClient(socket: Socket, sampleRate: Int, minBuffer: Int, chunkSize: Int) {
        Thread {
            // Boost thread priority for audio
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
            try {
                socket.tcpNoDelay = true
                socket.setSendBufferSize(chunkSize * 2)
                val out = DataOutputStream(socket.getOutputStream())

                // Send config to receiver
                out.writeInt(ReceiverService.CONFIG_HEADER)
                out.writeInt(sampleRate)
                out.writeInt(chunkSize)
                out.flush()
                LogManager.logSender("Config sent: ${sampleRate}Hz, ${chunkSize}B chunks")

                val config = AudioPlaybackCaptureConfiguration.Builder(mediaProjection!!)
                    .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                    .addMatchingUsage(AudioAttributes.USAGE_GAME)
                    .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                    .build()

                audioRecord = AudioRecord.Builder()
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setSampleRate(sampleRate)
                            .setEncoding(AUDIO_FORMAT)
                            .setChannelMask(CHANNEL_CONFIG)
                            .build()
                    )
                    .setBufferSizeInBytes(minBuffer)
                    .setAudioPlaybackCaptureConfig(config)
                    .build()

                val buffer = ByteArray(chunkSize)
                audioRecord?.startRecording()
                LogManager.logSender("Streaming! Chunk=${chunkSize}B, Rate=${sampleRate}Hz")

                while (isRunning && socket.isConnected) {
                    val bytesRead = audioRecord?.read(buffer, 0, chunkSize) ?: 0
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
            val channel = NotificationChannel(CHANNEL_ID, "Audio Sender", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AudioLink — Sending")
            .setContentText("Streaming phone audio...")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true).build()
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        audioRecord?.stop()
        audioRecord?.release()
        mediaProjection?.stop()
        serverSocket?.close()
        LogManager.logSender("Sender stopped")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
