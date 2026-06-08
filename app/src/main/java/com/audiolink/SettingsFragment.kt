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
    private lateinit var radioBufferSize: RadioGroup
    private lateinit var etAudioPort: EditText
    private lateinit var etDiscoveryPort: EditText
    private lateinit var etScanTimeout: EditText
    private lateinit var tvCurrentConfig: TextView
    private lateinit var btnSave: Button
    private lateinit var btnReset: Button
    private lateinit var tvSaved: TextView

    // Sample rate radio buttons
    private lateinit var rb8000: RadioButton
    private lateinit var rb11025: RadioButton
    private lateinit var rb16000: RadioButton
    private lateinit var rb22050: RadioButton
    private lateinit var rb44100: RadioButton
    private lateinit var rb48000: RadioButton

    // Buffer size radio buttons
    private lateinit var rb256: RadioButton
    private lateinit var rb512: RadioButton
    private lateinit var rb1024: RadioButton
    private lateinit var rb2048: RadioButton
    private lateinit var rb4096: RadioButton
    private lateinit var rb8192: RadioButton
    private lateinit var rb16384: RadioButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_settings, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Presets
        btnPresetGame  = view.findViewById(R.id.btnPresetGame)
        btnPresetMovie = view.findViewById(R.id.btnPresetMovie)
        btnPresetMusic = view.findViewById(R.id.btnPresetMusic)
        tvPresetDesc   = view.findViewById(R.id.tvPresetDesc)

        // Sample rate
        radioSampleRate = view.findViewById(R.id.radioSampleRate)
        rb8000  = view.findViewById(R.id.rb8000)
        rb11025 = view.findViewById(R.id.rb11025)
        rb16000 = view.findViewById(R.id.rb16000)
        rb22050 = view.findViewById(R.id.rb22050)
        rb44100 = view.findViewById(R.id.rb44100)
        rb48000 = view.findViewById(R.id.rb48000)

        // Buffer size
        radioBufferSize = view.findViewById(R.id.radioBufferSize)
        rb256   = view.findViewById(R.id.rb256)
        rb512   = view.findViewById(R.id.rb512)
        rb1024  = view.findViewById(R.id.rb1024)
        rb2048  = view.findViewById(R.id.rb2048)
        rb4096  = view.findViewById(R.id.rb4096)
        rb8192  = view.findViewById(R.id.rb8192)
        rb16384 = view.findViewById(R.id.rb16384)

        // Advanced
        etAudioPort     = view.findViewById(R.id.etAudioPort)
        etDiscoveryPort = view.findViewById(R.id.etDiscoveryPort)
        etScanTimeout   = view.findViewById(R.id.etScanTimeout)
        tvCurrentConfig = view.findViewById(R.id.tvCurrentConfig)
        btnSave  = view.findViewById(R.id.btnSaveSettings)
        btnReset = view.findViewById(R.id.btnResetSettings)
        tvSaved  = view.findViewById(R.id.tvSettingsSaved)

        loadCurrentValues()

        // Preset buttons
        btnPresetGame.setOnClickListener { applyPreset(SettingsManager.PRESET_GAME) }
        btnPresetMovie.setOnClickListener { applyPreset(SettingsManager.PRESET_MOVIE) }
        btnPresetMusic.setOnClickListener { applyPreset(SettingsManager.PRESET_MUSIC) }

        // Update config summary when selection changes
        radioSampleRate.setOnCheckedChangeListener { _, _ -> updateConfigSummary() }
        radioBufferSize.setOnCheckedChangeListener { _, _ -> updateConfigSummary() }

        btnSave.setOnClickListener { saveSettings() }
        btnReset.setOnClickListener { resetSettings() }
    }

    private fun applyPreset(preset: SettingsManager.Preset) {
        // Select correct sample rate radio
        selectSampleRate(preset.sampleRate)
        selectBufferSize(preset.bufferSize)
        updateConfigSummary()

        val desc = when (preset.name) {
            "Game"  -> "🎮 Game: 22050 Hz, 512 B buffer — lowest latency"
            "Movie" -> "🎬 Movie: 44100 Hz, 2048 B buffer — balanced"
            "Music" -> "🎵 Music: 48000 Hz, 4096 B buffer — best quality"
            else -> ""
        }
        tvPresetDesc.text = desc
        tvPresetDesc.setTextColor(android.graphics.Color.parseColor("#4CAF50"))

        // Highlight active preset button
        btnPresetGame.backgroundTintList  = tint("#1E1E1E")
        btnPresetMovie.backgroundTintList = tint("#1E1E1E")
        btnPresetMusic.backgroundTintList = tint("#1E1E1E")
        when (preset.name) {
            "Game"  -> btnPresetGame.backgroundTintList  = tint("#1565C0")
            "Movie" -> btnPresetMovie.backgroundTintList = tint("#1565C0")
            "Music" -> btnPresetMusic.backgroundTintList = tint("#1565C0")
        }
    }

    private fun selectSampleRate(rate: Int) {
        when (rate) {
            8000  -> rb8000.isChecked  = true
            11025 -> rb11025.isChecked = true
            16000 -> rb16000.isChecked = true
            22050 -> rb22050.isChecked = true
            44100 -> rb44100.isChecked = true
            48000 -> rb48000.isChecked = true
        }
    }

    private fun selectBufferSize(size: Int) {
        when (size) {
            256   -> rb256.isChecked   = true
            512   -> rb512.isChecked   = true
            1024  -> rb1024.isChecked  = true
            2048  -> rb2048.isChecked  = true
            4096  -> rb4096.isChecked  = true
            8192  -> rb8192.isChecked  = true
            16384 -> rb16384.isChecked = true
        }
    }

    private fun getSelectedSampleRate(): Int {
        return when (radioSampleRate.checkedRadioButtonId) {
            R.id.rb8000  -> 8000
            R.id.rb11025 -> 11025
            R.id.rb16000 -> 16000
            R.id.rb22050 -> 22050
            R.id.rb44100 -> 44100
            R.id.rb48000 -> 48000
            else -> SettingsManager.DEFAULT_SAMPLE_RATE
        }
    }

    private fun getSelectedBufferSize(): Int {
        return when (radioBufferSize.checkedRadioButtonId) {
            R.id.rb256   -> 256
            R.id.rb512   -> 512
            R.id.rb1024  -> 1024
            R.id.rb2048  -> 2048
            R.id.rb4096  -> 4096
            R.id.rb8192  -> 8192
            R.id.rb16384 -> 16384
            else -> SettingsManager.DEFAULT_BUFFER_SIZE
        }
    }

    private fun updateConfigSummary() {
        val rate = getSelectedSampleRate()
        val buf  = getSelectedBufferSize()
        // Rough latency estimate in ms
        val latencyMs = ((buf.toFloat() / (rate * 2 * 2)) * 1000).toInt() + 30
        val kbps = (rate * 2 * 2 * 8) / 1000
        tvCurrentConfig.text =
            "Sample Rate : $rate Hz\n" +
            "Buffer Size : $buf bytes\n" +
            "Bitrate     : ~$kbps kbps\n" +
            "Est. Latency: ~${latencyMs}ms"
    }

    private fun loadCurrentValues() {
        val ctx = requireContext()
        selectSampleRate(SettingsManager.getSampleRate(ctx))
        selectBufferSize(SettingsManager.getBufferSize(ctx))
        etAudioPort.setText(SettingsManager.getAudioPort(ctx).toString())
        etDiscoveryPort.setText(SettingsManager.getDiscoveryPort(ctx).toString())
        etScanTimeout.setText((SettingsManager.getScanTimeoutMs(ctx) / 1000).toString())
        updateConfigSummary()

        // Highlight active preset if matching
        val rate = SettingsManager.getSampleRate(ctx)
        val buf  = SettingsManager.getBufferSize(ctx)
        when {
            rate == 22050 && buf == 512  -> applyPresetHighlight("Game")
            rate == 44100 && buf == 2048 -> applyPresetHighlight("Movie")
            rate == 48000 && buf == 4096 -> applyPresetHighlight("Music")
        }
    }

    private fun applyPresetHighlight(name: String) {
        btnPresetGame.backgroundTintList  = tint("#1E1E1E")
        btnPresetMovie.backgroundTintList = tint("#1E1E1E")
        btnPresetMusic.backgroundTintList = tint("#1E1E1E")
        when (name) {
            "Game"  -> btnPresetGame.backgroundTintList  = tint("#1565C0")
            "Movie" -> btnPresetMovie.backgroundTintList = tint("#1565C0")
            "Music" -> btnPresetMusic.backgroundTintList = tint("#1565C0")
        }
    }

    private fun saveSettings() {
        val ctx = requireContext()
        val audioPort     = etAudioPort.text.toString().toIntOrNull()
        val discoveryPort = etDiscoveryPort.text.toString().toIntOrNull()
        val scanTimeout   = etScanTimeout.text.toString().toIntOrNull()

        if (audioPort == null || audioPort !in 1024..65535) {
            etAudioPort.error = "Valid port: 1024-65535"
            return
        }
        if (discoveryPort == null || discoveryPort !in 1024..65535) {
            etDiscoveryPort.error = "Valid port: 1024-65535"
            return
        }
        if (audioPort == discoveryPort) {
            etDiscoveryPort.error = "Must differ from audio port"
            return
        }
        if (scanTimeout == null || scanTimeout !in 2..60) {
            etScanTimeout.error = "Enter 2-60 seconds"
            return
        }

        SettingsManager.setSampleRate(ctx, getSelectedSampleRate())
        SettingsManager.setBufferSize(ctx, getSelectedBufferSize())
        SettingsManager.setAudioPort(ctx, audioPort)
        SettingsManager.setDiscoveryPort(ctx, discoveryPort)
        SettingsManager.setScanTimeout(ctx, scanTimeout)

        tvSaved.text = "✓ Settings saved — restart stream to apply"
        tvSaved.visibility = View.VISIBLE
        tvSaved.postDelayed({ tvSaved.visibility = View.GONE }, 3000)
    }

    private fun resetSettings() {
        SettingsManager.resetAll(requireContext())
        loadCurrentValues()
        tvPresetDesc.text = "Tap a preset to auto-configure"
        tvPresetDesc.setTextColor(android.graphics.Color.parseColor("#888888"))
        btnPresetGame.backgroundTintList  = tint("#1E1E1E")
        btnPresetMovie.backgroundTintList = tint("#1E1E1E")
        btnPresetMusic.backgroundTintList = tint("#1E1E1E")
        tvSaved.text = "✓ Reset to defaults"
        tvSaved.visibility = View.VISIBLE
        tvSaved.postDelayed({ tvSaved.visibility = View.GONE }, 2500)
    }

    private fun tint(hex: String) =
        android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor(hex))
}
