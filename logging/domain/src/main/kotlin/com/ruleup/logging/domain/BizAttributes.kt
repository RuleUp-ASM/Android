package com.ruleup.logging.domain

@JvmInline
value class BizAttrKey(
    val raw: String,
)

/** [BizAttrValue] 의 종류. 스키마가 키 이름뿐 아니라 **값 타입까지** 강제하기 위한 축이다. */
enum class BizAttrValueKind { STRING, INT64, REAL, BOOL }

sealed interface BizAttrValue {
    val kind: BizAttrValueKind

    data class Str(
        val v: String,
    ) : BizAttrValue {
        override val kind: BizAttrValueKind get() = BizAttrValueKind.STRING
    }

    data class Int64(
        val v: Long,
    ) : BizAttrValue {
        override val kind: BizAttrValueKind get() = BizAttrValueKind.INT64
    }

    data class Real(
        val v: Double,
    ) : BizAttrValue {
        override val kind: BizAttrValueKind get() = BizAttrValueKind.REAL
    }

    data class Bool(
        val v: Boolean,
    ) : BizAttrValue {
        override val kind: BizAttrValueKind get() = BizAttrValueKind.BOOL
    }
}

/**
 * 이벤트에 붙는 구조화 속성. 삽입 순서를 보존하고 [equals] 는 내부 맵을 따른다 —
 * 이벤트 팩토리의 출력을 그대로 박아 두는 골든 테스트가 이 성질에 의존한다.
 */
@JvmInline
value class BizAttributes private constructor(
    val entries: Map<BizAttrKey, BizAttrValue>,
) {
    companion object {
        val EMPTY = BizAttributes(emptyMap())

        fun of(source: Map<BizAttrKey, BizAttrValue>) = BizAttributes(source.toMap())
    }
}

/**
 * [bizAttributes] 안에서 쓰는 빌더. 키를 `String` 으로 받는 건 호출부가 언제나 리터럴이어서고,
 * 값 타입별 오버로드라 `Map<String, Any>` 와 달리 **넣는 순간 타입이 고정된다.**
 */
class BizAttributesBuilder
    internal constructor() {
        private val m = LinkedHashMap<BizAttrKey, BizAttrValue>()

        fun put(
            key: String,
            v: String,
        ) {
            m[BizAttrKey(key)] = BizAttrValue.Str(v)
        }

        /** `Int` 는 [BizAttrValue.Int64] 로 넓혀 담는다. 호출부에서 `.toLong()` 을 쓰지 않게 한다. */
        fun put(
            key: String,
            v: Int,
        ) {
            m[BizAttrKey(key)] = BizAttrValue.Int64(v.toLong())
        }

        fun put(
            key: String,
            v: Long,
        ) {
            m[BizAttrKey(key)] = BizAttrValue.Int64(v)
        }

        fun put(
            key: String,
            v: Double,
        ) {
            m[BizAttrKey(key)] = BizAttrValue.Real(v)
        }

        fun put(
            key: String,
            v: Boolean,
        ) {
            m[BizAttrKey(key)] = BizAttrValue.Bool(v)
        }

        internal fun build() = BizAttributes.of(m)
    }

fun bizAttributes(block: BizAttributesBuilder.() -> Unit): BizAttributes = BizAttributesBuilder().apply(block).build()
