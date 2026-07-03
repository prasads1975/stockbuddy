package com.gigakin.stockbuddy.ui.category

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gigakin.stockbuddy.data.db.entity.CategoryEntity
import com.gigakin.stockbuddy.databinding.ItemCategoryBinding

class CategoryAdapter(
    private val onEdit: (CategoryEntity) -> Unit = {},
    private val onDelete: (CategoryEntity) -> Unit = {}
) : RecyclerView.Adapter<CategoryAdapter.VH>() {
    private var items: List<CategoryEntity> = emptyList()
    private var filteredItems: List<CategoryEntity> = emptyList()

    fun submitList(list: List<CategoryEntity>) {
        items = list
        filteredItems = list
        notifyDataSetChanged()
    }

    fun filter(query: String) {
        filteredItems = if (query.isEmpty()) {
            items
        } else {
            items.filter { it.name.contains(query, ignoreCase = true) }
        }
        notifyDataSetChanged()
    }

    inner class VH(val b: ItemCategoryBinding) : RecyclerView.ViewHolder(b.root) {
        init {
            b.btnEdit.setOnClickListener { onEdit(filteredItems[adapterPosition]) }
            b.btnDelete.setOnClickListener { onDelete(filteredItems[adapterPosition]) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val cat = filteredItems[position]
        holder.b.tvName.text = cat.name
    }

    override fun getItemCount() = filteredItems.size
}
