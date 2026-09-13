package com.example.noisewatch.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class OnboardingPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("noisewatch_prefs", Context.MODE_PRIVATE)

    fun isOnboardingCompleted(): Boolean {
        return prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit { putBoolean(KEY_ONBOARDING_COMPLETED, completed) }
    }

    companion object {
        private const val KEY_ONBOARDING_COMPLETED = "key_onboarding_completed"
    }
}
