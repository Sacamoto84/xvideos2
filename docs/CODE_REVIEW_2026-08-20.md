# Код-ревью xvideos — проход 8

> **Срез:** `1dc7608` · **Статус:** открыт · **Индекс:** [все документы](README.md)

База прохода — состояние `master` на 20.08.2026. Прошлый срез с находками —
`5096919` (проход 7), после него 25 коммитов, 36 файлов кода и тестов,
+1498 / −284.

Линзы: **безопасность недоверенного входа** (S), **корректность и потеря
событий** (C), **главный поток и ввод-вывод** (T), **архитектура и сторожа**
(A).

Проход шёл чтением кода. На устройстве ничего из перечисленного не
воспроизводилось — где это меняет вес находки, сказано в ней самой.

## Находки

### S1 — P2P-соединение принимается автоматически, без сверки кода. Высокая.

[P2pReceiveController.kt:64](../core/src/main/java/com/client/xvideos/common/p2p/P2pReceiveController.kt:64),
[NearbyClientImpl.kt:193](../core/src/main/java/com/client/xvideos/common/p2p/nearby/NearbyClientImpl.kt:193),
[ScreenP2pReceive.kt:35](../core/src/main/java/com/client/xvideos/common/p2p/ui/ScreenP2pReceive.kt:35)

`onConnectionInitiated` отдаёт `info.authenticationDigits` — четыре цифры,
одинаковые на обоих телефонах. Это и есть вся защита Nearby Connections от
чужого подключения: пользователи сверяют цифры глазами и подтверждают. У нас
цифры уходят в `Timber.d` и больше нигде не появляются, а контроллер сразу
зовёт `nearby.acceptConnection(...)` с комментарием «Automatically accepting».
Экран приёма показывает `Connecting(endpointName)` как факт, а не как вопрос:
кнопок «принять/отклонить» на нём нет. `P2pReceiveController.reject()` не
вызывается ниоткуда — мёртвый код.

Что из этого следует. `serviceId` — это `com.client.xvideos.p2p`, он выводится
из имени пакета и секретом не является. Пока включена настройка
`p2p_background_receive`, `MainActivity` поднимает рекламу на весь сеанс
([MainActivity.kt:134](../app/src/main/java/com/client/xvideos/MainActivity.kt:134)),
а `P2pReceiveManager.handleStateChange` перезапускает её после каждого приёма и
после каждой ошибки. Любое устройство в радиусе Bluetooth/Wi-Fi Direct, знающее
`serviceId`, подключается и кладёт файлы в библиотеку — молча, без следа на
экране, кроме уведомления об успешном приёме. Обход каталога заблокирован
(см. `SafePath`, проход 7), а вот подмена содержимого — нет: `install` пишет с
`overwrite = true`, а `P2pInboxMerger.merge` перезаписывает совпадающие имена.

Чего у находки нет: атака не проверялась ни на устройстве, ни отдельным
клиентом Nearby. Оценка «высокая» — по цене последствия и тривиальности
условий, а не по замеренной эксплуатации.

### S2 — Объём принимаемого бандла ничем не ограничен. Средняя.

[P2pBundleInstaller.kt:24](../core/src/main/java/com/client/xvideos/common/p2p/P2pBundleInstaller.kt:24),
[ZipUtils.kt:43](../core/src/main/java/com/client/xvideos/common/zip/ZipUtils.kt:43)

Ни числа файлов в манифесте, ни размера каждого, ни суммарного объёма никто не
проверяет. `unzip` распаковывает архив целиком, не глядя ни на заявленный
размер записи, ни на итог, — классический zip-бомбовый профиль. Вместе с S1
это значит, что чужое устройство заполняет внутреннюю память до отказа.
`Payload.Type.FILE` Nearby пишет сразу на диск, то есть память процесса тут ни
при чём — кончится именно место в `filesDir`.

Простейшая мера — потолок на сумму `P2pManifestFile.size` и на распакованный
объём, с отказом до начала записи. Порог — решение владельца: библиотека сама
по себе большая.

Чего у находки нет: реального замера, при каком объёме устройство встаёт.

### S3 — Имя элемента не проверяется, хотя имя коллекции — проверяется. Низкая.

[FileDB.kt:61](../core/src/main/java/com/client/xvideos/common/fileDB/FileDB.kt:61),
[CollectionDB.kt:166](../core/src/main/java/com/client/xvideos/common/collectionDB/CollectionDB.kt:166),
[CollectionDB.kt:127](../core/src/main/java/com/client/xvideos/common/collectionDB/CollectionDB.kt:127)

Проход 7 завёл [CollectionName](../core/src/main/java/com/client/xvideos/common/collectionDB/CollectionName.kt)
ровно потому, что имя коллекции становится путём на диске. Имя элемента
становится путём точно так же — `File(dir, "$name.collection")`,
`File(dirPath, "$nameFile.$extension")` — и не проверяется ни в одном из двух
хранилищ. `insert`, `update`, `delete`, `read` берут строку как есть.

Источник строки сегодня — id и `username` из сетевых моделей R и L. Канал
HTTPS, `cleartextTrafficPermitted="false"`, так что практического пути атаки
отсюда нет — это защита в глубину и, главное, расхождение внутри одного
контракта: одна половина имени пути проверена, вторая нет. Ровно на такой
асимметрии и держалась дыра, которую закрыл `SafePath` (см. его же
преамбулу — «одну из копий забывают»).

Чего у находки нет: подтверждённого источника, который прислал бы `..` в id.
Это не воспроизводимый дефект, а разрыв контракта.

### S4 — `profileable` объявлен для всех сборок. Низкая.

[AndroidManifest.xml:98](../app/src/main/AndroidManifest.xml:98)

`<profileable android:shell="true" />` стоит в основном манифесте, значит
попадает и в release. Приложение с кодом доступа и зашифрованным хранилищем
паролей отдаёт себя профилировщику с shell-доступом. Место такому объявлению —
в `src/debug/AndroidManifest.xml`.

## Корректность

### C1 — Событие приёма может потеряться молча. Средняя.

[NearbyClientImpl.kt:40](../core/src/main/java/com/client/xvideos/common/p2p/nearby/NearbyClientImpl.kt:40),
[NearbyClientImpl.kt:45](../core/src/main/java/com/client/xvideos/common/p2p/nearby/NearbyClientImpl.kt:45)

`events` — `MutableSharedFlow(extraBufferCapacity = 64)` без `replay` и без
`onBufferOverflow`. Отправка идёт через `tryEmit`, результат отбрасывается:
`private fun emit(event: P2pEvent) { events.tryEmit(event) }`. При заполненном
буфере `tryEmit` возвращает `false`, и событие просто исчезает.

Буфер заполняет `TransferProgress`: он эмитится на каждый
`onPayloadTransferUpdate`, то есть десятки-сотни раз в секунду на большом
файле. Подписчик один, и он на главном потоке
([P2pReceiveManager.kt:34](../core/src/main/java/com/client/xvideos/common/p2p/P2pReceiveManager.kt:34)) —
то есть отстаёт ровно тогда, когда интерфейс занят. Потерянный
`TransferProgress` безвреден, потерянный `FilePayloadReceived` — нет:
`tryImport` ждёт «манифест + все файлы», условие не сойдётся никогда, экран
останется в `Receiving` без ошибки и без таймаута.

Лечится либо `onBufferOverflow = DROP_OLDEST` с выделением прогресса в
отдельный `StateFlow` (терять не жалко именно его), либо проверкой результата
`tryEmit` с логом. Сейчас различия между «событий не было» и «событие
выброшено» в коде нет.

Чего у находки нет: воспроизведения. Нужен большой файл и загруженный UI
одновременно; на устройстве не проверялось.

### C2 — Комментарий обещает перехват, которого не будет. Низкая.

[SecureCredentialStore.kt:89](../core/src/main/java/com/client/xvideos/common/settings/SecureCredentialStore.kt:89)

```kotlin
} catch (e: Exception) {
    // Всё остальное (IllegalStateException, NoClassDefFoundError в Preview,
    // отказ Keystore) означает «сейчас нельзя», а не «файл испорчен».
```

`NoClassDefFoundError` — наследник `Error`, а не `Exception`, и этим `catch` не
ловится. Сценарий Compose Preview, ради которого и написан весь `createOrNull`
с его контрактом «верни null», как раз и падает мимо перехвата. Либо
`catch (e: Throwable)`, либо убрать `NoClassDefFoundError` из комментария —
сейчас код и комментарий расходятся, и расхождение в пользу комментария.

### C3 — Атомарная запись атомарна не до конца. Низкая.

[AtomicWrite.kt:16](../core/src/main/java/com/client/xvideos/common/io/AtomicWrite.kt:16)

Три отдельные вещи в одной функции:

1. Имя временного файла детерминировано — `"$name.tmp"`. Внутри одного
   `FileDB`/`CollectionDB` записи сериализованы `lock`, но лок принадлежит
   *экземпляру*. Два экземпляра на один путь — и два `writeTextAtomically`
   пишут в один и тот же `.tmp`. Сегодня экземпляры создаются по одному на
   хранилище внутри `@Singleton`, так что гонки нет; она появится молча, если
   кто-нибудь заведёт второй экземпляр. `File.createTempFile` в том же каталоге
   снимает вопрос навсегда.
2. Фолбэк `delete()` + повторный `renameTo` — это ровно то окно, ради
   устранения которого функция и написана: между `delete` и `rename` читатель
   видит отсутствие файла. Оба хранилища закрывают это своим `lock`, но сама
   функция даёт слабую гарантию, а её KDoc — сильную.
3. Нет `fd.sync()` перед переименованием. При отключении питания
   переименование может дойти до диска раньше данных. Для этого приложения
   цена потери — один элемент списка, так что это скорее запись в известные
   ограничения, чем требование.

### C4 — Повторный импорт одного бандла. Низкая.

[P2pReceiveController.kt:107](../core/src/main/java/com/client/xvideos/common/p2p/P2pReceiveController.kt:107)

`tryImport` не помечает манифест обработанным. После успешного импорта
состояние `Done`, но `manifest` и `receivedFiles` живы до следующего `start()`.
Любое запоздавшее `FilePayloadReceived` (повтор от GMS) снова вызывает
`tryImport`, условие «все файлы на месте» снова выполняется, и бандл ставится
второй раз. Перезапись идемпотентна, так что видимого вреда нет — но `Done`
переустанавливается, а `pendingStopJob` заводится второй раз.

## Главный поток

### T1 — Приём P2P целиком выполняется на главном потоке. Высокая.

[P2pReceiveManager.kt:34](../core/src/main/java/com/client/xvideos/common/p2p/P2pReceiveManager.kt:34),
[P2pReceiveController.kt:112](../core/src/main/java/com/client/xvideos/common/p2p/P2pReceiveController.kt:112),
[NearbyClientImpl.kt:60](../core/src/main/java/com/client/xvideos/common/p2p/nearby/NearbyClientImpl.kt:60)

Цепочка: `scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)`
→ `eventsJob = scope.launch { nearby.events.collect { handle(it) } }` →
`handle` → `tryImport` → `importer.import(...)`. Ни один из трёх импортёров
не переключает диспетчер:

- `StoreBundleImporter` зовёт `P2pBundleInstaller.install` (копирование каждого
  файла) и `P2pInboxMerger.merge` (обход дерева, rename либо copy+delete);
- `LCollectionBundleImporter` и `RCollectionBundleImporter` зовут
  `ZipUtils.unzip` — распаковку архива коллекции целиком.

Всё это блокирующий ввод-вывод на UI-потоке. Отдельно: `receiveToCache`
копирует принятый payload из Nearby в свой кеш, и вызывается он из
`onPayloadTransferUpdate` — колбэка GMS, который без переданного `Executor`
приходит на главный поток. То есть большой файл копируется на главном потоке
дважды: сначала из Nearby в кеш, потом из кеша в store.

Показательно, что отправка сделана правильно: `ScreenP2pSend` оборачивает
каждую подготовку в `withContext(Dispatchers.IO)`
([ScreenP2pSend.kt:69](../core/src/main/java/com/client/xvideos/common/p2p/ui/ScreenP2pSend.kt:69)).
Приёмная половина этого просто не получила.

Чего у находки нет: замера ANR. Вывод сделан по коду; на устройстве 18.08
приём проверялся, но размер бандла в
[отчёте](DEVICE_CHECK_2026-08-18.md) не зафиксирован, так что «не подвисло» там
ничего не доказывает.

### T2 — R-хранилища ходят на диск синхронно, L-хранилища — нет. Средняя.

[SavedRed.kt:37](../feature-r/src/main/java/com/client/xvideos/r/common/saved/SavedRed.kt:37),
[R_Saved_Collection.kt:61](../feature-r/src/main/java/com/client/xvideos/r/common/saved/R_Saved_Collection.kt:61),
[R_Saved_Likes.kt:28](../feature-r/src/main/java/com/client/xvideos/r/common/saved/R_Saved_Likes.kt:28),
[R_Saved_Creator.kt:57](../feature-r/src/main/java/com/client/xvideos/r/common/saved/R_Saved_Creator.kt:57),
[LinkCollectionStore.kt:82](../core/src/main/java/com/client/xvideos/common/collectionDB/model/LinkCollectionStore.kt:82)

`SavedRed` — `@Singleton`, и в его `init` стоит
`collections.refreshCollectionList()`. Этот вызов синхронно обходит весь
каталог R-коллекций и разбирает Gson'ом каждый файл элемента. Инъекция
`@Inject lateinit var savedRed` в `MainActivity` означает, что конструктор
отрабатывает в `onCreate`, то есть на главном потоке, на пути запуска.

Тот же класс: `R_Saved_Likes.add/remove/refresh`, `R_Saved_Creator.refresh`,
`R_Saved_Collection.addCollection/deleteItemFromCollection/deleteCollection`,
`LinkCollectionStore.renameCollection` — все вызывают `FileDB`/`CollectionDB`
прямо из обработчика нажатия.

L-сторона и X-сторона эту работу уже проделали и подписали причину:
«Файловый I/O вынесен на `Dispatchers.IO`: раньше запись шла в главном потоке
(риск ANR)»
([SavedX_Favorites.kt:29](../feature-x/src/main/java/com/client/xvideos/x/feature/saved/SavedX_Favorites.kt:29)),
`SavedL_Likes.refresh` и `SavedL_Collection.refresh` читают на IO и публикуют
на Main. R-половина осталась как была.

## Архитектура

### A1 — Сторож глобального состояния не видит того, ради чего написан. Средняя.

[GlobalStateTest.kt:101](../app/src/test/java/com/client/xvideos/arch/GlobalStateTest.kt:101)

Детектор объявлений:

```kotlin
val declaration = line.removePrefix("@Volatile").trim()
val isDeclaration = declaration.startsWith("var ") ||
    declaration.startsWith("internal var ")
```

Под это не подходят ни `private var`, ни `lateinit var`. Следствия проверяемые:

- Удалённый `App.instance` был объявлен как `lateinit var instance: App`
  (`git show 6ef11da^:app/.../App.kt:211`). Сторож, написанный, чтобы такой
  глобал не вернулся незаметно, его возвращения не заметит.
- Существующие сегодня и невидимые для теста точки: `AppPath.root`
  (`private var`), `AppPath.r_nichesCache`, `AppPath.l_cacheDownload`,
  `AppPath.p2p_nearbyCache` (все `lateinit var` с `private set`),
  `AppContextHolder.context` (`@Volatile private var` на отдельной строке —
  `removePrefix` работает только когда аннотация в той же строке),
  `SecureCredentialStore.lastFailureLooksLikeBrokenKeyset`.
- Список `ALLOWED` при этом содержит `ApiClient.bearerToken` с пометкой
  «Запись закрыта (`private set`)» — то есть по замыслу авторов такие точки в
  списке быть должны. Значит, дело не в намеренном сужении, а в регулярке.
- KDoc метода говорит «**Непубличные** `var`», а условие ловит ровно публичные
  и `internal`. Документация описывает противоположное поведение.

Тест зелёный не потому, что множество глобалов совпало с ожидаемым, а потому
что часть множества он не собирает.

### A2 — Слой данных `:core` держит состояние Compose. Низкая.

[FileDB.kt:31](../core/src/main/java/com/client/xvideos/common/fileDB/FileDB.kt:31),
[LinkCollectionStore.kt:30](../core/src/main/java/com/client/xvideos/common/collectionDB/model/LinkCollectionStore.kt:30)

`FileDB.list` — `mutableStateListOf<T>()`, `LinkCollectionStore` держит
`collectionList`, `visibleDialog`, `visibleDialogCreateNew`,
`collectionItemGifInfo` на Compose-состоянии. Проход 6 убрал ровно это из слоя
данных L (коммит `3dd0c55`, «Compose в слое данных L и мёртвый `Saver`») — в
`:core` тот же приём остался, причём вместе с флагами видимости диалогов, то
есть с состоянием интерфейса в хранилище.

Цена переделки заметная: `replaceWith` и порядок публикации по номеру загрузки
завязаны на снапшот-лок, об этом прямо написано в KDoc обоих классов. Это не
дефект, это незакрытая половина уже принятого решения — и её стоит либо
закрыть, либо записать в «решения владельца», чтобы следующий проход не
открывал её заново.

### A3 — Мёртвые `@OptIn(DelicateCoroutinesApi::class)`. Низкая.

[R_Saved_Collection.kt:60](../feature-r/src/main/java/com/client/xvideos/r/common/saved/R_Saved_Collection.kt:60),
[SavedRed.kt:32](../feature-r/src/main/java/com/client/xvideos/r/common/saved/SavedRed.kt:32)

Аннотация стоит над функциями, в которых `GlobalScope` больше нет (убран, судя
по комментариям в `DropdownMenuItem_*`, при переходе на управляемый scope).
Осталась разметка, которая утверждает про код неправду и глушит будущее
предупреждение, если `GlobalScope` вернётся.

## Статус

| Находка | Класс | Статус | Коммит |
| --- | --- | --- | --- |
| S1 | безопасность | открыт | — |
| S2 | безопасность | открыт | — |
| S3 | безопасность | открыт | — |
| S4 | безопасность | открыт | — |
| C1 | корректность | открыт | — |
| C2 | корректность | открыт | — |
| C3 | корректность | открыт | — |
| C4 | корректность | открыт | — |
| T1 | главный поток | открыт | — |
| T2 | главный поток | открыт | — |
| A1 | архитектура | открыт | — |
| A2 | архитектура | открыт | — |
| A3 | архитектура | открыт | — |

## Проверка

```
./gradlew testDebugUnitTest --console=plain
BUILD SUCCESSFUL in 52s
```

`detekt` и `:app:lintRelease` в этом проходе не запускались: код не менялся,
находки получены чтением. Ручных сценариев на устройстве проход не выполнял.

Что нужно проверить на устройстве, когда до находок дойдут руки:

- приём бандла на 1–2 ГБ при одновременной прокрутке ленты — ожидается ANR
  (T1);
- приём с искусственной задержкой подписчика — ожидается зависание в
  `Receiving` без ошибки (C1);
- холодный старт с большой библиотекой R — замер времени `SavedRed.<init>` на
  главном потоке (T2);
- подключение сторонним клиентом Nearby с `serviceId`
  `com.client.xvideos.p2p` — ожидается приём без подтверждения (S1).

## Что проверено и находок не дало

Отрицательный результат — тоже результат; следующий проход может сюда не
ходить.

- **Код доступа.** PBKDF2-HMAC-SHA256, 120 000 итераций, 16-байтная соль из
  `SecureRandom`, сравнение через `MessageDigest.isEqual` (константное время),
  хеширование на `Dispatchers.Default`. Троттлинг держится на двух часах сразу,
  монотонный остаток отбрасывается после перезагрузки. Претензий нет.
- **Обход каталога.** `normalizeRelativePath` + `requireInside` применяются в
  `ZipUtils.unzip`, `P2pBundleInstaller.install` и в разборе манифеста —
  включая повторную проверку в установщике. Каноничные пути сравниваются с
  разделителем на конце префикса, символическая ссылка наружу не уводит.
- **Разбор манифеста.** Переход на kotlinx (`31d8122`) закрывает класс «Gson
  кладёт null в non-null». `ignoreUnknownKeys = true` при закрытом enum `P2pType`
  — верный баланс: лишнее поле не ломает приём, неизвестный тип отвергается.
  Значение по умолчанию у `metadataFileName` объяснено в коде и подтверждено
  тестом.
- **Манифест приложения.** `allowBackup="false"`, `dataExtractionRules` и
  `backup_rules` заданы, `networkSecurityConfig` запрещает cleartext, наружу
  открыта одна `SplashActivity`, `FileProvider` не экспортирован. Кроме
  `profileable` (S4) — чисто.
- **Порядок публикации в хранилищах.** Схема «номер до чтения диска, публикация
  под отдельным локом, устаревший результат не ложится поверх свежего» есть и в
  `FileDB`, и в `LinkCollectionStore`, и причина, почему публикация стоит вне
  основного лока, в обоих местах записана.
- **`StorageCleanupGate`.** Повторный `start` игнорируется под `@Synchronized`,
  падение уборки не роняет ожидающего, `await()` без запуска возвращается сразу.
  Контракт и код совпадают. Остаётся известная T1 прошлого прохода — обязанность
  дождаться по-прежнему на вызывающем.

## Что осталось открытым

Все тринадцать находок. Планов под них нет: проход только что закончен.

Порядок, в котором их разумно закрывать, если брать по цене и весу:

1. **T1** — правка на одну строку `withContext(Dispatchers.IO)` в трёх
   импортёрах плюс `Executor` для колбэков Nearby; эффект наибольший.
2. **S1** — требует UI (экран подтверждения с четырьмя цифрами) и продуктового
   решения: подтверждение на каждый приём или «доверенные устройства».
3. **A1** — правка регулярки в стороже и честное пополнение `ALLOWED`; после
   неё станет видно, сколько глобалов на самом деле.
4. **C1**, **T2** — механические.
5. **S2** — сначала решение владельца о пороге.
6. **S3**, **S4**, **C2**, **C3**, **C4**, **A2**, **A3** — по случаю, вместе с
   работой в тех же файлах.
