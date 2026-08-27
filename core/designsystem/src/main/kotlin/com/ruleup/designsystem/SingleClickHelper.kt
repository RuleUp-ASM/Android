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
 * 위젯별 throttle 을 넘어서는 전역 가드 — 서로 다른 내비 버튼을 연타해도 이중 네비게이션이 나지 않는다.
 * main-thread 전용.
 */
object SingleClickGuard {
    const val DEFAULT_THROTTLE_MILLIS = 300L

    private var lastGlobalClickTime = 0L

    /** 판정과 동시에 전역 시각을 갱신한다 — 결과를 버려도 다음 클릭이 막힌다. */
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
        if (now - lastClickTime < throttleMillis) return false
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
 * 중복 클릭을 막는 [Modifier.clickable].
 * 빠른 연속 탭이 정상인 UI(키패드·스텝퍼·그리드 선택)에선 [globalGuard] 를 꺼야 두 번째 탭이 먹는다.
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
 * `onClick` 파라미터를 받는 컴포넌트(Material `Button` 등)용 [singleClickable].
 * [globalGuard] 의 뜻은 [singleClickable] 과 같다.
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
