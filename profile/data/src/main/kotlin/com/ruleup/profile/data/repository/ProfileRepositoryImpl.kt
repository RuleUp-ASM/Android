package com.ruleup.profile.data.repository

import com.ruleup.domain.entity.category.Category
import com.ruleup.network.dto.getOrThrow
import com.ruleup.network.dto.requireField
import com.ruleup.network.dto.throwOnError
import com.ruleup.network.image.ImageReader
import com.ruleup.profile.data.api.ProfileApi
import com.ruleup.profile.data.dto.NicknameCheckRequest
import com.ruleup.profile.data.dto.UpdateProfileRequest
import com.ruleup.profile.data.dto.toDomain
import com.ruleup.profile.domain.entity.CategoryCatalog
import com.ruleup.profile.domain.entity.MyProfile
import com.ruleup.profile.domain.entity.NicknameCheck
import com.ruleup.profile.domain.entity.Profile
import com.ruleup.profile.domain.repository.ProfileRepository
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
        override suspend fun getMyProfile(): MyProfile = api.getMyProfile().getOrThrow().toDomain()

        override suspend fun checkNickname(nickname: String): NicknameCheck =
            api
                .checkNickname(NicknameCheckRequest(nickname = nickname))
                .getOrThrow()
                .toDomain()

        override suspend fun getCategories(): CategoryCatalog = api.getCategories().getOrThrow().toDomain()

        override suspend fun getProfile(): Profile = api.getProfile().getOrThrow().toDomain()

        override suspend fun updateProfile(
            nickname: String?,
            interestCategories: List<Category>?,
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
                .imageUrl
                .requireField("imageUrl")
        }

        override suspend fun deleteProfileImage() {
            api.deleteProfileImage().throwOnError()
        }
    }
