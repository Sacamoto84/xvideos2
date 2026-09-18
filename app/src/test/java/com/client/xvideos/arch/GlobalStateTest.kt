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
                "${order.map { it.first }}. AppPath нужен " +
                "Hilt-синглтонам, которые читают пути в конструкторе, " +
                "а Settings.init открывает зашифрованное хранилище уже по готовым путям.",
            positions.sorted(),
            positions,
        )
    }

    /**
     * Изменяемые `var` (включая `private var`, `lateinit var`, `internal var`) —
     * прямые члены именованного `object` или `companion object`.
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
                if (line.startsWith("//") || line.startsWith("*") || line.startsWith("/*")) continue

                if (!insideStaticScope && NAMED_STATIC_SCOPE.containsMatchIn(line)) {
                    insideStaticScope = true
                    braceDepth = 0
                }
                if (!insideStaticScope) continue

                val depthBefore = braceDepth
                braceDepth += line.count { it == '{' } - line.count { it == '}' }

                if (depthBefore == 1 && !line.contains("by remember")) {
                    VAR_DECLARATION.find(line)?.let { match ->
                        val name = match.groupValues[1].removeSurrounding("`")
                        yield("$path::$name")
                    }
                }

                if (braceDepth <= 0 && line.contains('}')) insideStaticScope = false
            }
        }
    }

    private companion object {

        /** Именованный `object Foo {` или `companion object [Имя] {`, но не `object : Интерфейс {`. */
        val NAMED_STATIC_SCOPE = Regex("""^(private |internal |public )?(companion )?object\s+[A-Za-z_]|^(private |internal |public )?companion object\s*\{""")

        val VAR_DECLARATION = Regex(
            """^(?:@\w+(?:\([^)]*\))?\s+)*(?:(?:private|internal|public|protected|lateinit|open|final|override)\s+)*var\s+(`[^`]+`|[A-Za-z0-9_]+)"""
        )

        /**
         * Известные изменяемые глобальные точки. Каждая — с причиной,
         * почему она не инжектируется.
         */
        val ALLOWED = sortedSetOf(
            // BuildConfig генерируется на модуль: у :core он свой, полей
            // приложения там нет. Точка сборки публикует их сюда.
            "common/AppBuildInfo.kt::debug",
            "common/AppBuildInfo.kt::versionName",
            // Application-контекст для базового слоя, до которого не дотягивается DI.
            "common/AppContextHolder.kt::context",
            // Монотонное время ухода приложения в фон для автоблокировки по таймеру.
            "common/applock/AppLockSession.kt::lastBackgroundElapsedMs",
            // Сессионный признак разблокировки приложения (биометрия/пин-код).
            "common/applock/AppLockSession.kt::unlocked",
            // Временный cache для шаринга Luscious (в cacheDir, вне бэкапа).
            "common/AppPath.kt::l_cacheDownload",
            // Входящие файлы P2P Nearby (в cacheDir, вне бэкапа).
            "common/AppPath.kt::p2p_nearbyCache",
            // Кеш ниш Red (в filesDir, вне бэкапа).
            "common/AppPath.kt::r_nichesCache",
            // Корень файлового хранилища (filesDir/store).
            "common/AppPath.kt::root",
            // Синглтон загрузчика изображений Coil процесса.
            "common/coil/CoilImageLoaderFactory.kt::instance",
            // Базовый слой умеет передавать байты, но не знает, куда их класть:
            // фабрику импортёров ставит точка сборки.
            "common/p2p/P2pReceiveManager.kt::importerFactory",
            // Фоновая корутина активного P2P приёма в P2pReceiveManager.
            "common/p2p/P2pReceiveManager.kt::job",
            // Та же причина со стороны отправки.
            "common/p2p/P2pSendPreparer.kt::l",
            // Признак повреждения keyset Keystore для безопасного пересоздания хранилища.
            "common/settings/SecureCredentialStore.kt::lastFailureLooksLikeBrokenKeyset",
            // Экземпляр SharedPreferences для настроек приложения.
            "common/settings/Settings.kt::pref",
            // Зашифрованное хранилище учетных данных Luscious.
            "common/settings/Settings.kt::securePref",
            // Процессный дисковый кеш видео предзагрузки ExoPlayer.
            "common/videoplayer/feed/FeedVideoCache.kt::instance",
            // Сессионное состояние процесса: «пропустил логин» живёт до
            // перезапуска и не принадлежит ни одному экрану.
            "l/LSession.kt::loginSkipped",
            // Анонимный токен redgifs. Запись закрыта (`private set`) и идёт
            // под мьютексом; снаружи доступно только чтение.
            "r/network/http/ApiClient.kt::bearerToken",
            // Выбранная страна: глобальна по смыслу, раньше была двумя
            // разрозненными top-level переменными.
            "x/feature/country/country.kt::current",
            // Счётчик явных переключений страны пользователем для инвалидации пагинации.
            "x/feature/country/country.kt::userSelectionEpoch",
        )

        /**
         * Вызовы `App.onCreate`, чей относительный порядок значим.
         *
         * `AppPath` нужен Hilt-синглтонам, которые читают пути в конструкторе,
         * а `Settings.init` открывает зашифрованное хранилище уже по готовым путям.
         */
        val REQUIRED_ORDER = listOf(
            "AppBuildInfo.init(",
            "AppContextHolder.init(",
            "AppPath.init(",
            "Settings.init(",
        )
    }
}
