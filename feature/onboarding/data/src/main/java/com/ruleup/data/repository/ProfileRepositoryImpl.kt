package com.ruleup.data.repository

import android.content.Context
import androidx.core.net.toUri
import com.ruleup.core.model.InterestCategory
import com.ruleup.data.api.ProfileApi
import com.ruleup.data.dto.request.UpdateProfileRequest
import com.ruleup.data.dto.response.toDomain
import com.ruleup.data.util.resolveImage
import com.ruleup.domain.model.Profile
import com.ruleup.domain.repository.ProfileRepository
import com.ruleup.network.dto.getOrThrow
import com.ruleup.network.dto.requireField
import com.ruleup.network.dto.throwOnError
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

class ProfileRepositoryImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
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

        override suspend fun uploadProfileImage(imageUri: String): String {
            val (image, fileName) = context.resolveImage(imageUri.toUri())

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
