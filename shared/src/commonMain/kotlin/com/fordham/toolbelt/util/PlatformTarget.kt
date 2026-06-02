package com.fordham.toolbelt.util

enum class PlatformTarget {
    Android,
    Ios
}

expect fun currentPlatformTarget(): PlatformTarget
