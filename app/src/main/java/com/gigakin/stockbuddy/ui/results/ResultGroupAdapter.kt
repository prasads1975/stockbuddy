package com.gigakin.stockbuddy.ui.results

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gigakin.stockbuddy.R
import com.gigakin.stockbuddy.data.repo.InventoryRepository
import com.gigakin.stockbuddy.databinding.ItemResultGroupBinding
import com.gigakin.stockbuddy.databinding.ItemResultUnitBinding

/**
 * FR-45: results grouped by Barcode, per-group count, tap to expand and see each unit's
 * RFID Tag ID (and Article ID, where enabled) — RFID Tag ID is always the unit-level
 * identifier, Article ID is shown only as supplementary info per Section 1.7.
 */
class ResultGroupAdapter(
    private val context: Context,
    private val articleIdEnabled: Boolean
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    internal data class Group(val barcode: String, val name: String, val category: String, val units: List<InventoryRepository.ResultItem>)
    private var groups: List<Group> = emptyList()
    private val expanded = mutableSetOf<String>()

    fun submitList(items: List<InventoryRepository.ResultItem>) {
        groups = items.groupBy { it.item.barcode }.map { (barcode, units) ->
            Group(barcode, units.first().item.name, units.first().item.categoryName, units)
        }
        notifyDataSetChanged()
    }

    private sealed class Row { data class Header(val group: Group) : Row(); data class Unit(val item: InventoryRepository.ResultItem) : Row() }
    private fun flatten(): List<Row> = groups.flatMap { g ->
        listOf(Row.Header(g)) + if (g.barcode in expanded) g.units.map { Row.Unit(it) } else emptyList()
    }

    override fun getItemViewType(position: Int) = if (flatten()[position] is Row.Header) 0 else 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        if (viewType == 0) GroupVH(ItemResultGroupBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        else UnitVH(ItemResultUnitBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = flatten()[position]) {
            is Row.Header -> (holder as GroupVH).bind(row.group)
            is Row.Unit -> (holder as UnitVH).bind(row.item)
        }
    }

    override fun getItemCount() = flatten().size

    inner class GroupVH(val b: ItemResultGroupBinding) : RecyclerView.ViewHolder(b.root) {
        internal fun bind(g: Group) {
            b.tvName.text = g.name
            b.tvBarcode.text = "Barcode: ${g.barcode}  •  ${g.category}"
            b.tvCount.text = "${g.units.size}"

            // Set left accent colour based on status (green/red/amber) — faster to scan than text label
            val statusColorId = when (g.units.firstOrNull()?.status) {
                InventoryRepository.Status.AVAILABLE -> com.gigakin.stockbuddy.R.color.status_available
                InventoryRepository.Status.MISSING -> com.gigakin.stockbuddy.R.color.status_missing
                InventoryRepository.Status.EXCESS -> com.gigakin.stockbuddy.R.color.status_excess
                else -> com.gigakin.stockbuddy.R.color.status_available
            }
            b.statusAccent.setBackgroundColor(context.getColor(statusColorId))

            b.root.setOnClickListener {
                if (g.barcode in expanded) expanded.remove(g.barcode) else expanded.add(g.barcode)
                notifyDataSetChanged()
            }
        }
    }

    inner class UnitVH(val b: ItemResultUnitBinding) : RecyclerView.ViewHolder(b.root) {
        internal fun bind(item: InventoryRepository.ResultItem) {
            b.tvRfid.text = "RFID: ${item.item.rfidTagId}"
            b.tvArticleId.text = if (articleIdEnabled && !item.item.articleId.isNullOrBlank()) "Article ID: ${item.item.articleId}" else ""
            b.tvArticleId.visibility = if (b.tvArticleId.text.isBlank()) android.view.View.GONE else android.view.View.VISIBLE
        }
    }
}
