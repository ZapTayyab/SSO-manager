package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.DailyWorkloadStat
import com.example.domain.FacultyWeeklyBreakdown
import com.example.domain.WorkloadLoadStatus
import com.example.ui.theme.LabAccent
import com.example.ui.theme.LectureAccent
import com.example.ui.theme.OfficeHoursAccent
import com.example.ui.theme.SeminarAccent
import com.example.ui.viewmodel.MainUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil

private enum class FacultyChartMode(val label: String) {
    STACKED_TYPES("Stacked by Type"),
    VS_TARGET_CAP("vs Target Cap")
}

@Composable
fun WorkloadScreen(
    uiState: MainUiState,
    onRebalanceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val workload = uiState.workloadSummary
    val semesterName = uiState.selectedSemester?.name ?: "Current Term"
    val dateFormatter = remember { SimpleDateFormat("MMM d, h:mm a", Locale.US) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item(key = "workload_header") {
            Column {
                Text(
                    text = "Workload Distribution",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "$semesterName · Faculty weekly breakdown & capacity balance",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (workload != null) {
            item(key = "workload_kpis") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    WorkloadKpiCard(
                        title = "Weekly Hours",
                        value = "${String.format(Locale.US, "%.1f", workload.totalWeeklyHours)}h",
                        subtitle = "Target ${workload.targetMaxHours}h max",
                        icon = Icons.Filled.Schedule,
                        modifier = Modifier.weight(1f)
                    )
                    WorkloadKpiCard(
                        title = "Balance Score",
                        value = "${workload.balanceScore}/100",
                        subtitle = if (workload.balanceScore >= 85) "Well distributed" else "Needs smoothing",
                        icon = Icons.Filled.Balance,
                        modifier = Modifier.weight(1f)
                    )
                    WorkloadKpiCard(
                        title = "Students",
                        value = "${workload.totalStudents}",
                        subtitle = "${workload.courseCount} courses",
                        icon = Icons.Filled.People,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (workload.facultyBreakdowns.isNotEmpty()) {
                item(key = "faculty_breakdown_dashboard") {
                    FacultyWeeklyWorkloadDashboardCard(
                        breakdowns = workload.facultyBreakdowns,
                        departmentAverageHours = workload.departmentAverageHours,
                        semesterName = semesterName
                    )
                }
            }

            item(key = "workload_chart_card") {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column {
                            Text(
                                text = "Your Daily Teaching Hours (Mon–Fri)",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Peak: ${workload.peakDayLabel} (${String.format(Locale.US, "%.1f", workload.peakDayHours)}h)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        DailyWorkloadBarChart(
                            dailyStats = workload.dailyStats,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(165.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            workload.dailyStats.forEach { stat ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = stat.shortName,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${String.format(Locale.US, "%.1f", stat.totalHours)}h",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = onRebalanceClick,
                            enabled = uiState.sessions.isNotEmpty(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("rebalance_workload_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoFixHigh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Auto-Balance Weekly Workload")
                        }
                    }
                }
            }
        }

        item(key = "audit_header") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.HistoryEdu,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Automated Update Audit Log",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        if (uiState.autoUpdateLogs.isEmpty()) {
            item(key = "audit_empty") {
                Text(
                    text = "Automatic timetable adjustments and semester rollovers will appear here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(uiState.autoUpdateLogs, key = { it.id }) { log ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = log.actionTitle,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = dateFormatter.format(Date(log.timestampMillis)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = log.summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item(key = "workload_bottom_spacer") { Spacer(modifier = Modifier.height(92.dp)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FacultyWeeklyWorkloadDashboardCard(
    breakdowns: List<FacultyWeeklyBreakdown>,
    departmentAverageHours: Double,
    semesterName: String
) {
    var chartMode by remember { mutableStateOf(FacultyChartMode.STACKED_TYPES) }
    var selectedFacultyId by remember(breakdowns) {
        mutableStateOf(
            breakdowns.firstOrNull { it.isCurrentUser }?.facultyId
                ?: breakdowns.firstOrNull()?.facultyId
        )
    }

    val selectedFaculty = remember(breakdowns, selectedFacultyId) {
        breakdowns.firstOrNull { it.facultyId == selectedFacultyId } ?: breakdowns.firstOrNull()
    }

    val maxChartHours = remember(breakdowns, departmentAverageHours, chartMode) {
        val maxVal = breakdowns.maxOfOrNull {
            if (chartMode == FacultyChartMode.VS_TARGET_CAP) {
                maxOf(it.totalWeeklyHours, it.targetMaxHours.toDouble())
            } else {
                it.totalWeeklyHours
            }
        } ?: 16.0
        val padded = maxOf(maxVal, departmentAverageHours) + 2.0
        (ceil(padded / 4.0) * 4.0).coerceAtLeast(12.0)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.testTag("faculty_workload_dashboard_card")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.BarChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "Weekly Hours by Faculty Member",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$semesterName · Tap a bar to inspect & balance load",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Chart series mode selector (Stacked breakdown vs Grouped capacity comparison)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FacultyChartMode.entries.forEach { mode ->
                    FilterChip(
                        selected = chartMode == mode,
                        onClick = { chartMode = mode },
                        label = { Text(mode.label) },
                        modifier = Modifier.testTag("chart_mode_${mode.name}")
                    )
                }
            }

            // Chart Canvas + Y-Axis + Interactive X-Axis
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // Y-Axis ticks (CartesianGrid labels)
                Column(
                    modifier = Modifier
                        .width(34.dp)
                        .fillMaxHeight()
                        .padding(bottom = 6.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    val step = (maxChartHours / 4.0)
                    for (tick in 4 downTo 0) {
                        Text(
                            text = "${(tick * step).toInt()}h",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                FacultyWeeklyChartCanvas(
                    breakdowns = breakdowns,
                    selectedFacultyId = selectedFaculty?.facultyId,
                    maxChartHours = maxChartHours,
                    departmentAverageHours = departmentAverageHours,
                    chartMode = chartMode,
                    onSelectFaculty = { selectedFacultyId = it },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }

            // Clickable X-Axis labels aligned with columns
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 42.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                breakdowns.forEach { member ->
                    val isSelected = member.facultyId == selectedFaculty?.facultyId
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedFacultyId = member.facultyId }
                            .padding(vertical = 6.dp, horizontal = 2.dp)
                            .testTag("faculty_chart_axis_${member.facultyId}"),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = member.shortLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "${String.format(Locale.US, "%.1f", member.totalWeeklyHours)}h",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Color Legend
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (chartMode == FacultyChartMode.STACKED_TYPES) {
                    ChartLegendDot("Lecture", LectureAccent)
                    ChartLegendDot("Lab", LabAccent)
                    ChartLegendDot("Seminar", SeminarAccent)
                    ChartLegendDot("Office Hrs", OfficeHoursAccent)
                } else {
                    ChartLegendDot("Scheduled", MaterialTheme.colorScheme.primary)
                    ChartLegendDot("Target Cap", MaterialTheme.colorScheme.outlineVariant)
                }
                ChartLegendDot(
                    "Dept Avg (${String.format(Locale.US, "%.1f", departmentAverageHours)}h)",
                    MaterialTheme.colorScheme.tertiary
                )
            }

            // Interactive Tooltip Inspector Panel for selected faculty member
            AnimatedVisibility(
                visible = selectedFaculty != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                selectedFaculty?.let { member ->
                    val statusColor = when (member.loadStatus) {
                        WorkloadLoadStatus.OVERLOADED -> MaterialTheme.colorScheme.error
                        WorkloadLoadStatus.UNDERLOADED -> MaterialTheme.colorScheme.tertiary
                        WorkloadLoadStatus.BALANCED -> MaterialTheme.colorScheme.secondary
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("faculty_chart_tooltip")
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (member.isCurrentUser) "${member.fullName} (You)" else member.fullName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${member.department} · ${member.sessionCount} sessions/wk",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    color = statusColor.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "${member.loadStatus.label} (${member.utilizationPercent}%)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = statusColor,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                BreakdownPill("Lec", member.lectureHours, LectureAccent, Modifier.weight(1f))
                                BreakdownPill("Lab", member.labHours, LabAccent, Modifier.weight(1f))
                                BreakdownPill("Sem", member.seminarHours, SeminarAccent, Modifier.weight(1f))
                                BreakdownPill("OH", member.officeHours, OfficeHoursAccent, Modifier.weight(1f))
                            }

                            val deltaFromAvg = member.totalWeeklyHours - departmentAverageHours
                            val balanceHint = when {
                                member.loadStatus == WorkloadLoadStatus.OVERLOADED ->
                                    "Near/above ${member.targetMaxHours}h cap (${String.format(Locale.US, "%+.1f", deltaFromAvg)}h vs avg). Consider reassigning 1 lab or seminar."
                                member.loadStatus == WorkloadLoadStatus.UNDERLOADED ->
                                    "${String.format(Locale.US, "%.1f", member.targetMaxHours - member.totalWeeklyHours)}h of spare capacity below ${member.targetMaxHours}h target."
                                else ->
                                    "Well-balanced at ${String.format(Locale.US, "%.1f", member.totalWeeklyHours)}h / ${member.targetMaxHours}h weekly target."
                            }
                            Text(
                                text = balanceHint,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Quick Faculty Utilization Comparison List
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                breakdowns.forEach { member ->
                    val ratio = if (member.targetMaxHours > 0) {
                        (member.totalWeeklyHours / member.targetMaxHours.toDouble()).toFloat().coerceIn(0f, 1f)
                    } else 0f
                    val barTint = when (member.loadStatus) {
                        WorkloadLoadStatus.OVERLOADED -> MaterialTheme.colorScheme.error
                        WorkloadLoadStatus.UNDERLOADED -> MaterialTheme.colorScheme.tertiary
                        WorkloadLoadStatus.BALANCED -> MaterialTheme.colorScheme.secondary
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedFacultyId = member.facultyId }
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (member.isCurrentUser) "${member.fullName} (You)" else member.fullName,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (member.isCurrentUser) FontWeight.Bold else FontWeight.Medium
                            )
                            Text(
                                text = "${String.format(Locale.US, "%.1f", member.totalWeeklyHours)}h / ${member.targetMaxHours}h",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        LinearProgressIndicator(
                            progress = { ratio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .clip(CircleShape),
                            color = barTint,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FacultyWeeklyChartCanvas(
    breakdowns: List<FacultyWeeklyBreakdown>,
    selectedFacultyId: String?,
    maxChartHours: Double,
    departmentAverageHours: Double,
    chartMode: FacultyChartMode,
    onSelectFaculty: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val avgLineColor = MaterialTheme.colorScheme.tertiary
    val selectedBackdrop = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
    val targetCapColor = MaterialTheme.colorScheme.outlineVariant
    val balancedColor = MaterialTheme.colorScheme.secondary
    val overloadedColor = MaterialTheme.colorScheme.error
    val underloadedColor = MaterialTheme.colorScheme.primary

    val animProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "faculty_chart_anim"
    )

    Canvas(
        modifier = modifier.pointerInput(breakdowns) {
            detectTapGestures { offset ->
                if (breakdowns.isNotEmpty() && size.width > 0) {
                    val slotW = size.width.toFloat() / breakdowns.size
                    val index = (offset.x / slotW).toInt().coerceIn(0, breakdowns.lastIndex)
                    onSelectFaculty(breakdowns[index].facultyId)
                }
            }
        }
    ) {
        val chartH = size.height
        val chartW = size.width
        val count = breakdowns.size.coerceAtLeast(1)
        val slotW = chartW / count

        // Horizontal Cartesian grid lines (4 intervals)
        for (i in 0..4) {
            val y = chartH * (i / 4f)
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(chartW, y),
                strokeWidth = 1.5f,
                pathEffect = if (i < 4) PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f) else null
            )
        }

        // Draw bars per faculty member
        breakdowns.forEachIndexed { index, member ->
            val slotLeft = index * slotW
            val isSelected = member.facultyId == selectedFacultyId

            if (isSelected) {
                drawRoundRect(
                    color = selectedBackdrop,
                    topLeft = Offset(slotLeft + 4f, 0f),
                    size = Size((slotW - 8f).coerceAtLeast(4f), chartH),
                    cornerRadius = CornerRadius(14f, 14f)
                )
            }

            if (chartMode == FacultyChartMode.STACKED_TYPES) {
                val barW = (slotW * 0.48f).coerceAtMost(48.dp.toPx())
                val barLeft = slotLeft + (slotW - barW) / 2f

                val segments = listOf(
                    member.lectureHours to LectureAccent,
                    member.labHours to LabAccent,
                    member.seminarHours to SeminarAccent,
                    member.officeHours to OfficeHoursAccent
                ).filter { it.first > 0.0 }

                var currentBottom = chartH
                segments.forEachIndexed { segIdx, (hours, segColor) ->
                    val segHeight = ((hours / maxChartHours) * chartH * animProgress).toFloat()
                    if (segHeight > 0f) {
                        val topY = (currentBottom - segHeight).coerceAtLeast(0f)
                        val isTop = segIdx == segments.lastIndex
                        drawRoundRect(
                            color = if (isSelected || selectedFacultyId == null) segColor else segColor.copy(alpha = 0.72f),
                            topLeft = Offset(barLeft, topY),
                            size = Size(barW, segHeight),
                            cornerRadius = if (isTop) CornerRadius(10f, 10f) else CornerRadius(2f, 2f)
                        )
                        currentBottom = topY
                    }
                }
            } else {
                val pairGap = 4.dp.toPx()
                val singleBarW = (slotW * 0.28f).coerceAtMost(24.dp.toPx())
                val groupLeft = slotLeft + (slotW - (singleBarW * 2 + pairGap)) / 2f

                val actualHeight = ((member.totalWeeklyHours / maxChartHours) * chartH * animProgress).toFloat()
                val capHeight = ((member.targetMaxHours / maxChartHours) * chartH * animProgress).toFloat()

                val actualColor = when (member.loadStatus) {
                    WorkloadLoadStatus.OVERLOADED -> overloadedColor
                    WorkloadLoadStatus.BALANCED -> balancedColor
                    WorkloadLoadStatus.UNDERLOADED -> underloadedColor
                }

                if (actualHeight > 0f) {
                    drawRoundRect(
                        color = actualColor,
                        topLeft = Offset(groupLeft, (chartH - actualHeight).coerceAtLeast(0f)),
                        size = Size(singleBarW, actualHeight),
                        cornerRadius = CornerRadius(8f, 8f)
                    )
                }
                if (capHeight > 0f) {
                    drawRoundRect(
                        color = targetCapColor,
                        topLeft = Offset(groupLeft + singleBarW + pairGap, (chartH - capHeight).coerceAtLeast(0f)),
                        size = Size(singleBarW, capHeight),
                        cornerRadius = CornerRadius(8f, 8f)
                    )
                }
            }
        }

        // Reference line for Department Average
        val avgY = (chartH - ((departmentAverageHours / maxChartHours).toFloat() * chartH)).coerceIn(0f, chartH)
        drawLine(
            color = avgLineColor,
            start = Offset(0f, avgY),
            end = Offset(chartW, avgY),
            strokeWidth = 3f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 8f), 0f)
        )
    }
}

@Composable
private fun ChartLegendDot(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BreakdownPill(
    label: String,
    hours: Double,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = accent.copy(alpha = 0.12f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = accent,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${String.format(Locale.US, "%.1f", hours)}h",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun WorkloadKpiCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DailyWorkloadBarChart(
    dailyStats: List<DailyWorkloadStat>,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val tealColor = MaterialTheme.colorScheme.secondary
    val amberColor = MaterialTheme.colorScheme.tertiary
    val errorColor = MaterialTheme.colorScheme.error
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val thresholdColor = MaterialTheme.colorScheme.outline

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val chartHeight = size.height
            val chartWidth = size.width
            val maxScaleHours = maxOf(6.0, (dailyStats.maxOfOrNull { it.totalHours } ?: 5.0) + 1.0)
            val count = dailyStats.size.coerceAtLeast(1)
            val slotWidth = chartWidth / count
            val barWidth = (slotWidth * 0.46f).coerceAtMost(56.dp.toPx())

            val thresholdY = chartHeight - ((3.5 / maxScaleHours).toFloat() * chartHeight)
            drawLine(
                color = thresholdColor,
                start = Offset(0f, thresholdY),
                end = Offset(chartWidth, thresholdY),
                strokeWidth = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
            )

            dailyStats.forEachIndexed { index, stat ->
                val left = index * slotWidth + (slotWidth - barWidth) / 2f
                drawRoundRect(
                    color = trackColor,
                    topLeft = Offset(left, 0f),
                    size = Size(barWidth, chartHeight),
                    cornerRadius = CornerRadius(12f, 12f)
                )

                val ratio = (stat.totalHours / maxScaleHours).toFloat().coerceIn(0f, 1f)
                val barHeight = chartHeight * ratio
                val barColor = when {
                    stat.hasSevereConflict -> errorColor
                    stat.isOverloaded -> amberColor
                    stat.totalHours > 0 -> tealColor
                    else -> primaryColor.copy(alpha = 0.25f)
                }

                if (barHeight > 0f) {
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(left, chartHeight - barHeight),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(12f, 12f)
                    )
                }
            }
        }
    }
}
