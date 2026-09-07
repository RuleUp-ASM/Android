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

/** 사용자 입력 마커. `View → ViewModel` 방향으로만 흐른다. */
interface MviIntent

/** 화면에 노출되는 불변 상태 마커. `ViewModel → View` 방향으로만 흐른다. */
interface UiState

/** [MviViewModel.reduce] 에 들어가는 내부 이벤트 마커. 화면이 직접 만들어 넣지 않는다. */
interface ReducerEvent

/** 단발성 사이드 이펙트 마커(토스트·화면 이동 등). 상태로 누적되지 않고 1회만 소비된다. */
interface MviEffect

/** 이펙트가 없는 화면을 위한 기본 타입. */
object NoEffect : MviEffect

/**
 * MVI 베이스 ViewModel. 외부 진입점은 [onIntent] 하나뿐이다.
 * 상태 변이는 [dispatch] → [reduce] 만 거친다 — 별도 `MutableStateFlow` 를 두면 전이가 흩어진다.
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
