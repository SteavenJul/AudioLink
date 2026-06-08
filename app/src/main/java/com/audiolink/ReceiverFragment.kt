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
    private var isScanning = false
    private var discoveryManager: DiscoveryManager? = null

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
            if (!isScanning) startScan() else stopScan()
        }

        btnConnect.setOnClickListener {
            if (!isConnected) {
                selectedDevice?.let { connectTo(it) }
            } else {
                disconnect()
            }
        }

        LogManager.logReceiver("Receiver tab ready")
    }

    private fun startScan() {
        isScanning = true
        deviceAdapter.clear()
        btnConnect.isEnabled = false
        selectedDevice = null
        btnScan.text = "Stop"
        tvStatus.text = "Scanning (8s)..."
        LogManager.logReceiver("Scan started...")

        discoveryManager?.stop()
        discoveryManager = DiscoveryManager(
            context = requireContext(),
            onDeviceFound = { device ->
                activity?.runOnUiThread {
                    deviceAdapter.addDevice(device)
                    tvStatus.text = "Found ${deviceAdapter.itemCount} device(s) — tap to select"
                }
                LogManager.logReceiver("Found: ${device.name} at ${device.ip}")
            },
            onLog = { LogManager.logReceiver(it) }
        )

        discoveryManager?.startListening(onScanComplete = {
            activity?.runOnUiThread {
                isScanning = false
                btnScan.text = "Scan"
                if (deviceAdapter.itemCount == 0) {
                    tvStatus.text = "No devices found — tap Scan to retry"
                    LogManager.logReceiver("No devices found")
                }
            }
        })
    }

    private fun stopScan() {
        isScanning = false
        discoveryManager?.stopListening()
        discoveryManager = null
        btnScan.text = "Scan"
        tvStatus.text = "Scan stopped"
        LogManager.logReceiver("Scan stopped by user")
    }

    private fun connectTo(device: DiscoveryManager.Device) {
        stopScan()
        LogManager.logReceiver("Connecting to ${device.ip}:${device.port}...")
        tvStatus.text = "Connecting to ${device.name}..."
        btnConnect.text = "Disconnect"
        btnConnect.backgroundTintList =
            android.content.res.ColorStateList.valueOf(
                android.graphics.Color.parseColor("#C62828")
            )
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
            android.content.res.ColorStateList.valueOf(
                android.graphics.Color.parseColor("#1565C0")
            )
        tvStatus.text = "Disconnected"
        selectedDevice = null
        LogManager.logReceiver("Disconnected")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        discoveryManager?.stop()
    }
}
