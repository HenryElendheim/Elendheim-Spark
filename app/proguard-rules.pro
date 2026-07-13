# ProGuard / R8 rules.
# Keep kotlinx.serialization generated serializers so JSON export/import
# keeps working if minification is ever turned on.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class com.elendheim.spark.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep, includedescriptorclasses class com.elendheim.spark.model.**$$serializer { *; }
