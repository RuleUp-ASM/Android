package com.ruleup.onboarding.domain.auth.usecase

import com.ruleup.onboarding.domain.profile.ProfileRepository
import javax.inject.Inject

/**
 * 닉네임 사용 가능 여부 확인 (명세 4.6).
 *
 * 서버에 형식/중복 검사를 요청하고 사용 가능하면 true 를 반환한다.
 * (형식 1차 검증은 호출부에서 먼저 거르므로 여기선 중복까지 통과한 최종 가용 여부를 본다.)
 * 실패 사유 세부값이 필요하면 [ProfileRepository.checkNickname] 를 직접 사용한다.
 */
class CheckNicknameUseCase
    @Inject
    constructor(
        private val profileRepository: ProfileRepository,
    ) {
        suspend operator fun invoke(nickname: String): Boolean = profileRepository.checkNickname(nickname).available
    }
