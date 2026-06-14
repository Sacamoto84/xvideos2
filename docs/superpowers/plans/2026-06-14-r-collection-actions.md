# R Collection Actions + Cover Image — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give R collections a long-press action menu (Rename / Share-P2P / Delete) matching L, implement `renameCollection` for R via the shared `CollectionDB`, and show a 96dp collection cover image in both R and L action dialogs.

**Architecture:** Rename primitive added to `CollectionDB` (folder rename) + a concrete `renameCollection` on `LinkCollectionStore` (R inherits; L keeps its existing direct-file rename — different storage layer, out of scope to migrate). R screen gains action/rename dialogs mirroring L. Both action dialogs get an `icon = { cover }` resolved by collection name. P2P item in R is shown disabled («скоро»); full R P2P export is a separate spec.

**Tech Stack:** Kotlin, Compose Material3, existing `LavenderDialog`, `CollectionDB`/`LinkCollectionStore`, `UrlImage`. Verification gate = `gradlew :app:compileDebugKotlin` (Compose layout not unit-tested).

---

## Verification note

Gate per task: `./gradlew :app:compileDebugKotlin` → `BUILD SUCCESSFUL` (Windows: `./gradlew.bat`).
Build commands are auto-routed to context-mode; run via `ctx_execute(language:"shell")` piping through
`grep -iE "^e: |error:|BUILD SUCCESSFUL|BUILD FAILED"` so only the result returns.
Compose UI has no unit tests — also eyeball `@Preview` in Android Studio.

Out of scope: real P2P export/receive for R collections; migrating L's storage to `CollectionDB`.

---

## Task 1: Shared `renameCollection` (CollectionDB + LinkCollectionStore)

**Files:**
- Modify: `app/src/main/java/com/client/xvideos/common/collectionDB/CollectionDB.kt`
- Modify: `app/src/main/java/com/client/xvideos/common/collectionDB/model/LinkCollectionStore.kt`

- [ ] **Step 1: Add `renameCollection` to `CollectionDB`**

Insert after the `deleteCollection(...)` function (after its closing `}` near line 52), before `deleteItem`:

```kotlin
    fun renameCollection(oldName: String, newName: String): Result<Boolean> =
        runCatching {
            val trimmed = newName.trim()
            if (trimmed.isBlank()) {
                throw IOException("Имя коллекции не может быть пустым")
            }
            if (oldName == trimmed) {
                return Result.success(true)
            }
            val oldDir = File(path, oldName)
            val newDir = File(path, trimmed)
            if (!oldDir.exists()) {
                Timber.w("Коллекция \"$oldName\" не найдена: ${oldDir.absolutePath}")
                return Result.success(false)
            }
            if (newDir.exists()) {
                throw IOException("Коллекция \"$trimmed\" уже существует")
            }
            if (!oldDir.renameTo(newDir)) {
                throw IOException("Не удалось переименовать коллекцию: ${oldDir.absolutePath}")
            }
            Timber.i("Переименована коллекция: $oldName -> $trimmed")
            Result.success(true)
        }.getOrElse { e ->
            Timber.e(e, "Ошибка при переименовании коллекции $oldName -> $newName")
            Result.failure(e)
        }
```

(`File`, `IOException`, `Timber` are already imported in this file.)

- [ ] **Step 2: Add concrete `renameCollection` to `LinkCollectionStore`**

In `LinkCollectionStore.kt`, add the import near the top (after the existing imports):

```kotlin
import com.client.xvideos.common.snackbar.SnackBar
```

Then add this method inside the class, after `abstract fun refreshCollectionList()` (before the closing `}`):

```kotlin
    open fun renameCollection(oldName: String, newName: String): Boolean =
        collectionDb.renameCollection(oldName, newName).fold(
            onSuccess = { ok ->
                if (ok) {
                    SnackBar.success("Коллекция переименована")
                    refreshCollectionList()
                } else {
                    SnackBar.error("Коллекция не найдена")
                }
                ok
            },
            onFailure = { e ->
                SnackBar.error("Ошибка переименования: ${e.message}")
                false
            }
        )
```

- [ ] **Step 3: Compile**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/client/xvideos/common/collectionDB/CollectionDB.kt app/src/main/java/com/client/xvideos/common/collectionDB/model/LinkCollectionStore.kt
git commit -m "feat(collections): shared renameCollection in CollectionDB + LinkCollectionStore (R gains rename)"
```

---

## Task 2: R action menu + rename dialog + cover + long-press rewire

**Files:**
- Modify: `app/src/main/java/com/client/xvideos/r/ui/explorer/tab/saved/tab/R_Screen_CollectionTab.kt`

- [ ] **Step 1: Add imports**

After the existing `import com.client.xvideos.common.theme.LavenderDialog` line, add:

```kotlin
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.coil.UrlImage
```

(`Text` here is `com.composeunstyled.Text` already imported; the menu item text uses `androidx.compose.material3.Text` — see Step 2 note.)

- [ ] **Step 2: Replace the delete-state block + the `R_SavedCollectionTabContent(...)` call**

Replace the current block (the `var itemPendingDelete ...` through the end of the `R_SavedCollectionTabContent(...)` call, i.e. lines ~71–104) with:

```kotlin
        var itemPendingAction by remember { mutableStateOf<String?>(null) }
        var itemPendingRename by remember { mutableStateOf<String?>(null) }
        var renameValue by remember { mutableStateOf("") }
        var itemPendingDelete by remember { mutableStateOf<String?>(null) }

        fun coverOf(name: String): String? =
            savedRed.collections.collectionList
                .firstOrNull { it.collection == name }
                ?.items?.lastOrNull()?.urls?.thumbnail

        // ---------- Меню действий (long-press) ----------
        itemPendingAction?.let { pending ->
            LavenderDialog(
                title = "Действие с коллекцией",
                onDismiss = { itemPendingAction = null },
                icon = { CollectionCoverIcon(coverOf(pending)) },
                content = {
                    androidx.compose.material3.Text(
                        pending,
                        fontSize = 16.sp,
                        color = Theme.L.b0
                    )
                    DropdownMenuItem(
                        text = { androidx.compose.material3.Text("Переименовать", style = Theme.L.Type.menuItem.copy(color = Color.Black)) },
                        onClick = {
                            renameValue = pending
                            itemPendingRename = pending
                            itemPendingAction = null
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = Theme.L.DialogLavande.buttonBackground) }
                    )
                    DropdownMenuItem(
                        enabled = false,
                        text = { androidx.compose.material3.Text("Поделиться (P2P) — скоро", style = Theme.L.Type.menuItem.copy(color = Color.Black)) },
                        onClick = { },
                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = Theme.L.DialogLavande.buttonBackground) }
                    )
                    DropdownMenuItem(
                        text = { androidx.compose.material3.Text("Удалить коллекцию", style = Theme.L.Type.menuItem.copy(color = Color.Black)) },
                        onClick = {
                            itemPendingDelete = pending
                            itemPendingAction = null
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Theme.L.DialogLavande.buttonBackground) }
                    )
                },
            )
        }

        // ---------- Переименование ----------
        itemPendingRename?.let { pending ->
            LavenderDialog(
                title = "Переименовать коллекцию",
                onDismiss = { itemPendingRename = null },
                icon = { CollectionCoverIcon(coverOf(pending)) },
                content = {
                    OutlinedTextField(
                        value = renameValue,
                        onValueChange = { renameValue = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        label = { androidx.compose.material3.Text("Название коллекции") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Theme.L.DialogLavande.buttonBackground,
                            unfocusedTextColor = Theme.L.DialogLavande.buttonBackground,
                            cursorColor = Theme.L.DialogLavande.buttonBackground,
                            focusedBorderColor = Theme.L.DialogLavande.buttonBackground,
                            unfocusedBorderColor = Theme.L.DialogLavande.buttonBackground,
                            focusedLabelColor = Theme.L.DialogLavande.buttonBackground,
                            unfocusedLabelColor = Theme.L.DialogLavande.buttonBackground,
                        ),
                    )
                },
                confirmText = "Сохранить",
                onConfirm = {
                    if (savedRed.collections.renameCollection(pending, renameValue)) {
                        itemPendingRename = null
                    }
                },
            )
        }

        // ---------- Удаление ----------
        itemPendingDelete?.let { pending ->
            LavenderDialog(
                title = "Удалить коллекцию?",
                onDismiss = { itemPendingDelete = null },
                icon = { CollectionCoverIcon(coverOf(pending)) },
                body = buildAnnotatedString {
                    append("Удалить «")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(pending) }
                    append("» из коллекции")
                },
                confirmText = "Удалить",
                onConfirm = {
                    savedRed.collections.deleteCollection(pending)
                    itemPendingDelete = null
                },
                destructive = true,
            )
        }

        R_SavedCollectionTabContent(
            selectedCollection = selectedCollection,
            collectionList = savedRed.collections.collectionList,
            gridState = vm.gridState,
            onCollectionClick = { savedRed.collections.selectedCollection.value = it },
            onCollectionLongClick = { itemPendingAction = it },
            onCreateNewCollectionClick = { savedRed.collections.visibleDialogCreateNew = true },
            navigationContent = {
                if (selectedCollection != null) {
                    Navigator(ScreenCollectionName(selectedCollection))
                }
            }
        )
```

Note: this file imports `Text` as `com.composeunstyled.Text`; the dialog uses `androidx.compose.material3.Text` (fully-qualified inline) for menu/label text so both coexist.

- [ ] **Step 3: Add the shared cover-icon composable**

At file scope (e.g. just after the `R_SavedCollectionTabContent(...)` function), add:

```kotlin
@Composable
private fun CollectionCoverIcon(coverUrl: String?) {
    val size = Theme.L.DialogLavande.iconSize
    if (coverUrl != null) {
        UrlImage(url = coverUrl, modifier = Modifier.clip(RoundedCornerShape(8.dp)).size(size))
    } else {
        Box(Modifier.clip(RoundedCornerShape(8.dp)).size(size).background(Color.Gray))
    }
}
```

(`@Composable`, `Modifier`, `Color` already imported; `Theme` already imported.)

- [ ] **Step 4: Compile**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/client/xvideos/r/ui/explorer/tab/saved/tab/R_Screen_CollectionTab.kt
git commit -m "feat(R): collection long-press action menu (rename/share-soon/delete) + cover image"
```

---

## Task 3: Cover image in L action dialog

**Files:**
- Modify: `app/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/saved/collection/L_Screen_CollectionTab.kt`

- [ ] **Step 1: Add a cover-icon to the L action dialog**

In the `itemPendingAction?.let { pending -> LavenderDialog( title = "Действие с коллекцией", onDismiss = ... ,` block, add an `icon` argument right after `onDismiss`:

```kotlin
                icon = {
                    val cover = savedL.collection.collectionList
                        .firstOrNull { it.collection == pending }?.previewUrl
                    val size = Theme.L.DialogLavande.iconSize
                    if (cover != null) {
                        UrlImage(url = cover, modifier = Modifier.clip(RoundedCornerShape(8.dp)).size(size))
                    } else {
                        Box(Modifier.clip(RoundedCornerShape(8.dp)).size(size).background(Color.Gray))
                    }
                },
```

Ensure these imports exist in the file (add any missing):

```kotlin
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.coil.UrlImage
```

(The file already uses `Box`, `clip`, `size`, `RoundedCornerShape`, `UrlImage`, `dp` in the Smart-collections list, so most are present — add only the ones the compiler reports missing.)

- [ ] **Step 2: Compile**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/saved/collection/L_Screen_CollectionTab.kt
git commit -m "feat(L): show collection cover image in action dialog"
```

---

## Task 4: Final verification

**Files:** none (verification only; fix inline if needed)

- [ ] **Step 1: Full compile**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`. Fix any unresolved-reference inline, re-run.

- [ ] **Step 2: Behaviour checklist (review / Android Studio preview)**

Confirm by reading the diff + previews:
- R long-press sets `itemPendingAction` (not `itemPendingDelete`) → action menu appears.
- R menu: Переименовать → rename dialog; «Поделиться (P2P) — скоро» is `enabled = false`; Удалить → delete dialog.
- R rename calls `savedRed.collections.renameCollection(...)` (inherited from `LinkCollectionStore`).
- Both R and L action dialogs pass `icon = { … cover … }` at 96dp (`Theme.L.DialogLavande.iconSize`), gray placeholder when cover is null.
- L P2P item still navigates to `ScreenP2pSend(P2pSendSource.ShareCollection(...))` (untouched).

- [ ] **Step 3: Commit (only if inline fixes were made)**

```bash
git add -A
git commit -m "fix(collections): verification pass for R action menu + covers"
```

---

## Self-Review

**Spec coverage:**
- Shared rename in base → Task 1 (CollectionDB + LinkCollectionStore; R inherits; L unchanged per spec's "differ → don't unify"). ✓
- R action menu (rename/P2P-disabled/delete) + long-press rewire → Task 2. ✓
- R rename dialog wired to inherited `renameCollection` → Task 2. ✓
- P2P item disabled «скоро» (R), L P2P untouched → Task 2 (`enabled = false`); L file only gains an icon (Task 3). ✓
- Cover 96dp in R and L action dialogs, gray placeholder → Tasks 2 & 3 (`Theme.L.DialogLavande.iconSize`). ✓
- Out of scope (real P2P-R, L storage migration) → no task touches them. ✓

**Placeholder scan:** No TBD/TODO. All dialog/menu code is literal. Imports listed per file. ✓

**Type/name consistency:** `renameCollection(oldName, newName)` signature identical in CollectionDB (`Result<Boolean>`) and LinkCollectionStore (`Boolean`, folding the Result), called as `savedRed.collections.renameCollection(pending, renameValue): Boolean` in Task 2. `coverOf(name)` / `CollectionCoverIcon(coverUrl)` defined and used within Task 2. `Theme.L.DialogLavande.iconSize` used in Tasks 2 & 3. `it.collection` / `previewUrl` / `items.last().urls.thumbnail` match the verified models. ✓
