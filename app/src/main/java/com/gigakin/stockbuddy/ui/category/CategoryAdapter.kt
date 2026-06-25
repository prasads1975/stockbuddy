package com.gigakin.stockbuddy.ui.category

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gigakin.stockbuddy.data.db.entity.CategoryEntity
import com.gigakin.stockbuddy.databinding.ItemCategoryBinding

class CategoryAdapter(private val onDelete: (CategoryEntity) -> Unit) : RecyclerView.Adapter<CategoryAdapter.VH>() {
    private var items: List<CategoryEntity> = emptyList()
    fun submitList(list: List<CategoryEntity>) { items = list; notifyDataSetChanged() }

    inner class VH(val b: ItemCategoryBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val cat = items[position]
        holder.b.tvName.text = cat.name
        holder.b.btnDelete.setOnClickListener { onDelete(cat) }
    }

    override fun getItemCount() = items.size
}
