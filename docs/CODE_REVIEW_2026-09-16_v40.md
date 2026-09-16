# Код-ревью xvideos — проход 61

> **Срез:** `b737438` + рабочее дерево · **Статус:** закрыт · **Индекс:** [все документы](README.md)

База прохода `b737438`. Изменено 6 исходных файлов в модулях `feature-r`, `feature-x`, `feature-l` и `app`, расширены unit-тесты в `feature-r` (`RedPooledVideoPlayerTest.kt`).

Линзы:
- `S` / Защита A-B зацикливания видеоплеера от некорректных значений `Float.NaN` и бесконечностей в [RedPooledVideoPlayer.kt](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/ui/video/RedPooledVideoPlayer.kt).
- `UI` / Фиксация стабильности Compose-контрактов (`@Stable`) для ScreenModel-моделей экранов в [ScreenFavoritesSM.kt](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/screens/favorites/ScreenFavoritesSM.kt), [ScreenXDashBoardsSM.kt](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/screens/dashboards/vm/ScreenXDashBoardsSM.kt), [R_Screen_Root.kt](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/ui/root/R_Screen_Root.kt), [ScreenExplorer.kt](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/ui/explorer/ScreenExplorer.kt), [L_ScreenExplorer.kt](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/L_ScreenExplorer.kt) и [ScreenRoot.kt](file:///g:/xvideos2/app/src/main/java/com/client/xvideos/screenRoot/ScreenRoot.kt).

Граница P2P (`core/.../p2p/`) заморожена по решению владельца от 11.09.2026 и не затрагивалась.

---

## Находки

### S40 — Передача невалидных Float (NaN / Infinite) в перемотку и A-B луп видеоплеера. Средняя.

[feature-r/src/main/java/com/client/xvideos/r/ui/video/RedPooledVideoPlayer.kt:151-165](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/ui/video/RedPooledVideoPlayer.kt#L151)

В `RedPooledVideoPlayer` логика перемотки и зацикливания A-B проверяла только `enableAB && timeB > timeA`. Если в `timeA` или `timeB` попадал `Float.NaN` или `Float.POSITIVE_INFINITY` (например, при вычислении смещений на основе неинициализированной длительности медиаресурса или сбоя конверсии времени), сравнение `timeB > timeA` могло вести себя непредсказуемо (для `NaN` сравнение ложно, но для `Infinity` истинно), приводя к передаче бесконечных значений в `player.seekTo(...)` или зацикливанию плеера в невалидной позиции.

**Исправление:**
- Вынесена чистая функция-предикат `isValidABRange(enableAB: Boolean, timeA: Float, timeB: Float): Boolean`, строго требующая `enableAB && timeA.isFinite() && timeB.isFinite() && timeA >= 0f && timeB > timeA`.
- В `RedPooledVideoPlayerTest.kt` добавлен модульный тест, проверяющий отсечение `NaN`, бесконечностей и отрицательных значений.

---

### UI75 — Отсутствие @Stable на классах Voyager ScreenModel в корневых экранах. Средняя.

[feature-x/src/main/java/com/client/xvideos/x/screens/favorites/ScreenFavoritesSM.kt:13](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/screens/favorites/ScreenFavoritesSM.kt#L13)
[feature-x/src/main/java/com/client/xvideos/x/screens/dashboards/vm/ScreenXDashBoardsSM.kt:16](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/screens/dashboards/vm/ScreenXDashBoardsSM.kt#L16)
[feature-r/src/main/java/com/client/xvideos/r/ui/root/R_Screen_Root.kt:16](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/ui/root/R_Screen_Root.kt#L16)
[feature-r/src/main/java/com/client/xvideos/r/ui/explorer/ScreenExplorer.kt:15](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/ui/explorer/ScreenExplorer.kt#L15)
[feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/L_ScreenExplorer.kt:19](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/L_ScreenExplorer.kt#L19)
[app/src/main/java/com/client/xvideos/screenRoot/ScreenRoot.kt:33](file:///g:/xvideos2/app/src/main/java/com/client/xvideos/screenRoot/ScreenRoot.kt#L33)

В корневых экранах модулей `ScreenModel` передаются в Composable-функции экранов. Из-за отсутствия явной аннотации `@Stable` компилятор Compose мог расценивать ScreenModel как нестабильные параметры, что приводило к пропуску оптимизации скиппинга (skippable recomposition) при рекомпозиции родительских контейнеров.

**Исправление:**
- Добавлена аннотация `@Stable` к ключевым `ScreenModel` корневых и навигационных экранов всех функциональных модулей (`ScreenFavoritesSM`, `ScreenXDashBoardsScreenModel`, `ScreenRedRootSM`, `ScreenRedExplorerSM`, `L_ScreenExplorerSM`, `ScreenRootSM`).

---

## Итог верификации

- **Unit-тесты:** `./gradlew testDebugUnitTest` — успешно (все тесты по всем 5 модулям выполнены без ошибок).
- **Detekt:** `./gradlew detekt --rerun-tasks` — 0 ошибок и предупреждений по всем 5 модулям.
- **Архитектурные ограничения:** Замороженный P2P (`core/.../p2p/`) не изменялся.
