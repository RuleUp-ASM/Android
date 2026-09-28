package com.ruleup.profile.presentation.agreements.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.domain.entity.user.AgreementType
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.onboarding.domain.intro.repository.IntroRepository
import com.ruleup.profile.domain.entity.AgreementRevokeForbiddenException
import com.ruleup.profile.domain.entity.AgreementSubmission
import com.ruleup.profile.domain.entity.AgreementVersionMismatchException
import com.ruleup.profile.domain.repository.AccountRepository
import com.ruleup.ui.error.userFacingMessage
import com.ruleup.ui.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 약관 · 개인정보 동의 관리 ViewModel. */
@HiltViewModel
class AgreementsViewModel
    @Inject
    constructor(
        private val accountRepository: AccountRepository,
        private val introRepository: IntroRepository,
        private val navigationHelper: NavigationHelper,
    ) : MviViewModel<AgreementsIntent, AgreementsState, AgreementsReducerEvent, AgreementsEffect>(
            AgreementsState.initial,
        ) {
        override fun onIntent(intent: AgreementsIntent) {
            when (intent) {
                AgreementsIntent.Load -> load()
                is AgreementsIntent.Toggle -> toggle(intent.type, intent.agreed)
                AgreementsIntent.Reconsent -> reconsent()
                AgreementsIntent.Back -> navigationHelper.navigateToBack()
            }
        }

        override fun reduce(
            state: AgreementsState,
            event: AgreementsReducerEvent,
        ): AgreementsState =
            when (event) {
                AgreementsReducerEvent.Loading -> state.copy(isLoading = true, errorMessage = null)

                is AgreementsReducerEvent.Loaded ->
                    state.copy(isLoading = false, status = event.status, errorMessage = null)

                is AgreementsReducerEvent.Failed -> state.copy(isLoading = false, errorMessage = event.message)

                is AgreementsReducerEvent.Submitting -> state.copy(submitting = event.type)

                is AgreementsReducerEvent.Reconsenting -> state.copy(isReconsenting = event.reconsenting)
            }

        private fun load() {
            dispatch(AgreementsReducerEvent.Loading)
            viewModelScope.launch {
                runCatching { accountRepository.getAgreements() }
                    .onSuccess { dispatch(AgreementsReducerEvent.Loaded(it)) }
                    .onFailure { dispatch(AgreementsReducerEvent.Failed(it.userFacingMessage("동의 상태를 불러오지 못했어요"))) }
            }
        }

        private fun toggle(
            type: AgreementType,
            agreed: Boolean,
        ) {
            if (currentState.submitting != null) return
            submit(listOf(AgreementSubmission(type = type, agreed = agreed, version = currentVersionOf(type))), type)
        }

        /** 재동의는 `reconsentRequired` 전부를 한 번에 보낸다 */
        private fun reconsent() {
            if (currentState.isReconsenting) return
            val status = currentState.status ?: return
            val submissions =
                status.reconsentRequired.map { type ->
                    AgreementSubmission(type = type, agreed = true, version = currentVersionOf(type))
                }
            if (submissions.isEmpty()) return
            dispatch(AgreementsReducerEvent.Reconsenting(true))
            viewModelScope.launch {
                runCatching { accountRepository.submitAgreements(submissions) }
                    .onSuccess { load() }
                    .onFailure { emitEffect(AgreementsEffect.ShowMessage(it.userMessage)) }
                dispatch(AgreementsReducerEvent.Reconsenting(false))
            }
        }

        /** 제출에 실을 약관 버전. */
        private fun currentVersionOf(type: AgreementType): String = introRepository.lastTermsVersions().of(type)

        private fun submit(
            submissions: List<AgreementSubmission>,
            locking: AgreementType,
        ) {
            dispatch(AgreementsReducerEvent.Submitting(locking))
            viewModelScope.launch {
                runCatching { accountRepository.submitAgreements(submissions) }
                    .onSuccess {
                        // 응답은 갱신된 항목만 오므로 전체를 다시 받는다
                        load()
                    }.onFailure {
                        emitEffect(AgreementsEffect.ShowMessage(it.userMessage))
                        // 버전이 어긋났으면 화면을 다시 받아야 다음 시도가 성공한다.
                        if (it is AgreementVersionMismatchException) load()
                    }
                dispatch(AgreementsReducerEvent.Submitting(null))
            }
        }
    }

/** 실패 문구. */
private val Throwable.userMessage: String
    get() =
        when (this) {
            is AgreementRevokeForbiddenException, is AgreementVersionMismatchException -> message.orEmpty()
            else -> message ?: "동의를 저장하지 못했어요"
        }
