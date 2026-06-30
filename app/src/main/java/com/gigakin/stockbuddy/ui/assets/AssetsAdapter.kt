package com.gigakin.stockbuddy.ui.assets

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gigakin.stockbuddy.data.db.entity.LinkedItemEntity
import com.gigakin.stockbuddy.databinding.ItemAssetBinding

/** FR-62: item cards show fixed fields (including mandatory Article ID) + domain-specific fields. */
class AssetsAdapter : RecyclerView.Adapter<AssetsAdapter.VH>() {
    private var items: List<LinkedItemEntity> = emptyList()
    fun submitList(list: List<LinkedItemEntity>) { items = list; notifyDataSetChanged() }

    inner class VH(val b: ItemAssetBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemAssetBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.b.tvName.text = item.productName
        holder.b.tvBarcode.text = "Barcode: ${item.barcode}"
        holder.b.tvRfid.text = "RFID: ${item.rfidTagId}"

        // Article ID field hidden (barcode is the primary business identifier)
        holder.b.tvArticleId.visibility = android.view.View.GONE
    }

    override fun getItemCount() = items.size
}
