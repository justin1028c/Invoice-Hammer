package com.fordham.toolbelt.util

import java.util.UUID

actual fun randomUUID(): String = UUID.randomUUID().toString()
