# Код-ревью xvideos — проход 71

> **Срез:** `91e6837` + рабочее дерево · **Статус:** закрыт · **Индекс:** [все документы](README.md)

База прохода `91e6837`. Изменено 5 исходных файлов в модуле `feature-r`.

Линзы:
- `T` / Инкапсуляция внутреннего состояния потока данных в [ScreenRedProfileSM.kt](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/ui/profile/ScreenRedProfileSM.kt) (`_list` объявлен `private` и отдаётся наружу через неизменяемый `asStateFlow()`).
- `UI` / Фиксация стабильности Compose-контрактов для моделей экрана и навигации (`@Stable` на [RNavigationState.kt](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/ui/explorer/RNavigationState.kt), [ScreenSaved.kt](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/saved/ScreenSaved.kt) и [R_Saved_Collection.kt](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/common/saved/R_Saved_Collection.kt); `@Stable` на [ISearchTemplate.kt](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/common/search/ISearchTemplate.kt)), гарантия smart skipping элементов подсказок поиска (`@Immutable` на [SuggestionItem](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/common/search/ISearchTemplate.kt)).

Граница P2P (`core/.../p2p/`) заморожена по решению владельца от 11.09.2026 и не затрагивалась.

---

## Находки

### T38 — Публичный мутабельный MutableStateFlow _list в ScreenRedProfileSM. Низкая.

[feature-r/src/main/java/com/client/xvideos/r/ui/profile/ScreenRedProfileSM.kt:70-71](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/ui/profile/ScreenRedProfileSM.kt#L70)

Поле `_list` было объявлено как публичный `val _list = MutableStateFlow<List<GifsInfo>>(emptyList())`, а `list` ссылался на него без приведения к read-only `asStateFlow()`. Это нарушало инкапсуляцию ScreenModel и позволяло внешним вызывающим компонентам мутировать поток напрямую в обход методов бизнес-логики.

**Исправление:**
- Поле `_list` сделано `private`.
- Поле `list` экспортируется через `_list.asStateFlow()`.

---

### UI85 — Нестабильные контракты навигации, вкладок и коллекций R. Средняя.

[feature-r/src/main/java/com/client/xvideos/r/ui/explorer/RNavigationState.kt:12](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/ui/explorer/RNavigationState.kt#L12)
[feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/saved/ScreenSaved.kt:124](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/saved/ScreenSaved.kt#L124)
[feature-r/src/main/java/com/client/xvideos/r/common/saved/R_Saved_Collection.kt:20](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/common/saved/R_Saved_Collection.kt#L20)

Классы `RNavigationState`, `R_SavedTabSM` и `R_Saved_Collection` содержат состояние пользовательского интерфейса (снапшоты вкладок, позицию скролла, видимость диалогов), однако не имели аннотации `@Stable`. Из-за этого Compose компилятор не мог рассматривать их как стабильные при передаче в Composable-функции экрана Saved и родительской навигации, что приводило к избыточным рекомпозициям.

**Исправление:**
- Добавлена аннотация `@Stable` к `RNavigationState`, `R_SavedTabSM` и `R_Saved_Collection`.

---

### UI86 — Отсутствие @Immutable на модели подсказок поиска SuggestionItem и @Stable на ISearchTemplate. Низкая.

[feature-r/src/main/java/com/client/xvideos/r/common/search/ISearchTemplate.kt:23-28](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/common/search/ISearchTemplate.kt#L23)

Класс `SuggestionItem` используется в выпадающем списке подсказок поиска (`CustomBasicTextFieldContent`). Без аннотации `@Immutable` Compose компилятор считает элементы списка нестабильными и рекомпонует каждый элемент подсказки при любом обновлении списка. Базовый класс шаблона поиска `ISearchTemplate` также не был помечен как `@Stable`.

**Исправление:**
- Добавлен `@Immutable` к `SuggestionItem`.
- Добавлен `@Stable` к `ISearchTemplate`.
