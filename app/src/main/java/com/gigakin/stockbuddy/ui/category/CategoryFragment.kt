package com.gigakin.stockbuddy.ui.category

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.gigakin.stockbuddy.R
import com.gigakin.stockbuddy.StockBuddyApp
import com.gigakin.stockbuddy.databinding.FragmentCategoryBinding
import com.gigakin.stockbuddy.util.LimitCheck
import com.gigakin.stockbuddy.util.ViewModelFactory

/** S17 — Category Management (FR-72/73), with the demo cap (Section 4, MVP Scope doc). */
class CategoryFragment : Fragment() {
    private var _binding: FragmentCategoryBinding? = null
    private val binding get() = _binding!!
    private val app get() = requireActivity().application as StockBuddyApp

    private val viewModel: CategoryViewModel by viewModels {
        ViewModelFactory { CategoryViewModel(app.categoryRepository) }
    }
    private lateinit var adapter: CategoryAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCategoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = CategoryAdapter { cat -> viewModel.delete(cat) }
        binding.recyclerCategories.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerCategories.adapter = adapter

        viewModel.categories.observe(viewLifecycleOwner) { adapter.submitList(it) }

        binding.btnAdd.setOnClickListener {
            val name = binding.editNewCategory.text?.toString()?.trim().orEmpty()
            if (name.isNotBlank()) viewModel.add(name)
        }

        viewModel.addResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is LimitCheck.Exceeded -> Snackbar.make(binding.root, result.message, Snackbar.LENGTH_LONG).show()
                is LimitCheck.Ok -> binding.editNewCategory.text?.clear()
                null -> {}
            }
            viewModel.consumeAddResult()
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
