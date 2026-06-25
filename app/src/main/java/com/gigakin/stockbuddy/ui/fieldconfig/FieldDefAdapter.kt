package com.gigakin.stockbuddy.ui.fieldconfig

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gigakin.stockbuddy.data.db.entity.FieldDefinitionEntity
import com.gigakin.stockbuddy.databinding.ItemFieldDefBinding

class FieldDefAdapter(private val onDelete: (FieldDefinitionEntity) -> Unit) : RecyclerView.Adapter<FieldDefAdapter.VH>() {
    private var items: List<FieldDefinitionEntity> = emptyList()
    fun submitList(list: List<FieldDefinitionEntity>) { items = list; notifyDataSetChanged() }

    inner class VH(val b: ItemFieldDefBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemFieldDefBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val f = items[position]
        holder.b.tvLabel.text = f.label + if (f.mandatory) " *" else ""
        holder.b.tvType.text = f.type
        holder.b.btnDelete.setOnClickListener { onDelete(f) }
    }

    override fun getItemCount() = items.size
}
