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
 * 앱이 모르는 값은 [fromValue] 가 null 을 돌려주고 화면은 상태 표기를 생략한다 — 임의로 실패·성공
 * 어느 쪽으로도 접지 않는다.
 */
enum class TodayVerificationStatus(
    val value: String,
) {
    // 오늘 인증 완료
    DONE("DONE"),

    // 00~03시 유예 구간 — 아직 판정 전
    CHECKING("CHECKING"),

    // 오늘은 판정 대상일이 아님 (반복 요일 밖)
    NOT_TARGET("NOT_TARGET"),
    ;

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
 * [pinnedNotice] 는 Phase 1 에서 서버가 항상 null 로 내려준다(공지 기능은 Phase 2). 필드를 지우지
 * 않고 null 로 두는 것이 서버 합의라, 화면도 값이 있을 때만 배너를 그리는 형태로 둔다.
 */
data class ChallengeRoom(
    // 서버 합의: 미지 값은 MEMBER 취급 (운영 스프린트의 role 값 추가에 대비)
    val myRole: MemberRole,
    // BOT 이면 "방장 되기"(선착순 클레임) 진입점을 노출한다
    val ownerType: OwnerType,
    val summary: RoomSummary,
    // 고정 공지 요약 — 없거나, 봇방장 방이거나, 작성자를 내가 차단했으면 null
    val pinnedNotice: NoticeSummary?,
    // 상위 3. 10회 미만 참여자는 등재되지 않아 3명보다 적을 수 있다
    val topRanking: List<RoomTopRanker>,
    val myTodayStatus: TodayVerificationStatus?,
)
