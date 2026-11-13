package com.example.ui.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.di.AppContainer
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
import com.example.domain.WorkloadSummary
import com.example.domain.conflict.TimetableConflictChecker
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.PreferencesRepository
import com.example.domain.repository.RecommendationAiRepository
import com.example.domain.repository.TimetableRepository
import com.example.domain.usecase.AnalyzeTimetableUseCase
import com.example.domain.usecase.AutoResolveConflictsUseCase
import com.example.domain.usecase.CheckSessionConflictUseCase
import com.example.domain.usecase.PrepareNewSemesterUseCase
import com.example.domain.usecase.ScheduleAnalysis
import com.example.platform.ConflictWarningEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

enum class TimetableViewMode {
    WEEK_GRID,
    AGENDA_LIST
}

@Immutable
data class UiBannerMessage(
    val message: String,
    val canUndo: Boolean = false
)

@Immutable
data class MainUiState(
    val isInitializing: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val savedCredential: SavedSsoCredential? = null,
    val directoryFaculty: List<FacultyMember> = emptyList(),
    val activeFaculty: FacultyMember? = null,
    val semesters: List<Semester> = emptyList(),
    val selectedSemester: Semester? = null,
    val sessions: List<CourseSession> = emptyList(),
    val allSemesterSessions: List<CourseSession> = emptyList(),
    val conflicts: List<ScheduleConflict> = emptyList(),
    val workloadSummary: WorkloadSummary? = null,
    val historicalExperiences: List<HistoricalExperience> = emptyList(),
    val autoUpdateLogs: List<AutoUpdateLog> = emptyList(),
    val aiResult: AiRecommendationResult? = null,
    val isGeneratingAi: Boolean = false,
    val isPreparingSemester: Boolean = false,
    val viewMode: TimetableViewMode = TimetableViewMode.WEEK_GRID,
    val searchQuery: String = "",
    val dismissedRecommendationIds: Set<String> = emptySet(),
    val loginError: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class TimetableViewModel(
    private val authRepository: AuthRepository,
    private val timetableRepository: TimetableRepository,
    private val preferencesRepository: PreferencesRepository,
    private val aiRepository: RecommendationAiRepository,
    private val checkSessionConflictUseCase: CheckSessionConflictUseCase,
    private val analyzeTimetableUseCase: AnalyzeTimetableUseCase,
    private val autoResolveConflictsUseCase: AutoResolveConflictsUseCase,
    private val prepareNewSemesterUseCase: PrepareNewSemesterUseCase
) : ViewModel() {

    private val _viewMode = MutableStateFlow(TimetableViewMode.WEEK_GRID)
    private val _searchQuery = MutableStateFlow("")
    private val _dismissedRecIds = MutableStateFlow<Set<String>>(emptySet())
    private val _aiResult = MutableStateFlow<AiRecommendationResult?>(null)
    private val _isGeneratingAi = MutableStateFlow(false)
    private val _isPreparingSemester = MutableStateFlow(false)
    private val _loginError = MutableStateFlow<String?>(null)
    private val _isInitialized = MutableStateFlow(false)

    private var lastSnapshotForUndo: Pair<String, List<CourseSession>>? = null
    private var lastAlertedConflictSignature: String? = null

    private val _snackbarEvents = MutableSharedFlow<UiBannerMessage>(extraBufferCapacity = 4)
    val snackbarEvents: SharedFlow<UiBannerMessage> = _snackbarEvents.asSharedFlow()

    private val _conflictWarnings = MutableSharedFlow<ConflictWarningEvent>(extraBufferCapacity = 4)
    val conflictWarnings: SharedFlow<ConflictWarningEvent> = _conflictWarnings.asSharedFlow()

    init {
        viewModelScope.launch {
            authRepository.initialize()
            _isInitialized.value = true
        }
    }

    private val activeFacultyFlow: StateFlow<FacultyMember?> =
        preferencesRepository.activeFacultyId
            .flatMapLatest { id ->
                if (id == null) flowOf(null) else timetableRepository.observeFaculty(id)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    private val selectedSemesterFlow: StateFlow<Semester?> =
        combine(timetableRepository.semesters, preferencesRepository.selectedSemesterId) { semesters, selectedId ->
            semesters.firstOrNull { it.id == selectedId }
                ?: semesters.firstOrNull { it.status == "ACTIVE" }
                ?: semesters.firstOrNull()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    private val sessionsFlow = combine(activeFacultyFlow, selectedSemesterFlow) { fac, sem ->
        fac?.id to sem?.id
    }.flatMapLatest { (facId, semId) ->
        if (facId != null && semId != null) {
            timetableRepository.observeSessionsForFacultySemester(facId, semId)
        } else {
            flowOf(emptyList())
        }
    }

    private val allSemesterSessionsFlow = selectedSemesterFlow.flatMapLatest { sem ->
        if (sem != null) timetableRepository.observeAllSessionsInSemester(sem.id) else flowOf(emptyList())
    }

    private val historyFlow = activeFacultyFlow.flatMapLatest { fac ->
        if (fac != null) timetableRepository.observeHistoricalExperiences(fac.id) else flowOf(emptyList())
    }

    private val logsFlow = activeFacultyFlow.flatMapLatest { fac ->
        if (fac != null) timetableRepository.observeAutoUpdateLogs(fac.id) else flowOf(emptyList())
    }

    private val authAndMetaFlow = combine(
        _isInitialized,
        preferencesRepository.themeMode,
        authRepository.savedCredential,
        authRepository.directoryFaculty,
        activeFacultyFlow
    ) { initialized, theme, cred, directory, activeFac ->
        AuthAndMetaTuple(initialized, theme, cred, directory, activeFac)
    }

    private val scheduleInputFlow = combine(
        timetableRepository.semesters,
        selectedSemesterFlow,
        sessionsFlow,
        allSemesterSessionsFlow,
        historyFlow,
        logsFlow
    ) { arr ->
        @Suppress("UNCHECKED_CAST")
        ScheduleInputTuple(
            semesters = arr[0] as List<Semester>,
            selectedSemester = arr[1] as Semester?,
            sessions = arr[2] as List<CourseSession>,
            allSessions = arr[3] as List<CourseSession>,
            history = arr[4] as List<HistoricalExperience>,
            logs = arr[5] as List<AutoUpdateLog>
        )
    }

    private val analysisFlow = combine(
        activeFacultyFlow,
        authRepository.directoryFaculty,
        scheduleInputFlow
    ) { faculty, allFaculty, sched ->
        Triple(faculty, allFaculty, sched)
    }.mapLatest { (faculty, allFaculty, sched) ->
        if (faculty == null) {
            null
        } else {
            val analysis = analyzeTimetableUseCase(
                faculty = faculty,
                facultySessions = sched.sessions,
                allSemesterSessions = sched.allSessions,
                historicalExperiences = sched.history,
                allFaculty = allFaculty.ifEmpty { listOf(faculty) }
            )
            val severeSummary = TimetableConflictChecker.summarizeDetectedConflicts(analysis.conflicts)
            val semesterId = sched.selectedSemester?.id.orEmpty()
            val severeIds = analysis.conflicts.filter { it.type.isSevere }.joinToString(",") { it.id }
            val signature = "${faculty.id}|$semesterId|$severeIds"
            if (severeSummary == null) {
                lastAlertedConflictSignature = null
            } else if (lastAlertedConflictSignature != signature) {
                lastAlertedConflictSignature = signature
                _conflictWarnings.tryEmit(
                    ConflictWarningEvent(
                        id = signature,
                        title = severeSummary.first,
                        message = severeSummary.second,
                        overlapCount = analysis.conflicts.count { it.type == com.example.domain.ConflictType.FACULTY_TIME_OVERLAP },
                        venueClashCount = analysis.conflicts.count { it.type == com.example.domain.ConflictType.ROOM_DOUBLE_BOOKED }
                    )
                )
            }
            analysis
        }
    }.flowOn(Dispatchers.Default)

    private val uiControlsFlow = combine(
        _viewMode,
        _searchQuery,
        _dismissedRecIds,
        _aiResult,
        _isGeneratingAi,
        _isPreparingSemester,
        _loginError
    ) { arr ->
        @Suppress("UNCHECKED_CAST")
        UiControlsTuple(
            viewMode = arr[0] as TimetableViewMode,
            searchQuery = arr[1] as String,
            dismissedIds = arr[2] as Set<String>,
            aiResult = arr[3] as AiRecommendationResult?,
            isGeneratingAi = arr[4] as Boolean,
            isPreparing = arr[5] as Boolean,
            loginError = arr[6] as String?
        )
    }

    val uiState: StateFlow<MainUiState> = combine(
        authAndMetaFlow,
        scheduleInputFlow,
        analysisFlow,
        uiControlsFlow
    ) { meta, sched, analysis, controls ->
        val filteredAiResult = controls.aiResult?.let { result ->
            result.copy(
                recommendations = result.recommendations.filterNot { it.id in controls.dismissedIds }
            )
        }

        MainUiState(
            isInitializing = !meta.isInitialized,
            themeMode = meta.themeMode,
            savedCredential = meta.savedCredential,
            directoryFaculty = meta.directory,
            activeFaculty = meta.activeFaculty,
            semesters = sched.semesters,
            selectedSemester = sched.selectedSemester,
            sessions = sched.sessions,
            allSemesterSessions = sched.allSessions,
            conflicts = analysis?.conflicts.orEmpty(),
            workloadSummary = analysis?.workloadSummary,
            historicalExperiences = sched.history,
            autoUpdateLogs = sched.logs,
            aiResult = filteredAiResult,
            isGeneratingAi = controls.isGeneratingAi,
            isPreparingSemester = controls.isPreparing,
            viewMode = controls.viewMode,
            searchQuery = controls.searchQuery,
            dismissedRecommendationIds = controls.dismissedIds,
            loginError = controls.loginError
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = MainUiState()
    )

    fun toggleLightDarkMode(currentlyDark: Boolean) {
        viewModelScope.launch {
            val next = if (currentlyDark) ThemeMode.LIGHT else ThemeMode.DARK
            preferencesRepository.setThemeMode(next)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferencesRepository.setThemeMode(mode)
        }
    }

    fun signInWithSsoCredentials(identifier: String, passcode: String, rememberInJson: Boolean) {
        viewModelScope.launch {
            _loginError.value = null
            if (identifier.trim().isEmpty()) {
                _loginError.value = "Enter your institutional SSO ID or university email."
                return@launch
            }
            val result = authRepository.signInWithSsoCredentials(identifier, passcode, rememberInJson)
            if (result == null) {
                _loginError.value = "Unable to verify institutional SSO credentials."
            } else {
                _aiResult.value = null
                _dismissedRecIds.value = emptySet()
            }
        }
    }

    fun signInWithDirectoryFaculty(facultyId: String, rememberInJson: Boolean = true) {
        viewModelScope.launch {
            _loginError.value = null
            _aiResult.value = null
            _dismissedRecIds.value = emptySet()
            authRepository.signInWithDirectoryFaculty(facultyId, rememberInJson)
        }
    }

    fun signOut(clearSavedCredentials: Boolean = false) {
        viewModelScope.launch {
            _aiResult.value = null
            _dismissedRecIds.value = emptySet()
            lastSnapshotForUndo = null
            authRepository.signOut(clearSavedCredentials)
        }
    }

    fun selectSemester(semesterId: String) {
        viewModelScope.launch {
            _aiResult.value = null
            _dismissedRecIds.value = emptySet()
            preferencesRepository.setSelectedSemesterId(semesterId)
        }
    }

    fun setViewMode(mode: TimetableViewMode) {
        _viewMode.value = mode
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun refreshAiRecommendations() {
        val state = uiState.value
        val faculty = state.activeFaculty ?: return
        val semester = state.selectedSemester ?: return
        val workload = state.workloadSummary ?: return

        viewModelScope.launch {
            _isGeneratingAi.value = true
            try {
                val result = aiRepository.generateScheduleRecommendations(
                    faculty = faculty,
                    semester = semester,
                    facultySessions = state.sessions,
                    allSemesterSessions = state.allSemesterSessions,
                    conflicts = state.conflicts,
                    workload = workload,
                    historicalExperiences = state.historicalExperiences
                )
                _aiResult.value = result
            } finally {
                _isGeneratingAi.value = false
            }
        }
    }

    fun applyRecommendation(rec: ScheduleRecommendation) {
        val state = uiState.value
        val faculty = state.activeFaculty ?: return
        val semester = state.selectedSemester ?: return

        viewModelScope.launch {
            lastSnapshotForUndo = semester.id to state.sessions
            val updated = timetableRepository.applyRecommendation(faculty, rec)
            if (updated != null) {
                _dismissedRecIds.value = _dismissedRecIds.value + rec.id
                _snackbarEvents.emit(
                    UiBannerMessage(
                        message = "Updated ${updated.courseCode} schedule automatically",
                        canUndo = true
                    )
                )
                refreshAiRecommendations()
            }
        }
    }

    fun dismissRecommendation(recId: String) {
        _dismissedRecIds.value = _dismissedRecIds.value + recId
    }

    fun autoResolveConflicts(goal: OptimizationGoal = OptimizationGoal.BALANCE_WORKLOAD) {
        val state = uiState.value
        val faculty = state.activeFaculty ?: return
        val semester = state.selectedSemester ?: return

        viewModelScope.launch {
            lastSnapshotForUndo = semester.id to state.sessions
            val outcome = autoResolveConflictsUseCase(
                faculty = faculty,
                semesterId = semester.id,
                goal = goal
            )
            if (outcome.resolvedCount > 0) {
                _snackbarEvents.emit(
                    UiBannerMessage(
                        message = "Auto-updated ${outcome.resolvedCount} session(s) to reduce conflicts",
                        canUndo = true
                    )
                )
                refreshAiRecommendations()
            } else {
                _snackbarEvents.emit(
                    UiBannerMessage(
                        message = "All unlocked sessions are already in optimal conflict-free slots",
                        canUndo = false
                    )
                )
            }
        }
    }

    fun undoLastAutoUpdate() {
        val state = uiState.value
        val faculty = state.activeFaculty ?: return
        val snapshot = lastSnapshotForUndo ?: return
        lastSnapshotForUndo = null

        viewModelScope.launch {
            timetableRepository.restoreSnapshot(faculty.id, snapshot.first, snapshot.second)
            _snackbarEvents.emit(UiBannerMessage("Restored previous timetable snapshot", canUndo = false))
            refreshAiRecommendations()
        }
    }

    fun saveSession(
        existingId: String?,
        courseCode: String,
        courseTitle: String,
        section: String,
        sessionType: String,
        dayOfWeek: Int,
        startMinuteOfDay: Int,
        durationMinutes: Int,
        roomCode: String,
        building: String,
        enrolledStudents: Int,
        isLocked: Boolean
    ) {
        val state = uiState.value
        val faculty = state.activeFaculty ?: return
        val semester = state.selectedSemester ?: return

        viewModelScope.launch {
            val session = CourseSession(
                id = existingId ?: "ses_${UUID.randomUUID().toString().take(8)}",
                facultyId = faculty.id,
                semesterId = semester.id,
                courseCode = courseCode.trim().uppercase(),
                courseTitle = courseTitle.trim(),
                section = section.trim().ifBlank { "Sec A" },
                sessionType = sessionType,
                dayOfWeek = dayOfWeek,
                startMinuteOfDay = startMinuteOfDay,
                durationMinutes = durationMinutes,
                roomCode = roomCode.trim(),
                building = building.trim(),
                enrolledStudents = enrolledStudents.coerceAtLeast(1),
                capacity = (enrolledStudents + 15).coerceAtLeast(30),
                isLocked = isLocked,
                sourceNote = if (existingId == null) "Added manually via SSO Timetable" else "Edited by faculty",
                updatedAtMillis = System.currentTimeMillis()
            )
            val conflictCheck = checkSessionConflictUseCase(
                candidate = session,
                facultySessions = state.sessions,
                allSemesterSessions = state.allSemesterSessions
            )
            timetableRepository.upsertSession(session)

            if (conflictCheck.hasConflict) {
                val signature = "${faculty.id}|${semester.id}|${session.id}|${conflictCheck.warningTitle}"
                lastAlertedConflictSignature = signature
                _conflictWarnings.emit(
                    ConflictWarningEvent(
                        id = signature,
                        title = conflictCheck.warningTitle,
                        message = conflictCheck.warningMessage,
                        overlapCount = conflictCheck.overlappingTeachingSessions.size,
                        venueClashCount = conflictCheck.overlappingVenueBookings.size
                    )
                )
                _snackbarEvents.emit(
                    UiBannerMessage(
                        message = conflictCheck.warningMessage,
                        canUndo = false
                    )
                )
            } else {
                _snackbarEvents.emit(
                    UiBannerMessage(
                        message = if (existingId == null) "Added ${session.courseCode}" else "Saved ${session.courseCode}",
                        canUndo = false
                    )
                )
            }
        }
    }

    fun deleteSession(session: CourseSession) {
        viewModelScope.launch {
            timetableRepository.deleteSession(session.id)
            _snackbarEvents.emit(UiBannerMessage("Removed ${session.courseCode}", canUndo = false))
        }
    }

    fun toggleSessionLock(session: CourseSession) {
        viewModelScope.launch {
            timetableRepository.toggleLock(session)
        }
    }

    fun prepareNewSemester(
        sourceSemesterId: String,
        targetSemesterId: String,
        goal: OptimizationGoal
    ) {
        val state = uiState.value
        val faculty = state.activeFaculty ?: return
        val sourceSem = state.semesters.firstOrNull { it.id == sourceSemesterId } ?: return
        val targetSem = state.semesters.firstOrNull { it.id == targetSemesterId } ?: return

        viewModelScope.launch {
            _isPreparingSemester.value = true
            try {
                val created = prepareNewSemesterUseCase(
                    faculty = faculty,
                    sourceSemesterId = sourceSem.id,
                    targetSemesterId = targetSem.id,
                    targetSemesterName = targetSem.name,
                    sourceSemesterName = sourceSem.name,
                    goal = goal
                )
                _snackbarEvents.emit(
                    UiBannerMessage(
                        message = "Prepared ${created.size} conflict-free sessions for ${targetSem.name}",
                        canUndo = false
                    )
                )
                refreshAiRecommendations()
            } finally {
                _isPreparingSemester.value = false
            }
        }
    }

    fun updatePreferences(
        maxWeeklyHours: Int,
        preferredStartHour: Int,
        preferredEndHour: Int,
        avoidBackToBackBuildings: Boolean,
        preferMorningLectures: Boolean
    ) {
        val faculty = uiState.value.activeFaculty ?: return
        viewModelScope.launch {
            val updated = faculty.copy(
                maxWeeklyHours = maxWeeklyHours.coerceIn(6, 24),
                preferredStartHour = preferredStartHour.coerceIn(7, 13),
                preferredEndHour = preferredEndHour.coerceIn(14, 21),
                avoidBackToBackBuildings = avoidBackToBackBuildings,
                preferMorningLectures = preferMorningLectures
            )
            timetableRepository.updateFacultyPreferences(updated)
            _snackbarEvents.emit(UiBannerMessage("Saved faculty scheduling preferences"))
        }
    }

    private data class AuthAndMetaTuple(
        val isInitialized: Boolean,
        val themeMode: ThemeMode,
        val savedCredential: SavedSsoCredential?,
        val directory: List<FacultyMember>,
        val activeFaculty: FacultyMember?
    )

    private data class ScheduleInputTuple(
        val semesters: List<Semester>,
        val selectedSemester: Semester?,
        val sessions: List<CourseSession>,
        val allSessions: List<CourseSession>,
        val history: List<HistoricalExperience>,
        val logs: List<AutoUpdateLog>
    )

    private data class UiControlsTuple(
        val viewMode: TimetableViewMode,
        val searchQuery: String,
        val dismissedIds: Set<String>,
        val aiResult: AiRecommendationResult?,
        val isGeneratingAi: Boolean,
        val isPreparing: Boolean,
        val loginError: String?
    )

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return TimetableViewModel(
                        authRepository = container.authRepository,
                        timetableRepository = container.timetableRepository,
                        preferencesRepository = container.preferencesRepository,
                        aiRepository = container.aiRepository,
                        checkSessionConflictUseCase = container.checkSessionConflictUseCase,
                        analyzeTimetableUseCase = container.analyzeTimetableUseCase,
                        autoResolveConflictsUseCase = container.autoResolveConflictsUseCase,
                        prepareNewSemesterUseCase = container.prepareNewSemesterUseCase
                    ) as T
                }
            }
    }
}
