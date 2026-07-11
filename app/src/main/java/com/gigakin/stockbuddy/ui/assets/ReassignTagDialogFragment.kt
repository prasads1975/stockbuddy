package com.gigakin.stockbuddy.ui.assets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.gigakin.stockbuddy.R
import com.gigakin.stockbuddy.StockBuddyApp
import com.gigakin.stockbuddy.data.db.dao.ProductSummary
import com.gigakin.stockbuddy.data.repo.ItemRepository
import com.gigakin.stockbuddy.databinding.DialogReassignTagBinding
import com.gigakin.stockbuddy.util.ViewModelFactory

/** Assets screen (S09): move a single RFID tag to a different product (edit icon on the expanded tag row). */
class ReassignTagDialogFragment : BottomSheetDialogFragment() {
    private var _binding: DialogReassignTagBinding? = null
    private val binding get() = _binding!!
    private val app get() = requireActivity().application as StockBuddyApp

    private val viewModel: AssetsViewModel by viewModels {
        ViewModelFactory { AssetsViewModel(app.productRepository, app.itemRepository, app.categoryRepository) }
    }

    private var products: List<ProductSummary> = emptyList()

    companion object {
        private const val ARG_RFID = "rfid"
        private const val ARG_CURRENT_BARCODE = "current_barcode"
        fun newInstance(rfid: String, currentBarcode: String) = ReassignTagDialogFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_RFID, rfid)
                putString(ARG_CURRENT_BARCODE, currentBarcode)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogReassignTagBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val rfid = arguments?.getString(ARG_RFID).orEmpty()
        val currentBarcode = arguments?.getString(ARG_CURRENT_BARCODE).orEmpty()
        binding.editTagRfid.setText(rfid)

        app.productRepository.search("", null).observe(viewLifecycleOwner) { list ->
            products = list
            binding.editTargetProduct.setAdapter(buildProductAdapter())
        }
        // Searchable exposed dropdown: typing filters (see buildProductAdapter), tapping/focusing
        // shows the full list, same as Material's standard "filterable exposed dropdown" pattern.
        binding.editTargetProduct.threshold = 1
        binding.editTargetProduct.setOnClickListener { binding.editTargetProduct.showDropDown() }
        binding.editTargetProduct.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.editTargetProduct.showDropDown()
        }

        binding.btnCloseDialog.setOnClickListener { dismiss() }
        binding.btnCancelDialog.setOnClickListener { dismiss() }

        binding.btnSaveReassign.setOnClickListener {
            binding.layoutTargetProduct.error = null
            val selectedLabel = binding.editTargetProduct.text?.toString().orEmpty()
            val target = products.firstOrNull { productLabel(it) == selectedLabel }
            if (target == null) {
                binding.layoutTargetProduct.error = getString(R.string.reassign_tag_product_not_found)
                return@setOnClickListener
            }
            viewModel.reassignTag(rfid, target.barcode) { result ->
                when (result) {
                    ItemRepository.ReassignResult.Success -> {
                        dismiss()
                        showReassignSuccess()
                    }
                    ItemRepository.ReassignResult.NoChange -> dismiss()
                    ItemRepository.ReassignResult.ProductNotFound ->
                        binding.layoutTargetProduct.error = getString(R.string.reassign_tag_product_not_found)
                    ItemRepository.ReassignResult.TagNotFound -> {
                        dismiss()
                        android.widget.Toast.makeText(requireContext(), R.string.reassign_tag_not_found, android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun productLabel(p: ProductSummary) = "${p.productName} (${p.barcode})"

    /**
     * Searchable product picker: default ArrayAdapter filtering only matches a "starts with" on
     * the full "Name (barcode)" label, so typing a barcode (which sits mid-string, in parens)
     * would never match. This overrides the filter to match a substring of either the product
     * name or the barcode, consistent with the Assets list's own search behavior.
     */
    private fun buildProductAdapter(): ArrayAdapter<String> {
        return object : ArrayAdapter<String>(requireContext(), android.R.layout.simple_list_item_1, products.map { productLabel(it) }) {
            override fun getFilter(): android.widget.Filter = object : android.widget.Filter() {
                override fun performFiltering(constraint: CharSequence?): FilterResults {
                    val query = constraint?.toString()?.trim().orEmpty()
                    val matches = if (query.isEmpty()) {
                        products
                    } else {
                        products.filter {
                            it.productName.contains(query, ignoreCase = true) || it.barcode.contains(query, ignoreCase = true)
                        }
                    }.map { productLabel(it) }
                    return FilterResults().apply { values = matches; count = matches.size }
                }

                @Suppress("UNCHECKED_CAST")
                override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                    clear()
                    (results?.values as? List<String>)?.let { addAll(it) }
                    notifyDataSetChanged()
                }

                override fun convertResultToString(resultValue: Any?): CharSequence = resultValue as? String ?: ""
            }
        }
    }

    /** Centered, auto-dismissing confirmation — same pattern as Individual Linking's "Item linked successfully". */
    private fun showReassignSuccess() {
        val activity = activity ?: return
        val dialog = android.app.Dialog(activity)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.view_success_overlay)
        dialog.setCancelable(true)
        dialog.window?.apply {
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
            setDimAmount(0f)
        }
        dialog.findViewById<android.widget.TextView>(R.id.tvSuccessMessage)?.setText(R.string.reassign_tag_success)
        dialog.show()
        activity.findViewById<View>(android.R.id.content)?.postDelayed({
            if (dialog.isShowing) dialog.dismiss()
        }, 1500)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
