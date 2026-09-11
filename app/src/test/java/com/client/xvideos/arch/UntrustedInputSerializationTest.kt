package com.client.xvideos.arch

import com.client.xvideos.arch.ProjectSources.invariantPath
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Сторож: проект полностью отказался от Gson в пользу kotlinx.serialization.
 *
 * Gson не вызывает конструкторы Kotlin и не смотрит на нуллабельность —
 * отсутствующее поле остаётся `null` в non-null типе, и падение случается
 * позже, вдали от разбора. Теперь во всех модулях (:core, :feature-l, :feature-x, :feature-r, :app)
 * используется kotlinx.serialization.
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

    @Test
    fun `весь проект полностью отказался от Gson`() {
        val offenders = ProjectSources.roots()
            .flatMap { root ->
                root.walkTopDown()
                    .filter { it.isFile && it.extension == "kt" }
                    .filter { file -> file.readText().contains("com.google.gson") }
                    .map { it.relativeTo(root).invariantPath() }
            }
            .toList()

        assertTrue(
            "Найдено использование Gson в проекте: $offenders. " +
                "Проект переведён на kotlinx.serialization, Gson запрещён.",
            offenders.isEmpty(),
        )
    }
}
