package com.gigakin.stockbuddy.ui.assets

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gigakin.stockbuddy.data.db.entity.ItemEntity
import com.gigakin.stockbuddy.databinding.ItemAssetBinding

/** FR-62: item cards show fixed fields + Article ID (if enabled) + domain-specific fields. */
class AssetsAdapter(private val articleIdEnabled: Boolean) : RecyclerView.Adapter<AssetsAdapter.VH>() {
    private var items: List<ItemEntity> = emptyList()
    fun submitList(list: List<ItemEntity>) { items = list; notifyDataSetChanged() }

    inner class VH(val b: ItemAssetBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemAssetBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.b.tvName.text = item.name
        holder.b.tvBarcode.text = "Barcode: ${item.barcode}"
        holder.b.tvRfid.text = "RFID: ${item.rfidTagId}"
        holder.b.tvArticleId.text = if (articleIdEnabled && !item.articleId.isNullOrBlank()) "Article ID: ${item.articleId}" else ""
        holder.b.tvArticleId.visibility = if (holder.b.tvArticleId.text.isBlank()) android.view.View.GONE else android.view.View.VISIBLE
    }

    override fun getItemCount() = items.size
}
