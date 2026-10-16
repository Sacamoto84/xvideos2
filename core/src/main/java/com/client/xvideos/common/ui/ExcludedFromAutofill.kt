package com.client.xvideos.common.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentDataType
import androidx.compose.ui.semantics.contentDataType
import androidx.compose.ui.semantics.semantics

/**
 * Поле не участвует в автозаполнении вовсе.
 *
 * Ставится на каждое поле ввода приложения, кроме формы входа L: та на
 * менеджер паролей рассчитана. За этим следит `AutofillExclusionTest`.
 *
 * Запрета на уровне окна (`importantForAutofill` у корня, см. `MainActivity`)
 * мало по двум причинам. Диалог и выпадающее меню — отдельные окна, на них он
 * не действует: имя коллекции и адрес DNS служба автозаполнения получала
 * вместе с текстом. И даже в закрытом окне Compose
 * сообщает системе о фокусе в любом редактируемом поле — сеанс службы
 * открывался, пусть и без полей в структуре (история запросов в
 * `dumpsys autofill`). С этой пометкой о фокусе не сообщается, и сеанс не
 * открывается.
 */
fun Modifier.excludedFromAutofill(): Modifier =
    semantics { contentDataType = ContentDataType.None }
