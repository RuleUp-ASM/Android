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
 * PHOTO 제출 시 imageUrl 누락 (명세 §3.4, HTTP 400 IMAGE_REQUIRED).
 */
class ImageRequiredException : Exception("사진 인증에는 이미지가 필요합니다.")

/**
 * 예비 수동 폴백 주1회 한도 초과 (명세 §9.2, HTTP 409 FALLBACK_LIMIT_EXCEEDED).
 * 이미 이번 주 폴백을 썼으면 그냥 NO_SIGNAL_RECEIVED 실패로 떨어진다 — 화면은 한도 안내.
 */
class FallbackLimitExceededException : Exception("이번 주 수동 인증 횟수를 모두 사용했어요.")

/**
 * 셋업 앵커가 유효하지 않음 (명세 setup, HTTP 400 INVALID_ANCHOR).
 * 반경 범위(500~5000m)·개수(최대 10) 위반 등 — 화면은 입력 수정 안내.
 */
class InvalidAnchorException : Exception("앵커 위치가 유효하지 않습니다.")

/**
 * 스크린타임 대상 앱 변경 쿨다운 (명세 my-screen-apps PUT, HTTP 429 SCREENTIME_CHANGE_COOLDOWN).
 * 최근 변경 직후 재변경 제한 — 화면은 "잠시 후 다시 시도" 안내.
 */
class ScreenAppChangeCooldownException : Exception("대상 앱을 방금 변경해 잠시 후 다시 시도할 수 있어요.")

/**
 * 대상 앱 세트가 유효하지 않음 (명세 my-screen-apps PUT, HTTP 400 INVALID_APP).
 * 패키지명 형식 오류·중복·개수(1~10) 초과 — 화면은 선택 수정 안내.
 */
class InvalidScreenAppException : Exception("대상 앱 선택이 유효하지 않습니다.")
