package com.ruleup.verification.domain.usecase

import com.ruleup.verification.domain.entity.GeofenceTarget
import com.ruleup.verification.domain.entity.LocationPin
import com.ruleup.verification.domain.repository.GeofenceRegistrar
import javax.inject.Inject

/**
 * 지도 핀 확정 → 멤버 앵커 전체를 지오펜스로 등록(명세 §5, setup·my-location 앵커 최대 10개).
 * requestId 는 `"{challengeMemberId}#{index}"` 로 부여되며, GEOFENCE 이벤트의 anchorId 로 서버에 보고된다.
 * 변경 시 재등록도 [GeofenceRegistrar.bind] 가 멱등 처리한다(§5.4.3). 반경은 [GeofenceTarget] 범위로 클램프한다.
 */
class BindLocationUseCase
    @Inject
    constructor(
        private val geofenceRegistrar: GeofenceRegistrar,
    ) {
        suspend operator fun invoke(
            challengeMemberId: String,
            anchors: List<LocationPin>,
            dwellMinutes: Int,
        ) {
            val targets =
                anchors.mapIndexed { index, pin ->
                    GeofenceTarget(
                        requestId = "$challengeMemberId$REQUEST_ID_SEPARATOR$index",
                        lat = pin.lat,
                        lng = pin.lng,
                        radiusM = pin.radiusM.coerceIn(GeofenceTarget.MIN_RADIUS_M, GeofenceTarget.MAX_RADIUS_M),
                        dwellMinutes = dwellMinutes,
                    )
                }
            geofenceRegistrar.bind(requestIdPrefix = challengeMemberId, targets = targets)
        }

        companion object {
            const val REQUEST_ID_SEPARATOR = "#"
        }
    }
