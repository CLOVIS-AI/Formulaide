package opensavvy.formulaide.fake

import kotlinx.coroutines.CoroutineScope
import opensavvy.formulaide.core.Form
import opensavvy.formulaide.test.FormTestCases
import opensavvy.formulaide.test.utils.TestClock.Companion.testClock

class FormTest : FormTestCases() {
	override suspend fun new(foreground: CoroutineScope, background: CoroutineScope): Form.Service =
		FakeForms(testClock())
}
