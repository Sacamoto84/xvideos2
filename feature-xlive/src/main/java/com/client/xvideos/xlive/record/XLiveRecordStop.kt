package com.client.xvideos.xlive.record

/**
 * Почему кончилась запись — и что сказать об этом снекбаром.
 *
 * @property isError снекбар ошибки, а не успеха.
 */
enum class XLiveRecordStop(val message: String, val isError: Boolean) {
    BUTTON("Запись сохранена", false),
    LEFT_ROOM("Запись сохранена", false),
    BACKGROUND("Запись сохранена", false),
    STREAM_ENDED("Эфир закончился — запись сохранена", false),
    NO_NETWORK("Нет сети — запись сохранена", true),
    NO_SPACE("Кончилось место — запись сохранена", true),

    /** Неожиданный сбой внутри записи: записанное до него склеивается. */
    ERROR("Запись прервалась — записанное сохранено", true),
}
