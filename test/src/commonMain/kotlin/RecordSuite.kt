package opensavvy.formulaide.test

import arrow.core.flatMap
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.withContext
import opensavvy.backbone.Ref.Companion.now
import opensavvy.formulaide.core.*
import opensavvy.formulaide.core.Field.Companion.choice
import opensavvy.formulaide.core.Field.Companion.group
import opensavvy.formulaide.core.Field.Companion.input
import opensavvy.formulaide.core.Field.Companion.label
import opensavvy.formulaide.test.assertions.*
import opensavvy.formulaide.test.execution.Factory
import opensavvy.formulaide.test.execution.Suite
import opensavvy.formulaide.test.utils.TestClock.Companion.currentInstant
import opensavvy.formulaide.test.utils.TestUsers.administratorAuth
import opensavvy.formulaide.test.utils.TestUsers.employeeAuth
import opensavvy.state.outcome.orThrow

data class RecordTestData(
	val departments: Department.Service,
	val templates: Template.Service,
	val forms: Form.Service,
	val records: Record.Service,
)

fun Suite.recordsTestSuite(
	create: Factory<RecordTestData>,
) {
	create(create)
	search(create)
}

private fun Suite.create(
	create: Factory<RecordTestData>,
) = suite("Create a record") {
	test("guests cannot create records to a private form") {
		val data = create()

		val records = data.records
		val forms = data.forms
		val departments = data.departments

		val form = withContext(administratorAuth) {
			createSimpleForm(forms, createDepartment(departments))
				.also { it.privatize().orThrow() }
				.now().orThrow()
				.versionsSorted.first()
		}

		shouldNotBeFound(
			records.create(
				form,
				"" to "true",
			)
		)
	}

	test("employees can create records to a private form", employeeAuth) {
		val data = create()

		val records = data.records
		val forms = data.forms
		val departments = data.departments

		val form = withContext(administratorAuth) {
			createSimpleForm(forms, createDepartment(departments))
				.also { it.privatize().orThrow() }
				.now().orThrow()
				.versionsSorted.first()
		}

		shouldSucceed(
			records.create(
				form,
				"" to "true",
			)
		)
	}

	test("create a record") {
		val data = create()

		val records = data.records
		val forms = data.forms
		val departments = data.departments

		val form = withContext(employeeAuth) {
			createSimpleForm(forms, createDepartment(departments))
				.also { withContext(administratorAuth) { it.publicize().orThrow() } }
				.now().orThrow()
				.versionsSorted.first()
		}

		val record = records.create(
			form,
			"" to "true",
		).shouldSucceed()

		withContext(employeeAuth) {
			record.now() shouldSucceedAnd {
				assertSoftly(it.historySorted.first()) {
					author shouldBe null
					source shouldBe Record.Status.Initial
					target shouldBe Record.Status.Step(0)
					reason shouldBe null

					submission shouldNotBe null
					submission!!.now() shouldSucceedAnd { submission ->
						val parsed = submission.parse().shouldSucceed()

						parsed[Field.Id.root] shouldBe true
					}
				}
			}

			records.search() shouldSucceedAnd {
				it shouldContain record
			}
		}
	}

	test("cannot create a record for another step than the initial one") {
		val data = create()

		val records = data.records
		val forms = data.forms
		val departments = data.departments

		val form = withContext(employeeAuth) {
			createSimpleForm(forms, createDepartment(departments))
				.also { withContext(administratorAuth) { it.publicize().orThrow() } }
				.now().orThrow()
				.versionsSorted.first()
		}

		shouldBeInvalid(
			records.create(
				Submission(
					form,
					1,
					mapOf(Field.Id.fromString("") to "true"),
				)
			)
		)
	}

	test("cannot create a record with an invalid submission") {
		val data = create()

		val records = data.records
		val forms = data.forms
		val departments = data.departments

		val form = withContext(employeeAuth) {
			createSimpleForm(forms, createDepartment(departments))
				.also { withContext(administratorAuth) { it.publicize().orThrow() } }
				.now().orThrow()
				.versionsSorted.first()
		}

		shouldBeInvalid(
			records.create(
				form,
				"" to "this is not a boolean",
			)
		)
	}
}

private suspend fun RecordTestData.createWorkflowForm(): Form.Version.Ref = withContext(administratorAuth) {
	val forms = forms
	val departments = departments

	val first = departments.create("First department").orThrow()
	val second = departments.create("Second department").orThrow()
	val third = departments.create("Third department").orThrow()

	forms.create(
		"The workflow tester",
		"First version",
		group(
			"Initial field",
			0 to group(
				"Identity",
				0 to input("First name", Input.Text()),
				1 to input("Last name", Input.Text()),
			),
			1 to input("Idea", Input.Text()),
		),
		Form.Step(
			id = 0,
			name = "Pre-filtering",
			reviewer = first,
			field = choice(
				"Kind of request",
				0 to label("Ecological"),
				1 to label("Industrial"),
				2 to label("Administrative"),
			)
		),
		Form.Step(
			id = 1,
			name = "Decide whether the request is worth it",
			reviewer = second,
			field = input("Request ID", Input.Text()),
		),
		Form.Step(
			id = 2,
			name = "Completed requests",
			reviewer = third,
			field = null,
		)
	).flatMap { it.now() }
		.orThrow()
		.versionsSorted.first()
}

private suspend fun RecordTestData.createTestRecord(
	form: Form.Version.Ref,
): Record.Ref {
	return records.create(
		form,
		"" to "",
		"0" to "",
		"0:0" to "My first name",
		"0:1" to "My last name",
		"1" to "Plant more trees!",
	).orThrow()
}

private fun Suite.search(
	create: Factory<RecordTestData>,
) = suite("Workflow") {
	test("Guests cannot access records") {
		val services = create()
		val form = services.createWorkflowForm()
		withContext(employeeAuth) { services.createTestRecord(form) }

		shouldNotBeAuthenticated(services.records.search())
	}

	test("Accept a record", employeeAuth) {
		val services = create()
		val form = services.createWorkflowForm()
		val record = services.createTestRecord(form)

		val before = currentInstant()
		advanceTimeBy(10)

		services.records.accept(
			record,
			null,
			"" to "1",
		).shouldSucceed()

		advanceTimeBy(10)
		val after = currentInstant()

		record.now() shouldSucceedAnd {
			it.historySorted shouldHaveSize 2
			it.status shouldBe Record.Status.Step(1)

			assertSoftly(it.historySorted[1]) {
				this::class shouldBe Record.Diff.Accept::class

				author!!.id shouldBe employeeAuth.user!!.id
				at shouldBeGreaterThan before
				at shouldBeLessThan after
				reason shouldBe null
				submission shouldNotBe null
				source shouldBe Record.Status.Step(0)
				target shouldBe Record.Status.Step(1)
			}
		}
	}

	test("Refuse a record", employeeAuth) {
		val services = create()
		val form = services.createWorkflowForm()
		val record = services.createTestRecord(form)

		val before = currentInstant()
		advanceTimeBy(10)

		services.records.refuse(
			record,
			"I don't like it",
		).shouldSucceed()

		advanceTimeBy(10)
		val after = currentInstant()

		record.now() shouldSucceedAnd {
			it.historySorted shouldHaveSize 2
			it.status shouldBe Record.Status.Refused

			assertSoftly(it.historySorted[1]) {
				this::class shouldBe Record.Diff.Refuse::class

				author!!.id shouldBe employeeAuth.user!!.id
				at shouldBeGreaterThan before
				at shouldBeLessThan after
				reason shouldBe "I don't like it"
				submission shouldBe null
				source shouldBe Record.Status.Step(0)
				target shouldBe Record.Status.Refused
			}
		}
	}

	test("Save additional data", employeeAuth) {
		val services = create()
		val form = services.createWorkflowForm()
		val record = services.createTestRecord(form)

		val before = currentInstant()
		advanceTimeBy(10)

		services.records.editCurrent(
			record,
			null,
			"" to "0",
		).shouldSucceed()

		advanceTimeBy(10)
		val after = currentInstant()

		record.now() shouldSucceedAnd {
			it.historySorted shouldHaveSize 2
			it.status shouldBe Record.Status.Step(0)

			assertSoftly(it.historySorted[1]) {
				this::class shouldBe Record.Diff.EditCurrent::class

				author!!.id shouldBe employeeAuth.user!!.id
				at shouldBeGreaterThan before
				at shouldBeLessThan after
				reason shouldBe null
				submission shouldNotBe null
				source shouldBe Record.Status.Step(0)
				target shouldBe Record.Status.Step(0)
			}
		}
	}

	test("Edit the initial submission", employeeAuth) {
		val services = create()
		val form = services.createWorkflowForm()
		val record = services.createTestRecord(form)

		val before = currentInstant()
		advanceTimeBy(10)

		services.records.editInitial(
			record,
			"I didn't like it",
			"" to "0",
		).shouldSucceed()

		advanceTimeBy(10)
		val after = currentInstant()

		record.now() shouldSucceedAnd {
			it.historySorted shouldHaveSize 2
			it.status shouldBe Record.Status.Step(0)

			assertSoftly(it.historySorted[1]) {
				this::class shouldBe Record.Diff.EditInitial::class

				author!!.id shouldBe employeeAuth.user!!.id
				at shouldBeGreaterThan before
				at shouldBeLessThan after
				reason shouldBe "I didn't like it"
				submission shouldNotBe null
				source shouldBe Record.Status.Step(0)
				target shouldBe Record.Status.Step(0)
			}
		}
	}

	test("Move back a record", employeeAuth) {
		val services = create()
		val form = services.createWorkflowForm()
		val record = services.createTestRecord(form)

		val before = currentInstant()
		advanceTimeBy(10)

		services.records.accept(
			record,
			null,
			"" to "0",
		).shouldSucceed()

		services.records.moveBack(
			record,
			0,
			"I didn't like it",
		).shouldSucceed()

		advanceTimeBy(10)
		val after = currentInstant()

		record.now() shouldSucceedAnd {
			it.historySorted shouldHaveSize 3
			it.status shouldBe Record.Status.Step(0)

			assertSoftly(it.historySorted[2]) {
				this::class shouldBe Record.Diff.MoveBack::class

				author!!.id shouldBe employeeAuth.user!!.id
				at shouldBeGreaterThan before
				at shouldBeLessThan after
				reason shouldBe "I didn't like it"
				submission shouldBe null
				source shouldBe Record.Status.Step(1)
				target shouldBe Record.Status.Step(0)
			}
		}
	}
}
