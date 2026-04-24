package com.timome.screenlock.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings_prefs")

enum class ServiceMode {
    TIMER, COUNTDOWN, NOTIFICATION, FLOATING_BALL
}

enum class LongPressPosition {
    TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
}

class SettingsDataStore(private val context: Context) {

    companion object {
        val SERVICE_MODE = stringPreferencesKey("service_mode")
        val TIMER_DURATION_MS = floatPreferencesKey("timer_duration_ms")
        val LONG_PRESS_DURATION_MS = floatPreferencesKey("long_press_duration_ms")
        val POSITION_TOP_LEFT = booleanPreferencesKey("position_top_left")
        val POSITION_TOP_RIGHT = booleanPreferencesKey("position_top_right")
        val POSITION_BOTTOM_LEFT = booleanPreferencesKey("position_bottom_left")
        val POSITION_BOTTOM_RIGHT = booleanPreferencesKey("position_bottom_right")
        val SPRING_DAMPING_RATIO = floatPreferencesKey("spring_damping_ratio")

        const val DEFAULT_TIMER_DURATION_MS = 300000f // 5分钟
        const val DEFAULT_LONG_PRESS_DURATION_MS = 3000f // 3秒
        const val DEFAULT_SPRING_DAMPING_RATIO = 0.5f // MediumBouncy
    }

    val serviceMode: Flow<ServiceMode> = context.settingsDataStore.data
        .map { preferences ->
            val modeString = preferences[SERVICE_MODE] ?: ServiceMode.TIMER.name
            ServiceMode.valueOf(modeString)
        }

    val timerDurationMs: Flow<Float> = context.settingsDataStore.data
        .map { preferences ->
            preferences[TIMER_DURATION_MS] ?: DEFAULT_TIMER_DURATION_MS
        }

    val longPressDurationMs: Flow<Float> = context.settingsDataStore.data
        .map { preferences ->
            preferences[LONG_PRESS_DURATION_MS] ?: DEFAULT_LONG_PRESS_DURATION_MS
        }

    val enabledPositions: Flow<Set<LongPressPosition>> = context.settingsDataStore.data
        .map { preferences ->
            val positions = mutableSetOf<LongPressPosition>()
            // 默认开启左上和右上
            if (preferences[POSITION_TOP_LEFT] != false) positions.add(LongPressPosition.TOP_LEFT)
            if (preferences[POSITION_TOP_RIGHT] != false) positions.add(LongPressPosition.TOP_RIGHT)
            if (preferences[POSITION_BOTTOM_LEFT] == true) positions.add(LongPressPosition.BOTTOM_LEFT)
            if (preferences[POSITION_BOTTOM_RIGHT] == true) positions.add(LongPressPosition.BOTTOM_RIGHT)
            positions
        }

    val positionTopLeft: Flow<Boolean> = context.settingsDataStore.data
        .map { it[POSITION_TOP_LEFT] != false }

    val positionTopRight: Flow<Boolean> = context.settingsDataStore.data
        .map { it[POSITION_TOP_RIGHT] != false }

    val positionBottomLeft: Flow<Boolean> = context.settingsDataStore.data
        .map { it[POSITION_BOTTOM_LEFT] == true }

    val positionBottomRight: Flow<Boolean> = context.settingsDataStore.data
        .map { it[POSITION_BOTTOM_RIGHT] == true }

    val springDampingRatio: Flow<Float> = context.settingsDataStore.data
        .map { preferences ->
            preferences[SPRING_DAMPING_RATIO] ?: DEFAULT_SPRING_DAMPING_RATIO
        }

    suspend fun setServiceMode(mode: ServiceMode) {
        context.settingsDataStore.edit { preferences ->
            preferences[SERVICE_MODE] = mode.name
        }
    }

    suspend fun setTimerDurationMs(duration: Float) {
        context.settingsDataStore.edit { preferences ->
            preferences[TIMER_DURATION_MS] = duration
        }
    }

    suspend fun setLongPressDurationMs(duration: Float) {
        context.settingsDataStore.edit { preferences ->
            preferences[LONG_PRESS_DURATION_MS] = duration
        }
    }

    suspend fun setPosition(position: LongPressPosition, enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            when (position) {
                LongPressPosition.TOP_LEFT -> preferences[POSITION_TOP_LEFT] = enabled
                LongPressPosition.TOP_RIGHT -> preferences[POSITION_TOP_RIGHT] = enabled
                LongPressPosition.BOTTOM_LEFT -> preferences[POSITION_BOTTOM_LEFT] = enabled
                LongPressPosition.BOTTOM_RIGHT -> preferences[POSITION_BOTTOM_RIGHT] = enabled
            }
        }
    }

    suspend fun setSpringDampingRatio(ratio: Float) {
        context.settingsDataStore.edit { preferences ->
            preferences[SPRING_DAMPING_RATIO] = ratio.coerceIn(0.1f, 1.0f)
        }
    }
}
