package com.ruleup.challenge.presentation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * `viewModelScope` 의 `Dispatchers.Main` 을 테스트 디스패처로 갈아끼운다. 유닛 테스트에는 메인 루퍼가
 * 없어 이걸 걸지 않으면 ViewModel 이 코루틴을 띄우는 순간 `IllegalStateException` 으로 죽는다.
 *
 * 기본값이 [UnconfinedTestDispatcher] 라 인텐트를 넣으면 그 자리에서 끝까지 돈다 — 테스트가
 * `advanceUntilIdle()` 을 흩뿌리지 않아도 되고, "진행 중" 상태는 대역이 명시적으로 붙잡을 때만 생긴다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
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
