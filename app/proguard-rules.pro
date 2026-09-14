# ProGuard / R8 rules for SKT Takip

# Room Database Rules
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep class androidx.room.** { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase {
    public void <init>(...);
    public static ** getDatabase(...);
}

# Keep Entities and Data Models
-keep class com.example.data.** { *; }
-keep class com.example.auth.** { *; }
-keep class com.example.util.** { *; }
-keep class com.example.sync.** { *; }

# ML Kit Barcode & Text Recognition Rules
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_** { *; }
-dontwarn com.google.mlkit.**

# Keep Kotlin Reflections / Attributes
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-dontwarn org.json.**
