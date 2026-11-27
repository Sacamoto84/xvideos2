package com.client.xvideos.common.translation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

@Composable
fun rememberEnglishToRussianTranslation(original: String, engine: TranslationEngine): String {
    if (engine == TranslationEngine.NONE || original.none { it in 'a'..'z' || it in 'A'..'Z' }) {
        return original
    }

    var displayedText by remember(original, engine) { mutableStateOf(original) }
    LaunchedEffect(original, engine) {
        try {
            val translation = withContext(Dispatchers.IO) {
                TextTranslator.translate(original, sourceLanguage = "en", targetLanguage = "ru", engine = engine)
            }
            if (translation.isNotBlank()) displayedText = translation
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            Timber.d(failure, "Text translation failed")
        }
    }
    return displayedText
}
