package com.ruleup.onboarding.data.profile.repository

import com.ruleup.entity.user.CategoryCatalog
import com.ruleup.entity.user.InterestCategory
import com.ruleup.entity.user.NicknameCheck
import com.ruleup.entity.user.Profile
import com.ruleup.network.dto.getOrThrow
import com.ruleup.network.dto.requireField
import com.ruleup.network.dto.throwOnError
import com.ruleup.network.image.ImageReader
import com.ruleup.onboarding.data.profile.api.ProfileApi
import com.ruleup.onboarding.data.profile.dto.NicknameCheckRequest
import com.ruleup.onboarding.data.profile.dto.UpdateProfileRequest
import com.ruleup.onboarding.data.profile.dto.toDomain
import com.ruleup.onboarding.domain.profile.repository.ProfileRepository
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

class ProfileRepositoryImpl
    @Inject
    constructor(
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
            val part =
                MultipartBody.Part.createFormData(
                    name = "image",
                    filename = "profile_image",
                    body = image.bytes.toRequestBody(image.mimeType.toMediaType()),
                )
            return api
                .uploadProfileImage(part)
                .getOrThrow()
                .profileImageUrl
                .requireField("profileImageUrl")
        }

        override suspend fun deleteProfileImage() {
            api.deleteProfileImage().throwOnError()
        }
    }
