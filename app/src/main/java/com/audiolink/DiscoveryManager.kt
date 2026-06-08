package com.audiolink

import android.content.Context
import android.net.wifi.WifiManager
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class DiscoveryManager(
    private val context: Context,
    private val onDeviceFound: (Device) -> Unit,
    private val onLog: (String) -> Unit
) {
    companion object {
        const val DISCOVERY_PORT = 9998
        const val AUDIO_PORT = 9999
        const val BROADCAST_INTERVAL = 2000L
        const val SENDER_TAG = "AUDIOLINK_SENDER"
        const val SCAN_TIMEOUT = 8000L
    }

    data class Device(val name: String, val ip: String, val port: Int)

    private var isBroadcasting = false
    private var isListening = false
    private var broadcastSocket: DatagramSocket? = null
    private var listenSocket: DatagramSocket? = null
    private var multicastLock: WifiManager.MulticastLock? = null

    fun startBroadcasting(deviceName: String) {
        isBroadcasting = true
        onLog("Broadcasting as '$deviceName'...")
        Thread {
            try {
                broadcastSocket = DatagramSocket()
                broadcastSocket?.broadcast = true
                val message = "$SENDER_TAG|$deviceName|$AUDIO_PORT"
                val data = message.toByteArray()
                while (isBroadcasting) {
                    try {
                        val packet = DatagramPacket(
                            data, data.size,
                            InetAddress.getByName("255.255.255.255"),
                            DISCOVERY_PORT
                        )
                        broadcastSocket?.send(packet)
                        onLog("Broadcast sent")
                    } catch (e: Exception) {
                        onLog("Broadcast error: ${e.message}")
                    }
                    Thread.sleep(BROADCAST_INTERVAL)
                }
            } catch (e: Exception) {
                onLog("Broadcast fatal: ${e.message}")
            }
        }.start()
    }

    fun startListening(onScanComplete: (() -> Unit)? = null) {
        isListening = true
        acquireMulticastLock()
        onLog("Scanning for ${SCAN_TIMEOUT / 1000}s...")
        Thread {
            val startTime = System.currentTimeMillis()
            try {
                listenSocket = DatagramSocket(DISCOVERY_PORT)
                listenSocket?.soTimeout = 1000
                val buffer = ByteArray(1024)
                while (isListening && (System.currentTimeMillis() - startTime) < SCAN_TIMEOUT) {
                    try {
                        val packet = DatagramPacket(buffer, buffer.size)
                        listenSocket?.receive(packet)
                        val message = String(packet.data, 0, packet.length)
                        val senderIp = packet.address.hostAddress ?: continue
                        if (message.startsWith(SENDER_TAG)) {
                            val parts = message.split("|")
                            if (parts.size >= 3) {
                                onDeviceFound(
                                    Device(
                                        name = parts[1],
                                        ip = senderIp,
                                        port = parts[2].toIntOrNull() ?: AUDIO_PORT
                                    )
                                )
                            }
                        }
                    } catch (e: Exception) {
                        // Read timeout — normal, keep looping
                    }
                }
            } catch (e: Exception) {
                onLog("Scan error: ${e.message}")
            } finally {
                isListening = false
                releaseMulticastLock()
                listenSocket?.close()
                onLog("Scan complete")
                onScanComplete?.invoke()
            }
        }.start()
    }

    private fun acquireMulticastLock() {
        val wifiManager = context.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as WifiManager
        multicastLock = wifiManager.createMulticastLock("AudioLinkDiscovery")
        multicastLock?.setReferenceCounted(true)
        multicastLock?.acquire()
    }

    private fun releaseMulticastLock() {
        if (multicastLock?.isHeld == true) multicastLock?.release()
    }

    fun stopBroadcasting() {
        isBroadcasting = false
        broadcastSocket?.close()
        onLog("Broadcast stopped")
    }

    fun stopListening() {
        isListening = false
        listenSocket?.close()
        releaseMulticastLock()
        onLog("Scan stopped manually")
    }

    fun stop() {
        stopBroadcasting()
        stopListening()
    }
}
