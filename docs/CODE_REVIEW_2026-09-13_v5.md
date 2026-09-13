# Код-ревью xvideos — проход 20

> **Срез:** `master` · **Статус:** закрыт · **Индекс:** [все документы](README.md)

База прохода — `master` (коммит `0eb9521` после прохода 19).
Линзы:
- **Корректность формирования сетевых запросов и роутинга API** (`RedApi`);
- **Обработка сетевых сбоев, состояний загрузки и предотвращение блокировки навигации в видеоплеерах** (`ScreenX_VideoPlayerFullScreen`, `ScreenX_VideoPlayer`, `ScreenX_VideoPlayerFullScreenSM`, `ScreenX_VideoPlayerSM`);
- **Устойчивость UI к сбоям загрузки пагинации и предоставление повтора запросов** (`DashboardsPaginatedListScreen`);
- **Защита от деления на ноль и аварийного завершения интерфейса при недопустимых размерах медиа** (`L_LazyRowPictureDetails`, `LFullScreenPage`, `PicsDetails`);
- **Потокобезопасность Compose State и изоляция асинхронного ввода-вывода резервного копирования** (`BackupSettingsSection`);
- **Архитектурная чистота DI-модулей и устранение устаревшего неиспользуемого состояния** (`ScreenX_VideoPlayerFullScreenSM`, `ScreenSavedCollectionSM`).

Ключевой итог: **все 6 находок 20-го прохода устранены**. Репозиторий полностью собирается, detekt чист (0 замечаний), все unit-тесты успешно проходят.

---

## Находки

### C39 — Искажение параметров запроса к RedGifs API из-за синтаксической ошибки в URL. Средняя.

[RedApi.kt:194](../feature-r/src/main/java/com/client/xvideos/r/network/api/RedApi.kt#L194)

В функции поиска креаторов `searchCreator` при формировании пути маршрута для `MediaType.ALL` с тегами:
```kotlin
path = "/v2/users/{username}/search?order={order}&page={page}&count={count}&&tags={tags}"
```
содержался двойной символ амперсанда (`&&tags={tags}`).
При интерполяции параметров роутером в Ktor/OkHttp формировался URL вида `...&count=100&&tags=tag1,tag2`. Парсеры query-параметров на бэкенде воспринимают `&&` как наличие безымянного пустого параметра, что приводило к сбою обработки параметров запроса и некорректной фильтрации результатов на стороне RedGifs API.

*Исправление:*
- Двойной амперсанд исправлен на корректный разделитель параметров: `&tags={tags}`.

---

### C40 / UI13 — Бесконечный спиннер и заблокированная навигация в полноэкранном и обычном плеере X при ошибках загрузки. Высокая.

[ScreenX_VideoPlayerFullScreen.kt:59-135](../feature-x/src/main/java/com/client/xvideos/x/screens/videoplayerFullScreen/ScreenX_VideoPlayerFullScreen.kt#L59-L135), [ScreenX_VideoPlayerFullScreenSM.kt:50-85](../feature-x/src/main/java/com/client/xvideos/x/screens/videoplayerFullScreen/ScreenX_VideoPlayerFullScreenSM.kt#L50-L85), [ScreenX_VideoPlayer.kt:35-50](../feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/ScreenX_VideoPlayer.kt#L35-L50), [ScreenX_VideoPlayerSM.kt:60-120](../feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/ScreenX_VideoPlayerSM.kt#L60-L120)

1. В `ScreenX_VideoPlayerFullScreenSM` и `ScreenX_VideoPlayerSM` при отказе сети или ошибке парсинга HLS-ссылка оставалась пустой (`passedString = ""` / `passedHLS = ""`), но статус ошибки не выставлялся.
2. В интерфейсе плееров проверка `if (vm.passedString == "") { CircularProgressIndicator(); return }` оставляла пользователя перед бесконечно вращающимся индикатором загрузки без какого-либо сообщения об ошибке и без возможности повторной загрузки.
3. В полноэкранном плеере досрочный `return` выполнялся **до** инициализации `DisposableEffect(Unit)` (управление ландшафтной ориентацией и скрытием системных панелей) и **до** регистрации `BackHandler { exit() }`. В случае сбоя сети пользователь оказывался заперт в пустом экране без системных кнопок навигации и без уведомления `EventBus` о закрытии.

*Исправление:*
- В `ScreenX_VideoPlayerFullScreenSM` и `ScreenX_VideoPlayerSM` добавлены состояния `isError`, `isLoading` и метод перезапуска `loadVideo()`, корректно обрабатывающий ошибки с установкой флага `isError = true`.
- В `ScreenX_VideoPlayerFullScreen` настройка альбомной ориентации, системных insets и обработчик кнопки «Назад» (`BackHandler`) вынесены в начало Composable перед ветвлением по состоянию загрузки.
- При возникновении ошибки на обоих экранах плеера отображается информативная плашка «Не удалось загрузить видео», кнопка «Повторить» (`vm.loadVideo()`) и кнопка выхода «Назад» (`exit()`).

---

### UI14 — Вечная загрузка без возможности повтора при сбое получения дашбордов X. Средняя.

[DashboardsPaginatedListScreen.kt:89-115](../feature-x/src/main/java/com/client/xvideos/x/screens/dashboards/DashboardsPaginatedListScreen.kt#L89-L115)

При открытии страниц дашбордов в случае сбоя сети или ошибки парсинга в `openNew(pageIndex)` выводился всплывающий `SnackBar.error`, однако список элементов `l` оставался пустым. Проверка `if (l.isEmpty()) CircularProgressIndicator(...)` приводила к тому, что после скрытия снекбара экран навсегда оставался с вращающимся спиннером в центре без объяснения причины и без элемента интерфейса для повторной попытки загрузки.

*Исправление:*
- Добавлено состояние ошибки `hasError` и счетчик-триггер перезапуска `retryTrigger`.
- При неудачной загрузке и пустом списке отображается сообщение «Ошибка загрузки видео» с кнопкой «Повторить», перезапускающей `LaunchedEffect`.

---

### C41 — Фатальное падение приложения (`IllegalArgumentException`) при нулевых размерах медиа в Luscious. Высокая.

[L_LazyRowPictureDetails.kt:157](../feature-l/src/main/java/com/client/xvideos/l/ui/element/lazyRowPictureDetails/L_LazyRowPictureDetails.kt#L157), [LFullScreenPage.kt:86-90](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenFullScreen/LFullScreenPage.kt#L86-L90)

В модели `PicsDetails` поля `width` и `height` имеют значения по умолчанию `0`.
При открытии картинок или видео с нулевыми размерами (при отсутствии метаданных на бэкенде либо повреждённых локальных файлах):
1. В `L_LazyRowPictureDetails.kt`: `val aspect = item.width.toFloat() / item.height` приводил к делению на ноль и давал `Float.POSITIVE_INFINITY` (или `Float.NaN`).
2. В `LFullScreenPage.kt`: расчёт `if (rotate) pageItem.height.toFloat() / pageItem.width else pageItem.width.toFloat() / pageItem.height` также приводил к делению на ноль.
3. В Compose реализация `Modifier.aspectRatio(ratio)` содержит строгую проверку: `require(ratio > 0) { "aspectRatio $ratio must be greater than zero" }`. Значения `Infinity`, `NaN` или `<= 0` немедленно приводили к неперехватываемому `IllegalArgumentException` на UI-потоке и падению процесса всего приложения.

*Исправление:*
- Добавлена строгая проверка положительности обоих измерений: `if (width > 0 && height > 0) width.toFloat() / height else 1f`.
- Написан юнит-тест в `LPureFunctionsTest`.

---

### T26 — Мутация Compose State на пуле `Dispatchers.IO` в секции резервного копирования. Средняя.

[BackupSettingsSection.kt:105-150, 295-360](../app/src/main/java/com/client/xvideos/screenSettings/backup/BackupSettingsSection.kt#L105-L150)

Функция `rememberApplicationScope()` предоставляет `CoroutineScope(SupervisorJob() + Dispatchers.IO)`. В `BackupSettingsSection.kt` запуск корутин через `scope.launch` напрямую приводил к выполнению кода на пуле потоков `Dispatchers.IO`. Из этого пула выполнялась прямая мутация состояний Jetpack Compose:
- `isWorking = true / false` (`MutableState<Boolean>`);
- `appendBackupLog` с мутацией `backupConsole` (`SnapshotStateList<String>`);
- `backupItems = items`, `restoreUri = uri`, `restoreItems = items`, `selectedRestorePaths = ...`.
Изменение снимка состояния Compose из фоновых потоков нарушает модель потокобезопасности Compose Snapshot State и чревато состоянием гонки, сбоями рекомпозиции и падениями во время применения снапшотов.

*Исправление:*
- Запуск корутин, управляющих состоянием интерфейса, переведён на `scope.launch(Dispatchers.Main)`.
- Синхронные блоки создания/чтения/восстановления архивов изолированы на пуле ввода-вывода через `withContext(Dispatchers.IO)`.

---

### A10 — Коллизия простых имён классов Dagger-модулей и мёртвый кэш в коллекциях. Низкая.

[ScreenX_VideoPlayerFullScreenSM.kt:88](../feature-x/src/main/java/com/client/xvideos/x/screens/videoplayerFullScreen/ScreenX_VideoPlayerFullScreenSM.kt#L88), [ScreenSavedCollectionSM.kt:40-65](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/saved/collection/ScreenSavedCollectionSM.kt#L40-L65)

1. Класс `@Module abstract class ScreenModuleItem` в пакете полноэкранного плеера дублировал простое имя аналогичного модуля в обычном плеере `ScreenX_VideoPlayerSM.kt`. Это нарушало соглашение об уникальности и читаемости имен модулей Dagger/Hilt.
2. В `ScreenSavedCollectionSM.kt` оставался неиспользуемый кэш `collectionHosts` и метод `hostFor`, потерявшие актуальность после выделения `ScreenLCollectionNameSM` с assisted-инъекцией.

*Исправление:*
- Модуль полноэкранного плеера переименован в `ScreenModuleItemFullScreen`.
- Неиспользуемый кэш и сопутствующие импорты удалены из `ScreenSavedCollectionSM`.

---

## Статус

| Находка | Класс | Статус | Коммит |
| --- | --- | --- | --- |
| C39 | корректность | закрыт | рабочее дерево (`master`) |
| C40 / UI13 | корректность / UI | закрыт | рабочее дерево (`master`) |
| UI14 | интерфейс | закрыт | рабочее дерево (`master`) |
| C41 | корректность | закрыт | рабочее дерево (`master`) |
| T26 | конкурентность | закрыт | рабочее дерево (`master`) |
| A10 | архитектура | закрыт | рабочее дерево (`master`) |

---

## Проверка

1. **Компиляция и Unit-тесты:**
```
./gradlew testDebugUnitTest --no-daemon
BUILD SUCCESSFUL in 15s (140 actionable tasks: 3 executed, 137 up-to-date)
```
Все тесты (150+) во всех модулях (`:app`, `:core`, `:feature-l`, `:feature-r`, `:feature-x`) успешно пройдены.

2. **Статический анализ Detekt:**
```
./gradlew detekt --no-daemon
BUILD SUCCESSFUL (0 issues found)
```
Все проверки code health и статического анализа соблюдены на 100%.

---

## Что осталось открытым

Все выявленные дефекты 20-го прохода устранены.
Транспортный слой P2P (`core/.../p2p/`) сохранён замороженным в строгом соответствии с решением владельца от 11.09.2026.
