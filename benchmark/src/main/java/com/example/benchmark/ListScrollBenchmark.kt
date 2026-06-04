package com.example.benchmark

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMacrobenchmarkApi
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.MemoryUsageMetric
import androidx.benchmark.macro.PowerMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test

class ListScrollBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @RequiresApi(Build.VERSION_CODES.Q)
    @OptIn(ExperimentalMacrobenchmarkApi::class, ExperimentalMetricApi::class)
    @Test
    fun scrollLikesGridAfterNavigation() = benchmarkRule.measureRepeated(
        packageName = "com.client.xvideos",
        metrics = listOf(FrameTimingMetric()),
        iterations = 6, // Уменьшил для начала
        startupMode = StartupMode.WARM, // Важно! Используем WARM режим
        compilationMode = CompilationMode.Partial(), // Partial лучше для фрейм-тайминга
        setupBlock = {
            // ВСЁ, что происходит здесь - НЕ измеряется
            navigateToLikesScreen()

            // Дайте UI стабилизироваться после навигации
            device.waitForIdle(1000)
        }
    ) {
        // ВСЁ, что происходит ЗДЕСЬ - измеряется FrameTimingMetric

        // Найдём грид (он уже должен быть на экране после setupBlock)
        val grid = device.findObject(By.res("lLikes"))
            ?: throw IllegalStateException("Grid 'lLikes' not found in measure block")

        // Выполняем скроллы - именно здесь собираются метрики кадров
        repeat(2) {
            // Скролл вниз с небольшой паузой
            grid.setGestureMargin(device.displayWidth / 5)
            repeat(4) {
                grid.fling(Direction.DOWN, 20000)
                //grid.scroll(Direction.DOWN,10f, 20000 )
                //Thread.sleep(100)
                device.waitForIdle() // Даём отрисоваться кадрам
                //device.waitForIdle(500)  // Дольше подождать idle
            }
            repeat(4) {
                grid.fling(Direction.UP,20000)
                //grid.scroll(Direction.UP,10f, 20000)
                //Thread.sleep(100)
                //device.waitForIdle(500)  // Дольше подождать idle
                device.waitForIdle()
            }
        }
        device.waitForIdle(2000)  // Дольше подождать idle
    }

    /**
     * Навигация до экрана с гридом закладок
     * Вызывается в setupBlock - не влияет на метрики
     */
    private fun MacrobenchmarkScope.navigateToLikesScreen() {
        pressHome()
        startActivityAndWait()

        // Обработка разрешения на файлы
        device.findObject(By.res("bPermission"))?.let { button ->
            button.click()
            device.wait(Until.hasObject(By.pkg("com.android.settings")), 10000)

            val toggle = device.findObject(By.text("Разрешить управление всеми файлами"))
                ?: device.findObject(By.text("Разрешить"))
                ?: device.findObject(By.text("Allow access to manage all files"))
                ?: device.findObject(By.res("com.android.settings:id/switch_widget"))
                ?: device.findObjects(By.clazz("android.widget.Switch")).firstOrNull()

            if (toggle != null && !toggle.isChecked) {
                toggle.click()
            }

            device.pressBack()
            device.wait(Until.hasObject(By.pkg(packageName)), 10000)
        }

        // Навигация к экрану закладок
        device.waitForObject(By.res("buttonL"), 5000)?.click()
            ?: throw IllegalStateException("buttonL not found")

        device.waitForObject(By.res("bBookMark"), 5000)?.click()
            ?: throw IllegalStateException("bBookMark not found")

        // Ждём появления грида
        device.waitForObject(By.res("lLikes"), 15000)
            ?: throw IllegalStateException("lLikes grid not found after navigation")
    }

    private fun UiDevice.waitForObject(selector: BySelector, timeoutMs: Long = 10000): UiObject2? {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            findObject(selector)?.let { return it }
            waitForIdle(500)
        }
        return null
    }
}