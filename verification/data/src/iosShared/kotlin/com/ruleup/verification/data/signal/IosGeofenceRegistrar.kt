package com.ruleup.verification.data.signal

import com.ruleup.verification.domain.entity.GeofenceTarget
import com.ruleup.verification.domain.port.GeofenceRegistrar
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

/**
 * iOS no-op 지오펜스 등록기(명세 §0·§8: iOS 자동인증은 GPS 단독으로 축소, 지오펜스 POC 는 Android 우선).
 * 공통 ViewModel(지도 바인딩)이 [GeofenceRegistrar] 를 요구하므로 iOS 그래프를 완성하기 위한 스텁이다.
 */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class IosGeofenceRegistrar : GeofenceRegistrar {
    override suspend fun reconcile(targets: List<GeofenceTarget>) = Unit

    override suspend fun bind(target: GeofenceTarget) = Unit

    override suspend fun unbind(requestId: String) = Unit

    override suspend fun clear() = Unit
}
