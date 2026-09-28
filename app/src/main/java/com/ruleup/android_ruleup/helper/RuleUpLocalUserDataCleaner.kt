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

/** 로그아웃·탈퇴 때 단말에 남은 사용자 데이터를 정리한다. */
@Singleton
class RuleUpLocalUserDataCleaner
    @Inject
    constructor(
        private val geofenceRegister: GeofenceRegister,
        private val verificationLocalStore: VerificationLocalStore,
        private val observability: Observability,
        private val syncScheduler: com.ruleup.verification.domain.repository.SyncScheduler,
        private val syncGate: com.ruleup.verification.data.sync.SyncGate,
    ) : LocalUserDataCleaner {
        override suspend fun clear(): Unit =
            syncGate.exclusively {
                syncScheduler.cancel()
                runCatching { geofenceRegister.clear() }
                    .onFailure { observability.w(TAG, it) { "지오펜스 해제 실패 — 다음 콜드스타트의 reconcile 이 보정" } }
                runCatching { verificationLocalStore.clearAll() }
                    .onFailure { observability.w(TAG, it) { "수집 버퍼 정리 실패" } }
                Unit
            }
    }

@Module
@InstallIn(SingletonComponent::class)
abstract class LocalUserDataCleanerModule {
    @Binds
    @Singleton
    abstract fun bindLocalUserDataCleaner(impl: RuleUpLocalUserDataCleaner): LocalUserDataCleaner
}
