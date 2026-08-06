# Как приложение разговаривает с API RedGifs

Написано 06.08.2026, после того как текстовый поиск в разделе R перестал
работать: адрес `/v2/search/gifs?query=…` начал отвечать 404. API меняется без
предупреждения, поэтому здесь собрано всё, что нужно, чтобы проверить любой
адрес вручную и сравнить со старым поведением.

## Устройство клиента

Весь сетевой слой R — три файла:

| файл | что делает |
|---|---|
| `network/http/Route.kt` | склеивает URL: `BASE + path`, подстановка `{ключ}` и экранирование |
| `network/http/ApiClient.kt` | ktor-клиент: токен, заголовки, повтор при 401 |
| `network/api/RedApi*.kt` | сами методы; каждый — один `Route` |

База: `https://api.redgifs.com`.

**Токен анонимный и берётся сам.** `ApiClient` при первом запросе идёт на
`GET /v2/auth/temporary`, получает `{"token": "..."}` и дальше шлёт его в
`Authorization: Bearer …`. На 401 токен обновляется один раз и запрос
повторяется — это в `withAuth`. Логин под мьютексом, чтобы параллельные
запросы не устроили шторм.

**Заголовки важны.** Без них API может отвечать иначе:

```
Referer: https://www.redgifs.com/
Origin: https://www.redgifs.com
User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 ...
Accept: application/json, text/plain, */*
Accept-Language: en-US,en;q=0.9
```

## Как повторить запрос руками

Получить токен:

```bash
curl -s https://api.redgifs.com/v2/auth/temporary \
  -H 'Referer: https://www.redgifs.com/' \
  -H 'Origin: https://www.redgifs.com'
```

Дальше подставлять его в `Authorization`. Удобно положить в переменную:

```bash
TOKEN=$(curl -s https://api.redgifs.com/v2/auth/temporary -H 'Referer: https://www.redgifs.com/' | sed 's/.*"token":"\([^"]*\)".*/\1/')
```

Проверить любой адрес — например текстовый поиск:

```bash
curl -s -o /dev/null -w '%{http_code}\n' "https://api.redgifs.com/v2/gifs/search?query=cat&order=latest&count=5&page=1&type=g" -H "Authorization: Bearer $TOKEN" -H 'Referer: https://www.redgifs.com/'
```

Код `200` — адрес живой, `404` — его больше нет. Чтобы посмотреть на форму
ответа, убрать `-o /dev/null -w` и добавить `| head -c 2000`.

### Отдельно: 200 ещё не значит, что параметр учли

У поиска это выяснилось дорогой ценой. Если имя параметра неверное, API не
ругается — он возвращает **обычную ленту без фильтрации**: те же 99 элементов
и 100 страниц, что и без всякого поиска. Со стороны выглядит как рабочий
поиск, который «почему-то показывает не то».

Проверять надо заведомо бессмысленным словом:

```bash
curl -s "https://api.redgifs.com/v2/gifs/search?query=zzzqqq&count=5&page=1&type=g" -H "Authorization: Bearer $TOKEN" -H 'Referer: https://www.redgifs.com/' | head -c 200
```

Правильный запрос отдаёт `"pages":0` и пустой список. Если вместо этого
пришла полная лента — параметр проигнорирован.

## Что приложение сейчас вызывает

Ленты и поиск гифок — все через `/v2/gifs/search`, отличаются только `order`:

| метод | путь |
|---|---|
| `getTopThisWeek` | `/v2/gifs/search?order=top7&count=&page=&type=` |
| `getTopThisMonth` | `/v2/gifs/search?order=top28&count=&page=&type=` |
| `getTopTrending` | `/v2/gifs/search?order=trending&count=&page=&type=` |
| `getTopLatest` | `/v2/gifs/search?order=latest&count=&page=&type=` |
| `searchGifs` | `/v2/gifs/search?query=&order=&count=&page=&type=g` |
| `searchImage` | `/v2/gifs/search?query=&order=&count=&page=&type=i` |

Остальное:

| назначение | путь |
|---|---|
| автор | `/v1/users/{username}` |
| поиск у автора | `/v2/users/{username}/search?...` |
| ниша и её содержимое | `/v2/niches/{niches}`, `/v2/niches/{niches}/gifs?...` |
| связанное с нишей | `/v2/niches/{niches}/related`, `/top-creators`, `/top-tags` |
| список ниш | `/v2/niches?order=&previews=yes&sort=&page=&count=` |
| поиск ниш | `/v2/niches/search?query=` |
| подсказки тегов | `/v2/search/suggest?query=` |
| тренд-теги | `/v2/search/trending` |
| подсказки авторов | `/v2/creators/suggest?query=` |
| тренды | `/v2/explore/trending-gifs`, `/v2/explore/trending-images` |

`type`: `g` — гифки, `i` — картинки, `all` — всё (`model/Order.kt`, `MediaType`).

Значения `order`, которые принимает поиск:

| в меню | на проводе | примечание |
|---|---|---|
| Relevant | `score` | только у поиска, у лент смысла не имеет |
| Top | `top` | |
| Week | `top7` | у лент это же значение зашито в путь `getTopThisWeek` |
| Month | `top28` | то же, `getTopThisMonth` |
| Trending | `trending` | |
| Latest | `latest` | |

Неверное значение `order` **не даёт ошибки**: приходит выдача, просто
отсортированная не так, как выбрано. Поэтому соответствие прибито тестом
`OrderWireValuesTest` в `:feature-r`.

Проверено на устройстве 06.08.2026: все шесть сортировок поиска работают, и
ленты Week/Month без поиска тоже — у них значения `TOP_WEEK`/`TOP_MONTH`
менялись, и это было единственное место, где правка могла задеть работавшее.

## Что известно про поломку

`/v2/search/gifs?query=…&order=…&count=…&page=…` работал примерно до июля 2026.
Потом стал возвращать 404 `HttpNotFoundException`: RedGifs перенёс поиск на
общий адрес лент `/v2/gifs/search`, а отдельную ветку убрал. **Имя параметра
осталось прежним — `query`.**

Промежуточная неверная догадка стоила отдельной итерации: адрес был исправлен
на живой, но параметр взят `search_text` — по образцу соседнего `searchImage`.
Запросы стали отвечать 200, и это выглядело как успех, хотя фильтрации не
было. `searchImage` был сломан ровно так же и молча, потому что его никто не
вызывает.

Заметить это повторно проще всего по логу: строки `RedApi_*` с 404 при том,
что ленты грузятся.

Другие адреса из таблицы выше на 404 не проверялись — если что-то ещё
отвалится, порядок тот же: взять curl из этого файла, сравнить с соседним
работающим методом.

## Где смотреть в коде

- `network/api/RedApi_Search.kt` — текстовый поиск;
- `network/api/RedApi.kt` — ленты, авторы, ниши, тренды;
- `common/pagin/ItemTopPagingSource.kt` — кто и когда зовёт поиск;
- `ui/ui/lazyrow123/LazyRow123Host.kt` — как строка поиска превращается в
  новый `Pager`.
