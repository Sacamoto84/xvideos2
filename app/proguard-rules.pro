# Правила R8.
#
# Принцип: здесь живёт только то, что R8 не может вывести сам, — рефлексия.
# Compose, Coil, Ktor, Hilt, Gson и kotlinx привозят свои consumer-правила
# внутри артефактов, дублировать их через `-keep class <библиотека>.** { *; }`
# не нужно: такие правила ничего не чинят, но выключают шринк на всей
# библиотеке.

# ---------- Обфускация выключена ----------

# R8 делает две независимые вещи: вырезает неиспользуемый код и переименовывает
# оставшийся. Здесь оставлено только первое.
#
# Что это даёт: стектрейсы из релиза читаются как есть, mapping.txt для их
# расшифровки не нужен и архивировать его на каждый выпуск не надо.
#
# Чего это НЕ отменяет: keep-правила ниже по-прежнему обязательны. Они защищают
# от *вырезания*, а не от переименования. Краш на старте (R8 вырезал
# конструктор WorkDatabase_Impl) случился именно из-за вырезания и повторится
# без правила для Room, даже с выключенной обфускацией.
#
# Включить обратно — убрать эту строку. Тогда вернуть и архивирование
# mapping.txt на каждую выпущенную сборку, иначе стектрейсы из продакшена
# восстановить нечем.
-dontobfuscate

# ---------- Атрибуты ----------

# Signature — дженерики в `object : TypeToken<List<Niche>>() {}`, без него
# Gson видит сырой List и падает.
# *Annotation* — @SerializedName, аннотации Hilt.
# InnerClasses/EnclosingMethod — те же анонимные подклассы TypeToken.
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Номера строк в стектрейсах. R8 инлайнит и склеивает методы, поэтому без
# LineNumberTable строки теряются даже без обфускации.
-keepattributes SourceFile,LineNumberTable

# ---------- Gson ----------

# Класс, у которого есть хотя бы одно поле с @SerializedName, — это модель,
# которую Gson заполняет рефлексией. Сохраняем такой класс ЦЕЛИКОМ.
#
# Почему не хватает обычного `-keepclassmembers ... @SerializedName <fields>`:
# он защищает только аннотированные поля. У моделей проекта рядом лежат поля
# без аннотации (например, в `l/net/AlbumList.kt` и `r/model/URL1.kt`), и их
# Gson ищет по имени. Обфускация сломала бы это молча: JSON бы распарсился,
# а поля остались бы null — без единого исключения в логе.
-if class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class <1> { *; }

# Gson пишет и читает enum по имени константы.
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Форматы на диске и в протоколе P2P. @SerializedName на них нет, значит под
# правило выше они не попадают, а Gson сопоставляет их по ИМЕНАМ ПОЛЕЙ —
# то есть имена полей здесь часть формата.
#
# Обфускация не стабильна между сборками. Без этих правил обновление
# приложения сделало бы нечитаемым всё, что записала предыдущая версия
# (.info загрузок, метаданные сохранённых лайков, кэш альбома), а два
# телефона с разными сборками перестали бы понимать друг друга по P2P.
# Сломалось бы это молча: JSON разбирается, поля остаются null.
-keep class com.client.xvideos.common.p2p.P2pManifest { *; }
-keep class com.client.xvideos.common.p2p.P2pManifestFile { *; }
-keep class com.client.xvideos.l.featured.saved.LCollectionConfig { *; }
-keep class com.client.xvideos.l.featured.saved.LSavedLikeMetadata { *; }
-keep class com.client.xvideos.l.featured.saved.LSavedLikePreview { *; }
-keep class com.client.xvideos.l.net.LAlbumBundleCache { *; }

# Пакеты моделей целиком — страховка от той же ошибки в будущем: модель,
# добавленная без @SerializedName, иначе сломается так же молча. Пакеты
# небольшие, потеря в размере несопоставима с ценой такого отказа.
-keep class com.client.xvideos.l.model.** { *; }
-keep class com.client.xvideos.r.model.** { *; }
-keep class com.client.xvideos.x.model.** { *; }
-keep class com.client.xvideos.common.collectionDB.model.** { *; }

# ---------- Room ----------

# Room ищет сгенерированную реализацию базы по имени класса и создаёт её
# рефлексией: Class.forName("<БазаДанных>_Impl").getDeclaredConstructor().newInstance().
# R8 этого вызова не видит, считает класс неинстанцируемым и вырезает у него
# конструктор — а следом, как «недостижимые», и createInvalidationTracker()
# с clearAllTables().
#
# Room 2.5.0 (приходит транзитивно через androidx.work) везёт неполное
# consumer-правило:
#     -keep class * extends androidx.room.RoomDatabase
# без блока членов, то есть сохраняет только имя класса. В 2.6+ его починили,
# дописав конструктор; здесь повторяем эту версию.
#
# Симптом без правила — краш на старте, ещё до первого экрана:
#     Unable to get provider androidx.startup.InitializationProvider:
#     Failed to create an instance of class androidx.work.impl.WorkDatabase
-keep class * extends androidx.room.RoomDatabase {
    <init>();
}

# ---------- kotlinx.serialization ----------

# Сериализаторы генерирует компилятор, так что имена полей обфускации не
# боятся — они уже зашиты в сгенерированный $serializer. Ронять нельзя сам
# сгенерированный код: без него `Json.decodeFromString` падает в рантайме.
# Задето три модели (x/search/model, l/model/UserProfile), то есть разбор
# выдачи X-поиска.
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

# ---------- Ktor / OkHttp ----------

# Ktor CIO дёргает JDK-API управления, которых на Android нет.
-dontwarn java.lang.management.**

# MediaType.Companion — object, Ktor достаёт его конструктор рефлексией.
-keepclassmembers class okhttp3.MediaType$Companion {
    <init>();
}

# Ktor и kotlinx тянут рефлексию в engine/serialization. Consumer-правила
# покрывают не всё, а цена ошибки здесь — отказ всей сети, поэтому пока
# держим целиком. Сужать — отдельной задачей, с прогоном на устройстве.
-keep class io.ktor.** { *; }
-keep class kotlinx.coroutines.** { *; }
-keep class kotlin.coroutines.** { *; }

# slf4j подтягивается транзитивно и резолвится рефлексией.
-keep class org.slf4j.** { *; }
-dontwarn org.slf4j.**
