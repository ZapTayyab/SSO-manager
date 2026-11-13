package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.FacultyMember
import com.example.domain.SavedSsoCredential

@Composable
fun SsoLoginScreen(
    directoryFaculty: List<FacultyMember>,
    savedCredential: SavedSsoCredential?,
    isDarkMode: Boolean,
    loginError: String?,
    isAuthenticating: Boolean = false,
    onToggleTheme: () -> Unit,
    onSignInWithCredentials: (identifier: String, passcode: String, rememberInJson: Boolean) -> Unit,
    onSelectDirectoryFaculty: (facultyId: String, rememberInJson: Boolean) -> Unit
) {
    var ssoInput by remember { mutableStateOf(savedCredential?.email ?: "evance@university.edu") }
    var passcode by remember { mutableStateOf(savedCredential?.passcode ?: "faculty2026") }
    var rememberInJson by remember { mutableStateOf(savedCredential?.rememberCredentials ?: true) }

    LaunchedEffect(savedCredential) {
        if (savedCredential != null) {
            ssoInput = savedCredential.email
            passcode = savedCredential.passcode
            rememberInJson = savedCredential.rememberCredentials
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Full-bleed aesthetic institute background image
        Image(
            painter = painterResource(id = R.drawable.img_institute_login_bg),
            contentDescription = "University institute campus background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Atmospheric gradient scrim ensuring high-contrast readability in both Light and Dark mode
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = if (isDarkMode) {
                            listOf(
                                Color(0xFF070C18).copy(alpha = 0.62f),
                                Color(0xFF0B1120).copy(alpha = 0.88f),
                                Color(0xFF0B1120).copy(alpha = 0.96f)
                            )
                        } else {
                            listOf(
                                Color(0xFF0E1D33).copy(alpha = 0.45f),
                                MaterialTheme.colorScheme.background.copy(alpha = 0.84f),
                                MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
                            )
                        }
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.widthIn(max = 560.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top bar with Enterprise Brand Mark + Light/Dark Mode Toggle Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        EnterpriseBrandMark(sizeDp = 40)
                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    letterSpacing = (-0.2).sp
                                ),
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "INSTITUTIONAL PORTAL",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    letterSpacing = 1.2.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }

                    ThemeTogglePillSwitch(
                        isDarkMode = isDarkMode,
                        onToggle = onToggleTheme,
                        onBlueHeader = true
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        color = Color.White.copy(alpha = 0.14f),
                        shape = CircleShape,
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.24f))
                    ) {
                        Text(
                            text = "FACULTY TIMETABLE CLOUD",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                letterSpacing = 1.2.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                    Text(
                        text = stringResource(R.string.sso_portal_title),
                        style = MaterialTheme.typography.displaySmall,
                        color = Color.White
                    )
                    Text(
                        text = stringResource(R.string.sso_portal_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }

                // Translucent layered Login Card with short-term JSON credential saver
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shape = MaterialTheme.shapes.extraLarge,
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (savedCredential != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DataObject,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Loaded saved credentials from sso_credentials.json (${savedCredential.ssoId})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = ssoInput,
                            onValueChange = { ssoInput = it },
                            label = { Text(stringResource(R.string.sso_id_label)) },
                            leadingIcon = {
                                Icon(Icons.Filled.Badge, contentDescription = null)
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("sso_identifier_input")
                        )

                        OutlinedTextField(
                            value = passcode,
                            onValueChange = { passcode = it },
                            label = { Text(stringResource(R.string.sso_passcode_label)) },
                            leadingIcon = {
                                Icon(Icons.Filled.Lock, contentDescription = null)
                            },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("sso_passcode_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Save Credentials (JSON)",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "Short-term local JSON credential cache",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = rememberInJson,
                                onCheckedChange = { rememberInJson = it },
                                modifier = Modifier.testTag("remember_json_switch")
                            )
                        }

                        if (loginError != null) {
                            Text(
                                text = loginError,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        Button(
                            onClick = { onSignInWithCredentials(ssoInput, passcode, rememberInJson) },
                            enabled = !isAuthenticating,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("sso_sign_in_button")
                        ) {
                            Text(
                                text = stringResource(R.string.sso_sign_in_button),
                                style = MaterialTheme.typography.labelLarge
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Quick Faculty Directory Sign-In
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(R.string.sso_directory_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    directoryFaculty.forEach { faculty ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("sso_quick_faculty_${faculty.ssoId}")
                                .clickable(enabled = !isAuthenticating) {
                                    onSelectDirectoryFaculty(faculty.id, rememberInJson)
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = faculty.fullName
                                                .split(" ")
                                                .takeLast(2)
                                                .joinToString("") { it.take(1) },
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = faculty.fullName,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${faculty.department} · SSO: ${faculty.ssoId}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Sign in as ${faculty.fullName}",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
