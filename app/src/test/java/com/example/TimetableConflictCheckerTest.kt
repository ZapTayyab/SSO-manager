package com.example

import com.example.domain.CourseSession
import com.example.domain.conflict.TimetableConflictChecker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimetableConflictCheckerTest {

    private fun session(
        id: String,
        facultyId: String = "fac_1",
        semesterId: String = "sem_fall",
        courseCode: String = "CS-301",
        day: Int = 1,
        startMinute: Int = 9 * 60 + 30,
        duration: Int = 90,
        room: String = "Turing 204",
        building: String = "Engineering West"
    ) = CourseSession(
        id = id,
        facultyId = facultyId,
        semesterId = semesterId,
        courseCode = courseCode,
        courseTitle = "Course $courseCode",
        section = "Sec A",
        sessionType = "LECTURE",
        dayOfWeek = day,
        startMinuteOfDay = startMinute,
        durationMinutes = duration,
        roomCode = room,
        building = building,
        enrolledStudents = 40,
        capacity = 60,
        isLocked = false,
        sourceNote = "",
        updatedAtMillis = 0L
    )

    @Test
    fun detectsOverlappingTeachingTimeForSameFaculty() {
        val existing = session(id = "s1", courseCode = "CS-301", day = 2, startMinute = 10 * 60, duration = 90)
        val candidate = session(
            id = "s2",
            courseCode = "CS-410",
            day = 2,
            startMinute = 10 * 60 + 45,
            duration = 60,
            room = "Euler 101",
            building = "Science Commons"
        )

        val result = TimetableConflictChecker.checkCandidateSession(
            candidate = candidate,
            facultySessions = listOf(existing),
            allSemesterSessions = listOf(existing)
        )

        assertTrue(result.hasConflict)
        assertTrue(result.hasTimeOverlap)
        assertFalse(result.hasVenueClash)
        assertEquals(1, result.overlappingTeachingSessions.size)
        assertTrue(result.warningMessage.contains("CS-301"))
    }

    @Test
    fun detectsVenueDoubleBookingAcrossDifferentFaculty() {
        val peerSession = session(
            id = "peer_1",
            facultyId = "fac_2",
            courseCode = "MATH-240",
            day = 3,
            startMinute = 13 * 60,
            duration = 90,
            room = "Turing 204",
            building = "Engineering West"
        )
        val candidate = session(
            id = "mine_1",
            facultyId = "fac_1",
            courseCode = "CS-320",
            day = 3,
            startMinute = 14 * 60,
            duration = 60,
            room = "turing 204 ",
            building = "Engineering West"
        )

        val result = TimetableConflictChecker.checkCandidateSession(
            candidate = candidate,
            facultySessions = emptyList(),
            allSemesterSessions = listOf(peerSession)
        )

        assertTrue(result.hasConflict)
        assertFalse(result.hasTimeOverlap)
        assertTrue(result.hasVenueClash)
        assertEquals(1, result.overlappingVenueBookings.size)
        assertTrue(result.warningMessage.contains("Turing 204", ignoreCase = true))
        assertTrue(result.warningMessage.contains("MATH-240"))
    }

    @Test
    fun allowsAdjacentSessionsAndSameRoomInDifferentBuildings() {
        val morning = session(
            id = "s1",
            facultyId = "fac_1",
            courseCode = "CS-101",
            day = 1,
            startMinute = 9 * 60,
            duration = 60,
            room = "Room 101",
            building = "Engineering West"
        )
        val adjacentSameTimeBoundary = session(
            id = "s2",
            facultyId = "fac_1",
            courseCode = "CS-202",
            day = 1,
            startMinute = 10 * 60, // starts right as s1 ends
            duration = 60,
            room = "Room 101",
            building = "Engineering West"
        )
        val differentBuildingSameRoomAndTime = session(
            id = "s3",
            facultyId = "fac_2",
            courseCode = "ENG-110",
            day = 1,
            startMinute = 9 * 60,
            duration = 60,
            room = "Room 101",
            building = "Humanities Hall"
        )

        val adjacentCheck = TimetableConflictChecker.checkCandidateSession(
            candidate = adjacentSameTimeBoundary,
            facultySessions = listOf(morning),
            allSemesterSessions = listOf(morning)
        )
        assertFalse(adjacentCheck.hasConflict)

        val differentBuildingVenueCheck = TimetableConflictChecker.checkCandidateSession(
            candidate = morning,
            facultySessions = listOf(morning),
            allSemesterSessions = listOf(morning, differentBuildingSameRoomAndTime)
        )
        assertFalse(differentBuildingVenueCheck.hasConflict)
    }

    @Test
    fun reportsCombinedWarningWhenBothTimeAndVenueCollide() {
        val existing = session(
            id = "s1",
            facultyId = "fac_1",
            courseCode = "CS-301",
            day = 4,
            startMinute = 11 * 60,
            duration = 90,
            room = "Ada Lab 102",
            building = "Engineering West"
        )
        val candidate = session(
            id = "s2",
            facultyId = "fac_1",
            courseCode = "CS-499",
            day = 4,
            startMinute = 11 * 60 + 30,
            duration = 60,
            room = "Ada Lab 102",
            building = "Engineering West"
        )

        val result = TimetableConflictChecker.checkCandidateSession(
            candidate = candidate,
            facultySessions = listOf(existing),
            allSemesterSessions = listOf(existing)
        )

        assertTrue(result.hasTimeOverlap)
        assertTrue(result.hasVenueClash)
        assertEquals("Schedule & Venue Conflict", result.warningTitle)
        assertTrue(result.warningMessage.contains("Ada Lab 102"))
    }
}
