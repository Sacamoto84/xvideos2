package com.client.xvideos.common.applock

/** Когда окну приложения нужен `FLAG_SECURE`. */
object WindowPrivacy {

    /**
     * Нужно ли закрыть окно от снимков экрана и превью в «Недавних».
     *
     * Свёрнутое приложение закрывается и при включённом замке, а не только при
     * «размытии в недавних»: пока сеанс разблокирован, замок ещё не показан, и
     * система снимала превью с содержимым. Замок закрывался позже, по таймауту,
     * а карточка в «Недавних» оставалась прежней — её было видно без кода.
     *
     * @param isLockShown Показан экран замка.
     * @param isMinimized Приложение уходит с экрана или уже ушло.
     * @param blurRecentTasks Настройка «размытие в недавних».
     * @param lockEnabled Замок приложения включён.
     */
    fun needsSecureFlag(
        isLockShown: Boolean,
        isMinimized: Boolean,
        blurRecentTasks: Boolean,
        lockEnabled: Boolean,
    ): Boolean = isLockShown || (isMinimized && (blurRecentTasks || lockEnabled))
}
