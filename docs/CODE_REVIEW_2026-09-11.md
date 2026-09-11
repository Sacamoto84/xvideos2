# Код-ревью xvideos — проход 10

> **Срез:** `6cbf5b0` + незакоммиченные изменения рабочего дерева · **Статус:** открыт · **Индекс:** [все документы](README.md)

База прохода — `master` на 11.09.2026 плюс лежащая в рабочем дереве
правка P2P (ручное подтверждение приёма, 7 файлов). Прошлый срез с
находками — `1dc7608` (проход 8); после него в коде изменилось немного:
косметика `feature-l` (миграция на Material3, работа с вырезом),
фикс `MainActivity` под Android 15, обновление `libs.versions.toml`.
Проход 9 (10.09) статусы снимал, новых находок почти не дал.

Линзы: **статус находок проходов 8–9**, **незакоммиченный P2P-дифф**,
**регрессии UI после миграции feature-l**, **состояние сборки и гигиена
репозитория**.

Ключевой итог в двух строках: **оба проверочных гейта сейчас красные**
(detekt — на `HEAD`, юнит-тесты — в рабочем дереве), а правка S1/T1 из
рабочего дерева закрывает находки прохода 8 лишь наполовину и при этом
ломает сборку тестов.

> **Обновление после прохода (решение владельца):** P2P оставлен как
> есть, включая эту правку — см. блок «Статус» ниже. Находки раздела
> «Оценка P2P-правки» сохранены для истории, к работе они больше не
> относятся. Из красных гейтов остался актуальным только detekt (C6,
> видеоплеер — не P2P).

## Статус находок прохода 8

| Находка | Статус | Комментарий |
| --- | --- | --- |
| S1 | частично, в рабочем дереве | авто-приём у получателя убран, код показывается — но только одной стороне (см. ниже) |
| S2 | открыт | лимитов на объём бандла по-прежнему нет ни в одной из трёх точек |
| S3 | частично | `CollectionDB` валидирует имя через `CollectionName`; `FileDB` берёт строку как есть ([FileDB.kt:62](../core/src/main/java/com/client/xvideos/common/fileDB/FileDB.kt:62)) |
| S4 | открыт | `profileable` в основном манифесте ([AndroidManifest.xml:97](../app/src/main/AndroidManifest.xml:97)); `src/debug/AndroidManifest.xml` не существует |
| C1 | открыт | `tryEmit` без проверки результата, `onBufferOverflow` не задан ([NearbyClientImpl.kt:46](../core/src/main/java/com/client/xvideos/common/p2p/nearby/NearbyClientImpl.kt:46)) |
| C2 | открыт | `catch (e: Exception)` с комментарием про `NoClassDefFoundError` ([SecureCredentialStore.kt:89](../core/src/main/java/com/client/xvideos/common/settings/SecureCredentialStore.kt:89)) |
| C3 | открыт | `"$name.tmp"`, без fsync ([AtomicWrite.kt:17](../core/src/main/java/com/client/xvideos/common/io/AtomicWrite.kt:17)) |
| C4 | открыт | `manifest`/`receivedFiles` не сбрасываются после импорта ([P2pReceiveController.kt:108](../core/src/main/java/com/client/xvideos/common/p2p/P2pReceiveController.kt:108)) |
| T1 | в основном закрыт в рабочем дереве | `import` на `Dispatchers.IO`, `receiveToCache` уведён с главного потока; остаток — новая находка T4 |
| T2 | частично | `DropdownMenuItem_Like/Follow` уже в корутине; `init` у `SavedRed`, клики `R_Screen_Root`/`RedProfileUserImage` и `deleteRecursively()` в [R_Screen_CollectionTab.kt:185](../feature-r/src/main/java/com/client/xvideos/r/ui/screens/screenRoot/screenCollectionTab/R_Screen_CollectionTab.kt:185) — синхронно на главном |
| A1 | открыт | детектор по-прежнему слеп к `private var`/`lateinit var`/`@Volatile` на отдельной строке, `ALLOWED` не пополнен ([GlobalStateTest.kt:96](../app/src/test/java/com/client/xvideos/arch/GlobalStateTest.kt:96)) |
| A2 | открыт | `mutableStateListOf` в `FileDB.list`, диалоговые флаги в `LinkCollectionStore` |
| A3 | открыт | плюс третий файл: [R_Saved_Creator.kt:56](../feature-r/src/main/java/com/client/xvideos/r/common/saved/R_Saved_Creator.kt:56) — мёртвый `@OptIn` над функцией без корутин |

Находки прохода 9: UI1 (`screenType` в companion у `ScreenRedExplorer`)
и UI2 (ScreenModel отсутствует в большинстве экранов) — обе открыты.

## Оценка P2P-правки из рабочего дерева

Правка убирает авто-приём у получателя, показывает `authenticationDigits`
и добавляет кнопки «Принять/Отклонить»; копирование payload'ов и импорт
уводятся на `Dispatchers.IO`. Направление верное, но в текущем виде:

1. **S1 закрыта наполовину.** Код подтверждения показывает только
   получатель; отправитель по-прежнему принимает соединение
   автоматически и код не показывает
   ([P2pShareController.kt:109-112](../core/src/main/java/com/client/xvideos/common/p2p/P2pShareController.kt:109)).
   Сверка цифр — двусторонняя процедура: получателю не с кем сверять.
   На стороне отправителя атака осталась: подставной endpoint с чужим
   именем в списке устройств получает бандл после одного тапа по списку.
2. **Правка не закончена** — тесты не компилируются (C5).
3. **Фоновый приём сломан** (C7).
4. Новый ввод-вывод на IO собран как неуправляемый скоуп на каждый
   payload (T4).

Коммитить в текущем виде нельзя. *(Вывод на момент прохода; решением
владельца P2P оставлен как есть — эта рекомендация не исполняется.)*

## Новые находки

### C5 — Рабочее дерево ломает сборку тестов. Высокая.

[P2pReceiveControllerTest.kt:45](../core/src/test/java/com/client/xvideos/common/p2p/P2pReceiveControllerTest.kt:45),
также строки 67, 78, 131.

`P2pEvent.ConnectionInitiated` получил третий параметр
`authenticationDigits`, но четыре вызова в тесте его не передают:

```
:core:compileDebugUnitTestKotlin FAILED
No value passed for parameter 'authenticationDigits'. (×4)
```

Коммит с такой правкой уронит `testDebugUnitTest` у любого, кто её
заберёт. Правка тривиальна (передать код в четырёх вызовах), но её нет —
то есть правку не прогоняли даже компиляцией тестов.

### C6 — detekt красный на `HEAD`: baseline протух. Высокая.

[CMPlayer2.kt:36](../core/src/main/java/com/client/xvideos/common/videoplayer/util/CMPlayer2.kt:36),
[baseline.xml:75](../config/detekt/baseline.xml:75)

В `CMPPlayer2` добавили параметр `volume: Float`, baseline-запись при
этом не обновилась — ID в baseline перечисляет 19 параметров, функция
принимает 20. С `ignoreFailures = false` в convention-плагине
`:core:detekt` падает:

```
:core:detekt FAILED
CMPPlayer2(...) has too many parameters. The current threshold is set to 12.
```

Файл и baseline в рабочем дереве не меняются, то есть **падение есть и
на чистом `HEAD`**. README проекта заявляет detekt с
`ignoreFailures = false` как рабочий гейт — фактически гейт красный
ровно с того коммита, где в плеер добавили `volume`. Проход 9 утверждал,
что detekt ронял только состояние `App.kt` — неполная диагностика.

Ближайший исход: обновить baseline-запись под текущую сигнатуру (или,
лучше, сгруппировать 20 параметров в объект конфигурации — это и есть
открытая задача из прохода 7).

### C7 — Фоновый приём не может принять передачу. Средняя.

[P2pReceiveController.kt:66-71](../core/src/main/java/com/client/xvideos/common/p2p/P2pReceiveController.kt:66),
[P2pBackgroundOverlay.kt:55-83](../core/src/main/java/com/client/xvideos/common/p2p/ui/P2pBackgroundOverlay.kt:55),
[MainActivity.kt:141](../app/src/main/java/com/client/xvideos/MainActivity.kt:141)

Настройка `p2p_background_receive` поднимает рекламу на весь сеанс из
`onCreate` Activity. Пока действовал авто-приём (S1), входящее соединение
в фоне проходило само. Теперь оно переводит контроллер в `Connecting` —
но `P2pBackgroundOverlay` слушает только `Progress/Success/Error` из
EventBus, события «ждёт подтверждения» нет, а `accept()` вызывается
только с экрана приёма. Итог: при закрытом экране приёма отправитель
видит вечное «Подключение…», подключение зависает до его дисконнекта,
после чего менеджер через 5 секунд молча перезапускает рекламу.

Это продуктовое решение, а не только код: варианты — уведомление с
кодом и кнопками, авто-приём только для фонового режима (осознанно
принять остаточный риск S1), или убрать настройку. Сейчас настройка
обещает то, чего не происходит.

### C8 — `ConnectionRejected` не обрабатывается приёмником. Низкая.

[P2pReceiveController.kt:63-105](../core/src/main/java/com/client/xvideos/common/p2p/P2pReceiveController.kt:63)

В `when` приёмника нет ветки `P2pEvent.ConnectionRejected` — событие
падает в `else -> Unit`. При авто-приёме этот путь был недостижим на
практике; с ручным подтверждением сценарий стал реальным: отправитель
отменил запрос после того, как получатель нажал «Принять», — экран
остаётся в `Connecting` с кнопками, которые больше ничего не делают.
Симметричная ветка у отправителя есть («Получатель отклонил»).

### C9 — Импортёры L/R берут файл из map без сверки с манифестом. Низкая.

[LCollectionBundleImporter.kt:27](../core/src/main/java/com/client/xvideos/common/p2p/imports/LCollectionBundleImporter.kt:27),
[RCollectionBundleImporter.kt:22](../core/src/main/java/com/client/xvideos/common/p2p/imports/RCollectionBundleImporter.kt:22)

`receivedFiles.values.firstOrNull()` — какой файл окажется первым в map,
тот и распакуется как zip коллекции. Манифест при этом не спрашивается.
Само по себе безобидно (в бандле один файл), но вместе с C4 — map не
чистится до следующего `start()` — запоздалый payload прошлой сессии
может подменить архив. Правильный источник — `manifest.files`.

### T4 — Неуправляемый скоуп на каждый payload. Средняя.

[NearbyClientImpl.kt:229](../core/src/main/java/com/client/xvideos/common/p2p/nearby/NearbyClientImpl.kt:229)

```kotlin
kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch { ... }
```

Правка T1 уводит копирование payload с главного потока — правильно. Но
реализовано как анонимный скоуп прямо в колбэке GMS, на каждый payload
заново: без родительского `Job`, без отмены из `stop()`/`stopAll()`
(копирование гигабайтного файла переживёт остановку приёма и уход с
экрана), с полностью квалифицированными именами вместо импортов, при
том что `import kotlinx.coroutines.launch` в тот же дифф добавлен.

Единственное место во всём P2P-тракте с по-настоящему неструктурированной
конкурентностью. Лечится одним полем класса
`CoroutineScope(SupervisorJob() + Dispatchers.IO)`, отменяемым в
`stopAll()`. Дополнительно стоит знать: `FilePayloadReceived` теперь
приходит из IO-потока, а `TransferProgress` — из колбэка на главном;
`MutableSharedFlow` это безопасно, но порядок событий между ними больше
не детерминирован (для текущей логики не важно).

### T5 — `SavedRed.tagsList` без синхронизации. Низкая.

[SavedRed.kt:30](../feature-r/src/main/java/com/client/xvideos/r/common/saved/SavedRed.kt:30)

`var tagsList = listOf<TagInfo>()` пишется из корутины на
`Dispatchers.IO` (`refreshTagList`) и читается из UI без `@Volatile` —
типичная проблема видимости. Остальные поля класса стейт-потоками уже
закрыты, это осталось.

### T6 — Бэкап/восстановление отменяются уходом с экрана. Низкая.

[BackupSettingsSection.kt:106](../app/src/main/java/com/client/xvideos/screenSettings/backup/BackupSettingsSection.kt:106),
129, 292

Корутины бэкапа и восстановления запущены на `rememberCoroutineScope()`.
Смена страницы настроек (`when(currentPage)` диспозит секцию) или уход
со экрана отменяет корутину посреди записи ZIP — на целевом URI остаётся
частичный файл. UI не застревает, но пользователь получает битый архив
без сообщения. Для часов-long операции нужен скоуп, живущий дольше
экрана (application scope + индикация прогресса).

### A5 — В git закоммичено 1146 файлов сессионных данных `.mimosa`. Средняя.

`.mimosa/hook-state/sess_*.json`, `*.continue.json`, `*.source` —
мусор инструментария разработки, попавший в индекс: 1146 файлов,
~120 тыс. строк в диффах между срезами. `.gitignore` каталог не
исключает. Из-за этого `git diff 1dc7608..HEAD` показывает 1166 файлов
вместо ~20 реально изменённых — история и ревью по диффу замусорены.

Лечение: убрать из индекса (`git rm -r --cached .mimosa`), добавить в
`.gitignore`, при желании вычистить историю. Отдельно решить судьбу
`.kilocode/mcp.json` (1 файл — возможно, осознанно).

### UI3 — Регрессии обработки выреза при миграции feature-l. Средняя.

Миграция с `displayCutoutPadding()`/`useCutoutPadding` на ручной
`getTopInsetDp()` прошла неровно, четыре места:

1. [LCollectionsTopBar.kt:39](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenRoot/tab/saved/collection/LCollectionsTopBar.kt:39)
   — топ-бар коллекций потерял `displayCutoutPadding()` совсем: ни в
   самом топ-баре, ни в хостах (`CollectionsGrid` кладёт его в
   `Scaffold(topBar=…)`, M3 Scaffold инсет не применяет, `ScreenSaved`
   паддит только bottom). Заголовок коллекции и иконка сортировки
   рендерятся от y=0 — под камерой на cutout-устройствах. Соседние таби
   (Albums, Likes) инсет применяют.
2. [ScreenAlbumList.kt:214-217](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/ScreenAlbumList.kt:214)
   — при непустом `title` хедер фиксирован в 40.dp без учёта выреза
   (текст заезжает под камеру при вырезе выше 40dp); при пустом —
   наоборот, `topInset` применяется всегда, даже с выключенной
   настройкой.
3. [L_ScreenSavedLikesTab.kt:97](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenRoot/tab/saved/likes/L_ScreenSavedLikesTab.kt:97)
   — сегмент-кнопки: замена `displayCutoutPadding()` на
   `padding(top = topInset)` потеряла боковую защиту в ландшафте.
4. Настройка `Settings.useCutoutPadding` стала no-op: читается в
   [ScreenAlbumList.kt:132](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/ScreenAlbumList.kt:132)
   и не используется; тумблер в настройках («смещение экрана сверху»)
   больше ни на что не влияет. Либо починить, либо убрать тумблер.

### UI4 — Скроллбар альбомов не учитывает header-спейсер. Низкая.

[L_ScreenSavedAlbumsTab.kt:60](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenRoot/tab/saved/albums/L_ScreenSavedAlbumsTab.kt:60)

`rememberVisibleRangePercentIgnoringFirstNForGrid(state)` вызван с
дефолтным `itemsToIgnore = 0`, хотя в гриде есть full-span спейсер
(item 0, `height(topInset)`). Хелпер имеет параметр ровно для этого
случая. Позиция и длина индикатора слегка врут.

### UI5 — Диалоги на `remember` и ключ `it.id` без дедупликации. Низкая.

[L_Screen_CollectionTab.kt:80-83](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenRoot/tab/saved/collection/L_Screen_CollectionTab.kt:80)
— `itemPendingAction/Rename/Delete`, `renameValue` на `remember`:
пересоздание Activity (поворот невозможен, но process death реален)
сбрасывает открытые диалоги и набранный текст. Аналогично
`ScreenAlbum.kt:126`.

[L_ScreenSavedAlbumsTab.kt:84](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenRoot/tab/saved/albums/L_ScreenSavedAlbumsTab.kt:84)
— `items(vm.albums, key = { it.id })` без дедупликации: дубль id роняет
`LazyLayout` («Key was already used») — ровно этот crash-mode задокументирован
у соседнего `ScreenAlbumList.kt:226`, где ключ делают уникальным.

## Мелочи, замеченные попутно

- **README отстал от стека**: заявлено Kotlin 2.4.10 / AGP 9.3.1 /
  Gradle 9.5.0, фактически 2.4.20 / 9.4.0 / 9.6.1; material3 прыгнул
  две alpha-версии подряд (1.5.0-alpha26 → alpha28) — по собственной
  пометке в `libs.versions.toml` alpha правит API между сборками,
  компиляцию feature-модулей после такого обновления стоит проверять
  явно.
- Из `libs.versions.toml` удалены все instrumented-test зависимости
  (espresso, uiautomator, ui-test-junit4) — UI-тестовой прослойки в
  проекте не осталось совсем; в README «149 юнит-тестов» стоит
  дополнить этим фактом.
- Устаревшие комментарии, противоречащие коду:
  [P2pReceiveManager.kt:21-23](../core/src/main/java/com/client/xvideos/common/p2p/P2pReceiveManager.kt:21)
  («через GlobalScope… только пока приложение активно» — ни то, ни
  другое), AndroidManifest.xml:60-62 (то же),
  [MainActivity.kt:100](../app/src/main/java/com/client/xvideos/MainActivity.kt:100)
  («На Android 15+» при guard `>= Q`, то есть API 29).
- [R_Saved_Collection.kt:25](../feature-r/src/main/java/com/client/xvideos/r/common/saved/R_Saved_Collection.kt:25)
  — `collectionDb.insert(...)` возвращает `Result`, который отброшен:
  при ошибке записи нет ни лога, ни снекбара (соседние операции
  результат обрабатывают).
- [ScreenP2pSend.kt:295](../core/src/main/java/com/client/xvideos/common/p2p/ui/ScreenP2pSend.kt:295)
  — deprecated `activity.requestPermissions(perms, 123)` без обработки
  результата; спасает перепроверка в `repeatOnLifecycle(RESUMED)`.
- `P2pManifest.metadataFileName` отправителем пишется, приёмной стороной
  не читается — поле держится только ради обратной совместимости старых
  сборок (это осознанно, фикс `d10cfc9`; стоит пометить в KDoc).
- Мёртвые импорты/suppress после миграции feature-l:
  `L_ScreenSavedAlbumsTab.kt:17,52`, `L_ScreenSavedLikesTab.kt:7,10`,
  `ScreenAlbum.kt:29`.

## Статус

> **Решение владельца от 11.09.2026: P2P не трогать.** Весь P2P-тракт
> (`core/.../p2p/`: приём, отправка, Nearby, импортёры и лежащий в рабочем
> дереве дифф) остаётся как есть, без правок и рефакторинга. Находки
> S1, S2, C1, C4, T1 (проход 8) и C5, C7, C8, C9, T4 (этот проход)
> переведены в разряд решений владельца — следующий проход P2P не
> открывает. Внимание — на остальную кодовую базу.

| Находка | Класс | Статус | Коммит |
| --- | --- | --- | --- |
| C5 | корректность | решение владельца — P2P не трогать | — |
| C6 | корректность | открыт (detekt, `CMPlayer2` — не P2P) | — |
| C7 | корректность | решение владельца — P2P не трогать | — |
| C8 | корректность | решение владельца — P2P не трогать | — |
| C9 | корректность | решение владельца — P2P не трогать | — |
| T4 | главный поток | решение владельца — P2P не трогать | — |
| T5 | конкурентность | открыт | — |
| T6 | жизненный цикл | открыт | — |
| A5 | архитектура | открыт | — |
| UI3 | UI | открыт | — |
| UI4 | UI | открыт | — |
| UI5 | UI | открыт | — |

## Проверка

```
.\gradlew.bat testDebugUnitTest --console=plain
BUILD FAILED — :core:compileDebugUnitTestKotlin: 4 × «No value passed
for parameter 'authenticationDigits'» (P2pReceiveControllerTest.kt:45,67,78,131).
:app:testDebugUnitTest — зелёный.

.\gradlew.bat detekt (в составе той же команды)
BUILD FAILED — :core:detekt: LongParameterList у CMPPlayer2 (20
параметров), baseline-запись не совпадает. :app:detekt — зелёный
(импорт launch в App.kt уже убран в рабочем дереве).
```

`:app:lintRelease` в этом проходе не запускался. Ручные сценарии на
устройстве не выполнялись.

Что стоит проверить на устройстве, когда до находок дойдут руки:

- приём при закрытом экране приёма с включённой настройкой
  `p2p_background_receive` — ожидается зависший «Подключение…» у
  отправителя (C7);
- сценарий «отправитель отменил после Принять» — ожидается вечный
  `Connecting` у получателя (C8);
- cutout-устройство, вертикально: таб коллекций L и альбомный список с
  непустым `title` — ожидается текст под камерой (UI3.1, UI3.2);
- ландшафт с вырезом сбоку: сегмент-кнопки на табе Likes L (UI3.3).

## Что осталось открытым

- **P2P заморожен решением владельца** — S1, S2, C1, C4, T1 (проход 8)
  и C5, C7, C8, C9, T4 (этот проход) не чинятся и не переоткрываются.
  Следующие ревью P2P не трогают.
- UI1, UI2 прохода 9 — без изменений.
- Из находок этого прохода остаются в работе: **C6** (detekt красный —
  baseline-запись `CMPPlayer2` протухла; `CMPlayer2` — видеоплеер,
  не P2P), **A5** (`.mimosa` в git: `git rm -r --cached .mimosa` плюс
  строка в `.gitignore`), **T5, T6, UI3–UI5** — по случаю работы в тех
  же файлах.
- Остальные 10 находок прохода 8 вне P2P: S3 (FileDB), S4, C2, C3,
  T2, A1, A2, A3 — без изменений.

## Что проверено и находок не дало

- **Дифф `MainActivity` (фикс Android 15)**: `navigationBars` вместо
  `tappableElement` — корректно для API 35; guard
  `isNavigationBarContrastEnforced` соответствует API-уровню свойства;
  двойного нижнего паддинга с `ScreenRoot.systemBarsPadding()` нет —
  `windowInsetsPadding` на Surface потребляет инсет, статус-бар скрыт.
- **Утечки контекста в P2P-тракте**: весь путь
  `App → P2pReceiveManager → sectionBundleImporter → NearbyClientImpl →
  LSendPreparer` принудительно проходит через `applicationContext`;
  импортёры держат только `File` и лямбды.
- **Порядок старта фоновой рекламы**: `storageCleanupGate.await()` перед
  P2P предотвращает гонку с пересозданием inbox; повторный вход на экран
  приёма уходит в `ensureAdvertising()`, а не во второй контроллер.
- **Zip-slip**: закрыт дважды — в `ZipUtils.unzip` и в
  `P2pBundleInstaller.install` (нормализация + `requireInside`), включая
  повторную проверку установщиком.
- **Миграция feature-l на Material3**: виджетов material1 в разделе не
  осталось (только не-визуальные `ExperimentalMaterialApi` у жестовых
  утилит). Единственное смешение material/material3 в проекте —
  `CustomBasicTextFieldContent.kt:33-38` в feature-r, диффом не тронуто.
- **Перестановка табов в `ScreenExplorer`**: иконки ↔ ветки `when` ↔
  `TabBarPoints` согласованы, внешние писатели ставят только 0 — reorder
  их не сломал.
- **Удаление `DUPLICATES` из `LCollectionFs`**: ссылок не осталось,
  персист sortOrder не хранит, `valueOf`-краша нет.
- **`VerticalScrollbar`**: контракт KDoc соблюдён (чтение в draw-фазе,
  guard'ы от деления на ноль и вылезания за дорожку).
- **`ScreenP2pSend`**: аккуратный `DisposableEffect`
  (`stop()` + `scope.cancel()` + оживление фоновой рекламы), отправка
  подтверждается доставкой всех payload'ов, раннего `Done` нет.
- **`App`/`CrashLog`/`onTerminate`**: цепочка uncaught-обработчиков
  сохранена, guard от незаполненного мониторa на месте.
