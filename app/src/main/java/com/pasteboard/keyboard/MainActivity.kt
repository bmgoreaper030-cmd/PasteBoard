package com.pasteboard.keyboard

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.pasteboard.keyboard.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: ScriptAdapter

    private val filePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { importFile(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ScriptManager.load(this)
        setupRecyclerView()
        setupButtons()
        updateEmptyState()
    }

    override fun onResume() {
        super.onResume()
        ScriptManager.load(this)
        adapter.notifyDataSetChanged()
        updateEmptyState()
    }

    private fun setupRecyclerView() {
        adapter = ScriptAdapter(
            getFiles = { ScriptManager.files },
            activeId = { ScriptManager.activeFileId },
            onSetActive = { file ->
                ScriptManager.setActive(this, file.id)
                adapter.notifyDataSetChanged()
            },
            onDelete = { file ->
                AlertDialog.Builder(this)
                    .setTitle("Delete \"${file.name}\"?")
                    .setMessage("This cannot be undone.")
                    .setPositiveButton("Delete") { _, _ ->
                        ScriptManager.deleteFile(this, file.id)
                        adapter.notifyDataSetChanged()
                        updateEmptyState()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun setupButtons() {
        binding.btnImport.setOnClickListener {
            filePicker.launch("text/plain")
        }

        binding.btnEnableKeyboard.setOnClickListener {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }
    }

    private fun importFile(uri: Uri) {
        val name = getFileName(uri) ?: "Script"
        ScriptManager.importFile(this, uri, name)
        adapter.notifyDataSetChanged()
        updateEmptyState()
        Snackbar.make(binding.root, "\"$name\" imported!", Snackbar.LENGTH_SHORT).show()
    }

    private fun getFileName(uri: Uri): String? {
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            return cursor.getString(nameIndex)
        }
        return uri.lastPathSegment
    }

    private fun updateEmptyState() {
        val isEmpty = ScriptManager.files.isEmpty()
        binding.emptyState.visibility = if (isEmpty) android.view.View.VISIBLE else android.view.View.GONE
        binding.recyclerView.visibility = if (isEmpty) android.view.View.GONE else android.view.View.VISIBLE
    }
}
