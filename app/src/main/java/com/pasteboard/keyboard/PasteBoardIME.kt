package com.pasteboard.keyboard

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast

class PasteBoardIME : InputMethodService() {

    private var isDark = false
    private var isShuffle = false

    override fun onCreateInputView(): View {
        ScriptManager.load(this)
        val view = layoutInflater.inflate(R.layout.keyboard_view, null)
        setupKeyboard(view)
        return view
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        ScriptManager.load(this)
    }

    private fun setupKeyboard(view: View) {
        updateFileLabel(view)

        // Paste line button
        view.findViewById<View>(R.id.btnPaste).setOnClickListener {
            val line = ScriptManager.consumeLine(this, isShuffle)
            if (line != null) {
                currentInputConnection?.commitText(line, 1)
                updateFileLabel(view)
            } else {
                Toast.makeText(this, "No script loaded — open PasteBoard app to import a .txt file", Toast.LENGTH_SHORT).show()
            }
        }

        // Shuffle toggle
        val btnShuffle = view.findViewById<ImageButton>(R.id.btnShuffle)
        btnShuffle.setOnClickListener {
            isShuffle = !isShuffle
            btnShuffle.setImageResource(
                if (isShuffle) R.drawable.ic_shuffle_on else R.drawable.ic_shuffle_off
            )
            btnShuffle.alpha = if (isShuffle) 1f else 0.5f
        }

        // Theme toggle
        val btnTheme = view.findViewById<ImageButton>(R.id.btnTheme)
        btnTheme.setOnClickListener {
            isDark = !isDark
            applyTheme(view)
        }

        // Cycle file
        view.findViewById<ImageButton>(R.id.btnNextFile).setOnClickListener {
            ScriptManager.cycleToNext(this)
            updateFileLabel(view)
        }

        // Delete
        view.findViewById<View>(R.id.btnDelete).setOnClickListener {
            currentInputConnection?.deleteSurroundingText(1, 0)
        }

        // Space
        view.findViewById<View>(R.id.btnSpace).setOnClickListener {
            currentInputConnection?.commitText(" ", 1)
        }

        // Return
        view.findViewById<View>(R.id.btnReturn).setOnClickListener {
            currentInputConnection?.commitText("\n", 1)
        }

        // Switch keyboard
        view.findViewById<View>(R.id.btnSwitch).setOnClickListener {
            switchToNextInputMethod(false)
        }

        applyTheme(view)
    }

    private fun updateFileLabel(view: View) {
        val file = ScriptManager.activeFile
        view.findViewById<TextView>(R.id.tvFileName).text =
            if (file != null) "📄 ${file.name}" else "No script — open app to import"
        view.findViewById<TextView>(R.id.tvLineCount).text =
            if (file != null) "${file.lines.size} lines" else ""
    }

    private fun applyTheme(view: View) {
        val bg = if (isDark) 0xFF1E1E1E.toInt() else 0xFFD1D5DB.toInt()
        val keyBg = if (isDark) 0xFF3A3A3A.toInt() else 0xFFF3F4F6.toInt()
        view.setBackgroundColor(bg)

        val keyIds = listOf(
            R.id.btnPaste, R.id.btnSpace, R.id.btnReturn, R.id.btnDelete
        )
        keyIds.forEach { id ->
            view.findViewById<View>(id)?.setBackgroundColor(keyBg)
        }
    }
}
