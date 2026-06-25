package com.ruleup.verification.domain.entity

/**
 * 장소 검색 결과 1건 (명세 §5.2·§11.7 places/search). 셋업·수정 시 앵커를 채우는 용도로만 쓰며,
 * 평가 시점엔 검색을 호출하지 않는다(셋업 1회 호출 → anchors[] 로 굳힘).
 */
data class Place(
    val name: String,
    val lat: Double,
    val lng: Double,
    val address: String?,
    val category: String?,
)
