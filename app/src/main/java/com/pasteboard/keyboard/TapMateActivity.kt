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
    private lateinit var recordingAdapter: ArrayAdapter<String>

    private var selectedRecordingId: String? = null
    private var isRecording = false
    private var countdown: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tapmate)
        TapRecorder.load(this)
        bindViews()
        setupControls()
        updateRecordingList()
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
        // Speed (0.5x to 5x, default 1x)
        seekSpeed.max = 90
        seekSpeed.progress = 10
        updateSpeedLabel()
        seekSpeed.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, f: Boolean) { updateSpeedLabel() }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })

        // Loops (0 = infinite, 1-100)
        seekLoops.max = 100
        seekLoops.progress = 0
        updateLoopsLabel()
        seekLoops.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, f: Boolean) { updateLoopsLabel() }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })

        // Delay min/max (0-5000ms)
        seekDelayMin.max = 5000
        seekDelayMax.max = 5000
        updateDelayLabel()
        val delayListener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, f: Boolean) { updateDelayLabel() }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        }
        seekDelayMin.setOnSeekBarChangeListener(delayListener)
        seekDelayMax.setOnSeekBarChangeListener(delayListener)

        // Record button — overlay the whole screen to capture touches
        btnRecord.setOnClickListener {
            if (!isRecording) startRecording() else stopRecording()
        }

        btnPlay.setOnClickListener {
            val id = selectedRecordingId ?: run {
                toast("Select a recording first"); return@setOnClickListener
            }
            if (TapAccessibilityService.instance == null) {
                promptAccessibility(); return@setOnClickListener
            }
            applySettings()
            startCountdownAndPlay(id)
        }

        btnStop.setOnClickListener {
            TapAccessibilityService.instance?.stopPlayback()
            countdown?.cancel()
            tvStatus.text = "Stopped"
            tvCountdown.text = ""
        }

        btnFloat.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")))
            } else {
                startService(Intent(this, FloatingService::class.java))
                finish()
            }
        }

        recordingList.setOnItemClickListener { _, _, pos, _ ->
            selectedRecordingId = TapRecorder.recordings[pos].id
            tvStatus.text = "Selected: ${TapRecorder.recordings[pos].name}"
        }

        recordingList.setOnItemLongClickListener { _, _, pos, _ ->
            val rec = TapRecorder.recordings[pos]
            AlertDialog.Builder(this)
                .setTitle("Delete \"${rec.name}\"?")
                .setPositiveButton("Delete") { _, _ ->
                    TapRecorder.deleteRecording(this, rec.id)
                    updateRecordingList()
                }
                .setNegativeButton("Cancel", null)
                .show()
            true
        }
    }

    private fun startRecording() {
        isRecording = true
        TapRecorder.startRecording()
        btnRecord.text = "⏹ Stop Recording"
        tvStatus.text = "Recording... tap anywhere on screen"

        // Transparent overlay to capture taps
        window.decorView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                TapRecorder.addEvent(event.rawX, event.rawY)
            }
            false
        }
    }

    private fun stopRecording() {
        isRecording = false
        window.decorView.setOnTouchListener(null)
        btnRecord.text = "⏺ Start Recording"

        val input = EditText(this).apply { hint = "Recording name" }
        AlertDialog.Builder(this)
            .setTitle("Save Recording")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val name = input.text.toString().ifEmpty { "Recording ${TapRecorder.recordings.size + 1}" }
                TapRecorder.stopRecording(this, name)
                updateRecordingList()
                tvStatus.text = "Saved!"
            }
            .setNegativeButton("Discard") { _, _ ->
                TapRecorder.currentEvents.clear()
                TapRecorder.isRecording = false
            }
            .show()
    }

    private fun startCountdownAndPlay(id: String) {
        val seconds = TapAccessibilityService.countdownSeconds
        if (seconds == 0) {
            TapAccessibilityService.instance?.startPlayback(id)
            tvStatus.text = "Playing..."
            return
        }
        countdown = object : CountDownTimer(seconds * 1000L, 1000) {
            override fun onTick(ms: Long) {
                tvCountdown.text = "Starting in ${ms / 1000 + 1}..."
            }
            override fun onFinish() {
                tvCountdown.text = ""
                TapAccessibilityService.instance?.startPlayback(id)
                tvStatus.text = "Playing..."
            }
        }.start()
    }

    private fun applySettings() {
        TapAccessibilityService.speedMultiplier = 0.5f + (seekSpeed.progress * 0.05f)
        TapAccessibilityService.loopCount = seekLoops.progress
        val min = seekDelayMin.progress.toLong()
        val max = maxOf(seekDelayMax.progress.toLong(), min)
        TapAccessibilityService.delayRange = if (switchShuffleDelay.isChecked) Pair(min, max) else Pair(0L, 0L)
        TapAccessibilityService.countdownSeconds = 3
    }

    private fun updateRecordingList() {
        val names = TapRecorder.recordings.map { "${it.name} (${it.events.size} taps)" }
        recordingAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, names)
        recordingList.adapter = recordingAdapter
    }

    private fun updateSpeedLabel() {
        val speed = 0.5f + (seekSpeed.progress * 0.05f)
        tvSpeed.text = "Speed: ${"%.1f".format(speed)}x"
        TapAccessibilityService.speedMultiplier = speed
    }

    private fun updateLoopsLabel() {
        tvLoops.text = if (seekLoops.progress == 0) "Loops: ∞ infinite" else "Loops: ${seekLoops.progress}"
    }

    private fun updateDelayLabel() {
        val min = seekDelayMin.progress
        val max = seekDelayMax.progress
        tvDelay.text = "Delay: ${min}ms – ${max}ms per tap"
    }

    private fun promptAccessibility() {
        AlertDialog.Builder(this)
            .setTitle("Enable Accessibility")
            .setMessage("TapMate needs Accessibility permission to replay taps.\n\nSettings → Accessibility → Installed apps → TapMate → Enable")
            .setPositiveButton("Open Settings") { _, _ ->
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
