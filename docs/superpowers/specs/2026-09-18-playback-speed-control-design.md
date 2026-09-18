# Спецификация: Управление скоростью воспроизведения видео

Дата: 18.09.2026. Согласовано в рамках сессии Brainstorming.

---

## 1. Понимание задачи (Understanding Summary)

* **Что создаётся:** Элемент управления скоростью воспроизведения видео с выпадающим меню (`DropdownMenu`) на 7 фиксированных скоростей: `0.25x`, `0.5x`, `0.75x`, `1.0x`, `1.25x`, `1.5x`, `2.0x`.
* **Где размещается:** Во всех основных видеоплеерах приложения:
  * Плеер раздела X: онлайн (`ScreenX_VideoPlayer`) и локальные файлы (`ScreenX_LocalVideoPlayer`) через панель `X_PlayerBottomBar`;
  * Полноэкранный плеер раздела R: лента и одиночный просмотр (`ScreenRedFullScreen`) через панель `FeedControls_Container_Line0`.
* **Для чего:** Предоставить пользователям возможность замедлять видео для детального анализа или ускорять для быстрого ознакомления.
* **Для кого:** Пользователи приложения, просматривающие видео в разделах X и R.
* **Ключевые ограничения:**
  * Меню выбора скорости — компактный всплывающий элемент (`DropdownMenu`) непосредственно над кнопкой скорости с визуальной отметкой (галочкой и жирным шрифтом) активного значения;
  * Срок действия выбранной скорости: действует только для текущего видео / сессии просмотра и сбрасывается в `1.0x` при открытии или свайпе на новый ролик;
  * Плавное переключение скорости воспроизведения на лету через Media3 ExoPlayer API без буферизации и перезапуска медиапотока;
  * В плеере со звуком (раздел R) сохраняется естественная тональность голоса (pitch compensation через Media3 Sonic).
* **Явные non-goals:**
  * Произвольный ввод скорости ползунком (поддерживаются только 7 фиксированных пресетов);
  * Глобальное сохранение выбранной скорости в настройках `Settings` для всех последующих роликов;
  * Изменение скорости в миниатюрных превью списков (`UrlVideoLite`).

---

## 2. Предположения (Assumptions)

1. **Модель `:core`:** Перечисление `PlayerSpeed` (`com.client.xvideos.common.videoplayer.model.PlayerSpeed`) расширяется значениями: `X0_25 (0.25f)`, `X0_5 (0.5f)`, `X0_75 (0.75f)`, `X1 (1.0f)`, `X1_25 (1.25f)`, `X1_5 (1.5f)`, `X2 (2.0f)`.
2. **Контракт `MediaPlayerHost`:** В `MediaPlayerHost` раскомментируется метод `setSpeed(speed: PlayerSpeed)`, обновляющий Compose-стейт `speed`. Он автоматически транслируется в `CMPlayer2` через существующий `LaunchedEffect(exoPlayer, config.speed)`.
3. **UI плеера X:** В нижней панели `X_PlayerBottomBar` добавляется кнопка скорости рядом с переключателем `Fit / Fill` и кнопкой полного экрана.
4. **UI плеера R:** В панель управления `FeedControls_Container_Line0` добавляется кнопка скорости и связывается со стейтом экрана `ScreenRedFullScreenSM`. В `RedPooledVideoPlayer` подключается эффект применения скорости к активному инстансу `ExoPlayer` из пула.
5. **Стилизация меню:** Всплывающее меню оформляется в тёмной полупрозрачной стилистике видеоплеера с контрастным текстом и индикацией активного пункта.
6. **Тестирование:** Логика маппинга скоростей и методы изменения скорости покрываются юнит-тестами.

---

## 3. Журнал решений (Decision Log)

| # | Решение | Выбранный вариант | Альтернативы | Обоснование |
|---|---------|-------------------|--------------|-------------|
| **D1** | Область покрытия плееров | Во всех основных плеерах приложения (X и R) | Только плеер X или только плеер R | Обеспечивает целостный и предсказуемый пользовательский опыт во всём приложении. |
| **D2** | Сохранение скорости | Сброс в `1.0x` для каждого нового видео | Глобальное сохранение в `Settings` или сессионное сохранение в памяти | Пользовательское требование: стандартное поведение медиаплееров без искажения темпа последующих роликов. |
| **D3** | Формат интерфейса выбора скорости | Всплывающее меню `DropdownMenu` над кнопкой | Нижняя шторка `ModalBottomSheet`, центральный диалог `AlertDialog` | Максимальная скорость выбора в 1 клик, не перекрывает весь кадр видео. |
| **D4** | Архитектура UI-компонента | Общий переиспользуемый компонент `PlaybackSpeedMenu` в модуле `:core` | Независимая верстка меню в `:feature-x` и `:feature-r` | DRY, отсутствие дублирования логики Compose меню, единый стиль индикации. |

---

## 4. Архитектура и финальный дизайн

### 4.1. Модуль `:core`

#### Расширение перечисления `PlayerSpeed`
Файл: `core/src/main/java/com/client/xvideos/common/videoplayer/model/enumPlayerSpeed.kt`

```kotlin
package com.client.xvideos.common.videoplayer.model

enum class PlayerSpeed(val speed: Float, val displayName: String) {
    X0_25(0.25f, "0.25x"),
    X0_5(0.5f, "0.5x"),
    X0_75(0.75f, "0.75x"),
    X1(1.0f, "1.0x"),
    X1_25(1.25f, "1.25x"),
    X1_5(1.5f, "1.5x"),
    X2(2.0f, "2.0x");

    companion object {
        val DEFAULT = X1
    }
}
```

#### Обновление `MediaPlayerHost`
Файл: `core/src/main/java/com/client/xvideos/common/videoplayer/host/MediaPlayerHost.kt`

```kotlin
fun setSpeed(speed: PlayerSpeed) {
    this.speed = speed
}
```

#### Обновление `CMPlayer2`
Файл: `core/src/main/java/com/client/xvideos/common/videoplayer/util/CMPlayer2.kt`

```kotlin
private fun PlayerSpeed.toFloat(): Float = this.speed
```

#### Компонент `PlaybackSpeedMenu`
Файл: `core/src/main/java/com/client/xvideos/common/videoplayer/ui/component/PlaybackSpeedMenu.kt`

Компонент предоставляет настраиваемый триггер (кнопку вызова) и открывает всплывающее меню `DropdownMenu` с 7 скоростями, радио-галочкой на активной скорости и тёмным оформлением в стиле плеера.

---

### 4.2. Модуль `:feature-x`

#### Интеграция в `X_PlayerBottomBar`
Файл: `feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/atom/X_PlayerBottomBar.kt`

В строку элементов нижней панели управления (между отображением общего времени и кнопкой `Fit/Fill`) встраивается `PlaybackSpeedMenu(currentSpeed = host.speed, onSpeedSelected = { host.setSpeed(it) })`. Кнопка триггера выполнена в едином стиле с кнопкой `Fit / Fill` (компактный скруглённый бейдж с жирным текстом 11.sp).

---

### 4.3. Модуль `:feature-r`

#### Интеграция в `FeedControls_Container_Line0` и `ScreenRedFullScreenSM`
Файлы:
* `feature-r/src/main/java/com/client/xvideos/r/ui/fullscreen/ScreenRedFullScreenSM.kt`
* `feature-r/src/main/java/com/client/xvideos/r/ui/fullscreen/bottom_bar/FeedControls_Container_Line0.kt`
* `feature-r/src/main/java/com/client/xvideos/r/ui/fullscreen/ScreenRedFullScreen.kt`
* `feature-r/src/main/java/com/client/xvideos/r/ui/video/RedPooledVideoPlayer.kt`

1. В `ScreenRedFullScreenSM` добавляется `var speed by mutableStateOf(PlayerSpeed.DEFAULT)` с методами `setSpeed` и `resetSpeed`.
2. В `FeedControls_Container_Line0` добавляется кнопка вызова меню скорости в общем горизонтальном ряду кнопок.
3. В `ScreenRedFullScreen` при листании `VerticalPager` на новую страницу вызывается `vm.resetSpeed()`.
4. В `RedPooledVideoPlayer` добавляется эффект:
```kotlin
LaunchedEffect(player, speed) {
    player?.setPlaybackSpeed(speed.speed)
}
```

---

## 5. План верификации

1. **Юнит-тесты `:core`:**
   * Проверка точности числовых значений `speed` и меток `displayName` для всех 7 элементов `PlayerSpeed`;
   * Тестирование метода `MediaPlayerHost.setSpeed`.
2. **Юнит-тесты `:feature-r`:**
   * Тестирование изменения и сброса скорости в `ScreenRedFullScreenSM`.
3. **Регрессионная верификация:**
   * Запуск полного набора юнит-тестов `./gradlew testDebugUnitTest`.
