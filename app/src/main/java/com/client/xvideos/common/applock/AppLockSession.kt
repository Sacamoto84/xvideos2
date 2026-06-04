package com.client.xvideos.common.applock

object AppLockSession {

    @Volatile
    private var unlocked = false

    fun isUnlocked(): Boolean = unlocked

    fun unlock() {
        unlocked = true
    }

    fun lock() {
        unlocked = false
    }
}
