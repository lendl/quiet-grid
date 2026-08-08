# kotlinx.serialization 1.6.3 bundles its own annotation-gated consumer rules for
# Companion/serializer() retention; only the $$serializer descriptor keep still
# needs an app-side rule, narrowed to actual @Serializable classes.
-if @kotlinx.serialization.Serializable class com.quietgrid.**
-keep,includedescriptorclasses class <1>$$serializer { *; }

# Hilt's @HiltViewModel/@LazyClassKey lookup matches ViewModel classes by Class.getName()
# against a string baked into Dagger-generated code at compile time. R8 is meant to keep
# that string in sync when it renames the class (via @IdentifierNameString), but under full
# R8 + class merging that rewrite doesn't reliably happen, causing the lookup to miss and
# Hilt to fall back to plain no-arg reflection construction, which crashes with
# NoSuchMethodException since these ViewModels take constructor args. Keeping names sidesteps
# the mismatch instead of depending on that rewrite.
-keepnames class * extends androidx.lifecycle.ViewModel
