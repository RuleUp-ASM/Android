package com.ruleup.onboarding.data.auth.repository

import android.content.Context
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.ruleup.onboarding.domain.auth.repository.SignupInviteStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.net.URI
import java.net.URLDecoder
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/** 설치·링크 진입 시 받은 초대 보관. */
@Singleton
class SignupInviteStoreImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : SignupInviteStore {
        private val preferences = context.getSharedPreferences("signup_invite", Context.MODE_PRIVATE)
        private val mutex = Mutex()

        override fun capture(link: String) {
            validInviteLink(link)?.let { preferences.edit().putString("link", it).apply() }
        }

        override suspend fun currentLink(): String? =
            mutex.withLock {
                preferences.getString("link", null)?.let { return@withLock it }
                if (preferences.getBoolean("referrerRead", false)) return@withLock null
                val referrer = readReferrer() ?: return@withLock null
                val link = inviteLinkFromReferrer(referrer)
                if (preferences.getString("link", null) == null && link != null) capture(link)
                preferences.edit().putBoolean("referrerRead", true).apply()
                preferences.getString("link", null)
            }

        override fun clear() {
            preferences
                .edit()
                .remove("link")
                .putBoolean("referrerRead", true)
                .apply()
        }

        private suspend fun readReferrer(): String? {
            val client = InstallReferrerClient.newBuilder(context).build()
            return try {
                withTimeoutOrNull(3000) {
                    suspendCancellableCoroutine { continuation ->
                        client.startConnection(
                            object : InstallReferrerStateListener {
                                override fun onInstallReferrerSetupFinished(code: Int) {
                                    val referrer =
                                        if (code == InstallReferrerClient.InstallReferrerResponse.OK) {
                                            runCatching { client.installReferrer.installReferrer }.getOrNull()
                                        } else {
                                            null
                                        }
                                    if (continuation.isActive) continuation.resume(referrer)
                                }

                                override fun onInstallReferrerServiceDisconnected() {
                                    if (continuation.isActive) continuation.resume(null)
                                }
                            },
                        )
                    }
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            } finally {
                client.endConnection()
            }
        }
    }

internal fun inviteLinkFromReferrer(raw: String): String? {
    val values =
        raw
            .split('&')
            .mapNotNull { part ->
                val pair = part.split('=', limit = 2)
                if (pair.size != 2) {
                    null
                } else {
                    runCatching {
                        URLDecoder.decode(pair[0], "UTF-8") to URLDecoder.decode(pair[1], "UTF-8")
                    }.getOrNull()
                }
            }.toMap()
    return validInviteLink(values["ruleup_invite_url"] ?: values["url"].orEmpty())
}

internal fun validInviteLink(raw: String): String? =
    runCatching {
        val uri = URI(raw)
        val segments =
            uri.path
                .orEmpty()
                .trim('/')
                .split('/')
        val valid =
            when {
                uri.scheme == "https" && uri.host == "android.ruleup.co.kr" && uri.userInfo == null ->
                    segments.size == 2 && segments[0] in setOf("inv", "c", "w") && segments[1].isNotBlank()
                uri.scheme == "ruleup" -> uri.host in setOf("inv", "c", "w") && segments.size == 1 && segments[0].isNotBlank()
                else -> false
            }
        raw.takeIf { valid }
    }.getOrNull()
