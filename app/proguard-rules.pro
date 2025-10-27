# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

############################################
# Android framework reflective lookups
############################################
# onClick handlers referenced from XML
-keepclassmembers class org.okane.voyagemapper.** {
    public void *(android.view.View);
}

# Components referenced by manifest / framework
-keep class ** extends android.app.Activity
-keep class ** extends android.app.Service
-keep class ** extends android.content.BroadcastReceiver
-keep class ** extends android.content.ContentProvider
-keep class ** extends android.app.Application
-keep class androidx.fragment.app.Fragment

############################################
# Google Maps / Places / Maps Utils
############################################
-keep class com.google.android.gms.maps.** { *; }
-keep class com.google.android.libraries.places.** { *; }
-keep class com.google.maps.android.** { *; }   # clustering etc.

############################################
# Your app code that’s reflectively accessed
############################################
# (safe blanket for your packages; tighten later if desired)
-keep class org.okane.voyagemapper.** { *; }

############################################
# Retrofit / OkHttp / Gson (if used)
############################################
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }
-keep class okhttp3.** { *; }
-keep class okio.** { *; }
-keep class com.google.gson.** { *; }
-keep class org.okane.voyagemapper.model.** { *; }  # your DTOs
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
