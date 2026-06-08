package com.audiolink

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.MulticastSocket

class DiscoveryManager(
    private val context: Context,
    private val onDeviceFound: (Device) -> Unit,
    private val onLog: (String) -> Unit
) {
    companion object {
        const val DISCOVERY_PORT = 9998
        const val AUDIO_PORT = 9999
        const val MULTICAST_GROUP = "224.0.0.251"
        const val BROADCAST_INTERVAL = 2000L
        const val SENDER_TAG = "AUDIOLINK_SENDER"
    }

    data class Device(
        val name: String,
        val ip: String,
        val port: Int
    )

    private var isBroadcasting = false
    private var isListening = false
    private var broadcastSocket: DatagramSocket? = null
    private var listenSocket: MulticastSocket? = null
    private var multicastLock: WifiManager.MulticastLock? = null

    // Call this on the SENDER (Poco X7 Pro)
    // Announces itself every 2 seconds so receivers can find it
    fun startBroadcasting(deviceName: String) {
        isBroadcasting = true
        onLog("Discovery: Starting broadcast as '$deviceName'")

        Thread {
            try {
                broadcastSocket = DatagramSocket()
                broadcastSocket?.broadcast = true
                val message = "$SENDER_TAG|$deviceName|$AUDIO_PORT"
                val data = message.toByteArray()

                while (isBroadcasting) {
                    try {
                        // Broadcast to 255.255.255.255 (all devices on network)
                        val packet = DatagramPacket(
                            data, data.size,
                            InetAddress.getByName("255.255.255.255"),
                            DISCOVERY_PORT
                        )
                        broadcastSocket?.send(packet)
                        onLog("Discovery: Broadcasting presence...")
                    } catch (e: Exception) {
                        onLog("Discovery: Broadcast error - ${e.message}")
                    }
                    Thread.sleep(BROADCAST_INTERVAL)
                }
            } catch (e: Exception) {
                onLog("Discovery: Fatal broadcast error - ${e.message}")
            }
        }.start()
    }

    // Call this on the RECEIVER (Oppo A52)
    // Listens for sender broadcasts and reports found devices
    fun startListening() {
        isListening = true
        acquireMulticastLock()
        onLog("Discovery: Listening for senders on network...")

        Thread {
            try {
                listenSocket = MulticastSocket(DISCOVERY_PORT)
                listenSocket?.soTimeout = 5000
                val buffer = ByteArray(1024)

                while (isListening) {
                    try {
                        val packet = DatagramPacket(buffer, buffer.size)
                        listenSocket?.receive(packet)

                        val message = String(packet.data, 0, packet.length)
                        val senderIp = packet.address.hostAddress ?: continue

                        if (message.startsWith(SENDER_TAG)) {
                            val parts = message.split("|")
                            if (parts.size >= 3) {
                                val device = Device(
                                    name = parts[1],
                                    ip = senderIp,
                                    port = parts[2].toIntOrNull() ?: AUDIO_PORT
                                )
                                onLog("Discovery: Found sender '${device.name}' at ${device.ip}")
                                onDeviceFound(device)
                            }
                        }
                    } catch (e: Exception) {
                        // Timeout is normal — just keep listening
                        if (isListening) onLog("Discovery: Still scanning...")
                    }
                }
            } catch (e: Exception) {
                onLog("Discovery: Listen error - ${e.message}")
            } finally {
                releaseMulticastLock()
            }
        }.start()
    }

    private fun acquireMulticastLock() {
        val wifiManager = context.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as WifiManager
        multicastLock = wifiManager.createMulticastLock("AudioLinkDiscovery")
        multicastLock?.setReferenceCounted(true)
        multicastLock?.acquire()
        onLog("Discovery: Multicast lock acquired")
    }

    private fun releaseMulticastLock() {
        if (multicastLock?.isHeld == true) {
            multicastLock?.release()
            onLog("Discovery: Multicast lock released")
        }
    }

    fun stopBroadcasting() {
        isBroadcasting = false
        broadcastSocket?.close()
        onLog("Discovery: Broadcast stopped")
    }

    fun stopListening() {
        isListening = false
        listenSocket?.close()
        releaseMulticastLock()
        onLog("Discovery: Listening stopped")
    }

    fun stop() {
        stopBroadcasting()
        stopListening()
    }
}
