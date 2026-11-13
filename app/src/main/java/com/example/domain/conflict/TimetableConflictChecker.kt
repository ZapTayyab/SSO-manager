package com.example.domain.conflict

import androidx.compose.runtime.Immutable
import com.example.domain.ConflictType
import com.example.domain.CourseSession
import com.example.domain.ScheduleConflict
import com.example.domain.TimetableFormatters

@Immutable
data class OverlapCheckResult(
    val hasTimeOverlap: Boolean,
    val hasVenueClash: Boolean,
    val overlappingTeachingSessions: List<CourseSession>,
    val overlappingVenueBookings: List<CourseSession>,
    val warningTitle: String,
    val warningMessage: String
) {
    val hasConflict: Boolean
        get() = hasTimeOverlap || hasVenueClash
}

@Immutable
data class ScheduleConflictSummary(
    val teachingOverlapPairs: List<Pair<CourseSession, CourseSession>>,
    val venueClashPairs: List<Pair<CourseSession, CourseSession>>,
    val warningTitle: String,
    val warningMessage: String
) {
    val hasConflict: Boolean
        get() = teachingOverlapPairs.isNotEmpty() || venueClashPairs.isNotEmpty()

    val totalCount: Int
        get() = teachingOverlapPairs.size + venueClashPairs.size
}

/**
 * Pure-Kotlin logic module dedicated to detecting overlapping teaching times
 * for a faculty member and double-booked campus venues across a semester.
 */
object TimetableConflictChecker {

    fun doTimeIntervalsOverlap(
        dayA: Int,
        startA: Int,
        endA: Int,
        dayB: Int,
        startB: Int,
        endB: Int
    ): Boolean {
        if (dayA != dayB) return false
        return startA < endB && startB < endA
    }

    fun isSameVenue(
        roomA: String,
        buildingA: String,
        roomB: String,
        buildingB: String
    ): Boolean {
        val normRoomA = roomA.trim()
        val normRoomB = roomB.trim()
        if (normRoomA.isEmpty() || normRoomB.isEmpty()) return false
        if (!normRoomA.equals(normRoomB, ignoreCase = true)) return false

        val normBldgA = buildingA.trim()
        val normBldgB = buildingB.trim()
        return normBldgA.isEmpty() || normBldgB.isEmpty() || normBldgA.equals(normBldgB, ignoreCase = true)
    }

    fun findOverlappingTeachingSessions(
        candidate: CourseSession,
        facultySessions: List<CourseSession>
    ): List<CourseSession> {
        val candidateEnd = candidate.endMinuteOfDay()
        return facultySessions.filter { other ->
            other.id != candidate.id &&
                other.semesterId == candidate.semesterId &&
                doTimeIntervalsOverlap(
                    dayA = candidate.dayOfWeek,
                    startA = candidate.startMinuteOfDay,
                    endA = candidateEnd,
                    dayB = other.dayOfWeek,
                    startB = other.startMinuteOfDay,
                    endB = other.endMinuteOfDay()
                )
        }
    }

    fun findOverlappingVenueBookings(
        candidate: CourseSession,
        allSemesterSessions: List<CourseSession>
    ): List<CourseSession> {
        val candidateEnd = candidate.endMinuteOfDay()
        return allSemesterSessions.filter { other ->
            other.id != candidate.id &&
                other.semesterId == candidate.semesterId &&
                isSameVenue(
                    roomA = candidate.roomCode,
                    buildingA = candidate.building,
                    roomB = other.roomCode,
                    buildingB = other.building
                ) &&
                doTimeIntervalsOverlap(
                    dayA = candidate.dayOfWeek,
                    startA = candidate.startMinuteOfDay,
                    endA = candidateEnd,
                    dayB = other.dayOfWeek,
                    startB = other.startMinuteOfDay,
                    endB = other.endMinuteOfDay()
                )
        }
    }

    fun checkCandidateSession(
        candidate: CourseSession,
        facultySessions: List<CourseSession>,
        allSemesterSessions: List<CourseSession>
    ): OverlapCheckResult {
        val timeOverlaps = findOverlappingTeachingSessions(candidate, facultySessions)
        val venueClashes = findOverlappingVenueBookings(candidate, allSemesterSessions)

        val hasTimeOverlap = timeOverlaps.isNotEmpty()
        val hasVenueClash = venueClashes.isNotEmpty()

        val dayLabel = TimetableFormatters.dayShort(candidate.dayOfWeek)
        val timeLabel = TimetableFormatters.formatTimeRange(
            candidate.startMinuteOfDay,
            candidate.durationMinutes
        )

        val (title, message) = when {
            hasTimeOverlap && hasVenueClash -> {
                val timeCourses = timeOverlaps.joinToString(", ") { it.courseCode }
                val venueCourses = venueClashes.joinToString(", ") { it.courseCode }
                "Schedule & Venue Conflict" to
                    "Warning: ${candidate.courseCode} ($dayLabel $timeLabel) overlaps teaching time with $timeCourses and double-books ${candidate.roomCode} with $venueCourses."
            }
            hasTimeOverlap -> {
                val timeCourses = timeOverlaps.joinToString(", ") {
                    "${it.courseCode} (${TimetableFormatters.formatTimeRange(it.startMinuteOfDay, it.durationMinutes)})"
                }
                "Overlapping Teaching Time" to
                    "Warning: ${candidate.courseCode} on $dayLabel ($timeLabel) overlaps with $timeCourses."
            }
            hasVenueClash -> {
                val venueCourses = venueClashes.joinToString(", ") { it.courseCode }
                "Venue Double-Booking" to
                    "Warning: Venue ${candidate.roomCode} (${candidate.building}) on $dayLabel ($timeLabel) is already booked for $venueCourses."
            }
            else -> "" to ""
        }

        return OverlapCheckResult(
            hasTimeOverlap = hasTimeOverlap,
            hasVenueClash = hasVenueClash,
            overlappingTeachingSessions = timeOverlaps,
            overlappingVenueBookings = venueClashes,
            warningTitle = title,
            warningMessage = message
        )
    }

    fun checkScheduleConflicts(
        facultySessions: List<CourseSession>,
        allSemesterSessions: List<CourseSession>
    ): ScheduleConflictSummary {
        val teachingPairs = mutableListOf<Pair<CourseSession, CourseSession>>()
        val sortedMine = facultySessions.sortedWith(
            compareBy<CourseSession> { it.dayOfWeek }.thenBy { it.startMinuteOfDay }
        )

        for (i in sortedMine.indices) {
            val a = sortedMine[i]
            for (j in i + 1 until sortedMine.size) {
                val b = sortedMine[j]
                if (a.semesterId == b.semesterId &&
                    doTimeIntervalsOverlap(
                        dayA = a.dayOfWeek,
                        startA = a.startMinuteOfDay,
                        endA = a.endMinuteOfDay(),
                        dayB = b.dayOfWeek,
                        startB = b.startMinuteOfDay,
                        endB = b.endMinuteOfDay()
                    )
                ) {
                    teachingPairs += a to b
                }
            }
        }

        val venuePairs = mutableListOf<Pair<CourseSession, CourseSession>>()
        val seenVenueKeys = mutableSetOf<String>()
        for (mine in sortedMine) {
            val clashes = findOverlappingVenueBookings(mine, allSemesterSessions)
            for (other in clashes) {
                val pairKey = listOf(mine.id, other.id).sorted().joinToString("_")
                if (seenVenueKeys.add(pairKey)) {
                    venuePairs += mine to other
                }
            }
        }

        val (title, message) = when {
            teachingPairs.isNotEmpty() && venuePairs.isNotEmpty() -> {
                val firstTime = teachingPairs.first()
                val firstVenue = venuePairs.first()
                "Timetable Conflicts Detected (${teachingPairs.size + venuePairs.size})" to
                    "Warning: ${firstTime.first.courseCode} overlaps ${firstTime.second.courseCode}, and ${firstVenue.first.roomCode} is double-booked with ${firstVenue.second.courseCode}."
            }
            teachingPairs.isNotEmpty() -> {
                val first = teachingPairs.first()
                val extra = if (teachingPairs.size > 1) " (+${teachingPairs.size - 1} more)" else ""
                "Teaching Time Overlap Detected" to
                    "Warning: ${first.first.courseCode} overlaps ${first.second.courseCode} on ${TimetableFormatters.dayShort(first.first.dayOfWeek)}$extra."
            }
            venuePairs.isNotEmpty() -> {
                val first = venuePairs.first()
                val extra = if (venuePairs.size > 1) " (+${venuePairs.size - 1} more)" else ""
                "Venue Double-Booking Detected" to
                    "Warning: ${first.first.roomCode} booked for both ${first.first.courseCode} and ${first.second.courseCode} on ${TimetableFormatters.dayShort(first.first.dayOfWeek)}$extra."
            }
            else -> "" to ""
        }

        return ScheduleConflictSummary(
            teachingOverlapPairs = teachingPairs,
            venueClashPairs = venuePairs,
            warningTitle = title,
            warningMessage = message
        )
    }

    fun summarizeDetectedConflicts(conflicts: List<ScheduleConflict>): Pair<String, String>? {
        val severe = conflicts.filter { it.type.isSevere }
        if (severe.isEmpty()) return null
        val timeCount = severe.count { it.type == ConflictType.FACULTY_TIME_OVERLAP }
        val venueCount = severe.count { it.type == ConflictType.ROOM_DOUBLE_BOOKED }
        val title = when {
            timeCount > 0 && venueCount > 0 -> "Time & Venue Conflicts (${severe.size})"
            timeCount > 0 -> "Teaching Time Overlap (${timeCount})"
            else -> "Venue Booking Conflict (${venueCount})"
        }
        val first = severe.first()
        val extra = if (severe.size > 1) " (+${severe.size - 1} more)" else ""
        return title to "Warning: ${first.title}$extra. Tap Auto-Resolve to fix."
    }
}
