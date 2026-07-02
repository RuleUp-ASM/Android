package com.ruleup.verification.domain.entity

/**
 * 내 인증 장소(앵커) 조회 결과(명세: GET /my-location). 위치 셋업/수정 재진입 시 지도에
 * 이전에 바인딩한 핀을 복원하기 위함.
 *
 * 바인딩된 앵커가 하나도 없으면 서버는 GEOFENCE_NOT_CONFIGURED(400) 로 실패하므로,
 * 성공 응답의 [anchors] 는 항상 1개 이상이다.
 */
data class MyLocation(
    // 바인딩된 앵커 목록(1개 이상)
    val anchors: List<LocationPin>,
    // 앵커 마지막 적용 시각(ISO-8601). 적용 이력 없으면 null
    val appliedFrom: String?,
)
