# Отчёт о код-ревью: 14 сентября 2026 (проход 35, v14)

Срез: `master`
Фокус прохода: мультилинзовое ревью по направлениям **Correctness**, **Concurrency / Thread Safety**, **UI / Lifecycle**.

---

## Сводка находок

| ID | Линза | Серьёзность | Статус | Суть |
| --- | --- | --- | --- | --- |
| **C77** | Correctness / Network Headers | Высокая | **закрыт** | В `MediaDownloadWorker.buildDownloadRequest` проверка `headers["User-Agent"]` зависела от регистра: при передаче `"user-agent"` значение отбрасывалось и заменялось дефолтным UA; жёсткий каст `as NotificationManager` заменён на безопасный. |
| **T41** | Concurrency / Cache Thread Safety | Средняя | **закрыт** | В `CoilImageLoaderFactory` метод `clearCache()` не был защищён синхронизацией `synchronized(this)`, допуская состояние гонки с одновременным вызовом `getImageLoader()`. |
| **UI37** | UI / Paging Lifecycle | Средняя | **закрыт** | В `R_Screen_Saved_LikesTab` подписка `snapshotFlow { vm.savedRed.likes.list.size }` без `.drop(1)` немедленно вызывала `pager.refresh()` при первом входе на экран, дублируя начальный сетевой запрос Paging 3. |

---

## Подробное описание и решения

### C77 — Case-insensitive обработка заголовка User-Agent и безопасный NotificationManager в `MediaDownloadWorker`
- **Файл:** [MediaDownloadWorker.kt](../core/src/main/java/com/client/xvideos/common/download/work/MediaDownloadWorker.kt)
- **Проблема:**
  1. В `buildDownloadRequest` чтение `val userAgent = headers["User-Agent"] ?: DEFAULT_USER_AGENT` выполнялось по точному регистру строки. При наличии заголовка в нижнем регистре (`"user-agent"`) выражение возвращало `null` и выставляло `DEFAULT_USER_AGENT`. В цикле же `headers.forEach` с условием `!k.equals("User-Agent", ignoreCase = true)` исходный заголовок отфильтровывался, silently перезаписывая пользовательский агент.
  2. `notificationManager` получался через `applicationContext.getSystemService(...) as NotificationManager` — жёсткий каст без проверки на `null`.
  3. Избыточный elvis-оператор на non-null типе `response.body`.
- **Решение:**
  1. Поиск заголовка переведён на регистронезависимый: `headers.entries.firstOrNull { it.key.equals("User-Agent", ignoreCase = true) }?.value ?: DEFAULT_USER_AGENT`.
  2. `notificationManager` сделан nullable с безопасными вызовами `?.notify()`.
  3. Удалена избыточная проверка `response.body`.

### T41 — Потокобезопасная очистка кэша в `CoilImageLoaderFactory`
- **Файл:** [CoilImageLoaderFactory.kt](../core/src/main/java/com/client/xvideos/common/coil/CoilImageLoaderFactory.kt)
- **Проблема:** Метод `recreate()` был защищён `synchronized(this)`, а `clearCache()` вызывался без блокировки. При вызове очистки кэша из фонового потока (настройки) параллельные запросы изображений из UI могли инициализировать `instance` в момент удаления каталогов с диска.
- **Решение:** Тело `clearCache()` обёрнуто в `synchronized(this)`. Очистка памяти и диска `instance?.apply { ... }`, удаление папок и создание нового экземпляра `instance = createImageLoader(appContext)` выполняются атомарно.

### UI37 — Устранение избыточного обновления Paging 3 при открытии экрана лайков RedGifs
- **Файл:** [R_Screen_Saved_LikesTab.kt](../feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/saved/tab/R_Screen_Saved_LikesTab.kt)
- **Проблема:** В `LaunchedEffect(Unit)` подписка `snapshotFlow { vm.savedRed.likes.list.size }.collect { pager.refresh() }` срабатывала сразу при первой композиции, так как `snapshotFlow` отдаёт текущее значение немедленно. Это приводило к сбросу позиции скролла и повторному запросу первой страницы пейджера.
- **Решение:** Добавлен оператор `.drop(1)`: обновление `pager.refresh()` вызывается только при последующих изменениях размера списка лайков.

---

## Верификация
1. `./gradlew detekt` — 0 ошибок во всех 5 модулях проекта.
2. `./gradlew testDebugUnitTest` — 100% тестов пройдены успешно (140 задач, включая новый тест в `DownloadWorkRequestTest`).
3. Код и логика P2P (`core/.../p2p/`) не затрагивались согласно правилу репозитория.
