package com.pasteboard.keyboard

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class TapEvent(
    val x: Float,
    val y: Float,
    val timestamp: Long,
    val duration: Long = 50L
)

data class TapRecording(
    val id: String,
    val name: String,
    val events: List<TapEvent>,
    val createdAt: Long = System.currentTimeMillis()
)

object TapRecorder {
    private const val PREFS = "tapmate_prefs"
    private const val KEY = "recordings"
    private val gson = Gson()

    private var _recordings = mutableListOf<TapRecording>()
    val recordings: List<TapRecording> get() = _recordings

    var isRecording = false
    var recordingStartTime = 0L
    val currentEvents = mutableListOf<TapEvent>()

    fun load(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY, null) ?: return
        val type = object : TypeToken<List<TapRecording>>() {}.type
        _recordings = gson.fromJson(json, type) ?: mutableListOf()
    }

    fun save(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, gson.toJson(_recordings))
            .apply()
    }

    fun startRecording() {
        isRecording = true
        recordingStartTime = System.currentTimeMillis()
        currentEvents.clear()
    }

    fun addEvent(x: Float, y: Float) {
        if (!isRecording) return
        currentEvents.add(TapEvent(x, y, System.currentTimeMillis() - recordingStartTime))
    }

    fun stopRecording(context: Context, name: String) {
        isRecording = false
        if (currentEvents.isEmpty()) return
        val rec = TapRecording(
            id = System.currentTimeMillis().toString(),
            name = name,
            events = currentEvents.toList()
        )
        _recordings.add(rec)
        save(context)
        currentEvents.clear()
    }

    fun deleteRecording(context: Context, id: String) {
        _recordings.removeAll { it.id == id }
        save(context)
    }
}
