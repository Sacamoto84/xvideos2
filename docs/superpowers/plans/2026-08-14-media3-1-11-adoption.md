# Media3 1.11.0 adoption Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Внедрить полезные для проекта новшества Media3 1.11.0: пул плееров + preload в вертикальной ленте R, Ktor-DataSource вместо `DefaultHttpDataSource`, Compose-поверхность вместо `PlayerView`/`AndroidView`, и выкинуть неиспользуемые media3-артефакты.

**Architecture:** Четыре независимые фазы. Фаза A — чистка зависимостей (ничего не ломает, ни от чего не зависит). Фаза B — новый слой `common/videoplayer/feed` в `:core` (`PlayerPool` из `media3-common-ktx` + `DefaultPreloadManager` + общий `SimpleCache`), который заменяет «плеер на каждую страницу пейджера» в `feature-r`; интерфейс `PlayerControls` и вся оверлей-логика экрана остаются прежними. Фаза C — единая `HttpDataSource.Factory` поверх Ktor-клиента. Фаза D — `ContentFrame`/`PlayerSurface` вместо `PlayerView` в общем `CMPPlayer2`, удаление мёртвого XML-лейаута.

Каждая фаза самостоятельна и мержится отдельно. Порядок A → B → C → D; C и D можно менять местами.

**Tech Stack:** Kotlin, Jetpack Compose, AndroidX Media3 1.11.0 (`exoplayer`, `common-ktx`, `ui-compose`, `datasource-ktor`, `database`), Ktor 3.5.2 (движок OkHttp), Voyager, Hilt, Paging 3.

**Проверенные факты об API 1.11.0** (получены распаковкой aar из `~/.gradle/caches` и исходников тега `1.11.0`):

- `androidx.media3.common.PlayerPool<T : Player>(poolCapacity: Int, playerFactory: () -> T)` — артефакт `media3-common-ktx`. Методы: `suspend acquire()`, `yield(player)`, `executeForAll {}`, `executeForAcquired {}`, `release()`. Все вызовы — только с главного потока.
- `androidx.media3.ui.compose.lifecycle.rememberPooledPlayer(mediaItem, playerPool, playerSetup, playerTeardown)` — артефакт `media3-ui-compose`, возвращает `T?`.
- `androidx.media3.ui.compose.lifecycle.SlidingWindowEffect(itemCountProvider, currentItemProvider, maxLookbehind, maxLookahead, batchSize, prefetchDistance = 2, onRangeEnterWindow, onRangeLeaveWindow)`.
- `androidx.media3.ui.compose.ContentFrame(player, modifier, surfaceType, contentScale, keepContentOnReset, shutter)` и `PlayerSurface(player, modifier, surfaceType)`.
- `DefaultPreloadManager.Builder(context, targetPreloadStatusControl)` + `.setLoadControl(..)`, `.setCache(..)`, `.setDataSourceFactory(..)`, `.buildExoPlayer()`, `.build()`; статусы — `DefaultPreloadManager.PreloadStatus.specifiedRangeLoaded(durationMs)` / `.specifiedRangeCached(durationMs)`.
- `androidx.media3.datasource.ktor.KtorDataSource.Factory(httpClient, userAgent = null, contentTypePredicate = null, transferListener = null)` + `setDefaultRequestProperties(map)`. Таймаутов у фабрики нет — их задаёт сам Ktor-клиент.
- Эталон интеграции: `demos/compose/.../shortform/ShortFormState.kt` и `demos/compose/.../layout/ShortFormPlayerScreen.kt` в репозитории `androidx/media` (тег 1.11.0).

**Команды проверки (Windows PowerShell, из корня репозитория):**

Сборка:
```
.\gradlew.bat assembleDebug
```

Юнит-тесты `:core`:
```
.\gradlew.bat :core:testDebugUnitTest
```

**Дисциплина коммитов:** в рабочем дереве есть незакоммиченные изменения (`.idea/*`, `gradle/libs.versions.toml` с апгрейдом версий). Каждый коммит стейджит **только** файлы своей задачи, перечисленные в шаге «Commit». `.idea/*` не трогаем.

**Тестовая база:** в проекте только JVM-юнит-тесты (JUnit4 + `kotlinx-coroutines-test`), без Robolectric и без инструментальных тестов. Поэтому TDD применяется к чистой логике (Задача 3); для Compose/ExoPlayer-обвязки проверка = компиляция + ручной прогон на устройстве по чек-листу.

---

## Фаза A — чистка media3-зависимостей

### Task 1: Убрать неиспользуемые media3-артефакты

**Files:**
- Modify: `core/build.gradle:86-102`
- Modify: `gradle/libs.versions.toml:101-117`

Обоснование: в `:core` через `api` экспортируется весь набор media3, но в коде нет ни одного импорта `androidx.media3.session`, `...cast`, `...ui.leanback`, `...exoplayer.rtsp`, `...exoplayer.workmanager`, `...transformer`, `...ui.compose`. DASH/SmoothStreaming-источники тоже не создаются (проверяется в шаге 1). `media3-ui-compose` возвращается обратно в Задаче 2 — он там реально нужен.

- [ ] **Step 1: Убедиться, что DASH/SmoothStreaming действительно не нужны**

Выполнить поиск (инструментом Grep, glob `**/src/**/*.kt`):
- шаблон `DashMediaSource|SsMediaSource|\.mpd|setMediaItem\(` — ожидаемо 0 совпадений.

Если совпадений 0 — удалять `dash`/`smoothstreaming` вместе с остальными (шаг 2). Если совпадения есть — оставить обе строки `dash`/`smoothstreaming` в шаге 2 нетронутыми, всё остальное удалить как написано.

- [ ] **Step 2: Удалить строки из `core/build.gradle`**

Удалить из блока `dependencies` следующие строки:

```groovy
    api libs.androidx.media3.exoplayer.dash
    api libs.androidx.media3.exoplayer.smoothstreaming
    api libs.androidx.media3.exoplayer.workmanager
    api libs.androidx.media3.ui.leanback
    api libs.androidx.media3.session
    api libs.androidx.media3.cast
    api libs.androidx.media3.transformer
    api libs.androidx.media3.exoplayer.rtsp
    api(libs.androidx.media3.ui.compose)
```

После правки блок media3 должен выглядеть так:

```groovy
    api libs.androidx.media3.ui
    api libs.androidx.media3.exoplayer
    api libs.androidx.media3.exoplayer.hls
    api libs.androidx.media3.common
    api libs.androidx.media3.extractor
    api libs.androidx.media3.decoder
    api libs.androidx.media3.datasource
    api libs.androidx.media3.effect
```

- [ ] **Step 3: Удалить неиспользуемые алиасы из `gradle/libs.versions.toml`**

Удалить строки:

```toml
androidx-media3-cast = { module = "androidx.media3:media3-cast", version.ref = "media3" }
androidx-media3-exoplayer-dash = { module = "androidx.media3:media3-exoplayer-dash", version.ref = "media3" }
androidx-media3-exoplayer-rtsp = { module = "androidx.media3:media3-exoplayer-rtsp", version.ref = "media3" }
androidx-media3-exoplayer-workmanager = { module = "androidx.media3:media3-exoplayer-workmanager", version.ref = "media3" }
androidx-media3-session = { module = "androidx.media3:media3-session", version.ref = "media3" }
androidx-media3-transformer = { module = "androidx.media3:media3-transformer", version.ref = "media3" }
androidx-media3-ui-leanback = { module = "androidx.media3:media3-ui-leanback", version.ref = "media3" }
androidx-media3-exoplayer-smoothstreaming = { group = "androidx.media3", name = "media3-exoplayer-smoothstreaming", version.ref = "media3" }
```

`androidx-media3-ui-compose` в toml **оставить** — он понадобится в Задаче 2.

- [ ] **Step 4: Собрать**

Run: `.\gradlew.bat assembleDebug`
Expected: `BUILD SUCCESSFUL`. Если сборка падает на неразрешённой ссылке `libs.androidx.media3.<что-то>` — значит алиас всё-таки используется; вернуть соответствующую строку в `core/build.gradle` и в toml, затем пересобрать.

- [ ] **Step 5: Commit**

```bash
git add core/build.gradle gradle/libs.versions.toml
git commit -m "chore(core): убрать неиспользуемые артефакты media3"
```

---

## Фаза B — пул плееров и preload в ленте R

### Task 2: Подключить артефакты для пула и предзагрузки

**Files:**
- Modify: `gradle/libs.versions.toml` (секция `[libraries]`)
- Modify: `core/build.gradle` (блок media3 в `dependencies`)

- [ ] **Step 1: Добавить алиасы в `gradle/libs.versions.toml`**

В секцию `[libraries]`, рядом с остальными `androidx-media3-*`, добавить:

```toml
androidx-media3-common-ktx = { module = "androidx.media3:media3-common-ktx", version.ref = "media3" }
androidx-media3-database = { module = "androidx.media3:media3-database", version.ref = "media3" }
```

(`androidx-media3-ui-compose` уже есть в файле.)

- [ ] **Step 2: Добавить зависимости в `core/build.gradle`**

В блок media3 добавить три строки:

```groovy
    api libs.androidx.media3.common.ktx
    api libs.androidx.media3.database
    api(libs.androidx.media3.ui.compose)
```

- [ ] **Step 3: Собрать**

Run: `.\gradlew.bat assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add core/build.gradle gradle/libs.versions.toml
git commit -m "build(core): подключить media3-common-ktx, media3-database и ui-compose"
```

---

### Task 3: Политика приоритетов предзагрузки (TDD)

**Files:**
- Create: `core/src/main/java/com/client/xvideos/common/videoplayer/feed/FeedPreloadPolicy.kt`
- Test: `core/src/test/java/com/client/xvideos/common/videoplayer/feed/FeedPreloadPolicyTest.kt`

Это единственная часть фазы, которая тестируется на JVM: чистая функция «насколько глубоко греть элемент ленты по расстоянию от текущей страницы». Media3-типов здесь нет намеренно — маппинг в `DefaultPreloadManager.PreloadStatus` живёт в Задаче 4.

- [ ] **Step 1: Написать падающий тест**

Создать `core/src/test/java/com/client/xvideos/common/videoplayer/feed/FeedPreloadPolicyTest.kt`:

```kotlin
package com.client.xvideos.common.videoplayer.feed

import org.junit.Assert.assertEquals
import org.junit.Test

class FeedPreloadPolicyTest {

    @Test
    fun `текущая страница и соседи греются полностью`() {
        assertEquals(FeedPreloadTier.NEAR_LOADED, FeedPreloadPolicy.tierFor(5, 5, 3))
        assertEquals(FeedPreloadTier.NEAR_LOADED, FeedPreloadPolicy.tierFor(4, 5, 3))
        assertEquals(FeedPreloadTier.NEAR_LOADED, FeedPreloadPolicy.tierFor(6, 5, 3))
    }

    @Test
    fun `в пределах ёмкости пула греем коротко`() {
        assertEquals(FeedPreloadTier.FAR_LOADED, FeedPreloadPolicy.tierFor(7, 5, 3))
        assertEquals(FeedPreloadTier.FAR_LOADED, FeedPreloadPolicy.tierFor(8, 5, 3))
        assertEquals(FeedPreloadTier.FAR_LOADED, FeedPreloadPolicy.tierFor(2, 5, 3))
    }

    @Test
    fun `дальше ёмкости пула только кеш на диск`() {
        assertEquals(FeedPreloadTier.CACHED_ONLY, FeedPreloadPolicy.tierFor(9, 5, 3))
        assertEquals(FeedPreloadTier.CACHED_ONLY, FeedPreloadPolicy.tierFor(0, 5, 3))
    }

    @Test
    fun `до первого свайпа текущая страница неизвестна и ничего не греем`() {
        assertEquals(FeedPreloadTier.CACHED_ONLY, FeedPreloadPolicy.tierFor(0, -1, 3))
        assertEquals(FeedPreloadTier.CACHED_ONLY, FeedPreloadPolicy.tierFor(1, -1, 3))
    }

    @Test
    fun `длительности прогрева убывают с расстоянием`() {
        assertEquals(3_000L, FeedPreloadPolicy.NEAR_LOADED_MS)
        assertEquals(1_000L, FeedPreloadPolicy.FAR_LOADED_MS)
        assertEquals(5_000L, FeedPreloadPolicy.CACHED_ONLY_MS)
    }
}
```

- [ ] **Step 2: Запустить тест и убедиться, что он падает**

Run: `.\gradlew.bat :core:testDebugUnitTest --tests "*FeedPreloadPolicyTest*"`
Expected: FAIL — компиляция теста не проходит, `Unresolved reference: FeedPreloadPolicy`.

- [ ] **Step 3: Написать минимальную реализацию**

Создать `core/src/main/java/com/client/xvideos/common/videoplayer/feed/FeedPreloadPolicy.kt`:

```kotlin
package com.client.xvideos.common.videoplayer.feed

import kotlin.math.abs

/** Насколько глубоко готовим элемент ленты к воспроизведению. */
enum class FeedPreloadTier {
    /** Текущая страница и прямые соседи: держим готовый к старту отрезок в памяти. */
    NEAR_LOADED,

    /** Ещё в пределах ёмкости пула: короткий отрезок, чтобы свайп не упирался в сеть. */
    FAR_LOADED,

    /** Далеко: только тянем начало файла в дисковый кеш, память не занимаем. */
    CACHED_ONLY,
}

/**
 * Чистая логика приоритетов предзагрузки ленты. Media3-типов тут нет намеренно:
 * так правило проверяется обычным JVM-тестом, а маппинг в
 * `DefaultPreloadManager.PreloadStatus` живёт в [FeedPlayerState].
 */
object FeedPreloadPolicy {

    const val NEAR_LOADED_MS = 3_000L
    const val FAR_LOADED_MS = 1_000L
    const val CACHED_ONLY_MS = 5_000L

    /**
     * @param itemIndex индекс элемента ленты, для которого считаем приоритет.
     * @param currentIndex индекс текущей страницы пейджера; отрицательное значение
     *        (`C.INDEX_UNSET`) означает «страница ещё не определилась».
     * @param poolCapacity размер пула плееров.
     */
    fun tierFor(itemIndex: Int, currentIndex: Int, poolCapacity: Int): FeedPreloadTier {
        if (currentIndex < 0) return FeedPreloadTier.CACHED_ONLY
        return when (abs(itemIndex - currentIndex)) {
            0, 1 -> FeedPreloadTier.NEAR_LOADED
            in 2..poolCapacity -> FeedPreloadTier.FAR_LOADED
            else -> FeedPreloadTier.CACHED_ONLY
        }
    }
}
```

- [ ] **Step 4: Запустить тест и убедиться, что он проходит**

Run: `.\gradlew.bat :core:testDebugUnitTest --tests "*FeedPreloadPolicyTest*"`
Expected: `BUILD SUCCESSFUL`, 5 тестов пройдено.

- [ ] **Step 5: Commit**

```bash
git add core/src/main/java/com/client/xvideos/common/videoplayer/feed/FeedPreloadPolicy.kt core/src/test/java/com/client/xvideos/common/videoplayer/feed/FeedPreloadPolicyTest.kt
git commit -m "feat(core): политика приоритетов предзагрузки ленты"
```

---

### Task 4: Общий кеш и состояние ленты (пул + preload-менеджер)

**Files:**
- Create: `core/src/main/java/com/client/xvideos/common/videoplayer/feed/FeedVideoCache.kt`
- Create: `core/src/main/java/com/client/xvideos/common/videoplayer/feed/FeedPlayerState.kt`

Важно: `SimpleCache` на одну папку в процессе может существовать **только в одном экземпляре** — второй бросает `IllegalStateException: Another SimpleCache instance uses the folder`. Поэтому кеш вынесен в процессный синглтон и **не освобождается** вместе с экраном.

- [ ] **Step 1: Создать процессный кеш**

Создать `core/src/main/java/com/client/xvideos/common/videoplayer/feed/FeedVideoCache.kt`:

```kotlin
package com.client.xvideos.common.videoplayer.feed

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

/**
 * Дисковый кеш предзагрузки видео.
 *
 * `SimpleCache` держит блокировку на своей папке, поэтому экземпляр в процессе
 * ровно один и живёт до смерти процесса: закрывать его вместе с экраном нельзя —
 * следующий экран не сможет открыть ту же папку.
 */
@OptIn(UnstableApi::class)
object FeedVideoCache {

    private const val DIR_NAME = "video_precache"
    private const val MAX_BYTES = 256L * 1024 * 1024

    @Volatile
    private var instance: SimpleCache? = null

    fun get(context: Context): Cache {
        instance?.let { return it }
        return synchronized(this) {
            instance ?: SimpleCache(
                File(context.applicationContext.cacheDir, DIR_NAME),
                LeastRecentlyUsedCacheEvictor(MAX_BYTES),
                StandaloneDatabaseProvider(context.applicationContext),
            ).also { instance = it }
        }
    }
}
```

- [ ] **Step 2: Создать состояние ленты**

Создать `core/src/main/java/com/client/xvideos/common/videoplayer/feed/FeedPlayerState.kt`:

```kotlin
package com.client.xvideos.common.videoplayer.feed

import android.content.Context
import androidx.annotation.MainThread
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlayerPool
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.preload.DefaultPreloadManager
import androidx.media3.exoplayer.source.preload.TargetPreloadStatusControl

/**
 * Пул плееров + менеджер предзагрузки для вертикальной ленты.
 *
 * Раньше каждая страница пейджера создавала собственный `ExoPlayer`
 * (`rememberExoPlayerWithLifecycle`), и при `beyondViewportPageCount = 2` в памяти
 * жило до пяти плееров с декодерами. Здесь плееров ровно [poolCapacity], страницы
 * берут их из [playerPool] и возвращают при уходе из композиции, а соседние
 * элементы греет [preloadManager] без своих плееров.
 *
 * Все методы — только с главного потока (требование `PlayerPool`).
 */
@MainThread
@OptIn(UnstableApi::class)
class FeedPlayerState(
    context: Context,
    private val poolCapacity: Int = DEFAULT_POOL_CAPACITY,
) {
    private val appContext = context.applicationContext

    private val statusControl =
        object : TargetPreloadStatusControl<Int, DefaultPreloadManager.PreloadStatus> {
            var currentPlayingIndex: Int = C.INDEX_UNSET

            override fun getTargetPreloadStatus(rankingData: Int): DefaultPreloadManager.PreloadStatus =
                when (FeedPreloadPolicy.tierFor(rankingData, currentPlayingIndex, poolCapacity)) {
                    FeedPreloadTier.NEAR_LOADED ->
                        DefaultPreloadManager.PreloadStatus.specifiedRangeLoaded(
                            FeedPreloadPolicy.NEAR_LOADED_MS
                        )

                    FeedPreloadTier.FAR_LOADED ->
                        DefaultPreloadManager.PreloadStatus.specifiedRangeLoaded(
                            FeedPreloadPolicy.FAR_LOADED_MS
                        )

                    FeedPreloadTier.CACHED_ONLY ->
                        DefaultPreloadManager.PreloadStatus.specifiedRangeCached(
                            FeedPreloadPolicy.CACHED_ONLY_MS
                        )
                }
        }

    private val builder: DefaultPreloadManager.Builder =
        DefaultPreloadManager.Builder(appContext, statusControl)
            .setLoadControl(
                DefaultLoadControl.Builder()
                    .setBufferDurationsMs(
                        /* minBufferMs = */ 5_000,
                        /* maxBufferMs = */ 20_000,
                        /* bufferForPlaybackMs = */ 500,
                        /* bufferForPlaybackAfterRebufferMs = */
                        DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS,
                    )
                    .setPrioritizeTimeOverSizeThresholds(true)
                    .build()
            )
            .setCache(FeedVideoCache.get(appContext))

    val playerPool: PlayerPool<ExoPlayer> = PlayerPool(poolCapacity) {
        builder.buildExoPlayer().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
            setHandleAudioBecomingNoisy(true)
            // Держим кодеки прогретыми между страницами — иначе выигрыш от пула
            // съедается пересозданием декодера на каждом свайпе.
            setForegroundMode(true)
        }
    }

    val preloadManager: DefaultPreloadManager = builder.build()

    /**
     * Ключ элемента для preload-менеджера. `mediaId` — позиция в ленте (по нему
     * менеджер удаляет элементы), `customCacheKey` — сам url, чтобы дисковый кеш
     * переживал перетасовку списка.
     */
    fun mediaItemFor(index: Int, url: String): MediaItem =
        MediaItem.Builder()
            .setUri(url)
            .setMediaId(index.toString())
            .setCustomCacheKey(url)
            .build()

    /** Источник для страницы: уже прогретый, либо добавленный сейчас. */
    fun mediaSourceFor(mediaItem: MediaItem, index: Int): MediaSource {
        preloadManager.getMediaSource(mediaItem)?.let { return it }
        preloadManager.add(mediaItem, index)
        return checkNotNull(preloadManager.getMediaSource(mediaItem)) {
            "preloadManager не отдал источник для ${mediaItem.mediaId}"
        }
    }

    fun updateCurrentPage(index: Int) {
        statusControl.currentPlayingIndex = index
        preloadManager.setCurrentPlayingIndex(index)
    }

    /** Элементы вошли в окно вокруг текущей страницы. `urlAt` возвращает null, если элемент ещё не подгружен пейджингом. */
    fun addRange(indices: IntRange, urlAt: (Int) -> String?) {
        indices.forEach { index ->
            val url = urlAt(index) ?: return@forEach
            preloadManager.add(mediaItemFor(index, url), index)
        }
        preloadManager.invalidate()
    }

    /** Элементы вышли из окна — снимаем с прогрева. */
    fun removeRange(indices: IntRange, urlAt: (Int) -> String?) {
        indices.forEach { index ->
            val url = urlAt(index) ?: return@forEach
            preloadManager.remove(mediaItemFor(index, url))
        }
    }

    /** Кеш ([FeedVideoCache]) намеренно не трогаем: он процессный. */
    fun release() {
        playerPool.release()
        preloadManager.release()
    }

    companion object {
        const val DEFAULT_POOL_CAPACITY = 3
    }
}

@Composable
fun rememberFeedPlayerState(
    poolCapacity: Int = FeedPlayerState.DEFAULT_POOL_CAPACITY,
): FeedPlayerState {
    val context = LocalContext.current
    val state = remember(context, poolCapacity) { FeedPlayerState(context, poolCapacity) }
    DisposableEffect(state) { onDispose { state.release() } }
    return state
}
```

- [ ] **Step 3: Собрать**

Run: `.\gradlew.bat assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add core/src/main/java/com/client/xvideos/common/videoplayer/feed/FeedVideoCache.kt core/src/main/java/com/client/xvideos/common/videoplayer/feed/FeedPlayerState.kt
git commit -m "feat(core): пул плееров и предзагрузка для вертикальной ленты"
```

---

### Task 5: Страница ленты на пуле плееров

**Files:**
- Create: `feature-r/src/main/java/com/client/xvideos/r/ui/video/RedPooledVideoPlayer.kt`

Сигнатура повторяет `RedVideoPlayerWithMenu` (`feature-r/src/main/java/com/client/xvideos/r/ui/video/RedVideoPlayerWithMenuContent.kt:42`) плюс два новых параметра — `feedState` и `index`. Так замена в экране будет точечной, а `ScreenRedFullScreenSM` и оверлеи не меняются вовсе.

- [ ] **Step 1: Создать композабл**

Создать `feature-r/src/main/java/com/client/xvideos/r/ui/video/RedPooledVideoPlayer.kt`:

```kotlin
package com.client.xvideos.r.ui.video

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.ContentFrame
import androidx.media3.ui.compose.lifecycle.rememberPooledPlayer
import com.client.xvideos.common.videoplayer.feed.FeedPlayerState
import com.client.xvideos.r.common.video.PlayerControls
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable

/**
 * Страница ленты, работающая на общем пуле плееров [FeedPlayerState].
 *
 * Отличие от [RedVideoPlayerWithMenu]: `ExoPlayer` не создаётся на каждую страницу,
 * а берётся из пула (`rememberPooledPlayer`) и возвращается туда же при уходе
 * страницы из композиции. Медиа-источник приходит от preload-менеджера, то есть
 * соседние ролики уже частично загружены к моменту свайпа.
 */
@OptIn(UnstableApi::class)
@Composable
fun RedPooledVideoPlayer(
    feedState: FeedPlayerState,
    index: Int,
    url: String,
    play: Boolean,
    isMute: Boolean,
    isCurrentPage: Boolean,
    autoRotate: Boolean,
    timeA: Float,
    timeB: Float,
    enableAB: Boolean,
    onChangeTime: (Pair<Float, Int>) -> Unit,
    onPlayerControlsReady: (PlayerControls) -> Unit,
    onClick: () -> Unit,
    isBuferring: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val mediaItem = remember(index, url) { feedState.mediaItemFor(index, url) }

    val player: ExoPlayer? = rememberPooledPlayer(
        mediaItem = mediaItem,
        playerPool = feedState.playerPool,
        playerSetup = { exo ->
            exo.setMediaSource(feedState.mediaSourceFor(mediaItem, index))
            exo.prepare()
        },
        playerTeardown = { exo -> exo.playWhenReady = false },
    )

    var isBuffering by remember(player) { mutableStateOf(true) }
    LaunchedEffect(isBuffering) { isBuferring(isBuffering) }

    LaunchedEffect(player, play, isCurrentPage) {
        player?.playWhenReady = play && isCurrentPage
    }

    LaunchedEffect(player, isMute) {
        player?.volume = if (isMute) 0f else 1f
    }

    LaunchedEffect(player, autoRotate) {
        val rotate = ScaleAndRotateTransformation.Builder()
            .setRotationDegrees(if (autoRotate) -90f else 0f)
            .build()
        player?.setVideoEffects(listOf(rotate))
    }

    DisposableEffect(player) {
        val exo = player
        if (exo == null) {
            onDispose { }
        } else {
            val listener = object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    isBuffering = playbackState == Player.STATE_BUFFERING
                }
            }
            exo.addListener(listener)
            isBuffering = exo.playbackState == Player.STATE_BUFFERING
            onDispose { exo.removeListener(listener) }
        }
    }

    // Время/длительность и петля A-B. Шаг 50 мс — как в прежнем CMPPlayer2,
    // чтобы поведение полосы времени и A-B не изменилось.
    LaunchedEffect(player, isCurrentPage, enableAB, timeA, timeB) {
        val exo = player ?: return@LaunchedEffect
        while (isActive) {
            val position = (exo.currentPosition / 1000f).coerceAtLeast(0f)
            val durationMs = exo.duration.takeIf { it != C.TIME_UNSET } ?: 0L
            if (isCurrentPage) onChangeTime(position to (durationMs / 1000).toInt())
            if (enableAB && position >= timeB) exo.seekTo((timeA * 1000).toLong())
            delay(50)
        }
    }

    LaunchedEffect(player, isCurrentPage) {
        val exo = player ?: return@LaunchedEffect
        if (!isCurrentPage) return@LaunchedEffect
        onPlayerControlsReady(object : PlayerControls {
            override fun forward(seconds: Float) {
                exo.seekTo(exo.currentPosition + (seconds * 1000).toLong())
            }

            override fun rewind(seconds: Float) {
                exo.seekTo((exo.currentPosition - (seconds * 1000).toLong()).coerceAtLeast(0L))
            }

            override fun seekTo(positionSeconds: Float) {
                exo.seekTo((positionSeconds * 1000).toLong().coerceAtLeast(0L))
            }

            override fun stop() {
                exo.playWhenReady = false
                exo.seekTo(0L)
            }

            override fun pause() {
                exo.playWhenReady = false
            }

            override fun play() {
                exo.playWhenReady = true
            }
        })
    }

    val zoomState = rememberZoomState(maxScale = 3f)

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        ContentFrame(
            player = player,
            modifier = Modifier
                .fillMaxSize()
                .zoomable(
                    zoomState = zoomState,
                    enableOneFingerZoom = false,
                    onTap = { onClick() },
                ),
            contentScale = ContentScale.Fit,
            keepContentOnReset = true,
        )

        if (isBuffering) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    modifier = Modifier.size(40.dp),
                    color = Color.LightGray,
                )
            }
        }
    }
}
```

- [ ] **Step 2: Собрать**

Run: `.\gradlew.bat assembleDebug`
Expected: `BUILD SUCCESSFUL`

Если компилятор ругается на `zoomable(...)`: сверить набор параметров с рабочим вызовом в `core/src/main/java/com/client/xvideos/common/videoplayer/ui/ComposeVideoPlayer.kt:52-57` и повторить его один в один.

- [ ] **Step 3: Commit**

```bash
git add feature-r/src/main/java/com/client/xvideos/r/ui/video/RedPooledVideoPlayer.kt
git commit -m "feat(r): страница ленты на пуле плееров media3"
```

---

### Task 6: Подключить пул к экрану полноэкранной ленты R

**Files:**
- Modify: `feature-r/src/main/java/com/client/xvideos/r/ui/fullscreen/ScreenRedFullScreen.kt:106-308`

- [ ] **Step 1: Добавить импорты**

В блок импортов `ScreenRedFullScreen.kt` добавить:

```kotlin
import androidx.media3.ui.compose.lifecycle.SlidingWindowEffect
import com.client.xvideos.common.videoplayer.feed.FeedPlayerState
import com.client.xvideos.common.videoplayer.feed.rememberFeedPlayerState
import com.client.xvideos.r.ui.video.RedPooledVideoPlayer
```

Импорт `com.client.xvideos.r.ui.video.RedVideoPlayerWithMenu` (строка 54) удалить.

- [ ] **Step 2: Вынести вычисление url в общую функцию**

В конец файла добавить:

```kotlin
/**
 * Адрес видео для элемента ленты: локальный файл, если ролик уже скачан,
 * иначе HLS с api.redgifs.com. Общая точка для страницы и для предзагрузки —
 * ключи preload-менеджера обязаны совпадать с тем, что реально играет плеер.
 */
private fun redVideoUrl(item: GifsInfo, vm: ScreenRedFullScreenSM): String =
    if (vm.downloadRed.downloader.findVideoInDownload(item.id, item.userName)) {
        "${AppPath.r_cache_download}/${item.userName}/${item.id}.mp4"
    } else {
        "https://api.redgifs.com/v2/gifs/${item.id.lowercase()}/hd.m3u8"
    }
```

- [ ] **Step 3: Создать состояние ленты и окно предзагрузки в `RedFullScreenFeed`**

В `RedFullScreenFeed` (строка 107) сразу после `var isVideoBuffering by remember { mutableStateOf(false) }` (строка 118) вставить:

```kotlin
    val feedState = rememberFeedPlayerState()

    SlidingWindowEffect(
        itemCountProvider = { pagerState.pageCount },
        currentItemProvider = { pagerState.settledPage },
        maxLookbehind = 2,
        maxLookahead = 4,
        batchSize = 3,
        onRangeEnterWindow = { range ->
            feedState.addRange(range) { i ->
                listGifs.peek(i)?.let { redVideoUrl(it, vm) }
            }
        },
        onRangeLeaveWindow = { range ->
            feedState.removeRange(range) { i ->
                listGifs.peek(i)?.let { redVideoUrl(it, vm) }
            }
        },
    )
```

`peek` вместо `get` — намеренно: он не дёргает пейджинг на подгрузку соседних страниц.

- [ ] **Step 4: Сообщать пулу текущую страницу**

В существующем `LaunchedEffect(pagerState, host)` (строка 120) внутрь `.collect { page -> ... }` первой строкой добавить:

```kotlin
                feedState.updateCurrentPage(page)
```

- [ ] **Step 5: Уменьшить окно пейджера и прокинуть параметры**

В `VerticalPager` (строка 134) заменить:

```kotlin
            beyondViewportPageCount = 2
```

на:

```kotlin
            // Плееров теперь ровно столько, сколько в пуле (3): держать в композиции
            // пять страниц незачем — прогрев соседей делает preload-менеджер.
            beyondViewportPageCount = 1
```

В обоих вызовах `RedFullScreenPage` внутри `VerticalPager` (основной — строка 141, fallback — строка 159) добавить два аргумента:

```kotlin
                    feedState = feedState,
                    index = index,
```

Для fallback-ветки (там, где `item = fallbackItem`) индекс — `index`, тот же самый параметр лямбды пейджера.

- [ ] **Step 6: Прокинуть параметры в `RedFullScreenSingle`**

В `RedFullScreenSingle` (строка 180) перед `RedFullScreenScaffold` добавить:

```kotlin
    val feedState = rememberFeedPlayerState(poolCapacity = 1)
```

и в вызов `RedFullScreenPage` (строка 188) добавить:

```kotlin
            feedState = feedState,
            index = 0,
```

- [ ] **Step 7: Переписать `RedFullScreenPage` на новый плеер**

В сигнатуру `RedFullScreenPage` (строка 247) добавить два параметра:

```kotlin
    feedState: FeedPlayerState,
    index: Int,
```

Тело `videoUri` (строки 257-264) заменить на:

```kotlin
    val videoUri = remember(item.id, item.userName) { redVideoUrl(item, vm) }
```

Вызов `RedVideoPlayerWithMenu(...)` (строки 267-294) заменить на:

```kotlin
        RedPooledVideoPlayer(
            feedState = feedState,
            index = index,
            url = videoUri,
            modifier = Modifier.padding(bottom = bottomPadding),
            play = play,
            isMute = vm.mute,
            isCurrentPage = isCurrentPage,
            autoRotate = vm.autoRotate,
            timeA = vm.timeA,
            timeB = vm.timeB,
            enableAB = vm.enableAB,
            onChangeTime = { time ->
                if (isCurrentPage) {
                    vm.currentPlayerTime = time.first
                    vm.currentPlayerDuration = time.second
                }
            },
            onPlayerControlsReady = { controls ->
                if (isCurrentPage) {
                    vm.currentPlayerControls = controls
                }
            },
            onClick = { if (isCurrentPage) vm.play = !vm.play },
            isBuferring = { buffering ->
                if (isCurrentPage) {
                    onBuffering(buffering)
                }
            },
        )
```

- [ ] **Step 8: Собрать**

Run: `.\gradlew.bat assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 9: Ручная проверка на устройстве**

Установить debug-сборку и пройти чек-лист на экране полноэкранной ленты R:
1. Открыть ленту из списка — видео стартует, полоса времени внизу движется.
2. Пролистать 10+ страниц вверх/вниз — каждое следующее видео стартует без чёрного экрана дольше ~0.5 с; звук не накладывается (играет ровно одна страница).
3. Кнопки перемотки/пауза в нижней панели работают на текущей странице.
4. Режим A-B (`vm.enableAB`) зацикливает отрезок.
5. Свернуть/развернуть приложение — воспроизведение не остаётся в фоне, при возврате продолжается.
6. Открыть ленту второй раз (выйти назад → зайти снова) — нет краша `Another SimpleCache instance uses the folder`.
7. Ролик, скачанный локально, играет с диска (проверить на элементе из «Загрузок»).

Зафиксировать результат в описании коммита, если что-то из списка не выполняется — чинить до коммита.

- [ ] **Step 10: Commit**

```bash
git add feature-r/src/main/java/com/client/xvideos/r/ui/fullscreen/ScreenRedFullScreen.kt
git commit -m "feat(r): лента использует пул плееров и предзагрузку media3"
```

---

### Task 7: Убрать осиротевший код старого плеера ленты

**Files:**
- Modify: `feature-r/src/main/java/com/client/xvideos/r/ui/video/RedVideoPlayerWithMenuContent.kt`

- [ ] **Step 1: Проверить, что `RedVideoPlayerWithMenu` больше не используется**

Grep по шаблону `RedVideoPlayerWithMenu` (glob `**/src/**/*.kt`).
Ожидается: единственное совпадение — объявление в `RedVideoPlayerWithMenuContent.kt:42`. Закомментированный вызов в `feature-r/src/main/java/com/client/xvideos/r/ui/profile/tikTok/TikTokStyleVideoFeed.kt` не считается (файл целиком закомментирован).

Если есть живые вызовы — задачу пропустить целиком и отметить все шаги как выполненные без изменений.

- [ ] **Step 2: Проверить, используется ли `CurrentTimeText`**

Grep по шаблону `CurrentTimeText` (glob `**/src/**/*.kt`).
- Если совпадение только одно (объявление в этом же файле, строка 136) — удалить файл целиком: `RedVideoPlayerWithMenuContent.kt`.
- Если есть внешние вызовы — удалить из файла только функцию `RedVideoPlayerWithMenu` (строки 40-133) и ставшие ненужными импорты, оставив `CurrentTimeText`.

- [ ] **Step 3: Собрать**

Run: `.\gradlew.bat assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add feature-r/src/main/java/com/client/xvideos/r/ui/video/
git commit -m "chore(r): удалить старый плеер ленты, заменённый пулом"
```

---

## Фаза C — Ktor DataSource

### Task 8: Перевести сетевой слой плеера на KtorDataSource

**Files:**
- Modify: `gradle/libs.versions.toml` (секция `[libraries]`)
- Modify: `core/build.gradle` (блок media3)
- Create: `core/src/main/java/com/client/xvideos/common/videoplayer/net/VideoHttpDataSource.kt`
- Modify: `core/src/main/java/com/client/xvideos/common/videoplayer/util/ExoplayerHelper.kt:67-123`
- Modify: `core/src/main/java/com/client/xvideos/common/videoplayer/feed/FeedPlayerState.kt`

Смысл: сейчас плеер ходит в сеть через `DefaultHttpDataSource` со своими таймаутами, а остальное приложение — через Ktor поверх OkHttp. Одна фабрика на Ktor даёт общие настройки (UA, таймауты, логирование) и корутины вместо блокирующих сокетов. Таймаутов у `KtorDataSource.Factory` нет — они задаются плагином `HttpTimeout` у клиента.

- [ ] **Step 1: Добавить зависимость**

В `gradle/libs.versions.toml`, секция `[libraries]`:

```toml
androidx-media3-datasource-ktor = { module = "androidx.media3:media3-datasource-ktor", version.ref = "media3" }
```

В `core/build.gradle`, блок media3:

```groovy
    api libs.androidx.media3.datasource.ktor
```

- [ ] **Step 2: Создать общий клиент и фабрику**

Создать `core/src/main/java/com/client/xvideos/common/videoplayer/net/VideoHttpDataSource.kt`:

```kotlin
package com.client.xvideos.common.videoplayer.net

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.ktor.KtorDataSource
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout

/**
 * Единый HTTP-слой для плеера: тот же Ktor + OkHttp, что и у остального
 * приложения, вместо отдельного `DefaultHttpDataSource` со своими таймаутами
 * и своим пулом соединений.
 *
 * Клиент процессный: `HttpClient` держит пул соединений, создавать его на
 * каждый плеер — терять keep-alive между роликами.
 */
@OptIn(UnstableApi::class)
object VideoHttpDataSource {

    const val USER_AGENT = "Mozilla/5.0 (Linux; Android 13) ExoPlayer/media3"

    private val client: HttpClient by lazy {
        HttpClient(OkHttp) {
            followRedirects = true
            install(HttpTimeout) {
                connectTimeoutMillis = 15_000
                socketTimeoutMillis = 15_000
                requestTimeoutMillis = 60_000
            }
        }
    }

    fun factory(headers: Map<String, String>? = null): HttpDataSource.Factory =
        KtorDataSource.Factory(httpClient = client, userAgent = USER_AGENT).apply {
            if (!headers.isNullOrEmpty()) setDefaultRequestProperties(headers)
        }
}
```

- [ ] **Step 3: Использовать фабрику в `ExoplayerHelper`**

В `core/src/main/java/com/client/xvideos/common/videoplayer/util/ExoplayerHelper.kt`:

1. Добавить импорт `import com.client.xvideos.common.videoplayer.net.VideoHttpDataSource`.
2. В `createHlsMediaSource` (строка 67) заменить тело:

```kotlin
@OptIn(UnstableApi::class)
fun createHlsMediaSource(mediaItem: MediaItem, headers: Map<String, String>?): MediaSource {
    val dataSourceFactory = VideoHttpDataSource.factory(headers)
    return HlsMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
}
```

3. В `createProgressiveMediaSource` (строка 79):

```kotlin
@OptIn(UnstableApi::class)
fun createProgressiveMediaSource(
    mediaItem: MediaItem,
    context: Context,
    headers: Map<String, String>?
): MediaSource {
    val httpDataSourceFactory = VideoHttpDataSource.factory(headers)
    return ProgressiveMediaSource.Factory(DefaultDataSource.Factory(context, httpDataSourceFactory))
        .createMediaSource(mediaItem)
}
```

4. В `createHlsMediaSourceWithDrm` (строка 96) заменить первые пять строк тела (объявление `headersMap` и `dataSourceFactory`) на:

```kotlin
    val dataSourceFactory = VideoHttpDataSource.factory(headers)
```

Остальную часть функции (DRM-менеджер и `return`) не трогать.

5. Удалить импорт `androidx.media3.datasource.DefaultHttpDataSource`, если он больше не используется в файле (проверить Grep по `DefaultHttpDataSource` в этом файле).

- [ ] **Step 4: Использовать ту же фабрику в предзагрузке**

В `core/src/main/java/com/client/xvideos/common/videoplayer/feed/FeedPlayerState.kt` добавить импорт:

```kotlin
import com.client.xvideos.common.videoplayer.net.VideoHttpDataSource
```

и в цепочку `DefaultPreloadManager.Builder(...)` после `.setCache(FeedVideoCache.get(appContext))` добавить:

```kotlin
            .setDataSourceFactory(VideoHttpDataSource.factory())
```

- [ ] **Step 5: Собрать**

Run: `.\gradlew.bat assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Ручная проверка на устройстве**

1. Лента R: HLS-ролик (`hd.m3u8`) играет, перемотка вперёд/назад работает (проверяет поддержку Range-запросов).
2. Экран X-плеера: mp4 играет, перемотка работает.
3. Локальный скачанный файл играет (идёт мимо HTTP — через `DefaultDataSource`).
4. Включить авиарежим на середине ролика — приложение не падает, показывает буферизацию (регрессия к уже закрытому падению от отказа сети, коммит `e3ceabd`).

- [ ] **Step 7: Commit**

```bash
git add gradle/libs.versions.toml core/build.gradle core/src/main/java/com/client/xvideos/common/videoplayer/net/VideoHttpDataSource.kt core/src/main/java/com/client/xvideos/common/videoplayer/util/ExoplayerHelper.kt core/src/main/java/com/client/xvideos/common/videoplayer/feed/FeedPlayerState.kt
git commit -m "feat(core): плеер ходит в сеть через KtorDataSource"
```

---

## Фаза D — Compose-поверхность вместо PlayerView

### Task 9: Перевести `CMPPlayer2` на `ContentFrame`

**Files:**
- Modify: `core/src/main/java/com/client/xvideos/common/videoplayer/util/CMPlayer2.kt` (полная замена содержимого)

Затрагивает все экраны, где используется `MediaPlayerHost` → `StaticPlayer` → `CMPPlayer2`: X-плеер, X-локальный плеер, L-полноэкранное видео, `UrlVideoLite`. Убирает `AndroidView` + `PlayerView` из этого пути.

Соответствие поведения: `resizeMode = RESIZE_MODE_FIT/ZOOM` → `ContentScale.Fit/Crop`; `setKeepContentOnPlayerReset(true)` → `keepContentOnReset = true`; `setShowBuffering(SHOW_BUFFERING_NEVER)` не нужен (индикатор рисует `ComposeVideoPlayer`); `playerView.keepScreenOn` → `LocalView.current.keepScreenOn`.

- [ ] **Step 1: Заменить содержимое `CMPlayer2.kt`**

```kotlin
package com.client.xvideos.common.videoplayer.util

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.media3.ui.compose.ContentFrame
import com.client.xvideos.common.videoplayer.host.DrmConfig
import com.client.xvideos.common.videoplayer.host.MediaPlayerError
import com.client.xvideos.common.videoplayer.model.PlayerSpeed
import com.client.xvideos.common.videoplayer.model.ScreenResize
import com.client.xvideos.common.videoplayer.rememberExoPlayerWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive

@OptIn(UnstableApi::class)
@Composable
fun CMPPlayer2(
    modifier: Modifier,
    url: String,
    isPause: Boolean,
    totalTime: (Int) -> Unit,
    currentTime: (Float) -> Unit,
    isSliding: Boolean,
    seekToTime: Float?,
    speed: PlayerSpeed,
    size: ScreenResize,
    bufferCallback: (Boolean) -> Unit,
    didEndVideo: () -> Unit,
    loop: Boolean,
    volume: Float,
    isLiveStream: Boolean,
    error: (MediaPlayerError) -> Unit,
    headers: Map<String, String>?,
    drmConfig: DrmConfig?,
    selectedQuality: VideoQuality?,
    autoRotate: Boolean, // можно менять как нужно
    poster: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val minBufferMs = 12_000
    val maxBufferMs = 45_000

    val exoPlayer = rememberExoPlayerWithLifecycle(
        url,
        context,
        isPause,
        isLiveStream,
        loop,
        headers,
        drmConfig,
        error,
        selectedQuality,
        minBufferMs = minBufferMs,
        maxBufferMs = maxBufferMs,
        bufferForPlaybackMs = 50,
        bufferForPlaybackAfterRebufferM = 100,
    )

    var isBuffering by remember { mutableStateOf(false) }

    LaunchedEffect(isBuffering) {
        bufferCallback(isBuffering)
    }

    LaunchedEffect(exoPlayer) {
        flow {
            while (isActive) {
                emit((exoPlayer.currentPosition / 1000f).coerceAtLeast(0f))
                delay(50)
            }
        }.collectLatest { currentTime(it) }
    }

    LaunchedEffect(autoRotate) {
        val rotateEffect = ScaleAndRotateTransformation.Builder()
            .setRotationDegrees(if (autoRotate) -90f else 0f).build()
        exoPlayer.setVideoEffects(listOf(rotateEffect))
    }

    // Раньше эти четыре строки жили в `update` у AndroidView. Теперь это обычные
    // эффекты: применяются при изменении своего входа, а не на каждый layout.
    LaunchedEffect(exoPlayer, isPause) { exoPlayer.playWhenReady = !isPause }
    LaunchedEffect(exoPlayer, volume) { exoPlayer.volume = volume }
    LaunchedEffect(exoPlayer, speed) { exoPlayer.setPlaybackSpeed(speed.toFloat()) }
    LaunchedEffect(exoPlayer, seekToTime) {
        seekToTime?.let { exoPlayer.seekTo((it * 1000).toLong()) }
    }

    // Экран не гасим только пока реально идёт воспроизведение.
    val view = LocalView.current
    DisposableEffect(view, isPause) {
        view.keepScreenOn = !isPause
        onDispose { view.keepScreenOn = false }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        ContentFrame(
            player = exoPlayer,
            modifier = modifier,
            contentScale = when (size) {
                ScreenResize.FIT -> ContentScale.Fit
                ScreenResize.FILL -> ContentScale.Crop
            },
            keepContentOnReset = true,
        )

        // Manage player listener and lifecycle
        DisposableEffect(key1 = exoPlayer) {
            val listener = createPlayerListener(
                isSliding,
                totalTime,
                currentTime = {},
                loadingState = { isBuffering = it },
                didEndVideo,
                error,
                poster,
                sourceUrl = url
            )

            exoPlayer.addListener(listener)

            onDispose {
                // release() выполняет создатель плеера (rememberExoPlayerWithLifecycle).
                // Здесь только снимаем слушатель и останавливаем воспроизведение.
                exoPlayer.stop()
                exoPlayer.clearMediaItems()
                exoPlayer.removeListener(listener)
            }
        }
    }
}

private fun PlayerSpeed.toFloat(): Float {
    return when (this) {
        PlayerSpeed.X0_5 -> 0.5f
        PlayerSpeed.X1 -> 1f
        PlayerSpeed.X1_5 -> 1.5f
        PlayerSpeed.X2 -> 2f
    }
}
```

- [ ] **Step 2: Собрать**

Run: `.\gradlew.bat assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Ручная проверка на устройстве**

1. Экран X-плеера (`ScreenX_VideoPlayer`): видео играет, пауза/перемотка/скорость работают, зум пальцами работает.
2. Экран X-локального видео: играет локальный файл.
3. L-полноэкранное видео: играет, переключение FIT/FILL меняет вписывание.
4. `UrlVideoLite` в списках: превью-видео проигрывается.
5. Экран не гаснет во время воспроизведения и гаснет на паузе.

- [ ] **Step 4: Commit**

```bash
git add core/src/main/java/com/client/xvideos/common/videoplayer/util/CMPlayer2.kt
git commit -m "refactor(core): CMPPlayer2 на Compose ContentFrame вместо PlayerView"
```

---

### Task 10: Удалить `PlayerView`-обвязку и мёртвый XML

**Files:**
- Delete: `core/src/main/java/com/client/xvideos/common/videoplayer/rememberPlayerView.kt`
- Delete: `app/src/main/res/layout/custom_player_controls.xml`

- [ ] **Step 1: Проверить, что `rememberPlayerView` больше нигде не вызывается**

Grep по шаблону `rememberPlayerView` (glob `**/src/**/*.kt`).
Ожидается: только объявление в удаляемом файле. Если есть внешние вызовы — задачу пропустить и отметить шаги выполненными без изменений.

- [ ] **Step 2: Проверить, что XML-лейаут не используется**

Grep по шаблону `custom_player_controls` (без фильтра по расширению, но по каталогам `*/src/*`).
Ожидается: 0 совпадений — лейаут не надувается ни из кода, ни из другого XML.

- [ ] **Step 3: Удалить файлы**

```bash
git rm core/src/main/java/com/client/xvideos/common/videoplayer/rememberPlayerView.kt app/src/main/res/layout/custom_player_controls.xml
```

- [ ] **Step 4: Собрать**

Run: `.\gradlew.bat assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git commit -m "chore: удалить PlayerView-обвязку и неиспользуемый лейаут контролов"
```

---

## Что сознательно не входит в план

- **MiniController, Cast, `onConnectAsync`, системный переключатель вывода.** В проекте нет ни одного `MediaSession`/`MediaSessionService`, фонового воспроизведения и уведомления плеера тоже нет. Эти API дают ценность только вместе с сессией — это отдельная фича («играть в фоне»), а не апгрейд 1.11.0.
- **Слот-API `Player(...)` и `PlayerDefaults.*` из `media3-ui-compose-material3`.** У проекта уже есть свои оверлеи (`FeedControls_Container_Line0`, `CanvasTimeDurationLine1`, `RedFullScreenOverlay`) с нестандартной логикой A-B и загрузок. Готовые m3-контролы их не заменяют, а параллельный набор контролов только добавит кода. Отдельные state-холдеры (`rememberPlayPauseButtonState` и т.п.) можно взять точечно позже — `media3-ui-compose` уже подключён после Задачи 2.
- **Главы MP4/M4A/M4B и HAGC (Eclipsa, ST 2094-50).** Контент проекта — короткие ролики без глав; HDR-метаданных ST 2094-50 в источниках нет.

---

## Порядок выполнения

1. Task 1 — чистка зависимостей (независима, можно мержить сразу).
2. Tasks 2-7 — пул плееров и предзагрузка в ленте R (основной выигрыш: до 5 живых `ExoPlayer` → 3, мгновенный старт при свайпе, дисковый кеш).
3. Task 8 — Ktor DataSource.
4. Tasks 9-10 — Compose-поверхность и удаление мёртвого кода.
