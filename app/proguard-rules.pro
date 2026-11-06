# ProGuard / R8 Rules for Fix Finder

-keepattributes Signature, *Annotation*, EnclosingMethod, InnerClasses

# Keep Data Models & Local Entities
-keep class com.example.fix_finder.data.model.** { *; }
-keep class com.example.fix_finder.data.local.** { *; }

# Gson Rules
-keepclassmembers class * implements com.google.gson.TypeAdapter { *; }
-keepclassmembers class * implements com.google.gson.TypeAdapterFactory { *; }
-keepclassmembers class * implements com.google.gson.JsonSerializer { *; }
-keepclassmembers class * implements com.google.gson.JsonDeserializer { *; }
-keepclassmembers enum * { *; }
-dontwarn com.google.gson.**

# Retrofit & OkHttp
-keepclassmembers class * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**
-dontwarn okhttp3.**

# Room Database
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.**

# Firebase & Credential Manager
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }
