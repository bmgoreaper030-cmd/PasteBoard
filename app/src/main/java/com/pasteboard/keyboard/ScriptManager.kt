package com.pasteboard.keyboard

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

data class ScriptFile(
    val id: String,
    val name: String,
    val lines: List<String>,
    var shuffledIndices: List<Int> = emptyList(),
    var currentIndex: Int = 0,
    var lastUsedIndex: Int = -1
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
            val indicesArr = obj.optJSONArray("shuffledIndices")
            val indices = if (indicesArr != null) {
                (0 until indicesArr.length()).map { indicesArr.getInt(it) }
            } else emptyList()
            _files.add(ScriptFile(
                id = obj.getString("id"),
                name = obj.getString("name"),
                lines = lines,
                shuffledIndices = indices,
                currentIndex = obj.optInt("currentIndex", 0),
                lastUsedIndex = obj.optInt("lastUsedIndex", -1)
            ))
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
            obj.put("lastUsedIndex", file.lastUsedIndex)
            val linesArr = JSONArray()
            file.lines.forEach { linesArr.put(it) }
            obj.put("lines", linesArr)
            val indicesArr = JSONArray()
            file.shuffledIndices.forEach { indicesArr.put(it) }
            obj.put("shuffledIndices", indicesArr)
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

        val shuffled = buildShuffledDeck(lines.indices.toList(), lastUsed = -1)
        val file = ScriptFile(
            id = System.currentTimeMillis().toString(),
            name = name.removeSuffix(".txt"),
            lines = lines,
            shuffledIndices = shuffled,
            currentIndex = 0,
            lastUsedIndex = -1
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

        return if (shuffle) {
            var deck = file.shuffledIndices
            var pos = file.currentIndex

            // Reshuffle if deck exhausted — ensure first card != last used
            if (deck.isEmpty() || pos >= deck.size) {
                deck = buildShuffledDeck(file.lines.indices.toList(), file.lastUsedIndex)
                pos = 0
            }

            val lineIndex = deck[pos]
            val line = file.lines[lineIndex]

            _files[idx] = file.copy(
                shuffledIndices = deck,
                currentIndex = pos + 1,
                lastUsedIndex = lineIndex
            )
            save(context)
            line
        } else {
            val lineIndex = file.currentIndex % file.lines.size
            val line = file.lines[lineIndex]
            _files[idx] = file.copy(
                currentIndex = (file.currentIndex + 1) % file.lines.size,
                lastUsedIndex = lineIndex
            )
            save(context)
            line
        }
    }

    // Builds a shuffled deck, guaranteeing first item != lastUsed
    private fun buildShuffledDeck(indices: List<Int>, lastUsed: Int): List<Int> {
        if (indices.size <= 1) return indices
        val deck = indices.toMutableList()
        deck.shuffle()
        // If first item is same as lastUsed, swap it with a random other position
        if (deck.first() == lastUsed) {
            val swapPos = (1 until deck.size).random()
            val tmp = deck[0]
            deck[0] = deck[swapPos]
            deck[swapPos] = tmp
        }
        return deck
    }
}
