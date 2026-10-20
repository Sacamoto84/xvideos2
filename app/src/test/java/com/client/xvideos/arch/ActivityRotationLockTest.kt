package com.client.xvideos.arch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Сторож: каждая Activity приложения сообщает `RotationLockKeeper`, что стала
 * верхней или перестала ею быть.
 *
 * Блокировку поворота возвращают по `onTopResumedActivityChanged`, а не по
 * жизненному циклу процесса (проход 200, T1). Новая Activity без этого вызова
 * тихо сломала бы возврат: уход из приложения с неё блокировку не вернёт.
 */
class ActivityRotationLockTest {

    @Test
    fun `каждая Activity передаёт onTopResumedActivityChanged хранителю блокировки поворота`() {
        val activities = ProjectSources.kotlinSourcesWithoutComments()
            .filter { (_, code) -> ACTIVITY.containsMatchIn(code) }
            .toList()
        assertTrue("Сторож не нашёл ни одной Activity — поиск сломан", activities.isNotEmpty())

        val missing = activities
            .filterNot { (_, code) -> CALL in code }
            .map { (path, _) -> path }
            .toSortedSet()

        assertEquals(
            "Activity не передаёт onTopResumedActivityChanged в RotationLockKeeper: уход из " +
                "приложения с неё не вернёт блокировку поворота.",
            emptySet<String>(),
            missing,
        )
    }

    @Test
    fun `правило находит наследников Activity`() {
        assertTrue(ACTIVITY.containsMatchIn("class MainActivity : ComponentActivity()//, ImageLoaderFactory"))
        assertTrue(ACTIVITY.containsMatchIn("class LWebProbeActivity : Activity() {"))
        assertEquals(false, ACTIVITY.containsMatchIn("fun Context.findActivity(): Activity? {"))
    }

    private companion object {
        val ACTIVITY = Regex("""\bclass\s+\w+\s*(?:\([^)]*\))?\s*:\s*[\w.]*Activity\s*\(""")
        const val CALL = "RotationLockKeeper.onTopResumedActivityChanged("
    }
}
