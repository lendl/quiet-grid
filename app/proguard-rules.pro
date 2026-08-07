# kotlinx.serialization 1.6.3 bundles its own annotation-gated consumer rules for
# Companion/serializer() retention; only the $$serializer descriptor keep still
# needs an app-side rule, narrowed to actual @Serializable classes.
-if @kotlinx.serialization.Serializable class com.quietgrid.**
-keep,includedescriptorclasses class <1>$$serializer { *; }
