# Поделиться коллекцией L по P2P — дизайн

Дата: 2026-06-13
Статус: одобрен

## Проблема

Из списка коллекций L нельзя передать коллекцию на другой телефон. Коллекция —
это папка `/xvideos/L/Collection/<имя>/<папка-item>/{media, preview,
metadata.json}` + опциональный `collection.json` (обложка). Это реальные
скачанные файлы, не только метаданные — поэтому передавать нужно содержимое.

## Решение

Третий пункт «Поделиться (P2P)» в уже существующем диалоге действий с
коллекцией (рядом с «Переименовать» / «Удалить коллекцию»). Коллекция
архивируется в один `.zip`, кладётся в outbox, передаётся как одна полезная
нагрузка P2P, на приёме распаковывается в зеркало inbox и существующим
`P2pInboxMerger.merge` переносится в `/xvideos/L/Collection/` с пофайловой
перезаписью (= merge при совпадении имени).

Zip вместо передачи файлов поштучно: коллекция = много папок-item с реальными
медиа; один архив — это одна атомарная передача вместо сотен payload'ов.
Архивация переиспользует фазу `Preparing` контроллера; распаковка —
единственный новый кусок конвейера.

## 1. ZipUtils (новый, `common/zip/ZipUtils.kt`)

Маленькая утилита (паттерн zip-slip guard взят из `XlrBackupManager`):

- `zipDirectory(sourceDir: File, zipFile: File)` — пишет содержимое
  `sourceDir` в `zipFile`; имена записей с префиксом `sourceDir.name`
  (`<имя>/item/media.jpg`), поэтому имя коллекции едет внутри архива.
  DEFLATED; директории как entry с `/`.
- `unzip(zipFile: File, destDir: File)` — распаковка в `destDir` с защитой
  от zip-slip: имя записи нормализуется (`\`→`/`, trim, запрет `..`, `:`,
  ведущего `/`), target проверяется `ensureInside(destDir, target)`.

Чистые функции на `File`, без Context/URI — тестируются на JVM.

## 2. Протокол

- `P2pType` += `L_COLLECTION`.
- Манифест/codec не меняются — только новое значение enum.
- Старый APK на L_COLLECTION-бандл: «Битый манифест» → Error → авторестарт
  рекламы. Не крэш.

## 3. Отправка

- `LCollectionExporter.export(collectionName: String, collectionRoot: File,
  outboxDir: File): P2pExportBundle?`:
  - `source = File(collectionRoot, collectionName)`; не существует, не папка
    или пуста → null;
  - `zipFile = File(outboxDir, "$collectionName.zip")`;
  - `ZipUtils.zipDirectory(source, zipFile)`;
  - бандл `P2pExportBundle(L_COLLECTION, storeRoot = outboxDir,
    files = [zipFile], metadataFile = null)`; relativePath = `<имя>.zip`.
  - ошибка зипования (runCatching) → null.
- Zip не зеркалит store: на приёме он не мёржится напрямую, а
  распаковывается. `outboxDir = File(AppPath.p2p_outbox)` (корень outbox).
- `P2pSendSource.ShareCollection(collectionName: String)` (Serializable —
  обычная строка).
- `ScreenP2pSend.prepareBundle` ветка `ShareCollection`: на `Dispatchers.IO`
  зовёт `LCollectionExporter.export(name, File(AppPath.l_collection),
  File(AppPath.p2p_outbox))`; null → `error("Не удалось подготовить
  коллекцию")` → `ShareState.Error`. Архивация идёт под фазой `Preparing`
  (спиннер «Подготовка файлов…»); уход с экрана отменяет `prepareJob`.

## 4. Приём

- `LCollectionBundleImporter(inboxRoot, mainRoot, collectionMirrorRoot,
  refresh)` : `BundleImporter`:
  - принятый zip — единственный файл в `receivedFiles.values`;
  - `ZipUtils.unzip(zip, collectionMirrorRoot)` где
    `collectionMirrorRoot = mirrorRoot(inboxRoot, mainRoot,
    File(AppPath.l_collection))` (= `inbox/L/Collection`); записи
    `<имя>/...` воссоздают `inbox/L/Collection/<имя>/...`;
  - `P2pInboxMerger.merge(inboxRoot, mainRoot)` → перенос в
    `/xvideos/L/Collection/<имя>/...` с пофайловой перезаписью;
  - `refresh()` → `savedL.collection.refreshCollectionList()`.
- Роутинг в `P2pReceiveManager` по типу:
  `R -> rLikesImporter`, `L_COLLECTION -> lCollectionImporter`,
  `else -> storeImporter`. `savedL()` уже в `P2pRefreshEntryPoint`,
  `.collection.refreshCollectionList()` доступен.
- Коллизия имени = merge с перезаписью (новые item добавляются, совпадающие
  по hash-folder перезаписываются). Совпадает с выбранным поведением.

## 5. UI

- В диалоге `itemPendingAction` ([L_Screen_CollectionTab.kt]) третий
  `DropdownMenuItem` «Поделиться (P2P)»: закрывает диалог и пушит
  `navigator.push(ScreenP2pSend(P2pSendSource.ShareCollection(pending)))`.
- Долгое нажатие на коллекции уже вызывает `onCollectionLongClick` →
  `itemPendingAction`; новый код только добавляет пункт.

## 6. Жизненный цикл / ошибки

- Outbox чистится как раньше: Done → `clearP2pOutbox()` + при старте
  приложения. Zip-артефакт уходит туда же.
- Inbox: после merge пуст (merger пересоздаёт). Битый/неполный zip до merge
  лежит в inbox, в боевой store не попадает.
- Zip-slip в принятом архиве → `unzip` бросает → `ReceiveState.Error`,
  боевой store не тронут.

## 7. Тесты

- `ZipUtilsTest`: zip папки → unzip → дерево идентично (имена+содержимое);
  запись с `../` отклонена (ensureInside бросает).
- `ExportersTest`: `LCollectionExporter` — zip создан в outbox, верхняя
  запись = имя коллекции, бандл type/relativePath; коллекция отсутствует/пуста
  → null.
- `LCollectionBundleImporterTest`: на вход zip (через ZipUtils.zipDirectory
  тестовой папки) → unzip+merge кладёт коллекцию в l_collection-корень,
  `refresh` дёрнут, inbox пуст.
- `P2pManifestCodecTest`: round-trip манифеста с типом L_COLLECTION.

## Вне объёма

- Пофайловый прогресс архивации (только спиннер «Подготовка»).
- Выбор отдельных item коллекции (передаётся вся).
- Передача без zip (поштучно).
