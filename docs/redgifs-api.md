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
| `getTopAllTime` | `/v2/gifs/search?order=top&count=&page=&type=` |
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

### Что бывает от неверного `order` — проверено 06.08.2026

Раньше здесь стояло «неверное значение `order` не даёт ошибки: приходит выдача,
просто отсортированная не так». **Это верно не для всех адресов**, и для лент
гифок — неверно.

`/v2/gifs/search` значение проверяет и отвечает `400`:

```json
{"error":{"code":"BadOrder",
  "message":"Bad sorting order \"zzzqqq\", must be one of: top, top7, top28, latest, score, trending."}}
```

То есть API сам перечисляет допустимый набор. Проверено запросами с временным
токеном по каждому значению `Order`:

| на проводе | `/v2/gifs/search` | `/v2/users/{name}/search` | где в приложении |
|---|---|---|---|
| `top` | ✅ | ✅ | `TOP` |
| `top7` | ✅ | ✅ | `TOP_WEEK` |
| `top28` | ✅ | ✅ | `TOP_MONTH`, `TOP28` |
| `latest` | ✅ | ✅ | `LATEST` |
| `trending` | ✅ | ✅ | `TRENDING` |
| `score` | ✅ | **400** | `RELEVANT` — только поиск |
| `oldest` | **400** | ✅ | `OLDEST` — только профиль |
| `alltime` | **400** | — | было `TOP_ALLTIME`, **удалено** — значения не существует |
| `recent`, `best`, `new` | **400** | 200, но в список не входит | были `RECENT`, `BEST` — **удалены** |
| `""` | **400** | — | `FORCE_TEMP` — служебный признак, на провод не уходит |

Профильный адрес ведёт себя иначе: его список — `trending, oldest, latest, top,
top7, top28`, `score` он отвергает, а неперечисленные `recent`/`best`/`new`
принимает и отдаёт выдачу, то есть просто их игнорирует. Отсюда и взялось старое
утверждение — оно про этот адрес, а не про ленты.

После этой проверки `Order` вычищен: удалены `TOP_ALLTIME`, `RECENT` и `BEST` —
три значения, которых сервер не принимает. Из оставшихся `OLDEST` живёт только в
меню профиля (`ScreenRedProfileSM.orderList`), где адрес его принимает, а
`RELEVANT` — только в меню поиска. `FORCE_TEMP("")` на провод не уходит: это
служебный признак «обновить», `ItemNailsPagingSource` отрабатывает его до
запроса.

Соответствие «константа — строка на проводе» прибито тестом
`OrderWireValuesTest` в `:feature-r`.

### «All time» — такой сортировки у RedGifs нет, сведена к `top`

Был `Order.TOP_ALLTIME("alltime")` в меню ленты гифок. Значение проверено:
`400 BadOrder`, в допустимый набор не входит и никогда не входило.

На провод оно при этом не уходило и `400` пользователь не видел:
`ItemTopPagingSource` уводил `TOP_ALLTIME` в `else` и отдавал `getTopThisWeek`, у
которого `order=top7` зашит в путь. То есть «All time» показывал неделю —
молча, и так было всегда.

Настоящий «топ за всё время» у RedGifs называется `top`: `top7` — неделя,
`top28` — месяц, `top` — без ограничения по времени. Поэтому `TOP_ALLTIME`
удалён, в наборе ленты его место занял `Order.TOP`, а под него заведён
`getTopAllTime` с `order=top`. Подпись в меню сменилась с «All time» на «Top» —
она же и в наборе поиска, где `Order.TOP` был с самого начала.

Следствие: наборы ленты и поиска теперь различаются ровно одним элементом —
`Relevant`, которого у лент быть не может. Это прибито тестом
`OrderNearestInTest`.

### Пустая первая страница у `trending` — не дефект

`order=trending&count=10&page=1` стабильно отдаёт `gifs: []` при `pages=1000` и
`total=10000`. На страницах со второй элементы есть.

Приложения это не касается: `PagingConfig` запрашивает `count=100`, а с ним
первая страница отдаёт 86 элементов. Записано, чтобы следующая проверка с
маленьким `count` не приняла это за поломку ленты.

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

Остальные адреса из таблицы выше проверены на устройстве 06.08.2026 и
работают: поиск ниш, авторов, тегов и подсказки. Отвалилась только ветка
`/v2/search/gifs`.

Если отвалится что-то ещё, порядок тот же: взять curl из этого файла и
сравнить с соседним работающим методом. И помнить про две ловушки выше — 200
здесь не означает ни того, что параметр учли, ни того, что сортировка та.

## Где смотреть в коде

- `network/api/RedApi_Search.kt` — текстовый поиск;
- `network/api/RedApi.kt` — ленты, авторы, ниши, тренды;
- `common/pagin/ItemTopPagingSource.kt` — кто и когда зовёт поиск;
- `ui/ui/lazyrow123/LazyRow123Host.kt` — как строка поиска превращается в
  новый `Pager`.
