package com.ruleup.logging.data.sink

import android.os.Bundle
import com.ruleup.logging.domain.BizAttrValue
import com.ruleup.logging.domain.BizLog
import java.util.concurrent.atomic.AtomicLong

/**
 * [BizLog] → 분석 백엔드 매핑.
 *
 * 두 백엔드가 이름을 **같은 값으로** 쓴다 — 갈리면 같은 지표를 두 번 정의하게 된다. 다른 것은
 * 제약뿐이라, Firebase 쪽만 절단한다: 이름 40자·키 40자·값 100자·이벤트당 파라미터 25개,
 * 값 타입은 `String`·`Long`·`Double` 뿐이라 `Boolean` 은 0/1 `Long` 이다.
 *
 * **절단은 조용히 일어난다.** 잘린 값은 분석에서 다른 값과 뭉치므로 [truncated] 로 세어 둔다.
 */
internal object BizEventMapper {
    private const val MAX_NAME = 40
    private const val MAX_KEY = 40
    private const val MAX_VALUE = 100
    private const val MAX_PARAMS = 25

    // 임의 스레드에서 동시에 증가한다. @Volatile 은 read-modify-write 를 보호하지 못해
    // 안전해 보이면서 카운트가 유실된다.
    private val truncations = AtomicLong()

    /** 절단이 발생한 누적 횟수. 개발 중 매핑 손실을 눈치채기 위한 진단값이다. */
    val truncated: Long get() = truncations.get()

    fun eventName(log: BizLog): String = log.event.name.take(MAX_NAME)

    fun toBundle(log: BizLog): Bundle {
        val bundle = Bundle()
        log.screen?.let { bundle.putString("screen", it.clampValue()) }
        var count = bundle.size()
        for ((key, value) in log.event.attrs.entries) {
            if (count >= MAX_PARAMS) {
                truncations.incrementAndGet()
                break
            }
            bundle.put(key.raw.clampKey(), value)
            count++
        }
        return bundle
    }

    /**
     * Amplitude 는 임의 타입을 받으므로 원래 타입 그대로 편다. Firebase 처럼 Boolean 을 0/1 Long 으로
     * 바꾸지 않는다 — 대시보드에서 true/false 로 읽히는 편이 낫다. 절단도 하지 않으므로, 두 도구를
     * 대조할 때 값이 다르면 Firebase 쪽이 잘린 것이다.
     */
    fun toProperties(log: BizLog): MutableMap<String, Any?> {
        val props = mutableMapOf<String, Any?>()
        log.screen?.let { props["screen"] = it }
        log.event.attrs.entries
            .forEach { (key, value) -> props[key.raw] = value.unwrap() }
        return props
    }

    private fun Bundle.put(
        key: String,
        value: BizAttrValue,
    ) {
        when (value) {
            is BizAttrValue.Str -> putString(key, value.v.clampValue())
            is BizAttrValue.Int64 -> putLong(key, value.v)
            is BizAttrValue.Real -> putDouble(key, value.v)
            is BizAttrValue.Bool -> putLong(key, if (value.v) 1L else 0L)
        }
    }

    private fun BizAttrValue.unwrap(): Any =
        when (this) {
            is BizAttrValue.Str -> v
            is BizAttrValue.Int64 -> v
            is BizAttrValue.Real -> v
            is BizAttrValue.Bool -> v
        }

    private fun String.clampKey(): String = if (length <= MAX_KEY) this else take(MAX_KEY).also { truncations.incrementAndGet() }

    private fun String.clampValue(): String = if (length <= MAX_VALUE) this else take(MAX_VALUE).also { truncations.incrementAndGet() }
}
