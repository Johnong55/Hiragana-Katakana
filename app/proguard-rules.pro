# Keep kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class vn.kana.**$$serializer { *; }
-keepclassmembers class vn.kana.** {
    *** Companion;
}
-keepclasseswithmembers class vn.kana.** {
    kotlinx.serialization.KSerializer serializer(...);
}
