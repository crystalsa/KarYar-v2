# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Preserve annotations, generics signatures, and enclosing methods for Room & Kotlin reflection
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable

# Preserve all enum values() and valueOf() methods (critical for Room TypeConverters like AttendanceStatus)
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Room Database, DAOs, Entities, TypeConverters, and generated _Impl classes
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class * extends androidx.room.RoomOpenHelper { *; }
-keep class * extends androidx.room.migration.Migration { *; }
-keep class **_Impl { *; }
-keep @androidx.room.Database class * { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Dao class * { *; }
-keepclassmembers class * {
    @androidx.room.TypeConverter *;
}

# Keep KarYar local database, converters, entities, and migration report models intact
-keep class com.karyar.app.data.local.AppDatabase { *; }
-keep class com.karyar.app.data.local.AppDatabase$* { *; }
-keep class com.karyar.app.data.local.MigrationReport { *; }
-keep class com.karyar.app.data.local.converter.** { *; }
-keepclassmembers class com.karyar.app.data.local.converter.** { *; }
-keep class com.karyar.app.data.local.entity.** { *; }
-keepclassmembers class com.karyar.app.data.local.entity.** { *; }
-keep enum com.karyar.app.data.local.entity.AttendanceStatus { *; }
-keep class com.karyar.app.data.local.dao.** { *; }
-keepclassmembers class com.karyar.app.data.local.dao.** { *; }

# Keep Domain Models used in calculations, UI states, PDF and CSV exports
-keep class com.karyar.app.domain.model.** { *; }
-keepclassmembers class com.karyar.app.domain.model.** { *; }

# Keep Font resources (Vazirmatn) and Report Export Utilities (PDF & CSV/Excel)
-keep class com.karyar.app.R$font { *; }
-keepclassmembers class com.karyar.app.R$font {
    public static <fields>;
}
-keep class com.karyar.app.util.** { *; }
-keepclassmembers class com.karyar.app.util.** { *; }

# Suppress optional Robolectric reflection lookup warning in AppDatabase.resolveMigrationContext
-dontwarn org.robolectric.**
