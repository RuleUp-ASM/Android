package com.ruleup.challenge.domain.entity

/**
 * 방 안에서 사람을 가리키는 공통 표현 (명세 `user` 오브젝트 — 스레드·랭킹 공용).
 *
 * 서버가 **차단·모더레이션 마스킹을 적용한 뒤** 내려주므로 앱은 [blocked] 로 가리는 게 아니라
 * "가려진 상태임"을 표시하는 데만 쓴다 — 목록에서 빼면 등수가 어긋난다.
 */
data class RoomUser(
    val userId: String,
    val nickname: String,
    val profileImageUrl: String?,
    val blocked: Boolean,
)

/**
 * 내 오늘 인증 상태 (명세 `myTodayStatus`).
 *
 * room 명세의 예시에는 [DONE]·[CHECKING]·[NOT_TARGET] 만 나오지만, 같은 사실을 내려주는
 * `GET /challenges/{id}/verifications/today` 의 `status` 는 [IN_PROGRESS]·[FAILED] 를 포함한 5종이다.
 * 어휘가 같으므로 5종을 모두 받아 둔다 — 서버가 안 보내면 아무 일도 없고, 보내면 정상 표기된다.
 *
 * 그래도 앱이 모르는 값은 [fromValue] 가 null 을 돌려주고 화면이 상태 표기를 생략한다 — 임의로
 * 실패·성공 어느 쪽으로도 접지 않는다.
 */
enum class TodayVerificationStatus(
    val value: String,
) {
    // 인증 창이 아직 열려 있음
    IN_PROGRESS("IN_PROGRESS"),

    // 귀속일은 끝났고 확정 전 — 늦게 오는 신호를 받는 유예 구간이라 성공·실패 양쪽으로 열려 있다
    CHECKING("CHECKING"),

    // 오늘 인증 완료
    DONE("DONE"),

    // 실패 확정
    FAILED("FAILED"),

    // 오늘은 판정 대상일이 아님 — 실패가 아니다
    NOT_TARGET("NOT_TARGET"),
    ;

    /** 실패로 확정됐는가. 재평가 중·진행 중은 아직 실패가 아니다. */
    val isFailure: Boolean
        get() = this == FAILED

    companion object {
        fun fromValue(value: String?): TodayVerificationStatus? = entries.find { it.value == value }
    }
}

/**
 * 방 홈 요약 (명세 `summary`).
 *
 * [roomSuccessRate] 는 판정 이력이 없으면 **null 이다 — 0 으로 접지 않는다.** 표본이 없는 것과
 * 0% 는 다른 사실이라, 0% 로 바꾸면 갓 만든 방이 실패한 방처럼 보인다.
 */
data class RoomSummary(
    val title: String,
    // 방 전체 성공률 0~1 = 성공 ÷ (성공+실패)
    val roomSuccessRate: Double?,
    val remainingDays: Int,
    val participantCount: Int,
    val capacity: Int,
)

/** 방 홈 랭킹 상위 3 (명세 `topRanking[]`). 전체 랭킹([RankingEntry])보다 필드가 적은 별개 표현이다. */
data class RoomTopRanker(
    val rank: Int,
    val userId: String,
    val nickname: String,
    val profileImageUrl: String?,
    // 성공률 0~1
    val successRate: Double,
)

/**
 * 챌린지 방 내부 일괄 조회 (명세: GET /challenges/{id}/room). ACTIVE 멤버 전용 — 비멤버는 403.
 *
 * **읽음 관련 필드는 없다** — 미읽음 뱃지는 "확인해야 할 일"로 읽혀 압박이 되므로 정책상 제외됐다.
 * 응답의 `pinnedNotice` 도 읽지 않는다 — 공지가 제품에서 빠졌다.
 */
data class ChallengeRoom(
    // 서버 합의: 미지 값은 MEMBER 취급 (운영 스프린트의 role 값 추가에 대비)
    val myRole: MemberRole,
    // BOT 이면 "방장 되기"(선착순 클레임) 진입점을 노출한다
    val ownerType: OwnerType,
    val summary: RoomSummary,
    // 상위 3. 10회 미만 참여자는 등재되지 않아 3명보다 적을 수 있다
    val topRanking: List<RoomTopRanker>,
    val myTodayStatus: TodayVerificationStatus?,
)
