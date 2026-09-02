-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keep,includedescriptorclasses class com.aiwatch.**$$serializer { *; }
-keepclassmembers class com.aiwatch.** {
    *** Companion;
}
-keepclasseswithmembers class com.aiwatch.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Instantiated by Play services, never by our own code.
-keep class * extends com.google.android.gms.wearable.WearableListenerService
