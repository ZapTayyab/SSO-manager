package com.example.data.local

import android.content.Context
import com.example.domain.AutoUpdateLog
import com.example.domain.CourseSession
import com.example.domain.FacultyMember
import com.example.domain.HistoricalExperience
import com.example.domain.SavedSsoCredential
import com.example.domain.Semester
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class WorkspaceSnapshot(
    val faculty: List<FacultyMember> = emptyList(),
    val semesters: List<Semester> = emptyList(),
    val sessions: List<CourseSession> = emptyList(),
    val historicalExperiences: List<HistoricalExperience> = emptyList(),
    val autoUpdateLogs: List<AutoUpdateLog> = emptyList()
)

/**
 * Lightweight, database-free JSON persistence layer for short-term user SSO credentials
 * and reactive in-memory/JSON timetable state stored in app-private filesDir.
 */
class JsonCredentialAndTimetableStore(context: Context) {

    private val appContext = context.applicationContext
    private val credentialsFile: File get() = File(appContext.filesDir, "sso_credentials.json")
    private val workspaceFile: File get() = File(appContext.filesDir, "timetable_workspace.json")
    private val mutex = Mutex()

    private val _savedCredential = MutableStateFlow<SavedSsoCredential?>(null)
    val savedCredential: StateFlow<SavedSsoCredential?> = _savedCredential.asStateFlow()

    private val _workspace = MutableStateFlow(WorkspaceSnapshot())
    val workspace: StateFlow<WorkspaceSnapshot> = _workspace.asStateFlow()

    private var initialized = false

    suspend fun initialize() {
        mutex.withLock {
            if (initialized) return
            withContext(Dispatchers.IO) {
                _savedCredential.value = readCredentialFromDisk()
                val loadedWorkspace = readWorkspaceFromDisk()
                if (loadedWorkspace != null && loadedWorkspace.faculty.isNotEmpty()) {
                    _workspace.value = loadedWorkspace
                } else {
                    val seeded = createInitialSeedSnapshot()
                    _workspace.value = seeded
                    writeWorkspaceToDisk(seeded)
                }
                initialized = true
            }
        }
    }

    suspend fun saveCredential(credential: SavedSsoCredential?) {
        mutex.withLock {
            withContext(Dispatchers.IO) {
                _savedCredential.value = credential
                if (credential == null) {
                    if (credentialsFile.exists()) credentialsFile.delete()
                } else {
                    val json = JSONObject().apply {
                        put("ssoId", credential.ssoId)
                        put("email", credential.email)
                        put("passcode", credential.passcode)
                        put("facultyId", credential.facultyId)
                        put("rememberCredentials", credential.rememberCredentials)
                        put("savedAtMillis", credential.savedAtMillis)
                    }
                    credentialsFile.writeText(json.toString(2))
                }
            }
        }
    }

    suspend fun mutateWorkspace(transform: (WorkspaceSnapshot) -> WorkspaceSnapshot) {
        mutex.withLock {
            val updated = transform(_workspace.value)
            _workspace.value = updated
            withContext(Dispatchers.IO) {
                writeWorkspaceToDisk(updated)
            }
        }
    }

    private fun readCredentialFromDisk(): SavedSsoCredential? {
        if (!credentialsFile.exists()) return null
        return runCatching {
            val obj = JSONObject(credentialsFile.readText())
            SavedSsoCredential(
                ssoId = obj.optString("ssoId"),
                email = obj.optString("email"),
                passcode = obj.optString("passcode"),
                facultyId = obj.optString("facultyId"),
                rememberCredentials = obj.optBoolean("rememberCredentials", true),
                savedAtMillis = obj.optLong("savedAtMillis", System.currentTimeMillis())
            )
        }.getOrNull()
    }

    private fun writeWorkspaceToDisk(snapshot: WorkspaceSnapshot) {
        val root = JSONObject().apply {
            put("faculty", JSONArray().apply {
                snapshot.faculty.forEach { f ->
                    put(JSONObject().apply {
                        put("id", f.id)
                        put("ssoId", f.ssoId)
                        put("fullName", f.fullName)
                        put("email", f.email)
                        put("department", f.department)
                        put("academicTitle", f.academicTitle)
                        put("maxWeeklyHours", f.maxWeeklyHours)
                        put("preferredStartHour", f.preferredStartHour)
                        put("preferredEndHour", f.preferredEndHour)
                        put("avoidBackToBackBuildings", f.avoidBackToBackBuildings)
                        put("preferMorningLectures", f.preferMorningLectures)
                    })
                }
            })
            put("semesters", JSONArray().apply {
                snapshot.semesters.forEach { s ->
                    put(JSONObject().apply {
                        put("id", s.id)
                        put("name", s.name)
                        put("code", s.code)
                        put("status", s.status)
                        put("dateRangeLabel", s.dateRangeLabel)
                        put("totalWeeks", s.totalWeeks)
                        put("sortOrder", s.sortOrder)
                    })
                }
            })
            put("sessions", JSONArray().apply {
                snapshot.sessions.forEach { c ->
                    put(JSONObject().apply {
                        put("id", c.id)
                        put("facultyId", c.facultyId)
                        put("semesterId", c.semesterId)
                        put("courseCode", c.courseCode)
                        put("courseTitle", c.courseTitle)
                        put("section", c.section)
                        put("sessionType", c.sessionType)
                        put("dayOfWeek", c.dayOfWeek)
                        put("startMinuteOfDay", c.startMinuteOfDay)
                        put("durationMinutes", c.durationMinutes)
                        put("roomCode", c.roomCode)
                        put("building", c.building)
                        put("enrolledStudents", c.enrolledStudents)
                        put("capacity", c.capacity)
                        put("isLocked", c.isLocked)
                        put("sourceNote", c.sourceNote)
                        put("updatedAtMillis", c.updatedAtMillis)
                    })
                }
            })
            put("historicalExperiences", JSONArray().apply {
                snapshot.historicalExperiences.forEach { h ->
                    put(JSONObject().apply {
                        put("id", h.id)
                        put("facultyId", h.facultyId)
                        put("semesterName", h.semesterName)
                        put("courseCode", h.courseCode)
                        put("courseTitle", h.courseTitle)
                        put("sessionType", h.sessionType)
                        put("dayOfWeek", h.dayOfWeek)
                        put("startMinuteOfDay", h.startMinuteOfDay)
                        put("durationMinutes", h.durationMinutes)
                        put("roomCode", h.roomCode)
                        put("building", h.building)
                        put("satisfactionScore", h.satisfactionScore)
                        put("attendanceRate", h.attendanceRate)
                        put("pastConflictsCount", h.pastConflictsCount)
                        put("lessonLearnedNote", h.lessonLearnedNote)
                    })
                }
            })
            put("autoUpdateLogs", JSONArray().apply {
                snapshot.autoUpdateLogs.forEach { l ->
                    put(JSONObject().apply {
                        put("id", l.id)
                        put("facultyId", l.facultyId)
                        put("semesterId", l.semesterId)
                        put("actionTitle", l.actionTitle)
                        put("summary", l.summary)
                        put("affectedCount", l.affectedCount)
                        put("timestampMillis", l.timestampMillis)
                    })
                }
            })
        }
        workspaceFile.writeText(root.toString())
    }

    private fun readWorkspaceFromDisk(): WorkspaceSnapshot? {
        if (!workspaceFile.exists()) return null
        return runCatching {
            val root = JSONObject(workspaceFile.readText())
            val facultyArr = root.getJSONArray("faculty")
            val faculty = (0 until facultyArr.length()).map { i ->
                val o = facultyArr.getJSONObject(i)
                FacultyMember(
                    id = o.getString("id"),
                    ssoId = o.getString("ssoId"),
                    fullName = o.getString("fullName"),
                    email = o.getString("email"),
                    department = o.getString("department"),
                    academicTitle = o.getString("academicTitle"),
                    maxWeeklyHours = o.getInt("maxWeeklyHours"),
                    preferredStartHour = o.getInt("preferredStartHour"),
                    preferredEndHour = o.getInt("preferredEndHour"),
                    avoidBackToBackBuildings = o.getBoolean("avoidBackToBackBuildings"),
                    preferMorningLectures = o.getBoolean("preferMorningLectures")
                )
            }
            val semArr = root.getJSONArray("semesters")
            val semesters = (0 until semArr.length()).map { i ->
                val o = semArr.getJSONObject(i)
                Semester(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    code = o.getString("code"),
                    status = o.getString("status"),
                    dateRangeLabel = o.getString("dateRangeLabel"),
                    totalWeeks = o.getInt("totalWeeks"),
                    sortOrder = o.getInt("sortOrder")
                )
            }
            val sesArr = root.getJSONArray("sessions")
            val sessions = (0 until sesArr.length()).map { i ->
                val o = sesArr.getJSONObject(i)
                CourseSession(
                    id = o.getString("id"),
                    facultyId = o.getString("facultyId"),
                    semesterId = o.getString("semesterId"),
                    courseCode = o.getString("courseCode"),
                    courseTitle = o.getString("courseTitle"),
                    section = o.getString("section"),
                    sessionType = o.getString("sessionType"),
                    dayOfWeek = o.getInt("dayOfWeek"),
                    startMinuteOfDay = o.getInt("startMinuteOfDay"),
                    durationMinutes = o.getInt("durationMinutes"),
                    roomCode = o.getString("roomCode"),
                    building = o.getString("building"),
                    enrolledStudents = o.getInt("enrolledStudents"),
                    capacity = o.getInt("capacity"),
                    isLocked = o.getBoolean("isLocked"),
                    sourceNote = o.getString("sourceNote"),
                    updatedAtMillis = o.getLong("updatedAtMillis")
                )
            }
            val expArr = root.getJSONArray("historicalExperiences")
            val experiences = (0 until expArr.length()).map { i ->
                val o = expArr.getJSONObject(i)
                HistoricalExperience(
                    id = o.getString("id"),
                    facultyId = o.getString("facultyId"),
                    semesterName = o.getString("semesterName"),
                    courseCode = o.getString("courseCode"),
                    courseTitle = o.getString("courseTitle"),
                    sessionType = o.getString("sessionType"),
                    dayOfWeek = o.getInt("dayOfWeek"),
                    startMinuteOfDay = o.getInt("startMinuteOfDay"),
                    durationMinutes = o.getInt("durationMinutes"),
                    roomCode = o.getString("roomCode"),
                    building = o.getString("building"),
                    satisfactionScore = o.getDouble("satisfactionScore"),
                    attendanceRate = o.getInt("attendanceRate"),
                    pastConflictsCount = o.getInt("pastConflictsCount"),
                    lessonLearnedNote = o.getString("lessonLearnedNote")
                )
            }
            val logArr = root.getJSONArray("autoUpdateLogs")
            val logs = (0 until logArr.length()).map { i ->
                val o = logArr.getJSONObject(i)
                AutoUpdateLog(
                    id = o.getString("id"),
                    facultyId = o.getString("facultyId"),
                    semesterId = o.getString("semesterId"),
                    actionTitle = o.getString("actionTitle"),
                    summary = o.getString("summary"),
                    affectedCount = o.getInt("affectedCount"),
                    timestampMillis = o.getLong("timestampMillis")
                )
            }
            WorkspaceSnapshot(faculty, semesters, sessions, experiences, logs)
        }.getOrNull()
    }

    private fun createInitialSeedSnapshot(): WorkspaceSnapshot {
        val now = System.currentTimeMillis()
        val faculty = listOf(
            FacultyMember(
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
            ),
            FacultyMember(
                id = "fac_thorne",
                ssoId = "mthorne",
                fullName = "Prof. Marcus Thorne",
                email = "m.thorne@university.edu",
                department = "Applied Mathematics",
                academicTitle = "Senior Lecturer",
                maxWeeklyHours = 16,
                preferredStartHour = 8,
                preferredEndHour = 16,
                avoidBackToBackBuildings = true,
                preferMorningLectures = true
            ),
            FacultyMember(
                id = "fac_patel",
                ssoId = "apatel",
                fullName = "Dr. Amina Patel",
                email = "a.patel@university.edu",
                department = "Data Engineering",
                academicTitle = "Assistant Professor",
                maxWeeklyHours = 12,
                preferredStartHour = 10,
                preferredEndHour = 18,
                avoidBackToBackBuildings = true,
                preferMorningLectures = false
            )
        )

        val semesters = listOf(
            Semester("sem_2026_fa", "Fall 2026", "2026-FA", "ACTIVE", "Sep 1 – Dec 18, 2026", 15, 1),
            Semester("sem_2027_sp", "Spring 2027", "2027-SP", "DRAFT", "Jan 18 – May 7, 2027", 15, 2),
            Semester("sem_2026_sp", "Spring 2026", "2026-SP", "HISTORICAL", "Jan 19 – May 8, 2026", 15, 3),
            Semester("sem_2025_fa", "Fall 2025", "2025-FA", "HISTORICAL", "Sep 2 – Dec 19, 2025", 15, 4)
        )

        val sessions = listOf(
            CourseSession("ses_fa26_v1", "fac_vance", "sem_2026_fa", "CS-301", "Algorithms & Complexity", "Sec A", "LECTURE", 1, 9 * 60 + 30, 90, "Turing 204", "Engineering West", 68, 80, true, "Core morning lecture pinned by department", now),
            CourseSession("ses_fa26_v2", "fac_vance", "sem_2026_fa", "CS-301", "Algorithms & Complexity", "Lab 01", "LAB", 2, 11 * 60 + 30, 120, "Ada Lab 102", "Engineering West", 34, 40, false, "Auto-imported from registrar draft", now),
            CourseSession("ses_fa26_v3", "fac_vance", "sem_2026_fa", "CS-410", "Distributed Systems", "Sec B", "LECTURE", 2, 10 * 60 + 30, 90, "Turing 301", "Engineering West", 52, 60, false, "Carried from Fall 2025 pattern", now),
            CourseSession("ses_fa26_v4", "fac_vance", "sem_2026_fa", "CS-410", "Distributed Systems", "Sec B", "LECTURE", 4, 14 * 60, 90, "Turing 301", "Engineering West", 52, 60, false, "Afternoon slot (historically lower attendance)", now),
            CourseSession("ses_fa26_v5", "fac_vance", "sem_2026_fa", "CS-520", "Graduate AI Ethics Seminar", "Sem 01", "SEMINAR", 1, 14 * 60, 90, "Turing 110", "Engineering West", 22, 28, false, "Weekly graduate discussion", now),
            CourseSession("ses_fa26_v6", "fac_vance", "sem_2026_fa", "CS-ADV", "Faculty Office Hours", "Open", "OFFICE_HOURS", 1, 15 * 60 + 35, 60, "Hub 215", "Humanities Hall", 15, 20, false, "Tight cross-campus transit from Engineering West", now),
            CourseSession("ses_fa26_v7", "fac_vance", "sem_2026_fa", "CS-301", "Algorithms & Complexity", "Sec A", "LECTURE", 3, 9 * 60 + 30, 90, "Turing 204", "Engineering West", 68, 80, true, "Core morning lecture pinned by department", now),
            CourseSession("ses_fa26_v8", "fac_vance", "sem_2026_fa", "CS-410", "Distributed Systems", "Lab 02", "LAB", 5, 10 * 60, 120, "Ada Lab 104", "Science Commons", 48, 50, false, "Friday systems lab", now),

            // Peer sessions
            CourseSession("ses_fa26_t1", "fac_thorne", "sem_2026_fa", "MATH-240", "Linear Algebra & Optimization", "Sec A", "LECTURE", 1, 9 * 60, 90, "Euler 101", "Science Commons", 75, 90, true, "Department core", now),
            CourseSession("ses_fa26_t2", "fac_thorne", "sem_2026_fa", "MATH-240", "Linear Algebra & Optimization", "Sec A", "LECTURE", 3, 9 * 60, 90, "Euler 101", "Science Commons", 75, 90, true, "Department core", now),
            CourseSession("ses_fa26_t3", "fac_thorne", "sem_2026_fa", "MATH-355", "Stochastic Modeling", "Sec B", "SEMINAR", 4, 10 * 60, 90, "Turing 204", "Engineering West", 28, 35, false, "Shared engineering room", now),
            CourseSession("ses_fa26_p1", "fac_patel", "sem_2026_fa", "DATA-210", "Data Pipelines & Streaming", "Sec A", "LECTURE", 2, 13 * 60, 90, "Ada Lab 102", "Engineering West", 42, 45, false, "Data lab lecture", now),
            CourseSession("ses_fa26_p2", "fac_patel", "sem_2026_fa", "DATA-340", "Cloud Warehousing", "Lab 01", "LAB", 4, 11 * 60, 120, "Ada Lab 104", "Science Commons", 36, 40, false, "Cloud lab", now),

            // Historical semesters for Dr. Elena Vance
            CourseSession("ses_sp26_v1", "fac_vance", "sem_2026_sp", "CS-301", "Algorithms & Complexity", "Sec A", "LECTURE", 1, 9 * 60 + 30, 90, "Turing 204", "Engineering West", 72, 80, true, "High attendance morning cohort", now - 10_000_000L),
            CourseSession("ses_sp26_v2", "fac_vance", "sem_2026_sp", "CS-301", "Algorithms & Complexity", "Lab 01", "LAB", 4, 10 * 60, 120, "Ada Lab 102", "Engineering West", 36, 40, false, "Thursday 10 AM lab had 96% completion", now - 10_000_000L),
            CourseSession("ses_sp26_v3", "fac_vance", "sem_2026_sp", "CS-415", "Compiler Design", "Sec A", "LECTURE", 2, 10 * 60, 90, "Turing 301", "Engineering West", 44, 60, false, "Balanced Tue/Thu rhythm", now - 10_000_000L),
            CourseSession("ses_sp26_v4", "fac_vance", "sem_2026_sp", "CS-415", "Compiler Design", "Sec A", "LECTURE", 5, 10 * 60, 90, "Turing 301", "Engineering West", 44, 60, false, "Zero conflicts in Spring 2026", now - 10_000_000L),
            CourseSession("ses_fa25_v1", "fac_vance", "sem_2025_fa", "CS-301", "Algorithms & Complexity", "Sec A", "LECTURE", 1, 9 * 60 + 30, 90, "Turing 204", "Engineering West", 70, 80, true, "Zero conflict slot", now - 25_000_000L),
            CourseSession("ses_fa25_v2", "fac_vance", "sem_2025_fa", "CS-410", "Distributed Systems", "Sec A", "LECTURE", 3, 13 * 60 + 30, 90, "Turing 301", "Engineering West", 50, 60, false, "Well-spaced mid-week block", now - 25_000_000L),
            CourseSession("ses_fa25_v3", "fac_vance", "sem_2025_fa", "CS-520", "Graduate AI Ethics Seminar", "Sem 01", "SEMINAR", 2, 14 * 60, 90, "Turing 110", "Engineering West", 24, 28, false, "High student engagement in afternoon seminar", now - 25_000_000L)
        )

        val experiences = listOf(
            HistoricalExperience("exp_v1", "fac_vance", "Spring 2026", "CS-301", "Algorithms & Complexity (Lab)", "LAB", 4, 10 * 60, 120, "Ada Lab 102", "Engineering West", 4.9, 96, 0, "Thursday 10:00 AM lab in Ada Lab 102 avoided Tuesday lecture congestion and achieved 96% attendance."),
            HistoricalExperience("exp_v2", "fac_vance", "Fall 2025", "CS-410", "Distributed Systems", "LECTURE", 3, 11 * 60, 90, "Turing 301", "Engineering West", 4.7, 93, 0, "Late-morning Turing 301 slots yielded 14% higher lab readiness than late-afternoon 2:00 PM blocks."),
            HistoricalExperience("exp_v3", "fac_vance", "Spring 2026", "CS-ADV", "Faculty Office Hours", "OFFICE_HOURS", 3, 14 * 60, 60, "Turing 215", "Engineering West", 4.8, 91, 0, "Keeping office hours inside Engineering West eliminated 15-minute cross-campus walking delays."),
            HistoricalExperience("exp_v4", "fac_vance", "Fall 2025", "CS-520", "Graduate AI Ethics Seminar", "SEMINAR", 2, 14 * 60, 90, "Turing 110", "Engineering West", 4.6, 94, 0, "Afternoon seminar discussions thrived in Turing 110 with 24 students."),
            HistoricalExperience("exp_t1", "fac_thorne", "Spring 2026", "MATH-240", "Linear Algebra & Optimization", "LECTURE", 1, 9 * 60, 90, "Euler 101", "Science Commons", 4.8, 95, 0, "Morning Mon/Wed cadence in Euler 101 kept student retention highest."),
            HistoricalExperience("exp_p1", "fac_patel", "Spring 2026", "DATA-210", "Data Pipelines & Streaming", "LECTURE", 2, 13 * 60, 90, "Ada Lab 102", "Engineering West", 4.7, 92, 0, "Dual-projector workstations in Ada Lab 102 improved live pipeline demos.")
        )

        val logs = listOf(
            AutoUpdateLog(
                id = "log_init_1",
                facultyId = "fac_vance",
                semesterId = "sem_2026_fa",
                actionTitle = "Registrar SSO Sync Completed",
                summary = "Imported 8 Fall 2026 sessions and flagged 2 scheduling conflicts for automated review.",
                affectedCount = 8,
                timestampMillis = now - 3_600_000L
            )
        )

        return WorkspaceSnapshot(faculty, semesters, sessions, experiences, logs)
    }
}
