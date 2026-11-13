package com.example

import com.example.domain.ConflictType
import com.example.domain.CourseSession
import com.example.domain.FacultyMember
import com.example.domain.HistoricalExperience
import com.example.domain.OptimizationGoal
import com.example.domain.TimetableIntelligenceEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimetableIntelligenceEngineTest {

    private val sampleFaculty = FacultyMember(
        id = "fac_1",
        ssoId = "evance",
        fullName = "Dr. Elena Vance",
        email = "e.vance@university.edu",
        department = "Computer Science",
        academicTitle = "Associate Professor",
        maxWeeklyHours = 14,
        preferredStartHour = 9,
        preferredEndHour = 17,
        avoidBackToBackBuildings = true,
        preferMorningLectures = true
    )

    @Test
    fun detectsOverlapAndAutoResolvesConflict() {
        val s1 = CourseSession(
            id = "s1",
            facultyId = "fac_1",
            semesterId = "sem_1",
            courseCode = "CS-301",
            courseTitle = "Algorithms",
            section = "Sec A",
            sessionType = "LECTURE",
            dayOfWeek = 2,
            startMinuteOfDay = 10 * 60,
            durationMinutes = 90,
            roomCode = "Turing 204",
            building = "Engineering West",
            enrolledStudents = 60,
            capacity = 80,
            isLocked = true,
            sourceNote = "",
            updatedAtMillis = 0L
        )
        val s2 = CourseSession(
            id = "s2",
            facultyId = "fac_1",
            semesterId = "sem_1",
            courseCode = "CS-410",
            courseTitle = "Distributed Systems",
            section = "Lab 01",
            sessionType = "LAB",
            dayOfWeek = 2,
            startMinuteOfDay = 11 * 60,
            durationMinutes = 120,
            roomCode = "Ada Lab 102",
            building = "Engineering West",
            enrolledStudents = 30,
            capacity = 40,
            isLocked = false,
            sourceNote = "",
            updatedAtMillis = 0L
        )

        val conflicts = TimetableIntelligenceEngine.detectConflicts(
            faculty = sampleFaculty,
            facultySessions = listOf(s1, s2),
            allSemesterSessions = listOf(s1, s2)
        )
        assertTrue(conflicts.any { it.type == ConflictType.FACULTY_TIME_OVERLAP })

        val outcome = TimetableIntelligenceEngine.autoResolveConflicts(
            faculty = sampleFaculty,
            facultySessions = listOf(s1, s2),
            allSemesterSessions = listOf(s1, s2),
            historicalExperiences = emptyList()
        )
        assertTrue(outcome.resolvedCount >= 1)

        val postConflicts = TimetableIntelligenceEngine.detectConflicts(
            faculty = sampleFaculty,
            facultySessions = outcome.updatedSessions,
            allSemesterSessions = outcome.updatedSessions
        )
        assertEquals(0, postConflicts.count { it.type.isSevere })
    }

    @Test
    fun prepareNewSemesterDraftAvoidsPeerRoomDoubleBooking() {
        val historical = listOf(
            CourseSession(
                id = "h1",
                facultyId = "fac_1",
                semesterId = "sem_old",
                courseCode = "CS-301",
                courseTitle = "Algorithms",
                section = "Sec A",
                sessionType = "LECTURE",
                dayOfWeek = 1,
                startMinuteOfDay = 9 * 60 + 30,
                durationMinutes = 90,
                roomCode = "Turing 204",
                building = "Engineering West",
                enrolledStudents = 65,
                capacity = 80,
                isLocked = false,
                sourceNote = "",
                updatedAtMillis = 0L
            )
        )
        val peerInTarget = listOf(
            CourseSession(
                id = "p1",
                facultyId = "fac_peer",
                semesterId = "sem_new",
                courseCode = "MATH-240",
                courseTitle = "Linear Algebra",
                section = "Sec A",
                sessionType = "LECTURE",
                dayOfWeek = 1,
                startMinuteOfDay = 9 * 60 + 30,
                durationMinutes = 90,
                roomCode = "Turing 204",
                building = "Engineering West",
                enrolledStudents = 50,
                capacity = 80,
                isLocked = true,
                sourceNote = "",
                updatedAtMillis = 0L
            )
        )
        val historyExp = listOf(
            HistoricalExperience(
                id = "e1",
                facultyId = "fac_1",
                semesterName = "Spring 2026",
                courseCode = "CS-301",
                courseTitle = "Algorithms",
                sessionType = "LECTURE",
                dayOfWeek = 3,
                startMinuteOfDay = 9 * 60 + 30,
                durationMinutes = 90,
                roomCode = "Turing 301",
                building = "Engineering West",
                satisfactionScore = 4.9,
                attendanceRate = 95,
                pastConflictsCount = 0,
                lessonLearnedNote = "Wed 9:30 AM worked best."
            )
        )

        val draft = TimetableIntelligenceEngine.prepareNewSemesterDraft(
            faculty = sampleFaculty,
            sourceHistoricalSessions = historical,
            targetSemesterId = "sem_new",
            existingInstitutionSessionsInTarget = peerInTarget,
            historicalExperiences = historyExp,
            goal = OptimizationGoal.MINIMIZE_CONFLICTS
        )

        assertEquals(1, draft.size)
        val conflicts = TimetableIntelligenceEngine.detectConflicts(
            faculty = sampleFaculty,
            facultySessions = draft,
            allSemesterSessions = draft + peerInTarget
        )
        assertEquals(0, conflicts.count { it.type.isSevere })
    }

    @Test
    fun computesPerFacultyWeeklyBreakdownBySessionType() {
        val peerFaculty = sampleFaculty.copy(
            id = "fac_2",
            ssoId = "mthorne",
            fullName = "Prof. Marcus Thorne",
            maxWeeklyHours = 16
        )
        val sessions = listOf(
            CourseSession("1", "fac_1", "sem_1", "CS-301", "Algorithms", "A", "LECTURE", 1, 540, 90, "Turing 204", "Eng", 50, 60, false, "", 0L),
            CourseSession("2", "fac_1", "sem_1", "CS-301", "Algorithms", "Lab", "LAB", 2, 600, 120, "Ada Lab", "Eng", 30, 40, false, "", 0L),
            CourseSession("3", "fac_1", "sem_1", "CS-520", "Ethics", "Sem", "SEMINAR", 3, 840, 90, "Turing 110", "Eng", 20, 30, false, "", 0L),
            CourseSession("4", "fac_1", "sem_1", "CS-ADV", "Office Hours", "OH", "OFFICE_HOURS", 4, 900, 60, "Turing 215", "Eng", 10, 20, false, "", 0L),
            CourseSession("5", "fac_2", "sem_1", "MATH-240", "Linear Algebra", "A", "LECTURE", 1, 540, 90, "Euler 101", "Sci", 70, 80, false, "", 0L)
        )

        val breakdown = TimetableIntelligenceEngine.computeFacultyWeeklyBreakdown(
            allFaculty = listOf(sampleFaculty, peerFaculty),
            allSemesterSessions = sessions,
            activeFacultyId = "fac_1"
        )

        assertEquals(2, breakdown.size)
        val first = breakdown.first { it.facultyId == "fac_1" }
        assertEquals(1.5, first.lectureHours, 0.01)
        assertEquals(2.0, first.labHours, 0.01)
        assertEquals(1.5, first.seminarHours, 0.01)
        assertEquals(1.0, first.officeHours, 0.01)
        assertEquals(6.0, first.totalWeeklyHours, 0.01)
        assertTrue(first.isCurrentUser)

        val second = breakdown.first { it.facultyId == "fac_2" }
        assertEquals(1.5, second.totalWeeklyHours, 0.01)
    }
}
