package com.ruleup.challenge.domain.entity

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

/** 챌린지 상태 (명세 status). 시작 전 UPCOMING, 시작 시 ACTIVE, 종료 시 COMPLETED. */
enum class ChallengeStatus(
    val value: String,
) {
    UPCOMING("UPCOMING"),
    ACTIVE("ACTIVE"),
    COMPLETED("COMPLETED"),
    ;

    companion object {
        fun fromValue(value: String?): ChallengeStatus? = entries.find { it.value == value }
    }
}

/**
 * 이미지 모더레이션 상태 (명세 moderationStatus). 이름 모더레이션은 폐기, 이미지 전용.
 * NONE(이미지 없음·즉시 모집) / PENDING_REVIEW(검수 중·모집 차단) / APPROVED / REJECTED.
 */
enum class ModerationStatus(
    val value: String,
) {
    NONE("NONE"),
    PENDING_REVIEW("PENDING_REVIEW"),
    APPROVED("APPROVED"),
    REJECTED("REJECTED"),
    ;

    /** 모집 차단 여부 — 검수 중이거나 거부됨. */
    val blocksRecruit: Boolean
        get() = this == PENDING_REVIEW || this == REJECTED

    companion object {
        fun fromValue(value: String?): ModerationStatus? = entries.find { it.value == value }
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
    // Step 1·2(입력 적합성·콘텐츠 검수) 차단 신호. true 면 나머지 필드는 무의미하며
    // 화면은 최초 생성 화면으로 복귀한다(오류 아님, 200).
    val fallback: Boolean,
    // 템플릿 매칭 성공 여부. false 면 수동(MANUAL) 추천만 내려온다.
    val matched: Boolean,
    // 매칭된 루틴 템플릿 id (직접 입력/무매칭이면 null)
    val templateId: Int?,
    // 정제된 제목
    val title: String,
    val description: String?,
    // 제목 기반 자동 분류 (인식 불가 시 null)
    val category: InterestCategory?,
    // 기본 인증 방식 (options 중 recommended)
    val recommendedMethod: SelectedMethod,
    // 선택 가능한 인증 옵션 (AUTO/MANUAL)
    val options: List<VerificationOption>,
    // 수정 가능한 목표값
    val params: List<ParamSpec>,
    // 자동 인증 동작 한 줄 설명
    val rationale: String?,
    val participationType: ParticipationType,
    // 그룹만, 참여 기준 매너 온도 초안
    val minMannerTemperature: Double?,
    // 기준 온도 입력 상한 (= 생성자 현재 온도). 폼 슬라이더 상한으로 사용.
    val maxMannerTemperature: Double?,
    // 최대 참여 인원 초안
    val maxParticipants: Int?,
    val repeatDays: List<RepeatDay>,
    val durationDays: Int,
    // ISO date
    val startDate: String,
    // ISO date
    val endDate: String,
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
    // 최대 참여 인원 (SOLO 는 1)
    val maxParticipants: Int,
    // 그룹만
    val minMannerTemperature: Double?,
    val repeatDays: List<RepeatDay>,
    val durationDays: Int,
    // ISO date
    val startDate: String,
    // ISO date, 서버 파생 (startDate + durationDays)
    val endDate: String,
    // 매칭된 루틴 (직접 입력이면 null)
    val templateId: Int?,
    // 이미지 모더레이션 상태 (모집 차단 판정용)
    val moderationStatus: ModerationStatus,
    // 인증 스냅샷 (생성 시점 고정)
    val verification: VerificationConfig?,
    // 목표값 (예: {"distance_km": 5})
    val params: Map<String, ParamValue>,
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
    // 최대 참여 인원 (GROUP 필수, SOLO 는 1)
    val maxParticipants: Int,
    // 그룹 참여 기준
    val minMannerTemperature: Double?,
    val repeatDays: List<RepeatDay>,
    val durationDays: Int,
    val startDate: String,
    // 매칭된 루틴 (직접 입력이면 null)
    val templateId: Int?,
    // 선택한 인증 방식
    val selectedMethod: SelectedMethod,
    // 목표값 (예: {"distance_km": 5})
    val params: Map<String, ParamValue>,
    // AUTO 선택 시 단말 보유 권한 토큰
    val grantedPermissions: List<String>,
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
    // 명시적 null = 이미지 제거, 생략(미전달) = 미변경. 변경 시 서버가 재모더레이션.
    val imageUrl: String? = null,
    val category: InterestCategory? = null,
    val repeatDays: List<RepeatDay>? = null,
    val durationDays: Int? = null,
    val startDate: String? = null,
    // 시작 전 목표값 조정 (같은 루틴, 값만)
    val params: Map<String, ParamValue>? = null,
    val penalty: Penalty? = null,
    val reward: Reward? = null,
    val minMannerTemperature: Double? = null,
    // 언제든(시작 전·진행 중) 수정 가능. 현재 인원 미만 축소 불가(서버 400).
    val maxParticipants: Int? = null,
)
