package com.ruleup.verification.presentation.detail.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.ui.mvi.MviViewModel
import com.ruleup.verification.domain.VerificationLocationPage
import com.ruleup.verification.domain.usecase.GetVerificationDetailUseCase
import com.ruleup.verification.presentation.render.CtaTarget
import com.ruleup.verification.presentation.render.failureReasonCta
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 챌린지 인증 결과/실패 상세(명세 3.3·§6.2). 진입 시 1회 조회하고, 실패 사유 CTA 라우팅을 담당한다
 * (장소 미설정 → 지도 핀, 권한 누락 → 권한 설정). PENDING≠실패 등 렌더 규칙은 화면이 적용한다(§6.4).
 */
@HiltViewModel
class VerificationDetailViewModel
    @Inject
    constructor(
        private val getVerificationDetailUseCase: GetVerificationDetailUseCase,
        private val navigationHelper: NavigationHelper,
    ) : MviViewModel<VerificationDetailIntent, VerificationDetailState, VerificationDetailReducerEvent, VerificationDetailEffect>(
            VerificationDetailState.initial,
        ) {
        private var challengeId: String? = null

        override fun onIntent(intent: VerificationDetailIntent) {
            when (intent) {
                is VerificationDetailIntent.Load -> {
                    challengeId = intent.challengeId
                    load(intent.challengeId)
                }

                VerificationDetailIntent.CtaClicked -> handleCta()
            }
        }

        override fun reduce(
            state: VerificationDetailState,
            event: VerificationDetailReducerEvent,
        ): VerificationDetailState =
            when (event) {
                VerificationDetailReducerEvent.Loading -> state.copy(isLoading = true, error = null)
                is VerificationDetailReducerEvent.Loaded -> state.copy(isLoading = false, detail = event.detail, error = null)
                is VerificationDetailReducerEvent.Failed -> state.copy(isLoading = false, error = event.message)
            }

        private fun load(id: String) {
            if (currentState.isLoading) return
            viewModelScope.launch {
                dispatch(VerificationDetailReducerEvent.Loading)
                runCatching { getVerificationDetailUseCase(id) }
                    .onSuccess { dispatch(VerificationDetailReducerEvent.Loaded(it)) }
                    .onFailure { dispatch(VerificationDetailReducerEvent.Failed(it.message ?: "검증 결과를 불러오지 못했어요")) }
            }
        }

        private fun handleCta() {
            val reason = currentState.detail?.today?.failureReason ?: return
            when (failureReasonCta(reason)) {
                CtaTarget.OPEN_LOCATION_PIN ->
                    // POC: challengeId 를 멤버 식별자로 사용(실제 플로우는 challengeMemberId 전달).
                    challengeId?.let {
                        navigationHelper.navigateTo(
                            VerificationLocationPage(challengeMemberId = it, defaultRadiusM = 100f, dwellMinutes = 60),
                        )
                    }

                CtaTarget.OPEN_PERMISSION_SETTINGS -> emitEffect(VerificationDetailEffect.OpenPermissionSettings)
                CtaTarget.NONE -> Unit
            }
        }
    }
