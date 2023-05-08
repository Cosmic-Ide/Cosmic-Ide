package org.cosmicide.rewrite.common

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

/**
 * A utility object to access shared preferences easily.
 */
object Prefs {
    private lateinit var prefs: SharedPreferences

    /**
     * Initializes shared preferences.
     * @param context The context of the application.
     */
    fun init(context: Context) {
        prefs = PreferenceManager.getDefaultSharedPreferences(context)
    }

    val useFastJarFs: Boolean 
        get() = prefs.getBoolean("use_fastjarfs", false)
    val useSpaces: Boolean
        get() = prefs.getBoolean("use_spaces", false)
    val tabSize: Int
        get() = prefs.getInt("tab_size", 4)
    val compilerJavaVersion: Int
        get() = prefs.getInt("java_version", 17)
    val useSSVM: Boolean
        get() = prefs.getBoolean("use_ssvm", false)
    val editorFontSize: Float
        get() {
            return runCatching {
                val fontSizeString = prefs.getString("font_size", "16") ?: "16"
                fontSizeString.toFloat().coerceIn(1f, 32f)
            }.getOrDefault(16f)
        }
}