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
        isCamouflage: Boolean = false,
    ): Boolean {
        // Если открыт калькулятор, не глушим окно черным экраном в Recent Apps,
        // позволяя системе сформировать естественную миниатюру калькулятора.
        if (isCamouflage && isLockShown) return false

        return isLockShown || (isMinimized && (blurRecentTasks || lockEnabled))
    }

    /**
     * Можно ли системе снимать задачу для «Недавних».
     *
     * `FLAG_SECURE` из `onPause` от снимка не спасает: при жесте «Домой» система
     * снимает задачу раньше, чем приходит `onPause`, и снимок выходил с
     * содержимым. Этим же снимком она закрывает окно при возврате, пока
     * приложение рисует первый кадр, — экран, с которого свернули, мелькал
     * перед замком. Запрет снимка действует заранее, поэтому решение то же, что
     * и для свёрнутого окна в [needsSecureFlag], но применяется, пока
     * приложение на экране.
     */
    fun allowsRecentsScreenshot(blurRecentTasks: Boolean, lockEnabled: Boolean): Boolean =
        !(blurRecentTasks || lockEnabled)
}
