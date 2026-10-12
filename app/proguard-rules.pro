# AVA — AI Companion ProGuard Rules
# Developed by Fahim Khan

# Keep Hilt
-keep class dagger.hilt.** { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }
-keepnames @dagger.hilt.android.lifecycle.HiltViewModel class *

# Keep Room entities
-keep class com.fahimkhan.ava.ai.memory.** { *; }

# Keep Retrofit / OkHttp
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }
-keep class retrofit2.** { *; }

# Keep Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }

# Keep ML Kit
-keep class com.google.mlkit.** { *; }

# Keep Generative AI (Gemini)
-keep class com.google.ai.** { *; }

# Keep Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# AVA: keep all screen/viewmodel classes
-keep class com.fahimkhan.ava.ui.** { *; }
-keep class com.fahimkhan.ava.services.** { *; }
-keep class com.fahimkhan.ava.security.** { *; }
