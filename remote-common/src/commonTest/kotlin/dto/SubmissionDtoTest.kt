package opensavvy.formulaide.remote.dto

import arrow.core.flatMap
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import opensavvy.backbone.Ref.Companion.now
import opensavvy.formulaide.core.Field
import opensavvy.formulaide.core.Field.Companion.label
import opensavvy.formulaide.core.Form
import opensavvy.formulaide.core.Submission
import opensavvy.formulaide.fake.FakeDepartments
import opensavvy.formulaide.fake.FakeForms
import opensavvy.formulaide.test.structure.TestClock
import opensavvy.formulaide.test.utils.TestUsers.administratorAuth
import opensavvy.state.outcome.orThrow
import kotlin.js.JsName
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class SubmissionDtoTest {

	@Test
	@JsName("conversion")
	fun `submission DTO conversion`() = runTest(administratorAuth) {
		val departments = FakeDepartments()
		val forms = FakeForms(TestClock(testScheduler))

		val dept = departments.create("Test").orThrow()
		val form = forms.create(
			"Test",
			"Initial version",
			label("unused"), // we will not check the validity of the submission, so the contents of the form are irrelevant
			Form.Step(0, "First step", dept, null)
		)
			.flatMap { it.now() }
			.map { it.versionsSorted.first() }
			.orThrow()

		val submission = Submission(
			form,
			0,
			mapOf(
				Field.Id(0, 1) to "Test 1",
				Field.Id(1, 2, 3) to "Test 2",
			)
		)
		println("Submission: $submission")

		val dto = submission.toDto()
		println("DTO:        $dto")

		assertEquals(submission, dto.toCore(forms).orThrow())
	}
}
