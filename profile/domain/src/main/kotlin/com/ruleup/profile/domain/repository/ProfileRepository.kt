package com.ruleup.profile.domain.repository

import com.ruleup.domain.entity.category.Category
import com.ruleup.profile.domain.entity.CategoryCatalog
import com.ruleup.profile.domain.entity.MyProfile
import com.ruleup.profile.domain.entity.NicknameCheck
import com.ruleup.profile.domain.entity.Profile

/**
 * 계정 프로필 계약 (명세 4.6~4.11). 구현은 :profile:data.
 *
 * 계정 정보의 소유자는 profile 이다. 온보딩은 최초 설정을 하고 지나가는 단계라 이 계약을
 * 빌려 쓴다(:onboarding:domain → :profile:domain).
 */
interface ProfileRepository {
    /**
     * 내 프로필 조회 (GET /api/v1/users/me).
     *
     * 로그인 응답의 `user` 는 홈 진입용 최소 정보라 생일·성별·약관 동의가 없다. 앱 재시작이나
     * 프로필 편집 후 최신 상태가 필요할 때 쓴다.
     *
     * 레거시 [getProfile] 과 공존한다 — 그쪽은 아직 구 엔드포인트(`GET /v1/profile`)를 보고 있고
     * 매너 온도·닉네임 변경 이력처럼 새 응답에 없는 필드를 마이페이지가 쓰고 있다. 마이페이지
     * 계약 정합화에서 하나로 합친다.
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
