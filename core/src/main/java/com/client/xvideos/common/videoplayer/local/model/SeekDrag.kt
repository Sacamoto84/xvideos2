package com.client.xvideos.common.videoplayer.local.model

import com.client.xvideos.common.videoplayer.host.MediaPlayerHost

/** Палец отпущен: одна перемотка в [positionSec] и превью снимается. */
fun MediaPlayerHost.finishSeekDrag(positionSec: Float, onScrub: (SeekScrub?) -> Unit) {
    seekTo(positionSec)
    onScrub(null)
}

/**
 * Панель ушла из композиции посреди перетаскивания: `detectDragGestures` не
 * зовёт `onDragCancel`, когда отменяют его корутину, и без этого превью
 * залипало, перемотка терялась, а `isSliding` навсегда блокировал время.
 * Перетаскивания нет — ничего.
 */
fun MediaPlayerHost.finishSeekDragIfActive(positionSec: Float, onScrub: (SeekScrub?) -> Unit) {
    if (isSliding) finishSeekDrag(positionSec, onScrub)
}
