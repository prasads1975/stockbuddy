package com.gigakin.stockbuddy.ui.inventory

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gigakin.stockbuddy.data.db.entity.InventorySessionEntity
import com.gigakin.stockbuddy.databinding.ItemSessionBinding
import java.text.SimpleDateFormat
import java.util.*

/** FR-30/31: past sessions, reverse chronological (handled by the DAO's ORDER BY). */
class SessionListAdapter(
    private val onClick: (InventorySessionEntity) -> Unit
) : RecyclerView.Adapter<SessionListAdapter.VH>() {

    private var items: List<InventorySessionEntity> = emptyList()
    fun submitList(list: List<InventorySessionEntity>) { items = list; notifyDataSetChanged() }

    inner class VH(val binding: ItemSessionBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemSessionBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val s = items[position]
        holder.binding.tvCode.text = s.code
        holder.binding.tvTimestamp.text = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(s.startedAt))
        holder.binding.root.setOnClickListener { onClick(s) }
    }

    override fun getItemCount() = items.size
}
