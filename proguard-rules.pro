
-libraryjars <java.home>/jmods/java.base.jmod(!**.jar;!module-info.class)
-libraryjars <java.home>/jmods/java.logging.jmod(!**.jar;!module-info.class)
-libraryjars <java.home>/jmods/java.xml.jmod(!**.jar;!module-info.class)
-libraryjars <java.home>/jmods/java.sql.jmod(!**.jar;!module-info.class)
-libraryjars <java.home>/jmods/java.desktop.jmod(!**.jar;!module-info.class)

# Essential Attributes for Jackson and Reflection
-keepattributes Signature,InnerClasses,*Annotation*,EnclosingMethod,SourceFile,LineNumberTable

# Keep SQLite JDBC Driver
-keep class org.sqlite.** { *; }
-keep interface org.sqlite.** { *; }

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

# Models: Allow renaming of classes and non-annotated members, but keep what Jackson needs
-keepclassmembers class dzve.model.** {
    @com.fasterxml.jackson.annotation.* *;
    <init>(...);
}
# Keep class names for models just in case of reflection/logging clarity (optional, user wanted MAX hiding, but this is safer for debugging)
# Remove "-keep class dzve.model.** { *; }" to allow full obfuscation of internal methods and field names (mapped by annotation)

# Config: Allow renaming, keep annotated
-keepclassmembers class dzve.config.** {
    @com.fasterxml.jackson.annotation.* *;
    <init>(...);
}

# Keep API classes for external developers (PUBLIC only)
-keep public class dzve.api.** {
    public *;
}
-keepclassmembers class dzve.api.** {
    public *;
}

# Keep API classes for external developers
-keep class dzve.api.** { *; }
-keepclassmembers class dzve.api.** { *; }

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
-dontwarn org.slf4j.**

-optimizationpasses 3
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*

# -obfuscationdictionary obfuscation-dictionary.txt
# -classobfuscationdictionary obfuscation-dictionary.txt
# -packageobfuscationdictionary obfuscation-dictionary.txt

# -printmapping mapping.txt

-dontnote **

# Keep directories (useful for resources structure)
-keepdirectories
