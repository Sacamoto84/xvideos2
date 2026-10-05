package com.client.xvideos.r.common.saved

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.io.writeTextAtomically
import com.client.xvideos.common.json.AppJson
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.common.util.replaceWith
import com.client.xvideos.r.model.Niche
import com.client.xvideos.r.model.NichesResponse
import kotlinx.serialization.encodeToString
import com.client.xvideos.r.network.api.RedApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

/**
 * Локальный файловый кэш полного списка ниш RedGifs.
 *
 * Поскольку API RedGifs не поддерживает сортировку по названию на сервере,
 * для полноценного поиска и клиентской фильтрации каталог ниш выгружается целиком
 * и кэшируется в `AppPath.r_nichesCache/niches.json`.
 *
 * Поддерживает:
 * - Автоматическое обновление, если кэш старше 24 часов ([refreshIfStale]);
 * - Отслеживание прогресса загрузки страниц ([progress], 0..1f);
 * - Реактивное уведомление UI через Compose [list] и ключ пересчета [version].
 *
 * @property scope Скоп корутин для выполнения фоновой загрузки.
 * @property redApi Сетевой клиент RedGifs.
 */
@Stable
class R_Saved_NichesCaches(
    val scope: CoroutineScope,
    val redApi: RedApi,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val loadPage: suspend (page: Int) -> Result<NichesResponse> =
        { page -> redApi.explorer.getExplorerNiches(page = page, count = 100) },
) {

    private companion object {
        const val CACHE_FILE_NAME = "niches.json"
        const val AUTO_REFRESH_MAX_AGE_HOURS = 24L
    }

    /**
     * Был обычный `mutableListOf`: чтение из composable ничего не подписывало,
     * и рекомпозиция держалась только на ручном [version]. Любой новый читатель,
     * забывший прочитать [version], молча не обновлялся бы.
     */
    val list = mutableStateListOf<Niche>()

    /**
     * Счётчик обновлений кэша. Нужен не для подписки (её теперь даёт сам
     * [list]), а как ключ пересчёта фильтрации и сортировки в
     * `R_ScreenNichesTab`: гонять её на каждое чтение списка дорого.
     */
    var version by mutableIntStateOf(0)
        private set

    /** Флаг выполнения сетевой синхронизации кэша прямо сейчас. */
    var isDownloading by mutableStateOf(false)

    /** Прогресс скачивания страниц каталога от 0.0f до 1.0f. */
    var progress by mutableFloatStateOf(0f)

    /** Флаг наличия загруженных данных в кэше. */
    var isDownloaded by mutableStateOf(false)

    /** Время с момента последнего обновления файла кэша (в часах). */
    var lastModifiedHour by mutableLongStateOf(-1)
    /** Время с момента последнего обновления файла кэша (в минутах). */
    var lastModifiedMinute by mutableLongStateOf(-1)

    /** Идущее или последнее чтение кэша с диска: проверка свежести ждёт его. */
    private var readJob: Job = readFromDisk()

    /**
     * Загружает все страницы каталога ниш из сети, атомарно сохраняет JSON в файл
     * и обновляет список в памяти.
     *
     * @param showSnackBar Показывать ли снэкбар об успехе/ошибке (по умолчанию true).
     */
    fun refresh(showSnackBar: Boolean = true) {
        if (isDownloading) return
        isDownloading = true
        progress = 0f

        scope.launch(ioDispatcher) {
            try {
                val niches = mutableListOf<Niche>()
                // Раньше здесь стояло getOrNull()!!: при любой сетевой ошибке
                // это был NPE, и пользователь видел снекбар с текстом
                // "Ошибка обновления java.lang.NullPointerException".
                val res = loadPage(1)
                    .getOrElse { error("не удалось загрузить ниши: ${it.message ?: "нет сети"}") }
                val pages = res.pages.coerceAtLeast(1)
                val step = if (pages > 1) 1f / (pages - 1) else 1f
                niches.addAll(res.niches)
                for (i in 2..pages) {
                    delay(200)
                    // Обрываем обновление целиком: записать на диск неполный
                    // список как полный хуже, чем не обновиться вообще.
                    val res2 = loadPage(i)
                        .getOrElse { error("страница $i из $pages не загрузилась: ${it.message ?: "нет сети"}") }
                    niches.addAll(res2.niches)
                    withContext(Dispatchers.Main) {
                        progress += step
                    }
                }
                val json = AppJson.encodeToString(niches)
                cacheFile.writeTextAtomically(json)
                withContext(Dispatchers.Main) {
                    list.replaceWith(niches)
                    version++
                    timeRefresh()
                    if (showSnackBar) {
                        SnackBar.success("Обновление завершено")
                    }
                    isDownloading = false
                    isDownloaded = true
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e("R niches cache refresh error: ${e.javaClass.simpleName}")
                withContext(Dispatchers.Main) {
                    if (showSnackBar) {
                        // Текст причины, а не e.toString(): тот начинается с
                        // имени класса исключения.
                        SnackBar.error("Ошибка обновления: ${e.message ?: "неизвестная ошибка"}")
                    }
                    isDownloading = false
                }
            }
        }
    }

    private val cacheFile = File(AppPath.r_nichesCache, CACHE_FILE_NAME)

    /**
     * Проверяет возраст файла кэша ниш на диске и запускает тихое обновление,
     * если файл отсутствует, пуст, не читается или старше [maxAgeHours] часов.
     *
     * Сначала дожидается чтения кэша с диска. Раньше проверка шла сразу за его
     * запуском: список в памяти ещё пуст, кэш считался устаревшим, и каждый
     * холодный старт заново качал весь каталог — при свежем файле на диске.
     */
    fun refreshIfStale(maxAgeHours: Long = AUTO_REFRESH_MAX_AGE_HOURS) {
        scope.launch(ioDispatcher) {
            readJob.join()
            if (isDownloading) return@launch
            timeRefresh()
            val shouldRefresh = !cacheFile.exists() || list.isEmpty() || lastModifiedHour >= maxAgeHours
            if (!shouldRefresh) return@launch

            Timber.i("R niches cache auto refresh: exists=${cacheFile.exists()} size=${list.size} ageHours=$lastModifiedHour")
            refresh(showSnackBar = false)
        }
    }

    /**
     * Считывает ранее сохраненный кэш ниш с диска.
     */
    fun readFromDisk(): Job {
        val job = scope.launch(ioDispatcher) {
            if (!cacheFile.exists() || cacheFile.length() == 0L) {
                return@launch
            }
            runCatching {
                val json = cacheFile.readText()
                val niches = AppJson.decodeFromString<List<Niche>>(json)
                withContext(Dispatchers.Main) {
                    list.replaceWith(niches)
                    version++
                    timeRefresh()
                    isDownloaded = list.isNotEmpty()
                }
            }.onFailure {
                Timber.e(it, "R niches cache read error")
                withContext(Dispatchers.Main) {
                    list.clear()
                    version++
                    isDownloaded = false
                }
            }
        }
        readJob = job
        return job
    }

    /** Пересчитывает прошедшее время с момента последнего сохранения файла кэша. */
    private fun timeRefresh() {
        if (!cacheFile.exists()) {
            lastModifiedHour = -1
            lastModifiedMinute = -1
            return
        }
        // Получаем время последней модификации
        val lastModified = cacheFile.lastModified() // время в миллисекундах с эпохи
        val now = System.currentTimeMillis()
        val diffMillis = now - lastModified
        lastModifiedMinute = diffMillis / (60 * 1000)
        lastModifiedHour = diffMillis / (60 * 60 * 1000)
    }
}
