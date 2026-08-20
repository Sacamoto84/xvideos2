package com.client.xvideos.r.common.saved

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.common.util.replaceWith
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.client.xvideos.r.model.Niche
import com.client.xvideos.r.network.api.RedApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File

class R_Saved_NichesCaches(
    val scope: CoroutineScope,
    val redApi: RedApi,
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

    var isDownloading by mutableStateOf(false)

    var progress by mutableFloatStateOf(0f)

    var isDownloaded by mutableStateOf(false)

    var lastModifiedHour by mutableLongStateOf(-1)
    var lastModifiedMinute by mutableLongStateOf(-1)

    init {
        readFromDisk()
    }

    fun refresh(showSnackBar: Boolean = true) {
        if (isDownloading) return
        isDownloading = true
        progress = 0f

        scope.launch {
            try {
                val niches = mutableListOf<Niche>()
                // Раньше здесь стояло getOrNull()!!: при любой сетевой ошибке
                // это был NPE, и пользователь видел снекбар с текстом
                // "Ошибка обновления java.lang.NullPointerException".
                val res = redApi.explorer.getExplorerNiches(page = 1, count = 100)
                    .getOrElse { error("не удалось загрузить ниши: ${it.message ?: "нет сети"}") }
                val pages = res.pages.coerceAtLeast(1)
                val step = if (pages > 1) 1f / (pages - 1) else 1f
                niches.addAll(res.niches)
                for (i in 2..pages) {
                    delay(200)
                    // Обрываем обновление целиком: записать на диск неполный
                    // список как полный хуже, чем не обновиться вообще.
                    val res2 = redApi.explorer.getExplorerNiches(page = i, count = 100)
                        .getOrElse { error("страница $i из $pages не загрузилась: ${it.message ?: "нет сети"}") }
                    niches.addAll(res2.niches)
                    progress += step
                }
                list.replaceWith(niches)
                val gson = GsonBuilder().setPrettyPrinting().create()
                val json = gson.toJson(niches)
                val file = cacheFile()
                if (file.exists()) {
                    file.delete()
                }
                file.writeText(json)
                version++
                timeRefresh()
                if (showSnackBar) {
                    SnackBar.success("Обновление завершено")
                }
                isDownloading = false
                isDownloaded = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "R niches cache refresh error")
                if (showSnackBar) {
                    SnackBar.error("Ошибка обновления ${e}")
                }
                isDownloading = false
            }
        }
    }

    fun refreshIfStale(maxAgeHours: Long = AUTO_REFRESH_MAX_AGE_HOURS) {
        timeRefresh()
        val file = cacheFile()
        val shouldRefresh = !file.exists() || list.isEmpty() || lastModifiedHour >= maxAgeHours

        if (!shouldRefresh || isDownloading) {
            return
        }

        Timber.i("R niches cache auto refresh: exists=${file.exists()} size=${list.size} ageHours=$lastModifiedHour")
        refresh(showSnackBar = false)
    }

    fun readFromDisk() {
        val file = cacheFile()
        if (!file.exists()) {
            return
        }
        runCatching {
            val json = file.readText()
            val gson = GsonBuilder().setPrettyPrinting().create()
            val niches = gson.fromJson<List<Niche>>(json, object : TypeToken<List<Niche>>() {}.type)
            list.replaceWith(niches)
            version++
            timeRefresh()
            isDownloaded = list.isNotEmpty()
        }.onFailure {
            Timber.e(it, "R niches cache read error")
            list.clear()
            version++
            isDownloaded = false
        }
    }

    private fun timeRefresh() {
        val file = cacheFile()
        if (!file.exists()) {
            lastModifiedHour = -1
            lastModifiedMinute = -1
            return
        }
        // Получаем время последней модификации
        val lastModified = file.lastModified() // время в миллисекундах с эпохи
        val now = System.currentTimeMillis()
        val diffMillis = now - lastModified
        lastModifiedMinute = diffMillis / (60 * 1000)
        lastModifiedHour = diffMillis / (60 * 60 * 1000)
    }

    private fun cacheFile(): File {
        return File(AppPath.r_nichesCache, CACHE_FILE_NAME)
    }

}
