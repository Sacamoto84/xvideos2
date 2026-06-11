# «Сохранить в галерею» в меню X / L / R

Дата: 2026-06-11
Статус: одобрено пользователем

## Контекст

В выпадающих меню всех трёх разделов нужен пункт «В галерею»: полноразмерное
медиа (не превью) сохраняется в `/storage/emulated/0/xvideos_download`,
видимую системной галерее. `MANAGE_EXTERNAL_STORAGE` у приложения уже есть
(пишет в `/storage/emulated/0/xvideos` через File API), поэтому обычный
File API + `MediaScannerConnection.scanFile`. В папке НЕТ `.nomedia` —
в отличие от служебной `xvideos/`.

## Общий механизм: `GallerySaver` (common)

Объект/класс в `common`:

- `val root = File(Environment.getExternalStorageDirectory(), "xvideos_download")`,
  создаётся при первом обращении.
- `saveLocal(context, src: File, fileName: String)` — копия `src` в `root/fileName`,
  затем `MediaScannerConnection.scanFile`, снекбар «Сохранено в галерею».
- `saveFromUrl(context, kDownloader, url: String, fileName: String)` —
  KDownloader качает напрямую в `root` (снекбар «Сохранение в галерею…»
  на старте), по завершении scan + снекбар успеха; ошибка → снекбар ошибки.
- Файл `root/fileName` уже существует → снекбар «Уже в галерее», выход
  (без перезаписи и повторного скачивания).
- Прогресс-бар не делаем — только снекбары (как у существующих закачек).

## Поведение по разделам

### X — меню дашборда + меню Избранного

- Файл `x_cache_download/<id>.mp4` существует → `saveLocal`.
- Нет → резолв прямого mp4 наилучшего качества тем же путём, что
  `SavedX_Downloads.download` (`readHtmlFromURLDirect` → `parserItemVideo` →
  `parseHTML5Player`). Резолв выносится из `download()` в переиспользуемую
  suspend-функцию (например `resolveDirectVideoUrl(item): String?`),
  `download()` переводится на неё. Затем `saveFromUrl`.
- Имя файла: `x_<id>.mp4`.

### L — меню альбома + меню Likes

- Item сохранён в лайках (`lFindLikeFolder`) → из папки лайка берётся
  **большой файл** (оригинал/видео, не превью) → `saveLocal`.
  Выбор большого файла — чистая функция (тестируема).
- Не сохранён → существующий `lDownloadMediaToShareCache(item)` (он скачивает
  оригинал; для видео — видео) → результат `saveLocal`.
- Имя файла: оригинальное имя скачанного файла (из URL/папки лайка).
- Колбэк добавляется в `ExpandMenuViewModel` (`saveToGallery(item)`),
  пункт — в `AlbumItemExpandMenu` и `SavedLikesItemExpandMenu`.

### R — общее меню видео (`ExpandMenuVideo`)

- `r_cache_download/<user>/<id>.mp4` существует → `saveLocal`.
- Нет → `downloadVideoUrl()` (hd → sd → silent) → `saveFromUrl` напрямую
  в галерею-папку, МИМО кеша R (не плодим записи в «Загрузках» отправителя).
- Имя файла: `r_<userName>_<id>.mp4`.
- Реализация — метод в `DownloadRed` (`saveToGallery(item)`), пункт меню —
  новый `DropdownMenuItem_SaveToGallery`.

## Отклонённые альтернативы

- Качать в раздел-кеш и копировать — двойная запись на диск, мусор в списках
  разделов (R «Загрузки», X Saved).
- MediaStore API (`RELATIVE_PATH`) — не нужен: All files access уже есть,
  а MediaStore не позволяет произвольную папку в корне.

## Тесты

- Юнит: выбор «большого файла» из папки L-лайка (не превью, видео
  приоритетнее), если выделяется чистой функцией.
- Руками: из каждого раздела по одному сохранению (кешированный файл и
  некешированный), файл появляется в `/storage/emulated/0/xvideos_download`
  и виден в системной галерее; повторное сохранение → «Уже в галерее».

## Объём

- Новый `GallerySaver` (~60 строк) в `common`.
- X: рефактор резолва URL в `SavedX_Downloads` + пункт в 2 меню + прокладка колбэков.
- L: функция выбора большого файла + `saveToGallery` в `ExpandMenuViewModel` + пункт в 2 меню.
- R: `DownloadRed.saveToGallery` + `DropdownMenuItem_SaveToGallery` + пункт в `ExpandMenuVideo`.
