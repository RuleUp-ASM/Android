package com.ruleup.verification.domain.setup

import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VerificationSetupCompletion
    @Inject
    constructor() {
        private val completed = ConcurrentHashMap.newKeySet<String>()

        fun record(challengeId: String) {
            completed.add(challengeId)
        }

        fun consume(challengeId: String): Boolean = completed.remove(challengeId)
    }
