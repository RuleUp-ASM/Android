package com.ruleup.verification.domain.usecase

import com.ruleup.domain.token.TokenRepository
import com.ruleup.verification.domain.entity.GeofenceTarget
import com.ruleup.verification.domain.entity.LocationPin
import com.ruleup.verification.domain.repository.GeofenceRegister
import javax.inject.Inject

/** 지도 핀 확정 → 멤버 앵커 전체를 지오펜스로 등록. */
class BindLocationUseCase
    @Inject
    constructor(
        private val geofenceRegister: GeofenceRegister,
        private val tokenRepository: TokenRepository,
    ) {
        suspend operator fun invoke(
            challengeId: String,
            anchors: List<LocationPin>,
            radiusM: Float,
            dwellMinutes: Int,
        ) {
            val memberKey =
                tokenRepository
                    .getUserId()
                    ?.let { userId -> "$userId$REQUEST_ID_SEPARATOR$challengeId" }
                    ?: challengeId
            val targets =
                anchors.mapIndexed { index, pin ->
                    GeofenceTarget(
                        requestId = "$memberKey$REQUEST_ID_SEPARATOR$index",
                        lat = pin.lat,
                        lng = pin.lng,
                        radiusM = radiusM,
                        dwellMinutes = dwellMinutes,
                    )
                }
            geofenceRegister.bind(requestIdPrefix = memberKey, targets = targets)
        }

        companion object {
            const val REQUEST_ID_SEPARATOR = "#"
        }
    }
