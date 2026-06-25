package com.gigakin.stockbuddy.ui.linking

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.gigakin.stockbuddy.databinding.FragmentLinkingOptionsBinding

/** S04 — choice between Individual Linking and Bulk Linking. QR mode (FR-01-04) is cut from MVP. */
class LinkingOptionsFragment : Fragment() {
    private var _binding: FragmentLinkingOptionsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLinkingOptionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.btnIndividual.setOnClickListener {
            findNavController().navigate(LinkingOptionsFragmentDirections.actionToIndividualLinking())
        }
        binding.btnBulk.setOnClickListener {
            findNavController().navigate(LinkingOptionsFragmentDirections.actionToBulkLinking())
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
