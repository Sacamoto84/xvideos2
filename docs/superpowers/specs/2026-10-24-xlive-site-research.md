# XLive: как устроен сайт — исследование

Дата: 24.10.2026. Источник — ответы сайта анониму и его JS-бандлы. Под своим
аккаунтом ничего не проверялось: всё, что требует входа, описано по коду
страниц и помечено «по коду».

Чат на странице трансляции сознательно не разбирался: он будет отдельной
задачей.

Пути ниже — от базового адреса раздела (`xLiveUrlStart` в
`feature-xlive/.../XLiveSite.kt`). `<ник>` — имя комнаты, оно же логин
модели.

## Главное для приложения

- **Анониму открыто почти всё**: список комнат с фильтрами, теги, данные
  комнаты, био и живой поток — обычный JSON без cookie и Referer.
- **Вход — только через WebView.** Форма защищена reCAPTCHA v3 с запасной
  v2, повторить её голым HTTP нельзя. После входа cookie сессии и
  `csrftoken` нужно передать в OkHttp — так же, как раздел X делает с
  `XWebViewCookieJar`.
- **Подписки требуют сессии.** Без неё `is_following` отвечает 401, списки
  перенаправляют на страницу входа.
- **Адрес потока живёт минуты**: токен в `hls_source` короткий, а сессия
  потока гаснет без запросов. Адрес берётся перед каждым запуском плеера.

## 1. Разделы сайта

| Путь | Что показывает | Чем запрашивается |
| --- | --- | --- |
| `/` | все открытые комнаты, по популярности | `room-list` без фильтров |
| `/female-cams/`, `/male-cams/`, `/couple-cams/`, `/trans-cams/` | по полу | `genders=f\|m\|c\|t` |
| `/teen-cams/`, `/18to21-cams/`, `/20to30-cams/`, `/30to50-cams/`, `/mature-cams/` | по возрасту | `from_age`/`to_age`: 18–20, 18–22, 20–31, 30–51, 50–100 (верхняя граница не включается) |
| `/north-american-cams/`, `/south-american-cams/`, `/euro-russian-cams/`, `/asian-cams/`, `/other-region-cams/` | по региону | `regions=NA\|SA\|ER\|AS\|O` |
| `/6-tokens-per-minute-private-cams/` и ещё четыре | по цене привата | `private_prices=6\|12-18\|30-42\|60-72\|90` |
| `/new-cams/`, `/gaming-cams/`, `/exhibitionist-cams/` | подборки | `new_cams`, `gaming`, `exhib` |
| `/spy-on-cams/` | комнаты в приватном шоу | `private=true` |
| `/followed-cams/`, `/followed-cams/offline/` | подписки, онлайн и офлайн | `follow=true`, `offline`; только с сессией |
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
| `private_prices` | `6`, `12-18`, `30-42`, `60-72`, `90` | фильтрует |
| `sort` | пусто — популярные, `rv`, `rand` | `rand` — случайный порядок |
| `follow` | `true` | анониму фильтр не применяется, приходит общий список |

По коду, не проверено: `room_size=sm|md|lg`, `search_sort=rel|pop`
(сортировка поиска), `spy_show_prices`.

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

Как работает капча:

1. Перед отправкой страница получает токен reCAPTCHA v3 с `action:
   "/auth/login/"`.
2. Токен уходит на `POST /accounts/ajax_validate_recaptcha3/`, сервер
   возвращает подписанное значение.
3. Оно кладётся в форму как `signed_recaptcha`.
4. Если v3 не прошла — показывается обычная капча v2.

Ещё есть вход через SSO: `/auth-sso/login/<провайдер>/`. Провайдеров
два — Google и аккаунт X (в разделе X вход уже есть).

Выход — `/auth/logout/`.

### Cookie

Анониму сайт ставит `csrftoken`, `__cf_bm` (Cloudflare) и `sbr`. Cookie
сессии после входа — по соглашению Django `sessionid`. Не проверено: под
аккаунтом не входили.

### Что из этого следует

Повторять капчу в коде нельзя и не нужно: вход — страница в WebView. После
входа cookie WebView отдаются HTTP-клиенту раздела. Для POST-запросов
нужен заголовок `X-CSRFToken` со значением cookie `csrftoken` и
`X-Requested-With: XMLHttpRequest` — так делает сам сайт.

## 5. Подписки

По коду бандлов; анонимно проверены только GET-запросы.

| Запрос | Что делает | Анониму |
| --- | --- | --- |
| `GET /follow/is_following/<ник>/` | подписан ли | 401, `"Login required to check if following."` |
| `POST /follow/follow/<ник>/?redirect=error` | подписаться; тело `location=<откуда>` | не проверялось |
| `POST /follow/unfollow/<ник>/?redirect=error` | отписаться | не проверялось |
| `GET /follow/api/online_followed_rooms/` | сколько подписок онлайн и их превью | 302 на вход |
| `GET /follow/api/online_followed_rooms_info/` | то же с полными данными комнат | 302 на вход |
| `GET /api/ts/roomlist/room-list/?follow=true` | подписки онлайн как список комнат | без сессии фильтр не работает |
| `GET /api/ts/follow/email_notifications/` | письма о начале эфира | не проверялось |

Ответ подписки, отписки и `is_following` по коду:

```json
{
  "following": true,
  "notification_frequency": "…",
  "online_count": 3,
  "total_count": 12
}
```

Признак подписки есть и в данных: `is_following` у комнаты в
`room-list`, `following` и `follow_notification_frequency` в JSON
комнаты.

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

- Всё, что требует входа: подписка и отписка, списки подписок, подписка на
  теги, имя cookie сессии.
- Витрина `/discover/` и мобильная вёрстка: мобильному User-Agent сайт
  отдаёт другую HTML-страницу, а API тот же.
- Статусы комнаты кроме `public` и `private`: что приходит у офлайн-комнаты
  и у комнаты с паролем.
- Требование подтверждать возраст: у анонима сейчас
  `is_age_verified: true`, но в коде есть `/accounts/age_verification/` — в
  некоторых странах сайт может его спросить.
