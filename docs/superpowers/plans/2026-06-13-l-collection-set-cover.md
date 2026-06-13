# L Collection Set-Cover Menu Item Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Пункт «Сделать обложкой» в выпадающем меню элемента коллекции L — вызывает существующий `setManualCover`.

**Architecture:** Новый element-компонент `DropdownMenuItem_SetCover` (по образцу `DropdownMenuItem_RemoveFromCollection`), вставлен в `SavedLikesItemExpandMenu` в ветку `if (isCollection)`. Бэкенд (`setManualCover` → `collection.json` + SnackBar + refresh) уже есть.

**Tech Stack:** Kotlin, Compose Material3, существующий `SavedL_Collection`.

**Spec:** `docs/superpowers/specs/2026-06-13-l-collection-set-cover-design.md`

**Структура файлов:**

| Файл | Роль |
|---|---|
| Create `app/src/main/java/com/client/xvideos/l/ui/element/expandMenu/element/DropdownMenuItem_SetCover.kt` | пункт меню |
| Modify `app/src/main/java/com/client/xvideos/l/ui/element/expandMenu/SavedLikesItemExpandMenu.kt` | вставка пункта |

Команды — из корня репо, Windows: `.\gradlew.bat`.

---

### Task 1: DropdownMenuItem_SetCover + проводка

Compose-меню юнит-тестами в проекте не покрыты — проверка компиляцией и смоуком (Task 2).

**Files:**
- Create: `app/src/main/java/com/client/xvideos/l/ui/element/expandMenu/element/DropdownMenuItem_SetCover.kt`
- Modify: `app/src/main/java/com/client/xvideos/l/ui/element/expandMenu/SavedLikesItemExpandMenu.kt`

- [ ] **Step 1: Create the menu item element**

Создать `DropdownMenuItem_SetCover.kt` (по образцу `DropdownMenuItem_RemoveFromCollection`):

```kotlin
package com.client.xvideos.l.ui.element.expandMenu.element

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.client.xvideos.common.theme.Theme.L.ExpandMenu.style
import com.client.xvideos.common.theme.Theme.L.ExpandMenu.tintColor
import com.client.xvideos.l.featured.saved.SavedL
import com.client.xvideos.l.model.PicsDetails

/**
 * Пункт «Сделать обложкой» для меню элемента коллекции.
 * Зовёт [SavedL_Collection.setManualCover] (использует currentCollectionName);
 * тот пишет collection.json, шлёт SnackBar и обновляет список коллекций.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownMenuItem_SetCover(item: PicsDetails? = null, savedL: SavedL? = null, onDismiss: () -> Unit) {
    DropdownMenuItem(
        leadingIcon = {
            Icon(Icons.Default.Wallpaper, contentDescription = "", tint = tintColor)
        },
        text = { Text("Сделать обложкой", style = style) },
        onClick = {
            if (item == null || savedL == null) {
                onDismiss()
                return@DropdownMenuItem
            }
            if (savedL.collection.currentCollectionName == null) {
                onDismiss()
                return@DropdownMenuItem
            }
            savedL.collection.setManualCover(item)
            onDismiss()
        }
    )
}
```

- [ ] **Step 2: Wire it into the collection item menu**

В `SavedLikesItemExpandMenu.kt` добавить импорт рядом с другими element-импортами:

```kotlin
import com.client.xvideos.l.ui.element.expandMenu.element.DropdownMenuItem_SetCover
```

В блоке `if (isCollection)`, после `DropdownMenuItem_RemoveFromCollection(...)`, добавить:

```kotlin
            if (isCollection) {
                DropdownMenuItem_RemoveFromCollection(item, onRemoveFromCollection, savedL) { expanded = false }
                DropdownMenuItem_SetCover(item, savedL) { expanded = false }
            }
```

(существующая строка `DropdownMenuItem_RemoveFromCollection` остаётся, добавляется только вторая строка внутри того же `if`.)

- [ ] **Step 3: Compile**

Run: `.\gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL (возможны deprecation-warnings от composeunstyled — не ошибки).

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/client/xvideos/l/ui/element/expandMenu/element/DropdownMenuItem_SetCover.kt app/src/main/java/com/client/xvideos/l/ui/element/expandMenu/SavedLikesItemExpandMenu.kt
git commit -m "feat(l): set-cover menu item in collection item dropdown"
```

---

### Task 2: Верификация

- [ ] **Step 1: Assemble debug**

Run: `.\gradlew.bat :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 2: Ручной смоук (чек-лист пользователю)**

1. Открыть коллекцию → у элемента меню (⋮) → «Сделать обложкой» → SnackBar «Обложка коллекции обновлена».
2. Вернуться к списку коллекций → обложка = выбранный элемент.
3. Сменить обложку другим элементом → грид обновился.
4. Пункт «Сделать обложкой» отсутствует в меню элемента вне коллекции (Likes/Album).

- [ ] **Step 3: Verify clean tree**

```bash
git status
```
Expected: clean.
