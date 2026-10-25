# XLive: как устроен сайт — исследование

Дата: 24.10.2026. Источник — ответы сайта анониму и его JS-бандлы. Вход и
подписки (разделы 4–5) проверены 25–26.10.2026 под аккаунтом владельца: вход
он делал сам в WebView приложения, запросы шли с его сессией. Что осталось
непроверенным, помечено «по коду».

Чат на странице трансляции сознательно не разбирался: он будет отдельной
задачей.

Пути ниже — от базового адреса раздела (`xLiveUrlStart` в
`feature-xlive/.../XLiveSite.kt`). `<ник>` — имя комнаты, оно же логин
модели.

## Главное для приложения

- **Анониму открыто почти всё**: список комнат с фильтрами, теги, данные
  комнаты, био и живой поток — обычный JSON без cookie и Referer.
- **Вход — только через WebView.** Форма защищена reCAPTCHA v3 с запасной
  v2, а страницу входа голому HTTP Cloudflare отдаёт с 403. После входа
  cookie сессии `sessionid` и `csrftoken` берутся из хранилища WebView в
  OkHttp — так же, как раздел X делает с `XWebViewCookieJar`.
- **Cookie сессии — ещё не вход.** `sessionid` появляется раньше, чем вход
  закончен. Вход есть, когда `follow/api/online_followed_rooms/` отвечает
  JSON, а не переадресацией на `/auth/login/`.
- **Подписки требуют сессии.** Без неё `is_following` отвечает 401, счётчик
  подписок уводит на страницу входа, а `room-list?follow=true` отдаёт общий
  список комнат.
- **Адрес потока живёт минуты**: токен в `hls_source` короткий, а сессия
  потока гаснет без запросов. Адрес берётся перед каждым запуском плеера.

## 1. Разделы сайта

| Путь | Что показывает | Чем запрашивается |
| --- | --- | --- |
| `/` | все открытые комнаты, по популярности | `room-list` без фильтров |
| `/female-cams/`, `/male-cams/`, `/couple-cams/`, `/trans-cams/` | по полу | `genders=f\|m\|c\|t` |
| `/teen-cams/`, `/18to21-cams/`, `/20to30-cams/`, `/30to50-cams/`, `/mature-cams/` | по возрасту | `from_age`/`to_age`: 18–20, 18–22, 20–31, 30–51, 50–100 (верхняя граница не включается) |
| `/north-american-cams/`, `/south-american-cams/`, `/euro-russian-cams/`, `/asian-cams/`, `/other-region-cams/` | по региону | `regions=NA\|SA\|ER\|AS\|O` |
| `/6-tokens-per-minute-private-cams/` и ещё четыре | по цене привата | `private_prices=6\|12,18\|30,42\|60,72\|90…` — список через запятую, см. раздел 2 |
| `/new-cams/`, `/gaming-cams/`, `/exhibitionist-cams/` | подборки | `new_cams`, `gaming`, `exhib` |
| `/spy-on-cams/` | комнаты в приватном шоу | `private=true` |
| `/followed-cams/`, `/followed-cams/offline/` | подписки, онлайн и офлайн | `follow=true`, `follow=true&offline=true`; только с сессией, см. раздел 5 |
| `/tags/` | таблица тегов | `hashtags/tag-table-data` |
| `/tag/<тег>/`, `/tag/<тег>/female\|male\|couple\|trans/` | комнаты с тегом | `room-list?hashtags=<тег>&genders=…` |
| `/discover/` | витрина подборок | подробно не разбиралась |
| `/<ник>/` | страница трансляции | HTML с JSON комнаты, см. раздел 6 |
| `/auth/login/`, `/accounts/register/` | вход и регистрация | см. раздел 4 |

## 2. Список комнат

`GET /api/ts/roomlist/room-list/`

### Параметры

Полный список берётся из бандла: `offset`, `limit`, `genders`, `regions`,
`hashtags`, `from_age`, `to_age`, `private_prices`, `spy_show_prices`,
`room_size`, `apps`, `asp_app`, `keywords`, `exhib`, `new_cams`, `gaming`,
`hidden`, `private`, `follow`, `fallback`, `sort`, `search_sort`, `after`.

Проверено на живом API:

| Параметр | Значения | Что вышло |
| --- | --- | --- |
| `limit` | до 100 | `limit=200` → `{"errors": {"limit": ["Ensure this value is less than or equal to 100."]}}`; сайт сам берёт 90 |
| `offset` | шаг `limit` | следующая страница, `total_count` при этом плавает: список живой |
| `genders` | `f`, `m`, `c`, `t` | фильтрует; транс в ответе приходит как `gender: "s"` |
| `hashtags` | тег; можно повторять | несколько тегов объединяются через **ИЛИ**: `asian` — 577 комнат, `asian` + `bigboobs` — 1158 |
| `from_age` / `to_age` | годы | `18`/`20` → только 18–19 лет |
| `regions` | `NA`, `SA`, `ER`, `AS`, `O` | фильтрует |
| `keywords` | строка | поиск, у `anna` — 500 комнат |
| `new_cams` / `private` | `true` | новые комнаты / комнаты в привате (`current_show: private`) |
| `private_prices` | точные цены через запятую: `6`, `12,18`, `30,42`, `60,72`, `90,120,…` | фильтрует. `12-18` и повтор параметра — 400; цена не из списка сервера (например, `24`, `300`) — 400. В списке комнат встречались 6, 12, 18, 30, 42, 60, 72, 90, 120, 150, 180, 240, 360, 480, 720, 960, 1920; `-1` — приват выключен |
| `sort` | пусто — популярные, `rv`, `rand` | `rand` — случайный порядок |
| `follow` | `true` | анониму фильтр не применяется, приходит общий список |
| `search_sort` | `rel`, `pop` | проверено 25.10: с ним выдача по слову нечёткая — у `ann` совпадений в первой странице 22 из 90 против 90 из 90 без него. Приложение его не передаёт |

Поиск по `keywords` — проверено 25.10:

- ответы нестабильны: один и тот же запрос подряд даёт `total_count` то
  настоящий (66, 68), то 500;
- когда совпадения кончаются, сервер добивает страницу чужими комнатами;
  у бессмысленного слова — 90 чужих комнат. Пометки у таких комнат нет
  (`source_name` у всех `rs`), `fallback=false` заметно этого не меняет.

По коду, не проверено: `room_size=sm|md|lg`, `spy_show_prices`.

### Ответ

```json
{
  "rooms": [ … ],
  "total_count": "6718",
  "all_rooms_count": "7898",
  "room_list_id": "…",
  "has_fingerprint": "False"
}
```

Числа в верхнем уровне приходят строками.

Поля комнаты: `username`, `gender`, `display_age`, `location`, `country`,
`current_show`, `room_subject`, `subject`, `tags`, `num_users`,
`num_followers`, `is_new`, `is_gaming`, `has_password`, `private_price`,
`spy_show_price`, `is_age_verified`, `is_following`, `label`,
`source_name`, `start_timestamp`, `start_dt_utc`, `img`.

- **Промо-комната.** На каждой странице может быть одна лишняя комната:
  `label: "promoted"`, `source_name: "pr"`, у неё одной есть
  `source_position`. Поэтому при `limit=90` приходит 91 комната.
- **Повторы между страницами** возможны: пока листаешь, комнаты меняют
  место. Склеивать страницы надо по `username`.
- **`img`** — снимок эфира с меткой времени в query. Новый снимок приходит
  со следующим запросом списка.
- `display_age` бывает 99 — похоже, возраст не указан. Не проверено.

## 3. Теги

### Страница `/tags/`

Таблица тегов с вкладками: без фильтра, «Женщины», «Мужчины», «Пары»,
«Трансгендеры». В строке — тег, число зрителей и комнат, при наведении —
превью комнат. Клик по тегу ведёт на `/tag/<тег>/`, с выбранным полом —
на `/tag/<тег>/female/` и т. п.

### API

`GET /api/ts/hashtags/tag-table-data/?sort=&page=&g=&limit=`

| Параметр | Значения |
| --- | --- |
| `g` | пусто — все, `f`, `m`, `c`, `t` |
| `sort` | `ht` — по тегу, `vc` — по зрителям, `rc` — по комнатам; `-` впереди — по убыванию; пусто — порядок сайта. Длиннее 10 символов — ошибка |
| `page` | с 1 |
| `limit` | до 100, сайт берёт 100 |

```json
{
  "hashtags": [
    {
      "hashtag": "bigboobs",
      "viewer_count": 249367,
      "room_count": 1195,
      "top_rooms": [ … до 12 комнат … ]
    }
  ],
  "total": 858
}
```

У комнаты в `top_rooms` поля `room` (ник), `img`, `viewers`, `gender`,
`display_age`, `location`, `country`, `subject`, `room_subject`, `tags`,
`label`, `start_timestamp`.

Всего около 860 тегов; у женщин — 638, у трансов — 237.

### Фильтрация по тегу

Комнаты тега — тот же `room-list` с `hashtags=<тег>`, вместе с `genders`
и остальными фильтрами. Несколько тегов — это ИЛИ, а не И.

### Подписка на теги

Код таблицы читает у тега поле `followed`, но анониму его не отдают.
Эндпоинта подписки на тег в бандлах не нашлось. Это надо смотреть под
аккаунтом.

## 4. Вход

### Форма `/auth/login/`

`POST /auth/login/` — обычная форма Django:

| Поле | Что это |
| --- | --- |
| `csrfmiddlewaretoken` | из скрытого поля формы, парное к cookie `csrftoken` |
| `username`, `password` | логин и пароль |
| `rememberme` | галочка «запомнить» |
| `signed_recaptcha` | подписанный токен reCAPTCHA v3, его добавляет JS |
| `sign_recaptcha_failed=1` | вместо него, если v3 не сработала |

Как работает капча (по коду):

1. Перед отправкой страница получает токен reCAPTCHA v3 с `action:
   "/auth/login/"`.
2. Токен уходит на `POST /accounts/ajax_validate_recaptcha3/`, сервер
   возвращает подписанное значение.
3. Оно кладётся в форму как `signed_recaptcha`.
4. Если v3 не прошла — показывается обычная капча v2.

Ещё есть вход через SSO: `/auth-sso/login/<провайдер>/`. Провайдеров
два — Google и аккаунт X (в разделе X вход уже есть).

Страницу входа голым HTTP с ПК Cloudflare отдаёт с 403 — и с десктопным
User-Agent, и с User-Agent WebView. API раздела при этом отвечает.

### Шаги после формы

После формы сайт может вести по своим служебным страницам. Их список —
`AUTH_PATHS` в бандле: `/auth/…`, `/auth-sso/…` (вход через другой
аккаунт), `/security/tfa/auth/` и `/security/tfa/auth/choose_device/`
(второй фактор), `/accounts/age_verification/`, `/agegate/`,
`/identity/`, `/oauth/`, `/mobile/accounts/…`.

Проверено: вход владельца шёл так — `/auth/login/` → страница другого
сайта (аккаунт для SSO) → `/auth-sso/…` дважды → главная. Cookie
`sessionid` стояла уже до того, как вход закончился. Первая версия
приложения сочла вход выполненным на служебной странице и оборвала
загрузку — сайт эту сессию так и не узнал: `viewer_username` в ответах был
`AnonymousUser`.

Поэтому вход считается выполненным, только когда WebView дошёл до страницы
сайта вне служебных и сайт ответил на `follow/api/online_followed_rooms/`
JSON-ом.

### Cookie

Проверено после входа:

| Cookie | Домен | Флаги | Что это |
| --- | --- | --- | --- |
| `sessionid` | `.www.<домен>` | `Secure`, `HttpOnly`, `SameSite=None` | сессия Django |
| `csrftoken` | `.www.<домен>` | `Secure` | токен CSRF, его значение идёт в `X-CSRFToken` |
| `__cf_bm` | — | — | Cloudflare |
| `sbr`, `affkey`, `whitelabels_hc`, `__utfpp`, `_ga…` | — | — | сайт и аналитика, для подписок не нужны |

- Снять `sessionid` в хранилище WebView можно только с тем же доменом —
  `Domain=.www.<домен>` — и с `Secure`. С `Domain=.<домен>` `setCookie`
  отвечает успехом, а cookie остаётся.
- Сессия не привязана к User-Agent: та же `sessionid` работала с ПК с
  десктопным User-Agent. Привязку к IP так не проверить: ПК и телефон,
  скорее всего, выходят в сеть через один адрес.
- С сессией `api/chatvideocontext/<ник>/` отдаёт в `viewer_username` ник
  аккаунта, без неё — `AnonymousUser`.

### Выход

`POST /auth/logout/` формой с `csrfmiddlewaretoken` — так выходит кнопка
сайта; GET в бандлах не встречается. Проверено: сайт отвечает
переадресацией, после неё прежнюю `sessionid` он не узнаёт — счётчик
подписок с ней уводит на вход. Клиент Ktor по переадресации после POST не
идёт, поэтому 3xx на выход — успех.

### POST-запросы

Сайт шлёт их с заголовками `X-CSRFToken` (значение cookie `csrftoken`) и
`X-Requested-With: XMLHttpRequest`. Проверено на подписке:

- без `X-CSRFToken` — 403, HTML-страница;
- без `Referer` и `Origin` — тоже 403: Django проверяет источник у POST по
  HTTPS;
- с `X-CSRFToken`, `X-Requested-With`, `Referer` (страница комнаты) и
  `Origin` — 200.

## 5. Подписки

Проверено под аккаунтом владельца 25–26.10. Подписка и отписка делались на
одной чужой комнате в эфире — запросом и кнопками приложения; после
проверки у аккаунта, как и до неё, подписок нет.

| Запрос | Со входом | Анониму |
| --- | --- | --- |
| `GET /follow/is_following/<ник>/` | 200, `{"following": false, "notification_frequency": "…"}` | 401, JSON `{"status", "detail", "code", "ts_context"}` |
| `POST /follow/follow/<ник>/?redirect=error`, форма `location=<откуда>` | 200, `{"following": true, "notification_frequency": "…"}` | не проверялось |
| `POST /follow/unfollow/<ник>/?redirect=error` | 200, `{"following": false, …}` | не проверялось |
| `GET /follow/api/online_followed_rooms/` | `{"online": 1, "total": 1, "online_rooms": [ … ]}` | 302 на `/auth/login/` |
| `GET /follow/api/online_followed_rooms_info/` | то же, комнаты с полными данными | 302 на `/auth/login/` |
| `GET /api/ts/roomlist/room-list/?follow=true` | только подписки в эфире, `total_count` — их число | фильтр не применяется, общий список |
| `GET /api/ts/roomlist/room-list/?follow=true&offline=true` | подписки не в эфире | фильтр не применяется, общий список |

- **Ответ подписки и отписки** — только `following` и
  `notification_frequency`. Полей `online_count` и `total_count`, которые
  читает код бандла, в ответе нет: счётчик берётся отдельным запросом.
- **`online_followed_rooms`** — сколько подписок всего (`total`) и в эфире
  (`online`), плюс превью онлайн-комнат (`room`, `image`). Приложение
  берёт его первым: это и счётчик, и проверка, что сайт узнаёт сессию.
- **`online_followed_rooms_info`** — у комнаты `username`, `age`,
  `gender`, `country`, `location`, `img`, `num_users`, `room_subject`,
  `tags`, `label`, `is_following`, `start_timestamp`.
- **`room-list?follow=true`** — те же поля, что у обычного списка
  (раздел 2), и `is_following: true`. Подписки офлайн —
  `follow=true&offline=true`: так строит запрос страница
  `/followed-cams/offline/`. С сессией фильтр работает (без подписок —
  `total_count: 0`), но офлайн-подписок у аккаунта не было, и как такой
  список выглядит на живых данных — не проверено. `offline=true` без
  `follow` фильтром не считается: приходит общий список.
- **Подписка видна и в данных:** `is_following` у комнаты в `room-list` и
  `following` в JSON комнаты (`api/chatvideocontext/<ник>/`) совпали с
  `is_following`.

По коду, не проверялось:

| Запрос | Что делает |
| --- | --- |
| `GET /api/ts/follow/followed_usernames/?usernames=<ник>,<ник>` | на кого из списка подписан аккаунт — пачкой |
| `POST /api/ts/follow/notifications/<ник>`, `notification_frequency` | как часто сообщать о начале эфира |
| `GET`/`POST /api/ts/follow/email_notifications/` | письма о начале эфира |

Платная подписка — фан-клуб — отдельная история: поля `fan_club_*` и
страница `/fanclub/join/<ник>/`. Здесь не разбиралась.

## 6. Страница трансляции `/<ник>/`

### Откуда данные

HTML страницы несёт `window.initialRoomDossier` — JSON строкой с
`\u`-экранированием. Тот же JSON отдаёт
`GET /api/chatvideocontext/<ник>/`, и приложению удобнее он.

Поля, нужные для экрана комнаты:

| Поле | Что это |
| --- | --- |
| `room_status` | видели `public` и `private`. Клиент сайта знает ещё `offline`, `away`, `hidden`, `passwordprotected` и состояния привата (`privatewatching`, `privatespying`…); какие из них приходят сюда — не проверено |
| `hls_source` | мастер-плейлист LL-HLS с токеном; пустой, когда открытого эфира нет |
| `broadcaster_username`, `broadcaster_gender` | ник и пол |
| `room_title` | тема комнаты вместе с хештегами |
| `num_viewers` | зрители сейчас |
| `start_timestamp` | начало эфира, unix-время |
| `is_widescreen`, `is_portrait` | ориентация видео |
| `private_show_price`, `private_min_minutes` | цена привата в токенах за минуту и минимум минут |
| `spy_private_show_price`, `premium_private_price` | цена подглядывания и премиум-привата |
| `satisfaction_score` | рейтинг: `percent`, `up_votes`, `down_votes` |
| `summary_card_image` | обложка комнаты |
| `room_has_pass` | вход по паролю |
| `following`, `follow_notification_frequency` | подписка зрителя |
| `apps_running` | запущенные приложения комнаты: цель чаевых и т. п. |
| `edge_region` | регион CDN, например `CHI` |
| `viewer_username` | `AnonymousUser` без входа |

Остальные из 77 полей относятся к чату, чаевым и модерации.

### Поток

`hls_source`: LL-HLS, H.264 + AAC, от 360p до 1080p, сегменты fMP4 без
шифрования. Плеер media3 играет его в режиме прямого эфира — проверено
экспериментом в `feature-xlive`. Позиция плеера на эфире почти не растёт:
она считается от начала скользящего окна.

### Блоки под видео

| Запрос | Что отдаёт |
| --- | --- |
| `GET /api/biocontext/<ник>/` | био, см. ниже |
| `GET /api/panel_context/<ник>/` | панель приложения под видео: `template` (например, `3_rows_of_labels`) и строки `rowN_label`/`rowN_value` — цель чаевых, лучший донат |
| `GET /api/online/<ник>/` | `{"online": true}` |
| `/photo_videos/photoset/list_popup/<ник>/` | список фотосетов, HTML |
| `/photo_videos/photoset/detail/<ник>/<id>/` | фотосет, HTML; не разбирался |

Поля био:

| Поле | Что это |
| --- | --- |
| `about_me` | HTML; внешние ссылки идут через `/external_link/?url=…` |
| `real_name`, `display_age`, `display_birthday`, `sex`, `subgender` | о модели |
| `body_type`, `body_decorations`, `smoke_drink`, `languages`, `location` | анкета |
| `interested_in` | список: `Women`, `Men`, `Couples`, `Trans` |
| `follower_count` | подписчики |
| `last_broadcast`, `time_since_last_broadcast` | последний эфир |
| `room_status` | статус комнаты |
| `social_medias` | ссылки: `title_name`, `image_url`, `link`, `tokens`, `is_free`, `purchased` |
| `photo_sets` | фотосеты: `id`, `name`, `cover_url`, `tokens`, `is_video`, `photo_count`, `num_videos`, `video_duration_in_seconds`, `user_can_access`, `fan_club_only`, `label_text` |
| `wish_list` | список желаний |
| `performer_has_fanclub`, `fan_club_cost`, `fan_club_join_url`, `fan_club_is_member` | фан-клуб |

Платные фотосеты помечены `tokens` > 0 и `user_can_access: false`.

### Чат — не разбирался

Сюда относятся вебсокет чата, `/push_service/room_history/`, смайлы
(`/api/ts/emoticons/`), чаевые (`/tipping/…`) и меню чаевых.

## 7. Что не проверено

- Под аккаунтом: подписка на теги, офлайн-подписки на живых данных (у
  аккаунта их не было), уведомления о начале эфира.
- Витрина `/discover/` и мобильная вёрстка: мобильному User-Agent сайт
  отдаёт другую HTML-страницу, а API тот же.
- Статусы комнаты кроме `public` и `private`: что приходит у офлайн-комнаты
  и у комнаты с паролем.
- Требование подтверждать возраст: у анонима сейчас
  `is_age_verified: true`, но в коде есть `/accounts/age_verification/` — в
  некоторых странах сайт может его спросить.
