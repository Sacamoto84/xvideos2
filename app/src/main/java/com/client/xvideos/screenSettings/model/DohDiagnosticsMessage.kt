package com.client.xvideos.screenSettings.model

import com.client.xvideos.common.net.doh.DohDiagnosticResult

/**
 * Итог проверки DNS для показа пользователю.
 *
 * @property text Строка итога.
 * @property isError Показывать как ошибку: имя не найдено либо его нашёл не выбранный резолвер.
 */
internal data class DohDiagnosticsMessage(val text: String, val isError: Boolean)

/**
 * Собирает итог проверки DNS по разделам.
 *
 * Имя, которое при включённом DoH нашёл системный DNS, — это отказ DoH, а не
 * успех: раньше такой итог показывался зелёным с названием провайдера, и
 * проверка не отличала работающий DoH от неработающего.
 *
 * @param results Раздел и результат поиска имени его сервера.
 */
internal fun dohDiagnosticsMessage(
    results: List<Pair<String, Result<DohDiagnosticResult>>>,
): DohDiagnosticsMessage {
    val found = results.mapNotNull { it.second.getOrNull() }
    val provider = found.firstOrNull()?.providerTitle
    val allFellBack = found.isNotEmpty() && found.all { it.usedFallback }

    val summary = results.joinToString(", ") { (section, result) ->
        result.fold(
            onSuccess = {
                val via = if (it.usedFallback && !allFellBack) " через системный DNS" else ""
                "$section ${it.elapsedMs} мс$via"
            },
            onFailure = { "$section не найден" },
        )
    }
    val text = when {
        provider == null -> "Ошибка DNS: $summary"
        allFellBack -> "$provider не ответил, имена нашёл системный DNS: $summary"
        else -> "$provider: $summary"
    }
    val isError = found.size < results.size || found.any { it.usedFallback }
    return DohDiagnosticsMessage(text, isError)
}
