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

# Keep IWindowManager and its inner classes
-keep class android.view.IWindowManager { *; }
-keep class android.view.IWindowManager$Stub { *; }
-keep class android.view.IWindowManager$Stub$Proxy { *; }

# Keep all methods named setForcedDisplaySize in these classes
-keepclassmembers class android.view.IWindowManager$Stub$Proxy {
    void setForcedDisplaySize(...);
}
-keepclassmembers class android.view.IWindowManager {
    void setForcedDisplaySize(...);
}

# If you use clearForcedDisplaySize or other reflection methods, add similar rules:
-keepclassmembers class android.view.IWindowManager$Stub$Proxy {
    void clearForcedDisplaySize(...);
}
-keepclassmembers class android.view.IWindowManager {
    void clearForcedDisplaySize(...);
}
