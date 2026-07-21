package com.ruleup.challenge.domain.usecase

import com.ruleup.challenge.domain.entity.MemberRoleChange
import com.ruleup.challenge.domain.entity.RoleAction
import com.ruleup.challenge.domain.repository.ChallengeRepository
import javax.inject.Inject

/**
 * 공동 관리자 임명/해제 (명세: PATCH /challenges/{id}/members/{userId}/role).
 * 임명·타인 해제는 OWNER, 본인 DEMOTE 는 MANAGER 본인만.
 */
class ChangeMemberRoleUseCase
    @Inject
    constructor(
        private val challengeRepository: ChallengeRepository,
    ) {
        suspend operator fun invoke(
            challengeId: String,
            userId: String,
            action: RoleAction,
        ): MemberRoleChange = challengeRepository.changeMemberRole(challengeId, userId, action)
    }
