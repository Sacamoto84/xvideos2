package com.client.xvideos.x.feature.country

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.AppContextHolder
import com.client.xvideos.common.util.launchCatching
import com.client.xvideos.feature.x.R
import com.client.xvideos.ui.theme.PornHubOrange
import com.client.xvideos.ui.theme.grayColor
import com.client.xvideos.x.model.getFlagEmoji
import com.client.xvideos.x.urlStart
import com.client.xvideos.x.feature.net.readHtmlFromURLWebView
import com.client.xvideos.x.parcer.parseSiteCountryFlag
import com.composables.core.Menu
import com.composables.core.MenuButton
import com.composables.core.MenuContent
import com.composables.core.rememberMenuState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import timber.log.Timber


// Data class для представления страны
// Страна: Австралия, Ссылка: /change-country/au, Класс флага: flag-au
private data class Country(val name: String, val url: String, val flagClass: String)

@Preview
@Composable
fun PreviewComposeCountry() {
    ComposeCountry(modifier = Modifier)
}

private val countries: List<Country> by lazy { parserCountry() }

/**
 * Глобальное состояние выбранной страны.
 * Раньше это были две разрозненные top-level переменные — собраны в один холдер.
 */
object CountryState {
    var current: String by mutableStateOf("❓")    // Текущая страна
}

@Composable
fun ComposeCountry(modifier: Modifier = Modifier) {

    val emojiFont = FontFamily(Font(R.font.flag)) // Убедитесь, что шрифт добавлен в res/font

    val state = rememberMenuState(expanded = false)

    val stateLazyList = rememberLazyListState()

    val scope = rememberCoroutineScope()

    Box(  Modifier.size(48.dp).then(modifier) )
    {

        Menu( modifier = Modifier, state = state )
        {

            //Сама кнопка для вызова диалога
            MenuButton( Modifier.fillMaxSize().background(Color(0xFF151515)) )
            {

                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    BasicText(
                        CountryState.current,
                        style = TextStyle(
                            fontWeight = FontWeight.Medium,
                            color = Color.White,
                            fontSize = 24.sp
                        )
                    )
                }

            }

            MenuContent(
                modifier = Modifier
                    .padding(bottom = 0.dp)
                    .width(312.dp)
                    //.clip(RoundedCornerShape(26.dp))
                    .alpha(0.9F)
                    .background(grayColor(0x35)),
                // exit = fadeOut()
                //, enter = fadeIn()
            ) {

                LazyColumn(state = stateLazyList) {
                    items(countries) {

                        Box(
                            modifier = Modifier
                                .fillMaxWidth().padding(vertical = 3.dp)
                                .padding(start = 8.dp)
                                .clickable {
                                    scope.launchCatching(message = "Смена страны не удалась: ${it.name}") {

                                        val s = readHtmlFromURLWebView(urlStart + it.url)
                                        parseSiteCountryFlag(s)?.let { it1 -> CountryState.current = it1 }

                                        withContext(Dispatchers.Main) {
                                            //CountryState.updateTrigger++
                                            Toast.makeText(
                                                AppContextHolder.applicationContext,
                                                "${getFlagEmoji(it.flagClass)} ${it.name}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }

                                    }
                                }
                        ) {

                            BasicText(
                                text = "${getFlagEmoji(it.flagClass)}  ${it.name} ",
                                style = TextStyle(
                                    fontFamily = emojiFont,
                                    fontSize = 28.sp,
                                    color = if (getFlagEmoji(it.flagClass) == CountryState.current) PornHubOrange else Color.LightGray
                                )
                            )


                        }
                    }

                }

            }

        }

    }


}


/**
 * Страна: Австралия, Ссылка: /change-country/au, Класс флага: flag-au
 *
 * Страна: Австрия, Ссылка: /change-country/at, Класс флага: flag-at
 *
 * Страна: Азербайджан, Ссылка: /change-country/az, Класс флага: flag-az
 *
 * Страна: Аргентина, Ссылка: /change-country/ar, Класс флага: flag-ar
 *
 * Страна: Афганистан, Ссылка: /change-country/af, Класс флага: flag-af
 *
 * Страна: Бангладеш, Ссылка: /change-country/bd, Класс флага: flag-bd
 *
 * Страна: Бельгия, Ссылка: /change-country/be, Класс флага: flag-be
 *
 * Страна: Болгария, Ссылка: /change-country/bg, Класс флага: flag-bg
 *
 */


private fun parserCountry(): List<Country> {
    // Парсинг HTML
    val document: Document = Jsoup.parse(html)

    // Извлекаем все элементы <li> с классом "country-"
    val countries = document.select("li[class^=country-]")

    // Список стран
    val countryList = countries.map { country ->
        val countryName = country.select("a").text().trim() // Название страны
        val countryHref = country.select("a").attr("href").trim() // Ссылка (href)
        val flagClass = country.select("span").attr("class").replace("flag-small", "")
            .trim() // Убираем "flag-small"

        // Создаем объект Country
        Country(countryName, countryHref, flagClass)
    }

    // Выводим список стран
    countryList.forEach { country ->
        Timber.i("Страна: ${country.name}, Ссылка: ${country.url}, Класс флага: ${country.flagClass}")
    }
    return countryList
}

private val html = """
<ul class="countries row"><li class="country-au col-xs-12 col-sm-4 col-md-3 col-lg-2">
	<a href="/change-country/au" class="btn"><span class="flag-small flag-au"></span> Австралия</a>
</li><li class="country-at col-xs-12 col-sm-4 col-md-3 col-lg-2">
	<a href="/change-country/at" class="btn"><span class="flag-small flag-at"></span> Австрия</a>
</li><li class="country-az col-xs-12 col-sm-4 col-md-3 col-lg-2">
	<a href="/change-country/az" class="btn"><span class="flag-small flag-az"></span> Азербайджан</a>
</li><li class="country-ar col-xs-12 col-sm-4 col-md-3 col-lg-2">
	<a href="/change-country/ar" class="btn"><span class="flag-small flag-ar"></span> Аргентина</a>
</li><li class="country-af col-xs-12 col-sm-4 col-md-3 col-lg-2">
	<a href="/change-country/af" class="btn"><span class="flag-small flag-af"></span> Афганистан</a>
</li><li class="country-bd col-xs-12 col-sm-4 col-md-3 col-lg-2">
	<a href="/change-country/bd" class="btn"><span class="flag-small flag-bd"></span> Бангладеш</a>
</li><li class="country-be col-xs-12 col-sm-4 col-md-3 col-lg-2">
	<a href="/change-country/be" class="btn"><span class="flag-small flag-be"></span> Бельгия</a>
</li><li class="country-bg col-xs-12 col-sm-4 col-md-3 col-lg-2">
	<a href="/change-country/bg" class="btn"><span class="flag-small flag-bg"></span> Болгария</a>
</li><li class="country-bo col-xs-12 col-sm-4 col-md-3 col-lg-2">
	<a href="/change-country/bo" class="btn"><span class="flag-small flag-bo"></span> Боливия</a>
</li><li class="country-br col-xs-12 col-sm-4 col-md-3 col-lg-2">
	<a href="/change-country/br" class="btn"><span class="flag-small flag-br"></span> Бразилия</a>
</li><li class="country-gb col-xs-12 col-sm-4 col-md-3 col-lg-2">
	<a href="/change-country/gb" class="btn"><span class="flag-small flag-gb"></span> Великобритания</a>
</li><li class="country-hu col-xs-12 col-sm-4 col-md-3 col-lg-2">
	<a href="/change-country/hu" class="btn"><span class="flag-small flag-hu"></span> Венгрия</a>
</li><li class="country-ve col-xs-12 col-sm-4 col-md-3 col-lg-2">
	<a href="/change-country/ve" class="btn"><span class="flag-small flag-ve"></span> Венесуэла</a>
</li><li class="country-vn col-xs-12 col-sm-4 col-md-3 col-lg-2">
	<a href="/change-country/vn" class="btn"><span class="flag-small flag-vn"></span> Вьетнам</a>
</li><li class="country-gt col-xs-12 col-sm-4 col-md-3 col-lg-2">
	<a href="/change-country/gt" class="btn"><span class="flag-small flag-gt"></span> Гватемала</a>
</li><li class="country-de col-xs-12 col-sm-4 col-md-3 col-lg-2">
	<a href="/change-country/de" class="btn"><span class="flag-small flag-de"></span> Германия</a>
</li><li class="country-hk col-xs-12 col-sm-4 col-md-3 col-lg-2">
	<a href="/change-country/hk" class="btn"><span class="flag-small flag-hk"></span> Гонконг</a>
</li><li class="country-gr col-xs-12 col-sm-4 col-md-3 col-lg-2">
	<a href="/change-country/gr" class="btn"><span class="flag-small flag-gr"></span> Греция</a>
</li><li class="country-ge col-xs-12 col-sm-4 col-md-3 col-lg-2">
	<a href="/change-country/ge" class="btn"><span class="flag-small flag-ge"></span> Грузия</a>
</li><li class="country-dk col-xs-12 col-sm-4 col-md-3 col-lg-2">
	<a href="/change-country/dk" class="btn"><span class="flag-small flag-dk"></span> Дания</a>
</li><li class="country-do col-xs-12 col-sm-4 col-md-3 col-lg-2">
	<a href="/change-country/do" class="btn"><span class="flag-small flag-do"></span> Доминиканская Республика</a>
</li><li class="country-eg col-xs-12 col-sm-4 col-md-3 col-lg-2">
	<a href="/change-country/eg" class="btn"><span class="flag-small flag-eg"></span> Египет</a>
</li><li class="country-il col-xs-12 col-sm-4 col-md-3 col-lg-2">
	<a href="/change-country/il" class="btn"><span class="flag-small flag-il"></span> Израиль</a>
</li><li class="country-in col-xs-12 col-sm-4 col-md-3 col-lg-2">
	<a href="/change-country/in" class="btn"><span class="flag-small flag-in"></span> Индия</a>
</li><li class="country-id col-xs-12 col-sm-4 col-md-3 col-lg-2">
	<a href="/change-country/id" class="btn"><span class="flag-small flag-id"></span> Индонезия</a>
</li><li class="country-jo col-xs-12 col-sm-4 col-md-3 col-lg-2">
	<a href="/change-country/jo" class="btn"><span class="flag-small flag-jo"></span> Иордания</a>
</li><li class="country-iq col-xs-12 col-sm-4 col-md-3 col-lg-2">
	<a href="/change-country/iq" class="btn"><span class="flag-small flag-iq"></span> Ирак</a>
</li><li class="country-ie col-xs-12 col-sm-4 col-md-3 col-lg-2">
	<a href="/change-country/ie" class="btn"><span class="flag-small flag-ie"></span> Ирландия</a>
</li><li class="country-is col-xs-12 col-sm-4 col-md-3 col-lg-2">
	<a href="/change-country/is" class="btn"><span class="flag-small flag-is"></span> Исландия</a>
</li><li class="country-es col-xs-12 col-sm-4 col-md-3 col-lg-2">
	<a href="/change-country/es" class="btn"><span class="flag-small flag-es"></span> Испания</a>
</li><li class="country-it col-xs-12 col-sm-4 col-md-3 col-lg-2">
	<a href="/change-country/it" class="btn"><span class="flag-small flag-it"></span> Италия</a>
</li><li class="country-kh col-xs-12 col-sm-4 col-md-3 col-lg-2">
	<a href="/change-country/kh" class="btn"><span class="flag-small flag-kh"></span> Камбоджа</a>
</li><li class="country-cm col-xs-12 col-sm-4 col-md-3 col-lg-2">
	<a href="/change-country/cm" class="btn"><span class="flag-small flag-cm"></span> Камерун</a>
</li><li class="country-ca col-xs-12 col-sm-4 col-md-3 col-lg-2">
	<a href="/change-country/ca" class="btn"><span class="flag-small flag-ca"></span> Канада</a>
</li><li class="country-qa col-xs-12 col-sm-4 col-md-3 col-lg-2">
	<a href="/change-country/qa" class="btn"><span class="flag-small flag-qa"></span> Катар</a>
</li><li class="country-ke col-xs-12 col-sm-4 col-md-3 col-lg-2">
	<a href="/change-country/ke" class="btn"><span class="flag-small flag-ke"></span> Кения</a>
</li><li class="country-cy col-xs-12 col-sm-4 col-md-3 col-lg-2">
	<a href="/change-country/cy" class="btn"><span class="flag-small flag-cy"></span> Кипр</a>
</li><li class="country-cn col-xs-12 col-sm-4 col-md-3 col-lg-2">
	<a href="/change-country/cn" class="btn"><span class="flag-small flag-cn"></span> Китай</a>
</li><li class="country-co col-xs-12 col-sm-4 col-md-3 col-lg-2">
	<a href="/change-country/co" class="btn"><span class="flag-small flag-co"></span> Колумбия</a>
</li><li class="country-la col-xs-12 col-sm-4 col-md-3 col-lg-2">
	<a href="/change-country/la" class="btn"><span class="flag-small flag-la"></span> Лаосская Народно-Демократическая Республика</a>
</li><li class="country-lv col-xs-12 col-sm-4 col-md-3 col-lg-2">
	<a href="/change-country/lv" class="btn"><span class="flag-small flag-lv"></span> Латвия</a>
</li><li class="country-lb col-xs-12 col-sm-4 col-md-3 col-lg-2">
	<a href="/change-country/lb" class="btn"><span class="flag-small flag-lb"></span> Ливан</a>
</li><li class="country-my col-xs-12 col-sm-4 col-md-3 col-lg-2">
	<a href="/change-country/my" class="btn"><span class="flag-small flag-my"></span> Малайзия</a>
</li><li class="country-mt col-xs-12 col-sm-4 col-md-3 col-lg-2">
	<a href="/change-country/mt" class="btn"><span class="flag-small flag-mt"></span> Мальта</a>
</li><li class="country-ma col-xs-12 col-sm-4 col-md-3 col-lg-2">
	<a href="/change-country/ma" class="btn"><span class="flag-small flag-ma"></span> Марокко</a>
</li><li class="country-mx col-xs-12 col-sm-4 col-md-3 col-lg-2">
	<a href="/change-country/mx" class="btn"><span class="flag-small flag-mx"></span> Мексика</a>
</li><li class="country-md col-xs-12 col-sm-4 col-md-3 col-lg-2">
	<a href="/change-country/md" class="btn"><span class="flag-small flag-md"></span> Молдова</a>
</li><li class="country-mm col-xs-12 col-sm-4 col-md-3 col-lg-2">
	<a href="/change-country/mm" class="btn"><span class="flag-small flag-mm"></span> Мьянма (бывшая Бирма)</a>
</li><li class="country-ng col-xs-12 col-sm-4 col-md-3 col-lg-2">
	<a href="/change-country/ng" class="btn"><span class="flag-small flag-ng"></span> Нигерия</a>
</li><li class="country-nl col-xs-12 col-sm-4 col-md-3 col-lg-2">
	<a href="/change-country/nl" class="btn"><span class="flag-small flag-nl"></span> Нидерланды</a>
</li><li class="country-nz col-xs-12 col-sm-4 col-md-3 col-lg-2">
	<a href="/change-country/nz" class="btn"><span class="flag-small flag-nz"></span> Новая Зеландия</a>
</li><li class="country-no col-xs-12 col-sm-4 col-md-3 col-lg-2">
	<a href="/change-country/no" class="btn"><span class="flag-small flag-no"></span> Норвегия</a>
</li><li class="country-tz col-xs-12 col-sm-4 col-md-3 col-lg-2">
	<a href="/change-country/tz" class="btn"><span class="flag-small flag-tz"></span> Объединённая Республика Танзания</a>
</li><li class="country-pk col-xs-12 col-sm-4 col-md-3 col-lg-2">
	<a href="/change-country/pk" class="btn"><span class="flag-small flag-pk"></span> Пакистан</a>
</li><li class="country-pe col-xs-12 col-sm-4 col-md-3 col-lg-2">
	<a href="/change-country/pe" class="btn"><span class="flag-small flag-pe"></span> Перу</a>
</li><li class="country-pl col-xs-12 col-sm-4 col-md-3 col-lg-2">
	<a href="/change-country/pl" class="btn"><span class="flag-small flag-pl"></span> Польша</a>
</li><li class="country-pt col-xs-12 col-sm-4 col-md-3 col-lg-2">
	<a href="/change-country/pt" class="btn"><span class="flag-small flag-pt"></span> Португалия</a>
</li><li class="country-kr col-xs-12 col-sm-4 col-md-3 col-lg-2">
	<a href="/change-country/kr" class="btn"><span class="flag-small flag-kr"></span> Республика Корея</a>
</li><li class="country-sg col-xs-12 col-sm-4 col-md-3 col-lg-2">
	<a href="/change-country/sg" class="btn"><span class="flag-small flag-sg"></span> Республика Сингапур</a>
</li><li class="country-ru col-xs-12 col-sm-4 col-md-3 col-lg-2">
	<a href="/change-country/ru" class="btn"><span class="flag-small flag-ru"></span> Россия</a>
</li><li class="country-ro col-xs-12 col-sm-4 col-md-3 col-lg-2">
	<a href="/change-country/ro" class="btn"><span class="flag-small flag-ro"></span> Румыния</a>
</li><li class="country-sn col-xs-12 col-sm-4 col-md-3 col-lg-2">
	<a href="/change-country/sn" class="btn"><span class="flag-small flag-sn"></span> Сенегал</a>
</li><li class="country-rs col-xs-12 col-sm-4 col-md-3 col-lg-2">
	<a href="/change-country/rs" class="btn"><span class="flag-small flag-rs"></span> Сербия</a>
</li><li class="country-sk col-xs-12 col-sm-4 col-md-3 col-lg-2">
	<a href="/change-country/sk" class="btn"><span class="flag-small flag-sk"></span> Словакия</a>
</li><li class="country-us col-xs-12 col-sm-4 col-md-3 col-lg-2">
	<a href="/change-country/us" class="btn"><span class="flag-small flag-us"></span> США</a>
</li><li class="country-th col-xs-12 col-sm-4 col-md-3 col-lg-2">
	<a href="/change-country/th" class="btn"><span class="flag-small flag-th"></span> Таиланд</a>
</li><li class="country-tw col-xs-12 col-sm-4 col-md-3 col-lg-2">
	<a href="/change-country/tw" class="btn"><span class="flag-small flag-tw"></span> Тайвань</a>
</li><li class="country-tn col-xs-12 col-sm-4 col-md-3 col-lg-2">
	<a href="/change-country/tn" class="btn"><span class="flag-small flag-tn"></span> Тунис</a>
</li><li class="country-ua col-xs-12 col-sm-4 col-md-3 col-lg-2">
	<a href="/change-country/ua" class="btn"><span class="flag-small flag-ua"></span> Украина</a>
</li><li class="country-ph col-xs-12 col-sm-4 col-md-3 col-lg-2">
	<a href="/change-country/ph" class="btn"><span class="flag-small flag-ph"></span> Филиппины</a>
</li><li class="country-fi col-xs-12 col-sm-4 col-md-3 col-lg-2">
	<a href="/change-country/fi" class="btn"><span class="flag-small flag-fi"></span> Финляндия</a>
</li><li class="country-fr col-xs-12 col-sm-4 col-md-3 col-lg-2">
	<a href="/change-country/fr" class="btn"><span class="flag-small flag-fr"></span> Франция</a>
</li><li class="country-cz col-xs-12 col-sm-4 col-md-3 col-lg-2">
	<a href="/change-country/cz" class="btn"><span class="flag-small flag-cz"></span> Чешская Республика</a>
</li><li class="country-cl col-xs-12 col-sm-4 col-md-3 col-lg-2">
	<a href="/change-country/cl" class="btn"><span class="flag-small flag-cl"></span> Чили</a>
</li><li class="country-ch col-xs-12 col-sm-4 col-md-3 col-lg-2">
	<a href="/change-country/ch" class="btn"><span class="flag-small flag-ch"></span> Швейцария</a>
</li><li class="country-se col-xs-12 col-sm-4 col-md-3 col-lg-2 current">
	<a href="/change-country/se" class="btn"><span class="flag-small flag-se"></span> Швеция</a>
</li><li class="country-lk col-xs-12 col-sm-4 col-md-3 col-lg-2">
	<a href="/change-country/lk" class="btn"><span class="flag-small flag-lk"></span> Шри-Ланка</a>
</li><li class="country-ec col-xs-12 col-sm-4 col-md-3 col-lg-2">
	<a href="/change-country/ec" class="btn"><span class="flag-small flag-ec"></span> Эквадор</a>
</li><li class="country-za col-xs-12 col-sm-4 col-md-3 col-lg-2">
	<a href="/change-country/za" class="btn"><span class="flag-small flag-za"></span> Южная Африка</a>
</li><li class="country-jp col-xs-12 col-sm-4 col-md-3 col-lg-2">
	<a href="/change-country/jp" class="btn"><span class="flag-small flag-jp"></span> Япония</a>
</li></ul>
"""
