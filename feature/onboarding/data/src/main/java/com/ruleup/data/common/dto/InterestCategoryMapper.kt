package com.ruleup.data.common.dto

import com.ruleup.core.model.InterestCategory

/** 서버 카테고리 코드 목록 → 도메인 [InterestCategory]. 알 수 없는 코드는 조용히 버린다. */
internal fun List<String>?.toInterestCategories(): List<InterestCategory> = this.orEmpty().mapNotNull(InterestCategory::fromValue)
