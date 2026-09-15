# Код-ревью xvideos — проход 44

> **Срез:** `a47bbf1` + рабочее дерево · **Статус:** закрыт · **Индекс:** [все документы](README.md)

База прохода `a47bbf12e2e6739b9afba200becf86ed227e1b96`. Изменено 5 файлов в модулях `feature-l`, `feature-x`, `feature-r`.
Линзы: устойчивость Compose UI при пустых коллекциях (`UI`), предотвращение холостых сетевых запросов (`C`), безопасность и консистентность файловых операций коллекций (`C`), защита корутин загрузки альбома от сбоев кэширования (`C`), сохранность структуры каталогов и корректность диспетчеризации колбэков (`T`). Граница P2P (`core/.../p2p/`) заморожена по решению владельца и не затрагивалась.

## Находки

### UI47 — Фатальный крах экрана полноэкранного просмотра `L_FullScreenImage` при пустом списке картинок. Критическая.

[feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenFullScreen/L_FullScreenImage.kt:149-178](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenFullScreen/L_FullScreenImage.kt#L149)

В экране `L_FullScreenImage` индекс начального элемента вычислялся как:
`val initialIndex = remember { filteredPic.indexOf(item).coerceIn(0, filteredPic.lastIndex) }`
А автоматическая прокрутка ленты миниатюр выполнялась как:
`lazyRowState.scrollToItem((currentIndex - 2).coerceIn(0, filteredPic.size - 1))`
Если коллекция `filteredPic` оказывалась пустой (`size == 0`), то `filteredPic.lastIndex` и `filteredPic.size - 1` равны `-1`.
Функция стандартной библиотеки Kotlin `Int.coerceIn(minimumValue, maximumValue)` требует, чтобы `minimumValue <= maximumValue`. При вызове `coerceIn(0, -1)` выбрасывалось исключение:
`IllegalArgumentException: Cannot coerce value to an empty range: maximum -1 is less than minimum 0.`
Это немедленно приводило к фатальному краху Compose UI и аварийному завершению приложения.
**Исправление:**
- Вычисление `initialIndex` защищено проверкой пустого списка: `if (filteredPic.isEmpty()) 0 else filteredPic.indexOf(item).coerceIn(0, filteredPic.lastIndex)`.
- Вызов `scrollToItem` ограничен условием `if (filteredPic.isNotEmpty())`.

### C99 — Холостые сетевые запросы поиска при пустом или пробельном вводе в `getSearchResults`. Средняя.

[feature-x/src/main/java/com/client/xvideos/x/search/getSearchResults.kt:48-52](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/search/getSearchResults.kt#L48)

В функции подсказок поиска `getSearchResults(query: String)` отсутствовала проверка на пустую строку.
Если пользователь отправлял пустой запрос или строку из пробелов, функция кодировала её через URLEncoder и выполняла бессмысленный сетевой запрос на `$urlStart/search-suggest/`, создавая ненужную нагрузку на сеть и трафик.
**Исправление:** Добавлен ранний возврат `if (query.isBlank()) return null` до кодирования параметров и сетевого вызова.

### C100 — Ложный показ ошибки удаления и отсутствие нормализации имени коллекции в `SavedL_Collection`. Средняя.

[feature-l/src/main/java/com/client/xvideos/l/featured/saved/SavedL_Collection.kt:303-326, 398-422](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/featured/saved/SavedL_Collection.kt#L303)

1. В методе `removeAll(items, collectionName)` отсутствовала проверка `if (uniqueItems.isEmpty()) return` (в отличие от парного метода `addAll`). Если в метод передавался пустой список, запускалась дисковая IO-корутина, обход файлов находил 0 совпадений и пользователю показывался некорректный снекбар ошибки «Файлы не найдены».
2. В приватном методе `remove(identifiers, collectionName)` аргумент `collectionName` передавался напрямую в `File(AppPath.l_collection, collectionName)` без валидации через `CollectionName.normalizeOrNull(collectionName)`.
**Исправление:**
- В `removeAll` добавлен ранний выход `if (uniqueItems.isEmpty()) return`.
- В приватный `remove` добавлена проверка и нормализация `CollectionName.normalizeOrNull(collectionName)`.

### C101 — Сбой загрузки альбома при исключениях записи дискового кэша в `AlbumInfo.cacheBundleIfComplete`. Средняя.

[feature-l/src/main/java/com/client/xvideos/l/net/AlbumInfo.kt:102-116](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/net/AlbumInfo.kt#L102)

В конце успешной загрузки данных альбома метод `fetchAlbumInfo` вызывал `cacheBundleIfComplete(repository, albumDetails)`.
Внутри него вызовы `LJson.encodeToString(bundle)` и `repository.putAlbumBundleCache(id, ...)` не были защищены от исключений. При сбое сериализации, переполнении диска или ошибке файлового дескриптора исключение прерывало корутину загрузки альбома уже после того, как сетевой ответ был успешно получен и распарсен.
**Исправление:** Вызовы сериализации и записи в дисковый кэш обёрнуты в защитный блок `runCatching { ... }.onFailure { Timber.w(...) }`.

### C102 — Удаление корневой директории загрузок и фоновый вызов колбэка в `DownloadRed.deleteAll`. Средняя.

[feature-r/src/main/java/com/client/xvideos/r/common/downloader/DownloadRed.kt:240-246](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/common/downloader/DownloadRed.kt#L240)

1. В методе `deleteAll()` вызов `File(AppPath.r_cache_download).deleteRecursively()` удалял саму директорию `r_cache_download` без вызова `mkdirs()`. Папка исчезала из файловой системы, из-за чего последующие операции сохранения файлов в эту папку могли сбоить.
2. Колбэк `onComplete()` вызывался напрямую из пула `Dispatchers.IO`, нарушая соглашение о вызове UI-колбэков на главном потоке.
**Исправление:**
- Добавлен вызов `mkdirs()` сразу после `deleteRecursively()`: `.apply { deleteRecursively(); mkdirs() }`.
- Вызов `onComplete()` переведён на `withContext(Dispatchers.Main)`.

## Статус

| Находка | Класс | Статус | Описание / Исправление |
| --- | --- | --- | --- |
| UI47 | Feature-L / FullScreen | Исправлен | Защита от краха `IllegalArgumentException` при пустом `filteredPic` в `L_FullScreenImage` |
| C99 | Feature-X / Search | Исправлен | Ранний выход `if (query.isBlank()) return null` в `getSearchResults` |
| C100 | Feature-L / Collections | Исправлен | Защита от ложного снекбара в `removeAll` и нормализация имени коллекции в `SavedL_Collection` |
| C101 | Feature-L / AlbumInfo | Исправлен | Защита корутины загрузки альбома от дисковых ошибок кэша в `AlbumInfo.cacheBundleIfComplete` |
| C102 | Feature-R / Downloader | Исправлен | Восстановление папки через `mkdirs()` и вызов `onComplete` на `Dispatchers.Main` в `DownloadRed.deleteAll` |
