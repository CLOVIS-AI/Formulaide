package opensavvy.formulaide.remote.server

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.debug.junit4.CoroutinesTimeout
import opensavvy.formulaide.core.User
import opensavvy.formulaide.fake.FakeDepartments
import opensavvy.formulaide.fake.FakeUsers
import opensavvy.formulaide.fake.spies.SpyDepartments.Companion.spied
import opensavvy.formulaide.fake.spies.SpyUsers.Companion.spied
import opensavvy.formulaide.remote.client.Users
import opensavvy.formulaide.remote.server.utils.TestClient
import opensavvy.formulaide.remote.server.utils.createTestServer
import opensavvy.formulaide.test.UserTestCases
import org.junit.Rule

class UserTest : UserTestCases() {

	@get:Rule
	val timeout = CoroutinesTimeout.seconds(5)

	override suspend fun new(
		foreground: CoroutineScope,
		background: CoroutineScope,
	): User.Service {
		val userService = FakeUsers().spied()

		val application = background.createTestServer(userService) {
			routing {
				users(userService, FakeDepartments().spied())
			}
		}

		return Users(
			TestClient(application.client),
			background.coroutineContext,
			FakeDepartments().spied(),
		).spied()
	}

}
