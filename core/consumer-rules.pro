# Core is consumed by :app and :wear; keep the rules that protect the wire format.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

# We always pass serializers explicitly (ModelEntry.serializer() etc.), so the
# reflective lookup path is not needed. Keep the generated companions anyway so
# that a future reified call does not silently break under R8.
-keepclassmembers class com.aiwatch.core.** {
    *** Companion;
}
-keepclasseswithmembers class com.aiwatch.core.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.aiwatch.core.**$$serializer { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
