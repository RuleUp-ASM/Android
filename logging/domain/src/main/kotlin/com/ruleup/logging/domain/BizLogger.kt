package com.ruleup.logging.domain

import kotlinx.coroutines.CoroutineDispatcher

/**
 * 비즈니스 이벤트 기록기.
 *
 * 앱 전체에 하나만 있고, 시작은 `:app` 이 [init] 으로 연다. 진단·성능(`:observability`)과 달리
 * **샘플링도 심각도 게이트도 없다** — 행동 이벤트는 발생한 만큼 다 나가야 전환율이 계산된다.
 *
 * 쌓아 두는 곳이 없다 — [record] 한 건이 그대로 한 번의 전송이 된다. 화면과 사용자 식별자는
 * 기록하는 쪽이 넘기지 않는다. 포트([BizScreenSource]·[BizUserSource])에서 기록 시점에 읽어
 * 붙이므로, 호출부는 "무엇이 일어났는가" 만 말하면 된다.
 *
 * [record] 는 값을 돌려주지 않고 즉시 반환한다. 전송은 IO 디스패처에서 이어 돌고, 실패해도 예외가
 * 밖으로 나오지 않는다 — 로그가 화면을 멈추게 하는 일은 없어야 한다.
 */
interface BizLogger {
    /**
     * 기록을 시작한다. 앱이 뜰 때와 다시 앞으로 나올 때 부른다.
     *
     * 여러 번 불려도 안전하다 — 이미 열려 있으면 그대로 둔다.
     */
    fun init()

    /**
     * 이벤트 한 건을 남긴다. 화면·사용자·시각을 붙여 그 자리에서 전송에 얹는다.
     *
     * [init] 전이나 [destroy] 뒤에 부르면 보낼 곳이 없어 버린다. 쌓아 두는 곳이 없으므로 전송이
     * 실패한 건도 다시 나가지 않는다 — 한 건도 잃으면 안 되는 이벤트가 생기면
     * [BizLogShooter] 구현이 재시도를 맡는다.
     *
     * @param event 무엇이 일어났는가. 이름과 속성은 feature domain 의 이벤트 팩토리가 소유한다.
     */
    fun record(event: BizEvent)

    /**
     * 기록을 닫는다. 앱이 백그라운드로 내려가는, 종료 직전의 마지막 신호에서 부른다.
     *
     * 아직 나가지 못한 전송이 끝나기를 기다린 다음 스코프를 정리한다.
     * (안드로이드는 프로세스 종료를 알려주지 않으므로, 그마저 못 하고 죽으면 그 건은 사라진다)
     */
    fun destroy()
}

/**
 * 기본 구현을 만든다.
 *
 * 구현 클래스를 내보내지 않는 것은 바깥이 [BizLogger] 계약만 보게 하려는 것이다 —
 * 디스패처가 안드로이드 모듈에서 오므로 생성자 주입을 쓸 수 없고, `:logging:data` 의 Hilt 모듈이
 * 이 함수를 `@Provides` 로 감싼다.
 */
fun createBizLogger(
    shooter: BizLogShooter,
    clock: BizLogClock,
    screenSource: BizScreenSource,
    userSource: BizUserSource,
    ioDispatcher: CoroutineDispatcher,
): BizLogger = BizLoggerImpl(shooter, clock, screenSource, userSource, ioDispatcher)
