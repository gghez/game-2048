# R8 keep rules for the release build.
#
# Compose, Material3, DataStore and play-services-games-v2 ship their own
# consumer rules, so almost nothing is needed here. The rules below only make
# explicit the reflection-touched surfaces called out in the codebase, so they
# survive R8 even if a future R8 default ever changes.

# Enum reflection: GameStatus.valueOf (GameViewModel.restore) and
# ThemeMode.valueOf (SettingsRepository) round-trip persisted state through the
# enum name. R8 keeps values()/valueOf() for enums by default; this is belt-and-
# suspenders so the implicit reflection never breaks.
-keepclassmembers enum com.gghez.game2048.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
