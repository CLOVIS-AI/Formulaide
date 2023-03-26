package opensavvy.formulaide.fake

import kotlinx.coroutines.ExperimentalCoroutinesApi
import opensavvy.formulaide.fake.spies.SpyDepartments.Companion.spied
import opensavvy.formulaide.test.execution.Executor
import opensavvy.formulaide.test.execution.Suite
import opensavvy.formulaide.test.execution.prepared
import opensavvy.formulaide.test.formTestSuite
import opensavvy.formulaide.test.utils.TestClock.Companion.testClock

class FakeFormTest : Executor() {

	@OptIn(ExperimentalCoroutinesApi::class)
	override fun Suite.register() {
		val departments by prepared { FakeDepartments().spied() }
		val templates by prepared { FakeTemplates(testClock()) }
		val forms by prepared { FakeForms(testClock()) }

		formTestSuite(departments, templates, forms)
	}

}
