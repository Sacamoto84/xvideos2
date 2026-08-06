package com.client.xvideos.r.common.saved

import com.client.xvideos.common.fileDB.FileDB
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.common.util.replaceWith
import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.r.model.sanitizeGifsInfoList
import com.client.xvideos.r.model.sanitizeOrNull
import timber.log.Timber
import kotlin.onSuccess

class R_Saved_Likes {

    val likesDb = FileDB(AppPath.r_likes, "likes", GifsInfo::class.java)

    var list = likesDb.list

    fun add(item: GifsInfo) {
        val safeItem = item.sanitizeOrNull() ?: run {
            SnackBar.error("Like add error: empty id")
            return
        }
        Timber.i(
            "R_Saved_Likes add() id:${safeItem.id} userName:${safeItem.userName} " +
                "url:${safeItem.urls.hd} -> ${likesDb.dirPath}/${safeItem.id}.likes"
        )
        likesDb.insert(safeItem.id, safeItem)
            .onSuccess {
                SnackBar.success("Like")
                list.add(safeItem)
            }
            .onFailure { e ->
                SnackBar.error("Ошибка добавления лайка ${e.message}")
            }
    }

    fun remove(item: GifsInfo) {
        Timber.i("R_Saved_Likes remove() id:${item.id} userName:${item.userName} url:${item.urls.hd}")
        likesDb.delete(item.id)
            .onSuccess {
                SnackBar.info("Unlike")
                // Раньше здесь был полный refresh(): пересканирование каталога и
                // Gson-разбор ВСЕХ лайков ради удаления одного элемента (O(n) чтений
                // с диска на каждый unlike). add() при этом правит список точечно —
                // делаем так же.
                list.removeAll { it.id == item.id }
            }
            .onFailure { e -> SnackBar.error("Ошибка удаления лайка ${e.message}") }
    }

    fun refresh() {
        likesDb.refresh()
        val current = list.toList()
        val sanitized = current.sanitizeGifsInfoList()
        // Переписываем список только если санитизация реально что-то изменила,
        // иначе получаем лишнюю перезапись и мигание списка.
        if (sanitized != current) {
            list.replaceWith(sanitized)
        }
    }

}
