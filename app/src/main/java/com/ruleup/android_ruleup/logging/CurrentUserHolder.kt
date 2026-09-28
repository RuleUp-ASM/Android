package com.ruleup.android_ruleup.logging

import com.ruleup.logging.domain.BizUserSource
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/** 지금 로그인한 사용자 식별자를 들고 있는 홀더. */
@Singleton
class CurrentUserHolder
    @Inject
    constructor() : BizUserSource {
        private val userId = AtomicReference<String?>(null)

        override fun currentUserId(): String? = userId.get()

        fun setUser(id: String?) {
            userId.set(id)
        }
    }
