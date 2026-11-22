package com.client.xvideos.common.translation

import com.client.xvideos.common.AppContextHolder
import com.google.android.gms.tasks.Task
import com.google.mlkit.common.MlKit
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object TextTranslator {
    private val mlKitInitialization = lazy { MlKit.initialize(AppContextHolder.applicationContext) }

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

        withContext(Dispatchers.IO) { mlKitInitialization.value }
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
