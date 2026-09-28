package com.ruleup.profile.domain.entity

import com.ruleup.domain.entity.user.AccountRestriction
import com.ruleup.domain.entity.user.AccountStatus

/** 제재 트랙. */
enum class SanctionTrack(
    val value: String,
) {
    AUTO("AUTO"),
    ADMIN("ADMIN"),
    ;

    companion object {
        fun fromValue(value: String?): SanctionTrack? = entries.find { it.value == value }
    }
}

/** 제재 종류. */
enum class SanctionType(
    val value: String,
) {
    // 기능 정지
    FEATURE_SUSPENSION("FEATURE_SUSPENSION"),

    // 계정 잠금
    LOCK("LOCK"),

    // 영구 정지
    BAN("BAN"),

    // 자동 제재: 해당 챌린지 강퇴
    CHALLENGE_KICK("CHALLENGE_KICK"),
    ;

    companion object {
        fun fromValue(value: String?): SanctionType? = entries.find { it.value == value }
    }
}

/** 지금 효력이 있는 제재. */
data class ActiveSanction(
    val sanctionId: String?,
    val track: SanctionTrack?,
    val type: SanctionType?,
    // 기능 정지일 때 무엇이 막혔는지
    val featureCode: String?,
    val reasonCode: String?,
    // 직권 제재에만 있다.
    val reasonText: String?,
    val startsAt: String?,
    val endsAt: String?,
    val reviewRequestable: Boolean,
)

/** 직권 제재 이력 한 건. */
data class AdminSanction(
    val sanctionId: String?,
    val type: SanctionType?,
    val featureCode: String?,
    val reasonCode: String?,
    val startsAt: String?,
    val endsAt: String?,
    // NONE / 요청됨 등 재검토 상태 (서버 문자열)
    val reviewStatus: String?,
)

/** 자동 제재 이력 한 건. */
data class AutoSanction(
    val sanctionId: String?,
    val type: SanctionType?,
    val challengeId: String?,
    val challengeTitle: String?,
    val reasonCode: String?,
    val permanent: Boolean,
    val rejoinAvailableAt: String?,
    val occurredAt: String?,
)

/** 제재 통지·이력. */
data class SanctionHistory(
    val accountStatus: AccountStatus,
    val activeSanction: ActiveSanction?,
    val admin: List<AdminSanction>,
    val auto: List<AutoSanction>,
) {
    val isEmpty: Boolean
        get() = activeSanction == null && admin.isEmpty() && auto.isEmpty()

    /** 이 계정에 걸린 제한. */
    val restriction: AccountRestriction
        get() =
            if (accountStatus == AccountStatus.ACTIVE) {
                AccountRestriction.None
            } else {
                when (activeSanction?.type) {
                    SanctionType.FEATURE_SUSPENSION -> AccountRestriction.Feature(activeSanction.featureCode)
                    SanctionType.BAN -> AccountRestriction.Banned
                    // CHALLENGE_KICK 은 방에서만 효력이 있어 계정을 잠그지 않는다.
                    SanctionType.CHALLENGE_KICK -> AccountRestriction.None
                    SanctionType.LOCK, null -> AccountRestriction.Locked
                }
            }
}
