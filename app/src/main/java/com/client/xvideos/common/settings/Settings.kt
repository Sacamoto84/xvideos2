package com.client.xvideos.common.settings

import android.content.SharedPreferences
import com.client.xvideos.App
import com.client.xvideos.common.json.JsonTypes
import com.client.xvideos.common.settings.element.SettingElementBoolean
import com.client.xvideos.common.settings.element.SettingElementInt
import com.client.xvideos.common.settings.element.SettingElementList
import com.client.xvideos.common.settings.element.SettingElementString
import com.client.xvideos.l.model.ThumbnailsSize

//data class DC_galleryCount(var g0: Boolean, var g1: Boolean, var g2: Boolean, var g3: Boolean, var g4: Boolean )

/**
 * Централизованная точка доступа к настройкам приложения, которые хранятся в [SharedPreferences].
 *
 * Объект инициализируется один раз через [init], после чего предоставляет ленивый доступ
 * к типизированным настройкам через обёртки [SettingElementBoolean], [SettingElementInt],
 * [SettingElementString] и [SettingElementList].
 *
 * Настройки сгруппированы по функциональным зонам приложения:
 * - `r_*` — настройки разделов RedGifs;
 * - `l_*` — настройки разделов Luscious;
 * - `xvideos_*` — настройки, относящиеся к основному разделу Xvideos.
 *
 * Важно: [init] должен быть вызван до первого обращения к любому полю объекта,
 * иначе доступ к `pref` приведёт к ошибке инициализации.
 */
object Settings {

    private lateinit var pref: SharedPreferences

    fun init(prefs: SharedPreferences) { pref = prefs }

    //-- app ---

    val image_cache_ram_percent by lazy { SettingElementInt(pref, "image_cache_ram_percent", 10) }

    val image_cache_disk_enabled by lazy { SettingElementBoolean(pref, "image_cache_disk_enabled", true) }

    val image_cache_disk_size_mb by lazy { SettingElementInt(pref, "image_cache_disk_size_mb", 500) }

    val app_lock_enabled by lazy { SettingElementBoolean(pref, "app_lock_enabled", false) }

    val p2p_background_receive by lazy { SettingElementBoolean(pref, "p2p_background_receive", false) }



    //-- red ---




    /**
     * Текущее количество столбиков в R Gifs Tab: 2, 3 или 4.
     */
    val r_explorerGifsTab_column_current_count by lazy { SettingElementInt(pref, "r_explorerGifsTab_column_current_count", 2) }





    /**
     * Текущее количество столбиков в R Saved Likes Tab: 2, 3 или 4.
     */
    val r_likesTab_column_current_count by lazy { SettingElementInt(pref, "r_likesTab_column_current_count", 2) }






    /**
     * Текущее количество столбиков в R Saved Collection Tab: 2, 3 или 4.
     */
    val r_collectionTab_column_current_count by lazy { SettingElementInt( pref, "r_collectionTab_column_current_count", 2 ) }



    val current_count_gifTab by lazy { SettingElementInt(pref, "current_count_gifTab", 2) }


    val r_current_count_niches by lazy { SettingElementInt(pref, "current_count_niches", 2) }

    /**
     * Селектор режима отображения профиля RedGifs.
     */
    val red_profile_selector by lazy { SettingElementInt(pref, "red_profile_selector", 1) }


    //-- luscious ---

    //Логин
    val l_login by lazy { SettingElementString( pref, "l_login", "") }
    val l_pass by lazy { SettingElementString( pref, "l_pass", "") }

    /**
     * Размер миниатюры в галерее
     */
    val thumbalistSize by lazy { SettingElementString( pref, "thumbalistSize", ThumbnailsSize.SMALL.value ) }

    val l_fullscreen_vertical_pager by lazy { SettingElementBoolean(pref, "l_fullscreen_vertical_pager", false) }


    /**
     * Количество столбиков в L Gifs Tab T 1 2 3 4
     */
    val l_gifsTab_G_0_4 by lazy {  SettingElementList<Boolean>( pref, "l_gifsTab_G_0_4",  typeToken = JsonTypes.listOf(Boolean::class.javaObjectType) , default = listOf(false, true, true, true, true))  }


    /**
     * Текущее количество столбиков в L Gifs Tab
     */
    val l_gifsTab_column_current_count by lazy { SettingElementInt(pref, "l_gifsTab_column_current_count", 2) }



    /**
     * Количество столбиков в L Likes Tab T 1 2 3 4
     */
    val l_likesTab_G_0_4 by lazy {  SettingElementList<Boolean>( pref, "l_likesTab_G_0_4",  typeToken = JsonTypes.listOf(Boolean::class.javaObjectType) , default = listOf(false, true, true, true, true))  }

    /**
     * Текущее количество столбиков в L Likes Tab
     */
    val l_likesTab_column_current_count by lazy { SettingElementInt(pref, "l_likesTab_column_current_count", 2) }

    /**
     * Количество столбиков в L Collection Tab T 1 2 3 4
     */
    val l_collectionTab_G_0_4 by lazy {  SettingElementList<Boolean>( pref, "l_collectionTab_G_0_4",  typeToken = JsonTypes.listOf(Boolean::class.javaObjectType) , default = listOf(false, true, true, true, true))  }

    /**
     * Текущее количество столбиков в L Collection Tab
     */
    val l_collectionTab_column_current_count by lazy { SettingElementInt(pref, "l_collectionTab_column_current_count", 2) }






    //-- xvideos ---

    //Вывод в дашбоард в две столбика
    val xvideos_row2 by lazy { SettingElementBoolean(pref, "x_row2", true) }

    val xvideos_shemale by lazy { SettingElementBoolean(pref, "x_shemale", true) }



}
