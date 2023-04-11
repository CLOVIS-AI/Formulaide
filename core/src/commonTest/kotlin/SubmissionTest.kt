package opensavvy.formulaide.core

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import opensavvy.backbone.Ref.Companion.now
import opensavvy.formulaide.core.Field.Companion.arity
import opensavvy.formulaide.core.Field.Companion.choice
import opensavvy.formulaide.core.Field.Companion.group
import opensavvy.formulaide.core.Field.Companion.input
import opensavvy.formulaide.core.Field.Companion.label
import opensavvy.formulaide.fake.FakeDepartments
import opensavvy.formulaide.fake.FakeFiles
import opensavvy.formulaide.fake.FakeForms
import opensavvy.formulaide.test.assertions.shouldBeInvalid
import opensavvy.formulaide.test.assertions.shouldSucceed
import opensavvy.formulaide.test.assertions.shouldSucceedAnd
import opensavvy.formulaide.test.structure.TestClock
import opensavvy.formulaide.test.utils.TestUsers.administratorAuth
import opensavvy.state.outcome.orThrow
import kotlin.js.JsName
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class SubmissionTest {

	//region Utilities

	private suspend fun createTestForm(field: Field) = withContext(administratorAuth) {
		FakeForms(TestClock(coroutineContext[TestCoroutineScheduler]!!))
			.create(
				"Test",
				"First version",
				field,
				Form.Step(
					0,
					"Validation",
					FakeDepartments().create("Test department for submissions").orThrow(),
					null,
				)
			).orThrow()
			.now().orThrow()
	}

	//endregion
	//region Simple cases

	@Test
	@JsName("parseLabel")
	fun `labels do not appear in submissions`() = runTest {
		val files = FakeFiles(TestClock(testScheduler))
		val form = createTestForm(
			label("Label"),
		)

		val submission = Submission(
			form = form.versionsSorted.first(),
			formStep = null,
			emptyMap(), // it's a label, it should not appear here
		)

		println(submission)
		shouldSucceed(submission.parse(files))
	}

	@Test
	@JsName("parseInputMissing")
	fun `inputs are mandatory`() = runTest {
		val files = FakeFiles(TestClock(testScheduler))
		val form = createTestForm(
			input("Input", Input.Text()),
		)

		val submission = Submission(
			form = form.versionsSorted.first(),
			formStep = null,
			emptyMap(),
		)

		println(submission)
		shouldBeInvalid(submission.parse(files))
	}

	@Test
	@JsName("parseInputText")
	fun `parse text`() = runTest {
		val files = FakeFiles(TestClock(testScheduler))
		val form = createTestForm(
			input("Input", Input.Text()),
		)

		val submission = Submission(
			form = form.versionsSorted.first(),
			formStep = null,
			mapOf(
				Field.Id.root to "answer"
			),
		)

		println(submission)
		submission.parse(files).shouldSucceedAnd {
			assertEquals("answer", it[Field.Id.root])
		}
	}

	@Test
	@JsName("parseInputInt")
	fun `parse int`() = runTest {
		val files = FakeFiles(TestClock(testScheduler))
		val form = createTestForm(
			input("Input", Input.Integer()),
		)

		val submission = Submission(
			form = form.versionsSorted.first(),
			formStep = null,
			mapOf(
				Field.Id.root to "6"
			),
		)

		println(submission)
		submission.parse(files).shouldSucceedAnd {
			assertEquals(6L, it[Field.Id.root])
		}
	}

	@Test
	@JsName("parseGroup")
	fun `parse group`() = runTest {
		val files = FakeFiles(TestClock(testScheduler))
		val form = createTestForm(
			group(
				"Group",
				0 to input("First name", Input.Text()),
				1 to input("Last name", Input.Text()),
			),
		)

		val submission = Submission(
			form = form.versionsSorted.first(),
			formStep = null,
			mapOf(
				Field.Id() to "", // group marker
				Field.Id(0) to "Alfred",
				Field.Id(1) to "Pennyworth",
			),
		)

		println(submission)
		submission.parse(files).shouldSucceedAnd {
			assertEquals("Alfred", it[Field.Id(0)])
			assertEquals("Pennyworth", it[Field.Id(1)])
		}
	}

	@Test
	@JsName("parseIncompleteGroup")
	fun `parse incomplete group`() = runTest {
		val files = FakeFiles(TestClock(testScheduler))
		val form = createTestForm(
			group(
				"Group",
				0 to input("First name", Input.Text()),
				1 to input("Last name", Input.Text()),
			),
		)

		val submission = Submission(
			form = form.versionsSorted.first(),
			formStep = null,
			mapOf(
				Field.Id(0) to "Alfred",
			),
		)

		println(submission)
		shouldBeInvalid(submission.parse(files))
	}

	@Test
	@JsName("parseChoice")
	fun `parse choice`() = runTest {
		val files = FakeFiles(TestClock(testScheduler))
		val form = createTestForm(
			choice(
				"Choice",
				0 to input("First option", Input.Text()),
				1 to input("Last option", Input.Text()),
			),
		)

		val submission = Submission(
			form = form.versionsSorted.first(),
			formStep = null,
			mapOf(
				Field.Id.root to "0",
				Field.Id(0) to "I prefer the first option",
			),
		)

		println(submission)
		submission.parse(files).shouldSucceedAnd {
			assertEquals(0, it[Field.Id.root])
			assertEquals("I prefer the first option", it[Field.Id(0)])
			assertEquals(null, it[Field.Id(1)])
		}
	}

	@Test
	@JsName("parseArity")
	fun `parse arity`() = runTest {
		val files = FakeFiles(TestClock(testScheduler))
		val form = createTestForm(
			arity(
				"Names",
				1u..2u,
				input("Name", Input.Text())
			),
		)

		val submission = Submission(
			form = form.versionsSorted.first(),
			formStep = null,
			mapOf(
				Field.Id(0) to "First name",
				Field.Id(1) to "Second name",
			),
		)

		println(submission)
		submission.parse(files).shouldSucceedAnd {
			assertEquals("First name", it[Field.Id(0)])
			assertEquals("Second name", it[Field.Id(1)])
		}
	}

	//endregion
	//region Complex cases

	private val complexField = group(
		"Bicycle request",
		0 to choice(
			"Type of request",
			0 to label("Regular bicycle given by the town hall"),
			1 to label("Financial aid to buy yourself an electrical bicycle"),
		),
		1 to choice(
			"Your situation",
			0 to label("Inhabitant of the city"),
			1 to group(
				"Employee working in the city",
				0 to input("Proof", Input.Text(maxLength = 10u)),
			),
		),
		2 to group(
			"Your identity",
			0 to input("Last name", Input.Text(maxLength = 50u)),
			1 to arity("First name(s)", 1u..10u, input("First name", Input.Text(maxLength = 50u))),
			2 to group(
				"Address",
				0 to arity("Address lines", 1u..3u, input("Address line", Input.Text(maxLength = 50u))),
			),
			3 to input("Contact", Input.Email),
			4 to arity("Phone", 0u..1u, input("Phone", Input.Phone))
		),
		3 to group(
			"Documents",
			0 to input("Tax document", Input.Text()),
		)
	)

	@Test
	@JsName("complexValid")
	fun `complex correct submission`() = runTest {
		val files = FakeFiles(TestClock(testScheduler))
		val form = createTestForm(complexField)
		println(form.versionsSorted.first().now().orThrow().field)

		val submission = Submission(
			form = form.versionsSorted.first(),
			formStep = null,
			mapOf(
				Field.Id() to "", // group marker
				Field.Id(0) to "1", // I want money for an e-bike
				Field.Id(1) to "0", // I'm a inhabitant
				Field.Id(2) to "", // group marker
				Field.Id(2, 0) to "Pennyworth",
				Field.Id(2, 1, 0) to "Alfred",
				Field.Id(2, 2) to "", // group marker
				Field.Id(2, 2, 0, 0) to "1007 Mountain Drive",
				Field.Id(2, 3) to "alfred@wayne.com",
				Field.Id(3) to "", // group marker
				Field.Id(3, 0) to "Document",
			)
		)

		println(submission)
		submission.parse(files).shouldSucceedAnd {
			assertEquals(1, it[Field.Id(0)])
		}
	}

	//endregion

}
