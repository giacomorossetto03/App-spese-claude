# R8 abilitato nel release. Room/Compose/Glance/DataStore includono consumer-rules.
# Sotto: regole per kotlinx.serialization ed enum usati via valueOf().

-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

# kotlinx.serialization (regole ufficiali)
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# Classi/serializer generati per i modelli di backup
-keep,includedescriptorclasses class com.personal.spese.**$$serializer { *; }
-keepclassmembers class com.personal.spese.core.backup.** { *; }

# Enum: preserva values()/valueOf() (usati nei mapper: ExpenseType.valueOf, ThemeMode.valueOf)
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
