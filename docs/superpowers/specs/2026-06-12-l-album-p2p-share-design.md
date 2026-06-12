# Поделиться альбомом L по P2P — дизайн

Дата: 2026-06-12
Статус: одобрен

## Проблема

Из `ScreenLAlbum` нельзя передать альбом на другой телефон. Альбом в L — это
метаданные (`AlbumDetails` JSON в FileDB: `/xvideos/L/Album/<id>.album`),
контент получатель качает с сервера сам при открытии.

## Решение

Кнопка «Поделиться» в шапке `ScreenLAlbum`: передаём по P2P **только файл
метаданных** `<id>.album` (пару КБ, кеш списка картинок не передаём —
обновится сам у получателя). У получателя альбом появляется в Saved Albums.

Переиспользуем файловый конвейер P2P целиком (манифест → inbox-зеркало →
merge → refresh): новый тип бандла, нулевой новый код приёма.

## 1. Протокол

- В `P2pType` добавляется `L_ALBUM`.
- Манифест/codec не меняются — только новое значение enum.
- Старый APK при приёме L_ALBUM-бандла: Gson не распарсит тип →
  «Битый манифест» → Error → авторестарт рекламы через 5 сек. Не крэш.
  Передача X/L/R между старым и новым APK не затронута.

## 2. Отправка

- Новый `LAlbumExporter` (в `common/p2p/export/Exporters.kt` рядом с
  остальными): `export(album: AlbumDetails, savedRoot: File, outboxAlbumRoot: File): P2pExportBundle?`
  - файл `<album.id>.album` есть в `savedRoot` (`AppPath.l_albums`) →
    бандл от savedRoot;
  - нет → сериализуем `album` Gson'ом в `outboxAlbumRoot/<id>.album`
    (байт-в-байт формат FileDB) и бандл от outbox-корня. Сети нет,
    мгновенно — фаза Preparing не используется, сразу `P2pSendSource.Ready`;
  - `album.id` не парсится в Long → null (кнопка покажет SnackBar-ошибку).
- relativePath в обоих случаях — `<id>.album` (outbox зеркалирует
  `/xvideos`, `outboxAlbumRoot = mirrorRoot(outbox, main, l_albums)`).
- Кнопка `AlbumInfoButtonShareAlbum` (atom рядом с
  `AlbumInfoButtonSaveAlbum`) в шапке `ScreenLAlbum`, видна при
  `parsed != null`. Клик → SM строит бандл (IO) →
  `navigator.push(ScreenP2pSend(P2pSendSource.Ready(bundle)))`.
- Outbox чистится как раньше: Done → `clearP2pOutbox()`, плюс при старте.

## 3. Приём

- `P2pReceiveManager.storeRootFor`: ветка `P2pType.L_ALBUM -> File(AppPath.l_albums)`.
- `refreshFor`: `L_ALBUM -> entryPoint.savedL().albums.refresh()`.
- Всё остальное — существующий путь: install в `inbox/L/Album`, merge в
  `/xvideos`, FileDB.refresh подхватывает новый `.album`.
- Альбом уже есть у получателя → merge перезапишет файл, refresh обновит
  запись. Безвредно.

## 4. Тесты

- `ExportersTest`: LAlbumExporter — saved-файл найден (бандл от savedRoot);
  не найден (файл создан в outbox, содержимое = Gson(album), бандл от
  outbox-корня); relativePath = `<id>.album` в обоих случаях; невалидный id → null.
- `StoreBundleImporterTest`: L_ALBUM-манифест → файл в `l_albums`-корне,
  refresh дёрнут с L_ALBUM.
- `P2pManifestCodecTest`: round-trip манифеста с типом L_ALBUM.

## Вне объёма

- Передача контента альбома файлами (групповая передача) — отдельный этап.
- Системный share ссылки на альбом.
- Кнопка share в списках альбомов (только ScreenLAlbum).
