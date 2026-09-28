package com.ruleup.challenge.presentation.create

import com.ruleup.challenge.domain.entity.DraftResult
import javax.inject.Inject
import javax.inject.Singleton

/** 복제한 초안 전달. */
@Singleton
class PendingChallengeDraft
    @Inject
    constructor() {
        private var draft: DraftResult.Ok? = null

        fun put(value: DraftResult.Ok) {
            draft = value
        }

        fun consume(): DraftResult.Ok? = draft.also { draft = null }
    }
