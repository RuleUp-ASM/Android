package com.ruleup.verification.domain.entity

/** 장소 검색 결과 1건. */
data class Place(
    val name: String,
    val lat: Double,
    val lng: Double,
    val address: String?,
    val category: String?,
)
