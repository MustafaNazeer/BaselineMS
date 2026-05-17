package com.mustafanazeer.baselinems.battery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mustafanazeer.baselinems.data.SessionDao
import com.mustafanazeer.baselinems.data.SessionEntity
import com.mustafanazeer.baselinems.data.TestResultDao
import com.mustafanazeer.baselinems.data.TestResultEntity
import com.mustafanazeer.baselinems.data.TestType
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

    private var _activeSessionId: String? = null
    val activeSessionId: String? get() = _activeSessionId

    fun start() {
        if (modules.isEmpty()) return
        viewModelScope.launch {
            val session = SessionEntity(deviceInfo = deviceInfo)
            sessionDao.insert(session)
            _activeSessionId = session.id
            _state.value = State.Running(index = 0)
        }
    }

    fun recordResult(testType: TestType, payload: TestResultPayload) {
        val current = _state.value
        if (current !is State.Running) return
        val sessionId = _activeSessionId ?: return

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val featuresJson = payload.featuresJsonOverride ?: Json.encodeToString(payload.features)
            testResultDao.insert(
                TestResultEntity(
                    sessionId = sessionId,
                    testType = testType,
                    startedAtEpochMs = now,
                    completedAtEpochMs = now,
                    qualityScore = payload.qualityScore,
                    featuresJson = featuresJson,
                    rawSensorRelativePath = payload.rawSensorRelativePath
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
        val sessionId = _activeSessionId
        viewModelScope.launch {
            if (sessionId != null) {
                val session = sessionDao.getById(sessionId)
                if (session != null) sessionDao.delete(session)
            }
            _activeSessionId = null
            _state.value = State.Idle
        }
    }

    fun reset() {
        _activeSessionId = null
        _state.value = State.Idle
    }
}
