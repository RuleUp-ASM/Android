package com.ruleup.challenge.presentation.detail.viewmodel

import com.ruleup.challenge.domain.entity.ChallengeDetail
import com.ruleup.challenge.domain.entity.ChallengeSetupInfo
import com.ruleup.ui.mvi.MviIntent
import com.ruleup.ui.mvi.ReducerEvent
import com.ruleup.ui.mvi.UiState

sealed interface ChallengeDetailIntent : MviIntent {
    /** 화면 진입 시 상세 + 셋업 요구사항 조회. */
    data class Load(
        val challengeId: String,
    ) : ChallengeDetailIntent

    /** 재진입(ON_RESUME) 시 셋업 상태 재확인 — 등록 화면에서 돌아오면 버튼 모드가 갱신되도록. */
    data object RefreshSetup : ChallengeDetailIntent

    /** "앱 등록하기" → 대상 앱 등록 화면으로 이동. */
    data object RegisterApps : ChallengeDetailIntent

    /** "인증 장소 등록하기" → 지도(앵커) 등록 화면으로 이동. */
    data object RegisterAnchor : ChallengeDetailIntent

    /** 권한 허용 모달에서 권한을 모두 확보함(셋업 퍼널: 권한 단계 완료). */
    data object PermissionGranted : ChallengeDetailIntent

    /** 필요한 등록이 모두 끝난(또는 불필요한) 뒤 시작. */
    data object Proceed : ChallengeDetailIntent

    data object Back : ChallengeDetailIntent
}

/**
 * 상세 하단 CTA 버튼이 유도할 다음 셋업 단계. GET setup 의 requiresTargetPackages/requiresAnchors 로
 * 필요한 등록만 노출한다: 권한 → (필요 시) 앱 등록 → (필요 시) 지도 앵커 → 시작.
 * 권한 허용 여부는 OS 런타임 권한(Context)으로 화면에서, 앱 등록 여부는 로컬 저장으로 판단한다.
 */
enum class DetailSetupAction {
    GRANT_PERMISSION,
    REGISTER_APPS,
    REGISTER_ANCHOR,
    JOIN,
}

data class ChallengeDetailState(
    val challengeId: String,
    val isLoading: Boolean,
    val detail: ChallengeDetail?,
    val errorMessage: String?,
    // 셋업 요구사항(GET setup). requiresAnchors/requiresTargetPackages 로 필요한 등록만 유도.
    val setup: ChallengeSetupInfo? = null,
    // 대상 앱이 로컬에 등록됐는지(앱 등록 화면 저장 여부).
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
        val setup: ChallengeSetupInfo?,
        val targetAppsRegistered: Boolean,
    ) : ChallengeDetailReducerEvent

    data class Failed(
        val message: String,
    ) : ChallengeDetailReducerEvent

    /** 셋업 상태 재확인 결과(앵커 등록/앱 등록 후 갱신). */
    data class SetupRefreshed(
        val setup: ChallengeSetupInfo?,
        val targetAppsRegistered: Boolean,
    ) : ChallengeDetailReducerEvent
}
