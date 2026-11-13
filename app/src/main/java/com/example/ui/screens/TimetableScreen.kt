package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.domain.CourseSession
import com.example.domain.ScheduleConflict
import com.example.domain.SessionType
import com.example.domain.TimetableFormatters
import com.example.ui.viewmodel.MainUiState
import com.example.ui.viewmodel.TimetableViewMode

@Composable
fun TimetableScreen(
    uiState: MainUiState,
    onSelectSemester: (String) -> Unit,
    onViewModeChange: (TimetableViewMode) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onAutoResolveClick: () -> Unit,
    onSessionClick: (CourseSession) -> Unit,
    onToggleLock: (CourseSession) -> Unit,
    onAddManualClick: () -> Unit,
    onNavigateToAdvisor: () -> Unit,
    modifier: Modifier = Modifier
) {
    val faculty = uiState.activeFaculty ?: return
    val workload = uiState.workloadSummary

    val filteredSessions by remember(uiState.sessions, uiState.searchQuery) {
        derivedStateOf {
            val q = uiState.searchQuery.trim()
            if (q.isBlank()) uiState.sessions
            else uiState.sessions.filter {
                it.courseCode.contains(q, ignoreCase = true) ||
                    it.courseTitle.contains(q, ignoreCase = true) ||
                    it.roomCode.contains(q, ignoreCase = true) ||
                    it.building.contains(q, ignoreCase = true)
            }
        }
    }

    val conflictingSessionIds = remember(uiState.conflicts) {
        uiState.conflicts.flatMap { listOfNotNull(it.primarySessionId, it.secondarySessionId) }.toSet()
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item(key = "semester_strip") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionEditorialHeader(
                    eyebrow = "Academic Term · ${faculty.ssoId}",
                    title = uiState.selectedSemester?.name ?: "Teaching Schedule",
                    trailingContent = {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)
                            )
                        ) {
                            Text(
                                text = "${uiState.sessions.size} classes",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                )
                SemesterSelectorStrip(
                    semesters = uiState.semesters,
                    selectedSemesterId = uiState.selectedSemester?.id,
                    onSelectSemester = onSelectSemester
                )
            }
        }

        if (uiState.sessions.isNotEmpty() && workload != null) {
            item(key = "conflict_banner") {
                ConflictStatusBanner(
                    conflicts = uiState.conflicts,
                    balanceScore = workload.balanceScore,
                    weeklyHours = workload.totalWeeklyHours,
                    targetHours = workload.targetMaxHours,
                    onAutoResolveClick = onAutoResolveClick
                )
            }
        }

        item(key = "search_and_mode") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Search course, room, building...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("timetable_search_input")
                )
                FilterChip(
                    selected = uiState.viewMode == TimetableViewMode.WEEK_GRID,
                    onClick = { onViewModeChange(TimetableViewMode.WEEK_GRID) },
                    modifier = Modifier.testTag("view_mode_grid"),
                    label = {
                        Icon(
                            imageVector = Icons.Filled.CalendarViewWeek,
                            contentDescription = "Week Grid View",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
                FilterChip(
                    selected = uiState.viewMode == TimetableViewMode.AGENDA_LIST,
                    onClick = { onViewModeChange(TimetableViewMode.AGENDA_LIST) },
                    modifier = Modifier.testTag("view_mode_agenda"),
                    label = {
                        Icon(
                            imageVector = Icons.Filled.ViewAgenda,
                            contentDescription = "Agenda List View",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }
        }

        if (filteredSessions.isEmpty()) {
            item(key = "empty_state") {
                EmptyScheduleStateCard(
                    onPrepareSemesterClick = onNavigateToAdvisor,
                    onAddManualClick = onAddManualClick
                )
            }
        } else if (uiState.viewMode == TimetableViewMode.WEEK_GRID) {
            item(key = "week_grid") {
                WeeklyTimetableGrid(
                    sessions = filteredSessions,
                    conflictingIds = conflictingSessionIds,
                    onSessionClick = onSessionClick
                )
            }
            if (uiState.conflicts.isNotEmpty()) {
                item(key = "conflict_breakdown") {
                    ConflictDetailsSection(
                        conflicts = uiState.conflicts,
                        sessions = uiState.sessions,
                        onSessionClick = onSessionClick
                    )
                }
            }
        } else {
            val grouped = filteredSessions.groupBy { it.dayOfWeek }.toSortedMap()
            grouped.forEach { (day, daySessions) ->
                item(key = "header_$day") {
                    Text(
                        text = TimetableFormatters.dayFull(day),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                items(daySessions, key = { it.id }) { session ->
                    AgendaSessionCard(
                        session = session,
                        hasConflict = session.id in conflictingSessionIds,
                        onClick = { onSessionClick(session) },
                        onToggleLock = { onToggleLock(session) }
                    )
                }
            }
        }

        item(key = "bottom_spacer") { Spacer(modifier = Modifier.height(92.dp)) }
    }
}

@Composable
private fun WeeklyTimetableGrid(
    sessions: List<CourseSession>,
    conflictingIds: Set<String>,
    onSessionClick: (CourseSession) -> Unit
) {
    val startHour = 8
    val endHour = 18
    val hourHeight = 64.dp
    val totalHeight = hourHeight * (endHour - startHour)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = MaterialTheme.shapes.large
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            val availableWidth = maxWidth
            val timeColumnWidth = 48.dp
            val dayColumnWidth: Dp = ((availableWidth - timeColumnWidth) / 5f).coerceAtLeast(104.dp)
            val horizontalScroll = rememberScrollState()

            Column {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.width(timeColumnWidth))
                    Row(
                        modifier = Modifier.horizontalScroll(horizontalScroll)
                    ) {
                        (1..5).forEach { day ->
                            val count = sessions.count { it.dayOfWeek == day }
                            Column(
                                modifier = Modifier
                                    .width(dayColumnWidth)
                                    .padding(bottom = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = TimetableFormatters.dayShort(day),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "$count ${if (count == 1) "class" else "classes"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(440.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Column(modifier = Modifier.width(timeColumnWidth)) {
                        (startHour until endHour).forEach { hr ->
                            Box(
                                modifier = Modifier
                                    .height(hourHeight)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.TopStart
                            ) {
                                Text(
                                    text = TimetableFormatters.formatHourShort(hr),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .height(totalHeight)
                            .horizontalScroll(horizontalScroll)
                    ) {
                        (1..5).forEach { day ->
                            val daySessions = sessions.filter { it.dayOfWeek == day }
                            Box(
                                modifier = Modifier
                                    .width(dayColumnWidth)
                                    .height(totalHeight)
                                    .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column {
                                    (startHour until endHour).forEach { _ ->
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(hourHeight)
                                                .border(
                                                    width = 0.5.dp,
                                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                                                )
                                        )
                                    }
                                }

                                daySessions.forEach { session ->
                                    val offsetHours = ((session.startMinuteOfDay - startHour * 60) / 60f).coerceAtLeast(0f)
                                    val durationHours = (session.durationMinutes / 60f).coerceAtLeast(0.75f)
                                    val topOffset = hourHeight * offsetHours
                                    val blockHeight = (hourHeight * durationHours).coerceAtLeast(48.dp)
                                    val type = SessionType.fromString(session.sessionType)
                                    val accent = sessionAccentColor(type)
                                    val isConflict = session.id in conflictingIds

                                    Surface(
                                        modifier = Modifier
                                            .offset(y = topOffset)
                                            .padding(horizontal = 3.dp, vertical = 1.dp)
                                            .fillMaxWidth()
                                            .height(blockHeight)
                                            .testTag("grid_session_${session.id}")
                                            .clickable { onSessionClick(session) },
                                        color = if (isConflict) {
                                            MaterialTheme.colorScheme.errorContainer
                                        } else {
                                            accent.copy(alpha = 0.14f)
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(
                                            width = if (isConflict) 1.5.dp else 1.dp,
                                            color = if (isConflict) MaterialTheme.colorScheme.error else accent.copy(alpha = 0.6f)
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(6.dp),
                                            verticalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = session.courseCode,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = if (isConflict) MaterialTheme.colorScheme.error else accent,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1
                                                )
                                                if (isConflict) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Warning,
                                                        contentDescription = "Conflict",
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                } else if (session.isLocked) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Lock,
                                                        contentDescription = "Locked",
                                                        tint = accent,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = session.roomCode,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = TimetableFormatters.formatMinuteOfDay(session.startMinuteOfDay),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
        }
    }
}

@Composable
private fun ConflictDetailsSection(
    conflicts: List<ScheduleConflict>,
    sessions: List<CourseSession>,
    onSessionClick: (CourseSession) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Active Conflict Breakdown",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        conflicts.forEach { conflict ->
            val target = sessions.firstOrNull { it.id == conflict.primarySessionId }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = target != null) { target?.let(onSessionClick) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(
                    1.dp,
                    if (conflict.type.isSevere) MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = conflict.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (conflict.type.isSevere) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
                    )
                    Text(
                        text = conflict.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (conflict.suggestedDayOfWeek != null && conflict.suggestedStartMinute != null) {
                        Text(
                            text = "Suggested slot: ${TimetableFormatters.formatSlotLabel(conflict.suggestedDayOfWeek, conflict.suggestedStartMinute)} · ${conflict.suggestedRoomCode}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AgendaSessionCard(
    session: CourseSession,
    hasConflict: Boolean,
    onClick: () -> Unit,
    onToggleLock: () -> Unit
) {
    val type = SessionType.fromString(session.sessionType)
    val accent = sessionAccentColor(type)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("agenda_session_${session.id}")
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = if (hasConflict) 1.5.dp else 1.dp,
            color = if (hasConflict) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(60.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (hasConflict) MaterialTheme.colorScheme.error else accent)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${session.courseCode} · ${session.section}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    SessionTypeBadge(session.sessionType)
                    if (hasConflict) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Conflict",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = session.courseTitle,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = TimetableFormatters.formatTimeRange(session.startMinuteOfDay, session.durationMinutes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${session.roomCode} (${session.building})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.People,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${session.enrolledStudents}/${session.capacity} students",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(
                onClick = onToggleLock,
                modifier = Modifier.testTag("lock_toggle_${session.id}")
            ) {
                Icon(
                    imageVector = if (session.isLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                    contentDescription = if (session.isLocked) "Unlock session" else "Lock session",
                    tint = if (session.isLocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
