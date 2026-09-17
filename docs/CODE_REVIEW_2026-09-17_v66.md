# Код-ревью xvideos — проход 87

> **Срез:** `94e4c62` + рабочее дерево · **Статус:** закрыт полностью · **Индекс:** [все документы](README.md)

База прохода `94e4c62`. Затронуты модули `:core`, `:feature-x`, `:app`, удалён мёртвый legacy-код и добавлены модульные тесты.

Линзы:
- `C` / Безопасность при работе со `SharedPreferences` и исключение NPE при значении null в `SettingElementString` ([SettingElementString.kt](../core/src/main/java/com/client/xvideos/common/settings/element/SettingElementString.kt)).
- `C` / Полноэкранный режим видеоплеера X: скрытие статус-бара наряду с навигационным баром и включение `BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE` ([ScreenX_VideoPlayer.kt](../feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/ScreenX_VideoPlayer.kt)).
- `UI` / Устранение повторной сортировки списка тегов на каждой рекомпозиции плеера X ([ComposeTags.kt](../feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/atom/ComposeTags.kt), [parserItemVideoTags.kt](../feature-x/src/main/java/com/client/xvideos/x/parcer/parserItemVideoTags.kt)).
- `UI` / Замена Material 2 `IconButton` на Material 3 компонент в главном меню ([MenuScreen.kt](../app/src/main/java/com/client/xvideos/screenRoot/MenuScreen.kt)).
- `UI` / Исправление некорректных иконок разделов настроек (RedGifs логотип заменён на `key_24` в приватности и пин-коде, Luscious заменён на `hard_disk_24` в кэше картинок) и унификация разделителя `SettingsDivider2` ([AppSettingsScreen.kt](../app/src/main/java/com/client/xvideos/screenSettings/AppSettingsScreen.kt), [AppLockSection.kt](../app/src/main/java/com/client/xvideos/screenSettings/components/AppLockSection.kt), [CacheSettingsSection.kt](../app/src/main/java/com/client/xvideos/screenSettings/section/CacheSettingsSection.kt)).
- `C` / Искоренение оператора `!!` в пользу безопасных локальных привязок (`DownloadTask.kt`, `CalculatorState.kt`, `XlrChunkedCrypto.kt`).
- `A` / Удаление заброшенных компонентов `ConfigText*`, не используемых после перехода настроек на Material 3 ([ConfigText.kt](../app/src/main/java/com/client/xvideos/screenSettings/ConfigText.kt)).
- `T` / Тестовое покрытие строковых настроек и парсера тегов ([SettingElementStringTest.kt](../core/src/test/java/com/client/xvideos/common/settings/SettingElementStringTest.kt), [ParserItemVideoTagsTest.kt](../feature-x/src/test/java/com/client/xvideos/x/parcer/ParserItemVideoTagsTest.kt)).

Граница P2P (`core/.../p2p/`) заморожена по решению владельца от 11.09.2026 и не затрагивалась.

---

## Находки

### C138 — Потенциальный NPE при чтении null из SharedPreferences в SettingElementString. Средняя.

[core/src/main/java/com/client/xvideos/common/settings/element/SettingElementString.kt:10,19](../core/src/main/java/com/client/xvideos/common/settings/element/SettingElementString.kt#L10)

`sharedPrefs.getString(name, default)!!` выбрасывал `NullPointerException`, если в настройках по ключу сохранялся `null` (или если дефолтное значение передано как пустая строка, но реализация хранилища вернула `null`). Это роняло инициализацию приложения или экрана настроек.

**Исправление:**
- Вызов `!!` заменён на элвис-оператор с гарантированным дефолтом `sharedPrefs.getString(name, default) ?: default`.
- В `OnSharedPreferenceChangeListener` также добавлена безопасная подстановка `?: default`.

---

### C139 — Неполное сокрытие системных панелей и отсутствие transient-жестов в плеере X. Средняя.

[feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/ScreenX_VideoPlayer.kt:97-120](../feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/ScreenX_VideoPlayer.kt#L97)

В `OrientationAndSystemBarsEffect`:
1. При переходе в `isFullScreen` скрывался только `WindowInsetsCompat.Type.navigationBars()`, оставляя статус-бар видимым на фоне широкоформатного видео.
2. Не настраивалось поведение `systemBarsBehavior = BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE`, из-за чего любой свайп или касание краёв экрана приводило к постоянному показу системных панелей и скачкообразному изменению размеров поверхности плеера.
3. При возврате в портретный режим скрывался `statusBars()` вместо его восстановления.

**Исправление:**
- В полноэкранном режиме скрываются `WindowInsetsCompat.Type.systemBars()` (статус-бар и навигация одновременно).
- Установлен режим `WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE` для временного появления панелей по жесту с автоскрытием.
- При выходе из полноэкранного режима и при `onDispose` восстанавливаются все системные панели `controller.show(WindowInsetsCompat.Type.systemBars())`.

---

### UI111 — Повторная сортировка коллекции тегов на каждой рекомпозиции в ComposeTags. Низкая.

[feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/atom/ComposeTags.kt:45](../feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/atom/ComposeTags.kt#L45)

В `ComposeTags` вызов `tags.tags.sorted()` выполнялся напрямую в теле `@Composable`-функции без кэширования через `remember`. Любая рекомпозиция плеера (обновление прогресса, перемотка, анимация контролов) приводила к повторному созданию списка и сортировке строк.

**Исправление:**
- В `ComposeTags` сортировка обёрнута в `val sortedTags = remember(tags.tags) { tags.tags.sorted() }`.
- В `parserItemVideoTags` добавлена сортировка `.sorted()` на этапе разбора HTML-разметки для детерминированного порядка элементов.

---

### UI112 — Смешение Material 2 и Material 3 компонентов в MenuScreen. Низкая.

[app/src/main/java/com/client/xvideos/screenRoot/MenuScreen.kt:20](../app/src/main/java/com/client/xvideos/screenRoot/MenuScreen.kt#L20)

`MenuScreen` импортировал устаревший `androidx.compose.material.IconButton` из Material 2, тогда как контейнер экрана построен на `androidx.compose.material3.Scaffold` и использует Material 3 тему.

**Исправление:**
- Импорт переведён на `androidx.compose.material3.IconButton`, обеспечивая единую систему стилей и корректные ripple-эффекты Material You.

---

### UI113 — Несоответствие иконок в настройках приложения. Низкая.

[app/src/main/java/com/client/xvideos/screenSettings/AppSettingsScreen.kt:421](../app/src/main/java/com/client/xvideos/screenSettings/AppSettingsScreen.kt#L421)
[app/src/main/java/com/client/xvideos/screenSettings/components/AppLockSection.kt:101](../app/src/main/java/com/client/xvideos/screenSettings/components/AppLockSection.kt#L101)
[app/src/main/java/com/client/xvideos/screenSettings/section/CacheSettingsSection.kt:79,86](../app/src/main/java/com/client/xvideos/screenSettings/section/CacheSettingsSection.kt#L79)

1. Для раздела «Приватность» в списке настроек и строки «Код доступа» использовалась иконка `R.drawable.icon_red` (логотип RedGifs) вместо символа ключа/замка.
2. В строках очистки общего дискового кэша картинок использовалась иконка `R.drawable.icon_luscious` вместо нейтральной иконки диска `R.drawable.hard_disk_24`.

**Исправление:**
- Пункты приватности и блокировки приложения переведены на `R.drawable.key_24`.
- Строки кэша изображений переведены на `R.drawable.hard_disk_24`.

---

### UI114 — Закомментированный разделитель и хардкод Spacer в AppSettingsScreen. Низкая.

[app/src/main/java/com/client/xvideos/screenSettings/AppSettingsScreen.kt:318-320](../app/src/main/java/com/client/xvideos/screenSettings/AppSettingsScreen.kt#L318)

В цикле отрисовки подразделов настроек находился закомментированный `//SettingsDivider()` с ручной вставкой `Spacer(Modifier.fillMaxWidth().height(2.dp).background(SettingsScreenBackground))`, тогда как в соседнем блоке использовался компонент `SettingsDivider2()`.

**Исправление:**
- Закомментированный код и дублирующий `Spacer` заменены на канонический `SettingsDivider2()`.

---

### C140 — Потенциальные сбои из-за использования небезопасного оператора !!. Низкая.

[core/src/main/java/com/client/xvideos/common/kdownloader/internal/DownloadTask.kt:252](../core/src/main/java/com/client/xvideos/common/kdownloader/internal/DownloadTask.kt#L252)
[app/src/main/java/com/client/xvideos/calculator/CalculatorState.kt:203-204](../app/src/main/java/com/client/xvideos/calculator/CalculatorState.kt#L203)
[core/src/main/java/com/client/xvideos/common/backup/XlrChunkedCrypto.kt:195,200,277,289,298](../core/src/main/java/com/client/xvideos/common/backup/XlrChunkedCrypto.kt#L195)

В цикле чтения `DownloadTask` использовался `inputStream!!.read(...)`, что приводило к `NullPointerException` при параллельной отмене или закрытии потока вместо корректного `IOException`. В `CalculatorState` при повторении операции использовались `lastOperator!!` и `lastOperand!!`. В `XlrChunkedCrypto` при закрытии и чтении потоков оставались операторы `!!`.

**Исправление:**
- В `DownloadTask` входящий поток привязан к локальной неизменяемой переменной `val stream = inputStream ?: ...`.
- В `CalculatorState` операнды безопасно извлекаются через `val op = lastOperator; val operand = lastOperand; if (op != null && operand != null)`.
- В `XlrChunkedCrypto` удалены все операторы `!!` с переходом на локальные неизменяемые ссылки и безопасные вызовы.

---

### A23 — Мёртвый неиспользуемый legacy-код компонентов настроек. Низкая.

[app/src/main/java/com/client/xvideos/screenSettings/ConfigText.kt](../app/src/main/java/com/client/xvideos/screenSettings/ConfigText.kt)
[app/src/main/java/com/client/xvideos/screenSettings/ConfigTextAndButtonWithDialog.kt](../app/src/main/java/com/client/xvideos/screenSettings/ConfigTextAndButtonWithDialog.kt)
[app/src/main/java/com/client/xvideos/screenSettings/ConfigTextAndCheckBox.kt](../app/src/main/java/com/client/xvideos/screenSettings/ConfigTextAndCheckBox.kt)

Файлы `ConfigText.kt`, `ConfigTextAndButtonWithDialog.kt`, `ConfigTextAndCheckBox.kt` являлись артефактами старого экрана настроек до редизайна на компоненты `SettingsListItem`/`SettingsGroup` и не имели ни одного реального использования в проекте (кроме собственных `@Preview`).

**Исправление:**
- Все 3 устаревших файла удалены из репозитория.

---

### T47 — Остаточный отладочный вывод println и несогласованность имён в SettingElement*. Низкая.

[core/src/main/java/com/client/xvideos/common/settings/element/SettingElementInt.kt:13](../core/src/main/java/com/client/xvideos/common/settings/element/SettingElementInt.kt#L13)
[core/src/main/java/com/client/xvideos/common/settings/element/SettingElementBoolean.kt:10](../core/src/main/java/com/client/xvideos/common/settings/element/SettingElementBoolean.kt#L10)

1. В `SettingElementInt.setValue` оставался забытый отладочный вывод `println("!!! setValue ${name} ${value}")`.
2. Внутренний `MutableStateFlow` в `SettingElementInt` и `SettingElementBoolean` назывался `_galleryCheckbox`, оставшись от копипаста старого чекбокса галереи.

**Исправление:**
- Вызов `println` удалён.
- Поле переименовано в `_field` для единообразия с публичным `val field: StateFlow<T>`.

---

### T48 — Отсутствие тестов на строковые настройки и парсер тегов. Низкая.

[core/src/test/java/com/client/xvideos/common/settings/SettingElementStringTest.kt](../core/src/test/java/com/client/xvideos/common/settings/SettingElementStringTest.kt)
[feature-x/src/test/java/com/client/xvideos/x/parcer/ParserItemVideoTagsTest.kt](../feature-x/src/test/java/com/client/xvideos/x/parcer/ParserItemVideoTagsTest.kt)

Классы `SettingElementString` и разбор тегов видео в `parserItemVideoTags` не были покрыты модульными тестами.

**Исправление:**
- Создан тестовый класс `SettingElementStringTest` с тестами на дефолтное значение, чтение сохранённого значения, безопасную обработку `null` из хранилища, реакцию на `setValue` и внешние обновления через `OnSharedPreferenceChangeListener`.
- Создан тестовый класс `ParserItemVideoTagsTest`, проверяющий нормализацию, дедупликацию, алфавитную сортировку тегов и парсинг моделей каналов и авторов.

---

## Статус

| Находка | Класс | Статус | Детали |
| --- | --- | --- | --- |
| C138 | корректность | закрыт | Безопасная обработка null в `SettingElementString` |
| C139 | UI / UX | закрыт | Полноэкранный плеер X: скрытие статус-бара и transient-жесты |
| UI111 | производительность | закрыт | Кэширование `remember` в `ComposeTags` и сортировка в парсере |
| UI112 | UI / архитектура | закрыт | Перевод `IconButton` в `MenuScreen` на Material 3 |
| UI113 | UI / консистентность | закрыт | Исправление иконок в настройках (`key_24`, `hard_disk_24`) |
| UI114 | чистота кода | закрыт | Использование `SettingsDivider2` вместо `Spacer` в `AppSettingsScreen` |
| C140 | стабильность | закрыт | Устранение операторов `!!` в `DownloadTask`, `CalculatorState`, `XlrChunkedCrypto` |
| A23 | чистота архитектуры | закрыт | Удаление мертвого legacy-кода `ConfigText*` |
| T47 | гигиена кода | закрыт | Удаление `println` и исправление `_galleryCheckbox` -> `_field` |
| T48 | тесты | закрыт | Тесты `SettingElementStringTest` и `ParserItemVideoTagsTest` |

---

## Проверка

```
.\gradlew.bat app:detekt core:detekt feature-l:detekt feature-r:detekt feature-x:detekt
BUILD SUCCESSFUL in 14s

.\gradlew.bat testDebugUnitTest
BUILD SUCCESSFUL in 32s (140 actionable tasks)

.\gradlew.bat compileReleaseKotlin
BUILD SUCCESSFUL in 40s (61 actionable tasks)
```
