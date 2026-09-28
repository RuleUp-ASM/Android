package com.ruleup.verification.data.api

import com.ruleup.verification.data.dto.KakaoCoord2AddressResponse
import com.ruleup.verification.data.dto.KakaoKeywordResponse
import retrofit2.http.GET
import retrofit2.http.Query

/** 카카오 로컬 */
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

    /** 좌표 → 주소 역지오코딩. */
    @GET("v2/local/geo/coord2address.json")
    suspend fun coord2Address(
        @Query("x") longitude: Double,
        @Query("y") latitude: Double,
    ): KakaoCoord2AddressResponse

    companion object {
        // 카카오 최대 15.
        const val DEFAULT_SIZE = 15
        const val SORT_ACCURACY = "accuracy"
    }
}
