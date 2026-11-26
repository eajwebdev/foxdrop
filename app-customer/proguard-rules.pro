# R8 full mode is on (gradle.properties). Libraries ship their own consumer rules (Hilt, Compose,
# kotlinx.serialization, Ktor). Add app-specific keep rules here only when a release test fails.

# Strip Timber/Log calls below WARN from release builds (§16: no logging of personal data).
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
