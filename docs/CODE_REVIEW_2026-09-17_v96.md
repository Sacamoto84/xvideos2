# Код-ревью: Итерация 117 (2026-09-17)

**Фокус ревью:** `:feature-l` и `:feature-r` (устранение гонок поиска в ScreenModel, защита файловых операций поиска лайков, очистка мертвого кода в нишах).

---

## 1. Контекст и цели
1. Проверить реализацию поиска альбомов [L_ScreenAlbumSearch.kt](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/albumSearch/L_ScreenAlbumSearch.kt) на предмет конкурентных гонок асинхронных корутин при повторном или быстром нажатии поиска.
2. Проверить файловую функцию сопоставления сохранённых лайков `lFindLikeFolder` в [LCollectionFs.kt](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/featured/saved/LCollectionFs.kt) на устойчивость к пустым и пробельным строкам.
3. Провести аудит [ScreenNicheSM.kt](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/ui/niche/ScreenNicheSM.kt) на предмет обработки краевых значений (пустое имя ниши) и накопленного закомментированного кода.
4. Разработать модульные тесты для файлового поиска лайков [LFindLikeFolderTest.kt](file:///g:/xvideos2/feature-l/src/test/java/com/client/xvideos/l/featured/saved/LFindLikeFolderTest.kt).

---

## 2. Выявленные замечания и исправления

### [S59] Гонка асинхронных запросов поиска альбомов в `ScreenLAlbumSearchSM`
- **Проблема:** Метод `ScreenLAlbumSearchSM.search()` запускал корутину в `screenModelScope` без сохранения ссылки на предыдущую задачу и без её отмены. При изменении запроса и повторном запуске более медленный первый запрос мог завершиться позже второго и перезаписать актуальный результат поиска (`result.value`), а также преждевременно перевести `isLoading.value` в `false`.
- **Решение:** В `ScreenLAlbumSearchSM` введено управление задачей `private var searchJob: Job? = null`:
  - При старте нового поиска вызывается `searchJob?.cancel()`.
  - Сброс `isLoading.value = false` перенесён в блок `finally`.
  - В `onDispose()` добавлен `searchJob?.cancel()`.
- **Статус:** Исправлено.

### [S60] Сканирование дискового корня по пустому URL в `lFindLikeFolder`
- **Проблема:** Если в `lFindLikeFolder(root: File, url: String)` передавалась пустая строка или строка из одних пробелов, проверка `startsWith("http")` возвращала `false`, после чего создавался `File(url)` (указывающий на текущую рабочую директорию процесса), а затем выполнялся холостой обход всех папок на диске `root.listFiles()`.
- **Решение:** В [LCollectionFs.kt](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/featured/saved/LCollectionFs.kt) добавлен ранний выход:
  ```kotlin
  val trimmed = url.trim()
  if (trimmed.isBlank()) return null
  ```
  Все дальнейшие проверки путей и URL переведены на использование санитизированной строки `trimmed`.
- **Статус:** Исправлено.

### [C26] Мертвый код и отсутствие проверки имени ниши в `ScreenNicheSM`
- **Проблема:**
  - В `ScreenNicheSM` находился блок из 23 строк закомментированного кода `expandMenuVideoList`.
  - В блоке инициализации `init` не проверялось имя ниши: при открытии с пустым или пробельным именем запускались холостые сетевые запросы к API RedGifs.
- **Решение:**
  - Закомментированный блок удалён.
  - Добавлена проверка `val trimmedNiche = nicheName.trim(); if (trimmedNiche.isNotBlank()) { ... } else { Timber.w(...) }`.
- **Статус:** Исправлено.

### [T63] Отсутствие модульных тестов для функции `lFindLikeFolder`
- **Проблема:** Критически важная функция поиска сохранённых медиаданных по локальным путям и ссылкам не имела изолированных юнит-тестов.
- **Решение:** Создан тестовый класс [LFindLikeFolderTest.kt](file:///g:/xvideos2/feature-l/src/test/java/com/client/xvideos/l/featured/saved/LFindLikeFolderTest.kt) с использованием `TemporaryFolder`, покрывающий 5 сценариев:
  1. Отсечение пустых и пробельных запросов без доступа к диску.
  2. Защита от поиска файлов вне корневой директории.
  3. Поиск папки по прямому локальному пути к медиафайлу.
  4. Поиск папки по удаленным URL (media и original).
  5. Корректный возврат `null` для несоответствующих URL.
- **Статус:** Исправлено.

---

## 3. Верификация
- Модульные тесты `:feature-l:testDebugUnitTest` и `:feature-r:testDebugUnitTest`: **Passed** (100%).
- Общепроектный набор тестов `testDebugUnitTest`: **Passed** (100%).
- Статический анализ `detekt`: **0 issues**.
- Релизная компиляция `compileReleaseKotlin`: **BUILD SUCCESSFUL in 11s**.
