package com.ruleup.verification.domain.usecase

import com.ruleup.domain.token.TokenRepository
import com.ruleup.verification.domain.entity.GeofenceTarget
import com.ruleup.verification.domain.entity.LocationPin
import com.ruleup.verification.domain.repository.GeofenceRegistrar
import javax.inject.Inject

/**
 * 지도 핀 확정 → 멤버 앵커 전체를 지오펜스로 등록(명세 §5, setup·my-location 앵커 최대 10개).
 * requestId 는 `"{userId}#{challengeId}#{index}"` 로 부여되며, GEOFENCE 이벤트의 anchorId 로 서버에 보고된다.
 * `(userId, challengeId)` 는 멤버 자연키(`uq_member(challenge_id, user_id)`, 재참여는 status 갱신)라
 * 멤버 단위 귀속이 보장되고, 같은 기기에서 계정을 전환해도 로컬 키가 섞이지 않는다.
 * userId 가 아직 저장되지 않은 세션(저장 도입 전 자동로그인)은 기존 관례대로 challengeId 접두로 폴백한다.
 * 변경 시 재등록도 [GeofenceRegistrar.bind] 가 멱등 처리한다(§5.4.3). 반경은 [GeofenceTarget] 범위로 클램프한다.
 */
class BindLocationUseCase
    @Inject
    constructor(
        private val geofenceRegistrar: GeofenceRegistrar,
        private val tokenRepository: TokenRepository,
    ) {
        suspend operator fun invoke(
            challengeId: String,
            anchors: List<LocationPin>,
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
                        radiusM = pin.radiusM.coerceIn(GeofenceTarget.MIN_RADIUS_M, GeofenceTarget.MAX_RADIUS_M),
                        dwellMinutes = dwellMinutes,
                    )
                }
            geofenceRegistrar.bind(requestIdPrefix = memberKey, targets = targets)
        }

        companion object {
            const val REQUEST_ID_SEPARATOR = "#"
        }
    }
