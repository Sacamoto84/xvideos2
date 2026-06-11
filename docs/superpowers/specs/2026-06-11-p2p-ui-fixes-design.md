# P2P UI: три правки (плашка, автозакрытие, refresh Likes)

Дата: 2026-06-11
Статус: одобрено пользователем

## Контекст

P2P-передача файлов между телефонами (пакет `com.client.xvideos.common.p2p`) работает.
Три UX-проблемы:

1. Плашка статуса приёма на рут-экране рисуется под вырезом камеры.
2. После успешной отправки экран отправителя не закрывается сам.
3. После приёма бандла типа L список Likes не обновляется — нужен перезапуск приложения.

## Правка 1: плашка под вырезом камеры

**Где:** `P2pBackgroundOverlay` в `app/src/main/java/com/client/xvideos/MainActivity.kt`
(строка ~276, модификатор `Surface`).

**Причина:** модификатор `.statusBarsPadding()` возвращает 0, потому что приложение
прячет системные бары (`windowInsetsController?.hide(WindowInsetsCompat.Type.systemBars())`,
MainActivity:150). Инсет выреза камеры — отдельный (`displayCutout`), он сообщается
даже при спрятанных барах.

**Решение:** заменить `.statusBarsPadding()` на `.displayCutoutPadding()`.

- На устройствах с вырезом — плашка опускается ровно под вырез.
- На устройствах без выреза — отступ 0, плашка сверху (как сейчас, приемлемо).
- Краевой случай: при свайпе бары показываются на ~2 сек (transient) и на
  устройствах без выреза плашка окажется под ними. Осознанно игнорируем.

## Правка 2: автозакрытие экрана отправки

**Где:** `Done`-ветка в `app/src/main/java/com/client/xvideos/common/p2p/ui/ScreenP2pSend.kt`.

**Решение:** в композицию ветки `ShareState.Done` добавить:

```kotlin
LaunchedEffect(Unit) {
    delay(1_000)
    navigator.pop()
}
```

- «Готово ✓» видно 1 секунду, затем авто-pop.
- Кнопка «Вернуться» остаётся — можно закрыть раньше вручную.
- Уход с экрана отменяет `LaunchedEffect` — двойного pop не будет
  (composable покидает композицию вместе с экраном).

## Правка 3: обновление Likes после приёма L

**Где:** `app/src/main/java/com/client/xvideos/common/p2p/P2pReceiveManager.kt`,
параметр `refreshFor = { /* ... */ }` (строка ~53) — сейчас пустой.

**Причина:** `StoreBundleImporter.import()` вызывает `refreshFor(manifest.type)`
после установки бандла, но колбэк ничего не делает.

**Решение:** Hilt EntryPoint. `P2pReceiveManager` — обычный `object` вне DI-графа,
но в `start(context)` доступен `applicationContext`:

```kotlin
@EntryPoint
@InstallIn(SingletonComponent::class)
interface P2pRefreshEntryPoint {
    fun savedL(): SavedL
}
```

В `start()`:

```kotlin
refreshFor = { type ->
    if (type == P2pType.L) {
        EntryPointAccessors
            .fromApplication(context.applicationContext, P2pRefreshEntryPoint::class.java)
            .savedL().likes.refresh()
    }
}
```

- `SavedL` — `@Singleton`; `likes.refresh()` читает каталог на IO и обновляет
  `mutableStateListOf` на Main — открытый экран Likes перерисуется сразу.
- X и R не трогаем: их экраны Saved перечитывают список при открытии
  (подтверждено пользователем).
- EntryPoint-интерфейс размещаем рядом с `P2pReceiveManager` (тот же файл).

## Отклонённые альтернативы

- Плашка: `union(statusBars, displayCutout)` или фиксированный dp — пользователь
  выбрал чистый `displayCutoutPadding()`.
- Refresh: событие через EventBus + подписка в `SavedL` — больше плумбинга
  ради того же результата.

## Тестирование

- Правки 1 и 2 — визуальные/навигационные, проверка руками на устройстве
  (плашка ниже выреза; экран отправителя закрывается через ~1 сек после «Готово»).
- Правка 3 — ручная проверка: принять L-бандл при открытом экране Likes,
  список должен пополниться без перезапуска. Юнит-тест не добавляем:
  логика — один `if` поверх уже покрытого `StoreBundleImporter`.

## Объём

Три файла: `MainActivity.kt` (1 строка), `ScreenP2pSend.kt` (~5 строк),
`P2pReceiveManager.kt` (~15 строк, включая EntryPoint).
