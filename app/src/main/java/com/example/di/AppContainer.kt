package com.example.di

import android.content.Context
import com.example.data.ai.SampleRecommendationAiRepository
import com.example.data.local.JsonCredentialAndTimetableStore
import com.example.data.local.ThemeAndSessionPreferences
import com.example.data.repository.JsonAuthRepository
import com.example.data.repository.JsonTimetableRepository
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.PreferencesRepository
import com.example.domain.repository.RecommendationAiRepository
import com.example.domain.repository.TimetableRepository
import com.example.domain.usecase.AnalyzeTimetableUseCase
import com.example.domain.usecase.AutoResolveConflictsUseCase
import com.example.domain.usecase.CheckSessionConflictUseCase
import com.example.domain.usecase.PrepareNewSemesterUseCase
import com.example.platform.ConflictAlertNotifier
import com.example.platform.TimetablePdfExporter

class AppContainer(context: Context) {
    private val store by lazy { JsonCredentialAndTimetableStore(context) }

    val conflictAlertNotifier by lazy { ConflictAlertNotifier(context) }
    val pdfExporter by lazy { TimetablePdfExporter(context) }

    val preferencesRepository: PreferencesRepository by lazy {
        ThemeAndSessionPreferences(context)
    }

    val authRepository: AuthRepository by lazy {
        JsonAuthRepository(store, preferencesRepository)
    }

    val timetableRepository: TimetableRepository by lazy {
        JsonTimetableRepository(store, preferencesRepository)
    }

    val aiRepository: RecommendationAiRepository by lazy {
        SampleRecommendationAiRepository()
    }

    val checkSessionConflictUseCase by lazy { CheckSessionConflictUseCase() }
    val analyzeTimetableUseCase by lazy { AnalyzeTimetableUseCase() }
    val autoResolveConflictsUseCase by lazy { AutoResolveConflictsUseCase(timetableRepository) }
    val prepareNewSemesterUseCase by lazy { PrepareNewSemesterUseCase(timetableRepository) }
}
