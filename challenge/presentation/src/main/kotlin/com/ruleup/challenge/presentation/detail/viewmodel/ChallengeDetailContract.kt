package com.ruleup.challenge.presentation.detail.viewmodel

import com.ruleup.challenge.domain.entity.ChallengeCalendar
import com.ruleup.challenge.domain.entity.ChallengeDetail
import com.ruleup.challenge.domain.entity.ChallengeMembers
import com.ruleup.challenge.domain.entity.ChallengeRanking
import com.ruleup.challenge.domain.entity.ChallengeRoom
import com.ruleup.challenge.domain.entity.ChallengeSetupInfo
import com.ruleup.challenge.domain.entity.ChallengeThreads
import com.ruleup.challenge.domain.entity.ChallengeWatchers
import com.ruleup.challenge.domain.entity.CrossChallengeRanking
import com.ruleup.challenge.domain.entity.JoinBlockReason
import com.ruleup.challenge.domain.entity.ThreadItem
import com.ruleup.challenge.domain.entity.WatcherInviteCard
import com.ruleup.report.domain.entity.ReportReason
import com.ruleup.report.domain.entity.ReportResult
import com.ruleup.ui.mvi.MviEffect
import com.ruleup.ui.mvi.MviIntent
import com.ruleup.ui.mvi.ReducerEvent
import com.ruleup.ui.mvi.UiState
import com.ruleup.verification.domain.entity.PermissionSnapshot
import com.ruleup.verification.domain.entity.TodayResult
import com.ruleup.verification.domain.entity.VerificationAccess

sealed interface ChallengeDetailIntent : MviIntent {
    /** 화면 진입 시 상세 + 셋업 요구사항 조회. */
    data class Load(
        val challengeId: String,
    ) : ChallengeDetailIntent

    /** 재진입(ON_RESUME) 시 셋업 상태 재확인 */
    data object RefreshSetup : ChallengeDetailIntent

    /** "앱 등록하기" → 대상 앱 등록 화면으로 이동. */
    data object RegisterApps : ChallengeDetailIntent

    /** "인증 장소 등록하기" → 지도(앵커) 등록 화면으로 이동. */
    data object RegisterAnchor : ChallengeDetailIntent

    /** 필요한 권한·등록이 모두 끝난 뒤 가입한다. */
    data object Proceed : ChallengeDetailIntent

    /** "이 템플릿으로 만들기" → 복제 초안 생성 후 생성 확인 화면으로. */
    data object CloneChallenge : ChallengeDetailIntent

    /** 가입 차단 안내 시트 닫기. */
    data object DismissJoinBlock : ChallengeDetailIntent

    /** 연결 실패 스낵바의 "다시 시도". */
    data object RetryJoin : ChallengeDetailIntent

    data object DismissJoinRetry : ChallengeDetailIntent

    /** 가입 차단 시트의 CTA(참여 중인 방 목록·내 티어·탐색 등)로 이동. */
    data object FollowJoinBlockAction : ChallengeDetailIntent

    /** (참여자 본인) 내 감시자 초대 생성 → 카카오톡 공유 카드 발송. */
    data object InviteWatcher : ChallengeDetailIntent

    /** (방장) 비공개 방 멤버 초대 링크 발급 후 공유. */
    data object InviteMember : ChallengeDetailIntent

    /** (방 상세) 상단 탭 전환. */
    data class SelectTab(
        val tab: RoomTab,
    ) : ChallengeDetailIntent

    /** (정보 탭) 이 챌린지의 알림 음소거 전환. */
    data class ToggleMute(
        val muted: Boolean,
    ) : ChallengeDetailIntent

    /** (솔로 정보 탭) 캘린더 월 이동. */
    data class ShiftCalendarMonth(
        val offset: Long,
    ) : ChallengeDetailIntent

    /** (피드 탭) 하단 도달 → 다음 페이지. */
    data object LoadMoreThreads : ChallengeDetailIntent

    /** (피드 탭) 실패 후 "다시 불러오기". */
    data object RetryThreads : ChallengeDetailIntent

    /** (랭킹 탭) 멤버 ↔ 방 순위 세그먼트 전환. */
    data class SelectRankingScope(
        val scope: RankingScope,
    ) : ChallengeDetailIntent

    /** (랭킹 탭 · 방 순위) 하단 도달 → 다음 페이지. */
    data object LoadMoreCrossRanking : ChallengeDetailIntent

    /** (방 홈) 그룹 랭킹으로 이동. */
    data object OpenRanking : ChallengeDetailIntent

    /** 권한 재연결 화면 이동. */
    data object OpenPermissionRepair : ChallengeDetailIntent

    /** 수동 인증 화면으로. */
    data object OpenManualCheck : ChallengeDetailIntent

    /** 권한 현황 재조회. */
    data object RefreshPermissions : ChallengeDetailIntent

    /** 이의 증빙 사진 선택. */
    data class PickAppealImage(
        val imageUri: String,
    ) : ChallengeDetailIntent

    /** 이의 시트를 닫는다. */
    data object DismissAppeal : ChallengeDetailIntent

    /** 판정 결과 모달 확인. */
    data object AcknowledgeResult : ChallengeDetailIntent

    /** 실패 건에 이의를 낸다. */
    data class SubmitAppeal(
        val verificationId: String,
        val reason: String,
    ) : ChallengeDetailIntent

    /** 이 챌린지를 신고하는 시트를 연다. */
    data object OpenReport : ChallengeDetailIntent

    /** 멤버 행 탭 */
    data class OpenMemberProfile(
        val userId: String,
    ) : ChallengeDetailIntent

    /** 방 멤버 행의 「신고」 */
    data class OpenUserReport(
        val userId: String,
    ) : ChallengeDetailIntent

    /** 신고 사유 선택. */
    data class SelectReportReason(
        val reason: ReportReason,
    ) : ChallengeDetailIntent

    /** 고른 사유로 신고를 접수한다. */
    data object SubmitReport : ChallengeDetailIntent

    /** 신고 시트(사유 선택·완료 공용)를 닫는다. */
    data object DismissReport : ChallengeDetailIntent

    data object OpenVerificationAccess : ChallengeDetailIntent

    data object VerificationPermissionsReturned : ChallengeDetailIntent

    /** 자동 인증 설정 시트에서 수집·이용에 동의하고 권한 요청을 이어간다. */
    data object ConfirmVerificationAccess : ChallengeDetailIntent

    data object DismissVerificationAccess : ChallengeDetailIntent

    /** (방 홈, 방장 전용) 챌린지 수정 화면으로 이동. */
    data object OpenSettings : ChallengeDetailIntent

    /** 챌린지 탈퇴(방장 포함). */
    data object LeaveChallenge : ChallengeDetailIntent

    data object Back : ChallengeDetailIntent
}

sealed interface ChallengeDetailEffect : MviEffect {
    data class RequestPermissions(
        val tokens: List<String>,
    ) : ChallengeDetailEffect

    /** 초대 생성 성공 → 사용자 본인 명의 카카오톡 공유 실행(룰업 직접 발송 금지). */
    data class ShareWatcherInvite(
        val card: WatcherInviteCard,
        val inviteUrl: String,
    ) : ChallengeDetailEffect

    /** 멤버 초대 링크 공유. */
    data class ShareMemberInvite(
        val challengeTitle: String,
        val inviteUrl: String,
    ) : ChallengeDetailEffect

    data class ShowMessage(
        val message: String,
    ) : ChallengeDetailEffect
}

/** 방 상세 상단 탭. */
enum class RoomTab(
    val label: String,
) {
    INFO("정보"),
    FEED("피드"),
    RANKING("랭킹"),
}

/** 랭킹 탭의 세그먼트. */
enum class RankingScope(
    val label: String,
) {
    // 같은 방의 참여자끼리
    MEMBER("멤버"),

    // 같은 모드의 방끼리
    ROOM("방 순위"),
}

/** 상세 하단 CTA 버튼이 유도할 다음 셋업 단계. */
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
    // 셋업 요구사항(GET setup).
    val setup: ChallengeSetupInfo? = null,
    // 대상 앱이 로컬에 등록됐는지(앱 등록 화면 저장 여부).
    val targetAppsRegistered: Boolean = false,
    // 이 챌린지에서의 "내 감시자"(감시자는 챌린지 × 참여자 단위).
    val watchers: ChallengeWatchers? = null,
    // 초대 생성 요청 중(버튼 중복 탭 방지).
    val isInvitingWatcher: Boolean = false,
    // 방 홈 일괄 조회 결과.
    val room: ChallengeRoom? = null,
    // 방 홈 멤버 목록(GET members).
    val members: ChallengeMembers? = null,
    // 탈퇴 요청 중(버튼 중복 탭 방지).
    val isMemberActionLoading: Boolean = false,
    // 현재 사용자 ID.
    val myUserId: String? = null,
    // 이 방의 알림 음소거 여부.
    val isMuted: Boolean? = null,
    val isMuteSubmitting: Boolean = false,
    // 솔로 캘린더가 보고 있는 달 YYYY-MM.
    val calendarMonth: String? = null,
    // 그 달의 판정 기록.
    val calendar: ChallengeCalendar? = null,
    val isCalendarLoading: Boolean = false,
    // 가입 요청 중(버튼 중복 탭 방지).
    val isJoining: Boolean = false,
    // 가입이 막힌 사유.
    val joinBlock: JoinBlock? = null,
    // 연결 문제로 참여가 실패했다.
    val joinRetryable: Boolean = false,
    // 복제 요청 중(버튼 스피너 + 중복 탭 차단).
    val isCloning: Boolean = false,
    // 방 상세 3탭 (room 이 있을 때만 의미가 있다)
    val selectedTab: RoomTab = RoomTab.INFO,
    // 피드.
    val threads: List<ThreadItem> = emptyList(),
    val threadsCursor: String? = null,
    // 첫 페이지 로딩(스켈레톤) / 다음 페이지 로딩(하단 스피너)을 구분한다
    val isThreadsLoading: Boolean = false,
    val isThreadsPaging: Boolean = false,
    val threadsError: String? = null,
    // 방 안 랭킹.
    val ranking: ChallengeRanking? = null,
    val isRankingLoading: Boolean = false,
    val rankingScope: RankingScope = RankingScope.MEMBER,
    // 방 밖 랭킹.
    val crossRanking: CrossChallengeRanking? = null,
    val isCrossRankingLoading: Boolean = false,
    // 오늘 인증 결과(인증 모듈).
    val todayResult: TodayResult? = null,
    // 이번 진입에서 판정 결과 모달을 이미 닫았는지.
    val resultAcknowledged: Boolean = false,
    // 이의 시트에 첨부된 사진의 업로드 결과 URL.
    val appealImageUrl: String? = null,
    val isUploadingAppealImage: Boolean = false,
    // 사유 입력 하단에 인라인으로 붙는 오류.
    val appealReasonError: String? = null,
    // 지금 이 기기의 권한 현황.
    val permissions: PermissionSnapshot? = null,
    // 이의 제출 중(중복 탭 방지).
    val isSubmittingAppeal: Boolean = false,
    // 신고 시트가 열려 있는지.
    val isReportSheetOpen: Boolean = false,
    // 신고 대상 사용자.
    val reportUserId: String? = null,
    // 참여에 필요한 동의와 권한.
    val pendingAccess: VerificationAccess? = null,
    val isAccessSubmitting: Boolean = false,
    val selectedReportReason: ReportReason? = null,
    val isSubmittingReport: Boolean = false,
    // 접수 결과.
    val reportResult: ReportResult? = null,
) : UiState {
    /** 참여 버튼을 아예 숨길지. */
    val hideJoinButton: Boolean
        get() = detail?.joinBlockReason?.isPrivateInviteOnly == true

    /** 복제 버튼을 활성할 수 있는지. */
    val canClone: Boolean
        get() = detail?.cloneable == true && !isCloning

    /** 정보 탭 헤더의 내 달성률(0~1). */
    val myProgressRate: Double?
        get() = ranking?.me?.successRate

    /** 피드를 더 받아올 수 있는지. */
    val canLoadMoreThreads: Boolean
        get() = threadsCursor != null && !isThreadsPaging && !isThreadsLoading && threadsError == null

    /** 방 밖 랭킹을 더 받아올 수 있는지. */
    val canLoadMoreCrossRanking: Boolean
        get() = crossRanking?.nextCursor != null && !isCrossRankingLoading

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

/** 가입 차단 안내에 필요한 값. */
data class JoinBlock(
    val reason: JoinBlockReason?,
    // REJOIN_COOLDOWN 일 때만
    val rejoinAvailableAt: String? = null,
)

sealed interface ChallengeDetailReducerEvent : ReducerEvent {
    data class VerificationAccessSubmitting(
        val submitting: Boolean,
    ) : ChallengeDetailReducerEvent

    data class Loading(
        val challengeId: String,
    ) : ChallengeDetailReducerEvent

    data class MuteLoaded(
        val muted: Boolean,
    ) : ChallengeDetailReducerEvent

    data class MuteSubmitting(
        val submitting: Boolean,
    ) : ChallengeDetailReducerEvent

    /** 월 이동. */
    data class CalendarMonthChanged(
        val month: String,
    ) : ChallengeDetailReducerEvent

    data class CalendarLoading(
        val loading: Boolean,
    ) : ChallengeDetailReducerEvent

    data class CalendarLoaded(
        val calendar: ChallengeCalendar,
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

    /** 내 감시자 목록 갱신(초대·해제 후 재조회 포함). */
    data class WatchersLoaded(
        val watchers: ChallengeWatchers,
    ) : ChallengeDetailReducerEvent

    /** 초대 생성 요청 시작/종료. */
    data class InvitingWatcher(
        val inviting: Boolean,
    ) : ChallengeDetailReducerEvent

    /** 방 홈 조회 성공 */
    data class RoomLoaded(
        val room: ChallengeRoom,
    ) : ChallengeDetailReducerEvent

    /** 멤버 목록 갱신(방 홈 조회 시). */
    data class MembersLoaded(
        val members: ChallengeMembers,
    ) : ChallengeDetailReducerEvent

    /** 탈퇴 요청 시작/종료. */
    data class MemberActionLoading(
        val loading: Boolean,
    ) : ChallengeDetailReducerEvent

    /** 현재 사용자 ID 로드됨(진입 시 1회). */
    data class MyUserIdLoaded(
        val userId: String?,
    ) : ChallengeDetailReducerEvent

    /** 가입 요청 시작/종료. */
    data class Joining(
        val joining: Boolean,
    ) : ChallengeDetailReducerEvent

    /** 가입이 게이트에 막힘 */
    data class JoinBlocked(
        val block: JoinBlock,
    ) : ChallengeDetailReducerEvent

    data object JoinBlockDismissed : ChallengeDetailReducerEvent

    data class JoinRetryable(
        val visible: Boolean,
    ) : ChallengeDetailReducerEvent

    /** 복제 요청 시작/종료. */
    data class Cloning(
        val cloning: Boolean,
    ) : ChallengeDetailReducerEvent

    data class TabSelected(
        val tab: RoomTab,
    ) : ChallengeDetailReducerEvent

    data class RankingScopeSelected(
        val scope: RankingScope,
    ) : ChallengeDetailReducerEvent

    /** 피드 조회 시작. */
    data class ThreadsLoading(
        val first: Boolean,
    ) : ChallengeDetailReducerEvent

    /** 피드 페이지 도착. */
    data class ThreadsLoaded(
        val page: ChallengeThreads,
        val reset: Boolean,
    ) : ChallengeDetailReducerEvent

    /** 피드 조회 실패. */
    data class ThreadsFailed(
        val message: String,
    ) : ChallengeDetailReducerEvent

    data class RankingLoading(
        val loading: Boolean,
    ) : ChallengeDetailReducerEvent

    data class RankingLoaded(
        val ranking: ChallengeRanking,
    ) : ChallengeDetailReducerEvent

    data class CrossRankingLoading(
        val loading: Boolean,
    ) : ChallengeDetailReducerEvent

    /** 방 밖 랭킹 페이지 도착. */
    data class CrossRankingLoaded(
        val ranking: CrossChallengeRanking,
        val append: Boolean,
    ) : ChallengeDetailReducerEvent

    /** 오늘 인증 결과 도착(인증 모듈). */
    data object ResultAcknowledged : ChallengeDetailReducerEvent

    data class AppealImageUploading(
        val uploading: Boolean,
    ) : ChallengeDetailReducerEvent

    data class AppealImageUploaded(
        val imageUrl: String?,
    ) : ChallengeDetailReducerEvent

    data class AppealReasonRejected(
        val message: String?,
    ) : ChallengeDetailReducerEvent

    data object AppealReset : ChallengeDetailReducerEvent

    data class PermissionsCaptured(
        val permissions: PermissionSnapshot,
    ) : ChallengeDetailReducerEvent

    data class TodayResultLoaded(
        val result: TodayResult,
    ) : ChallengeDetailReducerEvent

    data class SubmittingAppeal(
        val submitting: Boolean,
    ) : ChallengeDetailReducerEvent

    data object ReportSheetOpened : ChallengeDetailReducerEvent

    data class UserReportSheetOpened(
        val userId: String,
    ) : ChallengeDetailReducerEvent

    data object ReportSheetDismissed : ChallengeDetailReducerEvent

    data class VerificationAccessRequested(
        val access: VerificationAccess?,
    ) : ChallengeDetailReducerEvent

    data class ReportReasonSelected(
        val reason: ReportReason,
    ) : ChallengeDetailReducerEvent

    data class SubmittingReport(
        val submitting: Boolean,
    ) : ChallengeDetailReducerEvent

    data class ReportAccepted(
        val result: ReportResult,
    ) : ChallengeDetailReducerEvent
}
