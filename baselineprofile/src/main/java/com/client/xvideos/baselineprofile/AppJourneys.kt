package com.client.xvideos.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until

internal const val APP_PACKAGE = "com.client.xvideos"

internal fun MacrobenchmarkScope.openMenu(): UiObject2 {
    startActivityAndWait()
    return waitForTag("buttonR")
}

internal fun MacrobenchmarkScope.waitForTag(tag: String): UiObject2 =
    checkNotNull(device.wait(Until.findObject(By.res(tag)), 15_000)) {
        "Не найден элемент $tag. Используйте тестовое устройство без включённого замка приложения."
    }
