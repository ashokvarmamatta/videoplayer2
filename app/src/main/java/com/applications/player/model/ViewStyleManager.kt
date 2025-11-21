// Create this new file in a suitable package, e.g., com.applications.player.util
package com.applications.player.util

import android.content.Context
import android.content.SharedPreferences

// Enum to define the possible view styles
enum class ViewStyle {
    GRID,
    LIST
}

class ViewStyleManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("view_style_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_VIEW_STYLE = "style"
    }

    // Save the selected view style
    fun saveViewStyle(style: ViewStyle) {
        prefs.edit().putString(KEY_VIEW_STYLE, style.name).apply()
    }

    // Retrieve the saved view style, defaulting to GRID
    fun getViewStyle(): ViewStyle {
        val styleName = prefs.getString(KEY_VIEW_STYLE, ViewStyle.GRID.name)
        return ViewStyle.valueOf(styleName ?: ViewStyle.GRID.name)
    }
}
