package com.ruleup.profile.domain.usecase

import com.ruleup.profile.domain.entity.MyHome
import com.ruleup.profile.domain.repository.MyPageRepository
import javax.inject.Inject

/**
 * 마이 홈 일괄 조회 (명세: GET /me/home).
 * 온도·카운트(완주·진행 중·그룹)·프로필을 한 번에 받아 마이 탭 메인을 렌더링한다.
 */
class GetMyHomeUseCase
    @Inject
    constructor(
        private val myPageRepository: MyPageRepository,
    ) {
        suspend operator fun invoke(): MyHome = myPageRepository.getHome()
    }
