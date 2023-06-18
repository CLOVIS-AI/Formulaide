package opensavvy.formulaide.remote.server

import opensavvy.formulaide.fake.FakeDepartments
import opensavvy.formulaide.fake.FakeUsers
import opensavvy.formulaide.fake.spies.SpyDepartments.Companion.spied
import opensavvy.formulaide.remote.client.RemoteUsers
import opensavvy.formulaide.remote.server.utils.TestClient
import opensavvy.formulaide.remote.server.utils.createTestServer
import opensavvy.formulaide.test.structure.Suite
import opensavvy.formulaide.test.structure.TestExecutor
import opensavvy.formulaide.test.structure.prepare
import opensavvy.formulaide.test.structure.prepared
import opensavvy.formulaide.test.usersTestSuite

class RemoteUserTest : TestExecutor() {

	override fun Suite.register() {
		val createDepartments by prepared { FakeDepartments().spied() }

		val createUsers by prepared {
			val users = FakeUsers()
			val departments = prepare(createDepartments)

			val application = backgroundScope.createTestServer(users) {
				routing {
					users(users, departments)
				}
			}

			RemoteUsers(
				TestClient(application.client),
				backgroundScope,
				departments,
			)
		}

		usersTestSuite(createDepartments, createUsers)
	}

}
