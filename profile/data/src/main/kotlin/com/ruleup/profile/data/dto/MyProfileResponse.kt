package com.ruleup.profile.data.dto

import com.ruleup.domain.entity.category.toCategories
import com.ruleup.domain.entity.user.AccountStatus
import com.ruleup.domain.entity.user.AgreementConsent
import com.ruleup.domain.entity.user.AgreementType
import com.ruleup.domain.entity.user.Gender
import com.ruleup.domain.entity.user.LockInfo
import com.ruleup.domain.entity.user.NicknameStatus
import com.ruleup.domain.entity.user.SocialProvider
import com.ruleup.domain.entity.user.Tier
import com.ruleup.domain.entity.user.User
import com.ruleup.domain.entity.user.UserAccount
import com.ruleup.network.dto.requireField
import com.ruleup.profile.domain.entity.ImageModerationStatus
import com.ruleup.profile.domain.entity.MyProfile
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate

/** `GET /api/v1/users/me`. */
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
    @SerialName("provider") val provider: String? = null,
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
        // 파싱 실패는 null 로 접는다
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

/** 로그인 응답의 UserResponse 와 같은 규칙 */
internal fun MyUserResponse.toDomain(): User =
    User(
        id = id.requireField("user.id"),
        nickname = nickname.requireField("user.nickname"),
        profileImageUrl = profileImageUrl,
        account =
            UserAccount(
                nicknameStatus = NicknameStatus.fromValue(nicknameStatus),
                tier = Tier.fromValue(tier),
                score = score ?: 0,
                displayTier = displayTier?.let(Tier::fromValue) ?: Tier.fromValue(tier),
                provider = SocialProvider.fromValue(provider),
                interestCategories = interestCategories.toCategories(),
                onboardingCompleted = onboardingCompleted ?: true,
                accountStatus = AccountStatus.fromValue(accountStatus),
                lockInfo =
                    lockInfo?.let {
                        if (it.reason == null || it.unlockAt == null) null else LockInfo(it.reason, it.unlockAt)
                    },
            ),
    )
