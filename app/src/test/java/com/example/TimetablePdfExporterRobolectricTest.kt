package com.example

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.test.core.app.ApplicationProvider
import com.example.domain.CourseSession
import com.example.domain.FacultyMember
import com.example.domain.Semester
import com.example.platform.TimetablePdfExporter
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TimetablePdfExporterRobolectricTest {

    private val faculty = FacultyMember(
        id = "fac_vance",
        ssoId = "evance",
        fullName = "Dr. Elena Vance",
        email = "e.vance@university.edu",
        department = "Computer Science & AI",
        academicTitle = "Associate Professor",
        maxWeeklyHours = 14,
        preferredStartHour = 9,
        preferredEndHour = 17,
        avoidBackToBackBuildings = true,
        preferMorningLectures = true
    )

    private val semester = Semester(
        id = "sem_2026_fa",
        name = "Fall 2026",
        code = "2026-FA",
        status = "ACTIVE",
        dateRangeLabel = "Sep 1 – Dec 18, 2026",
        totalWeeks = 15,
        sortOrder = 1
    )

    private val sessions = listOf(
        CourseSession(
            id = "s1",
            facultyId = "fac_vance",
            semesterId = "sem_2026_fa",
            courseCode = "CS-301",
            courseTitle = "Algorithms & Complexity",
            section = "Sec A",
            sessionType = "LECTURE",
            dayOfWeek = 1,
            startMinuteOfDay = 9 * 60 + 30,
            durationMinutes = 90,
            roomCode = "Turing 204",
            building = "Engineering West",
            enrolledStudents = 68,
            capacity = 80,
            isLocked = true,
            sourceNote = "",
            updatedAtMillis = 0L
        ),
        CourseSession(
            id = "s2",
            facultyId = "fac_vance",
            semesterId = "sem_2026_fa",
            courseCode = "CS-410",
            courseTitle = "Distributed Systems",
            section = "Lab 02",
            sessionType = "LAB",
            dayOfWeek = 5,
            startMinuteOfDay = 10 * 60,
            durationMinutes = 120,
            roomCode = "Ada Lab 104",
            building = "Science Commons",
            enrolledStudents = 48,
            capacity = 50,
            isLocked = false,
            sourceNote = "",
            updatedAtMillis = 0L
        )
    )

    @Test
    fun generatesValidPdfBytesAndRendersTimetableCanvas() = runTest {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val exporter = TimetablePdfExporter(context)
        val output = ByteArrayOutputStream()

        exporter.writePdfToStream(
            outputStream = output,
            faculty = faculty,
            semester = semester,
            sessions = sessions,
            conflicts = emptyList()
        )

        val bytes = output.toByteArray()
        assertTrue("PDF byte output should not be empty", bytes.size > 100)
        val text = String(bytes, Charsets.US_ASCII)
        assertTrue(text.startsWith("%PDF-"))
        assertTrue(text.contains("%%EOF"))
        assertTrue(text.contains("CS-301"))

        // Also exercise Canvas timetable rendering routine
        val bitmap = Bitmap.createBitmap(842, 595, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        exporter.drawTimetablePage(
            canvas = canvas,
            pageWidth = 842,
            pageHeight = 595,
            faculty = faculty,
            semester = semester,
            sessions = sessions,
            conflicts = emptyList()
        )
    }

    @Test
    fun buildsExpectedFilenameAndShareableUriAndChooserIntent() = runTest {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val exporter = TimetablePdfExporter(context)

        val fileName = exporter.suggestedFileName(faculty, semester)
        assertEquals("SSO_Timetable_2026-FA_evance.pdf", fileName)

        val uri = exporter.exportToShareableUri(
            faculty = faculty,
            semester = semester,
            sessions = sessions,
            conflicts = emptyList()
        )
        assertEquals("content", uri.scheme)
        assertTrue(uri.toString().contains("SSO_Timetable_2026-FA_evance.pdf"))

        val chooser = exporter.createShareIntent(uri, faculty, semester)
        assertEquals(Intent.ACTION_CHOOSER, chooser.action)
        assertNotNull(chooser.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java))
    }
}
