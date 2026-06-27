package com.gigakin.stockbuddy.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.gigakin.stockbuddy.databinding.FragmentHomeBinding
import com.gigakin.stockbuddy.ui.inventory.InventoryCodeDialogFragment

/** S03 Home — hub for the 4 top-level modules (Section 3, SRS). */
class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Featured Inventory Card (primary action) — show inventory code entry dialog
        binding.cardInventoryFeatured.setOnClickListener {
            InventoryCodeDialogFragment().show(parentFragmentManager, "inventory_code")
        }

        // Secondary tiles
        binding.tileLinking.setOnClickListener {
            findNavController().navigate(HomeFragmentDirections.actionHomeToLinkingOptions())
        }
        binding.tileAssets.setOnClickListener {
            findNavController().navigate(HomeFragmentDirections.actionHomeToAssets())
        }
        binding.tileReports.setOnClickListener {
            findNavController().navigate(HomeFragmentDirections.actionHomeToReportsList())
        }
        binding.tileSettings.setOnClickListener {
            findNavController().navigate(HomeFragmentDirections.actionHomeToSettings())
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
