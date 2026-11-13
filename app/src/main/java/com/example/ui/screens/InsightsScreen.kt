package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.HistoricalExperience
import com.example.domain.OptimizationGoal
import com.example.domain.RecommendationCategory
import com.example.domain.ScheduleRecommendation
import com.example.domain.TimetableFormatters
import com.example.ui.viewmodel.MainUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InsightsScreen(
    uiState: MainUiState,
    onRefreshRecommendations: () -> Unit,
    onApplyRecommendation: (ScheduleRecommendation) -> Unit,
    onDismissRecommendation: (String) -> Unit,
    onPrepareNewSemester: (sourceId: String, targetId: String, goal: OptimizationGoal) -> Unit,
    onNavigateToTimetable: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(uiState.selectedSemester?.id, uiState.activeFaculty?.id) {
        if (uiState.aiResult == null && !uiState.isGeneratingAi) {
            onRefreshRecommendations()
        }
    }

    val semestersWithHistory = remember(uiState.semesters) {
        uiState.semesters.filter { it.id != "sem_2027_sp" }.ifEmpty { uiState.semesters }
    }
    val draftTargetDefault = remember(uiState.semesters) {
        uiState.semesters.firstOrNull { it.status == "DRAFT" }?.id
            ?: uiState.semesters.firstOrNull()?.id
            ?: "sem_2027_sp"
    }

    var selectedSourceId by remember(semestersWithHistory) {
        mutableStateOf(
            semestersWithHistory.firstOrNull { it.status == "HISTORICAL" }?.id
                ?: semestersWithHistory.firstOrNull()?.id
                ?: "sem_2026_sp"
        )
    }
    var selectedTargetId by remember(draftTargetDefault) { mutableStateOf(draftTargetDefault) }
    var selectedGoal by remember { mutableStateOf(OptimizationGoal.BALANCE_WORKLOAD) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item(key = "insights_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Historical Advisor",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "Intelligent recommendations for ${uiState.selectedSemester?.name ?: "current term"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onRefreshRecommendations,
                    enabled = !uiState.isGeneratingAi,
                    modifier = Modifier.testTag("refresh_ai_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Refresh recommendations"
                    )
                }
            }
        }

        item(key = "sample_ai_notice") {
            SampleAiNoticeBanner(customNotice = uiState.aiResult?.noticeMessage)
        }

        item(key = "executive_summary") {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "Schedule Synthesis",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    if (uiState.isGeneratingAi) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Text(
                                text = "Analyzing historical timetables & room occupancy...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    } else {
                        Text(
                            text = uiState.aiResult?.executiveSummary
                                ?: "Tap refresh to analyze your timetable against historical teaching records.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        val recs = uiState.aiResult?.recommendations.orEmpty()
        if (recs.isNotEmpty()) {
            item(key = "recs_title") {
                Text(
                    text = "Recommended Timetable Updates (${recs.size})",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            items(recs, key = { it.id }) { rec ->
                RecommendationCard(
                    recommendation = rec,
                    onApply = { onApplyRecommendation(rec) },
                    onDismiss = { onDismissRecommendation(rec.id) }
                )
            }
        } else if (!uiState.isGeneratingAi) {
            item(key = "no_recs") {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "No pending slot optimizations for ${uiState.selectedSemester?.name ?: "this term"}.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }

        item(key = "semester_preparer") {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                shape = MaterialTheme.shapes.large
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoFixHigh,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = "New Semester Auto-Preparer",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Minimize manual setup by drafting from historical experience",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text("Source Historical Term", style = MaterialTheme.typography.labelLarge)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        semestersWithHistory.forEach { sem ->
                            FilterChip(
                                selected = selectedSourceId == sem.id,
                                onClick = { selectedSourceId = sem.id },
                                label = { Text(sem.name) }
                            )
                        }
                    }

                    Text("Target Semester", style = MaterialTheme.typography.labelLarge)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        uiState.semesters.filter { it.status != "HISTORICAL" }.forEach { sem ->
                            FilterChip(
                                selected = selectedTargetId == sem.id,
                                onClick = { selectedTargetId = sem.id },
                                label = { Text("${sem.name} (${sem.status.lowercase()})") }
                            )
                        }
                    }

                    Text("Optimization Priority", style = MaterialTheme.typography.labelLarge)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OptimizationGoal.entries.forEach { goal ->
                            FilterChip(
                                selected = selectedGoal == goal,
                                onClick = { selectedGoal = goal },
                                label = { Text(goal.label) }
                            )
                        }
                    }

                    Button(
                        onClick = {
                            onPrepareNewSemester(selectedSourceId, selectedTargetId, selectedGoal)
                            onNavigateToTimetable()
                        },
                        enabled = !uiState.isPreparingSemester && selectedSourceId != selectedTargetId,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("prepare_semester_button")
                    ) {
                        if (uiState.isPreparingSemester) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.AutoFixHigh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.action_prepare_semester))
                        }
                    }
                }
            }
        }

        if (uiState.historicalExperiences.isNotEmpty()) {
            item(key = "history_header") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Historical Teaching Experience",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            items(uiState.historicalExperiences, key = { it.id }) { exp ->
                HistoricalExperienceCard(exp)
            }
        }

        item(key = "insights_bottom_spacer") { Spacer(modifier = Modifier.height(92.dp)) }
    }
}

@Composable
private fun RecommendationCard(
    recommendation: ScheduleRecommendation,
    onApply: () -> Unit,
    onDismiss: () -> Unit
) {
    val badgeColor = when (recommendation.category) {
        RecommendationCategory.CONFLICT_RESOLUTION -> MaterialTheme.colorScheme.error
        RecommendationCategory.HISTORICAL_OPTIMIZATION -> MaterialTheme.colorScheme.secondary
        RecommendationCategory.WORKLOAD_BALANCE -> MaterialTheme.colorScheme.primary
        RecommendationCategory.ROOM_FIT -> MaterialTheme.colorScheme.tertiary
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = badgeColor.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = recommendation.impactBadge,
                        style = MaterialTheme.typography.labelMedium,
                        color = badgeColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Text(
                    text = "${recommendation.confidencePercent}% match",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = recommendation.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = recommendation.rationale,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = "Precedent: ${recommendation.historicalPrecedent}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(10.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(0.8f)
                        .testTag("dismiss_rec_${recommendation.id}")
                ) {
                    Text("Dismiss")
                }
                Button(
                    onClick = onApply,
                    modifier = Modifier
                        .weight(1.2f)
                        .testTag("apply_rec_${recommendation.id}")
                ) {
                    Text("Apply Update")
                }
            }
        }
    }
}

@Composable
private fun HistoricalExperienceCard(experience: HistoricalExperience) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${experience.courseCode} · ${experience.semesterName}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = "Satisfaction rating",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${experience.satisfactionScore} · ${experience.attendanceRate}% att.",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Text(
                text = "${TimetableFormatters.formatSlotLabel(experience.dayOfWeek, experience.startMinuteOfDay)} in ${experience.roomCode} (${experience.building})",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = experience.lessonLearnedNote,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
