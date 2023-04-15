package opensavvy.formulaide.fake

import opensavvy.formulaide.fake.spies.SpyDepartments.Companion.spied
import opensavvy.formulaide.test.departmentTestSuite
import opensavvy.formulaide.test.structure.Suite
import opensavvy.formulaide.test.structure.TestExecutor
import opensavvy.formulaide.test.structure.prepared

class FakeDepartmentTest : TestExecutor() {

	override fun Suite.register() {
		val departments by prepared { FakeDepartments().spied() }

		departmentTestSuite(departments)
	}

}
