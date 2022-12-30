package opensavvy.formulaide.fake

import kotlinx.coroutines.CoroutineScope
import opensavvy.formulaide.core.Template
import opensavvy.formulaide.test.TemplateTestCases
import opensavvy.formulaide.test.utils.TestClock.Companion.testClock

class TemplateTest : TemplateTestCases() {

	override suspend fun new(
		foreground: CoroutineScope,
		background: CoroutineScope,
	): Template.Service = FakeTemplates(testClock())

}
