# A1: один механизм внедрения зависимостей

> **Для агентных исполнителей:** ОБЯЗАТЕЛЬНЫЙ СУБ-НАВЫК: используйте
> `superpowers:subagent-driven-development` (рекомендуется) или
> `superpowers:executing-plans` для выполнения плана задача за задачей.
> Шаги размечены чекбоксами (`- [ ]`).

**Цель:** убрать две глобальные точки, у которых уже есть готовая замена в
Hilt, а оставшиеся три закрыть сторожем, который не даст ни сломать порядок
инициализации молча, ни завести новую глобальную точку незаметно.

**Архитектура:** проект живёт на Hilt, но рядом существует рукописный
service locator из пяти мутабельных глобалов, которые заполняет `App.onCreate`
в неявно значимом порядке. Две из них снимаются дёшево: `App.instance` читает
ровно один потребитель, а `NetworkTrafficMonitor` — уже `@Singleton class …
@Inject constructor()`, то есть `companion.current` это костыль поверх готового
DI-класса. Оставшиеся три (`AppContextHolder`, `P2pReceiveManager.importerFactory`,
`P2pSendPreparers.l`) существуют по содержательной причине — базовый слой не
знает про разделы и про класс приложения, — поэтому вместо их устранения
контракт фиксируется тестом-сторожем в духе уже существующих
`LayerBoundariesTest` / `ModuleBoundariesTest`: текстовый разбор исходников, без
рефлексии и инструментальных тестов.

**Стек:** Kotlin, Hilt (`@Singleton`, `@EntryPoint`, `EntryPointAccessors`),
Compose, JUnit4.

**Радиус:** A2, A3, A4 — отдельные планы. `T1` (`awaitStorageCleanup` как
соглашение) частично закрывается задачей 1 этого плана: ожидание переезжает в
инжектируемый объект, но обязанность его дождаться остаётся на вызывающем —
полное решение (suspend-геттеры у `AppPath`) сюда не входит.

---

## Порядок задач

| Задача | Что | Зависимости |
| --- | --- | --- |
| 1. `StorageCleanupGate` | убрать `App.instance` | — |
| 2. `NetworkTrafficMonitorEntryPoint` | убрать `NetworkTrafficMonitor.current` | — |
| 3. Сторож глобального состояния | зафиксировать оставшиеся три + порядок | после 1 и 2 |

Задачи 1 и 2 независимы. Задача 3 идёт последней: её список известных глобалов
должен отражать уже сокращённое состояние.

---

## Файловая структура

**Создаются:**

- `core/src/main/java/com/client/xvideos/common/storage/StorageCleanupGate.kt` —
  `@Singleton`, держит `Job` фоновой уборки staging-папок и даёт его дождаться.
- `core/src/main/java/com/client/xvideos/common/traficStatistic/NetworkTrafficMonitorEntryPoint.kt`
  — `@EntryPoint` + `rememberTrafficFlow()`, по образцу
  `common/di/ApplicationScopeEntryPoint.kt`.
- `app/src/test/java/com/client/xvideos/arch/GlobalStateTest.kt` — сторож.

**Меняются:**

- `app/src/main/java/com/client/xvideos/App.kt` — `instance` удаляется,
  `awaitStorageCleanup` переезжает, запуск уборки идёт через `StorageCleanupGate`.
- `app/src/main/java/com/client/xvideos/MainActivity.kt:120` — `App.instance
  .awaitStorageCleanup()` → инжектированный `storageCleanupGate.await()`.
- `core/src/main/java/com/client/xvideos/common/traficStatistic/NetworkTrafficMonitor.kt`
  — удаляется `companion object { var current }`.
- `core/src/main/java/com/client/xvideos/common/traficStatistic/AppNetworkSpeedMonitor.kt:28`
  и `AppNetworkSpeedMonitorLite.kt:34` — на `rememberTrafficFlow()`.

---

### Задача 1: `StorageCleanupGate` вместо `App.instance`

`App.instance` читает ровно один потребитель — `MainActivity:120`, и только
ради `awaitStorageCleanup()`. Значит глобал держится не ради доступа к
`Application`, а ради одной корутины.

**Важно про порядок:** `AppPath.init()` обязан отработать до первого
Hilt-синглтона (они читают пути в конструкторе). Поэтому `App` **не** получает
gate через `@Inject lateinit var` — инъекция в `Application` происходит внутри
`super.onCreate()`, то есть слишком рано. Gate берётся лениво через
`EntryPointAccessors` уже после `AppPath.init()`, как это делает
`sectionBundleImporter`.

**Файлы:**
- Создать: `core/src/main/java/com/client/xvideos/common/storage/StorageCleanupGate.kt`
- Создать: `core/src/test/java/com/client/xvideos/common/storage/StorageCleanupGateTest.kt`
- Изменить: `app/src/main/java/com/client/xvideos/App.kt`
- Изменить: `app/src/main/java/com/client/xvideos/MainActivity.kt`

- [ ] **Шаг 1: Написать падающий тест**

Создать `core/src/test/java/com/client/xvideos/common/storage/StorageCleanupGateTest.kt`:

```kotlin
package com.client.xvideos.common.storage

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StorageCleanupGateTest {

    @Test
    fun `await не блокирует, если уборка не запускалась`() = runTest {
        val gate = StorageCleanupGate()

        gate.await()
        // Дошли сюда — значит await вернулся. Так ведёт себя процесс, в котором
        // App.onCreate ещё не успел стартовать уборку (unit-тесты, Preview).
        assertTrue(true)
    }

    @Test
    fun `await ждёт запущенную уборку и не запускает её дважды`() = runTest {
        val gate = StorageCleanupGate()
        val started = CompletableDeferred<Unit>()
        var runs = 0

        gate.start(this) {
            runs++
            started.complete(Unit)
        }
        gate.start(this) { runs++ }

        gate.await()
        started.await()

        assertEquals("повторный start обязан быть проигнорирован", 1, runs)
    }

    @Test
    fun `await переживает падение уборки`() = runTest {
        val gate = StorageCleanupGate()
        gate.start(this) { error("уборка упала") }

        gate.await()
        // Падение уборки не должно превращаться в падение ожидающего: он ждёт
        // «уборка больше не идёт», а не «уборка удалась».
        assertTrue(true)
    }
}
```

- [ ] **Шаг 2: Запустить тест и убедиться, что он падает**

```bash
./gradlew :core:testDebugUnitTest --tests "com.client.xvideos.common.storage.StorageCleanupGateTest"
```

Ожидается: ошибка компиляции — `Unresolved reference: StorageCleanupGate`.

- [ ] **Шаг 3: Написать `StorageCleanupGate.kt`**

```kotlin
package com.client.xvideos.common.storage

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Разовая уборка staging-папок и ожидание её завершения.
 *
 * Раньше `Job` уборки жил полем в `App`, а дождаться его можно было только
 * через `App.instance` — единственная причина, по которой этот глобал вообще
 * существовал. Теперь ожидание инжектируется как обычная зависимость.
 *
 * Ждать обязательно перед первым обращением к `AppPath.p2p_inbox`,
 * `p2p_outbox` и `l_cacheDownload`: уборка их рекурсивно удаляет и создаёт
 * заново, и работа с ними параллельно с этим потеряет файлы.
 */
@Singleton
class StorageCleanupGate @Inject constructor() {

    @Volatile
    private var job: Job? = null

    /**
     * Запускает уборку в [scope]. Повторный вызов игнорируется: уборка разовая
     * и на процесс одна.
     */
    @Synchronized
    fun start(scope: CoroutineScope, block: suspend () -> Unit) {
        if (job != null) return
        job = scope.launch {
            runCatching { block() }
                .onFailure { Timber.e(it, "StorageCleanupGate: уборка staging-папок упала") }
        }
    }

    /**
     * Ждёт завершения уборки. Возвращается сразу, если она не запускалась —
     * так выглядит процесс без `App.onCreate` (unit-тесты, Compose Preview).
     *
     * Падение самой уборки ожидающего не роняет: контракт — «уборка больше не
     * идёт», а не «уборка удалась». Ошибка уже в журнале.
     */
    suspend fun await() {
        job?.join()
    }
}
```

- [ ] **Шаг 4: Запустить тест и убедиться, что он проходит**

```bash
./gradlew :core:testDebugUnitTest --tests "com.client.xvideos.common.storage.StorageCleanupGateTest"
```

Ожидается: PASS, 3 теста.

- [ ] **Шаг 5: Перевести `App` на gate**

В `app/src/main/java/com/client/xvideos/App.kt` добавить импорты:

```kotlin
import com.client.xvideos.common.storage.StorageCleanupGate
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
```

Удалить поле `storageCleanup` вместе с его комментарием:

```kotlin
    // @Volatile: присваивается на главном потоке в onCreate, читается из
    // IO-корутины MainActivity.
    @Volatile
    private var storageCleanup: Job? = null
```

Удалить метод `awaitStorageCleanup` целиком (вместе с KDoc) — его роль
перешла к `StorageCleanupGate.await()`.

Заменить строку запуска уборки в `onCreate`:

```kotlin
        AppPath.init(this)
        storageCleanup = scope.launch { AppPath.cleanupTransientDirs() }
```

на

```kotlin
        AppPath.init(this)
        // Gate берётся лениво через EntryPoint, а не через `@Inject lateinit`:
        // инъекция в Application происходит внутри super.onCreate(), то есть
        // до AppPath.init(), а Hilt-синглтоны читают пути в конструкторе.
        EntryPointAccessors
            .fromApplication(this, StorageCleanupEntryPoint::class.java)
            .storageCleanupGate()
            .start(scope) { AppPath.cleanupTransientDirs() }
```

Удалить импорт `kotlinx.coroutines.Job`, если он больше не используется.

В конец файла, после класса `App`, добавить точку доступа:

```kotlin
/** Доступ к [StorageCleanupGate] из `App.onCreate`, где инъекция ещё слишком ранняя. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface StorageCleanupEntryPoint {
    fun storageCleanupGate(): StorageCleanupGate
}
```

Удалить `companion object` с `instance` целиком:

```kotlin
    companion object {
        /**
         * Singleton-доступ к `Application` там, где пока нет DI-контекста.
         *
         * Использовать осторожно: для новых зависимостей предпочтительнее Hilt,
         * чтобы не разносить глобальное состояние по коду.
         */
        lateinit var instance: App
            private set
    }
```

и присваивание `instance = this` в начале `onCreate`.

- [ ] **Шаг 6: Перевести `MainActivity` на инъекцию**

В `app/src/main/java/com/client/xvideos/MainActivity.kt` добавить импорт:

```kotlin
import com.client.xvideos.common.storage.StorageCleanupGate
```

Рядом с существующими инъекциями (`:73-77`) добавить:

```kotlin
    @Inject
    lateinit var storageCleanupGate: StorageCleanupGate
```

Заменить строку `:120`:

```kotlin
            App.instance.awaitStorageCleanup()
```

на

```kotlin
            storageCleanupGate.await()
```

- [ ] **Шаг 7: Убедиться, что `App.instance` больше нигде не читается**

```bash
grep -rn "App\.instance" app core feature-l feature-r feature-x --include=*.kt
```

Ожидается: только закомментированные упоминания в
`core/.../util/ToastShow.kt:7` и историческая ссылка в KDoc
`AppContextHolder.kt:9`. Живых обращений быть не должно.

- [ ] **Шаг 8: Собрать и прогнать тесты**

```bash
./gradlew :core:testDebugUnitTest :app:compileDebugKotlin
```

Ожидается: BUILD SUCCESSFUL.

- [ ] **Шаг 9: Коммит**

```bash
git add core/src/main/java/com/client/xvideos/common/storage core/src/test/java/com/client/xvideos/common/storage app/src/main/java/com/client/xvideos/App.kt app/src/main/java/com/client/xvideos/MainActivity.kt
git commit -m "refactor(app): ожидание уборки инжектируется, App.instance удалён"
```

---

### Задача 2: `NetworkTrafficMonitor` без статического слота

Класс уже `@Singleton class NetworkTrafficMonitor @Inject constructor()` — то
есть Hilt умеет его отдавать. `companion object { @Volatile var current }`
существует только потому, что два composable в базовом слое не знали, как
попросить зависимость. В том же модуле рядом лежит готовый образец —
`common/di/ApplicationScopeEntryPoint.kt` с `rememberApplicationScope()`.

**Файлы:**
- Создать: `core/src/main/java/com/client/xvideos/common/traficStatistic/NetworkTrafficMonitorEntryPoint.kt`
- Изменить: `core/src/main/java/com/client/xvideos/common/traficStatistic/NetworkTrafficMonitor.kt`
- Изменить: `core/src/main/java/com/client/xvideos/common/traficStatistic/AppNetworkSpeedMonitor.kt:26-29`
- Изменить: `core/src/main/java/com/client/xvideos/common/traficStatistic/AppNetworkSpeedMonitorLite.kt:32-35`
- Изменить: `app/src/main/java/com/client/xvideos/App.kt`

- [ ] **Шаг 1: Написать точку доступа**

Создать
`core/src/main/java/com/client/xvideos/common/traficStatistic/NetworkTrafficMonitorEntryPoint.kt`:

```kotlin
package com.client.xvideos.common.traficStatistic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Доступ к [NetworkTrafficMonitor] из composable вне DI-графа. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface NetworkTrafficMonitorEntryPoint {
    fun networkTrafficMonitor(): NetworkTrafficMonitor
}

/**
 * Поток статистики трафика для виджетов базового слоя.
 *
 * Раньше монитор доставался из `NetworkTrafficMonitor.current` — статического
 * слота, который заполнял `App.onCreate`. Слот был не нужен: класс и так
 * `@Singleton` с `@Inject`-конструктором, просто composable не умели его
 * попросить.
 *
 * В Compose Preview DI-графа нет, поэтому там отдаётся пустой поток — иначе
 * превью любого экрана со счётчиком трафика падало бы.
 */
@Composable
fun rememberTrafficFlow(): StateFlow<TrafficData> {
    val context = LocalContext.current
    val inPreview = LocalInspectionMode.current
    return remember(context, inPreview) {
        if (inPreview) {
            MutableStateFlow(TrafficData())
        } else {
            EntryPointAccessors
                .fromApplication(
                    context.applicationContext,
                    NetworkTrafficMonitorEntryPoint::class.java,
                )
                .networkTrafficMonitor()
                .trafficFlow
        }
    }
}
```

- [ ] **Шаг 2: Перевести оба виджета**

В `AppNetworkSpeedMonitor.kt` заменить

```kotlin
    val trafficFlow = remember {
        NetworkTrafficMonitor.current?.trafficFlow ?: MutableStateFlow(TrafficData())
    }
```

на

```kotlin
    val trafficFlow = rememberTrafficFlow()
```

То же самое в `AppNetworkSpeedMonitorLite.kt`. В обоих файлах удалить ставший
лишним импорт `kotlinx.coroutines.flow.MutableStateFlow`, если он больше нигде
в файле не используется.

- [ ] **Шаг 3: Удалить статический слот**

В `NetworkTrafficMonitor.kt` удалить весь блок:

```kotlin
    companion object {
        /**
         * Монитор процесса: создаёт и запускает его `App`, а виджеты базового
         * слоя берут готовый. Класс приложения им не виден — он в точке сборки.
         *
         * null до `App.onCreate` и в unit-тестах.
         */
        @Volatile
        var current: NetworkTrafficMonitor? = null
    }
```

- [ ] **Шаг 4: Перевести `App` на инъекцию монитора**

В `App.kt` заменить блок создания монитора:

```kotlin
        // Инициализируем монитор трафика
        networkTrafficMonitor = NetworkTrafficMonitor()
        NetworkTrafficMonitor.current = networkTrafficMonitor
        networkTrafficMonitor.startMonitoring()
```

на

```kotlin
        // Монитор берётся из графа — тем же EntryPoint, что и gate уборки:
        // здесь мы уже после AppPath.init(), синглтоны создавать можно.
        networkTrafficMonitor = EntryPointAccessors
            .fromApplication(this, NetworkTrafficMonitorEntryPoint::class.java)
            .networkTrafficMonitor()
        networkTrafficMonitor.startMonitoring()
```

и добавить импорт:

```kotlin
import com.client.xvideos.common.traficStatistic.NetworkTrafficMonitorEntryPoint
```

Поле `networkTrafficMonitor` с `lateinit var … private set` остаётся: его
использует `onTerminate`.

- [ ] **Шаг 5: Убедиться, что слот больше не используется**

```bash
grep -rn "NetworkTrafficMonitor.current" app core feature-l feature-r feature-x --include=*.kt
```

Ожидается: пусто.

- [ ] **Шаг 6: Собрать и прогнать тесты**

```bash
./gradlew :core:testDebugUnitTest :app:compileDebugKotlin
```

Ожидается: BUILD SUCCESSFUL.

- [ ] **Шаг 7: Проверить на устройстве**

Собрать debug, открыть экран со счётчиком трафика, убедиться, что цифры идут.
Затем открыть Compose Preview любого экрана с `AppNetworkSpeedMonitorLite` —
превью должно отрисоваться, а не упасть.

```bash
./gradlew :app:assembleDebug
```

- [ ] **Шаг 8: Коммит**

```bash
git add core/src/main/java/com/client/xvideos/common/traficStatistic app/src/main/java/com/client/xvideos/App.kt
git commit -m "refactor(core): монитор трафика берётся из графа, статический слот убран"
```

---

### Задача 3: Сторож глобального состояния и порядка инициализации

Оставшиеся три точки (`AppContextHolder`, `P2pReceiveManager.importerFactory`,
`P2pSendPreparers.l`) существуют по содержательной причине: базовый слой не
знает ни класса приложения, ни разделов. Их не убираем — фиксируем, чтобы
список не рос молча и чтобы порядок инициализации в `App.onCreate` нельзя было
переставить, не заметив.

Сторож текстовый, как `LayerBoundariesTest` и `ModuleBoundariesTest`: читает
исходники построчно, без рефлексии и без Android.

**Файлы:**
- Создать: `app/src/test/java/com/client/xvideos/arch/GlobalStateTest.kt`

- [ ] **Шаг 1: Написать сторож**

Создать `app/src/test/java/com/client/xvideos/arch/GlobalStateTest.kt`:

```kotlin
package com.client.xvideos.arch

import com.client.xvideos.arch.ProjectSources.invariantPath
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/**
 * Сторож глобального изменяемого состояния.
 *
 * Проект живёт на Hilt, но рядом есть несколько мутабельных статических точек,
 * которые заполняет `App.onCreate`. Часть из них убрана (`App.instance`,
 * `NetworkTrafficMonitor.current`), оставшиеся существуют по причине: базовый
 * слой не знает ни класса приложения, ни разделов.
 *
 * Тест не запрещает такие точки — он запрещает заводить их **незаметно**.
 * Новая строка в [ALLOWED] должна появляться вместе с ответом на вопрос
 * «почему это не зависимость?».
 */
class GlobalStateTest {

    @Test
    fun `список изменяемых глобальных точек не растёт`() {
        val found = ProjectSources.roots()
            .flatMap { root ->
                root.walkTopDown()
                    .filter { it.isFile && it.extension == "kt" }
                    .flatMap { file -> mutableStaticsIn(root, file) }
            }
            .toSortedSet()

        assertEquals(
            "Изменилось множество изменяемых глобальных точек. Если точка добавлена " +
                "осознанно — впишите её в ALLOWED вместе с причиной, почему это не " +
                "инжектируемая зависимость.",
            ALLOWED,
            found,
        )
    }

    /**
     * `var` внутри `object` или `companion object`, кроме приватных.
     *
     * Приватные исключены намеренно: они не доступны снаружи и глобальной
     * точкой доступа не являются — это внутреннее состояние синглтона.
     */
    private fun mutableStaticsIn(root: File, file: File): Sequence<String> {
        val path = file.relativeTo(root).invariantPath()
        var insideStaticScope = false
        var braceDepth = 0

        return sequence {
            for (raw in file.readLines()) {
                val line = raw.trim()
                if (!insideStaticScope &&
                    (line.startsWith("object ") || line.startsWith("companion object"))
                ) {
                    insideStaticScope = true
                    braceDepth = 0
                }
                if (insideStaticScope) {
                    braceDepth += line.count { it == '{' } - line.count { it == '}' }
                    val declaration = line.removePrefix("@Volatile").trim()
                    if (declaration.startsWith("var ") ||
                        declaration.startsWith("internal var ")
                    ) {
                        val name = declaration
                            .substringAfter("var ")
                            .substringBefore(':')
                            .substringBefore('=')
                            .trim()
                        yield("$path::$name")
                    }
                    if (braceDepth <= 0 && line.contains('}')) insideStaticScope = false
                }
            }
        }
    }

    private companion object {

        /**
         * Известные изменяемые глобальные точки. Каждая — с причиной,
         * почему она не инжектируется.
         */
        val ALLOWED = sortedSetOf(
            // Контекст для кода, до которого не дотягивается ни DI, ни Compose:
            // базовому слою класс приложения не виден.
            "common/AppContextHolder.kt::context",
            // Базовый слой умеет передавать байты, но не знает, куда их класть:
            // фабрику импортёров ставит точка сборки.
            "common/p2p/P2pReceiveManager.kt::importerFactory",
            // Та же причина со стороны отправки.
            "common/p2p/P2pSendPreparer.kt::l",
        )
    }
}
```

- [ ] **Шаг 2: Запустить сторож и привести `ALLOWED` в соответствие**

```bash
./gradlew :app:testDebugUnitTest --tests "com.client.xvideos.arch.GlobalStateTest"
```

Тест почти наверняка упадёт с первого раза и покажет фактическое множество:
проект содержит и другие `var` в `object`/`companion object`, которые задачи 1
и 2 не трогали (например, поля состояния в UI-объектах).

Разобрать каждую строку из отчёта:

- если это **точка доступа к зависимости** (кто-то снаружи её читает вместо
  инъекции) — вписать в `ALLOWED` с комментарием-причиной;
- если это **внутреннее состояние** синглтона, которое просто не помечено
  `private` — пометить `private` в исходнике, и из отчёта строка уйдёт сама;
- если это **забытый костыль** — снять его, как это сделано в задачах 1 и 2.

Повторять до зелёного. Каждое решение фиксировать комментарием: смысл сторожа в
том, что список читается как список осознанных исключений, а не как свалка.

- [ ] **Шаг 3: Дописать проверку порядка инициализации**

В тот же файл, вторым тестом:

```kotlin
    @Test
    fun `порядок инициализации в App onCreate не переставлен`() {
        val app = ProjectSources.roots()
            .map { File(it, "App.kt") }
            .firstOrNull { it.isFile }
            ?: error("Не найден App.kt")

        val text = app.readText()
        val order = REQUIRED_ORDER.map { marker ->
            marker to text.indexOf(marker)
        }

        order.forEach { (marker, at) ->
            assert(at >= 0) { "В App.onCreate пропал вызов $marker" }
        }

        val positions = order.map { it.second }
        assertEquals(
            "Порядок инициализации в App.onCreate значим и переставлен: " +
                "${order.map { it.first }}. AppBuildInfo нужен CrashLog для заголовка " +
                "падения, AppPath — Hilt-синглтонам, которые читают пути в конструкторе, " +
                "а Settings.init открывает зашифрованное хранилище уже по готовым путям.",
            positions.sorted(),
            positions,
        )
    }
```

и в `private companion object` добавить:

```kotlin
        /** Вызовы `App.onCreate`, чей относительный порядок значим. */
        val REQUIRED_ORDER = listOf(
            "AppBuildInfo.init(",
            "AppContextHolder.init(",
            "AppPath.init(",
            "CrashLog.install(",
            "Settings.init(",
        )
```

- [ ] **Шаг 4: Прогнать оба теста**

```bash
./gradlew :app:testDebugUnitTest --tests "com.client.xvideos.arch.GlobalStateTest"
```

Ожидается: PASS, 2 теста.

- [ ] **Шаг 5: Проверить, что сторож действительно ловит**

Временно переставить в `App.onCreate` строку `AppPath.init(this)` выше
`AppBuildInfo.init(...)`, прогнать тест — он обязан упасть. Вернуть порядок,
прогнать снова — зелёный. Сторож, который не падает на поломке, бесполезен.

- [ ] **Шаг 6: Коммит**

```bash
git add app/src/test/java/com/client/xvideos/arch/GlobalStateTest.kt
git commit -m "test(arch): сторож изменяемого глобального состояния и порядка старта"
```

---

## Финальная проверка

- [ ] **Полный прогон**

```bash
./gradlew test
```

- [ ] **Detekt**

```bash
./gradlew detekt
```

Учтите: на момент написания плана `detekt` уже красный на трёх
предсуществующих нарушениях (`CMPlayer2.kt`, `RedPooledVideoPlayer.kt` ×2). Их
чинит отдельная задача — важно лишь, чтобы этот план не добавил новых.

- [ ] **Сборка релиза**

```bash
./gradlew :app:assembleRelease
```

- [ ] **Проверка на устройстве**

Холодный старт приложения; переход в раздел с P2P-приёмом в первые секунды
после запуска (проверяет, что gate дожидается уборки); экран счётчика трафика.


---

## Статус исполнения

Выполнен 16.08.2026, ветка `arch/a1-a4`.

| Задача | Коммит |
| --- | --- |
| 1. `StorageCleanupGate` вместо `App.instance` | `6ef11da` |
| 2. Монитор трафика из графа | `a6a781e` |
| 3. Сторож глобального состояния и порядка | `b87f69d` |

### Отклонения от плана

1. **Сторож поймал 34 точки вместо ожидаемых трёх.** Парсер из шага 1 задачи 3
   считал глобалами локальные `var … by remember` внутри composable и поля
   анонимных `object : Интерфейс {`. Уточнён до именованных `object`, прямых
   членов (глубина ровно 1) и без `by remember` — осталось 9 реальных точек.
   Все девять внесены в `ALLOWED` с причинами: поля `AppBuildInfo`, две
   P2P-фабрики, `LSession.loginSkipped`, токен redgifs (`private set`), две
   вкладки экранов R, выбранная страна X.
2. **Дополнительно убран осиротевший импорт** `kotlinx.coroutines.Job` в `App.kt`
   и `remember` / `MutableStateFlow` в двух виджетах трафика — иначе detekt
   валился на `UnusedImports`.
3. **Проверено, что сторож ловит:** порядок в `App.onCreate` временно
   переставлен, тест упал, порядок возвращён. В плане это был шаг 5 задачи 3 —
   выполнен.
