# P2P-передача R-коллекций (R_COLLECTION) — зеркало L_COLLECTION

Дата: 2026-06-14
Статус: дизайн утверждён, готов к плану реализации

## Цель

Включить передачу R-коллекций по P2P (Nearby): экспорт коллекции отправителем и приём
с раскладкой в `r_collection` у получателя. Зеркалит обкатанный конвейер `L_COLLECTION`,
но R-специфичными классами (L-код не трогаем). Завершает фичу, где пункт «Поделиться (P2P)»
в R-меню коллекций пока стоит disabled «скоро».

## Решения (через brainstorming)

- **R-специфичные классы** (отдельные `RCollectionExporter` / `RCollectionBundleImporter` +
  `P2pType.R_COLLECTION`), зеркалящие L_COLLECTION. L-конвейер не трогаем — P2P хрупкий
  (см. [[p2p-architecture-decisions]]), регрессия L недопустима.
- **Формат — zip папки коллекции** (как L_COLLECTION), не manifest отдельных `.collection`
  (как R-likes). R-папка = мелкие JSON-ссылки, зип тривиален и идентичен L-пайплайну → минимум дивергенции.

## Не-цели (YAGNI)

- Не обобщаем L+R в общий exporter/importer (риск регресса обкатанного L).
- Не трогаем семантику Done / фильтрацию событий GMS / стратегию CLUSTER (см. [[p2p-architecture-decisions]]).
- Не передаём медиа (R = ссылки `GifsInfo`, контент по URL у получателя).

## Контекст (как есть)

- `P2pType = { X, R, L, L_ALBUM, L_COLLECTION }`. R = `.info`-метаданные → R Likes.
- L_COLLECTION: `LCollectionExporter.export(collectionName, collectionRoot, outboxDir)` зипует
  `l_collection/<name>` → `<name>.zip` в outbox → `P2pExportBundle(L_COLLECTION, outboxDir, [zip], null)`.
- Приём: `LCollectionBundleImporter(inbox, main, l_collection, refresh)` → unzip в
  `mirrorRoot(inbox, main, l_collection)` → `P2pInboxMerger.merge` → `refresh`.
- `P2pReceiveManager.start()`: `StoreBundleImporter.storeRootFor` (исчерпывающий when по типам),
  отдельные `rLikesImporter`/`lCollectionImporter`, итоговый `importer` диспетчеризует по `manifest.type`.
- Отправка: `ScreenP2pSend` bundleProvider `when(source)`; `P2pSendSource.ShareCollection(name)` → L.
- R-store: `AppPath.r_collection`, `savedRed.collections.refreshCollectionList()`.

## Раздел 1 — Протокол/типы

- `P2pType`: добавить `R_COLLECTION`. (Старый APK → «Битый манифест», graceful — как для L_COLLECTION.)
- `P2pSendSource`: добавить `data class ShareCollectionR(val collectionName: String) : P2pSendSource`.
  L-`ShareCollection` оставить без изменений.

## Раздел 2 — Экспорт

- Добавить `RCollectionExporter` (рядом с `LCollectionExporter` в `common/p2p/export/Exporters.kt`):
  `fun export(collectionName, collectionRoot, outboxDir): P2pExportBundle?` — зип `collectionRoot/<name>`
  в `<name>.zip` (outbox) → `P2pExportBundle(P2pType.R_COLLECTION, outboxDir, listOf(zip), null)`.
  Тело идентично `LCollectionExporter`, отличается только `P2pType`.
- Сеть не нужна (фаза `Preparing` не требуется — файлы уже локальны).

## Раздел 3 — Приём + роутинг

- Добавить `RCollectionBundleImporter(inboxRoot, mainRoot, collectionStoreRoot, refresh)` (зеркало
  `LCollectionBundleImporter`): unzip в `mirrorRoot(inbox, main, collectionStoreRoot)` →
  `P2pInboxMerger.merge(inbox, main)` → `refresh()`.
- `P2pReceiveManager.start()`:
  - `StoreBundleImporter.storeRootFor` when → добавить `P2pType.R_COLLECTION -> File(AppPath.r_collection)`
    (when исчерпывающий — компилятор обяжет добавить ветку).
  - Завести `rCollectionImporter = RCollectionBundleImporter(inbox, main, File(AppPath.r_collection),
    refresh = { entryPoint.savedRed().collections.refreshCollectionList() })`.
  - В итоговом `importer` when по `manifest.type` → `P2pType.R_COLLECTION -> rCollectionImporter.import(...)`.
- Семантика слияния — как у L: перезапись при совпадении имени коллекции.

## Раздел 4 — UI (R-меню действий)

- `R_Screen_CollectionTab`: пункт «Поделиться (P2P)» —
  - снять `enabled = false`, текст вернуть «Поделиться (P2P)» (без «скоро»);
  - `onClick = { itemPendingAction = null; navigator.push(ScreenP2pSend(P2pSendSource.ShareCollectionR(pending))) }`.
- Добавить `val navigator = LocalNavigator.currentOrThrow` в `Content()` (Voyager).

## Раздел 5 — Совместимость / верификация

- `P2pManifestFactory` строит манифест из `bundle.type`/файлов обобщённо — правок, вероятно, не
  требует; проверить на этапе плана.
- Гейт: `gradlew :app:compileDebugKotlin` → `BUILD SUCCESSFUL`.
- Ручная проверка (2 устройства): отправка R-коллекции из меню → приём → коллекция появилась в R
  с тем же содержимым; коллизия имени → перезапись; пустая коллекция → не падает.
- P2P юнит-тестами не покрыт; не нарушать инварианты Done/state из [[p2p-architecture-decisions]]
  (в этой фиче они не затрагиваются — используем существующий контроллер передачи как есть).

## Критерии готовности

- `P2pType.R_COLLECTION` + `ShareCollectionR` добавлены; L-типы/источник не изменены.
- `RCollectionExporter`/`RCollectionBundleImporter` зеркалят L; роутинг в `P2pReceiveManager` заведён.
- R-меню «Поделиться (P2P)» активно и открывает `ScreenP2pSend` с R-источником.
- Проект компилируется; L-конвейер не тронут.
