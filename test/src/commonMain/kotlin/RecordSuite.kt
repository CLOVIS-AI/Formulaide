package opensavvy.formulaide.test

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.withContext
import opensavvy.backbone.Ref.Companion.now
import opensavvy.formulaide.core.*
import opensavvy.formulaide.test.assertions.shouldBeInvalid
import opensavvy.formulaide.test.assertions.shouldNotBeFound
import opensavvy.formulaide.test.assertions.shouldSucceed
import opensavvy.formulaide.test.assertions.shouldSucceedAnd
import opensavvy.formulaide.test.execution.Factory
import opensavvy.formulaide.test.execution.Suite
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
					step shouldBe null
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
