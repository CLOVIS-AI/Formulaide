package opensavvy.formulaide.fake

import opensavvy.formulaide.fake.spies.SpyDepartments.Companion.spied
import opensavvy.formulaide.test.formTestSuite
import opensavvy.formulaide.test.structure.Suite
import opensavvy.formulaide.test.structure.TestExecutor
import opensavvy.formulaide.test.structure.clock
import opensavvy.formulaide.test.structure.prepared

class FakeFormTest : TestExecutor() {

	override fun Suite.register() {
		val departments by prepared { FakeDepartments().spied() }
		val templates by prepared { FakeTemplates(clock) }
		val forms by prepared { FakeForms(clock) }

		formTestSuite(departments, templates, forms)
	}

}
