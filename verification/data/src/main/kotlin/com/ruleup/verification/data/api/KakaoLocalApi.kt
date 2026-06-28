package com.ruleup.verification.data.api

import com.ruleup.verification.data.dto.KakaoKeywordResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * 카카오 로컬 — 키워드로 장소검색(명세 §5.2). 셋업·수정 시 앵커(지점 좌표)를 채우는 용도로,
 * 앱이 dapi.kakao.com 을 직접 호출한다(인증 헤더는 [com.ruleup.verification.data.di.KakaoLocalModule] 의 인터셉터).
 *
 * x=경도, y=위도(둘 다 선택). [radiusM] 를 주면 중심 반경 내로 한정(NEARBY_BRAND), 없으면 전국.
 */
interface KakaoLocalApi {
    @GET("v2/local/search/keyword.json")
    suspend fun searchKeyword(
        @Query("query") query: String,
        @Query("x") longitude: Double? = null,
        @Query("y") latitude: Double? = null,
        @Query("radius") radiusM: Int? = null,
        @Query("size") size: Int = DEFAULT_SIZE,
        @Query("sort") sort: String = SORT_ACCURACY,
    ): KakaoKeywordResponse

    companion object {
        // 카카오 최대 15(명세 §5.2 상한 10과 호환). 자동완성 후보 개수.
        const val DEFAULT_SIZE = 15
        const val SORT_ACCURACY = "accuracy"
    }
}
