package com.ruleup.domain.repository

import com.ruleup.core.model.InterestCategory
import com.ruleup.domain.model.Profile

interface ProfileRepository {

    /** 내 프로필을 조회한다. */
    suspend fun getMyProfile(): Profile

    /**
     * 프로필을 수정한다. null 인 인자는 변경하지 않는다.
     */
    suspend fun updateProfile(
        nickname: String? = null,
        interestCategories: List<InterestCategory>? = null,
        profileImageUrl: String? = null,
    ): Profile

    /** 프로필 사진을 업로드하고 저장된 URL 을 반환한다. */
    suspend fun uploadProfileImage(image: ByteArray, fileName: String): String

    /** 프로필 사진을 제거한다(기본 이미지로 되돌림). */
    suspend fun deleteProfileImage()
}
