package com.fordham.toolbelt.util

import platform.Foundation.NSUUID

actual fun randomUUID(): String = NSUUID().UUIDString()
