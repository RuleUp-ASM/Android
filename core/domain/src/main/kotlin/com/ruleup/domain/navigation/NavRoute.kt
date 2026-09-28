package com.ruleup.domain.navigation

import kotlinx.serialization.json.Json

/** 단일 통합 네비게이션 단위. */
data class NavRoute(
    val path: String,
    val args: Map<String, String> = emptyMap(),
)

/** 각 페이지별로 정의되는 typed argument 의 마커. */
interface Page {
    fun toRoute(): NavRoute
}

/** NavRoute.args 인코딩/디코딩에 공용으로 사용하는 Json 인스턴스. */
val NavRouteJson: Json = Json { ignoreUnknownKeys = true }
