package com.ruleup.challenge.domain.usecase

import com.ruleup.domain.token.TokenRepository
import javax.inject.Inject

/**
 * 현재 로그인 사용자 ID 조회 (세션 저장분). 멤버 목록에서 "내 행"을 식별해
 * 관리자 본인 해제(self-DEMOTE) 등 본인 한정 액션을 노출하는 데 쓴다.
 */
class GetCurrentUserIdUseCase
    @Inject
    constructor(
        private val tokenRepository: TokenRepository,
    ) {
        suspend operator fun invoke(): String? = tokenRepository.getUserId()
    }
