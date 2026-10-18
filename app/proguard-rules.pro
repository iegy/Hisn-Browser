# Keep Room-generated DAO implementation details discoverable at runtime.
-keep class androidx.room.** { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }

# WebView JavaScript bridges are intentionally not exposed by HISN.
# Do not add broad keep rules for arbitrary application classes.
