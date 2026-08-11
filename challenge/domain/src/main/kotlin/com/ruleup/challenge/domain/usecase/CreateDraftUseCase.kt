package com.ruleup.challenge.domain.usecase

import com.ruleup.challenge.domain.entity.DraftResult
import com.ruleup.challenge.domain.repository.ChallengeRepository
import javax.inject.Inject

/**
 * 루틴 설명으로 초안을 만든다 (명세 POST /challenges/draft — 경로 B).
 *
 * 길이 위반은 서버까지 보내지 않고 여기서 막는다 — 실시간 입력 UX 에서 왕복을 없애기 위해서다.
 * 서버도 같은 범위를 재검증한다(400 `ROUTINE_DESCRIPTION_REQUIRED` / `_TOO_LONG`).
 *
 * 반환이 [DraftResult.Fallback] 이어도 **실패가 아니다** — 호출자는 입력을 지우지 않고 안내만 띄운다.
 */
class CreateDraftUseCase
    @Inject
    constructor(
        private val challengeRepository: ChallengeRepository,
    ) {
        suspend operator fun invoke(description: String): DraftResult {
            val trimmed = description.trim()
            require(trimmed.isNotEmpty()) { "루틴 설명을 입력해 주세요." }
            require(trimmed.length <= MAX_DESCRIPTION_LENGTH) { "루틴 설명은 ${MAX_DESCRIPTION_LENGTH}자까지예요." }
            return challengeRepository.createDraft(trimmed)
        }

        companion object {
            const val MAX_DESCRIPTION_LENGTH = 200
        }
    }
