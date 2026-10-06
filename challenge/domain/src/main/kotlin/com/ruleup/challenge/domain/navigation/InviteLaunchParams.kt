package com.ruleup.challenge.domain.navigation

/**
 * 카카오톡 초대 카드의 앱 실행 파라미터(`androidExecutionParams`) 계약.
 *
 * 카드 버튼이 웹 주소만 가지면 카카오톡 인앱 브라우저가 그 페이지를 열고, 서버는 기기와 상관없이
 * 플레이스토어로 보낸다. 이 파라미터를 실으면 설치된 앱이 `kakao{앱키}://kakaolink?…` 로 바로 열린다.
 * 보내는 쪽(공유)과 받는 쪽(딥링크 해석)이 같은 키를 봐야 해서 한 곳에 둔다.
 */
object InviteLaunchParams {
    const val KEY_INVITE = "invite"
    const val KEY_TOKEN = "token"

    /** 감시자 초대 — 웹 주소의 `/w/{token}` 과 같은 뜻. */
    const val INVITE_WATCHER = "w"

    /** 챌린지 멤버 초대 — 웹 주소의 `/c/{token}` 과 같은 뜻. */
    const val INVITE_CHALLENGE = "c"

    fun watcher(token: String): Map<String, String> = mapOf(KEY_INVITE to INVITE_WATCHER, KEY_TOKEN to token)

    fun challenge(token: String): Map<String, String> = mapOf(KEY_INVITE to INVITE_CHALLENGE, KEY_TOKEN to token)
}
