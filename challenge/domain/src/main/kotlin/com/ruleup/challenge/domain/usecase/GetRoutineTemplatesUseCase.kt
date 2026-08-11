package com.ruleup.challenge.domain.usecase

import com.ruleup.challenge.domain.entity.RoutineTemplate
import com.ruleup.challenge.domain.repository.ChallengeRepository
import javax.inject.Inject

/**
 * 생성 화면 추천 루틴 3개 (명세 GET /challenges/recommendations).
 *
 * 서버가 항상 3개를 보장하므로 개수 파라미터가 없다. 실패해도 설명 입력 경로는 계속 쓸 수 있어야 하므로,
 * 호출자는 이 실패로 화면 전체를 에러로 만들지 않는다.
 */
class GetRoutineTemplatesUseCase
    @Inject
    constructor(
        private val challengeRepository: ChallengeRepository,
    ) {
        suspend operator fun invoke(): List<RoutineTemplate> = challengeRepository.getRoutineTemplates()
    }
