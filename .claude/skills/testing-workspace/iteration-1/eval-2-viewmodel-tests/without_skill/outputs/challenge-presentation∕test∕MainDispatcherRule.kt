package com.ruleup.challenge.presentation.testing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * `viewModelScope` 가 쓰는 `Dispatchers.Main` 을 테스트 디스패처로 갈아 끼운다.
 * 유닛 테스트에는 메인 루퍼가 없어, 이 교체 없이는 ViewModel 이 코루틴을 띄우는 순간 터진다.
 *
 * [UnconfinedTestDispatcher] 를 쓰는 이유는 실기기 동작에 더 가깝기 때문이다 — `viewModelScope` 는
 * `Main.immediate` 라 `launch` 블록이 첫 중단점까지 `onIntent` 안에서 그대로 실행된다. 중복 호출
 * 가드처럼 "인텐트가 반환되기 전에 상태가 이미 바뀌어 있다"에 기대는 코드는 이 디스패처에서만
 * 제대로 재현된다. 덤으로 테스트가 `advanceUntilIdle()` 없이 인텐트 → 단언으로 읽힌다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
