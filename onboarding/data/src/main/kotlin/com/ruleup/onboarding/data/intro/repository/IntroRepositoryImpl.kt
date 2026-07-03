package com.ruleup.onboarding.data.intro.repository

import android.content.Context
import android.os.Build
import com.ruleup.network.dto.getOrThrow
import com.ruleup.onboarding.data.intro.api.IntroApi
import com.ruleup.onboarding.data.intro.dto.toDomain
import com.ruleup.onboarding.domain.entity.AppVersionGate
import com.ruleup.onboarding.domain.intro.repository.IntroRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class IntroRepositoryImpl
    @Inject
    constructor(
        private val api: IntroApi,
        @ApplicationContext private val context: Context,
    ) : IntroRepository {
        override suspend fun getVersionGate(): AppVersionGate {
            val (versionCode, versionName) = context.appVersion()
            return api
                .getIntro(
                    appVersionCode = versionCode,
                    platform = PLATFORM_ANDROID,
                    versionName = versionName,
                ).getOrThrow()
                .toDomain()
        }

        private companion object {
            const val PLATFORM_ANDROID = "Android"
        }
    }

/** 설치된 앱의 (versionCode, versionName). versionName 미설정 시 빈 문자열. */
private fun Context.appVersion(): Pair<Int, String> {
    val info = packageManager.getPackageInfo(packageName, 0)
    val code =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.longVersionCode.toInt()
        } else {
            @Suppress("DEPRECATION")
            info.versionCode
        }
    return code to (info.versionName ?: "")
}
