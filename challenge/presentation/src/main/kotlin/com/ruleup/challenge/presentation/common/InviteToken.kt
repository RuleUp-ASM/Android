package com.ruleup.challenge.presentation.common

import java.net.URI

/** 초대 주소(`…/w/{token}`·`…/c/{token}`)의 두 번째 경로가 토큰이다. 형식이 다르면 null. */
internal fun inviteToken(inviteUrl: String): String? {
    val path = runCatching { URI(inviteUrl).path }.getOrNull() ?: return null
    val segments = path.split('/').filter { it.isNotBlank() }
    return segments.takeIf { it.size >= 2 }?.get(1)
}
