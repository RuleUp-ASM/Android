package com.ruleup.data.repository

import com.ruleup.data.api.ProfileApi
import com.ruleup.data.dto.request.UpdateProfileRequest
import com.ruleup.data.dto.response.toDomain
import com.ruleup.domain.model.InterestCategory
import com.ruleup.domain.model.Profile
import com.ruleup.domain.repository.ProfileRepository
import com.ruleup.network.dto.getOrThrow
import com.ruleup.network.dto.requireField
import com.ruleup.network.dto.throwOnError
import jakarta.inject.Inject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class ProfileRepositoryImpl
    @Inject
    constructor(
        private val profileApi: ProfileApi,
    ) : ProfileRepository {
        override suspend fun getMyProfile(): Profile = profileApi.getMyProfile().getOrThrow().toDomain()

        override suspend fun updateProfile(
            nickname: String?,
            interestCategories: List<InterestCategory>?,
            profileImageUrl: String?,
        ): Profile =
            profileApi
                .updateMyProfile(
                    request =
                        UpdateProfileRequest(
                            nickname = nickname,
                            interestCategories = interestCategories?.map { it.value },
                            profileImageUrl = profileImageUrl,
                        ),
                ).getOrThrow()
                .toDomain()

        override suspend fun uploadProfileImage(
            image: ByteArray,
            fileName: String,
        ): String {
            val body = image.toRequestBody("image/*".toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("image", fileName, body)
            return profileApi
                .uploadProfileImage(part)
                .getOrThrow()
                .profileImageUrl
                .requireField("profile_image_url")
        }

        override suspend fun deleteProfileImage() {
            profileApi.deleteProfileImage().throwOnError()
        }
    }
