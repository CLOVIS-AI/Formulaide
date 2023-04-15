package opensavvy.formulaide.remote.dto

import arrow.core.flatMap
import opensavvy.backbone.Ref.Companion.now
import opensavvy.formulaide.core.Field
import opensavvy.formulaide.core.Field.Companion.label
import opensavvy.formulaide.core.Form
import opensavvy.formulaide.core.Submission
import opensavvy.formulaide.fake.FakeDepartments
import opensavvy.formulaide.fake.FakeForms
import opensavvy.formulaide.test.structure.Suite
import opensavvy.formulaide.test.structure.TestExecutor
import opensavvy.formulaide.test.structure.clock
import opensavvy.formulaide.test.utils.TestUsers.administratorAuth
import kotlin.test.assertEquals

class SubmissionDtoTest : TestExecutor() {

	override fun Suite.register() {
		test("Submission DTO conversion", administratorAuth) {
			val departments = FakeDepartments()
			val forms = FakeForms(clock)

			val dept = departments.create("Test").bind()
			val form = forms.create(
				"Test",
				"Initial version",
				label("unused"), // we will not check the validity of the submission, so the contents of the form are irrelevant
				Form.Step(0, "First step", dept, null)
			)
				.flatMap { it.now() }
				.map { it.versionsSorted.first() }
				.bind()

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

			assertEquals(submission, dto.toCore(forms).bind())
		}
	}
}
