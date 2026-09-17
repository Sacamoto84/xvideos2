# Код-ревью: Итерация 115 (2026-09-17)

**Фокус ревью:** `:core` (сетевые утилиты воспроизведения видео-превью `urlVideoImage`, хэширование данных `toMD5` и очистка мертвого кода).

---

## 1. Контекст и цели
1. Провести аудит утилит хэширования [toMD5.kt](file:///g:/xvideos2/core/src/main/java/com/client/xvideos/common/util/toMD5.kt) на предмет платформно-независимого и детерминированного вычисления дайджестов для кэширования сетевых запросов и файловых таблиц.
2. Проверить механизм ротации CDN-зеркал и подмены хостов [UrlVideoLite.kt](file:///g:/xvideos2/core/src/main/java/com/client/xvideos/common/urlVideoImage/UrlVideoLite.kt) на обработку краевых случаев (отсутствие пути, некорректные или пустые URL).
3. Удалить заброшенный файл [UrlImage.kt](file:///g:/xvideos2/core/src/main/java/com/client/xvideos/common/urlVideoImage/UrlImage.kt) (24 строки полностью закомментированного кода).
4. Разработать модульные тесты для хелперов воспроизведения превью [UrlVideoLiteTest.kt](file:///g:/xvideos2/core/src/test/java/com/client/xvideos/common/urlVideoImage/UrlVideoLiteTest.kt) и дополнить [FormattingUtilsTest.kt](file:///g:/xvideos2/core/src/test/java/com/client/xvideos/common/util/FormattingUtilsTest.kt).

---

## 2. Выявленные замечания и исправления

### [S57] Платформозависимое хэширование в `toMD5()` при отсутствии явной кодировки
- **Проблема:** Метод `String.toMD5()` вызывал `this.toByteArray()` без указания кодировки. В зависимости от системной локали ОС/JVM (например, Windows-1251 против UTF-8) строки, содержащие не-ASCII символы (кириллические поисковые запросы раздела L), давали разные MD5-хэши, что ломало детерминированность ключей сетевого кэша `Repository` и папок `FolderTable`.
- **Решение:** В [toMD5.kt](file:///g:/xvideos2/core/src/main/java/com/client/xvideos/common/util/toMD5.kt) явно указана кодировка: `this.toByteArray(Charsets.UTF_8)`.
- **Статус:** Исправлено.

### [S58] Сбой подмены хоста в `String.withHost` для URL без пути
- **Проблема:** В `UrlVideoLite.kt` метод `withHost` искал слэш пути после схемы `indexOf('/', startIndex = schemeEnd + 3)`. Если адрес не содержал конечного слэша (например, `http://cdn.com`), возвращался `null`, и fallback-кандидаты зеркал не генерировались.
- **Решение:** Поиск пути сделан опциональным: `val pathAndQuery = if (pathStart >= 0) substring(pathStart) else ""`.
- **Статус:** Исправлено.

### [L42] Мертвый файл `UrlImage.kt` в пакете `urlVideoImage`
- **Проблема:** После переноса основного компонента `UrlImage` в пакет `coil`, в `common/urlVideoImage/UrlImage.kt` оставался полностью закомментированный файл (24 строки), создававший визуальный мусор и риск коллизий имён.
- **Решение:** Файл удалён через VCS.
- **Статус:** Исправлено.

### [T62] Отсутствие тестов для алгоритма формирования CDN-кандидатов и `toMD5`
- **Проблема:** Хелперы `xPreviewVideoCandidates`, `normalizedPreviewUrlOrNull`, `withHost` были приватными и не имели тестового покрытия. Также отсутствовали тесты на `toMD5`.
- **Решение:**
  - Область видимости хелперов в `UrlVideoLite.kt` изменена с `private` на `internal`.
  - Создан тестовый класс [UrlVideoLiteTest.kt](file:///g:/xvideos2/core/src/test/java/com/client/xvideos/common/urlVideoImage/UrlVideoLiteTest.kt) с проверкой фильтрации null-значений, замены хостов и генерации уникального пула CDN-зеркал.
  - В [FormattingUtilsTest.kt](file:///g:/xvideos2/core/src/test/java/com/client/xvideos/common/util/FormattingUtilsTest.kt) добавлены тесты на детерминированность `toMD5` для ASCII и UTF-8 строк.
- **Статус:** Исправлено.

---

## 3. Верификация
- Модульные тесты модуля `:core:testDebugUnitTest`: **Passed** (231 тест, 100%).
- Статический анализ `:core:detekt`: **0 issues**.
