# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in the Android SDK's proguard-android-optimize.txt
#
# For more details, see
#   https://developer.android.com/studio/build/shrink-code

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
-keepclassmembers class com.brave.adblock.AdBlockClient {
   public *;
   private *;
}
-keepclassmembers class org.mlm.browkorftv.webengine.webview.AndroidJSInterface {
   public *;
   private *;
}
-keep class org.mlm.browkorftv.webengine.webview.** { *; }
-keep class org.mlm.browkorftv.webengine.gecko.** { *; }

-dontobfuscate
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

#-keepclasseswithmembers class org.mlm.browkorftv.model.** {
#    <fields>;
#}