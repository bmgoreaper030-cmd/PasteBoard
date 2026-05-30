package com.pasteboard.keyboard

import android.inputmethodservice.InputMethodService
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast

class PasteBoardIME : InputMethodService() {

    private var isDark = false

    override fun onCreateInputView(): View {
        ScriptManager.load(this)
        val view = layoutInflater.inflate(R.layout.keyboard_view, null)
        setupKeyboard(view)
        return view
    }

    override fun onStartInput(attribute: android.view.inputmethod.EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        ScriptManager.load(this)
    }

    private fun setupKeyboard(view: View) {
        updateFileLabel(view)

        view.findViewById<Button>(R.id.btnPaste).setOnClickListener {
            val line = ScriptManager.consumeLine(this, shuffle = true)
            if (line != null) {
                currentInputConnection?.commitText(line, 1)
                updateFileLabel(view)
            } else {
                Toast.makeText(this, "Open PasteBoard app and import a .txt file", Toast.LENGTH_SHORT).show()
            }
        }

        view.findViewById<Button>(R.id.btnTheme).setOnClickListener {
            isDark = !isDark
            applyTheme(view)
        }

        view.findViewById<Button>(R.id.btnDelete).setOnClickListener {
            currentInputConnection?.deleteSurroundingText(1, 0)
        }

        view.findViewById<Button>(R.id.btnSpace).setOnClickListener {
            currentInputConnection?.commitText(" ", 1)
        }

        view.findViewById<Button>(R.id.btnReturn).setOnClickListener {
            currentInputConnection?.commitText("\n", 1)
        }

        view.findViewById<Button>(R.id.btnSwitch).setOnClickListener {
            switchToNextInputMode(false)
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
        view.setBackgroundColor(bg)
        val themeBtn = view.findViewById<Button>(R.id.btnTheme)
        themeBtn.text = if (isDark) "☀️" else "🌙"
    }
}
