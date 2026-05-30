package com.pasteboard.keyboard

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.Settings
import android.view.MotionEvent
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class TapMateActivity : AppCompatActivity() {

    private lateinit var btnRecord: Button
    private lateinit var btnPlay: Button
    private lateinit var btnStop: Button
    private lateinit var btnFloat: Button
    private lateinit var tvStatus: TextView
    private lateinit var tvCountdown: TextView
    private lateinit var seekSpeed: SeekBar
    private lateinit var tvSpeed: TextView
    private lateinit var seekLoops: SeekBar
    private lateinit var tvLoops: TextView
    private lateinit var seekDelayMin: SeekBar
    private lateinit var seekDelayMax: SeekBar
    private lateinit var tvDelay: TextView
    private lateinit var switchShuffleDelay: Switch
    private lateinit var recordingList: ListView

    private var selectedRecordingId: String? = null
    private var isRecording = false
    private var countdown: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tapmate)
        TapRecorder.load(this)
        bindViews()
        setupControls()
        refreshRecordingList()

        // If launched from float record button
        if (intent?.getStringExtra("action") == "save") {
            if (TapRecorder.currentEvents.isNotEmpty()) stopRecording()
        }
        if (intent?.getStringExtra("action") == "record") {
            startRecording()
        }
    }

    override fun onResume() {
        super.onResume()
        TapRecorder.load(this)
        refreshRecordingList()
    }

    private fun bindViews() {
        btnRecord = findViewById(R.id.btnRecord)
        btnPlay = findViewById(R.id.btnPlay)
        btnStop = findViewById(R.id.btnStop)
        btnFloat = findViewById(R.id.btnFloat)
        tvStatus = findViewById(R.id.tvStatus)
        tvCountdown = findViewById(R.id.tvCountdown)
        seekSpeed = findViewById(R.id.seekSpeed)
        tvSpeed = findViewById(R.id.tvSpeed)
        seekLoops = findViewById(R.id.seekLoops)
        tvLoops = findViewById(R.id.tvLoops)
        seekDelayMin = findViewById(R.id.seekDelayMin)
        seekDelayMax = findViewById(R.id.seekDelayMax)
        tvDelay = findViewById(R.id.tvDelay)
        switchShuffleDelay = findViewById(R.id.switchShuffleDelay)
        recordingList = findViewById(R.id.recordingList)
    }

    private fun setupControls() {
        seekSpeed.max = 90
        seekSpeed.progress = 10
        updateSpeedLabel()
        seekSpeed.setOnSeekBarChangeListener(simpleSeekListener { updateSpeedLabel() })

        seekLoops.max = 100
        seekLoops.progress = 0
        updateLoopsLabel()
        seekLoops.setOnSeekBarChangeListener(simpleSeekListener { updateLoopsLabel() })

        seekDelayMin.max = 5000
        seekDelayMax.max = 5000
        updateDelayLabel()
        seekDelayMin.setOnSeekBarChangeListener(simpleSeekListener { updateDelayLabel() })
        seekDelayMax.setOnSeekBarChangeListener(simpleSeekListener { updateDelayLabel() })

        btnRecord.setOnClickListener {
            if (!isRecording) startRecording() else stopRecording()
        }

        btnPlay.setOnClickListener {
            val id = selectedRecordingId ?: TapRecorder.recordings.lastOrNull()?.id ?: run {
                toast("No recording selected"); return@setOnClickListener
            }
            if (TapAccessibilityService.instance == null) {
                promptAccessibility(); return@setOnClickListener
            }
            applySettings()
            FloatingService.lastRecordingId = id
            startCountdownAndPlay(id)
        }

        btnStop.setOnClickListener {
            TapAccessibilityService.instance?.stopPlayback()
            countdown?.cancel()
            tvStatus.text = "Stopped"
            tvCountdown.text = ""
            FloatingService.updateButtons()
        }

        btnFloat.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")))
                toast("Enable overlay permission then tap again")
            } else {
                startService(Intent(this, FloatingService::class.java))
                toast("Floating buttons active!")
            }
        }

        recordingList.setOnItemClickListener { _, _, pos, _ ->
            if (pos < TapRecorder.recordings.size) {
                selectedRecordingId = TapRecorder.recordings[pos].id
                FloatingService.lastRecordingId = selectedRecordingId
                tvStatus.text = "Selected: ${TapRecorder.recordings[pos].name}"
            }
        }

        recordingList.setOnItemLongClickListener { _, _, pos, _ ->
            val rec = TapRecorder.recordings[pos]
            AlertDialog.Builder(this)
                .setTitle("Delete \"${rec.name}\"?")
                .setPositiveButton("Delete") { _, _ ->
                    TapRecorder.deleteRecording(this, rec.id)
                    refreshRecordingList()
                }
                .setNegativeButton("Cancel", null)
                .show()
            true
        }
    }

    private fun startRecording() {
        startService(android.content.Intent(this, RecordingOverlayService::class.java))
        btnRecord.text = "⏹ Stop Recording"
        tvStatus.text = "Recording via overlay — tap notification to stop"
        isRecording = true
        return
        // old code below:
        isRecording = true
        TapRecorder.startRecording()
        btnRecord.text = "⏹ Stop Recording"
        tvStatus.text = "Recording taps..."
        window.decorView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                TapRecorder.addEvent(event.rawX, event.rawY)
                tvStatus.text = "Recording... ${TapRecorder.currentEvents.size} taps"
            }
            false
        }
    }

    private fun stopRecording() {
        isRecording = false
        window.decorView.setOnTouchListener(null)
        btnRecord.text = "⏺ Start Recording"
        if (TapRecorder.currentEvents.isEmpty()) {
            tvStatus.text = "No taps recorded"; return
        }
        val input = EditText(this).apply { hint = "Recording name" }
        AlertDialog.Builder(this)
            .setTitle("Save Recording")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val name = input.text.toString().ifEmpty { "Recording ${TapRecorder.recordings.size + 1}" }
                TapRecorder.stopRecording(this, name)
                refreshRecordingList()
                tvStatus.text = "Saved: $name"
            }
            .setNegativeButton("Discard") { _, _ ->
                TapRecorder.currentEvents.clear()
                tvStatus.text = "Discarded"
            }
            .show()
    }

    private fun startCountdownAndPlay(id: String) {
        countdown = object : CountDownTimer(3000L, 1000) {
            override fun onTick(ms: Long) { tvCountdown.text = "Starting in ${ms / 1000 + 1}..." }
            override fun onFinish() {
                tvCountdown.text = ""
                TapAccessibilityService.instance?.startPlayback(id)
                tvStatus.text = "▶ Playing..."
                FloatingService.updateButtons()
            }
        }.start()
    }

    private fun applySettings() {
        TapAccessibilityService.speedMultiplier = 0.5f + (seekSpeed.progress * 0.05f)
        TapAccessibilityService.loopCount = seekLoops.progress
        val min = seekDelayMin.progress.toLong()
        val max = maxOf(seekDelayMax.progress.toLong(), min)
        TapAccessibilityService.delayRange = if (switchShuffleDelay.isChecked) Pair(min, max) else Pair(0L, 0L)
    }

    private fun refreshRecordingList() {
        val names = TapRecorder.recordings.map { "📌 ${it.name}  •  ${it.events.size} taps" }
        if (names.isEmpty()) {
            recordingList.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1,
                listOf("No recordings yet — tap Start Recording"))
        } else {
            recordingList.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, names)
        }
    }

    private fun updateSpeedLabel() {
        val speed = 0.5f + (seekSpeed.progress * 0.05f)
        tvSpeed.text = "Speed: ${"%.1f".format(speed)}x"
    }

    private fun updateLoopsLabel() {
        tvLoops.text = if (seekLoops.progress == 0) "Loops: ∞ infinite" else "Loops: ${seekLoops.progress}"
    }

    private fun updateDelayLabel() {
        tvDelay.text = "Delay: ${seekDelayMin.progress}ms – ${seekDelayMax.progress}ms per tap"
    }

    private fun simpleSeekListener(onChange: () -> Unit) = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(s: SeekBar?, p: Int, f: Boolean) = onChange()
        override fun onStartTrackingTouch(s: SeekBar?) {}
        override fun onStopTrackingTouch(s: SeekBar?) {}
    }

    private fun promptAccessibility() {
        AlertDialog.Builder(this)
            .setTitle("Enable Accessibility")
            .setMessage("Settings → Accessibility → Installed apps → TapMate → Enable")
            .setPositiveButton("Open Settings") { _, _ ->
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
