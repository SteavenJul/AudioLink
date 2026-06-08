package com.audiolink

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList

object LogManager {
    private const val MAX_LINES = 200
    private val senderLogs = CopyOnWriteArrayList<String>()
    private val receiverLogs = CopyOnWriteArrayList<String>()
    private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    private var senderListener: ((String) -> Unit)? = null
    private var receiverListener: ((String) -> Unit)? = null

    fun setSenderListener(listener: (String) -> Unit) {
        senderListener = listener
        // Replay existing logs to new listener
        senderLogs.forEach { listener(it) }
    }

    fun setReceiverListener(listener: (String) -> Unit) {
        receiverListener = listener
        receiverLogs.forEach { listener(it) }
    }

    fun logSender(message: String) {
        val line = "[${dateFormat.format(Date())}] $message"
        Log.d("AudioLink:Sender", message)
        if (senderLogs.size >= MAX_LINES) senderLogs.removeAt(0)
        senderLogs.add(line)
        senderListener?.invoke(line)
    }

    fun logReceiver(message: String) {
        val line = "[${dateFormat.format(Date())}] $message"
        Log.d("AudioLink:Receiver", message)
        if (receiverLogs.size >= MAX_LINES) receiverLogs.removeAt(0)
        receiverLogs.add(line)
        receiverListener?.invoke(line)
    }

    fun clearSender() { senderLogs.clear() }
    fun clearReceiver() { receiverLogs.clear() }
}
