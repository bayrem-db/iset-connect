# Retrofit / Gson models keep their field names
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.iset.connect.models.** { *; }
-keep class com.iset.connect.network.ApiResponse { *; }
-dontwarn okhttp3.**
