package com.bramble.app.data

import android.content.Context
import com.bramble.app.R

/** Saves the raccoon's name and pronouns, falling back to the defaults from strings.xml. */
class ProfileRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences("profile", Context.MODE_PRIVATE)

    fun name(): String = prefs.getString(KEY_NAME, null) ?: context.getString(R.string.raccoon_name)

    fun pronouns(): String = prefs.getString(KEY_PRONOUNS, null) ?: context.getString(R.string.raccoon_pronouns)

    fun save(name: String, pronouns: String) {
        prefs.edit().putString(KEY_NAME, name).putString(KEY_PRONOUNS, pronouns).apply()
    }

    private companion object {
        const val KEY_NAME = "name"
        const val KEY_PRONOUNS = "pronouns"
    }
}
