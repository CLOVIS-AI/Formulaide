package opensavvy.formulaide.remote.server

import io.ktor.server.testing.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.debug.junit4.CoroutinesTimeout
import opensavvy.formulaide.core.User
import opensavvy.formulaide.fake.FakeDepartments
import opensavvy.formulaide.fake.FakeUsers
import opensavvy.formulaide.fake.spies.SpyDepartments.Companion.spied
import opensavvy.formulaide.fake.spies.SpyUsers.Companion.spied
import opensavvy.formulaide.remote.client.Users
import opensavvy.formulaide.remote.server.utils.TestClient
import opensavvy.formulaide.remote.server.utils.TestServer
import opensavvy.formulaide.server.users
import opensavvy.formulaide.test.UserTestCases
import org.junit.Rule

class UserTest : UserTestCases(), TestServer {

	@get:Rule
	val timeout = CoroutinesTimeout.seconds(5)

	override lateinit var application: TestApplication

	override fun TestApplicationBuilder.configureTestServer() {
		routing {
			users(FakeUsers().spied(), FakeDepartments().spied())
		}
	}

	override suspend fun new(
		foreground: CoroutineScope,
		background: CoroutineScope,
	): User.Service = Users(
		TestClient(application.client),
		background.coroutineContext,
		FakeDepartments().spied(),
	).spied()

}
