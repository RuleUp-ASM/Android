package com.ruleup.ui.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 사용자 입력(클릭, 스크롤 등)을 표현하는 마커 인터페이스.
 * `View → ViewModel` 방향으로만 흐른다.
 */
interface MviIntent

/**
 * 화면에 노출되는 불변 상태의 마커 인터페이스.
 * `ViewModel → View` 방향으로만 흐른다.
 */
interface UiState

/**
 * Reducer 에 입력되는 내부 이벤트 마커 인터페이스.
 * (예: 데이터 로딩 완료 등) — 외부에서 직접 dispatch 하지 않는다.
 */
interface ReducerEvent

/**
 * 단발성 사이드 이펙트의 마커 인터페이스.
 * (예: 토스트/스낵바, 화면 이동, 외부 SDK 실행 등) — 상태로 누적되지 않고 1회 소비된다.
 * `ViewModel → View` 방향으로 흐른다.
 */
interface MviEffect

/** 이펙트가 없는 화면을 위한 기본 타입. */
object NoEffect : MviEffect

/**
 * MVI 베이스 ViewModel.
 *
 * - [uiState] 단일 [StateFlow] 만 View 에 노출한다.
 * - [effect] 로 단발성 사이드 이펙트를 흘려보낸다. (이펙트가 없는 화면은 [NoEffect] 사용)
 * - 외부 진입점은 [onIntent] 하나로 통일한다.
 * - 모든 상태 변이는 [reduce] 한 곳을 거쳐 [dispatch] 로만 일어난다.
 */
abstract class MviViewModel<I : MviIntent, S : UiState, E : ReducerEvent, F : MviEffect>(
    initialState: S,
) : ViewModel() {
    private val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<S> = _uiState.asStateFlow()

    private val _effect = Channel<F>(Channel.BUFFERED)
    val effect: Flow<F> = _effect.receiveAsFlow()

    protected val currentState: S
        get() = _uiState.value

    abstract fun onIntent(intent: I)

    protected abstract fun reduce(
        state: S,
        event: E,
    ): S

    protected fun dispatch(event: E) {
        _uiState.update { currentUiState -> reduce(currentUiState, event) }
    }

    protected fun emitEffect(effect: F) {
        viewModelScope.launch { _effect.send(effect) }
    }
}
