package com.audiolink

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment

class SenderFragment : Fragment() {

    private lateinit var tvStatus: TextView
    private lateinit var tvIp: TextView
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
        tvLog = view.findViewById(R.id.tvSenderLog)
        scrollLog = view.findViewById(R.id.scrollSenderLog)
        btnToggle = view.findViewById(R.id.btnSenderToggle)

        tvIp.text = "IP: 192.168.43.1 (hotspot)\nPort: 9999"
        tvStatus.text = "Ready — tap Start Server"

        btnToggle.setOnClickListener {
            addLog("Start button tapped — Phase 5 will wire this up")
            tvStatus.text = "Coming in Phase 5..."
        }

        addLog("Sender tab loaded")
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
