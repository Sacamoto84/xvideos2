package com.client.xvideos.common.translation

import com.google.android.gms.tasks.Task
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object TextTranslator {
    suspend fun translate(
        text: String,
        sourceLanguage: String,
        targetLanguage: String,
        engine: TranslationEngine
    ): String {
        if (text.isBlank()) return text
        return when (engine) {
            TranslationEngine.NONE -> text
            TranslationEngine.ML_KIT -> translateWithMlKit(text, sourceLanguage, targetLanguage)
        }
    }

    private suspend fun translateWithMlKit(
        text: String,
        sourceLanguage: String,
        targetLanguage: String
    ): String {
        val source = requireNotNull(TranslateLanguage.fromLanguageTag(sourceLanguage))
        val target = requireNotNull(TranslateLanguage.fromLanguageTag(targetLanguage))
        if (source == target) return text

        val options = TranslatorOptions.Builder()
            .setSourceLanguage(source)
            .setTargetLanguage(target)
            .build()
        val downloadConditions = DownloadConditions.Builder().requireWifi().build()
        return Translation.getClient(options).use { translator ->
            translator.downloadModelIfNeeded(downloadConditions).awaitResult()
            translator.translate(text).awaitResult()
        }
    }
}

private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        val error = task.exception
        when {
            error != null -> continuation.resumeWithException(error)
            task.isCanceled -> continuation.cancel()
            else -> continuation.resume(task.result)
        }
    }
}
