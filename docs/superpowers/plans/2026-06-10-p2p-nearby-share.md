# P2P Nearby Share — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Передать выбранный item (X/R/L) на другой телефон с этим же приложением напрямую через Google Nearby Connections; получатель раскладывает байты в свой локальный store нужного типа.

**Architecture:** Чистое ядро (модель манифеста + сборка/установка бандла + локаторы файлов store) отделено от Android/GMS-глины (NearbyClient-обёртка, контроллеры, Compose-UI). Ядро тестируется на JVM с temp-папками и фейковым `NearbyClient`; глина проверяется компиляцией. Формат провода = существующий формат файлов store (Подход A): передаём те же файлы + маленький JSON-манифест с относительными путями.

**Tech Stack:** Kotlin, Coroutines, Compose M3, Voyager, Gson, `com.google.android.gms:play-services-nearby`. Тесты: JUnit4 + kotlinx-coroutines-test.

---

## v1 ограничения (подтвердить на ревью)

- **Передаём только уже сохранённый/скачанный item.** Если файлов на диске нет — показываем «Сначала сохрани/скачай», передача не стартует. (Спек допускал докачку на лету — вынесено за v1, чтобы не тянуть сеть/резолв ссылок в exporter.)
- Один item за раз.
- Дедуп у получателя = перезапись файлов (`copyTo overwrite=true`); список в UI дедупит существующий `refresh()`.
- Без foreground-service: передача идёт пока открыт экран, держим экран включённым.

## Карта файлов

Новый пакет `app/src/main/java/com/client/xvideos/common/p2p/`:

| Файл | Ответственность | Тип |
|---|---|---|
| `P2pType.kt` | enum `X/R/L` | ядро |
| `P2pManifest.kt` | модель манифеста + Gson-кодек | ядро (тест) |
| `P2pExportBundle.kt` | DTO `(type, storeRoot, files, metadataFile)` | ядро |
| `P2pManifestFactory.kt` | сборка манифеста: relativePath + payloadId | ядро (тест) |
| `P2pBundleInstaller.kt` | запись принятых файлов в storeRoot по relativePath | ядро (тест) |
| `export/XBundleLocator.kt` / `RBundleLocator.kt` / `LBundleLocator.kt` | найти файлы item в store | ядро (тест) |
| `export/Exporters.kt` | `XExporter/RExporter/LExporter` → `P2pExportBundle?` | ядро (тест) |
| `import/StoreBundleImporter.kt` | установить бандл в нужный store + refresh | ядро (тест) |
| `nearby/NearbyClient.kt` | интерфейс + `P2pEvent` | контракт |
| `nearby/NearbyClientImpl.kt` | обёртка над GMS `ConnectionsClient` | глина |
| `P2pReceiveController.kt` | оркестрация приёма (advertise→accept→assemble→import) | ядро (тест) |
| `P2pShareController.kt` | оркестрация отправки (discover→connect→send) | ядро (тест) |
| `P2pPermissions.kt` | runtime-разрешения | глина |
| `ui/ScreenP2pReceive.kt` | экран приёма (Voyager) | глина |
| `ui/P2pDeviceSearchSheet.kt` | sheet поиска телефонов (отправка) | глина |
| `ui/P2pSendChooserDialog.kt` | выбор «Система / P2P рядом» | глина |

Тесты: `app/src/test/java/com/client/xvideos/common/p2p/...`.

Правки существующих:
- `gradle/libs.versions.toml`, `app/build.gradle` — зависимости + тест-сорсет.
- `app/src/main/AndroidManifest.xml` — проверить разрешения (уже есть).
- `app/src/main/java/com/client/xvideos/MainActivity.kt` — кнопка приёма в `MenuScreen` topBar.
- L/R/X точки «Поделиться» — добавить выбор «Система / P2P».

---

## Task 1: Зависимости и тест-сорсет

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle`

- [ ] **Step 1: Добавить версии и библиотеки в каталог**

В `gradle/libs.versions.toml` в секцию `[versions]` добавить:

```toml
playServicesNearby = "19.3.0"
kotlinxCoroutinesTest = "1.10.2"
```

В секцию `[libraries]` добавить (строка с `junit = ...` уже существует, её не трогаем):

```toml
play-services-nearby = { group = "com.google.android.gms", name = "play-services-nearby", version.ref = "playServicesNearby" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "kotlinxCoroutinesTest" }
```

- [ ] **Step 2: Подключить зависимости в модуле**

В `app/build.gradle` в блок `dependencies { ... }` (после строки `implementation(libs.core)`) добавить:

```groovy
    // P2P Nearby Connections
    implementation libs.play.services.nearby

    // Unit-тесты ядра P2P
    testImplementation libs.junit
    testImplementation libs.kotlinx.coroutines.test
```

- [ ] **Step 3: Проверить, что проект синхронизируется и тест-таска видна**

Run: `.\gradlew.bat :app:dependencies --configuration debugRuntimeClasspath -q | findstr nearby`
Expected: строка `com.google.android.gms:play-services-nearby:19.3.0` присутствует.

- [ ] **Step 4: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle
git commit -m "build: add play-services-nearby + unit test deps for p2p"
```

---

## Task 2: Проверить разрешения в манифесте

**Files:**
- Modify: `app/src/main/AndroidManifest.xml` (только при нехватке)

- [ ] **Step 1: Сверить наличие разрешений**

Открыть `app/src/main/AndroidManifest.xml`. Убедиться, что присутствуют (строки ~22–33 уже содержат их):
`ACCESS_WIFI_STATE`, `CHANGE_WIFI_STATE`, `BLUETOOTH`, `BLUETOOTH_ADMIN`, `ACCESS_COARSE_LOCATION`, `ACCESS_FINE_LOCATION`, `BLUETOOTH_ADVERTISE`, `BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN`, `NEARBY_WIFI_DEVICES`.

Если какого-то нет — добавить рядом с остальными. Если все есть — изменений не требуется. Ничего не коммитим, если файл не менялся.

- [ ] **Step 2: Подтвердить сборку манифеста**

Run: `.\gradlew.bat :app:processDebugMainManifest -q`
Expected: BUILD SUCCESSFUL, без ошибок про неизвестные permission.

---

## Task 3: P2pType

**Files:**
- Create: `app/src/main/java/com/client/xvideos/common/p2p/P2pType.kt`

- [ ] **Step 1: Создать enum**

```kotlin
package com.client.xvideos.common.p2p

/** Источник item: xvideos, redgifs, luscious. Определяет store на приёмной стороне. */
enum class P2pType { X, R, L }
```

- [ ] **Step 2: Проверить компиляцию**

Run: `.\gradlew.bat :app:compileDebugKotlin -q`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/client/xvideos/common/p2p/P2pType.kt
git commit -m "feat(p2p): add P2pType enum"
```

---

## Task 4: P2pManifest + кодек (TDD)

**Files:**
- Create: `app/src/main/java/com/client/xvideos/common/p2p/P2pManifest.kt`
- Test: `app/src/test/java/com/client/xvideos/common/p2p/P2pManifestCodecTest.kt`

- [ ] **Step 1: Написать падающий тест**

```kotlin
package com.client.xvideos.common.p2p

import org.junit.Assert.assertEquals
import org.junit.Test

class P2pManifestCodecTest {

    private val sample = P2pManifest(
        type = P2pType.L,
        metadataFileName = "metadata.json",
        files = listOf(
            P2pManifestFile(name = "media.jpg", relativePath = "album_x/media.jpg", payloadId = 10L, size = 123L),
            P2pManifestFile(name = "metadata.json", relativePath = "album_x/metadata.json", payloadId = 11L, size = 456L),
        ),
    )

    @Test
    fun `json round trip preserves all fields`() {
        val json = P2pManifestCodec.toJson(sample)
        val back = P2pManifestCodec.fromJson(json)
        assertEquals(sample, back)
    }

    @Test
    fun `bytes round trip preserves all fields`() {
        val bytes = P2pManifestCodec.toBytes(sample)
        val back = P2pManifestCodec.fromBytes(bytes)
        assertEquals(sample, back)
    }
}
```

- [ ] **Step 2: Запустить тест — убедиться, что не компилируется/падает**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.client.xvideos.common.p2p.P2pManifestCodecTest" -q`
Expected: FAIL — `Unresolved reference: P2pManifest`.

- [ ] **Step 3: Реализовать модель и кодек**

```kotlin
package com.client.xvideos.common.p2p

import com.google.gson.Gson

/**
 * Описание одного файла бандла.
 *
 * @param name имя файла (для UI/логов).
 * @param relativePath путь относительно корня store (через '/'), задаёт, куда положить файл у получателя.
 * @param payloadId id Nearby-payload'а; одинаков на обоих телефонах, по нему получатель сопоставляет байты.
 * @param size размер в байтах.
 */
data class P2pManifestFile(
    val name: String,
    val relativePath: String,
    val payloadId: Long,
    val size: Long,
)

/**
 * Control-сообщение, которое отправитель шлёт BYTES-payload'ом после всех файлов.
 *
 * @param type источник (определяет store у получателя).
 * @param metadataFileName имя файла-метаданных среди [files] (`metadata.json` / `<id>.info`), или null.
 * @param files список файлов бандла.
 */
data class P2pManifest(
    val type: P2pType,
    val metadataFileName: String?,
    val files: List<P2pManifestFile>,
)

/** Сериализация манифеста для передачи BYTES-payload'ом. */
object P2pManifestCodec {
    private val gson = Gson()

    fun toJson(manifest: P2pManifest): String = gson.toJson(manifest)
    fun fromJson(json: String): P2pManifest = gson.fromJson(json, P2pManifest::class.java)
    fun toBytes(manifest: P2pManifest): ByteArray = toJson(manifest).toByteArray(Charsets.UTF_8)
    fun fromBytes(bytes: ByteArray): P2pManifest = fromJson(String(bytes, Charsets.UTF_8))
}
```

- [ ] **Step 4: Запустить тест — должен пройти**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.client.xvideos.common.p2p.P2pManifestCodecTest" -q`
Expected: PASS (2 теста).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/client/xvideos/common/p2p/P2pManifest.kt app/src/test/java/com/client/xvideos/common/p2p/P2pManifestCodecTest.kt
git commit -m "feat(p2p): manifest model + gson codec"
```

---

## Task 5: P2pExportBundle

**Files:**
- Create: `app/src/main/java/com/client/xvideos/common/p2p/P2pExportBundle.kt`

- [ ] **Step 1: Создать DTO**

```kotlin
package com.client.xvideos.common.p2p

import java.io.File

/**
 * Готовый к отправке бандл: набор файлов store + контекст для построения манифеста.
 *
 * @param type источник.
 * @param storeRoot корень, относительно которого считаются relativePath файлов.
 * @param files файлы для отправки (медиа, превью, метаданные).
 * @param metadataFile файл-метаданные среди [files], или null.
 */
data class P2pExportBundle(
    val type: P2pType,
    val storeRoot: File,
    val files: List<File>,
    val metadataFile: File?,
)
```

- [ ] **Step 2: Проверить компиляцию**

Run: `.\gradlew.bat :app:compileDebugKotlin -q`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/client/xvideos/common/p2p/P2pExportBundle.kt
git commit -m "feat(p2p): export bundle DTO"
```

---

## Task 6: P2pManifestFactory (TDD)

**Files:**
- Create: `app/src/main/java/com/client/xvideos/common/p2p/P2pManifestFactory.kt`
- Test: `app/src/test/java/com/client/xvideos/common/p2p/P2pManifestFactoryTest.kt`

- [ ] **Step 1: Написать падающий тест**

```kotlin
package com.client.xvideos.common.p2p

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class P2pManifestFactoryTest {

    @get:Rule val tmp = TemporaryFolder()

    @Test
    fun `relative paths are computed from store root and payload ids attached`() {
        val root = tmp.newFolder("likes")
        val folder = File(root, "album_x").apply { mkdirs() }
        val media = File(folder, "media.jpg").apply { writeText("aaa") }
        val meta = File(folder, "metadata.json").apply { writeText("{}") }

        val manifest = P2pManifestFactory.create(
            type = P2pType.L,
            storeRoot = root,
            files = listOf(media, meta),
            metadataFile = meta,
            payloadIds = mapOf(media to 100L, meta to 101L),
        )

        assertEquals(P2pType.L, manifest.type)
        assertEquals("metadata.json", manifest.metadataFileName)
        val byName = manifest.files.associateBy { it.name }
        assertEquals("album_x/media.jpg", byName.getValue("media.jpg").relativePath)
        assertEquals(100L, byName.getValue("media.jpg").payloadId)
        assertEquals(3L, byName.getValue("media.jpg").size)
        assertEquals("album_x/metadata.json", byName.getValue("metadata.json").relativePath)
    }

    @Test
    fun `flat store layout yields bare file names as relative paths`() {
        val root = tmp.newFolder("download")
        val mp4 = File(root, "555.mp4").apply { writeText("v") }
        val info = File(root, "555.info").apply { writeText("{}") }

        val manifest = P2pManifestFactory.create(
            type = P2pType.X,
            storeRoot = root,
            files = listOf(mp4, info),
            metadataFile = info,
            payloadIds = mapOf(mp4 to 1L, info to 2L),
        )

        val byName = manifest.files.associateBy { it.name }
        assertEquals("555.mp4", byName.getValue("555.mp4").relativePath)
        assertTrue(manifest.files.all { !it.relativePath.contains('\\') })
    }
}
```

- [ ] **Step 2: Запустить — должен упасть**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.client.xvideos.common.p2p.P2pManifestFactoryTest" -q`
Expected: FAIL — `Unresolved reference: P2pManifestFactory`.

- [ ] **Step 3: Реализовать фабрику**

```kotlin
package com.client.xvideos.common.p2p

import java.io.File

/** Строит [P2pManifest] из файлов бандла: relativePath считается от [P2pExportBundle.storeRoot]. */
object P2pManifestFactory {

    fun create(
        type: P2pType,
        storeRoot: File,
        files: List<File>,
        metadataFile: File?,
        payloadIds: Map<File, Long>,
    ): P2pManifest {
        val rootPath = storeRoot.absoluteFile.normalize().path
        val entries = files.map { file ->
            val abs = file.absoluteFile.normalize().path
            require(abs.startsWith(rootPath)) { "File $abs is not inside store root $rootPath" }
            val rel = abs.removePrefix(rootPath)
                .trimStart(File.separatorChar)
                .replace(File.separatorChar, '/')
            val payloadId = payloadIds[file] ?: error("Missing payloadId for ${file.name}")
            P2pManifestFile(
                name = file.name,
                relativePath = rel,
                payloadId = payloadId,
                size = file.length(),
            )
        }
        return P2pManifest(type = type, metadataFileName = metadataFile?.name, files = entries)
    }
}
```

- [ ] **Step 4: Запустить — должен пройти**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.client.xvideos.common.p2p.P2pManifestFactoryTest" -q`
Expected: PASS (2 теста).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/client/xvideos/common/p2p/P2pManifestFactory.kt app/src/test/java/com/client/xvideos/common/p2p/P2pManifestFactoryTest.kt
git commit -m "feat(p2p): manifest factory with relative paths"
```

---

## Task 7: P2pBundleInstaller (TDD)

**Files:**
- Create: `app/src/main/java/com/client/xvideos/common/p2p/P2pBundleInstaller.kt`
- Test: `app/src/test/java/com/client/xvideos/common/p2p/P2pBundleInstallerTest.kt`

- [ ] **Step 1: Написать падающий тест**

```kotlin
package com.client.xvideos.common.p2p

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class P2pBundleInstallerTest {

    @get:Rule val tmp = TemporaryFolder()

    @Test
    fun `installs received files into store root by relative path`() {
        val received1 = tmp.newFile("payload_100").apply { writeText("MEDIA") }
        val received2 = tmp.newFile("payload_101").apply { writeText("{\"k\":1}") }
        val storeRoot = tmp.newFolder("likes")

        val manifest = P2pManifest(
            type = P2pType.L,
            metadataFileName = "metadata.json",
            files = listOf(
                P2pManifestFile("media.jpg", "album_x/media.jpg", 100L, 5L),
                P2pManifestFile("metadata.json", "album_x/metadata.json", 101L, 7L),
            ),
        )

        val written = P2pBundleInstaller.install(
            storeRoot = storeRoot,
            manifest = manifest,
            receivedFiles = mapOf(100L to received1, 101L to received2),
        )

        val media = File(storeRoot, "album_x/media.jpg")
        val meta = File(storeRoot, "album_x/metadata.json")
        assertTrue(media.exists())
        assertEquals("MEDIA", media.readText())
        assertEquals("{\"k\":1}", meta.readText())
        assertEquals(setOf(media.canonicalPath, meta.canonicalPath), written.map { it.canonicalPath }.toSet())
    }

    @Test(expected = IllegalStateException::class)
    fun `throws when a payload is missing`() {
        val storeRoot = tmp.newFolder("likes")
        val manifest = P2pManifest(
            type = P2pType.X,
            metadataFileName = "5.info",
            files = listOf(P2pManifestFile("5.mp4", "5.mp4", 1L, 1L)),
        )
        P2pBundleInstaller.install(storeRoot, manifest, receivedFiles = emptyMap())
    }
}
```

- [ ] **Step 2: Запустить — должен упасть**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.client.xvideos.common.p2p.P2pBundleInstallerTest" -q`
Expected: FAIL — `Unresolved reference: P2pBundleInstaller`.

- [ ] **Step 3: Реализовать установщик**

```kotlin
package com.client.xvideos.common.p2p

import java.io.File

/** Раскладывает принятые файлы по [P2pManifestFile.relativePath] внутри storeRoot (перезапись). */
object P2pBundleInstaller {

    fun install(
        storeRoot: File,
        manifest: P2pManifest,
        receivedFiles: Map<Long, File>,
    ): List<File> {
        return manifest.files.map { entry ->
            val source = receivedFiles[entry.payloadId]
                ?: error("Missing received file for payloadId ${entry.payloadId} (${entry.name})")
            val target = File(storeRoot, entry.relativePath)
            target.parentFile?.mkdirs()
            source.copyTo(target, overwrite = true)
            target
        }
    }
}
```

- [ ] **Step 4: Запустить — должен пройти**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.client.xvideos.common.p2p.P2pBundleInstallerTest" -q`
Expected: PASS (2 теста).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/client/xvideos/common/p2p/P2pBundleInstaller.kt app/src/test/java/com/client/xvideos/common/p2p/P2pBundleInstallerTest.kt
git commit -m "feat(p2p): bundle installer writes files by relative path"
```

---

## Task 8: Локаторы файлов store (TDD)

**Files:**
- Create: `app/src/main/java/com/client/xvideos/common/p2p/export/BundleLocators.kt`
- Test: `app/src/test/java/com/client/xvideos/common/p2p/export/BundleLocatorsTest.kt`

- [ ] **Step 1: Написать падающий тест**

```kotlin
package com.client.xvideos.common.p2p.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class BundleLocatorsTest {

    @get:Rule val tmp = TemporaryFolder()

    @Test
    fun `X locator returns flat files and info as metadata`() {
        val root = tmp.newFolder("xdl")
        File(root, "42.mp4").writeText("v")
        File(root, "42.jpg").writeText("p")
        File(root, "42.info").writeText("{}")

        val located = XBundleLocator.locate(root, 42L)!!
        assertEquals("42.info", located.metadataFile.name)
        assertEquals(setOf("42.mp4", "42.jpg", "42.info"), located.files.map { it.name }.toSet())
        assertEquals(root, located.storeRoot)
    }

    @Test
    fun `X locator returns null when video missing`() {
        val root = tmp.newFolder("xdl")
        File(root, "42.info").writeText("{}")
        assertNull(XBundleLocator.locate(root, 42L))
    }

    @Test
    fun `R locator nests under userName`() {
        val root = tmp.newFolder("rdl")
        val dir = File(root, "lili").apply { mkdirs() }
        File(dir, "9.mp4").writeText("v")
        File(dir, "9.info").writeText("{}")

        val located = RBundleLocator.locate(root, "lili", "9")!!
        assertEquals("9.info", located.metadataFile.name)
        assertEquals(setOf("9.mp4", "9.info"), located.files.map { it.name }.toSet())
        assertEquals(root, located.storeRoot)
    }

    @Test
    fun `L locator lists folder files and metadata json`() {
        val root = tmp.newFolder("ldl")
        val folder = File(root, "album_z").apply { mkdirs() }
        File(folder, "media.jpg").writeText("m")
        File(folder, "preview.640x480.jpg").writeText("pp")
        File(folder, "metadata.json").writeText("{}")

        val located = LBundleLocator.locate(folder)!!
        assertEquals("metadata.json", located.metadataFile.name)
        assertEquals(root, located.storeRoot)
        assertEquals(3, located.files.size)
    }

    @Test
    fun `L locator returns null without metadata`() {
        val root = tmp.newFolder("ldl")
        val folder = File(root, "album_z").apply { mkdirs() }
        File(folder, "media.jpg").writeText("m")
        assertNull(LBundleLocator.locate(folder))
    }
}
```

- [ ] **Step 2: Запустить — должен упасть**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.client.xvideos.common.p2p.export.BundleLocatorsTest" -q`
Expected: FAIL — `Unresolved reference: XBundleLocator`.

- [ ] **Step 3: Реализовать локаторы**

```kotlin
package com.client.xvideos.common.p2p.export

import java.io.File

/** Результат локации: корень store, файлы бандла, файл-метаданные. */
data class LocatedBundle(
    val storeRoot: File,
    val files: List<File>,
    val metadataFile: File,
)

/** X: `<storeRoot>/<id>.mp4|.jpg|.info` (плоско). Обязательны mp4 и info. */
object XBundleLocator {
    fun locate(storeRoot: File, id: Long): LocatedBundle? {
        val mp4 = File(storeRoot, "$id.mp4")
        val info = File(storeRoot, "$id.info")
        if (!mp4.exists() || !info.exists()) return null
        val jpg = File(storeRoot, "$id.jpg")
        val files = buildList {
            add(mp4); add(info); if (jpg.exists()) add(jpg)
        }
        return LocatedBundle(storeRoot, files, info)
    }
}

/** R: `<storeRoot>/<userName>/<id>.mp4|.jpg|.info`. Обязательны mp4 и info. */
object RBundleLocator {
    fun locate(storeRoot: File, userName: String, id: String): LocatedBundle? {
        val dir = File(storeRoot, userName)
        val mp4 = File(dir, "$id.mp4")
        val info = File(dir, "$id.info")
        if (!mp4.exists() || !info.exists()) return null
        val jpg = File(dir, "$id.jpg")
        val files = buildList {
            add(mp4); add(info); if (jpg.exists()) add(jpg)
        }
        return LocatedBundle(storeRoot, files, info)
    }
}

/** L: папка item с `metadata.json` + media/preview. storeRoot = родитель папки. */
object LBundleLocator {
    const val METADATA = "metadata.json"

    fun locate(itemFolder: File): LocatedBundle? {
        if (!itemFolder.isDirectory) return null
        val metadata = File(itemFolder, METADATA)
        if (!metadata.exists()) return null
        val parent = itemFolder.parentFile ?: return null
        val files = itemFolder.listFiles()?.filter { it.isFile }?.sortedBy { it.name }.orEmpty()
        if (files.isEmpty()) return null
        return LocatedBundle(storeRoot = parent, files = files, metadataFile = metadata)
    }
}
```

- [ ] **Step 4: Запустить — должен пройти**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.client.xvideos.common.p2p.export.BundleLocatorsTest" -q`
Expected: PASS (5 тестов).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/client/xvideos/common/p2p/export/BundleLocators.kt app/src/test/java/com/client/xvideos/common/p2p/export/BundleLocatorsTest.kt
git commit -m "feat(p2p): per-type bundle locators"
```

---

## Task 9: Exporters (TDD)

**Files:**
- Create: `app/src/main/java/com/client/xvideos/common/p2p/export/Exporters.kt`
- Test: `app/src/test/java/com/client/xvideos/common/p2p/export/ExportersTest.kt`

- [ ] **Step 1: Написать падающий тест**

```kotlin
package com.client.xvideos.common.p2p.export

import com.client.xvideos.common.p2p.P2pType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ExportersTest {

    @get:Rule val tmp = TemporaryFolder()

    @Test
    fun `X exporter builds bundle from store root and id`() {
        val root = tmp.newFolder("xdl")
        File(root, "7.mp4").writeText("v")
        File(root, "7.info").writeText("{}")

        val bundle = XExporter.export(root, id = 7L)!!
        assertEquals(P2pType.X, bundle.type)
        assertEquals(root, bundle.storeRoot)
        assertEquals("7.info", bundle.metadataFile!!.name)
    }

    @Test
    fun `X exporter returns null when not downloaded`() {
        val root = tmp.newFolder("xdl")
        assertNull(XExporter.export(root, id = 7L))
    }

    @Test
    fun `L exporter builds bundle from item folder`() {
        val root = tmp.newFolder("ldl")
        val folder = File(root, "album_q").apply { mkdirs() }
        File(folder, "media.jpg").writeText("m")
        File(folder, "metadata.json").writeText("{}")

        val bundle = LExporter.export(folder)!!
        assertEquals(P2pType.L, bundle.type)
        assertEquals(root, bundle.storeRoot)
    }
}
```

- [ ] **Step 2: Запустить — должен упасть**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.client.xvideos.common.p2p.export.ExportersTest" -q`
Expected: FAIL — `Unresolved reference: XExporter`.

- [ ] **Step 3: Реализовать exporters**

```kotlin
package com.client.xvideos.common.p2p.export

import com.client.xvideos.common.p2p.P2pExportBundle
import com.client.xvideos.common.p2p.P2pType
import java.io.File

/**
 * Exporters берут уже сохранённый/скачанный item и собирают [P2pExportBundle].
 * Корни store передаются параметром (тестируемость); вызывающий подставляет `AppPath.*`.
 * Возвращают null, если файлов на диске нет (v1: «Сначала сохрани»).
 */
object XExporter {
    fun export(storeRoot: File, id: Long): P2pExportBundle? {
        val l = XBundleLocator.locate(storeRoot, id) ?: return null
        return P2pExportBundle(P2pType.X, l.storeRoot, l.files, l.metadataFile)
    }
}

object RExporter {
    fun export(storeRoot: File, userName: String, id: String): P2pExportBundle? {
        val l = RBundleLocator.locate(storeRoot, userName, id) ?: return null
        return P2pExportBundle(P2pType.R, l.storeRoot, l.files, l.metadataFile)
    }
}

object LExporter {
    /** @param itemFolder папка сохранённого L-item (вызывающий находит её через существующий `lFindLikeFolder`). */
    fun export(itemFolder: File): P2pExportBundle? {
        val l = LBundleLocator.locate(itemFolder) ?: return null
        return P2pExportBundle(P2pType.L, l.storeRoot, l.files, l.metadataFile)
    }
}
```

- [ ] **Step 4: Запустить — должен пройти**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.client.xvideos.common.p2p.export.ExportersTest" -q`
Expected: PASS (3 теста).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/client/xvideos/common/p2p/export/Exporters.kt app/src/test/java/com/client/xvideos/common/p2p/export/ExportersTest.kt
git commit -m "feat(p2p): per-type exporters over locators"
```

---

## Task 10: StoreBundleImporter (TDD)

**Files:**
- Create: `app/src/main/java/com/client/xvideos/common/p2p/import/StoreBundleImporter.kt`
- Test: `app/src/test/java/com/client/xvideos/common/p2p/imports/StoreBundleImporterTest.kt`

- [ ] **Step 1: Написать падающий тест**

```kotlin
package com.client.xvideos.common.p2p.imports

import com.client.xvideos.common.p2p.P2pManifest
import com.client.xvideos.common.p2p.P2pManifestFile
import com.client.xvideos.common.p2p.P2pType
import com.client.xvideos.common.p2p.imports.StoreBundleImporter
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
    fun `imports into store root for type and triggers refresh`() = runTest {
        val xRoot = tmp.newFolder("xRoot")
        val received = tmp.newFile("p1").apply { writeText("VID") }
        var refreshed: P2pType? = null

        val importer = StoreBundleImporter(
            storeRootFor = { type -> if (type == P2pType.X) xRoot else error("unexpected") },
            refreshFor = { type -> refreshed = type },
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
        assertEquals(P2pType.X, refreshed)
    }
}
```

- [ ] **Step 2: Запустить — должен упасть**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.client.xvideos.common.p2p.imports.StoreBundleImporterTest" -q`
Expected: FAIL — `Unresolved reference: StoreBundleImporter`.

- [ ] **Step 3: Реализовать importer**

```kotlin
package com.client.xvideos.common.p2p.imports

import com.client.xvideos.common.p2p.P2pBundleInstaller
import com.client.xvideos.common.p2p.P2pManifest
import com.client.xvideos.common.p2p.P2pType
import java.io.File

/** Контракт импорта принятого бандла. */
fun interface BundleImporter {
    suspend fun import(manifest: P2pManifest, receivedFiles: Map<Long, File>)
}

/**
 * Кладёт принятый бандл в store нужного типа и дёргает refresh.
 *
 * @param storeRootFor корень store по типу (вызывающий подставляет `AppPath.*`).
 * @param refreshFor перечитать список store нужного типа (вызывающий подставляет `saved*.refresh()`).
 */
class StoreBundleImporter(
    private val storeRootFor: (P2pType) -> File,
    private val refreshFor: (P2pType) -> Unit,
) : BundleImporter {

    override suspend fun import(manifest: P2pManifest, receivedFiles: Map<Long, File>) {
        val root = storeRootFor(manifest.type)
        P2pBundleInstaller.install(root, manifest, receivedFiles)
        refreshFor(manifest.type)
    }
}
```

- [ ] **Step 4: Запустить — должен пройти**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.client.xvideos.common.p2p.imports.StoreBundleImporterTest" -q`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/client/xvideos/common/p2p/import/StoreBundleImporter.kt app/src/test/java/com/client/xvideos/common/p2p/imports/StoreBundleImporterTest.kt
git commit -m "feat(p2p): store bundle importer + BundleImporter contract"
```

---

## Task 11: NearbyClient контракт + события

**Files:**
- Create: `app/src/main/java/com/client/xvideos/common/p2p/nearby/NearbyClient.kt`

- [ ] **Step 1: Создать интерфейс и события**

```kotlin
package com.client.xvideos.common.p2p.nearby

import kotlinx.coroutines.flow.Flow
import java.io.File

/** События транспорта Nearby, общие для отправителя и получателя. */
sealed interface P2pEvent {
    data class EndpointFound(val endpointId: String, val name: String) : P2pEvent
    data class EndpointLost(val endpointId: String) : P2pEvent
    data class ConnectionInitiated(val endpointId: String, val endpointName: String, val authDigits: String) : P2pEvent
    data class Connected(val endpointId: String) : P2pEvent
    data class ConnectionRejected(val endpointId: String) : P2pEvent
    data class Disconnected(val endpointId: String) : P2pEvent
    data class FilePayloadReceived(val payloadId: Long, val file: File) : P2pEvent
    data class BytesPayloadReceived(val bytes: ByteArray) : P2pEvent {
        override fun equals(other: Any?) = this === other || (other is BytesPayloadReceived && bytes.contentEquals(other.bytes))
        override fun hashCode() = bytes.contentHashCode()
    }
    data class TransferProgress(val payloadId: Long, val transferred: Long, val total: Long) : P2pEvent
    data class Failed(val message: String) : P2pEvent
}

/** Тонкая обёртка над Nearby Connections. Реализация — [NearbyClientImpl]; в тестах — фейк. */
interface NearbyClient {
    val events: Flow<P2pEvent>

    fun startAdvertising(name: String)
    fun startDiscovery()
    fun requestConnection(endpointId: String, myName: String)
    fun acceptConnection(endpointId: String)
    fun rejectConnection(endpointId: String)

    /** Отправить файл; возвращает payloadId (одинаков на обоих телефонах). */
    suspend fun sendFile(endpointId: String, file: File): Long

    /** Отправить control-сообщение (манифест). */
    fun sendBytes(endpointId: String, bytes: ByteArray)

    fun stopAll()
}
```

- [ ] **Step 2: Проверить компиляцию**

Run: `.\gradlew.bat :app:compileDebugKotlin -q`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/client/xvideos/common/p2p/nearby/NearbyClient.kt
git commit -m "feat(p2p): NearbyClient contract + P2pEvent"
```

---

## Task 12: P2pReceiveController (TDD с фейком)

**Files:**
- Create: `app/src/main/java/com/client/xvideos/common/p2p/P2pReceiveController.kt`
- Test: `app/src/test/java/com/client/xvideos/common/p2p/FakeNearbyClient.kt`
- Test: `app/src/test/java/com/client/xvideos/common/p2p/P2pReceiveControllerTest.kt`

- [ ] **Step 1: Создать фейк транспорта (тестовый источник)**

```kotlin
package com.client.xvideos.common.p2p

import com.client.xvideos.common.p2p.nearby.NearbyClient
import com.client.xvideos.common.p2p.nearby.P2pEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import java.io.File

/** Фейковый транспорт: ручная эмиссия событий + запись вызовов. */
class FakeNearbyClient : NearbyClient {
    override val events = MutableSharedFlow<P2pEvent>(extraBufferCapacity = 64)

    val sentFiles = mutableListOf<Pair<String, File>>()
    val sentBytes = mutableListOf<ByteArray>()
    val accepted = mutableListOf<String>()
    var stopped = false
    var nextPayloadId = 1000L

    suspend fun emit(event: P2pEvent) { events.emit(event) }

    override fun startAdvertising(name: String) {}
    override fun startDiscovery() {}
    override fun requestConnection(endpointId: String, myName: String) {}
    override fun acceptConnection(endpointId: String) { accepted += endpointId }
    override fun rejectConnection(endpointId: String) {}
    override suspend fun sendFile(endpointId: String, file: File): Long {
        val id = nextPayloadId++
        sentFiles += endpointId to file
        return id
    }
    override fun sendBytes(endpointId: String, bytes: ByteArray) { sentBytes += bytes }
    override fun stopAll() { stopped = true }
}
```

- [ ] **Step 2: Написать падающий тест контроллера**

```kotlin
package com.client.xvideos.common.p2p

import com.client.xvideos.common.p2p.nearby.P2pEvent
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class P2pReceiveControllerTest {

    @get:Rule val tmp = TemporaryFolder()

    @Test
    fun `assembles bundle when manifest arrives before files and imports once complete`() = runTest {
        val fake = FakeNearbyClient()
        var imported: Pair<P2pManifest, Map<Long, File>>? = null
        val importer = BundleImporter { manifest, files -> imported = manifest to files }

        val controller = P2pReceiveController(
            nearby = fake,
            importer = importer,
            scope = backgroundScope,
            deviceName = "Pixel-Test",
        )
        controller.start()
        advanceUntilIdle()

        val fileA = tmp.newFile("a").apply { writeText("A") }
        val manifest = P2pManifest(
            type = P2pType.X,
            metadataFileName = "1.info",
            files = listOf(P2pManifestFile("1.mp4", "1.mp4", 5L, 1L)),
        )

        // Манифест приходит раньше файла.
        fake.emit(P2pEvent.ConnectionInitiated("E1", "Other", "1234"))
        fake.emit(P2pEvent.Connected("E1"))
        fake.emit(P2pEvent.BytesPayloadReceived(P2pManifestCodec.toBytes(manifest)))
        advanceUntilIdle()
        assertTrue("Импорт не должен случиться до прихода файла", imported == null)

        fake.emit(P2pEvent.FilePayloadReceived(5L, fileA))
        advanceUntilIdle()

        assertEquals(manifest, imported!!.first)
        assertEquals(fileA, imported!!.second.getValue(5L))
        assertEquals(ReceiveState.Done, controller.state.value)
        assertTrue(fake.stopped)
    }

    @Test
    fun `confirmConnection accepts the current endpoint`() = runTest {
        val fake = FakeNearbyClient()
        val controller = P2pReceiveController(fake, { _, _ -> }, backgroundScope, "Pixel-Test")
        controller.start()
        advanceUntilIdle()

        fake.emit(P2pEvent.ConnectionInitiated("E9", "Other", "0000"))
        advanceUntilIdle()
        controller.confirmConnection()

        assertEquals(listOf("E9"), fake.accepted)
    }
}
```

- [ ] **Step 3: Запустить — должен упасть**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.client.xvideos.common.p2p.P2pReceiveControllerTest" -q`
Expected: FAIL — `Unresolved reference: P2pReceiveController`.

- [ ] **Step 4: Реализовать контроллер**

```kotlin
package com.client.xvideos.common.p2p

import com.client.xvideos.common.p2p.nearby.NearbyClient
import com.client.xvideos.common.p2p.nearby.P2pEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/** Состояние экрана приёма. */
sealed interface ReceiveState {
    data object Idle : ReceiveState
    data object Advertising : ReceiveState
    data class Connecting(val endpointName: String, val authDigits: String) : ReceiveState
    data class Receiving(val transferred: Long, val total: Long) : ReceiveState
    data object Done : ReceiveState
    data class Error(val message: String) : ReceiveState
}

/**
 * Приёмная сторона: рекламируется, принимает соединение, буферизует payload'ы,
 * по приходу манифеста и всех файлов вызывает [importer].
 */
class P2pReceiveController(
    private val nearby: NearbyClient,
    private val importer: BundleImporter,
    private val scope: CoroutineScope,
    private val deviceName: String,
) {
    private val _state = MutableStateFlow<ReceiveState>(ReceiveState.Idle)
    val state: StateFlow<ReceiveState> = _state.asStateFlow()

    private val receivedFiles = mutableMapOf<Long, File>()
    private var manifest: P2pManifest? = null
    private var currentEndpoint: String? = null

    fun start() {
        _state.value = ReceiveState.Advertising
        scope.launch { nearby.events.collect { handle(it) } }
        nearby.startAdvertising(deviceName)
    }

    private suspend fun handle(event: P2pEvent) {
        when (event) {
            is P2pEvent.ConnectionInitiated -> {
                currentEndpoint = event.endpointId
                _state.value = ReceiveState.Connecting(event.endpointName, event.authDigits)
            }
            is P2pEvent.Connected -> _state.value = ReceiveState.Receiving(0, 0)
            is P2pEvent.TransferProgress -> _state.value = ReceiveState.Receiving(event.transferred, event.total)
            is P2pEvent.FilePayloadReceived -> {
                receivedFiles[event.payloadId] = event.file
                tryImport()
            }
            is P2pEvent.BytesPayloadReceived -> {
                manifest = runCatching { P2pManifestCodec.fromBytes(event.bytes) }.getOrNull()
                if (manifest == null) _state.value = ReceiveState.Error("Битый манифест") else tryImport()
            }
            is P2pEvent.Disconnected ->
                if (_state.value !is ReceiveState.Done) _state.value = ReceiveState.Error("Соединение разорвано")
            is P2pEvent.Failed -> _state.value = ReceiveState.Error(event.message)
            else -> Unit
        }
    }

    private suspend fun tryImport() {
        val m = manifest ?: return
        if (!m.files.all { receivedFiles.containsKey(it.payloadId) }) return
        try {
            importer.import(m, receivedFiles.toMap())
            _state.value = ReceiveState.Done
            nearby.stopAll()
        } catch (e: Exception) {
            _state.value = ReceiveState.Error(e.message ?: "Ошибка импорта")
        }
    }

    fun confirmConnection() { currentEndpoint?.let { nearby.acceptConnection(it) } }
    fun reject() {
        currentEndpoint?.let { nearby.rejectConnection(it) }
        nearby.stopAll()
        _state.value = ReceiveState.Idle
    }
    fun stop() { nearby.stopAll() }
}
```

- [ ] **Step 5: Запустить — должен пройти**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.client.xvideos.common.p2p.P2pReceiveControllerTest" -q`
Expected: PASS (2 теста).

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/client/xvideos/common/p2p/P2pReceiveController.kt app/src/test/java/com/client/xvideos/common/p2p/FakeNearbyClient.kt app/src/test/java/com/client/xvideos/common/p2p/P2pReceiveControllerTest.kt
git commit -m "feat(p2p): receive controller assembles + imports bundle"
```

---

## Task 13: P2pShareController (TDD с фейком)

**Files:**
- Create: `app/src/main/java/com/client/xvideos/common/p2p/P2pShareController.kt`
- Test: `app/src/test/java/com/client/xvideos/common/p2p/P2pShareControllerTest.kt`

- [ ] **Step 1: Написать падающий тест**

```kotlin
package com.client.xvideos.common.p2p

import com.client.xvideos.common.p2p.nearby.P2pEvent
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class P2pShareControllerTest {

    @get:Rule val tmp = TemporaryFolder()

    @Test
    fun `on connected sends every file then a manifest describing them`() = runTest {
        val root = tmp.newFolder("xdl")
        val mp4 = File(root, "3.mp4").apply { writeText("VVV") }
        val info = File(root, "3.info").apply { writeText("{}") }
        val bundle = P2pExportBundle(P2pType.X, root, listOf(mp4, info), info)

        val fake = FakeNearbyClient()
        val controller = P2pShareController(fake, backgroundScope, myName = "Sender", bundle = bundle)
        controller.start()
        advanceUntilIdle()

        fake.emit(P2pEvent.EndpointFound("E1", "Receiver"))
        advanceUntilIdle()
        controller.connectTo("E1")
        fake.emit(P2pEvent.Connected("E1"))
        advanceUntilIdle()

        assertEquals(2, fake.sentFiles.size)
        assertEquals(1, fake.sentBytes.size)
        val manifest = P2pManifestCodec.fromBytes(fake.sentBytes.first())
        assertEquals(P2pType.X, manifest.type)
        assertEquals(setOf("3.mp4", "3.info"), manifest.files.map { it.name }.toSet())
        assertTrue(manifest.files.all { it.payloadId >= 1000L })
    }
}
```

- [ ] **Step 2: Запустить — должен упасть**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.client.xvideos.common.p2p.P2pShareControllerTest" -q`
Expected: FAIL — `Unresolved reference: P2pShareController`.

- [ ] **Step 3: Реализовать контроллер**

```kotlin
package com.client.xvideos.common.p2p

import com.client.xvideos.common.p2p.nearby.NearbyClient
import com.client.xvideos.common.p2p.nearby.P2pEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/** Найденный рядом телефон. */
data class P2pEndpoint(val id: String, val name: String)

/** Состояние экрана отправки. */
sealed interface ShareState {
    data object Idle : ShareState
    data class Searching(val endpoints: List<P2pEndpoint>) : ShareState
    data class Connecting(val authDigits: String?) : ShareState
    data class Sending(val transferred: Long, val total: Long) : ShareState
    data object Done : ShareState
    data class Error(val message: String) : ShareState
}

/**
 * Отправляющая сторона: ищет телефоны, по подключению шлёт файлы [bundle], затем манифест.
 */
class P2pShareController(
    private val nearby: NearbyClient,
    private val scope: CoroutineScope,
    private val myName: String,
    private val bundle: P2pExportBundle,
) {
    private val _state = MutableStateFlow<ShareState>(ShareState.Idle)
    val state: StateFlow<ShareState> = _state.asStateFlow()

    private val endpoints = linkedMapOf<String, P2pEndpoint>()
    private var targetEndpoint: String? = null

    fun start() {
        _state.value = ShareState.Searching(emptyList())
        scope.launch { nearby.events.collect { handle(it) } }
        nearby.startDiscovery()
    }

    fun connectTo(endpointId: String) {
        targetEndpoint = endpointId
        _state.value = ShareState.Connecting(null)
        nearby.requestConnection(endpointId, myName)
    }

    private suspend fun handle(event: P2pEvent) {
        when (event) {
            is P2pEvent.EndpointFound -> {
                endpoints[event.endpointId] = P2pEndpoint(event.endpointId, event.name)
                if (_state.value is ShareState.Searching) _state.value = ShareState.Searching(endpoints.values.toList())
            }
            is P2pEvent.EndpointLost -> {
                endpoints.remove(event.endpointId)
                if (_state.value is ShareState.Searching) _state.value = ShareState.Searching(endpoints.values.toList())
            }
            is P2pEvent.ConnectionInitiated -> _state.value = ShareState.Connecting(event.authDigits)
            is P2pEvent.Connected -> sendBundle(event.endpointId)
            is P2pEvent.TransferProgress -> _state.value = ShareState.Sending(event.transferred, event.total)
            is P2pEvent.ConnectionRejected -> _state.value = ShareState.Error("Получатель отклонил")
            is P2pEvent.Disconnected ->
                if (_state.value !is ShareState.Done) _state.value = ShareState.Error("Соединение разорвано")
            is P2pEvent.Failed -> _state.value = ShareState.Error(event.message)
            else -> Unit
        }
    }

    private fun sendBundle(endpointId: String) {
        _state.value = ShareState.Sending(0, 0)
        scope.launch {
            try {
                val payloadIds = HashMap<File, Long>()
                for (file in bundle.files) payloadIds[file] = nearby.sendFile(endpointId, file)
                val manifest = P2pManifestFactory.create(
                    type = bundle.type,
                    storeRoot = bundle.storeRoot,
                    files = bundle.files,
                    metadataFile = bundle.metadataFile,
                    payloadIds = payloadIds,
                )
                nearby.sendBytes(endpointId, P2pManifestCodec.toBytes(manifest))
            } catch (e: Exception) {
                _state.value = ShareState.Error(e.message ?: "Ошибка отправки")
            }
        }
    }

    fun stop() { nearby.stopAll() }
}
```

- [ ] **Step 4: Запустить — должен пройти**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.client.xvideos.common.p2p.P2pShareControllerTest" -q`
Expected: PASS.

- [ ] **Step 5: Прогнать весь P2P-пакет**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.client.xvideos.common.p2p.*" -q`
Expected: PASS (все тесты ядра).

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/client/xvideos/common/p2p/P2pShareController.kt app/src/test/java/com/client/xvideos/common/p2p/P2pShareControllerTest.kt
git commit -m "feat(p2p): share controller sends files + manifest"
```

---

## Task 14: NearbyClientImpl (GMS-обёртка)

**Files:**
- Create: `app/src/main/java/com/client/xvideos/common/p2p/nearby/NearbyClientImpl.kt`

- [ ] **Step 1: Реализовать обёртку над ConnectionsClient**

```kotlin
package com.client.xvideos.common.p2p.nearby

import android.content.Context
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Реализация [NearbyClient] поверх Google Nearby Connections.
 * Стратегия P2P_POINT_TO_POINT (1:1, максимальная скорость, BT + Wi-Fi).
 */
class NearbyClientImpl(context: Context) : NearbyClient {

    private val appContext = context.applicationContext
    private val client: ConnectionsClient = Nearby.getConnectionsClient(appContext)
    private val serviceId = "${appContext.packageName}.p2p"

    override val events = MutableSharedFlow<P2pEvent>(extraBufferCapacity = 64)

    /** Накопленные FILE-payload'ы по id, чтобы по завершению отдать готовый File. */
    private val incomingFiles = HashMap<Long, Payload>()

    private fun emit(event: P2pEvent) { events.tryEmit(event) }

    override fun startAdvertising(name: String) {
        val options = AdvertisingOptions.Builder().setStrategy(STRATEGY).build()
        client.startAdvertising(name, serviceId, connectionLifecycle, options)
            .addOnFailureListener { emit(P2pEvent.Failed("Advertising: ${it.message}")) }
    }

    override fun startDiscovery() {
        val options = DiscoveryOptions.Builder().setStrategy(STRATEGY).build()
        client.startDiscovery(serviceId, discoveryCallback, options)
            .addOnFailureListener { emit(P2pEvent.Failed("Discovery: ${it.message}")) }
    }

    override fun requestConnection(endpointId: String, myName: String) {
        client.requestConnection(myName, endpointId, connectionLifecycle)
            .addOnFailureListener { emit(P2pEvent.Failed("Connect: ${it.message}")) }
    }

    override fun acceptConnection(endpointId: String) {
        client.acceptConnection(endpointId, payloadCallback)
            .addOnFailureListener { emit(P2pEvent.Failed("Accept: ${it.message}")) }
    }

    override fun rejectConnection(endpointId: String) {
        client.rejectConnection(endpointId)
    }

    override suspend fun sendFile(endpointId: String, file: File): Long =
        suspendCancellableCoroutine { cont ->
            try {
                val payload = Payload.fromFile(file)
                client.sendPayload(endpointId, payload)
                    .addOnSuccessListener { cont.resume(payload.id) }
                    .addOnFailureListener { cont.resumeWithException(it) }
            } catch (e: Exception) {
                cont.resumeWithException(e)
            }
        }

    override fun sendBytes(endpointId: String, bytes: ByteArray) {
        client.sendPayload(endpointId, Payload.fromBytes(bytes))
    }

    override fun stopAll() {
        runCatching { client.stopAllEndpoints() }
        runCatching { client.stopAdvertising() }
        runCatching { client.stopDiscovery() }
        incomingFiles.clear()
    }

    private val discoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            emit(P2pEvent.EndpointFound(endpointId, info.endpointName))
        }
        override fun onEndpointLost(endpointId: String) {
            emit(P2pEvent.EndpointLost(endpointId))
        }
    }

    private val connectionLifecycle = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            emit(P2pEvent.ConnectionInitiated(endpointId, info.endpointName, info.authenticationDigits))
        }
        override fun onConnectionResult(endpointId: String, resolution: ConnectionResolution) {
            when (resolution.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> emit(P2pEvent.Connected(endpointId))
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> emit(P2pEvent.ConnectionRejected(endpointId))
                else -> emit(P2pEvent.Failed("Result: ${resolution.status.statusCode}"))
            }
        }
        override fun onDisconnected(endpointId: String) {
            emit(P2pEvent.Disconnected(endpointId))
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            when (payload.type) {
                Payload.Type.BYTES -> payload.asBytes()?.let { emit(P2pEvent.BytesPayloadReceived(it)) }
                Payload.Type.FILE -> incomingFiles[payload.id] = payload
                else -> Unit
            }
        }
        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            emit(P2pEvent.TransferProgress(update.payloadId, update.bytesTransferred, update.totalBytes))
            if (update.status == PayloadTransferUpdate.Status.SUCCESS) {
                val payload = incomingFiles.remove(update.payloadId) ?: return
                val asFile = payload.asFile()
                val javaFile = asFile?.asJavaFile()
                if (javaFile != null) {
                    emit(P2pEvent.FilePayloadReceived(update.payloadId, javaFile))
                } else {
                    Timber.w("P2P: FILE payload ${update.payloadId} без javaFile")
                }
            }
        }
    }

    private companion object {
        val STRATEGY: Strategy = Strategy.P2P_POINT_TO_POINT
    }
}
```

> Примечание: `asFile().asJavaFile()` доступен на текущей версии play-services-nearby и возвращает уже сохранённый файл во внешнем каталоге приложения; импортёр копирует его в store. Если на целевом устройстве `asJavaFile()` вернёт null (scoped storage), будет залогировано и файл пропущен — обрабатывается в Task 16 как ошибка приёма.

- [ ] **Step 2: Проверить компиляцию**

Run: `.\gradlew.bat :app:compileDebugKotlin -q`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/client/xvideos/common/p2p/nearby/NearbyClientImpl.kt
git commit -m "feat(p2p): NearbyConnections client implementation"
```

---

## Task 15: P2pPermissions (runtime-разрешения)

**Files:**
- Create: `app/src/main/java/com/client/xvideos/common/p2p/P2pPermissions.kt`

- [ ] **Step 1: Реализовать helper**

```kotlin
package com.client.xvideos.common.p2p

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/** Набор и проверка runtime-разрешений для Nearby в зависимости от версии Android. */
object P2pPermissions {

    fun required(): Array<String> = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.NEARBY_WIFI_DEVICES,
        )
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> arrayOf(
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )
        else -> arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
        )
    }

    fun allGranted(context: Context): Boolean = required().all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
}
```

- [ ] **Step 2: Проверить компиляцию**

Run: `.\gradlew.bat :app:compileDebugKotlin -q`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/client/xvideos/common/p2p/P2pPermissions.kt
git commit -m "feat(p2p): runtime permissions helper"
```

---

## Task 16: Экран приёма + кнопка в MenuScreen

**Files:**
- Create: `app/src/main/java/com/client/xvideos/common/p2p/ui/ScreenP2pReceive.kt`
- Modify: `app/src/main/java/com/client/xvideos/MainActivity.kt` (topBar `MenuScreen`, ~строки 214–238)

- [ ] **Step 1: Создать экран приёма**

```kotlin
package com.client.xvideos.common.p2p.ui

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.p2p.P2pPermissions
import com.client.xvideos.common.p2p.P2pReceiveController
import com.client.xvideos.common.p2p.P2pType
import com.client.xvideos.common.p2p.ReceiveState
import com.client.xvideos.common.p2p.imports.StoreBundleImporter
import com.client.xvideos.common.p2p.nearby.NearbyClientImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File

/** Экран «Приём P2P»: рекламируется и принимает один item. */
class ScreenP2pReceive : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val scope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate) }

        val controller = remember {
            P2pReceiveController(
                nearby = NearbyClientImpl(context),
                importer = StoreBundleImporter(
                    storeRootFor = { type ->
                        when (type) {
                            P2pType.X -> File(AppPath.x_cache_download)
                            P2pType.R -> File(AppPath.r_cache_download)
                            P2pType.L -> File(AppPath.l_likes)
                        }
                    },
                    refreshFor = { /* экраны Saved перечитывают список при открытии */ },
                ),
                scope = scope,
                deviceName = Build.MODEL ?: "Android",
            )
        }

        val permissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->
            if (result.values.all { it }) controller.start()
        }

        LaunchedEffect(Unit) {
            if (P2pPermissions.allGranted(context)) controller.start()
            else permissionLauncher.launch(P2pPermissions.required())
        }

        DisposableEffect(Unit) {
            onDispose { controller.stop() }
        }

        val state by controller.state.collectAsState()

        Scaffold { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                when (val s = state) {
                    is ReceiveState.Idle,
                    is ReceiveState.Advertising -> {
                        CircularProgressIndicator()
                        Text("Ожидание отправителя…", modifier = Modifier.padding(top = 16.dp))
                    }
                    is ReceiveState.Connecting -> {
                        Text("Запрос от: ${s.endpointName}", style = MaterialTheme.typography.titleMedium)
                        Text("Код: ${s.authDigits}", modifier = Modifier.padding(vertical = 8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = { controller.confirmConnection() }) { Text("Принять") }
                            OutlinedButton(onClick = { controller.reject() }) { Text("Отклонить") }
                        }
                    }
                    is ReceiveState.Receiving -> {
                        CircularProgressIndicator()
                        val pct = if (s.total > 0) (s.transferred * 100 / s.total) else 0
                        Text("Приём… $pct%", modifier = Modifier.padding(top = 16.dp))
                    }
                    is ReceiveState.Done -> {
                        Text("Принято ✓", style = MaterialTheme.typography.titleLarge)
                        Button(onClick = { navigator.pop() }, modifier = Modifier.padding(top = 16.dp)) { Text("Готово") }
                    }
                    is ReceiveState.Error -> {
                        Text("Ошибка: ${s.message}", color = MaterialTheme.colorScheme.error)
                        Button(onClick = { navigator.pop() }, modifier = Modifier.padding(top = 16.dp)) { Text("Закрыть") }
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 2: Добавить кнопку приёма в `MenuScreen` topBar**

В `app/src/main/java/com/client/xvideos/MainActivity.kt`, внутри `topBar` объекта `MenuScreen` (в `Box`, рядом с кнопкой haptic-demo), добавить кнопку. Сначала добавить импорты (рядом с прочими `androidx.compose.material.icons`):

```kotlin
import androidx.compose.material.icons.filled.Wifi
import com.client.xvideos.common.p2p.ui.ScreenP2pReceive
```

Затем внутри `Box(modifier = Modifier.fillMaxWidth(), ...)` (после блока haptic-demo `IconButton`, перед закрытием `Box`) добавить:

```kotlin
                    // Приём item по P2P (Nearby)
                    IconButton(
                        onClick = { navigator.push(ScreenP2pReceive()) },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .displayCutoutPadding()
                            .size(48.dp)
                    ) {
                        Icon(
                            Icons.Default.Wifi,
                            contentDescription = "Приём P2P",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
```

- [ ] **Step 3: Проверить компиляцию**

Run: `.\gradlew.bat :app:compileDebugKotlin -q`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/client/xvideos/common/p2p/ui/ScreenP2pReceive.kt app/src/main/java/com/client/xvideos/MainActivity.kt
git commit -m "feat(p2p): receive screen + topBar entry in MenuScreen"
```

---

## Task 17: Sheet поиска телефонов + диалог выбора отправки

**Files:**
- Create: `app/src/main/java/com/client/xvideos/common/p2p/ui/P2pDeviceSearchSheet.kt`
- Create: `app/src/main/java/com/client/xvideos/common/p2p/ui/P2pSendChooserDialog.kt`

- [ ] **Step 1: Создать sheet отправки**

```kotlin
package com.client.xvideos.common.p2p.ui

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.p2p.P2pExportBundle
import com.client.xvideos.common.p2p.P2pShareController
import com.client.xvideos.common.p2p.ShareState
import com.client.xvideos.common.p2p.nearby.NearbyClientImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Bottom sheet «Поиск телефонов рядом» для отправки [bundle].
 * Требует уже выданных разрешений (проверяет вызывающий) — здесь только дискавери и отправка.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun P2pDeviceSearchSheet(
    bundle: P2pExportBundle,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate) }
    val controller = remember {
        P2pShareController(
            nearby = NearbyClientImpl(context),
            scope = scope,
            myName = Build.MODEL ?: "Android",
            bundle = bundle,
        )
    }

    LaunchedEffect(Unit) { controller.start() }
    DisposableEffect(Unit) { onDispose { controller.stop() } }

    val state by controller.state.collectAsState()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when (val s = state) {
                is ShareState.Idle,
                is ShareState.Searching -> {
                    Text("Телефоны рядом", style = MaterialTheme.typography.titleMedium)
                    val list = (s as? ShareState.Searching)?.endpoints.orEmpty()
                    if (list.isEmpty()) {
                        Text("Поиск…")
                        CircularProgressIndicator()
                    } else {
                        LazyColumn {
                            items(list, key = { it.id }) { ep ->
                                Text(
                                    ep.name,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { controller.connectTo(ep.id) }
                                        .padding(vertical = 12.dp)
                                )
                            }
                        }
                    }
                }
                is ShareState.Connecting -> Text("Соединение… код: ${s.authDigits ?: "…"}")
                is ShareState.Sending -> {
                    val pct = if (s.total > 0) (s.transferred * 100 / s.total) else 0
                    Text("Отправка… $pct%")
                    CircularProgressIndicator()
                }
                is ShareState.Done -> Text("Готово ✓")
                is ShareState.Error -> Text("Ошибка: ${s.message}", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
```

- [ ] **Step 2: Создать диалог выбора «Система / P2P»**

```kotlin
package com.client.xvideos.common.p2p.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Маленький диалог: выбрать способ «Поделиться» — системный chooser или P2P рядом.
 */
@Composable
fun P2pSendChooserDialog(
    onSystem: () -> Unit,
    onP2p: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Поделиться") },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { onDismiss(); onSystem() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Системное (через приложения)")
                }
                TextButton(onClick = { onDismiss(); onP2p() }, modifier = Modifier.fillMaxWidth()) {
                    Text("P2P рядом (Nearby)")
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}
```

- [ ] **Step 3: Проверить компиляцию**

Run: `.\gradlew.bat :app:compileDebugKotlin -q`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/client/xvideos/common/p2p/ui/P2pDeviceSearchSheet.kt app/src/main/java/com/client/xvideos/common/p2p/ui/P2pSendChooserDialog.kt
git commit -m "feat(p2p): sender device-search sheet + send chooser dialog"
```

---

## Task 18: Подключить P2P-отправку в L (likes)

**Files:**
- Modify: `app/src/main/java/com/client/xvideos/l/ui/element/expandMenu/ExpandMenuVM.kt`

Точка `share(item)` уже качает в share-cache и зовёт системный share. Добавляем: выбор «Система / P2P», и при P2P — находим сохранённую папку L и открываем sheet. Поскольку Compose-host для sheet нужен, прокинем флаг состояния и покажем UI в `ExpandMenuAlbum`/`ExpandMenuLikes`.

- [ ] **Step 1: Добавить state и P2P-ветку в ViewModel**

В `ExpandMenuViewModel` добавить импорты:

```kotlin
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.p2p.P2pExportBundle
import com.client.xvideos.common.p2p.export.LExporter
import com.client.xvideos.l.featured.saved.lFindLikeFolder
import com.client.xvideos.common.p2p.ui.P2pDeviceSearchSheet
import com.client.xvideos.common.p2p.ui.P2pSendChooserDialog
import java.io.File
```

Внутри класса добавить состояние и хелперы:

```kotlin
    // P2P share UI state
    var p2pChooserItem by mutableStateOf<PicsDetails?>(null)
        private set
    var p2pBundle by mutableStateOf<P2pExportBundle?>(null)
        private set

    fun onShareClicked(item: PicsDetails) { p2pChooserItem = item }
    fun dismissChooser() { p2pChooserItem = null }
    fun dismissP2p() { p2pBundle = null }

    fun startP2p(item: PicsDetails) {
        val url = item.url_to_original
        val folder = url?.let { lFindLikeFolder(File(AppPath.l_likes), it) }
        val bundle = folder?.let { LExporter.export(it) }
        if (bundle == null) {
            SnackBar.error("Сначала сохрани (Like) — нет файлов для P2P")
            return
        }
        p2pBundle = bundle
    }

    @Composable
    fun P2pShareHost() {
        p2pChooserItem?.let { item ->
            P2pSendChooserDialog(
                onSystem = { share(item) },
                onP2p = { startP2p(item) },
                onDismiss = { dismissChooser() },
            )
        }
        p2pBundle?.let { bundle ->
            P2pDeviceSearchSheet(bundle = bundle, onDismiss = { dismissP2p() })
        }
    }
```

Заменить переданный в меню `onShare` на `onShareClicked`:

```kotlin
        AlbumItemExpandMenu(
            item = item, onDownload = { it1 -> downloadLike(it1, album) },
            onShare = { it1 -> onShareClicked(it1) },
            isCollection = isCollection,
            savedL = saved,
            onRemoveFromCollection = { it -> },
            idAlbum = idAlbum
        )
```

И в конце `ExpandMenuAlbum` и `ExpandMenuLikes` (внутри их `@Composable` тел) вызвать host:

```kotlin
        P2pShareHost()
```

- [ ] **Step 2: Проверить компиляцию**

`lFindLikeFolder` объявлена `internal` в `app/src/main/java/com/client/xvideos/l/featured/saved/LCollectionFs.kt:427` — она доступна из ui-пакета (тот же модуль), правок видимости не требуется.

Run: `.\gradlew.bat :app:compileDebugKotlin -q`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/client/xvideos/l/ui/element/expandMenu/ExpandMenuVM.kt
git commit -m "feat(p2p): wire P2P send into L expand menu (system/p2p chooser)"
```

---

## Task 19: Подключить P2P-отправку в R и X (экраны загрузок)

**Files:**
- Modify: `app/src/main/java/com/client/xvideos/r/ui/explorer/tab/saved/tab/R_Screen_Saved_DownloadTab.kt` (~строки 94–101, 106–147)
- Modify: `app/src/main/java/com/client/xvideos/x/screens/saved/ScreenSavedX.kt`

R и X «Загрузки» работают с уже скачанными файлами — идеальны для P2P без оговорок.

- [ ] **Step 1: R — заменить системный share на выбор Система/P2P**

В `R_Screen_Saved_DownloadTab.Content()` добавить импорты:

```kotlin
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.p2p.P2pExportBundle
import com.client.xvideos.common.p2p.export.RExporter
import com.client.xvideos.common.p2p.ui.P2pDeviceSearchSheet
import com.client.xvideos.common.p2p.ui.P2pSendChooserDialog
import com.client.xvideos.common.snackbar.SnackBar
import java.io.File
```

Внутри `Content()` (после `val onShareClickHandler = ...`) добавить состояние и переопределить обработчик:

```kotlin
        var chooserItem by remember { mutableStateOf<GifsInfo?>(null) }
        var p2pBundle by remember { mutableStateOf<P2pExportBundle?>(null) }

        val onShareClickHandler = remember(context) {
            { item: GifsInfo -> chooserItem = item }
        }

        chooserItem?.let { item ->
            P2pSendChooserDialog(
                onSystem = { useCaseShareGifs(context, item) },
                onP2p = {
                    val bundle = RExporter.export(File(AppPath.r_cache_download), item.userName, item.id)
                    if (bundle == null) SnackBar.error("Нет скачанных файлов для P2P") else p2pBundle = bundle
                },
                onDismiss = { chooserItem = null },
            )
        }
        p2pBundle?.let { bundle ->
            P2pDeviceSearchSheet(bundle = bundle, onDismiss = { p2pBundle = null })
        }
```

(Старый `onShareClickHandler`, вызывавший `useCaseShareGifs` напрямую, удалить — оставить только новый, выставляющий `chooserItem`.)

- [ ] **Step 2: X — добавить пункт «Поделиться» в меню сохранённого**

Открыть `app/src/main/java/com/client/xvideos/x/screens/saved/ScreenSavedX.kt`. Найти строку, где у item есть `IconButton(onClick = onDelete)` (~строка 155). Рядом добавить кнопку share. В начало файла добавить импорты:

```kotlin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Share
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.p2p.P2pExportBundle
import com.client.xvideos.common.p2p.export.XExporter
import com.client.xvideos.common.p2p.ui.P2pDeviceSearchSheet
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.x.model.ItemsX
import java.io.File
```

В composable со списком сохранённого X (там, где доступен `item: ItemsX` и `onDelete`) добавить состояние P2P и кнопку:

```kotlin
        var p2pBundle by remember { mutableStateOf<P2pExportBundle?>(null) }

        IconButton(onClick = {
            val bundle = XExporter.export(File(AppPath.x_cache_download), item.id)
            if (bundle == null) SnackBar.error("Нет скачанного видео для P2P") else p2pBundle = bundle
        }) {
            Icon(Icons.Outlined.Share, contentDescription = "P2P", tint = Color.White)
        }

        p2pBundle?.let { bundle ->
            P2pDeviceSearchSheet(bundle = bundle, onDismiss = { p2pBundle = null })
        }
```

> Для X отправка идёт сразу в P2P (без диалога Система/P2P), т.к. системного share для X-сохранённого ранее не было — это новая возможность.

- [ ] **Step 3: Проверить компиляцию**

Run: `.\gradlew.bat :app:compileDebugKotlin -q`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/client/xvideos/r/ui/explorer/tab/saved/tab/R_Screen_Saved_DownloadTab.kt app/src/main/java/com/client/xvideos/x/screens/saved/ScreenSavedX.kt
git commit -m "feat(p2p): wire P2P send into R and X saved screens"
```

---

## Task 20: Полная проверка сборки и тестов

**Files:** —

- [ ] **Step 1: Прогнать все unit-тесты P2P**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.client.xvideos.common.p2p.*" -q`
Expected: PASS — все тесты из задач 4, 6, 7, 8, 9, 10, 12, 13.

- [ ] **Step 2: Собрать debug-APK**

Run: `.\gradlew.bat :app:assembleDebug -q`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Ручная проверка на двух телефонах (чек-лист)**

Установить debug-APK на два устройства. Проверить:
1. На телефоне-получателе: меню → кнопка Wi-Fi (вверху по центру) → выдать разрешения → «Ожидание отправителя…».
2. На телефоне-отправителе: открыть сохранённый X (Загрузки) / R (Загрузки) / L (Likes) → «Поделиться» → «P2P рядом».
3. Sheet показывает имя второго телефона → тап → оба показывают код → «Принять» на получателе.
4. Прогресс отправки/приёма → «Принято ✓».
5. На получателе открыть соответствующий раздел Saved — item на месте, открывается/играется.
6. Повторить для всех трёх типов (X, R, L).
7. Негатив: BT/Wi-Fi выключен → внятная ошибка; отмена в процессе (закрыть sheet) → нет крэша.

- [ ] **Step 4: Финальный commit (если были правки по итогам проверки)**

```bash
git add -A
git commit -m "test(p2p): manual two-device verification fixes"
```

---

## Self-Review (выполнено при написании плана)

**Покрытие спека:**
- Передача байтов медиа + метаданных → Tasks 7–10, 14 (FILE+BYTES payloads). ✓
- Маршрутизация по типу X/R/L → `P2pType`, `StoreBundleImporter` (Task 10), exporters (Task 9). ✓
- Кнопка приёма в `MenuScreen` topBar → Task 16. ✓
- Один item за раз → bundle на один item, контроллеры (Tasks 12–13). ✓
- Авто-импорт + уведомление → `tryImport` + «Принято ✓» (Tasks 12, 16). ✓
- Формат store (Подход A) → relativePath-манифест + installer (Tasks 6–7). ✓
- Выбор «Система / P2P» → `P2pSendChooserDialog` (Task 17), L/R (Tasks 18–19). ✓
- Разрешения → Task 2 (манифест) + Task 15 (runtime). ✓
- Ошибки/обрывы → `ShareState.Error`/`ReceiveState.Error`, обработка в контроллерах. ✓
- **Отклонение:** докачка не-сохранённых item за v1 НЕ реализуется (вместо неё — «Сначала сохрани»). Зафиксировано в разделе «v1 ограничения» для подтверждения на ревью.

**Плейсхолдеры:** не найдено — у каждого шага полный код/команды.

**Согласованность типов:** `P2pManifest`/`P2pManifestFile`/`P2pType`/`P2pExportBundle`/`NearbyClient`/`P2pEvent`/`ReceiveState`/`ShareState`/`BundleImporter` совпадают по сигнатурам между задачами и тестами.
