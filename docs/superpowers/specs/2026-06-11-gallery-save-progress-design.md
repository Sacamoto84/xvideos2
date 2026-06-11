# Прогресс скачивания при «Сохранить в галерею» (R и X)

Дата: 2026-06-11
Статус: одобрено пользователем

## Контекст

При сохранении в галерею из сети (видео не в кеше) нет индикации — непонятно,
качает или нет. У R и X уже есть зелёный прогресс-бар внизу с общей семантикой
`MutableStateFlow<Float>`: `0..1` — прогресс, `-2` — покой, `-3` — ошибка.
R: `Downloader.percent` (слушает `R_Screen_Root`), X: `SavedX_Downloads.percent`
(слушает `ScreenXDashBoards`).

## Решение

`GallerySaver.saveFromUrl` получает опциональный параметр
`progress: MutableStateFlow<Float>? = null`:

- `onStart` → `0f`
- `onProgress(p)` → `p / 100f`
- `onCompleted` → `-2f` (+ scan + снекбар, как сейчас)
- `onError` → `-3f` (+ снекбар, как сейчас)

Прокидка:

- `DownloadRed.saveToGallery` → `progress = downloader.percent` — бар R
  оживает без правок UI.
- `SavedX_Downloads.saveToGallery` → `progress = percent` — бар X аналогично.

`saveLocal` (копия из кеша) без прогресса — операция мгновенная.

## Тесты

Логика — проброс четырёх значений в flow внутри Android-колбэков KDownloader;
юнит не даёт ценности. Руками: R и X «В галерею» на нескачанном видео —
зелёный бар внизу бежит, по завершении гаснет; ошибка сети → бар в ошибку,
снекбар.

## Объём

`GallerySaver.kt` (~6 строк), `DownloadRed.kt` (1 аргумент),
`SavedX_Downloads.kt` (1 аргумент).
