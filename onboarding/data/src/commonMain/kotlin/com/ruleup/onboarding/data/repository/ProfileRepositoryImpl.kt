package com.ruleup.onboarding.data.repository

import com.ruleup.entity.user.CategoryCatalog
import com.ruleup.entity.user.InterestCategory
import com.ruleup.entity.user.NicknameCheck
import com.ruleup.entity.user.Profile
import com.ruleup.network.dto.getOrThrow
import com.ruleup.network.dto.requireField
import com.ruleup.network.dto.throwOnError
import com.ruleup.network.image.ImageReader
import com.ruleup.onboarding.data.api.ProfileApi
import com.ruleup.onboarding.data.dto.NicknameCheckRequest
import com.ruleup.onboarding.data.dto.UpdateProfileRequest
import com.ruleup.onboarding.data.dto.toDomain
import com.ruleup.onboarding.domain.profile.ProfileRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class ProfileRepositoryImpl(
    private val api: ProfileApi,
    private val imageReader: ImageReader,
) : ProfileRepository {
    override suspend fun checkNickname(nickname: String): NicknameCheck =
        api
            .checkNickname(NicknameCheckRequest(nickname = nickname))
            .getOrThrow()
            .toDomain()

    override suspend fun getCategories(): CategoryCatalog = api.getCategories().getOrThrow().toDomain()

    override suspend fun getProfile(): Profile = api.getProfile().getOrThrow().toDomain()

    override suspend fun updateProfile(
        nickname: String?,
        interestCategories: List<InterestCategory>?,
        profileImageUrl: String?,
    ): Profile =
        api
            .updateProfile(
                UpdateProfileRequest(
                    nickname = nickname,
                    interestCategories = interestCategories?.map { it.value },
                    profileImageUrl = profileImageUrl,
                ),
            ).getOrThrow()
            .toDomain()

    override suspend fun uploadProfileImage(imageUri: String): String {
        val image = imageReader.read(imageUri)
        val content =
            MultiPartFormDataContent(
                formData {
                    append(
                        key = "image",
                        value = image.bytes,
                        headers =
                            Headers.build {
                                append(HttpHeaders.ContentType, image.mimeType)
                                append(HttpHeaders.ContentDisposition, "filename=\"profile_image\"")
                            },
                    )
                },
            )
        return api
            .uploadProfileImage(content)
            .getOrThrow()
            .profileImageUrl
            .requireField("profileImageUrl")
    }

    override suspend fun deleteProfileImage() {
        api.deleteProfileImage().throwOnError()
    }
}
