package com.ruleup.entity.challenge

import com.ruleup.entity.user.InterestCategory

/** 참여 방식 (명세 3.x participationType). */
enum class ParticipationType(
    val value: String,
) {
    SOLO("SOLO"),
    GROUP("GROUP"),
    ;

    companion object {
        fun fromValue(value: String?): ParticipationType? = entries.find { it.value == value }
    }
}

/** 인증 방식 (명세 verificationMethods, 다중 선택). */
enum class VerificationMethod(
    val value: String,
    val label: String,
) {
    GPS("GPS", "GPS"),
    PHOTO("PHOTO", "사진"),
    SCREEN_TIME("SCREEN_TIME", "스크린타임"),
    SELF_CHECK("SELF_CHECK", "자체 체크"),
    ;

    companion object {
        fun fromValue(value: String?): VerificationMethod? = entries.find { it.value == value }
    }
}

/** 반복 요일 (명세 repeatDays). */
enum class RepeatDay(
    val value: String,
    val label: String,
) {
    MON("MON", "월"),
    TUE("TUE", "화"),
    WED("WED", "수"),
    THU("THU", "목"),
    FRI("FRI", "금"),
    SAT("SAT", "토"),
    SUN("SUN", "일"),
    ;

    companion object {
        fun fromValue(value: String?): RepeatDay? = entries.find { it.value == value }
    }
}

/** 챌린지 상태 (명세 status). 생성 직후 RECRUITING, 시작 시 ACTIVE. */
enum class ChallengeStatus(
    val value: String,
) {
    RECRUITING("RECRUITING"),
    ACTIVE("ACTIVE"),
    COMPLETED("COMPLETED"),
    ;

    companion object {
        fun fromValue(value: String?): ChallengeStatus? = entries.find { it.value == value }
    }
}

/** 익명/실명 (명세 CH-10 anonymity). */
enum class Anonymity(
    val value: String,
) {
    REAL("REAL"),
    ANONYMOUS("ANONYMOUS"),
    ;

    companion object {
        fun fromValue(value: String?): Anonymity? = entries.find { it.value == value }
    }
}

fun List<String>?.toVerificationMethods(): List<VerificationMethod> = this.orEmpty().mapNotNull(VerificationMethod::fromValue)

fun List<String>?.toRepeatDays(): List<RepeatDay> = this.orEmpty().mapNotNull(RepeatDay::fromValue)

/** SNS 공유 패널티 설정 (명세 penalty.snsShare). */
data class SnsShare(
    val enabled: Boolean,
    // 공유받을 번호 (선택)
    val phone: String?,
)

/** 패널티 설정 (명세 penalty). 매너 차감은 필수. */
data class Penalty(
    val mannerDeduction: Double,
    val snsShare: SnsShare,
    // 그룹 내 공유 여부
    val groupShare: Boolean,
)

/** 보상 설정 (명세 reward). 매너 가산은 필수. */
data class Reward(
    val mannerGain: Double,
)

/**
 * LLM 기본값 추천 결과 (명세 3.1). 구속력 없는 초안이며 사용자가 자유롭게 수정한다.
 */
data class ChallengeRecommendation(
    // 정제된 제목
    val title: String,
    val description: String?,
    // 제목 기반 자동 분류 (인식 불가 시 null)
    val category: InterestCategory?,
    val participationType: ParticipationType,
    // 그룹만, 참여 기준 매너 온도
    val minMannerTemperature: Double?,
    val repeatDays: List<RepeatDay>,
    val durationDays: Int,
    // ISO date
    val startDate: String,
    // ISO date
    val endDate: String,
    val verificationMethods: List<VerificationMethod>,
    val penalty: Penalty,
    val reward: Reward,
)

/**
 * 생성/수정된 챌린지 (명세 3.2/3.4 response).
 */
data class Challenge(
    val challengeId: String,
    val status: ChallengeStatus,
    val title: String,
    val description: String?,
    // 미설정 시 null
    val imageUrl: String?,
    val category: InterestCategory?,
    val participationType: ParticipationType,
    // 그룹만
    val minMannerTemperature: Double?,
    val repeatDays: List<RepeatDay>,
    val durationDays: Int,
    // ISO date
    val startDate: String,
    // ISO date, 서버 파생 (startDate + durationDays)
    val endDate: String,
    val verificationMethods: List<VerificationMethod>,
    val penalty: Penalty,
    val reward: Reward,
)

/**
 * 챌린지 생성 입력 (명세 3.2 request). 추천을 수정·확정한 최종값.
 * endDate 는 서버가 startDate + durationDays 로 파생한다.
 */
data class ChallengeForm(
    val title: String,
    val description: String?,
    // 대표 이미지 (선택)
    val imageUrl: String?,
    val category: InterestCategory,
    val participationType: ParticipationType,
    // 그룹 참여 기준
    val minMannerTemperature: Double?,
    val repeatDays: List<RepeatDay>,
    val durationDays: Int,
    val startDate: String,
    // 최소 1개 이상
    val verificationMethods: List<VerificationMethod>,
    val penalty: Penalty,
    val reward: Reward,
    val anonymity: Anonymity,
)

/**
 * 챌린지 수정 입력 (명세 3.4 request). 변경할 필드만 전달한다(전부 선택).
 */
data class ChallengeUpdate(
    val title: String? = null,
    val description: String? = null,
    val category: InterestCategory? = null,
    val repeatDays: List<RepeatDay>? = null,
    val durationDays: Int? = null,
    val startDate: String? = null,
    val verificationMethods: List<VerificationMethod>? = null,
    val penalty: Penalty? = null,
    val reward: Reward? = null,
    val minMannerTemperature: Double? = null,
)
