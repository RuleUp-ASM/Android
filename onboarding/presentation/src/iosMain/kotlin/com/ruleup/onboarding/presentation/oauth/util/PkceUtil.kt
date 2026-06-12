package com.ruleup.onboarding.presentation.oauth.util

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH
import platform.Security.SecRandomCopyBytes
import platform.Security.kSecRandomDefault
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * iOS PKCE 유틸(웹 OAuth 용). androidMain 의 [PkceUtil] 에 대응하나, SDK 가 내부 계산하던 S256 challenge 를
 * 웹 플로우에서 직접 만들어야 하므로 [codeChallengeS256] 도 제공한다.
 */
@OptIn(ExperimentalForeignApi::class, ExperimentalEncodingApi::class)
object PkceUtil {
    private val base64Url = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT)

    fun generateCodeVerifier(): String {
        val bytes = ByteArray(32)
        bytes.usePinned { pinned ->
            SecRandomCopyBytes(kSecRandomDefault, bytes.size.convert(), pinned.addressOf(0))
        }
        return base64Url.encode(bytes)
    }

    fun codeChallengeS256(verifier: String): String {
        val input = verifier.encodeToByteArray()
        val digest = ByteArray(CC_SHA256_DIGEST_LENGTH)
        input.usePinned { inPin ->
            digest.usePinned { outPin ->
                CC_SHA256(
                    inPin.addressOf(0),
                    input.size.convert(),
                    outPin.addressOf(0).reinterpret(),
                )
            }
        }
        return base64Url.encode(digest)
    }
}
