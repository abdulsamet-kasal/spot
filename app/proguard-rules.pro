# Spoyt — release build rules.
# R8 is enabled for release (isMinifyEnabled = true). NewPipe Extractor walks class
# hierarchies and interprets YouTube's JS player with Rhino, so both need broad keeps.

# --- NewPipe Extractor -------------------------------------------------------
# Keep the whole extractor API surface: services/extractors are looked up by name and
# models are built reflectively in a few places.
-keep class org.schabi.newpipe.extractor.** { *; }
-dontwarn org.schabi.newpipe.extractor.**
-dontwarn org.schabi.newpipe.**

# --- Rhino (JavaScript engine used by NewPipe's signature deciphering) -------
# Rhino loads classes by name and reflects over its own internals.
-keep class org.mozilla.javascript.** { *; }
-dontwarn org.mozilla.javascript.**

# --- OkHttp / Okio -----------------------------------------------------------
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# --- Kotlin / coroutines -----------------------------------------------------
-dontwarn kotlinx.coroutines.**
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault

# Keep source + line numbers so crash.log stack traces stay readable.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Room generates code that reads entities directly; nothing to keep, but silence
# warnings from sqlite vendors pulled in transitively.
-dontwarn android.room.**
