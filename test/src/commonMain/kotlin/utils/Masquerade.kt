package opensavvy.formulaide.test.utils

import kotlinx.coroutines.withContext
import opensavvy.backbone.now
import opensavvy.formulaide.core.Auth
import opensavvy.formulaide.core.User
import opensavvy.formulaide.core.User.Role.Companion.role
import opensavvy.formulaide.test.structure.Setup
import opensavvy.formulaide.test.structure.TestScope
import opensavvy.formulaide.test.structure.prepare

suspend fun <T> executeAsGuest(block: suspend () -> T) =
	withContext(Auth(User.Role.Guest, null)) { block() }

suspend fun <T> TestScope.executeAs(employee: User.Ref, block: suspend () -> T) =
	withContext(Auth(employee.now().bind().role, employee)) { block() }

suspend fun <T> TestScope.executeAs(employee: Setup<User.Ref>, block: suspend () -> T) =
	executeAs(prepare(employee), block)
