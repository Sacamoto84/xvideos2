# Код уничтожения — план реализации

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ввод альтернативного кода уничтожения на экране замка (или «=» в калькуляторе) мгновенно отрезает и стирает данные приложения, перезапускает процесс в чистый главный экран без показа экрана замка, а код уничтожения становится новым кодом доступа. Настройка задаётся в «Настройки → Приватность».

**Architecture:**
- В `:core` (`AppLockRepository`): хранение хеша и соли кода уничтожения в `AppLockSecureStore` (Keystore). Проверка любого кода на экране замка в постоянное время (`verifyUnlockCode`) с одновременным вычислением обоих PBKDF2 хешей, если код уничтожения задан.
- В `:core` (`common/duress/`): `DuressWipeIsolation` — мгновенное изолирование каталогов (`renameTo` в `pending_wipe/`) с сохранением белого списка настроек замка и очисткой всего остального; `DuressTokenStore` — одноразовый криптографический токен пропуска замка с двойной меткой времени (монотонные часы + настенные) и защитой от внешнего вызова `SplashActivity`.
- В `:app` (`duress/`): `RestartActivity` в отдельном изолированном процессе `:restart` для чистого перезапуска без всплывающих окон и пауз; очистка уведомлений через `NotificationManager.cancelAll()`. Доочистка `pending_wipe` в `App.onCreate` до `AppPath.init` (гарантия изоляции) и в фоне через `StorageCleanupGate`.
- В `:app` (`screenSettings/`): молекулы `DuressCodeRow` и `DuressCodeDialog` по правилам `GEMINI.md` (один компонент — один файл, минималистичные `@Preview`, `excludedFromAutofill`).

**Tech Stack:** Kotlin, Jetpack Compose / Material3, Android Keystore, EncryptedSharedPreferences, PBKDF2WithHmacSHA256, JUnit 4, Robolectric / Mockito / Coroutines Test.

**Spec:** [docs/superpowers/specs/2026-10-28-duress-code-design.md](../specs/2026-10-28-duress-code-design.md) — читать вместе с планом.

## Global Constraints

- Названия сайтов не пишутся нигде: только X / L / R / XLive.
- Все коммиты строго на русском языке, Conventional Commits (`feat(security): ...`, `docs(security): ...`).
- Правила Compose UI из `GEMINI.md`: один компонент — один файл, имя файла строго совпадает с Composable-функцией, обязательные лёгкие `@Preview`, запрет монолитных файлов, поля ввода помечены `excludedFromAutofill()`.
- Каждая новая Activity обязана вызывать `RotationLockKeeper.onTopResumedActivityChanged(this, isTopResumedActivity)` (сторож `ActivityRotationLockTest`).
- Строгий гейт перед каждым коммитом: `gradlew.bat testDebugUnitTest detekt :app:lintRelease`.
- Безопасность: в логи `Timber` и терминал не выводятся чувствительные данные и пароли.

---

## Кусок A — Хранение и криптография кода уничтожения в `:core`

### Task 1: Поддержка кода уничтожения в `AppLockRepository`

**Files:**
- Modify: `core/src/main/java/com/client/xvideos/common/applock/AppLockRepository.kt`
- Test: `core/src/test/java/com/client/xvideos/common/applock/AppLockRepositoryTest.kt`

**Interfaces:**
- `AppLockRepository`:
  - `fun isDuressCodeSet(context: Context? = null): Boolean`
  - `suspend fun setDuressCode(context: Context? = null, code: String): Result<Unit>` (проверка длины >= 4, проверка `!verifyPassword(code)` — код не равен коду доступа, сохранение соли и хеша в securePrefs)
  - `fun clearDuressCode(context: Context? = null)`
  - `suspend fun verifyDuressCode(context: Context? = null, code: String): Boolean`
  - `suspend fun verifyDuressCodeThrottled(context: Context? = null, code: String): AppLockCheckResult`
  - Обновить `clearPassword(context)`: при выключении замка удалять также `KEY_DURESS_SALT` и `KEY_DURESS_HASH`.
  - Обновить `setPassword(context, password)`: если код уничтожения задан, запрещать установку пароля, совпадающего с кодом уничтожения (`verifyDuressCode(password)`).

- [ ] **Step 1: Написать тесты** в `AppLockRepositoryTest`:
  - `isDuressCodeSet returns false initially, true after setDuressCode`
  - `setDuressCode fails if code shorter than 4 chars`
  - `setDuressCode fails if code matches access password`
  - `setPassword fails if new password matches duress code`
  - `verifyDuressCode validates correctly`
  - `clearDuressCode removes duress secrets but preserves access password`
  - `clearPassword removes both access password and duress code`
- [ ] **Step 2:** Запустить `:core:testDebugUnitTest --tests "*AppLockRepositoryTest"` — FAIL (методы отсутствуют).
- [ ] **Step 3:** Реализовать методы в `AppLockRepository.kt`.
- [ ] **Step 4:** Запустить `:core:testDebugUnitTest --tests "*AppLockRepositoryTest"` — PASS.

---

### Task 2: Проверка кода в постоянное время (`verifyUnlockCode`) и `promoteDuressCodeToAccessCode`

**Files:**
- Modify: `core/src/main/java/com/client/xvideos/common/applock/AppLockRepository.kt`
- Test: `core/src/test/java/com/client/xvideos/common/applock/AppLockRepositoryTest.kt`

**Interfaces:**
- `sealed interface AppLockUnlockResult`:
  - `data object AccessGranted : AppLockUnlockResult`
  - `data object DuressTriggered : AppLockUnlockResult`
  - `data object Wrong : AppLockUnlockResult`
- `AppLockRepository`:
  - `suspend fun verifyUnlockCode(context: Context? = null, code: String): AppLockUnlockResult` — если код уничтожения задан, ВСЕГДА вычисляет оба хеша (PBKDF2 для пароля доступа и PBKDF2 для кода уничтожения), исключая side-channel утечку времени проверки.
  - `fun promoteDuressCodeToAccessCode(context: Context? = null): Boolean` — атомарно переносит хеш и соль кода уничтожения в пароль доступа, удаляет код уничтожения и сбрасывает счётчики троттлинга.

- [ ] **Step 1: Написать тесты** в `AppLockRepositoryTest`:
  - `verifyUnlockCode returns AccessGranted for valid access code`
  - `verifyUnlockCode returns DuressTriggered for valid duress code`
  - `verifyUnlockCode returns Wrong for invalid code`
  - `verifyUnlockCode without duress code configured behaves normally`
  - `promoteDuressCodeToAccessCode makes duress code the new access password and removes duress code`
- [ ] **Step 2:** Запустить тесты — FAIL.
- [ ] **Step 3:** Реализовать `verifyUnlockCode` и `promoteDuressCodeToAccessCode`.
- [ ] **Step 4:** Запустить тесты — PASS.

---

## Кусок B — Изоляция данных и безопасный пропуск замка в `:core`

### Task 3: `DuressTokenStore` — одноразовый токен пропуска замка

**Files:**
- Create: `core/src/main/java/com/client/xvideos/common/duress/DuressTokenStore.kt`
- Test: `core/src/test/java/com/client/xvideos/common/duress/DuressTokenStoreTest.kt`

**Interfaces:**
- `object DuressTokenStore`:
  - `const val EXTRA_DURESS_TOKEN = "com.client.xvideos.extra.DURESS_TOKEN"`
  - `const val TOKEN_MAX_AGE_MS = 15_000L`
  - `fun createOneTimeToken(context: Context): String` (генерирует 128-битный токен, записывает в `dataDir/.duress_token` с `wallTime` и `elapsedRealtime`)
  - `fun consumeTokenIfValid(context: Context, tokenFromIntent: String?): Boolean` (читает, немедленно удаляет файл, сверяет токен constant-time через `MessageDigest.isEqual`, проверяет срок <= 15с по обоим часам)

- [ ] **Step 1: Написать тесты** в `DuressTokenStoreTest`:
  - `valid token within lifespan returns true and deletes file`
  - `second attempt with same token fails (single use)`
  - `invalid token string returns false and deletes file`
  - `null or empty token returns false`
  - `expired token (> 15 seconds) returns false`
  - `rebooted device (elapsedRealtime skew) returns false`
- [ ] **Step 2:** Запустить тесты — FAIL.
- [ ] **Step 3:** Реализовать `DuressTokenStore`.
- [ ] **Step 4:** Запустить тесты — PASS.

---

### Task 4: `DuressWipeIsolation` — мгновенная изоляция хранилища и фоновая очистка

**Files:**
- Create: `core/src/main/java/com/client/xvideos/common/duress/DuressWipeIsolation.kt`
- Test: `core/src/test/java/com/client/xvideos/common/duress/DuressWipeIsolationTest.kt`

**Interfaces:**
- `object DuressWipeIsolation`:
  - `fun isolateSensitiveData(context: Context): Boolean` (создаёт маркер `.wipe_in_progress`, перемещает все папки `files`, `cache`, `databases`, `app_webview`, `no_backup` и внешние каталоги в `pending_wipe/`, в `shared_prefs` сохраняет только 5 настроек и Keystore-хранилище, удаляет `secure_credentials.xml`, вызывает `promoteDuressCodeToAccessCode`)
  - `fun cleanPendingWipe(context: Context)` (рекурсивно удаляет `pending_wipe/`, после завершения удаляет маркер `.wipe_in_progress`)
  - `fun ensureIsolatedBeforeStartup(context: Context)` (если `.wipe_in_progress` существует при старте приложения, гарантирует изоляцию всех оставшихся приватных папок до инициализации Hilt и `AppPath`)

- [ ] **Step 1: Написать тесты** в `DuressWipeIsolationTest` с временными каталогами (моделирование структуры `dataDir`):
  - `isolateSensitiveData moves files, cache, databases to pending_wipe`
  - `isolateSensitiveData preserves app_lock_secure_prefs and preserved settings`
  - `isolateSensitiveData wipes secure_credentials and unpreserved preferences`
  - `cleanPendingWipe removes pending_wipe and marker file`
  - `ensureIsolatedBeforeStartup isolates residual data if marker exists`
- [ ] **Step 2:** Запустить тесты — FAIL.
- [ ] **Step 3:** Реализовать `DuressWipeIsolation`.
- [ ] **Step 4:** Запустить тесты — PASS.

---

## Кусок C — Перезапуск процесса и срабатывание на экране замка в `:app`

### Task 5: `RestartActivity` и `DuressLauncher`

**Files:**
- Create: `app/src/main/java/com/client/xvideos/duress/RestartActivity.kt`
- Create: `app/src/main/java/com/client/xvideos/duress/DuressLauncher.kt`
- Modify: `app/src/main/AndroidManifest.xml` (регистрация `RestartActivity` в `:restart`)

**Interfaces:**
- `RestartActivity : Activity()`:
  - Процесс: `android:process=":restart"`, тема `Theme.Translucent.NoTitleBar`, `exported="false"`, `excludeFromRecents="true"`.
  - Реализует `RotationLockKeeper.onTopResumedActivityChanged(this, isTopResumedActivity)`.
  - В `onCreate`: убивает старый PID, запускает целевой Intent (`SplashActivity` с токеном), `finish()`, `overridePendingTransition(0, 0)`, `exitProcess(0)`.
- `DuressLauncher`:
  - `fun execute(activity: Activity): Boolean` — вызывает `DuressWipeIsolation.isolateSensitiveData`, снимает все уведомления через `NotificationManager.cancelAll()`, создаёт токен `DuressTokenStore`, запускает `RestartActivity` и убивает текущий процесс. При неудаче возвращает `false`.

- [ ] **Step 1:** Реализовать `RestartActivity` и `DuressLauncher`.
- [ ] **Step 2:** Добавить запись `RestartActivity` в `AndroidManifest.xml`.
- [ ] **Step 3:** Запустить тест `ActivityRotationLockTest` — PASS.

---

### Task 6: Интеграция в `LockActivity`, `SplashActivity`, `App.onCreate`

**Files:**
- Modify: `app/src/main/java/com/client/xvideos/LockActivity.kt`
- Modify: `app/src/main/java/com/client/xvideos/SplashActivity.kt`
- Modify: `app/src/main/java/com/client/xvideos/App.kt`

**Details:**
- `LockActivity.tryUnlock(code)`:
  - Вызывает `AppLockRepository.verifyUnlockCode(this, code)`.
  - Если `AccessGranted`: сброс попыток, `AppLockSession.unlock()`, закрытие экрана.
  - Если `DuressTriggered`: сброс попыток, вызов `DuressLauncher.execute(this)`. Если изоляция не удалась — вернуть `false` (показать «Неверный код доступа» per spec 8).
  - Если `Wrong`: зарегистрировать ошибку, вернуть `false`.
- `SplashActivity.onCreate`:
  - Читает токен из входящего Intent через `DuressTokenStore.consumeTokenIfValid(this, token)`.
  - Если валиден: `AppLockSession.unlock()` (пропуск экрана замка).
  - Иначе: если замок включён — `AppLockSession.lock()`.
- `App.onCreate`:
  - Если процесс `:restart` (`Application.getProcessName().endsWith(":restart")`) — немедленный выход без инициализации Hilt/сервисов.
  - Перед `AppPath.init(this)`: `DuressWipeIsolation.ensureIsolatedBeforeStartup(this)`.
  - Внутри блока `StorageCleanupGate`: вызов `DuressWipeIsolation.cleanPendingWipe(this)`.

- [ ] **Step 1:** Внести изменения в `App.kt`, `SplashActivity.kt`, `LockActivity.kt`.
- [ ] **Step 2:** Запустить тесты `:app:testDebugUnitTest` — PASS.

---

## Кусок D — Настройки кода уничтожения в UI

### Task 7: Молекулы `DuressCodeRow` и `DuressCodeDialog`

**Files:**
- Create: `app/src/main/java/com/client/xvideos/screenSettings/molecule/DuressCodeRow.kt`
- Create: `app/src/main/java/com/client/xvideos/screenSettings/molecule/DuressCodeDialog.kt`

**Details:**
- `DuressCodeRow.kt`:
  - Параметры: `isSet: Boolean`, `onSetClick: () -> Unit`, `onChangeClick: () -> Unit`, `onRemoveClick: () -> Unit`.
  - Если `!isSet`: кнопка «Задать».
  - Если `isSet`: кнопки «Изменить» и «Убрать» (`SettingsDestructiveColor`).
  - Легковесный `@Preview`.
- `DuressCodeDialog.kt`:
  - Режимы `enum class DuressDialogMode { SET, CHANGE, REMOVE }`.
  - Поля ввода на базе `PasswordSettingField` (защита от автозаполнения, иконка скрытия/показа).
  - Проверка совпадения с текущим кодом доступа: `AppLockRepository.verifyPassword(code)` — запрет совпадения («Код уничтожения не может совпадать с кодом доступа»).
  - Легковесный `@Preview`.

- [ ] **Step 1:** Реализовать `DuressCodeRow.kt` и `DuressCodeDialog.kt`.
- [ ] **Step 2:** Проверить выполнение правил Compose UI (один компонент — один файл, preview, отсутствие ViewModel в параметрах).

---

### Task 8: Встраивание в `AppLockSection.kt`

**Files:**
- Modify: `app/src/main/java/com/client/xvideos/screenSettings/components/AppLockSection.kt`

**Details:**
- Отображение `DuressCodeRow` в блоке `if (enabled)` строго под пунктом «Код доступа».
- Диалог `DuressCodeDialog` по клику на кнопки.
- В `AppLockPasswordDialog` при установке/смене кода доступа (`SET` / `CHANGE`):
  - Проверка: если код уничтожения задан, запрещать новый код доступа, равный коду уничтожения («Код доступа не может совпадать с кодом уничтожения»).
- При выключении замка (`DISABLE`) — автоматический сброс кода уничтожения через `AppLockRepository.clearPassword`.

- [ ] **Step 1:** Интегрировать `DuressCodeRow` и `DuressCodeDialog` в `AppLockSection.kt`.
- [ ] **Step 2:** Проверить `AppLockSettingsSectionPreview`.

---

## Кусок E — Проверка сторожей, гейт и завершение

### Task 9: Архитектурные сторожа и полный гейт

- [ ] **Step 1: Запуск сторожей архитектуры:**
  - `ActivityRotationLockTest`
  - `AutofillExclusionTest`
  - `LayerBoundariesTest`
  - `ModuleBoundariesTest`
  - `AdaptiveLayoutTest`
- [ ] **Step 2: Запуск полного гейта:**
  `gradlew.bat testDebugUnitTest detekt :app:lintRelease`
- [ ] **Step 3:** Убедиться, что отчёт lintRelease не содержит Fatal/Error, detekt чист, все юнит-тесты зелёные.
- [ ] **Step 4:** Подготовить коммит согласно регламенту.
