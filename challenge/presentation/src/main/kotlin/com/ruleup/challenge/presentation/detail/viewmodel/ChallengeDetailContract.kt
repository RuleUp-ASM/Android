package com.ruleup.challenge.presentation.detail.viewmodel

import com.ruleup.challenge.domain.entity.ChallengeDetail
import com.ruleup.challenge.domain.entity.ChallengeMembers
import com.ruleup.challenge.domain.entity.ChallengeRoom
import com.ruleup.challenge.domain.entity.ChallengeSetupInfo
import com.ruleup.challenge.domain.entity.ChallengeWatchers
import com.ruleup.challenge.domain.entity.DelegationTicket
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

    /** 필요한 등록이 모두 끝난(또는 불필요한) 뒤 시작. */
    data object Proceed : ChallengeDetailIntent

    /** (참여자 본인) 내 감시자 초대 생성 → 카카오톡 공유 카드 발송. */
    data object InviteWatcher : ChallengeDetailIntent

    /** (참여자 본인) 내 감시자 해제 — REVOKED + 연락처 파기. */
    data class RemoveWatcher(
        val watcherId: String,
    ) : ChallengeDetailIntent

    /** (방 홈) 공지 목록으로 이동. */
    data object OpenNotices : ChallengeDetailIntent

    /** (방 홈) 고정 공지 배너 탭 → 공지 상세로 이동. */
    data class OpenNotice(
        val noticeId: String,
    ) : ChallengeDetailIntent

    /** (방 홈) 그룹 랭킹으로 이동. */
    data object OpenRanking : ChallengeDetailIntent

    /** (방 홈, 방장·관리자) 확인 대기함으로 이동. */
    data object OpenPendingReviews : ChallengeDetailIntent

    /** (방 홈, 비방장) 챌린지 탈퇴. 성공 시 이전 화면으로. */
    data object LeaveChallenge : ChallengeDetailIntent

    /** (방 홈, 방장·참여자 0명) 챌린지 삭제. 성공 시 이전 화면으로. */
    data object DeleteChallenge : ChallengeDetailIntent

    /** (방장) 멤버를 공동 관리자로 임명. */
    data class PromoteMember(
        val userId: String,
    ) : ChallengeDetailIntent

    /** (방장) 공동 관리자를 일반 멤버로 해제. */
    data class DemoteMember(
        val userId: String,
    ) : ChallengeDetailIntent

    /** (방장) 공동 관리자에게 방장 위임 요청. */
    data class RequestDelegation(
        val targetUserId: String,
    ) : ChallengeDetailIntent

    /** (방장) 대기 중인 방장 위임 요청 취소. */
    data object CancelDelegation : ChallengeDetailIntent

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
    // 방 홈 일괄 조회 결과. 그룹 챌린지의 ACTIVE 멤버만 조회에 성공하며(비멤버 403 흡수 → null),
    // 값이 있으면 상세를 방 홈(요약·공지·랭킹·오늘 상태)으로 확장 렌더링한다.
    val room: ChallengeRoom? = null,
    // 방 홈 멤버 목록(GET members). 방 홈일 때만 조회하며, 멤버 섹션·삭제 가능 여부 판정에 쓴다.
    val members: ChallengeMembers? = null,
    // 탈퇴/삭제/권한 변경/위임 요청 중(버튼 중복 탭 방지).
    val isMemberActionLoading: Boolean = false,
    // 방금 생성한 방장 위임 요청(PENDING). 취소(CANCEL)의 delegationId 출처 — 배너로 노출한다.
    val pendingDelegation: DelegationTicket? = null,
    // 위임 요청 대상 닉네임(배너 문구용).
    val pendingDelegationNickname: String? = null,
    // 현재 사용자 ID. 멤버 목록에서 "내 행"을 식별해 관리자 본인 해제(self-DEMOTE)를 노출하는 데 쓴다.
    val myUserId: String? = null,
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

    /** 방 홈 조회 성공 (그룹 챌린지 ACTIVE 멤버) — 재진입 시 미읽음 수 갱신 포함. */
    data class RoomLoaded(
        val room: ChallengeRoom,
    ) : ChallengeDetailReducerEvent

    /** 멤버 목록 갱신(방 홈 조회 시). */
    data class MembersLoaded(
        val members: ChallengeMembers,
    ) : ChallengeDetailReducerEvent

    /** 탈퇴/삭제/권한 변경/위임 요청 시작/종료. */
    data class MemberActionLoading(
        val loading: Boolean,
    ) : ChallengeDetailReducerEvent

    /** 방장 위임 요청 생성됨(배너 노출). */
    data class DelegationRequested(
        val ticket: DelegationTicket,
        val targetNickname: String?,
    ) : ChallengeDetailReducerEvent

    /** 방장 위임 요청 배너 해제(취소·응답 후). */
    data object DelegationCleared : ChallengeDetailReducerEvent

    /** 현재 사용자 ID 로드됨(진입 시 1회). */
    data class MyUserIdLoaded(
        val userId: String?,
    ) : ChallengeDetailReducerEvent
}
