package com.applications.player.presentation.settings

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.applications.player.R

// NOTE: Since I don't have access to the specific icons from your screenshot,
// I'll use placeholders from Icons.Default and Icons.AutoMirrored.

/**
 * Main composable for the Settings screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsHomeScreen() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { /* Handle back navigation */ }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()

        ) {
            // Navigation Items (Title + Description + Arrow)
            item {
                SettingsNavigationItem(
                    icon = Icons.Default.Settings, // Placeholder
                    title = "App Language",
                    description = null,
                    onClick = { /* Navigate to language settings */ }
                )
            }
            item {
                SettingsNavigationItem(
                    icon = Icons.Default.Settings, // Placeholder
                    title = "Show .nomedia files",
                    description = "Show files in folders containing .nomedia file.",
                    onClick = { /* Navigate to nomedia settings */ }
                )
            }
            item {
                SettingsNavigationItem(
                    icon = Icons.Default.Settings, // Placeholder
                    title = "Default screen orientation",
                    description = "Auto-rotate(sensor)",
                    onClick = { /* Navigate to orientation settings */ }
                )
            }
            item {
                SettingsNavigationItem(
                    icon = Icons.Default.Settings, // Placeholder
                    title = "Decoder",
                    description = "Use HW Decoder in Priority",
                    onClick = { /* Navigate to decoder settings */ }
                )
            }
            item {
                SettingsNavigationItem(
                    icon = Icons.Default.Settings, // Placeholder
                    title = "Gesture Control",
                    description = null,
                    onClick = { /* Navigate to gesture settings */ }
                )
            }
            item {
                // The screenshot has a slight visual break here, let's add a divider
                Divider(modifier = Modifier.padding(horizontal = 16.dp))
            }


            // Switch with Description Items
            item {
                SettingsSwitchWithDescription(
                    icon = Icons.Default.Settings, // Placeholder
                    title = "Show Hidden Files",
                    description = "Show files starting with dot(.).",
                    initialCheckedState = false,
                    onCheckedChange = { /* Update state in ViewModel */ }
                )
            }
            item {
                SettingsSwitchWithDescription(
                    icon = Icons.Default.Settings, // Placeholder
                    title = "Remember aspect ratio",
                    description = "Remember aspect ratio for all videos.",
                    initialCheckedState = true,
                    onCheckedChange = { /* Update state in ViewModel */ }
                )
            }

            // Simple Switch Items (Title + Switch)
            item {
                SettingsSwitchItem(
                    icon = Icons.Default.Settings, // Placeholder
                    title = "Longpress to play at 2X Speed",
                    initialCheckedState = true,
                    description = "On", // Displaying "On" below is optional, doing it as description for consistency
                    onCheckedChange = { /* Update state in ViewModel */ }
                )
            }
            item {
                SettingsSwitchItem(
                    icon = Icons.Default.Settings, // Placeholder
                    title = "Remember background play",
                    initialCheckedState = true,
                    description = "Remember background play for all videos.",
                    onCheckedChange = { /* Update state in ViewModel */ }
                )
            }
            item {
                SettingsSwitchItem(
                    icon = Icons.Default.Settings, // Placeholder
                    title = "Remember brightness",
                    initialCheckedState = true,
                    description = "Turn on to remember brightness for all videos.",
                    onCheckedChange = { /* Update state in ViewModel */ }
                )
            }
            item {
                SettingsSwitchItem(
                    icon = Icons.Default.Settings, // Placeholder
                    title = "Double tap to fast forward and rewind",
                    initialCheckedState = true,
                    description = "On",
                    onCheckedChange = { /* Update state in ViewModel */ }
                )
            }
            item {
                SettingsSwitchItem(
                    icon = Icons.Default.Settings, // Placeholder
                    title = "Auto Play Next",
                    initialCheckedState = true,
                    description = "Automatically play next video when current video ends.",
                    onCheckedChange = { /* Update state in ViewModel */ }
                )
            }
            item {
                SettingsSwitchItem(
                    icon = Icons.Default.Settings, // Placeholder
                    title = "Music",
                    initialCheckedState = false,
                    description = "Show music",
                    onCheckedChange = { /* Update state in ViewModel */ }
                )
            }

            // About App Navigation Item
            item {
                SettingsNavigationItem(
                    icon = Icons.Default.Settings, // Placeholder
                    title = "About App",
                    description = null,
                    onClick = { /* Navigate to About screen */ }
                )
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
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
        // Navigation Arrow
        AsyncImage(
            model = R.drawable.ic_launcher_foreground,
            contentDescription = "Navigate",
            modifier = Modifier.size(16.dp)
        )


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
    initialCheckedState: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    // Local state for demonstration
    var isChecked by remember { mutableStateOf(initialCheckedState) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                isChecked = !isChecked
                onCheckedChange(isChecked)
            }
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
            checked = isChecked,
            onCheckedChange = {
                isChecked = it
                onCheckedChange(it)
            }
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
    initialCheckedState: Boolean,
    description: String?,
    onCheckedChange: (Boolean) -> Unit,
) {
    // Use remember and mutableStateOf to manage the switch state locally for demonstration
    // In a real MVVM app, you would pass the current state from the ViewModel.
    var isChecked by remember { mutableStateOf(initialCheckedState) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                isChecked = !isChecked
                onCheckedChange(isChecked)
            }
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
                    text = if (isChecked) "On" else "Off", // Example of showing current state
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        // Switch
        Switch(
            checked = isChecked,
            onCheckedChange = {
                isChecked = it
                onCheckedChange(it)
            }
        )
    }
}