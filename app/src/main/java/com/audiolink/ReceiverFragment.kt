package com.audiolink

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

        // Setup device list
        deviceAdapter = DeviceAdapter { device ->
            selectedDevice = device
            btnConnect.isEnabled = true
            tvStatus.text = "Selected: ${device.name} (${device.ip})"
            LogManager.logReceiver("Device selected: ${device.name} at ${device.ip}")
        }
        rvDevices.layoutManager = LinearLayoutManager(requireContext())
        rvDevices.adapter = deviceAdapter

        // Wire log manager to UI
        LogManager.setReceiverListener { line ->
            activity?.runOnUiThread {
                tvLog.append("$line\n")
                scrollLog.post { scrollLog.fullScroll(View.FOCUS_DOWN) }
            }
        }

        tvStatus.text = "Ready — tap Scan to find devices"

        btnScan.setOnClickListener {
            deviceAdapter.clear()
            btnConnect.isEnabled = false
            selectedDevice = null
            tvStatus.text = "Scanning..."
            LogManager.logReceiver("Scan started — discovery coming in Phase 6")

            // Simulate finding a device for testing UI
            val testDevice = DiscoveryManager.Device(
                name = "Test Device",
                ip = "192.168.43.1",
                port = 9999
            )
            deviceAdapter.addDevice(testDevice)
            LogManager.logReceiver("(Test) Found device: ${testDevice.name}")
            tvStatus.text = "Found 1 device — tap to select"
        }

        btnConnect.setOnClickListener {
            selectedDevice?.let { device ->
                LogManager.logReceiver("Connect to ${device.ip}:${device.port} — Phase 5 will wire audio")
                tvStatus.text = "Connecting... (Phase 5)"
            }
        }

        LogManager.logReceiver("Receiver tab ready")
        LogManager.logReceiver("Waiting for Phase 5 audio + Phase 6 discovery...")
    }

    fun updateStatus(status: String) {
        activity?.runOnUiThread { tvStatus.text = status }
    }
}
