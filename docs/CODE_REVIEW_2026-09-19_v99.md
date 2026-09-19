# Код-ревью: Итерация 120 (2026-09-19)

**Фокус ревью:** `:feature-l`, `:feature-x`, `:app`, `:core` (гейты качества Detekt, синхронизация состояния серверных лайков Luscious, точность бейджа завершения истории X, безопасность и жизненный цикл диалогов бэкапа, потокобезопасность FileDB, миграция Material 3, модульные тесты).

---

## 1. Контекст и цели
1. Устранить нарушения Detekt в модуле `:feature-l` (`CyclomaticComplexMethod`, `LongMethod`, `ComplexCondition`), возникшие после реализации серверных подписок и лайков.
2. Ликвидировать баг рассинхронизации состояния [C34] в `ScreenLServerLikesSM`: при снятии серверного лайка картинка удалялась только из `filteredPic`, но оставалась в `_pictures`, из-за чего восстанавливалась при подгрузке следующих страниц (`loadNextPage()`).
3. Исправить ложное отображение бейджа «Просмотрено» [C36] в истории X: короткие ролики (< 2 мин) или случайные открытия на 2-3 секунды получали `lastPositionMs = 0L` и ошибочно помечались как полностью просмотренные.
4. Устранить уязвимости и сбои жизненного цикла в разделе бэкапов `:app` ([C33], [S12], [T43]): сохранение паролей в saved state Bundle через `rememberSaveable`, потеря URI архива при повороте экрана, отсутствие гарантированного сброса `isWorking = false` и обнуления паролей в `finally`.
5. Обеспечить корректное переключение на `Dispatchers.Main` [T44] при мутации Compose-состояния в `ScreenLAlbumSM.toggleServerFavorite` и гарантировать сброс флага загрузки.
6. Привести имя файла [C35] `LusciousServerFavoritesRepositoryStub.kt` в соответствие с его содержимым (`LusciousServerFavoritesRepositoryImpl.kt`).
7. Реализовать потокобезопасный метод `clear()` в [FileDB.kt](file:///g:/xvideos2/core/src/main/java/com/client/xvideos/common/fileDB/FileDB.kt) [T45] под защитой `lock` и перевести `SavedX_History.clearAll()` на его использование.
8. Завершить стандартизацию UI [UI39] в [ScreenXHistory.kt](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/screens/history/ScreenXHistory.kt), устранив устаревшие зависимости `androidx.compose.material.*`.
9. Разработать исчерпывающие модульные тесты для всех исправленных сценариев.

---

## 2. Выявленные замечания и исправления

### [A18] Quality gate Detekt в `:feature-l`
- **Проблема:** В [ExpandMenuVM.kt](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/ui/element/expandMenu/ExpandMenuVM.kt) метод `resolveAndPerformServerAction` превысил лимит цикломатической сложности (`CyclomaticComplexMethod` > 20). В [L_ScreenServerLikesTab.kt](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/saved/serverLikes/L_ScreenServerLikesTab.kt) и [L_ScreenSubscribedAlbumsTab.kt](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/saved/subscribedAlbums/L_ScreenSubscribedAlbumsTab.kt) функция `Content` превышала 120 строк (`LongMethod`) и содержала сложные предикаты (`ComplexCondition` > 3).
- **Решение:**
  - Логика поиска метаданных и кэширования `pictureId` вынесена из `resolveAndPerformServerAction` в приватные функции `findLocalFolderAndPictureId` и `cacheResolvedPictureId`.
  - Из `Content` экранов вкладок вынесены подкомпоненты `ServerLikesEmptyOrErrorState`, `SubscribedAlbumUnlikeDialog`, `SubscribedAlbumsEmptyOrErrorState` и `SubscribedAlbumsGrid`.
  - Условия автоподгрузки пагинации декомпозированы в простые именованные булевы переменные.
- **Статус:** Исправлено.

### [C34] Рассинхронизация состояния при удалении лайка на сервере Luscious
- **Проблема:** В [ExpandMenuVM.kt](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/ui/element/expandMenu/ExpandMenuVM.kt) при удалении лайка элемент удалялся только из списка хоста `host.filteredPic`. Поток `ScreenLServerLikesSM._pictures` оставался без изменений. При последующем вызове `loadNextPage()` объединение `_pictures.value + list` заново возвращало удалённый элемент в список.
- **Решение:**
  - В [LazyRowPictureDetailsHost.kt](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/ui/element/lazyRowPictureDetails/LazyRowPictureDetailsHost.kt) добавлен коллбэк `var onItemRemoved: ((PicsDetails) -> Unit)?` и метод `removePicture(picture: PicsDetails): Boolean`, очищающий также `selectedImage`.
  - В [ScreenLServerLikesSM.kt](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/saved/serverLikes/ScreenLServerLikesSM.kt) зарегистрирован слушатель `host.onItemRemoved`, фильтрующий `_pictures.value` по ID и ключу выбора, а также добавлен метод `unlikePicture(pic)`.
  - В [ExpandMenuVM.kt](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/ui/element/expandMenu/ExpandMenuVM.kt) удаление переведено на `host?.removePicture(it1)`.
  - Добавлен юнит-тест в `LusciousServerFavoritesTest`, подтверждающий, что пагинация не восстанавливает удалённый лайк.
- **Статус:** Исправлено.

### [C36] Ложный бейдж «Просмотрено» в истории X
- **Проблема:** В [SavedX_History.kt](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/feature/saved/SavedX_History.kt) значение `targetPosition = 0L` выставлялось не только для досмотренных до конца роликов (>=95%), но и для коротких роликов (< 2 мин) и при раннем закрытии (< 5 сек). В [ScreenXHistory.kt](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/screens/history/ScreenXHistory.kt) проверка `totalDurationMs > 0L && lastPositionMs == 0L` ошибочно вешала бейдж «ПРОСМОТРЕНО» на видео, открытые на пару секунд.
- **Решение:**
  - В [XHistoryItem.kt](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/model/XHistoryItem.kt) добавлено сериализуемое поле `val isCompleted: Boolean = false`, уточнены вычисления `isEligibleForResume` и `progressFraction`.
  - В `SavedX_History.updateProgress` флаг `isCompleted` устанавливается только при реальном достижении порога 95% длительности (`isFinished`), с сохранением статуса при случайном коротком повторном открытии (< 5 сек).
  - В `ScreenXHistory.kt` бейдж отображается строго по `if (historyItem.isCompleted)`.
  - Добавлены модульные тесты в `XHistoryItemTest` и `SavedX_HistoryTest`.
- **Статус:** Исправлено.

### [C33, S12, T43] Безопасность, жизненный цикл и try-finally в бэкапах
- **Проблема:**
  - Пароли в [BackupComponents.kt](file:///g:/xvideos2/app/src/main/java/com/client/xvideos/screenSettings/backup/BackupComponents.kt) хранились в `rememberSaveable`, утекая в открытом виде в Bundle сохранённого состояния Android.
  - В [BackupSettingsSection.kt](file:///g:/xvideos2/app/src/main/java/com/client/xvideos/screenSettings/backup/BackupSettingsSection.kt) `showRestorePasswordDialog` сохранялся через `rememberSaveable`, а `pendingRestoreUri` и `restoreUri` сбрасывались в `null` при повороте экрана через `remember`.
  - Длительные операции с `isWorking = true` и паролями не имели блоков `try ... finally`, что при отмене корутин приводило к вечной блокировке UI и оставлению паролей в оперативной памяти.
- **Решение:**
  - В `BackupComponents.kt` пароли переведены на `remember` и принудительно стираются пустыми строками при выходе из диалогов (`onDismiss` / `onConfirm`).
  - В `BackupSettingsSection.kt` состояния `pendingRestoreUri`, `restoreUri` и `restorePasswordError` переведены на `rememberSaveable`.
  - Все корутины создания, проверки и восстановления бэкапов обёрнуты в `try ... finally` с гарантированным сбросом `isWorking = false` и обнулением `password?.fill('\u0000')`.
- **Статус:** Исправлено.

### [T44] Потокобезопасность Compose State в `ScreenLAlbumSM.toggleServerFavorite`
- **Проблема:** В [ScreenLAlbumSM.kt](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbum/ScreenLAlbumSM.kt) `scope.launch` запускался в `@ApplicationScope` (работающей на `Dispatchers.IO`), напрямую меняя переменные `mutableStateOf` (`isServerFavorite`, `isServerFavoriteLoading`) в фоновом потоке, а при исключении флаг загрузки не сбрасывался.
- **Решение:** Мутации состояний перенесены на `Dispatchers.Main`, а `isServerFavoriteLoading = false` вынесен в блок `finally`.
- **Статус:** Исправлено.

### [C35] Корректное имя файла репозитория Luscious
- **Проблема:** Файл с продакшн-реализацией репозитория назывался `LusciousServerFavoritesRepositoryStub.kt`, хотя внутри содержал класс `LusciousServerFavoritesRepositoryImpl`.
- **Решение:** Файл переименован с помощью `git mv` в [LusciousServerFavoritesRepositoryImpl.kt](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/repository/LusciousServerFavoritesRepositoryImpl.kt), документация спецификации обновлена.
- **Статус:** Исправлено.

### [T45, UI39] Потокобезопасный `clear()` в FileDB и миграция Material 3
- **Проблема:** Метод `clearAll()` в `SavedX_History` удалял файлы напрямую с диска в обход синхронизации `FileDB.lock`. В `ScreenXHistory.kt` оставались устаревшие импорты Material 2 (`Scaffold`, `Icon`, `IconButton`, `Text`).
- **Решение:**
  - В [FileDB.kt](file:///g:/xvideos2/core/src/main/java/com/client/xvideos/common/fileDB/FileDB.kt) добавлен синхронизированный по `lock` и `publishLock` метод `clear(): Result<Boolean>`.
  - `SavedX_History.clearAll()` переведён на вызов `historyDb.clear()`.
  - В `ScreenXHistory.kt` импорты заменены на `androidx.compose.material3.*`, параметр `Scaffold` заменён на `containerColor`.
  - Добавлен модульный тест `clear` в `FileDBTest`.
- **Статус:** Исправлено.

---

## 3. Сводная таблица находок

| ID | Модуль | Тип | Описание | Решение | Статус |
|:---|:---|:---|:---|:---|:---|
| **A18** | `:feature-l` | Качество | Нарушения Detekt (`CyclomaticComplexMethod`, `LongMethod`, `ComplexCondition`) | Декомпозиция функций `findLocalFolderAndPictureId`, вынос UI-компонентов | Исправлено |
| **C34** | `:feature-l` | Баг | Рассинхронизация состояния серверных лайков при unliking | `onItemRemoved` в `LazyRowPictureDetailsHost` и синхронизация `_pictures` | Исправлено |
| **C36** | `:feature-x` | Баг | Ложный бейдж «Просмотрено» на коротких и незавершённых видео | Введение явного флага `isCompleted: Boolean` в `XHistoryItem` и `SavedX_History` | Исправлено |
| **C33** | `:app` | Жизненный цикл | Сброс `pendingRestoreUri` при повороте экрана | Использование `rememberSaveable` для URI архива бэкапа | Исправлено |
| **S12** | `:app` | Безопасность | Утечка паролей бэкапа в Bundle сохранённого состояния | Перевод на `remember` с очисткой по `onDismiss`/`onConfirm` | Исправлено |
| **T43** | `:app` | Надёжность | Отсутствие `try-finally` при операциях бэкапа | Гарантированный сброс `isWorking` и обнуление `password?.fill('\u0000')` | Исправлено |
| **T44** | `:feature-l` | Потоки | Изменение Compose-состояния на фоновом потоке в `ScreenLAlbumSM` | Перевод мутаций на `Dispatchers.Main` и сброс флага в `finally` | Исправлено |
| **C35** | `:feature-l` | Чистота | Несоответствие имени файла `LusciousServerFavoritesRepositoryStub.kt` | Переименование файла в `LusciousServerFavoritesRepositoryImpl.kt` | Исправлено |
| **T45** | `:core` | Потоки | Удаление файлов истории мимо блокировок `FileDB` | Реализация метода `clear(): Result<Boolean>` в `FileDB` | Исправлено |
| **UI39**| `:feature-x` | UI | Устаревшие импорты Material 2 в `ScreenXHistory.kt` | Полная миграция на Material 3 (`Scaffold`, `containerColor`, `Text`, `Icon`) | Исправлено |

---

## 4. Результаты верификации

- **Статический анализ:** `./gradlew detekt` — **BUILD SUCCESSFUL** (0 ошибок и предупреждений во всех модулях `:app`, `:core`, `:feature-l`, `:feature-r`, `:feature-x`).
- **Модульные тесты:** `./gradlew testDebugUnitTest` — **BUILD SUCCESSFUL** (140 задач, все тесты успешно пройдены).
- **P2P-граница:** директория `core/.../p2p/` не модифицировалась, проектные инварианты сохранены.
