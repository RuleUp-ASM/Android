package com.ruleup.domain.entity.user

/** 기능 정지가 겨냥하는 기능. */
enum class FeatureCode(
    val value: String,
    val label: String,
) {
    REPORT("REPORT", "신고"),
    ;

    companion object {
        fun fromValue(value: String?): FeatureCode? = entries.find { it.value == value }

        /** 모르는 코드는 코드 대신 뭉뚱그린다 */
        fun label(value: String?): String = fromValue(value)?.label ?: "일부 기능"
    }
}

/** 계정에 걸린 제한. */
sealed interface AccountRestriction {
    /** 제한 없음. */
    data object None : AccountRestriction

    /** [featureCode] 에 적힌 기능만 막힌다. */
    data class Feature(
        val featureCode: String?,
    ) : AccountRestriction

    /** 열람 전용. */
    data object Locked : AccountRestriction

    /** 영구 정지. */
    data object Banned : AccountRestriction

    /** 앱 전체를 잠금 화면에 고정해야 하는가. */
    val isFullLock: Boolean
        get() = this is Locked || this is Banned

    /** [feature] 를 지금 쓸 수 있는가. */
    fun blocks(feature: FeatureCode): Boolean =
        when (this) {
            None -> false
            is Feature -> featureCode == feature.value
            Locked, Banned -> true
        }
}
