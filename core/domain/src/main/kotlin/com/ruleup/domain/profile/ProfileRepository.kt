package com.ruleup.domain.profile

import com.ruleup.entity.user.CategoryCatalog
import com.ruleup.entity.user.InterestCategory
import com.ruleup.entity.user.NicknameCheck
import com.ruleup.entity.user.Profile

/**
 * 프로필 계약 (온보딩 명세 4.6~4.11, 구현은 :onboarding:data).
 * 온보딩(최초 설정)과 마이(재편집)가 함께 쓰는 횡단 계약이라 core 에 둔다 (feature 간 직접 의존 금지).
 */
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

    /**
     * 가입 기본정보 입력 (명세 PUT /onboarding/me). 추천 세그먼트용 생년월일·성별(둘 다 선택).
     * 원치 않는 값은 null 로 전달(전송 생략). 가입 완료 후(토큰 확보) 호출한다.
     */
    suspend fun updateOnboardingInfo(
        birthDate: String? = null,
        gender: String? = null,
    )
}
