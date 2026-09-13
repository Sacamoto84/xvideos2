# Код-ревью xvideos — проход 21

> **Срез:** `master` · **Статус:** закрыт · **Индекс:** [все документы](README.md)

База прохода — `master` (коммит `8e7e36e` после прохода 20).
Линзы:
- **Потокобезопасность дискового I/O и защита Android Main Thread от ANR/jank в разделе R Saved** (`R_Saved_Likes`, `R_Saved_Creator`, `R_Saved_Niches`, `R_Saved_Subscriptions`, `SavedRed`);
- **Защита холодного старта приложения от синхронного дискового чтения больших JSON-кэшей** (`R_Saved_NichesCaches`);
- **Детерминированный порядок списков сохранённого и скачанного контента** (`SavedX_Downloads`, `DownloadRed`, `FileDB`, `CollectionDB`);
- **Корректность диспетчеризации UI-колбэков и жизненного цикла Compose Popup меню** (`DropdownMenuItem_Like`);
- **Соблюдение архитектурного запрета на изменение P2P-транспорта** (`core/.../p2p/` не модифицировался).

Ключевой итог: **все 4 ключевые находки 21-го прохода устранены**. Репозиторий полностью собирается, detekt чист (0 замечаний), все unit-тесты успешно проходят.

---

## Находки

### T27 — Синхронный дисковый ввод-вывод FileDB на главном потоке в R Saved. Высокая.

[R_Saved_Likes.kt:19-51](../feature-r/src/main/java/com/client/xvideos/r/common/saved/R_Saved_Likes.kt#L19-L51), [R_Saved_Creator.kt:16-53](../feature-r/src/main/java/com/client/xvideos/r/common/saved/R_Saved_Creator.kt#L16-L53), [R_Saved_Niches.kt:15-37](../feature-r/src/main/java/com/client/xvideos/r/common/saved/R_Saved_Niches.kt#L15-L37), [R_Saved_Subscriptions.kt:51-78](../feature-r/src/main/java/com/client/xvideos/r/common/saved/R_Saved_Subscriptions.kt#L51-L78), [SavedRed.kt:19-24](../feature-r/src/main/java/com/client/xvideos/r/common/saved/SavedRed.kt#L19-L24)

В то время как `SavedX_Favorites`, `SavedL_Likes`, `SavedL_Albums` и `R_Saved_Collection` давно вынесли запись и чтение файлов на `Dispatchers.IO`, классы `R_Saved_Likes`, `R_Saved_Creator`, `R_Saved_Niches` и `R_Saved_Subscriptions` выполняли операции `insert`, `delete`, `update` и `refresh` синхронно на вызывающем потоке.
При кликах в UI (`DropdownMenuItem_Like`, `RedProfileCreaterInfo`, `NicheProfile`, диалог удаления ниши) вызов шёл напрямую с главного потока Android (Main/UI Thread). Запись файла `writeTextAtomically` создаёт временный файл, сериализует JSON, выполняет системный вызов fsync и atomic rename, что на медленных накопителях или при нагрузке на диск вызывало просадку FPS, микрофризы анимации и прямой риск ANR (Application Not Responding).
Кроме того, в `R_Saved_Subscriptions.remove` при удалении подписки вызывался метод `refresh()`, который приводил к полному O(N) перечитыванию и парсингу всех файлов подписок на диске вместо точечного удаления.

*Исправление:*
- Классы `R_Saved_Likes`, `R_Saved_Creator`, `R_Saved_Niches` получили параметр конструктора `scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)`.
- Синглтон `SavedRed` теперь передаёт свой `@ApplicationScope scope` во все дочерние хранилища (`likes`, `creators`, `niches`, `collections`, `subscriptions`, `nichesCache`).
- Все операции записи, удаления, обновления и перечитывания диска вынесены на `Dispatchers.IO`.
- Мутации списков состояний Compose (`SnapshotStateList`) и показ снекбаров изолированы и отправляются на `Dispatchers.Main`.
- В `R_Saved_Subscriptions.remove` полный пересчет диска заменен на O(1) удаление одного файла на диске с последующим точечным удалением из `listCreators` в памяти.

---

### T28 — Синхронное чтение большого кэша ниш на главном потоке при старте приложения. Средняя.

[R_Saved_NichesCaches.kt:60-142](../feature-r/src/main/java/com/client/xvideos/r/common/saved/R_Saved_NichesCaches.kt#L60-L142)

При создании Hilt-синглтона `SavedRed` на старте приложения (в `SplashActivity`/`MainActivity`) создавался экземпляр `R_Saved_NichesCaches`, у которого в блоке `init` вызывался метод `readFromDisk()`.
Этот метод синхронно открывал `niches.json` (содержащий сотни элементов с полной структурой метаданных) и выполнял JSON-десериализацию прямо на главном потоке в момент запуска приложения, увеличивая время холодного старта (Cold Start Time) и замедляя переход к первому кадру.

*Исправление:*
- Чтение файла `niches.json` и его десериализация в `readFromDisk()` перенесены в асинхронную корутину на `Dispatchers.IO`.
- Применение результатов в Compose State (`list.replaceWith`, `version++`, `timeRefresh()`, `isDownloaded = ...`) безопасно выполняется на `Dispatchers.Main`.
- В методе `refresh()` запись скачанного кэша на диск также изолирована на `Dispatchers.IO` с переключением на `Dispatchers.Main` для UI-состояний.

---

### UI15 — Недетерминированный порядок файлов в загрузках и FileDB/CollectionDB. Средняя.

[SavedX_Downloads.kt:180-186](../feature-x/src/main/java/com/client/xvideos/x/feature/saved/SavedX_Downloads.kt#L180-L186), [DownloadRed.kt:179-195](../feature-r/src/main/java/com/client/xvideos/r/common/downloader/DownloadRed.kt#L179-L195), [FileDB.kt:159](../core/src/main/java/com/client/xvideos/common/fileDB/FileDB.kt#L159), [CollectionDB.kt:199](../core/src/main/java/com/client/xvideos/common/collectionDB/CollectionDB.kt#L199)

Функции `File.listFiles` и `File.walkTopDown` в операционных системах Android/Linux возвращают список файлов в порядке inode или хеш-таблицы директории (недетерминированный порядок).
В результате:
1. В `SavedX_Downloads.refresh()` загруженные видео отображались в случайном порядке, который мог скачкообразно меняться после каждой загрузки или удаления.
2. В `DownloadRed.refreshDownloadList()` файлы `.info` также обрабатывались в случайном порядке обхода файловой системы.
3. В `FileDB.refresh()` и `CollectionDB.readAllCollections()` элементы директорий также не имели стабильного упорядочивания.

*Исправление:*
- В `SavedX_Downloads.refresh()` файлы `.info` теперь явно сортируются по `lastModified()` descending (`infos.sortedByDescending { it.lastModified() }`), гарантируя, что последние сохраненные видео стабильно находятся вверху списка.
- В `DownloadRed.refreshDownloadList()` файлы `.info` фильтруются и сортируются по `lastModified()` descending.
- В `FileDB.refresh()` файлы каталога сортируются по `lastModified()` descending.
- В `CollectionDB.readAllCollections()` элементы внутри каждой коллекции сортируются по `lastModified()` descending.

---

### UI16 — Ошибочная диспетчеризация UI-колбэков и задержка закрытия меню в DropdownMenuItem_Like. Низкая.

[DropdownMenuItem_Like.kt:20-35](../feature-r/src/main/java/com/client/xvideos/r/ui/expand_menu_video/DropdownMenuItem_Like.kt#L20-L35)

В Composable `DropdownMenuItem_Like`:
1. Колбэк закрытия меню `onDismiss.invoke()` и колбэк триггера обновления `onRunLike.invoke()` вызывались внутри корутины `savedRed.invoke().scope.launch` после `delay(200)` на `Dispatchers.IO`.
2. Вызов `onDismiss` с фонового потока после задержки задерживал исчезновение меню, создавая ощущение неотзывчивости UI (в соседнем `DropdownMenuItem_Follow` меню закрывается мгновенно, а отложенная корутина выполняется независимо).
3. `onRunLike.invoke()` запускал мутацию UI-состояния экрана на фоновом пуле потоков.

*Исправление:*
- `onDismiss.invoke()` вызывается немедленно при клике на элемент меню на главном потоке (синхронно закрывая меню).
- Вызов `onRunLike.invoke()` обёрнут в `withContext(Dispatchers.Main)` после завершения операций с лайком.

---

## Статус

| Находка | Класс | Статус | Коммит |
| --- | --- | --- | --- |
| T27 | конкурентность / I/O | закрыт | `master` |
| T28 | конкурентность / I/O | закрыт | `master` |
| UI15 | интерфейс / надежность | закрыт | `master` |
| UI16 | интерфейс / конкурентность | закрыт | `master` |

---

## Проверка

### Автоматические тесты
```powershell
./gradlew testDebugUnitTest --no-daemon
./gradlew detekt --no-daemon
```

Фактический результат:
- `testDebugUnitTest`: **BUILD SUCCESSFUL**, 140 actionable tasks, 0 ошибок. Все тесты `:app`, `:core`, `:feature-l`, `:feature-r`, `:feature-x` пройдены.
- `detekt`: **BUILD SUCCESSFUL**, 12 actionable tasks, **0 issues**.

---

## Что осталось открытым

P2P-транспортный слой (`core/.../p2p/`) остаётся перманентно замороженным согласно решению владельца от 11.09.2026.
Все остальные модули и компоненты чисты, полностью типизированы и протестированы.
