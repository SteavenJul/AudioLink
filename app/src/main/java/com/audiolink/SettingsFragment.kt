package com.audiolink

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class SettingsFragment : Fragment() {

    private lateinit var btnPresetGame: Button
    private lateinit var btnPresetMovie: Button
    private lateinit var btnPresetMusic: Button
    private lateinit var tvPresetDesc: TextView
    private lateinit var radioSampleRate: RadioGroup
    private lateinit var radioBufferMs: RadioGroup
    private lateinit var etAudioPort: EditText
    private lateinit var etDiscoveryPort: EditText
    private lateinit var etScanTimeout: EditText
    private lateinit var tvCurrentConfig: TextView
    private lateinit var btnSave: Button
    private lateinit var btnReset: Button
    private lateinit var tvSaved: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_settings, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnPresetGame  = view.findViewById(R.id.btnPresetGame)
        btnPresetMovie = view.findViewById(R.id.btnPresetMovie)
        btnPresetMusic = view.findViewById(R.id.btnPresetMusic)
        tvPresetDesc   = view.findViewById(R.id.tvPresetDesc)
        radioSampleRate = view.findViewById(R.id.radioSampleRate)
        radioBufferMs   = view.findViewById(R.id.radioBufferMs)
        etAudioPort     = view.findViewById(R.id.etAudioPort)
        etDiscoveryPort = view.findViewById(R.id.etDiscoveryPort)
        etScanTimeout   = view.findViewById(R.id.etScanTimeout)
        tvCurrentConfig = view.findViewById(R.id.tvCurrentConfig)
        btnSave  = view.findViewById(R.id.btnSaveSettings)
        btnReset = view.findViewById(R.id.btnResetSettings)
        tvSaved  = view.findViewById(R.id.tvSettingsSaved)

        loadCurrentValues()

        btnPresetGame.setOnClickListener  { applyPreset(SettingsManager.PRESET_GAME) }
        btnPresetMovie.setOnClickListener { applyPreset(SettingsManager.PRESET_MOVIE) }
        btnPresetMusic.setOnClickListener { applyPreset(SettingsManager.PRESET_MUSIC) }

        radioSampleRate.setOnCheckedChangeListener { _, _ -> updateSummary() }
        radioBufferMs.setOnCheckedChangeListener   { _, _ -> updateSummary() }

        btnSave.setOnClickListener  { saveSettings() }
        btnReset.setOnClickListener { resetSettings() }
    }

    private fun applyPreset(preset: SettingsManager.Preset) {
        selectSampleRate(preset.sampleRate)
        selectBufferMs(preset.bufferMs)
        updateSummary()

        val desc = when (preset.name) {
            "Game"  -> "🎮 Game: 22050Hz sender, 10ms buffer — lowest latency"
            "Movie" -> "🎬 Movie: 44100Hz sender, 80ms buffer — balanced"
            "Music" -> "🎵 Music: 48000Hz sender, 150ms buffer — best quality"
            else    -> ""
        }
        tvPresetDesc.text = desc
        tvPresetDesc.setTextColor(android.graphics.Color.parseColor("#4CAF50"))

        resetPresetButtons()
        when (preset.name) {
            "Game"  -> btnPresetGame.backgroundTintList  = tint("#1565C0")
            "Movie" -> btnPresetMovie.backgroundTintList = tint("#1565C0")
            "Music" -> btnPresetMusic.backgroundTintList = tint("#1565C0")
        }
    }

    private fun selectSampleRate(rate: Int) {
        val id = when (rate) {
            8000  -> R.id.rb8000
            11025 -> R.id.rb11025
            16000 -> R.id.rb16000
            22050 -> R.id.rb22050
            44100 -> R.id.rb44100
            else  -> R.id.rb48000
        }
        view?.findViewById<RadioButton>(id)?.isChecked = true
    }

    private fun selectBufferMs(ms: Int) {
        val id = when (ms) {
            0    -> R.id.rb0ms
            5    -> R.id.rb5ms
            10   -> R.id.rb10ms
            20   -> R.id.rb20ms
            40   -> R.id.rb40ms
            80   -> R.id.rb80ms
            150  -> R.id.rb150ms
            200  -> R.id.rb200ms
            else -> R.id.rb500ms
        }
        view?.findViewById<RadioButton>(id)?.isChecked = true
    }

    private fun getSelectedSampleRate(): Int {
        return when (radioSampleRate.checkedRadioButtonId) {
            R.id.rb8000  -> 8000
            R.id.rb11025 -> 11025
            R.id.rb16000 -> 16000
            R.id.rb22050 -> 22050
            R.id.rb44100 -> 44100
            else         -> 48000
        }
    }

    private fun getSelectedBufferMs(): Int {
        return when (radioBufferMs.checkedRadioButtonId) {
            R.id.rb0ms   -> 0
            R.id.rb5ms   -> 5
            R.id.rb10ms  -> 10
            R.id.rb20ms  -> 20
            R.id.rb40ms  -> 40
            R.id.rb80ms  -> 80
            R.id.rb150ms -> 150
            R.id.rb200ms -> 200
            else         -> 500
        }
    }

    private fun updateSummary() {
        val rate = getSelectedSampleRate()
        val ms   = getSelectedBufferMs()
        val bytes = if (ms == 0) 0 else (rate * 2 * 2 * ms) / 1000
        val kbps  = (rate * 2 * 2 * 8) / 1000
        tvCurrentConfig.text =
            "Sender  → Sample Rate : ${rate}Hz\n" +
            "Receiver→ Buffer      : ${ms}ms (${bytes}B)\n" +
            "Bitrate               : ~${kbps}kbps\n" +
            "Est. total latency    : ~${ms + 20}ms"
    }

    private fun loadCurrentValues() {
        val ctx = requireContext()
        selectSampleRate(SettingsManager.getSampleRate(ctx))
        selectBufferMs(SettingsManager.getBufferMs(ctx))
        etAudioPort.setText(SettingsManager.getAudioPort(ctx).toString())
        etDiscoveryPort.setText(SettingsManager.getDiscoveryPort(ctx).toString())
        etScanTimeout.setText((SettingsManager.getScanTimeoutMs(ctx) / 1000).toString())
        updateSummary()

        // Highlight matching preset
        val rate = SettingsManager.getSampleRate(ctx)
        val ms   = SettingsManager.getBufferMs(ctx)
        resetPresetButtons()
        when {
            rate == 22050 && ms == 10  -> btnPresetGame.backgroundTintList  = tint("#1565C0")
            rate == 44100 && ms == 80  -> btnPresetMovie.backgroundTintList = tint("#1565C0")
            rate == 48000 && ms == 150 -> btnPresetMusic.backgroundTintList = tint("#1565C0")
        }
    }

    private fun saveSettings() {
        val ctx           = requireContext()
        val audioPort     = etAudioPort.text.toString().toIntOrNull()
        val discoveryPort = etDiscoveryPort.text.toString().toIntOrNull()
        val scanTimeout   = etScanTimeout.text.toString().toIntOrNull()

        if (audioPort == null || audioPort !in 1024..65535) {
            etAudioPort.error = "Valid port: 1024-65535"; return
        }
        if (discoveryPort == null || discoveryPort !in 1024..65535) {
            etDiscoveryPort.error = "Valid port: 1024-65535"; return
        }
        if (audioPort == discoveryPort) {
            etDiscoveryPort.error = "Must differ from audio port"; return
        }
        if (scanTimeout == null || scanTimeout !in 2..60) {
            etScanTimeout.error = "Enter 2-60 seconds"; return
        }

        SettingsManager.setSampleRate(ctx, getSelectedSampleRate())
        SettingsManager.setBufferMs(ctx, getSelectedBufferMs())
        SettingsManager.setAudioPort(ctx, audioPort)
        SettingsManager.setDiscoveryPort(ctx, discoveryPort)
        SettingsManager.setScanTimeout(ctx, scanTimeout)

        tvSaved.text = "✓ Saved — restart stream to apply"
        tvSaved.visibility = View.VISIBLE
        tvSaved.postDelayed({ tvSaved.visibility = View.GONE }, 3000)
    }

    private fun resetSettings() {
        SettingsManager.resetAll(requireContext())
        loadCurrentValues()
        tvPresetDesc.text = "Tap a preset to auto-configure"
        tvPresetDesc.setTextColor(android.graphics.Color.parseColor("#888888"))
        resetPresetButtons()
        tvSaved.text = "✓ Reset to defaults"
        tvSaved.visibility = View.VISIBLE
        tvSaved.postDelayed({ tvSaved.visibility = View.GONE }, 2500)
    }

    private fun resetPresetButtons() {
        btnPresetGame.backgroundTintList  = tint("#1E1E1E")
        btnPresetMovie.backgroundTintList = tint("#1E1E1E")
        btnPresetMusic.backgroundTintList = tint("#1E1E1E")
    }

    private fun tint(hex: String) =
        android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor(hex))
}
