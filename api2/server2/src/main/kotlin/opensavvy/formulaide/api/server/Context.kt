package opensavvy.formulaide.api.server

import opensavvy.formulaide.api.Context
import opensavvy.spine.ktor.server.ContextGenerator

val context = ContextGenerator { Context() }
