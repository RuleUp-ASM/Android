package com.ruleup.ui.helper

import android.content.Context
import android.util.Log
import com.ruleup.domain.helper.MessageHelper
import com.ruleup.domain.message.IconType
import com.ruleup.domain.message.MessageEffect
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class MessageHelperImpl(
    val context: Context,
) : MessageHelper {
    private val _effect = Channel<MessageEffect>(Channel.BUFFERED)
    override val effect: Flow<MessageEffect> = _effect.receiveAsFlow()

    override fun showToast(toastMsg: String) {
        emit(MessageEffect.ShowToastMsg(toastMsg))
    }

    override fun showSnackBar(
        iconType: IconType,
        messageText: String,
        callToActionText: String?,
        onClickCTA: (() -> Unit)?,
    ) {
        emit(MessageEffect.ShowSnackBarError(messageText))
    }

    override fun showSnackBar(
        iconType: IconType,
        messageRes: Int,
        callToActionText: String?,
        onClickCTA: (() -> Unit)?,
    ) {
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

    override fun showTwoButtonDialog(
        titleText: String?,
        descText: String,
        cantIgnore: Boolean,
        leftButtonText: String,
        onClickLeftButton: (() -> Unit)?,
        rightButtonText: String,
        onClickRightButton: (() -> Unit)?,
    ) {
        // TBD
    }

    private fun emit(messageEffect: MessageEffect) {
        val result = _effect.trySend(messageEffect)
        if (result.isFailure) Log.w("MessageHelper", "dropped: $effect")
    }
}
