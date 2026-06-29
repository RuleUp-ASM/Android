package com.ruleup.ui.helper

import android.os.SystemClock
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SingleClickHelper(
    private val throttleMillis: Long = 500L,
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
        lastClickTime = now
        return true
    }
}

@Composable
fun rememberSingleClickHelper(throttleMillis: Long = 500L) = remember { SingleClickHelper(throttleMillis) }

/**
 * [Modifier.clickable] 의 단일 클릭(throttle) 버전.
 * 짧은 시간 내 연속 클릭을 무시하여 중복 실행을 막는다.
 */
@Composable
fun Modifier.singleClickable(
    enabled: Boolean = true,
    throttleMillis: Long = 500L,
    onClick: () -> Unit,
): Modifier {
    val helper = rememberSingleClickHelper(throttleMillis)
    return clickable(enabled = enabled) { helper.run(onClick) }
}

/**
 * Material `Button` 등 `onClick` 파라미터를 받는 컴포넌트용 throttle 래퍼.
 * 반환된 람다를 `onClick` 으로 넘기면 중복 클릭이 차단된다.
 */
@Composable
fun rememberSingleClick(
    throttleMillis: Long = 500L,
    onClick: () -> Unit,
): () -> Unit {
    val helper = rememberSingleClickHelper(throttleMillis)
    return { helper.run(onClick) }
}
