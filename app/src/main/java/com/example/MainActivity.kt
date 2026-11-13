package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.di.AppContainer
import com.example.domain.CourseSession
import com.example.domain.OptimizationGoal
import com.example.domain.ThemeMode
import com.example.platform.ConflictAlertNotifier
import com.example.platform.TimetablePdfExporter
import com.example.ui.screens.AnimatedNavItem
import com.example.ui.screens.EnterpriseCleanHeader
import com.example.ui.screens.InsightsScreen
import com.example.ui.screens.ModernAnimatedNavBar
import com.example.ui.screens.PdfExportBottomSheet
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SessionEditorBottomSheet
import com.example.ui.screens.SsoLoginScreen
import com.example.ui.screens.TimetableScreen
import com.example.ui.screens.WorkloadScreen
import com.example.ui.theme.SsoTimetableTheme
import com.example.ui.viewmodel.MainUiState
import com.example.ui.viewmodel.TimetableViewModel
import kotlinx.coroutines.launch

private const val ROUTE_TIMETABLE = "timetable"
private const val ROUTE_INSIGHTS = "insights"
private const val ROUTE_WORKLOAD = "workload"
private const val ROUTE_PROFILE = "profile"

class MainActivity : ComponentActivity() {
    private val appContainer by lazy { AppContainer(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: TimetableViewModel = viewModel(
                factory = TimetableViewModel.factory(appContainer)
            )
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val isSystemDark = isSystemInDarkTheme()
            val isDarkMode = when (uiState.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemDark
            }

            SsoTimetableTheme(themeMode = uiState.themeMode) {
                SsoTimetableApp(
                    uiState = uiState,
                    isDarkMode = isDarkMode,
                    viewModel = viewModel,
                    conflictAlertNotifier = appContainer.conflictAlertNotifier,
                    pdfExporter = appContainer.pdfExporter
                )
            }
        }
    }
}

@Composable
fun SsoTimetableApp(
    uiState: MainUiState,
    isDarkMode: Boolean,
    viewModel: TimetableViewModel,
    conflictAlertNotifier: ConflictAlertNotifier? = null,
    pdfExporter: TimetablePdfExporter? = null
) {
    if (uiState.isInitializing) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val activeFaculty = uiState.activeFaculty
    if (activeFaculty == null) {
        SsoLoginScreen(
            directoryFaculty = uiState.directoryFaculty,
            savedCredential = uiState.savedCredential,
            isDarkMode = isDarkMode,
            loginError = uiState.loginError,
            onToggleTheme = { viewModel.toggleLightDarkMode(isDarkMode) },
            onSignInWithCredentials = viewModel::signInWithSsoCredentials,
            onSelectDirectoryFaculty = viewModel::signInWithDirectoryFaculty
        )
    } else {
        key(activeFaculty.id) {
            AuthenticatedTimetableShell(
                uiState = uiState,
                isDarkMode = isDarkMode,
                viewModel = viewModel,
                conflictAlertNotifier = conflictAlertNotifier,
                pdfExporter = pdfExporter
            )
        }
    }
}

@Composable
private fun AuthenticatedTimetableShell(
    uiState: MainUiState,
    isDarkMode: Boolean,
    viewModel: TimetableViewModel,
    conflictAlertNotifier: ConflictAlertNotifier?,
    pdfExporter: TimetablePdfExporter?
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val resolvedNotifier = remember(context, conflictAlertNotifier) {
        conflictAlertNotifier ?: ConflictAlertNotifier(context)
    }
    val resolvedPdfExporter = remember(context, pdfExporter) {
        pdfExporter ?: TimetablePdfExporter(context)
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_TIMETABLE
    val snackbarHostState = remember { SnackbarHostState() }

    var editingSession by remember { mutableStateOf<CourseSession?>(null) }
    var isEditorOpen by remember { mutableStateOf(false) }
    var isExportSheetOpen by remember { mutableStateOf(false) }
    var isExportingPdf by remember { mutableStateOf(false) }

    val createPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        val faculty = uiState.activeFaculty ?: return@rememberLauncherForActivityResult
        if (uri != null) {
            coroutineScope.launch {
                isExportingPdf = true
                try {
                    val ok = resolvedPdfExporter.writePdfToUri(
                        uri = uri,
                        faculty = faculty,
                        semester = uiState.selectedSemester,
                        sessions = uiState.sessions,
                        conflicts = uiState.conflicts
                    )
                    isExportSheetOpen = false
                    snackbarHostState.showSnackbar(
                        if (ok) "Weekly timetable PDF saved" else "Unable to save PDF file"
                    )
                } finally {
                    isExportingPdf = false
                }
            }
        }
    }

    val navItems = remember(uiState.conflicts.size) {
        listOf(
            AnimatedNavItem(
                route = ROUTE_TIMETABLE,
                label = "Timetable",
                selectedIcon = Icons.Filled.CalendarMonth,
                unselectedIcon = Icons.Outlined.CalendarMonth,
                badgeCount = uiState.conflicts.size
            ),
            AnimatedNavItem(
                route = ROUTE_INSIGHTS,
                label = "Advisor",
                selectedIcon = Icons.Filled.AutoAwesome,
                unselectedIcon = Icons.Outlined.AutoAwesome
            ),
            AnimatedNavItem(
                route = ROUTE_WORKLOAD,
                label = "Workload",
                selectedIcon = Icons.Filled.BarChart,
                unselectedIcon = Icons.Outlined.BarChart
            ),
            AnimatedNavItem(
                route = ROUTE_PROFILE,
                label = "SSO",
                selectedIcon = Icons.Filled.Person,
                unselectedIcon = Icons.Outlined.Person
            )
        )
    }

    val navigateTo: (String) -> Unit = remember(navController) {
        { route ->
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    // Smart back navigation: close sheets first, or return from secondary tabs to Timetable
    BackHandler(enabled = isEditorOpen || isExportSheetOpen || currentRoute != ROUTE_TIMETABLE) {
        when {
            isEditorOpen -> isEditorOpen = false
            isExportSheetOpen -> isExportSheetOpen = false
            else -> navigateTo(ROUTE_TIMETABLE)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.snackbarEvents.collect { banner ->
            val result = snackbarHostState.showSnackbar(
                message = banner.message,
                actionLabel = if (banner.canUndo) "Undo" else null,
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed && banner.canUndo) {
                viewModel.undoLastAutoUpdate()
            }
        }
    }

    LaunchedEffect(viewModel, resolvedNotifier) {
        viewModel.conflictWarnings.collect { warning ->
            resolvedNotifier.notifyConflict(warning)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 700.dp

        Row(modifier = Modifier.fillMaxSize()) {
            if (useNavigationRail) {
                NavigationRail(
                    modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
                ) {
                    Spacer(modifier = Modifier.weight(0.1f))
                    navItems.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationRailItem(
                            selected = selected,
                            onClick = { navigateTo(item.route) },
                            icon = {
                                BadgedBox(
                                    badge = {
                                        if (item.badgeCount > 0) {
                                            Badge { Text(item.badgeCount.toString()) }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                }
                            },
                            label = { Text(item.label) },
                            modifier = Modifier.testTag("nav_${item.route}")
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            Scaffold(
                modifier = Modifier.weight(1f),
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                topBar = {
                    val headerTitle = when (currentRoute) {
                        ROUTE_TIMETABLE -> stringResource(R.string.app_name)
                        ROUTE_INSIGHTS -> "Schedule Advisor"
                        ROUTE_WORKLOAD -> "Workload Balance"
                        ROUTE_PROFILE -> "SSO & Preferences"
                        else -> stringResource(R.string.app_name)
                    }
                    val facultyName = uiState.activeFaculty?.fullName ?: "Faculty"
                    val termName = uiState.selectedSemester?.name ?: "Academic Portal"
                    EnterpriseCleanHeader(
                        title = headerTitle,
                        subtitle = "$facultyName · $termName",
                        isDarkMode = isDarkMode,
                        onToggleTheme = { viewModel.toggleLightDarkMode(isDarkMode) },
                        showExportPdf = currentRoute == ROUTE_TIMETABLE && uiState.sessions.isNotEmpty(),
                        onExportPdfClick = { isExportSheetOpen = true },
                        modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
                    )
                },
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                floatingActionButton = {
                    if (currentRoute == ROUTE_TIMETABLE) {
                        FloatingActionButton(
                            onClick = {
                                editingSession = null
                                isEditorOpen = true
                            },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.testTag("add_session_fab")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = stringResource(R.string.action_add_session)
                            )
                        }
                    }
                },
                bottomBar = {
                    if (!useNavigationRail) {
                        ModernAnimatedNavBar(
                            items = navItems,
                            currentRoute = currentRoute,
                            onSelectRoute = navigateTo
                        )
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.TopCenter
                ) {
                    val routeOrder = remember {
                        mapOf(
                            ROUTE_TIMETABLE to 0,
                            ROUTE_INSIGHTS to 1,
                            ROUTE_WORKLOAD to 2,
                            ROUTE_PROFILE to 3
                        )
                    }

                    NavHost(
                        navController = navController,
                        startDestination = ROUTE_TIMETABLE,
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 840.dp),
                        enterTransition = {
                            val fromIdx = routeOrder[initialState.destination.route] ?: 0
                            val toIdx = routeOrder[targetState.destination.route] ?: 0
                            val dir = if (toIdx >= fromIdx) 1 else -1
                            fadeIn(tween(220)) + slideInHorizontally(tween(240)) { it / 10 * dir }
                        },
                        exitTransition = {
                            val fromIdx = routeOrder[initialState.destination.route] ?: 0
                            val toIdx = routeOrder[targetState.destination.route] ?: 0
                            val dir = if (toIdx >= fromIdx) -1 else 1
                            fadeOut(tween(180)) + slideOutHorizontally(tween(220)) { it / 10 * dir }
                        },
                        popEnterTransition = {
                            fadeIn(tween(220)) + slideInHorizontally(tween(240)) { -it / 10 }
                        },
                        popExitTransition = {
                            fadeOut(tween(180)) + slideOutHorizontally(tween(220)) { it / 10 }
                        }
                    ) {
                        composable(ROUTE_TIMETABLE) {
                            TimetableScreen(
                                uiState = uiState,
                                onSelectSemester = viewModel::selectSemester,
                                onViewModeChange = viewModel::setViewMode,
                                onSearchQueryChange = viewModel::setSearchQuery,
                                onAutoResolveClick = { viewModel.autoResolveConflicts() },
                                onSessionClick = { session ->
                                    editingSession = session
                                    isEditorOpen = true
                                },
                                onToggleLock = viewModel::toggleSessionLock,
                                onAddManualClick = {
                                    editingSession = null
                                    isEditorOpen = true
                                },
                                onNavigateToAdvisor = { navigateTo(ROUTE_INSIGHTS) }
                            )
                        }
                        composable(ROUTE_INSIGHTS) {
                            InsightsScreen(
                                uiState = uiState,
                                onRefreshRecommendations = viewModel::refreshAiRecommendations,
                                onApplyRecommendation = viewModel::applyRecommendation,
                                onDismissRecommendation = viewModel::dismissRecommendation,
                                onPrepareNewSemester = viewModel::prepareNewSemester,
                                onNavigateToTimetable = { navigateTo(ROUTE_TIMETABLE) }
                            )
                        }
                        composable(ROUTE_WORKLOAD) {
                            WorkloadScreen(
                                uiState = uiState,
                                onRebalanceClick = {
                                    viewModel.autoResolveConflicts(OptimizationGoal.BALANCE_WORKLOAD)
                                }
                            )
                        }
                        composable(ROUTE_PROFILE) {
                            ProfileScreen(
                                uiState = uiState,
                                isDarkMode = isDarkMode,
                                onToggleTheme = { viewModel.toggleLightDarkMode(isDarkMode) },
                                onSelectThemeMode = viewModel::setThemeMode,
                                onSavePreferences = viewModel::updatePreferences,
                                onSaveAndReoptimize = { maxH, startH, endH, avoidB, morning ->
                                    viewModel.updatePreferences(maxH, startH, endH, avoidB, morning)
                                    viewModel.autoResolveConflicts(OptimizationGoal.BALANCE_WORKLOAD)
                                },
                                onSignOut = viewModel::signOut
                            )
                        }
                    }
                }
            }
        }
    }

    if (isEditorOpen) {
        SessionEditorBottomSheet(
            initialSession = editingSession,
            existingFacultySessions = uiState.sessions,
            allSemesterSessions = uiState.allSemesterSessions,
            semesterId = uiState.selectedSemester?.id.orEmpty(),
            facultyId = uiState.activeFaculty?.id.orEmpty(),
            onDismiss = { isEditorOpen = false },
            onSave = { id, code, title, sec, type, day, start, dur, room, bldg, enrolled, locked ->
                viewModel.saveSession(id, code, title, sec, type, day, start, dur, room, bldg, enrolled, locked)
                isEditorOpen = false
            },
            onDelete = { session ->
                viewModel.deleteSession(session)
                isEditorOpen = false
            }
        )
    }

    if (isExportSheetOpen) {
        val activeFaculty = uiState.activeFaculty
        if (activeFaculty != null) {
            val suggestedName = remember(activeFaculty.id, uiState.selectedSemester?.id) {
                resolvedPdfExporter.suggestedFileName(activeFaculty, uiState.selectedSemester)
            }
            PdfExportBottomSheet(
                semesterName = uiState.selectedSemester?.name ?: "Current Term",
                sessionCount = uiState.sessions.size,
                suggestedFileName = suggestedName,
                isExporting = isExportingPdf,
                onDismiss = { isExportSheetOpen = false },
                onSaveToDevice = {
                    createPdfLauncher.launch(suggestedName)
                },
                onSharePdf = {
                    coroutineScope.launch {
                        isExportingPdf = true
                        try {
                            val pdfUri = resolvedPdfExporter.exportToShareableUri(
                                faculty = activeFaculty,
                                semester = uiState.selectedSemester,
                                sessions = uiState.sessions,
                                conflicts = uiState.conflicts
                            )
                            isExportSheetOpen = false
                            val shareIntent = resolvedPdfExporter.createShareIntent(
                                pdfUri = pdfUri,
                                faculty = activeFaculty,
                                semester = uiState.selectedSemester
                            )
                            context.startActivity(shareIntent)
                        } catch (_: Exception) {
                            snackbarHostState.showSnackbar("Unable to prepare PDF share intent")
                        } finally {
                            isExportingPdf = false
                        }
                    }
                },
                onPrintPdf = {
                    isExportSheetOpen = false
                    resolvedPdfExporter.startPrintJob(
                        activityContext = context,
                        faculty = activeFaculty,
                        semester = uiState.selectedSemester,
                        sessions = uiState.sessions,
                        conflicts = uiState.conflicts
                    )
                }
            )
        }
    }
}
