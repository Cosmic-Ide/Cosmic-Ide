-dontobfuscate
-keepattributes LineNumberTable,SourceFile
-allowaccessmodification
-repackageclasses ''

-keep class javax.** { *; }
# we don't load kotlin-stdlib when running projects for better efficiency,
# so these will classes be needed by the programs when running.
-keep,allowoptimization class kotlin.** { *; }
# for kotlin compiler
-keep,allowoptimization class com.intellij.** { *; }
-keep,allowoptimization class org.jetbrains.kotlin.** { *; }
-keep,allowoptimization class org.xml.sax.** { *; }
-keep,allowoptimization class com.sun.xml.internal.stream.** { *; }
-keep,allowoptimization class jdk.xml.internal.** { *; }
# proguard's inlining breaks gson
-keep class org.jf.util.BitSetUtils { *; }
# -keepnames class com.google.googlejavaformat.** { *; }
# -keep,allowshrinking class com.sun.tools.classfile.** { *; }
