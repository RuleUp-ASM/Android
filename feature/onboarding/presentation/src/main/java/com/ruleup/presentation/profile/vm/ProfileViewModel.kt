package com.ruleup.presentation.profile.vm

import androidx.lifecycle.ViewModel
import com.ruleup.core.ui.mvi.MviViewModel
import com.ruleup.domain.model.Agreements
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel
    @Inject
    constructor() :
    MviViewModel<ProfileIntent, ProfileState, ProfileReducerEvent, ProfileEffect>(ProfileState.initial) {
        override fun onIntent(intent: ProfileIntent) {
            when (intent) {
                is ProfileIntent.SetNickName -> {
`                   dispatch(ProfileReducerEvent.NicknameEntered(intent.name))
                }
                is ProfileIntent.SetProfileIcon -> {
                    dispatch(ProfileReducerEvent.ProfileImageSelected(null))
                }
                is ProfileIntent.SetProfileInterest -> {
                    dispatch(ProfileReducerEvent.InterestsSelected(intent.interestCategories))
                }
            }
        }

        override fun reduce(
            state: ProfileState,
            event: ProfileReducerEvent,
        ): ProfileState =
            when (event) {
                is ProfileReducerEvent.NicknameEntered -> {
                    state.copy(nickname = event.nickname)
                }

                is ProfileReducerEvent.AgreementsUpdated -> {
                    state.copy(agreements = event.agreements)
                }

                is ProfileReducerEvent.ProfileImageSelected -> {
                    state.copy(profileImageUrl = event.url)
                }

                is ProfileReducerEvent.InterestsSelected -> {
                    state.copy(interests = event.interests)
                }

                is ProfileReducerEvent.Submitting -> {
                    state.copy(isSubmitting = true)
                }
            }
    }
