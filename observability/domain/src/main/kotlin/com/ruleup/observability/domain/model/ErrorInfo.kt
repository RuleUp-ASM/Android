package com.ruleup.observability.domain.model

/**
 * 예외의 **값 표현**. `Throwable` 을 그대로 담으면 직렬화가 막히고, `equals` 가 참조 동등성이 되어
 * 골든 테스트가 성립하지 않으며, 스택이 통째로 붙어 이벤트가 무거워진다.
 *
 * [message] 는 원본 그대로다 — **좌표·토큰이 예외 메시지에 섞이지 않게 하는 책임은 호출부에 있다.**
 */
data class ErrorInfo(
    /** 예외 클래스의 FQN. */
    val type: String,
    val message: String?,
    /** 예외 타입 + 상위 스택 프레임 기반의 안정 해시. 같은 지점의 예외는 같은 값을 갖는다. */
    val stackHash: String,
) {
    companion object {
        private const val FRAME_LIMIT = 16
        private const val CAUSE_DEPTH_LIMIT = 4

        /**
         * 같은 지점에서 난 같은 타입의 예외는 메시지가 달라도 같은 [stackHash] 를 갖는다.
         * 스택 트레이스 접근이 있어 저렴하지 않지만 예외 경로에서만 호출된다.
         */
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
