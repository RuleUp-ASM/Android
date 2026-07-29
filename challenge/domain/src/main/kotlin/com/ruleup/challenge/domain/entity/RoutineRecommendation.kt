package com.ruleup.challenge.domain.entity

import com.ruleup.domain.entity.user.InterestCategory

/**
 * 루틴 발견 추천 항목 (명세: GET /recommendations/routines).
 * 관심사 + 세그먼트(인구통계) 인기도 기반 루틴 템플릿 추천. 선택 시 [templateId] 로
 * by-template 초안 생성(POST /challenges/recommendation/by-template)을 호출한다.
 */
data class RoutineRecommendation(
    val templateId: Long,
    val title: String,
    val description: String?,
    // 서버 카테고리 코드를 앱 카테고리로 매핑(미매칭이면 null).
    val category: InterestCategory?,
    // 추천 사유(예: "20대 인기 루틴").
    val reason: String,
)
