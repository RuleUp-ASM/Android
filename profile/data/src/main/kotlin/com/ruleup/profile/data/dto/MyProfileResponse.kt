package com.ruleup.profile.data.dto

import com.ruleup.domain.entity.category.toCategories
import com.ruleup.domain.entity.user.AccountStatus
import com.ruleup.domain.entity.user.AgreementConsent
import com.ruleup.domain.entity.user.AgreementType
import com.ruleup.domain.entity.user.Gender
import com.ruleup.domain.entity.user.LockInfo
import com.ruleup.domain.entity.user.NicknameStatus
import com.ruleup.domain.entity.user.Tier
import com.ruleup.domain.entity.user.User
import com.ruleup.network.dto.requireField
import com.ruleup.profile.domain.entity.ImageModerationStatus
import com.ruleup.profile.domain.entity.MyProfile
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate

/** `GET /api/v1/users/me`. `user` 블록은 로그인 응답과 같은 스키마다. */
@Serializable
data class MyProfileResponse(
    @SerialName("user") val user: MyUserResponse? = null,
    @SerialName("birthDate") val birthDate: String? = null,
    @SerialName("gender") val gender: String? = null,
    @SerialName("agreements") val agreements: Map<String, AgreementConsentResponse>? = null,
)

@Serializable
data class MyUserResponse(
    @SerialName("id") val id: String? = null,
    @SerialName("nickname") val nickname: String? = null,
    @SerialName("nicknameStatus") val nicknameStatus: String? = null,
    @SerialName("profileImageUrl") val profileImageUrl: String? = null,
    @SerialName("profileImageStatus") val profileImageStatus: String? = null,
    @SerialName("tier") val tier: String? = null,
    @SerialName("score") val score: Int? = null,
    @SerialName("displayTier") val displayTier: String? = null,
    @SerialName("interestCategories") val interestCategories: List<String>? = null,
    @SerialName("onboardingCompleted") val onboardingCompleted: Boolean? = null,
    @SerialName("accountStatus") val accountStatus: String? = null,
    @SerialName("lockInfo") val lockInfo: MyLockInfoResponse? = null,
)

@Serializable
data class MyLockInfoResponse(
    @SerialName("reason") val reason: String? = null,
    @SerialName("unlockAt") val unlockAt: String? = null,
)

@Serializable
data class AgreementConsentResponse(
    @SerialName("agreed") val agreed: Boolean? = null,
    @SerialName("version") val version: String? = null,
    @SerialName("agreedAt") val agreedAt: String? = null,
)

internal fun MyProfileResponse.toDomain(): MyProfile {
    val userResponse = user.requireField("user")
    return MyProfile(
        user = userResponse.toDomain(),
        profileImageStatus = ImageModerationStatus.fromValue(userResponse.profileImageStatus),
        // 파싱 실패는 null 로 접는다 — 표시용이라, 형식이 어긋났다고 프로필 조회 전체를 실패시킬 이유가 없다.
        birthDate = birthDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
        gender = Gender.fromValue(gender),
        agreements =
            agreements
                .orEmpty()
                .mapNotNull { (key, value) ->
                    val type = AgreementType.entries.find { it.key == key } ?: return@mapNotNull null
                    type to AgreementConsent(agreed = value.agreed ?: false, version = value.version.orEmpty())
                }.toMap(),
    )
}

/** 로그인 응답의 UserResponse 와 같은 규칙 — id·nickname 만 필수고 나머지는 안전한 기본으로 떨어진다. */
internal fun MyUserResponse.toDomain(): User =
    User(
        id = id.requireField("user.id"),
        nickname = nickname.requireField("user.nickname"),
        nicknameStatus = NicknameStatus.fromValue(nicknameStatus),
        profileImageUrl = profileImageUrl,
        tier = Tier.fromValue(tier),
        score = score ?: 0,
        displayTier = displayTier?.let(Tier::fromValue) ?: Tier.fromValue(tier),
        interestCategories = interestCategories.toCategories(),
        onboardingCompleted = onboardingCompleted ?: true,
        accountStatus = AccountStatus.fromValue(accountStatus),
        lockInfo =
            lockInfo?.let {
                if (it.reason == null || it.unlockAt == null) null else LockInfo(it.reason, it.unlockAt)
            },
    )
