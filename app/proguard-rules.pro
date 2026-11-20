# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Ignore warnings for optional desktop java.beans classes referenced by Rhino
-dontwarn java.beans.**
-dontwarn org.mozilla.javascript.**

# Kotlin Serialization keep rules (required for JSON parsing, repositories, and extensions)
-keepattributes *Annotation*,InnerClasses,Signature,EnclosingMethod
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keep,allowobfuscation @kotlinx.serialization.Serializable class *
-keepclassmembers class * {
    kotlinx.serialization.KSerializer *();
}
-keep class kotlinx.serialization.** { *; }

# Keep CloudStream plugins, repositories, extensions, and API classes from obfuscation/shrinking
-keep class com.lagradost.cloudstream3.plugins.** { *; }
-keep class com.lagradost.cloudstream3.MainAPI { *; }
-keep class * extends com.lagradost.cloudstream3.MainAPI { *; }
-keep class * extends com.lagradost.cloudstream3.plugins.Plugin { *; }
-keep class com.lagradost.cloudstream3.utils.ExtractorApi { *; }
-keep class * extends com.lagradost.cloudstream3.utils.ExtractorApi { *; }

# Keep data models and sync provider / repository structures
-keep class com.lagradost.cloudstream3.syncproviders.providers.** { *; }
-keep class com.lagradost.cloudstream3.ui.result.** { *; }

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
