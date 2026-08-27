package com.ruleup.observability.domain.model

/**
 * TTI 측정의 결말. 우선순위는 [TIMEOUT] > [ABANDONED] > [COMPLETED] 다 —
 * 원래 독립 사건이지만 분석에서 한 축으로 묶어 보려고 더 구체적인 쪽만 남긴다.
 */
enum class TtiOutcome {
    /** [TtiPage.timelines] 가 전부 기록됐다. */
    COMPLETED,

    /** 일부 단계가 기록되지 않았다 — 다 뜨기 전에 사용자가 떠났다는 뜻이다. 이탈과 직결되는 값. */
    ABANDONED,

    /** 제한 시간을 넘겨 끝났다. */
    TIMEOUT,
}

/**
 * TTI 를 쪼개는 단계. 총 시간만 재면 "느리다"까지밖에 모른다 —
 * 네트워크가 느린지, 렌더가 느린지, 이미지가 늦게 오는지는 단계가 있어야 갈린다.
 */
enum class TtiTimeline {
    /** 화면 골격이 그려지기까지. */
    VIEW_CREATION,

    /** 요청을 보낼 수 있는 상태가 되기까지(인자 준비·인증 확인 등). */
    API_REQUEST_READY,

    /** 요청 → 응답. */
    API_RESPONSE,

    /** 응답을 화면에 반영하기까지. */
    VIEW_BINDING,

    /** 이미지 로딩 완료까지. 텍스트가 다 떠도 이미지가 비면 사용자는 "안 뜬 화면"으로 인식한다. */
    IMAGE_LOADED,
}

/** 한 단계의 소요 시간. 경과 시각이 아니라 **그 단계 자체의 길이**다 — 병목을 바로 읽기 위해서다. */
data class TtiPhase(
    val timeline: TtiTimeline,
    val durationMillis: Long,
)

enum class ProbeTrigger { JANK_DETECTED, TTI_SLOW, PERIODIC }
