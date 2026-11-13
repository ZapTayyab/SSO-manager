package com.example.domain

import androidx.compose.runtime.Immutable
import java.util.Locale

enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark")
}

@Immutable
data class SavedSsoCredential(
    val ssoId: String,
    val email: String,
    val passcode: String,
    val facultyId: String,
    val rememberCredentials: Boolean,
    val savedAtMillis: Long
)

@Immutable
data class FacultyMember(
    val id: String,
    val ssoId: String,
    val fullName: String,
    val email: String,
    val department: String,
    val academicTitle: String,
    val maxWeeklyHours: Int,
    val preferredStartHour: Int,
    val preferredEndHour: Int,
    val avoidBackToBackBuildings: Boolean,
    val preferMorningLectures: Boolean
)

@Immutable
data class Semester(
    val id: String,
    val name: String,
    val code: String,
    val status: String, // ACTIVE, DRAFT, HISTORICAL
    val dateRangeLabel: String,
    val totalWeeks: Int,
    val sortOrder: Int
)

@Immutable
data class CourseSession(
    val id: String,
    val facultyId: String,
    val semesterId: String,
    val courseCode: String,
    val courseTitle: String,
    val section: String,
    val sessionType: String, // LECTURE, LAB, SEMINAR, OFFICE_HOURS
    val dayOfWeek: Int, // 1 = Mon .. 5 = Fri
    val startMinuteOfDay: Int,
    val durationMinutes: Int,
    val roomCode: String,
    val building: String,
    val enrolledStudents: Int,
    val capacity: Int,
    val isLocked: Boolean,
    val sourceNote: String,
    val updatedAtMillis: Long
) {
    fun endMinuteOfDay(): Int = startMinuteOfDay + durationMinutes
}

@Immutable
data class HistoricalExperience(
    val id: String,
    val facultyId: String,
    val semesterName: String,
    val courseCode: String,
    val courseTitle: String,
    val sessionType: String,
    val dayOfWeek: Int,
    val startMinuteOfDay: Int,
    val durationMinutes: Int,
    val roomCode: String,
    val building: String,
    val satisfactionScore: Double,
    val attendanceRate: Int,
    val pastConflictsCount: Int,
    val lessonLearnedNote: String
)

@Immutable
data class AutoUpdateLog(
    val id: String,
    val facultyId: String,
    val semesterId: String,
    val actionTitle: String,
    val summary: String,
    val affectedCount: Int,
    val timestampMillis: Long
)

enum class SessionType(val label: String, val shortLabel: String) {
    LECTURE("Lecture", "LEC"),
    LAB("Laboratory", "LAB"),
    SEMINAR("Seminar", "SEM"),
    OFFICE_HOURS("Office Hours", "OH");

    companion object {
        fun fromString(value: String): SessionType =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: LECTURE
    }
}

enum class SemesterStatus(val label: String) {
    ACTIVE("Active"),
    DRAFT("Draft"),
    HISTORICAL("Historical");

    companion object {
        fun fromString(value: String): SemesterStatus =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: HISTORICAL
    }
}

enum class ConflictType(val label: String, val isSevere: Boolean) {
    FACULTY_TIME_OVERLAP("Time Overlap", true),
    ROOM_DOUBLE_BOOKED("Room Double-Booked", true),
    BUILDING_TRANSIT_GAP("Tight Building Transit", false),
    OUTSIDE_PREFERRED_HOURS("Outside Preferred Window", false),
    DAILY_LOAD_EXCEEDED("Daily Cap Exceeded", false)
}

@Immutable
data class ScheduleConflict(
    val id: String,
    val type: ConflictType,
    val primarySessionId: String,
    val secondarySessionId: String?,
    val title: String,
    val description: String,
    val dayOfWeek: Int,
    val suggestedDayOfWeek: Int?,
    val suggestedStartMinute: Int?,
    val suggestedRoomCode: String?,
    val suggestedBuilding: String?
)

@Immutable
data class DailyWorkloadStat(
    val dayOfWeek: Int,
    val shortName: String,
    val fullName: String,
    val totalHours: Double,
    val sessionCount: Int,
    val hasSevereConflict: Boolean,
    val isOverloaded: Boolean
)

enum class WorkloadLoadStatus(val label: String) {
    UNDERLOADED("Available Capacity"),
    BALANCED("Balanced"),
    OVERLOADED("Near / Over Cap")
}

@Immutable
data class FacultyWeeklyBreakdown(
    val facultyId: String,
    val fullName: String,
    val shortLabel: String,
    val department: String,
    val lectureHours: Double,
    val labHours: Double,
    val seminarHours: Double,
    val officeHours: Double,
    val totalWeeklyHours: Double,
    val targetMaxHours: Int,
    val sessionCount: Int,
    val utilizationPercent: Int,
    val loadStatus: WorkloadLoadStatus,
    val isCurrentUser: Boolean
)

@Immutable
data class WorkloadSummary(
    val totalWeeklyHours: Double,
    val targetMaxHours: Int,
    val balanceScore: Int,
    val totalStudents: Int,
    val courseCount: Int,
    val peakDayLabel: String,
    val peakDayHours: Double,
    val departmentAverageHours: Double,
    val dailyStats: List<DailyWorkloadStat>,
    val facultyBreakdowns: List<FacultyWeeklyBreakdown> = emptyList()
)

enum class RecommendationCategory(val label: String) {
    CONFLICT_RESOLUTION("Conflict Fix"),
    HISTORICAL_OPTIMIZATION("Historical Insight"),
    WORKLOAD_BALANCE("Workload Balance"),
    ROOM_FIT("Room Fit")
}

@Immutable
data class ScheduleRecommendation(
    val id: String,
    val title: String,
    val rationale: String,
    val historicalPrecedent: String,
    val impactBadge: String,
    val confidencePercent: Int,
    val category: RecommendationCategory,
    val targetSessionId: String,
    val targetCourseCode: String,
    val proposedDayOfWeek: Int,
    val proposedStartMinute: Int,
    val proposedRoomCode: String,
    val proposedBuilding: String
)

@Immutable
data class AiRecommendationResult(
    val executiveSummary: String,
    val recommendations: List<ScheduleRecommendation>,
    val isSampleMode: Boolean = true,
    val noticeMessage: String
)

enum class OptimizationGoal(val label: String, val description: String) {
    MINIMIZE_CONFLICTS("Zero Conflicts", "Prioritize eliminating all overlaps and room clashes"),
    BALANCE_WORKLOAD("Balanced Week", "Distribute teaching hours evenly across Monday–Friday"),
    MORNING_PRIORITY("Morning Focus", "Favor 08:30–12:30 slots with high historical attendance")
}

object TimetableFormatters {
    private val dayShortNames = mapOf(
        1 to "Mon",
        2 to "Tue",
        3 to "Wed",
        4 to "Thu",
        5 to "Fri"
    )

    private val dayFullNames = mapOf(
        1 to "Monday",
        2 to "Tuesday",
        3 to "Wednesday",
        4 to "Thursday",
        5 to "Friday"
    )

    fun dayShort(dayOfWeek: Int): String = dayShortNames[dayOfWeek] ?: "Day $dayOfWeek"

    fun dayFull(dayOfWeek: Int): String = dayFullNames[dayOfWeek] ?: "Day $dayOfWeek"

    fun formatMinuteOfDay(minuteOfDay: Int): String {
        val clamped = minuteOfDay.coerceIn(0, 23 * 60 + 59)
        val hour24 = clamped / 60
        val min = clamped % 60
        val period = if (hour24 >= 12) "PM" else "AM"
        val hour12 = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }
        return String.format(Locale.US, "%d:%02d %s", hour12, min, period)
    }

    fun formatTimeRange(startMinute: Int, durationMinutes: Int): String {
        return "${formatMinuteOfDay(startMinute)} – ${formatMinuteOfDay(startMinute + durationMinutes)}"
    }

    fun formatHourShort(hour24: Int): String {
        val period = if (hour24 >= 12) "PM" else "AM"
        val hour12 = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }
        return "$hour12 $period"
    }

    fun formatSlotLabel(dayOfWeek: Int, startMinute: Int): String {
        return "${dayShort(dayOfWeek)} ${formatMinuteOfDay(startMinute)}"
    }
}
