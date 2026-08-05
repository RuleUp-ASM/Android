package com.ruleup.onboarding.domain.auth

import com.ruleup.onboarding.domain.entity.OAuthProfile
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 가입이 끝날 때까지만 사는 signup_token 보관소.
 *
 * **메모리에만 둔다.** 5분짜리 임시 신원이라 디스크에 남으면 안 되고(FE 스펙), 그래서 백스택에도
 * 실을 수 없다 — `GenericNavKey` 는 직렬화되어 프로세스 사망을 넘어 저장되므로 네비게이션 인자로
 * 넘기면 그대로 saved state 에 남는다. 인자가 아니게 되면서 외부 딥링크로 주입할 표면도 사라진다.
 *
 * 로그인 화면과 온보딩이 서로 다른 ViewModel 이라 값을 넘길 자리가 필요하다. 가입 흐름은 앱 전체에
 * 하나뿐이므로 싱글턴이면 충분하다 — `SignupTimer` 와 같은 이유다.
 *
 * 프로세스가 죽으면 토큰도 사라진다. **그게 맞다** — 어차피 5분이면 만료되고, 복원해 봐야
 * `INVALID_SIGNUP_TOKEN` 으로 로그인부터 다시 하게 된다.
 */
@Singleton
class SignupSession
    @Inject
    constructor() {
        @Volatile
        private var token: String? = null

        @Volatile
        private var profile: OAuthProfile? = null

        fun start(
            signupToken: String,
            oauthProfile: OAuthProfile,
        ) {
            token = signupToken
            profile = oauthProfile
        }

        /** 진행 중인 가입의 토큰. 없으면 null — 호출부는 로그인부터 다시 시작시킨다. */
        fun token(): String? = token

        /**
         * IdP 가 준 프로필 힌트. 닉네임 프리필에 쓴다.
         *
         * **자동 제출하지 않는다** — 프리필한 닉네임도 check API 를 통과해야 다음 단계로 간다.
         * 남이 이미 쓰는 이름일 수 있다.
         */
        fun oauthProfile(): OAuthProfile? = profile

        /** 가입 완료·이탈 시 비운다. 남겨 두면 다음 가입 시도가 만료된 토큰을 물고 시작한다. */
        fun clear() {
            token = null
            profile = null
        }
    }
