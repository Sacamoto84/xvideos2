# Код-ревью xvideos — проход 112

> **Срез:** `d9ba203` + рабочее дерево · **Статус:** закрыт полностью · **Индекс:** [все документы](README.md)

База прохода `d9ba203`. Затронут модуль `:feature-l`: класс управления метаданными альбома [`AlbumInfo.kt`](../feature-l/src/main/java/com/client/xvideos/l/net/AlbumInfo.kt). Исправлено поведение вычисляемого свойства `downloadUrl`: ранее при отсутствии ссылки скачивания (`download_url` равен null или пустой строке) свойство возвращало голый базовый домен `"https://www.luscious.net"` вместо пустой строки. Устранён неиспользуемый аргумент `download` в основном конструкторе и удалено соответствующее подавление `UnusedPrivateProperty` из `config/detekt/baseline.xml`. Удалены закомментированные фрагменты устаревшего кода. Поведение покрыто тестами.

Линзы:
- `C` / Корректность адресации URL и чистота публичного API ([AlbumInfo.kt](../feature-l/src/main/java/com/client/xvideos/l/net/AlbumInfo.kt)).
- `Q` / Очистка Detekt baseline от устаревшего правила UnusedPrivateProperty ([baseline.xml](../config/detekt/baseline.xml)).
- `T` / Тестирование fallback-поведения свойства downloadUrl ([AlbumInfoRefreshTest.kt](../feature-l/src/test/java/com/client/xvideos/l/net/AlbumInfoRefreshTest.kt)).

Граница P2P (`core/.../p2p/`) заморожена по решению владельца от 11.09.2026 и не затрагивалась.

---

## Находки

### C23 — Ложный URL LusciousEndpoints.HOME при пустой ссылке скачивания и балластный параметр в AlbumInfo. Низкая.

[feature-l/src/main/java/com/client/xvideos/l/net/AlbumInfo.kt:198](../feature-l/src/main/java/com/client/xvideos/l/net/AlbumInfo.kt#L198)
[config/detekt/baseline.xml](../config/detekt/baseline.xml)

1. Свойство `downloadUrl` вычислялось как `LusciousEndpoints.HOME + albumInfo.value?.download_url.orEmpty()`. Если у альбома отсутствовала ссылка для скачивания, возвращался адрес домашней страницы Luscious, что вводило вызывающий код в заблуждение и вело к скачиванию главной HTML-страницы вместо архива.
2. В основном конструкторе `AlbumInfo` присутствовал параметр `download: Boolean = false`, который нигде не сохранялся и не использовался, порождая подавление `UnusedPrivateProperty` в `baseline.xml`.
3. В конце файла хранилось более 25 строк закомментированного мёртвого кода.

**Исправление:**
- `downloadUrl` переписан с безопасной проверкой `download_url?.takeIf { it.isNotBlank() }?.let { LusciousEndpoints.HOME + it }.orEmpty()`.
- Неиспользуемый параметр удалён из основного конструктора (сохранён вспомогательный вторичный конструктор для обратной совместимости).
- Удалена запись `UnusedPrivateProperty:AlbumInfo.kt` из `config/detekt/baseline.xml`.
- Удален закомментированный мертвый код.

---

### T55 — Модульное тестирование безопасного fallback свойства downloadUrl. Низкая.

[feature-l/src/test/java/com/client/xvideos/l/net/AlbumInfoRefreshTest.kt:88](../feature-l/src/test/java/com/client/xvideos/l/net/AlbumInfoRefreshTest.kt#L88)

Ранее логика формирования `downloadUrl` в `AlbumInfo` не проверялась изолированными тестами.

**Исправление:**
- В `AlbumInfoRefreshTest` добавлен тест `downloadUrl returns empty string when download_url is missing and prepends home when present`, проверяющий возврат пустой строки при отсутствии адреса архива.

---

## Сводка верификации

- `detekt`: 0 предупреждений и ошибок по всему проекту (`detekt --offline`).
- `testDebugUnitTest`: 100% тестов всех модулей зелёные (`testDebugUnitTest --offline`).
- `compileReleaseKotlin`: успешная компиляция release-версии всех модулей проекта.
