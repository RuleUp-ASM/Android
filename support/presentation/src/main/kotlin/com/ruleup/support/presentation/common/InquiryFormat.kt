package com.ruleup.support.presentation.common

/**
 * 화면에 보이는 접수번호. **서버가 주는 UUID 를 줄여서만 보여준다.**
 *
 * 전체 값이 36자라 한 줄에 들어가지 않는데, 앱이 보기 좋은 번호를 새로 만들면 CS 담당자가 그
 * 값으로 조회할 수 없어 대조 키가 사라진다. 그래서 표시만 줄이고 복사는 원문을 넘긴다.
 */
fun shortInquiryId(inquiryId: String): String =
    if (inquiryId.length <= SHORT_ID_LENGTH) inquiryId else inquiryId.take(SHORT_ID_LENGTH) + "…"

private const val SHORT_ID_LENGTH = 18

/**
 * "2026-09-05T14:22:00Z" → "09.05". 목록 줄처럼 연도가 필요 없는 자리에 쓴다.
 *
 * 파싱에 실패하면 받은 문자열을 그대로 돌려준다 — 날짜 한 칸을 비우는 것보다 원문이라도 보이는
 * 편이 사용자가 접수 시점을 가늠하는 데 낫다.
 */
fun shortDate(iso: String): String {
    val parts = iso.substringBefore('T').split('-')
    return if (parts.size == 3) "${parts[1]}.${parts[2]}" else iso
}

/** "2026-09-05T14:22:00Z" → "09.05 14:22". 상세·답변처럼 시각까지 필요한 자리에 쓴다. */
fun shortDateTime(iso: String): String {
    val date = shortDate(iso)
    val time = iso.substringAfter('T', "").take(5)
    return if (time.length == 5) "$date $time" else date
}
