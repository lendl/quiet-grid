# kotlinx.serialization bundles its own annotation-gated consumer rules for
# Companion/serializer() retention; only the $$serializer descriptor keep still
# needs an app-side rule, narrowed to actual @Serializable classes.
-if @kotlinx.serialization.Serializable class com.quietgrid.**
-keep,includedescriptorclasses class <1>$$serializer { *; }

# lifecycle's viewModel { } keys each ViewModel by its class name. R8 must never give two
# ViewModels one name by renaming or merging them, or a screen gets another screen's ViewModel.
-keepnames class * extends androidx.lifecycle.ViewModel
