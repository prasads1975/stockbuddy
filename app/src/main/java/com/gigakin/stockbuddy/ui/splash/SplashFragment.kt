package com.gigakin.stockbuddy.ui.splash

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.gigakin.stockbuddy.R
import com.gigakin.stockbuddy.databinding.FragmentSplashBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Branded splash screen — the nav-graph start destination (replaces the default Android splash).
 * Shows the StockBuddy identity + a short progress animation, then navigates to Home and removes
 * itself from the back stack (popUpTo splash, inclusive). Matches docs/html_screens/splash_screen.
 */
class SplashFragment : Fragment() {
    private var _binding: FragmentSplashBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSplashBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Gentle entrance fade-in.
        binding.root.alpha = 0f
        binding.root.animate().alpha(1f).setDuration(400L).start()

        // Animate the progress bar over ~1.8s, then advance to Home.
        // viewLifecycleOwner scope auto-cancels on view destroy (rotation / early exit).
        viewLifecycleOwner.lifecycleScope.launch {
            val steps = 50
            val totalMs = 1800L
            for (i in 0..steps) {
                val pct = i * 100 / steps
                binding.progressBar.progress = pct
                binding.tvPercent.text = getString(R.string.splash_percent, pct)
                delay(totalMs / steps)
            }
            findNavController().navigate(R.id.action_splash_to_home)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
