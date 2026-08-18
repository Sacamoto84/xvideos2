# A2: подтянуть `CollectionDB` до надёжности `FileDB`

> **Для агентных исполнителей:** ОБЯЗАТЕЛЬНЫЙ СУБ-НАВЫК: используйте
> `superpowers:subagent-driven-development` (рекомендуется) или
> `superpowers:executing-plans`. Шаги размечены чекбоксами (`- [ ]`).

**Цель:** снять разницу в надёжности между двумя файловыми хранилищами, не
сливая их: `CollectionDB` получает сериализацию операций и уборку временных
файлов, которые `FileDB` уже имеет, а разница в назначении фиксируется в KDoc,
чтобы её больше не приняли за случайное дублирование.

**Архитектура:** ревью 2026-08-16 назвало `CollectionDB` дубликатом `FileDB` —
это неверно, и план исходит из уточнённой картины. Классы решают разные задачи:
`FileDB` хранит **плоский** список файлов в одной папке (6 инстансов: альбомы L,
creators / likes / niches / subscriptions R, favorites X), `CollectionDB` —
**вложенный**: папки-коллекции, внутри каждой файлы элементов (боевой путь
R-коллекций через `LinkCollectionStore` → `R_Saved_Collection`). Слияние в один
класс задело бы все семь мест создания и боевой путь пользовательских данных
ради небольшой экономии кода; вместо этого выравнивается надёжность.

Что у `CollectionDB` уже есть после правок ревью: атомарная запись (C1) и
валидация имени коллекции (S2). Чего нет и что добавляет этот план: лока
сериализации операций, уборки `.tmp` от прерванной записи, упорядоченной
публикации списка.

**Стек:** Kotlin, Gson, Compose Runtime (`mutableStateListOf`), JUnit4 +
`TemporaryFolder`.

**Радиус:** A1, A3, A4 — отдельные планы. Формат данных на диске **не
меняется**: ни имена файлов, ни структура папок, ни содержимое JSON. Любая
правка, которая потребовала бы миграции пользовательских данных, в этот план не
входит.

---

## Порядок задач

| Задача | Что |
| --- | --- |
| 1. Лок и уборка `.tmp` в `CollectionDB` | сериализация операций, чистка мусора |
| 2. Упорядоченная публикация в `LinkCollectionStore` | устаревший результат не ложится поверх свежего |
| 3. KDoc обоих хранилищ | зафиксировать, что это не дубликаты |

Задачи независимы, но 3 идёт последней — она описывает уже итоговое состояние.

---

## Файловая структура

**Меняются:**

- `core/src/main/java/com/client/xvideos/common/collectionDB/CollectionDB.kt` —
  лок, уборка `.tmp`, KDoc.
- `core/src/main/java/com/client/xvideos/common/collectionDB/model/LinkCollectionStore.kt`
  — упорядоченная публикация.
- `core/src/main/java/com/client/xvideos/common/fileDB/FileDB.kt` — KDoc
  (ссылка на соседнее хранилище).
- `core/src/test/java/com/client/xvideos/common/collectionDB/CollectionDBTest.kt`
  — дописываются тесты.

---

### Задача 1: Лок и уборка временных файлов

`FileDB` сериализует операции с каталогом через `synchronized(lock)` и
подчищает `.tmp` от прерванной записи в `refresh()`. У `CollectionDB` нет ни
того, ни другого: после атомарной записи (C1) обрыв процесса между
`writeText` и `renameTo` оставляет `<id>.collection.tmp` навсегда — на чтение
он не влияет (`readAllCollections` фильтрует по расширению `collection`), но
копится в папке пользователя.

**Файлы:**
- Изменить: `core/src/main/java/com/client/xvideos/common/collectionDB/CollectionDB.kt`
- Изменить: `core/src/test/java/com/client/xvideos/common/collectionDB/CollectionDBTest.kt`

- [ ] **Шаг 1: Написать падающие тесты**

В конец класса `CollectionDBTest` добавить:

```kotlin
    @Test
    fun `readAllCollections подчищает временные файлы от прерванной записи`() {
        val root = tmp.newFolder("collections5")
        val db = db(root)
        db.insert("ok", "К", TestItem("ok", "https://x/ok"))
        // Так выглядит папка после обрыва процесса посреди writeTextAtomically.
        File(root, "К/битый.collection.tmp").writeText("{\"id\":\"би")

        db.readAllCollections().getOrThrow()

        val leftovers = File(root, "К").listFiles()?.map { it.name }.orEmpty()
        assertEquals(listOf("ok.collection"), leftovers)
    }

    @Test
    fun `параллельные insert и readAllCollections не мешают друг другу`() {
        val root = tmp.newFolder("collections6")
        val db = db(root)
        db.insert("seed", "К", TestItem("seed", "https://x/seed"))

        val start = java.util.concurrent.CountDownLatch(1)
        val failures = java.util.Collections.synchronizedList(mutableListOf<String>())

        val writer = Thread {
            start.await()
            repeat(200) { i -> db.insert("item$i", "К", TestItem("item$i", "https://x/$i")) }
        }
        val reader = Thread {
            start.await()
            repeat(200) {
                val result = db.readAllCollections()
                if (result.isFailure) failures += result.exceptionOrNull()?.toString().orEmpty()
            }
        }

        writer.start(); reader.start(); start.countDown()
        writer.join(30_000); reader.join(30_000)

        assertEquals("чтение не должно падать на параллельной записи: $failures", 0, failures.size)
    }
```

- [ ] **Шаг 2: Запустить тесты и посмотреть результат**

```bash
./gradlew :core:testDebugUnitTest --tests "com.client.xvideos.common.collectionDB.CollectionDBTest"
```

Ожидается: падает тест про уборку `.tmp` (её нет). Тест про параллельность
может пройти или падать нестабильно — после правки он станет
детерминированным.

- [ ] **Шаг 3: Добавить лок и уборку**

В `CollectionDB.kt` сразу после поля `gson` добавить:

```kotlin
    /**
     * Сериализует операции с каталогом — тот же контракт, что у
     * [com.client.xvideos.common.fileDB.FileDB]: две параллельные записи не
     * переплетаются, а чтение не видит окна между `delete()` и `renameTo()`
     * в фолбэк-ветке атомарной записи.
     */
    private val lock = Any()

    /** Расширение временного файла из [com.client.xvideos.common.io.writeTextAtomically]. */
    private val tempSuffix = ".collection.tmp"
```

Обернуть тела `create`, `deleteCollection`, `renameCollection`, `deleteItem`,
`insert` и `readAllCollections` в `synchronized(lock) { … }` — внутри
существующего `try` / `runCatching`, чтобы обработка ошибок не менялась.

Например, `insert` принимает вид:

```kotlin
    fun insert(name: String, collectionName: String, item: T): Result<Boolean> {
        return try {
            val safeName = CollectionName.normalizeOrNull(collectionName)
                ?: return Result.failure(IOException("Недопустимое имя коллекции: $collectionName"))
            Timber.i("!!! сохранить лайк GIFS -> likesItem() name:${name}")

            synchronized(lock) {
                val dir = File(path, safeName)

                if (!dir.exists()) {
                    val created = dir.mkdirs()
                    if (!created) { return Result.failure(IOException("Не удалось создать директорию: ${dir.absolutePath}")) }
                }

                val likesFile = File(dir, "${name}.collection")

                // Атомарно: обрыв процесса посреди writeText оставлял обрезанный
                // JSON, а readAllCollections молча выбрасывает такой файл через
                // mapNotNull — элемент пропадал без следа в логах.
                likesFile.writeTextAtomically(gson.toJson(item))
            }
            Result.success(true)
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при сохранении лайка GIF")
            Result.failure(e)
        }
    }
```

В `readAllCollections` внутри лока, до обхода папок, добавить уборку:

```kotlin
            cleanupTempFiles(root)
```

и приватный метод в конец класса:

```kotlin
    /**
     * Подчищает `.tmp`, оставшиеся от прерванной записи.
     *
     * На чтение они не влияют — фильтр идёт по расширению `collection`, — но
     * копятся в папке пользователя. `FileDB` делает то же самое в `refresh()`.
     */
    private fun cleanupTempFiles(root: File) {
        runCatching {
            root.listFiles { f -> f.isDirectory }?.forEach { dir ->
                dir.listFiles { f -> f.isFile && f.name.endsWith(tempSuffix) }
                    ?.forEach { it.delete() }
            }
        }
    }
```

- [ ] **Шаг 4: Запустить тесты**

```bash
./gradlew :core:testDebugUnitTest --tests "com.client.xvideos.common.collectionDB.CollectionDBTest"
```

Ожидается: PASS, 6 тестов.

- [ ] **Шаг 5: Коммит**

```bash
git add core/src/main/java/com/client/xvideos/common/collectionDB/CollectionDB.kt core/src/test/java/com/client/xvideos/common/collectionDB/CollectionDBTest.kt
git commit -m "fix(core): операции CollectionDB сериализованы, временные файлы подчищаются"
```

---

### Задача 2: Упорядоченная публикация списка коллекций

`FileDB.refresh()` после правки C2 публикует результат по номеру загрузки:
устаревший результат не ложится поверх свежего. `LinkCollectionStore` и его
наследник `R_Saved_Collection` публикуют в `collectionList` без всякого
упорядочивания — два параллельных `refreshCollectionList()` могут разложиться в
обратном порядке.

Публикация, как и в `FileDB`, остаётся вне основного лока: `replaceWith` берёт
снапшот-лок Compose, и вложение `lock -> snapshotLock` дало бы обратный порядок
захвата относительно кода, который зовёт хранилище из-под снапшота.

**Файлы:**
- Изменить: `core/src/main/java/com/client/xvideos/common/collectionDB/model/LinkCollectionStore.kt`

- [ ] **Шаг 1: Добавить упорядочивание в базовый класс**

В `LinkCollectionStore` добавить импорт:

```kotlin
import java.util.concurrent.atomic.AtomicLong
```

и рядом с `collectionList`:

```kotlin
    /**
     * Номер загрузки и последний опубликованный номер — тот же приём, что в
     * [com.client.xvideos.common.fileDB.FileDB]. Наследник обязан брать номер
     * [nextLoadSeq] до чтения диска и публиковать через [publish]: иначе два
     * параллельных refresh разложатся в порядке завершения, а не запуска.
     */
    private val loadSeq = AtomicLong(0)
    private val publishLock = Any()
    private var publishedSeq = 0L

    /** Номер очередной загрузки. Берётся до чтения диска. */
    protected fun nextLoadSeq(): Long = loadSeq.incrementAndGet()

    /** Публикует результат загрузки [seq], если он не устарел. */
    protected fun publish(seq: Long, items: List<CollectionEntity<T>>) {
        synchronized(publishLock) {
            if (seq > publishedSeq) {
                publishedSeq = seq
                collectionList.replaceWith(items)
            }
        }
    }
```

и импорт `replaceWith`:

```kotlin
import com.client.xvideos.common.util.replaceWith
```

- [ ] **Шаг 2: Перевести `R_Saved_Collection` на публикацию по номеру**

В `feature-r/src/main/java/com/client/xvideos/r/common/saved/R_Saved_Collection.kt`
в `refreshCollectionList()` взять номер до чтения и публиковать через
`publish`:

```kotlin
    override fun refreshCollectionList() {
        val seq = nextLoadSeq()
        val a = collectionDb.readAllCollections()
        if (a.isSuccess) {
            publish(
                seq,
                a.getOrThrow().map { collection ->
                    collection.copy(items = collection.items.sanitizeGifsInfoList())
                },
            )
        }
    }
```

Остальную часть метода (ветку `else` и всё, что идёт после) сохранить как
есть — план меняет только способ публикации.

- [ ] **Шаг 3: Собрать и прогнать тесты**

```bash
./gradlew :core:testDebugUnitTest :feature-r:testDebugUnitTest
```

Ожидается: BUILD SUCCESSFUL. Существующий
`feature-r/src/test/.../RFeedSessionStoreTest.kt` и прочие тесты R должны
остаться зелёными.

- [ ] **Шаг 4: Коммит**

```bash
git add core/src/main/java/com/client/xvideos/common/collectionDB/model/LinkCollectionStore.kt feature-r/src/main/java/com/client/xvideos/r/common/saved/R_Saved_Collection.kt
git commit -m "fix(core): публикация списка коллекций упорядочена по номеру загрузки"
```

---

### Задача 3: Зафиксировать, что это не дубликаты

Ревью приняло два хранилища за случайное дублирование. Чтобы следующее ревью
(и следующий разработчик) не начинали с той же ошибки, разница пишется в KDoc
обоих классов — со ссылками друг на друга.

**Файлы:**
- Изменить: `core/src/main/java/com/client/xvideos/common/fileDB/FileDB.kt`
- Изменить: `core/src/main/java/com/client/xvideos/common/collectionDB/CollectionDB.kt`

- [ ] **Шаг 1: KDoc `FileDB`**

Дописать в конец существующего KDoc класса `FileDB`:

```kotlin
 *
 * Хранит **плоский** список: файлы `<имя>.<extension>` в одной папке. Для
 * вложенного случая — папки-коллекции, внутри каждой файлы элементов — есть
 * соседний [com.client.xvideos.common.collectionDB.CollectionDB]. Это не
 * дубликат: разный уровень вложенности и разный жизненный цикл элементов.
 * Держите надёжность обоих в одном состоянии — атомарная запись, лок операций,
 * уборка `.tmp`.
```

- [ ] **Шаг 2: KDoc `CollectionDB`**

Заменить пустой KDoc класса (`/**\n *\n */`) на:

```kotlin
/**
 * Хранилище **вложенных** коллекций: `<path>/<коллекция>/<id>.collection`.
 *
 * Отличие от [com.client.xvideos.common.fileDB.FileDB] — уровень вложенности:
 * там плоский список файлов в одной папке, здесь папка на коллекцию и файлы
 * элементов внутри. Ревью 2026-08-16 приняло это за случайное дублирование;
 * это не так, слияние задело бы боевой путь R-коллекций ради небольшой
 * экономии кода.
 *
 * Общий контракт надёжности с `FileDB`: имя коллекции проверяется
 * ([CollectionName]), запись атомарна
 * ([com.client.xvideos.common.io.writeTextAtomically]), операции с каталогом
 * сериализованы локом, `.tmp` от прерванной записи подчищаются на чтении.
 * Элемент, который не разобрался, молча пропускается — обрезанный JSON не
 * должен ронять весь список.
 */
```

- [ ] **Шаг 3: Собрать**

```bash
./gradlew :core:compileDebugKotlin
```

- [ ] **Шаг 4: Коммит**

```bash
git add core/src/main/java/com/client/xvideos/common/fileDB/FileDB.kt core/src/main/java/com/client/xvideos/common/collectionDB/CollectionDB.kt
git commit -m "docs(core): FileDB и CollectionDB — разный уровень вложенности, не дубликаты"
```

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

- [ ] **Проверка на устройстве**

Формат данных не менялся, поэтому главное — регрессия на существующих данных:
открыть R → Saved → Collections, убедиться, что коллекции и их содержимое на
месте; создать коллекцию, добавить элемент, удалить элемент, переименовать
коллекцию, удалить коллекцию. После каждого действия список обновляется.
