package com.client.xvideos.common.kdownloader

enum class Status {
    QUEUED,
    RUNNING,
    PAUSED,
    COMPLETED,
    CANCELLED,
    FAILED,
    UNKNOWN;

    val isFinished: Boolean get() = this == COMPLETED || this == CANCELLED || this == FAILED
    val isRunning: Boolean get() = this == RUNNING
    val isPaused: Boolean get() = this == PAUSED
    val isQueued: Boolean get() = this == QUEUED
    val isCompleted: Boolean get() = this == COMPLETED
}
