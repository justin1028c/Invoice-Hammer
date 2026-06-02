# Project specific ProGuard rules

# Hilt rules
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

# Room: Allow obfuscation of entities but keep necessary infrastructure
-keep @androidx.room.Entity class *
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Dao interface *
-keepclassmembers class * {
  @androidx.room.PrimaryKey *;
  @androidx.room.ColumnInfo *;
  @androidx.room.Embedded *;
  @androidx.room.Relation *;
}

# Kotlin Serialization: Allow obfuscation but keep Serializer infrastructure
-keepattributes *Annotation*, EnclosingMethod, Signature
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keepclassmembers class ** {
    *** Companion;
    *** $serializer;
}

# Gemini SDK (Generative AI) rules
-keep class com.google.ai.client.generativeai.** { *; }
-dontwarn com.google.ai.client.generativeai.**

# SQLCipher: JNI reads field names — must not be stripped/renamed by R8
-keep class net.zetetic.database.sqlcipher.** { *; }
-keepclassmembers class net.zetetic.database.sqlcipher.** { *; }
-keep class net.sqlcipher.** { *; }
-keepclassmembers class net.sqlcipher.** { *; }
-dontwarn net.zetetic.database.sqlcipher.**

# Koin / app wiring (release DI)
-keep class org.koin.** { *; }
-keep class com.fordham.toolbelt.** { *; }
-keep class com.fordham.toolbelt.di.** { *; }
-keep class com.fordham.toolbelt.util.AndroidPlatformActions { *; }
-keep class com.fordham.toolbelt.data.implementation.AndroidDriveAuthTokenProvider { *; }

# Kotlin serialization
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-keepclassmembers class kotlinx.serialization.** { *; }
-keep @kotlinx.serialization.Serializable class * { *; }

# Stripe / Firebase (reflection-heavy SDKs)
-keep class com.stripe.** { *; }
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.stripe.**

# Native libs loaded from Kotlin
-keepclasseswithmembernames class * {
    native <methods>;
}

# Strip debug logs in production
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
