-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keep,includedescriptorclasses class cz.anty.purkynka.**$$serializer { *; }
-keepclassmembers class cz.anty.purkynka.** {
    *** Companion;
}
-keepclasseswithmembers class cz.anty.purkynka.** {
    kotlinx.serialization.KSerializer serializer(...);
}