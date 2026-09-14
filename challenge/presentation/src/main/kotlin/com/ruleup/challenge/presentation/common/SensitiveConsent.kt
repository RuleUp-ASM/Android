package com.ruleup.challenge.presentation.common

import com.ruleup.challenge.domain.entity.VerificationMethod
import com.ruleup.domain.entity.user.AgreementType
import com.ruleup.onboarding.domain.intro.repository.IntroRepository
import com.ruleup.profile.domain.entity.AgreementSubmission
import com.ruleup.profile.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * 위치·건강 인증 수단을 처음 쓸 때 받는 법정 개별 동의(LOCATION_INFO·HEALTH_INFO).
 *
 * 가입 때 받지 않는 항목이라, 이 인증을 쓰는 방을 만들거나 들어가는 순간이 유일한 수집 시점이다.
 */
class SensitiveConsent
    @Inject
    constructor(
        private val accountRepository: AccountRepository,
        private val introRepository: IntroRepository,
    ) {
        /** 아직 동의받지 못한 개별 동의. 필요 없거나 이미 동의했으면 null. */
        suspend fun missingFor(method: VerificationMethod): AgreementType? {
            val type = requiredFor(method) ?: return null
            return type.takeUnless { accountRepository.getAgreements().of(it)?.agreed == true }
        }

        suspend fun agree(type: AgreementType) {
            // 받은 적 없는 항목은 조회 응답에 버전이 없다 — 인트로가 준 현행 버전으로 기록한다.
            val version =
                accountRepository.getAgreements().of(type)?.version
                    ?: introRepository.lastTermsVersions().of(type)
            accountRepository.submitAgreements(listOf(AgreementSubmission(type = type, agreed = true, version = version)))
        }

        companion object {
            fun requiredFor(method: VerificationMethod): AgreementType? =
                when (method) {
                    VerificationMethod.GPS_PRESENCE, VerificationMethod.GPS_AVOID -> AgreementType.LOCATION_INFO
                    VerificationMethod.HEALTH, VerificationMethod.SLEEP -> AgreementType.HEALTH_INFO
                    VerificationMethod.SCREEN_TIME_MAX,
                    VerificationMethod.SCREEN_TIME_MIN,
                    VerificationMethod.WAKE,
                    VerificationMethod.SELF_CHECK,
                    -> null
                }
        }
    }
