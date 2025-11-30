package com.simplyawakeremake.ui

import android.content.Context
import androidx.annotation.StringRes

sealed class UiText {

    data class StringResource(
        @StringRes val resId: Int,
        val args: List<Any> = emptyList()
    ) : UiText()

    data class DynamicString(
        val value: String
    ) : UiText()

    fun asString(context: Context): String {
        return when (this) {
            is StringResource -> context.getString(resId, *args.toTypedArray())
            is DynamicString -> value
        }
    }
}
