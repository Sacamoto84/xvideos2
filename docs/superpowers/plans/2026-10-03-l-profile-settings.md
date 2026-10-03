# Профиль L в настройках: вход и выход — план реализации

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Строка «Профиль L» в настройках предлагает «Войти» или «Выйти» по состоянию; «Войти» открывает форму входа прямо из настроек; «Выйти» завершает сессию без перезапуска приложения.

**Architecture:** Источник истины — сохранённые логин и пароль. Репозиторий L на каждом запросе сверяется с ними и сам закрывает сессию, когда профиль стёрт; настройки только стирают профиль. Форма входа становится молекулой `LLoginForm`: её показывают раздел L (как раньше) и новый Voyager-экран `L_ScreenLogin` (из настроек).

**Tech Stack:** Kotlin, Jetpack Compose (material3), Voyager, Ktor client + `MockEngine` в тестах, JUnit4, detekt.

Спецификация: [2026-10-03-l-profile-settings-design.md](../specs/2026-10-03-l-profile-settings-design.md)

Общие правила для исполнителя:

- Названия сайтов не пишутся ни в коммитах, ни в логах, ни в документации — только L.
- Коммиты в плане не расписаны: владелец коммитит по своей команде, сообщения — на русском. Предлагаемая разбивка — в конце плана. Перед коммитом — Задача 6.
- Команды Gradle выполняются из корня репозитория. В Git Bash — `./gradlew.bat …`, в PowerShell — `.\gradlew.bat …`.
- Молекулы не принимают ScreenModel; у каждой молекулы есть `@Preview`; имя файла совпадает с именем Composable-функции.

---

## Файлы

| Файл | Действие | Ответственность |
|---|---|---|
| `feature-l/src/main/java/com/client/xvideos/l/repository/Repository.kt` | изменить | `dropSession()` вместо `logout()`; `ensureAuthenticated()` закрывает сессию при пустом профиле |
| `feature-l/src/test/java/com/client/xvideos/l/repository/RepositoryNetworkFailureTest.kt` | изменить | три теста выхода |
| `feature-l/src/main/java/com/client/xvideos/l/ui/screens/molecule/LLoginForm.kt` | создать переносом | форма входа; «Пропустить» — по желанию вызывающего |
| `feature-l/src/main/java/com/client/xvideos/l/ui/screens/L_ScreenLogin.kt` | перенести, затем создать заново | Voyager-экран входа для настроек |
| `feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/L_ScreenExplorer.kt` | изменить | вызывает `LLoginForm` |
| `config/detekt/baseline.xml` | изменить | убрать запись про старую сигнатуру формы |
| `app/src/main/java/com/client/xvideos/screenSettings/molecule/LProfileRow.kt` | создать | строка профиля: статус + кнопка по состоянию + диалог выхода |
| `app/src/main/java/com/client/xvideos/screenSettings/model/SettingsDetailParams.kt` | изменить | поле `onOpenLLogin` |
| `app/src/main/java/com/client/xvideos/screenSettings/molecule/SettingsDetailPage.kt` | изменить | признак «аккаунт сохранён», колбэк входа |
| `app/src/main/java/com/client/xvideos/screenSettings/section/LSettingsSection.kt` | изменить | строка профиля через `LProfileRow` |
| `app/src/main/java/com/client/xvideos/screenSettings/AppSettingsScreen.kt` | изменить | переход на `L_ScreenLogin` |

---

### Задача 1: Сброс сессии при пустом профиле (`feature-l`)

**Files:**
- Modify: `feature-l/src/main/java/com/client/xvideos/l/repository/Repository.kt`
- Test: `feature-l/src/test/java/com/client/xvideos/l/repository/RepositoryNetworkFailureTest.kt`

Что чинится. Настройки по «Выйти» стирают только сохранённые логин и пароль. `ensureAuthenticated()` пустой профиль пропускает, клиент остаётся вошедшим, и запросы до перезапуска процесса идут от прежнего пользователя. `logout()`, который закрывает сессию, в боевом коде не вызывается.

Как отличить режим в тесте: вошедший клиент шлёт POST, анонимный — GET.

- [ ] **Шаг 1: написать падающие тесты**

В `RepositoryNetworkFailureTest.kt` добавить импорт между `io.ktor.http.HttpHeaders` и `io.ktor.http.HttpStatusCode`:

```kotlin
import io.ktor.http.HttpMethod
```

Перед комментарием `// --- Кэш в памяти ---` вставить раздел:

```kotlin
    // --- Выход ---

    @Test
    fun `после очистки логина и пароля запросы идут анонимно`() = runBlocking {
        val server = FakeServer()
        var credentials = CREDENTIALS
        val repository = repository(server) { credentials }

        repository.openURI(query("A"))
        credentials = UserProfile()
        repository.openURI(query("B"))

        assertEquals("выход не должен обращаться ко входу", 1, server.loginCalls.get())
        assertEquals("до выхода запрос идёт от вошедшего", HttpMethod.Post, server.apiCalls.first().method)
        assertEquals("после выхода запрос идёт анонимно", HttpMethod.Get, server.apiCalls.last().method)
    }

    @Test
    fun `после выхода и ввода тех же данных вход пробуют снова`() = runBlocking {
        val server = FakeServer().apply {
            login = { respond(WRONG_CREDENTIALS_PAGE, HttpStatusCode.OK, HTML) }
        }
        var credentials = CREDENTIALS
        val repository = repository(server) { credentials }

        repository.openURI(query("A"))
        credentials = UserProfile()
        repository.openURI(query("B"))
        credentials = CREDENTIALS
        repository.openURI(query("C"))

        assertEquals(2, server.loginCalls.get())
    }
```

Существующий тест `после выхода ответ вошедшего пользователя запрашивается заново` заменить целиком — из него уходит вызов `repository.logout()`, выход теперь означает только очистку профиля:

```kotlin
    @Test
    fun `после выхода ответ вошедшего пользователя запрашивается заново`() = runBlocking {
        val server = FakeServer()
        var credentials = CREDENTIALS
        val repository = repository(server) { credentials }

        repository.openURI(query("A"), RepositoryUriConfig.CACHE_RAM)
        credentials = UserProfile()
        repository.openURI(query("A"), RepositoryUriConfig.CACHE_RAM)

        assertEquals(2, server.apiCalls.size)
    }
```

- [ ] **Шаг 2: запустить тесты и убедиться, что они падают**

Run: `./gradlew.bat :feature-l:testDebugUnitTest --tests "com.client.xvideos.l.repository.RepositoryNetworkFailureTest"`

Expected: `BUILD FAILED`, падают ровно три теста:

- `после очистки логина и пароля запросы идут анонимно` — «после выхода запрос идёт анонимно»: ожидался GET, пришёл POST;
- `после выхода и ввода тех же данных вход пробуют снова` — ожидалось 2, получено 1;
- `после выхода ответ вошедшего пользователя запрашивается заново` — ожидалось 2, получено 1.

Остальные тесты класса зелёные. Если упало что-то ещё или не упал один из трёх — остановиться и разобраться, код не трогать.

- [ ] **Шаг 3: заменить `logout()` на `dropSession()`**

В `Repository.kt` удалить метод `logout()` целиком вместе с его KDoc. На его место вставить:

```kotlin
    /**
     * Сохранённого профиля нет: забывает неудачный вход и, если клиент ещё
     * вошедший, закрывает его сессию. Вызывать под [authMutex].
     *
     * Так работает выход: настройки только стирают логин и пароль, а следующий
     * запрос приходит сюда. Раньше сессию закрывал отдельный `logout()`, но
     * настройки его не вызывали — после «Выйти» запросы до перезапуска шли от
     * имени прежнего пользователя, и RAM-кэш отдавал его ответы.
     *
     * Память о неудачном входе сбрасывается всегда: иначе «выйти и ввести те
     * же логин и пароль» после отказа сервера не давало новой попытки.
     *
     * Клиент меняется под [requestMutex]: без него close() старого клиента
     * приходился на середину выполняющегося запроса.
     */
    private suspend fun dropSession() {
        anonymousFallbackFor = null
        loginRetryAtMs = Long.MAX_VALUE
        if (!handler.loggedIn) return

        requestMutex.withLock {
            val oldHandler = handler
            handler = createHandler()
            oldHandler.close()
            clearRamCache()
        }
        clearHtmlChallengeUiState()
    }
```

Порядок мьютексов `authMutex` → `requestMutex` тот же, что был в `logout()`; обратного порядка в классе нет (`requestMutex` берётся ещё только в `postJsonThrottled`, вне `authMutex`).

- [ ] **Шаг 4: вызвать `dropSession()` из `ensureAuthenticated()`**

В `Repository.kt` заменить функцию `ensureAuthenticated()` целиком вместе с её KDoc:

```kotlin
    /**
     * Приводит сессию в соответствие с сохранёнными логином и паролем: входит,
     * если они заданы, и закрывает сессию, если их стёрли ([dropSession]).
     *
     * Неудачный вход не закрывает раздел: запросы идут анонимно, как после
     * «Пропустить», а пользователь получает одно предупреждение. Снова вход
     * пробуют после смены логина или пароля либо, если отказ был временным,
     * когда наступит [loginRetryAtMs]. Раньше отказ входа возвращался ошибкой
     * из каждого openURI и вход повторялся на каждом запросе — при сломанной
     * авторизации на сервере L не открывался вовсе, хотя анонимные запросы
     * проходили.
     */
    private suspend fun ensureAuthenticated(): Result<Unit> {
        return try {
            authMutex.withLock {
                val profile = credentials()
                val loginAllowed = profile != anonymousFallbackFor || nowMs() >= loginRetryAtMs
                // Без логина или пароля работаем анонимно (сервер отдаёт меньше
                // альбомов), с ними — входим.
                when {
                    !profile.isValid -> dropSession()
                    loginAllowed -> logInIfNeeded(profile)
                }
                SUCCESS_UNIT
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "openURI() login error")
            Result.failure(e)
        }
    }
```

Импорты не меняются: `Settings` по-прежнему нужен функции `savedCredentials()` в конце файла.

- [ ] **Шаг 5: запустить тесты класса и убедиться, что они проходят**

Run: `./gradlew.bat :feature-l:testDebugUnitTest --tests "com.client.xvideos.l.repository.RepositoryNetworkFailureTest"`

Expected: `BUILD SUCCESSFUL`, все тесты класса зелёные.

- [ ] **Шаг 6: убедиться, что `logout()` нигде не остался, и прогнать модуль**

Run: `git grep -n "logout(" -- "*.kt"`

Expected: пусто.

Run: `./gradlew.bat :feature-l:testDebugUnitTest`

Expected: `BUILD SUCCESSFUL`.

---

### Задача 2: Форма входа — молекула `LLoginForm` (`feature-l`)

**Files:**
- Move: `feature-l/src/main/java/com/client/xvideos/l/ui/screens/L_ScreenLogin.kt` → `feature-l/src/main/java/com/client/xvideos/l/ui/screens/molecule/LLoginForm.kt`
- Modify: `feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/L_ScreenExplorer.kt`
- Modify: `config/detekt/baseline.xml`

Перенос дословный. Меняется только то, что перечислено в шаге 2; тело формы не трогать. Экран `L_ScreenLogin` в этой задаче не создаётся — он в Задаче 3, чтобы перенос остался в истории переименованием.

- [ ] **Шаг 1: перенести файл**

```bash
mkdir -p feature-l/src/main/java/com/client/xvideos/l/ui/screens/molecule
git mv feature-l/src/main/java/com/client/xvideos/l/ui/screens/L_ScreenLogin.kt feature-l/src/main/java/com/client/xvideos/l/ui/screens/molecule/LLoginForm.kt
```

- [ ] **Шаг 2: пять правок в `LLoginForm.kt`**

1. Первая строка — пакет:

```kotlin
package com.client.xvideos.l.ui.screens.molecule
```

2. Объявление функции. Было `fun LLoginContent(...)` с параметром `onSkip: () -> Unit` и пустой строкой перед закрывающей скобкой. Стало:

```kotlin
/**
 * Форма входа в профиль L: логин, пароль, «Сохранить», «Назад».
 *
 * Показывается в двух местах: раздел L рисует её сам, пока профиль не задан, а
 * настройки открывают отдельным экраном `L_ScreenLogin`.
 *
 * @param onSkip «Пропустить» — работать без авторизации. `null` прячет кнопку:
 * из настроек пользователь пришёл именно входить.
 */
@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LLoginForm(
    initialLogin: String = "",
    initialPassword: String = "",
    onSaved: () -> Unit,
    onBack: () -> Unit,
    onSkip: (() -> Unit)?
) {
```

3. Строка лога в `onOpenWebsite` (вызов `Timber.w(e, …)`): в ней стоит старое имя файла и название сайта. Стало:

```kotlin
                Timber.w(e, "LLoginForm: не удалось открыть ссылку L")
```

4. Кнопка «Пропустить» в конце `Column` — обернуть в проверку:

```kotlin
        if (onSkip != null) {
            TextButton(
                onClick = onSkip,
                modifier = Modifier.padding(top = 24.dp).fillMaxWidth().height(64.dp),
                shape = buttonShape
            ) {
                Text(
                    text = "Пропустить",
                    fontSize = 22.sp,
                    fontFamily = Theme.L.fontFamilyKarla,
                    color = Theme.L.b0,
                )
            }
        }
```

5. Превью `LLoginContentPreview` заменить на два:

```kotlin
@Preview(showBackground = false)
@Composable
private fun LLoginFormPreview() {
    LLoginForm(
        onSaved = {},
        onBack = {},
        onSkip = {}
    )
}

@Preview(showBackground = false)
@Composable
private fun LLoginFormWithoutSkipPreview() {
    LLoginForm(
        onSaved = {},
        onBack = {},
        onSkip = null
    )
}
```

- [ ] **Шаг 3: перевести раздел L на `LLoginForm`**

В `L_ScreenExplorer.kt` удалить импорт `com.client.xvideos.l.ui.screens.LLoginContent` и добавить после импорта `…explorer.tab.saved.L_SavedTab`:

```kotlin
import com.client.xvideos.l.ui.screens.molecule.LLoginForm
```

Вызов формы — меняется только имя, аргументы прежние:

```kotlin
            LLoginForm(
                initialLogin = savedLogin,
                initialPassword = savedPassword,
                onSaved = onSavedLogin,
                onBack = onPop,
                onSkip = onSkipLogin
            )
```

- [ ] **Шаг 4: убрать устаревшую запись из baseline**

В `config/detekt/baseline.xml` удалить строку, начинающуюся с `<ID>LongMethod:L_ScreenLogin.kt$` (строка 61). Она привязана к старой сигнатуре и больше ничему не соответствует; `@Suppress("LongMethod")` на `LLoginForm` остаётся.

- [ ] **Шаг 5: проверить, что перенос дословный**

Команда для Git Bash (подстановка `<(…)` в PowerShell не работает):

```bash
diff --strip-trailing-cr <(git show HEAD:feature-l/src/main/java/com/client/xvideos/l/ui/screens/L_ScreenLogin.kt) feature-l/src/main/java/com/client/xvideos/l/ui/screens/molecule/LLoginForm.kt
```

Expected: отличия только в пяти местах из шага 2 — пакет; KDoc, имя и параметр `onSkip` функции; строка лога; обёртка `if (onSkip != null)` со сдвинутой кнопкой; превью. Любое другое отличие — ошибка переноса, исправить.

Run: `git grep -n "LLoginContent" -- "*.kt"`

Expected: пусто.

- [ ] **Шаг 6: собрать модуль**

Run: `./gradlew.bat :feature-l:compileDebugKotlin`

Expected: `BUILD SUCCESSFUL`.

---

### Задача 3: Экран `L_ScreenLogin` (`feature-l`)

**Files:**
- Create: `feature-l/src/main/java/com/client/xvideos/l/ui/screens/L_ScreenLogin.kt`

- [ ] **Шаг 1: создать экран**

```kotlin
package com.client.xvideos.l.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.l.ui.screens.molecule.LLoginForm

/**
 * Вход в профиль L отдельным экраном — его открывают настройки.
 *
 * Раздел L показывает ту же форму сам, внутри `L_ScreenExplorer`, и там у неё
 * есть «Пропустить». Здесь пропускать нечего: пользователь пришёл входить.
 */
class L_ScreenLogin : Screen {

    override val key: ScreenKey = "L_ScreenLogin"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        // Читаем один раз: «Сохранить» меняет настройки, и подписка на них
        // пересоздала бы поля формы за мгновение до закрытия экрана.
        val initialLogin = remember { Settings.l_login.field.value }
        val initialPassword = remember { Settings.l_pass.field.value }
        val onClose: () -> Unit = remember(navigator) { { navigator.pop() } }

        LLoginForm(
            initialLogin = initialLogin,
            initialPassword = initialPassword,
            onSaved = onClose,
            onBack = onClose,
            onSkip = null
        )
    }
}
```

- [ ] **Шаг 2: собрать модуль и прогнать архитектурные тесты**

Run: `./gradlew.bat :feature-l:compileDebugKotlin :app:testDebugUnitTest --tests "com.client.xvideos.arch.*"`

Expected: `BUILD SUCCESSFUL`. `ScreenSerializationTest` видит новый экран (полей в конструкторе нет), `GlobalStateTest` и `LayerBoundariesTest` не меняются.

---

### Задача 4: Строка профиля `LProfileRow` (`app`)

**Files:**
- Create: `app/src/main/java/com/client/xvideos/screenSettings/molecule/LProfileRow.kt`

Compose-UI-тестов в проекте нет: строка проверяется превью и на устройстве (Задача 6).

- [ ] **Шаг 1: создать молекулу**

```kotlin
package com.client.xvideos.screenSettings.molecule

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.screenSettings.DialogButton
import com.client.xvideos.screenSettings.SettingsPage
import com.client.xvideos.screenSettings.components.SettingsAccentColor
import com.client.xvideos.screenSettings.components.SettingsGroup
import com.client.xvideos.screenSettings.components.SettingsListItem
import com.client.xvideos.screenSettings.components.SettingsPreview

/**
 * Строка профиля L в настройках: показывает, сохранён ли аккаунт, и предлагает
 * действие, которое этому состоянию соответствует.
 *
 * Без аккаунта «Войти» сразу зовёт [onLogin]. С аккаунтом «Выйти» сначала
 * спрашивает подтверждение. Раньше кнопка в обоих случаях открывала диалог
 * выхода, и без аккаунта он предлагал выйти из профиля, которого нет.
 *
 * @param login сохранённый логин; показывается, когда [hasAccount].
 * @param hasAccount сохранены и логин, и пароль.
 */
@Composable
internal fun LProfileRow(
    login: String,
    hasAccount: Boolean,
    onLogin: () -> Unit,
    onLogout: () -> Unit,
) {
    var logoutDialogVisible by remember { mutableStateOf(false) }
    val onDismissLogout = remember { { logoutDialogVisible = false } }
    val onAskLogout = remember { { logoutDialogVisible = true } }

    DialogButton(
        visible = logoutDialogVisible,
        title = "Выйти из профиля L?",
        body = "Логин и пароль $login будут удалены с устройства. L продолжит работать без авторизации.",
        buttonText = "Выйти",
        onDismiss = onDismissLogout,
        onBlockConfirmed = onLogout
    )

    val buttonText = if (hasAccount) "Выйти" else "Войти"
    val onButtonClick = if (hasAccount) onAskLogout else onLogin
    val trailing: @Composable () -> Unit = remember(buttonText, onButtonClick) {
        {
            TextButton(onClick = onButtonClick) {
                Text(buttonText, color = SettingsAccentColor, fontWeight = FontWeight.Medium)
            }
        }
    }

    SettingsListItem(
        icon = SettingsPage.L.icon,
        text = "Профиль L",
        subtitle = if (hasAccount) login else "Вход не выполнен",
        trailing = trailing
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF1B1B1F)
@Composable
private fun LProfileRowPreview() = SettingsPreview {
    SettingsGroup {
        LProfileRow(login = "", hasAccount = false, onLogin = {}, onLogout = {})
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1B1B1F)
@Composable
private fun LProfileRowWithAccountPreview() = SettingsPreview {
    SettingsGroup {
        LProfileRow(login = "preview_user", hasAccount = true, onLogin = {}, onLogout = {})
    }
}
```

Пояснения к коду:

- `DialogButton` после подтверждения сам вызывает `onDismiss`, закрывать диалог в `onLogout` не нужно.
- Значок берётся из `SettingsPage.L.icon` — тот же, что у пункта «L» на главной странице настроек.

- [ ] **Шаг 2: собрать модуль**

Run: `./gradlew.bat :app:compileDebugKotlin`

Expected: `BUILD SUCCESSFUL`.

---

### Задача 5: Проводка в настройках (`app`)

**Files:**
- Modify: `app/src/main/java/com/client/xvideos/screenSettings/model/SettingsDetailParams.kt`
- Modify: `app/src/main/java/com/client/xvideos/screenSettings/molecule/SettingsDetailPage.kt`
- Modify: `app/src/main/java/com/client/xvideos/screenSettings/section/LSettingsSection.kt`
- Modify: `app/src/main/java/com/client/xvideos/screenSettings/AppSettingsScreen.kt`

- [ ] **Шаг 1: поле в `SettingsDetailParams`**

Последнее поле конструктора — было `val onBackupDataChanged: () -> Unit`, стало:

```kotlin
    val onBackupDataChanged: () -> Unit,
    val onOpenLLogin: () -> Unit = {}
```

Значение по умолчанию оставляет без изменений три превью, которые собирают параметры сами.

- [ ] **Шаг 2: признак аккаунта в `SettingsDetailPage`**

Строку `val lLogin by Settings.l_login.field.collectAsStateWithLifecycle()` дополнить:

```kotlin
    val lLogin by Settings.l_login.field.collectAsStateWithLifecycle()
    val lPass by Settings.l_pass.field.collectAsStateWithLifecycle()
    // То же правило, что на входе в раздел L: аккаунт есть, когда заданы и логин, и пароль.
    val hasLAccount = lLogin.isNotBlank() && lPass.isNotBlank()
```

Ветку `SettingsPage.L` заменить:

```kotlin
        SettingsPage.L -> LSettingsSection(
            lLogin = lLogin,
            hasLAccount = hasLAccount,
            onLogin = params.onOpenLLogin,
            modifier = modifier
        )
```

Пароль ниже этой функции не передаётся — только признак.

- [ ] **Шаг 3: `LSettingsSection` на `LProfileRow`**

Импорты: удалить `com.client.xvideos.screenSettings.components.SettingsButtonRowWithDialog` (иначе detekt `UnusedImports`), добавить:

```kotlin
import com.client.xvideos.screenSettings.molecule.LProfileRow
```

Сигнатуру и начало тела — от `@Composable` до объявления `onLogoutL` включительно — заменить. Уходят `isLoginBlank`, `loginValueText`, `logoutDialogBody`:

```kotlin
@Composable
internal fun LSettingsSection(
    lLogin: String,
    hasLAccount: Boolean,
    onLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val thumbnailSize by Settings.thumbalistSize.field.collectAsStateWithLifecycle()
    val currentDisplayName = remember(thumbnailSize) {
        ThumbnailsSize.fromValue(thumbnailSize)?.displayName ?: "?"
    }

    // Стираем только сохранённый профиль: сессию закрывает репозиторий L, когда
    // на следующем запросе увидит, что профиля больше нет.
    val onLogoutL = remember {
        {
            Settings.l_login.setValue("")
            Settings.l_pass.setValue("")
            SnackBar.success("Вы вышли из профиля L")
        }
    }
```

Первый элемент `SettingsGroup` — вызов `SettingsButtonRowWithDialog(...)` целиком — заменить:

```kotlin
        LProfileRow(
            login = lLogin,
            hasAccount = hasLAccount,
            onLogin = onLogin,
            onLogout = onLogoutL
        )
```

Остальное содержимое группы (разделители, размер миниатюры, колонки) не трогать.

Превью в конце файла:

```kotlin
@Preview(showBackground = true, backgroundColor = 0xFF1B1B1F)
@Composable
private fun LSettingsSectionPreview() = SettingsPreview {
    LSettingsSection(lLogin = "preview_user", hasLAccount = true, onLogin = {}, modifier = Modifier)
}
```

- [ ] **Шаг 4: переход на экран входа в `AppSettingsScreen`**

Импорт — после `com.client.xvideos.common.util.getTopInsetDp`:

```kotlin
import com.client.xvideos.l.ui.screens.L_ScreenLogin
```

В `Content()` после объявления `onBack`:

```kotlin
        val onOpenLLogin: () -> Unit = remember(navigator) { { navigator.push(L_ScreenLogin()) } }
```

В вызове `AppSettingsScreenContent(...)` после `onRefreshFileStats = refreshFileStats` добавить аргумент:

```kotlin
            onRefreshFileStats = refreshFileStats,
            onOpenLLogin = onOpenLLogin
```

В сигнатуре `AppSettingsScreenContent` после `modifier` добавить параметр:

```kotlin
    modifier: Modifier = Modifier,
    onOpenLLogin: () -> Unit = {}
```

Значение по умолчанию обязательно: у функции одиннадцать параметров без значений по умолчанию, порог detekt `LongParameterList` — двенадцать, параметры со значением по умолчанию не считаются. Без него detekt упадёт.

Блок `detailParams` — добавить ключ `remember` и аргумент конструктора:

```kotlin
    val detailParams = remember(
        currentPage,
        imageCacheSizeBytes,
        storageStats,
        sizeRedTotal,
        sizeRedDownload,
        onClearImageCache,
        onClearDownload,
        data,
        context,
        onBackupDataChanged,
        onOpenLLogin
    ) {
        SettingsDetailParams(
            currentPage = currentPage,
            imageCacheSizeBytes = imageCacheSizeBytes,
            storageStats = storageStats,
            sizeRedTotal = sizeRedTotal,
            sizeRedDownload = sizeRedDownload,
            onClearImageCache = onClearImageCache,
            onClearDownload = onClearDownload,
            data = data,
            context = context,
            onBackupDataChanged = onBackupDataChanged,
            onOpenLLogin = onOpenLLogin
        )
    }
```

- [ ] **Шаг 5: собрать и прогнать тесты модуля**

Run: `./gradlew.bat :app:testDebugUnitTest`

Expected: `BUILD SUCCESSFUL`.

---

### Задача 6: Полная проверка

- [ ] **Шаг 1: все модули и detekt**

Run: `./gradlew.bat testDebugUnitTest detekt`

Expected: `BUILD SUCCESSFUL`. В отчёт идёт фактический результат: число выполненных задач, упавшие тесты и замечания detekt, если они есть.

- [ ] **Шаг 2: установить сборку на эмулятор**

Только на эмулятор — на телефон владельца сборку не ставить:

```bash
ANDROID_SERIAL=emulator-5554 ./gradlew.bat :app:installDebug
MSYS_NO_PATHCONV=1 /c/zip/bin/adb -s emulator-5554 shell monkey -p com.client.xvideos -c android.intent.category.LAUNCHER 1
```

Экран читать через `uiautomator dump` (текст и границы), а не по снимкам:

```bash
MSYS_NO_PATHCONV=1 /c/zip/bin/adb -s emulator-5554 shell uiautomator dump /sdcard/ui.xml
MSYS_NO_PATHCONV=1 /c/zip/bin/adb -s emulator-5554 shell cat /sdcard/ui.xml
```

- [ ] **Шаг 3: сценарии на устройстве**

Путь: главное меню → «Настройки» → «L».

Перед началом посмотреть, что показывает строка «Профиль L». Если на эмуляторе сохранён аккаунт владельца, подтверждение «Выйти» сотрёт его логин и пароль, и вводить их заново придётся владельцу — подтверждать выход только с его согласия.

| № | Условие | Действие | Ожидается |
|---|---|---|---|
| 1 | аккаунта нет | открыть страницу L настроек | строка «Профиль L», под ней «Вход не выполнен», кнопка «Войти» |
| 2 | аккаунта нет | «Войти» | форма входа; кнопки «Сохранить» и «Назад»; кнопки «Пропустить» нет |
| 3 | форма из настроек | «Назад», затем системная кнопка «назад» из повторно открытой формы | оба раза возврат на страницу L настроек, а не на главную страницу настроек |
| 4 | аккаунта нет, приложение перезапущено | главное меню → L | форма входа с кнопкой «Пропустить», как раньше |
| 5 | аккаунт сохранён | открыть страницу L настроек | под «Профиль L» — логин, кнопка «Выйти» |
| 6 | аккаунт сохранён | «Выйти» → «Отмена» | диалог «Выйти из профиля L?» закрылся, строка не изменилась |
| 7 | аккаунт сохранён | «Выйти» → «Выйти» | снэкбар «Вы вышли из профиля L», строка — «Вход не выполнен» и «Войти» |
| 8 | сразу после сценария 7, без перезапуска | открыть списки альбомов L | выдача анонимная; в `logcat` после выхода нет новых записей `L login` |

Сценарии 5–8 требуют настоящего аккаунта. Логин и пароль вводит владелец; исполнитель пароли не вводит.

- [ ] **Шаг 4: отчёт**

Сообщить владельцу: что прошло, что не проверено и почему. Сценарии, которые не выполнялись, перечислить явно — не писать «должно работать».

---

## Предлагаемая разбивка коммитов

Коммитит владелец по своей команде. Естественные границы:

1. `fix(l): выход из профиля закрывает сессию без перезапуска` — Задача 1.
2. `refactor(l): форма входа вынесена в молекулу` — Задача 2. Отдельным коммитом перенос остаётся в истории переименованием файла.
3. `feat(settings): вход и выход в строке профиля L` — Задачи 3–5.
