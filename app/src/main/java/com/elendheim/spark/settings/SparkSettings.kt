package com.elendheim.spark.settings

/**
 * Every user-facing preference in one immutable snapshot.
 *
 * Accessibility is not a bolt-on here -> most of these fields exist to make the
 * app usable for more people: motion can be switched off, contrast raised, tap
 * targets grown, text scaled, and wheel meaning never rests on colour alone.
 *
 * Defaults are chosen so a brand-new install already looks and behaves the way
 * the app is meant to: dark, calm, animated, readable.
 */
data class SparkSettings(
    // Accessibility
    val textScale: Float = 1.0f,             // extra multiplier on top of the system font size
    val highContrast: Boolean = false,       // brighter text, firmer borders
    val reduceMotion: Boolean = false,       // skip the collide animation for an instant reveal
    val colorblindPalette: Boolean = false,  // alternate, higher-separation wheel colours
    val showWheelLabels: Boolean = true,     // show each wheel's name, so colour is never the only cue
    val largerTapTargets: Boolean = false,   // grow buttons and toggles for motor accessibility
    val haptics: Boolean = true,             // vibrate on collide / save
    val announceResult: Boolean = true,      // speak the new idea for screen readers
    val lineByLineResult: Boolean = false,   // stack picks one per line instead of A x B x C

    // Content & behaviour
    val weightingEnabled: Boolean = false,   // honour per-entry weights globally
    val defaultDeckId: String? = null,       // which deck opens on launch (null = first deck)
    val mixLimit: Int = 0                    // how many wheels to mix at once; 0 = all of them
)
