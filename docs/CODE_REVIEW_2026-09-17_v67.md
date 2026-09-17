# Код-ревью xvideos — проход 88

> **Срез:** `94e4c62` + рабочее дерево · **Статус:** закрыт полностью · **Индекс:** [все документы](README.md)

База прохода `94e4c62`. Затронуты модули `:core`, `:feature-l`, `:feature-r`, `:feature-x`, удалены мёртвые настройки, усилена безопасность путей файловой системы и расширено тестовое покрытие.

Линзы:
- `C` / Исправление некорректных сообщений об ошибках в снекбарах (группы $\rightarrow$ альбомы) в `SavedL_Albums` ([SavedL_Albums.kt](../feature-l/src/main/java/com/client/xvideos/l/featured/saved/SavedL_Albums.kt)).
- `C` / Устранение избыточного двойного переключения контекста `Dispatchers.Main` в `SavedX_Favorites.refresh()` ([SavedX_Favorites.kt](../feature-x/src/main/java/com/client/xvideos/x/feature/saved/SavedX_Favorites.kt)).
- `UI` / Устранение однобуквенных имён `val l` для списков иконок табов в пользу `EXPLORER_TAB_ICONS` и `SAVED_TAB_ICONS` ([ScreenExplorer.kt](../feature-r/src/main/java/com/client/xvideos/r/ui/explorer/ScreenExplorer.kt), [ScreenSaved.kt](../feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/saved/ScreenSaved.kt), [ScreenSaved.kt](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/saved/ScreenSaved.kt)).
- `C` / Защита файловой системы от внедрения null-байтов (`\u0000`) в `SafePath` ([SafePath.kt](../core/src/main/java/com/client/xvideos/common/io/SafePath.kt), [SafePathTest.kt](../core/src/test/java/com/client/xvideos/common/io/SafePathTest.kt)).
- `UI` / Очистка KDoc от устаревшей внешней ссылки и отладочного префикса `!!!` в `DashboardsPaginatedListScreen` ([DashboardsPaginatedListScreen.kt](../feature-x/src/main/java/com/client/xvideos/x/screens/dashboards/DashboardsPaginatedListScreen.kt)).
- `A` / Удаление неиспользуемой настройки `current_count_gifTab` в `Settings` ([Settings.kt](../core/src/main/java/com/client/xvideos/common/settings/Settings.kt)).
- `T` / Добавление тестов валидации альбомов Luscious и дедупликации ниш RedGifs ([SavedL_AlbumsTest.kt](../feature-l/src/test/java/com/client/xvideos/l/featured/saved/SavedL_AlbumsTest.kt), [RSavedNichesTest.kt](../feature-r/src/test/java/com/client/xvideos/r/RSavedNichesTest.kt)).

Граница P2P (`core/.../p2p/`) заморожена по решению владельца от 11.09.2026 и не затрагивалась.

---

## Находки

### C141 — Некорректный текст снекбаров «Ошибка добавления группы» при работе с альбомами L. Средняя.

[feature-l/src/main/java/com/client/xvideos/l/featured/saved/SavedL_Albums.kt:45,75,92](../feature-l/src/main/java/com/client/xvideos/l/featured/saved/SavedL_Albums.kt#L45)

При успешном добавлении и удалении альбома `SavedL_Albums` выводил корректные сообщения `"Альбом сохранен"` и `"Альбом удален"`. Однако в блоках `.onFailure` из-за копипаста выводились ошибки:
`SnackBar.error("Ошибка добавления группы ${e.message}")` и `SnackBar.error("Ошибка удаления группы ${e.message}")`. В модуле Luscious сущностей «групп» нет — это приводило в замешательство пользователя при сетевых сбоях или ошибках дисковой базы.

**Исправление:**
- Сообщения об ошибках исправлены на `"Ошибка добавления альбома ${e.message}"` и `"Ошибка удаления альбома ${e.message}"`.

---

### C142 — Избыточный двойной переход на Dispatchers.Main в SavedX_Favorites.refresh(). Низкая.

[feature-x/src/main/java/com/client/xvideos/x/feature/saved/SavedX_Favorites.kt:88-92](../feature-x/src/main/java/com/client/xvideos/x/feature/saved/SavedX_Favorites.kt#L88)

В методе `refresh()` корутина на `Dispatchers.IO` последовательно дважды переключалась на главный поток:
```kotlin
val ids = withContext(Dispatchers.Main) { list.map { it.id } }
withContext(Dispatchers.Main) {
    favoriteIds.clear()
    favoriteIds.addAll(ids)
}
```
Это порождало два подряд планирования задач в очереди `Looper.getMainLooper()`, ненужные переключения потоков и задержку синхронизации кэша идентификаторов.

**Исправление:**
- Вызовы объединены в один блок `withContext(Dispatchers.Main)`:
```kotlin
withContext(Dispatchers.Main) {
    favoriteIds.clear()
    favoriteIds.addAll(list.map { it.id })
}
```

---

### UI115 — Однобуквенные неинформативные имена свойств списков иконок в ScreenExplorer и ScreenSaved. Низкая.

[feature-r/src/main/java/com/client/xvideos/r/ui/explorer/ScreenExplorer.kt:46](../feature-r/src/main/java/com/client/xvideos/r/ui/explorer/ScreenExplorer.kt#L46)
[feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/saved/ScreenSaved.kt:57](../feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/saved/ScreenSaved.kt#L57)
[feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/saved/ScreenSaved.kt:49](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/saved/ScreenSaved.kt#L49)

В экранах навигации списки иконок объявлялись как публичные или приватные поля с однобуквенным именем `val l = persistentListOf(...)`. Это нарушало соглашения об именовании констант и ухудшало читаемость UI-дерева.

**Исправление:**
- В `feature-r` `ScreenExplorer` свойство переименовано в `private val EXPLORER_TAB_ICONS`.
- В `feature-r` и `feature-l` `ScreenSaved` свойства инкапсулированы и переименованы в `private val SAVED_TAB_ICONS`.

---

### C143 — Отсутствие защиты от null-байтов (\u0000) в SafePath. Средняя.

[core/src/main/java/com/client/xvideos/common/io/SafePath.kt:27,67](../core/src/main/java/com/client/xvideos/common/io/SafePath.kt#L27)

Функции `normalizeRelativePath` и `isUnsafeItemName` проверяли двоеточия, точки и слэши, но не проверяли наличие null-байта `\u0000`. В POSIX-системах, Linux/Android и на уровне нативного слоя JVM null-байт приводит к усечению строки пути (Null Byte Injection), что может вызывать обход ограничений или неожиданные исключения `IllegalArgumentException`/`IOException` в системных вызовах.

**Исправление:**
- В `normalizeRelativePath` добавлено обязательное условие: `require(!name.contains(':') && !name.contains('\u0000'))`.
- В `isUnsafeItemName` добавлено условие: `|| name.contains('\u0000')`.
- В `SafePathTest` добавлены тесты на гарантированное отклонение путей и имён файлов, содержащих `\u0000`.

---

### UI116 — Остаточная внешняя ссылка в KDoc и префикс логов в DashboardsPaginatedListScreen. Низкая.

[feature-x/src/main/java/com/client/xvideos/x/screens/dashboards/DashboardsPaginatedListScreen.kt:65,80](../feature-x/src/main/java/com/client/xvideos/x/screens/dashboards/DashboardsPaginatedListScreen.kt#L65)

1. В KDoc компонента `DashboardsPaginatedListScreen` присутствовала отладочная ссылка `![Логотип Markdown](https://ah-img.luscious.net/...)` на сторонний ресурс.
2. В логе `Timber.i` присутствовал отладочный восклицательный префикс `"!!! openNew ..."`.

**Исправление:**
- KDoc заменён на лаконичное описание: `/** Экран страницы пагинированного списка видео дашборда (Best, Top Rated, Newest). */`.
- Логирование очищено: `Timber.i("openNew numberScreen:$numberScreen url:$url")`.

---

### A24 — Мёртвое неиспользуемое свойство настройки current_count_gifTab. Низкая.

[core/src/main/java/com/client/xvideos/common/settings/Settings.kt:179](../core/src/main/java/com/client/xvideos/common/settings/Settings.kt#L179)

Свойство `current_count_gifTab by lazy { SettingElementInt(pref, "current_count_gifTab", 2) }` не имело ни одного использования в кодовой базе. Реальная настройка сетки таба GIF в RedGifs называется `r_explorerGifsTab_column_current_count`.

**Исправление:**
- Мёртвое свойство удалено из `Settings.kt`.

---

### T49 — Тестовое покрытие альбомов L, ниш R и путей SafePath. Низкая.

[feature-l/src/test/java/com/client/xvideos/l/featured/saved/SavedL_AlbumsTest.kt](../feature-l/src/test/java/com/client/xvideos/l/featured/saved/SavedL_AlbumsTest.kt)
[feature-r/src/test/java/com/client/xvideos/r/RSavedNichesTest.kt](../feature-r/src/test/java/com/client/xvideos/r/RSavedNichesTest.kt)
[core/src/test/java/com/client/xvideos/common/io/SafePathTest.kt](../core/src/test/java/com/client/xvideos/common/io/SafePathTest.kt)

Добавлены модульные тесты:
- `SavedL_AlbumsTest`: валидация строкового id альбома (отклонение пустых строк, нечисловых символов, чисел с плавающей точкой; корректное распознавание `Long`), идемпотентная замена при сохранении, удаление альбомов по id.
- `RSavedNichesTest`: идемпотентное добавление ниши без дублирования, удаление ниш по id, проверка неизменности списка при попытке удаления несуществующей записи, регистронезависимый поиск по названию.
- `SafePathTest`: расширение тестов на отклонение `\u0000` в `normalizeRelativePath` и `isUnsafeItemName`.

---

## Статус

| Находка | Класс | Статус | Детали |
| --- | --- | --- | --- |
| C141 | корректность | закрыт | Исправление текста ошибок снекбаров в `SavedL_Albums` |
| C142 | производительность | закрыт | Объединение `withContext(Dispatchers.Main)` в `SavedX_Favorites` |
| UI115 | чистота кода | закрыт | Переименование `val l` в `EXPLORER_TAB_ICONS` / `SAVED_TAB_ICONS` |
| C143 | безопасность | закрыт | Отклонение null-байтов (`\u0000`) в `SafePath` |
| UI116 | чистота кода | закрыт | Очистка KDoc и логов в `DashboardsPaginatedListScreen` |
| A24 | чистота архитектуры | закрыт | Удаление мертвого свойства `current_count_gifTab` в `Settings` |
| T49 | тесты | закрыт | Тесты `SavedL_AlbumsTest`, `RSavedNichesTest`, `SafePathTest` |

---

## Проверка

```
.\gradlew.bat app:detekt core:detekt feature-l:detekt feature-r:detekt feature-x:detekt
BUILD SUCCESSFUL in 11s (12 actionable tasks)

.\gradlew.bat testDebugUnitTest
BUILD SUCCESSFUL in 53s (140 actionable tasks)

.\gradlew.bat compileReleaseKotlin
BUILD SUCCESSFUL in 33s (61 actionable tasks)
```
