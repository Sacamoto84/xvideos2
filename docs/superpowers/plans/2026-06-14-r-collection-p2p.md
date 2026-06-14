# R Collection P2P (R_COLLECTION) — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add P2P transfer of R collections by mirroring the proven L_COLLECTION pipeline as a new `P2pType.R_COLLECTION` with R-specific exporter/importer, then enable the «Поделиться (P2P)» item in the R collection action menu.

**Architecture:** New `P2pType.R_COLLECTION` + `P2pSendSource.ShareCollectionR`; `RCollectionExporter` zips `r_collection/<name>` to outbox; `RCollectionBundleImporter` unzips into `r_collection` + merges + refreshes `savedRed.collections`; `P2pReceiveManager` routes the new type; `ScreenP2pSend` exports it; the R menu pushes the send screen. L code is untouched.

**Tech Stack:** Kotlin, Nearby Connections P2P, `ZipUtils`, Voyager. Verification gate = `gradlew :app:compileDebugKotlin` (P2P has no unit tests; real check is a 2-device manual transfer).

---

## Verification note

Gate: `./gradlew :app:compileDebugKotlin` → `BUILD SUCCESSFUL`. Build is auto-routed to context-mode;
run via `ctx_execute(language:"shell")` piping through `grep -iE "^e: |error:|BUILD SUCCESSFUL|BUILD FAILED"`.

**Compile atomicity:** Adding `P2pType.R_COLLECTION` makes the exhaustive `when (type)` in
`P2pReceiveManager.storeRootFor` non-exhaustive, and adding `ShareCollectionR` breaks the
exhaustive `when (source)` in `ScreenP2pSend`. Therefore **all of Task 1's edits must land before
the first compile** — do not compile mid-Task-1. (The `importer` `when (manifest.type)` and
`refreshFor` already have `else`, so they don't break, but we add an explicit R_COLLECTION branch
for routing.)

Do not change Done-semantics / GMS-event filtering / CLUSTER strategy — this feature reuses the
existing transfer controller unchanged.

---

## Task 1: Backend — R_COLLECTION type, exporter, importer, routing, send (atomic)

**Files:**
- Modify: `app/src/main/java/com/client/xvideos/common/p2p/P2pType.kt`
- Modify: `app/src/main/java/com/client/xvideos/common/p2p/P2pSendSource.kt`
- Modify: `app/src/main/java/com/client/xvideos/common/p2p/export/Exporters.kt`
- Create: `app/src/main/java/com/client/xvideos/common/p2p/imports/RCollectionBundleImporter.kt`
- Modify: `app/src/main/java/com/client/xvideos/common/p2p/P2pReceiveManager.kt`
- Modify: `app/src/main/java/com/client/xvideos/common/p2p/ui/ScreenP2pSend.kt`

- [ ] **Step 1: Add the enum value (`P2pType.kt`)**

Replace:
```kotlin
enum class P2pType { X, R, L, L_ALBUM, L_COLLECTION }
```
with:
```kotlin
enum class P2pType { X, R, L, L_ALBUM, L_COLLECTION, R_COLLECTION }
```

- [ ] **Step 2: Add the send source (`P2pSendSource.kt`)**

After the `ShareCollection` data class (before the closing `}` of the sealed interface), add:
```kotlin

    /** Коллекция R: зипуется в outbox на экране отправки и шлётся одним архивом. */
    data class ShareCollectionR(val collectionName: String) : P2pSendSource
```

- [ ] **Step 3: Add `RCollectionExporter` (`Exporters.kt`)**

At the end of the file (after `object LCollectionExporter { … }`), add:
```kotlin

/**
 * Коллекция R — папка коллекции (мелкие `.collection` JSON-ссылки) одним zip-архивом.
 * Зеркало [LCollectionExporter], отличается только типом [P2pType.R_COLLECTION].
 */
object RCollectionExporter {

    fun export(collectionName: String, collectionRoot: File, outboxDir: File): P2pExportBundle? {
        val source = File(collectionRoot, collectionName)
        if (!source.isDirectory) return null
        if (source.walkTopDown().none { it.isFile }) return null

        return runCatching {
            outboxDir.mkdirs()
            val zipFile = File(outboxDir, "$collectionName.zip")
            ZipUtils.zipDirectory(source, zipFile)
            P2pExportBundle(P2pType.R_COLLECTION, outboxDir, listOf(zipFile), null)
        }.getOrNull()
    }
}
```
(`P2pExportBundle`, `P2pType`, `ZipUtils`, `File` already imported in this file.)

- [ ] **Step 4: Create `RCollectionBundleImporter.kt`**

```kotlin
package com.client.xvideos.common.p2p.imports

import com.client.xvideos.common.p2p.P2pInboxMerger
import com.client.xvideos.common.p2p.P2pManifest
import com.client.xvideos.common.p2p.mirrorRoot
import com.client.xvideos.common.zip.ZipUtils
import java.io.File

/**
 * Приём коллекции R: распаковывает принятый zip в зеркало `inbox/R/Collection`,
 * мёржит в боевой store (перезапись при совпадении имени) и дёргает refresh.
 * Зеркало `LCollectionBundleImporter`; отличается корнем store и refresh-колбэком.
 */
class RCollectionBundleImporter(
    private val inboxRoot: File,
    private val mainRoot: File,
    private val collectionStoreRoot: File,
    private val refresh: () -> Unit,
) : BundleImporter {

    override suspend fun import(manifest: P2pManifest, receivedFiles: Map<Long, File>) {
        val zip = receivedFiles.values.firstOrNull()
            ?: error("R_COLLECTION bundle has no file")
        val mirror = mirrorRoot(inboxRoot, mainRoot, collectionStoreRoot)
        ZipUtils.unzip(zip, mirror)
        P2pInboxMerger.merge(inboxRoot, mainRoot)
        refresh()
    }
}
```
(`BundleImporter` is in the same package — no import needed.)

- [ ] **Step 5: Route the new type (`P2pReceiveManager.kt`)**

5a. Add import near the other `imports.*` imports:
```kotlin
import com.client.xvideos.common.p2p.imports.RCollectionBundleImporter
```

5b. In `StoreBundleImporter.storeRootFor`'s `when (type)`, add the branch (after `P2pType.L_COLLECTION -> …`):
```kotlin
                    P2pType.R_COLLECTION -> File(AppPath.r_collection)
```

5c. After the `lCollectionImporter` val, add:
```kotlin
        val rCollectionImporter = RCollectionBundleImporter(
            inboxRoot = File(AppPath.p2p_inbox),
            mainRoot = File(AppPath.main),
            collectionStoreRoot = File(AppPath.r_collection),
            refresh = { entryPoint.savedRed().collections.refreshCollectionList() },
        )
```

5d. In the `importer` `when (manifest.type)`, add the branch (before `else -> storeImporter.import(...)`):
```kotlin
                P2pType.R_COLLECTION -> rCollectionImporter.import(manifest, files)
```

- [ ] **Step 6: Export the new source (`ScreenP2pSend.kt`)**

6a. Add import (next to the `LCollectionExporter` import):
```kotlin
import com.client.xvideos.common.p2p.export.RCollectionExporter
```

6b. In the `when (source)` of the bundle builder, add the branch after the `ShareCollection` branch:
```kotlin
    is P2pSendSource.ShareCollectionR -> withContext(Dispatchers.IO) {
        RCollectionExporter.export(
            collectionName = source.collectionName,
            collectionRoot = File(AppPath.r_collection),
            outboxDir = File(AppPath.p2p_outbox),
        ) ?: error("Не удалось подготовить коллекцию")
    }
```

- [ ] **Step 7: Compile (atomic)**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`. If a different exhaustive `when (type)` is flagged (e.g.
`ExpandMenuVM.kt:60` — only if its `type` is `P2pType`), add a no-op `R_COLLECTION` branch or
confirm it is a different enum. Fix and re-run.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/client/xvideos/common/p2p/P2pType.kt app/src/main/java/com/client/xvideos/common/p2p/P2pSendSource.kt app/src/main/java/com/client/xvideos/common/p2p/export/Exporters.kt app/src/main/java/com/client/xvideos/common/p2p/imports/RCollectionBundleImporter.kt app/src/main/java/com/client/xvideos/common/p2p/P2pReceiveManager.kt app/src/main/java/com/client/xvideos/common/p2p/ui/ScreenP2pSend.kt
git commit -m "feat(p2p): R_COLLECTION transfer (exporter/importer/routing) mirroring L_COLLECTION"
```

---

## Task 2: Enable «Поделиться (P2P)» in the R action menu

**Files:**
- Modify: `app/src/main/java/com/client/xvideos/r/ui/explorer/tab/saved/tab/R_Screen_CollectionTab.kt`

- [ ] **Step 1: Add imports**

After `import cafe.adriel.voyager.navigator.Navigator`, add:
```kotlin
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.p2p.P2pSendSource
import com.client.xvideos.common.p2p.ui.ScreenP2pSend
```

- [ ] **Step 2: Obtain the navigator in `Content()`**

Right after `val vm = getScreenModel<ScreenSavedCollectionSM>()` (top of `Content()`), add:
```kotlin
        val navigator = LocalNavigator.currentOrThrow
```

- [ ] **Step 3: Enable the P2P menu item**

Replace the disabled item:
```kotlin
                    DropdownMenuItem(
                        enabled = false,
                        text = { androidx.compose.material3.Text("Поделиться (P2P) — скоро", style = Theme.L.Type.menuItem.copy(color = Color.Black)) },
                        onClick = { },
                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = Theme.L.DialogLavande.buttonBackground) }
                    )
```
with:
```kotlin
                    DropdownMenuItem(
                        text = { androidx.compose.material3.Text("Поделиться (P2P)", style = Theme.L.Type.menuItem.copy(color = Color.Black)) },
                        onClick = {
                            itemPendingAction = null
                            navigator.push(ScreenP2pSend(P2pSendSource.ShareCollectionR(pending)))
                        },
                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = Theme.L.DialogLavande.buttonBackground) }
                    )
```

- [ ] **Step 4: Compile**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/client/xvideos/r/ui/explorer/tab/saved/tab/R_Screen_CollectionTab.kt
git commit -m "feat(R): enable P2P share in collection action menu (ShareCollectionR)"
```

---

## Task 3: Final verification

**Files:** none (verification only; fix inline if needed)

- [ ] **Step 1: Full compile**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`. Fix any unresolved reference inline, re-run.

- [ ] **Step 2: Grep — R_COLLECTION wired end-to-end**

Run:
```bash
cd app/src/main/java/com/client/xvideos
grep -rnE 'R_COLLECTION|ShareCollectionR|RCollectionExporter|RCollectionBundleImporter' common/p2p/ r/ui/explorer/tab/saved/tab/R_Screen_CollectionTab.kt
```
Expected: hits in P2pType, P2pSendSource, Exporters, RCollectionBundleImporter, P2pReceiveManager
(storeRootFor + importer + val), ScreenP2pSend, and R_Screen_CollectionTab.

- [ ] **Step 3: L untouched check**

Run:
```bash
cd app/src/main/java/com/client/xvideos
git diff --name-only master.. | grep -E 'l_collection|LCollection|L_Screen_CollectionTab' || echo "L collection pipeline untouched"
```
Expected: `L collection pipeline untouched` (no L collection files in the diff).

- [ ] **Step 4: Manual 2-device test (user)**

On two devices with the new build: open an R collection's long-press menu → «Поделиться (P2P)» →
send screen finds the peer → transfer completes → receiver shows the collection in R with the same
items. Re-send to test name-collision overwrite. (Agent cannot run this; report it as a manual gate.)

- [ ] **Step 5: Commit (only if inline fixes were made)**

```bash
git add -A
git commit -m "fix(p2p): verification pass for R_COLLECTION transfer"
```

---

## Self-Review

**Spec coverage:**
- Раздел 1 (types): `P2pType.R_COLLECTION` (Task 1.1) + `ShareCollectionR` (Task 1.2). ✓
- Раздел 2 (export): `RCollectionExporter` zip mirror (Task 1.3). ✓
- Раздел 3 (import+routing): `RCollectionBundleImporter` (Task 1.4) + `P2pReceiveManager` storeRootFor/importer/val (Task 1.5). ✓
- Раздел 4 (UI): R menu item enabled → `ScreenP2pSend(ShareCollectionR)` + navigator (Task 2). ✓
- Раздел 5 (compat/verify): compile gate (every task) + grep + 2-device manual (Task 3). ✓
- L untouched (R-specific) → no L file edited; Task 3.3 asserts it. ✓

**Placeholder scan:** No TBD/TODO. All code literal; the only conditional is Task 1.7's note about
`ExpandMenuVM.kt:60` which is a compiler-driven check, not a placeholder. ✓

**Type/name consistency:** `P2pType.R_COLLECTION`, `P2pSendSource.ShareCollectionR(collectionName)`,
`RCollectionExporter.export(collectionName, collectionRoot, outboxDir)`,
`RCollectionBundleImporter(inboxRoot, mainRoot, collectionStoreRoot, refresh)` —
identical across Task 1 definitions and their Task 1.5/1.6/Task 2 call sites.
`entryPoint.savedRed().collections.refreshCollectionList()` matches the R store accessor used in
`R_Screen_CollectionTab` (`savedRed.collections`). `AppPath.r_collection` / `p2p_inbox` /
`p2p_outbox` match `AppPath`. ✓
