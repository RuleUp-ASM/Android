package com.ruleup.verification.domain.usecase

import com.ruleup.verification.domain.entity.GeofenceTarget
import com.ruleup.verification.domain.port.GeofenceRegistrar
import javax.inject.Inject

/**
 * 지도 핀 확정 → 멤버 좌표 지오펜스 등록(명세 §5). SHARED/PER_MEMBER 모두 하나의 [GeofenceTarget] 로
 * 귀결되며(차이는 누가 편집하느냐 = 서버 권한), 변경 시 재등록도 [bind] 로 멱등 처리된다(§5.4.3).
 * 반경은 50~1000m 로 클램프한다(§5.3).
 */
class BindLocationUseCase
    @Inject
    constructor(
        private val geofenceRegistrar: GeofenceRegistrar,
    ) {
        suspend operator fun invoke(
            challengeMemberId: String,
            lat: Double,
            lng: Double,
            radiusM: Float,
            dwellMinutes: Int,
        ) {
            val target =
                GeofenceTarget(
                    requestId = challengeMemberId,
                    lat = lat,
                    lng = lng,
                    radiusM = radiusM.coerceIn(GeofenceTarget.MIN_RADIUS_M, GeofenceTarget.MAX_RADIUS_M),
                    dwellMinutes = dwellMinutes,
                )
            geofenceRegistrar.bind(target)
        }
    }
