# Media3 Review Fixes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Закрыть все замечания кодревью ветки `feature/media3-1-11-adoption` — от утечки holder'ов в preload-менеджере до восстановления `validateDistributionUrl` в Gradle wrapper.

**Architecture:** Учёт прогретых элементов вынимается из `FeedPlayerState` в чистый generic-класс `FeedPreloadRegistry<T>` без media3-типов — он становится JVM-тестируемым, и там же чинится двойной `add()`. Определение статуса скачанности уходит с главного потока в готовый `StateFlow<Set<String>>`, который `DownloadRed` уже перестраивает при каждом изменении кеша. Окно предзагрузки начинает вестись по реальному `itemCount` пейджинга вместо фантомного `pageCount` пейджера.

**Tech Stack:** Kotlin, Jetpack Compose, media3 1.11.0 (`DefaultPreloadManager`, `PlayerPool`, `rememberPooledPlayer`, `SlidingWindowEffect`), androidx.collection 1.6.0, Paging 3.5.1, JUnit4, Gradle 9.6.1.

---

## Статус исполнения (2026-08-15)

План выполнен целиком, 17 коммитов поверх `c7c1fe7`. Собрано и проверено офлайн:
`:app:assembleDebug` + `:core:testDebugUnitTest` + `:feature-r:testDebugUnitTest` —
BUILD SUCCESSFUL, 0 failures, 0 errors. Новых тестов 12: `FeedPreloadRegistryTest` (8),
`DownloadedVideoKeysTest` (4).

Отклонения от плана — два, оба из-за окружения:

1. ~~**`distributionSha256Sum` не добавлен** (Task 1, шаг 1).~~ **Закрыто 2026-08-16 в `5a37be0`**,
   когда появилась сеть: сумма взята с официального
   `https://services.gradle.org/distributions/gradle-9.6.1-bin.zip.sha256`, wrapper её принял.
   Остальная часть Task 1 — возврат `validateDistributionUrl`, `networkTimeout`, `retries`,
   `retryBackOffMs` — была выполнена сразу; это и была регрессия B1.
2. **Ручные сценарии на устройстве не прогонялись** (шаги проверки в задачах 7, 8, 9, 11, 12
   и финальный шаг 2). `adb devices` пуст. Сценарии собраны в чеклист ниже.

Результат Task 16 (B2): комментарий в манифесте **подтверждён**, правок не потребовалось.
`FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_CONNECTED_DEVICE` и `POST_NOTIFICATIONS` из merged
manifest ушли. `WAKE_LOCK` там есть, но в утверждение комментария он не входил — его объявляет
сам `androidx.media3:media3-exoplayer:1.11.0`.

### Прогон на устройстве (2026-08-15, 2602BPC18G, Android 16)

Прогнан через adb: `uiautomator dump` для состояния экрана, `input swipe/tap` для жестов,
`logcat -b crash` и `dumpsys meminfo` для результата. Полный экран ленты открывается
**долгим нажатием** на карточку сетки (`onLongClick = onItemClick` в `LazyRow123GridItem`),
обычный тап запускает инлайн-превью.

- [x] Сетка ленты R: 3 экрана вниз, 3 вверх — без крашей, процесс жив.
- [x] Полный экран из середины сетки — стартует и играет: `00:02 / 00:05` через 5 с (C7).
- [x] 20 быстрых свайпов подряд — ноль `ERROR_CODE_TIMEOUT`, ноль `customCacheKey` (A1), время идёт: `00:06 → 00:12` за 4 с.
- [x] Перемотка полосой времени сразу после свайпа: `00:03 → 00:06` (C4).
- [x] ~~Свёрнутое приложение: 0 активных кодеков в `dumpsys media.codec` — декодеры отпущены (C6).~~ **Свидетельство недействительно, см. перепроверку 2026-08-16.**
- [x] 10 циклов вход/выход в полный экран: Dalvik heap 27 МБ, `Activities: 1`, `ViewRootImpl: 1` — утечки объектов нет. Рост PSS весь в Graphics и не монотонный (327 → 409 → 327 МБ) (C1).
- [x] `logcat -b crash` — без `IllegalStateException: FeedPlayerState` (A5).

**Найдена и исправлена регрессия правки A4** — коммит `8ccde7e`. `playerTeardown`
выполняется внутри деактивации подкомпозиции Compose, одновременно с отцеплением surface
у `ContentFrame`. `setVideoEffects` там роняло операцию по таймауту:

```
ExoPlaybackException: Unexpected runtime error (ERROR_CODE_TIMEOUT)
  at LayoutNodeSubcompositionsState.deactivateOutOfFrame(SubcomposeLayout.kt:792)
```

Плеер уходил в `STATE_IDLE` и возвращался в пул сломанным; на 3-5 свайпе так вырождались
все три плеера, и лента вставала на вечном спиннере. Teardown снят: исходная проблема A4
уже закрыта на стороне получения плеера — `LaunchedEffect(player, autoRotate)` и
`LaunchedEffect(player, isMute)` имеют ключ `player`.

### Перепроверка фона (2026-08-16, то же устройство)

Замер `dumpsys media.codec` из прогона 2026-08-15 **недействителен**: на этом устройстве
такого сервиса нет вовсе — `dumpsys -l` его не содержит, вызов отвечает
`Can't find service: media.codec`. Команда возвращала пустой вывод, и пустота была
прочитана как «ноль кодеков».

Правильный источник — `dumpsys media.resource_manager`, блок своего pid. Замер на
полном экране ленты, один и тот же процесс до и после:

| | foreground, играет | свёрнуто |
|---|---|---|
| `c2.mtk.avc.decoder` (hw video) | держится | **держится** |
| `c2.android.aac.decoder` (sw audio) | держится | **держится** |
| `battery/hw-video-codec` | есть | снят |
| Priority | 0 | 400 |

**Вывод: правка C6 не отпускает декодеры в фоне.** `setForegroundMode(false)` вместе с
`playWhenReady = false` оставляет плеер в `STATE_READY`, а декодер держится за состоянием
плеера, не за foreground-режимом. Чтобы освободить кодек, нужен переход в `STATE_IDLE`
(`stop()`) либо возврат плеера в пул.

Смягчающее обстоятельство: удерживается **один** hw-декодер, а не по одному на каждый из
трёх плееров пула — соседние страницы своих видеодекодеров не заводят. То есть исходное
опасение C6 («три плеера держат декодеры в фоне») на практике вдвое-втрое меньше, но
заявленного эффекта правка не даёт. Отдельной задачей.

Заодно проверено в том же прогоне:

- [x] **C6 — воспроизведение продолжается после возврата из фона.** HOME → возврат: процесс
      тот же (pid не сменился), лента играет дальше. Краш `NotSerializableException`,
      блокировавший сценарий 2026-08-15, в этот раз не воспроизвёлся.
- [x] **Стоимость возврата из фона.** `setForegroundMode(true)` на трёх плеерах синхронно в
      `LifecycleStartEffect` — подозрение на блокировку главного потока не подтвердилось:
      в logcat при возврате нет ни одного `Skipped N frames` от процесса приложения.

### Не проверено на устройстве

- [ ] **A4 — поворот не залипает.** Кнопки нижней панели без `content-desc`, вслепую тапать нельзя (среди них скачивание и блокировка). Гарантируется ключом `player` у `LaunchedEffect(player, autoRotate)`: эффекты выставляются заново каждому полученному плееру. Путь отработан 20 свайпами без таймаутов.
- [ ] **P1 — скачанный ролик играет с диска.** Нужен скачанный элемент; кнопку скачивания вслепую не нажать.
- [ ] **C7 — конец ленты.** Лента на сотни элементов, доскроллить жестами непрактично.
- [ ] A-B петля.

### Предсуществующий краш (не из этих правок)

HOME на экране полноэкранной ленты роняет приложение:

```
android.os.BadParcelableException: Parcelable encountered IOException writing
  serializable object (name = com.client.xvideos.r.ui.fullscreen.ScreenRedFullScreen)
Caused by: java.io.NotSerializableException: com.client.xvideos.r.model.GifsInfo
```

Voyager сохраняет стек экранов в saved instance state, `ScreenRedFullScreen` держит
`val item: GifsInfo`, а `GifsInfo` — обычная data class без `Serializable`/`@Parcelize`.
**Проверено: воспроизводится идентично на `c7c1fe7`**, то есть до всех правок ревью.
Заведено отдельной задачей.

### Оставшаяся задача

- [x] Добавить `distributionSha256Sum` в `gradle/wrapper/gradle-wrapper.properties` — сделано в `5a37be0`.

---

## Порядок и фазы

| Фаза | Задачи | Что закрывает |
|---|---|---|
| 1. Блокеры | 1–4 | B1, C1, C2, C3 |
| 2. Производительность | 5–7 | P1, P2, C7 |
| 3. Корректность и контракты | 8–14 | A1, C4, C5, C6, C8, A4, A5 |
| 4. Гигиена и решения | 15–20 | A6, B2, B3, B4, B5, P3, P4, P5 |

Фазы независимы между собой, внутри фазы порядок обязателен. Задача 2 обязана быть выполнена до задач 3 и 4 — они правят тот же метод `mediaSourceFor`.

---

## Правки ревью, принятые до начала работ

Три пункта ревью при проверке оказались либо неточными, либо решаются документацией, а не кодом. Они закрываются задачами 18–20, но решение фиксируется здесь, чтобы исполнитель не пытался «дочинить» их кодом:

1. **P2 — сигнатура колбэка времени.** В ревью сказано, что `(Float, Int) -> Unit` убирает бокс. Это неверно: Kotlin-функциональные типы генерик, `Function2<Float, Integer, Unit>` боксит оба примитива на каждом вызове. Бокс убирает только `fun interface` с примитивными параметрами — задача 6 делает именно так.
2. **P4 — ёмкость пула 3 без запаса.** Проверено: Compose в `applyChanges` вызывает `onForgotten` (то есть `PlayerPool.yield()`) раньше `onRemembered` уходящей/приходящей пары страниц, а `rememberPooledPlayer` вдобавок делает `acquire()` из `scope.launch`, то есть после кадра. К моменту `acquire()` плеер уже в канале. Ёмкость 3 корректна, поднимать до 4 — вернуть потребление памяти, ради снижения которого ветка и делалась. **Кода не меняем**, фиксируем разбор в KDoc (задача 19).
3. **P5 — `RedFullScreenSingle` поднимает полный `FeedPlayerState`.** Альтернатива — второй путь создания плеера, то есть ровно та дублирующая обвязка, которую ветка удаляла. `SimpleCache` и так процессный, `DefaultPreloadManager` дешёвый. Оставляем общий путь, чиним только то, что он не работал (задача 3), и документируем решение (задача 19).

---

## Структура файлов

**Создаются:**

| Файл | Ответственность |
|---|---|
| `core/src/main/java/com/client/xvideos/common/videoplayer/feed/FeedPreloadRegistry.kt` | Чистый учёт «индекс ленты → отданный в прогрев элемент» + список ожидающих индексов. Без media3-типов, generic по типу элемента. |
| `core/src/test/java/com/client/xvideos/common/videoplayer/feed/FeedPreloadRegistryTest.kt` | JVM-тесты реестра: повторный track, смена элемента на индексе, снятие с ожидания, forget. |
| `feature-r/src/test/java/com/client/xvideos/r/common/downloader/DownloadedVideoKeysTest.kt` | JVM-тесты построения ключей скачанных роликов из списка файлов. |

**Изменяются:**

| Файл | Что меняется |
|---|---|
| `gradle/wrapper/gradle-wrapper.properties` | Возврат `validateDistributionUrl`, таймаутов, ретраев; `distributionSha256Sum`. |
| `core/src/main/java/com/client/xvideos/common/videoplayer/feed/FeedPlayerState.kt` | Переезд учёта в реестр, `invalidate()` после `add`, деградация вместо `checkNotNull`, `inferContentType`, `setForegroundMode`, проверка главного потока. |
| `core/src/main/java/com/client/xvideos/common/videoplayer/util/CMPlayer2.kt` | `WeakHashMap` в `KeepScreenOnCounter`. |
| `core/src/main/java/com/client/xvideos/common/ui/lazy/ViewportCacheWindow.kt` | Переименование файла, валидация долей. |
| `core/build.gradle` | `api` → `implementation` для media3-модулей без публичного API. |
| `feature-r/src/main/java/com/client/xvideos/r/common/downloader/DownloadRed.kt` | Новый `downloadedVideoKeys: StateFlow<Set<String>>`, один проход `walkTopDown`. |
| `feature-r/src/main/java/com/client/xvideos/r/ui/video/RedPooledVideoPlayer.kt` | `fun interface` тика времени, отзыв controls, `playerTeardown`, переименование `isBuferring`. |
| `feature-r/src/main/java/com/client/xvideos/r/ui/fullscreen/ScreenRedFullScreen.kt` | `itemCount` вместо `pageCount`, `currentPage` в обоих провайдерах, url из набора ключей, отзыв controls, `updateCurrentPage(0)` на одиночном экране. |
| `feature-l/.../AlbumListFilterTags.kt`, `AlbumListFilterGenres.kt` | `distinct()` перед выдачей в `LazyColumn` с ключами. |
| `gradle/libs.versions.toml` | Комментарий о сознательном использовании alpha material3. |

---

# ФАЗА 1 — БЛОКЕРЫ

## Task 1: Восстановить настройки Gradle wrapper (B1)

**Files:**
- Modify: `gradle/wrapper/gradle-wrapper.properties`

- [ ] **Step 1: Получить официальную контрольную сумму дистрибутива**

```bash
curl -sSL https://services.gradle.org/distributions/gradle-9.6.1-bin.zip.sha256
```

Ожидаемо: одна строка из 64 hex-символов без перевода строки. Скопировать её — она пойдёт в `distributionSha256Sum` на следующем шаге.

Если curl недоступен, тот же файл открывается браузером по этому же адресу.

- [ ] **Step 2: Переписать файл целиком**

Заменить содержимое `gradle/wrapper/gradle-wrapper.properties` на:

```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-9.6.1-bin.zip
distributionSha256Sum=ВСТАВИТЬ_ХЕШ_ИЗ_ШАГА_1
networkTimeout=10000
retries=0
retryBackOffMs=500
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

Строку-таймстамп `#Fri Aug 14 21:50:44 MSK 2026` не возвращать: её пишет Android Studio, полезной информации в ней нет, а в диффах она шумит.

- [ ] **Step 3: Проверить, что wrapper принимает файл и проходит проверку суммы**

```bash
./gradlew.bat --version
```

Ожидаемо: `Gradle 9.6.1` без строк вида `Verification of Gradle distribution failed`. Если дистрибутив уже лежит в `~/.gradle/wrapper/dists`, wrapper проверит сумму существующего архива — расхождение будет видно сразу.

- [ ] **Step 4: Коммит**

```bash
git add gradle/wrapper/gradle-wrapper.properties
git commit -m "build: вернуть validateDistributionUrl и таймауты в gradle wrapper"
```

---

## Task 2: Вынести учёт прогрева в тестируемый реестр (C1, A2, A3)

Двойной `preloadManager.add()` на один индекс приводит к тому, что media3 кладёт новый `MediaSourceHolder` поверх старого без release (`BasePreloadManager.add()` → `mediaSourceHolderMap.put(...)`), а играющий `ExoPlayer` остаётся с источником, которого больше нет в карте. Учёт переезжает в отдельный класс, который явно говорит вызывающему, что делать.

**Files:**
- Create: `core/src/main/java/com/client/xvideos/common/videoplayer/feed/FeedPreloadRegistry.kt`
- Create: `core/src/test/java/com/client/xvideos/common/videoplayer/feed/FeedPreloadRegistryTest.kt`
- Modify: `core/src/main/java/com/client/xvideos/common/videoplayer/feed/FeedPlayerState.kt`

- [ ] **Step 1: Написать падающий тест реестра**

Создать `core/src/test/java/com/client/xvideos/common/videoplayer/feed/FeedPreloadRegistryTest.kt`:

```kotlin
package com.client.xvideos.common.videoplayer.feed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedPreloadRegistryTest {

    @Test
    fun `первый track просит добавить элемент`() {
        val registry = FeedPreloadRegistry<String>()
        assertEquals(PreloadAction.Add("a"), registry.track(1, "a"))
    }

    @Test
    fun `повторный track тем же элементом не просит ничего`() {
        val registry = FeedPreloadRegistry<String>()
        registry.track(1, "a")
        assertEquals(PreloadAction.None, registry.track(1, "a"))
    }

    @Test
    fun `track другим элементом на том же индексе просит заменить`() {
        val registry = FeedPreloadRegistry<String>()
        registry.track(1, "a")
        assertEquals(PreloadAction.Replace("a", "b"), registry.track(1, "b"))
    }

    @Test
    fun `track снимает индекс с ожидания`() {
        val registry = FeedPreloadRegistry<String>()
        registry.markPending(1)
        registry.track(1, "a")
        assertTrue(registry.pendingIndices().isEmpty())
    }

    @Test
    fun `forget отдаёт отслеживаемый элемент и чистит ожидание`() {
        val registry = FeedPreloadRegistry<String>()
        registry.markPending(1)
        registry.track(1, "a")
        assertEquals("a", registry.forget(1))
        assertNull(registry.forget(1))
        assertTrue(registry.pendingIndices().isEmpty())
    }

    @Test
    fun `forget неизвестного индекса ничего не отдаёт`() {
        val registry = FeedPreloadRegistry<String>()
        assertNull(registry.forget(42))
    }

    @Test
    fun `pendingIndices отдаёт копию и переживает правку реестра во время обхода`() {
        val registry = FeedPreloadRegistry<String>()
        registry.markPending(1)
        registry.markPending(2)
        val snapshot = registry.pendingIndices()
        snapshot.forEach { registry.track(it, "item$it") }
        assertEquals(listOf(1, 2), snapshot.sorted())
        assertTrue(registry.pendingIndices().isEmpty())
    }

    @Test
    fun `clear чистит и отслеживаемое и ожидающее`() {
        val registry = FeedPreloadRegistry<String>()
        registry.track(1, "a")
        registry.markPending(2)
        registry.clear()
        assertNull(registry.forget(1))
        assertTrue(registry.pendingIndices().isEmpty())
    }
}
```

- [ ] **Step 2: Убедиться, что тест не компилируется**

```bash
./gradlew.bat :core:testDebugUnitTest --tests "com.client.xvideos.common.videoplayer.feed.FeedPreloadRegistryTest"
```

Ожидаемо: FAILURE с `Unresolved reference: FeedPreloadRegistry` и `Unresolved reference: PreloadAction`.

- [ ] **Step 3: Написать реестр**

Создать `core/src/main/java/com/client/xvideos/common/videoplayer/feed/FeedPreloadRegistry.kt`:

```kotlin
package com.client.xvideos.common.videoplayer.feed

import androidx.collection.MutableIntObjectMap
import androidx.collection.MutableIntSet

/**
 * Что вызывающему нужно сделать с менеджером прогрева после изменения реестра.
 *
 * Существует, потому что `BasePreloadManager.add()` в media3 не идемпотентен: он
 * всегда создаёт новый `MediaSource` и кладёт holder поверх старого через `put()`,
 * не освобождая предыдущий. Повторный `add` одного и того же элемента течёт, а
 * играющий плеер остаётся с источником, которого уже нет в карте менеджера.
 * Поэтому решение «добавлять или нет» принимает реестр, а не вызывающий код.
 */
sealed interface PreloadAction<out T> {

    /** Индекс уже отдан ровно с этим элементом — трогать менеджер нельзя. */
    data object None : PreloadAction<Nothing>

    /** Индекса в прогреве не было — добавить. */
    data class Add<T>(val item: T) : PreloadAction<T>

    /** На индексе был другой элемент — сначала снять [old], затем добавить [new]. */
    data class Replace<T>(val old: T, val new: T) : PreloadAction<T>
}

/**
 * Учёт того, что именно отдано в прогрев: индекс в ленте → добавленный элемент.
 *
 * Собрать элемент заново по данным пейджинга в момент удаления нельзя: к тому
 * времени, когда индекс покидает окно, пейджинг уже мог выбросить его из списка —
 * url не найдётся, снятие с прогрева не случится, и источник останется в
 * менеджере навсегда. Поэтому помним сами.
 *
 * Generic и без media3-типов намеренно: так вся арифметика проверяется обычным
 * JVM-тестом, а работа с `DefaultPreloadManager` остаётся в [FeedPlayerState].
 *
 * `MutableIntObjectMap`/`MutableIntSet` вместо `Map<Int, _>`/`Set<Int>`: ключ —
 * примитивный индекс, и на каждом свайпе окно правится целыми диапазонами.
 * Обычные коллекции боксили бы каждый индекс в `Integer`.
 *
 * Только главный поток — см. контракт [FeedPlayerState].
 */
class FeedPreloadRegistry<T : Any> {

    private val tracked = MutableIntObjectMap<T>()

    /**
     * Индексы, которые вошли в окно, но не имели элемента на тот момент.
     *
     * `SlidingWindowEffect` считает диапазон вошедшим сразу и второй раз
     * `onRangeEnterWindow` для него не позовёт. Помним такие индексы, чтобы
     * догреть их, когда данные приедут.
     */
    private val pending = MutableIntSet()

    /**
     * Индекс отдан в прогрев с элементом [item].
     *
     * Снимает индекс с ожидания: элемент нашёлся, догревать больше нечего —
     * именно здесь закрывается сценарий, в котором страница добавляла источник
     * сама, индекс оставался в ожидании, и догрев добавлял его повторно.
     */
    fun track(index: Int, item: T): PreloadAction<T> {
        pending -= index
        val old = tracked.put(index, item)
        return when {
            old == null -> PreloadAction.Add(item)
            old == item -> PreloadAction.None
            else -> PreloadAction.Replace(old, item)
        }
    }

    /** Индекс вошёл в окно, но элемента для него ещё нет. */
    fun markPending(index: Int) {
        pending += index
    }

    /** Индекс вышел из окна. Возвращает элемент, который нужно снять с прогрева. */
    fun forget(index: Int): T? {
        pending -= index
        return tracked.remove(index)
    }

    /**
     * Ожидающие индексы отдельным списком.
     *
     * Именно копия: вызывающий будет звать [track] прямо во время обхода, а
     * править множество во время итерации нельзя.
     */
    fun pendingIndices(): List<Int> {
        if (pending.isEmpty()) return emptyList()
        val result = ArrayList<Int>(pending.size)
        pending.forEach { result += it }
        return result
    }

    fun clear() {
        tracked.clear()
        pending.clear()
    }
}
```

- [ ] **Step 4: Прогнать тест**

```bash
./gradlew.bat :core:testDebugUnitTest --tests "com.client.xvideos.common.videoplayer.feed.FeedPreloadRegistryTest"
```

Ожидаемо: BUILD SUCCESSFUL, 8 тестов пройдено.

- [ ] **Step 5: Перевести `FeedPlayerState` на реестр**

В `core/src/main/java/com/client/xvideos/common/videoplayer/feed/FeedPlayerState.kt` удалить поля `preloadedItems` и `pendingIndices` вместе с их KDoc (строки 99–111 и 150–159 текущего файла), удалить импорты `androidx.collection.MutableIntList`, `androidx.collection.MutableIntObjectMap`, `androidx.collection.MutableIntSet`, и заменить блок методов `mediaSourceFor` / `addRange` / `retryPending` / `removeRange` / `release` на:

```kotlin
    private val registry = FeedPreloadRegistry<MediaItem>()

    /**
     * Применить решение реестра к менеджеру прогрева.
     *
     * `Replace` снимает старый элемент перед добавлением нового: без этого
     * `BasePreloadManager` положил бы новый holder поверх старого через `put()`,
     * не освободив предыдущий.
     *
     * Имя не `apply`: так называется scope-функция стандартной библиотеки, и
     * затенять её членом класса — верный способ получить неочевидный вызов.
     */
    private fun applyAction(index: Int, action: PreloadAction<MediaItem>) {
        when (action) {
            is PreloadAction.None -> Unit
            is PreloadAction.Add -> preloadManager.add(action.item, index)
            is PreloadAction.Replace -> {
                preloadManager.remove(action.old)
                preloadManager.add(action.new, index)
            }
        }
    }

    /** Источник для страницы: уже прогретый, либо добавленный сейчас. */
    fun mediaSourceFor(mediaItem: MediaItem, index: Int): MediaSource {
        preloadManager.getMediaSource(mediaItem)?.let { return it }
        applyAction(index, registry.track(index, mediaItem))
        return checkNotNull(preloadManager.getMediaSource(mediaItem)) {
            "preloadManager не отдал источник для ${mediaItem.mediaId}"
        }
    }

    fun updateCurrentPage(index: Int) {
        statusControl.currentPlayingIndex = index
        // Отдельный invalidate() не нужен: setCurrentPlayingIndex доходит до
        // SimpleRankingDataComparator, а тот при смене индекса синхронно дёргает
        // InvalidationListener, который BasePreloadManager в конструкторе повесил
        // на собственный invalidate().
        preloadManager.setCurrentPlayingIndex(index)
    }

    /** Элементы вошли в окно вокруг текущей страницы. `urlAt` возвращает null, если элемент ещё не подгружен пейджингом. */
    fun addRange(indices: IntRange, urlAt: (Int) -> String?) {
        indices.forEach { index ->
            val url = urlAt(index)
            if (url == null) {
                registry.markPending(index)
                return@forEach
            }
            applyAction(index, registry.track(index, mediaItemFor(index, url)))
        }
        preloadManager.invalidate()
    }

    /** Догреть индексы, у которых url не было в момент входа в окно. */
    fun retryPending(urlAt: (Int) -> String?) {
        val waiting = registry.pendingIndices()
        if (waiting.isEmpty()) return
        var resolvedAny = false
        waiting.forEach { index ->
            val url = urlAt(index) ?: return@forEach
            applyAction(index, registry.track(index, mediaItemFor(index, url)))
            resolvedAny = true
        }
        if (resolvedAny) preloadManager.invalidate()
    }

    /** Элементы вышли из окна — снимаем с прогрева по собственному учёту. */
    fun removeRange(indices: IntRange) {
        indices.forEach { index ->
            val mediaItem = registry.forget(index) ?: return@forEach
            preloadManager.remove(mediaItem)
        }
    }

    /** Кеш ([FeedVideoCache]) намеренно не трогаем: он процессный. */
    fun release() {
        registry.clear()
        playerPool.release()
        preloadManager.release()
    }
```

- [ ] **Step 6: Собрать модуль**

```bash
./gradlew.bat :core:compileDebugKotlin
```

Ожидаемо: BUILD SUCCESSFUL.

- [ ] **Step 7: Прогнать все тесты core**

```bash
./gradlew.bat :core:testDebugUnitTest
```

Ожидаемо: BUILD SUCCESSFUL, `FeedPreloadPolicyTest` и `FeedPreloadRegistryTest` зелёные.

- [ ] **Step 8: Коммит**

```bash
git add core/src/main/java/com/client/xvideos/common/videoplayer/feed/FeedPreloadRegistry.kt core/src/test/java/com/client/xvideos/common/videoplayer/feed/FeedPreloadRegistryTest.kt core/src/main/java/com/client/xvideos/common/videoplayer/feed/FeedPlayerState.kt
git commit -m "fix(core): не добавлять один индекс в прогрев дважды

BasePreloadManager.add() кладёт новый holder поверх старого через put()
и не освобождает предыдущий. Страница добавляла источник сама, индекс
оставался в ожидании, догрев добавлял его повторно — старый holder тёк,
а играющий плеер оставался с источником вне карты менеджера.

Учёт вынесен в FeedPreloadRegistry: он же снимает индекс с ожидания при
добавлении и требует remove перед заменой элемента на индексе."
```

---

## Task 3: Планировать прогрев сразу после добавления (C2)

`add()` без `invalidate()` кладёт holder в карту, но прогрев для него не планируется. На одиночном экране `invalidate()` не вызывался вообще ни разу, а `currentPlayingIndex` навсегда оставался `C.INDEX_UNSET`.

**Files:**
- Modify: `core/src/main/java/com/client/xvideos/common/videoplayer/feed/FeedPlayerState.kt`
- Modify: `feature-r/src/main/java/com/client/xvideos/r/ui/fullscreen/ScreenRedFullScreen.kt`

- [ ] **Step 1: Добавить `invalidate()` в `mediaSourceFor`**

В `FeedPlayerState.kt` заменить тело `mediaSourceFor` на:

```kotlin
    /** Источник для страницы: уже прогретый, либо добавленный сейчас. */
    fun mediaSourceFor(mediaItem: MediaItem, index: Int): MediaSource {
        preloadManager.getMediaSource(mediaItem)?.let { return it }
        val action = registry.track(index, mediaItem)
        applyAction(index, action)
        // add() сам по себе только кладёт holder в карту. Без invalidate()
        // менеджер не пересобирает очередь приоритетов, и добавленный страницей
        // элемент не греется до следующего внешнего invalidate — а на одиночном
        // экране такого вызова нет вовсе.
        if (action !is PreloadAction.None) preloadManager.invalidate()
        return checkNotNull(preloadManager.getMediaSource(mediaItem)) {
            "preloadManager не отдал источник для ${mediaItem.mediaId}"
        }
    }
```

- [ ] **Step 2: Задать текущую страницу на одиночном экране**

В `ScreenRedFullScreen.kt` в функции `RedFullScreenSingle` после строки `val feedState = rememberFeedPlayerState(poolCapacity = 1)` добавить:

```kotlin
    // Без этого statusControl.currentPlayingIndex остаётся C.INDEX_UNSET, политика
    // всегда отдаёт CACHED_ONLY, и preload-менеджер на этом экране не делает ничего.
    LaunchedEffect(feedState) { feedState.updateCurrentPage(0) }
```

- [ ] **Step 3: Собрать**

```bash
./gradlew.bat :feature-r:compileDebugKotlin
```

Ожидаемо: BUILD SUCCESSFUL.

- [ ] **Step 4: Коммит**

```bash
git add core/src/main/java/com/client/xvideos/common/videoplayer/feed/FeedPlayerState.kt feature-r/src/main/java/com/client/xvideos/r/ui/fullscreen/ScreenRedFullScreen.kt
git commit -m "fix(core): планировать прогрев сразу после добавления источника"
```

---

## Task 4: Не падать, если менеджер прогрева не отдал источник (C3)

`checkNotNull` вызывается из `playerSetup` во время свайпа — любая неожиданность в media3 превращается в краш вместо чёрного кадра.

**Files:**
- Modify: `core/src/main/java/com/client/xvideos/common/videoplayer/feed/FeedPlayerState.kt`

- [ ] **Step 1: Вынести фабрику датасорса в поле**

В `FeedPlayerState.kt` перед объявлением `builder` добавить:

```kotlin
    // Не голая http-фабрика: `DefaultPreloadManager` подставляет её как есть,
    // вместо своего `DefaultDataSource.Factory`, и локальные файлы (скачанные
    // ролики отдаются голым путём, без схемы) ушли бы в Ktor как HTTP-запрос.
    // `DefaultDataSource` разбирает схему и уводит локальные файлы в
    // `FileDataSource`, а сеть — в Ktor.
    private val dataSourceFactory = DefaultDataSource.Factory(appContext, VideoHttpDataSource.factory())
```

и в цепочке `builder` заменить строку

```kotlin
            .setDataSourceFactory(DefaultDataSource.Factory(appContext, VideoHttpDataSource.factory()))
```

на

```kotlin
            .setDataSourceFactory(dataSourceFactory)
```

Комментарий о фабрике, который стоял над `.setDataSourceFactory(...)`, переехал к полю — из цепочки его убрать.

- [ ] **Step 2: Заменить `checkNotNull` на деградацию**

В том же файле заменить `mediaSourceFor` на:

```kotlin
    /** Источник для страницы: уже прогретый, либо добавленный сейчас. */
    fun mediaSourceFor(mediaItem: MediaItem, index: Int): MediaSource {
        preloadManager.getMediaSource(mediaItem)?.let { return it }
        val action = registry.track(index, mediaItem)
        applyAction(index, action)
        // add() сам по себе только кладёт holder в карту. Без invalidate()
        // менеджер не пересобирает очередь приоритетов, и добавленный страницей
        // элемент не греется до следующего внешнего invalidate — а на одиночном
        // экране такого вызова нет вовсе.
        if (action !is PreloadAction.None) preloadManager.invalidate()
        val preloaded = preloadManager.getMediaSource(mediaItem)
        if (preloaded != null) return preloaded
        // Метод зовётся из playerSetup прямо во время свайпа. Уронить приложение
        // здесь нельзя: играем мимо прогрева тем же стеком датасорсов.
        Timber.w("preloadManager не отдал источник для ${mediaItem.mediaId} — играем мимо прогрева")
        return fallbackSourceFactory.createMediaSource(mediaItem)
    }

    /**
     * Запасная фабрика на случай, когда preload-менеджер не отдал источник.
     * Тот же `dataSourceFactory`, что и у прогрева, — иначе локальные файлы
     * снова ушли бы в Ktor как HTTP-запрос.
     */
    private val fallbackSourceFactory: MediaSource.Factory =
        DefaultMediaSourceFactory(appContext).setDataSourceFactory(dataSourceFactory)
```

- [ ] **Step 3: Добавить импорты**

В шапку `FeedPlayerState.kt` добавить:

```kotlin
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import timber.log.Timber
```

- [ ] **Step 4: Собрать**

```bash
./gradlew.bat :core:compileDebugKotlin
```

Ожидаемо: BUILD SUCCESSFUL.

- [ ] **Step 5: Коммит**

```bash
git add core/src/main/java/com/client/xvideos/common/videoplayer/feed/FeedPlayerState.kt
git commit -m "fix(core): играть мимо прогрева вместо краша, если источник не пришёл"
```

---

# ФАЗА 2 — ПРОИЗВОДИТЕЛЬНОСТЬ

## Task 5: Убрать File.exists() с главного потока (P1)

`peekUrl` → `redVideoUrl` → `Downloader.findVideoInDownload` → `File.exists()` вызывается синхронно на главном потоке для каждого индекса окна и на каждое изменение `itemCount`. `DownloadRed.refreshDownloadList()` уже обходит папку кеша при каждом изменении (скачивание, удаление одного ролика, полная очистка) — добавляем в этот же обход набор ключей скачанных видео.

Использовать существующий `downloadList` нельзя: он собирается по `.info`-файлам, а `.info` существует и у незавершённой закачки без `.mp4`.

**Files:**
- Modify: `feature-r/src/main/java/com/client/xvideos/r/common/downloader/DownloadRed.kt`
- Create: `feature-r/src/test/java/com/client/xvideos/r/common/downloader/DownloadedVideoKeysTest.kt`
- Modify: `feature-r/src/main/java/com/client/xvideos/r/ui/fullscreen/ScreenRedFullScreen.kt`

- [ ] **Step 1: Написать падающий тест ключей**

Создать `feature-r/src/test/java/com/client/xvideos/r/common/downloader/DownloadedVideoKeysTest.kt`:

```kotlin
package com.client.xvideos.r.common.downloader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DownloadedVideoKeysTest {

    @Test
    fun `ключ собирается из креатора и id`() {
        assertEquals("Creator/AbcDef", downloadedVideoKey("Creator", "AbcDef"))
    }

    @Test
    fun `ключи файлов берут креатора из имени папки`() {
        val files = listOf(
            File("/cache/r/Creator/AbcDef.mp4"),
            File("/cache/r/Other/Xyz.mp4"),
        )
        assertEquals(setOf("Creator/AbcDef", "Other/Xyz"), downloadedVideoKeys(files))
    }

    @Test
    fun `файл без родительской папки не роняет разбор`() {
        assertEquals(setOf("/AbcDef"), downloadedVideoKeys(listOf(File("AbcDef.mp4"))))
    }

    @Test
    fun `набор ключей отвечает на проверку скачанности`() {
        val keys = downloadedVideoKeys(listOf(File("/cache/r/Creator/AbcDef.mp4")))
        assertTrue(downloadedVideoKey("Creator", "AbcDef") in keys)
        assertFalse(downloadedVideoKey("Creator", "Missing") in keys)
    }
}
```

- [ ] **Step 2: Убедиться, что тест не компилируется**

```bash
./gradlew.bat :feature-r:testDebugUnitTest --tests "com.client.xvideos.r.common.downloader.DownloadedVideoKeysTest"
```

Ожидаемо: FAILURE с `Unresolved reference: downloadedVideoKey`.

- [ ] **Step 3: Написать функции ключей**

В конец `feature-r/src/main/java/com/client/xvideos/r/common/downloader/DownloadRed.kt`, после закрывающей скобки класса, добавить:

```kotlin
/**
 * Ключ скачанного ролика: креатор + id. Формат совпадает с раскладкой кеша
 * `<r_cache_download>/<userName>/<id>.mp4`, поэтому набор ключей строится
 * прямо из списка файлов, без повторного обхода диска.
 */
internal fun downloadedVideoKey(userName: String, id: String): String = "$userName/$id"

/**
 * Набор ключей скачанных видео по списку `.mp4`-файлов кеша.
 *
 * Отдельная функция, а не лямбда внутри обхода: это единственная арифметика в
 * этом пути, и она проверяется обычным JVM-тестом.
 */
internal fun downloadedVideoKeys(videoFiles: List<File>): Set<String> =
    videoFiles.mapTo(mutableSetOf()) {
        downloadedVideoKey(it.parentFile?.name.orEmpty(), it.nameWithoutExtension)
    }
```

- [ ] **Step 4: Прогнать тест**

```bash
./gradlew.bat :feature-r:testDebugUnitTest --tests "com.client.xvideos.r.common.downloader.DownloadedVideoKeysTest"
```

Ожидаемо: BUILD SUCCESSFUL, 4 теста пройдено.

- [ ] **Step 5: Отдавать набор ключей потоком**

В `DownloadRed.kt` после объявления `downloadList` добавить:

```kotlin
    /**
     * Ключи роликов, у которых на диске лежит готовый `.mp4`.
     *
     * Отдельно от [downloadList]: тот собирается по `.info`-файлам, а `.info`
     * существует и у оборванной закачки без видео. Нужен, чтобы окно
     * предзагрузки ленты не дёргало `File.exists()` на главном потоке для
     * каждого индекса — набор перестраивается тем же обходом, что и список.
     */
    private val _downloadedVideoKeys = MutableStateFlow<Set<String>>(emptySet())
    val downloadedVideoKeys: StateFlow<Set<String>> = _downloadedVideoKeys.asStateFlow()
```

Затем заменить тело `refreshDownloadList()` на:

```kotlin
    fun refreshDownloadList() {
        scope.launch(Dispatchers.IO) {
            val rootDir = File(AppPath.r_cache_download)

            // Один обход на оба результата: раньше папка обходилась ради `.info`,
            // а скачанность видео проверялась потом по одному File.exists() на
            // элемент — и делалось это на главном потоке.
            val allFiles = if (rootDir.exists() && rootDir.isDirectory) {
                rootDir.walkTopDown().filter { it.isFile }.toList()
            } else {
                emptyList()
            }

            val gson = GsonBuilder().create()

            val result = mutableListOf<GifsInfo>()

            allFiles.forEach { file ->
                if (file.extension != "info") return@forEach
                try {
                    val content = file.readText()
                    val obj = gson.fromJson(content, GifsInfo::class.java)
                    result.add(obj)
                } catch (e: Exception) {
                    // Битый .info пропускаем, но в лог приложения, а не в stdout.
                    Timber.w(e, "Ошибка при чтении файла ${file.absolutePath}")
                }
            }

            _downloadList.emit(result)
            _downloadedVideoKeys.emit(downloadedVideoKeys(allFiles.filter { it.extension == "mp4" }))
        }
    }
```

- [ ] **Step 6: Читать набор на экране ленты вместо File.exists()**

В `ScreenRedFullScreen.kt` заменить `peekUrl` и `redVideoUrl` на:

```kotlin
/**
 * Url элемента ленты по индексу для окна предзагрузки. `peek` (а не `get`) —
 * намеренно: он не дёргает пейджинг на подгрузку соседних страниц.
 */
private fun LazyPagingItems<GifsInfo>.peekUrl(index: Int, downloadedKeys: Set<String>): String? {
    if (index < 0 || index >= itemCount) return null
    return peek(index)?.let { redVideoUrl(it, downloadedKeys) }
}

/**
 * Адрес видео для элемента ленты: локальный файл, если ролик уже скачан,
 * иначе HLS с api.redgifs.com. Общая точка для страницы и для предзагрузки —
 * ключи preload-менеджера обязаны совпадать с тем, что реально играет плеер.
 *
 * Скачанность берётся из готового набора ключей, а не из `File.exists()`:
 * функция зовётся из окна предзагрузки на главном потоке для каждого индекса.
 */
private fun redVideoUrl(item: GifsInfo, downloadedKeys: Set<String>): String =
    if (downloadedVideoKey(item.userName, item.id) in downloadedKeys) {
        "${AppPath.r_cache_download}/${item.userName}/${item.id}.mp4"
    } else {
        "https://api.redgifs.com/v2/gifs/${item.id.lowercase()}/hd.m3u8"
    }
```

Добавить импорт:

```kotlin
import com.client.xvideos.r.common.downloader.downloadedVideoKey
```

- [ ] **Step 7: Протянуть набор через композиции**

В `RedFullScreenFeed` после `val listGifs = host.pager.collectAsLazyPagingItems()` добавить:

```kotlin
    val downloadedKeys by vm.downloadRed.downloadedVideoKeys.collectAsStateWithLifecycle()
```

Заменить оба вызова `listGifs.peekUrl(i, vm)` (в `SlidingWindowEffect` и в обоих `retryPending`) на `listGifs.peekUrl(i, downloadedKeys)`.

В обоих вызовах `RedFullScreenPage(...)` внутри `RedFullScreenFeed` добавить аргумент `downloadedKeys = downloadedKeys`.

В `RedFullScreenSingle` после `var isVideoBuffering by remember { mutableStateOf(false) }` добавить:

```kotlin
    val downloadedKeys by vm.downloadRed.downloadedVideoKeys.collectAsStateWithLifecycle()
```

и передать `downloadedKeys = downloadedKeys` в свой вызов `RedFullScreenPage`.

В сигнатуру `RedFullScreenPage` добавить параметр `downloadedKeys: Set<String>,` (после `feedState: FeedPlayerState,`) и заменить первую строку её тела на:

```kotlin
    val videoUri = remember(item.id, item.userName, downloadedKeys) { redVideoUrl(item, downloadedKeys) }
```

- [ ] **Step 8: Собрать и прогнать тесты**

```bash
./gradlew.bat :feature-r:compileDebugKotlin :feature-r:testDebugUnitTest
```

Ожидаемо: BUILD SUCCESSFUL.

- [ ] **Step 9: Коммит**

```bash
git add feature-r/src/main/java/com/client/xvideos/r/common/downloader/DownloadRed.kt feature-r/src/test/java/com/client/xvideos/r/common/downloader/DownloadedVideoKeysTest.kt feature-r/src/main/java/com/client/xvideos/r/ui/fullscreen/ScreenRedFullScreen.kt
git commit -m "perf(r): убрать File.exists с главного потока в окне предзагрузки

Скачанность ролика бралась одним File.exists() на индекс — синхронно на
главном потоке, на каждый вошедший диапазон и на каждую приехавшую
страницу пейджинга. DownloadRed теперь строит набор ключей скачанных
видео тем же обходом папки, которым уже собирает downloadList."
```

---

## Task 6: Убрать аллокации из тика времени (P2)

`Pair<Float, Int>` создаётся 20 раз в секунду с боксом обоих примитивов. Kotlin-функциональный тип `(Float, Int) -> Unit` бокс не убирает — параметры `Function2` генерик. Убирает `fun interface` с примитивной сигнатурой.

**Files:**
- Modify: `feature-r/src/main/java/com/client/xvideos/r/ui/video/RedPooledVideoPlayer.kt`
- Modify: `feature-r/src/main/java/com/client/xvideos/r/ui/fullscreen/ScreenRedFullScreen.kt`

- [ ] **Step 1: Объявить `fun interface`**

В `RedPooledVideoPlayer.kt` перед объявлением `RedPooledVideoPlayer` добавить:

```kotlin
/**
 * Тик времени плеера ленты.
 *
 * `fun interface`, а не `(Float, Int) -> Unit`: у Kotlin-функциональных типов
 * параметры генерик, `Function2<Float, Integer, Unit>` боксит оба примитива на
 * каждом вызове — а вызовов здесь 20 в секунду. У `fun interface` сигнатура
 * компилируется в `onTime(float, int)`, без бокса и без промежуточного `Pair`.
 */
fun interface FeedTimeListener {
    fun onTime(positionSeconds: Float, durationSeconds: Int)
}
```

- [ ] **Step 2: Заменить параметр и цикл**

В сигнатуре `RedPooledVideoPlayer` заменить `onChangeTime: (Pair<Float, Int>) -> Unit,` на `onTimeChanged: FeedTimeListener,`.

Заменить блок опроса времени (`LaunchedEffect(player, isCurrentPage, enableAB, timeA, timeB)`) на:

```kotlin
    // Время/длительность и петля A-B. Шаг 50 мс — как в прежнем CMPPlayer2,
    // чтобы поведение полосы времени и A-B не изменилось.
    LaunchedEffect(player, isCurrentPage, enableAB, timeA, timeB) {
        val exo = player ?: return@LaunchedEffect
        // Нетекущие страницы не играют (playWhenReady = play && isCurrentPage), время на них
        // не движется — крутить на них опрос смысла нет. Без этого выхода при трёх живых
        // страницах работали бы три корутины по 20 Гц.
        if (!isCurrentPage) return@LaunchedEffect
        // Аналог прежнего `distinctUntilChanged()`: на паузе значение не меняется,
        // и апстрим не дёргается 20 раз в секунду впустую. Примитивные локальные
        // переменные вместо Pair — на 20 Гц это единственная аллокация в цикле.
        // NaN не равен ничему, включая себя, поэтому первый проход всегда репортит.
        var lastPosition = Float.NaN
        var lastDuration = -1
        while (isActive) {
            val position = (exo.currentPosition / 1000f).coerceAtLeast(0f)
            val durationMs = exo.duration.takeIf { it != C.TIME_UNSET } ?: 0L
            val duration = (durationMs / 1000).toInt()
            if (position != lastPosition || duration != lastDuration) {
                lastPosition = position
                lastDuration = duration
                onTimeChanged.onTime(position, duration)
            }
            if (enableAB && position >= timeB) exo.seekTo((timeA * 1000).toLong())
            delay(50)
        }
    }
```

- [ ] **Step 3: Обновить вызов**

В `ScreenRedFullScreen.kt` в `RedFullScreenPage` заменить блок `onChangeTime = { ... }` на:

```kotlin
            onTimeChanged = { position, duration ->
                if (isCurrentPage) {
                    vm.currentPlayerTime = position
                    vm.currentPlayerDuration = duration
                }
            },
```

Импорт `FeedTimeListener` не нужен: SAM-конверсия из лямбды работает по типу параметра.

- [ ] **Step 4: Собрать**

```bash
./gradlew.bat :feature-r:compileDebugKotlin
```

Ожидаемо: BUILD SUCCESSFUL.

- [ ] **Step 5: Коммит**

```bash
git add feature-r/src/main/java/com/client/xvideos/r/ui/video/RedPooledVideoPlayer.kt feature-r/src/main/java/com/client/xvideos/r/ui/fullscreen/ScreenRedFullScreen.kt
git commit -m "perf(r): убрать Pair и бокс из тика времени плеера ленты"
```

---

## Task 7: Вести окно по реальным элементам пейджинга (C7, C5)

`itemCountProvider = { pagerState.pageCount }` даёт индексы, которых в списке нет: `pageCount = max(startIndex + 1, itemCount + appendExtra)`. Для них `peekUrl` всегда возвращает null, и они навсегда оседают в ожидании. Заодно приводим оба провайдера к `currentPage`: сейчас окно ведётся по `settledPage`, а ранжирование прогрева — по `currentPage`, и ранжирование может ссылаться на индекс вне окна.

**Files:**
- Modify: `feature-r/src/main/java/com/client/xvideos/r/ui/fullscreen/ScreenRedFullScreen.kt`

- [ ] **Step 1: Переключить провайдеры**

Заменить блок `SlidingWindowEffect(...)` на:

```kotlin
    SlidingWindowEffect(
        // Именно itemCount пейджинга, а не pagerState.pageCount: счётчик страниц
        // пейджера больше списка на «хвост» дозагрузки и на startIndex до прихода
        // данных. Для таких индексов url не существует, они оседали в ожидании
        // догрева навсегда.
        itemCountProvider = { listGifs.itemCount },
        // Тот же currentPage, что уходит в updateCurrentPage ниже. Раньше окно
        // велось по settledPage, а ранжирование — по currentPage, и ранжирование
        // могло ссылаться на индекс, ещё не вошедший в окно.
        currentItemProvider = { pagerState.currentPage },
        maxLookbehind = 2,
        maxLookahead = 4,
        // Батч применяется только при восстановлении после прыжка: в обычной
        // прокрутке SlidingWindowEffect каждый раз пересобирает окно целиком
        // (см. ветку else в его reconcile), и порционная догрузка не включается.
        batchSize = 3,
        onRangeEnterWindow = { range ->
            feedState.addRange(range) { i ->
                listGifs.peekUrl(i, downloadedKeys)
            }
        },
        onRangeLeaveWindow = { range ->
            feedState.removeRange(range)
        },
    )
```

- [ ] **Step 2: Проверить вручную на устройстве**

Собрать и поставить сборку:

```bash
./gradlew.bat :app:installDebug
```

Сценарий проверки:
1. Открыть ленту R, тапнуть по ролику в середине сетки — открывается полный экран с этого ролика.
2. Убедиться, что ролик стартует, а не висит в спиннере (это проверка того, что стартовое окно догрелось по приходу данных пейджинга).
3. Пролистать 10 роликов вниз, 10 вверх — переходы без чёрных кадров и без спиннера дольше секунды.
4. Долистать до конца ленты — приложение не зависает на последнем элементе.

- [ ] **Step 3: Коммит**

```bash
git add feature-r/src/main/java/com/client/xvideos/r/ui/fullscreen/ScreenRedFullScreen.kt
git commit -m "fix(r): вести окно предзагрузки по элементам пейджинга, а не по страницам пейджера"
```

---

# ФАЗА 3 — КОРРЕКТНОСТЬ И КОНТРАКТЫ

## Task 8: Определять HLS по Uri, а не по хвосту строки (A1)

`url.endsWith(".m3u8")` ломается на подписанном url с query и на редиректе — и тогда `customCacheKey` вернётся на адаптивный поток, то есть вернётся краш `customCacheKey must be null for type: 2`, ради которого условие и писалось. `Util.inferContentType(Uri)` — та же логика, которой пользуется сам media3.

**Files:**
- Modify: `core/src/main/java/com/client/xvideos/common/videoplayer/feed/FeedPlayerState.kt`

- [ ] **Step 1: Переписать `mediaItemFor`**

Заменить метод целиком на:

```kotlin
    /**
     * Ключ элемента для preload-менеджера. `mediaId` — позиция в ленте (по нему
     * менеджер удаляет элементы), `customCacheKey` — сам url, чтобы дисковый кеш
     * переживал перетасовку списка.
     *
     * `customCacheKey` ставим только прогрессивным потокам. Адаптивным его
     * запрещает `DownloadRequest` (`customCacheKey must be null for type: 2`), а
     * до него доходит `PreCacheHelper` на уровне прогрева `specifiedRangeCached` —
     * с ключом приложение падало на первом же дальнем элементе ленты. Плейлист
     * и сегменты HLS кешируются по своим адресам, отдельный ключ им не нужен.
     *
     * Тип определяет `Util.inferContentType` — то же, чем пользуется сам media3.
     * Проверка `endsWith(".m3u8")` ломалась бы на подписанном url с `?token=…`
     * и на редиректе, то есть ровно там, где краш и вернулся бы.
     */
    fun mediaItemFor(index: Int, url: String): MediaItem {
        val uri = Uri.parse(url)
        return MediaItem.Builder()
            .setUri(uri)
            .setMediaId(index.toString())
            .apply {
                if (Util.inferContentType(uri) == C.CONTENT_TYPE_OTHER) setCustomCacheKey(url)
            }
            .build()
    }
```

- [ ] **Step 2: Добавить импорты**

```kotlin
import android.net.Uri
import androidx.media3.common.util.Util
```

- [ ] **Step 3: Собрать**

```bash
./gradlew.bat :core:compileDebugKotlin
```

Ожидаемо: BUILD SUCCESSFUL.

- [ ] **Step 4: Проверить на устройстве, что дальний прогрев не роняет приложение**

```bash
./gradlew.bat :app:installDebug
```

Открыть ленту R, быстро пролистать 15+ роликов подряд. Ожидаемо: без краша `customCacheKey must be null for type: 2`. Проверить logcat:

```bash
adb logcat -d -s AndroidRuntime | tail -40
```

Ожидаемо: без `IllegalStateException` из `DownloadRequest`.

- [ ] **Step 5: Коммит**

```bash
git add core/src/main/java/com/client/xvideos/common/videoplayer/feed/FeedPlayerState.kt
git commit -m "fix(core): определять тип потока через Util.inferContentType"
```

---

## Task 9: Отзывать controls вместе со страницей (C4)

Сейчас страница выставляет `vm.currentPlayerControls`, а обнуляет их централизованно коллектор смены страницы. Ушедшая страница свои controls не отзывает — между её dispose и следующим эффектом полоса времени дёргает плеер, который `PlayerPool.yield()` уже остановил и очистил.

**Files:**
- Modify: `feature-r/src/main/java/com/client/xvideos/r/ui/video/RedPooledVideoPlayer.kt`
- Modify: `feature-r/src/main/java/com/client/xvideos/r/ui/fullscreen/ScreenRedFullScreen.kt`

- [ ] **Step 1: Добавить параметр отзыва**

В сигнатуре `RedPooledVideoPlayer` после `onPlayerControlsReady: (PlayerControls) -> Unit,` добавить:

```kotlin
    onPlayerControlsRelease: (PlayerControls) -> Unit,
```

- [ ] **Step 2: Заменить LaunchedEffect на DisposableEffect**

Заменить блок `LaunchedEffect(player, isCurrentPage) { ... onPlayerControlsReady(...) ... }` на:

```kotlin
    // DisposableEffect, а не LaunchedEffect: страница обязана отозвать свои controls
    // при уходе из композиции. PlayerPool.yield() к этому моменту уже сделал плееру
    // stop() и clearMediaItems(), и оставленная снаружи ссылка дёргала бы пустой плеер.
    DisposableEffect(player, isCurrentPage) {
        val exo = player
        if (exo == null || !isCurrentPage) {
            onDispose { }
        } else {
            val controls = object : PlayerControls {
                override fun forward(seconds: Float) {
                    exo.seekTo(exo.clampSeekPositionMs(exo.currentPosition + (seconds * 1000).toLong()))
                }

                override fun rewind(seconds: Float) {
                    exo.seekTo(exo.clampSeekPositionMs(exo.currentPosition - (seconds * 1000).toLong()))
                }

                override fun seekTo(positionSeconds: Float) {
                    exo.seekTo(exo.clampSeekPositionMs((positionSeconds * 1000).toLong()))
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
            }
            onPlayerControlsReady(controls)
            onDispose { onPlayerControlsRelease(controls) }
        }
    }
```

- [ ] **Step 3: Передать отзыв и убрать централизованное обнуление**

В `ScreenRedFullScreen.kt` в `RedFullScreenPage` после блока `onPlayerControlsReady = { ... }` добавить:

```kotlin
            onPlayerControlsRelease = { controls ->
                // Сравнение по ссылке: страница отзывает только свои controls и не
                // затирает те, что успела выставить пришедшая ей на смену.
                if (vm.currentPlayerControls === controls) vm.currentPlayerControls = null
            },
```

В коллекторе `snapshotFlow { pagerState.currentPage }` удалить строку:

```kotlin
                vm.currentPlayerControls = null
```

- [ ] **Step 4: Собрать**

```bash
./gradlew.bat :feature-r:compileDebugKotlin
```

Ожидаемо: BUILD SUCCESSFUL.

- [ ] **Step 5: Проверить на устройстве**

```bash
./gradlew.bat :app:installDebug
```

Сценарий: открыть полный экран ленты, свайпнуть на следующий ролик, сразу потянуть полосу времени внизу. Ожидаемо: перемотка работает на каждом ролике сразу после свайпа, без «мёртвого» первого касания.

- [ ] **Step 6: Коммит**

```bash
git add feature-r/src/main/java/com/client/xvideos/r/ui/video/RedPooledVideoPlayer.kt feature-r/src/main/java/com/client/xvideos/r/ui/fullscreen/ScreenRedFullScreen.kt
git commit -m "fix(r): страница сама отзывает свои controls при уходе из композиции"
```

---

## Task 10: Отпускать декодеры на время фона (C6)

`setForegroundMode(true)` держит кодеки трёх плееров даже когда приложение свёрнуто и `playWhenReady` снят.

**Files:**
- Modify: `core/src/main/java/com/client/xvideos/common/videoplayer/feed/FeedPlayerState.kt`

- [ ] **Step 1: Добавить метод переключения**

В `FeedPlayerState` после `updateCurrentPage` добавить:

```kotlin
    /**
     * Держать ли кодеки прогретыми.
     *
     * `setForegroundMode(true)` удерживает декодеры даже на снятом `playWhenReady`,
     * поэтому на уходе приложения в фон режим нужно снимать явно — иначе три
     * плеера держат аппаратные декодеры всё время, пока приложение свёрнуто.
     */
    fun setForegroundMode(foreground: Boolean) {
        playerPool.executeForAll { setForegroundMode(foreground) }
    }
```

- [ ] **Step 2: Повесить на жизненный цикл**

Заменить `rememberFeedPlayerState` на:

```kotlin
@Composable
fun rememberFeedPlayerState(
    poolCapacity: Int = FeedPlayerState.DEFAULT_POOL_CAPACITY,
): FeedPlayerState {
    val context = LocalContext.current
    val state = remember(context, poolCapacity) { FeedPlayerState(context, poolCapacity) }
    LifecycleStartEffect(state) {
        state.setForegroundMode(true)
        onStopOrDispose { state.setForegroundMode(false) }
    }
    DisposableEffect(state) { onDispose { state.release() } }
    return state
}
```

- [ ] **Step 3: Добавить импорт**

```kotlin
import androidx.lifecycle.compose.LifecycleStartEffect
```

- [ ] **Step 4: Собрать**

```bash
./gradlew.bat :core:compileDebugKotlin
```

Ожидаемо: BUILD SUCCESSFUL.

- [ ] **Step 5: Коммит**

```bash
git add core/src/main/java/com/client/xvideos/common/videoplayer/feed/FeedPlayerState.kt
git commit -m "fix(core): снимать foreground mode с плееров ленты в фоне"
```

---

## Task 11: Сбрасывать состояние плеера при возврате в пул (A4)

`PlayerPool.yield()` сбрасывает только `playWhenReady`, `stop()` и `clearMediaItems()`. `volume`, `videoEffects` и `playbackSpeed` переезжают на следующую страницу. Сейчас спасает то, что каждая страница выставляет громкость и поворот своими эффектами — это неявный инвариант, который сломается на первом же новом свойстве.

**Files:**
- Modify: `feature-r/src/main/java/com/client/xvideos/r/ui/video/RedPooledVideoPlayer.kt`
- Modify: `core/src/main/java/com/client/xvideos/common/videoplayer/feed/FeedPlayerState.kt`

- [ ] **Step 1: Передать `playerTeardown`**

В `RedPooledVideoPlayer.kt` заменить вызов `rememberPooledPlayer` на:

```kotlin
    val player: ExoPlayer? = rememberPooledPlayer(
        mediaItem = mediaItem,
        playerPool = feedState.playerPool,
        playerSetup = { exo ->
            exo.setMediaSource(feedState.mediaSourceFor(mediaItem, index))
            exo.prepare()
        },
        // PlayerPool.yield() сбрасывает только playWhenReady/stop/clearMediaItems.
        // Всё, что страница ставила на плеер сама, она обязана снять сама —
        // иначе следующая страница получит чужой поворот, громкость и скорость.
        playerTeardown = { exo ->
            exo.setVideoEffects(emptyList())
            exo.volume = 1f
            exo.setPlaybackSpeed(1f)
        },
    )
```

- [ ] **Step 2: Записать инвариант в KDoc пула**

В `FeedPlayerState.kt` над объявлением `playerPool` добавить:

```kotlin
    /**
     * Пул плееров ленты.
     *
     * Внимание: `PlayerPool.yield()` при возврате плеера сбрасывает только
     * `playWhenReady`, `stop()` и `clearMediaItems()`. Любое другое свойство,
     * выставленное на плеере страницей (громкость, видеоэффекты, скорость),
     * страница обязана снять сама через `playerTeardown` у `rememberPooledPlayer` —
     * пул за этим не следит.
     */
```

- [ ] **Step 3: Собрать**

```bash
./gradlew.bat :core:compileDebugKotlin :feature-r:compileDebugKotlin
```

Ожидаемо: BUILD SUCCESSFUL.

- [ ] **Step 4: Проверить на устройстве**

```bash
./gradlew.bat :app:installDebug
```

Сценарий: в полном экране ленты включить поворот (`autoRotate`), свайпнуть на следующий ролик, выключить поворот, свайпнуть ещё раз. Ожидаемо: поворот применяется только там, где включён, и не «залипает» на новых страницах.

- [ ] **Step 5: Коммит**

```bash
git add feature-r/src/main/java/com/client/xvideos/r/ui/video/RedPooledVideoPlayer.kt core/src/main/java/com/client/xvideos/common/videoplayer/feed/FeedPlayerState.kt
git commit -m "fix(r): снимать эффекты и громкость с плеера при возврате в пул"
```

---

## Task 12: Проверять главный поток явно (A5)

Контракт `@MainThread` у `FeedPlayerState` живёт только в KDoc. `PlayerPool` свою проверку делает сам (`verifyMainThread()`) — приводим класс к тому же уровню.

**Files:**
- Modify: `core/src/main/java/com/client/xvideos/common/videoplayer/feed/FeedPlayerState.kt`

- [ ] **Step 1: Добавить проверку и расставить вызовы**

В `FeedPlayerState` перед `companion object` добавить:

```kotlin
    /**
     * Тот же контракт, что у `PlayerPool.verifyMainThread()`: реестр прогрева и
     * `DefaultPreloadManager` не потокобезопасны, а нарушение видно не сразу —
     * оно всплывает рассинхроном учёта через десятки свайпов.
     */
    private fun verifyMainThread() {
        check(Looper.myLooper() == Looper.getMainLooper()) {
            "FeedPlayerState: все методы только с главного потока"
        }
    }
```

Первой строкой в теле методов `mediaSourceFor`, `updateCurrentPage`, `addRange`, `retryPending`, `removeRange`, `setForegroundMode` и `release` добавить:

```kotlin
        verifyMainThread()
```

- [ ] **Step 2: Добавить импорт**

```kotlin
import android.os.Looper
```

- [ ] **Step 3: Собрать**

```bash
./gradlew.bat :core:compileDebugKotlin
```

Ожидаемо: BUILD SUCCESSFUL.

- [ ] **Step 4: Проверить, что приложение не падает на проверке**

```bash
./gradlew.bat :app:installDebug
```

Открыть ленту R, полный экран, пролистать 10 роликов, свернуть и развернуть приложение, выйти из экрана. Ожидаемо: без `IllegalStateException: FeedPlayerState`.

- [ ] **Step 5: Коммит**

```bash
git add core/src/main/java/com/client/xvideos/common/videoplayer/feed/FeedPlayerState.kt
git commit -m "fix(core): проверять главный поток в FeedPlayerState"
```

---

## Task 13: Не удерживать View счётчиком keepScreenOn (C8)

`mutableMapOf<View, Int>` держит сильную ссылку на host-view: любой непарный `acquire` превращается в утечку всей иерархии.

**Files:**
- Modify: `core/src/main/java/com/client/xvideos/common/videoplayer/util/CMPlayer2.kt`

- [ ] **Step 1: Заменить карту**

Заменить объявление `counts` в `KeepScreenOnCounter` на:

```kotlin
    // WeakHashMap, а не mutableMapOf: карта переживает свои View (счётчик —
    // process-wide object), и непарный release превратил бы её в утечку всей
    // Compose-иерархии. Слабый ключ делает такую утечку невозможной.
    private val counts = WeakHashMap<View, Int>()
```

- [ ] **Step 2: Добавить импорт**

```kotlin
import java.util.WeakHashMap
```

- [ ] **Step 3: Собрать**

```bash
./gradlew.bat :core:compileDebugKotlin
```

Ожидаемо: BUILD SUCCESSFUL.

- [ ] **Step 4: Коммит**

```bash
git add core/src/main/java/com/client/xvideos/common/videoplayer/util/CMPlayer2.kt
git commit -m "fix(core): слабые ключи в счётчике keepScreenOn"
```

---

## Task 14: Привести окно предзагрузки списков в порядок (A6)

Имя файла не совпадает с публичной функцией, доли не проверяются — отрицательное значение даст отрицательное окно.

**Files:**
- Rename: `core/src/main/java/com/client/xvideos/common/ui/lazy/ViewportCacheWindow.kt` → `core/src/main/java/com/client/xvideos/common/ui/lazy/ViewportFractionCacheWindow.kt`
- Modify: тот же файл

- [ ] **Step 1: Переименовать файл**

```bash
git mv core/src/main/java/com/client/xvideos/common/ui/lazy/ViewportCacheWindow.kt core/src/main/java/com/client/xvideos/common/ui/lazy/ViewportFractionCacheWindow.kt
```

- [ ] **Step 2: Добавить проверку долей**

В `viewportFractionCacheWindow` первой строкой тела добавить:

```kotlin
): LazyLayoutCacheWindow {
    require(ahead >= 0f && behind >= 0f) {
        "Доли окна не могут быть отрицательными: ahead=$ahead, behind=$behind"
    }
    return ViewportFractionCacheWindow(ahead, behind)
}
```

(то есть однострочное выражение `= ViewportFractionCacheWindow(ahead, behind)` заменяется на блок выше)

- [ ] **Step 3: Собрать**

```bash
./gradlew.bat :core:compileDebugKotlin :feature-r:compileDebugKotlin :feature-x:compileDebugKotlin
```

Ожидаемо: BUILD SUCCESSFUL.

- [ ] **Step 4: Коммит**

```bash
git add core/src/main/java/com/client/xvideos/common/ui/lazy/
git commit -m "refactor(core): имя файла по функции и проверка долей окна предзагрузки"
```

---

# ФАЗА 4 — ГИГИЕНА И РЕШЕНИЯ

## Task 15: Переименовать `isBuferring`

Опечатка в имени публичного параметра composable.

**Files:**
- Modify: `feature-r/src/main/java/com/client/xvideos/r/ui/video/RedPooledVideoPlayer.kt`
- Modify: `feature-r/src/main/java/com/client/xvideos/r/ui/fullscreen/ScreenRedFullScreen.kt`

- [ ] **Step 1: Переименовать параметр**

В `RedPooledVideoPlayer.kt` заменить `isBuferring: (Boolean) -> Unit,` на `onBufferingChanged: (Boolean) -> Unit,` и вызов `LaunchedEffect(isBuffering) { isBuferring(isBuffering) }` на:

```kotlin
    LaunchedEffect(isBuffering) { onBufferingChanged(isBuffering) }
```

- [ ] **Step 2: Обновить вызов**

В `ScreenRedFullScreen.kt` в `RedFullScreenPage` заменить `isBuferring = { buffering ->` на `onBufferingChanged = { buffering ->`.

- [ ] **Step 3: Собрать**

```bash
./gradlew.bat :feature-r:compileDebugKotlin
```

Ожидаемо: BUILD SUCCESSFUL.

- [ ] **Step 4: Коммит**

```bash
git add feature-r/src/main/java/com/client/xvideos/r/ui/video/RedPooledVideoPlayer.kt feature-r/src/main/java/com/client/xvideos/r/ui/fullscreen/ScreenRedFullScreen.kt
git commit -m "refactor(r): исправить опечатку в имени параметра isBuferring"
```

---

## Task 16: Подтвердить состав merged manifest (B2)

Комментарий в манифесте утверждает, что три разрешения ушли из merged manifest. Читая исходники, это не проверить.

**Files:**
- Modify: `app/src/main/AndroidManifest.xml` (только если проверка не подтвердится)

- [ ] **Step 1: Собрать merged manifest**

```bash
./gradlew.bat :app:processReleaseManifest
```

Ожидаемо: BUILD SUCCESSFUL.

- [ ] **Step 2: Проверить наличие разрешений**

```bash
grep -nE "FOREGROUND_SERVICE|POST_NOTIFICATIONS|WAKE_LOCK" app/build/intermediates/merged_manifest/release/processReleaseManifest/AndroidManifest.xml
```

Ожидаемо: пусто (нулевой вывод, `grep` вернёт код 1).

- [ ] **Step 3: Развилка**

Если вывод пуст — комментарий верен, менять ничего не нужно, перейти к шагу 4.

Если какое-то разрешение нашлось — найти источник:

```bash
grep -nE "FOREGROUND_SERVICE|POST_NOTIFICATIONS|WAKE_LOCK" app/build/outputs/logs/manifest-merger-release-report.txt
```

и переписать комментарий в `app/src/main/AndroidManifest.xml`, указав реальный источник вместо утверждения, что разрешения ушли.

- [ ] **Step 4: Коммит**

Если правок не потребовалось — коммита нет, просто отметить задачу выполненной.

Если комментарий правился:

```bash
git add app/src/main/AndroidManifest.xml
git commit -m "docs: поправить комментарий о разрешениях по отчёту merger"
```

---

## Task 17: Сузить видимость media3-зависимостей (B4)

`api` для модулей, которых нет в публичном API `core`, тянет их в compile-classpath каждого feature-модуля.

**Files:**
- Modify: `core/build.gradle`

- [ ] **Step 1: Заменить `api` на `implementation`**

В блоке `dependencies` заменить четыре строки:

```groovy
    api libs.androidx.media3.extractor
    api libs.androidx.media3.decoder
    api libs.androidx.media3.datasource.ktor
    api libs.androidx.media3.database
```

на:

```groovy
    // implementation, а не api: прямых импортов у них нет ни в core, ни в
    // feature-модулях — extractor и decoder нужны рантайму exoplayer, ktor-датасорс
    // и database живут внутри VideoHttpDataSource и FeedVideoCache. Явная строка
    // всё так же фиксирует версию через version.ref.
    implementation libs.androidx.media3.extractor
    implementation libs.androidx.media3.decoder
    implementation libs.androidx.media3.datasource.ktor
    implementation libs.androidx.media3.database
```

Комментарий про «extractor и decoder оставлены явно», стоявший над этими строками, заменён текстом выше — старый удалить.

- [ ] **Step 2: Собрать все модули**

```bash
./gradlew.bat :app:assembleDebug
```

Ожидаемо: BUILD SUCCESSFUL. Если какой-то feature-модуль не соберётся из-за `Unresolved reference` на media3-класс — вернуть соответствующую строку в `api` и дописать в комментарий, кто именно её использует.

- [ ] **Step 3: Коммит**

```bash
git add core/build.gradle
git commit -m "build(core): implementation вместо api для внутренних media3-модулей"
```

---

## Task 18: Защитить ключи списков фильтров от дублей (B5)

`key = { it }` и `key = { it.title }` роняют `LazyColumn` при дублирующемся ключе. Путь добавления через `rememberSelectableTags` дубли исключает, но фильтр приходит и из сохранённых запросов. Дедупликация в источнике надёжнее, чем ключ с индексом: она сохраняет смысл ключа при перестановке элементов.

**Files:**
- Modify: `feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/molecule/filter/atom/AlbumListFilterTags.kt`
- Modify: `feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/molecule/filter/atom/AlbumListFilterGenres.kt`

- [ ] **Step 1: Дедуплицировать теги**

В `AlbumListFilterTags.kt` заменить:

```kotlin
    val tagsPlus = filter.tagPlus
    val tagsMinus = filter.tagMinus
```

на:

```kotlin
    // distinct обязателен: ниже эти списки уходят в LazyColumn с key = { it },
    // а дублирующийся ключ роняет список. Фильтр приходит и из сохранённых
    // запросов, где дубль технически возможен.
    val tagsPlus = remember(filter.tagPlus) { filter.tagPlus.distinct() }
    val tagsMinus = remember(filter.tagMinus) { filter.tagMinus.distinct() }
```

- [ ] **Step 2: Дедуплицировать жанры**

В `AlbumListFilterGenres.kt` заменить:

```kotlin
    val genresPlus = filter.genresPlus
    val genresMinus = filter.genresMinus
```

на:

```kotlin
    // distinctBy обязателен: ниже списки уходят в LazyColumn с key = { it.title },
    // а дублирующийся ключ роняет список.
    val genresPlus = remember(filter.genresPlus) { filter.genresPlus.distinctBy { it.title } }
    val genresMinus = remember(filter.genresMinus) { filter.genresMinus.distinctBy { it.title } }
```

`import androidx.compose.runtime.remember` в обоих файлах уже есть — добавлять не нужно.

- [ ] **Step 3: Собрать**

```bash
./gradlew.bat :feature-l:compileDebugKotlin
```

Ожидаемо: BUILD SUCCESSFUL.

- [ ] **Step 4: Коммит**

```bash
git add feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/molecule/filter/atom/
git commit -m "fix(l): снимать дубли перед выдачей фильтров в списки с ключами"
```

---

## Task 19: Зафиксировать разобранные решения в коде (P3, P4, P5)

Три пункта ревью закрываются документацией — см. раздел «Правки ревью, принятые до начала работ». Чтобы следующий читатель не открыл их заново, разбор переезжает в код.

**Files:**
- Modify: `core/src/main/java/com/client/xvideos/common/videoplayer/feed/FeedPlayerState.kt`
- Modify: `feature-r/src/main/java/com/client/xvideos/r/ui/fullscreen/ScreenRedFullScreen.kt`

- [ ] **Step 1: Обосновать ёмкость пула (P4)**

В `FeedPlayerState.kt` заменить `companion object` на:

```kotlin
    companion object {
        /**
         * Ёмкость пула ровно по числу живых страниц пейджера
         * (`beyondViewportPageCount = 1` даёт три страницы), без запаса — и этого
         * достаточно.
         *
         * Compose в `applyChanges` вызывает `onForgotten` уходящей страницы раньше
         * `onRemembered` пришедшей, то есть `PlayerPool.yield()` возвращает плеер в
         * канал до того, как новая страница вообще начнёт его просить. Вдобавок
         * `rememberPooledPlayer` зовёт `acquire()` из `scope.launch`, то есть уже
         * после кадра. К моменту `acquire()` свободный плеер в канале есть, и
         * `tryReceive()` срабатывает сразу — спиннера от исчерпания пула не бывает.
         *
         * Поднимать ёмкость до четырёх значит вернуть потребление памяти, ради
         * снижения которого пул и вводился.
         */
        const val DEFAULT_POOL_CAPACITY = 3
    }
```

- [ ] **Step 2: Обосновать общий путь на одиночном экране (P5)**

В `ScreenRedFullScreen.kt` над `RedFullScreenSingle` добавить KDoc:

```kotlin
/**
 * Экран одного ролика — вход без ленты (`feedKey == null`).
 *
 * Использует тот же [FeedPlayerState], что и лента, хотя прогревать здесь нечего:
 * альтернатива — второй путь создания плеера, то есть ровно та дублирующая
 * обвязка, которую ветка удаляла. Цена общего пути мала: `SimpleCache` и так
 * процессный и общий с лентой, `DefaultPreloadManager` без элементов ничего не
 * делает, пул сведён к одному плееру через `poolCapacity = 1`.
 */
```

- [ ] **Step 3: Обосновать `batchSize` (P3)**

Комментарий про батч уже добавлен в задаче 7 (шаг 1). Проверить, что он на месте:

```bash
grep -n "Батч применяется только при восстановлении после прыжка" feature-r/src/main/java/com/client/xvideos/r/ui/fullscreen/ScreenRedFullScreen.kt
```

Ожидаемо: одна строка. Если пусто — задача 7 не выполнена, вернуться к ней.

- [ ] **Step 4: Собрать**

```bash
./gradlew.bat :core:compileDebugKotlin :feature-r:compileDebugKotlin
```

Ожидаемо: BUILD SUCCESSFUL.

- [ ] **Step 5: Коммит**

```bash
git add core/src/main/java/com/client/xvideos/common/videoplayer/feed/FeedPlayerState.kt feature-r/src/main/java/com/client/xvideos/r/ui/fullscreen/ScreenRedFullScreen.kt
git commit -m "docs: зафиксировать разбор ёмкости пула и одиночного экрана"
```

---

## Task 20: Отметить сознательное использование alpha material3 (B3)

`material3 = 1.5.0-alpha26` уходит в master, и alpha уже правит API (`LazyExpandMenuAnchor.kt` пришлось дописать импорт `ExposedDropdownMenu`).

**Files:**
- Modify: `gradle/libs.versions.toml`

- [ ] **Step 1: Добавить комментарий к версии**

Заменить строку `material3 = "1.5.0-alpha26"` на:

```toml
# Alpha сознательно: 1.5.0 в стабильном канале ещё нет, а нужные компоненты
# (ExposedDropdownMenu в новом виде) есть только здесь. Обновляем вместе с
# composeBom и проверяем compileDebugKotlin всех feature-модулей — alpha правит
# API между сборками.
material3 = "1.5.0-alpha26"
```

- [ ] **Step 2: Проверить, что каталог версий читается**

```bash
./gradlew.bat :app:dependencies --configuration debugRuntimeClasspath > NUL
```

Ожидаемо: BUILD SUCCESSFUL.

- [ ] **Step 3: Коммит**

```bash
git add gradle/libs.versions.toml
git commit -m "docs(deps): пояснить выбор alpha material3"
```

---

# Финальная проверка

- [ ] **Step 1: Полная сборка и все тесты**

```bash
./gradlew.bat clean :app:assembleDebug :core:testDebugUnitTest :feature-r:testDebugUnitTest
```

Ожидаемо: BUILD SUCCESSFUL. Тестов: `FeedPreloadPolicyTest` (8), `FeedPreloadRegistryTest` (8), `DownloadedVideoKeysTest` (4).

- [ ] **Step 2: Прогон на устройстве**

```bash
./gradlew.bat :app:installDebug
```

Сценарий целиком:
1. Лента R, сетка — прокрутить вниз 3 экрана, вверх 3 экрана. Превью не мигают.
2. Тап по ролику из середины — полный экран открывается и стартует без долгого спиннера.
3. Свайп вниз 15 роликов подряд быстро — без краша, без чёрных кадров.
4. На каждом из первых трёх роликов сразу после свайпа потянуть полосу времени — перемотка работает.
5. Включить A-B, проверить петлю; выключить.
6. Свернуть приложение на середине ролика — звук замолкает. Развернуть — воспроизведение продолжается.
7. Скачать ролик, вернуться в ленту, открыть его же — играет с диска (проверяется отсутствием сетевых запросов в logcat).
8. Выйти из полного экрана, зайти снова — без роста памяти между заходами.

Проверить logcat на исключения:

```bash
adb logcat -d -s AndroidRuntime | tail -40
```

Ожидаемо: пусто.

- [ ] **Step 3: Проверить, что ничего не забыто**

```bash
git log --oneline master..HEAD | head -25
```

Ожидаемо: 18 новых коммитов поверх `c7c1fe7` (задачи 16 и 19 могут не дать коммита, если правок не потребовалось).

---

## Соответствие плана замечаниям ревью

| Замечание | Задача | Как закрыто |
|---|---|---|
| C1 двойной add | 2 | `FeedPreloadRegistry` + `PreloadAction`, тесты |
| C2 нет invalidate | 3 | `invalidate()` после add, `updateCurrentPage(0)` на одиночном |
| C3 checkNotNull | 4 | Деградация на `DefaultMediaSourceFactory` |
| C4 controls без владельца | 9 | `DisposableEffect` + `onPlayerControlsRelease` |
| C5 settledPage vs currentPage | 7 | Оба провайдера на `currentPage` |
| C6 foreground mode в фоне | 10 | `setForegroundMode` + `LifecycleStartEffect` |
| C7 фантомные индексы | 7 | `itemCountProvider = { listGifs.itemCount }` |
| C8 сильная ссылка на View | 13 | `WeakHashMap` |
| P1 File.exists на UI-потоке | 5 | `downloadedVideoKeys: StateFlow<Set<String>>`, тесты |
| P2 Pair 20 раз в секунду | 6 | `fun interface FeedTimeListener` |
| P3 мёртвый batchSize | 7, 19 | Комментарий с разбором ветки reconcile |
| P4 ёмкость пула | 19 | Разбор в KDoc, кода не меняем |
| P5 полный state на одиночном | 3, 19 | `updateCurrentPage(0)` + KDoc с обоснованием |
| A1 endsWith(".m3u8") | 8 | `Util.inferContentType` |
| A2 четыре роли в классе | 2 | Учёт вынесен в `FeedPreloadRegistry` |
| A3 mediaId = индекс | 2 | `PreloadAction.Replace` снимает старый элемент при съезде индексов |
| A4 пул не сбрасывает состояние | 11 | `playerTeardown` + KDoc-инвариант |
| A5 @MainThread только в KDoc | 12 | `verifyMainThread()` |
| A6 имя файла, валидация, UA | 14, 15 | Переименование, `require`, `onBufferingChanged`. UA остаётся в core — им пользуются и R, и X через `ExoplayerHelper` |
| B1 wrapper | 1 | Возврат настроек + `distributionSha256Sum` |
| B2 merged manifest | 16 | Проверка по отчёту merger |
| B3 alpha material3 | 20 | Комментарий в каталоге версий |
| B4 api вместо implementation | 17 | Сужение видимости четырёх модулей |
| B5 ключи списков | 18 | `distinct()` в источнике |
