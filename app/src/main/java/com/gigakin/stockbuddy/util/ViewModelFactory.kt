package com.gigakin.stockbuddy.util

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/** Generic factory for manually-wired ViewModels (NFR-27, no DI framework — see StockBuddyApp). */
class ViewModelFactory(private val creator: () -> ViewModel) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = creator() as T
}
