# Lavender Dialog Unification — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Route all 11 app dialogs through one lavender Material3 composable `LavenderDialog`, styled entirely from `Theme.L.DialogLavande`.

**Architecture:** Add missing tokens to the existing `Theme.L.DialogLavande` object, create `common/theme/LavenderDialog.kt` (canonical composable built on M3 `BasicAlertDialog` + `Surface`), then convert each dialog to call it. Filled primary button + text dismiss; destructive actions get a red fill.

**Tech Stack:** Kotlin, Jetbrains Compose (Material3), existing `Theme` object. Verification gate = Kotlin compile (`gradlew :app:compileDebugKotlin`) + `@Preview` (no unit tests for Compose layout).

---

## Verification note

Compose UI layout is not unit-tested in this project. The gate per task is:
`./gradlew :app:compileDebugKotlin` → expected `BUILD SUCCESSFUL`.
(Windows: `./gradlew.bat :app:compileDebugKotlin`.) If the Gradle/Android SDK is
unavailable in the environment, fall back to: careful diff review + open each changed
file's `@Preview` in Android Studio. Do NOT mark a task complete without the compile passing.

Out of scope (do NOT touch): all expand-menus, `ThumbnailSizeSelector`, `SortByOrder`,
`AlbumFilterDisplay`, `AlbumListPageSelector` and page selectors with keyboard input,
`MenuDotConfig` (legacy).

---

## Task 1: Extend `Theme.L.DialogLavande` tokens

**Files:**
- Modify: `app/src/main/java/com/client/xvideos/common/theme/Theme.kt` (object `DialogLavande`, ~lines 101-121)

- [ ] **Step 1: Add the missing tokens**

In `object DialogLavande`, after the existing `button` TextStyle (before the closing `}` at ~line 121), add:

```kotlin
            val bodyColor = Color(0xFF474747)              // Цвет текста тела
            val dismissTextColor = Color(0xFF6552A5)       // Цвет текста кнопки отмены
            val buttonBackgroundDestructive = Color(0xFFF44336) // Красный для деструктива
            val cornerRadius = 28.dp                        // Скругление диалога
            val iconSize = 96.dp                            // Размер иконки
```

- [ ] **Step 2: Compile**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/client/xvideos/common/theme/Theme.kt
git commit -m "feat(theme): extend DialogLavande tokens (body/dismiss/destructive/corner/icon)"
```

---

## Task 2: Create canonical `LavenderDialog` composable

**Files:**
- Create: `app/src/main/java/com/client/xvideos/common/theme/LavenderDialog.kt`

- [ ] **Step 1: Write the file**

```kotlin
package com.client.xvideos.common.theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Канонический лавандовый диалог (стиль A). Все настройки — из Theme.L.DialogLavande.
 * Кнопки: filled главная + text отмена; destructive=true → красная заливка.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LavenderDialog(
    title: String,
    onDismiss: () -> Unit,
    icon: (@Composable () -> Unit)? = null,
    body: AnnotatedString? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null,
    confirmText: String? = null,
    onConfirm: () -> Unit = {},
    destructive: Boolean = false,
    dismissText: String = "Отмена",
) {
    val d = Theme.L.DialogLavande
    val centered = icon != null

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(d.cornerRadius), color = d.content) {
            Column(Modifier.padding(24.dp)) {

                if (icon != null) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { icon() }
                    Spacer(Modifier.height(16.dp))
                }

                Text(
                    text = title,
                    style = Theme.L.Type.dialogTitle.copy(color = Color.Black, fontWeight = FontWeight.Bold),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = if (centered) TextAlign.Center else TextAlign.Start,
                )

                if (body != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = body,
                        style = Theme.L.Type.dialogBody.copy(color = d.bodyColor),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = if (centered) TextAlign.Center else TextAlign.Start,
                    )
                }

                if (content != null) {
                    Spacer(Modifier.height(12.dp))
                    content()
                }

                Spacer(Modifier.height(20.dp))
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(dismissText, style = d.button.copy(color = d.dismissTextColor))
                    }
                    if (confirmText != null) {
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = onConfirm,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (destructive) d.buttonBackgroundDestructive else d.buttonBackground,
                                contentColor = d.buttonTextColor,
                            ),
                            shape = RoundedCornerShape(d.buttonBorderRadius),
                        ) {
                            Text(confirmText, color = d.buttonTextColor)
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun LavenderDialogPreview() {
    LavenderDialog(
        title = "Удалить Альбом?",
        onDismiss = {},
        body = AnnotatedString("Альбом будет удалён из сохранённых."),
        confirmText = "Удалить",
        onConfirm = {},
        destructive = true,
    )
}
```

- [ ] **Step 2: Compile**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/client/xvideos/common/theme/LavenderDialog.kt
git commit -m "feat(theme): add canonical LavenderDialog composable"
```

---

## Task 3: Convert AlbumDialogDeleteAlbum, DialogSubscriptionDelete, DialogNicheDelete

These three are icon + title + body + destructive confirm. Replace each `AlertDialog(...)`
block with a `LavenderDialog(...)` call; keep the function signature and data wiring.

**Files:**
- Modify: `app/src/main/java/com/client/xvideos/l/ui/screens/screenAlbum/dialog/AlbumDialogDeleteAlbum.kt`
- Modify: `app/src/main/java/com/client/xvideos/r/ui/explorer/tab/saved/tab/DialogSubscriptionDelete.kt`
- Modify: `app/src/main/java/com/client/xvideos/r/ui/explorer/tab/saved/tab/savedNiche/DialogNicheDelete.kt`

- [ ] **Step 1: AlbumDialogDeleteAlbum — replace the `AlertDialog(...)` call**

Body of `fun AlbumDialogDeleteAlbum(pending: AlbumDetails, onDismiss, onClick)` becomes:

```kotlin
    LavenderDialog(
        title = "Удалить Альбом?",
        onDismiss = onDismiss,
        icon = { UrlImage(pending.cover.url, modifier = Modifier.size(96.dp)) },
        body = buildAnnotatedString {
            append("Удалить «")
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(pending.title) }
            append("» из сохранённых?")
        },
        confirmText = "Удалить",
        onConfirm = onClick,
        destructive = true,
    )
```

Add import: `import com.client.xvideos.common.theme.LavenderDialog`. Keep imports for
`UrlImage`, `buildAnnotatedString`, `SpanStyle`, `withStyle`, `FontWeight`, `Modifier`, `size`, `dp`.

- [ ] **Step 2: DialogSubscriptionDelete — replace the `AlertDialog(...)` call**

Inside `user()?.let { pending -> ... }`, replace `AlertDialog(...)` with:

```kotlin
        LavenderDialog(
            title = "Удалить подписку?",
            onDismiss = onDismiss,
            icon = {
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).size(96.dp).background(Color.DarkGray),
                    contentAlignment = Alignment.Center
                ) {
                    if (pending.urlProfile != null) UrlImage(url = pending.urlProfile)
                    else Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(32.dp), tint = Color.White)
                }
            },
            body = buildAnnotatedString {
                append("Удалить автора «")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(pending.name) }
                append("» из подписок?")
            },
            confirmText = "Удалить",
            onConfirm = { onConfirm(pending.name) },
            destructive = true,
        )
```

Add import `LavenderDialog`. Keep `Box/clip/size/background/Icon/Person/Color/Alignment/UrlImage`.

- [ ] **Step 3: DialogNicheDelete — replace the `AlertDialog(...)` call**

Inside `item?.let { pending -> ... }`:

```kotlin
        LavenderDialog(
            title = "Удалить группу?",
            onDismiss = onDismiss,
            icon = { UrlImage(pending.thumbnail, modifier = Modifier.clip(RoundedCornerShape(8.dp)).size(96.dp)) },
            body = buildAnnotatedString {
                append("Удалить «")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(pending.name) }
                append("» из сохранённых?")
            },
            confirmText = "Удалить",
            onConfirm = { onConfirm(pending) },
            destructive = true,
        )
```

Add import `LavenderDialog`.

- [ ] **Step 4: Compile**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/client/xvideos/l/ui/screens/screenAlbum/dialog/AlbumDialogDeleteAlbum.kt app/src/main/java/com/client/xvideos/r/ui/explorer/tab/saved/tab/DialogSubscriptionDelete.kt app/src/main/java/com/client/xvideos/r/ui/explorer/tab/saved/tab/savedNiche/DialogNicheDelete.kt
git commit -m "refactor(dialogs): album/subscription/niche delete -> LavenderDialog (red destructive)"
```

---

## Task 4: Convert ConfirmDeleteFavoriteDialog (X)

**Files:**
- Modify: `app/src/main/java/com/client/xvideos/x/screens/favorites/FavoritesDeleteDialog.kt`

- [ ] **Step 1: Replace the `AlertDialog(...)` call**

Body of `fun ConfirmDeleteFavoriteDialog(item: ItemsX, onConfirm, onDismiss)`:

```kotlin
    LavenderDialog(
        title = "Удалить из избранного?",
        onDismiss = onDismiss,
        icon = {
            UrlImage(
                url = item.previewImage,
                modifier = Modifier.width(160.dp).aspectRatio(352f / 198f).clip(RoundedCornerShape(8.dp))
            )
        },
        confirmText = "Удалить",
        onConfirm = onConfirm,
        destructive = true,
    )
```

Add import `import com.client.xvideos.common.theme.LavenderDialog`. Keep `UrlImage/width/aspectRatio/clip/RoundedCornerShape/Modifier/dp`. Remove the `XvideosTheme(darkTheme=true)` wrapper from the body if present (it only wrapped the old dark AlertDialog — the `@Preview` may keep it).

- [ ] **Step 2: Compile**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/client/xvideos/x/screens/favorites/FavoritesDeleteDialog.kt
git commit -m "refactor(dialogs): favorite delete -> LavenderDialog"
```

---

## Task 5: Convert P2pSendChooserDialog (chooser, no confirm)

**Files:**
- Modify: `app/src/main/java/com/client/xvideos/common/p2p/ui/P2pSendChooserDialog.kt`

- [ ] **Step 1: Replace the `AlertDialog(...)` call**

Body of `fun P2pSendChooserDialog(onSystem, onP2p, onDismiss)`:

```kotlin
    LavenderDialog(
        title = "Поделиться",
        onDismiss = onDismiss,
        content = {
            TextButton(onClick = { onDismiss(); onSystem() }, modifier = Modifier.fillMaxWidth()) {
                Text("Системное (через приложения)", color = Theme.L.DialogLavande.dismissTextColor)
            }
            TextButton(onClick = { onDismiss(); onP2p() }, modifier = Modifier.fillMaxWidth()) {
                Text("P2P рядом (Nearby)", color = Theme.L.DialogLavande.dismissTextColor)
            }
        },
    )
```

Add imports: `LavenderDialog`, `com.client.xvideos.common.theme.Theme`. Keep `TextButton/Text/Modifier/fillMaxWidth`. `confirmText` omitted → no primary button (only Отмена).

- [ ] **Step 2: Compile**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/client/xvideos/common/p2p/ui/P2pSendChooserDialog.kt
git commit -m "refactor(dialogs): P2P chooser -> LavenderDialog"
```

---

## Task 6: Convert DialogButton + DialogBlock (settings/block)

**Files:**
- Modify: `app/src/main/java/com/client/xvideos/common/settings/ui/DialogButton.kt`
- Modify: `app/src/main/java/com/client/xvideos/r/common/block/ui/DialogBlock.kt`

- [ ] **Step 1: DialogButton — keep signature, delegate to LavenderDialog**

Replace the body of `fun DialogButton(visible, title, body, buttonText, onDismiss, onBlockConfirmed, composable)`:

```kotlin
    if (visible) {
        LavenderDialog(
            title = title,
            onDismiss = onDismiss,
            body = if (body.isNotEmpty()) AnnotatedString(body) else null,
            content = if (composable != {}) { { composable() } } else null,
            confirmText = buttonText,
            onConfirm = { onBlockConfirmed(); onDismiss() },
        )
    }
```

Note: `composable` default is `{}`; passing it always as `content = { composable() }` is fine
(empty lambda renders nothing). Simplify to `content = { composable() }`. Add imports:
`LavenderDialog`, `androidx.compose.ui.text.AnnotatedString`. The `ConfigTextAndButtonWithDialog`
caller is unchanged (same `DialogButton` signature).

- [ ] **Step 2: DialogBlock — replace body with LavenderDialog (red destructive)**

Replace the body of `fun DialogBlock(visible, onDismiss, onBlockConfirmed)`:

```kotlin
    if (visible) {
        LavenderDialog(
            title = "Подтвердите блокировку",
            onDismiss = onDismiss,
            body = AnnotatedString("Вы уверены, что хотите заблокировать этот GIFs?"),
            confirmText = "Блокировать",
            onConfirm = { onBlockConfirmed(); onDismiss() },
            destructive = true,
        )
    }
```

Add imports: `LavenderDialog`, `androidx.compose.ui.text.AnnotatedString`.

- [ ] **Step 3: Compile**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/client/xvideos/common/settings/ui/DialogButton.kt app/src/main/java/com/client/xvideos/r/common/block/ui/DialogBlock.kt
git commit -m "refactor(dialogs): settings DialogButton + DialogBlock -> LavenderDialog"
```

---

## Task 7: Convert DaialogNewCollection (text input)

**Files:**
- Modify: `app/src/main/java/com/client/xvideos/common/collectionDB/ui/DaialogNewCollection.kt`

- [ ] **Step 1: Replace the `Dialog(...)` block with LavenderDialog + input content**

Inside `fun DaialogNewCollection(visible, onDismiss, onBlockConfirmed)`, after the existing
`if (!visible) return`, `var text by remember...`, `focusRequester`, and `LaunchedEffect`,
replace the `Dialog(...) { ... }` with:

```kotlin
    LavenderDialog(
        title = "Создать коллекцию",
        onDismiss = onDismiss,
        content = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                singleLine = true,
                label = { Text("Название коллекции") },
            )
        },
        confirmText = "Создать",
        onConfirm = {
            onBlockConfirmed(text)
            onDismiss()
        },
    )
```

Add imports: `LavenderDialog`. Keep `OutlinedTextField/Text/Modifier/fillMaxWidth/focusRequester/remember/mutableStateOf/getValue/setValue/LaunchedEffect/FocusRequester`.

- [ ] **Step 2: Compile**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/client/xvideos/common/collectionDB/ui/DaialogNewCollection.kt
git commit -m "refactor(dialogs): new-collection input -> LavenderDialog (fixes default-purple button)"
```

---

## Task 8: Convert DialogCollection (R) + L_DialogCollection (L) — list dialogs

The collection-list body (title + scrollable list + footer buttons) moves into the
`content` slot; the footer "Создать"/"Отмена" become `LavenderDialog`'s standard actions.
List text colors flip to dark on the lavender background. This fixes the R invisible-title bug.

**Files:**
- Modify: `app/src/main/java/com/client/xvideos/common/collectionDB/ui/DialogCollection.kt`
- Modify: `app/src/main/java/com/client/xvideos/l/ui/screens/explorer/L_DialogCollection.kt`

- [ ] **Step 1: DialogCollection — replace `Dialog { DialogCollectionContent(...) }` and rewrite content**

Keep `fun DialogCollection(visible, onDismiss, onClickNewCollection, onSelectCollection, savedRed)`
guarded by `if (visible)`. Replace its `Dialog(...) { DialogCollectionContent(...) }` with a
`LavenderDialog` whose `content` is the list. Rewrite `DialogCollectionContent`'s inner list into
the content lambda (drop the old Column/border/background/footer — the shell provides them):

```kotlin
    if (visible) {
        LavenderDialog(
            title = "Добавить в коллекцию",
            onDismiss = onDismiss,
            content = {
                LazyColumn(
                    state = rememberLazyListState(),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp, max = 420.dp)
                ) {
                    items(savedRed().collections.collectionList) { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .padding(horizontal = 8.dp).padding(vertical = 4.dp)
                                .clickable(onClick = { onSelectCollection(item.collection) }),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (item.items.isNotEmpty()) {
                                UrlImage(url = item.items.last().urls.thumbnail, modifier = Modifier.clip(RoundedCornerShape(25)).size(72.dp))
                            } else {
                                Box(Modifier.clip(RoundedCornerShape(25)).size(72.dp).background(Color.Gray))
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(item.collection, color = Color.Black, fontFamily = Theme.R.fontFamilyDMsanss)
                        }
                    }
                }
            },
            confirmText = "Создать",
            onConfirm = { onClickNewCollection(); onDismiss() },
        )
    }
```

Delete the now-unused `DialogCollectionContent` composable and its old `Dialog`/Column chrome.
Update the `@Preview` to call `DialogCollection(visible = true, ...)` or remove it if it
referenced `DialogCollectionContent`. Add import `LavenderDialog`. List item text is now
`Color.Black` (was White) — correct on lavender.

- [ ] **Step 2: L_DialogCollection — same conversion**

Replace the `Dialog(...) { Column(... dark chrome ...) }` in `fun L_DialogCollection(savedL: SavedL)` with:

```kotlin
    LavenderDialog(
        title = if (savedL.collection.collectionItemsPendingAdd.size > 1)
            "Добавить в коллекцию (${savedL.collection.collectionItemsPendingAdd.size})"
        else "Добавить в коллекцию",
        onDismiss = { savedL.collection.visibleDialog = false },
        content = {
            LazyColumn(
                state = rememberLazyListState(),
                modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp, max = 420.dp)
            ) {
                items(savedL.collection.collectionList.size) { index ->
                    val collectionItem = savedL.collection.collectionList[index]
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .padding(horizontal = 8.dp).padding(vertical = 4.dp)
                            .clickable(onClick = {
                                savedL.collection.addPendingToCollection(collectionItem.collection)
                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            }),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (collectionItem.previewUrl != null) {
                            UrlImage(url = collectionItem.previewUrl, modifier = Modifier.clip(RoundedCornerShape(25)).size(72.dp))
                        } else {
                            Box(Modifier.clip(RoundedCornerShape(25)).size(72.dp).background(Color.Gray))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(collectionItem.collection, color = Color.Black, fontFamily = Theme.L.fontFamilyDMsanss)
                    }
                }
            }
        },
        confirmText = "Создать",
        onConfirm = {
            savedL.collection.visibleDialog = false
            savedL.collection.visibleDialogCreateNew = true
        },
    )
```

Keep `val haptic = LocalHapticFeedback.current` at the top of the function. Add import
`LavenderDialog`. Item text now `Color.Black`.

- [ ] **Step 3: Compile**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/client/xvideos/common/collectionDB/ui/DialogCollection.kt app/src/main/java/com/client/xvideos/l/ui/screens/explorer/L_DialogCollection.kt
git commit -m "refactor(dialogs): collection list dialogs -> LavenderDialog (fixes R invisible title)"
```

---

## Task 9: Final compile + preview verification pass

**Files:** none (verification only; fix inline if needed)

- [ ] **Step 1: Full module compile**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`. Fix any unresolved reference / unused-import error inline, re-run.

- [ ] **Step 2: Grep — every target dialog now calls LavenderDialog**

Run:
```bash
cd app/src/main/java/com/client/xvideos
grep -rL 'LavenderDialog' \
  l/ui/screens/screenAlbum/dialog/AlbumDialogDeleteAlbum.kt \
  r/ui/explorer/tab/saved/tab/DialogSubscriptionDelete.kt \
  r/ui/explorer/tab/saved/tab/savedNiche/DialogNicheDelete.kt \
  x/screens/favorites/FavoritesDeleteDialog.kt \
  common/p2p/ui/P2pSendChooserDialog.kt \
  common/settings/ui/DialogButton.kt \
  r/common/block/ui/DialogBlock.kt \
  common/collectionDB/ui/DaialogNewCollection.kt \
  common/collectionDB/ui/DialogCollection.kt \
  l/ui/screens/explorer/L_DialogCollection.kt
```
Expected: no output (every file references `LavenderDialog`).

- [ ] **Step 3: Grep — no leftover raw dialog containers in converted files**

Run:
```bash
cd app/src/main/java/com/client/xvideos
grep -rnE 'AlertDialog\(|androidx.compose.ui.window.Dialog' \
  l/ui/screens/screenAlbum/dialog/AlbumDialogDeleteAlbum.kt \
  r/ui/explorer/tab/saved/tab/DialogSubscriptionDelete.kt \
  r/ui/explorer/tab/saved/tab/savedNiche/DialogNicheDelete.kt \
  x/screens/favorites/FavoritesDeleteDialog.kt \
  common/p2p/ui/P2pSendChooserDialog.kt \
  common/settings/ui/DialogButton.kt \
  r/common/block/ui/DialogBlock.kt \
  common/collectionDB/ui/DaialogNewCollection.kt \
  common/collectionDB/ui/DialogCollection.kt \
  l/ui/screens/explorer/L_DialogCollection.kt
```
Expected: no output (only `LavenderDialog` provides the container now). Imports in `@Preview` are OK.

- [ ] **Step 4: Commit (if any inline fixes were made)**

```bash
git add -A
git commit -m "fix(dialogs): final compile + verification pass for lavender unification"
```

---

## Self-Review

**Spec coverage:**
- DialogLavande token additions → Task 1. ✓
- Canonical LavenderDialog (BasicAlertDialog + Surface, filled/text/destructive) → Task 2. ✓
- All 11 dialogs converted: Album/Subscription/Niche (Task 3), Favorite (Task 4), P2P (Task 5),
  DialogButton/DialogBlock (Task 6), NewCollection (Task 7), DialogCollection/L_DialogCollection
  (Task 8). LavenderDialog itself replaces the deleted DialogTemplate (Task 2). ✓ (11 = 1 canonical + 10 call sites)
- Destructive red on Удалить/Блокировать → Tasks 3,4,6. ✓
- Bug fixes (R invisible title, NewCollection default button) → Tasks 7,8. ✓
- `DialogButton` signature preserved (ConfigTextAndButtonWithDialog untouched) → Task 6. ✓
- Out-of-scope objects untouched → no task touches menus/selectors/MenuDotConfig. ✓
- Verification = compile + grep → Task 9 (and per-task compile). ✓

**Placeholder scan:** No TBD/TODO. Each conversion shows the exact `LavenderDialog(...)` call with
real data fields. Import lines named per file. ✓

**Type/name consistency:** `LavenderDialog` signature (Task 2) — params `title`, `onDismiss`,
`icon`, `body`, `content`, `confirmText`, `onConfirm`, `destructive`, `dismissText` — used
identically in Tasks 3-8. Token names (`bodyColor`, `dismissTextColor`,
`buttonBackgroundDestructive`, `cornerRadius`, `iconSize`) defined in Task 1, used in Task 2. ✓
