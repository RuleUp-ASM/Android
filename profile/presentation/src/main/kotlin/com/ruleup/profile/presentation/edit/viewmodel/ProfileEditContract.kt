package com.ruleup.profile.presentation.edit.viewmodel

import com.ruleup.domain.entity.category.Category
import com.ruleup.domain.entity.category.InterestLimits
import com.ruleup.profile.domain.entity.Profile
import com.ruleup.profile.presentation.common.SuspendedBlock
import com.ruleup.ui.mvi.MviEffect
import com.ruleup.ui.mvi.MviIntent
import com.ruleup.ui.mvi.ReducerEvent
import com.ruleup.ui.mvi.UiState

sealed interface ProfileEditIntent : MviIntent {
    /** 진입 */
    data object Load : ProfileEditIntent

    data class ChangeNickname(
        val nickname: String,
    ) : ProfileEditIntent

    data class ToggleCategory(
        val category: Category,
    ) : ProfileEditIntent

    /** 갤러리에서 고른 이미지 업로드 (업로드 즉시 반영). */
    data class PickImage(
        val uri: String,
    ) : ProfileEditIntent

    data object RemoveImage : ProfileEditIntent

    /** 저장 */
    data object Save : ProfileEditIntent

    data object DismissSaveBlock : ProfileEditIntent

    /** 제재 이력으로 간다 */
    data object OpenSanctionHistory : ProfileEditIntent

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
    val selectedCategories: List<Category>,
    // 카테고리 선택 상한.
    val maxSelectable: Int,
    // 닉네임 30일 제한
    val nicknameLockedDays: Int,
    val isSaving: Boolean,
    // 이미지 업로드/제거 진행 중
    val isImageBusy: Boolean,
    val errorMessage: String?,
    // 저장 진입점은 숨기지 않는다
    val saveBlock: SuspendedBlock?,
) : UiState {
    val nicknameLocked: Boolean get() = nicknameLockedDays > 0

    companion object {
        val initial =
            ProfileEditState(
                isLoading = true,
                profile = null,
                nickname = "",
                selectedCategories = emptyList(),
                maxSelectable = InterestLimits.MAX,
                nicknameLockedDays = 0,
                isSaving = false,
                isImageBusy = false,
                errorMessage = null,
                saveBlock = null,
            )
    }
}

sealed interface ProfileEditReducerEvent : ReducerEvent {
    data object Loading : ProfileEditReducerEvent

    data class SaveBlocked(
        val block: SuspendedBlock?,
    ) : ProfileEditReducerEvent

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
        val categories: List<Category>,
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

    data class Saved(
        val profile: Profile,
    ) : ProfileEditReducerEvent
}
