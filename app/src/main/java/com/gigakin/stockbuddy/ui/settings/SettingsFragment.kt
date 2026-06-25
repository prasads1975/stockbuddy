package com.gigakin.stockbuddy.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.gigakin.stockbuddy.BuildConfig
import com.gigakin.stockbuddy.databinding.FragmentSettingsBinding

/**
 * S16 — Settings: minimal shell, exactly two destinations (Section 1.9, MVP Scope doc).
 * No login, no role gating, no License/Backup/Admin Password/Operator PIN items.
 */
class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.tileCategories.setOnClickListener {
            findNavController().navigate(SettingsFragmentDirections.actionSettingsToCategories())
        }
        binding.tileFieldConfig.setOnClickListener {
            findNavController().navigate(SettingsFragmentDirections.actionSettingsToFieldConfig())
        }
        binding.tvAppInfo.text = "StockBuddy ${BuildConfig.VERSION_NAME} (${BuildConfig.APPLICATION_ID})\n" +
            "Demo limits: ${com.gigakin.stockbuddy.util.DemoLimits.MAX_ITEMS} items / " +
            "${com.gigakin.stockbuddy.util.DemoLimits.MAX_CATEGORIES} categories / " +
            "${com.gigakin.stockbuddy.util.DemoLimits.MAX_SESSIONS} sessions"
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
