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

        rvDevices.layoutManager = LinearLayoutManager(requireContext())

        tvStatus.text = "Ready — tap Scan to find devices"

        btnScan.setOnClickListener {
            addLog("Scan tapped — Phase 6 will wire discovery here")
            tvStatus.text = "Scanning... (Phase 6)"
        }

        btnConnect.setOnClickListener {
            addLog("Connect tapped — Phase 5 will wire audio here")
        }

        addLog("Receiver tab loaded")
        addLog("Phase 3 complete — UI shell ready")
    }

    fun addLog(message: String) {
        activity?.runOnUiThread {
            val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                .format(java.util.Date())
            tvLog.append("[$timestamp] $message\n")
            scrollLog.post { scrollLog.fullScroll(View.FOCUS_DOWN) }
        }
    }
}
