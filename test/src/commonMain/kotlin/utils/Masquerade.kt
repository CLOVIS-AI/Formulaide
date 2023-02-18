package opensavvy.formulaide.test.utils

import kotlinx.coroutines.withContext
import opensavvy.backbone.Ref.Companion.now
import opensavvy.formulaide.core.Auth
import opensavvy.formulaide.core.User
import opensavvy.formulaide.core.User.Role.Companion.role
import opensavvy.state.outcome.orThrow

suspend fun executeAsGuest(block: suspend () -> Unit) =
	withContext(Auth(User.Role.Guest, null)) { block() }

suspend fun executeAs(employee: User.Ref, block: suspend () -> Unit) =
	withContext(Auth(employee.now().orThrow().role, employee)) { block() }
