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
    // 서버 설정 인증 반경(m) — 지도 원 표시·지오펜스 등록이 이 값을 쓴다
    val serverRadiusM: Float? = null,
    // 이번 달에 앵커를 바꿀 수 있는지(월 1회). 모르면 false — 못 바꾸는데 열어 두면 눌렀다 429 를 본다
    val changeAvailable: Boolean = false,
    // 다음 변경 가능 시각(ISO-8601). 이번 달 여유가 남아 있으면 null
    val nextChangeAvailableAt: String? = null,
)
