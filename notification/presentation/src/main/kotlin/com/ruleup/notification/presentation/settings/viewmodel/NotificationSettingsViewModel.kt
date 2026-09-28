package com.ruleup.notification.presentation.settings.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.notification.domain.entity.NotificationGroup
import com.ruleup.notification.domain.entity.NotificationSettingsUpdate
import com.ruleup.notification.domain.repository.NotificationRepository
import com.ruleup.ui.error.userFacingMessage
import com.ruleup.ui.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/** 알림 설정 ViewModel. */
@HiltViewModel
class NotificationSettingsViewModel
    @Inject
    constructor(
        private val notificationRepository: NotificationRepository,
        private val navigationHelper: NavigationHelper,
    ) : MviViewModel<
            NotificationSettingsIntent,
            NotificationSettingsState,
            NotificationSettingsReducerEvent,
            NotificationSettingsEffect,
        >(
            NotificationSettingsState.initial,
        ) {
        override fun onIntent(intent: NotificationSettingsIntent) {
            when (intent) {
                NotificationSettingsIntent.Load -> load()

                is NotificationSettingsIntent.ToggleMaster ->
                    submit(NotificationSettingsUpdate(pushEnabled = intent.enabled))

                is NotificationSettingsIntent.ToggleGroup -> submit(intent.group.update(intent.enabled))

                NotificationSettingsIntent.OpenSystemSettings ->
                    emitEffect(NotificationSettingsEffect.OpenSystemSettings)

                NotificationSettingsIntent.Back -> navigationHelper.navigateToBack()
            }
        }

        override fun reduce(
            state: NotificationSettingsState,
            event: NotificationSettingsReducerEvent,
        ): NotificationSettingsState =
            when (event) {
                NotificationSettingsReducerEvent.Loading -> state.copy(isLoading = true, errorMessage = null)

                is NotificationSettingsReducerEvent.Loaded ->
                    state.copy(isLoading = false, settings = event.settings, errorMessage = null)

                is NotificationSettingsReducerEvent.Failed ->
                    state.copy(isLoading = false, errorMessage = event.message)

                is NotificationSettingsReducerEvent.Submitting -> state.copy(submitting = event.submitting)

                is NotificationSettingsReducerEvent.PermissionChecked ->
                    state.copy(systemPermissionDenied = event.denied)
            }

        /** 화면이 OS 권한을 확인해 알려 준다 */
        fun onPermissionChecked(denied: Boolean) {
            dispatch(NotificationSettingsReducerEvent.PermissionChecked(denied))
        }

        private fun load() {
            dispatch(NotificationSettingsReducerEvent.Loading)
            viewModelScope.launch {
                runCatching { notificationRepository.getSettings() }
                    .onSuccess { dispatch(NotificationSettingsReducerEvent.Loaded(it)) }
                    .onFailure { dispatch(NotificationSettingsReducerEvent.Failed(it.userFacingMessage("알림 설정을 불러오지 못했어요"))) }
            }
        }

        private fun submit(update: NotificationSettingsUpdate) {
            if (currentState.submitting) return
            dispatch(NotificationSettingsReducerEvent.Submitting(true))
            viewModelScope.launch {
                runCatching { notificationRepository.updateSettings(update) }
                    .onSuccess { result ->
                        dispatch(NotificationSettingsReducerEvent.Loaded(result.settings))
                        // 마케팅은 알림이 아니라 수신 동의를 바꾼 것이다
                        update.marketing?.let { agreed ->
                            emitEffect(
                                NotificationSettingsEffect.ShowMessage(
                                    consentMessage(agreed, result.marketingConsentSyncedAt),
                                ),
                            )
                        }
                    }.onFailure {
                        emitEffect(NotificationSettingsEffect.ShowMessage(it.userFacingMessage("설정을 바꾸지 못했어요")))
                    }
                dispatch(NotificationSettingsReducerEvent.Submitting(false))
            }
        }
    }

/** 그룹 하나만 담은 변경 요청. */
private fun NotificationGroup.update(enabled: Boolean): NotificationSettingsUpdate =
    when (this) {
        NotificationGroup.ACCOUNT -> NotificationSettingsUpdate(account = enabled)
        NotificationGroup.CHALLENGE -> NotificationSettingsUpdate(challenge = enabled)
        NotificationGroup.MARKETING -> NotificationSettingsUpdate(marketing = enabled)
        NotificationGroup.REMINDER -> NotificationSettingsUpdate()
    }

/** 광고성 수신 동의·철회는 처리 일시를 알려야 한다(정보통신망법) */
internal fun consentMessage(
    agreed: Boolean,
    syncedAt: String?,
    // 처리 기준은 서버(KST)다.
    zone: ZoneId = SERVICE_ZONE,
): String {
    val action = if (agreed) "마케팅 정보 수신에 동의했어요" else "마케팅 정보 수신을 철회했어요"
    // 분 단위는 사용자가 대조할 방법이 없다
    val at =
        syncedAt
            ?.let { runCatching { OffsetDateTime.parse(it).atZoneSameInstant(zone) }.getOrNull() }
            ?.format(DateTimeFormatter.ofPattern("M월 d일"))
            ?: return action
    return "$action · $at 처리됐어요"
}

private val SERVICE_ZONE: ZoneId = ZoneId.of("Asia/Seoul")
