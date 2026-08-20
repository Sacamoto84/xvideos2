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

    @Test
    fun `порядок инициализации в App onCreate не переставлен`() {
        val app = ProjectSources.roots()
            .map { File(it, "App.kt") }
            .firstOrNull { it.isFile }
            ?: error("Не найден App.kt")

        val text = app.readText()
        val order = REQUIRED_ORDER.map { marker -> marker to text.indexOf(marker) }

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

    /**
     * Непубличные `var` — прямые члены именованного `object` или
     * `companion object`.
     *
     * Три ограничения, каждое отсекает свой класс ложных срабатываний:
     *
     * - **именованный** object: `object : SomeInterface {` — анонимный объект
     *   внутри класса, его поля глобальными точками не являются;
     * - **прямой член** (глубина ровно 1 от открывающей скобки): иначе сюда
     *   попадают локальные `var` внутри функций синглтона — счётчики в
     *   `XlrBackupManager` и подобное;
     * - **без `by remember`**: это состояние композиции, а не процесса.
     */
    private fun mutableStaticsIn(root: File, file: File): Sequence<String> {
        val path = file.relativeTo(root).invariantPath()
        var insideStaticScope = false
        var braceDepth = 0

        return sequence {
            for (raw in file.readLines()) {
                val line = raw.trim()
                if (!insideStaticScope && NAMED_STATIC_SCOPE.containsMatchIn(line)) {
                    insideStaticScope = true
                    braceDepth = 0
                }
                if (!insideStaticScope) continue

                val depthBefore = braceDepth
                braceDepth += line.count { it == '{' } - line.count { it == '}' }

                val declaration = line.removePrefix("@Volatile").trim()
                val isDeclaration = declaration.startsWith("var ") ||
                    declaration.startsWith("internal var ")
                // depthBefore, а не braceDepth: `var x = mutableStateOf(0)` со
                // скобкой на той же строке иначе выпадает из подсчёта.
                if (isDeclaration && depthBefore == 1 && !declaration.contains("by remember")) {
                    val name = declaration
                        .substringAfter("var ")
                        .substringBefore(':')
                        .substringBefore('=')
                        .substringBefore(" by ")
                        .trim()
                    yield("$path::$name")
                }

                if (braceDepth <= 0 && line.contains('}')) insideStaticScope = false
            }
        }
    }

    private companion object {

        /** Именованный `object Foo {` или `companion object [Имя] {`, но не `object : Интерфейс {`. */
        val NAMED_STATIC_SCOPE = Regex("""^(private |internal |public )?(companion )?object\s+[A-Za-z_]|^(private |internal |public )?companion object\s*\{""")

        /**
         * Известные изменяемые глобальные точки. Каждая — с причиной,
         * почему она не инжектируется.
         */
        val ALLOWED = sortedSetOf(
            // BuildConfig генерируется на модуль: у :core он свой, полей
            // приложения там нет. Точка сборки публикует их сюда.
            "common/AppBuildInfo.kt::debug",
            "common/AppBuildInfo.kt::versionName",
            // Базовый слой умеет передавать байты, но не знает, куда их класть:
            // фабрику импортёров ставит точка сборки.
            "common/p2p/P2pReceiveManager.kt::importerFactory",
            // Та же причина со стороны отправки.
            "common/p2p/P2pSendPreparer.kt::l",
            // Сессионное состояние процесса: «пропустил логин» живёт до
            // перезапуска и не принадлежит ни одному экрану.
            "l/LSession.kt::loginSkipped",
            // Анонимный токен redgifs. Запись закрыта (`private set`) и идёт
            // под мьютексом; снаружи доступно только чтение.
            "r/network/http/ApiClient.kt::bearerToken",
            // Выбранная вкладка экрана, переживающая уход из композиции.
            "r/ui/explorer/ScreenExplorer.kt::screenType",
            "r/ui/explorer/tab/saved/ScreenSaved.kt::screenType",
            // Выбранная страна: глобальна по смыслу, раньше была двумя
            // разрозненными top-level переменными.
            "x/feature/country/country.kt::current",
        )

        /**
         * Вызовы `App.onCreate`, чей относительный порядок значим.
         *
         * `AppBuildInfo` нужен `CrashLog` для заголовка падения, `AppPath` —
         * Hilt-синглтонам, которые читают пути в конструкторе, а `Settings.init`
         * открывает зашифрованное хранилище уже по готовым путям.
         */
        val REQUIRED_ORDER = listOf(
            "AppBuildInfo.init(",
            "AppContextHolder.init(",
            "AppPath.init(",
            "CrashLog.install(",
            "Settings.init(",
        )
    }
}
