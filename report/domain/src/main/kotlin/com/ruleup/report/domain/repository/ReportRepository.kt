package com.ruleup.report.domain.repository

import com.ruleup.report.domain.entity.BlockList
import com.ruleup.report.domain.entity.ReportResult
import com.ruleup.report.domain.entity.ReportTarget

/**
 * 신고 접수와 개인 차단(명세: 신고 접수 API, 2026-08-26 개편).
 *
 * 실패는 전부 `ReportException` 으로 온다 — 호출부가 `ApiException` 코드를 읽지 않는다.
 */
interface ReportRepository {
    /**
     * 신고를 접수한다(명세 POST /reports).
     *
     * 서버는 **단일 동기 트랜잭션**으로 차단 등재 → 컨텍스트 스냅샷 → 전건 적재까지 끝낸다.
     * 적재 자체는 어떤 제재도 발동시키지 않는다 — 제재는 운영자가 계정 단위로만 내린다.
     * 따라서 호출부는 "신고했으니 조치된다"고 안내하면 안 된다.
     *
     * 같은 대상을 다시 신고해도 **에러가 아니다.** 서버는 차단을 재적용하고 건을 하나 더 쌓은 뒤
     * 정상 201 을 준다. 중복 접수를 앱에서 막지 말고 결과를 그대로 보여준다 — 신고자에게는
     * 언제나 정상 접수로 보여야 한다.
     */
    suspend fun report(target: ReportTarget): ReportResult

    /** 내가 차단한 사용자·챌린지 목록(명세 GET /users/me/blocks). */
    suspend fun getBlocks(): BlockList

    /**
     * 사용자 차단을 푼다(명세 DELETE /users/me/blocks/users/{id}).
     *
     * **신고 취소가 아니다** — 신고 건과 스냅샷은 그대로 남는다. 해제를 취소로 처리하면 가해자가
     * 피해자에게 해제를 종용해 기록을 지우는 경로가 생긴다. 화면 문구도 이 구분을 지켜야 한다.
     *
     * 목록에 없으면 `ReportFailure.BLOCK_ENTRY_NOT_FOUND`. 반환값이 없는 이유는 서버가 성공 시
     * `removed` 를 **항상 true** 로 주기 때문이다(해제할 게 없으면 404 로 갈린다).
     */
    suspend fun unblockUser(userId: String)

    /** 챌린지 차단을 푼다(명세 DELETE /users/me/blocks/challenges/{id}). 마찬가지로 신고 기록은 남는다. */
    suspend fun unblockChallenge(challengeId: String)
}
