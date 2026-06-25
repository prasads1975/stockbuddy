package com.gigakin.stockbuddy.ui.inventory

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.gigakin.stockbuddy.StockBuddyApp
import com.gigakin.stockbuddy.databinding.FragmentReportsListBinding

/** S15 — Inventory Reports List (FR-30/31). */
class ReportsListFragment : Fragment() {
    private var _binding: FragmentReportsListBinding? = null
    private val binding get() = _binding!!
    private val app get() = requireActivity().application as StockBuddyApp
    private lateinit var adapter: SessionListAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentReportsListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = SessionListAdapter { session ->
            findNavController().navigate(
                ReportsListFragmentDirections.actionReportsToResults(session.id, session.code)
            )
        }
        binding.recyclerSessions.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerSessions.adapter = adapter

        app.inventoryRepository.observeSessions().observe(viewLifecycleOwner) { sessions ->
            adapter.submitList(sessions)
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
