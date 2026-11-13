package com.example.data.ai

import com.example.domain.AiRecommendationResult
import com.example.domain.CourseSession
import com.example.domain.FacultyMember
import com.example.domain.HistoricalExperience
import com.example.domain.RecommendationCategory
import com.example.domain.ScheduleConflict
import com.example.domain.ScheduleRecommendation
import com.example.domain.Semester
import com.example.domain.TimetableFormatters
import com.example.domain.TimetableIntelligenceEngine
import com.example.domain.WorkloadSummary
import com.example.domain.repository.RecommendationAiRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class SampleRecommendationAiRepository : RecommendationAiRepository {
    override suspend fun generateScheduleRecommendations(
        faculty: FacultyMember,
        semester: Semester,
        facultySessions: List<CourseSession>,
        allSemesterSessions: List<CourseSession>,
        conflicts: List<ScheduleConflict>,
        workload: WorkloadSummary,
        historicalExperiences: List<HistoricalExperience>
    ): AiRecommendationResult = withContext(Dispatchers.Default) {
        delay(180)

        val recommendations = mutableListOf<ScheduleRecommendation>()
        val usedSessionIds = mutableSetOf<String>()

        for (conflict in conflicts) {
            val targetId = conflict.secondarySessionId ?: conflict.primarySessionId
            val targetSession = facultySessions.firstOrNull { it.id == targetId }
                ?: facultySessions.firstOrNull { it.id == conflict.primarySessionId }
                ?: continue
            if (!usedSessionIds.add(targetSession.id)) continue

            val suggestedDay = conflict.suggestedDayOfWeek
            val suggestedStart = conflict.suggestedStartMinute
            val suggestedRoom = conflict.suggestedRoomCode
            val suggestedBuilding = conflict.suggestedBuilding

            if (suggestedDay != null && suggestedStart != null && suggestedRoom != null && suggestedBuilding != null) {
                val matchingHist = historicalExperiences.firstOrNull {
                    it.courseCode.equals(targetSession.courseCode, ignoreCase = true)
                }
                val precedent = matchingHist?.lessonLearnedNote
                    ?: "Historical peer schedules show ${TimetableFormatters.formatSlotLabel(suggestedDay, suggestedStart)} in $suggestedRoom maintains zero room contention."

                recommendations += ScheduleRecommendation(
                    id = "rec_conf_${conflict.id}",
                    title = "Relocate ${targetSession.courseCode} (${targetSession.section}) to ${TimetableFormatters.formatSlotLabel(suggestedDay, suggestedStart)}",
                    rationale = "Resolves '${conflict.title}' and keeps your schedule inside your ${TimetableFormatters.formatHourShort(faculty.preferredStartHour)}–${TimetableFormatters.formatHourShort(faculty.preferredEndHour)} preference window.",
                    historicalPrecedent = precedent,
                    impactBadge = if (conflict.type.isSevere) "Resolves Severe Conflict" else "Improves Transit & Flow",
                    confidencePercent = 96,
                    category = RecommendationCategory.CONFLICT_RESOLUTION,
                    targetSessionId = targetSession.id,
                    targetCourseCode = targetSession.courseCode,
                    proposedDayOfWeek = suggestedDay,
                    proposedStartMinute = suggestedStart,
                    proposedRoomCode = suggestedRoom,
                    proposedBuilding = suggestedBuilding
                )
            }
        }

        for (session in facultySessions) {
            if (session.isLocked || usedSessionIds.contains(session.id)) continue

            val bestHist = historicalExperiences
                .filter {
                    it.courseCode.equals(session.courseCode, ignoreCase = true) &&
                        it.sessionType.equals(session.sessionType, ignoreCase = true)
                }
                .maxByOrNull { it.satisfactionScore * 10 + it.attendanceRate }

            if (bestHist != null &&
                (bestHist.dayOfWeek != session.dayOfWeek || bestHist.startMinuteOfDay != session.startMinuteOfDay)
            ) {
                val bestSlot = TimetableIntelligenceEngine.findBestSlotForSession(
                    session = session,
                    faculty = faculty,
                    currentFacultySessions = facultySessions.filter { it.id != session.id },
                    allSemesterSessions = allSemesterSessions.filter { it.id != session.id },
                    historicalExperiences = historicalExperiences
                )
                if (bestSlot != null &&
                    (bestSlot.first != session.dayOfWeek || bestSlot.second != session.startMinuteOfDay)
                ) {
                    usedSessionIds.add(session.id)
                    recommendations += ScheduleRecommendation(
                        id = "rec_hist_${session.id}",
                        title = "Align ${session.courseCode} with ${bestHist.semesterName} peak slot (${TimetableFormatters.formatSlotLabel(bestSlot.first, bestSlot.second)})",
                        rationale = "Historical experience from ${bestHist.semesterName} achieved ${bestHist.attendanceRate}% attendance and ${bestHist.satisfactionScore}/5.0 faculty rating in a similar block.",
                        historicalPrecedent = bestHist.lessonLearnedNote,
                        impactBadge = "+${bestHist.attendanceRate}% Hist. Attendance",
                        confidencePercent = 91,
                        category = RecommendationCategory.HISTORICAL_OPTIMIZATION,
                        targetSessionId = session.id,
                        targetCourseCode = session.courseCode,
                        proposedDayOfWeek = bestSlot.first,
                        proposedStartMinute = bestSlot.second,
                        proposedRoomCode = bestSlot.third.first,
                        proposedBuilding = bestSlot.third.second
                    )
                }
            }
        }

        val peakDayStat = workload.dailyStats.maxByOrNull { it.totalHours }
        val lightestDayStat = workload.dailyStats.minByOrNull { it.totalHours }
        if (peakDayStat != null && lightestDayStat != null &&
            peakDayStat.totalHours - lightestDayStat.totalHours >= 2.5
        ) {
            val movablePeak = facultySessions.firstOrNull {
                it.dayOfWeek == peakDayStat.dayOfWeek && !it.isLocked && !usedSessionIds.contains(it.id)
            }
            if (movablePeak != null) {
                val bestSlot = TimetableIntelligenceEngine.findBestSlotForSession(
                    session = movablePeak,
                    faculty = faculty,
                    currentFacultySessions = facultySessions.filter { it.id != movablePeak.id },
                    allSemesterSessions = allSemesterSessions.filter { it.id != movablePeak.id },
                    historicalExperiences = historicalExperiences
                )
                if (bestSlot != null && bestSlot.first != movablePeak.dayOfWeek) {
                    usedSessionIds.add(movablePeak.id)
                    recommendations += ScheduleRecommendation(
                        id = "rec_load_${movablePeak.id}",
                        title = "Rebalance ${peakDayStat.shortName} peak by moving ${movablePeak.courseCode} to ${TimetableFormatters.dayShort(bestSlot.first)}",
                        rationale = "${peakDayStat.fullName} currently carries ${String.format("%.1f", peakDayStat.totalHours)}h while ${lightestDayStat.fullName} has ${String.format("%.1f", lightestDayStat.totalHours)}h. Shifting reduces daily teaching fatigue.",
                        historicalPrecedent = "Evenly distributed 2–3h teaching days historically reduced late-semester grading backlog.",
                        impactBadge = "Balances Weekly Load",
                        confidencePercent = 88,
                        category = RecommendationCategory.WORKLOAD_BALANCE,
                        targetSessionId = movablePeak.id,
                        targetCourseCode = movablePeak.courseCode,
                        proposedDayOfWeek = bestSlot.first,
                        proposedStartMinute = bestSlot.second,
                        proposedRoomCode = bestSlot.third.first,
                        proposedBuilding = bestSlot.third.second
                    )
                }
            }
        }

        val summary = when {
            facultySessions.isEmpty() ->
                "${semester.name} currently has no assigned sessions. Use the New Semester Auto-Preparer below to draft a conflict-free timetable from your historical semesters."
            conflicts.isNotEmpty() ->
                "Detected ${conflicts.size} scheduling issue(s) in ${semester.name} (Balance Score: ${workload.balanceScore}/100). Applying the top historical recommendations below will restore conflict-free flow."
            else ->
                "${semester.name} is conflict-free with a ${workload.balanceScore}/100 workload balance score across ${String.format("%.1f", workload.totalWeeklyHours)} weekly hours."
        }

        AiRecommendationResult(
            executiveSummary = summary,
            recommendations = recommendations,
            isSampleMode = true,
            noticeMessage = "Sample AI mode: Enable Firebase AI Logic in project settings to synthesize live Gemini scheduling recommendations."
        )
    }
}
