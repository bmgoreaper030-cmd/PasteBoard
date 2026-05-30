package com.pasteboard.keyboard

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.*
import android.widget.Button
import android.widget.LinearLayout
import androidx.core.app.NotificationCompat

class FloatingService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatView: View
    private var playBtn: Button? = null
    private var recordBtn: Button? = null

    companion object {
        var instance: FloatingService? = null
        var lastRecordingId: String? = null

        fun updateButtons() {
            instance?.refreshButtons()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        startForeground(1, buildNotification())
        showFloatingButtons()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun showFloatingButtons() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        // Play/Stop button
        playBtn = Button(this).apply {
            text = if (TapAccessibilityService.isPlaying) "⏹" else "▶"
            textSize = 20f
            setBackgroundColor(0xFF3B82F6.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            layoutParams = LinearLayout.LayoutParams(148, 148)
        }

        // Record button
        recordBtn = Button(this).apply {
            text = "⏺"
            textSize = 16f
            setBackgroundColor(0xFFEF4444.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            layoutParams = LinearLayout.LayoutParams(148, 100).apply {
                topMargin = 8
            }
        }

        layout.addView(playBtn)
        layout.addView(recordBtn)

        val closeBtn = Button(this).apply {
            text = "✕"
            textSize = 14f
            setBackgroundColor(0xFF6B7280.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            layoutParams = LinearLayout.LayoutParams(148, 80).apply { topMargin = 4 }
        }
        layout.addView(closeBtn)
        closeBtn.setOnClickListener { stopSelf() }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 16
            y = 300
        }

        windowManager.addView(layout, params)

        // Drag
        var dx = 0f; var dy = 0f
        layout.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> { dx = event.rawX - params.x; dy = event.rawY - params.y; false }
                MotionEvent.ACTION_MOVE -> {
                    params.x = (event.rawX - dx).toInt()
                    params.y = (event.rawY - dy).toInt()
                    windowManager.updateViewLayout(layout, params)
                    true
                }
                else -> false
            }
        }

        playBtn?.setOnClickListener {
            if (TapAccessibilityService.isPlaying) {
                TapAccessibilityService.instance?.stopPlayback()
                playBtn?.text = "▶"
                playBtn?.setBackgroundColor(0xFF3B82F6.toInt())
            } else {
                val id = lastRecordingId ?: TapRecorder.recordings.lastOrNull()?.id
                if (id != null && TapAccessibilityService.instance != null) {
                    TapAccessibilityService.instance?.startPlayback(id)
                    playBtn?.text = "⏹"
                    playBtn?.setBackgroundColor(0xFF22C55E.toInt())
                }
            }
        }

        recordBtn?.setOnClickListener {
            val i = Intent(this, TapMateActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra("action", "record")
            }
            startActivity(i)
        }

        floatView = layout
    }

    fun refreshButtons() {
        playBtn?.text = if (TapAccessibilityService.isPlaying) "⏹" else "▶"
        playBtn?.setBackgroundColor(
            if (TapAccessibilityService.isPlaying) 0xFF22C55E.toInt() else 0xFF3B82F6.toInt()
        )
    }

    private fun buildNotification(): Notification {
        val channelId = "tapmate_float"
        val channel = NotificationChannel(channelId, "TapMate", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        val stopIntent = PendingIntent.getService(
            this, 0,
            Intent(this, FloatingService::class.java).apply { action = "STOP" },
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("TapMate Active")
            .setContentText("▶ tap float button to play • ⏺ to record")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .addAction(android.R.drawable.ic_delete, "Stop Service", stopIntent)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        if (::floatView.isInitialized) {
            try { windowManager.removeView(floatView) } catch (e: Exception) {}
        }
    }
}
