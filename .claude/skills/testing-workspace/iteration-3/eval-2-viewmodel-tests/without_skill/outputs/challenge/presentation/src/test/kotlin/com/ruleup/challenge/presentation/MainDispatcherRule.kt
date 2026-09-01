package com.ruleup.challenge.presentation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * `viewModelScope` 가 쓰는 `Dispatchers.Main` 을 테스트 디스패처로 교체한다.
 *
 * 기본값이 [UnconfinedTestDispatcher] 인 건 ViewModel 이 인텐트를 받는 즉시 코루틴을 띄우기 때문이다 —
 * 즉시 실행이라 인텐트 한 줄 뒤에 상태를 바로 읽을 수 있고, 응답을 붙잡아 두고 진행 중 상태를 볼
 * 때만 fake 쪽 게이트로 멈춘다.
 */
class MainDispatcherRule(
    private val dispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
