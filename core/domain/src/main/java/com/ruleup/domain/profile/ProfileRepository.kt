package com.ruleup.domain.profile

import com.ruleup.entity.user.CategoryCatalog
import com.ruleup.entity.user.InterestCategory
import com.ruleup.entity.user.NicknameCheck
import com.ruleup.entity.user.Profile

interface ProfileRepository {
    /** 닉네임 형식/중복 검사 (명세 4.6). */
    suspend fun checkNickname(nickname: String): NicknameCheck

    /** 관심 카테고리 마스터 조회 (명세 4.7). */
    suspend fun getCategories(): CategoryCatalog

    /** 내 프로필 조회 (명세 4.8). */
    suspend fun getProfile(): Profile

    /** 프로필 수정. 변경할 필드만 전달한다 (명세 4.9). */
    suspend fun updateProfile(
        nickname: String? = null,
        interestCategories: List<InterestCategory>? = null,
        profileImageUrl: String? = null,
    ): Profile

    /** 프로필 사진 업로드 후 URL 반환 (명세 4.10). */
    suspend fun uploadProfileImage(imageUri: String): String

    /** 프로필 사진 제거 (명세 4.11). */
    suspend fun deleteProfileImage()
}
