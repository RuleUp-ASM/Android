package com.ruleup.android_ruleup.helper

import com.ruleup.domain.helper.LocalUserDataCleaner
import com.ruleup.observability.domain.api.Observability
import com.ruleup.observability.domain.api.w
import com.ruleup.verification.domain.repository.GeofenceRegister
import com.ruleup.verification.domain.repository.VerificationLocalStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "[Logout]"

/**
 * 로그아웃·탈퇴 때 단말에 남은 사용자 데이터를 정리한다.
 *
 * **OS 지오펜스 해제를 먼저 한다.** 로컬 목표를 먼저 지우면 무엇을 해제해야 하는지 알 수 없는
 * 펜스가 OS 에 남아, 계정이 바뀐 뒤에도 계속 울린다.
 *
 * 각 단계를 따로 감싼다 — 앞 단계가 실패했다고 뒤를 건너뛰면 지울 수 있었던 것까지 남는다.
 */
@Singleton
class RuleUpLocalUserDataCleaner
    @Inject
    constructor(
        private val geofenceRegister: GeofenceRegister,
        private val verificationLocalStore: VerificationLocalStore,
        private val observability: Observability,
    ) : LocalUserDataCleaner {
        override suspend fun clear() {
            runCatching { geofenceRegister.clear() }
                .onFailure { observability.w(TAG, it) { "지오펜스 해제 실패 — 다음 콜드스타트의 reconcile 이 보정" } }
            runCatching { verificationLocalStore.clearAll() }
                .onFailure { observability.w(TAG, it) { "수집 버퍼 정리 실패" } }
        }
    }

@Module
@InstallIn(SingletonComponent::class)
abstract class LocalUserDataCleanerModule {
    @Binds
    @Singleton
    abstract fun bindLocalUserDataCleaner(impl: RuleUpLocalUserDataCleaner): LocalUserDataCleaner
}
