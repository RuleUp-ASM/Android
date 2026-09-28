package com.ruleup.verification.domain.entity

/** sync 최소 간격 위반. */
class SyncTooFrequentException(
    val retryAfterSec: Int? = null,
) : Exception("sync 요청이 너무 잦습니다.")

/** 잘못된 신호 페이로드. */
class InvalidSignalPayloadException : Exception("신호 페이로드가 유효하지 않습니다.")

/** 페이로드가 서버 상한 초과. */
class SyncPayloadTooLargeException : Exception("한 번에 보낼 수 있는 신호 양을 넘었습니다.")

/** 수동 인증 당일 중복 제출. */
class AlreadyVerifiedException : Exception("오늘은 이미 인증했습니다.")

/** 수동 인증 제출 기한 경과. */
class InvalidTargetDateException : Exception("오늘이 지나 체크할 수 없어요.")

/** 셋업 앵커가 유효하지 않음. */
class InvalidAnchorException : Exception("앵커 위치가 유효하지 않습니다.")

/** 인증 장소 변경이 인증 윈도우 중에 들어옴. */
class LocationLockedInWindowException : Exception("인증이 진행 중인 동안에는 장소를 바꿀 수 없어요.")

/** 이번 달 변경 횟수 소진. */
class SettingChangeLimitException : Exception("이번 달 변경 횟수를 모두 썼어요.")

/** 이의 사유 형식 미달. */
class InvalidAppealReasonException : Exception("사유를 조금 더 적어 주세요.")

/** 이의 신청 기한 경과. */
class AppealWindowClosedException : Exception("이의 신청 기한이 지났어요.")

/** 이의 대상이 실패 상태가 아님. */
class AppealNotFailedException : Exception("이미 정정된 인증이에요.")

/** 수동 인증 취소 기한 경과. */
class CancelWindowClosedException : Exception("오늘이 지나 취소할 수 없어요.")

/** 대상 앱 세트가 유효하지 않음. */
class InvalidScreenAppException : Exception("대상 앱 선택이 유효하지 않습니다.")
