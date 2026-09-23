package com.ruleup.domain.entity.user

/**
 * 기능 정지가 겨냥하는 기능 (명세 `featureCode`).
 *
 * 백오피스가 집행할 수 있는 값은 지금 [REPORT] 하나뿐이다 — 정책 § 2 의 「콘텐츠 수정 정지」는
 * 대응하는 코드가 없어 집행 수단이 없다(백오피스 테크 스펙 오픈 이슈 #9).
 *
 * [label] 은 화면에 그대로 쓴다. 서버가 준 영문 코드를 날것으로 보여 주면 사용자는 무엇이
 * 막혔는지 읽을 수 없다.
 */
enum class FeatureCode(
    val value: String,
    val label: String,
) {
    REPORT("REPORT", "신고"),
    ;

    companion object {
        fun fromValue(value: String?): FeatureCode? = entries.find { it.value == value }

        /** 모르는 코드는 코드 대신 뭉뚱그린다 — 영문 원문을 노출하느니 범위를 흐리는 편이 낫다. */
        fun label(value: String?): String = fromValue(value)?.label ?: "일부 기능"
    }
}

/**
 * 계정에 걸린 제한. **앱의 모든 게이트가 이 한 값을 본다.**
 *
 * [AccountStatus] 만으로는 무엇이 막혔는지 가릴 수 없다 — 서버의 `users.status` 는
 * `ACTIVE`·`SUSPENDED`·`WITHDRAWN` 3종뿐이라 **기능 정지도 전체 잠금도 똑같이 `SUSPENDED` 로
 * 온다**(백오피스 테크 스펙 부록 A). 종류와 기간은 활성 제재의 `type` 이 들고 있다.
 *
 * 화면마다 `accountStatus` 와 제재 `type` 을 따로 해석하면 **같은 계정이 경로에 따라 잠기기도 하고
 * 통과하기도 한다** — 자동 로그인은 잠금 화면인데 재로그인은 홈으로 들어가고(AUTH-13), 신고 기능만
 * 정지된 계정이 앱 전체에 갇혔다(SAN-05).
 */
sealed interface AccountRestriction {
    /** 제한 없음. **조회에 실패했을 때의 기본값이기도 하다** — 모른다고 잠그면 앱을 통째로 잃는다. */
    data object None : AccountRestriction

    /** [featureCode] 에 적힌 기능만 막힌다. 나머지 화면은 평소와 같다. */
    data class Feature(
        val featureCode: String?,
    ) : AccountRestriction

    /** 열람 전용. 쓰기 API 가 전부 막히고 앱은 잠금 화면으로 고정된다. */
    data object Locked : AccountRestriction

    /** 영구 정지. 해제일이 없다. */
    data object Banned : AccountRestriction

    /** 앱 전체를 잠금 화면에 고정해야 하는가. 기능 정지는 그 기능만 막으므로 여기 들지 않는다. */
    val isFullLock: Boolean
        get() = this is Locked || this is Banned

    /** [feature] 를 지금 쓸 수 있는가. 전체 잠금은 모든 기능을 포함한다. */
    fun blocks(feature: FeatureCode): Boolean =
        when (this) {
            None -> false
            is Feature -> featureCode == feature.value
            Locked, Banned -> true
        }
}
