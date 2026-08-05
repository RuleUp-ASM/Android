package com.ruleup.android_ruleup.navigation

import androidx.lifecycle.ViewModel
import com.ruleup.onboarding.domain.auth.AppEntry
import com.ruleup.onboarding.domain.auth.SessionBootstrap
import com.ruleup.onboarding.domain.auth.SessionBootstrapState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * 앱이 처음 열릴 곳을 흘려보낸다.
 *
 * 목적지 계산은 [SessionBootstrap] 이 한다. 여기는 그 결과를 네비게이션 호스트가 쓸 수 있게
 * 꺼내 주기만 한다 — 호스트가 컴포저블이라 도메인 싱글턴을 직접 주입받을 자리가 없다.
 *
 * 강제 업데이트는 여기 오지 않는다. 그 상태에서는 [SessionBootstrapState.Resolved] 를 방출하지
 * 않으므로 게이트가 걸린 채로 어디로도 이동하지 않는다.
 */
@HiltViewModel
class AppEntryViewModel
    @Inject
    constructor(
        sessionBootstrap: SessionBootstrap,
    ) : ViewModel() {
        val entry: Flow<AppEntry> =
            sessionBootstrap.state
                .filterIsInstance<SessionBootstrapState.Resolved>()
                .map { it.entry }
    }
