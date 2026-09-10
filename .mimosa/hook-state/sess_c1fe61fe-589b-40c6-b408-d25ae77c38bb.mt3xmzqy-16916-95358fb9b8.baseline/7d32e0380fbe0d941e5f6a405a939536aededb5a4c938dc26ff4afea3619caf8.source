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
