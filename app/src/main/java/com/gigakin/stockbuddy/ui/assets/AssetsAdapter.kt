package com.gigakin.stockbuddy.ui.assets

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.gigakin.stockbuddy.data.db.entity.FieldDefinitionEntity
import com.gigakin.stockbuddy.data.db.entity.LinkedItemEntity
import com.gigakin.stockbuddy.databinding.ItemAssetBinding
import com.gigakin.stockbuddy.util.JsonAttributes

/** FR-62: item cards show fixed fields + dynamically rendered domain-specific fields based on showOnAssets. */
class AssetsAdapter(
    private val fieldDefs: List<FieldDefinitionEntity> = emptyList(),
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
        holder.b.tvCategory.text = item.category

        // Render dynamic custom fields
        renderCustomFields(holder.b.dynamicFieldsContainer, item.attributesJson)
    }

    private fun renderCustomFields(container: LinearLayout, attributesJson: String) {
        container.removeAllViews()

        val attributes = try {
            JsonAttributes.toMap(attributesJson)
        } catch (e: Exception) {
            emptyMap()
        }

        val fieldsToShow = fieldDefs.filter { it.showOnAssets }

        fieldsToShow.forEach { fieldDef ->
            val value = attributes[fieldDef.key] ?: "—"

            val row = LinearLayout(container.context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 4, 0, 4)
                }
                orientation = LinearLayout.HORIZONTAL
            }

            // Label (with ellipsis for long names)
            val labelView = TextView(container.context).apply {
                layoutParams = LinearLayout.LayoutParams(80, LinearLayout.LayoutParams.WRAP_CONTENT)
                text = fieldDef.label
                textSize = 10f
                setTypeface(null, android.graphics.Typeface.BOLD)
                isAllCaps = true
                setTextColor(container.context.getColor(com.gigakin.stockbuddy.R.color.md_theme_onSurfaceVariant))
                letterSpacing = 0.05f
                ellipsize = android.text.TextUtils.TruncateAt.END
                maxLines = 1
            }

            // Value
            val valueView = TextView(container.context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
                text = value
                textSize = 14f
                setTextColor(container.context.getColor(com.gigakin.stockbuddy.R.color.md_theme_onSurfaceVariant))
            }

            row.addView(labelView)
            row.addView(valueView)
            container.addView(row)
        }
    }

    override fun getItemCount() = items.size
}
