package com.ruleup.notification.data.api

import com.ruleup.network.dto.ApiException
import com.ruleup.network.dto.BaseResponse
import com.ruleup.network.dto.EmptyData
import com.ruleup.network.dto.ErrorBody
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * 음소거·읽음 처리는 204 로 끝난다.
 * 본문이 없는 성공을 실패로 보면 화면이 실패 토스트를 띄우고 스위치를 되돌린다(#559).
 */
class NoContentResponseTest {
    @Test
    fun `본문 없는 204 는 성공으로 본다`() {
        val noContent =
            Response.success<BaseResponse<EmptyData>>(
                null,
                okhttp3.Response
                    .Builder()
                    .code(204)
                    .message("No Content")
                    .protocol(Protocol.HTTP_1_1)
                    .request(Request.Builder().url("https://example.com/").build())
                    .build(),
            )

        noContent.throwOnFailure()
    }

    @Test
    fun `봉투에 실패가 실려 오면 서버 오류 코드를 올린다`() {
        val failed =
            Response.success(
                BaseResponse<EmptyData>(
                    success = false,
                    error = ErrorBody(code = "CHALLENGE_NOT_JOINED", message = "참여하지 않은 챌린지"),
                ),
            )

        val error = assertFailsWith<ApiException> { failed.throwOnFailure() }
        assertEquals("CHALLENGE_NOT_JOINED", error.code)
    }

    @Test
    fun `봉투 없이 HTTP 오류로 끝나면 HTTP 오류로 올린다`() {
        val serverError = Response.error<BaseResponse<EmptyData>>(502, "".toResponseBody())

        assertFailsWith<HttpException> { serverError.throwOnFailure() }
    }
}
