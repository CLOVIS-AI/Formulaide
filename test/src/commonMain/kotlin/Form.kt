package opensavvy.formulaide.test

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import opensavvy.backbone.Ref.Companion.now
import opensavvy.formulaide.core.*
import opensavvy.formulaide.core.Field.Companion.group
import opensavvy.formulaide.core.Field.Companion.input
import opensavvy.formulaide.core.Field.Companion.label
import opensavvy.formulaide.core.Field.Companion.labelFrom
import opensavvy.formulaide.fake.FakeDepartments
import opensavvy.formulaide.fake.FakeTemplates
import opensavvy.formulaide.fake.spies.SpyDepartments.Companion.spied
import opensavvy.formulaide.test.assertions.*
import opensavvy.formulaide.test.cases.TestCase
import opensavvy.formulaide.test.cases.TestUsers.administratorAuth
import opensavvy.formulaide.test.cases.TestUsers.employeeAuth
import opensavvy.formulaide.test.utils.TestClock.Companion.currentInstant
import opensavvy.formulaide.test.utils.TestClock.Companion.testClock
import opensavvy.state.outcome.orThrow
import kotlin.js.JsName
import kotlin.test.*

//region Test data

internal suspend fun testSimpleForm(
	forms: Form.Service,
	reviewer: Department.Ref,
) = withContext(administratorAuth) {
	forms.create(
		"Simple",
		"First version",
		input("The illusion of choice", Input.Toggle),
		Form.Step(
			0,
			"Received",
			reviewer,
			null,
		)
	).orThrow()
}

internal suspend fun testIdeaForm(
	forms: Form.Service,
	eligibilityReviewer: Department.Ref,
	finalDecisionReviewer: Department.Ref,
) = withContext(administratorAuth) {
	forms.create(
		"Ideas",
		"First version",
		group(
			"Root",
			0 to group(
				"Identity",
				0 to input("First name", Input.Text(maxLength = 30u)),
				1 to input("Last name", Input.Text(maxLength = 30u)),
			),
			1 to input("Idea", Input.Text(maxLength = 30u)),
		),
		Form.Step(
			0,
			"Verification of eligibility",
			eligibilityReviewer,
			null,
		),
		Form.Step(
			1,
			"Waiting list",
			finalDecisionReviewer,
			null,
		),
		Form.Step(
			2,
			"Archive",
			finalDecisionReviewer,
			null,
		)
	).orThrow()
		.also { it.publicize() }
}

//endregion

@Suppress("FunctionName")
@OptIn(ExperimentalCoroutinesApi::class)
abstract class FormTestCases : TestCase<Form.Service> {

	//region Employee & guest access

	@Test
	@JsName("guestsList")
	fun `guests can list public forms`() = runTest {
		val forms = new()

		val dept = createDepartment(FakeDepartments().spied())
		val private = testSimpleForm(forms, dept)
		val public = testIdeaForm(forms, dept, dept)

		forms.list().shouldSucceedAnd {
			it shouldContain public
			it shouldNotContain private
		}
	}

	@Test
	@JsName("guestsListClosed")
	fun `guests cannot list closed forms`() = runTest {
		val forms = new()

		shouldNotBeAuthenticated(forms.list(includeClosed = true))
	}

	@Test
	@JsName("employeeList")
	fun `employees can list open forms`() = runTest(employeeAuth) {
		val forms = new()

		val dept = createDepartment(FakeDepartments().spied())
		val private = testSimpleForm(forms, dept)
		val public = testIdeaForm(forms, dept, dept)

		forms.list().shouldSucceedAnd {
			assertContains(it, public)
			assertContains(it, private)
		}
	}

	@Test
	@JsName("employeesListClosed")
	fun `employees can list closed forms`() = runTest(employeeAuth) {
		val forms = new()

		val dept = createDepartment(FakeDepartments().spied())
		val private = testSimpleForm(forms, dept)
			.also { withContext(administratorAuth) { it.close().orThrow() } }
		val public = testIdeaForm(forms, dept, dept)
			.also { withContext(administratorAuth) { it.close().orThrow() } }

		forms.list(includeClosed = true).shouldSucceedAnd {
			assertContains(it, private)
			assertContains(it, public)
		}
	}

	@Test
	@JsName("guestCreate")
	fun `guests cannot create forms`() = runTest {
		val forms = new()

		val dept = createDepartment(FakeDepartments().spied())

		shouldNotBeAuthenticated(
			forms.create(
				"A new form",
				"Initial version",
				label("Field"),
				Form.Step(0, "Validation", dept, null)
			)
		)
	}

	@Test
	@JsName("employeesCreate")
	fun `employees cannot create forms`() = runTest(employeeAuth) {
		val forms = new()

		val dept = createDepartment(FakeDepartments().spied())

		shouldNotBeAuthorized(
			forms.create(
				"A new form",
				"Initial version",
				label("Field"),
				Form.Step(0, "Validation", dept, null)
			)
		)
	}

	@Test
	@JsName("guestCreateVersion")
	fun `guests cannot create new versions`() = runTest {
		val forms = new()

		val dept = createDepartment(FakeDepartments().spied())
		val form = testSimpleForm(forms, dept)

		shouldNotBeAuthenticated(
			form.createVersion(
				"Title",
				label("Field"),
				Form.Step(
					0,
					"Validation",
					dept,
					null,
				)
			)
		)
	}

	@Test
	@JsName("employeeCreateVersion")
	fun `employees cannot create new versions`() = runTest(employeeAuth) {
		val forms = new()

		val dept = createDepartment(FakeDepartments().spied())
		val form = testSimpleForm(forms, dept)

		shouldNotBeAuthorized(
			form.createVersion(
				"Title",
				label("Field"),
				Form.Step(
					0,
					"Validation",
					dept,
					null,
				)
			)
		)
	}

	@Test
	@JsName("guestEdit")
	fun `guests cannot edit forms`() = runTest {
		val forms = new()

		val dept = createDepartment(FakeDepartments().spied())
		val form = testSimpleForm(forms, dept)

		shouldNotBeAuthenticated(form.rename("New name"))
		shouldNotBeAuthenticated(form.open())
		shouldNotBeAuthenticated(form.close())
		shouldNotBeAuthenticated(form.publicize())
		shouldNotBeAuthenticated(form.privatize())
	}

	@Test
	@JsName("employeeEdit")
	fun `employees cannot edit forms`() = runTest(employeeAuth) {
		val forms = new()

		val dept = createDepartment(FakeDepartments().spied())
		val form = testSimpleForm(forms, dept)

		shouldNotBeAuthorized(form.rename("New name"))
		shouldNotBeAuthorized(form.open())
		shouldNotBeAuthorized(form.close())
		shouldNotBeAuthorized(form.publicize())
		shouldNotBeAuthorized(form.privatize())
	}

	//endregion
	//region Administrator access

	@Test
	@JsName("createForm")
	fun `create form`() = runTest(administratorAuth) {
		val forms = new()
		val dept = createDepartment(FakeDepartments().spied())
		val testStart = currentInstant()
		advanceTimeBy(10)

		forms.create(
			"An example form",
			"First version",
			label("The field"),
			Form.Step(
				0,
				"Validation",
				dept,
				null,
			)
		).shouldSucceedAnd { ref ->
			ref.now().shouldSucceedAnd {
				assertEquals("An example form", it.name)
				assertTrue(it.open)
				assertFalse(it.public)

				it.versions.first().now().shouldSucceedAnd { version ->
					assertEquals("First version", version.title)
					assertTrue(
						testStart < version.creationDate,
						"The date ${version.creationDate} should be in the past compared to the start of the test at $testStart"
					)
					assertTrue(
						version.creationDate > Instant.DISTANT_PAST,
						"The form creation shouldn't keep the date provided by the client"
					)

					assertEquals(label("The field"), version.field)
				}
			}
		}
	}

	@Test
	@JsName("createFormVersion")
	fun `create form version`() = runTest(administratorAuth) {
		val forms = new()
		val dept = createDepartment(FakeDepartments().spied())
		val testStart = currentInstant()
		advanceTimeBy(10)

		val idea = testIdeaForm(forms, dept, dept)
		advanceTimeBy(10)

		idea.createVersion(
			"Second version",
			label("Other field"),
			Form.Step(0, "Validation", dept, null)
		).shouldSucceedAnd { versionRef ->
			versionRef.now().shouldSucceedAnd { version ->
				assertEquals("Second version", version.title)
				assertTrue(
					testStart < version.creationDate,
					"The date ${version.creationDate} should be in the past compared to the start of the test at $testStart"
				)
				assertTrue(
					version.creationDate > Instant.DISTANT_PAST,
					"The form creation shouldn't keep the date provided by the client"
				)

				assertEquals(label("Other field"), version.field)
			}

			idea.now().shouldSucceedAnd {
				assertEquals(2, it.versions.size, "Expected two versions, found ${it.versions}")
				assertContains(it.versions, versionRef, "The version we just created should be one of the two versions")
			}
		}
	}

	@Test
	@JsName("createFormInvalidTemplate")
	fun `cannot create a form with an invalid field import`() = runTest(administratorAuth) {
		val forms = new()
		val templates = FakeTemplates(testClock())
		val dept = createDepartment(FakeDepartments())

		val cities = testCityTemplate(templates).now()
			.map { it.versions.first() }
			.orThrow()

		forms.create(
			"Test",
			"Initial version",
			labelFrom(cities, "This field does not match the imported template at all"),
			Form.Step(0, "Validation", dept, null),
		).shouldFail()
	}

	@Test
	@JsName("createFormVersionInvalidTemplate")
	fun `cannot create a form version with an invalid field import`() = runTest(administratorAuth) {
		val forms = new()
		val templates = FakeTemplates(testClock())
		val dept = createDepartment(FakeDepartments())

		val cities = testCityTemplate(templates).now()
			.map { it.versions.first() }
			.orThrow()

		val idea = testIdeaForm(forms, dept, dept)

		idea.createVersion(
			"New version",
			labelFrom(cities, "This field does not match the imported template at all"),
			Form.Step(0, "Validation", dept, null)
		).shouldFail()
	}

	@Test
	@JsName("closeForm")
	fun `close form`() = runTest(administratorAuth) {
		val forms = new()
		val dept = createDepartment(FakeDepartments().spied())

		val idea = testIdeaForm(forms, dept, dept)

		shouldSucceed(idea.close())
		idea.now().shouldSucceedAnd { assertFalse(it.open) }
		forms.list(includeClosed = false).shouldSucceedAnd { it shouldNotContain idea }
		forms.list(includeClosed = true).shouldSucceedAnd { it shouldContain idea }

		shouldSucceed(idea.open())
		idea.now().shouldSucceedAnd { assertTrue(it.open) }
		forms.list(includeClosed = false).shouldSucceedAnd { it shouldContain idea }
		forms.list(includeClosed = true).shouldSucceedAnd { it shouldContain idea }
	}

	@Test
	@JsName("publishForm")
	fun `publicize form`() = runTest(administratorAuth) {
		val forms = new()
		val dept = createDepartment(FakeDepartments().spied())

		val idea = testIdeaForm(forms, dept, dept)

		shouldSucceed(idea.privatize())
		idea.now().shouldSucceedAnd { assertFalse(it.public) }
		forms.list().shouldSucceedAnd { assertContains(it, idea) }

		shouldSucceed(idea.publicize())
		idea.now().shouldSucceedAnd { assertTrue(it.public) }
		forms.list().shouldSucceedAnd { assertContains(it, idea) }
	}

	@Test
	@JsName("renameForm")
	fun `rename form`() = runTest(administratorAuth) {
		val forms = new()
		val dept = createDepartment(FakeDepartments().spied())

		val idea = testIdeaForm(forms, dept, dept)

		shouldSucceed(idea.rename("Alternative ideas"))
		idea.now().shouldSucceedAnd {
			assertEquals("Alternative ideas", it.name)
		}
	}

	//endregion

}
