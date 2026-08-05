package com.ruleup.onboarding.domain.auth.usecase

import com.ruleup.profile.domain.entity.NicknameCheck
import com.ruleup.profile.domain.repository.ProfileRepository
import javax.inject.Inject

/**
 * 닉네임 사용 가능 여부 확인(POST /nicknames/check).
 *
 * 결과를 그대로 돌려준다. 예전엔 `available` 만 뽑아 Boolean 으로 줄였는데, 그러면 화면이 "왜
 * 안 되는지"를 말할 수 없다 — 형식 위반·중복·해제 후 1주 잠금은 안내 문구가 서로 다르고, 잠금은
 * 언제부터 쓸 수 있는지(`availableAt`)까지 보여줘야 한다.
 *
 * 형식 위반도 200 으로 온다(`valid=false, reason=FORMAT`). 실시간 확인 UX 에서 에러 봉투 분기를
 * 없애려는 서버 결정이라, 호출부는 예외가 아니라 결과를 보고 판단한다.
 */
class CheckNicknameUseCase
    @Inject
    constructor(
        private val profileRepository: ProfileRepository,
    ) {
        suspend operator fun invoke(nickname: String): NicknameCheck = profileRepository.checkNickname(nickname)
    }
