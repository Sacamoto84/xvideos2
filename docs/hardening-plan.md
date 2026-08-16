# План закрытия находок кодревью 2026-08-16

> **Для агентных исполнителей:** ОБЯЗАТЕЛЬНЫЙ СУБ-НАВЫК: используйте
> `superpowers:subagent-driven-development` (рекомендуется) или
> `superpowers:executing-plans` для выполнения плана задача за задачей.
> Шаги размечены чекбоксами (`- [ ]`) для отслеживания.

**Цель:** закрыть находки ревью по границе доверия и целостности данных: обход
пути при приёме P2P-бандла, имя коллекции без санитизации, неатомарная запись
коллекций, расхождение `FileDB` с собственным контрактом потокобезопасности,
плюс мелкие дефекты бэкапа и обход троттлинга код-доступа.

**Архитектура:** три проверки, размазанные по проекту копипастой
(нормализация пути, containment-проверка, атомарная запись temp+rename),
собираются в один пакет `common/io` и переиспользуются; на этом фундаменте
чинятся конкретные дефекты. Валидация имени коллекции ставится в доменные
методы (`createCollection` / `renameCollection`), а не в UI — диалоги уже
показывают ошибки через `SnackBar`, править их не придётся. Арифметика
блокировки код-доступа выносится из `AppLockRepository` в чистый объект, чтобы
её можно было протестировать на JVM без Android.

**Стек:** Kotlin, Android Gradle (модули `:core`, `:feature-l`, `:feature-r`),
JUnit4 + `org.junit.rules.TemporaryFolder`, `kotlinx-coroutines-test`,
Gson, Compose Runtime (`mutableStateListOf`).

**Радиус:** в этот план **не входят**:

- Архитектурные находки A1 (два механизма DI), A2 (дублирование
  `CollectionDB`/`FileDB`), A3 (Gson против kotlinx.serialization), A4 (учёт
  статусов ревью) — рефакторинги другого масштаба, каждый со своим планом.
- T1 — `App.awaitStorageCleanup` как соглашение, а не механизм. Правка меняет
  контракт доступа к `AppPath.p2p_inbox` / `p2p_outbox` / `l_cacheDownload`
  (suspend-геттер вместо `val`), а значит задевает `AppPath`,
  `P2pSectionImporters`, `P2pReceiveManager`, `Exporters` и `ScreenP2pSend`.
  Радиус великоват для плана про дефекты — нужен отдельный.
- T2 — `EventBus` с однопоточным диспетчером: медленный подписчик подвешивает
  всю шину. При `extraBufferCapacity = 1024` это не дефект, а свойство выбранной
  схемы; менять его без замера — гадание.

Здесь только дефекты: S1-S4, C1-C5 и T3.

---

## Порядок задач

| Задача | Находка ревью | Приоритет |
| --- | --- | --- |
| 1. Общий `common/io` | фундамент для 2 и 4 | — |
| 2. Валидация `relativePath` в P2P | S1 | критично |
| 3. Валидация имени коллекции | S2 | высокий |
| 4. Атомарная запись в `CollectionDB` | C1 | высокий |
| 5. Локи в `FileDB` | C2 | высокий |
| 6. Мелочи `XlrBackupManager` | C3, C4, C5 | средний |
| 7. Троттлинг код-доступа по монотонным часам | S3 | средний |
| 8. `SecureCredentialStore` без потери секретов | S4 | низкий |
| 9. `onTerminate` не маскирует ошибку старта | T3 | низкий |

Задача 1 обязательна перед 2 и 4. Остальные независимы и могут идти в любом
порядке.

---

## Файловая структура

**Создаются:**

- `core/src/main/java/com/client/xvideos/common/io/SafePath.kt` — нормализация
  относительного пути из недоверенного источника и проверка, что цель не
  выходит за корень. Сейчас существует в трёх копиях (`ZipUtils`,
  `XlrBackupManager`, и не существует там, где нужнее всего — в
  `P2pBundleInstaller`).
- `core/src/main/java/com/client/xvideos/common/io/AtomicWrite.kt` — запись
  файла через temp + rename. Сейчас копия в `FileDB`, отсутствует в
  `CollectionDB`.
- `core/src/main/java/com/client/xvideos/common/collectionDB/CollectionName.kt`
  — валидация имени коллекции, которое становится именем папки.
- `core/src/main/java/com/client/xvideos/common/applock/AppLockThrottle.kt` —
  чистая арифметика блокировки ввода, без `Context`.
- `core/src/test/java/com/client/xvideos/common/io/SafePathTest.kt`
- `core/src/test/java/com/client/xvideos/common/collectionDB/CollectionNameTest.kt`
- `core/src/test/java/com/client/xvideos/common/collectionDB/CollectionDBTest.kt`
- `core/src/test/java/com/client/xvideos/common/fileDB/FileDBTest.kt`
- `core/src/test/java/com/client/xvideos/common/applock/AppLockThrottleTest.kt`

**Меняются:**

- `core/src/main/java/com/client/xvideos/common/zip/ZipUtils.kt:61-77` —
  приватные хелперы заменяются вызовами `SafePath`.
- `core/src/main/java/com/client/xvideos/common/backup/XlrBackupManager.kt` —
  `:139` (`setLevel`), `:240-258` (манифест), `:216-223` (откат), `:507-522`
  (хелперы → `SafePath`).
- `core/src/main/java/com/client/xvideos/common/p2p/P2pManifest.kt` — валидация
  `relativePath` в `P2pManifestCodec.fromJson`.
- `core/src/main/java/com/client/xvideos/common/p2p/P2pBundleInstaller.kt:16` —
  containment-проверка.
- `core/src/main/java/com/client/xvideos/common/collectionDB/CollectionDB.kt` —
  валидация имени + атомарная запись.
- `core/src/main/java/com/client/xvideos/common/fileDB/FileDB.kt:92,131,145` —
  локи и общая атомарная запись.
- `core/src/main/java/com/client/xvideos/common/applock/AppLockRepository.kt:89-115`
  — переход на `AppLockThrottle`.
- `core/src/main/java/com/client/xvideos/common/settings/SecureCredentialStore.kt:37-65`
  — не удалять хранилище на транзиентной ошибке.
- `feature-l/src/main/java/com/client/xvideos/l/featured/saved/SavedL_Collection.kt:84-150`
  — валидация имени.
- `app/src/main/java/com/client/xvideos/App.kt:194-197` — `onTerminate` не
  трогает неинициализированный `lateinit`.

---

### Задача 1: Общий `common/io` — путь и атомарная запись

Чистый вынос без изменения поведения. Существующие `ZipUtilsTest` и
`XlrRestoreApplyTest` должны остаться зелёными — они и есть страховка.

**Файлы:**
- Создать: `core/src/main/java/com/client/xvideos/common/io/SafePath.kt`
- Создать: `core/src/main/java/com/client/xvideos/common/io/AtomicWrite.kt`
- Создать: `core/src/test/java/com/client/xvideos/common/io/SafePathTest.kt`
- Изменить: `core/src/main/java/com/client/xvideos/common/zip/ZipUtils.kt:61-77`
- Изменить: `core/src/main/java/com/client/xvideos/common/backup/XlrBackupManager.kt:507-522`
- Изменить: `core/src/main/java/com/client/xvideos/common/fileDB/FileDB.kt:145-156`

- [ ] **Шаг 1: Написать падающий тест**

Создать `core/src/test/java/com/client/xvideos/common/io/SafePathTest.kt`:

```kotlin
package com.client.xvideos.common.io

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SafePathTest {

    @get:Rule val tmp = TemporaryFolder()

    @Test
    fun `нормализация приводит разделители и убирает пустые сегменты`() {
        assertEquals("a/b/c.txt", normalizeRelativePath("a\\b//c.txt"))
        assertEquals("a/b", normalizeRelativePath("/a/b/"))
    }

    @Test
    fun `путь с двумя точками отвергается`() {
        assertThrows(IllegalArgumentException::class.java) {
            normalizeRelativePath("../escape.txt")
        }
        assertThrows(IllegalArgumentException::class.java) {
            normalizeRelativePath("a/../../escape.txt")
        }
    }

    @Test
    fun `пустое имя и двоеточие отвергаются`() {
        assertThrows(IllegalArgumentException::class.java) { normalizeRelativePath("   ") }
        assertThrows(IllegalArgumentException::class.java) { normalizeRelativePath("C:/data") }
    }

    @Test
    fun `requireInside пропускает цель внутри корня`() {
        val root = tmp.newFolder("root")
        requireInside(root, File(root, "a/b.txt"))
        requireInside(root, root)
    }

    @Test
    fun `requireInside отвергает цель снаружи`() {
        val root = tmp.newFolder("root2")
        val outside = File(root.parentFile, "outside.txt")
        assertThrows(IllegalArgumentException::class.java) { requireInside(root, outside) }
    }

    @Test
    fun `requireInside не путает соседа с общим префиксом имени`() {
        val root = tmp.newFolder("data")
        val sibling = File(root.parentFile, "data_backup")
        assertThrows(IllegalArgumentException::class.java) { requireInside(root, sibling) }
    }
}
```

- [ ] **Шаг 2: Запустить тест и убедиться, что он падает**

```bash
./gradlew :core:testDebugUnitTest --tests "com.client.xvideos.common.io.SafePathTest"
```

Ожидается: ошибка компиляции — `Unresolved reference: normalizeRelativePath`.

- [ ] **Шаг 3: Написать `SafePath.kt`**

```kotlin
package com.client.xvideos.common.io

import java.io.File

/*
 * Проверки пути, пришедшего из недоверенного источника: zip-архива, манифеста
 * чужого устройства, файла бэкапа. Раньше жили тремя копиями — в ZipUtils, в
 * XlrBackupManager и (не жили вовсе) в P2pBundleInstaller. Разошедшиеся копии
 * одной проверки безопасности — это ровно тот случай, когда одну из них
 * забывают: P2pBundleInstaller и забыли.
 */

/**
 * Приводит относительный путь к каноничному виду `a/b/c` и отвергает всё, чем
 * можно выйти за корень распаковки.
 *
 * Отвергается: пустое имя, абсолютный путь, `..` и `.` в любом сегменте,
 * двоеточие (диск в windows-путях и ADS в NTFS).
 *
 * @throws IllegalArgumentException если путь небезопасен.
 */
fun normalizeRelativePath(raw: String): String {
    val name = raw.replace('\\', '/').trim('/')
    require(name.isNotBlank()) { "Пустое имя пути" }
    require(!name.startsWith("/") && !name.contains(':')) { "Небезопасный путь: $raw" }
    val parts = name.split('/').filter { it.isNotBlank() }
    require(parts.none { it == "." || it == ".." }) { "Небезопасный путь: $raw" }
    return parts.joinToString("/")
}

/**
 * Проверяет, что [target] лежит внутри [root] (или совпадает с ним).
 *
 * Сравниваются канонические пути: без этого символическая ссылка внутри
 * корня уводила бы запись наружу. Разделитель в конце префикса обязателен —
 * иначе `/data/xvideos_backup` считался бы лежащим внутри `/data/xvideos`.
 *
 * @throws IllegalArgumentException если цель выходит за корень.
 */
fun requireInside(root: File, target: File) {
    val rootPath = root.canonicalPath
    val targetPath = target.canonicalPath
    require(targetPath == rootPath || targetPath.startsWith(rootPath + File.separator)) {
        "Путь выходит за пределы корня: $targetPath"
    }
}
```

- [ ] **Шаг 4: Запустить тест и убедиться, что он проходит**

```bash
./gradlew :core:testDebugUnitTest --tests "com.client.xvideos.common.io.SafePathTest"
```

Ожидается: PASS, 6 тестов.

- [ ] **Шаг 5: Написать `AtomicWrite.kt`**

```kotlin
package com.client.xvideos.common.io

import java.io.File
import java.io.IOException

/**
 * Пишет текст во временный файл рядом и переименовывает его поверх целевого.
 *
 * Переименование в пределах одной ФС атомарно, поэтому читатель видит либо
 * старое содержимое целиком, либо новое целиком — но не обрывок. Обрезанный
 * JSON в хранилищах приложения не диагностируется: он молча отбрасывается на
 * чтении, и элемент просто исчезает из списка.
 *
 * @throws IOException если записать файл не удалось.
 */
fun File.writeTextAtomically(text: String) {
    val temp = File(parentFile, "$name.tmp")
    temp.parentFile?.mkdirs()
    temp.writeText(text, Charsets.UTF_8)
    if (!temp.renameTo(this)) {
        // На некоторых ФС renameTo не перезаписывает существующий файл.
        delete()
        if (!temp.renameTo(this)) {
            temp.delete()
            throw IOException("Не удалось записать файл: $absolutePath")
        }
    }
}
```

- [ ] **Шаг 6: Переключить `ZipUtils` на общие хелперы**

В `core/src/main/java/com/client/xvideos/common/zip/ZipUtils.kt` удалить
приватные `normalizeEntryName` и `ensureInside` (строки 61-77) и заменить их
вызовы. В `unzip` строки

```kotlin
                val name = normalizeEntryName(entry.name)
                val target = File(root, name).canonicalFile
                ensureInside(root, target)
```

заменить на

```kotlin
                val name = normalizeRelativePath(entry.name)
                val target = File(root, name).canonicalFile
                requireInside(root, target)
```

и добавить импорты в шапку файла:

```kotlin
import com.client.xvideos.common.io.normalizeRelativePath
import com.client.xvideos.common.io.requireInside
```

- [ ] **Шаг 7: Переключить `XlrBackupManager` на общие хелперы**

В `core/src/main/java/com/client/xvideos/common/backup/XlrBackupManager.kt`
удалить приватные `normalizedEntryName` (`:507-514`) и `ensureInside`
(`:516-522`), добавить импорты:

```kotlin
import com.client.xvideos.common.io.normalizeRelativePath
import com.client.xvideos.common.io.requireInside
```

и заменить все вызовы:

- `normalizedEntryName(` → `normalizeRelativePath(` (встречается в
  `inspectBackup`, `validateBackup`, `extractBackup`, `normalizeSelectedPaths`);
- `ensureInside(` → `requireInside(` (встречается в `applyRestoredPaths` и
  `extractBackup`).

Тип исключения на пути `..` меняется с `IllegalStateException` на
`IllegalArgumentException`. `XlrRestoreApplyTest` ловит падение через
`runCatching`, тип не проверяет — тест остаётся зелёным.

- [ ] **Шаг 8: Переключить `FileDB` на общую атомарную запись**

В `core/src/main/java/com/client/xvideos/common/fileDB/FileDB.kt` удалить
приватный `writeAtomically` (`:145-156`), добавить импорт:

```kotlin
import com.client.xvideos.common.io.writeTextAtomically
```

и заменить оба вызова `writeAtomically(file, json)` (в `insert` и `update`) на
`file.writeTextAtomically(json)`.

- [ ] **Шаг 9: Прогнать весь модуль**

```bash
./gradlew :core:testDebugUnitTest
```

Ожидается: PASS. Особенно важны `ZipUtilsTest` и `XlrRestoreApplyTest` — они
подтверждают, что вынос не изменил поведение.

- [ ] **Шаг 10: Коммит**

```bash
git add core/src/main/java/com/client/xvideos/common/io core/src/test/java/com/client/xvideos/common/io core/src/main/java/com/client/xvideos/common/zip/ZipUtils.kt core/src/main/java/com/client/xvideos/common/backup/XlrBackupManager.kt core/src/main/java/com/client/xvideos/common/fileDB/FileDB.kt
git commit -m "refactor(core): одна проверка пути и одна атомарная запись на весь проект"
```

---

### Задача 2: Валидация `relativePath` в P2P-манифесте (S1)

Головная находка ревью. Путь из манифеста чужого устройства попадает в
`File(storeRoot, relativePath)` без единой проверки — пир пишет куда угодно
внутри UID приложения, включая `shared_prefs` с хешем код-доступа.

**Файлы:**
- Изменить: `core/src/main/java/com/client/xvideos/common/p2p/P2pManifest.kt`
- Изменить: `core/src/main/java/com/client/xvideos/common/p2p/P2pBundleInstaller.kt:13-20`
- Тест: `core/src/test/java/com/client/xvideos/common/p2p/P2pManifestCodecTest.kt`
- Тест: `core/src/test/java/com/client/xvideos/common/p2p/P2pBundleInstallerTest.kt`

- [ ] **Шаг 1: Написать падающие тесты на разбор манифеста**

В конец класса `P2pManifestCodecTest` (перед закрывающей скобкой файла)
добавить:

```kotlin
    @Test
    fun `файл с выходом за корень отвергается`() {
        val json = """{"type":"L","metadataFileName":null,""" +
            """"files":[{"name":"a.jpg","relativePath":"../../shared_prefs/x.xml","payloadId":1,"size":2}]}"""

        assertThrows(IllegalArgumentException::class.java) {
            P2pManifestCodec.fromJson(json)
        }
    }

    @Test
    fun `файл с абсолютным путём отвергается`() {
        val json = """{"type":"L","metadataFileName":null,""" +
            """"files":[{"name":"a.jpg","relativePath":"/data/data/com.client.xvideos/a.jpg","payloadId":1,"size":2}]}"""

        assertThrows(IllegalArgumentException::class.java) {
            P2pManifestCodec.fromJson(json)
        }
    }

    @Test
    fun `файл с пустым путём отвергается`() {
        val json = """{"type":"L","metadataFileName":null,""" +
            """"files":[{"name":"a.jpg","relativePath":"","payloadId":1,"size":2}]}"""

        assertThrows(IllegalArgumentException::class.java) {
            P2pManifestCodec.fromJson(json)
        }
    }
```

- [ ] **Шаг 2: Написать падающий тест на установщик**

В конец класса `P2pBundleInstallerTest` добавить:

```kotlin
    @Test
    fun `установщик отвергает путь за пределами store root`() {
        val received = tmp.newFile("payload_200").apply { writeText("EVIL") }
        val storeRoot = tmp.newFolder("likes_evil")
        val outside = File(storeRoot.parentFile, "stolen.txt")

        val manifest = P2pManifest(
            type = P2pType.L,
            metadataFileName = null,
            files = listOf(P2pManifestFile("a.jpg", "../stolen.txt", 200L, 4L)),
        )

        try {
            P2pBundleInstaller.install(storeRoot, manifest, mapOf(200L to received))
            fail("Ожидался отказ на пути за пределами store root")
        } catch (e: IllegalArgumentException) {
            // ожидаемо
        }
        assertFalse("файл не должен появиться снаружи store root", outside.exists())
    }
```

и дописать импорты в шапку `P2pBundleInstallerTest.kt`:

```kotlin
import org.junit.Assert.assertFalse
import org.junit.Assert.fail
```

- [ ] **Шаг 3: Запустить тесты и убедиться, что они падают**

```bash
./gradlew :core:testDebugUnitTest --tests "com.client.xvideos.common.p2p.P2pManifestCodecTest" --tests "com.client.xvideos.common.p2p.P2pBundleInstallerTest"
```

Ожидается: FAIL — четыре новых теста не проходят, потому что ни разбор, ни
установщик путь не проверяют.

- [ ] **Шаг 4: Добавить проверку в разбор манифеста**

В `core/src/main/java/com/client/xvideos/common/p2p/P2pManifest.kt` дописать
импорт:

```kotlin
import com.client.xvideos.common.io.normalizeRelativePath
```

и в `P2pManifestCodec.fromJson` заменить блок проверки файлов

```kotlin
        parsed.files.forEach { file ->
            @Suppress("SENSELESS_COMPARISON")
            require(file.name != null && file.relativePath != null) {
                "P2P-манифест: у файла нет имени или пути"
            }
        }

        return parsed
```

на

```kotlin
        parsed.files.forEach { file ->
            @Suppress("SENSELESS_COMPARISON")
            require(file.name != null && file.relativePath != null) {
                "P2P-манифест: у файла нет имени или пути"
            }
            // relativePath приходит с чужого устройства и напрямую задаёт, куда
            // ляжет файл. Без нормализации пир кладёт `../../shared_prefs/...`
            // и переписывает что угодно внутри UID приложения — включая хеш
            // код-доступа. Отвергаем битый путь здесь, чтобы runCatching в
            // P2pReceiveController поймал его до единой записи на диск.
            normalizeRelativePath(file.relativePath)
        }

        return parsed
```

- [ ] **Шаг 5: Добавить containment-проверку в установщик**

Заменить содержимое
`core/src/main/java/com/client/xvideos/common/p2p/P2pBundleInstaller.kt`
целиком:

```kotlin
package com.client.xvideos.common.p2p

import com.client.xvideos.common.io.normalizeRelativePath
import com.client.xvideos.common.io.requireInside
import java.io.File

/** Раскладывает принятые файлы по [P2pManifestFile.relativePath] внутри storeRoot (перезапись). */
object P2pBundleInstaller {

    /**
     * Путь каждого файла нормализуется и проверяется на попадание внутрь
     * [storeRoot] ещё раз, хотя [P2pManifestCodec.fromJson] уже отверг бы
     * битый манифест. Это защита в глубину: установщик — последняя точка перед
     * записью на диск, и он не должен полагаться на то, что кто-то выше по
     * стеку проверил вход. Ровно на такой цепочке «проверка в другом месте»
     * дыра и держалась.
     */
    fun install(
        storeRoot: File,
        manifest: P2pManifest,
        receivedFiles: Map<Long, File>,
    ): List<File> {
        val root = storeRoot.canonicalFile
        return manifest.files.map { entry ->
            val source = receivedFiles[entry.payloadId]
                ?: error("Missing received file for payloadId ${entry.payloadId} (${entry.name})")
            val target = File(root, normalizeRelativePath(entry.relativePath)).canonicalFile
            requireInside(root, target)
            target.parentFile?.mkdirs()
            source.copyTo(target, overwrite = true)
            target
        }
    }
}
```

- [ ] **Шаг 6: Запустить тесты и убедиться, что они проходят**

```bash
./gradlew :core:testDebugUnitTest --tests "com.client.xvideos.common.p2p.*"
```

Ожидается: PASS. Существующие `P2pBundleInstallerTest`,
`StoreBundleImporterTest`, `LCollectionBundleImporterTest` тоже зелёные —
`storeRoot` в них уже канонический, а `canonicalFile` в тесте сравнивается
через `canonicalPath`.

- [ ] **Шаг 7: Коммит**

```bash
git add core/src/main/java/com/client/xvideos/common/p2p core/src/test/java/com/client/xvideos/common/p2p
git commit -m "fix(p2p): путь из чужого манифеста больше не выводит запись за пределы store"
```

---

### Задача 3: Валидация имени коллекции (S2)

Имя коллекции вводит пользователь, и оно становится именем папки. `..` или `/`
уводят `mkdirs`, `renameTo` и — что хуже всего — `deleteRecursively` за пределы
корня коллекций. UI фильтрует только пустую строку.

Валидация ставится в доменные методы, а не в диалоги: `createCollection` и
`renameCollection` уже умеют показывать `SnackBar.error`, поэтому UI не
меняется вовсе.

**Файлы:**
- Создать: `core/src/main/java/com/client/xvideos/common/collectionDB/CollectionName.kt`
- Создать: `core/src/test/java/com/client/xvideos/common/collectionDB/CollectionNameTest.kt`
- Изменить: `core/src/main/java/com/client/xvideos/common/collectionDB/CollectionDB.kt`
- Изменить: `feature-l/src/main/java/com/client/xvideos/l/featured/saved/SavedL_Collection.kt:84-150`

- [ ] **Шаг 1: Написать падающий тест**

Создать
`core/src/test/java/com/client/xvideos/common/collectionDB/CollectionNameTest.kt`:

```kotlin
package com.client.xvideos.common.collectionDB

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CollectionNameTest {

    @Test
    fun `обычное имя проходит и обрезается по краям`() {
        assertEquals("Мои гифки", CollectionName.normalizeOrNull("  Мои гифки  "))
        assertEquals("cats_2024", CollectionName.normalizeOrNull("cats_2024"))
    }

    @Test
    fun `пустое имя отвергается`() {
        assertEquals(null, CollectionName.normalizeOrNull(""))
        assertEquals(null, CollectionName.normalizeOrNull("   "))
    }

    @Test
    fun `разделители пути отвергаются`() {
        assertEquals(null, CollectionName.normalizeOrNull("a/b"))
        assertEquals(null, CollectionName.normalizeOrNull("a\\b"))
        assertEquals(null, CollectionName.normalizeOrNull("C:name"))
    }

    @Test
    fun `точечные имена отвергаются`() {
        assertEquals(null, CollectionName.normalizeOrNull("."))
        assertEquals(null, CollectionName.normalizeOrNull(".."))
    }

    @Test
    fun `имя с ведущей точкой отвергается`() {
        // .xlr_old_ — служебный префикс XlrBackupManager: коллекция с таким
        // именем была бы стёрта восстановлением бэкапа.
        assertEquals(null, CollectionName.normalizeOrNull(".xlr_old_L"))
        assertEquals(null, CollectionName.normalizeOrNull(".hidden"))
    }

    @Test
    fun `isValid согласован с normalizeOrNull`() {
        assertTrue(CollectionName.isValid("нормальное"))
        assertFalse(CollectionName.isValid("../побег"))
    }
}
```

- [ ] **Шаг 2: Запустить тест и убедиться, что он падает**

```bash
./gradlew :core:testDebugUnitTest --tests "com.client.xvideos.common.collectionDB.CollectionNameTest"
```

Ожидается: ошибка компиляции — `Unresolved reference: CollectionName`.

- [ ] **Шаг 3: Написать `CollectionName.kt`**

```kotlin
package com.client.xvideos.common.collectionDB

/**
 * Имя коллекции — это имя папки на диске, поэтому проверять его обязательно.
 *
 * Раньше не проверялось нигде: UI отсекал только пустую строку, а дальше имя
 * шло прямо в `File(root, name)`. Имя `..` уводило `deleteRecursively()` на
 * родительский каталог, то есть на весь раздел.
 *
 * Проверка отвергает, а не «чинит» имя: коллекция видна пользователю в списке,
 * и тихая подмена `a/b` на `a_b` расходится с тем, что он ввёл, а для
 * переименования существующей коллекции ещё и промахнётся мимо папки.
 */
object CollectionName {

    /**
     * Ведущая точка запрещена: такие папки скрыты в списках, и с них же
     * начинается служебный префикс `.xlr_old_` отодвинутых копий в
     * [com.client.xvideos.common.backup.XlrBackupManager] — коллекция с таким
     * именем была бы стёрта восстановлением бэкапа.
     */
    private const val HIDDEN_PREFIX = '.'

    /**
     * Возвращает имя, обрезанное по краям, или `null`, если оно непригодно как
     * имя папки.
     */
    fun normalizeOrNull(raw: String): String? {
        val name = raw.trim()
        if (name.isBlank()) return null
        if (name.startsWith(HIDDEN_PREFIX)) return null
        if (name.any { it == '/' || it == '\\' || it == ':' }) return null
        return name
    }

    fun isValid(raw: String): Boolean = normalizeOrNull(raw) != null
}
```

- [ ] **Шаг 4: Запустить тест и убедиться, что он проходит**

```bash
./gradlew :core:testDebugUnitTest --tests "com.client.xvideos.common.collectionDB.CollectionNameTest"
```

Ожидается: PASS, 6 тестов.

- [ ] **Шаг 5: Подключить валидацию в `CollectionDB`**

В `core/src/main/java/com/client/xvideos/common/collectionDB/CollectionDB.kt`
заменить тело `create` (`:14-28`):

```kotlin
    fun create(collectionName: String): Result<Boolean> {
        return try {
            val safeName = CollectionName.normalizeOrNull(collectionName)
                ?: return Result.failure(IOException("Недопустимое имя коллекции: $collectionName"))
            Timber.i("!!! Создать коллекцию collectionCreateToDisk() collectionName:$safeName")
            val dir = File(path, safeName)
            if (!dir.exists()) {
                val created = dir.mkdirs()
                if (!created) { return Result.failure(IOException("Не удалось создать директорию: ${dir.absolutePath}")) }
            }
            Result.success(true)
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при создании коллекции $collectionName")
            Result.failure(e)
        }
    }
```

Заменить начало `deleteCollection` (`:31-38`):

```kotlin
    fun deleteCollection(collectionName: String): Result<Boolean> =
        runCatching {
            val safeName = CollectionName.normalizeOrNull(collectionName)
                ?: throw IOException("Недопустимое имя коллекции: $collectionName")
            val dir = File(path, safeName)

            if (!dir.exists()) {
                Timber.w("Коллекция \"$safeName\" не найдена: ${dir.absolutePath}")
                return Result.success(false)      // ничего не удаляли
            }
```

(остаток метода — без изменений).

Заменить начало `renameCollection` (`:52-62`):

```kotlin
    fun renameCollection(oldName: String, newName: String): Result<Boolean> =
        runCatching {
            val trimmed = CollectionName.normalizeOrNull(newName)
                ?: throw IOException("Недопустимое имя коллекции: $newName")
            val safeOldName = CollectionName.normalizeOrNull(oldName)
                ?: throw IOException("Недопустимое имя коллекции: $oldName")
            if (safeOldName == trimmed) {
                return Result.success(true)
            }
            val oldDir = File(path, safeOldName)
            val newDir = File(path, trimmed)
```

(остаток метода — без изменений; прежняя проверка `trimmed.isBlank()` теперь
покрыта `normalizeOrNull`).

Заменить начало `insert` (`:109-119`) и `deleteItem` (`:80-88`) — в обоих
`collectionName` идёт в путь:

```kotlin
    fun deleteItem(itemId: String, collectionName: String): Result<Boolean> {
        return try {
            val safeName = CollectionName.normalizeOrNull(collectionName)
                ?: return Result.failure(IOException("Недопустимое имя коллекции: $collectionName"))
            Timber.i("!!! удалить лайк GIFS -> deleteItem() id:$itemId из коллекции:$safeName")

            val dir = File(path, safeName)
            val likesFile = File(dir, "$itemId.collection")
```

```kotlin
    fun insert(name: String, collectionName: String, item: T): Result<Boolean> {
        return try {
            val safeName = CollectionName.normalizeOrNull(collectionName)
                ?: return Result.failure(IOException("Недопустимое имя коллекции: $collectionName"))
            Timber.i("!!! сохранить лайк GIFS -> likesItem() name:${name}")

            val dir = File(path, safeName)
```

(остаток обоих методов — без изменений; обратите внимание, что в `insert`
конкатенация `File("$path/$collectionName")` заменена на двухаргументный
конструктор `File(path, safeName)`).

- [ ] **Шаг 6: Подключить валидацию в `SavedL_Collection`**

В `feature-l/src/main/java/com/client/xvideos/l/featured/saved/SavedL_Collection.kt`
добавить импорт:

```kotlin
import com.client.xvideos.common.collectionDB.CollectionName
```

Заменить `createCollection` (`:84-94`):

```kotlin
    fun createCollection(collectionName: String) {
        Timber.i("SavedL_Collection createCollection() collectionName:$collectionName")
        val safeName = CollectionName.normalizeOrNull(collectionName)
        if (safeName == null) {
            SnackBar.error("Недопустимое название коллекции")
            return
        }
        val collectionRoot = File(AppPath.l_collection, safeName)
        if (collectionRoot.exists()) {
            SnackBar.error("Коллекция уже существует")
            return
        }
        collectionRoot.mkdirs()
        SnackBar.success("Коллекция $safeName создана")
        refreshCollectionList()
    }
```

Заменить начало `deleteCollection` (`:96-100`):

```kotlin
    fun deleteCollection(collectionName: String) {
        Timber.i("SavedL_Collection deleteCollection() collectionName:$collectionName")
        // Имя приходит из списка на экране, но список строится по содержимому
        // каталога: папка с «плохим» именем, созданная старой сборкой или
        // приехавшая по P2P, дала бы deleteRecursively() за пределами корня.
        val safeName = CollectionName.normalizeOrNull(collectionName)
        if (safeName == null) {
            SnackBar.error("Недопустимое название коллекции")
            return
        }
        // deleteRecursively по всей коллекции — это тысячи файлов, только на IO.
        scope.launch(Dispatchers.IO) {
            val deleted = File(AppPath.l_collection, safeName).deleteRecursively()
```

Дальше в теле корутины заменить все оставшиеся `collectionName` на `safeName`
(сравнение `currentCollectionName == collectionName`, текст снекбаров).

Заменить начало `renameCollection` (`:116-126`):

```kotlin
    fun renameCollection(oldName: String, newName: String): Boolean {
        Timber.i("SavedL_Collection renameCollection() oldName:$oldName newName:$newName")
        val trimmedNewName = CollectionName.normalizeOrNull(newName)
        if (trimmedNewName == null) {
            SnackBar.error("Недопустимое название коллекции")
            return false
        }
        val safeOldName = CollectionName.normalizeOrNull(oldName)
        if (safeOldName == null) {
            SnackBar.error("Недопустимое название коллекции")
            return false
        }
        if (safeOldName == trimmedNewName) {
            return true
        }

        val oldRoot = File(AppPath.l_collection, safeOldName)
        val newRoot = File(AppPath.l_collection, trimmedNewName)
```

Дальше в теле метода заменить оставшиеся `oldName` на `safeOldName`
(в том числе сравнение `currentCollectionName == oldName`).

- [ ] **Шаг 7: Проверить, что непроверенных вхождений не осталось**

```bash
grep -n "collectionName\|oldName" feature-l/src/main/java/com/client/xvideos/l/featured/saved/SavedL_Collection.kt
```

Внутри `createCollection`, `deleteCollection` и `renameCollection` исходные
`collectionName` / `oldName` должны остаться только в строке `Timber.i(...)` —
она логирует то, что пришло на вход, и это правильно. Любое другое вхождение
внутри этих трёх методов означает незаменённое использование: заменить на
`safeName` / `safeOldName`. Методы вне этой тройки (`setCollection`, `refresh`,
`addAll`, `refreshDuplicates`) задача не трогает — они получают имя из уже
провалидированного списка.

- [ ] **Шаг 8: Собрать оба модуля**

```bash
./gradlew :core:compileDebugKotlin :feature-l:compileDebugKotlin
```

Ожидается: BUILD SUCCESSFUL.

- [ ] **Шаг 9: Прогнать тесты**

```bash
./gradlew :core:testDebugUnitTest :feature-l:testDebugUnitTest
```

Ожидается: PASS.

- [ ] **Шаг 10: Коммит**

```bash
git add core/src/main/java/com/client/xvideos/common/collectionDB core/src/test/java/com/client/xvideos/common/collectionDB feature-l/src/main/java/com/client/xvideos/l/featured/saved/SavedL_Collection.kt
git commit -m "fix(core): имя коллекции проверяется перед тем, как стать путём на диске"
```

---

### Задача 4: Атомарная запись в `CollectionDB` (C1)

`insert` пишет JSON голым `writeText`. Обрыв процесса посреди записи оставляет
обрезанный файл, а `readAllCollections` молча выбрасывает его через
`mapNotNull` — элемент исчезает без единой ошибки.

**Файлы:**
- Изменить: `core/src/main/java/com/client/xvideos/common/collectionDB/CollectionDB.kt` (метод `insert`)
- Создать: `core/src/test/java/com/client/xvideos/common/collectionDB/CollectionDBTest.kt`

- [ ] **Шаг 1: Написать падающий тест**

Создать
`core/src/test/java/com/client/xvideos/common/collectionDB/CollectionDBTest.kt`:

```kotlin
package com.client.xvideos.common.collectionDB

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

data class TestItem(val id: String, val url: String)

class CollectionDBTest {

    @get:Rule val tmp = TemporaryFolder()

    private fun db(root: File) = CollectionDB(root.absolutePath, TestItem::class.java)

    @Test
    fun `insert пишет через временный файл и не оставляет мусора`() {
        val root = tmp.newFolder("collections")
        val db = db(root)

        assertTrue(db.insert("item1", "МояКоллекция", TestItem("item1", "https://x/1")).isSuccess)

        val dir = File(root, "МояКоллекция")
        val names = dir.listFiles()?.map { it.name }.orEmpty()
        assertEquals(listOf("item1.collection"), names)
    }

    @Test
    fun `insert перезаписывает существующий элемент целиком`() {
        val root = tmp.newFolder("collections2")
        val db = db(root)

        db.insert("item1", "К", TestItem("item1", "https://x/старый-очень-длинный-адрес"))
        db.insert("item1", "К", TestItem("item1", "https://x/1"))

        val items = db.readAllCollections().getOrThrow().single().items
        assertEquals(listOf(TestItem("item1", "https://x/1")), items)
    }

    @Test
    fun `обрезанный файл не ломает чтение остальных`() {
        val root = tmp.newFolder("collections3")
        val db = db(root)
        db.insert("ok", "К", TestItem("ok", "https://x/ok"))
        File(root, "К/broken.collection").writeText("{\"id\":\"bro")

        val items = db.readAllCollections().getOrThrow().single().items
        assertEquals(listOf(TestItem("ok", "https://x/ok")), items)
    }

    @Test
    fun `insert с недопустимым именем коллекции не создаёт папку`() {
        val root = tmp.newFolder("collections4")
        val db = db(root)

        assertTrue(db.insert("item1", "../побег", TestItem("item1", "https://x/1")).isFailure)
        assertEquals(0, root.listFiles()?.size ?: -1)
    }
}
```

- [ ] **Шаг 2: Запустить тест и убедиться, что первый падает**

```bash
./gradlew :core:testDebugUnitTest --tests "com.client.xvideos.common.collectionDB.CollectionDBTest"
```

Ожидается: FAIL в `insert пишет через временный файл и не оставляет мусора` —
после голого `writeText` файла `.tmp` не остаётся, но и гарантии атомарности
нет; тест начнёт проходить только после перехода на `writeTextAtomically`,
который создаёт и убирает `item1.collection.tmp`. Остальные три должны пройти
сразу (они фиксируют поведение, которое ломать нельзя).

> Если все четыре прошли на голом `writeText` — это ожидаемо: атомарность
> нельзя доказать unit-тестом, не убив процесс. Тест закрепляет отсутствие
> мусора и корректную перезапись; сама атомарность обеспечивается тем, что код
> вызывает общий `writeTextAtomically`, покрытый задачей 1. Переходите к шагу 3.

- [ ] **Шаг 3: Перевести `insert` на атомарную запись**

В `core/src/main/java/com/client/xvideos/common/collectionDB/CollectionDB.kt`
добавить импорт:

```kotlin
import com.client.xvideos.common.io.writeTextAtomically
```

и в `insert` заменить

```kotlin
            // Сохраняем URL как JSON в файл
            val gson = Gson()
            val json = gson.toJson(item)
            likesFile.writeText(json, Charsets.UTF_8)
```

на

```kotlin
            // Атомарно: обрыв процесса посреди writeText оставлял обрезанный
            // JSON, а readAllCollections молча выбрасывает такой файл через
            // mapNotNull — элемент пропадал без следа в логах.
            likesFile.writeTextAtomically(gson.toJson(item))
```

- [ ] **Шаг 4: Вынести `Gson` в поле класса**

В шапке класса `CollectionDB` (сразу после объявления, перед `fun create`)
добавить:

```kotlin
    // Gson потокобезопасен и дорог в конструировании: раньше экземпляр
    // создавался на каждый insert и на каждый файл в readAllCollections.
    private val gson = Gson()
```

и убрать локальные `val gson = Gson()` в `insert` и `Gson().fromJson<T>(...)`
в `readAllCollections` (заменить на `gson.fromJson<T>(text, type)`).

- [ ] **Шаг 5: Запустить тест и убедиться, что он проходит**

```bash
./gradlew :core:testDebugUnitTest --tests "com.client.xvideos.common.collectionDB.CollectionDBTest"
```

Ожидается: PASS, 4 теста.

- [ ] **Шаг 6: Коммит**

```bash
git add core/src/main/java/com/client/xvideos/common/collectionDB core/src/test/java/com/client/xvideos/common/collectionDB
git commit -m "fix(core): запись элемента коллекции стала атомарной"
```

---

### Задача 5: `FileDB` выполняет то, что обещает его KDoc (C2)

KDoc заявляет, что все публичные методы потокобезопасны и что два параллельных
`refresh()` не переплетаются. На деле `read()` не под локом вовсе, а публикация
результата `refresh()` стоит за пределами `synchronized` — устаревший результат
может лечь последним.

Публикация не заносится под основной лок намеренно: `list.replaceWith` берёт
снапшот-лок Compose, и вложение `lock → snapshotLock` дало бы обратный порядок
захвата относительно кода, который зовёт `FileDB` из-под снапшота. Вместо
этого публикация упорядочивается по номеру загрузки.

**Файлы:**
- Изменить: `core/src/main/java/com/client/xvideos/common/fileDB/FileDB.kt`
- Создать: `core/src/test/java/com/client/xvideos/common/fileDB/FileDBTest.kt`

- [ ] **Шаг 1: Написать падающий тест**

Создать `core/src/test/java/com/client/xvideos/common/fileDB/FileDBTest.kt`:

```kotlin
package com.client.xvideos.common.fileDB

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

data class Row(val id: String, val value: String)

class FileDBTest {

    @get:Rule val tmp = TemporaryFolder()

    private fun db(root: File) = FileDB(root.absolutePath, "row", Row::class.java)

    @Test
    fun `insert и read возвращают записанное`() {
        val root = tmp.newFolder("db")
        val db = db(root)

        assertTrue(db.insert("a", Row("a", "one")).isSuccess)
        assertEquals(Row("a", "one"), db.read("a").getOrThrow())
    }

    @Test
    fun `refresh наполняет список только валидными записями`() {
        val root = tmp.newFolder("db2")
        val db = db(root)
        db.insert("a", Row("a", "one"))
        db.insert("b", Row("b", "two"))
        File(root, "broken.row").writeText("{\"id\":\"bro")

        assertTrue(db.refresh().isSuccess)
        assertEquals(setOf("a", "b"), db.list.map { it.id }.toSet())
    }

    @Test
    fun `параллельные update и read не наблюдают пропажу файла`() {
        val root = tmp.newFolder("db3")
        val db = db(root)
        db.insert("a", Row("a", "start"))

        val start = CountDownLatch(1)
        val failures = java.util.Collections.synchronizedList(mutableListOf<String>())

        val writer = Thread {
            start.await()
            repeat(300) { i -> db.update("a", Row("a", "v$i")) }
        }
        val reader = Thread {
            start.await()
            repeat(300) {
                val result = db.read("a")
                if (result.isFailure) failures += result.exceptionOrNull()?.toString().orEmpty()
            }
        }

        writer.start(); reader.start(); start.countDown()
        writer.join(30_000); reader.join(30_000)

        assertEquals("read не должен видеть окно, в котором файла нет: $failures", 0, failures.size)
    }

    @Test
    fun `параллельные refresh не оставляют список в устаревшем состоянии`() {
        val root = tmp.newFolder("db4")
        val db = db(root)
        repeat(20) { i -> db.insert("k$i", Row("k$i", "v$i")) }

        val pool = java.util.concurrent.Executors.newFixedThreadPool(4)
        repeat(40) { pool.submit { db.refresh() } }
        pool.shutdown()
        assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS))

        assertEquals(20, db.list.size)
    }
}
```

- [ ] **Шаг 2: Запустить тест и убедиться, что он падает**

```bash
./gradlew :core:testDebugUnitTest --tests "com.client.xvideos.common.fileDB.FileDBTest"
```

Ожидается: FAIL в `параллельные update и read не наблюдают пропажу файла` —
`read()` попадает в окно между `delete()` и `renameTo()`. Тест на `refresh`
может проходить или падать нестабильно; после правки он станет
детерминированным.

- [ ] **Шаг 3: Внести правки в `FileDB`**

В `core/src/main/java/com/client/xvideos/common/fileDB/FileDB.kt` добавить
импорт:

```kotlin
import java.util.concurrent.atomic.AtomicLong
```

Рядом с объявлением `lock` (`:24-25`) добавить поля упорядочивания публикации:

```kotlin
    /**
     * Номер загрузки и последний опубликованный номер.
     *
     * Публикация в [list] стоит за пределами [lock] намеренно: `replaceWith`
     * берёт снапшот-лок Compose, и захват `lock -> snapshotLock` встретился бы
     * с обратным порядком у кода, который зовёт FileDB из-под снапшота.
     * Порядок вместо этого восстанавливается по номеру: результат более старой
     * загрузки не может лечь поверх более новой.
     */
    private val loadSeq = AtomicLong(0)
    private val publishLock = Any()
    private var publishedSeq = 0L
```

Заменить тело `read` (`:92-106`), обернув работу с файлом в лок:

```kotlin
    fun read(nameFile: String): Result<T> {
        return try {
            synchronized(lock) {
                val file = File(dirPath, "$nameFile.$extension")
                if (!file.exists()) {
                    return Result.failure(FileNotFoundException("!!! Файл не найден: ${file.absolutePath}"))
                }
                val json = file.readText(Charsets.UTF_8)
                val obj = gson.fromJson(json, clazz)
                    ?: return Result.failure(NullPointerException("!!! Десериализация вернула null"))
                Result.success(obj)
            }
        } catch (e: Exception) {
            Timber.e(e, "!!! Ошибка при чтении файла $nameFile")
            Result.failure(e)
        }
    }
```

Заменить `refresh` (`:108-138`):

```kotlin
    fun refresh(): Result<Boolean> {
        return try {
            // Номер берётся под тем же локом, что и чтение каталога, поэтому
            // порядок номеров совпадает с порядком загрузок. Пара, а не
            // присваивание внешней val изнутри лямбды: так не приходится
            // полагаться на definite-assignment сквозь inline-функцию.
            val (seq, loaded) = synchronized(lock) {
                val dir = File(dirPath)
                if (!dir.exists() || !dir.isDirectory) {
                    return Result.failure(IOException("!!! Директория не существует: $dirPath"))
                }

                cleanupTempFiles(dir)

                val files = dir.listFiles { file -> file.extension == extension } ?: emptyArray()

                loadSeq.incrementAndGet() to files.mapNotNull { file ->
                    try {
                        val json = file.readText(Charsets.UTF_8)
                        gson.fromJson(json, clazz)
                    } catch (e: Exception) {
                        Timber.e(e, "!!! FileDB refresh Ошибка при чтении файла $dirPath ${file.name}")
                        null
                    }
                }
            }

            synchronized(publishLock) {
                if (seq > publishedSeq) {
                    publishedSeq = seq
                    list.replaceWith(loaded)
                }
            }

            Result.success(true)
        } catch (e: Exception) {
            Timber.e(e, "!!! Ошибка при обновлении списка из директории $dirPath")
            Result.failure(e)
        }
    }
```

Обновить KDoc класса (`:11-17`), чтобы он описывал фактический контракт:

```kotlin
/**
 * val nichesDb = FileDB<NichesInfo>(AppPath.niches_red, "niches", object : TypeToken<NichesInfo>() {}.type)
 *
 * Все публичные методы синхронные и потокобезопасны: операции с каталогом
 * сериализованы через [lock], а запись файлов атомарна (temp + rename), чтобы
 * обрыв процесса посреди записи не оставлял обрезанный JSON. Публикация
 * результата [refresh] в [list] идёт вне [lock], но упорядочена по номеру
 * загрузки — устаревший результат не ляжет поверх свежего.
 */
```

- [ ] **Шаг 4: Запустить тест и убедиться, что он проходит**

```bash
./gradlew :core:testDebugUnitTest --tests "com.client.xvideos.common.fileDB.FileDBTest"
```

Ожидается: PASS, 4 теста.

- [ ] **Шаг 5: Прогнать весь модуль — `FileDB` используют многие**

```bash
./gradlew :core:testDebugUnitTest
```

Ожидается: PASS.

- [ ] **Шаг 6: Коммит**

```bash
git add core/src/main/java/com/client/xvideos/common/fileDB/FileDB.kt core/src/test/java/com/client/xvideos/common/fileDB/FileDBTest.kt
git commit -m "fix(core): read под локом, публикация refresh упорядочена по номеру загрузки"
```

---

### Задача 6: Мелочи `XlrBackupManager` (C3, C4, C5)

Три независимые правки в одном файле: неправильный вызов `setLevel`, склейка
манифеста строками, неполный откат восстановления.

**Файлы:**
- Изменить: `core/src/main/java/com/client/xvideos/common/backup/XlrBackupManager.kt:139`, `:240-258`, `:194-238`
- Тест: `core/src/test/java/com/client/xvideos/common/backup/XlrRestoreApplyTest.kt`

- [ ] **Шаг 1: Убрать неправильный `setLevel` (C3)**

В `createBackup` удалить строку `:139`:

```kotlin
                zip.setLevel(ZipOutputStream.DEFLATED)
```

`ZipOutputStream.DEFLATED` — константа *метода* сжатия (8), а `setLevel` ждёт
уровень `Deflater` из диапазона 0..9. Совпадение диапазонов маскировало
ошибку: вместо «включить deflate» выставлялся уровень 8. Deflate и так метод
по умолчанию, уровень по умолчанию (6) для бэкапа подходит — строка просто
лишняя.

- [ ] **Шаг 2: Собрать манифест через Gson (C4)**

Заменить `writeManifest` (`:240-258`) целиком:

```kotlin
    private fun writeManifest(zip: ZipOutputStream, selectedPaths: List<String>, options: XlrBackupOptions) {
        // Раньше JSON склеивался строками: путь с кавычкой или бэкслешем давал
        // невалидный манифест. Сейчас его никто не парсит, но schemaVersion в
        // файле означает, что собираются — и тогда сломались бы старые архивы.
        val manifest = mapOf(
            "schemaVersion" to SCHEMA_VERSION,
            "createdAt" to utcNowText(),
            "sections" to sections,
            "paths" to selectedPaths,
            "modes" to mapOf(
                "L" to options.lMode.name,
                "R" to options.rMode.name,
            ),
        )
        zip.putNextEntry(ZipEntry(MANIFEST_ENTRY))
        zip.write(GsonBuilder().setPrettyPrinting().create().toJson(manifest).toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }
```

и добавить импорт в шапку файла:

```kotlin
import com.google.gson.GsonBuilder
```

- [ ] **Шаг 3: Написать падающий тест на неполный откат (C5)**

В конец класса `XlrRestoreApplyTest` добавить:

```kotlin
    @Test
    fun `откат убирает папку, которой до восстановления не существовало`() {
        val main = tmp.newFolder("main")
        val temp = tmp.newFolder("temp")

        // X появляется только из бэкапа: отодвигать нечего, значит в movedAside
        // записи не будет и очистить target при откате может только `written`.
        temp.writeFile("X/новое.txt", "новое")

        // Второй путь ведёт за пределы корня — падение случится после того, как
        // X уже перенесён.
        runCatching { XlrBackupManager.applyRestoredPaths(main, temp, listOf("X", "../снаружи")) }
            .onSuccess { error("ожидалось падение на пути за пределами корня") }

        assertFalse(
            "папка, созданная только этим восстановлением, должна быть убрана",
            File(main, "X").exists()
        )
    }
```

- [ ] **Шаг 4: Запустить тест и посмотреть на результат**

```bash
./gradlew :core:testDebugUnitTest --tests "com.client.xvideos.common.backup.XlrRestoreApplyTest"
```

Ожидается: новый тест **проходит** — при успешном переносе `written += target`
успевает выполниться. Он фиксирует поведение, которое правка шага 5 не должна
сломать. Дыра из ревью уже́е: она открывается, только если падение случилось
*внутри* `copyRecursively` конкретного пути, когда `target` ещё не попал в
`written`. Такой сценарий детерминированно не воспроизводится unit-тестом на
`File`, поэтому правка страхуется существующими тестами отката, а не новым.

- [ ] **Шаг 5: Заносить цель в `written` до записи (C5)**

В `applyRestoredPaths` заменить блок `:214-224`:

```kotlin
                target.parentFile?.mkdirs()
                // В список очистки — до записи, а не после: падение внутри
                // copyRecursively оставляло полузаписанную папку, которой нет
                // ни в written, ни в movedAside, и откат её не трогал.
                written += target
                val restored = File(tempRoot, path)
                if (restored.exists()) {
                    if (!restored.renameTo(target)) {
                        restored.copyRecursively(target, overwrite = true)
                    }
                } else {
                    target.mkdirs()
                }
            }
```

(строка `written += target` перед `val restored`, прежняя строка `written +=
target` после блока `if/else` удаляется).

- [ ] **Шаг 6: Прогнать тесты бэкапа**

```bash
./gradlew :core:testDebugUnitTest --tests "com.client.xvideos.common.backup.*"
```

Ожидается: PASS, 5 тестов.

- [ ] **Шаг 7: Проверить создание бэкапа вручную**

Собрать debug и на устройстве: Настройки → Бэкап → создать архив → открыть
получившийся `.zip` на компьютере и убедиться, что `backup.json` — валидный
JSON и содержит `schemaVersion`, `paths`, `modes`.

```bash
./gradlew :app:assembleDebug
```

- [ ] **Шаг 8: Коммит**

```bash
git add core/src/main/java/com/client/xvideos/common/backup/XlrBackupManager.kt core/src/test/java/com/client/xvideos/common/backup/XlrRestoreApplyTest.kt
git commit -m "fix(core): манифест бэкапа через Gson, полный откат восстановления, лишний setLevel"
```

---

### Задача 7: Троттлинг код-доступа не обходится переводом часов (S3)

Блокировка ввода считается по `System.currentTimeMillis()`. Перевод системного
времени вперёд снимает её мгновенно, без root. При четырёхзначном коде
экспоненциальный backoff — единственная реальная защита.

Решение: хранить обе отметки — настенную и монотонную (`elapsedRealtime`).
Блокировка активна, пока **любая** из них не истекла. Перевод часов не двигает
`elapsedRealtime`; перезагрузка обнуляет `elapsedRealtime`, но настенная
отметка переживает её.

Арифметика выносится в чистый объект: `AppLockRepository` тянет `Context`,
`SharedPreferences` и `android.util.Base64`, которых в JVM-тестах нет.

**Файлы:**
- Создать: `core/src/main/java/com/client/xvideos/common/applock/AppLockThrottle.kt`
- Создать: `core/src/test/java/com/client/xvideos/common/applock/AppLockThrottleTest.kt`
- Изменить: `core/src/main/java/com/client/xvideos/common/applock/AppLockRepository.kt:25-35,89-124`

- [ ] **Шаг 1: Написать падающий тест**

Создать
`core/src/test/java/com/client/xvideos/common/applock/AppLockThrottleTest.kt`:

```kotlin
package com.client.xvideos.common.applock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLockThrottleTest {

    @Test
    fun `первые попытки не блокируют`() {
        repeat(AppLockThrottle.FREE_ATTEMPTS) { i ->
            val state = AppLockThrottle.onFailedAttempt(
                attempts = i,
                wallNow = 1_000L,
                elapsedNow = 500L,
            )
            assertEquals(0L, state.lockoutUntilWall)
            assertEquals(0L, state.lockoutUntilElapsed)
        }
    }

    @Test
    fun `первая блокировка длится базовые тридцать секунд`() {
        val state = AppLockThrottle.onFailedAttempt(
            attempts = AppLockThrottle.FREE_ATTEMPTS,
            wallNow = 1_000L,
            elapsedNow = 500L,
        )
        assertEquals(1_000L + 30_000L, state.lockoutUntilWall)
        assertEquals(500L + 30_000L, state.lockoutUntilElapsed)
        assertEquals(AppLockThrottle.FREE_ATTEMPTS + 1, state.attempts)
    }

    @Test
    fun `задержка удваивается и упирается в потолок`() {
        val second = AppLockThrottle.onFailedAttempt(AppLockThrottle.FREE_ATTEMPTS + 1, 0L, 0L)
        assertEquals(60_000L, second.lockoutUntilWall)

        val far = AppLockThrottle.onFailedAttempt(AppLockThrottle.FREE_ATTEMPTS + 40, 0L, 0L)
        assertEquals(30 * 60_000L, far.lockoutUntilWall)
    }

    @Test
    fun `перевод часов вперёд не снимает блокировку`() {
        val state = AppLockThrottle.onFailedAttempt(AppLockThrottle.FREE_ATTEMPTS, 1_000L, 500L)

        // Пользователь перевёл системное время на сутки вперёд, монотонные часы
        // при этом не сдвинулись.
        val remaining = AppLockThrottle.remainingMillis(
            state = state,
            wallNow = 1_000L + 86_400_000L,
            elapsedNow = 600L,
        )
        assertTrue("блокировка обязана держаться на монотонных часах", remaining > 0L)
    }

    @Test
    fun `перевод часов назад не продлевает блокировку сверх монотонного срока`() {
        val state = AppLockThrottle.onFailedAttempt(AppLockThrottle.FREE_ATTEMPTS, 1_000L, 500L)

        val remaining = AppLockThrottle.remainingMillis(
            state = state,
            wallNow = 0L,
            elapsedNow = 500L + 30_000L,
        )
        // Настенные часы ушли назад — по ним срок ещё не истёк, поэтому лок
        // держится. Это осознанный выбор: ошибаемся в сторону блокировки.
        assertTrue(remaining > 0L)
    }

    @Test
    fun `после истечения обоих сроков блокировки нет`() {
        val state = AppLockThrottle.onFailedAttempt(AppLockThrottle.FREE_ATTEMPTS, 1_000L, 500L)

        val remaining = AppLockThrottle.remainingMillis(
            state = state,
            wallNow = 1_000L + 30_001L,
            elapsedNow = 500L + 30_001L,
        )
        assertEquals(0L, remaining)
    }

    @Test
    fun `перезагрузка обнуляет монотонные часы но настенный срок держит`() {
        val state = AppLockThrottle.onFailedAttempt(AppLockThrottle.FREE_ATTEMPTS + 5, 1_000L, 900_000L)

        // После перезагрузки elapsedRealtime начинается с нуля и меньше
        // сохранённого срока — по нему лок считался бы активным вечно, поэтому
        // такой случай обязан отбрасываться, а срок брать с настенных часов.
        val remaining = AppLockThrottle.remainingMillis(
            state = state,
            wallNow = 1_000L + 100L,
            elapsedNow = 42L,
        )
        assertTrue(remaining > 0L)
        assertTrue("после перезагрузки срок ограничен настенными часами", remaining <= 30 * 60_000L)
    }
}
```

- [ ] **Шаг 2: Запустить тест и убедиться, что он падает**

```bash
./gradlew :core:testDebugUnitTest --tests "com.client.xvideos.common.applock.AppLockThrottleTest"
```

Ожидается: ошибка компиляции — `Unresolved reference: AppLockThrottle`.

- [ ] **Шаг 3: Написать `AppLockThrottle.kt`**

```kotlin
package com.client.xvideos.common.applock

/**
 * Арифметика блокировки ввода код-доступа: чистые функции, без `Context`.
 *
 * Вынесено из [AppLockRepository] по двум причинам. Первая — тестируемость:
 * репозиторий тянет `SharedPreferences` и `android.util.Base64`, которых в
 * JVM-тестах нет, а проверять надо именно арифметику. Вторая — сам дефект:
 * раньше срок считался только по `System.currentTimeMillis()`, и перевод
 * системного времени вперёд снимал блокировку мгновенно, без root. При
 * четырёхзначном коде backoff — единственная реальная защита от перебора.
 *
 * Поэтому срок хранится дважды: настенными часами (переживают перезагрузку) и
 * монотонными (`SystemClock.elapsedRealtime`, не поддаются переводу времени).
 * Блокировка держится, пока не истёк хотя бы один из них.
 */
object AppLockThrottle {

    /** Сколько попыток без задержки до начала блокировки. */
    const val FREE_ATTEMPTS = 4

    /** Базовая длительность блокировки; далее удваивается на каждую ошибку. */
    const val BASE_LOCKOUT_MS = 30_000L
    const val MAX_LOCKOUT_MS = 30 * 60_000L // 30 минут
    private const val MAX_BACKOFF_SHIFT = 16

    /**
     * @param attempts общее число неудачных попыток, включая последнюю.
     * @param lockoutUntilWall срок по настенным часам (epoch millis), 0 — блокировки нет.
     * @param lockoutUntilElapsed срок по монотонным часам, 0 — блокировки нет.
     */
    data class State(
        val attempts: Int,
        val lockoutUntilWall: Long,
        val lockoutUntilElapsed: Long,
    )

    /**
     * Регистрирует неудачную попытку.
     *
     * @param attempts сколько ошибок было ДО этой.
     */
    fun onFailedAttempt(attempts: Int, wallNow: Long, elapsedNow: Long): State {
        val total = attempts + 1
        if (total <= FREE_ATTEMPTS) {
            return State(attempts = total, lockoutUntilWall = 0L, lockoutUntilElapsed = 0L)
        }
        val shift = (total - FREE_ATTEMPTS - 1).coerceIn(0, MAX_BACKOFF_SHIFT)
        val duration = (BASE_LOCKOUT_MS shl shift).coerceAtMost(MAX_LOCKOUT_MS)
        return State(
            attempts = total,
            lockoutUntilWall = wallNow + duration,
            lockoutUntilElapsed = elapsedNow + duration,
        )
    }

    /**
     * Сколько миллисекунд осталось до конца блокировки (0 — ввод разрешён).
     *
     * Берётся максимум из двух остатков. Монотонный остаток отбрасывается,
     * если он больше максимально возможной блокировки: так выглядит
     * перезагрузка, после которой `elapsedRealtime` начинается заново и
     * сохранённый срок оказывается «в будущем» навсегда.
     */
    fun remainingMillis(state: State, wallNow: Long, elapsedNow: Long): Long {
        val byWall = (state.lockoutUntilWall - wallNow).coerceAtLeast(0L)
        val byElapsedRaw = (state.lockoutUntilElapsed - elapsedNow).coerceAtLeast(0L)
        val byElapsed = if (byElapsedRaw > MAX_LOCKOUT_MS) 0L else byElapsedRaw
        return maxOf(byWall, byElapsed)
    }
}
```

- [ ] **Шаг 4: Запустить тест и убедиться, что он проходит**

```bash
./gradlew :core:testDebugUnitTest --tests "com.client.xvideos.common.applock.AppLockThrottleTest"
```

Ожидается: PASS, 7 тестов.

- [ ] **Шаг 5: Перевести `AppLockRepository` на `AppLockThrottle`**

В `core/src/main/java/com/client/xvideos/common/applock/AppLockRepository.kt`
добавить импорт:

```kotlin
import android.os.SystemClock
```

Удалить константы `:29-35` (`FREE_ATTEMPTS`, `BASE_LOCKOUT_MS`,
`MAX_LOCKOUT_MS`, `MAX_BACKOFF_SHIFT`) — они переехали в `AppLockThrottle`.
Рядом с `KEY_LOCKOUT_UNTIL` (`:27`) добавить ключ монотонного срока:

```kotlin
    private const val KEY_LOCKOUT_UNTIL_ELAPSED = "app_lock_lockout_until_elapsed"
```

Заменить `lockoutRemainingMillis` (`:89-93`):

```kotlin
    fun lockoutRemainingMillis(context: Context): Long {
        val prefs = context.applicationContext.defaultSharedPreferences()
        val state = AppLockThrottle.State(
            attempts = prefs.getInt(KEY_FAILED_ATTEMPTS, 0),
            lockoutUntilWall = prefs.getLong(KEY_LOCKOUT_UNTIL, 0L),
            lockoutUntilElapsed = prefs.getLong(KEY_LOCKOUT_UNTIL_ELAPSED, 0L),
        )
        return AppLockThrottle.remainingMillis(
            state = state,
            wallNow = System.currentTimeMillis(),
            elapsedNow = SystemClock.elapsedRealtime(),
        )
    }
```

Заменить `registerFailedAttempt` (`:100-115`):

```kotlin
    fun registerFailedAttempt(context: Context): Long {
        val prefs = context.applicationContext.defaultSharedPreferences()
        val state = AppLockThrottle.onFailedAttempt(
            attempts = prefs.getInt(KEY_FAILED_ATTEMPTS, 0),
            wallNow = System.currentTimeMillis(),
            elapsedNow = SystemClock.elapsedRealtime(),
        )
        prefs.edit {
            putInt(KEY_FAILED_ATTEMPTS, state.attempts)
            putLong(KEY_LOCKOUT_UNTIL, state.lockoutUntilWall)
            putLong(KEY_LOCKOUT_UNTIL_ELAPSED, state.lockoutUntilElapsed)
        }
        return state.lockoutUntilWall
    }
```

Дописать удаление нового ключа в `resetFailedAttempts` (`:118-124`):

```kotlin
        prefs.edit {
            remove(KEY_FAILED_ATTEMPTS)
            remove(KEY_LOCKOUT_UNTIL)
            remove(KEY_LOCKOUT_UNTIL_ELAPSED)
        }
```

Обновить KDoc `lockoutRemainingMillis` (`:85-88`), заменив его на:

```kotlin
    /**
     * Сколько миллисекунд осталось до конца блокировки ввода (0 — ввод разрешён).
     * Срок хранится и настенными, и монотонными часами — см. [AppLockThrottle]:
     * перезапуск приложения задержку не сбрасывает, перевод системного времени
     * её не снимает.
     */
```

- [ ] **Шаг 6: Собрать модуль**

```bash
./gradlew :core:compileDebugKotlin :core:testDebugUnitTest
```

Ожидается: BUILD SUCCESSFUL, тесты PASS.

- [ ] **Шаг 7: Проверить на устройстве**

Собрать debug, включить код-доступ, ввести неверный код 5 раз подряд —
появится задержка. Не закрывая экран замка, перевести системное время на сутки
вперёд и вернуться в приложение: задержка обязана остаться. Затем дождаться её
естественного истечения и убедиться, что ввод разблокировался.

```bash
./gradlew :app:assembleDebug
```

- [ ] **Шаг 8: Коммит**

```bash
git add core/src/main/java/com/client/xvideos/common/applock core/src/test/java/com/client/xvideos/common/applock
git commit -m "fix(core): блокировка код-доступа держится на монотонных часах"
```

---

### Задача 8: `SecureCredentialStore` не теряет секреты на транзиентной ошибке (S4)

Сейчас любой провал `build()` ведёт к `deleteSharedPreferences`. Повреждённый
keyset пересоздать действительно нужно, но временно недоступный Keystore
(direct boot, отдельные прошивки) — не повод стирать сохранённый пароль.

Unit-теста у задачи нет и быть не может: `EncryptedSharedPreferences` и
`MasterKey` требуют настоящего Android Keystore, а Robolectric в проект не
подключён. Проверка — ручная, сценарий в шаге 3.

**Файлы:**
- Изменить: `core/src/main/java/com/client/xvideos/common/settings/SecureCredentialStore.kt:37-65`

- [ ] **Шаг 1: Разделить причины отказа**

Заменить `createOrNull` и `build` целиком:

```kotlin
    /**
     * Открывает зашифрованное хранилище.
     *
     * Возвращает `null`, если Keystore недоступен — так бывает в Compose Preview
     * (LayoutLib) и на отдельных прошивках. Вызывающий обязан пережить `null`:
     * лучше не сохранить секрет вообще, чем записать его открытым текстом.
     */
    fun createOrNull(context: Context): SharedPreferences? {
        val appContext = context.applicationContext
        build(appContext)?.let { return it }

        // Пересоздаём файл ТОЛЬКО если ключи действительно не подходят к нему.
        // Раньше пересоздание запускал любой провал build(), включая временную
        // недоступность Keystore (direct boot, часть прошивок) — и сохранённый
        // пароль стирался там, где достаточно было вернуть null и попробовать
        // позже.
        if (!lastFailureLooksLikeBrokenKeyset) {
            Timber.w("SecureCredentialStore: Keystore недоступен, $FILE_NAME оставлен как есть")
            return null
        }

        // Повреждённый keyset (сброс ключей Keystore, восстановление из бэкапа,
        // смена биометрии на части прошивок) расшифровать уже нечем — файл можно
        // только пересоздать. Пользователь потеряет сохранённый пароль и введёт
        // его заново; это лучше, чем неработающий вход в раздел L.
        Timber.w("SecureCredentialStore: keyset повреждён, пересоздаю $FILE_NAME")
        appContext.deleteSharedPreferences(FILE_NAME)
        return build(appContext)
    }

    /**
     * Признак того, что последний отказ [build] выглядит как несовпадение
     * ключей с файлом, а не как недоступность Keystore. Хранится полем, а не
     * возвращается, чтобы не менять сигнатуру `SharedPreferences?`.
     */
    @Volatile
    private var lastFailureLooksLikeBrokenKeyset = false

    private fun build(appContext: Context): SharedPreferences? = try {
        val masterKey = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            appContext,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        ).also { lastFailureLooksLikeBrokenKeyset = false }
    } catch (e: Exception) {
        // Tink бросает GeneralSecurityException/IOException, когда keyset не
        // разбирается или не расшифровывается имеющимся мастер-ключом. Всё
        // остальное (IllegalStateException, NoClassDefFoundError в Preview,
        // отказ Keystore) означает «сейчас нельзя», а не «файл испорчен».
        lastFailureLooksLikeBrokenKeyset =
            e is java.security.GeneralSecurityException || e is java.io.IOException
        Timber.e(e, "SecureCredentialStore: EncryptedSharedPreferences недоступны")
        null
    }
```

- [ ] **Шаг 2: Собрать модуль**

```bash
./gradlew :core:compileDebugKotlin :core:testDebugUnitTest
```

Ожидается: BUILD SUCCESSFUL, тесты PASS.

- [ ] **Шаг 3: Проверить на устройстве**

1. Собрать debug, зайти в раздел L, сохранить логин/пароль Luscious.
2. Перезапустить приложение — учётные данные подставляются.
3. Открыть Compose Preview любого экрана, использующего `Settings` — превью
   рендерится, файл `secure_credentials` на устройстве не тронут.
4. Проверить, что повреждённый keyset по-прежнему лечится: `adb shell run-as
   <applicationId> rm -f shared_prefs/secure_credentials.xml`, запустить
   приложение — оно не падает, просит ввести пароль заново.

```bash
./gradlew :app:assembleDebug
```

- [ ] **Шаг 4: Коммит**

```bash
git add core/src/main/java/com/client/xvideos/common/settings/SecureCredentialStore.kt
git commit -m "fix(core): недоступный Keystore больше не стирает сохранённые учётные данные"
```

---

### Задача 9: `onTerminate` не маскирует ошибку старта (T3)

`onTerminate` обращается к `lateinit networkTrafficMonitor`. Если `onCreate`
упал раньше строки, где монитор создаётся (например, на `AppPath.init`), в
логе окажется `UninitializedPropertyAccessException` вместо настоящей причины
падения.

Unit-теста нет: `Application.onTerminate` вызывается только средой Android, а
Robolectric в проект не подключён. Правка на одну строку и очевидна по чтению.

**Файлы:**
- Изменить: `app/src/main/java/com/client/xvideos/App.kt:194-197`

- [ ] **Шаг 1: Внести правку**

Заменить `onTerminate`:

```kotlin
    override fun onTerminate() {
        super.onTerminate()
        // Проверка инициализации, а не голое обращение: если onCreate упал до
        // создания монитора, здесь вылетал UninitializedPropertyAccessException
        // и затирал в логе настоящую причину падения.
        if (::networkTrafficMonitor.isInitialized) {
            networkTrafficMonitor.destroy()
        }
    }
```

- [ ] **Шаг 2: Собрать модуль**

```bash
./gradlew :app:compileDebugKotlin
```

Ожидается: BUILD SUCCESSFUL.

- [ ] **Шаг 3: Коммит**

```bash
git add app/src/main/java/com/client/xvideos/App.kt
git commit -m "fix(app): onTerminate не затирает причину падения при старте"
```

---

## Финальная проверка

- [ ] **Полный прогон тестов**

```bash
./gradlew test
```

Ожидается: PASS во всех модулях, включая сторожей архитектуры
(`LayerBoundariesTest`, `ModuleBoundariesTest`, `ScreenSerializationTest`).

- [ ] **Detekt**

```bash
./gradlew detekt
```

Ожидается: BUILD SUCCESSFUL. Новые файлы в baseline не входят, поэтому любые
их нарушения всплывут.

- [ ] **Сборка релиза**

```bash
./gradlew :app:assembleRelease
```

- [ ] **Отметить закрытые находки**

Дописать в `docs/CODE_REVIEW_2026-08-16.md` (если файл ревью будет создан)
статусы S1-S4, C1-C5 со ссылками на коммиты. Архитектурные находки A1-A4
остаются открытыми — под них нужны отдельные планы.
