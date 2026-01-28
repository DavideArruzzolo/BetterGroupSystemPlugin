
-libraryjars <java.home>/jmods/java.base.jmod(!**.jar;!module-info.class)
-libraryjars <java.home>/jmods/java.logging.jmod(!**.jar;!module-info.class)
-libraryjars <java.home>/jmods/java.xml.jmod(!**.jar;!module-info.class)
-libraryjars <java.home>/jmods/java.sql.jmod(!**.jar;!module-info.class)
-libraryjars <java.home>/jmods/java.desktop.jmod(!**.jar;!module-info.class)

-libraryjars C:\Users\arruz\AppData\Roaming\Hytale\install\release\package\game\latest\Server\HytaleServer.jar

# Keep the Main Plugin Class (Entry Point) - specific methods required by Hytale
-keep public class dzve.BetterGroupSystemPlugin {
    public <init>(com.hypixel.hytale.server.core.plugin.JavaPluginInit);
    public void setup();
    public void shutdown();
}

# Keep Hytale API classes if they are in the project (unlikely, but good practice)
-keep class com.hypixel.hytale.** { *; }
-keep interface com.hypixel.hytale.** { *; }

# Aggressive Obfuscation:
# 1. Flatten all packages to 'a'
-repackageclasses 'a'
# 2. Allow access modification to enable more renaming (making public/private as needed)
-allowaccessmodification

# Serialization: Keep members with Jackson annotations to avoid breaking JSON data completely
-keepclassmembers class * {
    @com.fasterxml.jackson.annotation.* *;
}
-keep class com.fasterxml.jackson.** { *; }
-keepnames class com.fasterxml.jackson.** { *; }

-keepclassmembers class * {
    <init>(...);
}

-dontwarn lombok.**
-keep class lombok.** { *; }

-dontwarn javax.annotation.**
-dontwarn org.checkerframework.**
-dontwarn lombok.**
-dontwarn java.sql.**
-dontwarn java.beans.**
-dontwarn java.awt.**

-optimizationpasses 3
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*

# -obfuscationdictionary obfuscation-dictionary.txt
# -classobfuscationdictionary obfuscation-dictionary.txt
# -packageobfuscationdictionary obfuscation-dictionary.txt

# -printmapping mapping.txt

-dontnote **
