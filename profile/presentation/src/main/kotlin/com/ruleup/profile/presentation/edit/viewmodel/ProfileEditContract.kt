package com.ruleup.profile.presentation.edit.viewmodel

import com.ruleup.domain.entity.user.InterestCategory
import com.ruleup.domain.entity.user.Profile
import com.ruleup.ui.mvi.MviEffect
import com.ruleup.ui.mvi.MviIntent
import com.ruleup.ui.mvi.ReducerEvent
import com.ruleup.ui.mvi.UiState

sealed interface ProfileEditIntent : MviIntent {
    /** 진입 — 내 프로필 + 카테고리 마스터 조회로 폼 프리필. */
    data object Load : ProfileEditIntent

    data class ChangeNickname(
        val nickname: String,
    ) : ProfileEditIntent

    data class ToggleCategory(
        val category: InterestCategory,
    ) : ProfileEditIntent

    /** 갤러리에서 고른 이미지 업로드 (업로드 즉시 반영). */
    data class PickImage(
        val uri: String,
    ) : ProfileEditIntent

    /** 프로필 사진 제거. */
    data object RemoveImage : ProfileEditIntent

    /** 저장 — 닉네임 변경 시 선검사(4.6) 후 변경 필드만 PATCH. */
    data object Save : ProfileEditIntent

    data object Back : ProfileEditIntent
}

sealed interface ProfileEditEffect : MviEffect {
    data class ShowMessage(
        val message: String,
    ) : ProfileEditEffect
}

data class ProfileEditState(
    val isLoading: Boolean,
    // 서버 프로필 원본 (저장 시 변경 여부 비교 기준)
    val profile: Profile?,
    val nickname: String,
    val selectedCategories: List<InterestCategory>,
    // 카테고리 선택 상한 (마스터 조회, 기본 6)
    val maxSelectable: Int,
    // 닉네임 30일 제한 — 변경 가능일까지 남은 일수 (0 = 변경 가능)
    val nicknameLockedDays: Int,
    val isSaving: Boolean,
    // 이미지 업로드/제거 진행 중
    val isImageBusy: Boolean,
    val errorMessage: String?,
) : UiState {
    val nicknameLocked: Boolean get() = nicknameLockedDays > 0

    companion object {
        const val NICKNAME_MAX_LENGTH = 12

        val initial =
            ProfileEditState(
                isLoading = true,
                profile = null,
                nickname = "",
                selectedCategories = emptyList(),
                maxSelectable = 6,
                nicknameLockedDays = 0,
                isSaving = false,
                isImageBusy = false,
                errorMessage = null,
            )
    }
}

sealed interface ProfileEditReducerEvent : ReducerEvent {
    data object Loading : ProfileEditReducerEvent

    data class Loaded(
        val profile: Profile,
        val maxSelectable: Int,
        val nicknameLockedDays: Int,
    ) : ProfileEditReducerEvent

    data class Failed(
        val message: String,
    ) : ProfileEditReducerEvent

    data class NicknameChanged(
        val nickname: String,
    ) : ProfileEditReducerEvent

    data class CategoriesChanged(
        val categories: List<InterestCategory>,
    ) : ProfileEditReducerEvent

    data class ImageBusy(
        val busy: Boolean,
    ) : ProfileEditReducerEvent

    /** 업로드/제거 후 서버 반영 결과 URL (제거면 null). */
    data class ImageChanged(
        val profileImageUrl: String?,
    ) : ProfileEditReducerEvent

    data class Saving(
        val saving: Boolean,
    ) : ProfileEditReducerEvent

    /** 저장 성공 — 원본 프로필 갱신. */
    data class Saved(
        val profile: Profile,
    ) : ProfileEditReducerEvent
}
