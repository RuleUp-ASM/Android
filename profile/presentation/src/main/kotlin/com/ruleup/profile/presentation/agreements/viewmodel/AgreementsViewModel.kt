package com.ruleup.profile.presentation.agreements.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.domain.entity.user.AgreementType
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.onboarding.domain.intro.repository.IntroRepository
import com.ruleup.profile.domain.entity.AgreementRevokeForbiddenException
import com.ruleup.profile.domain.entity.AgreementSubmission
import com.ruleup.profile.domain.entity.AgreementVersionMismatchException
import com.ruleup.profile.domain.repository.AccountRepository
import com.ruleup.ui.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 약관 · 개인정보 동의 관리 ViewModel.
 *
 * 토글은 **낙관적으로 반영하지 않는다** — 동의 시각이 법적 증거라, 서버가 받아들인 응답으로만
 * 화면을 갱신한다. 그 사이 그 행만 잠근다.
 */
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
                    .onFailure { dispatch(AgreementsReducerEvent.Failed(it.message ?: "동의 상태를 불러오지 못했어요")) }
            }
        }

        private fun toggle(
            type: AgreementType,
            agreed: Boolean,
        ) {
            if (currentState.submitting != null) return
            submit(listOf(AgreementSubmission(type = type, agreed = agreed, version = currentVersionOf(type))), type)
        }

        /** 재동의는 `reconsentRequired` 전부를 한 번에 보낸다 — 서버가 한 트랜잭션으로 처리한다. */
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

        /**
         * 제출에 실을 약관 버전.
         *
         * **응답의 `version` 은 사용자가 동의했던 버전이지 현행 버전이 아니다.** 그걸 그대로
         * 되보내면 재동의가 자기가 동의했던 구버전을 다시 내밀어 서버가 400
         * `AGREEMENT_VERSION_MISMATCH` 로 막는다 — 「다시 동의하기」가 영영 성공하지 못한다(ONB-16).
         *
         * 현행 버전은 진입 때 받은 `GET /intro` 가 들고 있다. 못 받았으면 폴백으로 떨어지고,
         * 그때는 서버가 재검증해 400 으로 알려 준다 — 버전을 몰라 화면을 막는 것보다 낫다.
         */
        private fun currentVersionOf(type: AgreementType): String = introRepository.lastTermsVersions().of(type)

        private fun submit(
            submissions: List<AgreementSubmission>,
            locking: AgreementType,
        ) {
            dispatch(AgreementsReducerEvent.Submitting(locking))
            viewModelScope.launch {
                runCatching { accountRepository.submitAgreements(submissions) }
                    .onSuccess {
                        // 응답은 갱신된 항목만 오므로 전체를 다시 받는다 — 부분 응답으로 화면을
                        // 덮으면 안 건드린 항목이 사라진다.
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

/** 실패 문구. 필수 약관 철회는 탈퇴로 안내해야 해서 일반 오류와 갈라 둔다. */
private val Throwable.userMessage: String
    get() =
        when (this) {
            is AgreementRevokeForbiddenException, is AgreementVersionMismatchException -> message.orEmpty()
            else -> message ?: "동의를 저장하지 못했어요"
        }
