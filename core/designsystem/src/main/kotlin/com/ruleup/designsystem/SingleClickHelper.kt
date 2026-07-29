package com.ruleup.designsystem

import android.os.SystemClock
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 앱 전역 클릭 가드. 컴포넌트별 throttle 과 별개로, "서로 다른 위젯"이라도 아주 짧은 시간 안에
 * 연속 실행되는 것을 막는다(예: 서로 다른 두 내비 버튼을 순간 연타 → 이중 네비게이션 방지).
 * main-thread 전용.
 */
object SingleClickGuard {
    /** 전역 가드 기본 창(ms). */
    const val DEFAULT_THROTTLE_MILLIS = 300L

    // 모든 SingleClickHelper 가 공유하는 마지막 '실행 승인' 시각(monotonic, main-thread 전용).
    private var lastGlobalClickTime = 0L

    /** [now] 클릭을 전역적으로 통과시킬지 판정하고, 통과 시 전역 시각을 갱신한다. */
    fun tryPass(
        now: Long,
        throttleMillis: Long,
    ): Boolean {
        if (now - lastGlobalClickTime < throttleMillis) return false
        lastGlobalClickTime = now
        return true
    }
}

class SingleClickHelper(
    private val throttleMillis: Long = 500L,
    // 0 이면 전역 가드 미적용(고빈도 탭 UI: 키패드·스텝퍼·그리드 선택 등).
    private val globalThrottleMillis: Long = SingleClickGuard.DEFAULT_THROTTLE_MILLIS,
) {
    private val _isRunning = MutableStateFlow(false)
    val isRunning = _isRunning.asStateFlow()
    private var lastClickTime = 0L

    fun run(block: () -> Unit) {
        if (!pass()) return
        block()
    }

    fun launch(
        scope: CoroutineScope,
        block: suspend () -> Unit,
    ) {
        if (_isRunning.value) return
        if (!pass()) return

        scope.launch {
            _isRunning.value = true
            try {
                block()
            } finally {
                _isRunning.value = false
            }
        }
    }

    private fun pass(): Boolean {
        val now = SystemClock.elapsedRealtime()
        // 1) 로컬(이 위젯) throttle.
        if (now - lastClickTime < throttleMillis) return false
        // 2) 전역(교차 위젯) 가드 — 통과 시 전역 시각이 갱신된다. globalThrottleMillis<=0 이면 건너뛴다.
        if (globalThrottleMillis > 0L && !SingleClickGuard.tryPass(now, globalThrottleMillis)) return false
        lastClickTime = now
        return true
    }
}

@Composable
fun rememberSingleClickHelper(
    throttleMillis: Long = 500L,
    globalThrottleMillis: Long = SingleClickGuard.DEFAULT_THROTTLE_MILLIS,
) = remember(throttleMillis, globalThrottleMillis) { SingleClickHelper(throttleMillis, globalThrottleMillis) }

/**
 * [Modifier.clickable] 의 단일 클릭(throttle) 버전.
 * 짧은 시간 내 연속 클릭을 무시하여 중복 실행을 막는다.
 *
 * [globalGuard] 가 true 면 [SingleClickGuard] 로 "서로 다른 위젯 간" 이중 실행까지 막는다(기본).
 * 여러 항목을 빠르게 연속 탭해야 하는 UI(키패드·스텝퍼·그리드 선택 등)에선 false 로 끈다.
 */
@Composable
fun Modifier.singleClickable(
    enabled: Boolean = true,
    throttleMillis: Long = 500L,
    globalGuard: Boolean = true,
    onClick: () -> Unit,
): Modifier {
    val helper =
        rememberSingleClickHelper(
            throttleMillis = throttleMillis,
            globalThrottleMillis = if (globalGuard) SingleClickGuard.DEFAULT_THROTTLE_MILLIS else 0L,
        )
    return clickable(enabled = enabled) { helper.run(onClick) }
}

/**
 * Material `Button` 등 `onClick` 파라미터를 받는 컴포넌트용 throttle 래퍼.
 * 반환된 람다를 `onClick` 으로 넘기면 중복 클릭이 차단된다.
 *
 * [globalGuard] 는 [singleClickable] 과 동일 — 서로 다른 위젯 간 이중 실행까지 막는다(기본).
 */
@Composable
fun rememberSingleClick(
    throttleMillis: Long = 500L,
    globalGuard: Boolean = true,
    onClick: () -> Unit,
): () -> Unit {
    val helper =
        rememberSingleClickHelper(
            throttleMillis = throttleMillis,
            globalThrottleMillis = if (globalGuard) SingleClickGuard.DEFAULT_THROTTLE_MILLIS else 0L,
        )
    return { helper.run(onClick) }
}
