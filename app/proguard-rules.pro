# SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
# SPDX-License-Identifier: GPL-3.0-or-later

# Proguard / R8 rules — populated incrementally as libraries are added.
#
# CameraX, ML Kit, Compose and Hilt ship their own consumer ProGuard rules
# inside their AARs, which R8 applies automatically; we only add what those
# rules do not already cover.

# Keep Hilt entry points
-keep class dagger.hilt.** { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }

# Preserve line numbers so release crash stack traces stay deobfuscatable
# against build/outputs/mapping/release/mapping.txt; hide the original
# source file name.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
