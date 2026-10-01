package com.client.xvideos.x.feature.country.model

import androidx.compose.runtime.Immutable
import com.client.xvideos.x.model.getFlagEmoji
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import timber.log.Timber

@Immutable
data class Country(
    val name: String,
    val url: String,
    val flagClass: String,
    val flagEmoji: String = getFlagEmoji(flagClass)
)

internal val countries: List<Country> by lazy { parserCountry() }

private fun parserCountry(): List<Country> {
    val document: Document = Jsoup.parse(html)
    val countryElements = document.select("li[class^=country-]")
    val countryList = countryElements.map { country ->
        val countryName = country.select("a").text().trim()
        val countryHref = country.select("a").attr("href").trim()
        val flagClass = country.select("span").attr("class").replace("flag-small", "").trim()
        Country(countryName, countryHref, flagClass)
    }
    Timber.d("parserCountry: parsed ${countryList.size} countries")
    return countryList
}

private const val html = """
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
