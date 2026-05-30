package com.pasteboard.keyboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ScriptAdapter(
    private val getFiles: () -> List<ScriptFile>,
    private val activeId: () -> String?,
    private val onSetActive: (ScriptFile) -> Unit,
    private val onDelete: (ScriptFile) -> Unit
) : RecyclerView.Adapter<ScriptAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.tvName)
        val lines: TextView = view.findViewById(R.id.tvLines)
        val preview: TextView = view.findViewById(R.id.tvPreview)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
        val activeIndicator: View = view.findViewById(R.id.activeIndicator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_script, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val file = getFiles()[position]
        val isActive = file.id == activeId()

        holder.name.text = file.name
        holder.lines.text = "${file.lines.size} lines"
        holder.preview.text = file.lines.firstOrNull() ?: ""
        holder.activeIndicator.visibility = if (isActive) View.VISIBLE else View.INVISIBLE

        holder.itemView.setOnClickListener { onSetActive(file) }
        holder.btnDelete.setOnClickListener { onDelete(file) }

        holder.itemView.alpha = if (isActive) 1f else 0.75f
    }

    override fun getItemCount() = getFiles().size
}
