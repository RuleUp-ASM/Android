package com.ruleup.verification.domain.usecase

import com.ruleup.verification.domain.entity.Place
import com.ruleup.verification.domain.port.VerificationRepository
import dev.zacsweers.metro.Inject

/**
 * 장소 검색(명세 §5.2·§11.7). 셋업/수정 시 앵커를 채우는 두 모드의 입력을 만든다:
 * - MANUAL: 키워드만 — 본인이 갈 지점을 직접 다중 선택.
 * - NEARBY_BRAND: 키워드 + 중심좌표([lat]/[lng]) + 반경([radiusM]) — 반경 내 동일 브랜드 일괄.
 */
class SearchPlacesUseCase
    @Inject
    constructor(
        private val verificationRepository: VerificationRepository,
    ) {
        suspend operator fun invoke(
            query: String,
            lat: Double? = null,
            lng: Double? = null,
            radiusM: Int? = null,
        ): List<Place> = verificationRepository.searchPlaces(query, lat, lng, radiusM)
    }
