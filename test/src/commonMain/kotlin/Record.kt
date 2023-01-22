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
import opensavvy.formulaide.test.assertions.assertSuccess
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
			testSimpleForm(forms, testDepartment(departments))
				.now()
				.orThrow()
				.versionsSorted
				.first()
		}

		assertSuccess(
			records.create(
				Submission(
					form = form,
					formStep = null,
					data = mapOf(
						Field.Id.root to "true",
					)
				)
			)
		) { ref ->
			withContext(employeeAuth) {
				assertSuccess(ref.now()) {
					val initial = it.historySorted.first()

					assertEquals(null, initial.author)
					assertEquals(null, initial.step)
					assertEquals(null, initial.reason)
					assertSuccess(initial.submission!!.now()) { submission ->
						val parsed = submission.parse().orThrow()

						assertEquals(true, parsed[Field.Id.root])
					}
				}

				assertSuccess(records.search(emptyList())) {
					assertContains(it, ref)
				}
			}
		}
	}

}
