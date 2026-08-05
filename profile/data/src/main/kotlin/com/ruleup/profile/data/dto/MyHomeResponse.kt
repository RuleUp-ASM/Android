package com.ruleup.profile.data.dto

import com.ruleup.domain.entity.user.NicknameStatus
import com.ruleup.network.dto.requireField
import com.ruleup.profile.domain.entity.MyHome
import com.ruleup.profile.domain.entity.MyHomeCounts
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------- 마이 홈 일괄 조회 (GET /me/home) ----------
@Serializable
data class MyHomeCountsResponse(
    @SerialName("completed")
    val completed: Int? = null,
    @SerialName("inProgress")
    val inProgress: Int? = null,
    @SerialName("groups")
    val groups: Int? = null,
)

@Serializable
data class MyHomeResponse(
    // 본인 화면 — 검수 상태 무관하게 본인이 정한 닉네임
    @SerialName("nickname")
    val nickname: String? = null,
    // PENDING / APPROVED / REJECTED (검수 뱃지용)
    @SerialName("nicknameStatus")
    val nicknameStatus: String? = null,
    @SerialName("profileImageUrl")
    val profileImageUrl: String? = null,
    @SerialName("mannerTemperature")
    val mannerTemperature: Double? = null,
    @SerialName("counts")
    val counts: MyHomeCountsResponse? = null,
)

internal fun MyHomeResponse.toDomain(): MyHome =
    MyHome(
        nickname = nickname.requireField("nickname"),
        nicknameStatus = NicknameStatus.fromValue(nicknameStatus),
        profileImageUrl = profileImageUrl,
        mannerTemperature = mannerTemperature ?: 0.0,
        counts =
            MyHomeCounts(
                completed = counts?.completed ?: 0,
                inProgress = counts?.inProgress ?: 0,
                groups = counts?.groups ?: 0,
            ),
    )
