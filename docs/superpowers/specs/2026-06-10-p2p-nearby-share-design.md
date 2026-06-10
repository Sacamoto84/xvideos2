# P2P-передача элементов через Nearby Connections — дизайн

Дата: 2026-06-10
Статус: утверждён (брейншторм)

## Цель

Дать возможность передать выбранный элемент (item) из любого источника **X / R / L** на другой
телефон с этим же приложением напрямую (peer-to-peer), без интернета у получателя, через
Google Nearby Connections API. Элемент передаётся целиком: медиафайл(ы) + метаданные, и на
приёмной стороне раскладывается в локальное «сохранённое» нужного типа так, будто пользователь
сохранил его сам.

## Ключевые решения (из брейншторма)

1. **Что передаём:** реальные байты медиафайла (а не ссылку). Работает офлайн у получателя.
2. **Маршрутизация:** отправитель указывает тип источника `X` / `R` / `L`; item — это не только
   картинка, но и файл с метаданными. Получатель раскладывает в store соответствующего типа.
3. **Получатель видим:** явная кнопка/экран «Приём P2P» (advertising только когда экран открыт).
   Точка входа — IconButton в `topBar` экрана `MenuScreen`.
4. **Масштаб v1:** один элемент за раз (мультивыбор — позже).
5. **Импорт у получателя:** авто-импорт в store + уведомление (соединение уже подтверждено кодом).
6. **Формат провода — Подход A:** переиспользуем формат бандла store. Передаём ровно те же файлы,
   что пишет обычное сохранение, плюс маленький control-манифест. Идентичные форматы на обоих
   телефонах → совместимость без отдельной транспортной схемы, получателю сеть не нужна.
7. **Точка отправки:** существующий пункт меню «Поделиться» открывает мини-выбор
   «Система (системный chooser) / P2P рядом».

## Контекст кодовой базы

Три источника, у каждого свой формат «сохранённого» бандла на диске:

- **L** (luscious, модель `PicsDetails`): папка с `media.<ext>` + `preview.*` + `metadata.json`
  (`LSavedLikeMetadata`, внутри весь `PicsDetails` + данные альбома).
  Сборка: `lPersistPicsDetailsToFolder(...)`; чтение: `readLSavedLikeMetadata(...)` /
  `LSavedLikeMetadata.toPicsDetails(...)`.
  Файлы: `app/src/main/java/com/client/xvideos/l/featured/saved/LMediaPersist.kt`,
  `.../LSavedLikeMetadata.kt`.
- **X** (xvideos, модель `ItemsX`): `<id>.mp4` + `<id>.jpg` + `<id>.info` (JSON `ItemsX`) в
  `AppPath.x_cache_download`. Сборка/чтение/дедуп: `SavedX_Downloads`
  (`download`, `contains(id)`, `refresh`).
  Файл: `app/src/main/java/com/client/xvideos/x/feature/saved/SavedX_Downloads.kt`.
- **R** (redgifs, модель `GifsInfo`): аналогичный store в `r/common/saved/`.

Точка входа отправки уже есть: `AlbumItemExpandMenu` / R / X expand-меню вызывают
`onShare(item)` (см. `app/src/main/java/com/client/xvideos/l/ui/element/expandMenu/AlbumItemExpandMenu.kt`).
Текущий `useCaseShareFile` делает системный `ACTION_SEND`.

Точка входа приёма: `MenuScreen` в `app/src/main/java/com/client/xvideos/MainActivity.kt:205`
(в `topBar` уже есть IconButton'ы настроек и haptic-demo).

Платформа: minSdk 26, targetSdk 37. Зависимости Nearby в проекте пока нет.

## Архитектура

Новый общий пакет `common/p2p` + адаптеры на каждый источник.

### Транспортный слой `common/p2p/nearby`

- **`NearbyClient`** (интерфейс) — тонкая обёртка над `Nearby.getConnectionsClient`.
  Методы: `startAdvertising(name)`, `startDiscovery()`, `requestConnection(endpointId, name)`,
  `acceptConnection(endpointId)`, `rejectConnection(endpointId)`,
  `sendFile(endpointId, File): Long /*payloadId*/`, `sendBytes(endpointId, ByteArray)`,
  `stopAll()`. Стратегия **`P2P_POINT_TO_POINT`** (1:1, макс. пропускная способность,
  BT + Wi-Fi). `serviceId = packageName + ".p2p"`.
  Реализация `NearbyClientImpl` оборачивает GMS-колбэки в поток `Flow<P2pEvent>`.
  Интерфейс нужен, чтобы контроллеры тестировались in-memory фейком без GMS.
- **`P2pEvent`** (sealed): `EndpointFound(id,name)`, `EndpointLost(id)`,
  `ConnectionInitiated(id, authCode)`, `Connected(id)`, `Rejected(id)`, `Disconnected(id)`,
  `PayloadProgress(payloadId, transferred, total)`,
  `PayloadReceived(payloadId, payload)`, `Error(throwable)`.

### Доменный слой `common/p2p`

- **`P2pType { X, R, L }`** — тег источника.
- **`P2pManifest`** — control-сообщение (передаётся как BYTES, JSON):
  `{ type: P2pType, metadataFileName: String?, files: [ { name, payloadId, size } ] }`.
  `metadataFileName` указывает, какой из файлов — JSON метаданных (`metadata.json` / `<id>.info`).
- **`P2pExportBundle`** — `(manifest, files: List<File>, tempDir: File?)`.
- **`P2pExporter`** (интерфейс) `suspend fun export(): P2pExportBundle` + per-type реализации
  `LExporter` / `RExporter` / `XExporter`:
  - если item уже сохранён в store → находим папку/файлы и переиспользуем их (без копии, читаем напрямую);
  - иначе → собираем бандл во временную папку (`L`: `lPersistPicsDetailsToFolder` в temp-root;
    `X`: качаем mp4+jpg+info как `SavedX_Downloads.download`, но в temp; `R`: аналогично);
  - возвращаем список файлов + манифест.
- **`P2pImporter`** (интерфейс) `suspend fun import(type, manifest, files)` + per-type
  `LImporter` / `RImporter` / `XImporter`:
  - кладут принятые файлы в корень store нужного типа с именами из манифеста;
  - L: создают папку (имя из `metadata.json` `folderName`), пишут файлы, существующий
    `readLSavedLikeMetadata` подхватит; X: пишут `<id>.mp4/.jpg/.info`; R: аналогично;
  - вызывают `refresh()` соответствующего store;
  - дедуп: L — детерминированное имя папки (album+sha) → перезапись/пропуск; X/R — `contains(id)`.
- **`P2pShareController`** (sender) и **`P2pReceiveController`** (receiver) — оркестрация поверх
  `NearbyClient` + exporter/importer. Выдают наружу UI-state (`Flow`/`StateFlow`).

### UI

- Sender: **`P2pDeviceSearchSheet`** — bottom sheet «Поиск телефонов рядом»: спиннер,
  список найденных эндпоинтов, статусы «Соединение… / Код XXXX / Отправка NN% / Готово / Ошибка».
  Открывается из выбора «P2P рядом» в меню «Поделиться».
- Sender: мини-выбор **«Система / P2P рядом»** — вставка в обработчик `onShare(item)`.
- Receiver: **`ScreenP2pReceive`** — «Приём P2P»: «Ожидание отправителя…», подтверждение кода,
  прогресс приёма, тост «Принято». Пушится из IconButton в `topBar` `MenuScreen`.

### Платформа

- `AndroidManifest.xml` — разрешения:
  `BLUETOOTH_ADVERTISE`, `BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN` (API 31+),
  `ACCESS_FINE_LOCATION` (скан до API 31), `NEARBY_WIFI_DEVICES` (API 33+),
  legacy `BLUETOOTH`, `BLUETOOTH_ADMIN`, `ACCESS_WIFI_STATE`, `CHANGE_WIFI_STATE`,
  `ACCESS_COARSE_LOCATION`. Runtime-запрос нужных перед advertise/discovery.
- `gradle/libs.versions.toml` + `app/build.gradle` — зависимость
  `com.google.android.gms:play-services-nearby`.

## Поток данных и протокол

### Отправитель (меню item → «P2P рядом»)
1. `Exporter.export()` готовит файлы бандла (сохранённый item — переиспользуем; иначе — temp).
   Падение скачивания → стоп до поиска, сообщение об ошибке.
2. Открывается `P2pDeviceSearchSheet` → `startDiscovery()`. Список найденных (имя = модель/ник).
3. Тап по телефону → `requestConnection`. Оба видят 4-значный код. Получатель подтверждает,
   отправитель авто-принимает (инициатор), код показывает для сверки.
4. `Connected` → для каждого файла `sendFile` (копим `name→payloadId`), затем `sendBytes`
   с `P2pManifest`.
5. `PayloadProgress` → «Отправка NN%». Done → «Готово», чистим temp-папку.

### Получатель (`ScreenP2pReceive`, advertising)
1. Входящее соединение → показать код → подтвердить (`acceptConnection`).
2. Буферизуем входящие FILE-payload'ы по `payloadId`; ждём BYTES-манифест и завершение всех
   перечисленных в нём файлов (manifest может прийти раньше, чем файлы дозагрузятся).
3. `Importer.import(type, manifest, files)` раскладывает файлы в store нужного типа, пишет
   metadata, `refresh()`. Тост «Принято».
4. Дедуп при совпадении: перезапись либо «Уже есть».

### Расположение принятых файлов
Nearby отдаёт FILE-payload как `Payload.Type.FILE` (на API 29+ — content `Uri` в Downloads/Nearby).
`Importer` копирует из `Uri`/файла в корень store потоково. Временные/исходные payload-файлы
после импорта удаляются.

## Ошибки и краевые случаи
- Нет прав (BT/scan/wifi/location) → показать rationale, мягкий выход.
- BT/Wi-Fi выключены → подсказать включить (Nearby требует их для канала).
- Никого не нашли за таймаут → «Телефоны не найдены» + повтор.
- Reject / обрыв в процессе → чистим частичные файлы (получатель удаляет недособранную папку),
  сообщение об ошибке у обеих сторон.
- Большое видео → прогресс, keep-screen-on, отмена через `stopAllEndpoints`.
- Дубликат у получателя → детерминированное имя/`contains()` → перезапись или «Уже есть».
- Фон во время передачи → v1: держим экран включённым, без foreground-service (ограничение).
- Temp export-папка чистится после отправки в любом исходе (успех/ошибка/отмена).

## Тестирование
- **Unit:** round-trip сериализации `P2pManifest`; каждый `Exporter` собирает верный набор файлов
  (mock store-каталоги); каждый `Importer` раскладывает файлы и вызывает `refresh()`;
  маршрутизация по `P2pType`; дедуп/перезапись.
- **Контроллеры:** фейковый `NearbyClient`, соединяющий sender↔receiver в памяти — полный
  happy-path и сценарии обрыва, без реальных устройств/GMS.
- **Ручное (2 телефона):** передача L / R / X; поток выдачи разрешений; BT выключен; отмена в
  процессе; большое видео.

## Вне scope v1
- Мультивыбор и пакетная отправка.
- Передача альбома целиком (только одиночный item).
- Foreground-service для передачи в фоне.
- Кросс-тип конвертация (тип сохраняется как был: X→X, R→R, L→L).
