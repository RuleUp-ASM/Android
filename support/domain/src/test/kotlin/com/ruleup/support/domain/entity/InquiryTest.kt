package com.ruleup.support.domain.entity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InquiryCategoryTest {
    @Test
    fun `명세의 6종만 정의돼 있다`() {
        // 여기 없는 값을 보내면 서버가 400 INVALID_REQUEST 로 막는다.
        assertEquals(
            listOf(
                "VERIFICATION",
                "DEVICE_PERMISSION",
                "CHALLENGE_GROUP",
                "ACCOUNT_LOGIN",
                "REPORT_SANCTION",
                "ERROR_ETC",
            ),
            InquiryCategory.entries.map { it.value },
        )
    }

    @Test
    fun `모르는 분류는 null 이라 목록에서 그 문의가 사라지지 않는다`() {
        // 운영자가 6종 밖으로 옮겼을 때 항목을 통째로 숨기면 사용자가 자기 문의를 잃는다.
        assertNull(InquiryCategory.fromValue("BILLING"))
        assertNull(InquiryCategory.fromValue(null))
    }
}

class InquiryStatusTest {
    @Test
    fun `상태는 접수됨과 답변 완료 둘뿐이다`() {
        // 중간 상태를 추가하면 화면이 있지도 않은 「검토중」을 표현할 수 있게 된다.
        assertEquals(listOf("RECEIVED", "ANSWERED"), InquiryStatus.entries.map { it.value })
    }

    @Test
    fun `모르는 상태는 접수됨으로 떨어진다`() {
        // 답변이 없는데 「답변 완료」로 보이면 상세에 들어갔다가 빈 답변을 만난다.
        assertEquals(InquiryStatus.RECEIVED, InquiryStatus.fromValue("IN_REVIEW"))
        assertEquals(InquiryStatus.RECEIVED, InquiryStatus.fromValue(null))
    }
}

class InquiryBodyTest {
    @Test
    fun `열 자 미만은 만들 수 없다`() {
        // 서버의 400 INQUIRY_BODY_LENGTH 가 사용자에게 가기 전에 여기서 막힌다.
        assertFailsWith<IllegalArgumentException> { InquiryBody.of("짧아요") }
    }

    @Test
    fun `천 자를 넘으면 만들 수 없다`() {
        assertFailsWith<IllegalArgumentException> { InquiryBody.of("가".repeat(1_001)) }
    }

    @Test
    fun `앞뒤 공백은 길이 판정에 들어가지 않는다`() {
        // 공백만 채워 열 자를 넘기는 입력이 통과하면 운영자가 읽을 내용이 없는 문의가 쌓인다.
        assertFailsWith<IllegalArgumentException> { InquiryBody.of("  짧아요   ") }
        assertEquals("인증이 실패로 떴어요", InquiryBody.of("  인증이 실패로 떴어요  ").value)
    }

    @Test
    fun `경계값은 통과한다`() {
        assertEquals(10, InquiryBody.of("가".repeat(10)).value.length)
        assertEquals(1_000, InquiryBody.of("가".repeat(1_000)).value.length)
    }

    @Test
    fun `isValid 가 of 와 같은 경계를 본다`() {
        // 버튼 활성 판정이 of 와 어긋나면 누를 수 있는 버튼이 예외로 떨어진다.
        assertFalse(InquiryBody.isValid("가".repeat(9)))
        assertTrue(InquiryBody.isValid("가".repeat(10)))
        assertTrue(InquiryBody.isValid("가".repeat(1_000)))
        assertFalse(InquiryBody.isValid("가".repeat(1_001)))
    }
}

class InquirySubmissionTest {
    @Test
    fun `사진이 세 장을 넘으면 만들 수 없다`() {
        assertFailsWith<IllegalArgumentException> {
            InquirySubmission(
                category = InquiryCategory.ERROR_ETC,
                body = InquiryBody.of("탐색 탭에서 앱이 종료돼요"),
                imageUrls = List(4) { "/files/$it.jpg" },
            )
        }
    }

    @Test
    fun `사진 없이도 접수된다`() {
        val submission =
            InquirySubmission(
                category = InquiryCategory.VERIFICATION,
                body = InquiryBody.of("기상 인증이 실패로 떴어요"),
            )
        assertEquals(emptyList(), submission.imageUrls)
    }
}

class InquirySummaryTest {
    @Test
    fun `답변 시각이 있으면 새 답변으로 본다`() {
        // 읽음 지점을 서버가 보관하지 않아 답변 유무가 유일한 기준이다.
        assertTrue(summary(answeredAt = "2026-09-06T11:08:00Z").hasNewAnswer)
        assertFalse(summary(answeredAt = null).hasNewAnswer)
    }

    private fun summary(answeredAt: String?) =
        InquirySummary(
            inquiryId = "3d6ad414-5fb6-81aa-8d11-ca6ffd272529",
            category = InquiryCategory.VERIFICATION,
            status = if (answeredAt == null) InquiryStatus.RECEIVED else InquiryStatus.ANSWERED,
            preview = "기상 인증이 실패로 떴어요",
            createdAt = "2026-09-05T14:22:00Z",
            answeredAt = answeredAt,
        )
}
