package com.ruleup.profile.domain.entity

import com.ruleup.domain.entity.user.AccountRestriction
import com.ruleup.domain.entity.user.AccountStatus

/**
 * 제재 트랙 (명세 `track`). 자동 제재와 직권 제재는 **합산하지 않는다** — 누적으로 승격하는 경로가
 * 없어서, 한 목록에 섞으면 사용자가 없는 누적 규칙을 상상하게 된다.
 */
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

/**
 * 제재 종류 (명세 `type`). 직권은 [FEATURE_SUSPENSION]·[LOCK]·[BAN], 자동은 [CHALLENGE_KICK] 이다.
 */
enum class SanctionType(
    val value: String,
) {
    // 기능 정지 — featureCode 가 무엇이 막혔는지 말한다
    FEATURE_SUSPENSION("FEATURE_SUSPENSION"),

    // 계정 잠금 — 열람은 되고 쓰기가 막힌다
    LOCK("LOCK"),

    // 영구 정지 — endsAt 이 없다
    BAN("BAN"),

    // 자동 제재: 해당 챌린지 강퇴
    CHALLENGE_KICK("CHALLENGE_KICK"),
    ;

    companion object {
        fun fromValue(value: String?): SanctionType? = entries.find { it.value == value }
    }
}

/**
 * 지금 효력이 있는 제재 (명세 `activeSanction`). 없으면 null 이고 계정은 [AccountStatus.ACTIVE] 다.
 *
 * [endsAt] 은 [SanctionType.BAN] 이면 null 이다 — 영구 정지에는 해제일이 없다. 화면이 이 null 을
 * "곧 풀림"으로 접으면 안 된다.
 */
data class ActiveSanction(
    val sanctionId: String,
    val track: SanctionTrack?,
    val type: SanctionType?,
    // 기능 정지일 때 무엇이 막혔는지
    val featureCode: String?,
    val reasonCode: String?,
    // 직권 제재에만 있다. 모더레이션 거부 사유는 회피 방지로 상세를 담지 않는다
    val reasonText: String?,
    val startsAt: String?,
    val endsAt: String?,
    val reviewRequestable: Boolean,
)

/** 직권 제재 이력 한 건 (명세 `admin[]`). */
data class AdminSanction(
    val sanctionId: String,
    val type: SanctionType?,
    val featureCode: String?,
    val reasonCode: String?,
    val startsAt: String?,
    val endsAt: String?,
    // NONE / 요청됨 등 재검토 상태 (서버 문자열)
    val reviewStatus: String?,
)

/**
 * 자동 제재 이력 한 건 (명세 `auto[]`). 지금은 챌린지 강퇴뿐이다.
 *
 * [permanent] 가 true 면 부정행위 검출로 인한 강퇴이고 그 챌린지에 다시 들어갈 수 없다 —
 * 이때 [rejoinAvailableAt] 은 null 이다.
 */
data class AutoSanction(
    val sanctionId: String,
    val type: SanctionType?,
    val challengeId: String?,
    val challengeTitle: String?,
    val reasonCode: String?,
    val permanent: Boolean,
    val rejoinAvailableAt: String?,
    val occurredAt: String?,
)

/**
 * 제재 통지·이력 (명세: GET /users/me/sanctions).
 *
 * **열람 전용이다** — 이의 제기 버튼을 두지 않는다. 강퇴는 CS 문의, 직권 제재는 CS 경유 재검토
 * 1회로만 다툰다.
 *
 * 잠금 상태에서도 열려야 하는 화면이다 — 잠금 사유와 해제일을 볼 수 없으면 사용자가 상황을
 * 알 방법이 없다.
 */
data class SanctionHistory(
    val accountStatus: AccountStatus,
    val activeSanction: ActiveSanction?,
    val admin: List<AdminSanction>,
    val auto: List<AutoSanction>,
) {
    val isEmpty: Boolean
        get() = activeSanction == null && admin.isEmpty() && auto.isEmpty()

    /**
     * 이 계정에 걸린 제한. **게이트가 보는 유일한 값이다.**
     *
     * [accountStatus] 는 정지 여부만 말하고 종류는 [activeSanction] 이 들고 있다 —
     * 서버는 기능 정지도 전체 잠금도 `SUSPENDED` 로 내린다(백오피스 테크 스펙 부록 A).
     * 그래서 둘을 여기서 한 번만 합치고, 화면은 합쳐진 값만 본다.
     *
     * 정지인데 활성 제재가 없으면 전체 잠금으로 본다 — 종류를 모르는 정지를 통과시키면
     * 제재가 조용히 풀린다. 서버도 같은 경우를 방어적으로 다룬다(부록 A).
     */
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
