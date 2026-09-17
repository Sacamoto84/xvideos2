# Код-ревью xvideos — проход 91

> **Срез:** `94e4c62` + рабочее дерево · **Статус:** закрыт полностью · **Индекс:** [все документы](README.md)

База прохода `94e4c62`. Затронуты модули `:feature-x`, `:feature-r`. Выполнена дедупликация утилит извлечения `Activity` через общее ядро `:core`, удалён мёртвый код и избыточный `Scaffold` в профиле Red, устранены однобуквенные переменные и обеспечен сброс флага загрузки `isLoading` при отмене задач.

Линзы:
- `A` / Дедупликация функции-расширения `Context.findActivity()` через общий модуль `:core` ([ScreenX_VideoPlayer.kt](../feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/ScreenX_VideoPlayer.kt), [ScreenX_VideoPlayerFullScreen.kt](../feature-x/src/main/java/com/client/xvideos/x/screens/videoplayerFullScreen/ScreenX_VideoPlayerFullScreen.kt)).
- `UI` / Устранение избыточного `Scaffold` без верхних/нижних баров и аннотаций `@SuppressLint` в `ScreenRedProfile` ([ScreenRedProfile.kt](../feature-r/src/main/java/com/client/xvideos/r/ui/profile/ScreenRedProfile.kt)).
- `C` / Сброс флага загрузки `isLoading.value = false` при вызове `ScreenRedProfileSM.clear()` ([ScreenRedProfileSM.kt](../feature-r/src/main/java/com/client/xvideos/r/ui/profile/ScreenRedProfileSM.kt)).
- `S` / Удаление закомментированного мёртвого кода и промежуточных переменных `val r`, `val res` в `RedApi` ([RedApi.kt](../feature-r/src/main/java/com/client/xvideos/r/network/api/RedApi.kt)).
- `S` / Чистка однобуквенных переменных (`res`, `s`, `h`, `m`, `sec`, `p`, `it1`, `t`) в видеоплеерах, загрузчике и экранах ([ScreenX_VideoPlayerSM.kt](../feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/ScreenX_VideoPlayerSM.kt), [ScreenX_VideoPlayerFullScreenSM.kt](../feature-x/src/main/java/com/client/xvideos/x/screens/videoplayerFullScreen/ScreenX_VideoPlayerFullScreenSM.kt), [country.kt](../feature-x/src/main/java/com/client/xvideos/x/feature/country/country.kt), [X_PlayerBottomBar.kt](../feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/atom/X_PlayerBottomBar.kt), [Downloader.kt](../feature-r/src/main/java/com/client/xvideos/r/common/downloader/Downloader.kt), [ScreenRedProfile.kt](../feature-r/src/main/java/com/client/xvideos/r/ui/profile/ScreenRedProfile.kt), [ScreenRedProfileSM.kt](../feature-r/src/main/java/com/client/xvideos/r/ui/profile/ScreenRedProfileSM.kt)).

Граница P2P (`core/.../p2p/`) заморожена по решению владельца от 11.09.2026 и не затрагивалась.

---

## Находки

### A38 — Дублирование функции извлечения Activity из ContextWrapper. Низкая.

[feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/ScreenX_VideoPlayer.kt:250-257](../feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/ScreenX_VideoPlayer.kt#L250)
[feature-x/src/main/java/com/client/xvideos/x/screens/videoplayerFullScreen/ScreenX_VideoPlayerFullScreen.kt:208-215](../feature-x/src/main/java/com/client/xvideos/x/screens/videoplayerFullScreen/ScreenX_VideoPlayerFullScreen.kt#L208)

В обоих экранах видеоплеера X дублировалась приватная функция-расширение `Context.findActivityOrNull()`, разворачивающая цепочку `ContextWrapper` до `Activity`. При этом в модуле `:core` уже существует каноническая функция `Context.findActivity(): Activity?` (`com.client.xvideos.common.util.findActivity`).

**Исправление:**
- Удалены локальные копии `Context.findActivityOrNull()`.
- Использована единая утилита `Context.findActivity()` из `:core`.
- Удалены неиспользуемые импорты `Activity`, `Context`, `ContextWrapper`.

---

### UI120 — Избыточный пустой Scaffold и подавление предупреждений в ScreenRedProfile. Низкая.

[feature-r/src/main/java/com/client/xvideos/r/ui/profile/ScreenRedProfile.kt:90-120](../feature-r/src/main/java/com/client/xvideos/r/ui/profile/ScreenRedProfile.kt#L90)

Компонент `RedProfileScreenContent` оборачивался в `Scaffold(containerColor = Theme.background)`, у которого не было ни верхнего бара, ни нижнего бара, ни плавающей кнопки. Параметр `padding` игнорировался, что приводило к необходимости аннотации `@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")`. Внутри `Scaffold` также содержалось 12 пустых строк. В `Content()` присутствовало устаревшее подавление несуществующего `UnusedBoxWithConstraintsScope`.

**Исправление:**
- Избыточный `Scaffold` заменён на стандартный легковесный контейнер `Box(modifier = Modifier.fillMaxSize().background(Theme.background))`.
- Удалены обе аннотации `@SuppressLint` и импорты `android.annotation.SuppressLint` и `androidx.compose.material3.Scaffold`.
- Упрощена обработка добавления тегов в `onAppendLoaded`: устранены вложенные `it1`/`it2`/`t` в пользу прямого `pager.itemSnapshotList.items.forEach { gifItem -> vm.tagsAdd(gifItem.tags) }`.

---

### C146 — Зависание флага isLoading при отмене загрузки профиля в ScreenRedProfileSM.clear(). Средняя.

[feature-r/src/main/java/com/client/xvideos/r/ui/profile/ScreenRedProfileSM.kt:189-195](../feature-r/src/main/java/com/client/xvideos/r/ui/profile/ScreenRedProfileSM.kt#L189)

При сбросе профиля (`clear()`) вызывалась отмена корутины `loadJob?.cancel()`. Если отмена происходила до того, как корутина успела зайти в `try` или если отменённый джоб был заменен, флаг `isLoading.value` мог остаться в значении `true`, блокируя последующие вызовы `loadNextPage`.

**Исправление:**
- В `ScreenRedProfileSM.clear()` добавлен явный сброс `isLoading.value = false`.
- Переименована переменная `val r` $\rightarrow$ `result`.

---

### S43 — Закомментированный мёртвый код и однобуквенные переменные в RedApi, Downloader, плеерах X. Низкая.

[feature-r/src/main/java/com/client/xvideos/r/network/api/RedApi.kt:142-173](../feature-r/src/main/java/com/client/xvideos/r/network/api/RedApi.kt#L142)
[feature-r/src/main/java/com/client/xvideos/r/common/downloader/Downloader.kt:86,169](../feature-r/src/main/java/com/client/xvideos/r/common/downloader/Downloader.kt#L86)
[feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/ScreenX_VideoPlayerSM.kt:129-165](../feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/ScreenX_VideoPlayerSM.kt#L129)
[feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/atom/X_PlayerBottomBar.kt:119-126](../feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/atom/X_PlayerBottomBar.kt#L119)

В `RedApi` присутствовали закомментированные фрагменты кода и промежуточные переменные `val r`, `val res`. В `Downloader.kt` строковый путь обозначался как `val p`. В `ScreenX_VideoPlayerSM` и `ScreenX_VideoPlayerFullScreenSM` переменные кэша и html-содержимого именовались `res` и `s`. В `X_PlayerBottomBar.formatTime` секунды и часы обозначались `s`, `h`, `m`, `sec`.

**Исправление:**
- В `RedApi` удалён закомментированный код, результаты запросов возвращаются напрямую через `return api.request(...)`.
- `p` переименован в `creatorPath`.
- `res` и `s` переименованы в `cachedHtml` и `htmlContent`.
- `s`, `h`, `m`, `sec` переименованы в `validSeconds`, `hours`, `minutes`, `seconds`.
- В `country.kt` переменные `s` и `it1` переименованы в `htmlContent` и `selectedFlag`.

---

## Сводка верификации

- `detekt`: 0 предупреждений и ошибок во всех модулях проекта (`:app`, `:core`, `:feature-l`, `:feature-r`, `:feature-x`).
- `testDebugUnitTest`: 140 задач выполнено, все юнит-тесты успешно пройдены (100%).
- `compileReleaseKotlin`: 61 задача выполнена, чистая сборка релизных Kotlin-артефактов без ошибок.
