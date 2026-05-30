package com.pasteboard.keyboard

import android.app.*
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.*
import android.widget.Button
import androidx.core.app.NotificationCompat

class FloatingService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatView: View

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(1, buildNotification())
        showFloatingButton()
    }

    private fun showFloatingButton() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        floatView = LayoutInflater.from(this).inflate(R.layout.floating_button, null)

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

        windowManager.addView(floatView, params)

        val btn = floatView.findViewById<Button>(R.id.floatBtn)
        updateButton(btn)

        // Drag support
        var dx = 0f; var dy = 0f
        floatView.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> { dx = event.rawX - params.x; dy = event.rawY - params.y; false }
                MotionEvent.ACTION_MOVE -> {
                    params.x = (event.rawX - dx).toInt()
                    params.y = (event.rawY - dy).toInt()
                    windowManager.updateViewLayout(floatView, params)
                    true
                }
                else -> false
            }
        }

        btn.setOnClickListener {
            if (TapAccessibilityService.isPlaying) {
                TapAccessibilityService.instance?.stopPlayback()
            } else {
                // Open app to select recording
                val i = Intent(this, TapMateActivity::class.java)
                i.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(i)
            }
            updateButton(btn)
        }
    }

    private fun updateButton(btn: Button) {
        btn.text = if (TapAccessibilityService.isPlaying) "⏹" else "▶"
    }

    private fun buildNotification(): Notification {
        val channelId = "tapmate_float"
        val channel = NotificationChannel(channelId, "TapMate", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("TapMate Running")
            .setContentText("Tap the floating button to stop")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::floatView.isInitialized) windowManager.removeView(floatView)
    }
}
