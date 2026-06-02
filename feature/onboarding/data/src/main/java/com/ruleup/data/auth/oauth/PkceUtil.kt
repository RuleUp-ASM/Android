package com.ruleup.data.auth.oauth

import android.util.Base64
import java.security.SecureRandom

object PkceUtil {
    fun generateCodeVerifier(): String {
        val bytes = ByteArray(32).also { SecureRandom().nextBytes(it) }
        return Base64.encodeToString(
            bytes,
            Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP,
        )
    }
}
