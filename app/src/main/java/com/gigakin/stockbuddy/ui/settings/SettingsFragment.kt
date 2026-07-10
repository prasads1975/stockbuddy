package com.gigakin.stockbuddy.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.gigakin.stockbuddy.BuildConfig
import com.gigakin.stockbuddy.R
import com.gigakin.stockbuddy.StockBuddyApp
import com.gigakin.stockbuddy.databinding.FragmentSettingsBinding
import com.gigakin.stockbuddy.util.ReaderStatus

/**
 * S16 — Settings: minimal shell. Two functional destinations (Field Configuration, Category
 * Management) plus a read-only Application Details section (App Info + a static License Status
 * placeholder for visual parity with the design). No login, role gating, backup, admin
 * password, or operator PIN — and the License card carries NO licensing logic (out of scope).
 */
class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val app get() = requireActivity().application as StockBuddyApp

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Back button navigation to Home
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // Reader status observer
        app.scannerManager.status.observe(viewLifecycleOwner) { status ->
            val (colorRes, textRes) = when (status) {
                ReaderStatus.CONNECTED -> R.color.status_available to R.string.reader_connected
                ReaderStatus.NOT_CONNECTED -> R.color.status_excess to R.string.reader_not_connected
                ReaderStatus.NOT_AVAILABLE -> R.color.reader_not_available to R.string.reader_not_available
            }
            ImageViewCompat.setImageTintList(
                binding.readerStatusIcon,
                ResourcesCompat.getColorStateList(requireContext().resources, colorRes, null)
            )
            binding.tvReaderStatus.text = getString(textRes)
        }

        // Card click listeners
        binding.cardFieldConfig.setOnClickListener {
            findNavController().navigate(SettingsFragmentDirections.actionSettingsToFieldConfig())
        }
        binding.cardCategories.setOnClickListener {
            findNavController().navigate(SettingsFragmentDirections.actionSettingsToCategories())
        }

        // App info display
        binding.tvAppInfo.text = "Version ${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})"
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
