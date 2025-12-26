#############################################
# Project-specific R8 / ProGuard rules
#############################################

# --- Useful debug mapping options (optional) ---
# Uncomment to preserve line numbers for stack traces
#-keepattributes SourceFile,LineNumberTable
# Uncomment (requires SourceFile kept) to hide original file names
#-renamesourcefileattribute SourceFile


#############################################
# Gson
#############################################

# Keep (but allow obfuscation of) fields annotated with @SerializedName
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# Gson TypeToken is used for generic type reflection
-keepattributes Signature
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken


#############################################
# Retrofit (and Kotlin suspend support)
#############################################

# Retrofit needs runtime annotations on interfaces/methods/params
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,AnnotationDefault,Signature

# Keep Retrofit service interfaces (R8 full mode can strip signatures otherwise)
-if interface * { @retrofit2.http.* public *** *(...); }
-keep,allowoptimization,allowshrinking,allowobfuscation class <3>

# Keep generic signature of Call/Response (used in adapters / suspend wrapping)
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# Suspend functions are wrapped in Continuation; generic type info can be needed
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
