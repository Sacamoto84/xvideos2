package com.client.xvideos.common.ui

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PlatformImeOptions
import com.client.xvideos.common.settings.Settings

/**
 * Хелпер для создания [KeyboardOptions] с поддержкой инкогнито-режима ввода.
 *
 * Передаёт клавиатуре (Gboard, SwiftKey, Samsung Keyboard и др.) запрет на
 * сохранение слов в личный пользовательский словарь, отключает автоисправление
 * и микрофон для конфиденциального поиска.
 */
object IncognitoKeyboard {

    private const val PRIVATE_IME_OPTIONS =
        "com.google.android.inputmethod.latin.noMicrophoneKey,nm,noPersonalizedLearning"

    fun options(
        capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
        autoCorrectEnabled: Boolean = false,
        keyboardType: KeyboardType = KeyboardType.Text,
        imeAction: ImeAction = ImeAction.Default,
        forceIncognito: Boolean = false
    ): KeyboardOptions {
        val isIncognito = forceIncognito || (Settings.isInitialized && Settings.keyboard_incognito_enabled.field.value)
        return if (isIncognito) {
            KeyboardOptions(
                capitalization = capitalization,
                autoCorrectEnabled = false,
                keyboardType = keyboardType,
                imeAction = imeAction,
                platformImeOptions = PlatformImeOptions(
                    privateImeOptions = PRIVATE_IME_OPTIONS
                )
            )
        } else {
            KeyboardOptions(
                capitalization = capitalization,
                autoCorrectEnabled = autoCorrectEnabled,
                keyboardType = keyboardType,
                imeAction = imeAction
            )
        }
    }
}
