@file:OptIn(KtorExperimentalLocationsAPI::class)

package me.devnatan.katan.webserver.environment.routes

import io.ktor.locations.*

@Location("/")
class IndexRoute

@Location("/info")
class InfoRoute