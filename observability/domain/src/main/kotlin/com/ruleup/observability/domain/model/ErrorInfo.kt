package com.ruleup.observability.domain.model

/** 예외의 값 표현. */
data class ErrorInfo(
    /** 예외 클래스의 FQN. */
    val type: String,
    val message: String?,
    /** 예외 타입 + 상위 스택 프레임 기반의 안정 해시. */
    val stackHash: String,
) {
    companion object {
        private const val FRAME_LIMIT = 16
        private const val CAUSE_DEPTH_LIMIT = 4

        /** 같은 지점에서 난 같은 타입의 예외는 메시지가 달라도 같은 [stackHash] 를 갖는다. */
        fun from(throwable: Throwable): ErrorInfo {
            var hash = 0
            var current: Throwable? = throwable
            var depth = 0
            while (current != null && depth < CAUSE_DEPTH_LIMIT) {
                val frames = current.stackTrace
                hash = hash * 31 + current.javaClass.name.hashCode()
                var i = 0
                while (i < frames.size && i < FRAME_LIMIT) {
                    val frame = frames[i]
                    hash = hash * 31 + frame.className.hashCode()
                    hash = hash * 31 + frame.methodName.hashCode()
                    hash = hash * 31 + frame.lineNumber
                    i++
                }
                val next = current.cause
                current = if (next === current) null else next
                depth++
            }
            return ErrorInfo(
                type = throwable.javaClass.name,
                message = throwable.message,
                stackHash = Integer.toHexString(hash),
            )
        }
    }
}
