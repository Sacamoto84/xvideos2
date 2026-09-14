# Код-ревью xvideos — проход 22

> **Срез:** `master` · **Статус:** закрыт · **Индекс:** [все документы](README.md)

База прохода — `master` (коммит `e5d0281` после прохода 21).
Линзы:
- **Математическая и графическая корректность расчёта пропорций и масштабирования (защита от деления на ноль, `Float.NaN` и `Infinity` в Compose Modifier / GraphicsLayer)** (`L_FullScreenImage`, `PicsDetailsMedia`, `UrlImage`);
- **Устойчивость и граничные условия интерактивных жестов плеера при нулевой/отрицательной длительности видео и нулевых габаритах канваса** (`CanvasTimeDurationLine`);
- **UX экрана блокировки и согласованность валидации ввода между экранной клавиатурой (IME Action) и кнопкой подтверждения** (`AppLockScreen`, `Theme`);
- **Соблюдение архитектурного запрета на изменение P2P-транспорта** (`core/.../p2p/` не модифицировался).

Ключевой итог: **все 4 ключевые находки 22-го прохода устранены**. Репозиторий полностью собирается, detekt чист (0 замечаний), все unit-тесты успешно проходят (273 задачи в сборке выполнены).

---

## Находки

### C42 — Падение UI (`IllegalArgumentException`) при нулевых размерах медиа в ленте Luscious FullScreen. Высокая.

[L_FullScreenImage.kt:306](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenFullScreen/L_FullScreenImage.kt#L306), [PicsDetailsMedia.kt:11-13](../feature-l/src/main/java/com/client/xvideos/l/model/PicsDetailsMedia.kt#L11-L13)

В `L_FullScreenImage.kt` при рендеринге миниатюр нижней карусели использовался прямой расчет:
`Modifier.aspectRatio(it1.width.toFloat() / it1.height)`
Если у картинки `it1.height == 0` или `it1.width == 0` (поля по умолчанию в `PicsDetails` равны `0`), деление приводило к `Float.POSITIVE_INFINITY` или `Float.NaN`.
Реализация `Modifier.aspectRatio` в Compose содержит проверку:
`require(ratio > 0) { "aspectRatio $ratio must be greater than zero" }`
Значения `Infinity` или `NaN` немедленно вызывали необработанный `IllegalArgumentException` на UI-потоке, приводя к падению приложения.

*Исправление:*
- В `PicsDetailsMedia.kt` добавлена функция `PicsDetails.safeAspectRatio()` с безопасной проверкой `if (width > 0 && height > 0) width.toFloat() / height else 1f`.
- В `L_FullScreenImage.kt` прямой расчет заменен вызовом `.aspectRatio(it1.safeAspectRatio())`.
- Юнит-тест в `LPureFunctionsTest` дополнен прямой проверкой расширения `safeAspectRatio()`.

---

### C43 — Деление на ноль и передача `Infinity` в `graphicsLayer` при повороте изображения в UrlImage. Средняя.

[UrlImage.kt:289-305](../core/src/main/java/com/client/xvideos/common/coil/UrlImage.kt#L289-L305)

При отображении картинки в полноэкранном режиме с поворотом (`isFullScreen && rotate`) проверка:
`if (containerSize != IntSize.Zero)`
не защищала от случая, когда одна из сторон контейнера равна `0` (например, при анимации изменения размера или первого прохода лейаута `containerSize = IntSize(0, 500)`).
В этом случае условие `!= IntSize.Zero` истинно, что приводило к делению на ноль (`containerSize.height.toFloat() / containerSize.width`) и установке `scaleX = Float.POSITIVE_INFINITY` / `scaleY = Float.POSITIVE_INFINITY` в `graphicsLayer`.

*Исправление:*
- Условие ужесточено до `containerSize.width > 0 && containerSize.height > 0`.
- Расчет масштаба вынесен в локальное значение `val scale` без дублирования выражений для `scaleX` и `scaleY`.

---

### C44 — Потенциальный сбой при жестах перемотки при `size.width <= 0` или `duration <= 0` в таймлайне видео. Средняя.

[CanvasTimeDurationLine.kt:76-88, 213-257](../feature-r/src/main/java/com/client/xvideos/r/ui/video/CanvasTimeDurationLine.kt#L76-L88)

В Composable `CanvasTimeDurationLine1` жестовые обработчики тапа (`awaitFirstDown`) и перетаскивания (`horizontalDrag`) вычисляли время как:
`val newTime = (downX / size.width) * duration`
`onSeek(newTime.coerceIn(0f, duration.toFloat()))`
Если пользователь касался шкалы до окончания измерения компонента (`size.width <= 0`) или до загрузки длительности видео (`duration <= 0`, либо отрицательное значение `C.TIME_UNSET`), вычисление давало `NaN` или `Infinity`.
Вызов `coerceIn` при отрицательном `duration` приводил к падению с `IllegalArgumentException: Cannot coerce value to an empty range`.
Кроме того, добавление проверок внутрь метода поднимало цикломатическую сложность Composable-функции выше лимита detekt (`CyclomaticComplexMethod`).

*Исправление:*
- Вычисление позиции вынесено в чистую функцию `calculateSeekTime(x: Float, width: Int, duration: Int): Float?`, возвращающую `null` при неположительных размерах или длительности и проверяющую `!isNaN() && !isInfinite()`.
- Обработка жестов инкапсулирована в `Modifier.timelineSeekGestures`.
- Написан изолированный юнит-тест `CanvasTimeDurationLineTest` с 5 сценариями проверки граничных условий.

---

### UI17 — Рассинхронизация валидации ввода кода доступа между виртуальной клавиатурой и кнопкой отправки. Низкая.

[AppLockScreen.kt:296](../core/src/main/java/com/client/xvideos/common/applock/AppLockScreen.kt#L296)

В `keyboardActions` нажатие клавиши `ImeAction.Done` на виртуальной клавиатуре выполняло отправку при условии `password.isNotEmpty() && !isLockedOut`, тогда как кнопка «Разблокировать» на экране активировалась только при `password.isNotBlank() && !isLockedOut`.
При вводе одних лишь пробелов нажатие кнопки клавиатуры приводило к попытке авторизации с некорректным кодом, вызывая вывод ошибки и приближая пользователя к временной блокировке ввода (lockout).

*Исправление:*
- Условие в `keyboardActions` синхронизировано с кнопкой отправки: `password.isNotBlank() && !isLockedOut`.
- В рамках данного цикла также улучшен UX экрана: добавлен автоматический фокус на поле пароля и показ клавиатуры при входе на экран, а оформление переведено на гармоничную палитру Material 3 Lavender (`Purple80` / `Color(0xFF381E72)`).

---

## Статус

| Находка | Класс | Статус | Коммит |
| --- | --- | --- | --- |
| C42 | корректность / UI | закрыт | `master` |
| C43 | корректность / графика | закрыт | `master` |
| C44 | корректность / жесты | закрыт | `master` |
| UI17 | интерфейс / UX | закрыт | `master` |

---

## Проверка

### Автоматические тесты
```powershell
./gradlew test --no-daemon
./gradlew detekt --no-daemon
```

Фактический результат:
- `test`: **BUILD SUCCESSFUL**, 273 actionable tasks, 0 ошибок. Все тесты модулей `:app`, `:core`, `:feature-l`, `:feature-r`, `:feature-x` пройдены.
- `detekt`: **BUILD SUCCESSFUL**, 12 actionable tasks, **0 issues**.

---

## Что осталось открытым

P2P-транспортный слой (`core/.../p2p/`) остаётся перманентно замороженным согласно решению владельца от 11.09.2026.
Все остальные модули и компоненты чисты, полностью типизированы и протестированы.
