package com.ruleup.profile.presentation.edit.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.domain.entity.category.Category
import com.ruleup.domain.entity.category.InterestLimits
import com.ruleup.domain.entity.user.NickNameUtil
import com.ruleup.domain.entity.user.NicknameValidation
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.profile.domain.entity.NicknameCheckReason
import com.ruleup.profile.domain.repository.AccountRepository
import com.ruleup.profile.domain.repository.ProfileRepository
import com.ruleup.profile.presentation.common.SuspendedBlock
import com.ruleup.profile.presentation.common.sanctionUntilLabel
import com.ruleup.ui.error.userFacingMessage
import com.ruleup.ui.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/** 프로필 편집 ViewModel. */
@HiltViewModel
class ProfileEditViewModel
    @Inject
    constructor(
        private val profileRepository: ProfileRepository,
        private val accountRepository: AccountRepository,
        private val navigationHelper: NavigationHelper,
    ) : MviViewModel<ProfileEditIntent, ProfileEditState, ProfileEditReducerEvent, ProfileEditEffect>(
            ProfileEditState.initial,
        ) {
        override fun onIntent(intent: ProfileEditIntent) {
            if (currentState.isSaving) return
            when (intent) {
                ProfileEditIntent.Load -> load()
                is ProfileEditIntent.ChangeNickname ->
                    dispatch(
                        ProfileEditReducerEvent.NicknameChanged(
                            intent.nickname.take(NickNameUtil.MAX_LENGTH),
                        ),
                    )

                is ProfileEditIntent.ToggleCategory -> toggleCategory(intent.category)
                is ProfileEditIntent.PickImage -> dispatch(ProfileEditReducerEvent.ImageSelected(intent.uri))
                ProfileEditIntent.RemoveImage -> dispatch(ProfileEditReducerEvent.ImageSelected(null))
                ProfileEditIntent.Save -> save()
                ProfileEditIntent.Back -> navigationHelper.navigateToBack()
                ProfileEditIntent.DismissSaveBlock -> dispatch(ProfileEditReducerEvent.SaveBlocked(null))
                ProfileEditIntent.OpenSanctionHistory -> {
                    dispatch(ProfileEditReducerEvent.SaveBlocked(null))
                    navigationHelper.navigateByRoute(NavRoute(AppRoutes.MY_SANCTIONS))
                }
            }
        }

        override fun reduce(
            state: ProfileEditState,
            event: ProfileEditReducerEvent,
        ): ProfileEditState =
            when (event) {
                ProfileEditReducerEvent.Loading -> state.copy(isLoading = true, errorMessage = null)

                is ProfileEditReducerEvent.SaveBlocked -> state.copy(saveBlock = event.block)

                is ProfileEditReducerEvent.Loaded ->
                    state.copy(
                        isLoading = false,
                        profile = event.profile,
                        nickname = event.profile.nickname,
                        selectedCategories = event.profile.interestCategories,
                        maxSelectable = event.maxSelectable,
                        nicknameLockedDays = event.nicknameLockedDays,
                        errorMessage = null,
                    )

                is ProfileEditReducerEvent.Failed -> state.copy(isLoading = false, errorMessage = event.message)

                is ProfileEditReducerEvent.NicknameChanged -> state.copy(nickname = event.nickname)

                is ProfileEditReducerEvent.CategoriesChanged -> state.copy(selectedCategories = event.categories)

                is ProfileEditReducerEvent.ImageChanged ->
                    state.copy(
                        profile = state.profile?.let { it.copy(user = it.user.copy(profileImageUrl = event.profileImageUrl)) },
                        pendingImageUri = null,
                        removeImage = false,
                    )

                is ProfileEditReducerEvent.ImageSelected ->
                    state.copy(
                        pendingImageUri = event.uri,
                        removeImage = event.uri == null && state.profile?.profileImageUrl != null,
                    )

                is ProfileEditReducerEvent.Saving -> state.copy(isSaving = event.saving, isImageBusy = event.saving)

                is ProfileEditReducerEvent.Saved ->
                    state.copy(
                        profile = event.profile,
                        nickname = event.profile.nickname,
                        selectedCategories = event.profile.interestCategories,
                        nicknameLockedDays = event.profile.nicknameChangeableAfter.remainingDays(),
                    )
            }

        private fun load() {
            if (currentState.profile != null) return
            dispatch(ProfileEditReducerEvent.Loading)
            viewModelScope.launch {
                runCatching {
                    val profileDeferred = async { profileRepository.getProfile() }
                    // 마스터 조회 실패는 기본 상한(6)으로 흡수한다.
                    val catalogDeferred = async { runCatching { profileRepository.getCategories() }.getOrNull() }
                    profileDeferred.await() to catalogDeferred.await()
                }.onSuccess { (profile, catalog) ->
                    dispatch(
                        ProfileEditReducerEvent.Loaded(
                            profile = profile,
                            maxSelectable = catalog?.maxSelectable ?: InterestLimits.MAX,
                            nicknameLockedDays = profile.nicknameChangeableAfter.remainingDays(),
                        ),
                    )
                }.onFailure {
                    dispatch(ProfileEditReducerEvent.Failed(it.userFacingMessage("프로필을 불러오지 못했어요")))
                }
            }
        }

        private fun toggleCategory(category: Category) {
            val current = currentState.selectedCategories
            val next =
                if (category in current) {
                    current - category
                } else {
                    if (current.size >= currentState.maxSelectable) {
                        emitEffect(
                            ProfileEditEffect.ShowMessage("관심 분야는 최대 ${currentState.maxSelectable}개까지 고를 수 있어요"),
                        )
                        return
                    }
                    current + category
                }
            dispatch(ProfileEditReducerEvent.CategoriesChanged(next))
        }

        private fun save() {
            val state = currentState
            val profile = state.profile ?: return
            if (state.isSaving) return

            val trimmed = state.nickname.trim()
            val nicknameChanged = !state.nicknameLocked && trimmed != profile.nickname
            val categoriesChanged = state.selectedCategories.toSet() != profile.interestCategories.toSet()

            if (!nicknameChanged && !categoriesChanged && !state.imageChanged) {
                emitEffect(ProfileEditEffect.ShowMessage("변경된 내용이 없어요"))
                return
            }
            // 온보딩과 같은 규칙으로 막는다
            val validation = NickNameUtil.validate(trimmed)
            if (nicknameChanged && !validation.isValid) {
                emitEffect(ProfileEditEffect.ShowMessage(NickNameUtil.message(validation)))
                return
            }
            if (categoriesChanged && state.selectedCategories.isEmpty()) {
                emitEffect(ProfileEditEffect.ShowMessage("관심 분야를 1개 이상 골라주세요"))
                return
            }

            dispatch(ProfileEditReducerEvent.Saving(true))
            viewModelScope
                .launch {
                    // 쓰기가 막힌 상태면 PATCH 를 보내지 않는다
                    val history = runCatching { accountRepository.getSanctions() }.getOrNull()
                    if (history?.restriction?.isFullLock == true) {
                        dispatch(
                            ProfileEditReducerEvent.SaveBlocked(
                                SuspendedBlock(until = history.activeSanction?.endsAt?.let(::sanctionUntilLabel)),
                            ),
                        )
                        return@launch
                    }
                    runCatching {
                        if (nicknameChanged) {
                            val check = profileRepository.checkNickname(trimmed)
                            if (!check.valid || !check.available) {
                                val message =
                                    when (check.reason) {
                                        NicknameCheckReason.DUPLICATED -> "이미 사용 중인 닉네임이에요"
                                        NicknameCheckReason.FORMAT -> NickNameUtil.message(NicknameValidation.INVALID_CHAR)
                                        // 최근에 해제된 닉네임은 1주간 잠긴다.
                                        NicknameCheckReason.RECENTLY_RELEASED ->
                                            check.availableAt
                                                ?.let { "최근에 해제된 닉네임이에요. $it 부터 쓸 수 있어요" }
                                                ?: "최근에 해제된 닉네임이라 잠시 쓸 수 없어요"
                                        null -> "사용할 수 없는 닉네임이에요"
                                    }
                                emitEffect(ProfileEditEffect.ShowMessage(message))
                                return@launch
                            }
                        }
                        if (nicknameChanged || categoriesChanged) {
                            val updated =
                                profileRepository.updateProfile(
                                    nickname = trimmed.takeIf { nicknameChanged },
                                    interestCategories = state.selectedCategories.takeIf { categoriesChanged },
                                )
                            dispatch(ProfileEditReducerEvent.Saved(updated))
                        }
                        if (state.pendingImageUri != null) {
                            val url = profileRepository.uploadProfileImage(state.pendingImageUri)
                            dispatch(ProfileEditReducerEvent.ImageChanged(url))
                        } else if (state.removeImage) {
                            profileRepository.deleteProfileImage()
                            dispatch(ProfileEditReducerEvent.ImageChanged(null))
                        }
                    }.onSuccess {
                        emitEffect(ProfileEditEffect.ShowMessage("프로필을 저장했어요"))
                        navigationHelper.navigateToBack()
                    }.onFailure {
                        emitEffect(ProfileEditEffect.ShowMessage(it.userFacingMessage("프로필을 저장하지 못했어요")))
                    }
                }.invokeOnCompletion { dispatch(ProfileEditReducerEvent.Saving(false)) }
        }
    }

// nicknameChangeableAfter(ISO 8601, null = 즉시 가능) → 남은 일수 (지났으면 0)
private fun String?.remainingDays(): Int {
    if (this == null) return 0
    val date =
        runCatching { OffsetDateTime.parse(this).toLocalDate() }
            .recoverCatching { LocalDate.parse(this.substringBefore('T')) }
            .getOrNull() ?: return 0
    return ChronoUnit.DAYS
        .between(LocalDate.now(), date)
        .coerceAtLeast(0)
        .toInt()
}
