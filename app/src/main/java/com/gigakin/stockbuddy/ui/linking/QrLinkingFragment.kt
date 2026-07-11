package com.gigakin.stockbuddy.ui.linking

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.gigakin.stockbuddy.R
import com.gigakin.stockbuddy.StockBuddyApp
import com.gigakin.stockbuddy.databinding.FragmentQrLinkingBinding
import com.gigakin.stockbuddy.util.QrPayloadParser
import com.gigakin.stockbuddy.util.ReaderStatus
import com.gigakin.stockbuddy.util.ViewModelFactory

/**
 * S07 — QR Code Linking. Arms the C72 2D imager (no live camera feed); on a successful decode
 * of the agreed JSON payload (FR-02), navigates to Individual Linking with the fields pre-filled.
 * Emulator/no-imager → FR-81a unavailable state (scan disabled).
 */
class QrLinkingFragment : Fragment() {
    private var _binding: FragmentQrLinkingBinding? = null
    private val binding get() = _binding!!
    private val app get() = requireActivity().application as StockBuddyApp

    private val viewModel: QrLinkingViewModel by viewModels {
        ViewModelFactory { QrLinkingViewModel(app.scannerManager) }
    }

    private var scanAnim: ObjectAnimator? = null
    private var navigated = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentQrLinkingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        binding.btnScan.setOnClickListener { viewModel.triggerScan() }

        // Reader status → status bar + FR-81a graceful degradation.
        app.scannerManager.status.observe(viewLifecycleOwner) { status ->
            val (colorRes, textRes) = when (status) {
                ReaderStatus.CONNECTED -> R.color.status_available to R.string.reader_connected
                ReaderStatus.NOT_CONNECTED -> R.color.md_theme_error to R.string.reader_not_connected
                ReaderStatus.NOT_AVAILABLE -> R.color.md_theme_onSurfaceVariant to R.string.reader_not_available
            }
            binding.readerStatusIcon.setColorFilter(requireContext().getColor(colorRes))
            binding.tvReaderStatus.text = getString(textRes)

            val available = status == ReaderStatus.CONNECTED
            binding.btnScan.isEnabled = available
            binding.tvScanState.text = getString(
                if (available) R.string.qr_linking_scanning else R.string.qr_linking_state_unavailable
            )
        }

        viewModel.decoded.observe(viewLifecycleOwner) { raw ->
            if (raw != null) handleDecoded(raw)
        }
    }

    override fun onStart() {
        super.onStart()
        navigated = false
        val ok = viewModel.openImager()
        if (ok) startScanAnimation()
    }

    override fun onStop() {
        super.onStop()
        stopScanAnimation()
        viewModel.closeImager()
    }

    private fun handleDecoded(raw: String) {
        viewModel.consumeDecoded()
        if (navigated) return

        val payload = QrPayloadParser.parse(raw)
        if (payload == null) {
            Snackbar.make(binding.root, R.string.qr_linking_invalid, Snackbar.LENGTH_LONG).show()
            viewModel.triggerScan()  // re-arm for another attempt
            return
        }

        navigated = true
        findNavController().navigate(
            QrLinkingFragmentDirections.actionQrToIndividualLinking(
                fromQr = true,
                prefillProductName = payload.productName,
                prefillBarcode = payload.barcode,
                prefillCategory = payload.category,
                prefillRfid = payload.rfid,
                prefillAttributesJson = QrPayloadParser.attributesToJson(payload.attributes)
            )
        )
    }

    private fun startScanAnimation() {
        val distance = 208f * resources.displayMetrics.density
        scanAnim = ObjectAnimator.ofFloat(binding.scanLine, "translationY", 0f, distance).apply {
            duration = 1600
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    private fun stopScanAnimation() {
        scanAnim?.cancel(); scanAnim = null
        _binding?.scanLine?.translationY = 0f
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
