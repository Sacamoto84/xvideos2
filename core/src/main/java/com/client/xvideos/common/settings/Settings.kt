package com.client.xvideos.common.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.client.xvideos.common.json.JsonTypes
import com.client.xvideos.common.settings.element.SettingElementBoolean
import com.client.xvideos.common.settings.element.SettingElementInt
import com.client.xvideos.common.settings.element.SettingElementList
import com.client.xvideos.common.settings.element.SettingElementSecureString
import com.client.xvideos.common.settings.element.SettingElementString
import timber.log.Timber

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

    /**
     * Зашифрованное хранилище для секретов. `null`, если Keystore недоступен
     * (Compose Preview) или [init] вызвали без контекста.
     */
    private var securePref: SharedPreferences? = null

    /**
     * @param prefs общий файл настроек.
     * @param context нужен, чтобы открыть зашифрованное хранилище для учётных
     *   данных. Без него секреты не сохраняются на диск — см.
     *   [SettingElementSecureString].
     */
    fun init(prefs: SharedPreferences, context: Context? = null) {
        pref = prefs
        securePref = context?.let { SecureCredentialStore.createOrNull(it) }
        migrateLusciousCredentials()
    }

    /**
     * Переносит логин и пароль Luscious из общего файла настроек в зашифрованный
     * и вычищает открытые копии.
     *
     * Выполняется один раз: после переноса ключей `l_login`/`l_pass` в обычных
     * настройках не остаётся, и следующий запуск сразу выходит на первой проверке.
     */
    private fun migrateLusciousCredentials() {
        if (securePref == null) return

        val legacyLogin = pref.getString(KEY_L_LOGIN, null)
        val legacyPass = pref.getString(KEY_L_PASS, null)
        if (legacyLogin == null && legacyPass == null) return

        if (!legacyLogin.isNullOrEmpty()) l_login.setValue(legacyLogin)
        if (!legacyPass.isNullOrEmpty()) l_pass.setValue(legacyPass)

        pref.edit {
            remove(KEY_L_LOGIN)
            remove(KEY_L_PASS)
        }
        Timber.i("Settings: учётные данные Luscious перенесены в зашифрованное хранилище")
    }

    private const val KEY_L_LOGIN = "l_login"
    private const val KEY_L_PASS = "l_pass"

    //-- app ---

    val image_cache_ram_percent by lazy { SettingElementInt(pref, "image_cache_ram_percent", 10) }

    val image_cache_disk_enabled by lazy { SettingElementBoolean(pref, "image_cache_disk_enabled", true) }

    val image_cache_disk_size_mb by lazy { SettingElementInt(pref, "image_cache_disk_size_mb", 500) }

    val app_lock_enabled by lazy { SettingElementBoolean(pref, "app_lock_enabled", false) }

    val p2p_background_receive by lazy { SettingElementBoolean(pref, "p2p_background_receive", false) }


    /**
     * Используем смещение экрана сверху, область моноброви и камеры
     *
     * ```kotlin
     * val usePadding = Settings.useCutoutPadding.field.collectAsStateWithLifecycle().value
     * ```
     */
    val useCutoutPadding by lazy { SettingElementBoolean(pref, "use_cutout_padding", true) }

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

    // Логин. Лежит в зашифрованном хранилище, а не в общем файле настроек:
    // раньше пароль от стороннего сервиса хранился открытым текстом рядом с
    // остальными настройками. Миграция старых значений — в [init].
    val l_login by lazy { SettingElementSecureString(securePref, KEY_L_LOGIN, "") }
    val l_pass by lazy { SettingElementSecureString(securePref, KEY_L_PASS, "") }

    /**
     * Размер миниатюры в галерее
     */
    val thumbalistSize by lazy { SettingElementString( pref, "thumbalistSize", ThumbnailsSize.SMALL.value ) }

    val l_fullscreen_vertical_pager by lazy { SettingElementBoolean(pref, "l_fullscreen_vertical_pager", false) }

    /** Звук видео в полноэкранном просмотре L. По умолчанию выключен. */
    val l_fullscreen_video_muted by lazy { SettingElementBoolean(pref, "l_fullscreen_video_muted", true) }


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
