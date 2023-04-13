package opensavvy.formulaide.fake

import opensavvy.formulaide.test.structure.Suite
import opensavvy.formulaide.test.structure.TestExecutor
import opensavvy.formulaide.test.structure.clock
import opensavvy.formulaide.test.structure.prepared
import opensavvy.formulaide.test.templateTestSuite

class FakeTemplateTest : TestExecutor() {

	override fun Suite.register() {
		val templates by prepared { FakeTemplates(clock) }

		templateTestSuite(templates)
	}

}
