package com.ruleup.verification.domain.usecase

import com.ruleup.verification.domain.entity.Place
import com.ruleup.verification.domain.port.VerificationRepository
import javax.inject.Inject

/**
 * 지도 탭 지점 역지오코딩(명세 §5.3 보강). 탭한 좌표의 이름/주소를 채워 확인 카드에 보여준다.
 * 결과가 없으면 null — 화면은 좌표만으로도 선택을 진행한다.
 */
class ReverseGeocodeUseCase
    @Inject
    constructor(
        private val verificationRepository: VerificationRepository,
    ) {
        suspend operator fun invoke(
            lat: Double,
            lng: Double,
        ): Place? = verificationRepository.reverseGeocode(lat, lng)
    }
