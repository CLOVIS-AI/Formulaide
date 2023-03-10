package opensavvy.formulaide.fake

import kotlinx.coroutines.ExperimentalCoroutinesApi
import opensavvy.formulaide.fake.spies.SpyDepartments.Companion.spied
import opensavvy.formulaide.fake.spies.SpyUsers.Companion.spied
import opensavvy.formulaide.test.execution.Executor
import opensavvy.formulaide.test.execution.Suite
import opensavvy.formulaide.test.execution.prepared
import opensavvy.formulaide.test.usersTestSuite

class FakeUserTest : Executor() {

	@OptIn(ExperimentalCoroutinesApi::class)
	override fun Suite.register() {
		val createDepartments by prepared { FakeDepartments().spied() }
		val createUsers by prepared { FakeUsers().spied() }

		usersTestSuite(
			createDepartments = createDepartments,
			createUsers = createUsers,
		)
	}

}
