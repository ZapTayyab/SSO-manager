package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.CourseSession
import com.example.domain.ScheduleConflict
import com.example.domain.Semester
import com.example.domain.SemesterStatus
import com.example.domain.SessionType
import com.example.domain.TimetableFormatters
import com.example.domain.conflict.TimetableConflictChecker
import com.example.ui.theme.LabAccent
import com.example.ui.theme.LectureAccent
import com.example.ui.theme.OfficeHoursAccent
import com.example.ui.theme.SeminarAccent

@Composable
fun ThemeTogglePillSwitch(
    isDarkMode: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val trackColor by animateColorAsState(
        targetValue = if (isDarkMode) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(250),
        label = "theme_switch_track"
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (isDarkMode) 28.dp else 2.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "theme_switch_thumb"
    )

    Surface(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(CircleShape)
            .clickable(
                role = Role.Switch,
                onClick = onToggle
            )
            .testTag("theme_mode_toggle_switch"),
        shape = CircleShape,
        color = trackColor,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        tonalElevation = 2.dp
    ) {
        Box(
            modifier = Modifier
                .width(64.dp)
                .height(34.dp)
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.LightMode,
                    contentDescription = null,
                    tint = if (!isDarkMode) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .size(16.dp)
                )
                Icon(
                    imageVector = Icons.Filled.DarkMode,
                    contentDescription = null,
                    tint = if (isDarkMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .size(16.dp)
                )
            }
            Surface(
                modifier = Modifier
                    .offset(x = thumbOffset)
                    .size(26.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                shadowElevation = 2.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isDarkMode) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                        contentDescription = if (isDarkMode) "Switch to light mode" else "Switch to dark mode",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EnterpriseBrandMark(
    modifier: Modifier = Modifier,
    sizeDp: Int = 38
) {
    Surface(
        modifier = modifier.size(sizeDp.dp),
        shape = RoundedCornerShape(11.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
        shadowElevation = 2.dp
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_art),
            contentDescription = stringResource(R.string.app_name),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun EnterpriseCleanHeader(
    title: String,
    subtitle: String,
    isDarkMode: Boolean,
    onToggleTheme: () -> Unit,
    showExportPdf: Boolean = false,
    onExportPdfClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    EnterpriseBrandMark(sizeDp = 38)
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (showExportPdf) {
                        IconButton(
                            onClick = onExportPdfClick,
                            modifier = Modifier.testTag("export_pdf_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PictureAsPdf,
                                contentDescription = "Export weekly timetable to PDF",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    ThemeTogglePillSwitch(
                        isDarkMode = isDarkMode,
                        onToggle = onToggleTheme
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
            )
        }
    }
}

data class AnimatedNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val badgeCount: Int = 0
)

@Composable
fun ModernAnimatedNavBar(
    items: List<AnimatedNavItem>,
    currentRoute: String,
    onSelectRoute: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            tonalElevation = 6.dp,
            shadowElevation = 10.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val selected = currentRoute == item.route
                    val pillColor by animateColorAsState(
                        targetValue = if (selected) MaterialTheme.colorScheme.primary
                        else Color.Transparent,
                        animationSpec = tween(durationMillis = 220),
                        label = "nav_pill_color"
                    )
                    val contentColor by animateColorAsState(
                        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = tween(durationMillis = 220),
                        label = "nav_content_color"
                    )
                    val iconScale by animateFloatAsState(
                        targetValue = if (selected) 1.08f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "nav_icon_scale"
                    )
                    Box(
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(20.dp))
                            .background(pillColor)
                            .clickable(
                                role = Role.Tab,
                                onClick = { onSelectRoute(item.route) }
                            )
                            .padding(
                                horizontal = if (selected) 16.dp else 12.dp,
                                vertical = 10.dp
                            )
                            .testTag("nav_${item.route}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            BadgedBox(
                                badge = {
                                    if (item.badgeCount > 0) {
                                        Badge(
                                            containerColor = MaterialTheme.colorScheme.error,
                                            contentColor = MaterialTheme.colorScheme.onError
                                        ) {
                                            Text(item.badgeCount.toString())
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label,
                                    tint = contentColor,
                                    modifier = Modifier
                                        .size(22.dp)
                                        .scale(iconScale)
                                )
                            }

                            AnimatedVisibility(
                                visible = selected,
                                enter = fadeIn(tween(180)) + expandHorizontally(
                                    spring(dampingRatio = Spring.DampingRatioNoBouncy)
                                ),
                                exit = fadeOut(tween(120)) + shrinkHorizontally()
                            ) {
                                Text(
                                    text = item.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = contentColor,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun sessionAccentColor(type: SessionType): Color = when (type) {
    SessionType.LECTURE -> LectureAccent
    SessionType.LAB -> LabAccent
    SessionType.SEMINAR -> SeminarAccent
    SessionType.OFFICE_HOURS -> OfficeHoursAccent
}

@Composable
fun SessionTypeBadge(sessionType: String, modifier: Modifier = Modifier) {
    val type = SessionType.fromString(sessionType)
    val accent = sessionAccentColor(type)
    val icon = when (type) {
        SessionType.LECTURE -> Icons.Filled.School
        SessionType.LAB -> Icons.Filled.Science
        SessionType.SEMINAR -> Icons.Filled.Groups
        SessionType.OFFICE_HOURS -> Icons.Filled.Schedule
    }
    Surface(
        modifier = modifier,
        color = accent.copy(alpha = 0.14f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = type.label,
                tint = accent,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = type.shortLabel,
                style = MaterialTheme.typography.labelMedium,
                color = accent,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SemesterSelectorStrip(
    semesters: List<Semester>,
    selectedSemesterId: String?,
    onSelectSemester: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        semesters.forEach { semester ->
            val selected = semester.id == selectedSemesterId
            val status = SemesterStatus.fromString(semester.status)
            val statusColor = when (status) {
                SemesterStatus.ACTIVE -> MaterialTheme.colorScheme.secondary
                SemesterStatus.DRAFT -> MaterialTheme.colorScheme.tertiary
                SemesterStatus.HISTORICAL -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            FilterChip(
                selected = selected,
                onClick = { onSelectSemester(semester.id) },
                modifier = Modifier.testTag("semester_chip_${semester.id}"),
                label = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Text(
                            text = "${semester.name} · ${status.label}",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

@Composable
fun ConflictStatusBanner(
    conflicts: List<ScheduleConflict>,
    balanceScore: Int,
    weeklyHours: Double,
    targetHours: Int,
    onAutoResolveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val severeCount = conflicts.count { it.type.isSevere }
    val hasConflicts = conflicts.isNotEmpty()

    val containerColor = when {
        severeCount > 0 -> MaterialTheme.colorScheme.errorContainer
        hasConflicts -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.secondaryContainer
    }
    val contentColor = when {
        severeCount > 0 -> MaterialTheme.colorScheme.onErrorContainer
        hasConflicts -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = if (hasConflicts) Icons.Filled.Warning else Icons.Filled.CheckCircle,
                    contentDescription = if (hasConflicts) "Scheduling conflicts detected" else "Schedule conflict-free",
                    tint = contentColor,
                    modifier = Modifier.size(24.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = if (hasConflicts) {
                            "${conflicts.size} Scheduling ${if (conflicts.size == 1) "Issue" else "Issues"} Detected"
                        } else {
                            "Conflict-Free Schedule"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        color = contentColor,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (hasConflicts) {
                            conflicts.first().title + " · ${String.format("%.1f", weeklyHours)}h/${targetHours}h"
                        } else {
                            "Balance $balanceScore/100 · ${String.format("%.1f", weeklyHours)}h / ${targetHours}h weekly target"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor.copy(alpha = 0.9f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (hasConflicts) {
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onAutoResolveClick,
                    modifier = Modifier.testTag("auto_resolve_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoFixHigh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.action_auto_resolve),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
fun SampleAiNoticeBanner(
    customNotice: String? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = "Sample AI mode notice",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = stringResource(R.string.ai_sample_notice_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = customNotice ?: stringResource(R.string.ai_sample_notice_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun EmptyScheduleStateCard(
    onPrepareSemesterClick: () -> Unit,
    onAddManualClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_empty_schedule),
                contentDescription = "Empty teaching timetable illustration",
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(150.dp)
                    .clip(MaterialTheme.shapes.medium),
                contentScale = ContentScale.Crop
            )
            Text(
                text = stringResource(R.string.empty_schedule_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.empty_schedule_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onAddManualClick,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("empty_add_session_button")
                ) {
                    Text(stringResource(R.string.action_add_session))
                }
                Button(
                    onClick = onPrepareSemesterClick,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("empty_auto_prepare_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoFixHigh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Auto-Prepare")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SessionEditorBottomSheet(
    initialSession: CourseSession?,
    existingFacultySessions: List<CourseSession> = emptyList(),
    allSemesterSessions: List<CourseSession> = emptyList(),
    semesterId: String = "",
    facultyId: String = "",
    onDismiss: () -> Unit,
    onSave: (
        existingId: String?,
        courseCode: String,
        courseTitle: String,
        section: String,
        sessionType: String,
        dayOfWeek: Int,
        startMinuteOfDay: Int,
        durationMinutes: Int,
        roomCode: String,
        building: String,
        enrolledStudents: Int,
        isLocked: Boolean
    ) -> Unit,
    onDelete: ((CourseSession) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var courseCode by remember(initialSession) { mutableStateOf(initialSession?.courseCode ?: "CS-320") }
    var courseTitle by remember(initialSession) { mutableStateOf(initialSession?.courseTitle ?: "Machine Learning Foundations") }
    var section by remember(initialSession) { mutableStateOf(initialSession?.section ?: "Sec A") }
    var sessionType by remember(initialSession) { mutableStateOf(initialSession?.sessionType ?: "LECTURE") }
    var dayOfWeek by remember(initialSession) { mutableIntStateOf(initialSession?.dayOfWeek ?: 1) }
    var startMinute by remember(initialSession) { mutableIntStateOf(initialSession?.startMinuteOfDay ?: 9 * 60 + 30) }
    var durationMinutes by remember(initialSession) { mutableIntStateOf(initialSession?.durationMinutes ?: 90) }
    var roomCode by remember(initialSession) { mutableStateOf(initialSession?.roomCode ?: "Turing 204") }
    var building by remember(initialSession) { mutableStateOf(initialSession?.building ?: "Engineering West") }
    var enrolledText by remember(initialSession) { mutableStateOf((initialSession?.enrolledStudents ?: 45).toString()) }
    var isLocked by remember(initialSession) { mutableStateOf(initialSession?.isLocked ?: false) }

    val resolvedSemesterId = initialSession?.semesterId ?: semesterId
    val resolvedFacultyId = initialSession?.facultyId ?: facultyId
    val liveConflictCheck = remember(
        initialSession?.id,
        resolvedFacultyId,
        resolvedSemesterId,
        courseCode,
        dayOfWeek,
        startMinute,
        durationMinutes,
        roomCode,
        building,
        existingFacultySessions,
        allSemesterSessions
    ) {
        val candidate = CourseSession(
            id = initialSession?.id ?: "__draft_candidate__",
            facultyId = resolvedFacultyId,
            semesterId = resolvedSemesterId,
            courseCode = courseCode.trim().uppercase().ifBlank { "COURSE" },
            courseTitle = courseTitle.trim(),
            section = section.trim(),
            sessionType = sessionType,
            dayOfWeek = dayOfWeek,
            startMinuteOfDay = startMinute,
            durationMinutes = durationMinutes,
            roomCode = roomCode.trim(),
            building = building.trim(),
            enrolledStudents = enrolledText.toIntOrNull() ?: 40,
            capacity = 60,
            isLocked = isLocked,
            sourceNote = "",
            updatedAtMillis = 0L
        )
        TimetableConflictChecker.checkCandidateSession(
            candidate = candidate,
            facultySessions = existingFacultySessions,
            allSemesterSessions = allSemesterSessions
        )
    }

    val timeSlots = remember {
        listOf(8 * 60 + 30, 9 * 60 + 30, 10 * 60 + 30, 11 * 60 + 30, 13 * 60, 14 * 60, 15 * 60 + 30)
    }
    val roomPresets = remember {
        listOf(
            "Turing 204" to "Engineering West",
            "Turing 301" to "Engineering West",
            "Ada Lab 102" to "Engineering West",
            "Ada Lab 104" to "Science Commons",
            "Euler 101" to "Science Commons",
            "Hub 215" to "Humanities Hall"
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (initialSession == null) "Schedule New Class" else "Edit Teaching Session",
                    style = MaterialTheme.typography.headlineSmall
                )
                if (initialSession != null && onDelete != null) {
                    IconButton(
                        onClick = { onDelete(initialSession) },
                        modifier = Modifier.testTag("delete_session_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete session",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = courseCode,
                    onValueChange = { courseCode = it },
                    label = { Text("Course Code") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_course_code")
                )
                OutlinedTextField(
                    value = section,
                    onValueChange = { section = it },
                    label = { Text("Section") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(0.8f)
                        .testTag("input_section")
                )
            }

            OutlinedTextField(
                value = courseTitle,
                onValueChange = { courseTitle = it },
                label = { Text("Course Title") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_course_title")
            )

            Text("Session Type", style = MaterialTheme.typography.labelLarge)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SessionType.entries.forEach { type ->
                    FilterChip(
                        selected = sessionType == type.name,
                        onClick = { sessionType = type.name },
                        label = { Text(type.label) }
                    )
                }
            }

            Text("Day of Week", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                (1..5).forEach { day ->
                    FilterChip(
                        selected = dayOfWeek == day,
                        onClick = { dayOfWeek = day },
                        modifier = Modifier.weight(1f),
                        label = { Text(TimetableFormatters.dayShort(day)) }
                    )
                }
            }

            Text("Start Time", style = MaterialTheme.typography.labelLarge)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                timeSlots.forEach { slot ->
                    FilterChip(
                        selected = startMinute == slot,
                        onClick = { startMinute = slot },
                        label = { Text(TimetableFormatters.formatMinuteOfDay(slot)) }
                    )
                }
            }

            Text("Duration", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(60, 90, 120).forEach { dur ->
                    FilterChip(
                        selected = durationMinutes == dur,
                        onClick = { durationMinutes = dur },
                        label = { Text("${dur}m") }
                    )
                }
            }

            Text("Campus Room & Building", style = MaterialTheme.typography.labelLarge)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                roomPresets.forEach { (presetRoom, presetBldg) ->
                    FilterChip(
                        selected = roomCode == presetRoom,
                        onClick = {
                            roomCode = presetRoom
                            building = presetBldg
                        },
                        label = { Text("$presetRoom · ${presetBldg.substringBefore(" ")}") }
                    )
                }
            }

            OutlinedTextField(
                value = enrolledText,
                onValueChange = { enrolledText = it.filter { ch -> ch.isDigit() } },
                label = { Text("Enrolled Students") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (isLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text("Lock Session Slot", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Prevent Auto-Resolve from moving this time block",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = isLocked,
                        onCheckedChange = { isLocked = it },
                        modifier = Modifier.testTag("lock_session_switch")
                    )
                }
            }

            if (liveConflictCheck.hasConflict) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("editor_conflict_warning_card")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = "Conflict warning",
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = liveConflictCheck.warningTitle,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = liveConflictCheck.warningMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        onSave(
                            initialSession?.id,
                            courseCode,
                            courseTitle,
                            section,
                            sessionType,
                            dayOfWeek,
                            startMinute,
                            durationMinutes,
                            roomCode,
                            building,
                            enrolledText.toIntOrNull() ?: 40,
                            isLocked
                        )
                    },
                    enabled = courseCode.isNotBlank() && courseTitle.isNotBlank(),
                    modifier = Modifier
                        .weight(1.4f)
                        .testTag("save_session_button")
                ) {
                    Text(if (initialSession == null) "Add to Timetable" else "Save Changes")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfExportBottomSheet(
    semesterName: String,
    sessionCount: Int,
    suggestedFileName: String,
    isExporting: Boolean,
    onDismiss: () -> Unit,
    onSaveToDevice: () -> Unit,
    onSharePdf: () -> Unit,
    onPrintPdf: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.PictureAsPdf,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Column {
                    Text(
                        text = "Export Weekly Timetable PDF",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$semesterName · $sessionCount classes · $suggestedFileName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Button(
                onClick = onSaveToDevice,
                enabled = !isExporting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("pdf_save_device_button")
            ) {
                Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save PDF to Device")
            }

            OutlinedButton(
                onClick = onSharePdf,
                enabled = !isExporting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("pdf_share_button")
            ) {
                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Share PDF")
            }

            OutlinedButton(
                onClick = onPrintPdf,
                enabled = !isExporting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("pdf_print_button")
            ) {
                Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Print Weekly Schedule")
            }

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Close")
            }
        }
    }
}
