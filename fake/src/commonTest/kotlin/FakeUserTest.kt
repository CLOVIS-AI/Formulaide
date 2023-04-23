package opensavvy.formulaide.fake

import opensavvy.formulaide.fake.spies.SpyDepartments.Companion.spied
import opensavvy.formulaide.test.structure.Suite
import opensavvy.formulaide.test.structure.TestExecutor
import opensavvy.formulaide.test.structure.prepared
import opensavvy.formulaide.test.usersTestSuite

class FakeUserTest : TestExecutor() {

	override fun Suite.register() {
		val createDepartments by prepared { FakeDepartments().spied() }
		val createUsers by prepared { FakeUsers() }

		usersTestSuite(
			createDepartments = createDepartments,
			createUsers = createUsers,
		)
	}

}
