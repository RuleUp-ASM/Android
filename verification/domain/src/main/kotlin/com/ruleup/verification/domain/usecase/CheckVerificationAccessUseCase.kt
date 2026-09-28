package com.ruleup.verification.domain.usecase

import com.ruleup.profile.domain.repository.AccountRepository
import com.ruleup.verification.domain.entity.PermissionSnapshot
import com.ruleup.verification.domain.entity.VerificationAccess
import com.ruleup.verification.domain.repository.PermissionStatusProvider
import javax.inject.Inject

class CheckVerificationAccessUseCase
    @Inject
    constructor(
        private val permissionStatusProvider: PermissionStatusProvider,
        private val accountRepository: AccountRepository,
    ) {
        suspend operator fun invoke(requiredPermissions: List<String>): VerificationAccess {
            val permissions = permissionStatusProvider.capture()
            val requiredConsents = requiredPermissions.mapNotNull(PermissionSnapshot::requiredConsentFor).distinct()
            val agreements = requiredConsents.takeIf { it.isNotEmpty() }?.let { accountRepository.getAgreements() }
            return VerificationAccess(
                permissions = permissions,
                missingPermissions = requiredPermissions.distinct().filter { permissions.isGranted(it) == false },
                missingConsents = requiredConsents.filter { agreements?.of(it)?.agreed != true },
            )
        }
    }
