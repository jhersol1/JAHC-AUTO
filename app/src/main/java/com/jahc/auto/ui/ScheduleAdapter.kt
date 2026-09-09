package com.jahc.auto.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jahc.auto.data.Schedule
import com.jahc.auto.databinding.ItemScheduleBinding

class ScheduleAdapter(
    private val onToggle: (Schedule, Boolean) -> Unit,
    private val onEdit: (Schedule) -> Unit,
    private val onDelete: (Schedule) -> Unit
) : ListAdapter<Schedule, ScheduleAdapter.VH>(Diff) {

    object Diff : DiffUtil.ItemCallback<Schedule>() {
        override fun areItemsTheSame(a: Schedule, b: Schedule) = a.id == b.id
        override fun areContentsTheSame(a: Schedule, b: Schedule) = a == b
    }

    inner class VH(val binding: ItemScheduleBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(ItemScheduleBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val s = getItem(position)
        holder.binding.title.text = if (s.title.isNotBlank()) s.title else s.contactName.ifBlank { s.phone }.ifBlank { "Sin título" }
        holder.binding.subtitle.text = "${s.target} • ${s.timeLabel()} • ${if (s.repeatDaily) "Diario" else "Una vez"}"
        holder.binding.message.text = s.message.take(80)
        holder.binding.switchEnabled.isChecked = s.enabled
        holder.binding.switchEnabled.setOnCheckedChangeListener { _, checked -> onToggle(s, checked) }
        holder.binding.btnEdit.setOnClickListener { onEdit(s) }
        holder.binding.btnDelete.setOnClickListener { onDelete(s) }
    }
}
