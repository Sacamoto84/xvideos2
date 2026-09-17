# Код-ревью xvideos — проход 80

> **Срез:** `4844535` + рабочее дерево · **Статус:** закрыт полностью · **Индекс:** [все документы](README.md)

База прохода `4844535`. Изменено 6 файлов в модуле `:app`, удалено 3 файла в `:core` и `:app`.

Линзы:
- `A` / Полное удаление экрана «Диагностика» и подсистемы `CrashLog`, актуализация точек старта приложения и тестов порядка инициализации ([App.kt](../app/src/main/java/com/client/xvideos/App.kt), [AppSettingsScreen.kt](../app/src/main/java/com/client/xvideos/screenSettings/AppSettingsScreen.kt), [GlobalStateTest.kt](../app/src/test/java/com/client/xvideos/arch/GlobalStateTest.kt), [file_paths.xml](../app/src/main/res/xml/file_paths.xml)).
- `C` / Устранение неиспользуемого импорта `SettingsDivider` в `AppSettingsScreen.kt`, разблокировка статического анализатора `app:detekt` ([AppSettingsScreen.kt](../app/src/main/java/com/client/xvideos/screenSettings/AppSettingsScreen.kt)).
- `UI` / Чистка сигнатуры `SettingsDivider2` (удаление неиспользуемого параметра) и вынос диалога верификации камуфляжа из внутреннего содержимого карточки настроек ([SettingsListItems.kt](../app/src/main/java/com/client/xvideos/screenSettings/components/SettingsListItems.kt), [AppLockSection.kt](../app/src/main/java/com/client/xvideos/screenSettings/components/AppLockSection.kt)).

Граница P2P (`core/.../p2p/`) заморожена по решению владельца от 11.09.2026 и не затрагивалась.

---

## Находки

### C133 — Падение задачи app:detekt из-за неиспользуемого импорта SettingsDivider в AppSettingsScreen. Низкая.

[app/src/main/java/com/client/xvideos/screenSettings/AppSettingsScreen.kt:69](../app/src/main/java/com/client/xvideos/screenSettings/AppSettingsScreen.kt#L69)

В файле `AppSettingsScreen.kt` оставался неиспользуемый импорт `com.client.xvideos.screenSettings.components.SettingsDivider`. При запуске статического анализатора сборка прерывалась ошибкой `The import 'com.client.xvideos.screenSettings.components.SettingsDivider' is unused. [UnusedImports]`.

**Исправление:**
- Неиспользуемый импорт удалён. Проверка `app:detekt` проходит успешно.

---

### UI96 — Мёртвый параметр startIndent в SettingsDivider2. Низкая.

[app/src/main/java/com/client/xvideos/screenSettings/components/SettingsListItems.kt:101](../app/src/main/java/com/client/xvideos/screenSettings/components/SettingsListItems.kt#L101)

Компонент `SettingsDivider2` объявлял параметр `startIndent: androidx.compose.ui.unit.Dp = 56.dp`, однако в теле функции создавался `Spacer(Modifier.fillMaxWidth().height(2.dp).background(SettingsScreenBackground))`. Параметр никак не использовался, вводя разработчиков в заблуждение относительно возможности задания отступа.

**Исправление:**
- Неиспользуемый параметр удалён из сигнатуры `SettingsDivider2()`.

---

### A19 — Размещение диалога CamouflageVerificationDialog внутри карточки SettingsGroup и нарушение отступов. Низкая.

[app/src/main/java/com/client/xvideos/screenSettings/components/AppLockSection.kt:77-149](../app/src/main/java/com/client/xvideos/screenSettings/components/AppLockSection.kt#L77)

Диалог `CamouflageVerificationDialog` вызывался непосредственно внутри блока разметки карточки `SettingsGroup` между элементами списка, при этом внутри лямбды присутствовали сбитые 4-пробельные отступы. Это нарушало общее соглашение экрана настроек, где диалоги (`AppLockPasswordDialog`) вынесены в начало Composable-функции вне структуры элементов карточек.

**Исправление:**
- Вызов `CamouflageVerificationDialog` перемещён на верхний уровень рядом с `AppLockPasswordDialog`.
- Исправлено форматирование и отступы внутри блока `SettingsGroup`.

---

### A20 — Полное удаление экрана «Диагностика», подсистемы CrashLog и актуализация архитектурного сторожа. Низкая.

[app/src/main/java/com/client/xvideos/App.kt:95](../app/src/main/java/com/client/xvideos/App.kt#L95)
[app/src/main/java/com/client/xvideos/screenSettings/AppSettingsScreen.kt:400](../app/src/main/java/com/client/xvideos/screenSettings/AppSettingsScreen.kt#L400)
[app/src/test/java/com/client/xvideos/arch/GlobalStateTest.kt:55-63](../app/src/test/java/com/client/xvideos/arch/GlobalStateTest.kt#L55)
[app/src/main/res/xml/file_paths.xml:15](../app/src/main/res/xml/file_paths.xml#L15)

Функционал экрана «Диагностика» (просмотр размера, шаринг и очистка журнала ошибок) признан невостребованным.
1. Удалены файлы `DiagnosticsSettingsSection.kt`, `CrashLog.kt` и сопутствующие тесты `CrashLogTest.kt`.
2. В `App.kt` удалена посадка `ReleaseErrorTree` и вызов `CrashLog.install()`.
3. В `AppSettingsScreen.kt` страница `Diagnostics` удалена из перечисления `SettingsPage` и списка основных вкладок `primaryPages`.
4. В `GlobalStateTest.kt` обновлен проверяемый порядок инициализации `REQUIRED_ORDER` и текст утверждения, убран вызов `CrashLog.install(`.
5. В `file_paths.xml` актуализирован комментарий для пути `app_store`.

---

## Статус

| Находка | Класс | Статус | Детали |
| --- | --- | --- | --- |
| C133 | корректность | закрыт | Удалён неиспользуемый импорт `SettingsDivider`, `app:detekt` успешен |
| UI96 | UI / Compose | закрыт | Удалён мёртвый параметр `startIndent` из `SettingsDivider2` |
| A19 | архитектура | закрыт | Диалог вынесен из карточки, выровнены отступы в `AppLockSection` |
| A20 | архитектура | закрыт | Полная зачистка `CrashLog`, экрана «Диагностика» и сторожей инициализации |

## Проверка

```
.\gradlew.bat app:detekt core:detekt feature-l:detekt feature-r:detekt feature-x:detekt
BUILD SUCCESSFUL

.\gradlew.bat testDebugUnitTest
BUILD SUCCESSFUL in 14s (141 actionable tasks)

.\gradlew.bat compileReleaseKotlin
BUILD SUCCESSFUL in 9s
```
