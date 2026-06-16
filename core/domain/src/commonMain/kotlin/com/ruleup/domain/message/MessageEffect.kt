package com.ruleup.domain.message

sealed interface MessageEffect {
    data class ShowToastMsg(
        val message: String,
    ) : MessageEffect

    data class ShowSnackBarError(
        val message: String,
    ) : MessageEffect

    data class ShowOneButtonDialog(
        val titleText: String?,
        val descText: String,
        val cantIgnore: Boolean,
        val buttonText: String,
        val onClickButton: (() -> Unit)?,
    ) : MessageEffect
}
