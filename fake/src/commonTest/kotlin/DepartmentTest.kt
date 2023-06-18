package opensavvy.formulaide.fake

import opensavvy.formulaide.fake.utils.commonIds
import opensavvy.formulaide.test.departmentTestSuite
import opensavvy.formulaide.test.identifierParsingSuite
import opensavvy.formulaide.test.structure.Suite
import opensavvy.formulaide.test.structure.TestExecutor
import opensavvy.formulaide.test.structure.prepare
import opensavvy.formulaide.test.structure.prepared

class FakeDepartmentTest : TestExecutor() {

	override fun Suite.register() {
		val departments by prepared { FakeDepartments() }

		departmentTestSuite(departments)

		identifierParsingSuite(
			departments,
			*commonIds,
		) { prepare(departments).Ref(it) }
	}

}
