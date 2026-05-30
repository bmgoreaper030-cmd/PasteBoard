package com.pasteboard.keyboard

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

data class ScriptFile(
    val id: String,
    val name: String,
    val lines: List<String>,
    var currentIndex: Int = 0
)

object ScriptManager {

    private const val PREFS_NAME = "pasteboard_prefs"
    private const val KEY_FILES = "script_files"
    private const val KEY_ACTIVE = "active_file_id"

    private var _files = mutableListOf<ScriptFile>()
    val files: List<ScriptFile> get() = _files

    var activeFileId: String? = null
        private set

    val activeFile: ScriptFile?
        get() = _files.firstOrNull { it.id == activeFileId }

    fun load(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_FILES, null) ?: return
        activeFileId = prefs.getString(KEY_ACTIVE, null)

        _files.clear()
        val arr = JSONArray(json)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val linesArr = obj.getJSONArray("lines")
            val lines = (0 until linesArr.length()).map { linesArr.getString(it) }
            _files.add(
                ScriptFile(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    lines = lines,
                    currentIndex = obj.optInt("currentIndex", 0)
                )
            )
        }
        if (activeFileId == null) activeFileId = _files.firstOrNull()?.id
    }

    fun save(context: Context) {
        val arr = JSONArray()
        _files.forEach { file ->
            val obj = JSONObject()
            obj.put("id", file.id)
            obj.put("name", file.name)
            obj.put("currentIndex", file.currentIndex)
            val linesArr = JSONArray()
            file.lines.forEach { linesArr.put(it) }
            obj.put("lines", linesArr)
            arr.put(obj)
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_FILES, arr.toString())
            .putString(KEY_ACTIVE, activeFileId)
            .apply()
    }

    fun importFile(context: Context, uri: Uri, name: String) {
        val content = context.contentResolver.openInputStream(uri)
            ?.bufferedReader()?.readText() ?: return
        val lines = content.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (lines.isEmpty()) return

        val file = ScriptFile(
            id = System.currentTimeMillis().toString(),
            name = name.removeSuffix(".txt"),
            lines = lines
        )
        _files.add(file)
        if (activeFileId == null) activeFileId = file.id
        save(context)
    }

    fun deleteFile(context: Context, id: String) {
        _files.removeAll { it.id == id }
        if (activeFileId == id) activeFileId = _files.firstOrNull()?.id
        save(context)
    }

    fun setActive(context: Context, id: String) {
        activeFileId = id
        save(context)
    }

    fun cycleToNext(context: Context) {
        if (_files.size <= 1) return
        val idx = _files.indexOfFirst { it.id == activeFileId }
        activeFileId = _files[(idx + 1) % _files.size].id
        save(context)
    }

    fun consumeLine(context: Context, shuffle: Boolean): String? {
        val idx = _files.indexOfFirst { it.id == activeFileId }
        if (idx == -1) return null
        val file = _files[idx]
        if (file.lines.isEmpty()) return null

        val line = if (shuffle) {
            file.lines.random()
        } else {
            val line = file.lines[file.currentIndex % file.lines.size]
            _files[idx] = file.copy(currentIndex = (file.currentIndex + 1) % file.lines.size)
            line
        }
        save(context)
        return line
    }
}
