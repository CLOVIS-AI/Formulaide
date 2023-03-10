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
import opensavvy.formulaide.test.execution.prepare
import opensavvy.formulaide.test.execution.prepared
import opensavvy.formulaide.test.usersTestSuite

class RemoteUserTest : Executor() {

	@OptIn(ExperimentalCoroutinesApi::class)
	override fun Suite.register() {
		val createDepartments by prepared { FakeDepartments().spied() }

		val createUsers by prepared {
			val users = FakeUsers().spied()
			val departments = prepare(createDepartments)

			val application = backgroundScope.createTestServer(users) {
				routing {
					users(users, departments)
				}
			}

			Users(
				TestClient(application.client),
				backgroundScope.coroutineContext,
				departments,
			)
		}

		usersTestSuite(createDepartments, createUsers)
	}

}
