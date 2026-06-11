# R P2P: передача только метаданных (.info + .jpg, без видео)

Дата: 2026-06-11
Статус: одобрено пользователем

## Контекст

P2P-передача в R сейчас шлёт полный бандл (mp4 + jpg + info) и требует
скачанного видео (автоскачивание перед отправкой). Для R это лишнее:
получатель отображает контент по метаданным (видео стримится по URL из
`GifsInfo`). Передаём только `.info` + `.jpg` — быстро, без тяжёлых задач.

Применяется **везде в R**: меню фида/профиля (`ExpandMenuVideo`) и
Saved-таб (`R_Screen_Saved_DownloadTab`). L и X не трогаем.

## Отправитель

### Новое: `DownloadRed.shareMetaByP2p(item: GifsInfo, onReady: (P2pExportBundle) -> Unit)`

Собирает временный бандл в `context.cacheDir/p2p_r_export/<userName>/<id>.*`:

1. Папка экспорта очищается перед сборкой (`deleteRecursively`).
2. `.info` — сериализация `GifsInfo` через gson (формат идентичен тому,
   что пишет закачка в `Downloader.downloadRedName onCompleted`).
3. `.jpg`:
   - локальный `r_cache_download/<user>/<id>.jpg` существует → копируем в tmp;
   - нет → качаем превью по `item.previewUrl()` через KDownloader в tmp;
   - URL отсутствует или ошибка скачивания → бандл без jpg (не блокируем).
4. `onReady(P2pExportBundle(P2pType.R, tmpRoot, files, infoFile))` на Main.

mp4 не участвует. В `r_cache_download` отправителя ничего не пишется —
иначе мусорная запись в его «Загрузках» и recovery начнёт докачивать видео.

Чистая сборка (готовые файлы → бандл) выносится в функцию без сети/Android —
юнит-тестируема. Сетевая часть (докачка jpg) — тонкая обёртка.

### Подключение

- `ExpandMenuVideo` onP2p: `shareMetaByP2p(item) { navigator?.push(ScreenP2pSend(it)) }`.
  Убираем `ensureDownloaded` + `RExporter` из этого пути.
- `R_Screen_Saved_DownloadTab` onP2p: то же самое (единое поведение).
  `RExporter`/`RBundleLocator` из P2P-флоу R больше не используются
  (сами объекты остаются — их используют тесты и X/L-аналоги).
- Системный share не меняется (ему нужен mp4, `downloadItemAndShare` остаётся).

## Получатель

- `P2pBundleInstaller` без изменений: кладёт `<user>/<id>.info|.jpg`
  в `r_cache_download` по relativePath из манифеста.
- `P2pReceiveManager.refreshFor`: добавить ветку `P2pType.R` →
  `DownloadRed.refreshDownloadList()` через Hilt EntryPoint
  (`P2pRefreshEntryPoint` дополняется методом `downloadRed()`).
  Отменяет прошлое решение «refresh только для L»: R-приём теперь
  рабочий сценарий, без refresh список не обновится до рестарта.
- Отображение: список «Загрузки» читает `.info` → `GifsInfo`; превью —
  переданный jpg; видео играет по URL. Размер файла покажет 0 (mp4 нет) —
  осознанная косметика.
- `recoverIncompleteDownloads` посчитает такие item'ы недокачанными и при
  ручном запуске докачает видео — это фича: получатель сам решает,
  нужен ли оффлайн.

## Отклонённые альтернативы

- Писать `.info` в `r_cache_download` отправителя и слать оттуда — мусор
  в списке Загрузок отправителя + ложные кандидаты на докачку.
- Флаг metadata-only в манифесте — не нужен: установщик просто кладёт
  присланные файлы, состав бандла определяет отправитель.
- Только `.info` без jpg — пустые превью в списке получателя
  (список рисует локальный jpg).

## Тесты

- Юнит: сборка metadata-бандла — (а) info + jpg, (б) только info;
  правильный storeRoot/relativePath (через существующий
  `P2pManifestFactory`).
- Руками: P2P из фида (jpg качается) и из Saved (jpg копируется);
  у получателя список обновился сразу, превью на месте, видео играет
  по URL; у отправителя в «Загрузках» мусора не появилось.

## Объём

- `DownloadRed.kt`: +`shareMetaByP2p` (~40 строк), чистый билдер.
- `ExpandMenuVideo.kt`: onP2p упрощается.
- `R_Screen_Saved_DownloadTab.kt`: onP2p упрощается.
- `P2pReceiveManager.kt`: ветка R в `refreshFor`, EntryPoint +1 метод.
- Тест на билдер бандла.
