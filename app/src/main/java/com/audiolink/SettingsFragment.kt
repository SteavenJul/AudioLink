package com.audiolink

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment

class SettingsFragment : Fragment() {

    private lateinit var etAudioPort: EditText
    private lateinit var etDiscoveryPort: EditText
    private lateinit var etScanTimeout: EditText
    private lateinit var etBufferMultiplier: EditText
    private lateinit var etRetryCount: EditText
    private lateinit var etRetryDelay: EditText
    private lateinit var btnSave: Button
    private lateinit var btnReset: Button
    private lateinit var tvSaved: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_settings, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etAudioPort        = view.findViewById(R.id.etAudioPort)
        etDiscoveryPort    = view.findViewById(R.id.etDiscoveryPort)
        etScanTimeout      = view.findViewById(R.id.etScanTimeout)
        etBufferMultiplier = view.findViewById(R.id.etBufferMultiplier)
        etRetryCount       = view.findViewById(R.id.etRetryCount)
        etRetryDelay       = view.findViewById(R.id.etRetryDelay)
        btnSave            = view.findViewById(R.id.btnSaveSettings)
        btnReset           = view.findViewById(R.id.btnResetSettings)
        tvSaved            = view.findViewById(R.id.tvSettingsSaved)

        loadCurrentValues()

        btnSave.setOnClickListener { saveSettings() }
        btnReset.setOnClickListener { resetSettings() }
    }

    private fun loadCurrentValues() {
        val ctx = requireContext()
        etAudioPort.setText(SettingsManager.getAudioPort(ctx).toString())
        etDiscoveryPort.setText(SettingsManager.getDiscoveryPort(ctx).toString())
        etScanTimeout.setText((SettingsManager.getScanTimeoutMs(ctx) / 1000).toString())
        etBufferMultiplier.setText(SettingsManager.getBufferMultiplier(ctx).toString())
        etRetryCount.setText(SettingsManager.getRetryCount(ctx).toString())
        etRetryDelay.setText((SettingsManager.getRetryDelayMs(ctx) / 1000).toString())
    }

    private fun saveSettings() {
        val ctx = requireContext()
        val audioPort        = etAudioPort.text.toString().toIntOrNull()
        val discoveryPort    = etDiscoveryPort.text.toString().toIntOrNull()
        val scanTimeout      = etScanTimeout.text.toString().toIntOrNull()
        val bufferMultiplier = etBufferMultiplier.text.toString().toIntOrNull()
        val retryCount       = etRetryCount.text.toString().toIntOrNull()
        val retryDelay       = etRetryDelay.text.toString().toIntOrNull()

        if (audioPort == null || audioPort !in 1024..65535) {
            etAudioPort.error = "Enter a valid port (1024-65535)"
            return
        }
        if (discoveryPort == null || discoveryPort !in 1024..65535) {
            etDiscoveryPort.error = "Enter a valid port (1024-65535)"
            return
        }
        if (audioPort == discoveryPort) {
            etDiscoveryPort.error = "Audio and Discovery ports must differ"
            return
        }
        if (scanTimeout == null || scanTimeout !in 2..60) {
            etScanTimeout.error = "Enter 2-60 seconds"
            return
        }
        if (bufferMultiplier == null || bufferMultiplier !in 1..8) {
            etBufferMultiplier.error = "Enter 1-8"
            return
        }
        if (retryCount == null || retryCount !in 1..50) {
            etRetryCount.error = "Enter 1-50"
            return
        }
        if (retryDelay == null || retryDelay !in 1..30) {
            etRetryDelay.error = "Enter 1-30 seconds"
            return
        }

        SettingsManager.setAudioPort(ctx, audioPort)
        SettingsManager.setDiscoveryPort(ctx, discoveryPort)
        SettingsManager.setScanTimeout(ctx, scanTimeout)
        SettingsManager.setBufferMultiplier(ctx, bufferMultiplier)
        SettingsManager.setRetryCount(ctx, retryCount)
        SettingsManager.setRetryDelay(ctx, retryDelay)

        showSavedConfirmation()
    }

    private fun resetSettings() {
        SettingsManager.resetAll(requireContext())
        loadCurrentValues()
        tvSaved.text = "✓ Reset to defaults"
        tvSaved.visibility = View.VISIBLE
        tvSaved.postDelayed({ tvSaved.visibility = View.GONE }, 2500)
    }

    private fun showSavedConfirmation() {
        tvSaved.text = "✓ Settings saved"
        tvSaved.visibility = View.VISIBLE
        tvSaved.postDelayed({ tvSaved.visibility = View.GONE }, 2500)
    }
}
