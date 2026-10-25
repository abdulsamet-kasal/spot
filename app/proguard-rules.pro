# Spoyt — release build rules.
# minifyEnabled is false for this personal-use app, so these are only consulted if
# shrinking is ever turned on.

# NewPipe Extractor is used via reflection-free Java APIs, but keep its model classes
# so R8 never strips constructors we reach through the extractor hierarchy.
-keep class org.schabi.newpipe.extractor.** { *; }
-dontwarn org.schabi.newpipe.extractor.**

# OkHttp / Okio
-dontwarn okhttp3.**
-dontwarn okio.**
-keepattributes Signature
-keepattributes *Annotation*

# Kotlin coroutines
-dontwarn kotlinx.coroutines.**
