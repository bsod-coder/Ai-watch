# Keep the OpenRouter wire format intact under R8. Serializers are always passed
# explicitly, but keep the generated companions so reflection stays available.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keep,includedescriptorclasses class com.aiwatch.**$$serializer { *; }
-keepclassmembers class com.aiwatch.** {
    *** Companion;
}
-keepclasseswithmembers class com.aiwatch.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# OkHttp / OkIO
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Wear Data Layer listener services are instantiated by Play services.
-keep class * extends com.google.android.gms.wearable.WearableListenerService
