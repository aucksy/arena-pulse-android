# Keep kotlinx.serialization generated serializers for the cached leaderboard.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keepclassmembers class com.arenapulse.android.** {
    *** Companion;
}
-keepclasseswithmembers class com.arenapulse.android.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.arenapulse.android.**$$serializer { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
