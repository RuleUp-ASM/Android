package com.ruleup.report.domain.entity

/**
 * 신고 사유(명세 POST /reports `reason`).
 *
 * **사유별 처리 차이는 없다** — 운영자 검토 시 분류 힌트로만 쓴다. 자유 텍스트는 받지 않는다
 * (2026-08-26 개편: 성실히 적히지 않아 판단 재료로 못 쓰게 되면서 LLM 접수 필터와 함께 폐지).
 *
 * 고를 수 있는 목록이 대상마다 다르므로 화면은 [forUser]/[forChallenge] 를 그대로 그린다.
 * 목록을 화면에서 다시 추리면 서버만 아는 제약이 화면마다 복제된다.
 */
enum class ReportReason(
    val value: String,
) {
    // 부정 인증 의심 — 사람의 행위라 챌린지 신고에는 없다.
    CHEATING_SUSPECT("CHEATING_SUSPECT"),

    INAPPROPRIATE("INAPPROPRIATE"),

    SPAM_AD("SPAM_AD"),

    ETC("ETC"),
    ;

    companion object {
        /** 사용자 신고에서 고를 수 있는 사유 4종. */
        val forUser: List<ReportReason> = entries

        /** 챌린지 신고 — [CHEATING_SUSPECT] 를 보내면 서버가 400 INVALID_REPORT_REASON 으로 막는다. */
        val forChallenge: List<ReportReason> = entries - CHEATING_SUSPECT
    }
}

/**
 * 신고가 발생한 화면(명세 `contextType`).
 *
 * 스냅샷에 "어떤 화면에서 신고했는지"를 남기는 용도라 카운트나 판정에는 쓰이지 않는다.
 * NOTICE·COMMENT 는 공지·댓글 기능과 함께 Phase 2 라 여기 없다.
 */
enum class ReportContext(
    val value: String,
) {
    PROFILE("PROFILE"),
    CHALLENGE_DETAIL("CHALLENGE_DETAIL"),
    ROOM("ROOM"),
}

/**
 * 신고 대상(명세 `targetType` — USER / CHALLENGE **2갈래뿐**).
 *
 * 대상마다 지켜야 할 규칙이 달라 sealed 로 갈라 두고 각자의 `init` 에서 막는다. 규칙을 화면에
 * 두면 신고 진입점이 늘 때마다 같은 검사를 베껴야 하고, 한 곳만 빠져도 400 이 사용자에게 간다.
 */
sealed interface ReportTarget {
    val reason: ReportReason
    val context: ReportContext

    /**
     * 사용자 신고. 본인은 신고할 수 없으나 그 검사는 서버가 한다 — 앱은 내 userId 를 확실히 알 수
     * 있는 자리가 아니어서, 여기서 막으면 오히려 정상 신고를 놓칠 수 있다.
     */
    data class User(
        val userId: String,
        override val reason: ReportReason,
        override val context: ReportContext,
        // 이 사람의 행위가 벌어진 챌린지. 스냅샷에 방 정보를 담기 위한 것이지 카운트 집계용이 아니다.
        val challengeId: String? = null,
    ) : ReportTarget {
        init {
            require(context == ReportContext.PROFILE || challengeId != null) {
                "${context.value} 에서 하는 사용자 신고에는 발생한 챌린지가 필요해요."
            }
        }
    }

    /** 챌린지 신고. */
    data class Challenge(
        val challengeId: String,
        override val reason: ReportReason,
        override val context: ReportContext,
    ) : ReportTarget {
        init {
            require(reason in ReportReason.forChallenge) {
                "챌린지는 ${reason.value} 사유로 신고할 수 없어요."
            }
        }
    }
}

/**
 * 접수 직후 **내 화면에만** 적용되는 효과(명세 `hiddenEffect`).
 *
 * 차단은 개인 설정이라 다른 사람 화면은 그대로다. 신고자에게 "가려졌다"를 알려 주려고 내려온다.
 */
enum class HiddenEffect(
    val value: String,
) {
    // 임시 닉네임·기본 이미지로 바뀌고 작성 글이 보이지 않는다. 사용자 신고는 항상 이 효과다.
    USER_CONTENT_MASKED("USER_CONTENT_MASKED"),

    // 미참여 챌린지 — 탐색 목록에서 통째로 빠진다.
    CHALLENGE_HIDDEN("CHALLENGE_HIDDEN"),

    // 참여 중인 챌린지 — 방을 없애지 않고 기본 이미지·임시 제목·빈 설명으로 가린다. 나가려면 직접 나가야 한다.
    CHALLENGE_MASKED("CHALLENGE_MASKED"),
    ;

    companion object {
        /** 모르는 값은 null — 효과 안내 문구만 생략하면 되고 접수 자체는 성공이다. */
        fun fromValue(value: String?): HiddenEffect? = entries.find { it.value == value }
    }
}

/**
 * 접수 결과(명세 POST /reports 201).
 *
 * 서버는 처리 경과·결과를 신고자에게 알리지 않는다(익명성·보복 방지). 그래서 화면이 보여줄 수
 * 있는 것은 완료 안내와 [hiddenEffect] 뿐이고, "검토 중" 같은 상태 표시는 만들 수 없다.
 *
 * 응답의 `blocked` 는 담지 않는다 — 유저·챌린지 신고 모두 **항상 true** 라 분기가 생기지 않는다.
 */
data class ReportResult(
    val reportId: String,
    val hiddenEffect: HiddenEffect?,
)
