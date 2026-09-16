# Код-ревью xvideos — проход 52 (фокус: feature-l)

> **Срез:** `8a8dcf2` + рабочее дерево · **Статус:** закрыт · **Индекс:** [все документы](README.md)

База прохода `8a8dcf2`. Изменено 6 файлов в модуле `feature-l`, обновлён 1 файл тестов `LFullScreenPayloadTest.kt`.
Линзы:
- `T` / `C` / Off-Main Safety и сетевой ввод-вывод: вынос тяжелых сетевых GraphQL-запросов и JSON-парсинга (`getAlbumList`, `getAlbumListAggregations`) в `ScreenLAlbumListSM` (`init` и `loadInitialData`) на `Dispatchers.IO`, исключающий блокировку главного потока Android при открытии списка альбомов и смене фильтров.
- `C` / стабильность против сбоев сети: устранение риска падения приложения через `.getOrThrow()` в `ScreenLAlbumLandingTagSM` при отсутствии интернета или сетевых ошибках с безопасным переключением на `withContext(Dispatchers.IO)` и логированием ошибки.
- `T` / Off-Main безопасность поиска: вынос вызова `getLandingPageAlbumSearch` в `ScreenLAlbumSearchSM` на `Dispatchers.IO`.
- `UI` / адаптивность и доступность формы авторизации: добавление `.imePadding()` и `.verticalScroll(rememberScrollState())` в `LLoginContent` (`L_ScreenLogin.kt`), предотвращающее обрезание кнопок «Сохранить», «Назад» и «Пропустить» экранной клавиатурой и на компактных экранах.
- `UI` / `Q` / точность позиционирования в полноэкранном режиме: доработка `resolveInitialIndex` в `L_FullScreenImage.kt` с фоллбэком на сопоставление по `selectionKey()`, исключающая сброс выбранной картинки на индекс `0` при незначительных отличиях параметров URL/токенов в `PicsDetails`.
- `C` / атомарность файловой системы: гарантированная очистка частично скопированного каталога-приёмника при сбое резервного копирования в `SavedL_Collection.renameCollection`.
Граница P2P (`core/.../p2p/`) заморожена по решению владельца от 11.09.2026 и не затрагивалась.

## Находки

### T21 — Выполнение сетевых запросов и парсинга GraphQL на главном потоке в `ScreenLAlbumListSM`. Высокая.

[feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/ScreenAlbumListSM.kt:100-170](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/ScreenAlbumListSM.kt#L100)

В методах `init` и `loadInitialData()` класса `ScreenLAlbumListSM` корутины запускались в контексте `screenModelScope`, диспетчером по умолчанию для которого является `Dispatchers.Main.immediate`. Внутри корутины напрямую вызывались методы `luscious.getAlbumList(1, filter.value)` и `luscious.getAlbumListAggregations(1, filter.value)`. Это приводило к синхронному открытию HTTP-сокетов, дисковому I/O базы Room и парсингу крупных JSON-ответов прямо на UI-потоке Android (при этом блок обновления стейтов содержал избыточный `withContext(Dispatchers.Main)`, что свидетельствовало об ошибочном предположении автора о фоновом контексте). В соседнем методе `loadAlbumList(page)` вызовы уже были корректно обёрнуты в `withContext(Dispatchers.IO)`.
**Исправление:** Вызовы `getAlbumList` и `getAlbumListAggregations` в `init` и `loadInitialData()` явно вынесены в `withContext(Dispatchers.IO)`. UI-поток защищен от фризов и ANR при инициализации экрана списка альбомов L.

### C126 — Падение приложения из-за `.getOrThrow()` на корутине UI-потока в `ScreenLAlbumLandingTagSM`. Высокая.

[feature-l/src/main/java/com/client/xvideos/l/ui/screens/albumLandingTag/ScreenLAlbumLandingTag.kt:420-435](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/ui/screens/albumLandingTag/ScreenLAlbumLandingTag.kt#L420)

В `ScreenLAlbumLandingTagSM.init` выполнялся запуск корутины `screenModelScope.launch { albumTopHits.value = luscious.getLandingPageAlbumTag(tag).getOrThrow() }`. При отсутствии сети, ошибке DNS или сбое HTTP вызов `getOrThrow()` выбрасывал исключение, которое не перехватывалось и приводило к немедленному аварийному завершению (crash) приложения на главном потоке. Кроме того, запрос выполнялся без переключения на `Dispatchers.IO`.
**Исправление:** Запрос обёрнут в `withContext(Dispatchers.IO)`, вызов переведён на безопасное получение `res.getOrNull()` с фиксацией ошибки в логах через `Timber.w` и корректным пробросом `CancellationException`.

### T22 — Выполнение поиска альбомов L на UI-потоке в `ScreenLAlbumSearchSM`. Средняя.

[feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/albumSearch/L_ScreenAlbumSearch.kt:236-248](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/albumSearch/L_ScreenAlbumSearch.kt#L236)

В `ScreenLAlbumSearchSM.search()` сетевой вызов `luscious.getLandingPageAlbumSearch(query)` выполнялся непосредственно в `screenModelScope.launch` на `Dispatchers.Main.immediate`.
**Исправление:** Запрос вынесен в `withContext(Dispatchers.IO)`.

### UI63 — Обрезание кнопок формы авторизации клавиатурой и невозможность скролла в `L_ScreenLogin`. Средняя.

[feature-l/src/main/java/com/client/xvideos/l/ui/screens/L_ScreenLogin.kt:90-105](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/ui/screens/L_ScreenLogin.kt#L90)

Компонент `LLoginContent` отображал форму авторизации суммарной высотой ~650dp внутри вертикальной `Column` с `verticalArrangement = Arrangement.Center`. При открытии программной клавиатуры (IME) или на экранах небольшой диагонали/в альбомной ориентации нижние кнопки («Сохранить», «Назад», «Пропустить») вытеснялись за пределы видимой области. Из-за отсутствия вертикального скролла и учёта IME-инсетов пользователь не мог прокрутить экран и отправить форму или пропустить авторизацию.
**Исправление:** К контейнеру `Column` применены модификаторы `.imePadding()`, `.verticalScroll(rememberScrollState())` и безопасные вертикальные отступы `padding(horizontal = 16.dp, vertical = 24.dp)`.

### UI64 — Сброс стартовой позиции на первый элемент при открытии полноэкранного просмотра `L_FullScreenImage`. Средняя.

[feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenFullScreen/L_FullScreenImage.kt:345-354](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenFullScreen/L_FullScreenImage.kt#L345)

Функция `resolveInitialIndex(items, target)` искала целевой элемент через `items.indexOf(target)`. В классе `PicsDetails` метод `equals` производит поэлементное сравнение всех свойств, включая вложенный список `Thumbnails` со специфичными для сессии токенами и query-параметрами CDN. Если объект `target`, переданный при клике из сетки, имел минимальные расхождения по параметрам эскизов или ссылкам альбома с элементами пейлоада, `indexOf` возвращал `-1`, который через `coerceIn(0, items.lastIndex)` приводился к `0`. В результате пользователь открывал картинку, например, под номером 30, но полноэкранный просмотр открывался на 0-м элементе.
**Исправление:** Функция `resolveInitialIndex` доработана: сначала проверяется прямое равенство, а при отсутствии совпадения выполняется поиск по устойчивому ключу `it.selectionKey() == targetKey`. Добавлены тесты в `LFullScreenPayloadTest.kt`, подтверждающие корректность точного поиска, сопоставления по ключу и безопасного фоллбэка.

### C127 — Оставление повреждённых директорий при сбое резервного копирования в `SavedL_Collection.renameCollection`. Средняя.

[feature-l/src/main/java/com/client/xvideos/l/featured/saved/SavedL_Collection.kt:156-168](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/featured/saved/SavedL_Collection.kt#L156)

Если метод `oldRoot.renameTo(newRoot)` завершался неудачей (например, из-за границ файловых систем или блокировок файлов дескрипторами), выполнялась резервная ветка `oldRoot.copyRecursively(newRoot, overwrite = false)`. Метод `copyRecursively` возвращает `Boolean` (`false` при ошибке копирования любого файла) либо выбрасывает исключение. При частичном сбое копирования папка `newRoot` оставалась недозаписанной и поврежденной на диске.
**Исправление:** Добавлена проверка результата копирования и блок очистки `newRoot.deleteRecursively()` при неуспехе или исключении, что гарантирует целостность файловой системы.

---

## Итог верификации

- **Unit-тесты:** `./gradlew testDebugUnitTest` — успешно (все тесты по всем 5 модулям `core`, `feature-l`, `feature-r`, `feature-x`, `app` пройдены).
- **Detekt:** `./gradlew detekt --rerun-tasks` — 0 ошибок и предупреждений по всем 5 модулям.
- **Архитектурные ограничения:** P2P (`core/.../p2p/`) не изменялся.
