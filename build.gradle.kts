// Root build file.
// Plugins are declared here with `apply false` so the version is fixed
// once for the whole build; each module then applies the ones it needs.

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
}
