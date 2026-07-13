package com.ruleup.challenge.presentation.detail.viewmodel

import com.ruleup.challenge.domain.entity.ChallengeDetail
import com.ruleup.challenge.domain.entity.ChallengeSetupInfo
import com.ruleup.challenge.domain.entity.ChallengeWatchers
import com.ruleup.challenge.domain.entity.WatcherInviteCard
import com.ruleup.ui.mvi.MviEffect
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

    /** (참여자 본인) 내 감시자 초대 생성 → 카카오톡 공유 카드 발송. */
    data object InviteWatcher : ChallengeDetailIntent

    /** (참여자 본인) 내 감시자 해제 — REVOKED + 연락처 파기. */
    data class RemoveWatcher(
        val watcherId: String,
    ) : ChallengeDetailIntent

    data object Back : ChallengeDetailIntent
}

sealed interface ChallengeDetailEffect : MviEffect {
    /**
     * 초대 생성 성공 → 사용자 본인 명의 카카오톡 공유 실행(룰업 직접 발송 금지).
     * 카드 문구는 서버 kakaoShare 페이로드를 그대로 쓴다(없으면 ViewModel 이 기본 문구 구성).
     */
    data class ShareWatcherInvite(
        val card: WatcherInviteCard,
        val inviteUrl: String,
    ) : ChallengeDetailEffect

    data class ShowMessage(
        val message: String,
    ) : ChallengeDetailEffect
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
    // 이 챌린지에서의 "내 감시자"(감시자는 챌린지 × 참여자 단위). 조회 성공 시에만 값이 있고
    // null 이면(미참여 403 등) 감시자 섹션을 숨긴다 — 권한 판단은 서버에 위임.
    val watchers: ChallengeWatchers? = null,
    // 초대 생성 요청 중(버튼 중복 탭 방지).
    val isInvitingWatcher: Boolean = false,
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

    /** 내 감시자 목록 갱신(초대·해제 후 재조회 포함). 조회 성공 = 참여자 = 섹션 노출. */
    data class WatchersLoaded(
        val watchers: ChallengeWatchers,
    ) : ChallengeDetailReducerEvent

    /** 초대 생성 요청 시작/종료. */
    data class InvitingWatcher(
        val inviting: Boolean,
    ) : ChallengeDetailReducerEvent
}
