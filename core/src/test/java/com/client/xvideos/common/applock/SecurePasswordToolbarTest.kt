package com.client.xvideos.common.applock

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurePasswordToolbarTest {

    private class FakeTextToolbar : TextToolbar {
        override var status: TextToolbarStatus = TextToolbarStatus.Hidden
        var hideCalled = false
        var lastRect: Rect? = null
        var lastOnCopy: (() -> Unit)? = null
        var lastOnPaste: (() -> Unit)? = null
        var lastOnCut: (() -> Unit)? = null
        var lastOnSelectAll: (() -> Unit)? = null

        override fun hide() {
            hideCalled = true
        }

        override fun showMenu(
            rect: Rect,
            onCopyRequested: (() -> Unit)?,
            onPasteRequested: (() -> Unit)?,
            onCutRequested: (() -> Unit)?,
            onSelectAllRequested: (() -> Unit)?
        ) {
            lastRect = rect
            lastOnCopy = onCopyRequested
            lastOnPaste = onPasteRequested
            lastOnCut = onCutRequested
            lastOnSelectAll = onSelectAllRequested
        }
    }

    @Test
    fun `secure toolbar suppresses copy and cut while preserving paste and selectAll`() {
        val fake = FakeTextToolbar()
        val secureToolbar = object : TextToolbar {
            override val status: TextToolbarStatus get() = fake.status
            override fun hide() = fake.hide()
            override fun showMenu(
                rect: Rect,
                onCopyRequested: (() -> Unit)?,
                onPasteRequested: (() -> Unit)?,
                onCutRequested: (() -> Unit)?,
                onSelectAllRequested: (() -> Unit)?
            ) {
                fake.showMenu(
                    rect = rect,
                    onCopyRequested = null,
                    onPasteRequested = onPasteRequested,
                    onCutRequested = null,
                    onSelectAllRequested = onSelectAllRequested
                )
            }
        }

        var copyCalled = false
        var pasteCalled = false
        var cutCalled = false
        var selectAllCalled = false

        val testRect = Rect(0f, 0f, 100f, 50f)
        secureToolbar.showMenu(
            rect = testRect,
            onCopyRequested = { copyCalled = true },
            onPasteRequested = { pasteCalled = true },
            onCutRequested = { cutCalled = true },
            onSelectAllRequested = { selectAllCalled = true }
        )

        assertEquals(testRect, fake.lastRect)
        assertNull("Копирование обязано быть заблокировано", fake.lastOnCopy)
        assertNull("Вырезание обязано быть заблокировано", fake.lastOnCut)

        fake.lastOnPaste?.invoke()
        assertTrue("Вставка должна оставаться доступной", pasteCalled)

        fake.lastOnSelectAll?.invoke()
        assertTrue("Выделение всего должно оставаться доступным", selectAllCalled)

        secureToolbar.hide()
        assertTrue(fake.hideCalled)
    }
}
