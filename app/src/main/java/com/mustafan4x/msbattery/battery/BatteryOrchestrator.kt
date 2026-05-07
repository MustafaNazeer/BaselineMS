package com.mustafan4x.msbattery.battery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mustafan4x.msbattery.data.SessionDao
import com.mustafan4x.msbattery.data.SessionEntity
import com.mustafan4x.msbattery.data.TestResultDao
import com.mustafan4x.msbattery.data.TestResultEntity
import com.mustafan4x.msbattery.data.TestType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class BatteryOrchestrator(
    val modules: List<TestModule>,
    private val sessionDao: SessionDao,
    private val testResultDao: TestResultDao,
    private val deviceInfo: String
) : ViewModel() {

    sealed class State {
        data object Idle : State()
        data class Running(val index: Int) : State()
        data object Completed : State()
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    private var activeSessionId: String? = null

    fun start() {
        if (modules.isEmpty()) return
        viewModelScope.launch {
            val session = SessionEntity(deviceInfo = deviceInfo)
            sessionDao.insert(session)
            activeSessionId = session.id
            _state.value = State.Running(index = 0)
        }
    }

    fun recordResult(testType: TestType, qualityScore: Double, features: Map<String, Double>) {
        val current = _state.value
        if (current !is State.Running) return
        val sessionId = activeSessionId ?: return

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val featuresJson = Json.encodeToString(features)
            testResultDao.insert(
                TestResultEntity(
                    sessionId = sessionId,
                    testType = testType,
                    startedAtEpochMs = now,
                    completedAtEpochMs = now,
                    qualityScore = qualityScore,
                    featuresJson = featuresJson
                )
            )

            val nextIndex = current.index + 1
            if (nextIndex >= modules.size) {
                val session = sessionDao.getById(sessionId)
                if (session != null) {
                    sessionDao.update(session.copy(completedAtEpochMs = now))
                }
                _state.value = State.Completed
            } else {
                _state.value = State.Running(index = nextIndex)
            }
        }
    }

    fun cancel() {
        val sessionId = activeSessionId
        viewModelScope.launch {
            if (sessionId != null) {
                val session = sessionDao.getById(sessionId)
                if (session != null) sessionDao.delete(session)
            }
            activeSessionId = null
            _state.value = State.Idle
        }
    }

    fun reset() {
        activeSessionId = null
        _state.value = State.Idle
    }
}
