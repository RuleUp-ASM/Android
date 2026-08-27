package com.ruleup.android_ruleup.helper

import android.content.Context
import android.util.Log
import com.ruleup.domain.helper.MessageHelper
import com.ruleup.domain.message.MessageEffect
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject

class MessageHelperImpl
    @Inject
    constructor(
        @ApplicationContext val context: Context,
    ) : MessageHelper {
        private val _effect = Channel<MessageEffect>(Channel.BUFFERED)
        override val effect: Flow<MessageEffect> = _effect.receiveAsFlow()

        override fun showToast(toastMsg: String) {
            emit(MessageEffect.ShowToastMsg(toastMsg))
        }

        override fun showSnackBar(messageText: String) {
            emit(MessageEffect.ShowSnackBarError(messageText))
        }

        override fun showSnackBar(messageRes: Int) {
            emit(MessageEffect.ShowSnackBarError(context.getString(messageRes)))
        }

        override fun showOneButtonDialog(
            titleText: String?,
            descText: String,
            cantIgnore: Boolean,
            buttonText: String,
            onClickButton: (() -> Unit)?,
        ) {
            emit(
                MessageEffect.ShowOneButtonDialog(
                    titleText = titleText,
                    descText = descText,
                    cantIgnore = cantIgnore,
                    buttonText = buttonText,
                    onClickButton = onClickButton,
                ),
            )
        }

        private fun emit(messageEffect: MessageEffect) {
            val result = _effect.trySend(messageEffect)
            if (result.isFailure) Log.w("MessageHelper", "dropped: $effect")
        }
    }
