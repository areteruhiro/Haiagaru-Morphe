# MPEs are minified independently; their private class names must not overlap.
-repackageclasses app.morphe.extension.isolated

# Go mobile binds JNI symbols to these exact class/method names.
-keep class go.** { *; }
-keep class app.morphe.extension.chmate.vpn.nativecore.** { *; }
