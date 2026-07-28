package com.ruleup.observability.data

import com.ruleup.observability.domain.port.Sink
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds

/**
 * 다른 모듈이 출구를 더할 수 있게 여는 주입구.
 *
 * `@Multibinds` 선언이 있어야 **기여자가 하나도 없을 때 빈 집합**이 주입된다. 없으면 릴리스
 * 빌드처럼 아무도 `@IntoSet Sink` 를 내놓지 않는 변형에서 Hilt 가 바인딩 누락으로 실패한다.
 *
 * 현재 기여자는 `:observability:debug` 의 인스펙터 하나이며, `:app` 이 `debugImplementation` 으로
 * 물기 때문에 릴리스에서는 자동으로 비어 있다.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ExtraSinksModule {
    @Multibinds
    abstract fun extraSinks(): Set<Sink>
}
