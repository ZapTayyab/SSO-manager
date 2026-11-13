package com.example.domain.repository

import com.example.domain.AiRecommendationResult
import com.example.domain.AutoUpdateLog
import com.example.domain.CourseSession
import com.example.domain.FacultyMember
import com.example.domain.HistoricalExperience
import com.example.domain.OptimizationGoal
import com.example.domain.SavedSsoCredential
import com.example.domain.ScheduleConflict
import com.example.domain.ScheduleRecommendation
import com.example.domain.Semester
import com.example.domain.ThemeMode
import com.example.domain.TimetableIntelligenceEngine
import com.example.domain.WorkloadSummary
import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {
    val themeMode: Flow<ThemeMode>
    val activeFacultyId: Flow<String?>
    val selectedSemesterId: Flow<String?>

    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setActiveFacultyId(facultyId: String?)
    suspend fun setSelectedSemesterId(semesterId: String)
}

interface AuthRepository {
    val savedCredential: Flow<SavedSsoCredential?>
    val directoryFaculty: Flow<List<FacultyMember>>

    suspend fun initialize()
    suspend fun signInWithSsoCredentials(
        identifier: String,
        passcode: String,
        rememberInJson: Boolean
    ): FacultyMember?

    suspend fun signInWithDirectoryFaculty(facultyId: String, rememberInJson: Boolean)
    suspend fun signOut(clearSavedCredentials: Boolean = false)
}

interface TimetableRepository {
    val semesters: Flow<List<Semester>>

    fun observeFaculty(facultyId: String): Flow<FacultyMember?>
    fun observeSessionsForFacultySemester(facultyId: String, semesterId: String): Flow<List<CourseSession>>
    fun observeAllSessionsInSemester(semesterId: String): Flow<List<CourseSession>>
    fun observeHistoricalExperiences(facultyId: String): Flow<List<HistoricalExperience>>
    fun observeAutoUpdateLogs(facultyId: String): Flow<List<AutoUpdateLog>>

    suspend fun upsertSession(session: CourseSession)
    suspend fun deleteSession(sessionId: String)
    suspend fun toggleLock(session: CourseSession)
    suspend fun updateFacultyPreferences(updated: FacultyMember)
    suspend fun restoreSnapshot(facultyId: String, semesterId: String, previousSessions: List<CourseSession>)
    suspend fun autoResolveSemesterConflicts(
        faculty: FacultyMember,
        semesterId: String,
        goal: OptimizationGoal
    ): TimetableIntelligenceEngine.AutoResolveOutcome

    suspend fun applyRecommendation(faculty: FacultyMember, recommendation: ScheduleRecommendation): CourseSession?
    suspend fun autoPrepareNewSemester(
        faculty: FacultyMember,
        sourceSemesterId: String,
        targetSemesterId: String,
        targetSemesterName: String,
        sourceSemesterName: String,
        goal: OptimizationGoal
    ): List<CourseSession>
}

interface RecommendationAiRepository {
    suspend fun generateScheduleRecommendations(
        faculty: FacultyMember,
        semester: Semester,
        facultySessions: List<CourseSession>,
        allSemesterSessions: List<CourseSession>,
        conflicts: List<ScheduleConflict>,
        workload: WorkloadSummary,
        historicalExperiences: List<HistoricalExperience>
    ): AiRecommendationResult
}
