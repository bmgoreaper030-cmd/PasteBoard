package com.pasteboard.keyboard

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.view.accessibility.AccessibilityEvent

class TapAccessibilityService : AccessibilityService() {

    companion object {
        var instance: TapAccessibilityService? = null
        var isPlaying = false
        var currentRecordingId: String? = null
        var speedMultiplier: Float = 1.0f
        var loopCount: Int = 0 // 0 = infinite
        var delayRange: Pair<Long, Long> = Pair(0L, 0L) // min, max extra delay ms
        var countdownSeconds: Int = 3
        const val ACTION_STOP = "com.pasteboard.keyboard.STOP"
        const val ACTION_START = "com.pasteboard.keyboard.START"
    }

    private val handler = Handler(Looper.getMainLooper())
    private var wakeLock: PowerManager.WakeLock? = null
    private var currentLoop = 0

    override fun onServiceConnected() {
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() { stopPlayback() }

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    fun startPlayback(recordingId: String) {
        val recording = TapRecorder.recordings.firstOrNull { it.id == recordingId } ?: return
        isPlaying = true
        currentRecordingId = recordingId
        currentLoop = 0

        // Acquire wake lock
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "TapMate:WakeLock")
        wakeLock?.acquire(10 * 60 * 1000L) // 10 min max

        // Start countdown then play
        handler.postDelayed({ playRecording(recording) }, countdownSeconds * 1000L)
    }

    private fun playRecording(recording: TapRecording) {
        if (!isPlaying) return

        var delay = 0L
        recording.events.forEachIndexed { index, event ->
            val scaledTime = (event.timestamp / speedMultiplier).toLong()
            val extraDelay = if (delayRange.second > 0) {
                (delayRange.first..delayRange.second).random()
            } else 0L

            handler.postDelayed({
                if (!isPlaying) return@postDelayed
                dispatchTap(event.x, event.y, event.duration)
            }, scaledTime + extraDelay)

            if (index == recording.events.lastIndex) {
                val totalDuration = (recording.events.last().timestamp / speedMultiplier).toLong() + extraDelay + 500L
                handler.postDelayed({
                    if (!isPlaying) return@postDelayed
                    currentLoop++
                    if (loopCount == 0 || currentLoop < loopCount) {
                        playRecording(recording)
                    } else {
                        stopPlayback()
                    }
                }, totalDuration)
            }
            delay = scaledTime
        }
    }

    private fun dispatchTap(x: Float, y: Float, duration: Long) {
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0, duration.coerceAtLeast(1))
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        dispatchGesture(gesture, null, null)
    }

    fun stopPlayback() {
        isPlaying = false
        currentRecordingId = null
        handler.removeCallbacksAndMessages(null)
        wakeLock?.release()
        wakeLock = null
    }
}
