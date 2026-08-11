package com.ruleup.challenge.domain.usecase

import com.ruleup.challenge.domain.entity.DraftResult
import com.ruleup.challenge.domain.repository.ExploreRepository
import javax.inject.Inject

/**
 * 템플릿 복제 (명세 POST /challenges/{id}/clone).
 *
 * 대상 방의 설정을 프리필한 초안을 만들어 **생성 확인 화면**으로 보낸다 — 응답 초안이 생성 모듈의 draft
 * 와 동일 스키마라 확인 화면·생성 API 를 그대로 재사용한다. 공개 그룹만 복제할 수 있다.
 */
class CloneChallengeUseCase
    @Inject
    constructor(
        private val exploreRepository: ExploreRepository,
    ) {
        suspend operator fun invoke(challengeId: String): DraftResult.Ok = exploreRepository.clone(challengeId)
    }
