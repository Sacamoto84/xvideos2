# XLive: запись эфира — план реализации

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** кнопка записи эфира в комнате XLive, подвкладка «Записи» и экран записи с плеером X, XLive — отдельный раздел резервной копии.

**Architecture:** запись — процессный объект `XLiveRecordings` в `feature-xlive` (как `XLiveSaved`): своя загрузка LL-HLS по отдельному адресу, две дорожки fMP4 в сырые файлы, склейка `MediaExtractor`/`MediaMuxer`. Комната только запускает и останавливает запись. Общие части плеера скачанного ролика X переезжают в `:core`, экран записи собирает их. Данные XLive переезжают из `X/Live` в папку `XLive`, она становится четвёртым разделом копии.

**Tech Stack:** Kotlin, Compose/Material3, Voyager, Ktor (OkHttp), kotlinx.serialization, FileDB, media3 (плеер), Android `MediaExtractor`/`MediaMuxer`, JUnit 4 + kotlinx-coroutines-test.

**Spec:** [docs/superpowers/specs/2026-10-26-xlive-recording-design.md](../specs/2026-10-26-xlive-recording-design.md) — читать вместе с планом.

## Global Constraints

- Названия сайтов — нигде: ни в коде, ни в логах, ни в коммитах, ни в документах. Новые идентификаторы не содержат название сайта X даже подстрокой без учёта регистра; перед коммитом — `git diff --cached -U0 | grep '^+' | grep -i <подстрока>` по добавленным строкам.
- Ники, адреса потока и сегментов, теги — не в `Timber` и не в команды терминала. В логе записи — только числа и имена классов исключений.
- Атомы и молекулы — по одному на файл, имя файла = имя функции, у каждого `@Preview` (минималистичное). Корневой `Screen*.kt` — только экран, привязка ScreenModel и `Screen*Content`. Молекулы и атомы не принимают ScreenModel.
- Новые кнопки-значки — с подсказкой `HintTooltip` (`core/.../common/ui/HintTooltip.kt`).
- Сетки — `adaptiveGridColumns()`; ленивые списки на всю ширину — `contentPadding = adaptiveListPadding(...)`; позиции списков — `LazyGridPosition` в ScreenModel, не `rememberLazyGridState` на экране.
- Тесты ScreenModel и всего `feature-xlive` — без `runTest`: `UnconfinedTestDispatcher`/`StandardTestDispatcher` со своим `TestCoroutineScheduler`, `Dispatchers.setMain` только в ScreenModel-тестах, как в `XLiveRoomSMTest`.
- Производное от `SnapshotStateList` — через `derivedStateOf`, не `remember(list)`.
- Гейт перед каждым коммитом: `python C:/Users/Ivan/.claude/skills/gradle-run/scripts/gradle_run.py run --workflow <id> --scope broad --question "<вопрос>" -- gradlew.bat testDebugUnitTest detekt :app:lintRelease --continue`; итог — из JUnit XML (`*/build/test-results/testDebugUnitTest/*.xml`) и `app/build/reports/lint-results-release.xml` (Error/Fatal = 0).
- Коммит — только с согласия владельца, в `master`, без пуша, явными путями (`git add -- <пути>`, `git commit -F msg -- <пути>`), на русском, Conventional Commits, последняя строка `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`. Один коммит на кусок A/B/C/D. Перед коммитом `git diff --cached --stat` — без чужих файлов (staged rename от `git mv`).
- Телефон владельца: debug-сборка, `adb install -r -t app/build/outputs/apk/debug/app-debug.apk`; код блокировки — только из переменной окружения в команде, нигде не записывать; экраны с превью эфиров не снимать — `uiautomator dump`, числа из файлов. P2P и «Поделиться» не проверять.
- Складной AVD `Pixel_10_Pro_Fold` — для раскладки (куски A и D): внешний экран, внутренний портрет и альбомная, Medium (`wm density 475` → `wm density reset` на каждом дисплее).

## Review Focus

1. **Поворот, сложение, смена плотности посреди записи** — запись идёт дальше. Пересоздание Activity тоже вызывает `onDispose` у экрана комнаты; останавливать можно только при `isChangingConfigurations == false`. Тест: Task 8, `screenGone_whileChangingConfigurations_keepsRecording`.
2. **Новый адрес после 403 с другой нумерацией сегментов.** Номер в новом плейлисте меньше ожидаемого или сильно больше. Запись не должна писать дубли, не должна ждать номер, которого не будет. Она продолжает с края эфира и считает пропуск. Тест: Task 6, `refreshedAddress_withRestartedNumbers_continuesFromEdge`.
3. **Процесс убит посреди склейки** — остались сырые файлы и `video.mp4.part`. Следующий запуск доклеивает. Запись без единого сегмента видео удаляется. Склейка не удалась — статус `BROKEN`, запись не пропадает. Тесты: Task 7, `finishPending_*`.
4. **Восстановление старого архива с `X/Live`:**
   - «замена» заменяет `XLive/Saved` и `XLive/History`;
   - «объединение» добавляет недостающее;
   - `X/Live` после переноса не остаётся.

   Тесты: Task 1 и Task 2.
5. **Уход с экрана комнаты по навигации вперёд**: тег, вход, другая комната. Запись останавливается так же, как по «Назад». Комната B, открытая поверх A, не наследует запись A. Тест: Task 8, `screenGone_notChangingConfigurations_stopsWithLeftRoom`.

---

## Кусок A — XLive в своей папке и отдельный раздел копии

### Task 1: Пути XLive и перенос из `X/Live`

**Files:**
- Modify: `core/src/main/java/com/client/xvideos/common/AppPath.kt` (enum `Folder`, блок «XLive», `init`)
- Create: `core/src/main/java/com/client/xvideos/common/backup/XLiveLegacyFolder.kt`
- Test: `core/src/test/java/com/client/xvideos/common/backup/XLiveLegacyFolderTest.kt`

**Interfaces:**
- Produces:
  - `AppPath.xlive_saved = "$main/XLive/Saved"`, `AppPath.xlive_history = "$main/XLive/History"`, `AppPath.xlive_records = "$main/XLive/Records"` (новый); `Folder.XLIVE("XLive")`; `init` создаёт все три.
  - `object XLiveLegacyFolder { const val LEGACY = "X/Live"; fun move(mainRoot: File, mode: XlrRestoreMode): Boolean }` — переносит `X/Live/Saved` → `XLive/Saved`, `X/Live/History` → `XLive/History`; `true`, если что-то перенесено. `MERGE`: файл, которого в новом месте нет, переносится, совпавший остаётся новым. `REPLACE`: папка нового места заменяется папкой из `X/Live` целиком (только те из `Saved`/`History`, что есть в `X/Live`). После переноса `X/Live` удаляется целиком. Нет `X/Live` — `false`, ничего не трогает.

- [ ] **Step 1: Тесты** в `XLiveLegacyFolderTest` (TemporaryFolder как `mainRoot`):
  - `noLegacy_returnsFalseAndTouchesNothing` — есть только `XLive/Saved/a.room`; `move(root, MERGE) == false`, файл на месте.
  - `merge_movesMissingFiles_keepsNewOnConflict` — `X/Live/Saved/{a,b}.room` (содержимое "old"), `XLive/Saved/b.room` ("new"); после `move(root, MERGE)`: `XLive/Saved/a.room == "old"`, `XLive/Saved/b.room == "new"`, `X/Live` не существует.
  - `replace_replacesWholeFolder` — `X/Live/History/a.room` ("old"), `XLive/History/{a,c}.room` ("new"); после `move(root, REPLACE)`: в `XLive/History` только `a.room == "old"`, `c.room` нет.
  - `replace_keepsFolderAbsentInLegacy` — `X/Live/Saved/a.room`, `XLive/History/h.room`; после `REPLACE` `XLive/History/h.room` на месте.
  - `emptyNewDirs_fromInit_areFilled` — `XLive/Saved` и `XLive/History` пустые (их создал `AppPath.init`), `X/Live/Saved/a.room`; после `MERGE` файл в `XLive/Saved`.
- [ ] **Step 2:** `gradlew.bat :core:testDebugUnitTest --tests "*XLiveLegacyFolderTest"` — FAIL (нет класса).
- [ ] **Step 3:** реализовать `XLiveLegacyFolder.move` и пути в `AppPath`. Перенос — `renameTo`, при неудаче `copyTo` + удаление; ошибки — в `Timber.w` без путей.
- [ ] **Step 4:** тот же запуск — PASS.

### Task 2: Раздел `XLive` в резервной копии

**Files:**
- Modify: `core/src/main/java/com/client/xvideos/common/backup/XlrBackupManager.kt:60` (`sections`), `restoreArchive` (после `applyRestoredPaths` — `XLiveLegacyFolder.move(mainRoot, mode)`)
- Modify: `app/src/main/java/com/client/xvideos/screenSettings/backup/BackupSelection.kt` (`backupItemIcon`: `"XLive" -> R.drawable.icon_xlive`)
- Modify: `app/src/main/java/com/client/xvideos/screenSettings/backup/BackupComponents.kt:297` («Все X/L/R» → «Все разделы»), `BackupSettingsSection.kt:220` («Backup X/L/R» → «Backup X/L/R/XLive»)
- Test: `core/src/test/java/com/client/xvideos/common/backup/XlrRestoreXLiveTest.kt`; дополнить `XlrBackupModelsTest` или новый `XlrBackupSectionsTest`

**Interfaces:**
- Consumes: `XLiveLegacyFolder.move` (Task 1).
- Produces: раздел `"XLive"` в `currentBackupItems` с детьми `XLive/Saved`, `XLive/History`, `XLive/Records`; `normalizeSelectedPaths` принимает `XLive/...`.

- [ ] **Step 1: Тесты:**
  - `currentItems_listsXLiveSectionWithChildren` — `currentBackupItems(baseDir = tmp)` при `XLive/Saved/a.room`, `XLive/Records/1/record.json`: есть элемент `path == "XLive"`, `parentPath == null`, и дети `XLive/Records`, `XLive/Saved`.
  - `normalize_acceptsXLivePaths` — `normalizeSelectedPaths(setOf("XLive/Records", "Y/z")) == listOf("XLive/Records")`.
  - `partialRecordingFiles_areNotBackedUp` — `shouldIncludeBackupEntry("XLive/Records/1/video.part", DEFAULT) == false`, `"XLive/Records/1/video.mp4.part"` → `false`, `"XLive/Records/1/video.mp4"` → `true`, `"XLive/Records/1/.storyboard.jpg"` → `false`.
  - `restoreOldArchiveWithXLiveInsideX_replace_movesToXLive` — ZIP из `X/Live/Saved/a.room` («из архива»), текущие `XLive/Saved/b.room`; `restoreArchive(root, setOf("X"), REPLACE) { zip }`: в `XLive/Saved` только `a.room`, `X/Live` нет. Тот же с `MERGE` — `a.room` и `b.room`. Сборка ZIP — как в `XlrRestorePartialArchiveTest`.
- [ ] **Step 2:** `:core:testDebugUnitTest --tests "*XlrRestoreXLiveTest" --tests "*XlrBackupSectionsTest"` — FAIL.
- [ ] **Step 3:** `sections = listOf("X", "L", "R", "XLive")`; вызов переноса в `restoreArchive` после `applyRestoredPaths`; значок и подписи в `app`. Проверить, что `XlrRestorePaths`, журнал и `recoverInterruptedRestore` не перечисляют разделы сами (`grep -n '"X"' core/src/main/java/com/client/xvideos/common/backup/`).
- [ ] **Step 4:** тот же запуск — PASS; `:app:testDebugUnitTest --tests "*BackupControllerTest"` — PASS.

### Task 3: Перенос при первом обращении к XLive; проверка и коммит A

**Files:**
- Modify: `feature-xlive/src/main/java/com/client/xvideos/xlive/saved/XLiveSaved.kt` — до создания первого хранилища один раз `XLiveLegacyFolder.move(File(AppPath.main), XlrRestoreMode.MERGE)` (ленивый `moved`, вызывается из `myStore`/`historyStore` и из `XLiveRecordings` в Task 7).

- [ ] **Step 1:** гейт — зелёный, цифры из XML.
- [ ] **Step 2: телефон.**
  1. Сборка поверх текущей.
  2. До установки — список `run-as com.client.xvideos ls files/store/X/Live/Saved files/store/X/Live/History | wc -l`.
  3. Открыть XLive → «Избранное»: «Мои» и «История» с тем же числом комнат.
  4. В `files/store/XLive/{Saved,History}` — те же файлы, `X/Live` нет.
  5. Настройки → резервная копия: раздел XLive со значком-логотипом и галочками `History`, `Saved`, их размеры.
- [ ] **Step 3: складной AVD.** Список копии с четырьмя разделами — внешний и внутренний экран, по дампу.
- [ ] **Step 4:** показать владельцу итог и спросить согласие на коммит. Сообщение: `feat(xlive): своя папка XLive и отдельный раздел в резервной копии`.

---

## Кусок B — запись

### Task 4: Модель записи и хранилище записей

**Files:**
- Modify: `feature-xlive/src/main/java/com/client/xvideos/xlive/model/XLiveBio.kt` — `@Serializable`
- Create: `feature-xlive/src/main/java/com/client/xvideos/xlive/model/XLiveRecord.kt`
- Create: `feature-xlive/src/main/java/com/client/xvideos/xlive/record/XLiveRecordStore.kt`
- Test: `feature-xlive/src/test/java/com/client/xvideos/xlive/record/XLiveRecordStoreTest.kt`

**Interfaces:**
- Produces:
  ```kotlin
  @Serializable enum class XLiveRecordStatus { RECORDING, FINISHING, READY, BROKEN }

  @Serializable data class XLiveRecordRoom(
      val title: String = "", val gender: String = "", val viewers: Int = 0,
      val ratingPercent: Int? = null, val broadcastStartedAt: Long = 0L,
  )

  @Serializable data class XLiveRecord(
      val id: Long,                 // время начала, unix-мс; имя папки
      val username: String,
      val status: XLiveRecordStatus = XLiveRecordStatus.RECORDING,
      val durationMs: Long = 0L,
      val sizeBytes: Long = 0L,
      val width: Int = 0,
      val height: Int = 0,
      val gaps: Int = 0,
      val positionSec: Float = 0f,
      val room: XLiveRecordRoom = XLiveRecordRoom(),
      val bio: XLiveBio? = null,
  )

  class XLiveRecordStore(
      dirPath: String, private val scope: CoroutineScope,
      private val diskDispatcher: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(1),
  ) {
      val records: StateFlow<ImmutableList<XLiveRecord>>   // от новых к старым по id
      fun dirOf(id: Long): File                            // <dirPath>/<id>
      fun put(record: XLiveRecord)                         // пишет <id>/record.json
      fun update(id: Long, change: (XLiveRecord) -> XLiveRecord)
      fun remove(id: Long)                                 // удаляет папку целиком
      fun refresh()
  }
  ```
  Файлы записи — константы в `XLiveRecordStore.Companion`: `RECORD_JSON = "record.json"`, `SNAPSHOT = "snapshot.jpg"`, `VIDEO = "video.mp4"`, `VIDEO_RAW = "video.part"`, `AUDIO_RAW = "audio.part"`, `MUX_TEMP = "video.mp4.part"`, `STORYBOARD = ".storyboard.jpg"`.
  Все операции — одна последовательная очередь, как у `XLiveRoomStore`. JSON — общий `Json { ignoreUnknownKeys = true }` проекта (найти: `grep -rn "ignoreUnknownKeys" core/src/main`).

- [ ] **Step 1: Тесты** (хранилище с `Dispatchers.Unconfined`, как `testRoomStore`):
  - `put_thenRefresh_readsBackNewestFirst` — записи с id 1, 3, 2 → `records.value.map { it.id } == listOf(3, 2, 1)` и после нового стора на той же папке.
  - `update_changesOnlyThatRecord`.
  - `remove_deletesFolderWithFiles` — папка `<id>` с `video.mp4` удалена.
  - `brokenJson_isSkipped` — папка с мусорным `record.json` не роняет чтение, в списке её нет.
  - `bio_roundTrips` — `XLiveBio(realName = "Anna", age = 24, interestedIn = listOf("Men"))` читается равным.
- [ ] **Step 2:** `:feature-xlive:testDebugUnitTest --tests "*XLiveRecordStoreTest"` — FAIL.
- [ ] **Step 3:** реализовать.
- [ ] **Step 4:** PASS.

### Task 5: Разбор плейлистов

**Files:**
- Create: `feature-xlive/src/main/java/com/client/xvideos/xlive/record/XLivePlaylists.kt`
- Test: `feature-xlive/src/test/java/com/client/xvideos/xlive/record/XLivePlaylistsTest.kt` + тексты плейлистов в `feature-xlive/src/test/resources/record/` (`master.m3u8`, `video.m3u8`, `audio.m3u8` — синтетические, адреса `https://edge.test/...`, без настоящих адресов)

**Interfaces:**
- Produces:
  ```kotlin
  data class XLiveVariant(val videoUrl: String, val audioUrl: String?, val bandwidth: Long, val width: Int, val height: Int)
  data class XLiveSegment(val msn: Long, val url: String, val durationSec: Double)
  data class XLiveMediaPlaylist(val initUrl: String?, val segments: List<XLiveSegment>) {
      val lastMsn: Long?            // номер последнего полного сегмента
  }
  fun parseBestVariant(master: String, masterUrl: String): XLiveVariant?
  fun parseMediaPlaylist(text: String, playlistUrl: String): XLiveMediaPlaylist?
  fun blockingReloadUrl(playlistUrl: String, nextMsn: Long): String   // добавляет _HLS_msn, сохраняя прочие параметры
  ```
- [ ] **Step 1: Тесты:**
  - `bestVariant_isMaxBandwidth_withItsAudioGroup` — в мастере 5 вариантов (896k…7128k), две группы аудио; результат: `height == 1080`, `audioUrl` — из группы этого варианта, относительные адреса разрешены от `masterUrl`.
  - `bestVariant_withoutAudioGroup_hasNullAudio`.
  - `mediaPlaylist_readsInitAndNumberedSegments` — `EXT-X-MEDIA-SEQUENCE:100`, 4 `EXTINF` по 1.6 → msn 100..103, `initUrl` из `EXT-X-MAP`; строки `EXT-X-PART`, `EXT-X-PRELOAD-HINT`, `EXT-X-RENDITION-REPORT` не дают сегментов.
  - `mediaPlaylist_garbage_isNull`.
  - `blockingReload_appendsMsnKeepingToken` — `https://edge.test/v.m3u8?t=abc` + 104 → `https://edge.test/v.m3u8?t=abc&_HLS_msn=104`.
- [ ] **Step 2:** FAIL. **Step 3:** реализовать. **Step 4:** PASS.

### Task 6: Движок записи

**Files:**
- Modify: `feature-xlive/src/main/java/com/client/xvideos/xlive/network/XLiveApi.kt` — `suspend fun fetchText(url: String): XLiveHttp<String>`, `suspend fun fetchBytes(url: String): XLiveHttp<ByteArray>` (код ответа без исключения на 4xx; `IOException` — только сеть)
- Create: `feature-xlive/src/main/java/com/client/xvideos/xlive/record/XLiveHttp.kt` — `data class XLiveHttp<T>(val code: Int, val body: T?)`
- Create: `feature-xlive/src/main/java/com/client/xvideos/xlive/record/XLiveRecordStop.kt`
- Create: `feature-xlive/src/main/java/com/client/xvideos/xlive/record/XLiveRecorder.kt`
- Test: `feature-xlive/src/test/java/com/client/xvideos/xlive/record/XLiveRecorderTest.kt` (+ `FakeRecordNet` в том же пакете тестов)

**Interfaces:**
- Consumes: `parseBestVariant`, `parseMediaPlaylist`, `blockingReloadUrl` (Task 5); `XLiveRoom` (есть).
- Produces:
  ```kotlin
  enum class XLiveRecordStop(val message: String, val isError: Boolean) {
      BUTTON("Запись сохранена", false),
      LEFT_ROOM("Запись сохранена", false),
      BACKGROUND("Запись сохранена", false),
      STREAM_ENDED("Эфир закончился — запись сохранена", false),
      NO_NETWORK("Нет сети — запись сохранена", true),
      NO_SPACE("Кончилось место — запись сохранена", true),
  }

  interface XLiveRecordNet {
      suspend fun room(name: String): XLiveRoom
      suspend fun text(url: String): XLiveHttp<String>
      suspend fun bytes(url: String): XLiveHttp<ByteArray>
  }

  class XLiveRecorder(
      private val username: String,
      private val videoOut: File, private val audioOut: File,
      private val net: XLiveRecordNet,
      private val usableBytes: () -> Long,
      private val nowMs: () -> Long,
      private val stallTimeoutMs: Long = 25_000L,
      private val reserveBytes: Long = 300L * 1024 * 1024,
  ) {
      /** Пишет до остановки; возвращает причину. Отмена корутины = BUTTON. */
      suspend fun run(): XLiveRecordStop
      val stats: StateFlow<XLiveRecordStats>   // videoSegments, audioSegments, gaps, bytes, width, height
  }
  data class XLiveRecordStats(val videoSegments: Int = 0, val audioSegments: Int = 0, val gaps: Int = 0, val bytes: Long = 0L, val width: Int = 0, val height: Int = 0)
  ```
  Поведение (спека, «Запись: процесс»):
  - свой адрес через `net.room`;
  - лучший вариант;
  - init и полные сегменты, каждая дорожка в своей корутине, перечитывание через `blockingReloadUrl(next)`;
  - номер в плейлисте больше ожидаемого — пропуск: `gaps += 1`, продолжить с наименьшего доступного;
  - 403/404/410 на плейлисте или init — новый адрес через `net.room`. Не `public` или пустой `hlsUrl` — `STREAM_ENDED`. После нового адреса, если ожидаемого номера в плейлисте нет и все номера меньше ожидаемого, — продолжить с последнего доступного, `gaps += 1`;
  - `IOException` — повтор через 1 с; если ни одна дорожка не продвинулась за `stallTimeoutMs` (по `nowMs`) — `NO_NETWORK`;
  - перед каждым сегментом: `usableBytes() < stats.bytes + reserveBytes` — `NO_SPACE`;
  - лог — `Timber.i("XLive: запись — %s, сегментов %d, пропусков %d", stop, n, gaps)`.
- [ ] **Step 1: Тесты** — `FakeRecordNet` отдаёт плейлисты из Task 5 с подставленными номерами и байты сегментов `"v100"`, `"a100"`…; часы и задержки — `StandardTestDispatcher(TestCoroutineScheduler())`, запуск через `TestScope(dispatcher).launch { run() }` + `scheduler.advanceUntilIdle()/advanceTimeBy`, без `runTest`:
  - `writesInitThenSegmentsInOrder_bothTracks` — после 3 обновлений плейлиста: `videoOut` == `"vinit"+"v100"+"v101"+"v102"`, `audioOut` аналогично.
  - `numberJump_countsGap_andContinues` — плейлист перескочил с 101 на 104: `gaps == 1`, записаны 104, 105.
  - `playlist403_takesFreshAddress_andContinuesSameNumbers` — после 403 `net.room` вызван второй раз, номера продолжаются без пропуска.
  - `refreshedAddress_withRestartedNumbers_continuesFromEdge` — новый адрес с номерами 5..8 при ожидаемом 120: записан 8, `gaps == 1`, нет зацикливания (не больше N запросов).
  - `roomOffline_onRefresh_stopsStreamEnded` — после 403 `room` → `status = "offline"`: `run() == STREAM_ENDED`, записанное на месте.
  - `networkDown_longerThanStall_stopsNoNetwork` — `IOException` на всё после 2 сегментов, `advanceTimeBy(26_000)`: `NO_NETWORK`.
  - `networkBlip_shorterThanStall_continues` — 3 ошибки подряд, потом ответы: запись продолжается, `gaps` по нумерации.
  - `lowSpace_stopsNoSpace_beforeWriting` — `usableBytes = { 100 }`: `NO_SPACE`, сегмент не дописан.
  - `cancel_returnsButton` — отмена корутины `run()`.
- [ ] **Step 2:** FAIL. **Step 3:** реализовать `XLiveRecorder` и методы `XLiveApi`. **Step 4:** PASS.

### Task 7: Склейка, жизненный цикл записи, доклейка

**Files:**
- Create: `feature-xlive/src/main/java/com/client/xvideos/xlive/record/XLiveMuxer.kt` — `fun interface XLiveMuxer { suspend fun mux(video: File, audio: File?, out: File): Long }` (возвращает длительность, мс; бросает `IOException`) и `object MediaMuxerJoin : XLiveMuxer` — `MediaExtractor` по каждому файлу, `MediaMuxer(OUTPUT_FORMAT_MPEG_4)`, сдвиг времени: из каждой метки вычитается `min(первая метка видео, первая метка звука)`; без перекодирования
- Create: `feature-xlive/src/main/java/com/client/xvideos/xlive/record/XLiveRecordings.kt`
- Create: `feature-xlive/src/main/java/com/client/xvideos/xlive/record/XLiveRecordingSession.kt`
- Modify: `app/src/main/java/com/client/xvideos/screenSettings/backup/BackupEngine.kt:114` — после `XLiveSaved.refreshAll()` ещё `XLiveRecordings.refresh()`
- Test: `feature-xlive/src/test/java/com/client/xvideos/xlive/record/XLiveRecordingsTest.kt`

**Interfaces:**
- Consumes: `XLiveRecordStore` (Task 4), `XLiveRecorder`, `XLiveRecordStop` (Task 6), `XLiveLegacyFolder.move` (Task 1).
- Produces:
  ```kotlin
  /** Записи на процесс: хранилище, текущая запись, доклейка. Как XLiveSaved — объект, хранилище лениво. */
  object XLiveRecordings {
      val store: XLiveRecordStore
      fun start(room: XLiveRoom, bio: XLiveBio?): XLiveRecordingSession   // предыдущая сессия, если есть, останавливается с LEFT_ROOM
      fun finishPending()      // доклейка RECORDING/FINISHING-записей, у которых нет живой сессии; зовёт ScreenXLive при входе в раздел
      fun refresh()            // после восстановления копии
  }

  /** То, что видит комната; подменяется в тестах XLiveRoomSM. */
  interface XLiveRecordingHandle {
      val state: StateFlow<XLiveRecordingUi>
      fun attachBio(bio: XLiveBio)                 // дописать анкету, если пришла позже
      fun stop(reason: XLiveRecordStop)            // идемпотентно
  }

  class XLiveRecordingSession internal constructor(…) : XLiveRecordingHandle {
      val id: Long
  }

  data class XLiveRecordingUi(
      val isRecording: Boolean = false,
      val isFinishing: Boolean = false,
      val startedAtElapsedMs: Long = 0L,   // SystemClock.elapsedRealtime() в момент старта — для плашки
  )
  ```
  Ход сессии:
  1. `id = System.currentTimeMillis()`; `store.put(XLiveRecord(id, username, room = …, bio = bio))`.
  2. Снимок `xLiveSnapshotUrl` скачивается в `<id>/snapshot.jpg` в фоне; ошибка снимка не мешает записи.
  3. Работает `XLiveRecorder.run()`.
  4. Затем статус `FINISHING`, `muxer.mux(video.part, audio.part, video.mp4.part)`, переименование в `video.mp4`, удаление `.part`.
  5. Статус `READY`, `durationMs`, `sizeBytes`, `width/height`, `gaps`.
  6. Снекбар `EventBus.postEvent(Event.ShowSnackBar(if (stop.isError) UiMessage.Error(stop.message) else UiMessage.Success(stop.message)))`.

  Особые случаи:
  - ни одного сегмента видео — папка удаляется, снекбар `UiMessage.Error("Запись не сохранена: нет данных")`;
  - склейка бросила — статус `BROKEN`, сырые файлы остаются, снекбар `UiMessage.Error("Запись не склеилась")`.

  Сессия живёт в `scope` объекта (`SupervisorJob`), не в ScreenModel. Зависимости для тестов — `internal`-конструктор `XLiveRecordingsCore(store, net, muxer, scope, now…)`, объект — тонкая обёртка.
- [ ] **Step 1: Тесты** (`XLiveRecordingsCore` с фейковым `XLiveRecorder`-раннером или `FakeRecordNet` из Task 6 и фейковым `XLiveMuxer`, пишущим `"muxed"`):
  - `stop_muxesAndMarksReady_deletesRaw` — после `stop(BUTTON)`: в папке `video.mp4 == "muxed"`, нет `*.part`, статус `READY`, `sizeBytes == 5`.
  - `noVideoSegments_removesRecord` — стоп до первого сегмента: папки нет, в `store.records` пусто.
  - `muxFails_marksBroken_keepsRaw`.
  - `finishPending_muxesLeftovers` — в папке `record.json` со статусом `RECORDING`, `video.part`, `audio.part`, сессии нет: после `finishPending()` — `READY`, `video.mp4` есть.
  - `finishPending_withMuxTempLeftover_restartsMux` — есть `video.mp4.part` (обрывок): он перезаписан, итог `READY`.
  - `finishPending_skipsLiveSession` — запись текущей сессии не трогается.
  - `start_whileRecording_stopsPreviousWithLeftRoom`.
  - `attachBio_writesBioIntoRecord`.
- [ ] **Step 2:** FAIL. **Step 3:** реализовать. `MediaMuxerJoin` юнит-тестами не покрыть (`MediaMuxer` — заглушка в JVM-тестах): в отчёте так и сказать, проверка — на телефоне (Task 8, шаг проверки) и `ffprobe` вытянутого файла (только метаданные). **Step 4:** PASS.

### Task 8: Кнопка и плашка в комнате; проверка и коммит B

**Files:**
- Create: `feature-xlive/src/main/java/com/client/xvideos/xlive/ui/room/atom/XLiveRecordButton.kt` — `@Composable fun XLiveRecordButton(recording: Boolean, finishing: Boolean, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier)`; круглая 38dp как `XLiveSaveButton`; не пишет — красная точка `Icons.Filled.FiberManualRecord` на `XLiveColors.Pill`; пишет — `Icons.Filled.Stop` на `XLiveColors.Accent`; `finishing` — недоступна; `HintTooltip`: «Записать эфир» / «Остановить запись» / «Запись сохраняется»
- Create: `feature-xlive/src/main/java/com/client/xvideos/xlive/ui/room/atom/XLiveRecordingBadge.kt` — `@Composable fun XLiveRecordingBadge(startedAtElapsedMs: Long, modifier: Modifier = Modifier)`; «● 03:12», тикает раз в секунду от `SystemClock.elapsedRealtime()`; формат — `mm:ss`, с часом `h:mm:ss` (функция `recordingElapsedLabel(ms: Long): String` в `model/XLiveLabels.kt`, тест в `XLiveLabelsTest`)
- Modify: `XLiveRoomHeader.kt` — параметры `recording: XLiveRecordingUi`, `canRecord: Boolean`, `onToggleRecording: () -> Unit`; кнопка третьей, после `XLiveSaveButton`, отступ 8dp
- Modify: `XLiveRoomPlayer.kt` — параметр `recordingStartedAt: Long?`; плашка `Alignment.TopStart`, отступ 8dp
- Modify: `XLiveRoomActions.kt` — `onToggleRecording: () -> Unit = {}`
- Modify: `XLiveRoomSM.kt`:
  - `recording: StateFlow<XLiveRecordingUi>`;
  - `fun toggleRecording()` — старт только в `Live`, с последней `room` и `_bio.value`; анкета, пришедшая позже, — `session.attachBio`;
  - `fun onScreenGone(changingConfigurations: Boolean)` — `false` → `stop(LEFT_ROOM)`;
  - `fun onAppBackground(changingConfigurations: Boolean)` — `false` → `stop(BACKGROUND)`;
  - `onDispose()` ScreenModel → `stop(LEFT_ROOM)`;
  - зависимость `startRecording: (XLiveRoom, XLiveBio?) -> XLiveRecordingHandle = XLiveRecordings::start` (Task 7) — в тестах фейк.
- Modify: `ScreenXLiveRoom.kt` (только `Content()`):
  - `DisposableEffect(vm)` → `onDispose { vm.onScreenGone(activity?.isChangingConfigurations == true) }`;
  - `LifecycleEventEffect(Lifecycle.Event.ON_STOP) { vm.onAppBackground(activity?.isChangingConfigurations == true) }`;
  - `LocalView.current.keepScreenOn = recording.isRecording` через `DisposableEffect(recording.isRecording)` со сбросом в `onDispose`.
- Test: `feature-xlive/src/test/java/com/client/xvideos/xlive/ui/room/XLiveRoomSMTest.kt` — дополнить

- [ ] **Step 1: Тесты** в `XLiveRoomSMTest` (фейковый `startRecording`, считает старты и причины стопа):
  - `toggleRecording_live_startsWithRoomAndBio`.
  - `toggleRecording_notLive_doesNothing`.
  - `toggleRecording_twice_stopsWithButton`.
  - `screenGone_notChangingConfigurations_stopsWithLeftRoom`.
  - `screenGone_whileChangingConfigurations_keepsRecording`.
  - `appBackground_notChanging_stopsWithBackground`.
  - `bioArrivesAfterStart_isAttached`.
  - `recordingElapsedLabel` в `XLiveLabelsTest`: `0 → "00:00"`, `192_000 → "03:12"`, `3_725_000 → "1:02:05"`.
- [ ] **Step 2:** FAIL. **Step 3:** реализовать. **Step 4:** PASS; превью новых атомов собираются.
- [ ] **Step 5: гейт** — зелёный.
- [ ] **Step 6: телефон — сначала главный риск.**
  1. Открыть комнату в эфире.
  2. Записать 3 минуты при идущем просмотре. Плеер всё это время играет, без «Не удалось получить поток».
  3. Остановить кнопкой.
  4. Из `record.json` (через `run-as cat`) взять `gaps`, `durationMs`, `sizeBytes`, `width/height`.
  5. Сравнить `durationMs` с настенным временем записи.
  6. `adb exec-out run-as … cat files/store/XLive/Records/<id>/video.mp4 > scratch` и `ffprobe -show_entries format=duration:stream=codec_type` — две дорожки, длительность ≈ записи. Файл не открывать и не смотреть, после — удалить.

  **Цифры пропусков сообщить владельцу.** Если пропусков больше ~1 на минуту, сначала опережающая загрузка (части по `PRELOAD-HINT` или параллельные запросы сегментов), потом снова к владельцу — так в спеке.
- [ ] **Step 7: телефон — остановки.** Каждая даёт `READY` и снекбар:
  - «Назад»;
  - тег из темы;
  - Home;
  - кнопка питания;
  - поворот посреди записи — запись продолжается: `record.json` в `RECORDING`, сегменты растут.
- [ ] **Step 8: складной AVD.** Сложение и раскрытие посреди записи — запись идёт. Сеть AVD до сайта может не пустить, тогда так и сказать, проверка остаётся на телефоне.
- [ ] **Step 9:** итог владельцу, согласие, коммит: `feat(xlive): запись эфира кнопкой в шапке комнаты`.

---

## Кусок C — общие части плеера X в `:core`

### Task 9: Перенос плеера скачанного ролика и сеток кадров

**Files** (`git mv`; пакет назначения `com.client.xvideos.common.videoplayer.local`, сетки — `com.client.xvideos.common.videoplayer.storyboard`):
- из `feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/`:
  - `atom/FitModeToggle.kt`, `atom/OrientationAndSystemBarsEffect.kt`, `atom/ResumePlaybackPill.kt`, `atom/StoryboardFrame.kt`;
  - `model/ControlsAutoHide.kt`, `model/PanelTouchTarget.kt`, `model/rememberStoryboardImage.kt`, `model/SeekDrag.kt`, `model/SeekScrub.kt`, `model/StoryboardGrid.kt`;
  - `molecule/LocalPlayerFullScreenTopBar.kt`, `molecule/LocalVideoPlayerContent.kt`, `molecule/SeekPreviewFollow.kt`, `molecule/SeekPreviewOverlay.kt`;
  - `molecule/X_PlayerBottomBar.kt` → `PlayerBottomBar.kt`, функция `PlayerBottomBar`;
- из `feature-x/src/main/java/com/client/xvideos/x/feature/saved/`: `LocalStoryboards.kt`, `LocalStoryboardSheet.kt`, `RetrieverStoryboardRenderer.kt`;
- константы `STORYBOARD_COLUMNS`, `STORYBOARD_ROWS` из `x/model/StoryboardLayout.kt` — в `storyboard/StoryboardSheet.kt` в `:core`; в X — импорт оттуда;
- тесты вместе с кодом: `ControlsAutoHideTest`, `SeekDragTest`, `StoryboardGridTest`, `X_PlayerBottomBarTest` → `PlayerBottomBarTest`, `LocalStoryboardsTest`, `LocalStoryboardSheetTest` → в `core/src/test/java/com/client/xvideos/common/videoplayer/...`;
- в `:feature-x` — импорты в `ScreenX_LocalVideoPlayer.kt`, `ScreenX_VideoPlayer.kt`, `VideoPlayerContentView.kt`, `PlayerFullScreenTopBar.kt`, `SavedX_Downloads.kt` и всех, кто использует перенесённое (`grep -rln` по старым пакетам должен вернуть пусто).

**Interfaces:**
- Produces:
  - `LocalVideoPlayerContent(…, muted: Boolean = true, onToggleMute: (() -> Unit)? = null)`: хост создаётся с `isMuted = muted`, `LaunchedEffect(muted)` → `host.mute()`/`host.unmute()`. Вызов X не меняется (по умолчанию без звука, кнопки нет).
  - `PlayerBottomBar(…, muted: Boolean = true, onToggleMute: (() -> Unit)? = null)`: при `onToggleMute != null` — кнопка звука `Icons.AutoMirrored.Filled.VolumeOff/VolumeUp` перед меню скорости, с `HintTooltip` «Включить звук» / «Выключить звук».
  - `LocalStoryboards(dir: String, scope: CoroutineScope, renderer: StoryboardRenderer, io: CoroutineDispatcher = Dispatchers.IO, videoFile: (Long) -> File = { File(dir, "$it.mp4") }, storyboardFile: (Long) -> File = { File(dir, localStoryboardFileName(it)) })` — X по умолчанию как раньше.

- [ ] **Step 1:** до переноса — `:feature-x:testDebugUnitTest` зелёный, сохранить число тестов из XML.
- [ ] **Step 2:** перенос `git mv` + пакеты + импорты; `git diff --cached -M --stat` — переименования, не удаление+создание.
- [ ] **Step 3: тесты:**
  - `LocalStoryboardsTest.customFileMapping_usesGivenFiles` — `videoFile = { File(dir, "$it/video.mp4") }`, `storyboardFile = { File(dir, "$it/.storyboard.jpg") }`; `obtain(7)` рендерит из `7/video.mp4` в `7/.storyboard.jpg`.
  - `PlayerBottomBarTest`: без `onToggleMute` кнопки звука нет, с ним — есть и зовёт колбэк (в стиле существующего `X_PlayerBottomBarTest`).
- [ ] **Step 4:** `:core:testDebugUnitTest` и `:feature-x:testDebugUnitTest` — PASS. Число тестов X + перенесённых в `:core` = прежнему + новые.
- [ ] **Step 5:** гейт — зелёный.
- [ ] **Step 6: телефон — X без изменений.** Уровень проверки — как в прошлых проходах по плееру X: по дампам, без снимков с содержимым.
  - Плеер скачанного ролика: пауза, ползунок с превью кадра, скорость, на весь экран с поворотом, свайп-перемотка, «Продолжить с …», без звука.
  - Плеер ролика из сети: панель та же.
- [ ] **Step 7:** итог владельцу, согласие, коммит: `refactor(player): общие части плеера скачанного ролика — в :core`.

---

## Кусок D — «Записи» и экран записи

### Task 10: Подвкладка «Записи»

**Files:**
- Modify: `feature-xlive/.../ui/saved/XLiveSavedTab.kt` — `RECORDS("Записи")` четвёртым
- Create: `feature-xlive/.../ui/saved/XLiveRecordsState.kt` — `@Immutable data class XLiveRecordsState(val records: ImmutableList<XLiveRecord> = persistentListOf(), val isLoaded: Boolean = false)`
- Create: `feature-xlive/.../ui/saved/XLiveRecordsActions.kt` — `onOpen: (Long) -> Unit`, `onRemove: (Long) -> Unit`
- Create: `feature-xlive/.../ui/saved/molecule/XLiveRecordsGrid.kt` — `XLiveRecordsGrid(state, gridState: LazyGridState, actions, modifier)`: `adaptiveGridColumns()`, `adaptiveListPadding`, заглушка «Здесь будут записи эфиров — кнопка записи в шапке комнаты», диалог удаления по долгому нажатию
- Create: `feature-xlive/.../ui/saved/molecule/XLiveRecordCard.kt` — `XLiveRecordCard(record, snapshot: File?, onClick, onLongClick, modifier)`:
  - вид `XLiveSavedRoomCard`;
  - на снимке справа снизу: `READY` — длительность (`recordingElapsedLabel(durationMs)`), `RECORDING`/`FINISHING` — «сохраняется», `BROKEN` — «не завершена». Плашки — атом `ui/saved/atom/XLiveRecordStatusBadge.kt`;
  - под ником — `recordDateSizeLabel(id, sizeBytes)` = «26 окт, 21:40 · 2,1 ГБ». Функция — в `model/XLiveLabels.kt`, размер — `formatBytes` из `core/.../common/util`.
- Create: `feature-xlive/.../ui/saved/molecule/XLiveRemoveRecordDialog.kt` — «Удалить запись?», текст «Файл записи удалится с телефона.», «Удалить» / «Отмена»
- Modify: `XLiveSM.kt`:
  - зависимость `records: XLiveRecordStore = XLiveRecordings.store`;
  - `val recordsState: StateFlow<XLiveRecordsState>`;
  - `val recordsPosition = LazyGridPosition()`;
  - `fun removeRecord(id: Long)`;
  - ветки `RECORDS` во всех `when (_savedTab.value)` (`refreshSaved`, `removeSaved`, строка ~249);
  - при входе в «Избранное» — `XLiveRecordings.finishPending()`.
- Modify: `ScreenXLive.kt` — ветка `XLiveSavedTab.RECORDS -> XLiveRecordsGrid(...)`; `onOpen` → `navigator.push(ScreenXLiveRecord(id))`; позиция `vm.recordsPosition.rememberState()`
- Test: `XLiveSMTest` — дополнить; `XLiveLabelsTest`

- [ ] **Step 1: Тесты:**
  - `recordsTab_showsRecordsNewestFirst`.
  - `removeRecord_removesFromStore`.
  - `selectRecordsTab_keepsOtherTabsPositions`.
  - `recordDateSizeLabel(id = <26.10.2026 21:40 в фиксированном поясе теста>, size)` начинается с `"26 окт, 21:40 · "` и кончается `formatBytes(size)`.
- [ ] **Step 2:** FAIL. **Step 3:** реализовать. **Step 4:** PASS.

### Task 11: Экран записи; проверка и коммит D

**Files:**
- Create: `feature-xlive/.../ui/record/ScreenXLiveRecord.kt`:
  - `class ScreenXLiveRecord(private val id: Long) : Screen`, `key = "ScreenXLiveRecord_$id"`;
  - `OrientationAndSystemBarsEffect(vm.isFullScreen)`;
  - `@Composable fun ScreenXLiveRecordContent(record: XLiveRecord?, storyboard: File?, isSaved: Boolean, muted: Boolean, isFullScreen: Boolean, aboutExpanded: Boolean, actions: XLiveRecordActions, modifier: Modifier = Modifier)`.
- Create: `feature-xlive/.../ui/record/XLiveRecordSM.kt`:
  - `XLiveRecordSM(id, store = XLiveRecordings.store, saved = XLiveSaved.my, storyboards: suspend (Long) -> File? = …)`;
  - `record: StateFlow<XLiveRecord?>`;
  - `isSaved`;
  - `muted` — `true` при открытии, решение 2 спеки;
  - `isFullScreen`, `aboutExpanded`;
  - функции `toggleSaved()`, `toggleMute()`, `toggleFullScreen()`, `exitFullScreen()`, `toggleAbout()`, `savePosition(sec: Float)` → `store.update(id) { it.copy(positionSec = sec) }`, не чаще раза в 3 с — как `onSaveProgress` у X.
- Create: `feature-xlive/.../ui/record/XLiveRecordActions.kt` — `onBack`, `onOpenRoom`, `onOpenTag(String)`, `onToggleSaved`, `onToggleMute`, `onToggleFullScreen`, `onExitFullScreen`, `onToggleAbout`, `onSavePosition(Float, Int)`
- Create: `feature-xlive/.../ui/record/molecule/XLiveRecordHeader.kt`:
  - аватар, ник, имя, плашка «ЗАПИСЬ · 26 ОКТ 21:40» — атом `ui/record/atom/XLiveRecordBadge.kt`, серый `XLiveColors.Pill`;
  - справа `XLiveSaveButton` и атом `ui/record/atom/XLiveOpenRoomButton.kt` (`Icons.AutoMirrored.Filled.OpenInNew`, `HintTooltip` «Открыть комнату»);
  - плашки `XLiveStatPill`: длительность, размер, зрители, рейтинг, подписчики.
- Переиспользовать `XLiveRoomTopic` и `XLiveRoomAboutCard` из `ui/room/molecule` как есть.
- Плеер: `LocalVideoPlayerContent` из Task 9:
  - `fileUrl = File(store.dirOf(id), VIDEO).toURI().toString()`;
  - `resumePosition = record.positionSec.takeIf { it > 0f }`;
  - `muted`, `onToggleMute`, `title = record.username`;
  - `storyboard` — через `LocalStoryboards(AppPath.xlive_records, …, videoFile = { File(dirOf(it), VIDEO) }, storyboardFile = { File(dirOf(it), STORYBOARD) })` — один экземпляр в `XLiveRecordings`.
- Портрет: колонка с прокруткой; плеер 16:9; ширина — `adaptiveContentWidth()` после фона. На весь экран — только плеер.
- Test: `feature-xlive/src/test/java/com/client/xvideos/xlive/ui/record/XLiveRecordSMTest.kt`

- [ ] **Step 1: Тесты:**
  - `opensMuted`.
  - `savePosition_writesIntoRecord`.
  - `toggleSaved_putsUsernameIntoMy_withAgeFromBio`.
  - `missingRecord_isNull` — id нет в хранилище.
  - `fullScreen_toggleAndExit`.
- [ ] **Step 2:** FAIL. **Step 3:** реализовать. **Step 4:** PASS.
- [ ] **Step 5:** гейт — зелёный.
- [ ] **Step 6: телефон.**
  1. «Записи»: карточка записи из куска B — снимок, длительность, дата, размер. Снимки не делать, данные из дампа.
  2. Экран записи: плеер играет, ползунок, превью кадра, на весь экран с поворотом и обратно, звук кнопкой.
  3. Повторное открытие — «Продолжить с …».
  4. «Открыть комнату» — экран комнаты по нику.
  5. Тег — экран тега.
  6. Удаление — папки нет.
  7. Резервная копия: в XLive три галочки с размерами; копия только с XLive, восстановление «объединением» — запись на месте.
- [ ] **Step 7: складной AVD.** «Записи» и экран записи:
  - внешний экран;
  - внутренний экран, портрет и альбомная;
  - Medium;
  - сложение посреди прокрутки «Записей» — позиция и вкладка сохраняются (по дампу).

  Если на AVD записи нет: положить тестовую запись (папка с `record.json` и коротким mp4 из `screenrecord` эмулятора, как в памяти проекта для X) через `run-as`.
- [ ] **Step 8:** итог владельцу, согласие, коммит: `feat(xlive): вкладка «Записи» и экран записи с плеером`.
- [ ] **Step 9:** спека — раздел «Как реализовано» с sha кусков A–D, отдельным docs-коммитом с согласия владельца.
