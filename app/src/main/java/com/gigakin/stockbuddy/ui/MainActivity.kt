package com.gigakin.stockbuddy.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import com.gigakin.stockbuddy.R
import com.gigakin.stockbuddy.databinding.ActivityMainBinding

/**
 * Single-Activity + Navigation Component host. Each fragment manages its own header and
 * status bar (FR-81). Navigation between fragments is handled by the Navigation Component.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun onSupportNavigateUp(): Boolean =
        findNavController(R.id.nav_host_fragment).navigateUp() || super.onSupportNavigateUp()
}
