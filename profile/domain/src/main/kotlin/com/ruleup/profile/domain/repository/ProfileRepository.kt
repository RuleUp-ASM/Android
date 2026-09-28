package com.ruleup.profile.domain.repository

import com.ruleup.domain.entity.category.Category
import com.ruleup.profile.domain.entity.CategoryCatalog
import com.ruleup.profile.domain.entity.MemberProfile
import com.ruleup.profile.domain.entity.MyProfile
import com.ruleup.profile.domain.entity.NicknameCheck
import com.ruleup.profile.domain.entity.Profile

/** 계정 프로필 계약. */
interface ProfileRepository {
    /** 내 프로필 조회 (GET /api/v1/users/me). */
    suspend fun getMyProfile(): MyProfile

    /** 닉네임 형식/중복 검사. */
    suspend fun checkNickname(nickname: String): NicknameCheck

    /** 관심 카테고리 마스터 조회. */
    suspend fun getCategories(): CategoryCatalog

    /** 내 프로필 조회. */
    suspend fun getProfile(): Profile

    /** 프로필 수정. */
    suspend fun updateProfile(
        nickname: String? = null,
        interestCategories: List<Category>? = null,
    ): Profile

    /** 프로필 사진 업로드 후 URL 반환. */
    suspend fun uploadProfileImage(imageUri: String): String

    /** 프로필 사진 제거. */
    suspend fun deleteProfileImage()

    /** 타인 프로필 조회. */
    suspend fun getMemberProfile(userId: String): MemberProfile
}
