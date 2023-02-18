package opensavvy.formulaide.remote.dto

import arrow.core.flatMap
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import opensavvy.backbone.Ref.Companion.now
import opensavvy.formulaide.core.Field
import opensavvy.formulaide.core.Form
import opensavvy.formulaide.core.Record
import opensavvy.formulaide.core.Submission
import opensavvy.formulaide.fake.FakeDepartments
import opensavvy.formulaide.fake.FakeForms
import opensavvy.formulaide.fake.FakeRecords
import opensavvy.formulaide.test.utils.TestClock.Companion.testClock
import opensavvy.formulaide.test.utils.TestUsers
import opensavvy.formulaide.test.utils.TestUsers.administratorAuth
import opensavvy.formulaide.test.utils.TestUsers.employeeAuth
import opensavvy.state.outcome.orThrow
import kotlin.js.JsName
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class RecordDtoTest {

	@Test
	@JsName("conversion")
	fun `record DTO conversion`() = runTest(administratorAuth) {
		val departments = FakeDepartments()
		val clock = testClock()
		val forms = FakeForms(clock)
		val records = FakeRecords(clock)

		val dept = departments.create("Test").orThrow()
		val form = forms.create(
			"Test",
			"Initial version",
			Field.label("unused"), // we will not check the validity of the submission, so the contents of the form are irrelevant
			Form.Step(0, "First step", dept, null),
			Form.Step(1, "Second step", dept, null),
		)
			.tap { it.publicize().orThrow() }
			.flatMap { it.now() }
			.map { it.versionsSorted.first() }
			.orThrow()

		val initialSubmission = records.create(
			Submission(
				form,
				null,
				mapOf(
					Field.Id(0, 1) to "Test 1",
					Field.Id(1, 2, 3) to "Test 2",
				)
			)
		).orThrow()
			.now().orThrow()
			.historySorted.first().submission!!

		val editedSubmission = records.create(
			Submission(
				form,
				null,
				mapOf(
					Field.Id(0, 1) to "Test 1",
					Field.Id(1, 2, 3) to "Test 3",
				)
			)
		).orThrow()
			.now().orThrow()
			.historySorted.first().submission!!

		val record = Record(
			form,
			clock.now(),
			clock.now(),
			listOf(
				Record.Diff.Initial(
					submission = initialSubmission,
					author = null,
					firstStep = 0,
					at = clock.now(),
				),
				Record.Diff.EditInitial(
					submission = editedSubmission,
					author = employeeAuth.user!!,
					reason = "The answer was obviously wrong",
					currentStatus = Record.Status.Step(0),
					at = clock.now(),
				),
				Record.Diff.Accept(
					submission = null,
					author = employeeAuth.user!!,
					source = Record.Status.Step(0),
					target = Record.Status.Step(1),
					reason = "Now it's correct",
					at = clock.now(),
				),
				Record.Diff.MoveBack(
					author = administratorAuth.user!!,
					source = Record.Status.Step(1),
					target = Record.Status.Step(0),
					reason = "It was obviously wrong",
					at = clock.now(),
				),
				Record.Diff.Refuse(
					author = employeeAuth.user!!,
					source = Record.Status.Step(0),
					reason = "It was wrong",
					at = clock.now(),
				)
			)
		)
		println("Record: $record")

		val dto = record.toDto()
		println("DTO:    $dto")

		assertEquals(record, dto.toCore(forms, TestUsers, initialSubmission.backbone).orThrow())
	}

}
