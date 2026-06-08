package com.audiolink

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
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
    private var discoveryManager: DiscoveryManager? = null

    private val projectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            LogManager.logSender("MediaProjection granted! Starting service...")
            val serviceIntent = Intent(requireContext(), SenderService::class.java)
            serviceIntent.putExtra(SenderService.EXTRA_RESULT_CODE, result.resultCode)
            serviceIntent.putExtra(SenderService.EXTRA_DATA, result.data!!)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                requireContext().startForegroundService(serviceIntent)
            } else {
                requireContext().startService(serviceIntent)
            }
            val deviceName = android.os.Build.MODEL
            discoveryManager = DiscoveryManager(
                context = requireContext(),
                onDeviceFound = {},
                onLog = { LogManager.logSender(it) }
            )
            discoveryManager?.startBroadcasting(deviceName)
            LogManager.logSender("Broadcasting as '$deviceName'")
            isRunning = true
            btnToggle.text = "Stop Server"
            tvStatus.text = "Streaming — visible to receivers"
        } else {
            LogManager.logSender("MediaProjection denied or cancelled")
            tvStatus.text = "Permission denied — try again"
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

    override fun onResume() {
        super.onResume()
        tvPort.text = "Port: ${SettingsManager.getAudioPort(requireContext())}"
    }

    private fun startServer() {
        LogManager.logSender("Requesting screen capture permission...")
        tvStatus.text = "Requesting permission..."
        val manager = requireContext().getSystemService(MediaProjectionManager::class.java)
        projectionLauncher.launch(manager.createScreenCaptureIntent())
    }

    private fun stopServer() {
        discoveryManager?.stopBroadcasting()
        discoveryManager = null
        requireContext().stopService(Intent(requireContext(), SenderService::class.java))
        isRunning = false
        btnToggle.text = "Start Server"
        tvStatus.text = "Stopped"
        LogManager.logSender("Server stopped")
    }

    private fun detectAndShowIp() {
        val ip = getBestLocalIp()
        tvIp.text = "IP: $ip"
        tvPort.text = "Port: ${SettingsManager.getAudioPort(requireContext())}"
        LogManager.logSender("Detected IP: $ip")
    }

    private fun getBestLocalIp(): String {
        val candidates = mutableListOf<String>()
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return "unavailable"
            for (iface in interfaces.asSequence()) {
                if (!iface.isUp || iface.isLoopback) continue
                for (addr in iface.inetAddresses.asSequence()) {
                    if (addr.isLoopbackAddress) continue
                    val ip = addr.hostAddress ?: continue
                    if (ip.contains(':')) continue
                    candidates.add(ip)
                    LogManager.logSender("Interface ${iface.name}: $ip")
                }
            }
        } catch (e: Exception) {
            LogManager.logSender("IP detection error: ${e.message}")
        }
        candidates.firstOrNull { it.startsWith("192.168.43.") }?.let { return it }
        candidates.firstOrNull { it.startsWith("192.168.") }?.let { return it }
        candidates.firstOrNull { it.startsWith("10.") }?.let { return it }
        return candidates.firstOrNull() ?: "unavailable"
    }

    private fun checkPermissions() {
        val permissionsNeeded = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.RECORD_AUDIO)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(), Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsNeeded.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (permissionsNeeded.isNotEmpty()) {
            LogManager.logSender("Requesting permissions: $permissionsNeeded")
            ActivityCompat.requestPermissions(
                requireActivity(),
                permissionsNeeded.toTypedArray(),
                200
            )
        } else {
            LogManager.logSender("All permissions granted")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        discoveryManager?.stopBroadcasting()
    }
}
