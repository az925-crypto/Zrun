# Add project specific ProGuard rules here.
# Keep Room generated classes.
-keep class * extends androidx.room.RoomDatabase
-keepclassmembers class * { @androidx.room.* <methods>; }
