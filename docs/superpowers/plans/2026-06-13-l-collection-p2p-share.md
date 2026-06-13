# L Collection P2P Share Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Пункт «Поделиться (P2P)» в диалоге коллекции L — архивирует коллекцию в zip, передаёт по P2P, на приёме распаковывает в `/xvideos/L/Collection` с пофайловой перезаписью.

**Architecture:** Новый `P2pType.L_COLLECTION`. Отправка: `LCollectionExporter` зипует папку коллекции (записи `<имя>/item/…`) в outbox одним файлом; архивация идёт в существующей фазе `Preparing`/`bundleProvider`. Приём: `LCollectionBundleImporter` распаковывает zip в зеркало `inbox/L/Collection`, затем существующий `P2pInboxMerger.merge` переносит в `/xvideos` с перезаписью, refresh списка коллекций.

**Tech Stack:** Kotlin, `java.util.zip`, Compose + Voyager, Nearby Connections, JUnit4 + kotlinx-coroutines-test.

**Spec:** `docs/superpowers/specs/2026-06-13-l-collection-p2p-share-design.md`

**Структура файлов:**

| Файл | Роль |
|---|---|
| Create `app/src/main/java/com/client/xvideos/common/zip/ZipUtils.kt` | zipDirectory / unzip (zip-slip guard) |
| Create `app/src/test/java/com/client/xvideos/common/zip/ZipUtilsTest.kt` | round-trip + zip-slip |
| Modify `app/src/main/java/com/client/xvideos/common/p2p/P2pType.kt` | + `L_COLLECTION` |
| Modify `app/src/main/java/com/client/xvideos/common/p2p/export/Exporters.kt` | + `LCollectionExporter` |
| Create `app/src/main/java/com/client/xvideos/common/p2p/imports/LCollectionBundleImporter.kt` | unzip → merge → refresh |
| Modify `app/src/main/java/com/client/xvideos/common/p2p/P2pReceiveManager.kt` | storeRootFor ветка + routing L_COLLECTION |
| Modify `app/src/main/java/com/client/xvideos/common/p2p/P2pSendSource.kt` | + `ShareCollection` |
| Modify `app/src/main/java/com/client/xvideos/common/p2p/ui/ScreenP2pSend.kt` | prepareBundle ветка ShareCollection |
| Modify `app/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/saved/collection/L_Screen_CollectionTab.kt` | пункт диалога + navigator |
| Tests | `P2pManifestCodecTest`, `ExportersTest`, новый `LCollectionBundleImporterTest` |

Все команды — из корня репо, Windows: `.\gradlew.bat`.

---

### Task 1: ZipUtils — zipDirectory + unzip (TDD)

**Files:**
- Create: `app/src/main/java/com/client/xvideos/common/zip/ZipUtils.kt`
- Create: `app/src/test/java/com/client/xvideos/common/zip/ZipUtilsTest.kt`

- [ ] **Step 1: Write the failing tests**

Создать `app/src/test/java/com/client/xvideos/common/zip/ZipUtilsTest.kt`:

```kotlin
package com.client.xvideos.common.zip

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ZipUtilsTest {

    @get:Rule val tmp = TemporaryFolder()

    @Test
    fun `zip then unzip reproduces tree with folder name as top entry`() {
        val src = tmp.newFolder("MyCol")
        File(src, "item1").mkdirs()
        File(src, "item1/media.jpg").writeText("MEDIA")
        File(src, "item1/metadata.json").writeText("{}")
        File(src, "collection.json").writeText("{\"v\":1}")

        val zipFile = File(tmp.newFolder("out"), "MyCol.zip")
        ZipUtils.zipDirectory(src, zipFile)
        assertTrue(zipFile.exists())

        val dest = tmp.newFolder("dest")
        ZipUtils.unzip(zipFile, dest)

        assertEquals("MEDIA", File(dest, "MyCol/item1/media.jpg").readText())
        assertEquals("{}", File(dest, "MyCol/item1/metadata.json").readText())
        assertEquals("{\"v\":1}", File(dest, "MyCol/collection.json").readText())
    }

    @Test
    fun `unzip rejects zip-slip entries`() {
        val evilZip = File(tmp.newFolder("evil"), "evil.zip")
        ZipOutputStream(evilZip.outputStream().buffered()).use { zip ->
            zip.putNextEntry(ZipEntry("../escape.txt"))
            zip.write("x".toByteArray())
            zip.closeEntry()
        }
        val dest = tmp.newFolder("dest2")
        try {
            ZipUtils.unzip(evilZip, dest)
            fail("Expected zip-slip to be rejected")
        } catch (e: IllegalArgumentException) {
            // ожидаемо
        }
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.client.xvideos.common.zip.ZipUtilsTest"`
Expected: FAIL — compilation error, `ZipUtils` не существует.

- [ ] **Step 3: Implement ZipUtils**

Создать `app/src/main/java/com/client/xvideos/common/zip/ZipUtils.kt`:

```kotlin
package com.client.xvideos.common.zip

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Маленькая zip-утилита для P2P-передачи папок (коллекции L).
 * [zipDirectory] кладёт содержимое [sourceDir] в архив с префиксом
 * `sourceDir.name` (имя папки едет внутри архива). [unzip] распаковывает с
 * защитой от zip-slip. Чистые функции на File — тестируются на JVM.
 */
object ZipUtils {

    fun zipDirectory(sourceDir: File, zipFile: File) {
        zipFile.parentFile?.mkdirs()
        val prefix = sourceDir.name
        ZipOutputStream(BufferedOutputStream(zipFile.outputStream())).use { zip ->
            zip.putNextEntry(ZipEntry("$prefix/"))
            zip.closeEntry()
            sourceDir.walkTopDown().forEach { file ->
                if (file == sourceDir) return@forEach
                val rel = file.relativeTo(sourceDir).invariantSeparatorsPath.trim('/')
                if (rel.isBlank()) return@forEach
                val entryName = "$prefix/$rel"
                if (file.isDirectory) {
                    zip.putNextEntry(ZipEntry("$entryName/"))
                    zip.closeEntry()
                } else {
                    zip.putNextEntry(ZipEntry(entryName))
                    BufferedInputStream(file.inputStream()).use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
        }
    }

    fun unzip(zipFile: File, destDir: File) {
        val root = destDir.canonicalFile
        root.mkdirs()
        ZipInputStream(BufferedInputStream(zipFile.inputStream())).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val name = normalizeEntryName(entry.name)
                val target = File(root, name).canonicalFile
                ensureInside(root, target)
                if (entry.isDirectory || entry.name.endsWith("/")) {
                    target.mkdirs()
                } else {
                    target.parentFile?.mkdirs()
                    BufferedOutputStream(target.outputStream()).use { out -> zip.copyTo(out) }
                }
                zip.closeEntry()
            }
        }
    }

    private fun normalizeEntryName(raw: String): String {
        val name = raw.replace('\\', '/').trim('/')
        require(name.isNotBlank()) { "Empty zip entry name" }
        require(!name.startsWith("/") && !name.contains(':')) { "Unsafe zip entry: $raw" }
        val parts = name.split('/').filter { it.isNotBlank() }
        require(parts.none { it == "." || it == ".." }) { "Unsafe zip entry: $raw" }
        return parts.joinToString("/")
    }

    private fun ensureInside(root: File, target: File) {
        val rootPath = root.absolutePath
        val targetPath = target.absolutePath
        require(targetPath == rootPath || targetPath.startsWith(rootPath + File.separator)) {
            "Zip entry escapes target dir: $targetPath"
        }
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.client.xvideos.common.zip.ZipUtilsTest"`
Expected: PASS (2 теста).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/client/xvideos/common/zip/ZipUtils.kt app/src/test/java/com/client/xvideos/common/zip/ZipUtilsTest.kt
git commit -m "feat(zip): ZipUtils - zipDirectory and unzip with zip-slip guard"
```

---

### Task 2: P2pType.L_COLLECTION + codec + exhaustive when (TDD)

Добавление значения enum ломает exhaustive `when` в `P2pReceiveManager.storeRootFor` — правка ветки входит в эту задачу (роутинг приёма — Task 5).

**Files:**
- Modify: `app/src/main/java/com/client/xvideos/common/p2p/P2pType.kt`
- Modify: `app/src/main/java/com/client/xvideos/common/p2p/P2pReceiveManager.kt`
- Test: `app/src/test/java/com/client/xvideos/common/p2p/P2pManifestCodecTest.kt`

- [ ] **Step 1: Write the failing test**

В `P2pManifestCodecTest.kt` добавить в конец класса:

```kotlin
    @Test
    fun `collection manifest round trip`() {
        val m = P2pManifest(
            type = P2pType.L_COLLECTION,
            metadataFileName = null,
            files = listOf(P2pManifestFile("MyCol.zip", "MyCol.zip", 1L, 100L)),
        )
        assertEquals(m, P2pManifestCodec.fromBytes(P2pManifestCodec.toBytes(m)))
    }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.client.xvideos.common.p2p.P2pManifestCodecTest"`
Expected: FAIL — compilation error, `L_COLLECTION` не существует.

- [ ] **Step 3: Add enum value and exhaustive branch**

`P2pType.kt` целиком:

```kotlin
package com.client.xvideos.common.p2p

/**
 * Источник item: xvideos, redgifs, luscious. Определяет store на приёмной стороне.
 * [L_ALBUM] — метаданные альбома L (`<id>.album`), контент получатель качает сам.
 * [L_COLLECTION] — коллекция L одним zip-архивом (реальные файлы).
 */
enum class P2pType { X, R, L, L_ALBUM, L_COLLECTION }
```

В `P2pReceiveManager.kt` в `storeRootFor`-`when` добавить ветку (после `L_ALBUM`),
чтобы `when` оставался exhaustive (L_COLLECTION фактически роутится мимо
storeImporter в Task 5, но компилятор требует ветку):

```kotlin
                    P2pType.L_ALBUM -> File(AppPath.l_albums)
                    P2pType.L_COLLECTION -> File(AppPath.l_collection)
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.client.xvideos.common.p2p.P2pManifestCodecTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/client/xvideos/common/p2p/P2pType.kt app/src/main/java/com/client/xvideos/common/p2p/P2pReceiveManager.kt app/src/test/java/com/client/xvideos/common/p2p/P2pManifestCodecTest.kt
git commit -m "feat(p2p): L_COLLECTION bundle type"
```

---

### Task 3: LCollectionExporter (TDD)

**Files:**
- Modify: `app/src/main/java/com/client/xvideos/common/p2p/export/Exporters.kt`
- Test: `app/src/test/java/com/client/xvideos/common/p2p/export/ExportersTest.kt`

- [ ] **Step 1: Write the failing tests**

В `ExportersTest.kt` добавить импорт:

```kotlin
import com.client.xvideos.common.zip.ZipUtils
```

и тесты в конец класса:

```kotlin
    @Test
    fun `L collection exporter zips collection into outbox`() {
        val main = tmp.newFolder("xvideos")
        val collectionRoot = File(main, "L/Collection").apply { mkdirs() }
        val outboxDir = File(main, "outbox").apply { mkdirs() }
        val col = File(collectionRoot, "MyCol/item1").apply { mkdirs() }
        File(col, "media.jpg").writeText("M")
        File(col, "metadata.json").writeText("{}")

        val bundle = LCollectionExporter.export("MyCol", collectionRoot, outboxDir)!!

        assertEquals(P2pType.L_COLLECTION, bundle.type)
        assertEquals(outboxDir, bundle.storeRoot)
        val zip = bundle.files.single()
        assertEquals("MyCol.zip", zip.name)
        // содержимое архива воспроизводит коллекцию с именем-префиксом
        val check = File(main, "check").apply { mkdirs() }
        ZipUtils.unzip(zip, check)
        assertEquals("M", File(check, "MyCol/item1/media.jpg").readText())
    }

    @Test
    fun `L collection exporter returns null for missing or empty collection`() {
        val main = tmp.newFolder("xvideos")
        val collectionRoot = File(main, "L/Collection").apply { mkdirs() }
        val outboxDir = File(main, "outbox").apply { mkdirs() }

        // нет папки
        assertNull(LCollectionExporter.export("Nope", collectionRoot, outboxDir))
        // папка есть, но пустая
        File(collectionRoot, "Empty").mkdirs()
        assertNull(LCollectionExporter.export("Empty", collectionRoot, outboxDir))
    }
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.client.xvideos.common.p2p.export.ExportersTest"`
Expected: FAIL — compilation error, `LCollectionExporter` не существует.

- [ ] **Step 3: Implement LCollectionExporter**

В `Exporters.kt` добавить импорт:

```kotlin
import com.client.xvideos.common.zip.ZipUtils
```

и объект в конец файла:

```kotlin
/**
 * Коллекция L — вся папка коллекции одним zip-архивом (реальные файлы, не
 * только метаданные). Архив пишется в [outboxDir] как `<имя>.zip`; записи
 * внутри начинаются с имени коллекции, поэтому на приёме распаковка в зеркало
 * `inbox/L/Collection` сразу даёт `inbox/L/Collection/<имя>/...`.
 * Возвращает null, если коллекции нет или она пуста (нет ни одного файла).
 */
object LCollectionExporter {

    fun export(collectionName: String, collectionRoot: File, outboxDir: File): P2pExportBundle? {
        val source = File(collectionRoot, collectionName)
        if (!source.isDirectory) return null
        if (source.walkTopDown().none { it.isFile }) return null

        return runCatching {
            outboxDir.mkdirs()
            val zipFile = File(outboxDir, "$collectionName.zip")
            ZipUtils.zipDirectory(source, zipFile)
            P2pExportBundle(P2pType.L_COLLECTION, outboxDir, listOf(zipFile), null)
        }.getOrNull()
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.client.xvideos.common.p2p.export.ExportersTest"`
Expected: PASS (9 тестов: 7 прежних + 2 новых).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/client/xvideos/common/p2p/export/Exporters.kt app/src/test/java/com/client/xvideos/common/p2p/export/ExportersTest.kt
git commit -m "feat(p2p): LCollectionExporter - zip collection into outbox bundle"
```

---

### Task 4: LCollectionBundleImporter (TDD)

**Files:**
- Create: `app/src/main/java/com/client/xvideos/common/p2p/imports/LCollectionBundleImporter.kt`
- Create: `app/src/test/java/com/client/xvideos/common/p2p/imports/LCollectionBundleImporterTest.kt`

- [ ] **Step 1: Write the failing test**

Создать `app/src/test/java/com/client/xvideos/common/p2p/imports/LCollectionBundleImporterTest.kt`:

```kotlin
package com.client.xvideos.common.p2p.imports

import com.client.xvideos.common.p2p.P2pManifest
import com.client.xvideos.common.p2p.P2pManifestFile
import com.client.xvideos.common.p2p.P2pType
import com.client.xvideos.common.zip.ZipUtils
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class LCollectionBundleImporterTest {

    @get:Rule val tmp = TemporaryFolder()

    @Test
    fun `unzips received archive into collection store and merges`() = runTest {
        val main = tmp.newFolder("xvideos")
        val inbox = File(main, "inbox").apply { mkdirs() }
        val collectionStore = File(main, "L/Collection").apply { mkdirs() }

        // Готовим zip коллекции "MyCol" (как его собрал бы отправитель).
        val srcRoot = tmp.newFolder("src")
        val srcCol = File(srcRoot, "MyCol/item1").apply { mkdirs() }
        File(srcCol, "media.jpg").writeText("M")
        File(srcCol, "metadata.json").writeText("{}")
        val zip = File(tmp.newFolder("zipdir"), "MyCol.zip")
        ZipUtils.zipDirectory(File(srcRoot, "MyCol"), zip)

        var refreshed = false
        val importer = LCollectionBundleImporter(
            inboxRoot = inbox,
            mainRoot = main,
            collectionStoreRoot = collectionStore,
            refresh = { refreshed = true },
        )

        importer.import(
            P2pManifest(
                type = P2pType.L_COLLECTION,
                metadataFileName = null,
                files = listOf(P2pManifestFile("MyCol.zip", "MyCol.zip", 1L, zip.length())),
            ),
            mapOf(1L to zip),
        )

        assertEquals("M", File(collectionStore, "MyCol/item1/media.jpg").readText())
        assertEquals("{}", File(collectionStore, "MyCol/item1/metadata.json").readText())
        assertTrue(inbox.listFiles().isNullOrEmpty())
        assertTrue(refreshed)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.client.xvideos.common.p2p.imports.LCollectionBundleImporterTest"`
Expected: FAIL — compilation error, `LCollectionBundleImporter` не существует.

- [ ] **Step 3: Implement LCollectionBundleImporter**

Создать `app/src/main/java/com/client/xvideos/common/p2p/imports/LCollectionBundleImporter.kt`:

```kotlin
package com.client.xvideos.common.p2p.imports

import com.client.xvideos.common.p2p.P2pInboxMerger
import com.client.xvideos.common.p2p.P2pManifest
import com.client.xvideos.common.p2p.mirrorRoot
import com.client.xvideos.common.zip.ZipUtils
import java.io.File

/**
 * Приём коллекции L: распаковывает принятый zip в зеркало `inbox/L/Collection`,
 * затем переносит содержимое в боевой store существующим merge (пофайловая
 * перезапись = слияние при совпадении имени коллекции) и дёргает refresh.
 *
 * @param inboxRoot staging-папка приёма (`AppPath.p2p_inbox`).
 * @param mainRoot корень `/xvideos` (`AppPath.main`).
 * @param collectionStoreRoot корень коллекций (`AppPath.l_collection`).
 * @param refresh перечитать список коллекций (`savedL.collection.refreshCollectionList()`).
 */
class LCollectionBundleImporter(
    private val inboxRoot: File,
    private val mainRoot: File,
    private val collectionStoreRoot: File,
    private val refresh: () -> Unit,
) : BundleImporter {

    override suspend fun import(manifest: P2pManifest, receivedFiles: Map<Long, File>) {
        val zip = receivedFiles.values.firstOrNull()
            ?: error("L_COLLECTION bundle has no file")
        val mirror = mirrorRoot(inboxRoot, mainRoot, collectionStoreRoot)
        ZipUtils.unzip(zip, mirror)
        P2pInboxMerger.merge(inboxRoot, mainRoot)
        refresh()
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.client.xvideos.common.p2p.imports.LCollectionBundleImporterTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/client/xvideos/common/p2p/imports/LCollectionBundleImporter.kt app/src/test/java/com/client/xvideos/common/p2p/imports/LCollectionBundleImporterTest.kt
git commit -m "feat(p2p): LCollectionBundleImporter - unzip into collection store via merge"
```

---

### Task 5: Проводка — source, prepareBundle, receive routing

UI/менеджер юнит-тестами не покрыты — проверка компиляцией, полным прогоном и смоуком (Task 7).

**Files:**
- Modify: `app/src/main/java/com/client/xvideos/common/p2p/P2pSendSource.kt`
- Modify: `app/src/main/java/com/client/xvideos/common/p2p/ui/ScreenP2pSend.kt`
- Modify: `app/src/main/java/com/client/xvideos/common/p2p/P2pReceiveManager.kt`

- [ ] **Step 1: Add ShareCollection to P2pSendSource**

В `P2pSendSource.kt` после блока `DownloadL` (перед закрывающей `}` интерфейса) добавить:

```kotlin
    /** Коллекция L: зипуется в outbox на экране отправки и шлётся одним архивом. */
    data class ShareCollection(val collectionName: String) : P2pSendSource
```

- [ ] **Step 2: Add ShareCollection branch to prepareBundle**

В `ScreenP2pSend.kt` добавить импорт:

```kotlin
import com.client.xvideos.common.p2p.export.LCollectionExporter
```

В функции `prepareBundle`, в `when (source)`, добавить ветку после `DownloadL`:

```kotlin
    is P2pSendSource.ShareCollection -> withContext(Dispatchers.IO) {
        LCollectionExporter.export(
            collectionName = source.collectionName,
            collectionRoot = File(AppPath.l_collection),
            outboxDir = File(AppPath.p2p_outbox),
        ) ?: error("Не удалось подготовить коллекцию")
    }
```

- [ ] **Step 3: Route L_COLLECTION on receive in P2pReceiveManager**

В `P2pReceiveManager.kt`, после создания `rLikesImporter` (строка ~73), добавить
импортер коллекции:

```kotlin
        val lCollectionImporter = LCollectionBundleImporter(
            inboxRoot = File(AppPath.p2p_inbox),
            mainRoot = File(AppPath.main),
            collectionStoreRoot = File(AppPath.l_collection),
            refresh = { entryPoint.savedL().collection.refreshCollectionList() },
        )
```

Заменить лямбду `importer`:

```kotlin
        val importer = BundleImporter { manifest, files ->
            when (manifest.type) {
                P2pType.R -> rLikesImporter.import(manifest, files)
                P2pType.L_COLLECTION -> lCollectionImporter.import(manifest, files)
                else -> storeImporter.import(manifest, files)
            }
        }
```

Добавить импорт в шапку файла:

```kotlin
import com.client.xvideos.common.p2p.imports.LCollectionBundleImporter
```

- [ ] **Step 4: Compile and run the full unit test suite**

Run: `.\gradlew.bat :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL, все тесты PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/client/xvideos/common/p2p/P2pSendSource.kt app/src/main/java/com/client/xvideos/common/p2p/ui/ScreenP2pSend.kt app/src/main/java/com/client/xvideos/common/p2p/P2pReceiveManager.kt
git commit -m "feat(p2p): wire ShareCollection source and L_COLLECTION receive routing"
```

---

### Task 6: UI — пункт «Поделиться (P2P)» в диалоге коллекции

**Files:**
- Modify: `app/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/saved/collection/L_Screen_CollectionTab.kt`

- [ ] **Step 1: Add navigator and dialog item**

В `L_Screen_CollectionTab.kt`:

1. Добавить импорты:

```kotlin
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.p2p.P2pSendSource
import com.client.xvideos.common.p2p.ui.ScreenP2pSend
```

2. В начале `Content()` после `val savedL = vm.savedL` (строка ~89) добавить:

```kotlin
        val navigator = LocalNavigator.currentOrThrow
```

3. В диалоге `itemPendingAction`, после `DropdownMenuItem` «Удалить коллекцию»
   (внутри той же `Column`, строка ~148), добавить пункт:

```kotlin
                        DropdownMenuItem(
                            text = { Text("Поделиться (P2P)", style = Theme.L.Type.menuItem) },
                            onClick = {
                                itemPendingAction = null
                                navigator.push(ScreenP2pSend(P2pSendSource.ShareCollection(pending)))
                            }
                        )
```

- [ ] **Step 2: Compile**

Run: `.\gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/saved/collection/L_Screen_CollectionTab.kt
git commit -m "feat(l): Share collection (P2P) item in collection dialog"
```

---

### Task 7: Финальная верификация

- [ ] **Step 1: Full unit test run + assemble**

Run: `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug`
Expected: BUILD SUCCESSFUL, все тесты PASS.

- [ ] **Step 2: Ручной смоук на двух устройствах (чек-лист пользователю)**

1. Долгое нажатие на коллекции → диалог → «Поделиться (P2P)» → экран отправки,
   фаза «Подготовка файлов…» (архивация) → поиск → передача → «Готово».
2. На приёмнике коллекция появилась в списке (refresh без перезахода); открытие
   показывает item'ы, медиа на месте.
3. У получателя уже была коллекция с тем же именем → элементы слиты
   (новые добавлены, совпадающие перезаписаны), старые уникальные на месте.
4. После передачи `/xvideos/outbox` пуст; `/xvideos/inbox` пуст.
5. Обычные передачи (L-item, L-альбом, X, R) работают как раньше.

- [ ] **Step 3: Verify clean tree**

```bash
git status
```
Expected: clean.
