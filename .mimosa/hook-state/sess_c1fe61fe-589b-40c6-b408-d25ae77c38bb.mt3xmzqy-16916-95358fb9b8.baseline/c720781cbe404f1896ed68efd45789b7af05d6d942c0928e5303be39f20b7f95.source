package com.client.xvideos.screenSettings

import com.client.xvideos.l.featured.saved.SavedL
import com.client.xvideos.r.common.block.BlockRed
import com.client.xvideos.r.common.downloader.DownloadRed
import com.client.xvideos.r.common.saved.SavedRed

/**
 * Синглтоны разделов, которые нужны экрану настроек.
 *
 * Раньше они шли четырьмя отдельными параметрами и тащились через три
 * вложенные функции. Когда для обновления R после восстановления бэкапа
 * понадобился ещё и [blockRed], `AppSettingsScreenContent` и
 * `AppSettingsScreenBody` перевалили за порог detekt в двенадцать параметров.
 *
 * Порог тут не придирка: список из пятнадцати позиций и правда невозможно
 * прочитать на месте вызова. Один держатель убирает три параметра из каждой
 * функции и заодно даёт понятное место, куда добавлять следующий.
 *
 * Все поля nullable: превью экрана настроек рисуется без DI.
 */
internal data class SettingsDataHolders(
    val savedRed: SavedRed? = null,
    val blockRed: BlockRed? = null,
    val downloadRed: DownloadRed? = null,
    val savedL: SavedL? = null,
)
