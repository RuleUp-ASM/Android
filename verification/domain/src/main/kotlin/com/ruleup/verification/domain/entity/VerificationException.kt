package com.ruleup.verification.domain.entity

/**
 * sync 최소 간격 위반 (명세 §3.3, HTTP 429 SYNC_TOO_FREQUENT).
 * 백오프 후 재시도하며 전송분은 synced 미표시로 유지한다(중복은 BE 멱등이 방어).
 */
class SyncTooFrequentException(
    val retryAfterSec: Int? = null,
) : Exception("sync 요청이 너무 잦습니다.")

/**
 * 잘못된 신호 페이로드 (명세 §3.3, HTTP 400 INVALID_SIGNAL_PAYLOAD).
 * 해당 배치는 폐기(synced 처리)해 무한 재전송을 막는다.
 */
class InvalidSignalPayloadException : Exception("신호 페이로드가 유효하지 않습니다.")

/**
 * 페이로드가 서버 상한 초과 (명세 sync, HTTP 413 SYNC_PAYLOAD_TOO_LARGE).
 * 배치를 반으로 갈라 재전송한다. 더 못 쪼개는데도 초과면 폐기한다 — 같은 요청을 무한히 되풀이한다.
 */
class SyncPayloadTooLargeException : Exception("한 번에 보낼 수 있는 신호 양을 넘었습니다.")

/**
 * 수동 인증 당일 중복 제출 (명세 §3.4, HTTP 409 ALREADY_VERIFIED).
 * 오류가 아니라 "이미 인증됨"으로 안내한다(명세 §6.5).
 */
class AlreadyVerifiedException : Exception("오늘은 이미 인증했습니다.")

/**
 * 수동 인증 제출 기한 경과 (명세 POST /challenges/{id}/verifications, HTTP 400 INVALID_TARGET_DATE).
 *
 * 수동 인증은 당일(KST) 마감이다. 화면을 열어 둔 채 자정을 넘기면 실제로 발생하므로 일반 오류
 * 문구로 뭉개지 않는다 — 사용자는 방금까지 되던 버튼이 왜 막혔는지 알 수 없다.
 */
class InvalidTargetDateException : Exception("오늘이 지나 체크할 수 없어요.")

/**
 * 셋업 앵커가 유효하지 않음 (명세 setup·my-location, HTTP 400 INVALID_ANCHOR·ANCHOR_LIMIT_EXCEEDED).
 * 좌표 범위 오류나 개수(최대 3) 초과 — 화면은 앵커 목록 상단에 인라인으로 안내한다.
 */
class InvalidAnchorException : Exception("앵커 위치가 유효하지 않습니다.")

/**
 * 인증 장소 변경이 인증 윈도우 중에 들어옴 (명세 my-location PUT, HTTP 409 LOCATION_LOCKED_IN_WINDOW).
 * 그날 판정을 흔들 수 없어 거부된다 — 화면은 **익일 재시도**를 안내한다.
 */
class LocationLockedInWindowException : Exception("인증이 진행 중인 동안에는 장소를 바꿀 수 없어요.")

/**
 * 이번 달 변경 횟수 소진 (명세 my-location PUT, HTTP 429 SETTING_CHANGE_LIMIT).
 *
 * 언제부터 다시 바꿀 수 있는지는 이 예외가 아니라 `GET /my-location` 의 `nextChangeAvailableAt`
 * 에서 읽는다 — 공통 에러 본문에 그 필드를 실을 자리가 없다.
 */
class SettingChangeLimitException : Exception("이번 달 변경 횟수를 모두 썼어요.")

/**
 * 이의 사유 형식 미달 (명세 appeals, HTTP 400 INVALID_REASON).
 *
 * 화면이 [AppealPolicy.MIN_REASON_LENGTH]자 하한으로 먼저 막으므로 정상 흐름에서는 오지 않는다.
 * 도달했다면 입력 하단에 인라인으로 알린다 — 접수·이력 어디에도 남지 않아 다시 쓰면 그만이다.
 */
class InvalidAppealReasonException : Exception("사유를 조금 더 적어 주세요.")

/**
 * 이의 신청 기한 경과 (명세 appeals, HTTP 409 APPEAL_WINDOW_CLOSED).
 * 이의는 판정 다음 날까지만 낼 수 있다 — 화면은 안내 후 상태를 다시 읽어 진입점을 거둔다.
 */
class AppealWindowClosedException : Exception("이의 신청 기한이 지났어요.")

/**
 * 이의 대상이 실패 상태가 아님 (명세 appeals, HTTP 409 NOT_FAILED).
 *
 * **오류로 보여줄 실패가 아니다** — 이미 인용됐거나 성공으로 정정된 건이다. 화면은 조용히 상태만
 * 다시 읽어 성공 표시로 바꾼다.
 */
class AppealNotFailedException : Exception("이미 정정된 인증이에요.")

/**
 * 수동 인증 취소 기한 경과 (명세 DELETE /verifications/{id}, HTTP 409 CANCEL_WINDOW_CLOSED).
 * 당일(KST)이 지나면 취소할 수 없다 — 화면은 체크를 되돌리지 않고 사유를 안내한다.
 */
class CancelWindowClosedException : Exception("오늘이 지나 취소할 수 없어요.")

/**
 * 대상 앱 세트가 유효하지 않음 (명세 my-screen-apps PUT, HTTP 400 INVALID_APP).
 * 패키지명 형식 오류·중복·개수(1~10) 초과 — 화면은 선택 수정 안내.
 */
class InvalidScreenAppException : Exception("대상 앱 선택이 유효하지 않습니다.")
