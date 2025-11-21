package com.applications.player.data

import android.content.Context
import android.content.SharedPreferences
import com.applications.player.domain.SettingsRepository
import com.applications.player.presentation.settings.SettingsScreenState
import com.google.gson.Gson
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

private const val PREFERENCES_NAME = "settings_preferences"
private const val KEY_SETTINGS = "settings_state"

class SettingsRepositoryImpl(context: Context) : SettingsRepository {

    private val preferences: SharedPreferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    override fun getSettings(): Flow<SettingsScreenState> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_SETTINGS) {
                trySend(loadSettings())
            }
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        trySend(loadSettings())
        awaitClose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    // This function seems to be unused, but it's good practice to have it.
    // If you intend to save the whole state at once, you can use it.
    /*
    override suspend fun saveSettings(settings: SettingsScreenState) {
        val json = gson.toJson(settings)
        preferences.edit().putString(KEY_SETTINGS, json).apply()
    }
    */

    // A private save function is better to avoid confusion with update methods.
    private fun saveSettings(settings: SettingsScreenState) {
        val json = gson.toJson(settings)
        preferences.edit().putString(KEY_SETTINGS, json).apply()
    }

    private fun loadSettings(): SettingsScreenState {
        val json = preferences.getString(KEY_SETTINGS, null)
        return if (json != null) {
            gson.fromJson(json, SettingsScreenState::class.java)
        } else {
            SettingsScreenState() // Default settings
        }
    }

    // This is a placeholder implementation.
    override suspend fun updateAppLanguage(language: String) {
        val currentSettings = loadSettings()
        saveSettings(currentSettings.copy(appLanguage = language))
    }

    override suspend fun updateDefaultScreenOrientation(orientation: String) {
        val currentSettings = loadSettings()
        saveSettings(currentSettings.copy(defaultScreenOrientation = orientation))
    }

    override suspend fun updateDecoder(decoder: String) {
        val currentSettings = loadSettings()
        saveSettings(currentSettings.copy(decoder = decoder))
    }

    override suspend fun updateShowNoMediaFiles(show: Boolean) {
        val currentSettings = loadSettings()
        saveSettings(currentSettings.copy(showNoMediaFiles = show))
    }

    override suspend fun updateShowHiddenFiles(show: Boolean) {
        val currentSettings = loadSettings()
        saveSettings(currentSettings.copy(showHiddenFiles = show))
    }

    override suspend fun updateRememberAspectRatio(remember: Boolean) {
        val currentSettings = loadSettings()
        saveSettings(currentSettings.copy(rememberAspectRatio = remember))
    }

    override suspend fun updateLongPressToPlayAt2xSpeed(enable: Boolean) {
        val currentSettings = loadSettings()
        saveSettings(currentSettings.copy(longPressToPlayAt2xSpeed = enable))
    }

    override suspend fun updateRememberBackgroundPlay(remember: Boolean) {
        val currentSettings = loadSettings()
        saveSettings(currentSettings.copy(rememberBackgroundPlay = remember))
    }

    override suspend fun updateRememberBrightness(remember: Boolean) {
        val currentSettings = loadSettings()
        saveSettings(currentSettings.copy(rememberBrightness = remember))
    }

    override suspend fun updateDoubleTapToFastForwardAndRewind(enable: Boolean) {
        val currentSettings = loadSettings()
        saveSettings(currentSettings.copy(doubleTapToFastForwardAndRewind = enable))
    }

    override suspend fun updateAutoPlayNext(enable: Boolean) {
        val currentSettings = loadSettings()
        saveSettings(currentSettings.copy(autoPlayNext = enable))
    }

    override suspend fun updateShowMusic(show: Boolean) {
        val currentSettings = loadSettings()
        saveSettings(currentSettings.copy(showMusic = show))
    }

    override suspend fun updateBrightness(brightness: Int) {
        val currentSettings = loadSettings()
        saveSettings(currentSettings.copy(brightness = brightness))

    }


}
