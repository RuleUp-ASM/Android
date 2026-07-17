package com.ruleup.profile.domain.usecase

import com.ruleup.domain.profile.ProfileRepository
import com.ruleup.entity.user.CategoryCatalog
import com.ruleup.entity.user.InterestCategory
import com.ruleup.entity.user.NicknameCheck
import com.ruleup.entity.user.Profile
import javax.inject.Inject

// 프로필 편집(마이 → 재편집) 유스케이스 모음. 계약은 core 의 ProfileRepository(구현 :onboarding:data) 재사용 —
// 서버 파이프라인(닉네임 30일 제한·LLM 검수·이미지 모더레이션)은 이미 동작 중이라 조회/수정 호출만 한다.

/** 내 프로필 조회 (명세 4.8) — 편집 폼 프리필용. */
class GetProfileUseCase
    @Inject
    constructor(
        private val profileRepository: ProfileRepository,
    ) {
        suspend operator fun invoke(): Profile = profileRepository.getProfile()
    }

/** 관심 카테고리 마스터 조회 (명세 4.7) — maxSelectable(1~6) 확인용. */
class GetCategoryCatalogUseCase
    @Inject
    constructor(
        private val profileRepository: ProfileRepository,
    ) {
        suspend operator fun invoke(): CategoryCatalog = profileRepository.getCategories()
    }

/** 닉네임 형식/중복 검사 (명세 4.6) — 저장 전 선검사. */
class CheckNicknameUseCase
    @Inject
    constructor(
        private val profileRepository: ProfileRepository,
    ) {
        suspend operator fun invoke(nickname: String): NicknameCheck = profileRepository.checkNickname(nickname)
    }

/** 프로필 수정 (명세 4.9) — 변경한 필드만 전달한다. */
class UpdateProfileUseCase
    @Inject
    constructor(
        private val profileRepository: ProfileRepository,
    ) {
        suspend operator fun invoke(
            nickname: String? = null,
            interestCategories: List<InterestCategory>? = null,
        ): Profile = profileRepository.updateProfile(nickname = nickname, interestCategories = interestCategories)
    }

/** 프로필 사진 업로드 (명세 4.10) — 업로드 후 서버 URL 반환. */
class UploadProfileImageUseCase
    @Inject
    constructor(
        private val profileRepository: ProfileRepository,
    ) {
        suspend operator fun invoke(imageUri: String): String = profileRepository.uploadProfileImage(imageUri)
    }

/** 프로필 사진 제거 (명세 4.11). */
class DeleteProfileImageUseCase
    @Inject
    constructor(
        private val profileRepository: ProfileRepository,
    ) {
        suspend operator fun invoke() = profileRepository.deleteProfileImage()
    }
