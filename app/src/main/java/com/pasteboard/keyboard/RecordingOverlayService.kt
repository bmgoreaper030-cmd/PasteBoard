package com.pasteboard.keyboard

import android.app.*
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.*
import android.widget.Toast
import androidx.core.app.NotificationCompat

class RecordingOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(2, buildNotification())
        showOverlay()
        Toast.makeText(this, "Recording started — tap anywhere!", Toast.LENGTH_SHORT).show()
    }

    private fun showOverlay() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        overlayView = View(this).apply {
            setBackgroundColor(0x22FF0000)
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        )

        overlayView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                TapRecorder.addEvent(event.rawX, event.rawY)
                Toast.makeText(this, "Tap ${TapRecorder.currentEvents.size} recorded", Toast.LENGTH_SHORT).show()
            }
            false
        }

        windowManager.addView(overlayView, params)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") {
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        try { windowManager.removeView(overlayView) } catch (e: Exception) {}
        // Show save dialog via broadcast
        val i = Intent(this, TapMateActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra("action", "save")
        }
        startActivity(i)
    }

    private fun buildNotification(): Notification {
        val channelId = "recording_overlay"
        val channel = NotificationChannel(channelId, "Recording", NotificationManager.IMPORTANCE_HIGH)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        val stopIntent = PendingIntent.getService(
            this, 0,
            Intent(this, RecordingOverlayService::class.java).apply { action = "STOP" },
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("🔴 Recording taps...")
            .setContentText("Tap anywhere to record. Tap here to stop.")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(stopIntent)
            .build()
    }
}
