package com.client.xvideos.screenSettings

import androidx.annotation.DrawableRes
import com.client.xvideos.R
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

internal enum class SettingsPage(
    val title: String,
    @DrawableRes val icon: Int,
    val subtitle: String
) {
    Main(
        title = "Настройки",
        icon = R.drawable.memory_24,
        subtitle = ""
    ),
    Appearance(
        title = "Отображение",
        icon = R.drawable.ic_blur_24,
        subtitle = "Стиль кнопок скролла и эффекты"
    ),
    Privacy(
        title = "Приватность",
        icon = R.drawable.key_24,
        subtitle = "Пароль и блокировка приложения"
    ),
    Network(
        title = "Сеть и DNS",
        icon = R.drawable.ic_dns_24,
        subtitle = "DNS-over-HTTPS, IPv4/IPv6"
    ),
    WebServer(
        title = "Просмотр на ПК",
        icon = R.drawable.hard_drive_2_24,
        subtitle = "Локальный Web-сервер по Wi-Fi"
    ),
    Cache(
        title = "Кэш",
        icon = R.drawable.hard_disk_24,
        subtitle = "RAM, изображения и очистка"
    ),
    L(
        title = "L",
        icon = R.drawable.icon_luscious,
        subtitle = "Профиль, миниатюры и колонки"
    ),
    Red(
        title = "R",
        icon = R.drawable.icon_red,
        subtitle = "Размеры папок, Downloads и Niches cache"
    ),
    X(
        title = "X",
        icon = R.drawable.icon_xvideos_white,
        subtitle = "Отображение и фильтры X"
    ),
    Storage(
        title = "Статистика",
        icon = R.drawable.hard_drive_2_24,
        subtitle = "Статистика по X, L и R"
    ),
    Backup(
        title = "Backup",
        icon = R.drawable.hard_drive_2_24,
        subtitle = "X, L, R в ZIP"
    ),
    P2P(
        title = "P2P",
        icon = R.drawable.icon_red,
        subtitle = "Передача файлов рядом"
    );

    companion object {
        val primaryPages: PersistentList<SettingsPage> = persistentListOf(
            Appearance,
            Privacy,
            Network,
            WebServer,
            Cache,
            Storage,
            Backup,
            P2P
        )
        val contentPages: PersistentList<SettingsPage> = persistentListOf(X, L, Red)
        val detailPages: List<SettingsPage>
            get() = primaryPages + contentPages
    }
}
