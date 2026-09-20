package com.ruleup.onboarding.presentation.terms

/**
 * 약관 원문 마크다운을 화면에 그릴 조각으로 쪼갠다.
 *
 * 마크다운 라이브러리를 들이지 않는다 — 여는 문서가 앱에 번들된 약관 3종뿐이고, 그 문서들이 쓰는
 * 문법도 제목·굵게·목록·표·구분선으로 한정된다. 범용 렌더러를 의존성으로 안고 가는 값이
 * 여기서 얻는 것보다 크다.
 *
 * **표는 셀을 줄로 푼다.** 좁은 화면에서 표를 그리면 가로 스크롤이 생기거나 글자가 잘리는데,
 * 약관의 표는 「항목 — 설명」 형태라 줄로 풀어도 뜻이 상하지 않는다.
 */
internal sealed interface TermsBlock {
    /** [level] 1~3. 문서 제목·장·조 순으로 굵기가 준다. */
    data class Heading(
        val level: Int,
        val text: String,
    ) : TermsBlock

    data class Paragraph(
        val text: String,
    ) : TermsBlock

    data class Bullet(
        val text: String,
        val depth: Int,
    ) : TermsBlock

    /** 표 한 행을 「항목 · 설명」 한 줄로 편 것. */
    data class TableRow(
        val cells: List<String>,
    ) : TermsBlock

    data object Divider : TermsBlock
}

private val HEADING = Regex("""^(#{1,6})\s+(.*)$""")
private val BULLET = Regex("""^(\s*)[-*]\s+(.*)$""")
private val NUMBERED = Regex("""^(\s*)(\d+\.)\s+(.*)$""")

// |---|---| 같은 구분 행. 표의 뼈대일 뿐 내용이 없어 버린다.
private val TABLE_SEPARATOR = Regex("""^\|[\s:|-]+\|$""")

/**
 * 원문을 블록 목록으로 바꾼다.
 *
 * 굵게(`**…**`)는 **떼어낸다** — 조항 안에서 강조가 잦아 그대로 두면 별표가 글자로 보인다.
 * 강조를 살리려면 AnnotatedString 이 필요한데, 약관을 읽는 데 굵기가 없다고 뜻이 바뀌지는 않는다.
 */
internal fun parseTermsMarkdown(raw: String): List<TermsBlock> =
    raw
        .lineSequence()
        .mapNotNull { line -> line.toBlock() }
        .toList()

private fun String.toBlock(): TermsBlock? {
    val line = trimEnd()
    if (line.isBlank()) return null
    if (line.trim() == "---" || line.trim() == "***") return TermsBlock.Divider

    HEADING.matchEntire(line)?.let { m ->
        return TermsBlock.Heading(level = m.groupValues[1].length, text = m.groupValues[2].clean())
    }
    if (TABLE_SEPARATOR.matches(line.trim())) return null
    if (line.trim().startsWith("|")) {
        val cells =
            line
                .trim()
                .trim('|')
                .split('|')
                .map { it.clean() }
                .filter { it.isNotEmpty() }
        return cells.takeIf { it.isNotEmpty() }?.let(TermsBlock::TableRow)
    }
    BULLET.matchEntire(line)?.let { m ->
        return TermsBlock.Bullet(text = m.groupValues[2].clean(), depth = m.groupValues[1].length / 2)
    }
    NUMBERED.matchEntire(line)?.let { m ->
        return TermsBlock.Bullet(
            text = "${m.groupValues[2]} ${m.groupValues[3].clean()}",
            depth = m.groupValues[1].length / 2,
        )
    }
    return TermsBlock.Paragraph(line.clean())
}

/** 강조 표시를 떼고 남은 공백을 정리한다. */
private fun String.clean(): String = replace("**", "").replace("__", "").trim()
