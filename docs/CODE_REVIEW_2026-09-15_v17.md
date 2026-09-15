# Код-ревью xvideos — проход 38

> **Срез:** `a47bbf1` + рабочее дерево · **Статус:** закрыт · **Индекс:** [все документы](README.md)

База прохода `a47bbf12e2e6739b9afba200becf86ed227e1b96`. Изменено 6 файлов в модуле `feature-l`.
Линзы: конкурентность и персистентность (`T`), производительность и аллокации Compose (`UI`), UX и стабильность списков (`UI`), надёжность отрисовки Canvas (`C`). Граница P2P (`core/.../p2p/`) заморожена по решению владельца и не затрагивалась.

## Находки

### T43 — Гонка параллельных записей и рассинхронизация `persist` в `AlbumFilterPresetManager`. Средняя.

[feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/molecule/filter/atom/AlbumFilterPresetManager.kt:59](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/molecule/filter/atom/AlbumFilterPresetManager.kt#L59)

В методах `savePreset` и `deletePreset` список для сохранения в `SharedPreferences` захватывался из локальной переменной CAS-цикла и передавался в асинхронный запуск `scope.launch { persist(prefs, updatedList) }` на пуле `Dispatchers.IO`. При последовательном или конкурентном сохранении/удалении нескольких пресетов корутины могли выполниться в обратном порядке, затирая актуальные данные устаревшим состоянием.
**Исправление:** Запись сериализована через `persistMutex: Mutex`, а сохранение выполняется по актуальному срезу `_presets.value` под блокировкой `persistMutex.withLock`.

### UI39 — Аллокация и парсинг `SimpleDateFormat` на каждый кадр рекомпозиции в `AlbumFilterSavedPresetsDialog`. Низкая.

[feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/molecule/filter/atom/AlbumFilterSavedPresetsDialog.kt:58](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/molecule/filter/atom/AlbumFilterSavedPresetsDialog.kt#L58)

В теле Composable-функции `AlbumFilterSavedPresetsDialog` экземпляр `val dateFormat = SimpleDateFormat("d MMM, HH:mm", Locale.getDefault())` создавался напрямую без `remember`. При любых рекомпозициях диалога происходило повторное создание тяжёлого объекта парсера даты и компиляция строки шаблона, создавая избыточное давление на GC.
**Исправление:** Инициализация обёрнута в `remember { SimpleDateFormat(...) }`.

### UI40 — Отсутствие плейсхолдера в `AlbumFilterSaveDialog` при пустом вводе имени. Низкая.

[feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/molecule/filter/atom/AlbumFilterSaveDialog.kt:108](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/molecule/filter/atom/AlbumFilterSaveDialog.kt#L108)

В поле ввода `BasicTextField` отсутствовал плейсхолдер. Если пользователь стирал предложенное имя пресета, текстовое поле выглядело как пустой неинформативный тёмный блок.
**Исправление:** В `Box` с текстовым полем добавлен адаптивный плейсхолдер `"Preset name"`, исчезающий при вводе текста.

### UI41 — Отсутствие стабильных ключей `key` в `LazyColumn` диалога выбора фильтров `AlbumFilterSelectDialog`. Низкая.

[feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/molecule/filter/atom/AlbumFilterSelectDialog.kt:112](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/molecule/filter/atom/AlbumFilterSelectDialog.kt#L112)

Вызов `items(items)` в списке опций фильтрации не передавал ключ идентификатора `key`. При выборе пункта или обновлении списка Compose производил позиционную рекомпозицию всех элементов списка вместо эффективного точечного обновления.
**Исправление:** Добавлен стабильный ключ: `items(items, key = { itemTitle(it) })`.

### UI42 — Потенциальное зацикливание отрисовки при делении на ноль в `CheckerboardBackground` и избыточный проброс `palette`. Низкая.

[feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenFullScreen/CheckerboardBackground.kt:24](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenFullScreen/CheckerboardBackground.kt#L24), [feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/molecule/filter/atom/AlbumFilterAudiencesDialog.kt:136](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/molecule/filter/atom/AlbumFilterAudiencesDialog.kt#L136)

1. В `CheckerboardBackground.kt` расчёт `(size.width / squareSizePx).toInt()` не проверял `squareSizePx <= 0f` или нулевой размер холста. При нулевом размере пикселя деление с плавающей точкой давало `Float.POSITIVE_INFINITY`, что при приведении к `Int` давало `Int.MAX_VALUE` (2 147 483 647 итераций в цикле отрисовки Canvas), приводя к ANR.
2. В `AlbumFilterAudiencesDialog.kt` объект `palette: StyleGenresTags.Palette` избыточно пробрасывался в приватную функцию строки вместо прямого доступа к синглтону.
**Исправление:**
- В `CheckerboardBackground` добавлен guard: `if (squareSizePx <= 0f || size.width <= 0f || size.height <= 0f) return@drawBehind`.
- В `AlbumFilterAudiencesDialog` убран лишний параметр и обеспечен прямой доступ к палитре.

## Статус

| Находка | Класс | Статус | Описание / Исправление |
| --- | --- | --- | --- |
| T43 | конкурентность | закрыт | `AlbumFilterPresetManager`: сериализация сохранения через `Mutex` и срез `_presets.value` |
| UI39 | производительность | закрыт | `AlbumFilterSavedPresetsDialog`: `SimpleDateFormat` обёрнут в `remember` |
| UI40 | UI / UX | закрыт | `AlbumFilterSaveDialog`: добавлен плейсхолдер `"Preset name"` |
| UI41 | UI / производительность | закрыт | `AlbumFilterSelectDialog`: стабильные ключи `key = { itemTitle(it) }` в `LazyColumn` |
| UI42 | корректность / UI | закрыт | `CheckerboardBackground`: защита от деления на ноль; очистка сигнатуры в `AlbumFilterAudiencesDialog` |

## Проверка

```
> Task :feature-l:compileDebugKotlin SUCCESS
> Task :feature-l:detekt SUCCESS
> Task :feature-l:testDebugUnitTest SUCCESS
> Task :app:testDebugUnitTest SUCCESS
BUILD SUCCESSFUL in 12s
```

## Что осталось открытым

- Все открытые ранее проектные решения владельца (включая заморозку P2P) сохраняются в силе.
- Все находки прохода 38 закрыты полностью.
