package opensavvy.formulaide.test

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import opensavvy.backbone.Ref.Companion.now
import opensavvy.formulaide.core.Field
import opensavvy.formulaide.core.Record
import opensavvy.formulaide.core.Submission
import opensavvy.formulaide.fake.FakeDepartments
import opensavvy.formulaide.fake.FakeForms
import opensavvy.formulaide.test.assertions.shouldSucceedAnd
import opensavvy.formulaide.test.cases.TestCase
import opensavvy.formulaide.test.cases.TestUsers.employeeAuth
import opensavvy.formulaide.test.utils.TestClock.Companion.testClock
import opensavvy.state.outcome.orThrow
import kotlin.js.JsName
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

@Suppress("FunctionName")
@OptIn(ExperimentalCoroutinesApi::class)
abstract class RecordTestCases : TestCase<Record.Service> {

	@Test
	@JsName("create")
	fun `create record`() = runTest {
		val records = new()
		val clock = testClock()
		val forms = FakeForms(clock)
		val departments = FakeDepartments()

		val form = withContext(employeeAuth) {
			testSimpleForm(forms, createDepartment(departments))
				.now()
				.orThrow()
				.versionsSorted
				.first()
		}

		records.create(
			Submission(
				form = form,
				formStep = null,
				data = mapOf(
					Field.Id.root to "true",
				)
			)
		).shouldSucceedAnd { ref ->
			withContext(employeeAuth) {
				ref.now().shouldSucceedAnd {
					val initial = it.historySorted.first()

					assertEquals(null, initial.author)
					assertEquals(null, initial.step)
					assertEquals(null, initial.reason)
					initial.submission!!.now().shouldSucceedAnd { submission ->
						val parsed = submission.copy(form = form.copy(backbone = forms.versions)).parse().orThrow()

						assertEquals(true, parsed[Field.Id.root])
					}
				}

				records.search(emptyList()).shouldSucceedAnd {
					assertContains(it, ref)
				}
			}
		}
	}

}
