package com.ruleup.notification.data.api

import com.ruleup.network.dto.BaseResponse
import com.ruleup.network.dto.EmptyData
import com.ruleup.network.dto.throwOnError
import retrofit2.HttpException
import retrofit2.Response

/**
 * 본문 없는 204 를 성공으로 받는다.
 * Retrofit 은 suspend 반환형의 `?` 를 보지 않아서, `BaseResponse<EmptyData>?` 로 받으면 204 에서 예외를 던진다.
 */
internal fun Response<BaseResponse<EmptyData>>.throwOnFailure() {
    if (!isSuccessful) throw HttpException(this)
    // 서버가 봉투를 실어 보내면 그 안의 실패를 올린다
    body()?.throwOnError()
}
