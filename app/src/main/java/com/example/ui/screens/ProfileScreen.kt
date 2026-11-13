package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.ThemeMode
import com.example.domain.TimetableFormatters
import com.example.ui.viewmodel.MainUiState
import kotlin.math.roundToInt

@Composable
fun ProfileScreen(
    uiState: MainUiState,
    isDarkMode: Boolean,
    onToggleTheme: () -> Unit,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onSavePreferences: (
        maxWeeklyHours: Int,
        preferredStartHour: Int,
        preferredEndHour: Int,
        avoidBackToBackBuildings: Boolean,
        preferMorningLectures: Boolean
    ) -> Unit,
    onSaveAndReoptimize: (
        maxWeeklyHours: Int,
        preferredStartHour: Int,
        preferredEndHour: Int,
        avoidBackToBackBuildings: Boolean,
        preferMorningLectures: Boolean
    ) -> Unit,
    onSignOut: (clearSavedCredentials: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val faculty = uiState.activeFaculty ?: return

    var maxHours by remember(faculty) { mutableFloatStateOf(faculty.maxWeeklyHours.toFloat()) }
    var startHour by remember(faculty) { mutableFloatStateOf(faculty.preferredStartHour.toFloat()) }
    var endHour by remember(faculty) { mutableFloatStateOf(faculty.preferredEndHour.toFloat()) }
    var avoidBuildings by remember(faculty) { mutableStateOf(faculty.avoidBackToBackBuildings) }
    var preferMorning by remember(faculty) { mutableStateOf(faculty.preferMorningLectures) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item(key = "profile_header") {
            SectionEditorialHeader(
                eyebrow = "Institutional Credentials",
                title = "Faculty SSO & Preferences",
                subtitle = "Manage identity cache, theme appearance, and scheduling rules"
            )
        }

        item(key = "faculty_card") {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = MaterialTheme.shapes.large
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = faculty.fullName
                                .split(" ")
                                .takeLast(2)
                                .joinToString("") { it.take(1) },
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = faculty.fullName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = Icons.Filled.VerifiedUser,
                                contentDescription = "SSO Verified",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = "${faculty.academicTitle} · ${faculty.department}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text(
                                text = "SSO ID: ${faculty.ssoId} · ${faculty.email}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }

        // Light & Dark Mode Appearance Card
        item(key = "appearance_card") {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = MaterialTheme.shapes.large
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (isDarkMode) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text("Dark Mode", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = if (isDarkMode) "Dark Slate theme active" else "Ivory Academic light theme active",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = isDarkMode,
                            onCheckedChange = { onToggleTheme() },
                            modifier = Modifier.testTag("profile_dark_mode_switch")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = uiState.themeMode == mode,
                                onClick = { onSelectThemeMode(mode) },
                                modifier = Modifier.weight(1f),
                                label = { Text(mode.label) }
                            )
                        }
                    }
                }
            }
        }

        if (uiState.savedCredential != null) {
            item(key = "json_cred_status") {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DataObject,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "JSON Credential Saver Active",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "Credentials cached locally in sso_credentials.json for ${uiState.savedCredential.email}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }
        }

        item(key = "constraints_card") {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = MaterialTheme.shapes.large
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Scheduling Constraints & Experience Rules",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Weekly Teaching Target", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "${maxHours.roundToInt()} hours",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = maxHours,
                            onValueChange = { maxHours = it },
                            valueRange = 8f..20f,
                            steps = 11
                        )
                    }

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Earliest Preferred Start", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                TimetableFormatters.formatHourShort(startHour.roundToInt()),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = startHour,
                            onValueChange = { startHour = it },
                            valueRange = 7f..12f,
                            steps = 4
                        )
                    }

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Latest Preferred Finish", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                TimetableFormatters.formatHourShort(endHour.roundToInt()),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = endHour,
                            onValueChange = { endHour = it },
                            valueRange = 15f..20f,
                            steps = 4
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Avoid Cross-Building Rush", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Flag back-to-back sessions under 15m across distant buildings",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = avoidBuildings,
                            onCheckedChange = { avoidBuildings = it }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Prioritize Morning Lectures", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Favor 08:30–11:30 slots during auto-preparation",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = preferMorning,
                            onCheckedChange = { preferMorning = it }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                onSavePreferences(
                                    maxHours.roundToInt(),
                                    startHour.roundToInt(),
                                    endHour.roundToInt(),
                                    avoidBuildings,
                                    preferMorning
                                )
                            },
                            modifier = Modifier
                                .weight(0.9f)
                                .testTag("save_preferences_button")
                        ) {
                            Text("Save Rules")
                        }
                        Button(
                            onClick = {
                                onSaveAndReoptimize(
                                    maxHours.roundToInt(),
                                    startHour.roundToInt(),
                                    endHour.roundToInt(),
                                    avoidBuildings,
                                    preferMorning
                                )
                            },
                            modifier = Modifier
                                .weight(1.3f)
                                .testTag("save_reoptimize_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoFixHigh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save & Auto-Update")
                        }
                    }
                }
            }
        }

        item(key = "sign_out_actions") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { onSignOut(false) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("sso_sign_out_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sign Out")
                }
                OutlinedButton(
                    onClick = { onSignOut(true) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("sso_clear_json_sign_out_button")
                ) {
                    Text("Clear JSON & Exit")
                }
            }
        }

        item(key = "profile_bottom_spacer") { Spacer(modifier = Modifier.height(92.dp)) }
    }
}
