package com.example.platform

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.core.content.FileProvider
import com.example.domain.CourseSession
import com.example.domain.FacultyMember
import com.example.domain.ScheduleConflict
import com.example.domain.Semester
import com.example.domain.SessionType
import com.example.domain.TimetableFormatters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Renders the weekly teaching timetable to a landscape PDF document using Android's
 * native PdfDocument API, supporting zero-permission SAF saving, FileProvider sharing,
 * and system PrintManager printing.
 */
class TimetablePdfExporter(private val context: Context) {

    private val appContext = context.applicationContext

    fun suggestedFileName(faculty: FacultyMember, semester: Semester?): String {
        val semSlug = (semester?.code ?: "Current_Term")
            .replace(Regex("[^A-Za-z0-9_-]"), "_")
        val facSlug = faculty.ssoId
            .replace(Regex("[^A-Za-z0-9_-]"), "_")
            .ifBlank { "faculty" }
        return "SSO_Timetable_${semSlug}_$facSlug.pdf"
    }

    suspend fun writePdfToStream(
        outputStream: OutputStream,
        faculty: FacultyMember,
        semester: Semester?,
        sessions: List<CourseSession>,
        conflicts: List<ScheduleConflict>
    ) = withContext(Dispatchers.IO) {
        try {
            val document = buildPdfDocument(faculty, semester, sessions, conflicts)
            try {
                document.writeTo(outputStream)
                outputStream.flush()
            } finally {
                document.close()
            }
        } catch (_: IllegalStateException) {
            // Fallback for JVM/Robolectric environments without native Skia PDF JNI
            writeFallbackPdf14(outputStream, faculty, semester, sessions, conflicts)
        } catch (_: UnsatisfiedLinkError) {
            writeFallbackPdf14(outputStream, faculty, semester, sessions, conflicts)
        }
    }

    suspend fun writePdfToUri(
        uri: Uri,
        faculty: FacultyMember,
        semester: Semester?,
        sessions: List<CourseSession>,
        conflicts: List<ScheduleConflict>
    ): Boolean = withContext(Dispatchers.IO) {
        val stream = appContext.contentResolver.openOutputStream(uri) ?: return@withContext false
        stream.use { out ->
            writePdfToStream(out, faculty, semester, sessions, conflicts)
        }
        true
    }

    suspend fun exportToShareableUri(
        faculty: FacultyMember,
        semester: Semester?,
        sessions: List<CourseSession>,
        conflicts: List<ScheduleConflict>
    ): Uri = withContext(Dispatchers.IO) {
        val exportDir = File(appContext.cacheDir, "exports").apply { mkdirs() }
        val file = File(exportDir, suggestedFileName(faculty, semester))
        FileOutputStream(file).use { out ->
            writePdfToStream(out, faculty, semester, sessions, conflicts)
        }
        FileProvider.getUriForFile(
            appContext,
            "${appContext.packageName}.fileprovider",
            file
        )
    }

    fun createShareIntent(pdfUri: Uri, faculty: FacultyMember, semester: Semester?): Intent {
        val semName = semester?.name ?: "Current Semester"
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, pdfUri)
            putExtra(Intent.EXTRA_SUBJECT, "${faculty.fullName} — $semName Weekly Timetable")
            putExtra(
                Intent.EXTRA_TEXT,
                "Attached is the weekly teaching timetable PDF for ${faculty.fullName} ($semName)."
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(shareIntent, "Share Weekly Timetable PDF").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun startPrintJob(
        activityContext: Context,
        faculty: FacultyMember,
        semester: Semester?,
        sessions: List<CourseSession>,
        conflicts: List<ScheduleConflict>
    ) {
        val printManager = activityContext.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val jobName = "SSO Timetable - ${semester?.name ?: "Weekly"} (${faculty.ssoId})"

        val adapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback.onLayoutCancelled()
                    return
                }
                val info = PrintDocumentInfo.Builder(suggestedFileName(faculty, semester))
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(1)
                    .build()
                callback.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback.onWriteCancelled()
                    return
                }
                val document = buildPdfDocument(faculty, semester, sessions, conflicts)
                try {
                    FileOutputStream(destination.fileDescriptor).use { out ->
                        document.writeTo(out)
                    }
                    callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback.onWriteFailed(e.message)
                } finally {
                    document.close()
                }
            }
        }

        val attributes = PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.NA_LETTER.asLandscape())
            .build()
        printManager.print(jobName, adapter, attributes)
    }

    fun buildPdfDocument(
        faculty: FacultyMember,
        semester: Semester?,
        sessions: List<CourseSession>,
        conflicts: List<ScheduleConflict>
    ): PdfDocument {
        val pageWidth = 842
        val pageHeight = 595
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = document.startPage(pageInfo)
        drawTimetablePage(page.canvas, pageWidth, pageHeight, faculty, semester, sessions, conflicts)
        document.finishPage(page)
        return document
    }

    internal fun drawTimetablePage(
        canvas: Canvas,
        pageWidth: Int,
        pageHeight: Int,
        faculty: FacultyMember,
        semester: Semester?,
        sessions: List<CourseSession>,
        conflicts: List<ScheduleConflict>
    ) {
        val margin = 28f
        val contentWidth = pageWidth - margin * 2f

        // Background
        canvas.drawColor(Color.WHITE)

        // Header banner
        val headerRect = RectF(margin, margin, pageWidth - margin, margin + 68f)
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#162A45")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(headerRect, 10f, 10f, headerPaint)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 16f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#DCE6F5")
            textSize = 10.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val metaRightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 10f
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        val semLabel = semester?.let { "${it.name} (${it.dateRangeLabel})" } ?: "Current Academic Term"
        canvas.drawText(
            "INSTITUTIONAL SSO WEEKLY TEACHING TIMETABLE",
            headerRect.left + 16f,
            headerRect.top + 25f,
            titlePaint
        )
        canvas.drawText(
            "${faculty.fullName} · ${faculty.department} · SSO: ${faculty.ssoId}",
            headerRect.left + 16f,
            headerRect.top + 43f,
            subtitlePaint
        )
        canvas.drawText(
            semLabel,
            headerRect.left + 16f,
            headerRect.top + 57f,
            subtitlePaint
        )

        val totalHours = sessions.sumOf { it.durationMinutes } / 60.0
        val conflictCount = conflicts.count { it.type.isSevere }
        canvas.drawText(
            "${sessions.size} Classes · ${String.format(Locale.US, "%.1f", totalHours)} hrs/wk",
            headerRect.right - 16f,
            headerRect.top + 26f,
            metaRightPaint
        )
        val statusText = if (conflictCount > 0) {
            "$conflictCount Severe Conflict(s) Flagged"
        } else {
            "Conflict-Free Schedule"
        }
        val statusPaint = Paint(metaRightPaint).apply {
            color = if (conflictCount > 0) Color.parseColor("#FCA5A5") else Color.parseColor("#86EFAC")
        }
        canvas.drawText(
            statusText,
            headerRect.right - 16f,
            headerRect.top + 43f,
            statusPaint
        )
        val exportedDate = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.US).format(Date())
        val datePaint = Paint(subtitlePaint).apply { textAlign = Paint.Align.RIGHT; textSize = 9f }
        canvas.drawText(
            "Exported $exportedDate",
            headerRect.right - 16f,
            headerRect.top + 57f,
            datePaint
        )

        // Grid Geometry
        val gridTop = headerRect.bottom + 14f
        val footerHeight = 32f
        val gridBottom = pageHeight - margin - footerHeight
        val dayHeaderHeight = 24f
        val bodyTop = gridTop + dayHeaderHeight
        val bodyHeight = gridBottom - bodyTop

        val timeColWidth = 54f
        val dayColWidth = (contentWidth - timeColWidth) / 5f
        val startHour = 8
        val endHour = 18
        val totalGridHours = endHour - startHour
        val hourHeight = bodyHeight / totalGridHours

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CBD5E1")
            strokeWidth = 0.8f
            style = Paint.Style.STROKE
        }
        val dayBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F1F5F9")
            style = Paint.Style.FILL
        }
        val dayTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 10.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val timeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#475569")
            textSize = 8.5f
            textAlign = Paint.Align.RIGHT
        }

        // Day headers
        canvas.drawRect(margin + timeColWidth, gridTop, pageWidth - margin, bodyTop, dayBgPaint)
        for (day in 1..5) {
            val left = margin + timeColWidth + (day - 1) * dayColWidth
            val right = left + dayColWidth
            canvas.drawRect(left, gridTop, right, gridBottom, borderPaint)
            canvas.drawText(
                TimetableFormatters.dayFull(day),
                (left + right) / 2f,
                gridTop + 16f,
                dayTextPaint
            )
        }
        canvas.drawLine(margin + timeColWidth, bodyTop, pageWidth - margin, bodyTop, borderPaint)

        // Hourly horizontal lines & time labels
        val gridLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E2E8F0")
            strokeWidth = 0.6f
        }
        for (h in 0..totalGridHours) {
            val y = bodyTop + h * hourHeight
            canvas.drawLine(margin + timeColWidth, y, pageWidth - margin, y, gridLinePaint)
            if (h < totalGridHours) {
                canvas.drawText(
                    TimetableFormatters.formatHourShort(startHour + h),
                    margin + timeColWidth - 6f,
                    y + 10f,
                    timeTextPaint
                )
            }
        }

        // Session blocks
        val conflictIds = conflicts.flatMap { listOfNotNull(it.primarySessionId, it.secondarySessionId) }.toSet()
        val cardFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val cardBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1.1f }
        val codePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 9.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val detailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E293B")
            textSize = 8f
        }
        val subDetailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#475569")
            textSize = 7.5f
        }

        for (day in 1..5) {
            val daySessions = sessions.filter { it.dayOfWeek == day }.sortedBy { it.startMinuteOfDay }
            val dayLeft = margin + timeColWidth + (day - 1) * dayColWidth

            daySessions.forEach { session ->
                val startHours = ((session.startMinuteOfDay - startHour * 60) / 60f).coerceIn(0f, totalGridHours - 0.5f)
                val durHours = (session.durationMinutes / 60f).coerceAtLeast(0.75f)
                val top = bodyTop + startHours * hourHeight + 1.5f
                val bottom = (top + durHours * hourHeight - 3f).coerceAtMost(gridBottom - 1.5f)

                // Slight horizontal inset if overlapping another session on the same day
                val hasSameDayOverlap = daySessions.any { other ->
                    other.id != session.id &&
                        session.startMinuteOfDay < other.endMinuteOfDay() &&
                        other.startMinuteOfDay < session.endMinuteOfDay()
                }
                val isConflict = session.id in conflictIds

                val insetLeft = if (hasSameDayOverlap && daySessions.indexOf(session) % 2 == 1) 14f else 3f
                val insetRight = if (hasSameDayOverlap && daySessions.indexOf(session) % 2 == 0) 14f else 3f
                val rect = RectF(dayLeft + insetLeft, top, dayLeft + dayColWidth - insetRight, bottom)

                val (fillHex, strokeHex) = when {
                    isConflict -> "#FEE2E2" to "#DC2626"
                    else -> when (SessionType.fromString(session.sessionType)) {
                        SessionType.LECTURE -> "#E0E7FF" to "#1E40AF"
                        SessionType.LAB -> "#CCFBF1" to "#0F766E"
                        SessionType.SEMINAR -> "#FEF3C7" to "#B45309"
                        SessionType.OFFICE_HOURS -> "#F3E8FF" to "#6D28D9"
                    }
                }

                cardFillPaint.color = Color.parseColor(fillHex)
                cardBorderPaint.color = Color.parseColor(strokeHex)
                codePaint.color = Color.parseColor(strokeHex)

                canvas.drawRoundRect(rect, 5f, 5f, cardFillPaint)
                canvas.drawRoundRect(rect, 5f, 5f, cardBorderPaint)

                val textX = rect.left + 5f
                val maxTextWidth = (rect.width() - 10f).coerceAtLeast(24f)
                val headerLine = ellipsize("${session.courseCode} (${session.section})", codePaint, maxTextWidth)
                canvas.drawText(headerLine, textX, rect.top + 11f, codePaint)

                if (rect.height() >= 28f) {
                    val timeLine = ellipsize(
                        TimetableFormatters.formatTimeRange(session.startMinuteOfDay, session.durationMinutes),
                        detailPaint,
                        maxTextWidth
                    )
                    canvas.drawText(timeLine, textX, rect.top + 21f, detailPaint)
                }
                if (rect.height() >= 38f) {
                    val roomLine = ellipsize("${session.roomCode} · ${session.building}", subDetailPaint, maxTextWidth)
                    canvas.drawText(roomLine, textX, rect.top + 31f, subDetailPaint)
                }
            }
        }

        // Footer legend
        val footerY = pageHeight - margin - 8f
        val legendPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#475569")
            textSize = 8.5f
        }
        canvas.drawText(
            "Legend: Lecture (Blue) · Laboratory (Teal) · Seminar (Amber) · Office Hours (Purple) · Conflict Flagged (Red)",
            margin,
            footerY,
            legendPaint
        )
    }

    private fun ellipsize(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var end = text.length
        while (end > 1 && paint.measureText(text.substring(0, end) + "…") > maxWidth) {
            end--
        }
        return text.substring(0, end) + "…"
    }

    private fun writeFallbackPdf14(
        outputStream: OutputStream,
        faculty: FacultyMember,
        semester: Semester?,
        sessions: List<CourseSession>,
        conflicts: List<ScheduleConflict>
    ) {
        fun esc(text: String): String = text
            .replace("\\", "\\\\")
            .replace("(", "\\(")
            .replace(")", "\\)")

        val contentStream = buildString {
            append("BT\n/F1 14 Tf\n40 550 Td\n")
            append("(${esc("SSO Weekly Teaching Timetable - ${semester?.name ?: "Current Term"}")}) Tj\n")
            append("/F1 10 Tf\n0 -20 Td\n")
            append("(${esc("${faculty.fullName} | ${faculty.department} | SSO: ${faculty.ssoId}")}) Tj\n")
            sessions.sortedWith(compareBy<CourseSession> { it.dayOfWeek }.thenBy { it.startMinuteOfDay })
                .forEach { s ->
                    val line = "${TimetableFormatters.dayShort(s.dayOfWeek)} " +
                        "${TimetableFormatters.formatTimeRange(s.startMinuteOfDay, s.durationMinutes)} | " +
                        "${s.courseCode} (${s.section}) | ${s.roomCode}"
                    append("0 -16 Td\n(${esc(line)}) Tj\n")
                }
            if (conflicts.isNotEmpty()) {
                append("0 -20 Td\n(${esc("Conflicts detected: ${conflicts.size}")}) Tj\n")
            }
            append("ET\n")
        }
        val streamBytes = contentStream.toByteArray(Charsets.US_ASCII)
        val objects = listOf(
            "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n",
            "2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n",
            "3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 842 595] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>\nendobj\n",
            "4 0 obj\n<< /Length ${streamBytes.size} >>\nstream\n$contentStream\nendstream\nendobj\n",
            "5 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n"
        )

        val sb = StringBuilder("%PDF-1.4\n")
        val offsets = mutableListOf<Int>()
        objects.forEach { obj ->
            offsets += sb.length
            sb.append(obj)
        }
        val xrefStart = sb.length
        sb.append("xref\n0 ${objects.size + 1}\n")
        sb.append("0000000000 65535 f \n")
        offsets.forEach { offset ->
            sb.append(String.format(Locale.US, "%010d 00000 n \n", offset))
        }
        sb.append("trailer\n<< /Size ${objects.size + 1} /Root 1 0 R >>\nstartxref\n$xrefStart\n%%EOF\n")
        outputStream.write(sb.toString().toByteArray(Charsets.US_ASCII))
        outputStream.flush()
    }
}
