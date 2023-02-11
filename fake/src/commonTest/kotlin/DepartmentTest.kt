package opensavvy.formulaide.fake

import kotlinx.coroutines.ExperimentalCoroutinesApi
import opensavvy.formulaide.fake.spies.SpyDepartments.Companion.spied
import opensavvy.formulaide.test.departmentTestSuite
import opensavvy.formulaide.test.execution.Executor
import opensavvy.formulaide.test.execution.Suite

class FakeDepartmentTest : Executor() {

	@OptIn(ExperimentalCoroutinesApi::class)
	override fun Suite.register() {
		departmentTestSuite { FakeDepartments().spied() }
	}

}
