package com.ruleup.report.domain.entity

/** 신고 사유. */
enum class ReportReason(
    val value: String,
) {
    // 부정 인증 의심
    CHEATING_SUSPECT("CHEATING_SUSPECT"),

    INAPPROPRIATE("INAPPROPRIATE"),

    SPAM_AD("SPAM_AD"),

    ETC("ETC"),
    ;

    companion object {
        /** 사용자 신고에서 고를 수 있는 사유 4종. */
        val forUser: List<ReportReason> = entries

        /** 챌린지 신고 */
        val forChallenge: List<ReportReason> = entries - CHEATING_SUSPECT
    }
}

/** 신고가 발생한 화면. */
enum class ReportContext(
    val value: String,
) {
    PROFILE("PROFILE"),
    CHALLENGE_DETAIL("CHALLENGE_DETAIL"),
    ROOM("ROOM"),
}

/** 신고 대상. */
sealed interface ReportTarget {
    val reason: ReportReason
    val context: ReportContext

    /** 사용자 신고. */
    data class User(
        val userId: String,
        override val reason: ReportReason,
        override val context: ReportContext,
        // 이 사람의 행위가 벌어진 챌린지.
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

/** 접수 직후 내 화면에만 적용되는 효과. */
enum class HiddenEffect(
    val value: String,
) {
    // 임시 닉네임·기본 이미지로 바뀌고 작성 글이 보이지 않는다.
    USER_CONTENT_MASKED("USER_CONTENT_MASKED"),

    // 미참여 챌린지
    CHALLENGE_HIDDEN("CHALLENGE_HIDDEN"),

    // 참여 중인 챌린지
    CHALLENGE_MASKED("CHALLENGE_MASKED"),
    ;

    companion object {
        /** 모르는 값은 null */
        fun fromValue(value: String?): HiddenEffect? = entries.find { it.value == value }
    }
}

/** 접수 결과. */
data class ReportResult(
    val reportId: String,
    val hiddenEffect: HiddenEffect?,
)
