package com.ruleup.profile.domain.usecase

import com.ruleup.profile.domain.entity.FriendInvitation
import com.ruleup.profile.domain.repository.MyPageRepository
import javax.inject.Inject

/**
 * 친구 초대 정보 조회 (명세: GET /me/invitation).
 * 코드/링크는 유저당 1개 멱등 생성 — 초대 전달은 사용자 본인 채널(카카오톡·복사·QR)로만 한다.
 */
class GetFriendInvitationUseCase
    @Inject
    constructor(
        private val myPageRepository: MyPageRepository,
    ) {
        suspend operator fun invoke(): FriendInvitation = myPageRepository.getInvitation()
    }
