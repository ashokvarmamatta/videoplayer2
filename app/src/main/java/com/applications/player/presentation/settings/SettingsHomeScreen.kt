package com.applications.player.presentation.settings

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.applications.player.R
import com.applications.player.presentation.basic.LanguagesActivity
import org.koin.androidx.compose.koinViewModel

// A map to get language name from code, similar to the one in LanguagesActivity
private val languageMap = mapOf(
    "en" to "English",
    "ar" to "العربية",
    "bg" to "български",
    "bn" to "বাংলা",
    "ca" to "Català",
    "cs" to "Čeština",
    "da" to "Dansk",
    "de" to "Deutsch",
    "el" to "Ελληνικά",
    "es" to "Español",
    "fi" to "Suomi",
    "fr" to "Français",
    "hi" to "हिन्दी",
    "hr" to "Hrvatski",
    "hu" to "Magyar",
    "in" to "Bahasa Indonesia",
    "it" to "Italiano",
    "iw" to "עברית",
    "ja" to "日本語",
    "ko" to "한국어",
    "lt" to "Lietuvių",
    "lv" to "Latviešu",
    "nb" to "Norwegian",
    "nl" to "Nederlands",
    "pl" to "Polski",
    "pt" to "Português",
    "ro" to "Română",
    "ru" to "Русский",
    "sk" to "Slovenčina",
    "sl" to "Slovenščina",
    "sr" to "Српски",
    "sv" to "Svenska",
    "ta" to "தமிழ்",
    "te" to "తెలుగు",
    "th" to "ไทย",
    "tr" to "Türkçe",
    "ur" to "اردو",
    "uk" to "Українська",
    "vi" to "Tiếng Việt",
    "zh-rCN" to "简体中文",
    "zh-rTW" to "繁體中文"
)


/**
 * Main composable for the Settings screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsHomeScreen(viewModel: SettingsViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val sp = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
    val languageCode = sp.getString("language", "en") ?: "en"
    val selectedLanguage = languageMap[languageCode] ?: "English"



    // This is where the dialog is conditionally shown
    if (uiState.isDecoderDialogShown) {
        DecoderSelectionDialog(
            currentDecoder = uiState.decoder,
            onDismiss = viewModel::onDismissDecoderDialog,
            onDecoderSelected = viewModel::onDecoderSelected
        )
    }

    Scaffold(
    ) { paddingValues ->
        paddingValues
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
        ) {

            item {
                SettingsNavigationItem(
                    icon = Icons.Default.Settings, // Placeholder
                    title = stringResource(R.string.app_language),
                    description = selectedLanguage,
                    onClick = {
                        context.startActivity(LanguagesActivity.getIntent(context as Activity))
                    }
                )
            }
            item {
                SettingsNavigationItem(
                    icon = Icons.Default.Settings, // Placeholder
                    title = stringResource(R.string.show_nomedia_files),
                    description = stringResource(R.string.show_files_in_folders_containing_nomedia_file),
                    onClick = { /* Navigate to nomedia settings */ }
                )
            }
            item {
                SettingsSwitchItem(
                    icon = Icons.Default.Settings, // Placeholder
                    title = stringResource(R.string.default_screen_orientation),
                    checked = uiState.defaultScreenOrientation == "Landscape", // Assuming "Landscape" is the "On" state
                    description = uiState.defaultScreenOrientation,
                    onCheckedChange = { isChecked ->
                        val newOrientation = if (isChecked) "Landscape" else "Automatic"
                        viewModel.onOrientationSelected(newOrientation)
                    }
                )
            }
            item {
                SettingsNavigationItem(
                    icon = Icons.Default.Settings, // Placeholder
                    title = stringResource(R.string.decoder),
                    description = uiState.decoder,
                    onClick = viewModel::onShowDecoderDialog // This triggers the dialog
                )
            }
            /*item {
                SettingsNavigationItem(
                    icon = Icons.Default.Settings, // Placeholder
                    title = "Gesture Control",
                    description = null,
                    onClick = { */ // Navigate to gesture settings */ }

        item {
            // The screenshot has a slight visual break here, let's add a divider
            Divider(modifier = Modifier.padding(horizontal = 16.dp))
        }


        // Switch with Description Items
       /* item {
            SettingsSwitchWithDescription(
                icon = Icons.Default.Settings, // Placeholder
                title = "Show Hidden Files",
                description = "Show files starting with dot(.).",
                checked = uiState.showHiddenFiles,
                onCheckedChange = viewModel::onShowHiddenFilesChange
            )
        }*/
        item {
            SettingsSwitchWithDescription(
                icon = Icons.Default.Settings, // Placeholder
                title = stringResource(R.string.remember_aspect_ratio),
                description = stringResource(R.string.remember_aspect_ratio_for_all_videos),
                checked = uiState.rememberAspectRatio,
                onCheckedChange = viewModel::onRememberAspectRatioChange
            )
        }

        // Simple Switch Items (Title + Switch)
        item {
            SettingsSwitchItem(
                icon = Icons.Default.Settings, // Placeholder
                title = stringResource(R.string.longpress_to_play_at_2x_speed),
                checked = uiState.longPressToPlayAt2xSpeed,
                description = "On", // Displaying "On" below is optional, doing it as description for consistency
                onCheckedChange = viewModel::onLongPressToPlayAt2xSpeedChange
            )
        }
        item {
            SettingsSwitchItem(
                icon = Icons.Default.Settings, // Placeholder
                title = stringResource(R.string.remember_background_play),
                checked = uiState.rememberBackgroundPlay,
                description = stringResource(R.string.remember_background_play_for_all_videos),
                onCheckedChange = viewModel::onRememberBackgroundPlayChange
            )
        }
        item {
            SettingsSwitchItem(
                icon = Icons.Default.Settings, // Placeholder
                title = stringResource(R.string.remember_brightness),
                checked = uiState.rememberBrightness,

                description = stringResource(R.string.turn_on_to_remember_brightness_for_all_videos),
                onCheckedChange = viewModel::onRememberBrightnessChange
            )
        }
        item {
            SettingsSwitchItem(
                icon = Icons.Default.Settings, // Placeholder
                title = stringResource(R.string.double_tap_to_fast_forward_and_rewind),
                checked = uiState.doubleTapToFastForwardAndRewind,
                description = "On",
                onCheckedChange = viewModel::onDoubleTapToFastForwardAndRewindChange
            )
        }
        item {
            SettingsSwitchItem(
                icon = Icons.Default.Settings, // Placeholder
                title = stringResource(R.string.auto_play_next),
                checked = uiState.autoPlayNext,
                description = stringResource(R.string.automatically_play_next_video_when_current_video_ends),
                onCheckedChange = viewModel::onAutoPlayNextChange
            )
        }
       /* item {
            SettingsSwitchItem(
                icon = Icons.Default.Settings, // Placeholder
                title = "Music",
                checked = uiState.showMusic,
                description = "Show music",
                onCheckedChange = viewModel::onShowMusicChange
            )
        }*/

        /*// About App Navigation Item
        item {
            SettingsNavigationItem(
                icon = Icons.Default.Info, // Placeholder
                title = "About App",
                description = null,
                onClick = { *//* Navigate to About screen *//* }
            )
        }*/
        item {
            SettingsNavigationItem(
                icon = Icons.Default.Share, // Placeholder
                title = stringResource(R.string.share_app),
                description = null,
                onClick = {
                    val sendIntent: Intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, "Check out this cool video player app: https://play.google.com/store/apps/details?id=${context.packageName}")
                        type = "text/plain"
                    }
                    val shareIntent = Intent.createChooser(sendIntent, null)
                    context.startActivity(shareIntent)
                }
            )
        }
        item {
            SettingsNavigationItem(
                icon = Icons.Default.Star, // Placeholder
                title = stringResource(R.string.rate_this_app),
                description = null,
                onClick = {
                    try {
                        val rateIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}"))
                        context.startActivity(rateIntent)
                    } catch (e: ActivityNotFoundException) {
                        val rateIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}"))
                        context.startActivity(rateIntent)
                    }
                }
            )
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}
}

@Composable
fun DecoderSelectionDialog(
    currentDecoder: String,
    onDismiss: () -> Unit,
    onDecoderSelected: (String) -> Unit
) {
    val decoderOptions = listOf("Hardware", "Software")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.decoder1)) },
        text = {
            Column {
                decoderOptions.forEach { decoder ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (decoder == currentDecoder),
                                onClick = { onDecoderSelected(decoder) },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (decoder == currentDecoder),
                            onClick = null // Recommended for accessibility with selectable parent
                        )
                        Text(
                            text = decoder,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL")
            }
        }
    )
}


/**
 * A reusable composable for a setting item that navigates to a sub-screen.
 */
@Composable
fun SettingsNavigationItem(
    icon: ImageVector,
    title: String,
    description: String?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        Icon(
            imageVector = icon,
            contentDescription = null, // Content description for setting icon isn't necessary
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        // Text Content
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
       /* // Navigation Arrow
        AsyncImage(
            model = R.drawable.ic_launcher_foreground,
            contentDescription = "Navigate",
            modifier = Modifier.size(16.dp)
        )*/


    }
}


/**
 * A reusable composable for a setting item with a title, description, and a toggle switch.
 */
@Composable
fun SettingsSwitchWithDescription(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        // Text Content
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        // Switch
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFFFF8D3B), // Your desired color FF8D3B
                uncheckedThumbColor = Color(0xFFE0E0E0),
                uncheckedTrackColor = Color(0xFFBDBDBD)
            )
        )
    }
}


/**
 * A reusable composable for a setting item with a title and a toggle switch.
 */
@Composable
fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    description: String?,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        // Text Content
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        // Switch
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFFFF8D3B), // Your desired color FF8D3B
                uncheckedThumbColor = Color(0xFFE0E0E0),
                uncheckedTrackColor = Color(0xFFBDBDBD)
            )
        )
    }
}
