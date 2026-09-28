package com.ruleup.onboarding.presentation.terms

/** 약관 원문 마크다운을 화면에 그릴 조각으로 쪼갠다. */
internal sealed interface TermsBlock {
    /** [level] 1~3. */
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

// |---|---| 같은 구분 행.
private val TABLE_SEPARATOR = Regex("""^\|[\s:|-]+\|$""")

/** 원문을 블록 목록으로 바꾼다. */
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
