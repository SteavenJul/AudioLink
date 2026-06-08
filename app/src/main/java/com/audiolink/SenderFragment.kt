package com.audiolink

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
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
    private var isRunning = false

    private val projectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            LogManager.logSender("MediaProjection permission granted!")
            LogManager.logSender("Starting audio capture...")
            val intent = Intent(requireContext(), SenderService::class.java).apply {
                putExtra(SenderService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(SenderService.EXTRA_DATA, result.data)
            }
            requireContext().startForegroundService(intent)
            isRunning = true
            btnToggle.text = "Stop Server"
            tvStatus.text = "Streaming phone audio..."
        } else {
            LogManager.logSender("MediaProjection permission denied!")
            tvStatus.text = "Permission denied — tap Start to try again"
        }
    }

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

        LogManager.setSenderListener { line ->
            activity?.runOnUiThread {
                tvLog.append("$line\n")
                scrollLog.post { scrollLog.fullScroll(View.FOCUS_DOWN) }
            }
        }

        detectAndShowIp()
        checkPermissions()

        btnToggle.setOnClickListener {
            if (!isRunning) startServer() else stopServer()
        }

        LogManager.logSender("Sender tab ready")
    }

    private fun startServer() {
        LogManager.logSender("Requesting screen capture permission...")
        tvStatus.text = "Requesting permission..."
        val manager = requireContext().getSystemService(MediaProjectionManager::class.java)
        projectionLauncher.launch(manager.createScreenCaptureIntent())
    }

    private fun stopServer() {
        requireContext().stopService(Intent(requireContext(), SenderService::class.java))
        isRunning = false
        btnToggle.text = "Start Server"
        tvStatus.text = "Stopped"
        LogManager.logSender("Server stopped")
    }

    private fun detectAndShowIp() {
        val ip = getHotspotIp()
        tvIp.text = "IP: $ip"
        tvPort.text = "Port: ${DiscoveryManager.AUDIO_PORT}"
        LogManager.logSender("Detected IP: $ip")
    }

    private fun getHotspotIp(): String {
        try {
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
        if (!hasPermission(Manifest.permission.RECORD_AUDIO))
            missing.add(Manifest.permission.RECORD_AUDIO)
        if (missing.isNotEmpty()) {
            LogManager.logSender("Requesting permissions: $missing")
            ActivityCompat.requestPermissions(requireActivity(), missing.toTypedArray(), 200)
        } else {
            LogManager.logSender("All permissions granted")
        }
    }

    private fun hasPermission(p: String) =
        ContextCompat.checkSelfPermission(requireContext(), p) == PackageManager.PERMISSION_GRANTED
}
