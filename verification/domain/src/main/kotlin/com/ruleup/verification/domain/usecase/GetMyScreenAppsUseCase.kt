package com.ruleup.verification.domain.usecase

import com.ruleup.verification.domain.entity.MyScreenApps
import com.ruleup.verification.domain.repository.VerificationRepository
import javax.inject.Inject

/**
 * 내 스크린타임 대상 앱 조회(명세: GET /my-screen-apps). 앱 셋업/수정 화면 재진입 시 이전 선택을 복원한다.
 * 미설정이면 [null] (등록 안 됨). 그 외 실패는 예외로 전파된다.
 */
class GetMyScreenAppsUseCase
    @Inject
    constructor(
        private val verificationRepository: VerificationRepository,
    ) {
        suspend operator fun invoke(challengeId: String): MyScreenApps? = verificationRepository.getMyScreenApps(challengeId)
    }
