package com.example.data.repository

import com.example.data.local.JsonCredentialAndTimetableStore
import com.example.domain.AutoUpdateLog
import com.example.domain.CourseSession
import com.example.domain.FacultyMember
import com.example.domain.HistoricalExperience
import com.example.domain.OptimizationGoal
import com.example.domain.SavedSsoCredential
import com.example.domain.ScheduleRecommendation
import com.example.domain.Semester
import com.example.domain.TimetableFormatters
import com.example.domain.TimetableIntelligenceEngine
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.PreferencesRepository
import com.example.domain.repository.TimetableRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class JsonAuthRepository(
    private val store: JsonCredentialAndTimetableStore,
    private val prefs: PreferencesRepository
) : AuthRepository {

    override val savedCredential: Flow<SavedSsoCredential?> = store.savedCredential

    override val directoryFaculty: Flow<List<FacultyMember>> = store.workspace.map { snap ->
        snap.faculty.sortedBy { it.fullName }
    }

    override suspend fun initialize() {
        store.initialize()
    }

    override suspend fun signInWithSsoCredentials(
        identifier: String,
        passcode: String,
        rememberInJson: Boolean
    ): FacultyMember? {
        store.initialize()
        val trimmed = identifier.trim()
        if (trimmed.isBlank()) return null

        val currentFaculty = store.workspace.value.faculty
        val existing = currentFaculty.firstOrNull {
            it.ssoId.equals(trimmed, ignoreCase = true) || it.email.equals(trimmed, ignoreCase = true)
        }

        val resolvedFaculty = existing ?: run {
            val cleanNamePart = trimmed.substringBefore("@")
                .replace('.', ' ')
                .replace('_', ' ')
                .split(" ")
                .filter { it.isNotBlank() }
                .joinToString(" ") { part -> part.replaceFirstChar { it.uppercaseChar() } }
                .ifBlank { "Faculty Member" }
            val email = if (trimmed.contains("@")) trimmed else "$trimmed@university.edu"
            val ssoId = trimmed.substringBefore("@").lowercase()
            val newMember = FacultyMember(
                id = "fac_${UUID.randomUUID().toString().take(8)}",
                ssoId = ssoId,
                fullName = "Prof. $cleanNamePart",
                email = email,
                department = "School of Computing & Engineering",
                academicTitle = "Visiting Faculty",
                maxWeeklyHours = 14,
                preferredStartHour = 9,
                preferredEndHour = 17,
                avoidBackToBackBuildings = true,
                preferMorningLectures = true
            )
            store.mutateWorkspace { snap ->
                snap.copy(faculty = snap.faculty + newMember)
            }
            newMember
        }

        if (rememberInJson) {
            store.saveCredential(
                SavedSsoCredential(
                    ssoId = resolvedFaculty.ssoId,
                    email = resolvedFaculty.email,
                    passcode = passcode,
                    facultyId = resolvedFaculty.id,
                    rememberCredentials = true,
                    savedAtMillis = System.currentTimeMillis()
                )
            )
        } else {
            store.saveCredential(null)
        }

        prefs.setActiveFacultyId(resolvedFaculty.id)
        return resolvedFaculty
    }

    override suspend fun signInWithDirectoryFaculty(facultyId: String, rememberInJson: Boolean) {
        store.initialize()
        val faculty = store.workspace.value.faculty.firstOrNull { it.id == facultyId } ?: return
        if (rememberInJson) {
            store.saveCredential(
                SavedSsoCredential(
                    ssoId = faculty.ssoId,
                    email = faculty.email,
                    passcode = "sso-token-${faculty.ssoId}",
                    facultyId = faculty.id,
                    rememberCredentials = true,
                    savedAtMillis = System.currentTimeMillis()
                )
            )
        }
        prefs.setActiveFacultyId(faculty.id)
    }

    override suspend fun signOut(clearSavedCredentials: Boolean) {
        if (clearSavedCredentials) {
            store.saveCredential(null)
        }
        prefs.setActiveFacultyId(null)
    }
}

class JsonTimetableRepository(
    private val store: JsonCredentialAndTimetableStore,
    private val prefs: PreferencesRepository
) : TimetableRepository {

    override val semesters: Flow<List<Semester>> = store.workspace.map { snap ->
        snap.semesters.sortedBy { it.sortOrder }
    }

    override fun observeFaculty(facultyId: String): Flow<FacultyMember?> =
        store.workspace.map { snap -> snap.faculty.firstOrNull { it.id == facultyId } }

    override fun observeSessionsForFacultySemester(
        facultyId: String,
        semesterId: String
    ): Flow<List<CourseSession>> = store.workspace.map { snap ->
        snap.sessions
            .filter { it.facultyId == facultyId && it.semesterId == semesterId }
            .sortedWith(compareBy<CourseSession> { it.dayOfWeek }.thenBy { it.startMinuteOfDay })
    }

    override fun observeAllSessionsInSemester(semesterId: String): Flow<List<CourseSession>> =
        store.workspace.map { snap ->
            snap.sessions.filter { it.semesterId == semesterId }
        }

    override fun observeHistoricalExperiences(facultyId: String): Flow<List<HistoricalExperience>> =
        store.workspace.map { snap ->
            snap.historicalExperiences
                .filter { it.facultyId == facultyId }
                .sortedWith(
                    compareByDescending<HistoricalExperience> { it.satisfactionScore }
                        .thenByDescending { it.attendanceRate }
                )
        }

    override fun observeAutoUpdateLogs(facultyId: String): Flow<List<AutoUpdateLog>> =
        store.workspace.map { snap ->
            snap.autoUpdateLogs
                .filter { it.facultyId == facultyId }
                .sortedByDescending { it.timestampMillis }
                .take(20)
        }

    override suspend fun upsertSession(session: CourseSession) {
        val updatedSession = session.copy(updatedAtMillis = System.currentTimeMillis())
        store.mutateWorkspace { snap ->
            val filtered = snap.sessions.filterNot { it.id == updatedSession.id }
            snap.copy(sessions = filtered + updatedSession)
        }
    }

    override suspend fun deleteSession(sessionId: String) {
        store.mutateWorkspace { snap ->
            snap.copy(sessions = snap.sessions.filterNot { it.id == sessionId })
        }
    }

    override suspend fun toggleLock(session: CourseSession) {
        upsertSession(session.copy(isLocked = !session.isLocked))
    }

    override suspend fun updateFacultyPreferences(updated: FacultyMember) {
        store.mutateWorkspace { snap ->
            snap.copy(
                faculty = snap.faculty.map { if (it.id == updated.id) updated else it }
            )
        }
    }

    override suspend fun restoreSnapshot(
        facultyId: String,
        semesterId: String,
        previousSessions: List<CourseSession>
    ) {
        store.mutateWorkspace { snap ->
            val others = snap.sessions.filterNot { it.facultyId == facultyId && it.semesterId == semesterId }
            val log = AutoUpdateLog(
                id = "log_${UUID.randomUUID().toString().take(8)}",
                facultyId = facultyId,
                semesterId = semesterId,
                actionTitle = "Restored Previous Schedule Snapshot",
                summary = "Reverted automatic timetable changes back to ${previousSessions.size} prior sessions.",
                affectedCount = previousSessions.size,
                timestampMillis = System.currentTimeMillis()
            )
            snap.copy(
                sessions = others + previousSessions,
                autoUpdateLogs = snap.autoUpdateLogs + log
            )
        }
    }

    override suspend fun autoResolveSemesterConflicts(
        faculty: FacultyMember,
        semesterId: String,
        goal: OptimizationGoal
    ): TimetableIntelligenceEngine.AutoResolveOutcome {
        val snap = store.workspace.value
        val currentMine = snap.sessions.filter { it.facultyId == faculty.id && it.semesterId == semesterId }
        val allInSemester = snap.sessions.filter { it.semesterId == semesterId }
        val history = snap.historicalExperiences.filter { it.facultyId == faculty.id }

        val outcome = TimetableIntelligenceEngine.autoResolveConflicts(
            faculty = faculty,
            facultySessions = currentMine,
            allSemesterSessions = allInSemester,
            historicalExperiences = history,
            goal = goal
        )

        if (outcome.resolvedCount > 0) {
            store.mutateWorkspace { current ->
                val others = current.sessions.filterNot { it.facultyId == faculty.id && it.semesterId == semesterId }
                val log = AutoUpdateLog(
                    id = "log_${UUID.randomUUID().toString().take(8)}",
                    facultyId = faculty.id,
                    semesterId = semesterId,
                    actionTitle = "Auto-Resolved ${outcome.resolvedCount} Schedule Issue(s)",
                    summary = outcome.changesDescriptions.joinToString("; "),
                    affectedCount = outcome.resolvedCount,
                    timestampMillis = System.currentTimeMillis()
                )
                current.copy(
                    sessions = others + outcome.updatedSessions,
                    autoUpdateLogs = current.autoUpdateLogs + log
                )
            }
        }
        return outcome
    }

    override suspend fun applyRecommendation(
        faculty: FacultyMember,
        recommendation: ScheduleRecommendation
    ): CourseSession? {
        val snap = store.workspace.value
        val target = snap.sessions.firstOrNull {
            it.facultyId == faculty.id && it.id == recommendation.targetSessionId
        } ?: return null

        val updated = target.copy(
            dayOfWeek = recommendation.proposedDayOfWeek,
            startMinuteOfDay = recommendation.proposedStartMinute,
            roomCode = recommendation.proposedRoomCode,
            building = recommendation.proposedBuilding,
            sourceNote = "Applied recommendation: ${recommendation.impactBadge}",
            updatedAtMillis = System.currentTimeMillis()
        )

        store.mutateWorkspace { current ->
            val others = current.sessions.filterNot { it.id == updated.id }
            val log = AutoUpdateLog(
                id = "log_${UUID.randomUUID().toString().take(8)}",
                facultyId = faculty.id,
                semesterId = updated.semesterId,
                actionTitle = "Applied Historical Recommendation",
                summary = "${updated.courseCode} moved to ${TimetableFormatters.formatSlotLabel(updated.dayOfWeek, updated.startMinuteOfDay)} in ${updated.roomCode}.",
                affectedCount = 1,
                timestampMillis = System.currentTimeMillis()
            )
            current.copy(
                sessions = others + updated,
                autoUpdateLogs = current.autoUpdateLogs + log
            )
        }
        return updated
    }

    override suspend fun autoPrepareNewSemester(
        faculty: FacultyMember,
        sourceSemesterId: String,
        targetSemesterId: String,
        targetSemesterName: String,
        sourceSemesterName: String,
        goal: OptimizationGoal
    ): List<CourseSession> {
        val snap = store.workspace.value
        val sourceSessions = snap.sessions.filter { it.facultyId == faculty.id && it.semesterId == sourceSemesterId }
        val existingTargetAll = snap.sessions.filter { it.semesterId == targetSemesterId }
        val history = snap.historicalExperiences.filter { it.facultyId == faculty.id }

        val generated = TimetableIntelligenceEngine.prepareNewSemesterDraft(
            faculty = faculty,
            sourceHistoricalSessions = sourceSessions,
            targetSemesterId = targetSemesterId,
            existingInstitutionSessionsInTarget = existingTargetAll,
            historicalExperiences = history,
            goal = goal
        )

        store.mutateWorkspace { current ->
            val others = current.sessions.filterNot { it.facultyId == faculty.id && it.semesterId == targetSemesterId }
            val log = AutoUpdateLog(
                id = "log_${UUID.randomUUID().toString().take(8)}",
                facultyId = faculty.id,
                semesterId = targetSemesterId,
                actionTitle = "Auto-Prepared $targetSemesterName Draft",
                summary = "Rolled over ${generated.size} sessions from $sourceSemesterName optimized for '${goal.label}'.",
                affectedCount = generated.size,
                timestampMillis = System.currentTimeMillis()
            )
            current.copy(
                sessions = others + generated,
                autoUpdateLogs = current.autoUpdateLogs + log
            )
        }
        prefs.setSelectedSemesterId(targetSemesterId)
        return generated
    }
}
