package com.ruleup.profile.data.dto

import com.ruleup.domain.entity.user.AgreementType
import com.ruleup.network.dto.ApiException
import com.ruleup.profile.domain.entity.AgreementRevokeForbiddenException
import com.ruleup.profile.domain.entity.AgreementSubmission
import com.ruleup.profile.domain.entity.AgreementVersionMismatchException
import com.ruleup.profile.domain.entity.SanctionType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 동의 응답 매핑. 동의 상태는 **법적 증거**라 여기서 값을 지어내면 받지도 않은 동의를 받았다고
 * 화면이 말하게 된다.
 */
class AgreementResponseMappingTest {
    @Test
    fun `모르는 동의 항목은 화면에 세우지 않는다`() {
        // 이름도 설명도 없는 토글을 만들 수 없다.
        val status = AgreementStatusResponse(agreements = listOf(AgreementItemResponse(type = "PUSH_V2"))).toDomain()

        assertTrue(status.agreements.isEmpty())
    }

    @Test
    fun `필수 여부를 안 주면 항목 정의를 믿는다`() {
        // 필수를 선택으로 보이면 눌러 놓고 거절당하는 철회 토글이 열린다.
        val status =
            AgreementStatusResponse(
                agreements = listOf(AgreementItemResponse(type = "TOS", required = null)),
            ).toDomain()

        assertEquals(true, status.agreements.single().required)
    }

    @Test
    fun `동의한 적 없음과 철회함을 버전으로 가른다`() {
        val never = AgreementItemResponse(type = "HEALTH_INFO", agreed = false, version = null).toDomain()
        val revoked = AgreementItemResponse(type = "MARKETING", agreed = false, version = "1.0").toDomain()

        assertEquals(false, never?.everAgreed)
        assertEquals(true, revoked?.everAgreed)
    }

    @Test
    fun `동의 API 가 받지 않는 항목은 요청에서 뺀다`() {
        // 폐기된 야간 알림을 실어 보내면 서버가 400 으로 전체를 막는다.
        val request =
            listOf(
                AgreementSubmission(AgreementType.NIGHT_PUSH, agreed = true, version = "1.0"),
                AgreementSubmission(AgreementType.MARKETING, agreed = true, version = "1.0"),
            ).toRequest()

        assertEquals(listOf("MARKETING"), request.agreements.map { it.type })
    }

    @Test
    fun `필수 약관 철회 거부는 탈퇴 안내로 갈리게 도메인 예외로 옮긴다`() {
        val failure = ApiException(code = "AGREEMENT_REVOKE_FORBIDDEN", message = "필수 약관").toAgreementFailure()

        assertTrue(failure is AgreementRevokeForbiddenException)
    }

    @Test
    fun `버전 불일치는 다시 불러오기로 갈리게 도메인 예외로 옮긴다`() {
        val failure = ApiException(code = "AGREEMENT_VERSION_MISMATCH", message = "버전").toAgreementFailure()

        assertTrue(failure is AgreementVersionMismatchException)
    }
}

/**
 * 제재 응답 매핑. 해제일이 없는 것과 곧 풀리는 것을 섞으면 **영구 정지를 임시 제재처럼** 보여 준다.
 */
class SanctionResponseMappingTest {
    @Test
    fun `영구 정지는 해제일 없이 그대로 전한다`() {
        val history =
            SanctionHistoryResponse(
                activeSanction = ActiveSanctionResponse(sanctionId = "s1", type = "BAN", endsAt = null),
            ).toDomain()

        assertEquals(SanctionType.BAN, history.activeSanction?.type)
        assertNull(history.activeSanction?.endsAt)
    }

    @Test
    fun `재검토 가능 여부를 모르면 버튼을 열지 않는다`() {
        // 눌러도 되는지는 서버만 안다 — 열어 두면 사용자가 헛되이 시도한다.
        val history =
            SanctionHistoryResponse(
                activeSanction = ActiveSanctionResponse(sanctionId = "s1", reviewRequestable = null),
            ).toDomain()

        assertEquals(false, history.activeSanction?.reviewRequestable)
    }

    @Test
    fun `식별자 없는 이력은 목록에 세우지 않는다`() {
        val history =
            SanctionHistoryResponse(
                admin = listOf(AdminSanctionResponse(sanctionId = null)),
                auto = listOf(AutoSanctionResponse(sanctionId = null)),
            ).toDomain()

        assertTrue(history.admin.isEmpty())
        assertTrue(history.auto.isEmpty())
    }

    @Test
    fun `제재가 하나도 없으면 빈 이력으로 본다`() {
        val history = SanctionHistoryResponse().toDomain()

        assertTrue(history.isEmpty)
    }
}
