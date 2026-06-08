package com.audiolink

import android.Manifest
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import java.net.NetworkInterface

class SenderFragment : Fragment() {

    private lateinit var tvStatus: TextView
    private lateinit var tvIp: TextView
    private lateinit var tvPort: TextView
    private lateinit var tvLog: TextView
    private lateinit var scrollLog: ScrollView
    private lateinit var btnToggle: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_sender, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvStatus = view.findViewById(R.id.tvSenderStatus)
        tvIp = view.findViewById(R.id.tvIpAddress)
        tvPort = view.findViewById(R.id.tvPort)
        tvLog = view.findViewById(R.id.tvSenderLog)
        scrollLog = view.findViewById(R.id.scrollSenderLog)
        btnToggle = view.findViewById(R.id.btnSenderToggle)

        // Wire up log manager to this UI
        LogManager.setSenderListener { line ->
            activity?.runOnUiThread {
                tvLog.append("$line\n")
                scrollLog.post { scrollLog.fullScroll(View.FOCUS_DOWN) }
            }
        }

        detectAndShowIp()
        checkPermissions()

        btnToggle.setOnClickListener {
            LogManager.logSender("Start button tapped — audio coming in Phase 5")
            tvStatus.text = "Phase 5 will activate streaming"
        }

        LogManager.logSender("Sender tab ready")
        LogManager.logSender("Permissions: ${getPermissionStatus()}")
        LogManager.logSender("Waiting for Phase 5 to wire audio...")
    }

    private fun detectAndShowIp() {
        val ip = getHotspotIp()
        tvIp.text = "IP: $ip"
        tvPort.text = "Port: ${DiscoveryManager.AUDIO_PORT}"
        LogManager.logSender("Detected IP: $ip")
    }

    private fun getHotspotIp(): String {
        try {
            // Hotspot IP is always 192.168.43.1 on Android
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    val ip = addr.hostAddress ?: continue
                    if (ip.startsWith("192.168.43")) return ip
                }
            }
        } catch (e: Exception) {
            LogManager.logSender("IP detection error: ${e.message}")
        }
        return "192.168.43.1"
    }

    private fun checkPermissions() {
        val missing = mutableListOf<String>()
        if (!hasPermission(Manifest.permission.RECORD_AUDIO)) missing.add("RECORD_AUDIO")
        if (missing.isNotEmpty()) {
            LogManager.logSender("Missing permissions: $missing")
            ActivityCompat.requestPermissions(requireActivity(), missing.toTypedArray(), 200)
        } else {
            LogManager.logSender("All permissions granted")
        }
    }

    private fun hasPermission(p: String) =
        ContextCompat.checkSelfPermission(requireContext(), p) == PackageManager.PERMISSION_GRANTED

    private fun getPermissionStatus(): String {
        val audio = hasPermission(Manifest.permission.RECORD_AUDIO)
        return "RECORD_AUDIO=$audio"
    }

    fun updateStatus(status: String) {
        activity?.runOnUiThread { tvStatus.text = status }
    }
}
