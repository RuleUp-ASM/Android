package com.ruleup.android_ruleup.logging

import com.ruleup.logging.domain.BizUserSource
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 지금 로그인한 사용자 식별자를 들고 있는 홀더. [App] 이 `TokenRepository.userId` 를 구독해 채운다.
 *
 * 흐름(Flow)을 기록기에 그대로 넘기지 않는 것은 [BizUserSource] 가 이벤트마다 **동기로** 불리기
 * 때문이다 — 거기서 코루틴을 돌리면 기록하는 화면이 전송 준비를 기다리게 된다.
 *
 * 서버 userId 만 담는다. 이메일·이름·전화번호를 넣지 않는다.
 */
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
