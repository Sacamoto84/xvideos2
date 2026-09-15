# Код-ревью xvideos — проход 36

> **Срез:** `a47bbf1` + рабочее дерево · **Статус:** закрыт · **Индекс:** [все документы](README.md)

База прохода `a47bbf12e2e6739b9afba200becf86ed227e1b96`, 10 изменённых файлов, 8 новых файлов в модуле `feature-l`.
Линзы: архитектура и гейты качества (`A`), корректность и производительность (`C`), безопасность данных (`S`), конкурентность (`T`), UI и Compose-гигиена (`UI`). Граница P2P (`core/.../p2p/`) заморожена по решению владельца и не затрагивалась.

## Находки

### A17 — Падение GlobalStateTest из-за изменяемого состояния `var isInitialized` в AlbumFilterPresetManager. Высокая.

[feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/molecule/filter/atom/AlbumFilterPresetManager.kt:28](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/molecule/filter/atom/AlbumFilterPresetManager.kt#L28)

Архитектурный тест `:app:testDebugUnitTest --tests com.client.xvideos.arch.GlobalStateTest` проверяет чистоту синглтонов и глобальных объектов от незащищённого мутабельного состояния (`var`). В `AlbumFilterPresetManager` использовалась незащищённая мутабельная переменная `var isInitialized = false`, что приводило к сбою тестового гейта CI/CD и потенциальному состоянию гонки при многопоточной инициализации из разных экранов.
**Исправление:** Переведено на потокобезопасный `AtomicBoolean(false)` с методом `compareAndSet(false, true)`. Тест `GlobalStateTest` и все тесты модулей проходят успешно.

### C78 — Аллокации памяти в побайтовых потоках `XlrEncryptedInputStream`/`XlrEncryptedOutputStream`, потеря причин исключений и дублирование логики магии бэкапа. Средняя.

[core/src/main/java/com/client/xvideos/common/backup/XlrChunkedCrypto.kt:151](file:///g:/xvideos2/core/src/main/java/com/client/xvideos/common/backup/XlrChunkedCrypto.kt#L151)

1. В методах `XlrEncryptedInputStream.read()` и `XlrEncryptedOutputStream.write(b: Int)` для чтения/записи одиночного байта на каждый вызов аллоцировался временный массив `ByteArray(1)`. При потоковой передаче больших объёмов данных это создавало избыточное давление на сборщик мусора (GC churn).
2. Пользовательские исключения `XlrInvalidPasswordException` и `XlrCorruptedBackupException` не поддерживали передачу исходного `cause: Throwable?`, из-за чего скрывались низкоуровневые причины ошибок криптографии (AEADBadTagException и др.).
3. В методе `detectType` дублировалась 4-байтовая проверка заголовка.
**Исправление:** 
- `read()` считывает байт напрямую из буфера `decryptedChunk` с инкрементом смещения.
- `write(b: Int)` записывает байт напрямую в буфер `buffer[bufferOffset++]` с вызовом `flushChunk()` при заполнении.
- В исключения добавлен параметр `cause: Throwable? = null` с передачей пойманных ошибок.
- Выделен хелпер `matchesMagic` для устранения дублирования. Константа шифрования вынесена в `const val CIPHER_ALGORITHM`.

### S11 — Хранение чувствительных паролей бэкапа в памяти без обнуления в жизненном цикле Compose. Средняя.

[app/src/main/java/com/client/xvideos/screenSettings/backup/BackupSettingsSection.kt:70](file:///g:/xvideos2/screenSettings/backup/BackupSettingsSection.kt#L70), [app/src/main/java/com/client/xvideos/screenSettings/backup/BackupComponents.kt:190](file:///g:/xvideos2/app/src/main/java/com/client/xvideos/screenSettings/backup/BackupComponents.kt#L190)

Введённые пользователем пароли для шифрования и восстановления резервных копий (`CharArray`) оставались в памяти после скрытия или ухода с экрана `BackupSettingsSection`, а также при сбоях дешифрования или отмене диалогов.
**Исправление:**
- Добавлен `DisposableEffect(Unit)` в `BackupSettingsSection`, затирающий `createPassword.fill('\u0000')` и `restorePassword.fill('\u0000')` при выходе с экрана.
- В диалогах восстановления пароль гарантированно затирается в блоках отмены и ошибок дешифрования.
- Очищен неиспользуемый импорт `KeyboardOptions`.

### T42 — Неатомарное обновление и блокирующий I/O в `AlbumFilterPresetManager`. Средняя.

[feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/molecule/filter/atom/AlbumFilterPresetManager.kt:55](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/molecule/filter/atom/AlbumFilterPresetManager.kt#L55)

В `AlbumFilterPresetManager` список пресетов обновлялся через прямое присвоение `_presets.value = ...`, что создавало окно для гонки при одновременных вызовах сохранения/удаления. Также сохранение в `SharedPreferences` происходило синхронно в вызывающем потоке.
**Исправление:**
- Обновление переведено на атомарный оператор `_presets.update { ... }`.
- Персистентное сохранение вынесено в фоновый скоуп `CoroutineScope(Dispatchers.IO)`.

### UI38 — Потенциальный `NoSuchElementException` в `AlbumFilterDisplay`, превышение лимита строк Detekt и избыточные параметры стилей. Низкая.

[feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/molecule/filter/atom/AlbumFilterDisplay.kt:25](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/molecule/filter/atom/AlbumFilterDisplay.kt#L25), [feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/molecule/filter/AlbumListFilter.kt](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/molecule/filter/AlbumListFilter.kt)

1. В `AlbumFilterDisplay.kt` поиск активного элемента `list.first { it.primary == primary }` мог вызвать падение `NoSuchElementException`, если значение во внешнем фильтре не совпадает ни с одним пунктом.
2. В файлах `AlbumListFilter.kt`, `AlbumFilterTagsDialog.kt`, `AlbumFilterGenresDialog.kt`, `AlbumFilterSaveDialog.kt` методы превышали порог Detekt `LongMethod <= 120`, а также передавали объект палитры через фиктивный несуществующий тип `StyleGenresTags.PaletteColors`.
3. В ряде файлов накопились неиспользуемые импорты (`ScreenAlbumList.kt`, `AlbumListFilterGenres.kt` и др.).
**Исправление:**
- Выбор элемента заменён на безопасный: `list.firstOrNull { it.primary == primary } ?: list.first()`.
- Крупные Composable-функции декомпозированы на читаемые подкомпоненты без нарушения правил Detekt.
- Передача `palette` удалена — подкомпоненты обращаются к синглтону `StyleGenresTags.Palette` напрямую.
- Добавлен отсутствовавший импорт `SavedAlbumFilter` в `AlbumFilterSavedPresetsDialog.kt`.
- Все неиспользуемые импорты удалены.

## Статус

| Находка | Класс | Статус | Описание / Исправление |
| --- | --- | --- | --- |
| A17 | архитектура | закрыт | `AlbumFilterPresetManager` переведён на `AtomicBoolean`, `GlobalStateTest` пройден |
| C78 | корректность | закрыт | `XlrChunkedCrypto` оптимизирован: устранение GC аллокаций `ByteArray(1)`, проброс `cause`, `matchesMagic` |
| S11 | безопасность | закрыт | Обнуление `CharArray` паролей в `BackupSettingsSection` и диалогах восстановления |
| T42 | конкурентность | закрыт | Атомарный `_presets.update` и асинхронный I/O в `AlbumFilterPresetManager` |
| UI38 | UI / гигиена | закрыт | Безопасный выбор в `AlbumFilterDisplay`, декомпозиция диалогов под Detekt `<= 120`, чистка импортов |

## Проверка

```
> Task :feature-l:compileDebugKotlin SUCCESS
> Task :app:detekt UP-TO-DATE
> Task :core:detekt UP-TO-DATE
> Task :feature-l:detekt SUCCESS
> Task :feature-r:detekt UP-TO-DATE
> Task :feature-x:detekt UP-TO-DATE
BUILD SUCCESSFUL in 1s

> Task :core:testDebugUnitTest UP-TO-DATE
> Task :feature-l:testDebugUnitTest SUCCESS
> Task :feature-r:testDebugUnitTest UP-TO-DATE
> Task :feature-x:testDebugUnitTest UP-TO-DATE
> Task :app:testDebugUnitTest SUCCESS
BUILD SUCCESSFUL in 10s (140 actionable tasks, 14 executed, 126 up-to-date)
```

## Что осталось открытым

- Все открытые ранее проектные решения владельца (включая заморозку P2P) сохраняются в силе.
- Все новые находки прохода 36 полностью закрыты и протестированы.
