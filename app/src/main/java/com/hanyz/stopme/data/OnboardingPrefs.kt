package com.hanyz.stopme.data

import android.content.Context

// Menyimpan apakah layar izin awal (onboarding) sudah pernah diselesaikan
object OnboardingPrefs {
    private const val PREFS = "stopme_prefs"
    private const val KEY_PERMISSION_DONE = "permission_onboarding_done"

    fun isPermissionOnboardingDone(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_PERMISSION_DONE, false)

    fun setPermissionOnboardingDone(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_PERMISSION_DONE, true)
            .apply()
    }
}
