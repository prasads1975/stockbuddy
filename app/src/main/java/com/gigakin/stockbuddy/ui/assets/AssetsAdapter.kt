package com.gigakin.stockbuddy.ui.assets

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.gigakin.stockbuddy.R
import com.gigakin.stockbuddy.data.db.dao.ProductSummary
import com.gigakin.stockbuddy.data.db.entity.FieldDefinitionEntity
import com.gigakin.stockbuddy.data.db.entity.LinkedItemEntity
import com.gigakin.stockbuddy.databinding.ItemAssetBinding
import com.gigakin.stockbuddy.databinding.ItemRfidTagRowBinding
import com.gigakin.stockbuddy.util.JsonAttributes

/**
 * FR-62 (v2, product-level): cards show one product with its barcode, category, linked-unit count,
 * and the domain-specific fields (showOnAssets) from the product's attributes. Each card also has
 * an expandable "RFID Tags (N)" row listing the individual physical units linked to the product,
 * with per-tag edit (reassign to another product) and delete.
 */
class AssetsAdapter(
    private val fieldDefs: List<FieldDefinitionEntity> = emptyList(),
    private val onEditClick: (ProductSummary) -> Unit = {},
    private val onDeleteClick: (ProductSummary) -> Unit = {},
    private val onTagsExpandRequested: (barcode: String) -> Unit = {},
    private val onTagEditClick: (LinkedItemEntity) -> Unit = {},
    private val onTagDeleteClick: (LinkedItemEntity) -> Unit = {}
) : RecyclerView.Adapter<AssetsAdapter.VH>() {
    private var items: List<ProductSummary> = emptyList()
    private val expandedBarcodes = mutableSetOf<String>()
    private val tagsCache = mutableMapOf<String, List<LinkedItemEntity>>()
    private var currentQuery: String = ""
    private var lastAutoExpandQuery: String? = null
    private var recyclerView: RecyclerView? = null

    override fun onAttachedToRecyclerView(rv: RecyclerView) { super.onAttachedToRecyclerView(rv); recyclerView = rv }
    override fun onDetachedFromRecyclerView(rv: RecyclerView) { super.onDetachedFromRecyclerView(rv); recyclerView = null }

    fun submitList(list: List<ProductSummary>, query: String = "") {
        items = list
        currentQuery = query
        // A tag can be deleted/reassigned via a dialog that doesn't share this adapter's cache
        // (ReassignTagDialogFragment owns its own ViewModel instance). Evict any cached tag list
        // whose size no longer matches the live linked_count so an expanded card auto-refetches.
        tagsCache.keys.retainAll { barcode ->
            val current = list.firstOrNull { it.barcode == barcode }
            current != null && tagsCache[barcode]?.size == current.linkedCount
        }

        // Auto-expand cards that matched via RFID, once per distinct query (not on every
        // unrelated data refresh while the same search is still active, and never overriding a
        // user's manual collapse of a card that isn't itself a fresh RFID hit).
        if (query.isBlank()) {
            lastAutoExpandQuery = null
        } else if (query != lastAutoExpandQuery) {
            lastAutoExpandQuery = query
            list.filter { it.matchedByRfid }.forEach { expandedBarcodes.add(it.barcode) }
        }

        notifyDataSetChanged()
    }

    /** Called by the Fragment once tags for a barcode have been loaded, to render the expanded section. */
    fun setTagsForBarcode(barcode: String, tags: List<LinkedItemEntity>) {
        tagsCache[barcode] = tags
        val index = items.indexOfFirst { it.barcode == barcode }
        if (index < 0) return
        // notifyItemChanged() throws if called while RecyclerView is mid layout/scroll (can happen
        // if a caller's load resolves synchronously). Defer in that case instead of crashing.
        val rv = recyclerView
        if (rv != null && rv.isComputingLayout) {
            rv.post { notifyItemChanged(index) }
        } else {
            notifyItemChanged(index)
        }
    }

    inner class VH(val b: ItemAssetBinding) : RecyclerView.ViewHolder(b.root) {
        init {
            b.btnEdit.setOnClickListener { onEditClick(items[adapterPosition]) }
            b.btnDelete.setOnClickListener { onDeleteClick(items[adapterPosition]) }
            b.rowTagsToggle.setOnClickListener {
                val barcode = items[adapterPosition].barcode
                if (barcode in expandedBarcodes) expandedBarcodes.remove(barcode) else expandedBarcodes.add(barcode)
                notifyItemChanged(adapterPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemAssetBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.b.tvName.text = item.productName
        holder.b.tvBarcode.text = "${item.barcode}  •  ${item.linkedCount} unit${if (item.linkedCount == 1) "" else "s"}"
        holder.b.tvCategory.text = item.categoryName

        renderCustomFields(holder.b.dynamicFieldsContainer, item.attributesJson)
        renderTagsSection(holder, item)
    }

    private fun renderTagsSection(holder: VH, item: ProductSummary) {
        val b = holder.b
        val isExpanded = item.barcode in expandedBarcodes
        b.tvTagsToggleLabel.text = b.root.context.getString(R.string.rfid_tags_toggle_format, item.linkedCount)
        b.ivTagsExpandIcon.rotation = if (isExpanded) 180f else 0f
        b.rfidTagsContainer.visibility = if (isExpanded) android.view.View.VISIBLE else android.view.View.GONE

        b.rfidTagsContainer.removeAllViews()
        if (isExpanded) {
            // Defer: this runs inside onBindViewHolder (mid layout-pass). The load callback can
            // resolve synchronously enough to call notifyItemChanged() before layout finishes,
            // which RecyclerView throws on. Posting runs it just after the current pass completes.
            if (item.barcode !in tagsCache) b.root.post { onTagsExpandRequested(item.barcode) }
            val tags = tagsCache[item.barcode].orEmpty()
            tags.forEach { tag ->
                val row = ItemRfidTagRowBinding.inflate(LayoutInflater.from(b.root.context), b.rfidTagsContainer, false)
                row.tvTagRfid.text = tag.rfidTagId
                if (currentQuery.isNotBlank() && tag.rfidTagId.contains(currentQuery, ignoreCase = true)) {
                    row.tvTagRfid.setTextColor(row.root.context.getColor(R.color.md_theme_primary))
                    row.tvTagRfid.setTypeface(row.tvTagRfid.typeface, android.graphics.Typeface.BOLD)
                }
                row.btnTagEdit.setOnClickListener { onTagEditClick(tag) }
                row.btnTagDelete.setOnClickListener { onTagDeleteClick(tag) }
                b.rfidTagsContainer.addView(row.root)
            }
        }
    }

    /** Custom fields render 2-per-row, same label-above-value column style as the Barcode/Category row. */
    private fun renderCustomFields(container: LinearLayout, attributesJson: String) {
        container.removeAllViews()

        val attributes = try {
            JsonAttributes.toMap(attributesJson)
        } catch (e: Exception) {
            emptyMap()
        }

        val fieldsToShow = fieldDefs.filter { it.showOnAssets }

        fieldsToShow.chunked(2).forEach { pair ->
            val row = LinearLayout(container.context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 4, 0, 4)
                }
                orientation = LinearLayout.HORIZONTAL
            }

            pair.forEach { fieldDef ->
                val value = attributes[fieldDef.key] ?: "—"

                val column = LinearLayout(container.context).apply {
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    orientation = LinearLayout.VERTICAL
                }

                val labelView = TextView(container.context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { bottomMargin = 4 }
                    text = fieldDef.label
                    textSize = 10f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    isAllCaps = true
                    setTextColor(container.context.getColor(com.gigakin.stockbuddy.R.color.md_theme_onSurfaceVariant))
                    letterSpacing = 0.05f
                    ellipsize = android.text.TextUtils.TruncateAt.END
                    maxLines = 1
                }

                val valueView = TextView(container.context).apply {
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    text = value
                    textSize = 14f
                    setTextColor(container.context.getColor(com.gigakin.stockbuddy.R.color.md_theme_onSurfaceVariant))
                }

                column.addView(labelView)
                column.addView(valueView)
                row.addView(column)
            }

            // Odd field out: pad with an empty half-width spacer so it doesn't stretch full-width.
            if (pair.size == 1) {
                row.addView(android.view.View(container.context).apply {
                    layoutParams = LinearLayout.LayoutParams(0, 0, 1f)
                })
            }

            container.addView(row)
        }
    }

    override fun getItemCount() = items.size
}
