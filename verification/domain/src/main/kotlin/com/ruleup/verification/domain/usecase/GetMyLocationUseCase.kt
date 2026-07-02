package com.ruleup.verification.domain.usecase

import com.ruleup.verification.domain.entity.MyLocation
import com.ruleup.verification.domain.port.VerificationRepository
import javax.inject.Inject

/**
 * 내 인증 장소(앵커) 조회(명세: GET /my-location). 앵커 등록 화면 진입 시 호출해 등록 여부를 판별한다.
 * 미등록이면 [null] 을 돌려주며, 호출자는 이때만 등록을 진행한다(이미 등록돼 있으면 재등록하지 않는다).
 */
class GetMyLocationUseCase
    @Inject
    constructor(
        private val verificationRepository: VerificationRepository,
    ) {
        suspend operator fun invoke(challengeId: String): MyLocation? = verificationRepository.getMyLocation(challengeId)
    }
