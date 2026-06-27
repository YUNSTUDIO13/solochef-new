# ─── Kotlin Serialization ───
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keep,includedescriptorclasses class com.example.solochef.**$$serializer { *; }
-keepclassmembers class com.example.solochef.** {
    *** Companion;
}
-keepclasseswithmembers class com.example.solochef.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.example.solochef.model.** {
    <fields>;
}

# ─── Compose ───
-keep class androidx.compose.** { *; }

# ─── Coil ───
-keep class coil.** { *; }
-keep class okhttp3.** { *; }
-keep class okio.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# ─── Coroutines ───
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
