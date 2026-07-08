# Hardened Android Obfuscation & ProGuard Configuration

# Android Core Components
-keep public class * extends android.app.Service
-keep public class * extends android.app.Application
-keep public class * extends android.app.Activity
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.view.View
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference
-keep public class * extends androidx.preference.Preference
-keep public class * extends com.google.android.vending.licensing.ILicensingService

# Hardened Obfuscation Dictionary
-obfuscationdictionary dictionary.txt
-classobfuscationdictionary dictionary.txt
-packageobfuscationdictionary dictionary.txt

# Preserve essential annotations, generics signature metadata, and Kotlin reflection metadata
-keepattributes Signature, *Annotation*, InnerClasses, EnclosingMethod, Metadata, AnnotationDefault

# Room Database: Preserve infrastructure and constructor reflection pathways
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class * implements androidx.room.RoomDatabaseConstructor { *; }
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Entity class * { *; }
-keepclassmembers class * {
  @androidx.room.PrimaryKey *;
  @androidx.room.ColumnInfo *;
  @androidx.room.Embedded *;
  @androidx.room.Relation *;
}

# Android WorkManager: Keep reflection-based instantiation of Workers
-keep class * extends androidx.work.ListenableWorker {
    <init>(...);
}

# Kotlin Serialization: Preserve serializer companion references
-keep @kotlinx.serialization.Serializable class * { *; }
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keepclassmembers class ** {
    *** Companion;
    *** $serializer;
}
-keepclassmembers class kotlinx.serialization.** { *; }

# Koin Dependency Injection
-keep class org.koin.** { *; }
-dontwarn org.koin.**

# Ktor HTTP Client Engine
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# Kotlin Coroutines internal resolution
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.coroutines.android.AndroidDispatcherFactory {
    public *** createDispatcher(...);
}
-dontwarn kotlinx.coroutines.**

# SQLCipher / SQLite Database Encryption
-keep class net.zetetic.database.sqlcipher.** { *; }
-keepclassmembers class net.zetetic.database.sqlcipher.** { *; }
-keep class net.sqlcipher.** { *; }
-keepclassmembers class net.sqlcipher.** { *; }
-dontwarn net.zetetic.database.sqlcipher.**

# Stripe payment SDK reflections
-keep class com.stripe.** { *; }
-dontwarn com.stripe.**

# Firebase & Google Services Client SDKs
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Gemini SDK (Generative AI) rules
-keep class com.google.ai.client.generativeai.** { *; }
-dontwarn com.google.ai.client.generativeai.**

# Native library bindings
-keepclasseswithmembernames class * {
    native <methods>;
}

# Strip debug logs in production builds
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Suppress warnings from missing Protobuf classes in Google MediaPipe Tasks SDK
-dontwarn com.google.protobuf.**

# Preserve BuildConfig fields for reflection in AndroidSecretProvider
-keep class com.fordham.toolbelt.BuildConfig { *; }
