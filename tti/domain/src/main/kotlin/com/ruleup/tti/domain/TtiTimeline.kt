package com.ruleup.tti.domain

/** TTI(Time To Interactive) 를 이루는 구간. */
enum class TtiTimeline {
    /** 화면 진입 → ViewModel 생성 · 첫 컴포지션. */
    VIEW_CREATE,

    /** 서버 요청 → 응답. */
    BACKEND,

    /** 받은 데이터로 다시 그리기 시작 → 그 프레임이 실제로 그려짐. */
    VIEW_BINDING,

    /** 이미지처럼 뒤늦게 채워지는 큰 덩어리 로딩. */
    BIG_PART_LOADING,
    ;

    companion object {
        /** 한 건이 완성되려면 채워야 하는 구간 수. */
        val REQUIRED_COUNT: Int = entries.size
    }
}
