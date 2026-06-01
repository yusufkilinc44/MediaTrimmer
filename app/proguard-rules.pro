-keep class androidx.media3.transformer.** { *; }
-keep class com.yusufkilinc.mediatrimmer.domain.model.** { *; }
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
