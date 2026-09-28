package com.ruleup.logging.data.sink

import android.os.Bundle
import com.ruleup.logging.domain.BizAttrValue
import com.ruleup.logging.domain.BizLog
import java.util.concurrent.atomic.AtomicLong

/** [BizLog] → 분석 백엔드 매핑. */
internal object BizEventMapper {
    private const val MAX_NAME = 40
    private const val MAX_KEY = 40
    private const val MAX_VALUE = 100
    private const val MAX_PARAMS = 25

    // 임의 스레드에서 동시에 증가한다.
    private val truncations = AtomicLong()

    /** 절단이 발생한 누적 횟수. */
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

    /** Amplitude 는 임의 타입을 받으므로 원래 타입 그대로 편다. */
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
