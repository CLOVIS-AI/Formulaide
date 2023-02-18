package opensavvy.formulaide.remote.server

import kotlinx.coroutines.ExperimentalCoroutinesApi
import opensavvy.formulaide.fake.FakeDepartments
import opensavvy.formulaide.fake.FakeUsers
import opensavvy.formulaide.fake.spies.SpyDepartments.Companion.spied
import opensavvy.formulaide.fake.spies.SpyUsers.Companion.spied
import opensavvy.formulaide.remote.client.Users
import opensavvy.formulaide.remote.server.utils.TestClient
import opensavvy.formulaide.remote.server.utils.createTestServer
import opensavvy.formulaide.test.execution.Executor
import opensavvy.formulaide.test.execution.Suite
import opensavvy.formulaide.test.usersTestSuite

class RemoteUserTest : Executor() {

	@OptIn(ExperimentalCoroutinesApi::class)
	override fun Suite.register() {
		usersTestSuite(
			createDepartments = { FakeDepartments().spied() },
			createUsers = {
				val users = FakeUsers().spied()

				val application = backgroundScope.createTestServer(users) {
					routing {
						users(users, FakeDepartments().spied())
					}
				}

				Users(
					TestClient(application.client),
					backgroundScope.coroutineContext,
					FakeDepartments().spied(),
				)
			},
		)
	}

}
