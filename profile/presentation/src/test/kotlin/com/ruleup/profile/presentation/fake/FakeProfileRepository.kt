package com.ruleup.profile.presentation.fake

import com.ruleup.domain.entity.category.Category
import com.ruleup.profile.domain.entity.CategoryCatalog
import com.ruleup.profile.domain.entity.MyProfile
import com.ruleup.profile.domain.entity.NicknameCheck
import com.ruleup.profile.domain.entity.Profile
import com.ruleup.profile.domain.repository.ProfileRepository

/**
 * 테스트용 [ProfileRepository]. 준비하지 않은 메서드는 호출되면 실패한다 —
 * 저장 화면이 의도치 않은 요청을 보내도 조용히 지나가지 않게 하려는 것이다.
 */
class FakeProfileRepository(
    private val profile: (() -> Profile)? = null,
    private val categories: (() -> CategoryCatalog)? = null,
    private val checkNickname: ((String) -> NicknameCheck)? = null,
    private val updateProfile: (() -> Profile)? = null,
    private val uploadImage: ((String) -> String)? = null,
) : ProfileRepository {
    /** 어떤 메서드가 불렸는지. "안 보냈다"도 계약이라 호출 자체를 남긴다. */
    val calls = mutableListOf<String>()

    /** 마지막으로 저장 요청한 값. 바뀐 것만 보내는지 확인할 때 쓴다. */
    var lastUpdatedNickname: String? = null
        private set
    var lastUpdatedCategories: List<Category>? = null
        private set

    override suspend fun getProfile(): Profile {
        calls += "getProfile"
        return requireNotNull(profile) { "getProfile 을 준비하지 않았다" }()
    }

    override suspend fun getCategories(): CategoryCatalog {
        calls += "getCategories"
        return requireNotNull(categories) { "getCategories 를 준비하지 않았다" }()
    }

    override suspend fun checkNickname(nickname: String): NicknameCheck {
        calls += "checkNickname"
        return requireNotNull(checkNickname) { "checkNickname 을 준비하지 않았다" }(nickname)
    }

    override suspend fun updateProfile(
        nickname: String?,
        interestCategories: List<Category>?,
        profileImageUrl: String?,
    ): Profile {
        calls += "updateProfile"
        lastUpdatedNickname = nickname
        lastUpdatedCategories = interestCategories
        return requireNotNull(updateProfile) { "updateProfile 을 준비하지 않았다" }()
    }

    override suspend fun uploadProfileImage(imageUri: String): String {
        calls += "uploadProfileImage"
        return requireNotNull(uploadImage) { "uploadProfileImage 를 준비하지 않았다" }(imageUri)
    }

    override suspend fun deleteProfileImage() {
        calls += "deleteProfileImage"
    }

    override suspend fun getMyProfile(): MyProfile = throw NotImplementedError()
}
