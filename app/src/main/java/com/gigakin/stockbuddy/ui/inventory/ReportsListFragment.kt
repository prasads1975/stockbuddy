package com.gigakin.stockbuddy.ui.inventory

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.gigakin.stockbuddy.StockBuddyApp
import com.gigakin.stockbuddy.databinding.FragmentReportsListBinding
import com.gigakin.stockbuddy.util.ReaderStatus
import kotlinx.coroutines.launch

private const val TAG = "ReportsListFragment"

/** S15 — Inventory Reports List (FR-30/31). */
class ReportsListFragment : Fragment() {
    private var _binding: FragmentReportsListBinding? = null
    private val binding get() = _binding!!
    private val app get() = requireActivity().application as StockBuddyApp
    private lateinit var adapter: SessionListAdapter
    private var allSessions = emptyList<com.gigakin.stockbuddy.data.db.entity.InventorySessionEntity>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentReportsListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Back button
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        // Reader status bar
        app.scannerManager.status.observe(viewLifecycleOwner) { status ->
            binding.readerStatusIcon.setColorFilter(
                requireContext().getColor(
                    when (status) {
                        ReaderStatus.CONNECTED -> com.gigakin.stockbuddy.R.color.status_available
                        ReaderStatus.NOT_CONNECTED -> android.R.color.holo_orange_light
                        ReaderStatus.NOT_AVAILABLE -> com.gigakin.stockbuddy.R.color.md_theme_onSurfaceVariant
                    }
                )
            )
            binding.tvReaderStatus.text = when (status) {
                ReaderStatus.CONNECTED -> getString(com.gigakin.stockbuddy.R.string.reader_connected)
                ReaderStatus.NOT_CONNECTED -> getString(com.gigakin.stockbuddy.R.string.reader_not_connected)
                ReaderStatus.NOT_AVAILABLE -> getString(com.gigakin.stockbuddy.R.string.reader_not_available)
            }
        }

        // Adapter setup
        adapter = SessionListAdapter { session ->
            findNavController().navigate(
                ReportsListFragmentDirections.actionReportsToResults(session.id, session.code)
            )
        }
        binding.recyclerSessions.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerSessions.adapter = adapter

        // Search functionality
        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterSessions(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Load sessions
        lifecycleScope.launch {
            val sessions = app.inventoryRepository.getAllSessions()
            Log.d(TAG, "DEBUG: Total sessions in database = ${sessions.size}")
        }

        app.inventoryRepository.observeSessions().observe(viewLifecycleOwner) { sessions ->
            Log.d(TAG, "observeSessions callback: received ${sessions.size} sessions")
            allSessions = sessions
            filterSessions(binding.searchInput.text.toString())
            binding.emptyState.visibility = if (sessions.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun filterSessions(query: String) {
        val filtered = if (query.isBlank()) {
            allSessions
        } else {
            allSessions.filter { session ->
                session.code.contains(query, ignoreCase = true)
            }
        }
        adapter.submitList(filtered)
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
