# Правила R8.
#
# Принцип: здесь живёт только то, что R8 не может вывести сам, — рефлексия и
# метаданные стектрейсов. Compose, Coil, Ktor и Hilt привозят свои
# consumer-правила внутри артефактов.

# ---------- Обфускация выключена ----------

# R8 делает две независимые вещи: вырезает неиспользуемый код и переименовывает
# оставшийся. Здесь оставлено только первое.
#
# Что это даёт: стектрейсы из релиза читаются как есть, mapping.txt для их
# расшифровки не нужен.
#
# Чего это НЕ отменяет: keep-правила ниже по-прежнему обязательны — они
# защищают от *вырезания*, а не от переименования.
-dontobfuscate

# ---------- Атрибуты ----------

# Signature/InnerClasses/EnclosingMethod и аннотации нужны Hilt, Voyager и reflection.
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Номера строк в стектрейсах. R8 инлайнит и склеивает методы, поэтому без
# LineNumberTable строки теряются даже без обфускации.
-keepattributes SourceFile,LineNumberTable

# ---------- Enums & kotlinx.serialization ----------

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Сериализаторы генерирует компилятор kotlinx.serialization; имена полей уже
# зашиты в $serializer, но сам сгенерированный Companion/serializer нельзя вырезать.
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
