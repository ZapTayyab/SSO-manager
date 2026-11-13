package com.example.domain

import com.example.domain.conflict.TimetableConflictChecker
import java.util.UUID
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

object TimetableIntelligenceEngine {

    private val campusRooms = listOf(
        "Turing 204" to "Engineering West",
        "Turing 301" to "Engineering West",
        "Turing 110" to "Engineering West",
        "Turing 215" to "Engineering West",
        "Ada Lab 102" to "Engineering West",
        "Ada Lab 104" to "Science Commons",
        "Euler 101" to "Science Commons",
        "Hub 215" to "Humanities Hall"
    )

    private val candidateStartMinutes = listOf(
        8 * 60 + 30,
        9 * 60 + 30,
        10 * 60,
        11 * 60,
        13 * 60,
        14 * 60,
        15 * 60 + 30
    )

    fun detectConflicts(
        faculty: FacultyMember,
        facultySessions: List<CourseSession>,
        allSemesterSessions: List<CourseSession>,
        historicalExperiences: List<HistoricalExperience> = emptyList()
    ): List<ScheduleConflict> {
        val conflicts = mutableListOf<ScheduleConflict>()
        val byDay = facultySessions.groupBy { it.dayOfWeek }

        for ((day, daySessions) in byDay) {
            val sorted = daySessions.sortedBy { it.startMinuteOfDay }
            for (i in sorted.indices) {
                val a = sorted[i]
                for (j in i + 1 until sorted.size) {
                    val b = sorted[j]
                    val overlaps = TimetableConflictChecker.doTimeIntervalsOverlap(
                        dayA = a.dayOfWeek,
                        startA = a.startMinuteOfDay,
                        endA = a.endMinuteOfDay(),
                        dayB = b.dayOfWeek,
                        startB = b.startMinuteOfDay,
                        endB = b.endMinuteOfDay()
                    )
                    if (overlaps) {
                        val movable = if (a.isLocked && !b.isLocked) b else if (b.isLocked && !a.isLocked) a else b
                        val suggestion = findBestSlotForSession(
                            session = movable,
                            faculty = faculty,
                            currentFacultySessions = facultySessions.filter { it.id != movable.id },
                            allSemesterSessions = allSemesterSessions.filter { it.id != movable.id },
                            historicalExperiences = historicalExperiences
                        )
                        conflicts += ScheduleConflict(
                            id = "overlap_${a.id}_${b.id}",
                            type = ConflictType.FACULTY_TIME_OVERLAP,
                            primarySessionId = a.id,
                            secondarySessionId = b.id,
                            title = "${a.courseCode} overlaps ${b.courseCode}",
                            description = "${TimetableFormatters.dayShort(day)}: ${a.courseCode} (${TimetableFormatters.formatTimeRange(a.startMinuteOfDay, a.durationMinutes)}) collides with ${b.courseCode} (${TimetableFormatters.formatTimeRange(b.startMinuteOfDay, b.durationMinutes)}).",
                            dayOfWeek = day,
                            suggestedDayOfWeek = suggestion?.first,
                            suggestedStartMinute = suggestion?.second,
                            suggestedRoomCode = suggestion?.third?.first,
                            suggestedBuilding = suggestion?.third?.second
                        )
                    } else if (faculty.avoidBackToBackBuildings) {
                        val gapMinutes = b.startMinuteOfDay - a.endMinuteOfDay()
                        if (gapMinutes in 0..14 && !a.building.equals(b.building, ignoreCase = true)) {
                            val movable = if (!b.isLocked) b else a
                            val suggestion = findBestSlotForSession(
                                session = movable,
                                faculty = faculty,
                                currentFacultySessions = facultySessions.filter { it.id != movable.id },
                                allSemesterSessions = allSemesterSessions.filter { it.id != movable.id },
                                historicalExperiences = historicalExperiences
                            )
                            conflicts += ScheduleConflict(
                                id = "transit_${a.id}_${b.id}",
                                type = ConflictType.BUILDING_TRANSIT_GAP,
                                primarySessionId = a.id,
                                secondarySessionId = b.id,
                                title = "Tight ${gapMinutes}m transit: ${a.building} → ${b.building}",
                                description = "${a.courseCode} ends ${TimetableFormatters.formatMinuteOfDay(a.endMinuteOfDay())} in ${a.building}, leaving only $gapMinutes min before ${b.courseCode} in ${b.building}.",
                                dayOfWeek = day,
                                suggestedDayOfWeek = suggestion?.first,
                                suggestedStartMinute = suggestion?.second,
                                suggestedRoomCode = suggestion?.third?.first,
                                suggestedBuilding = suggestion?.third?.second
                            )
                        }
                    }
                }
            }

            val dailyHours = daySessions.sumOf { it.durationMinutes } / 60.0
            if (dailyHours > 5.5 && daySessions.size >= 3) {
                val lastMovable = daySessions.lastOrNull { !it.isLocked } ?: daySessions.last()
                val suggestion = findBestSlotForSession(
                    session = lastMovable,
                    faculty = faculty,
                    currentFacultySessions = facultySessions.filter { it.id != lastMovable.id },
                    allSemesterSessions = allSemesterSessions.filter { it.id != lastMovable.id },
                    historicalExperiences = historicalExperiences
                )
                conflicts += ScheduleConflict(
                    id = "cap_$day",
                    type = ConflictType.DAILY_LOAD_EXCEEDED,
                    primarySessionId = lastMovable.id,
                    secondarySessionId = null,
                    title = "${TimetableFormatters.dayFull(day)} overloaded (${String.format("%.1f", dailyHours)}h)",
                    description = "Teaching ${String.format("%.1f", dailyHours)} hours on ${TimetableFormatters.dayFull(day)} increases fatigue; consider shifting ${lastMovable.courseCode}.",
                    dayOfWeek = day,
                    suggestedDayOfWeek = suggestion?.first,
                    suggestedStartMinute = suggestion?.second,
                    suggestedRoomCode = suggestion?.third?.first,
                    suggestedBuilding = suggestion?.third?.second
                )
            }
        }

        for (mine in facultySessions) {
            val roomClashes = TimetableConflictChecker.findOverlappingVenueBookings(
                candidate = mine,
                allSemesterSessions = allSemesterSessions
            )
            for (other in roomClashes) {
                if (other.facultyId == mine.facultyId) continue
                val suggestion = findBestSlotForSession(
                    session = mine,
                    faculty = faculty,
                    currentFacultySessions = facultySessions.filter { it.id != mine.id },
                    allSemesterSessions = allSemesterSessions.filter { it.id != mine.id },
                    historicalExperiences = historicalExperiences
                )
                conflicts += ScheduleConflict(
                    id = "room_${mine.id}_${other.id}",
                    type = ConflictType.ROOM_DOUBLE_BOOKED,
                    primarySessionId = mine.id,
                    secondarySessionId = other.id,
                    title = "Room ${mine.roomCode} double-booked",
                    description = "${mine.courseCode} conflicts with ${other.courseCode} in ${mine.roomCode} on ${TimetableFormatters.dayShort(mine.dayOfWeek)}.",
                    dayOfWeek = mine.dayOfWeek,
                    suggestedDayOfWeek = suggestion?.first,
                    suggestedStartMinute = suggestion?.second,
                    suggestedRoomCode = suggestion?.third?.first,
                    suggestedBuilding = suggestion?.third?.second
                )
            }
        }

        val prefStartMin = faculty.preferredStartHour * 60
        val prefEndMin = faculty.preferredEndHour * 60
        for (session in facultySessions) {
            if (session.startMinuteOfDay < prefStartMin || session.endMinuteOfDay() > prefEndMin) {
                val suggestion = findBestSlotForSession(
                    session = session,
                    faculty = faculty,
                    currentFacultySessions = facultySessions.filter { it.id != session.id },
                    allSemesterSessions = allSemesterSessions.filter { it.id != session.id },
                    historicalExperiences = historicalExperiences
                )
                conflicts += ScheduleConflict(
                    id = "pref_${session.id}",
                    type = ConflictType.OUTSIDE_PREFERRED_HOURS,
                    primarySessionId = session.id,
                    secondarySessionId = null,
                    title = "${session.courseCode} outside preferred hours",
                    description = "Scheduled at ${TimetableFormatters.formatTimeRange(session.startMinuteOfDay, session.durationMinutes)}, outside your ${TimetableFormatters.formatHourShort(faculty.preferredStartHour)}–${TimetableFormatters.formatHourShort(faculty.preferredEndHour)} window.",
                    dayOfWeek = session.dayOfWeek,
                    suggestedDayOfWeek = suggestion?.first,
                    suggestedStartMinute = suggestion?.second,
                    suggestedRoomCode = suggestion?.third?.first,
                    suggestedBuilding = suggestion?.third?.second
                )
            }
        }

        return conflicts
    }

    fun computeFacultyWeeklyBreakdown(
        allFaculty: List<FacultyMember>,
        allSemesterSessions: List<CourseSession>,
        activeFacultyId: String
    ): List<FacultyWeeklyBreakdown> {
        val byFaculty = allSemesterSessions.groupBy { it.facultyId }
        return allFaculty.map { member ->
            val memberSessions = byFaculty[member.id].orEmpty()
            val lectureHours = memberSessions
                .filter { SessionType.fromString(it.sessionType) == SessionType.LECTURE }
                .sumOf { it.durationMinutes } / 60.0
            val labHours = memberSessions
                .filter { SessionType.fromString(it.sessionType) == SessionType.LAB }
                .sumOf { it.durationMinutes } / 60.0
            val seminarHours = memberSessions
                .filter { SessionType.fromString(it.sessionType) == SessionType.SEMINAR }
                .sumOf { it.durationMinutes } / 60.0
            val officeHours = memberSessions
                .filter { SessionType.fromString(it.sessionType) == SessionType.OFFICE_HOURS }
                .sumOf { it.durationMinutes } / 60.0
            val totalHours = lectureHours + labHours + seminarHours + officeHours
            val utilization = if (member.maxWeeklyHours > 0) {
                ((totalHours / member.maxWeeklyHours.toDouble()) * 100.0).roundToInt()
            } else {
                0
            }
            val status = when {
                totalHours > member.maxWeeklyHours * 0.95 -> WorkloadLoadStatus.OVERLOADED
                totalHours < member.maxWeeklyHours * 0.50 -> WorkloadLoadStatus.UNDERLOADED
                else -> WorkloadLoadStatus.BALANCED
            }
            val tokens = member.fullName.trim().split(" ").filter { it.isNotBlank() }
            val shortLabel = when {
                tokens.size >= 2 -> "${tokens.first()} ${tokens.last()}"
                tokens.isNotEmpty() -> tokens.first()
                else -> member.ssoId
            }
            FacultyWeeklyBreakdown(
                facultyId = member.id,
                fullName = member.fullName,
                shortLabel = shortLabel,
                department = member.department,
                lectureHours = lectureHours,
                labHours = labHours,
                seminarHours = seminarHours,
                officeHours = officeHours,
                totalWeeklyHours = totalHours,
                targetMaxHours = member.maxWeeklyHours,
                sessionCount = memberSessions.size,
                utilizationPercent = utilization,
                loadStatus = status,
                isCurrentUser = member.id == activeFacultyId
            )
        }.sortedByDescending { it.totalWeeklyHours }
    }

    fun computeWorkload(
        faculty: FacultyMember,
        facultySessions: List<CourseSession>,
        conflicts: List<ScheduleConflict>,
        allFaculty: List<FacultyMember> = listOf(faculty),
        allSemesterSessions: List<CourseSession> = facultySessions
    ): WorkloadSummary {
        val dailyStats = (1..5).map { day ->
            val daySessions = facultySessions.filter { it.dayOfWeek == day }
            val hours = daySessions.sumOf { it.durationMinutes } / 60.0
            val hasSevere = conflicts.any { it.dayOfWeek == day && it.type.isSevere }
            DailyWorkloadStat(
                dayOfWeek = day,
                shortName = TimetableFormatters.dayShort(day),
                fullName = TimetableFormatters.dayFull(day),
                totalHours = hours,
                sessionCount = daySessions.size,
                hasSevereConflict = hasSevere,
                isOverloaded = hours > 4.5
            )
        }

        val totalHours = dailyStats.sumOf { it.totalHours }
        val meanDaily = totalHours / 5.0
        val variance = dailyStats.sumOf { (it.totalHours - meanDaily) * (it.totalHours - meanDaily) } / 5.0
        val stdDev = sqrt(variance)

        val severePenalty = conflicts.count { it.type.isSevere } * 18
        val softPenalty = conflicts.count { !it.type.isSevere } * 7
        val spreadPenalty = (stdDev * 14).roundToInt()
        val overloadPenalty = if (totalHours > faculty.maxWeeklyHours) {
            ((totalHours - faculty.maxWeeklyHours) * 8).roundToInt()
        } else 0

        val rawScore = if (facultySessions.isEmpty()) 100 else (100 - severePenalty - softPenalty - spreadPenalty - overloadPenalty)
        val balanceScore = rawScore.coerceIn(15, 100)

        val peak = dailyStats.maxByOrNull { it.totalHours }
        val totalStudents = facultySessions
            .distinctBy { "${it.courseCode}_${it.section}" }
            .sumOf { it.enrolledStudents }

        val courseCount = facultySessions.map { it.courseCode }.distinct().size
        val breakdowns = computeFacultyWeeklyBreakdown(
            allFaculty = allFaculty,
            allSemesterSessions = allSemesterSessions,
            activeFacultyId = faculty.id
        )
        val activeFacultyLoads = breakdowns.filter { it.totalWeeklyHours > 0.0 }
        val deptAvg = if (activeFacultyLoads.isNotEmpty()) {
            activeFacultyLoads.map { it.totalWeeklyHours }.average()
        } else {
            (faculty.maxWeeklyHours - 1.5).coerceAtLeast(10.0)
        }

        return WorkloadSummary(
            totalWeeklyHours = totalHours,
            targetMaxHours = faculty.maxWeeklyHours,
            balanceScore = balanceScore,
            totalStudents = totalStudents,
            courseCount = courseCount,
            peakDayLabel = peak?.fullName ?: "Monday",
            peakDayHours = peak?.totalHours ?: 0.0,
            departmentAverageHours = deptAvg,
            dailyStats = dailyStats,
            facultyBreakdowns = breakdowns
        )
    }

    fun findBestSlotForSession(
        session: CourseSession,
        faculty: FacultyMember,
        currentFacultySessions: List<CourseSession>,
        allSemesterSessions: List<CourseSession>,
        historicalExperiences: List<HistoricalExperience>,
        goal: OptimizationGoal = OptimizationGoal.BALANCE_WORKLOAD
    ): Triple<Int, Int, Pair<String, String>>? {
        val prefStart = faculty.preferredStartHour * 60
        val prefEnd = faculty.preferredEndHour * 60

        val roomOptions = buildList {
            add(session.roomCode to session.building)
            campusRooms.forEach { if (it.first != session.roomCode) add(it) }
        }

        val matchingHistory = historicalExperiences.filter {
            it.courseCode.equals(session.courseCode, ignoreCase = true) ||
                it.sessionType.equals(session.sessionType, ignoreCase = true)
        }

        var bestScore = Double.NEGATIVE_INFINITY
        var bestResult: Triple<Int, Int, Pair<String, String>>? = null

        for (day in 1..5) {
            val dayMine = currentFacultySessions.filter { it.dayOfWeek == day }
            val dayHours = dayMine.sumOf { it.durationMinutes } / 60.0

            for (startMin in candidateStartMinutes) {
                val endMin = startMin + session.durationMinutes
                if (startMin < prefStart || endMin > prefEnd) continue

                val facultyOverlap = dayMine.any { other ->
                    TimetableConflictChecker.doTimeIntervalsOverlap(
                        dayA = day,
                        startA = startMin,
                        endA = endMin,
                        dayB = other.dayOfWeek,
                        startB = other.startMinuteOfDay,
                        endB = other.endMinuteOfDay()
                    )
                }
                if (facultyOverlap) continue

                for ((room, building) in roomOptions) {
                    val roomTaken = allSemesterSessions.any { other ->
                        TimetableConflictChecker.isSameVenue(room, building, other.roomCode, other.building) &&
                            TimetableConflictChecker.doTimeIntervalsOverlap(
                                dayA = day,
                                startA = startMin,
                                endA = endMin,
                                dayB = other.dayOfWeek,
                                startB = other.startMinuteOfDay,
                                endB = other.endMinuteOfDay()
                            )
                    }
                    if (roomTaken) continue

                    var score = 100.0

                    if (faculty.avoidBackToBackBuildings) {
                        val tightTransit = dayMine.any { other ->
                            val gap1 = startMin - other.endMinuteOfDay()
                            val gap2 = other.startMinuteOfDay - endMin
                            val isTight = (gap1 in 0..14) || (gap2 in 0..14)
                            isTight && !other.building.equals(building, ignoreCase = true)
                        }
                        if (tightTransit) score -= 45.0
                    }

                    if (dayMine.any { it.building.equals(building, ignoreCase = true) }) {
                        score += 12.0
                    }
                    if (room == session.roomCode && building == session.building) {
                        score += 8.0
                    }

                    val projectedHours = dayHours + session.durationMinutes / 60.0
                    score -= when (goal) {
                        OptimizationGoal.BALANCE_WORKLOAD -> projectedHours * 9.0
                        OptimizationGoal.MINIMIZE_CONFLICTS -> projectedHours * 5.0
                        OptimizationGoal.MORNING_PRIORITY -> projectedHours * 5.0
                    }

                    if ((faculty.preferMorningLectures || goal == OptimizationGoal.MORNING_PRIORITY) &&
                        session.sessionType != "OFFICE_HOURS"
                    ) {
                        if (startMin <= 11 * 60) score += 18.0
                        else if (startMin >= 14 * 60) score -= 10.0
                    }

                    val histMatch = matchingHistory.firstOrNull {
                        it.dayOfWeek == day && abs(it.startMinuteOfDay - startMin) <= 60
                    }
                    if (histMatch != null) {
                        score += (histMatch.satisfactionScore * 6.0) + (histMatch.attendanceRate * 0.15)
                    }
                    if (matchingHistory.any { it.roomCode.equals(room, ignoreCase = true) && it.satisfactionScore >= 4.5 }) {
                        score += 10.0
                    }

                    if (day == session.dayOfWeek && startMin == session.startMinuteOfDay && room == session.roomCode) {
                        score -= 5.0
                    }

                    if (score > bestScore) {
                        bestScore = score
                        bestResult = Triple(day, startMin, room to building)
                    }
                }
            }
        }
        return bestResult
    }

    data class AutoResolveOutcome(
        val updatedSessions: List<CourseSession>,
        val changesDescriptions: List<String>,
        val resolvedCount: Int
    )

    fun autoResolveConflicts(
        faculty: FacultyMember,
        facultySessions: List<CourseSession>,
        allSemesterSessions: List<CourseSession>,
        historicalExperiences: List<HistoricalExperience>,
        goal: OptimizationGoal = OptimizationGoal.BALANCE_WORKLOAD
    ): AutoResolveOutcome {
        val workingMine = facultySessions.toMutableList()
        val otherFacultySessions = allSemesterSessions.filter { it.facultyId != faculty.id }
        val changes = mutableListOf<String>()

        repeat(5) {
            val currentConflicts = detectConflicts(
                faculty = faculty,
                facultySessions = workingMine,
                allSemesterSessions = workingMine + otherFacultySessions,
                historicalExperiences = historicalExperiences
            )
            if (currentConflicts.isEmpty()) return@repeat

            val conflict = currentConflicts.firstOrNull() ?: return@repeat
            val candidateIds = listOfNotNull(conflict.secondarySessionId, conflict.primarySessionId)
            val targetIdx = candidateIds
                .map { id -> workingMine.indexOfFirst { it.id == id } }
                .firstOrNull { idx -> idx >= 0 && !workingMine[idx].isLocked }
                ?: candidateIds.map { id -> workingMine.indexOfFirst { it.id == id } }.firstOrNull { it >= 0 }
                ?: return@repeat

            val target = workingMine[targetIdx]
            if (target.isLocked && currentConflicts.size == 1) return@repeat

            val bestSlot = findBestSlotForSession(
                session = target,
                faculty = faculty,
                currentFacultySessions = workingMine.filter { it.id != target.id },
                allSemesterSessions = (workingMine.filter { it.id != target.id }) + otherFacultySessions,
                historicalExperiences = historicalExperiences,
                goal = goal
            ) ?: return@repeat

            val oldLabel = TimetableFormatters.formatSlotLabel(target.dayOfWeek, target.startMinuteOfDay)
            val newLabel = TimetableFormatters.formatSlotLabel(bestSlot.first, bestSlot.second)

            if (bestSlot.first != target.dayOfWeek ||
                bestSlot.second != target.startMinuteOfDay ||
                bestSlot.third.first != target.roomCode
            ) {
                workingMine[targetIdx] = target.copy(
                    dayOfWeek = bestSlot.first,
                    startMinuteOfDay = bestSlot.second,
                    roomCode = bestSlot.third.first,
                    building = bestSlot.third.second,
                    sourceNote = "Auto-updated to resolve ${conflict.type.label.lowercase()}",
                    updatedAtMillis = System.currentTimeMillis()
                )
                changes += "${target.courseCode} (${target.section}): $oldLabel → $newLabel in ${bestSlot.third.first}"
            } else {
                return@repeat
            }
        }

        return AutoResolveOutcome(
            updatedSessions = workingMine,
            changesDescriptions = changes,
            resolvedCount = changes.size
        )
    }

    fun prepareNewSemesterDraft(
        faculty: FacultyMember,
        sourceHistoricalSessions: List<CourseSession>,
        targetSemesterId: String,
        existingInstitutionSessionsInTarget: List<CourseSession>,
        historicalExperiences: List<HistoricalExperience>,
        goal: OptimizationGoal
    ): List<CourseSession> {
        val otherFacultyInTarget = existingInstitutionSessionsInTarget.filter { it.facultyId != faculty.id }
        val placedMine = mutableListOf<CourseSession>()

        val orderedSource = sourceHistoricalSessions.sortedWith(
            compareByDescending<CourseSession> { it.isLocked }
                .thenByDescending { it.sessionType == "LECTURE" }
                .thenBy { it.dayOfWeek }
        )

        for (src in orderedSource) {
            val draftTemplate = src.copy(
                id = "ses_${targetSemesterId.takeLast(4)}_${UUID.randomUUID().toString().take(6)}",
                semesterId = targetSemesterId,
                updatedAtMillis = System.currentTimeMillis()
            )

            val bestSlot = findBestSlotForSession(
                session = draftTemplate,
                faculty = faculty,
                currentFacultySessions = placedMine,
                allSemesterSessions = placedMine + otherFacultyInTarget,
                historicalExperiences = historicalExperiences,
                goal = goal
            )

            val finalSession = if (bestSlot != null && !src.isLocked) {
                draftTemplate.copy(
                    dayOfWeek = bestSlot.first,
                    startMinuteOfDay = bestSlot.second,
                    roomCode = bestSlot.third.first,
                    building = bestSlot.third.second,
                    sourceNote = "Auto-prepared from historical experience (${goal.label})"
                )
            } else {
                draftTemplate.copy(
                    sourceNote = "Rolled over from historical semester"
                )
            }
            placedMine += finalSession
        }

        return placedMine
    }
}
