package com.ruleup.profile.presentation.home.viewmodel

import com.ruleup.profile.domain.entity.GroupChallengeSummary
import com.ruleup.profile.domain.entity.MyHome
import com.ruleup.profile.domain.entity.StatsReport
import com.ruleup.ui.mvi.MviEffect
import com.ruleup.ui.mvi.MviIntent
import com.ruleup.ui.mvi.ReducerEvent
import com.ruleup.ui.mvi.UiState

sealed interface MyHomeIntent : MviIntent {
    data object Load : MyHomeIntent

    /** 재진입(ON_RESUME) 시 재조회 — 프로필 편집·챌린지 완주 등으로 값이 바뀐다. */
    data object Refresh : MyHomeIntent

    data object OpenProfileEdit : MyHomeIntent

    /** 티어 카드의 「자세히」 — 점수·구간표·최근 변동. */
    data object OpenTier : MyHomeIntent

    /** 메뉴: 인증 기록 — 월 캘린더와 일자별 판정 결과. */
    data object OpenCalendar : MyHomeIntent

    data object OpenAppeals : MyHomeIntent

    /** 메뉴: 그룹 랭킹 — 참여 중 그룹 챌린지를 골라 랭킹 화면으로 (1개면 바로 이동). */
    data object OpenRanking : MyHomeIntent

    data class SelectPickedChallenge(
        val challengeId: String,
    ) : MyHomeIntent

    data object DismissChallengePicker : MyHomeIntent

    data object OpenStats : MyHomeIntent

    data object OpenInvite : MyHomeIntent

    /**
     * 메뉴: 신고한 사용자·챌린지.
     *
     * Figma 는 설정 허브 안에 두지만 앱에 설정 화면이 아직 없어 마이 홈에 직접 단다 —
     * 진입점이 없으면 차단을 풀 방법이 사라진다.
     */
    data object OpenBlocks : MyHomeIntent

    /**
     * 메뉴: 감시자 — 감시자는 챌린지별로 붙으므로 방을 먼저 고른다(1개면 바로 이동).
     *
     * 지정·상태 확인은 방 상세의 감시자 섹션이 담당한다 — Figma 1134:1603 의 전용 화면과 내용이
     * 같아 화면을 하나 더 만들지 않았다.
     */
    data object OpenWatchers : MyHomeIntent

    /** 메뉴: 알림 설정 — 서버 미완(명세 `수정중`)이라 진입점만 두고 안내한다. */
    data object OpenNotificationSettings : MyHomeIntent

    /** 메뉴: 계정 · 약관 — 설정 허브. */
    data object OpenSettings : MyHomeIntent

    data object OpenHomeTab : MyHomeIntent

    /** 하단 탭: 탐색으로 전환. */
    data object OpenChallengeTab : MyHomeIntent

    /** 하단 탭: 내 챌린지(진행 중 / 완료·이탈). */
    data object OpenMyChallengesTab : MyHomeIntent
}

/** 선택 시트가 무엇을 고르는 중인지. 고른 뒤 갈 곳이 달라진다. */
enum class ChallengePickerTarget {
    RANKING,
    WATCHERS,
}

data class ChallengePicker(
    val target: ChallengePickerTarget,
    val challenges: List<GroupChallengeSummary>,
)

sealed interface MyHomeEffect : MviEffect {
    data class ShowMessage(
        val message: String,
    ) : MyHomeEffect
}

data class MyHomeState(
    val isLoading: Boolean,
    val home: MyHome?,
    // 전체 성공률 카드용. 마이 홈은 /me/home 과 /me/stats 두 응답을 합쳐 그린다
    val stats: StatsReport?,
    val errorMessage: String?,
    // 챌린지 선택 시트 (null = 닫힘). 랭킹·감시자 둘 다 방 단위라 같은 시트를 쓴다
    val picker: ChallengePicker? = null,
    val isLoadingPicker: Boolean = false,
) : UiState {
    companion object {
        val initial =
            MyHomeState(
                isLoading = true,
                home = null,
                stats = null,
                errorMessage = null,
            )
    }
}

sealed interface MyHomeReducerEvent : ReducerEvent {
    data object Loading : MyHomeReducerEvent

    data class Loaded(
        val home: MyHome,
    ) : MyHomeReducerEvent

    /**
     * 통계는 마이 홈의 필수 데이터가 아니다 — 실패해도 화면은 그대로 뜨고 성공률 칸만 비운다.
     * 그래서 [Failed] 와 따로 둔다.
     */
    data class StatsLoaded(
        val stats: StatsReport,
    ) : MyHomeReducerEvent

    data class Failed(
        val message: String,
    ) : MyHomeReducerEvent

    data class LoadingPicker(
        val loading: Boolean,
    ) : MyHomeReducerEvent

    /** 참여 중 챌린지 2개 이상 — 선택 시트 노출. */
    data class PickerShown(
        val picker: ChallengePicker,
    ) : MyHomeReducerEvent

    data object PickerDismissed : MyHomeReducerEvent
}
