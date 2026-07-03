package com.gigakin.stockbuddy.ui.assets

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gigakin.stockbuddy.data.db.entity.LinkedItemEntity
import com.gigakin.stockbuddy.databinding.ItemAssetBinding

/** FR-62: item cards show fixed fields (including mandatory Article ID) + domain-specific fields. */
class AssetsAdapter(
    private val onEditClick: (LinkedItemEntity) -> Unit = {},
    private val onDeleteClick: (LinkedItemEntity) -> Unit = {}
) : RecyclerView.Adapter<AssetsAdapter.VH>() {
    private var items: List<LinkedItemEntity> = emptyList()
    fun submitList(list: List<LinkedItemEntity>) { items = list; notifyDataSetChanged() }

    inner class VH(val b: ItemAssetBinding) : RecyclerView.ViewHolder(b.root) {
        init {
            b.btnEdit.setOnClickListener { onEditClick(items[adapterPosition]) }
            b.btnDelete.setOnClickListener { onDeleteClick(items[adapterPosition]) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemAssetBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.b.tvName.text = item.productName
        holder.b.tvBarcode.text = item.barcode
        holder.b.tvCategory.text = item.categoryName
        holder.b.tvPrice.text = item.attributesJson?.let { extractPrice(it) } ?: "—"
    }

    private fun extractPrice(attributesJson: String): String {
        return try {
            val map = com.gigakin.stockbuddy.util.JsonAttributes.fromMap(attributesJson)
            map["Price"]?.toString() ?: "—"
        } catch (e: Exception) {
            "—"
        }
    }

    override fun getItemCount() = items.size
}
