# kotlinx.serialization: keep generated serializers for @Serializable classes.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.quranforwomen.app.**$$serializer { *; }
-keepclassmembers class com.quranforwomen.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.quranforwomen.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
