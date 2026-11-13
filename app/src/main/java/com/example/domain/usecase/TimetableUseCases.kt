package com.example.domain.usecase

import com.example.domain.CourseSession
import com.example.domain.FacultyMember
import com.example.domain.HistoricalExperience
import com.example.domain.OptimizationGoal
import com.example.domain.ScheduleConflict
import com.example.domain.TimetableIntelligenceEngine
import com.example.domain.WorkloadSummary
import com.example.domain.conflict.OverlapCheckResult
import com.example.domain.conflict.TimetableConflictChecker
import com.example.domain.repository.TimetableRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ScheduleAnalysis(
    val conflicts: List<ScheduleConflict>,
    val workloadSummary: WorkloadSummary
)

class CheckSessionConflictUseCase(
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    suspend operator fun invoke(
        candidate: CourseSession,
        facultySessions: List<CourseSession>,
        allSemesterSessions: List<CourseSession>
    ): OverlapCheckResult = withContext(dispatcher) {
        TimetableConflictChecker.checkCandidateSession(
            candidate = candidate,
            facultySessions = facultySessions,
            allSemesterSessions = allSemesterSessions
        )
    }
}

class AnalyzeTimetableUseCase(
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    suspend operator fun invoke(
        faculty: FacultyMember,
        facultySessions: List<CourseSession>,
        allSemesterSessions: List<CourseSession>,
        historicalExperiences: List<HistoricalExperience>,
        allFaculty: List<FacultyMember> = listOf(faculty)
    ): ScheduleAnalysis = withContext(dispatcher) {
        val conflicts = TimetableIntelligenceEngine.detectConflicts(
            faculty = faculty,
            facultySessions = facultySessions,
            allSemesterSessions = allSemesterSessions,
            historicalExperiences = historicalExperiences
        )
        val workload = TimetableIntelligenceEngine.computeWorkload(
            faculty = faculty,
            facultySessions = facultySessions,
            conflicts = conflicts,
            allFaculty = allFaculty,
            allSemesterSessions = allSemesterSessions
        )
        ScheduleAnalysis(conflicts = conflicts, workloadSummary = workload)
    }
}

class AutoResolveConflictsUseCase(
    private val repository: TimetableRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    suspend operator fun invoke(
        faculty: FacultyMember,
        semesterId: String,
        goal: OptimizationGoal = OptimizationGoal.BALANCE_WORKLOAD
    ): TimetableIntelligenceEngine.AutoResolveOutcome = withContext(dispatcher) {
        repository.autoResolveSemesterConflicts(faculty, semesterId, goal)
    }
}

class PrepareNewSemesterUseCase(
    private val repository: TimetableRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    suspend operator fun invoke(
        faculty: FacultyMember,
        sourceSemesterId: String,
        targetSemesterId: String,
        targetSemesterName: String,
        sourceSemesterName: String,
        goal: OptimizationGoal
    ): List<CourseSession> = withContext(dispatcher) {
        repository.autoPrepareNewSemester(
            faculty = faculty,
            sourceSemesterId = sourceSemesterId,
            targetSemesterId = targetSemesterId,
            targetSemesterName = targetSemesterName,
            sourceSemesterName = sourceSemesterName,
            goal = goal
        )
    }
}
