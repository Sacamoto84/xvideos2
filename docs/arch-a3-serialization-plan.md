# A3: kotlinx.serialization на недоверенном входе

> **Для агентных исполнителей:** ОБЯЗАТЕЛЬНЫЙ СУБ-НАВЫК: используйте
> `superpowers:subagent-driven-development` (рекомендуется) или
> `superpowers:executing-plans`. Шаги размечены чекбоксами (`- [ ]`).

**Цель:** убрать класс дефектов «Gson молча кладёт `null` в non-null поле» там,
где данные приходят снаружи и им нельзя доверять, — начиная с P2P-манифеста
чужого устройства. Хранилище на диске остаётся на Gson: формат уже записан у
пользователей, и его миграция — отдельная задача с обратной совместимостью
чтения.

**Архитектура:** Gson не вызывает конструкторы Kotlin и не смотрит на
нуллабельность — объект создаётся через `Unsafe.allocateInstance`, поля
заполняются рефлексией. Отсутствующее в JSON поле остаётся `null` в non-null
типе, и падение случается позже, вдали от разбора. Проект уже платит за это
вручную: `P2pManifestCodec.fromJson` руками проверяет каждое поле через
`require`, а `feature-r` держит `sanitizeOrNull` / `sanitizeGifsInfoList`,
которые вызываются в десяти местах. kotlinx.serialization ловит то же самое на
уровне компилятора и разбора.

Мигрируем не всё: 48 файлов с Gson против 3 с kotlinx — полный переход задел бы
модели хранилища, а значит потребовал бы слоя обратной совместимости чтения,
иначе у пользователей пропадут сохранённые данные. Точечно берётся вход,
которому нельзя доверять по определению.

**Стек:** Kotlin, kotlinx.serialization (плагин + `kotlinx-serialization-json`),
Gson (остаётся на хранилище), JUnit4.

**Радиус:** A1, A2, A4 — отдельные планы. Модели хранилища (`FileDB`,
`CollectionDB`, `Settings`, кеши) **не трогаем**. Ktor-клиент не переводится на
другой конвертер — только исправляется имя алиаса, которое сейчас врёт.

---

## Порядок задач

| Задача | Что | Риск |
| --- | --- | --- |
| 1. Честный алиас в version catalog | имя говорит, что подключено | нулевой |
| 2. `P2pManifest` на kotlinx | вход с чужого устройства | низкий |
| 3. Сторож: в `common/p2p` нет Gson | не даёт откатиться | нулевой |
| 4. R-модели сети (следующий шаг) | самый крупный кусок, вынесен отдельно | средний |

Задача 1 независима. Задача 3 идёт после 2. Задача 4 описана как следующий шаг,
а не как готовые к исполнению шаги — её объём требует своего плана.

---

## Файловая структура

**Меняются:**

- `gradle/libs.versions.toml:161` — алиас `ktor-serialization-kotlinx-json`
  переименовывается в честный.
- `core/build.gradle` — подключается плагин `kotlin.serialization`, правится
  ссылка на переименованный алиас.
- `core/src/main/java/com/client/xvideos/common/p2p/P2pManifest.kt` — модели на
  `@Serializable`, кодек на `Json`.
- `core/src/test/java/com/client/xvideos/common/p2p/P2pManifestCodecTest.kt` —
  тесты приводятся к новому поведению.

**Создаётся:**

- `app/src/test/java/com/client/xvideos/arch/UntrustedInputSerializationTest.kt`
  — сторож.

---

### Задача 1: Алиас в version catalog не должен врать

В `gradle/libs.versions.toml:161`:

```toml
ktor-serialization-kotlinx-json = { module = "io.ktor:ktor-serialization-gson", version.ref = "ktor" }
```

Имя говорит «kotlinx-json», модуль — **gson**. `core/build.gradle:117`
подключает `api(libs.ktor.serialization.kotlinx.json)` в полной уверенности,
что в проекте Ktor + kotlinx, — а получает Ktor + Gson. Это ловушка ровно того
рода, из-за которой A3 вообще появилось: следующий разработчик увидит имя и
решит, что сеть уже на kotlinx.

Переключать сам конвертер здесь **не будем**: это поменяло бы разбор всех
Ktor-ответов разом. Задача — чтобы имя перестало врать.

**Файлы:**
- Изменить: `gradle/libs.versions.toml:161`
- Изменить: `core/build.gradle:117`

- [ ] **Шаг 1: Переименовать алиас**

В `gradle/libs.versions.toml` заменить строку:

```toml
ktor-serialization-kotlinx-json = { module = "io.ktor:ktor-serialization-gson", version.ref = "ktor" }
```

на

```toml
# Именно gson, а не kotlinx: конвертер Ktor-клиента исторически на Gson.
# Алиас раньше назывался ktor-serialization-kotlinx-json и вводил в
# заблуждение — код подключал его, думая, что сеть на kotlinx.
ktor-serialization-gson = { module = "io.ktor:ktor-serialization-gson", version.ref = "ktor" }
```

- [ ] **Шаг 2: Поправить ссылку**

В `core/build.gradle:117` заменить:

```groovy
    api(libs.ktor.serialization.kotlinx.json)
```

на

```groovy
    api(libs.ktor.serialization.gson)
```

- [ ] **Шаг 3: Проверить, что других ссылок нет**

```bash
grep -rn "ktor.serialization.kotlinx\|ktor-serialization-kotlinx" --include=*.gradle --include=*.toml --include=*.kts .
```

Ожидается: пусто.

- [ ] **Шаг 4: Собрать**

```bash
./gradlew :core:compileDebugKotlin
```

Ожидается: BUILD SUCCESSFUL. Ни одна зависимость фактически не поменялась —
изменилось только имя алиаса.

- [ ] **Шаг 5: Коммит**

```bash
git add gradle/libs.versions.toml core/build.gradle
git commit -m "build: алиас Ktor-конвертера больше не врёт про kotlinx"
```

---

### Задача 2: `P2pManifest` на kotlinx.serialization

Манифест приходит с чужого устройства — единственный вход в приложении, где
источник заведомо недоверенный. Сейчас `P2pManifestCodec.fromJson` компенсирует
поведение Gson вручную: три `require` и `@Suppress("SENSELESS_COMPARISON")` на
каждое поле, потому что компилятор не верит, что non-null поле может быть
`null`. kotlinx.serialization делает эту работу сам и падает на разборе, а не
позже.

Формат JSON не меняется: имена полей те же, поэтому манифест от старой версии
приложения читается новой, и наоборот.

**Файлы:**
- Изменить: `core/build.gradle` (плагин)
- Изменить: `core/src/main/java/com/client/xvideos/common/p2p/P2pManifest.kt`
- Изменить: `core/src/test/java/com/client/xvideos/common/p2p/P2pManifestCodecTest.kt`

- [ ] **Шаг 1: Подключить плагин к `:core`**

В `core/build.gradle` в блок `plugins` добавить (как это уже сделано в
`feature-x/build.gradle:7`):

```groovy
    alias(libs.plugins.kotlin.serialization)
```

Зависимость `api libs.kotlinx.serialization.json` в модуле уже есть
(`core/build.gradle:118`) — добавлять её не нужно.

- [ ] **Шаг 2: Написать тесты под новое поведение**

Заменить в `P2pManifestCodecTest` три теста, которые сейчас проверяют ручные
`require`, на проверки разбора kotlinx. Тела тестов
`неизвестный тип отвергается`, `манифест без списка файлов отвергается`,
`файл без пути отвергается` заменить на:

```kotlin
    @Test
    fun `неизвестный тип отвергается`() {
        // Так выглядит бандл из более новой версии приложения.
        val json = """{"type":"L_SOMETHING_NEW","metadataFileName":null,"files":[]}"""

        assertThrows(SerializationException::class.java) {
            P2pManifestCodec.fromJson(json)
        }
    }

    @Test
    fun `манифест без списка файлов отвергается`() {
        val json = """{"type":"L","metadataFileName":"metadata.json"}"""

        assertThrows(SerializationException::class.java) {
            P2pManifestCodec.fromJson(json)
        }
    }

    @Test
    fun `файл без пути отвергается`() {
        val json = """{"type":"L","metadataFileName":null,"files":[{"name":"a.jpg","payloadId":1,"size":2}]}"""

        assertThrows(SerializationException::class.java) {
            P2pManifestCodec.fromJson(json)
        }
    }

    @Test
    fun `пустой json отвергается`() {
        assertThrows(SerializationException::class.java) {
            P2pManifestCodec.fromJson("null")
        }
    }
```

Тесты `файл с выходом за корень отвергается` и `файл с пустым путём
отвергается` (из правки S1) **оставить без изменений**: путь по-прежнему
проверяет `normalizeRelativePath`, и он по-прежнему бросает
`IllegalArgumentException`. Тест `файл с абсолютным путём принимается, но
кладётся внутрь store` тоже остаётся.

Дописать импорт:

```kotlin
import kotlinx.serialization.SerializationException
```

- [ ] **Шаг 3: Запустить тесты и убедиться, что они падают**

```bash
./gradlew :core:testDebugUnitTest --tests "com.client.xvideos.common.p2p.P2pManifestCodecTest"
```

Ожидается: FAIL — сейчас бросается `IllegalArgumentException` / `IllegalStateException`,
а не `SerializationException`.

- [ ] **Шаг 4: Перевести модели и кодек**

Заменить `core/src/main/java/com/client/xvideos/common/p2p/P2pManifest.kt`
целиком:

```kotlin
package com.client.xvideos.common.p2p

import com.client.xvideos.common.io.normalizeRelativePath
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Описание одного файла бандла.
 *
 * @param name имя файла (для UI/логов).
 * @param relativePath путь относительно корня store (через '/'), задаёт, куда положить файл у получателя.
 * @param payloadId id Nearby-payload'а; одинаков на обоих телефонах, по нему получатель сопоставляет байты.
 * @param size размер в байтах.
 */
@Serializable
data class P2pManifestFile(
    val name: String,
    val relativePath: String,
    val payloadId: Long,
    val size: Long,
)

/**
 * Control-сообщение, которое отправитель шлёт BYTES-payload'ом после всех файлов.
 *
 * @param type источник (определяет store у получателя).
 * @param metadataFileName имя файла-метаданных среди [files] (`metadata.json` / `<id>.info`), или null.
 * @param files список файлов бандла.
 */
@Serializable
data class P2pManifest(
    val type: P2pType,
    val metadataFileName: String?,
    val files: List<P2pManifestFile>,
)

/** Сериализация манифеста для передачи BYTES-payload'ом. */
object P2pManifestCodec {

    /*
     * kotlinx, а не Gson. Манифест приходит с чужого устройства, а Gson не
     * вызывает конструкторы Kotlin и не смотрит на нуллабельность: объект
     * создаётся через Unsafe, поля заполняются рефлексией, и отсутствующее
     * поле остаётся null в non-null типе — падение случалось позже, вдали от
     * разбора. Раньше это компенсировалось руками: три require и
     * @Suppress("SENSELESS_COMPARISON") на каждое поле. Теперь проверку делает
     * сам разбор.
     *
     * ignoreUnknownKeys: бандл из более новой версии приложения может нести
     * поля, которых мы не знаем. Лишнее поле — не повод отказаться от приёма;
     * неизвестный `type` по-прежнему отказ, потому что он решает, в какое
     * хранилище лягут файлы.
     */
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun toJson(manifest: P2pManifest): String = json.encodeToString(manifest)

    /**
     * Разбирает манифест, пришедший **с чужого устройства**, и проверяет пути.
     *
     * Структуру проверяет kotlinx (отсутствующее поле, неизвестный `type` —
     * `SerializationException`). Остаётся проверка, которую библиотека сделать
     * не может: `relativePath` напрямую задаёт, куда ляжет файл, и без
     * нормализации пир кладёт `../../shared_prefs/...`. Вызывающая сторона
     * (`P2pReceiveController`) оборачивает разбор в `runCatching`.
     */
    fun fromJson(raw: String): P2pManifest {
        val parsed = json.decodeFromString<P2pManifest>(raw)
        parsed.files.forEach { file -> normalizeRelativePath(file.relativePath) }
        return parsed
    }

    fun toBytes(manifest: P2pManifest): ByteArray = toJson(manifest).toByteArray(Charsets.UTF_8)
    fun fromBytes(bytes: ByteArray): P2pManifest = fromJson(String(bytes, Charsets.UTF_8))
}
```

- [ ] **Шаг 5: Пометить `P2pType`**

`P2pType` — enum, участвующий в сериализуемой модели. В
`core/src/main/java/com/client/xvideos/common/p2p/P2pType.kt` добавить
аннотацию к объявлению enum:

```kotlin
@Serializable
enum class P2pType {
```

и импорт `kotlinx.serialization.Serializable`. Остальное тело enum не меняется.

- [ ] **Шаг 6: Запустить тесты**

```bash
./gradlew :core:testDebugUnitTest --tests "com.client.xvideos.common.p2p.*"
```

Ожидается: PASS. Проверьте, что среди прошедших есть round-trip тесты
(`json round trip preserves all fields`, `bytes round trip`, `album manifest`,
`collection manifest`) — они подтверждают, что формат на проводе не изменился.

- [ ] **Шаг 7: Проверить совместимость версий на устройстве**

Это ключевая ручная проверка задачи: **передать бандл между двумя сборками** —
старой (с Gson-манифестом) и новой. Форматы должны быть взаимно читаемы, потому
что имена полей и структура JSON не менялись.

1. Собрать debug **до** правки, поставить на устройство A.
2. Собрать debug **после** правки, поставить на устройство B.
3. Отправить коллекцию L с A на B — приём должен пройти.
4. Отправить обратно с B на A — тоже.

```bash
./gradlew :app:assembleDebug
```

Если шаг невыполним (нет второго устройства), это надо явно записать как
непроверенное — формат совместим по построению, но подтверждения не будет.

- [ ] **Шаг 8: Коммит**

```bash
git add core/build.gradle core/src/main/java/com/client/xvideos/common/p2p core/src/test/java/com/client/xvideos/common/p2p
git commit -m "refactor(p2p): манифест чужого устройства разбирается kotlinx, а не Gson"
```

---

### Задача 3: Сторож — в `common/p2p` не должно быть Gson

Узкий и точный сторож: он не пытается судить обо всём проекте, а фиксирует один
инвариант — приём с чужого устройства разбирается библиотекой, которая уважает
нуллабельность. Правило легко проверить текстом и невозможно нарушить случайно.

**Файлы:**
- Создать: `app/src/test/java/com/client/xvideos/arch/UntrustedInputSerializationTest.kt`

- [ ] **Шаг 1: Написать сторож**

```kotlin
package com.client.xvideos.arch

import com.client.xvideos.arch.ProjectSources.invariantPath
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Сторож: данные с чужого устройства не разбираются Gson.
 *
 * Gson не вызывает конструкторы Kotlin и не смотрит на нуллабельность —
 * отсутствующее поле остаётся `null` в non-null типе, и падение случается
 * позже, вдали от разбора. Для P2P это вход, которому нельзя доверять по
 * определению, поэтому там только kotlinx.serialization.
 *
 * Сторож намеренно узкий: он ничего не говорит про остальной проект, где Gson
 * остаётся осознанно (формат хранилища уже записан у пользователей).
 */
class UntrustedInputSerializationTest {

    @Test
    fun `пакет p2p не использует Gson`() {
        val offenders = ProjectSources.roots()
            .flatMap { root ->
                val p2p = File(root, "common/p2p")
                if (!p2p.isDirectory) {
                    emptySequence()
                } else {
                    p2p.walkTopDown()
                        .filter { it.isFile && it.extension == "kt" }
                        .filter { file -> file.readText().contains("com.google.gson") }
                        .map { it.relativeTo(root).invariantPath() }
                }
            }
            .toList()

        assertTrue(
            "Gson в приёме с чужого устройства: $offenders. Используйте " +
                "kotlinx.serialization — Gson молча кладёт null в non-null поле.",
            offenders.isEmpty(),
        )
    }
}
```

- [ ] **Шаг 2: Запустить сторож**

```bash
./gradlew :app:testDebugUnitTest --tests "com.client.xvideos.arch.UntrustedInputSerializationTest"
```

Ожидается: PASS — после задачи 2 `P2pManifest.kt` был единственным местом с
Gson в этом пакете. Если тест падает, значит в `common/p2p` есть ещё
Gson-точка: перевести её на kotlinx тем же способом.

- [ ] **Шаг 3: Проверить, что сторож ловит**

Временно вернуть `import com.google.gson.Gson` в любой файл `common/p2p`,
прогнать тест — он обязан упасть. Убрать импорт, прогнать снова — зелёный.

- [ ] **Шаг 4: Коммит**

```bash
git add app/src/test/java/com/client/xvideos/arch/UntrustedInputSerializationTest.kt
git commit -m "test(arch): сторож — приём с чужого устройства без Gson"
```

---

### Задача 4: Модели сети R и L — следующий шаг

Здесь описан объём и порядок, но не пошаговые инструкции: этот кусок крупнее
трёх предыдущих вместе взятых и заслуживает собственного плана, написанного
после того, как задачи 1-3 покажут, как миграция идёт на практике.

**Что входит:**

- `feature-r/.../model/` — `GifsInfo`, `MediaResponse`, `CreatorResponse`,
  `NichesInfo`, `NichesResponse`, `TopCreatorsResponse`, `UserInfo`, `URL1`.
  Подключить `alias(libs.plugins.kotlin.serialization)` в
  `feature-r/build.gradle`.
- `feature-l/.../model/` и `feature-l/.../net/` — `AlbumDetails`,
  `PicsDetails`, `PicsDetailsMedia`, ответы GraphQL.

**Чем это сложнее задачи 2:**

1. **`sanitizeOrNull` не исчезает целиком.** В `GifsInfo.kt:33` и `URL1.kt:16`
   лежит не только защита от `null`, но и нормализация URL. kotlinx закрывает
   первую половину, вторая остаётся — и её вызовы в десяти местах (`pagin/*`,
   `saved/*`) должны сохраниться. Разбирать построчно, что из проверки было
   компенсацией Gson, а что доменной логикой.
2. **Часть моделей одновременно хранится на диске.** `GifsInfo` пишется в
   `FileDB` (`R_Saved_Likes`) и в `CollectionDB` (`R_Saved_Collection`). Если
   модель переводится на `@Serializable`, а хранилище остаётся на Gson —
   работать будет, но это ровно та развилка, где легко потерять данные
   пользователя. Нужен явный тест round-trip через оба хранилища на реальном
   файле, записанном старой версией.
3. **Ktor-конвертер остаётся на Gson** (см. задачу 1). Пока он не переключён,
   часть ответов всё равно проходит через Gson независимо от аннотаций на
   моделях — миграция моделей не даёт полного эффекта, пока не решён конвертер.

**Рекомендуемый порядок, когда дойдут руки:** сначала модели, которые **не**
попадают в хранилище (`MediaResponse`, `CreatorResponse`, `NichesResponse`,
`TopCreatorsResponse`), — там риск потери данных нулевой. Модели, которые
хранятся (`GifsInfo`, `UserInfo`, `NichesInfo`), — последними и с тестом на
чтение файла, записанного до миграции.

---

## Финальная проверка

- [ ] **Полный прогон**

```bash
./gradlew test
```

- [ ] **Сборка релиза**

```bash
./gradlew :app:assembleRelease
```

R8 и kotlinx.serialization требуют правил обфускации; плагин поставляет их сам,
но релизная сборка — единственное место, где отсутствие правил всплывёт.

- [ ] **Проверка на устройстве**

P2P-передача между двумя сборками (см. задачу 2, шаг 7) — главный сценарий.
Дополнительно: приём коллекции L и приём лайка R на релизной сборке, чтобы
подтвердить, что R8 не съел сериализаторы.


---

## Статус исполнения

Выполнен 16.08.2026, ветка `arch/a1-a4`.

| Задача | Коммит |
| --- | --- |
| 1. Честный алиас Ktor-конвертера | `d67e6a6` |
| 2. `P2pManifest` на kotlinx | `31d8122` |
| 3. Сторож против Gson в `common/p2p` | `31d8122` |
| 4. Модели сети R и L | не начата — остаётся следующим шагом |

### Отклонения от плана

1. **Добавлены два теста совместимости формата, которых в плане не было:**
   `манифест от Gson-версии приложения читается` (разбор байт-в-байт JSON от
   прежней реализации) и `наш манифест читается разбором без строгих полей`
   (точное сравнение выдачи, подтверждает что `encodeDefaults` держит
   `metadataFileName: null` явным).

   Это заменяет шаг 7 задачи 2 — ручную передачу бандла между старой и новой
   сборкой на двух устройствах. Тесты надёжнее: они выполняются на каждой
   сборке и не требуют второго телефона. Ручной сценарий можно не выполнять.
2. **Проверено, что сторож ловит:** Gson временно возвращён в `P2pStaging.kt`,
   тест упал, импорт убран.
3. **Релиз с R8 собран** — сериализаторы kotlinx пережили обфускацию, правила
   плагина работают.
