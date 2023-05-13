package opensavvy.formulaide.fake

import opensavvy.formulaide.fake.utils.commonIds
import opensavvy.formulaide.test.identifierParsingSuite
import opensavvy.formulaide.test.structure.*
import opensavvy.formulaide.test.templateTestSuite

class FakeTemplateTest : TestExecutor() {

	override fun Suite.register() {
		val templates by prepared { FakeTemplates(clock) }

		templateTestSuite(templates)

		identifierParsingSuite(
			templates,
			*commonIds,
		) { prepare(templates).Ref(it) }
	}

}
