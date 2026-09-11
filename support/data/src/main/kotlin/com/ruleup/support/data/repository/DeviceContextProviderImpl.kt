package com.ruleup.support.data.repository

import android.content.Context
import android.os.Build
import com.ruleup.support.domain.entity.InquiryDeviceContext
import com.ruleup.support.domain.repository.DeviceContextProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * 접수에 자동으로 붙는 진단 정보 채집 (명세 POST /inquiries 의 자동 첨부 4종).
 *
 * **어떤 항목도 접수를 막지 않는다** — 읽기에 실패하면 그 항목만 null 로 두고 나머지를 보낸다.
 */
class DeviceContextProviderImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : DeviceContextProvider {
        override suspend fun capture(): InquiryDeviceContext =
            InquiryDeviceContext(
                appVersion = appVersion(),
                osVersion = "Android ${Build.VERSION.RELEASE.orEmpty()}".trim(),
                deviceModel = Build.MODEL,
                // 최근 오류 로그 ID 를 앱이 보관하는 자리가 아직 없다. Crashlytics 세션 id 를 꺼내
                // 실으려면 :app 이 가진 인스턴스를 포트로 내려야 해서 이번 범위에서는 비워 둔다.
                errorLogId = null,
            )

        private fun appVersion(): String? =
            try {
                context.packageManager
                    .getPackageInfo(context.packageName, 0)
                    .versionName
            } catch (e: Exception) {
                null
            }
    }
