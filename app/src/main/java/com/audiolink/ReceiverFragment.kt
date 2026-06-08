package com.audiolink

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ReceiverFragment : Fragment() {

    private lateinit var tvStatus: TextView
    private lateinit var tvLog: TextView
    private lateinit var scrollLog: ScrollView
    private lateinit var btnScan: Button
    private lateinit var btnConnect: Button
    private lateinit var rvDevices: RecyclerView
    private lateinit var deviceAdapter: DeviceAdapter
    private var selectedDevice: DiscoveryManager.Device? = null
    private var isConnected = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_receiver, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvStatus = view.findViewById(R.id.tvReceiverStatus)
        tvLog = view.findViewById(R.id.tvReceiverLog)
        scrollLog = view.findViewById(R.id.scrollReceiverLog)
        btnScan = view.findViewById(R.id.btnScan)
        btnConnect = view.findViewById(R.id.btnReceiverToggle)
        rvDevices = view.findViewById(R.id.rvDevices)

        deviceAdapter = DeviceAdapter { device ->
            selectedDevice = device
            btnConnect.isEnabled = true
            tvStatus.text = "Selected: ${device.name} (${device.ip})"
            LogManager.logReceiver("Selected: ${device.name} at ${device.ip}")
        }
        rvDevices.layoutManager = LinearLayoutManager(requireContext())
        rvDevices.adapter = deviceAdapter

        LogManager.setReceiverListener { line ->
            activity?.runOnUiThread {
                tvLog.append("$line\n")
                scrollLog.post { scrollLog.fullScroll(View.FOCUS_DOWN) }
            }
        }

        tvStatus.text = "Ready — tap Scan to find devices"

        btnScan.setOnClickListener {
            if (!isConnected) {
                deviceAdapter.clear()
                btnConnect.isEnabled = false
                selectedDevice = null
                tvStatus.text = "Scanning... (Phase 6 will add real discovery)"
                LogManager.logReceiver("Scanning network...")

                // Temp: add manual entry for testing
                val testDevice = DiscoveryManager.Device(
                    name = "Poco X7 Pro",
                    ip = "192.168.43.1",
                    port = 9999
                )
                deviceAdapter.addDevice(testDevice)
                LogManager.logReceiver("Found: ${testDevice.name} at ${testDevice.ip}")
                tvStatus.text = "Found device — tap to select then Connect"
            }
        }

        btnConnect.setOnClickListener {
            if (!isConnected) {
                selectedDevice?.let { device -> connectTo(device) }
            } else {
                disconnect()
            }
        }

        LogManager.logReceiver("Receiver tab ready")
    }

    private fun connectTo(device: DiscoveryManager.Device) {
        LogManager.logReceiver("Connecting to ${device.ip}:${device.port}...")
        tvStatus.text = "Connecting..."
        btnConnect.text = "Disconnect"
        btnConnect.backgroundTintList =
            android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#C62828"))
        isConnected = true

        val intent = Intent(requireContext(), ReceiverService::class.java).apply {
            putExtra(ReceiverService.EXTRA_HOST, device.ip)
            putExtra(ReceiverService.EXTRA_PORT, device.port)
        }
        requireContext().startForegroundService(intent)
        tvStatus.text = "Connected to ${device.name}"
    }

    private fun disconnect() {
        requireContext().stopService(Intent(requireContext(), ReceiverService::class.java))
        isConnected = false
        btnConnect.text = "Connect"
        btnConnect.isEnabled = false
        btnConnect.backgroundTintList =
            android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#1565C0"))
        tvStatus.text = "Disconnected"
        selectedDevice = null
        LogManager.logReceiver("Disconnected")
    }
}
