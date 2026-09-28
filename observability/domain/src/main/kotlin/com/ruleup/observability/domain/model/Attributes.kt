package com.ruleup.observability.domain.model

@JvmInline
value class AttrKey(
    val raw: String,
)

/** [AttrValue] 의 종류. */
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

/** 이벤트에 붙는 구조화 속성. */
@JvmInline
value class Attributes private constructor(
    val entries: Map<AttrKey, AttrValue>,
) {
    companion object {
        val EMPTY = Attributes(emptyMap())

        fun of(source: Map<AttrKey, AttrValue>) = Attributes(source.toMap())
    }
}

/** [attributes] 안에서 쓰는 빌더. */
class AttributesBuilder
    internal constructor() {
        private val m = LinkedHashMap<AttrKey, AttrValue>()

        fun put(
            key: String,
            v: String,
        ) {
            m[AttrKey(key)] = AttrValue.Str(v)
        }

        /** `Int` 는 [AttrValue.Int64] 로 넓혀 담는다. */
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
