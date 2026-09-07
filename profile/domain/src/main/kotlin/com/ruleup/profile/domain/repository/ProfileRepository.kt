package com.ruleup.profile.domain.repository

import com.ruleup.domain.entity.category.Category
import com.ruleup.profile.domain.entity.CategoryCatalog
import com.ruleup.profile.domain.entity.MyProfile
import com.ruleup.profile.domain.entity.NicknameCheck
import com.ruleup.profile.domain.entity.Profile

/**
 * 계정 프로필 계약 (명세 4.6~4.11). 구현은 :profile:data.
 * 계정 정보의 소유자가 profile 이라 온보딩이 이 계약을 빌려 쓴다(:onboarding:domain → :profile:domain).
 */
interface ProfileRepository {
    /**
     * 내 프로필 조회 (GET /api/v1/users/me). 로그인 응답에 없는 생일·성별·약관 동의가 여기 있다.
     * 매너 온도·닉네임 변경 이력은 아직 이 응답에 없어 레거시 [getProfile] 과 공존한다.
     */
    suspend fun getMyProfile(): MyProfile

    /** 닉네임 형식/중복 검사 (명세 4.6). */
    suspend fun checkNickname(nickname: String): NicknameCheck

    /** 관심 카테고리 마스터 조회 (명세 4.7). */
    suspend fun getCategories(): CategoryCatalog

    /** 내 프로필 조회 (명세 4.8). */
    suspend fun getProfile(): Profile

    /** 프로필 수정. 변경할 필드만 전달한다 (명세 4.9). */
    suspend fun updateProfile(
        nickname: String? = null,
        interestCategories: List<Category>? = null,
        profileImageUrl: String? = null,
    ): Profile

    /** 프로필 사진 업로드 후 URL 반환 (명세 4.10). */
    suspend fun uploadProfileImage(imageUri: String): String

    /** 프로필 사진 제거 (명세 4.11). */
    suspend fun deleteProfileImage()
}
