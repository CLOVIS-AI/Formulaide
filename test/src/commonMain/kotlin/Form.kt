package opensavvy.formulaide.test

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

		val dept = testDepartment(FakeDepartments().spied())
		val private = testSimpleForm(forms, dept)
		val public = testIdeaForm(forms, dept, dept)

		assertSuccess(forms.list()) {
			assertContains(it, public)
			assertNotContains(it, private)
		}
	}

	@Test
	@JsName("guestsListClosed")
	fun `guests cannot list closed forms`() = runTest {
		val forms = new()

		assertUnauthenticated(forms.list(includeClosed = true))
	}

	@Test
	@JsName("employeeList")
	fun `employees can list open forms`() = runTest(employeeAuth) {
		val forms = new()

		val dept = testDepartment(FakeDepartments().spied())
		val private = testSimpleForm(forms, dept)
		val public = testIdeaForm(forms, dept, dept)

		assertSuccess(forms.list()) {
			assertContains(it, public)
			assertContains(it, private)
		}
	}

	@Test
	@JsName("employeesListClosed")
	fun `employees can list closed forms`() = runTest(employeeAuth) {
		val forms = new()

		val dept = testDepartment(FakeDepartments().spied())
		val private = testSimpleForm(forms, dept)
			.also { withContext(administratorAuth) { it.close().orThrow() } }
		val public = testIdeaForm(forms, dept, dept)
			.also { withContext(administratorAuth) { it.close().orThrow() } }

		assertSuccess(forms.list(includeClosed = true)) {
			assertContains(it, private)
			assertContains(it, public)
		}
	}

	@Test
	@JsName("guestCreate")
	fun `guests cannot create forms`() = runTest {
		val forms = new()

		val dept = testDepartment(FakeDepartments().spied())

		assertUnauthenticated(
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

		val dept = testDepartment(FakeDepartments().spied())

		assertUnauthorized(
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

		val dept = testDepartment(FakeDepartments().spied())
		val form = testSimpleForm(forms, dept)

		assertUnauthenticated(
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

		val dept = testDepartment(FakeDepartments().spied())
		val form = testSimpleForm(forms, dept)

		assertUnauthorized(
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

		val dept = testDepartment(FakeDepartments().spied())
		val form = testSimpleForm(forms, dept)

		assertUnauthenticated(form.rename("New name"))
		assertUnauthenticated(form.open())
		assertUnauthenticated(form.close())
		assertUnauthenticated(form.publicize())
		assertUnauthenticated(form.privatize())
	}

	@Test
	@JsName("employeeEdit")
	fun `employees cannot edit forms`() = runTest(employeeAuth) {
		val forms = new()

		val dept = testDepartment(FakeDepartments().spied())
		val form = testSimpleForm(forms, dept)

		assertUnauthorized(form.rename("New name"))
		assertUnauthorized(form.open())
		assertUnauthorized(form.close())
		assertUnauthorized(form.publicize())
		assertUnauthorized(form.privatize())
	}

	//endregion
	//region Administrator access

	@Test
	@JsName("createForm")
	fun `create form`() = runTest(administratorAuth) {
		val forms = new()
		val dept = testDepartment(FakeDepartments().spied())
		val testStart = currentInstant()
		advanceTimeBy(10)

		assertSuccess(
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
			)
		) { ref ->
			assertSuccess(ref.now()) {
				assertEquals("An example form", it.name)
				assertTrue(it.open)
				assertFalse(it.public)

				assertSuccess(it.versions.first().now()) { version ->
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
		val dept = testDepartment(FakeDepartments().spied())
		val testStart = currentInstant()
		advanceTimeBy(10)

		val idea = testIdeaForm(forms, dept, dept)
		advanceTimeBy(10)

		assertSuccess(
			idea.createVersion(
				"Second version",
				label("Other field"),
				Form.Step(0, "Validation", dept, null)
			)
		) { versionRef ->
			assertSuccess(versionRef.now()) { version ->
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

			assertSuccess(idea.now()) {
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
		val dept = testDepartment(FakeDepartments())

		val cities = testCityTemplate(templates).now()
			.map { it.versions.first() }
			.orThrow()

		assertFails(
			forms.create(
				"Test",
				"Initial version",
				labelFrom(cities, "This field does not match the imported template at all"),
				Form.Step(0, "Validation", dept, null),
			)
		)
	}

	@Test
	@JsName("createFormVersionInvalidTemplate")
	fun `cannot create a form version with an invalid field import`() = runTest(administratorAuth) {
		val forms = new()
		val templates = FakeTemplates(testClock())
		val dept = testDepartment(FakeDepartments())

		val cities = testCityTemplate(templates).now()
			.map { it.versions.first() }
			.orThrow()

		val idea = testIdeaForm(forms, dept, dept)

		assertFails(
			idea.createVersion(
				"New version",
				labelFrom(cities, "This field does not match the imported template at all"),
				Form.Step(0, "Validation", dept, null)
			)
		)
	}

	@Test
	@JsName("closeForm")
	fun `close form`() = runTest(administratorAuth) {
		val forms = new()
		val dept = testDepartment(FakeDepartments().spied())

		val idea = testIdeaForm(forms, dept, dept)

		assertSuccess(idea.close())
		assertSuccess(idea.now()) { assertFalse(it.open) }
		assertSuccess(forms.list(includeClosed = false)) { assertNotContains(it, idea) }
		assertSuccess(forms.list(includeClosed = true)) { assertContains(it, idea) }

		assertSuccess(idea.open())
		assertSuccess(idea.now()) { assertTrue(it.open) }
		assertSuccess(forms.list(includeClosed = false)) { assertContains(it, idea) }
		assertSuccess(forms.list(includeClosed = true)) { assertContains(it, idea) }
	}

	@Test
	@JsName("publishForm")
	fun `publicize form`() = runTest(administratorAuth) {
		val forms = new()
		val dept = testDepartment(FakeDepartments().spied())

		val idea = testIdeaForm(forms, dept, dept)

		assertSuccess(idea.privatize())
		assertSuccess(idea.now()) { assertFalse(it.public) }
		assertSuccess(forms.list()) { assertContains(it, idea) }

		assertSuccess(idea.publicize())
		assertSuccess(idea.now()) { assertTrue(it.public) }
		assertSuccess(forms.list()) { assertContains(it, idea) }
	}

	@Test
	@JsName("renameForm")
	fun `rename form`() = runTest(administratorAuth) {
		val forms = new()
		val dept = testDepartment(FakeDepartments().spied())

		val idea = testIdeaForm(forms, dept, dept)

		assertSuccess(idea.rename("Alternative ideas"))
		assertSuccess(idea.now()) {
			assertEquals("Alternative ideas", it.name)
		}
	}

	//endregion

}
