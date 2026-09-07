package com.ruleup.challenge.domain.entity

import com.ruleup.domain.entity.category.Category
import com.ruleup.domain.entity.user.Tier

/**
 * 내 챌린지 목록 탭 (명세 `filter` 쿼리). 홈의 참여 중 목록과 챌린지 탭이 같은 API 를 쓰고
 * 이 값으로 갈린다.
 */
enum class MyChallengeFilter(
    val value: String,
) {
    // UPCOMING + ACTIVE
    IN_PROGRESS("IN_PROGRESS"),

    // 완주·기간 만료
    COMPLETED("COMPLETED"),

    // 강퇴·중도 탈퇴·자동 탈퇴
    LEFT("LEFT"),
}

/**
 * 방을 떠난 방식 (명세 `leftType` — LEFT 탭에서만 온다).
 *
 * 강퇴 경로는 **자동 제재 3종뿐**이다(2026-08-26 개편) — 부정행위 검출·연속 실패·권한 미허용.
 * 신고 누적 강퇴([KICK_REPORT])와 방장 재량 강퇴([KICK_BY_OWNER])는 폐지됐지만, 이미 적재된
 * 값이 그대로 내려오므로 읽기 호환을 위해 남겨 둔다.
 */
enum class LeftType(
    val value: String,
) {
    SELF("SELF"),

    // 부정행위 검출 — 해당 챌린지 영구 차단
    KICK_CHEAT("KICK_CHEAT"),
    KICK_FAIL("KICK_FAIL"),
    KICK_PERMISSION("KICK_PERMISSION"),

    // 티어 미달 자동 탈퇴
    AUTO_TIER("AUTO_TIER"),

    // 계정 제재(잠금·영구 정지)에 따른 전 챌린지 자동 탈퇴
    AUTO_SANCTION("AUTO_SANCTION"),

    // 챌린지 직권 폐쇄에 따른 자동 탈퇴
    AUTO_CLOSED("AUTO_CLOSED"),

    // 폐지 — 기존 적재분 읽기 호환
    KICK_REPORT("KICK_REPORT"),
    KICK_BY_OWNER("KICK_BY_OWNER"),
    ;

    /** 내가 나간 것이 아니라 밀려난 것인가 — 목록 뱃지 문구가 갈린다. */
    val isKicked: Boolean
        get() = this != SELF

    companion object {
        /**
         * 미지 값은 null — "이탈"이라는 사실만 남기고 방식은 비운다. 모르는 사유를 [SELF] 로 접으면
         * 강퇴당한 사람에게 스스로 나갔다고 말하게 된다.
         */
        fun fromValue(value: String?): LeftType? = entries.find { it.value == value }
    }
}

/**
 * 내 챌린지 목록 항목 (명세: GET /challenges).
 * 승인제 폐기로 멤버십은 항상 확정 상태다(memberStatus 없음). 내 역할은 [myRole].
 *
 * 제목·설명·이미지는 **심사 상태별 대체 규칙이 적용된 값**으로 온다 — 심사 중·거부면 제목은
 * AI 임시 제목이고 설명·이미지는 null 이다. 클라가 심사 상태를 다시 판단하지 않는다.
 */
data class MyChallenge(
    val challengeId: String,
    val title: String,
    val description: String?,
    // 대표 이미지 (없으면 기본 이미지)
    val imageUrl: String?,
    val category: Category?,
    val mode: ChallengeMode,
    // 그룹만 값이 있다 — 솔로는 공개 범위 개념이 없다
    val visibility: ChallengeVisibility?,
    val status: ChallengeStatus,
    val participantCount: Int,
    val capacity: Int,
    // 최소 입장 티어 (없으면 null)
    val minTier: Tier?,
    // 주간 수행 횟수 1~7. 판정 주기는 1주 고정이고 요일 지정은 없다(구 repeatDays 폐기)
    val weeklyCount: Int,
    val period: ChallengePeriod,
    // 내 역할 (OWNER / MANAGER / MEMBER). 이탈·완료 건은 그 시점의 역할
    val myRole: MemberRole,
    val ownerType: OwnerType,
    // LEFT 탭에서만. 그 외 탭은 null
    val leftType: LeftType?,
    // 이탈 시각 ISO-8601. LEFT 탭에서만
    val leftAt: String?,
    /**
     * 이 방에서의 **내 성공률** 0~1 (명세 2026-09-07 신규).
     *
     * 완료 탭이면 종료 시점의 **최종 성공률**이고 진행 중이면 현재 시점 값이다. 기간 진척도와는
     * 다른 값이다 — 남은 날짜를 성공률로 읽으면 안 된다.
     *
     * 판정 이력이 없으면 **null 이다. 0.0 으로 접지 않는다** — 갓 시작한 방이 전부 실패한 것처럼
     * 보인다. 화면은 null 이면 성공률 줄을 그리지 않는다.
     */
    val successRate: Double?,
) {
    /** 목록 탭·뱃지 판정. 시작 전 방은 진행 지표 대신 시작일 카운트다운을 보여준다. */
    val isUpcoming: Boolean
        get() = status == ChallengeStatus.UPCOMING
}

/**
 * 내 챌린지 목록 한 페이지 (명세: GET /challenges).
 *
 * 진행 중은 무료 동시 참여가 3개라 사실상 한 페이지지만 완료·이탈은 누적이므로 커서로 넘긴다.
 * [nextCursor] 가 null 이면 마지막 페이지다 — [hasNext] 와 어긋나면 커서를 믿는다.
 */
data class MyChallengePage(
    val challenges: List<MyChallenge>,
    val nextCursor: String?,
    val hasNext: Boolean,
)
