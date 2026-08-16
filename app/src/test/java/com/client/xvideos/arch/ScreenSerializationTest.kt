package com.client.xvideos.arch

import com.client.xvideos.arch.ProjectSources.roots
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Сторож: экраны Voyager обязаны целиком сериализоваться.
 *
 * `cafe.adriel.voyager.core.screen.Screen` на Android объявлен как
 * `interface Screen : Serializable`, а `SnapshotStateStack` сохраняет стек через
 * `listSaver(save = { stack -> stack.items })` — то есть кладёт в saved state
 * **сами объекты экранов**. Когда система парселит saved state активити, экран
 * уходит через `Parcel.writeSerializable`, Java-сериализация обходит его поля, и
 * первое несериализуемое роняет приложение:
 *
 * ```
 * android.os.BadParcelableException: Parcelable encountered IOException writing
 *   serializable object (name = ...ScreenRedFullScreen)
 * Caused by: java.io.NotSerializableException: ...model.GifsInfo
 * ```
 *
 * Ошибка в этом проекте срабатывала трижды — `P2pSendSource`, `GifsInfo`,
 * `AlbumListFilter` — и каждый раз чинилась поимённо. Сторож закрывает класс
 * ошибки целиком: новый экран с обычной data class в конструкторе валит сборку,
 * а не воспроизводится на устройстве через раз.
 *
 * **Проверяется транзитивно:** от параметров конструктора экрана вглубь, по
 * параметрам конструкторов встреченных типов проекта. Так ловится и
 * `FilterGenre` внутри `AlbumListFilter`, а не только верхний тип.
 *
 * **Чего сторож не видит:** типы не из `com.client.xvideos` (`String`, `List`,
 * библиотечные модели). Они в счёт не идут — иначе пришлось бы держать список
 * сериализуемости всего внешнего мира. Для них остаются точечные тесты вроде
 * `ScreenRedFullScreenSerializationTest`.
 */
class ScreenSerializationTest {

    @Test
    fun `поля экранов Voyager сериализуются`() {
        val declarations = declarations()
        // Parcelable-экраны пропускаем намеренно: в `Parcel.writeValue` Parcelable
        // проверяется раньше Serializable, то есть такой экран уходит в saved state
        // через Parcelable и до Java-сериализации дело не доходит. А поля
        // `@Parcelize` уже проверяет компиляторный плагин — второй сторож поверх
        // него только дублировал бы диагностику. Так живёт L_FullScreenImage.
        val screens = declarations.values.flatten().filter { it.isScreen && !it.isParcelable }

        assertTrue(
            "Не нашлось ни одного экрана — сторож перестал что-либо проверять",
            screens.size >= 10
        )

        val problems = mutableListOf<String>()
        screens.forEach { screen ->
            walk(screen, declarations, path = listOf(screen.name), problems = problems)
        }

        assertTrue(
            buildString {
                appendLine("Экраны Voyager уходят в saved state целиком, а эти их поля не сериализуются.")
                appendLine("Пометьте тип `: Serializable` — как уже сделано у GifsInfo, AlbumListFilter, P2pSendSource.")
                appendLine()
                problems.sorted().forEach { appendLine("  $it") }
            },
            problems.isEmpty()
        )
    }

    /**
     * Обход вглубь по типам проекта.
     *
     * `visited` не нужен отдельным параметром: путь и так несёт всю цепочку, а
     * циклы в моделях (тип, ссылающийся на себя) обрываются проверкой вхождения
     * в текущий путь.
     */
    private fun walk(
        decl: Decl,
        declarations: Map<String, List<Decl>>,
        path: List<String>,
        problems: MutableList<String>,
    ) {
        referencedProjectTypes(decl.constructor, declarations).forEach { name ->
            if (name in path) return@forEach
            val targets = declarations[name] ?: return@forEach
            targets.forEach { target ->
                if (!target.isSerializationSafe) {
                    problems += (path + name).joinToString(" → ")
                    return@forEach
                }
                walk(target, declarations, path + name, problems)
            }
        }
    }

    /**
     * Имена типов проекта, упомянутые в блоке конструктора.
     *
     * Берём все опознанные идентификаторы, а не разбираем параметры по одному:
     * разбор с учётом дефолтных значений, дженериков и аннотаций — это
     * пересказ парсера Kotlin, а лишний найденный тип сторожу не вредит.
     * Пересечение с объявлениями проекта само отсеивает и `String`, и
     * `SerializedName`, и всё библиотечное. Заодно так видны аргументы
     * дженериков: `List<FilterGenre>` даёт `FilterGenre`.
     */
    private fun referencedProjectTypes(
        constructor: String,
        declarations: Map<String, List<Decl>>,
    ): Set<String> =
        IDENTIFIER.findAll(constructor)
            .map { it.value }
            .filter { it in declarations }
            .toSet()

    private fun declarations(): Map<String, List<Decl>> =
        roots()
            .flatMap { it.walkTopDown() }
            .filter { it.isFile && it.extension == "kt" }
            .flatMap { parse(it.readText()) }
            .groupBy { it.name }

    /** Объявление типа: что за тип, сериализуем ли он, что у него в конструкторе. */
    private data class Decl(
        val name: String,
        val isEnum: Boolean,
        val constructor: String,
        val supertypes: String,
    ) {
        val isScreen: Boolean = SCREEN.containsMatchIn(supertypes)

        val isParcelable: Boolean = PARCELABLE.containsMatchIn(supertypes)

        /**
         * Перечисления сериализуются сами — `java.lang.Enum implements Serializable`.
         *
         * `Parcelable` здесь не в счёт, и это не упущение: Java-сериализация о нём
         * не знает. Внутри Serializable-экрана Parcelable-поле упадёт так же, как
         * обычная data class.
         */
        val isSerializationSafe: Boolean = isEnum || SERIALIZABLE.containsMatchIn(supertypes)
    }

    private fun parse(text: String): List<Decl> = DECLARATION.findAll(text).map { match ->
        var i = match.range.last + 1
        i = skipGenerics(text, i)
        val constructor = if (i < text.length && text[i] == '(') {
            val end = matchingParen(text, i)
            // Комментарии режем сразу: KDoc параметра ссылается на соседние типы
            // (`[LFullScreenPayload]`), и без чистки сторож принимал упоминание в
            // документации за поле экрана.
            val body = text.substring(i + 1, end).replace(COMMENT, " ")
            i = end + 1
            body
        } else {
            ""
        }
        Decl(
            name = match.groupValues[2],
            isEnum = match.groupValues[1].startsWith("enum"),
            constructor = constructor,
            supertypes = supertypeWindow(text, i),
        )
    }.toList()

    /** Пропустить `<...>` у обобщённого объявления, чтобы не принять их за конструктор. */
    private fun skipGenerics(text: String, from: Int): Int {
        var i = from
        while (i < text.length && text[i].isWhitespace()) i++
        if (i >= text.length || text[i] != '<') return i
        var depth = 0
        while (i < text.length) {
            when (text[i]) {
                '<' -> depth++
                '>' -> if (--depth == 0) return i + 1
            }
            i++
        }
        return i
    }

    private fun matchingParen(text: String, open: Int): Int {
        var depth = 0
        var i = open
        while (i < text.length) {
            when (text[i]) {
                '(' -> depth++
                ')' -> if (--depth == 0) return i
            }
            i++
        }
        return text.length - 1
    }

    /**
     * Список супертипов: от конца конструктора до тела класса.
     *
     * Режем по первой `{` либо по пустой строке — второе нужно типам без тела
     * (`data class URL1(...) : Serializable`, дальше в файле функции-расширения).
     * Без этого окно утекло бы в соседнее объявление и сторож увидел бы чужую
     * сериализуемость.
     */
    private fun supertypeWindow(text: String, from: Int): String {
        val brace = text.indexOf('{', from).takeIf { it >= 0 } ?: text.length
        val blankLine = BLANK_LINE.find(text, from)?.range?.first ?: text.length
        return text.substring(from, minOf(brace, blankLine, text.length))
    }

    private companion object {
        val DECLARATION = Regex(
            """(?m)^[ \t]*(?:(?:public|internal|private|protected|abstract|sealed|open|data|value|inner|annotation)[ \t]+)*(enum[ \t]+class|class|object|interface)[ \t]+([A-Za-z_]\w*)"""
        )
        val IDENTIFIER = Regex("""\b[A-Z]\w*\b""")
        val SCREEN = Regex("""\bScreen\b""")
        val SERIALIZABLE = Regex("""\bSerializable\b""")
        val PARCELABLE = Regex("""\bParcelable\b""")
        val COMMENT = Regex("""/\*.*?\*/|//[^\r\n]*""", RegexOption.DOT_MATCHES_ALL)
        val BLANK_LINE = Regex("""\r?\n[ \t]*\r?\n""")
    }
}
