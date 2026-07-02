package com.ruleup.challenge.presentation.detail.viewmodel

import com.ruleup.challenge.domain.entity.ChallengeDetail
import com.ruleup.ui.mvi.MviIntent
import com.ruleup.ui.mvi.ReducerEvent
import com.ruleup.ui.mvi.UiState

sealed interface ChallengeDetailIntent : MviIntent {
    /** 화면 진입 시 상세 조회. */
    data class Load(
        val challengeId: String,
    ) : ChallengeDetailIntent

    /** 재진입(ON_RESUME) 시 대상 앱 등록 상태 재확인 — 앱 등록 화면에서 돌아오면 버튼 모드가 갱신되도록. */
    data object RefreshSetup : ChallengeDetailIntent

    /** "앱 등록하기" → 대상 앱 등록 화면으로 이동. */
    data object RegisterApps : ChallengeDetailIntent

    /** 권한·앱 등록이 모두 끝난 뒤 참여 진행(좌표 바인딩 화면으로 이동). */
    data object Proceed : ChallengeDetailIntent

    data object Back : ChallengeDetailIntent
}

/**
 * 상세 하단 CTA 버튼이 유도할 다음 셋업 단계. 자동 인증 챌린지는 권한 → 앱 등록 → 참여 순으로 진행한다.
 * 권한 허용 여부는 OS 런타임 권한(Context)으로 화면에서 판단하고, 앱 등록 여부는 로컬 저장으로 판단한다.
 */
enum class DetailSetupAction {
    GRANT_PERMISSION,
    REGISTER_APPS,
    JOIN,
}

data class ChallengeDetailState(
    val challengeId: String,
    val isLoading: Boolean,
    val detail: ChallengeDetail?,
    val errorMessage: String?,
    // 대상 앱이 로컬에 등록됐는지(앱 등록 화면 저장 여부). 버튼 모드 결정에 사용.
    val targetAppsRegistered: Boolean = false,
) : UiState {
    companion object {
        val initial =
            ChallengeDetailState(
                challengeId = "",
                isLoading = true,
                detail = null,
                errorMessage = null,
            )
    }
}

sealed interface ChallengeDetailReducerEvent : ReducerEvent {
    data class Loading(
        val challengeId: String,
    ) : ChallengeDetailReducerEvent

    data class Loaded(
        val detail: ChallengeDetail,
        val targetAppsRegistered: Boolean,
    ) : ChallengeDetailReducerEvent

    data class Failed(
        val message: String,
    ) : ChallengeDetailReducerEvent

    /** 대상 앱 등록 상태 재확인 결과. */
    data class SetupRefreshed(
        val targetAppsRegistered: Boolean,
    ) : ChallengeDetailReducerEvent
}
