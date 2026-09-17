# Код-ревью xvideos — проход 99

> **Срез:** `063f8cf` + рабочее дерево · **Статус:** закрыт полностью · **Индекс:** [все документы](README.md)

База прохода `063f8cf`. Затронут модуль `:feature-l`. Выполнена декомпозиция логики расчёта страниц альбома: вычисление вынесено в чистую функцию `calculateAlbumPages`, а нормализация медиа-ссылок `normalizePictureUrls` переведена на уровень пакета без привязки к экземпляру модели, очищен отладочный спам в logcat (`Timber.i`, префиксы `!!!`, `iii`, `eee`) в `AlbumPicsDetails`, `AlbumList`, `ScreenAlbumListSM`, `ScreenLAlbumLandingTag`, `ScreenLAlbumSM`, `ScreenCollectionName`, `L_LazyRowPictureDetails`, `ExpandMenuVM`, удалён закомментированный отладочный код, и добавлен набор модульных тестов `AlbumPicsDetailsUtilsTest`.

Линзы:
- `C` / Инкапсуляция математики разбиения на страницы с учётом потолка деления (`ceil`) и ограничений сервера `calculateAlbumPages`, вынос `normalizePictureUrls` в чистую функцию ([AlbumPicsDetails.kt](../feature-l/src/main/java/com/client/xvideos/l/net/AlbumPicsDetails.kt)).
- `L` / Перевод сетевых логов чанков и жизненного цикла стейт-машин в `Timber.d`, удаление отладочных префиксов `!!!`, `iii`, `eee` ([AlbumPicsDetails.kt](../feature-l/src/main/java/com/client/xvideos/l/net/AlbumPicsDetails.kt), [AlbumList.kt](../feature-l/src/main/java/com/client/xvideos/l/net/AlbumList.kt), [ScreenAlbumListSM.kt](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/ScreenAlbumListSM.kt), [ScreenLAlbumLandingTag.kt](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/albumLandingTag/ScreenLAlbumLandingTag.kt), [ScreenLAlbumSM.kt](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbum/ScreenLAlbumSM.kt), [ScreenCollectionName.kt](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/saved/collection/ScreenCollectionName.kt), [L_LazyRowPictureDetails.kt](../feature-l/src/main/java/com/client/xvideos/l/ui/element/lazyRowPictureDetails/L_LazyRowPictureDetails.kt), [ExpandMenuVM.kt](../feature-l/src/main/java/com/client/xvideos/l/ui/element/expandMenu/ExpandMenuVM.kt)).
- `D` / Очистка закомментированных вызовов и тестового кода в `ScreenAlbumListSM.kt` и `AlbumList.kt` ([ScreenAlbumListSM.kt](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/ScreenAlbumListSM.kt), [AlbumList.kt](../feature-l/src/main/java/com/client/xvideos/l/net/AlbumList.kt)).
- `T` / Добавление тестов пагинации и нормализации ссылок `AlbumPicsDetailsUtilsTest` ([AlbumPicsDetailsUtilsTest.kt](../feature-l/src/test/java/com/client/xvideos/l/net/AlbumPicsDetailsUtilsTest.kt)).

Граница P2P (`core/.../p2p/`) заморожена по решению владельца от 11.09.2026 и не затрагивалась.

---

## Находки

### C19 — Инкапсуляция расчёта страниц альбома calculateAlbumPages и чистая функция normalizePictureUrls. Средняя.

[feature-l/src/main/java/com/client/xvideos/l/net/AlbumPicsDetails.kt:164, 384](../feature-l/src/main/java/com/client/xvideos/l/net/AlbumPicsDetails.kt#L164)

Алгоритм вычисления числа страниц альбома с округлением вверх и выбором максимума между оценкой клиента и сервера находился внутри тела приватного метода парсера `parsePage`, не поддавался изолированному тестированию и смешивал десериализацию JSON с логикой пагинации. Метод `normalizePictureUrls` находился внутри класса `AlbumPicsDetails`, хотя не обращался к его состоянию.

**Исправление:**
- Вычисление страниц вынесено в чистую функцию `internal fun calculateAlbumPages(totalPagesFromInfo: Int?, totalItems: Int?, itemsPerPage: Int?): Int`.
- Метод `normalizePictureUrls` вынесен на уровень пакета как чистая функция без побочных эффектов.

---

### L33 — Засорение информационного уровня логов и маркеры !!!/iii/eee в экранах и SM альбомов. Низкая.

[feature-l/src/main/java/com/client/xvideos/l/net/AlbumPicsDetails.kt:192, 217, 227, 236, 245, 323](../feature-l/src/main/java/com/client/xvideos/l/net/AlbumPicsDetails.kt#L192)
[feature-l/src/main/java/com/client/xvideos/l/net/AlbumList.kt:91, 117, 130, 144, 180](../feature-l/src/main/java/com/client/xvideos/l/net/AlbumList.kt#L91)
[feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/ScreenAlbumListSM.kt:98, 171, 178, 182, 188, 208](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/ScreenAlbumListSM.kt#L98)
[feature-l/src/main/java/com/client/xvideos/l/ui/screens/albumLandingTag/ScreenLAlbumLandingTag.kt:421, 443](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/albumLandingTag/ScreenLAlbumLandingTag.kt#L421)
[feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbum/ScreenLAlbumSM.kt:114, 119, 126](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbum/ScreenLAlbumSM.kt#L114)
[feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/saved/collection/ScreenCollectionName.kt:129](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/saved/collection/ScreenCollectionName.kt#L129)
[feature-l/src/main/java/com/client/xvideos/l/ui/element/lazyRowPictureDetails/L_LazyRowPictureDetails.kt:198](../feature-l/src/main/java/com/client/xvideos/l/ui/element/lazyRowPictureDetails/L_LazyRowPictureDetails.kt#L198)
[feature-l/src/main/java/com/client/xvideos/l/ui/element/expandMenu/ExpandMenuVM.kt:127](../feature-l/src/main/java/com/client/xvideos/l/ui/element/expandMenu/ExpandMenuVM.kt#L127)

Множество логов пошаговой загрузки страниц, скролла к элементу, отправки файла (`share`) и жизненного цикла SM отправлялись в `Timber.i` с нестандартными префиксами `!!!`, `iii`, `eee`.

**Исправление:**
- Все подобные события переведены на уровень `Timber.d`.
- Удалены временные префиксы логов.

---

### D18 — Закомментированный отладочный код в ScreenAlbumListSM и AlbumList. Низкая.

[feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/ScreenAlbumListSM.kt:103, 121, 124, 125, 130, 156, 157, 164](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/ScreenAlbumListSM.kt#L103)
[feature-l/src/main/java/com/client/xvideos/l/net/AlbumList.kt:201](../feature-l/src/main/java/com/client/xvideos/l/net/AlbumList.kt#L201)

В `ScreenAlbumListSM.kt` и `AlbumList.kt` оставались закомментированные отладочные строки манипуляций со стейтами загрузки и дампа элементов.

**Исправление:**
- Мёртвый закомментированный код полностью удалён.

---

### T42 — Модульное тестирование алгоритма расчёта страниц и нормализации анимированных медиа Luscious. Средняя.

[feature-l/src/test/java/com/client/xvideos/l/net/AlbumPicsDetailsUtilsTest.kt](../feature-l/src/test/java/com/client/xvideos/l/net/AlbumPicsDetailsUtilsTest.kt)

Граничные случаи расчёта страниц и автоматическое определение анимированных ресурсов (`.gif`, `.mp4`) не были покрыты модульными тестами.

**Исправление:**
- Добавлен тестовый класс `AlbumPicsDetailsUtilsTest` (4 теста), покрывающий граничные значения (null, 0, отрицательные числа), потолок деления (`ceil`), выбор максимума между данными сервера и клиента, а также распознавание gif/видео в `normalizePictureUrls`.

---

## Сводка верификации

- `detekt`: 0 предупреждений и ошибок в `:feature-l`.
- `testDebugUnitTest`: 100% юнит-тестов `:feature-l` зелёные, включая `AlbumPicsDetailsUtilsTest`.
