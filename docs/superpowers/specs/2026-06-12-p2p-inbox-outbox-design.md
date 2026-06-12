# P2P inbox/outbox — дизайн

Дата: 2026-06-12
Статус: одобрен

## Проблема

1. **Отправка L**: item не из Likes нельзя передать по P2P — `startP2p`
   (`ExpandMenuVM`) показывает «Сначала сохрани (Like)». Пользователь не хочет
   сохранять item в Likes ради передачи.
2. **Приём**: `StoreBundleImporter` кладёт принятые файлы напрямую в боевой
   store (`l_likes`, `x_cache_download`). Промежуточного staging нет.

## Решение (подход «Зеркальные корни»)

Две временные папки в корне `/xvideos`:

- `/xvideos/outbox` — staging отправителя: сюда скачиваются файлы item'ов,
  которых нет в локальном store.
- `/xvideos/inbox` — staging получателя: сюда устанавливаются принятые файлы,
  после успешного приёма содержимое переносится в `/xvideos`.

Обе зеркалируют структуру `/xvideos` от корня: `outbox/L/Likes/<folder>/…`,
`inbox/X/Download/…`. Благодаря зеркалу `relativePath` в манифесте, посчитанный
от outbox-корня, идентичен посчитанному от боевого store — **протокол P2P не
меняется, совместимость со старыми APK полная**. Перенос inbox → `/xvideos` —
это перенос содержимого в корень: папки сами «встают» на свои места.

Объём v1: outbox только для L (X шарится с экрана Saved — уже скачан;
R передаёт только метаданные). Inbox — для X и L. Групповая передача и
передача списков групп/альбомов — следующий этап, дизайн им не мешает:
generic merge переносит любое содержимое inbox.

## 1. Папки и AppPath

- `AppPath.p2p_outbox = "$main/outbox"`, `AppPath.p2p_inbox = "$main/inbox"`.
- Helper зеркала: `mirrorRoot(base: File, storeRoot: File): File` —
  `base/<storeRoot относительно AppPath.main>`.
- Очистка обеих папок (deleteRecursively + mkdirs) при старте приложения в
  `AppPath.initInternalStorage()` (рядом с `clearLShareCache()`).
- `.nomedia` уже лежит в корне `/xvideos` и закрывает вложенные папки от
  индексации галереей.

## 2. Отправка (outbox, L)

- `startP2p` (`ExpandMenuVM`): сначала обычный экспорт из Likes. **Любой** фейл
  локации (нет папки, битый/неполный бандл) → не ошибка, а источник
  «надо скачать».
- `ScreenP2pSend` принимает `P2pSendSource` вместо готового бандла:
  - `Ready(bundle: P2pExportBundle)` — item уже в store;
  - `DownloadL(item: PicsDetails)` — качаем в outbox.
- Новая фаза `P2pShareController` — `Preparing(progress)` перед поиском
  устройств. Для `DownloadL`:
  1. `lPersistPicsDetailsToFolder(item, root = outbox/L/Likes, …)` —
     существующая функция, уже принимает root; пишет media + preview +
     `metadata.json`, прогресс через `LDownloadProgress`;
  2. `LExporter.export(<папка в outbox>)` → обычный flow отправки.
- Скачивание в контроллер передаётся как suspend-lambda (тестируемость).
- Уход с экрана в фазе Preparing → отмена корутины скачивания.
- Ошибка скачивания → `ShareState.Error("Не удалось скачать файлы")`.
- `Done` (семантика «все payload'ы переданы» уже реализована) → очистка outbox.

## 3. Приём (inbox)

- `StoreBundleImporter`: install файлов манифеста в `mirrorRoot(inbox,
  storeRoot)` вместо боевого store (`P2pBundleInstaller` не меняется —
  меняется передаваемый root).
- После install — `P2pInboxMerger.merge(inboxRoot, mainRoot)`:
  - walk всех файлов inbox, для каждого: `mkdirs` родителя в `/xvideos`,
    target существует → `delete()`, затем `renameTo` (та же ФС — мгновенно);
  - fallback на copy+delete, если rename не сработал;
  - после переноса — снос остатков inbox (пустые папки) + mkdirs.
- Merge generic: переносит всё содержимое inbox, не только файлы текущего
  манифеста — готов под будущие групповые передачи.
- `refreshFor(type)` после merge (как сейчас: L → `savedL.likes.refresh()`,
  X — экран Saved перечитывает при открытии).
- R (`RLikesBundleImporter`) — без изменений, файлы не пишет, inbox не трогает.
- Гарантия: полуполученный бандл никогда не попадает в боевой store —
  import зовётся только при «манифест + все файлы», до merge всё в inbox.

## 4. Жизненный цикл и ошибки

| Событие | inbox | outbox |
|---|---|---|
| Старт приложения | снос + mkdirs | снос + mkdirs |
| Успешная отправка (Done) | — | очистка |
| Успешный приём | merge опустошает | — |
| Обрыв/крэш посередине | мусор до следующего старта | мусор до следующего старта |

- Боевой store не загрязняется ни при каком сценарии обрыва.
- Merge упал посередине: перенесённые файлы уже на месте (валидные бандлы),
  остаток в inbox до рестарта; повторная передача перезапишет.
- Параллельные приём и отправка не конфликтуют: разные папки.

## 5. Тесты

- `P2pInboxMergerTest` (новый): структура переносится в корень, перезапись
  существующих файлов, остатки inbox снесены, fallback copy+delete.
- `StoreBundleImporterTest`: порядок install → merge → refresh; файлы в итоге
  в боевом store; inbox пуст.
- `ExportersTest`: `LExporter` от outbox-корня даёт те же `relativePath`,
  что от боевого.
- `P2pShareControllerTest`: фаза Preparing (фейковая suspend-lambda
  скачивания), отмена при уходе, ошибка скачивания → Error.

## Вне объёма v1

- Групповая передача (несколько item'ов за сессию).
- Передача списков групп и альбомов.
- Outbox для X/R.
