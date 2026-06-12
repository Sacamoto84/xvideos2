# P2P inbox/outbox Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Временные staging-папки `/xvideos/inbox` и `/xvideos/outbox`: отправка нескачанных L-item через outbox, приём всех бандлов через inbox с переносом в боевой store после успеха.

**Architecture:** Обе папки зеркалируют структуру `/xvideos` от корня, поэтому `relativePath` манифеста не меняется (полная совместимость протокола). Отправитель: новая фаза `Preparing` в `P2pShareController` — `bundleProvider` качает item в outbox через существующий `lPersistPicsDetailsToFolder`. Приёмник: `StoreBundleImporter` ставит файлы в зеркало inbox, затем `P2pInboxMerger` переносит всё содержимое в `/xvideos` (rename, та же ФС) и дёргает refresh.

**Tech Stack:** Kotlin, Compose + Voyager, Hilt (EntryPoint), Nearby Connections, JUnit4 + kotlinx-coroutines-test, Gson.

**Spec:** `docs/superpowers/specs/2026-06-12-p2p-inbox-outbox-design.md`

**Структура файлов:**

| Файл | Роль |
|---|---|
| Create `app/src/main/java/com/client/xvideos/common/p2p/P2pStaging.kt` | `mirrorRoot()` + `P2pInboxMerger` |
| Create `app/src/main/java/com/client/xvideos/common/p2p/P2pSendSource.kt` | sealed-источник для экрана отправки |
| Create `app/src/test/java/com/client/xvideos/common/p2p/P2pStagingTest.kt` | тесты mirror/merge |
| Modify `app/src/main/java/com/client/xvideos/common/AppPath.kt` | пути inbox/outbox + очистка при старте |
| Modify `app/src/main/java/com/client/xvideos/common/p2p/imports/StoreBundleImporter.kt` | install в inbox + merge |
| Modify `app/src/main/java/com/client/xvideos/common/p2p/P2pReceiveManager.kt` | проводка inboxRoot/mainRoot |
| Modify `app/src/main/java/com/client/xvideos/l/featured/saved/LMediaPersist.kt` | `Result<Unit>` → `Result<File>` |
| Modify `app/src/main/java/com/client/xvideos/common/p2p/P2pShareController.kt` | `bundleProvider` + `ShareState.Preparing` |
| Modify `app/src/main/java/com/client/xvideos/common/p2p/ui/ScreenP2pSend.kt` | приём `P2pSendSource`, фаза Preparing, очистка outbox |
| Modify `app/src/main/java/com/client/xvideos/l/ui/element/expandMenu/ExpandMenuVM.kt` | `startP2p` без ошибки «Сначала сохрани» |

Все команды — из корня репо, Windows: `.\gradlew.bat`.

---

### Task 1: P2pStaging — mirrorRoot + P2pInboxMerger (TDD)

**Files:**
- Create: `app/src/main/java/com/client/xvideos/common/p2p/P2pStaging.kt`
- Create: `app/src/test/java/com/client/xvideos/common/p2p/P2pStagingTest.kt`
- Test (доп.): `app/src/test/java/com/client/xvideos/common/p2p/export/ExportersTest.kt`

- [ ] **Step 1: Write the failing tests**

Создать `app/src/test/java/com/client/xvideos/common/p2p/P2pStagingTest.kt`:

```kotlin
package com.client.xvideos.common.p2p

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class P2pStagingTest {

    @get:Rule val tmp = TemporaryFolder()

    @Test
    fun `mirrorRoot maps store root inside staging base`() {
        val main = tmp.newFolder("xvideos")
        val inbox = File(main, "inbox")
        val store = File(main, "L/Likes")

        assertEquals(File(inbox, "L/Likes"), mirrorRoot(inbox, main, store))
    }

    @Test
    fun `merge moves files into main keeping structure and empties inbox`() {
        val main = tmp.newFolder("xvideos")
        val inbox = File(main, "inbox").apply { mkdirs() }
        File(inbox, "L/Likes/item1").mkdirs()
        File(inbox, "L/Likes/item1/media.jpg").writeText("M")
        File(inbox, "L/Likes/item1/metadata.json").writeText("{}")
        File(inbox, "X/Download").mkdirs()
        File(inbox, "X/Download/7.mp4").writeText("V")

        P2pInboxMerger.merge(inbox, main)

        assertEquals("M", File(main, "L/Likes/item1/media.jpg").readText())
        assertEquals("{}", File(main, "L/Likes/item1/metadata.json").readText())
        assertEquals("V", File(main, "X/Download/7.mp4").readText())
        // inbox пересоздан пустым
        assertTrue(inbox.exists())
        assertTrue(inbox.listFiles().isNullOrEmpty())
    }

    @Test
    fun `merge overwrites existing files in main`() {
        val main = tmp.newFolder("xvideos")
        val inbox = File(main, "inbox").apply { mkdirs() }
        File(main, "X/Download").mkdirs()
        File(main, "X/Download/7.mp4").writeText("OLD")
        File(inbox, "X/Download").mkdirs()
        File(inbox, "X/Download/7.mp4").writeText("NEW")

        P2pInboxMerger.merge(inbox, main)

        assertEquals("NEW", File(main, "X/Download/7.mp4").readText())
    }

    @Test
    fun `merge on missing inbox is a no-op`() {
        val main = tmp.newFolder("xvideos")
        P2pInboxMerger.merge(File(main, "inbox"), main)
        // не упало — этого достаточно
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.client.xvideos.common.p2p.P2pStagingTest"`
Expected: FAIL — compilation error, `mirrorRoot` / `P2pInboxMerger` не существуют.

- [ ] **Step 3: Write implementation**

Создать `app/src/main/java/com/client/xvideos/common/p2p/P2pStaging.kt`:

```kotlin
package com.client.xvideos.common.p2p

import java.io.File

/**
 * Зеркало store-корня внутри staging-папки (inbox/outbox):
 * `base/<storeRoot относительно mainRoot>`. Благодаря зеркалу relativePath
 * манифеста, посчитанный от staging-корня, идентичен боевому.
 */
fun mirrorRoot(base: File, mainRoot: File, storeRoot: File): File =
    File(base, storeRoot.absoluteFile.normalize().relativeTo(mainRoot.absoluteFile.normalize()).path)

/**
 * Переносит ВСЁ содержимое inbox в main с сохранением структуры: rename
 * (та же ФС — мгновенно), fallback copy+delete. Существующие файлы
 * перезаписываются. После переноса inbox пересоздаётся пустым.
 */
object P2pInboxMerger {

    fun merge(inboxRoot: File, mainRoot: File) {
        if (!inboxRoot.exists()) return
        inboxRoot.walkTopDown().filter { it.isFile }.forEach { file ->
            val target = File(mainRoot, file.relativeTo(inboxRoot).path)
            target.parentFile?.mkdirs()
            if (target.exists()) target.delete()
            if (!file.renameTo(target)) {
                file.copyTo(target, overwrite = true)
                file.delete()
            }
        }
        inboxRoot.deleteRecursively()
        inboxRoot.mkdirs()
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.client.xvideos.common.p2p.P2pStagingTest"`
Expected: PASS (4 теста).

- [ ] **Step 5: Add equivalence test — LExporter от outbox-зеркала даёт те же relativePath**

В `app/src/test/java/com/client/xvideos/common/p2p/export/ExportersTest.kt` добавить импорты и тест:

```kotlin
import com.client.xvideos.common.p2p.P2pManifestFactory
import com.client.xvideos.common.p2p.mirrorRoot
```

```kotlin
    @Test
    fun `L exporter from outbox mirror yields same relative paths as store`() {
        val main = tmp.newFolder("xvideos")
        val storeLikes = File(main, "L/Likes").apply { mkdirs() }
        val outboxLikes = mirrorRoot(File(main, "outbox"), main, storeLikes).apply { mkdirs() }
        for (root in listOf(storeLikes, outboxLikes)) {
            val folder = File(root, "album_q").apply { mkdirs() }
            File(folder, "media.jpg").writeText("m")
            File(folder, "metadata.json").writeText("{}")
        }

        fun relPaths(root: File): List<String> {
            val b = LExporter.export(File(root, "album_q"))!!
            val manifest = P2pManifestFactory.create(
                type = b.type,
                storeRoot = b.storeRoot,
                files = b.files,
                metadataFile = b.metadataFile,
                payloadIds = b.files.withIndex().associate { (i, f) -> f to (1000L + i) },
            )
            return manifest.files.map { it.relativePath }
        }

        assertEquals(relPaths(storeLikes), relPaths(outboxLikes))
    }
```

- [ ] **Step 6: Run exporter tests**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.client.xvideos.common.p2p.export.ExportersTest"`
Expected: PASS (4 теста).

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/client/xvideos/common/p2p/P2pStaging.kt app/src/test/java/com/client/xvideos/common/p2p/P2pStagingTest.kt app/src/test/java/com/client/xvideos/common/p2p/export/ExportersTest.kt
git commit -m "feat(p2p): mirrorRoot and P2pInboxMerger for staging folders"
```

---

### Task 2: AppPath — пути inbox/outbox + очистка при старте

`AppPath` завязан на `android.os.Environment` — юнит-тестов нет (как и у остального `AppPath`); проверка — компиляция и Task 3/5, где пути используются.

**Files:**
- Modify: `app/src/main/java/com/client/xvideos/common/AppPath.kt`

- [ ] **Step 1: Add paths, init, and clear functions**

В `AppPath.kt` после блока `//--- L ---` (после `val l_collection`) добавить:

```kotlin
    //--- P2P staging ---
    /**
     * Временные папки P2P. Содержимое зеркалирует структуру `/xvideos`
     * (`inbox/L/Likes/...`), что позволяет переносить принятое в корень
     * одним merge. Очищаются при старте приложения и после успешной передачи.
     */
    val p2p_inbox: String = "$main/inbox"
    val p2p_outbox: String = "$main/outbox"
```

В `init {}` после `File(x_cache_download).mkdirs()` добавить:

```kotlin
        File(p2p_inbox).mkdirs()
        File(p2p_outbox).mkdirs()
```

В `initInternalStorage()` после `clearLShareCache()` добавить:

```kotlin
        clearP2pInbox()
        clearP2pOutbox()
```

В конец объекта (после `clearLShareCache()`) добавить:

```kotlin
    fun clearP2pInbox() = clearStagingDir(File(p2p_inbox))

    fun clearP2pOutbox() = clearStagingDir(File(p2p_outbox))

    private fun clearStagingDir(dir: File) {
        dir.deleteRecursively()
        dir.mkdirs()
    }
```

- [ ] **Step 2: Compile**

Run: `.\gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/client/xvideos/common/AppPath.kt
git commit -m "feat(p2p): inbox/outbox staging paths cleared on app start"
```

---

### Task 3: StoreBundleImporter через inbox (TDD)

**Files:**
- Modify: `app/src/main/java/com/client/xvideos/common/p2p/imports/StoreBundleImporter.kt`
- Modify: `app/src/main/java/com/client/xvideos/common/p2p/P2pReceiveManager.kt:56-69`
- Test: `app/src/test/java/com/client/xvideos/common/p2p/imports/StoreBundleImporterTest.kt`

- [ ] **Step 1: Rewrite the test for inbox staging**

Заменить содержимое `StoreBundleImporterTest.kt` целиком:

```kotlin
package com.client.xvideos.common.p2p.imports

import com.client.xvideos.common.p2p.P2pManifest
import com.client.xvideos.common.p2p.P2pManifestFile
import com.client.xvideos.common.p2p.P2pType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class StoreBundleImporterTest {

    @get:Rule val tmp = TemporaryFolder()

    @Test
    fun `installs via inbox then merges into store root and triggers refresh`() = runTest {
        val main = tmp.newFolder("xvideos")
        val xRoot = File(main, "X/Download").apply { mkdirs() }
        val inbox = File(main, "inbox").apply { mkdirs() }
        val received = tmp.newFile("p1").apply { writeText("VID") }
        var refreshed: P2pType? = null

        val importer = StoreBundleImporter(
            storeRootFor = { type -> if (type == P2pType.X) xRoot else error("unexpected") },
            refreshFor = { type -> refreshed = type },
            inboxRoot = inbox,
            mainRoot = main,
        )

        val manifest = P2pManifest(
            type = P2pType.X,
            metadataFileName = "8.info",
            files = listOf(P2pManifestFile("8.mp4", "8.mp4", 1L, 3L)),
        )

        importer.import(manifest, mapOf(1L to received))

        val target = File(xRoot, "8.mp4")
        assertTrue(target.exists())
        assertEquals("VID", target.readText())
        // staging опустошён после merge
        assertTrue(inbox.listFiles().isNullOrEmpty())
        assertEquals(P2pType.X, refreshed)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.client.xvideos.common.p2p.imports.StoreBundleImporterTest"`
Expected: FAIL — compilation error, у `StoreBundleImporter` нет параметров `inboxRoot`/`mainRoot`.

- [ ] **Step 3: Implement importer staging**

Заменить содержимое `StoreBundleImporter.kt` целиком:

```kotlin
package com.client.xvideos.common.p2p.imports

import com.client.xvideos.common.p2p.P2pBundleInstaller
import com.client.xvideos.common.p2p.P2pInboxMerger
import com.client.xvideos.common.p2p.P2pManifest
import com.client.xvideos.common.p2p.P2pType
import com.client.xvideos.common.p2p.mirrorRoot
import java.io.File

/** Контракт импорта принятого бандла. */
fun interface BundleImporter {
    suspend fun import(manifest: P2pManifest, receivedFiles: Map<Long, File>)
}

/**
 * Ставит принятый бандл в зеркало inbox, переносит содержимое inbox в main
 * (боевой store) и дёргает refresh. Полуполученный бандл в боевой store не
 * попадает: import зовётся только при «манифест + все файлы».
 *
 * @param storeRootFor корень store по типу (вызывающий подставляет `AppPath.*`).
 * @param refreshFor перечитать список store нужного типа (вызывающий подставляет `saved*.refresh()`).
 * @param inboxRoot staging-папка приёма (`AppPath.p2p_inbox`).
 * @param mainRoot корень `/xvideos` (`AppPath.main`); store-корни лежат внутри него.
 */
class StoreBundleImporter(
    private val storeRootFor: (P2pType) -> File,
    private val refreshFor: (P2pType) -> Unit,
    private val inboxRoot: File,
    private val mainRoot: File,
) : BundleImporter {

    override suspend fun import(manifest: P2pManifest, receivedFiles: Map<Long, File>) {
        val staging = mirrorRoot(inboxRoot, mainRoot, storeRootFor(manifest.type))
        P2pBundleInstaller.install(staging, manifest, receivedFiles)
        P2pInboxMerger.merge(inboxRoot, mainRoot)
        refreshFor(manifest.type)
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.client.xvideos.common.p2p.imports.StoreBundleImporterTest"`
Expected: PASS.

- [ ] **Step 5: Wire inbox roots in P2pReceiveManager**

В `P2pReceiveManager.kt` в вызове конструктора `StoreBundleImporter` (строка ~56) добавить два аргумента после `refreshFor`:

```kotlin
        val storeImporter = StoreBundleImporter(
            storeRootFor = { type ->
                when (type) {
                    P2pType.X -> File(AppPath.x_cache_download)
                    // R сюда не попадает — идёт через RLikesBundleImporter.
                    P2pType.R -> File(AppPath.r_cache_download)
                    P2pType.L -> File(AppPath.l_likes)
                }
            },
            refreshFor = { type ->
                // X: экран Saved перечитывает список при открытии.
                if (type == P2pType.L) entryPoint.savedL().likes.refresh()
            },
            inboxRoot = File(AppPath.p2p_inbox),
            mainRoot = File(AppPath.main),
        )
```

- [ ] **Step 6: Compile and run all p2p tests**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.client.xvideos.common.p2p.*"`
Expected: PASS, без падений в остальных p2p-тестах.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/client/xvideos/common/p2p/imports/StoreBundleImporter.kt app/src/main/java/com/client/xvideos/common/p2p/P2pReceiveManager.kt app/src/test/java/com/client/xvideos/common/p2p/imports/StoreBundleImporterTest.kt
git commit -m "feat(p2p): receive bundles into inbox staging, merge into store after success"
```

---

### Task 4: lPersistPicsDetailsToFolder возвращает папку item'а

Outbox-скачиванию нужна папка результата для `LExporter.export(folder)`. Оба текущих call-site (`SavedL_Likes.kt:40`, `SavedL_Collection.kt:223`) используют результат только через `onSuccess`/`onFailure` с выводом типов — правки им не нужны.

**Files:**
- Modify: `app/src/main/java/com/client/xvideos/l/featured/saved/LMediaPersist.kt:295-391`

- [ ] **Step 1: Change return type**

В `LMediaPersist.kt`:

1. Сигнатура (строка ~295-300): `): Result<Unit> {` → `): Result<File> {`
   (если возвращаемый тип не указан явно — добавить; KDoc дополнить строкой `* @return папка сохранённого item'а.`).
2. В конце `runCatching`-блока (строка ~387) заменить последнее выражение `Unit` на `folder`.

Итоговый хвост функции:

```kotlin
            writeLSavedLikeMetadata(File(folder, L_METADATA_FILE_NAME), metadata)
        } catch (e: Exception) {
            if (folder.listFiles().isNullOrEmpty() || !File(folder, L_METADATA_FILE_NAME).exists()) {
                folder.deleteRecursively()
            }
            throw e
        } finally {
            client.close()
        }
        folder
    }.also {
        if (progressStarted) progress.finish()
    }
```

- [ ] **Step 2: Compile and run L tests**

Run: `.\gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL (call-sites компилируются без правок).

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.client.xvideos.l.*"`
Expected: PASS (или «no tests found» — тогда достаточно компиляции).

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/client/xvideos/l/featured/saved/LMediaPersist.kt
git commit -m "refactor(l): lPersistPicsDetailsToFolder returns saved item folder"
```

---

### Task 5: P2pSendSource (TDD)

**Files:**
- Create: `app/src/main/java/com/client/xvideos/common/p2p/P2pSendSource.kt`
- Create: `app/src/test/java/com/client/xvideos/common/p2p/P2pSendSourceTest.kt`

- [ ] **Step 1: Write the failing test**

Создать `app/src/test/java/com/client/xvideos/common/p2p/P2pSendSourceTest.kt`:

```kotlin
package com.client.xvideos.common.p2p

import com.client.xvideos.l.model.PicsDetails
import org.junit.Assert.assertEquals
import org.junit.Test

class P2pSendSourceTest {

    @Test
    fun `DownloadL round-trips PicsDetails through json`() {
        val item = PicsDetails(
            height = 846,
            width = 1280,
            is_animated = false,
            // остальные параметры — значениями по умолчанию / null,
            // подставить обязательные параметры конструктора по факту компиляции
        )

        val source = P2pSendSource.DownloadL.of(item)
        val restored = source.item()

        assertEquals(item.height, restored?.height)
        assertEquals(item.width, restored?.width)
        assertEquals(item.is_animated, restored?.is_animated)
    }
}
```

Примечание исполнителю: у `PicsDetails` (`app/src/main/java/com/client/xvideos/l/model/PicsDetails.kt:57`) могут быть обязательные параметры без default — заполнить минимальными значениями (`""`, `null`, `0`), цель теста — round-trip Gson, не полнота полей.

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.client.xvideos.common.p2p.P2pSendSourceTest"`
Expected: FAIL — compilation error, `P2pSendSource` не существует.

- [ ] **Step 3: Implement P2pSendSource**

Создать `app/src/main/java/com/client/xvideos/common/p2p/P2pSendSource.kt`:

```kotlin
package com.client.xvideos.common.p2p

import com.client.xvideos.l.model.PicsDetails
import com.google.gson.Gson
import java.io.Serializable

/**
 * Источник данных для экрана отправки P2P.
 *
 * Serializable: лежит внутри Voyager-экрана `ScreenP2pSend`, который Android
 * сохраняет в saved instance state при сворачивании (см. [P2pExportBundle]).
 * [PicsDetails] — Parcelable, не Serializable, поэтому [DownloadL] хранит его
 * Gson-JSON'ом.
 */
sealed interface P2pSendSource : Serializable {

    /** Бандл уже в store — отправляем как есть. */
    data class Ready(val bundle: P2pExportBundle) : P2pSendSource

    /** Item не скачан: качаем в outbox на экране отправки и шлём оттуда. */
    data class DownloadL(val itemJson: String) : P2pSendSource {

        fun item(): PicsDetails? =
            runCatching { Gson().fromJson(itemJson, PicsDetails::class.java) }.getOrNull()

        companion object {
            fun of(item: PicsDetails) = DownloadL(Gson().toJson(item))
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.client.xvideos.common.p2p.P2pSendSourceTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/client/xvideos/common/p2p/P2pSendSource.kt app/src/test/java/com/client/xvideos/common/p2p/P2pSendSourceTest.kt
git commit -m "feat(p2p): P2pSendSource - ready bundle or download-to-outbox item"
```

---

### Task 6: P2pShareController — bundleProvider + фаза Preparing (TDD)

Добавление `ShareState.Preparing` ломает exhaustive `when` в `ScreenP2pSend` — минимальная UI-ветка входит в эту же задачу, чтобы компиляция оставалась зелёной.

**Files:**
- Modify: `app/src/main/java/com/client/xvideos/common/p2p/P2pShareController.kt`
- Modify: `app/src/main/java/com/client/xvideos/common/p2p/ui/ScreenP2pSend.kt:115-117` (только ветка `when`)
- Test: `app/src/test/java/com/client/xvideos/common/p2p/P2pShareControllerTest.kt`

- [ ] **Step 1: Write the failing tests**

В `P2pShareControllerTest.kt` добавить импорты:

```kotlin
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runCurrent
```

и три теста в конец класса:

```kotlin
    @Test
    fun `stays in preparing while provider downloads then searches`() = runTest {
        val gate = CompletableDeferred<P2pExportBundle>()
        val fake = FakeNearbyClient()
        val controller = P2pShareController(
            nearby = fake,
            scope = backgroundScope,
            myName = "Sender",
            bundleProvider = { gate.await() },
        )

        controller.start()
        runCurrent()
        assertEquals(ShareState.Preparing, controller.state.value)

        val root = tmp.newFolder("outbox")
        val mp4 = File(root, "3.mp4").apply { writeText("V") }
        gate.complete(P2pExportBundle(P2pType.X, root, listOf(mp4), null))
        runCurrent()
        assertTrue(controller.state.value is ShareState.Searching)
    }

    @Test
    fun `prepare failure puts controller into error`() =
        runTest(UnconfinedTestDispatcher()) {
            val fake = FakeNearbyClient()
            val controller = P2pShareController(
                nearby = fake,
                scope = backgroundScope,
                myName = "Sender",
                bundleProvider = { error("network down") },
            )

            controller.start()

            assertTrue(controller.state.value is ShareState.Error)
        }

    @Test
    fun `bundle is prepared once across restarts`() =
        runTest(UnconfinedTestDispatcher()) {
            val root = tmp.newFolder("outbox")
            val mp4 = File(root, "3.mp4").apply { writeText("V") }
            val bundle = P2pExportBundle(P2pType.X, root, listOf(mp4), null)
            var calls = 0
            val fake = FakeNearbyClient()
            val controller = P2pShareController(
                nearby = fake,
                scope = backgroundScope,
                myName = "Sender",
                bundleProvider = { calls++; bundle },
            )

            controller.start()
            controller.start() // рестарт после разрыва — скачивание не повторяется

            assertEquals(1, calls)
            assertTrue(controller.state.value is ShareState.Searching)
        }
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.client.xvideos.common.p2p.P2pShareControllerTest"`
Expected: FAIL — compilation error, нет параметра `bundleProvider` и `ShareState.Preparing`.

- [ ] **Step 3: Implement Preparing + bundleProvider**

В `P2pShareController.kt`:

1. В `ShareState` после `data object Idle : ShareState` добавить:

```kotlin
    /** Подготовка файлов: скачивание item'а в outbox перед поиском устройств. */
    data object Preparing : ShareState
```

2. Заменить объявление класса и `start()`:

```kotlin
/**
 * Отправляющая сторона: готовит бандл ([bundleProvider] — мгновенно для store
 * или скачивание в outbox), ищет телефоны, по подключению шлёт файлы, затем манифест.
 */
class P2pShareController(
    private val nearby: NearbyClient,
    private val scope: CoroutineScope,
    private val myName: String,
    private val bundleProvider: suspend () -> P2pExportBundle,
) {
    /** Готовый бандл (store): без фазы скачивания. */
    constructor(
        nearby: NearbyClient,
        scope: CoroutineScope,
        myName: String,
        bundle: P2pExportBundle,
    ) : this(nearby, scope, myName, bundleProvider = { bundle })

    private val _state = MutableStateFlow<ShareState>(ShareState.Idle)
    val state: StateFlow<ShareState> = _state.asStateFlow()

    private val endpoints = linkedMapOf<String, P2pEndpoint>()
    private var targetEndpoint: String? = null
    private var eventsJob: kotlinx.coroutines.Job? = null
    private var prepareJob: kotlinx.coroutines.Job? = null

    /** Бандл, закешированный после первой подготовки — рестарт не качает заново. */
    private var bundle: P2pExportBundle? = null

    /** Payload'ы, поставленные в очередь и ещё не подтверждённые доставкой. */
    private val pendingPayloads = mutableSetOf<Long>()
    private var allEnqueued = false

    fun start() {
        Timber.d("P2P Sender: Starting (prepare + discovery)...")
        endpoints.clear()
        targetEndpoint = null
        pendingPayloads.clear()
        allEnqueued = false
        eventsJob?.cancel()
        prepareJob?.cancel()
        _state.value = ShareState.Preparing
        prepareJob = scope.launch {
            val prepared = try {
                bundle ?: bundleProvider().also { bundle = it }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "P2P Sender: prepare failed")
                _state.value = ShareState.Error("Не удалось скачать файлы")
                return@launch
            }
            Timber.d("P2P Sender: bundle ready (${prepared.files.size} файлов), starting discovery")
            _state.value = ShareState.Searching(emptyList())
            eventsJob = scope.launch { nearby.events.collect { handle(it) } }
            nearby.startDiscovery()
        }
    }
```

3. В `sendBundle(endpointId)` первой строкой получить бандл из кеша:

```kotlin
    private fun sendBundle(endpointId: String) {
        val b = bundle ?: run {
            _state.value = ShareState.Error("Файлы не готовы")
            return
        }
        _state.value = ShareState.Sending(0, 0)
        pendingPayloads.clear()
        allEnqueued = false
        scope.launch {
            try {
                val payloadIds = HashMap<File, Long>()
                for (file in b.files) {
                    val id = nearby.sendFile(endpointId, file)
                    payloadIds[file] = id
                    pendingPayloads.add(id)
                }
                val manifest = P2pManifestFactory.create(
                    type = b.type,
                    storeRoot = b.storeRoot,
                    files = b.files,
                    metadataFile = b.metadataFile,
                    payloadIds = payloadIds,
                )
                pendingPayloads.add(nearby.sendBytes(endpointId, P2pManifestCodec.toBytes(manifest)))
                allEnqueued = true
                Timber.d("P2P Sender: всё в очереди, ждём подтверждений доставки (${pendingPayloads.size})")
                // Подтверждения могли прийти раньше, чем мы дошли сюда.
                maybeDone()
            } catch (e: Exception) {
                Timber.e(e, "P2P Sender: Transfer failed")
                _state.value = ShareState.Error(e.message ?: "Ошибка отправки")
            }
        }
    }
```

4. `stop()` отменяет подготовку:

```kotlin
    fun stop() {
        prepareJob?.cancel()
        nearby.stopAll()
    }
```

5. В `ScreenP2pSend.kt` в `when (val s = state)` добавить ветку перед `is ShareState.Connecting`
   (минимальная — полноценный прогресс придёт в Task 7):

```kotlin
                    is ShareState.Preparing -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Подготовка файлов…", style = MaterialTheme.typography.headlineSmall)
                        }
                    }
```

- [ ] **Step 4: Run all share controller tests**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.client.xvideos.common.p2p.P2pShareControllerTest"`
Expected: PASS — 4 старых теста (через вторичный конструктор) + 3 новых.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/client/xvideos/common/p2p/P2pShareController.kt app/src/main/java/com/client/xvideos/common/p2p/ui/ScreenP2pSend.kt app/src/test/java/com/client/xvideos/common/p2p/P2pShareControllerTest.kt
git commit -m "feat(p2p): Preparing phase with lazy bundle provider in share controller"
```

---

### Task 7: ScreenP2pSend + ExpandMenuVM — outbox-скачивание и очистка

UI-проводка: юнит-тестов на Compose-экраны в проекте нет, проверка — компиляция, полный прогон тестов и ручной смоук (Task 8).

**Files:**
- Modify: `app/src/main/java/com/client/xvideos/common/p2p/ui/ScreenP2pSend.kt`
- Modify: `app/src/main/java/com/client/xvideos/l/ui/element/expandMenu/ExpandMenuVM.kt:172-217`

- [ ] **Step 1: ScreenP2pSend принимает P2pSendSource**

В `ScreenP2pSend.kt`:

1. Добавить импорты:

```kotlin
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.p2p.P2pSendSource
import com.client.xvideos.common.p2p.export.LExporter
import com.client.xvideos.common.p2p.mirrorRoot
import com.client.xvideos.l.featured.saved.LDownloadProgress
import com.client.xvideos.l.featured.saved.lPersistPicsDetailsToFolder
import com.client.xvideos.l.net.Luscious
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.withContext
import java.io.File
```

2. После `findActivity()` добавить EntryPoint и подготовку бандла:

```kotlin
/** Доступ к Hilt-синглтонам из Voyager-экрана — объекта вне DI-графа. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface P2pSendEntryPoint {
    fun luscious(): Luscious
}

/**
 * Ready — бандл уже в store. DownloadL — качаем item в outbox-зеркало
 * `outbox/L/Likes` (структура повторяет /xvideos, поэтому relativePath
 * манифеста совпадает с боевым) и экспортируем оттуда.
 */
private suspend fun prepareBundle(
    context: Context,
    source: P2pSendSource,
    progress: LDownloadProgress,
): P2pExportBundle = when (source) {
    is P2pSendSource.Ready -> source.bundle
    is P2pSendSource.DownloadL -> withContext(Dispatchers.IO) {
        val item = source.item() ?: error("Битые данные item")
        val luscious = EntryPointAccessors
            .fromApplication(context.applicationContext, P2pSendEntryPoint::class.java)
            .luscious()
        val outboxLikes = mirrorRoot(
            base = File(AppPath.p2p_outbox),
            mainRoot = File(AppPath.main),
            storeRoot = File(AppPath.l_likes),
        )
        val folder = lPersistPicsDetailsToFolder(item, outboxLikes, luscious, progress).getOrThrow()
        LExporter.export(folder) ?: error("Не удалось подготовить файлы")
    }
}
```

3. Заменить объявление экрана и создание контроллера:

```kotlin
/**
 * Экран «Отправка P2P»: подготовка файлов (outbox при необходимости),
 * поиск устройств и передача.
 */
data class ScreenP2pSend(val source: P2pSendSource) : Screen {
```

```kotlin
        val downloadProgress = remember { LDownloadProgress(scope) }
        val controller = remember {
            P2pShareController(
                nearby = NearbyClientImpl(context),
                scope = scope,
                myName = Build.MODEL ?: "Android",
                bundleProvider = { prepareBundle(context.applicationContext, source, downloadProgress) },
            )
        }
```

4. Ветку `Preparing` из Task 6 заменить на версию с прогрессом:

```kotlin
                    is ShareState.Preparing -> {
                        val pct by downloadProgress.percentDownload.collectAsState()
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (pct in 0f..1f) {
                                LinearProgressIndicator(
                                    progress = { pct },
                                    modifier = Modifier.fillMaxWidth().height(8.dp)
                                )
                                Text("Подготовка файлов: ${(pct * 100).toInt()}%", modifier = Modifier.padding(top = 8.dp))
                            } else {
                                CircularProgressIndicator()
                                Text("Подготовка файлов…", modifier = Modifier.padding(top = 16.dp))
                            }
                        }
                    }
```

5. В ветке `Done` дополнить `LaunchedEffect` очисткой outbox (передача подтверждена — staging больше не нужен):

```kotlin
                    is ShareState.Done -> {
                        // Показываем «Готово» секунду и закрываем экран сами.
                        // Уход с экрана отменяет эффект — двойного pop не будет.
                        LaunchedEffect(Unit) {
                            withContext(Dispatchers.IO) { AppPath.clearP2pOutbox() }
                            kotlinx.coroutines.delay(1_000)
                            navigator.pop()
                        }
```

(остальное содержимое ветки без изменений).

- [ ] **Step 2: ExpandMenuVM — без ошибки «Сначала сохрани»**

В `ExpandMenuVM.kt` заменить блок `// ---- P2P share ----` (строки ~172-217):

```kotlin
    // ---- P2P share ----

    var p2pChooserItem by mutableStateOf<PicsDetails?>(null)
        private set
    var p2pSource by mutableStateOf<P2pSendSource?>(null)
        private set

    fun onShareClicked(item: PicsDetails) { p2pChooserItem = item }
    fun dismissChooser() { p2pChooserItem = null }
    fun dismissP2p() { p2pSource = null }

    fun startP2p(item: PicsDetails) {
        val url = item.url_to_original
        val folder = url?.let { lFindLikeFolder(File(AppPath.l_likes), it) }
        val bundle = folder?.let { LExporter.export(it) }
        // Нет в Likes (или бандл битый) — экран отправки скачает item в outbox,
        // не помечая его сохранённым.
        p2pSource = if (bundle != null) P2pSendSource.Ready(bundle) else P2pSendSource.DownloadL.of(item)
    }

    /**
     * Хост диалога P2P-шаринга. Должен компоноваться РОВНО ОДИН РАЗ на контейнер
     * (список/экран), не внутри per-item элементов — state общий на ViewModel,
     * каждый экземпляр хоста показал бы свой диалог.
     */
    @Composable
    fun P2pShareHost() {
        val navigator = cafe.adriel.voyager.navigator.LocalNavigator.current
        p2pChooserItem?.let { item ->
            P2pSendChooserDialog(
                onSystem = { share(item) },
                onP2p = { startP2p(item) },
                onDismiss = { dismissChooser() },
            )
        }
        p2pSource?.let { source ->
            // Навигация — side effect, нельзя звать прямо из композиции:
            // рекомпозиции дублировали бы push.
            androidx.compose.runtime.LaunchedEffect(source) {
                navigator?.push(ScreenP2pSend(source))
                dismissP2p()
            }
        }
    }
```

Импорты: добавить `import com.client.xvideos.common.p2p.P2pSendSource`; удалить ставший неиспользуемым `import com.client.xvideos.common.p2p.P2pExportBundle` и `import com.client.xvideos.common.snackbar.SnackBar`, если на него не осталось ссылок в файле (`share()`/`saveToGallery()` его используют — тогда оставить).

- [ ] **Step 3: Find other ScreenP2pSend call sites**

Run: `Grep pattern="ScreenP2pSend\(" path="app/src/main/java" output_mode="content"`
Каждый найденный вызов `ScreenP2pSend(bundle)` (X-экран `ScreenSavedX.kt:164` и R-таб передают готовый бандл) обернуть:

```kotlin
ScreenP2pSend(P2pSendSource.Ready(bundle))
```

с импортом `com.client.xvideos.common.p2p.P2pSendSource`.

- [ ] **Step 4: Compile and run the full unit test suite**

Run: `.\gradlew.bat :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL, все тесты PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/client/xvideos/common/p2p/ui/ScreenP2pSend.kt app/src/main/java/com/client/xvideos/l/ui/element/expandMenu/ExpandMenuVM.kt app/src/main/java/com/client/xvideos/x/screens/saved/ScreenSavedX.kt app/src/main/java/com/client/xvideos/r/ui/explorer/tab/saved/tab/R_Screen_Saved_DownloadTab.kt
git commit -m "feat(p2p): send undownloaded L items via outbox with Preparing progress"
```

---

### Task 8: Финальная верификация

- [ ] **Step 1: Full unit test run**

Run: `.\gradlew.bat :app:testDebugUnitTest`
Expected: все тесты PASS.

- [ ] **Step 2: Assemble debug build**

Run: `.\gradlew.bat :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Ручной смоук на двух устройствах (чек-лист пользователю)**

1. L-item НЕ из Likes → P2P share → экран показывает «Подготовка файлов» с прогрессом → поиск → передача → «Готово». Item НЕ появился в Likes отправителя.
2. На приёмнике item появился в Likes (список обновился без перезахода).
3. После передачи `/xvideos/outbox` и `/xvideos/inbox` пусты.
4. L-item ИЗ Likes → отправка работает как раньше (без фазы скачивания).
5. X-item с экрана Saved → отправка/приём работают (приём через inbox).
6. R-item → лайк появился на приёмнике (метаданные, без файлов).
7. Оборвать передачу (выключить Bluetooth посередине) → на приёмнике в `/xvideos/X` и `/xvideos/L` мусора нет; перезапуск приложения чистит inbox/outbox.

- [ ] **Step 4: Commit any leftovers and report**

```bash
git status
```
Expected: clean (всё закоммичено по задачам).
