package com.ruleup.profile.data.dto

import com.ruleup.domain.entity.user.AccountStatus
import com.ruleup.domain.entity.user.LockInfo
import com.ruleup.domain.entity.user.NicknameStatus
import com.ruleup.domain.entity.user.Tier
import com.ruleup.network.dto.requireField
import com.ruleup.profile.domain.entity.MyHome
import com.ruleup.profile.domain.entity.MyHomeCounts
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------- 마이 홈 일괄 조회 (GET /me/home) ----------
@Serializable
data class MyHomeCountsResponse(
    @SerialName("inProgress")
    val inProgress: Int? = null,
    @SerialName("completed")
    val completed: Int? = null,
    @SerialName("left")
    val left: Int? = null,
)

@Serializable
data class LockInfoResponse(
    @SerialName("reason")
    val reason: String? = null,
    @SerialName("unlockAt")
    val unlockAt: String? = null,
)

@Serializable
data class MyHomeResponse(
    // 본인 화면 — 검수 상태 무관하게 본인이 정한 닉네임
    @SerialName("nickname")
    val nickname: String? = null,
    // PENDING / APPROVED / REJECTED / CONFLICT (검수 뱃지용)
    @SerialName("nicknameStatus")
    val nicknameStatus: String? = null,
    @SerialName("profileImageUrl")
    val profileImageUrl: String? = null,
    @SerialName("tier")
    val tier: String? = null,
    @SerialName("score")
    val score: Int? = null,
    @SerialName("displayTier")
    val displayTier: String? = null,
    @SerialName("counts")
    val counts: MyHomeCountsResponse? = null,
    @SerialName("accountStatus")
    val accountStatus: String? = null,
    @SerialName("lockInfo")
    val lockInfo: LockInfoResponse? = null,
)

internal fun MyHomeResponse.toDomain(): MyHome =
    MyHome(
        nickname = nickname.requireField("nickname"),
        nicknameStatus = NicknameStatus.fromValue(nicknameStatus),
        profileImageUrl = profileImageUrl,
        tier = Tier.fromValue(tier),
        score = score ?: 0,
        // 표시 티어가 비면 실제 티어로 떨어뜨린다 — 유예 밴드를 모르는 쪽이 부풀리는 쪽보다 안전하다.
        displayTier = displayTier?.let(Tier::fromValue) ?: Tier.fromValue(tier),
        counts =
            MyHomeCounts(
                inProgress = counts?.inProgress ?: 0,
                completed = counts?.completed ?: 0,
                left = counts?.left ?: 0,
            ),
        accountStatus = AccountStatus.fromValue(accountStatus),
        lockInfo = lockInfo?.toDomain(),
    )

/** 사유·해제 시각 중 하나라도 비면 잠금 안내를 그리지 않는다 — 빈칸 배너는 불안만 준다. */
internal fun LockInfoResponse.toDomain(): LockInfo? {
    val reason = reason ?: return null
    val unlockAt = unlockAt ?: return null
    return LockInfo(reason = reason, unlockAt = unlockAt)
}
