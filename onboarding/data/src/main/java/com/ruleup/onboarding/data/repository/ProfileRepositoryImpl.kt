package com.ruleup.onboarding.data.repository

import android.content.Context
import androidx.core.net.toUri
import com.ruleup.entity.user.CategoryCatalog
import com.ruleup.entity.user.InterestCategory
import com.ruleup.entity.user.NicknameCheck
import com.ruleup.entity.user.Profile
import com.ruleup.network.dto.getOrThrow
import com.ruleup.network.dto.requireField
import com.ruleup.network.dto.throwOnError
import com.ruleup.onboarding.data.api.ProfileApi
import com.ruleup.onboarding.data.dto.NicknameCheckRequest
import com.ruleup.onboarding.data.dto.UpdateProfileRequest
import com.ruleup.onboarding.data.dto.toDomain
import com.ruleup.onboarding.domain.profile.ProfileRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class ProfileRepositoryImpl(
    private val api: ProfileApi,
    private val context: Context,
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
        val uri = imageUri.toUri()
        val resolver = context.contentResolver
        val bytes =
            withContext(Dispatchers.IO) {
                resolver.openInputStream(uri)?.use { it.readBytes() }
            } ?: throw IllegalArgumentException("이미지를 읽을 수 없습니다: $imageUri")
        val mimeType = resolver.getType(uri) ?: "image/*"
        val part =
            MultipartBody.Part.createFormData(
                name = "image",
                filename = "profile_image",
                body = bytes.toRequestBody(mimeType.toMediaTypeOrNull()),
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
