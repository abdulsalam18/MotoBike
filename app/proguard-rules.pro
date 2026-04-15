-keep class com.android.**{*;}
-keep class com.squareup.**{*;}
-keep class org.jsoup.**{*;}
-keep public class com.google.android.gms.ads.**{public *;}
-keep public class com.google.ads.**{public *;}
-dontwarn android.webkit.JavascriptInterface
-keepattributes JavascriptInterface
-keep public class com.android.MemoryManager{public *;}
-keepattributes Exceptions, InnerClasses, Signature, Deprecated, SourceFile, LineNumberTable, *Annotation*, EnclosingMethod
-keep class * extends java.util.ListResourceBundle {
   protected Object[][] getContents();
}
-keep public class com.google.android.gms.common.internal.safeparcel.SafeParcelable {
   public static final *** NULL;
}
-keepnames @com.google.android.gms.common.annotation.KeepName class *
-keepclassmembernames class * {
   @com.google.android.gms.common.annotation.KeepName *;
}
-keepnames class * implements android.os.Parcelable {
   public static final ** CREATOR;
}
# =========================
# Your App Package
# =========================
-keep class com.abdulsalam.motobike.** { *; }
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }