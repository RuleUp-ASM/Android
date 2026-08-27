package com.ruleup.observability.domain.model

@JvmInline
value class AttrKey(
    val raw: String,
)

/** [AttrValue] 의 종류. 스키마가 키 이름뿐 아니라 **값 타입까지** 강제하기 위한 축이다. */
enum class AttrValueKind { STRING, INT64, REAL, BOOL }

sealed interface AttrValue {
    val kind: AttrValueKind

    data class Str(
        val v: String,
    ) : AttrValue {
        override val kind: AttrValueKind get() = AttrValueKind.STRING
    }

    data class Int64(
        val v: Long,
    ) : AttrValue {
        override val kind: AttrValueKind get() = AttrValueKind.INT64
    }

    data class Real(
        val v: Double,
    ) : AttrValue {
        override val kind: AttrValueKind get() = AttrValueKind.REAL
    }

    data class Bool(
        val v: Boolean,
    ) : AttrValue {
        override val kind: AttrValueKind get() = AttrValueKind.BOOL
    }
}

/**
 * 이벤트에 붙는 구조화 속성. 삽입 순서를 보존하고 [equals] 는 내부 맵을 따른다 —
 * 페이로드 팩토리의 출력을 고정하는 골든 테스트가 이 성질에 의존한다.
 */
@JvmInline
value class Attributes private constructor(
    val entries: Map<AttrKey, AttrValue>,
) {
    companion object {
        val EMPTY = Attributes(emptyMap())

        fun of(source: Map<AttrKey, AttrValue>) = Attributes(source.toMap())
    }
}

/**
 * [attributes] 안에서 쓰는 빌더. 키를 `String` 으로 받는 건 호출부가 언제나 리터럴이어서고,
 * 값 타입별 오버로드라 `Map<String, Any>` 와 달리 **넣는 순간 타입이 고정된다.**
 */
class AttributesBuilder
    internal constructor() {
        private val m = LinkedHashMap<AttrKey, AttrValue>()

        fun put(
            key: String,
            v: String,
        ) {
            m[AttrKey(key)] = AttrValue.Str(v)
        }

        /** `Int` 는 [AttrValue.Int64] 로 넓혀 담는다. 호출부에서 `.toLong()` 을 쓰지 않게 한다. */
        fun put(
            key: String,
            v: Int,
        ) {
            m[AttrKey(key)] = AttrValue.Int64(v.toLong())
        }

        fun put(
            key: String,
            v: Long,
        ) {
            m[AttrKey(key)] = AttrValue.Int64(v)
        }

        fun put(
            key: String,
            v: Double,
        ) {
            m[AttrKey(key)] = AttrValue.Real(v)
        }

        fun put(
            key: String,
            v: Boolean,
        ) {
            m[AttrKey(key)] = AttrValue.Bool(v)
        }

        internal fun build() = Attributes.of(m)
    }

fun attributes(block: AttributesBuilder.() -> Unit): Attributes = AttributesBuilder().apply(block).build()
