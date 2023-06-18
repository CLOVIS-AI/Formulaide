package opensavvy.formulaide.fake

import opensavvy.formulaide.fake.spies.SpyDepartments.Companion.spied
import opensavvy.formulaide.fake.utils.commonIds
import opensavvy.formulaide.test.formTestSuite
import opensavvy.formulaide.test.identifierParsingSuite
import opensavvy.formulaide.test.structure.*

class FakeFormTest : TestExecutor() {

	override fun Suite.register() {
		val departments by prepared { FakeDepartments().spied() }
		val templates by prepared { FakeTemplates(clock) }
		val forms by prepared { FakeForms(clock) }

		formTestSuite(departments, templates, forms)

		identifierParsingSuite(
			forms,
			*commonIds,
		) { prepare(forms).Ref(it) }
	}

}
