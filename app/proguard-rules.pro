# Add project specific ProGuard rules here.

# Keep application classes
-keep class com.bicy.whitenoise.** { *; }

# Keep data classes for JSON serialization
-keep class com.bicy.whitenoise.storage.** { *; }
-keep class com.bicy.whitenoise.music.** { *; }
-keep class com.bicy.whitenoise.subPage.** { *; }
-keep class com.bicy.whitenoise.audio.** { *; }

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep Gson serialization
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Keep Lottie
-keep class com.airbnb.lottie.** { *; }

# Keep Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep Kotlin coroutines
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Keep security class
-keep class com.bicy.whitenoise.security.SecurityManager { *; }

# Obfuscate string literals for security (limited support)
-adaptclassstrings

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# Keep line numbers for debugging
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Optimization settings
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

# Optimization
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*,!code/allocation/variable

# Allow optimization
-allowaccessmodification
