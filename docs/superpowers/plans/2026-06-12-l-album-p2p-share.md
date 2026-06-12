# L Album P2P Share Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Кнопка «Share Album (P2P)» в ScreenLAlbum — передаёт файл метаданных `<id>.album` на другой телефон; у получателя альбом появляется в Saved Albums.

**Architecture:** Новый `P2pType.L_ALBUM` в протоколе. Отправка: `LAlbumExporter` берёт `<id>.album` из `/xvideos/L/Album` (сохранён) или пишет `AlbumDetails` Gson'ом в outbox-зеркало (не сохранён) — сети нет, фаза Preparing не нужна, сразу `Ready`. Приём: существующий конвейер (inbox-зеркало → merge → `FileDB.refresh`), только две ветки в `P2pReceiveManager`.

**Tech Stack:** Kotlin, Compose + Voyager, Nearby Connections, Gson (pretty printing — формат FileDB), JUnit4.

**Spec:** `docs/superpowers/specs/2026-06-12-l-album-p2p-share-design.md`

**Структура файлов:**

| Файл | Роль |
|---|---|
| Modify `app/src/main/java/com/client/xvideos/common/p2p/P2pType.kt` | + `L_ALBUM` |
| Modify `app/src/main/java/com/client/xvideos/common/p2p/P2pReceiveManager.kt:56-69` | ветки store/refresh для L_ALBUM |
| Modify `app/src/main/java/com/client/xvideos/common/p2p/export/Exporters.kt` | + `LAlbumExporter` |
| Create `app/src/main/java/com/client/xvideos/l/ui/screens/screenAlbum/atom/AlbumInfoButtonShareAlbum.kt` | кнопка в шапке |
| Modify `app/src/main/java/com/client/xvideos/l/ui/screens/screenAlbum/ScreenLAlbumSM.kt` | `shareAlbumP2p()` + state навигации |
| Modify `app/src/main/java/com/client/xvideos/l/ui/screens/screenAlbum/ScreenAlbum.kt` | кнопка + push ScreenP2pSend |
| Tests | `P2pManifestCodecTest`, `StoreBundleImporterTest`, `ExportersTest` |

Все команды — из корня репо, Windows: `.\gradlew.bat`.

---

### Task 1: Протокол — P2pType.L_ALBUM + ветки приёма (TDD)

Добавление значения в enum ломает exhaustive `when` в `P2pReceiveManager.storeRootFor` — правка веток входит в эту же задачу.

**Files:**
- Modify: `app/src/main/java/com/client/xvideos/common/p2p/P2pType.kt`
- Modify: `app/src/main/java/com/client/xvideos/common/p2p/P2pReceiveManager.kt:56-69`
- Test: `app/src/test/java/com/client/xvideos/common/p2p/P2pManifestCodecTest.kt`
- Test: `app/src/test/java/com/client/xvideos/common/p2p/imports/StoreBundleImporterTest.kt`

- [ ] **Step 1: Write the failing tests**

В `P2pManifestCodecTest.kt` добавить в конец класса:

```kotlin
    @Test
    fun `album manifest round trip`() {
        val m = P2pManifest(
            type = P2pType.L_ALBUM,
            metadataFileName = "42.album",
            files = listOf(P2pManifestFile("42.album", "42.album", 1L, 10L)),
        )
        assertEquals(m, P2pManifestCodec.fromBytes(P2pManifestCodec.toBytes(m)))
    }
```

В `StoreBundleImporterTest.kt` добавить в конец класса:

```kotlin
    @Test
    fun `L_ALBUM manifest lands in albums root and refreshes`() = runTest {
        val main = tmp.newFolder("xvideos")
        val albumsRoot = File(main, "L/Album").apply { mkdirs() }
        val inbox = File(main, "inbox").apply { mkdirs() }
        val received = tmp.newFile("p2").apply { writeText("{json}") }
        var refreshed: P2pType? = null

        val importer = StoreBundleImporter(
            storeRootFor = { type -> if (type == P2pType.L_ALBUM) albumsRoot else error("unexpected") },
            refreshFor = { refreshed = it },
            inboxRoot = inbox,
            mainRoot = main,
        )

        importer.import(
            P2pManifest(
                type = P2pType.L_ALBUM,
                metadataFileName = "42.album",
                files = listOf(P2pManifestFile("42.album", "42.album", 1L, 6L)),
            ),
            mapOf(1L to received),
        )

        assertEquals("{json}", File(albumsRoot, "42.album").readText())
        assertTrue(inbox.listFiles().isNullOrEmpty())
        assertEquals(P2pType.L_ALBUM, refreshed)
    }
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.client.xvideos.common.p2p.P2pManifestCodecTest" --tests "com.client.xvideos.common.p2p.imports.StoreBundleImporterTest"`
Expected: FAIL — compilation error, `L_ALBUM` не существует.

- [ ] **Step 3: Add enum value and receive branches**

`P2pType.kt` целиком:

```kotlin
package com.client.xvideos.common.p2p

/**
 * Источник item: xvideos, redgifs, luscious. Определяет store на приёмной стороне.
 * [L_ALBUM] — метаданные альбома L (`<id>.album`), контент получатель качает сам.
 */
enum class P2pType { X, R, L, L_ALBUM }
```

В `P2pReceiveManager.kt` заменить блок создания `storeImporter`:

```kotlin
        val storeImporter = StoreBundleImporter(
            storeRootFor = { type ->
                when (type) {
                    P2pType.X -> File(AppPath.x_cache_download)
                    // R сюда не попадает — идёт через RLikesBundleImporter.
                    P2pType.R -> File(AppPath.r_cache_download)
                    P2pType.L -> File(AppPath.l_likes)
                    P2pType.L_ALBUM -> File(AppPath.l_albums)
                }
            },
            refreshFor = { type ->
                // X: экран Saved перечитывает список при открытии.
                when (type) {
                    P2pType.L -> entryPoint.savedL().likes.refresh()
                    P2pType.L_ALBUM -> entryPoint.savedL().albums.refresh()
                    else -> Unit
                }
            },
            inboxRoot = File(AppPath.p2p_inbox),
            mainRoot = File(AppPath.main),
        )
```

Примечание: `SavedL.albums` — это `SavedL_Albums` с методом `refresh()` (`SavedL_Albums.kt:77`); если у `SavedL` поле называется иначе — посмотреть `app/src/main/java/com/client/xvideos/l/featured/saved/SavedL.kt`.

- [ ] **Step 4: Run tests to verify they pass**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.client.xvideos.common.p2p.*"`
Expected: PASS, включая оба новых теста; остальные p2p-тесты не сломаны.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/client/xvideos/common/p2p/P2pType.kt app/src/main/java/com/client/xvideos/common/p2p/P2pReceiveManager.kt app/src/test/java/com/client/xvideos/common/p2p/P2pManifestCodecTest.kt app/src/test/java/com/client/xvideos/common/p2p/imports/StoreBundleImporterTest.kt
git commit -m "feat(p2p): L_ALBUM bundle type received into L/Album store"
```

---

### Task 2: LAlbumExporter (TDD)

**Files:**
- Modify: `app/src/main/java/com/client/xvideos/common/p2p/export/Exporters.kt`
- Test: `app/src/test/java/com/client/xvideos/common/p2p/export/ExportersTest.kt`

- [ ] **Step 1: Write the failing tests**

В `ExportersTest.kt` добавить импорты:

```kotlin
import com.client.xvideos.l.model.AlbumDetails
import com.google.gson.GsonBuilder
import org.junit.Assert.assertTrue
```

и тесты в конец класса. Хелпер `albumDetails(id)`: у `AlbumDetails`
(`app/src/main/java/com/client/xvideos/l/model/AlbumDetails.kt:5`) много
обязательных параметров — заполнить минимальными значениями (`""`, `0`,
`emptyList()`, `null` для nullable) по факту компиляции; для тестов важен
только `id`.

```kotlin
    private fun albumDetails(id: String): AlbumDetails = AlbumDetails(
        id = id,
        // остальные обязательные параметры конструктора — минимальными
        // значениями ("", 0, emptyList(), null) по факту компиляции
    )

    @Test
    fun `L album exporter uses saved file when album is saved`() {
        val main = tmp.newFolder("xvideos")
        val savedRoot = File(main, "L/Album").apply { mkdirs() }
        val outboxRoot = File(main, "outbox/L/Album")
        File(savedRoot, "42.album").writeText("{\"id\":\"42\"}")

        val bundle = LAlbumExporter.export(albumDetails("42"), savedRoot, outboxRoot)!!

        assertEquals(P2pType.L_ALBUM, bundle.type)
        assertEquals(savedRoot, bundle.storeRoot)
        assertEquals(listOf(File(savedRoot, "42.album")), bundle.files)
        assertEquals("42.album", bundle.metadataFile!!.name)
    }

    @Test
    fun `L album exporter writes gson file to outbox when not saved`() {
        val main = tmp.newFolder("xvideos")
        val savedRoot = File(main, "L/Album").apply { mkdirs() }
        val outboxRoot = File(main, "outbox/L/Album")

        val bundle = LAlbumExporter.export(albumDetails("42"), savedRoot, outboxRoot)!!

        assertEquals(P2pType.L_ALBUM, bundle.type)
        assertEquals(outboxRoot, bundle.storeRoot)
        val file = File(outboxRoot, "42.album")
        assertTrue(file.exists())
        // содержимое читается обратно как AlbumDetails (формат FileDB)
        val back = GsonBuilder().setPrettyPrinting().create()
            .fromJson(file.readText(), AlbumDetails::class.java)
        assertEquals("42", back.id)
    }

    @Test
    fun `L album exporter returns null for invalid id`() {
        val main = tmp.newFolder("xvideos")
        assertNull(
            LAlbumExporter.export(
                albumDetails(""),
                File(main, "L/Album"),
                File(main, "outbox/L/Album"),
            )
        )
    }
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.client.xvideos.common.p2p.export.ExportersTest"`
Expected: FAIL — compilation error, `LAlbumExporter` не существует.

- [ ] **Step 3: Implement LAlbumExporter**

В `Exporters.kt` добавить импорты:

```kotlin
import com.client.xvideos.l.model.AlbumDetails
import com.google.gson.GsonBuilder
```

и объект в конец файла:

```kotlin
/**
 * Альбом L — только файл метаданных `<id>.album` (контент получатель качает сам).
 * Сохранённый альбом берётся из [savedRoot] (`AppPath.l_albums`); несохранённый
 * сериализуется в [outboxAlbumRoot] (outbox-зеркало l_albums) в формате FileDB
 * (Gson, pretty printing). Возвращает null при невалидном id или ошибке записи.
 */
object LAlbumExporter {

    private val gson = GsonBuilder().setPrettyPrinting().create()

    fun export(album: AlbumDetails, savedRoot: File, outboxAlbumRoot: File): P2pExportBundle? {
        if (album.id.toLongOrNull() == null) return null
        val fileName = "${album.id}.album"

        val savedFile = File(savedRoot, fileName)
        if (savedFile.exists()) {
            return P2pExportBundle(P2pType.L_ALBUM, savedRoot, listOf(savedFile), savedFile)
        }

        return runCatching {
            outboxAlbumRoot.mkdirs()
            val outFile = File(outboxAlbumRoot, fileName)
            outFile.writeText(gson.toJson(album), Charsets.UTF_8)
            P2pExportBundle(P2pType.L_ALBUM, outboxAlbumRoot, listOf(outFile), outFile)
        }.getOrNull()
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.client.xvideos.common.p2p.export.ExportersTest"`
Expected: PASS (7 тестов: 4 старых + 3 новых).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/client/xvideos/common/p2p/export/Exporters.kt app/src/test/java/com/client/xvideos/common/p2p/export/ExportersTest.kt
git commit -m "feat(p2p): LAlbumExporter - album metadata bundle from store or outbox"
```

---

### Task 3: UI — кнопка в ScreenLAlbum + shareAlbumP2p

Compose-экраны юнит-тестами в проекте не покрываются — проверка компиляцией, полным прогоном и смоуком (Task 4).

**Files:**
- Create: `app/src/main/java/com/client/xvideos/l/ui/screens/screenAlbum/atom/AlbumInfoButtonShareAlbum.kt`
- Modify: `app/src/main/java/com/client/xvideos/l/ui/screens/screenAlbum/ScreenLAlbumSM.kt`
- Modify: `app/src/main/java/com/client/xvideos/l/ui/screens/screenAlbum/ScreenAlbum.kt`

- [ ] **Step 1: Create the button atom**

Создать `AlbumInfoButtonShareAlbum.kt` (стиль — копия `AlbumInfoButtonSaveAlbum`):

```kotlin
package com.client.xvideos.l.ui.screens.screenAlbum.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.theme.Theme

/** Кнопка «поделиться альбомом по P2P» в шапке ScreenLAlbum. */
@Composable
fun AlbumInfoButtonShareAlbum(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(top = 2.dp, bottom = 4.dp)
            .height(46.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, Theme.L.grey3, RoundedCornerShape(4.dp))
            .background(Theme.L.grey6)
            .clickable(onClick = { onClick() }),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "Share Album (P2P)",
            color = Color.White,
            style = Theme.L.Type.button.copy(color = Color.White)
        )
    }
}

@Preview
@Composable
fun AlbumInfoButtonShareAlbumPreview() {
    AlbumInfoButtonShareAlbum(onClick = {})
}
```

- [ ] **Step 2: Add shareAlbumP2p to ScreenLAlbumSM**

В `ScreenLAlbumSM.kt` добавить импорты:

```kotlin
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.p2p.P2pSendSource
import com.client.xvideos.common.p2p.export.LAlbumExporter
import com.client.xvideos.common.p2p.mirrorRoot
import com.client.xvideos.l.model.AlbumDetails
import java.io.File
```

и после `saveAlbum()` (строка ~67) добавить:

```kotlin
    /** Источник для экрана P2P-отправки альбома; экран наблюдает и пушит ScreenP2pSend. */
    var p2pAlbumSource by mutableStateOf<P2pSendSource?>(null)
        private set

    fun dismissP2pAlbum() { p2pAlbumSource = null }

    /**
     * Поделиться альбомом по P2P: бандл — только файл метаданных `<id>.album`
     * (из l_albums, если альбом сохранён, иначе пишется в outbox-зеркало).
     */
    fun shareAlbumP2p(album: AlbumDetails) {
        scope.launch(Dispatchers.IO) {
            val outboxAlbumRoot = mirrorRoot(
                base = File(AppPath.p2p_outbox),
                mainRoot = File(AppPath.main),
                storeRoot = File(AppPath.l_albums),
            )
            val bundle = LAlbumExporter.export(album, File(AppPath.l_albums), outboxAlbumRoot)
            if (bundle == null) {
                SnackBar.error("Не удалось подготовить альбом для P2P")
                return@launch
            }
            withContext(Dispatchers.Main) {
                p2pAlbumSource = P2pSendSource.Ready(bundle)
            }
        }
    }
```

- [ ] **Step 3: Wire button and navigation in ScreenAlbum.kt**

1. Импорты:

```kotlin
import com.client.xvideos.common.p2p.ui.ScreenP2pSend
import com.client.xvideos.l.ui.screens.screenAlbum.atom.AlbumInfoButtonShareAlbum
```

2. После строки `AlbumInfoButtonSaveAlbum(saved, onClick = ...)` (ScreenAlbum.kt:259) добавить:

```kotlin
                                AlbumInfoButtonShareAlbum(onClick = { vm.shareAlbumP2p(parsed) })
```

3. В `Content()` перед `Scaffold(` (после блока диалога удаления, строка ~158) добавить наблюдение:

```kotlin
        vm.p2pAlbumSource?.let { source ->
            // Навигация — side effect, нельзя звать прямо из композиции.
            LaunchedEffect(source) {
                navigator.push(ScreenP2pSend(source))
                vm.dismissP2pAlbum()
            }
        }
```

- [ ] **Step 4: Compile and run the full unit test suite**

Run: `.\gradlew.bat :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL, все тесты PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/client/xvideos/l/ui/screens/screenAlbum/atom/AlbumInfoButtonShareAlbum.kt app/src/main/java/com/client/xvideos/l/ui/screens/screenAlbum/ScreenLAlbumSM.kt app/src/main/java/com/client/xvideos/l/ui/screens/screenAlbum/ScreenAlbum.kt
git commit -m "feat(l): Share Album (P2P) button in ScreenLAlbum"
```

---

### Task 4: Финальная верификация

- [ ] **Step 1: Full unit test run + assemble**

Run: `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug`
Expected: BUILD SUCCESSFUL, все тесты PASS.

- [ ] **Step 2: Ручной смоук на двух устройствах (чек-лист пользователю)**

1. Открыть НЕсохранённый альбом → «Share Album (P2P)» → экран отправки сразу в поиске (без «Подготовка файлов») → передача → «Готово».
2. На приёмнике альбом появился в Saved Albums без перезахода; открытие альбома подгружает картинки с сервера.
3. Открыть СОХРАНЁННЫЙ альбом → share → у получателя альбом появился/обновился.
4. После передачи `/xvideos/outbox` пуст; `<id>.album` у получателя лежит в `/xvideos/L/Album`.
5. Обычные передачи L-item/X/R работают как раньше.

- [ ] **Step 3: Verify clean tree**

```bash
git status
```
Expected: clean.
