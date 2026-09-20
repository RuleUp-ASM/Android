package com.ruleup.notification.presentation.settings.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.notification.domain.entity.NotificationGroup
import com.ruleup.notification.domain.entity.NotificationSettingsUpdate
import com.ruleup.notification.domain.repository.NotificationRepository
import com.ruleup.ui.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * 알림 설정 ViewModel.
 *
 * **낙관적으로 반영하지 않는다** — 마케팅 토글은 광고성 수신 동의와 같은 트랜잭션으로 처리되고
 * 그 시각이 법적 기록이라, 서버가 받아들인 값으로만 화면을 갱신한다.
 *
 * 알림 센터 항목은 설정 화면에 없다 — 어떤 설정으로도 적재를 막을 수 없기 때문이다.
 */
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

        /** 화면이 OS 권한을 확인해 알려 준다 — 서버는 이 값을 모른다. */
        fun onPermissionChecked(denied: Boolean) {
            dispatch(NotificationSettingsReducerEvent.PermissionChecked(denied))
        }

        private fun load() {
            dispatch(NotificationSettingsReducerEvent.Loading)
            viewModelScope.launch {
                runCatching { notificationRepository.getSettings() }
                    .onSuccess { dispatch(NotificationSettingsReducerEvent.Loaded(it)) }
                    .onFailure { dispatch(NotificationSettingsReducerEvent.Failed(it.message ?: "알림 설정을 불러오지 못했어요")) }
            }
        }

        private fun submit(update: NotificationSettingsUpdate) {
            if (currentState.submitting) return
            dispatch(NotificationSettingsReducerEvent.Submitting(true))
            viewModelScope.launch {
                runCatching { notificationRepository.updateSettings(update) }
                    .onSuccess { result ->
                        dispatch(NotificationSettingsReducerEvent.Loaded(result.settings))
                        // 마케팅은 알림이 아니라 수신 동의를 바꾼 것이다 — 그 사실을 알려 준다.
                        update.marketing?.let { agreed ->
                            emitEffect(
                                NotificationSettingsEffect.ShowMessage(
                                    consentMessage(agreed, result.marketingConsentSyncedAt),
                                ),
                            )
                        }
                    }.onFailure {
                        emitEffect(NotificationSettingsEffect.ShowMessage(it.message ?: "설정을 바꾸지 못했어요"))
                    }
                dispatch(NotificationSettingsReducerEvent.Submitting(false))
            }
        }
    }

/**
 * 그룹 하나만 담은 변경 요청.
 *
 * 리마인더는 그룹 토글이 없으므로 요청에 실을 필드가 없다 — 화면이 토글 자체를 그리지 않지만,
 * `when` 을 exhaustive 하게 두려고 빈 요청으로 떨어뜨린다.
 */
private fun NotificationGroup.update(enabled: Boolean): NotificationSettingsUpdate =
    when (this) {
        NotificationGroup.ACCOUNT -> NotificationSettingsUpdate(account = enabled)
        NotificationGroup.CHALLENGE -> NotificationSettingsUpdate(challenge = enabled)
        NotificationGroup.MARKETING -> NotificationSettingsUpdate(marketing = enabled)
        NotificationGroup.REMINDER -> NotificationSettingsUpdate()
    }

/** 광고성 수신 동의·철회는 처리 일시를 알려야 한다(정보통신망법) — 서버가 준 처리 시각을 함께 띄운다. */
internal fun consentMessage(
    agreed: Boolean,
    syncedAt: String?,
    // 처리 기준은 서버(KST)다. 기기 시간대로 옮기면 해외에서 하루 어긋난 날짜가 보인다.
    zone: ZoneId = SERVICE_ZONE,
): String {
    val action = if (agreed) "마케팅 정보 수신에 동의했어요" else "마케팅 정보 수신을 철회했어요"
    // 분 단위는 사용자가 대조할 방법이 없다 — 확인 가능한 정밀도(날짜)까지만 말한다.
    val at =
        syncedAt
            ?.let { runCatching { OffsetDateTime.parse(it).atZoneSameInstant(zone) }.getOrNull() }
            ?.format(DateTimeFormatter.ofPattern("M월 d일"))
            ?: return action
    return "$action · $at 처리됐어요"
}

private val SERVICE_ZONE: ZoneId = ZoneId.of("Asia/Seoul")
