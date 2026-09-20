package com.ruleup.onboarding.presentation.terms

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 약관 원문 파서.
 *
 * 법정 고지 문서라 **글자가 빠지거나 마크업 기호가 그대로 보이면 안 된다.** 라이브러리를 쓰지 않고
 * 직접 쪼개므로, 실제 약관이 쓰는 문법마다 무엇이 남고 무엇이 사라지는지 여기서 고정한다.
 */
class TermsBlockTest {
    @Test
    fun `제목은 깊이를 유지한다`() {
        val blocks = parseTermsMarkdown("# 서비스 이용약관\n## 제1장 총칙\n### 제1조 (목적)")

        assertEquals(
            listOf(1 to "서비스 이용약관", 2 to "제1장 총칙", 3 to "제1조 (목적)"),
            blocks.filterIsInstance<TermsBlock.Heading>().map { it.level to it.text },
        )
    }

    @Test
    fun `강조 기호는 글자로 보이지 않는다`() {
        // ** 를 남기면 조항 한복판에 별표가 뜬다. 약관은 강조보다 읽히는 게 먼저다.
        val blocks = parseTermsMarkdown("회원에게 **불리한 변경**은 30일 전에 공지합니다.")

        assertEquals("회원에게 불리한 변경은 30일 전에 공지합니다.", (blocks.single() as TermsBlock.Paragraph).text)
    }

    @Test
    fun `표는 셀을 잃지 않고 줄로 펴진다`() {
        // 좁은 화면에서 표를 그리면 글자가 잘린다. 약관의 표는 「항목 — 설명」이라 줄로 펴도 뜻이 산다.
        val blocks =
            parseTermsMarkdown(
                """
                | 목적 | 내용 |
                |---|---|
                | 회원 관리 | 본인 식별, 중복 가입 방지 |
                """.trimIndent(),
            )

        val rows = blocks.filterIsInstance<TermsBlock.TableRow>()
        assertEquals(2, rows.size)
        assertEquals(listOf("회원 관리", "본인 식별, 중복 가입 방지"), rows[1].cells)
    }

    @Test
    fun `표 구분 행은 내용이 없어 버린다`() {
        val blocks = parseTermsMarkdown("|---|:---:|\n| 값 | 값2 |")

        assertEquals(1, blocks.filterIsInstance<TermsBlock.TableRow>().size)
    }

    @Test
    fun `번호 목록도 목록으로 읽고 번호를 남긴다`() {
        // 약관은 조항 번호가 곧 참조 수단이다 — 지우면 "제2조 3항"을 가리킬 수 없다.
        val blocks = parseTermsMarkdown("1. 서비스: 습관 형성 서비스를 말합니다.")

        assertEquals("1. 서비스: 습관 형성 서비스를 말합니다.", (blocks.single() as TermsBlock.Bullet).text)
    }

    @Test
    fun `빈 줄은 블록을 만들지 않는다`() {
        assertTrue(parseTermsMarkdown("\n   \n").isEmpty())
    }

    @Test
    fun `구분선은 한 블록으로 남는다`() {
        assertEquals(listOf(TermsBlock.Divider), parseTermsMarkdown("---"))
    }
}
