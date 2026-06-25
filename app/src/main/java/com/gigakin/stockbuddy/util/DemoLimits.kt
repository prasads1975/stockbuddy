package com.gigakin.stockbuddy.util

import com.gigakin.stockbuddy.BuildConfig

/**
 * Section 4 (MVP/Demo Scope doc): demo-build-only caps, sourced from BuildConfig so they're
 * trivial to raise or strip out before a real customer pilot. NOT part of the production SRS.
 */
object DemoLimits {
    const val MAX_ITEMS = BuildConfig.DEMO_MAX_ITEMS
    const val MAX_CATEGORIES = BuildConfig.DEMO_MAX_CATEGORIES
    const val MAX_SESSIONS = BuildConfig.DEMO_MAX_SESSIONS
}
