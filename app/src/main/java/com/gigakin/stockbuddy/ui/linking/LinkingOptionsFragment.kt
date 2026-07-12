package com.gigakin.stockbuddy.ui.linking

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.gigakin.stockbuddy.R
import com.gigakin.stockbuddy.StockBuddyApp
import com.gigakin.stockbuddy.databinding.FragmentLinkingOptionsBinding
import com.gigakin.stockbuddy.util.ReaderStatusBar

/**
 * S04 — choice between QR Code Linking (S07, imager), Individual Linking, and Bulk Linking.
 */
class LinkingOptionsFragment : Fragment() {
    private var _binding: FragmentLinkingOptionsBinding? = null
    private val binding get() = _binding!!
    private val app get() = requireActivity().application as StockBuddyApp

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLinkingOptionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Back button navigation
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // QR Code Linking (S07) — imager-based scan screen.
        binding.btnQrLinking.setOnClickListener {
            findNavController().navigate(LinkingOptionsFragmentDirections.actionToQrLinking())
        }

        // Card click listeners
        binding.btnIndividual.setOnClickListener {
            findNavController().navigate(LinkingOptionsFragmentDirections.actionToIndividualLinking())
        }
        binding.btnBulk.setOnClickListener {
            findNavController().navigate(LinkingOptionsFragmentDirections.actionToBulkLinking())
        }

        // Observe reader status for hardware status bar (FR-81) — centralised in ReaderStatusBar.
        app.scannerManager.status.observe(viewLifecycleOwner) { status ->
            ReaderStatusBar.bind(status, binding.readerStatusBar, binding.readerStatusIcon, binding.tvReaderStatus)
        }
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
