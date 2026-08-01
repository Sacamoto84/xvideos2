# Правила R8.
#
# Принцип: здесь живёт только то, что R8 не может вывести сам, — рефлексия.
# Compose, Coil, Ktor, Hilt, Gson и kotlinx привозят свои consumer-правила
# внутри артефактов, дублировать их через `-keep class <библиотека>.** { *; }`
# не нужно: такие правила ничего не чинят, но выключают шринк на всей
# библиотеке.

# ---------- Атрибуты ----------

# Signature — дженерики в `object : TypeToken<List<Niche>>() {}`, без него
# Gson видит сырой List и падает.
# *Annotation* — @SerializedName, аннотации Hilt.
# InnerClasses/EnclosingMethod — те же анонимные подклассы TypeToken.
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Читаемые стектрейсы из релиза. Имена файлов при этом заменяются на "SourceFile",
# а восстановить исходные можно по build/outputs/mapping/release/mapping.txt.
# Этот файл нужно сохранять на каждый выпущенный билд.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

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
