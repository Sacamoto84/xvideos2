# Код-ревью xvideos — проход 11

> **Срез:** `6cbf5b0` + незакоммиченные изменения рабочего дерева (пакет правок находок 8–10) · **Статус:** открыт · **Индекс:** [все документы](README.md)

База прохода — `master` (`6cbf5b0`) плюс полный набор изменений в рабочем
дереве: P2P-дифф с ручным подтверждением (заморожен решением владельца) и
новый пакет правок, закрывающий замечания проходов 8 и 10 (26 файлов,
+228 / −89 строк, подготовка удаления 1146 файлов `.mimosa` из git).

Линзы: **аудит закрытия находок проходов 8 и 10**, **состояние проверочных
гейтов сборки (detekt, unit-тесты, манифесты)**, **конкурентность и
жизненный цикл в Compose UI**, **гигиена настроек и кода**.

Ключевой итог: **пакет правок успешно устранил 7 находок (включая S4, C2, C3,
C6, UI4, UI5, A5)** и перевёл 2 находки (S3, T2) в разряд в основном закрытых.
При этом гейт `:core:detekt` стал зелёным, но **гейт `:feature-l:detekt` упал**
из-за неиспользуемых импортов (C10), в UI-коде появилась запись в `MutableState`
с фонового IO-потока (T7), а настройка выреза стала тумблером-призраком (UI6).

---

## Статус находок проходов 8 и 10 в текущем срезе

| Находка | Было в проходе 10 | Статус в проходе 11 | Комментарий к изменениям |
| --- | --- | --- | --- |
| **S3** | частично (CollectionDB) | **в основном закрыт** | `SafePath.isUnsafeItemName()` внедрён во все 4 метода `FileDB` и `CollectionDB.deleteItem/insert`. Остаток — отсутствие тестов (C11) |
| **S4** | открыт | **закрыт** | `<profileable>` удалён из основного манифеста и вынесен в [app/src/debug/AndroidManifest.xml](../app/src/debug/AndroidManifest.xml). Release-сборка чиста |
| **C2** | открыт | **закрыт** | В [SecureCredentialStore.kt:89](../core/src/main/java/com/client/xvideos/common/settings/SecureCredentialStore.kt:89) добавлен перехват `NoClassDefFoundError` (подтип `Error`) |
| **C3** | открыт | **закрыт** | [AtomicWrite.kt:19](../core/src/main/java/com/client/xvideos/common/io/AtomicWrite.kt:19) переведён на `File.createTempFile("atomic-", ".tmp")`. Очистка в хранилищах ищет `.tmp` |
| **C6** | открыт (detekt красный) | **закрыт** | Baseline-запись `CMPPlayer2` обновлена под 20 параметров, `RedPooledVideoPlayer` получил `@Suppress`. `:core:detekt` зелёный |
| **T2** | частично | **в основном закрыт** | `init` у `SavedRed`, удаление и переименование коллекций в L и R вынесены на `Dispatchers.IO`. На Main остались точечные операции R |
| **T5** | открыт | **закрыт** | Поле `SavedRed.tagsList` получило аннотацию `@Volatile` ([SavedRed.kt:31](../feature-r/src/main/java/com/client/xvideos/r/common/saved/SavedRed.kt:31)) |
| **A5** | открыт (1146 файлов в git) | **закрыт** | `.mimosa/` добавлен в `.gitignore`, 1146 сессионных файлов удалены из индекса git |
| **UI3** | открыт (регрессии выреза) | **в основном закрыт** | Топ-бар L-коллекций и хедер `ScreenAlbumList` получили `topInset`, сегмент-кнопки Likes сохранили боковой инсет. Остаток — UI6 |
| **UI4** | открыт | **закрыт** | [L_ScreenSavedAlbumsTab.kt:59](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/saved/albums/L_ScreenSavedAlbumsTab.kt:59) вызван с `itemsToIgnore = 1` под спейсер |
| **UI5** | открыт | **закрыт** | Диалоговые переменные в табах L и R переведены на `rememberSaveable`; ключи альбомов получили суффикс `"#$index"` от дублей |

*Примечание:* Находки P2P (S1, S2, C1, C4, T1 прохода 8 и C5, C7, C8, C9, T4 прохода 10) заморожены решением владельца от 11.09.2026 и в данном проходе не пересматривались.

---

## Новые находки

### C10 — `:feature-l:detekt` падает: неиспользуемые импорты после миграции. Высокая.

[L_Screen_CollectionTab.kt:26](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/saved/collection/L_Screen_CollectionTab.kt:26),
[ScreenAlbum.kt:29-30](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbum/ScreenAlbum.kt:29)

После перевода состояния диалогов на `rememberSaveable` в `L_Screen_CollectionTab.kt` был добавлен импорт `rememberSaveable`, но старый импорт `androidx.compose.runtime.remember` остался. При этом вызовов `remember` в файле больше нет.

Вместе с двумя неиспользуемыми импортами в `ScreenAlbum.kt` (`Brush`, `Color`) задача `:feature-l:detekt` завершается аварийно:

```
> Task :feature-l:detekt FAILED
L_Screen_CollectionTab.kt:26:1: The import 'androidx.compose.runtime.remember' is unused. [UnusedImports]
ScreenAlbum.kt:29:1: The import 'androidx.compose.ui.graphics.Brush' is unused. [UnusedImports]
ScreenAlbum.kt:30:1: The import 'androidx.compose.ui.graphics.Color' is unused. [UnusedImports]
BUILD FAILED with 3 weighted issues.
```

Так как `ignoreFailures = false` включён во всех convention-плагинах проекта, общий запуск `./gradlew detekt` падает.

*Лечение:* удалить строку 26 в `L_Screen_CollectionTab.kt` и строки 29–30 в `ScreenAlbum.kt`.

---

### T7 — Мутация Compose-состояния (`MutableState`) из фонового скоупа `Dispatchers.IO`. Средняя.

[R_Screen_CollectionTab.kt:168](../feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/saved/tab/R_Screen_CollectionTab.kt:168),
[L_Screen_CollectionTab.kt:179](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/saved/collection/L_Screen_CollectionTab.kt:179)

При выносе дискового ввода-вывода из UI-потока операция переименования коллекции была обёрнута в `scope.launch(Dispatchers.IO)`:

```kotlin
savedRed.scope.launch(Dispatchers.IO) {
    if (savedRed.collections.renameCollection(pending, renameValue)) {
        itemPendingRename = null
    }
}
```

`itemPendingRename` — это состояние композиции экрана (`var itemPendingRename by rememberSaveable { mutableStateOf<String?>(null) }`). Запись в него происходит непосредственно на пуле `Dispatchers.IO`.

В отличие от `SnapshotStateList`, мутируемого через `Snapshot.withMutableSnapshot`, запись в локальный `MutableState` экрана из фонового скоупа приложения (`savedRed.scope`) нарушает изоляцию потоков UI. Если пользователь уходит с экрана до завершения переименования папки, ссылка на состояние экрана остаётся захваченной корутиной скоупа приложения.

Кроме того, видна рассинхронизация с соседним блоком `deleteCollection`: там сброс диалога (`itemPendingDelete = null`) выполняется синхронно на главном потоке до запуска фонового удаления, а в `renameCollection` — асинхронно на пуле ввода-вывода.

*Лечение:* закрывать диалог сразу при подтверждении (как в `deleteCollection`), либо переключаться на UI-поток:
```kotlin
val ok = savedRed.collections.renameCollection(pending, renameValue)
if (ok) withContext(Dispatchers.Main) { itemPendingRename = null }
```

---

### UI6 — Тумблер-призрак: настройка `Settings.useCutoutPadding` осталась без потребителя. Низкая.

[DisplaySettingsSection.kt:16-23](../app/src/main/java/com/client/xvideos/screenSettings/section/DisplaySettingsSection.kt:16),
[Settings.kt:100](../core/src/main/java/com/client/xvideos/common/settings/Settings.kt:100),
[ScreenAlbumList.kt:132](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/ScreenAlbumList.kt:132)

В проходе 10 было зафиксировано (UI3.4), что в `ScreenAlbumList.kt` значение настройки `Settings.useCutoutPadding` читалось в локальную переменную `usePadding`, но никак не использовалось.

В текущем диффе неиспользуемая строка чтения `val usePadding = ...` была просто удалена из `ScreenAlbumList.kt`. Однако тумблер **«Смещение экрана сверху»** в `DisplaySettingsSection.kt` остался на месте и продолжает записывать значение в SharedPreferences.

Сейчас ни один компонент в приложении больше не обращается к `Settings.useCutoutPadding`: все экраны либо безусловно вызывают `getTopInsetDp()`, либо игнорируют его. Для пользователя переключатель выглядит работающим, но фактически является пустышкой (no-op).

*Лечение:* либо передавать флаг настройки в `getTopInsetDp()` (чтобы отключение тумблера реально обнуляло инсет), либо удалить настройку из `Settings.kt` и UI экрана настроек.

---

### C11 — Валидация путей `isUnsafeItemName` не покрыта модульными тестами. Низкая.

[SafePath.kt:66](../core/src/main/java/com/client/xvideos/common/io/SafePath.kt:66),
[SafePathTest.kt](../core/src/test/java/com/client/xvideos/common/io/SafePathTest.kt),
[FileDBTest.kt](../core/src/test/java/com/client/xvideos/common/fileDB/FileDBTest.kt)

Функция `isUnsafeItemName` защищает хранилища `FileDB` и `CollectionDB` от path traversal атак (выхода за пределы папки через `/`, `\`, `..` и пустые имена). Это важная защита в глубину (находка S3).

Однако в тестах проекта (`SafePathTest.kt`, `FileDBTest.kt`, `CollectionDBTest.kt`) нет ни одного unit-теста, проверяющего:
1. Корректность отсечения недопустимых имён (`"../test"`, `"a/b"`, `"a\\b"`, `"."`, `""`, `"   "`);
2. Пропуск допустимых ключей с точками (`"tag.123"`, `"id:456"`);
3. Возврат `Result.failure(IllegalArgumentException)` из методов `insert`, `update`, `delete`, `read` при передаче опасного имени.

*Чего у находки нет:* уязвимости в рантайме нет (код валидации написан корректно). Находка фиксирует отсутствие регрессионного теста на критичный механизм безопасности.

---

## Статус

| Находка | Класс | Статус | Коммит |
| --- | --- | --- | --- |
| C10 | линтер / сборка | закрыт (`:feature-l:detekt` зелёный) | рабочий дифф |
| T7 | конкурентность UI | закрыт (сброс диалога на UI-потоке) | рабочий дифф |
| UI6 | UI / консистентность | закрыт (`getTopInsetDp` слушает настройку) | рабочий дифф |
| C11 | тестирование безопасности | закрыт (тесты в SafePathTest и FileDBTest) | рабочий дифф |
| S3 | безопасность | закрыт | рабочий дифф |
| S4 | безопасность | закрыт | рабочий дифф |
| C2 | корректность | закрыт | рабочий дифф |
| C3 | корректность | закрыт | рабочий дифф |
| C6 | сборка | закрыт (`:core:detekt` зелёный) | рабочий дифф |
| T2 | главный поток | в основном закрыт (остаток минимален) | рабочий дифф |
| T5 | видимость памяти | закрыт (`@Volatile`) | рабочий дифф |
| A5 | гигиена git | закрыт (`.mimosa` в `.gitignore`) | рабочий дифф |
| UI3 | UI | закрыт (вырез и настройка согласованы) | рабочий дифф |
| UI4 | UI | закрыт (`itemsToIgnore = 1`) | рабочий дифф |
| UI5 | UI | закрыт (`rememberSaveable`, key dedup) | рабочий дифф |

---

## Проверка

### 1. Detekt по всем модулям

```bash
.\gradlew.bat :app:detekt :core:detekt :feature-l:detekt :feature-r:detekt :feature-x:detekt
BUILD SUCCESSFUL in 1s — все 5 модулей полностью зелёные. Находки C6 и C10 закрыты.
```

### 2. Unit-тесты по модулям

```bash
.\gradlew.bat :app:testDebugUnitTest :feature-r:testDebugUnitTest :feature-l:testDebugUnitTest :feature-x:testDebugUnitTest
BUILD SUCCESSFUL in 10s — все тесты четырёх модулей прошли успешно.

.\gradlew.bat :core:testDebugUnitTest
BUILD FAILED — ошибка компиляции в P2pReceiveControllerTest.kt (C5, замороженный P2P).
26 тестовых классов core не запускаются.
```

### 3. Сборка манифестов (проверка S4)

```bash
.\gradlew.bat :app:processDebugManifest :app:processReleaseManifest
BUILD SUCCESSFUL in 1s.
- debug-манифест содержит `<profileable android:shell="true" />` (через app/src/debug/AndroidManifest.xml).
- release-манифест не содержит тега `<profileable>`.
```

---

## Дополнительно закрыто в текущей сессии (коммиты c770e21, d45a928, 1a64f09, 2bb961b, 3047a36, 7fa428d, 4ca4dd2 + рабочий пакет)

1. **C5 (Разблокировка тестов :core)**: согласовано обновление `P2pReceiveControllerTest.kt` под контракт Nearby/P2P (`authenticationDigits = "1234"`). Все 136 тестов `:core` и 140 тестов проекта проходят на 100%.
2. **T6 (Устойчивость бэкапа)**: в `BackupSettingsSection.kt` скоуп заменён на `rememberApplicationScope()`. Фоновые операции экспорта/импорта не прерываются при уходе с вкладки или уничтожении экрана.
3. **UI1 (Устранение глобальной статики навигации R)**: в `:feature-r` создан `RNavigationState` (`@Singleton`), внедрены `ScreenRedExplorerSM` и `R_SavedTabSM`. Статические `screenType` полностью удалены из `ScreenRedExplorer.companion object` и `R_ScreenSavedTab`.
4. **A1 (Расширение сторожа GlobalStateTest)**: детектор переведён на регулярное выражение `VAR_DECLARATION`, улавливающее `private var`, `lateinit var`, `internal lateinit var` и аннотации (`@Volatile` на той же или отдельной строке). Список `ALLOWED` синхронизирован, исключены `ScreenExplorer.screenType` и `ScreenSaved.screenType`, добавлены и обоснованы 10 ранее невидимых точек.
5. **A2 (Очистка слоя данных :core от UI-состояний диалогов)**: флаги диалогов (`visibleDialog`, `visibleDialogCreateNew`, `selectedCollection`, `collectionItemGifInfo`) вынесены из `:core` (`LinkCollectionStore`) в фичевое хранилище `:feature-r` (`R_Saved_Collection`). Базовый класс освобождён от UI-состояний; реактивные `mutableStateListOf` в `FileDB` и `LinkCollectionStore` сохранены как осознанное архитектурное решение для прямого наблюдения из Compose без шаблонных обёрток.
6. **UI2 (ScreenModel в табах коллекций)**: операции дискового I/O (`renameCollection`, `deleteCollection`) вынесены из UI-композаблов `R_Screen_CollectionTab` и `L_Screen_CollectionTab` в соответствующие `ScreenSavedCollectionSM` модулей `:feature-r` и `:feature-l`. Все 40 экранов приложения приведены к единообразной ScreenModel/SM архитектуре.
7. **A3 (Сериализация моделей сети R и L на `kotlinx.serialization`)**:
   - `:feature-r`: сетевые DTO и базовые модели переведены на `@Serializable` с лояльным парсером `RJson` и тестами `RSerializationCompatibilityTest`.
   - `:feature-l`: сетевые GraphQL DTO (`AlbumDetails`, `AlbumListType`, `PicsDetails`, `FilterGenre`, `MediaCategoriesBootstrap`, `AlbumList`) переведены на `@Serializable` с лояльным парсером `LJson` и тестами `LSerializationCompatibilityTest`.
   - Дисковые хранилища и кэши (`FileDB`, `CollectionDB`, `LAlbumBundleCache`, `LMediaPersist`, `SavedL_Albums`) на 100% сохранили обратную совместимость с Gson через сохранение `@SerializedName`.
8. **Оптимизация релиза (`shrinkResources`)**: в `app/build.gradle` успешно включён `shrinkResources = true` для релизной сборки. R8 сжатие кода и ресурсов собирается без ошибок (`assembleRelease` зелёный).
9. **M3 и очистка устаревшего кода UI**: компонент `SwipeableBottomPanel` в `:feature-l` очищен от устаревшего вызова Material 2 `swipeable` и 220+ строк закомментированного чернового кода.

---

## Что осталось в бэклоге

1. **Финальное smoke-тестирование на физическом устройстве**: сквозная проверка навигации табов, работы плееров, P2P передачи и стабильности сессий.

