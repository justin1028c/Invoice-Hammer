package com.fordham.toolbelt.di

import org.koin.core.module.Module
import io.ktor.client.HttpClient

expect fun platformModule(): Module
expect fun platformHttpClient(): HttpClient

