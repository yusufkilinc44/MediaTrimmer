-keep class com.arthenica.ffmpegkit.** { *; }
-keep class com.yusufkilinc.mediatrimmer.domain.model.** { *; }
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
