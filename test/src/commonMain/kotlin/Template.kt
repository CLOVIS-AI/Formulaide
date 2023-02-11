package opensavvy.formulaide.test

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import opensavvy.backbone.Ref.Companion.now
import opensavvy.formulaide.core.Field.Companion.arity
import opensavvy.formulaide.core.Field.Companion.group
import opensavvy.formulaide.core.Field.Companion.input
import opensavvy.formulaide.core.Field.Companion.label
import opensavvy.formulaide.core.Field.Companion.labelFrom
import opensavvy.formulaide.core.Input
import opensavvy.formulaide.core.Template
import opensavvy.formulaide.test.assertions.*
import opensavvy.formulaide.test.cases.TestCase
import opensavvy.formulaide.test.cases.TestUsers.administratorAuth
import opensavvy.formulaide.test.cases.TestUsers.employeeAuth
import opensavvy.formulaide.test.utils.TestClock.Companion.currentInstant
import opensavvy.state.outcome.orThrow
import kotlin.js.JsName
import kotlin.test.*

//region Test data

internal suspend fun testCityTemplate(templates: Template.Service) = withContext(administratorAuth) {
	templates.create(
		"Cities",
		"First version",
		group(
			"City",
			0 to input("Name", Input.Text(maxLength = 50u)),
			1 to input("Postal code", Input.Text(maxLength = 5u)),
		)
	).orThrow()
}

internal suspend fun testIdentityTemplate(templates: Template.Service) = withContext(administratorAuth) {
	templates.create(
		"Identities",
		"First version",
		group(
			"Identity",
			0 to arity("First name(s)", 1u..10u, input("First name", Input.Text(maxLength = 30u))),
			1 to input("Last name", Input.Text(maxLength = 30u)),
			// In the future, add the city here
		)
	).orThrow()
}

//endregion

@Suppress("FunctionName")
@OptIn(ExperimentalCoroutinesApi::class)
abstract class TemplateTestCases : TestCase<Template.Service> {

	//region Guest access

	@Test
	@JsName("guestList")
	fun `guests cannot list templates`() = runTest {
		val templates = new()

		shouldNotBeAuthenticated(templates.list())
		shouldNotBeAuthenticated(templates.list(includeClosed = true))
	}

	@Test
	@JsName("guestCreate")
	fun `guests cannot create templates`() = runTest {
		val templates = new()

		shouldNotBeAuthenticated(
			templates.create(
				"A new template",
				Template.Version(Clock.System.now(), "First version", label("field"))
			)
		)
	}

	@Test
	@JsName("guestCreateVersion")
	fun `guests cannot create new versions of a template`() = runTest {
		val templates = new()

		val template = testCityTemplate(templates)

		shouldNotBeAuthenticated(
			template.createVersion(
				Template.Version(
					Clock.System.now(),
					"Second version",
					label("field")
				)
			)
		)
	}

	@Test
	@JsName("guestEdit")
	fun `guests cannot edit templates`() = runTest {
		val templates = new()

		val template = testCityTemplate(templates)

		shouldNotBeAuthenticated(template.rename("New name"))
		shouldNotBeAuthenticated(template.open())
		shouldNotBeAuthenticated(template.close())
	}

	@Test
	@JsName("guestGet")
	fun `guests cannot access templates`() = runTest {
		val templates = new()

		val template = testCityTemplate(templates)

		shouldNotBeAuthenticated(template.now())
	}

	//endregion
	//region Employee access

	@Test
	@JsName("employeeList")
	fun `employees can list templates`() = runTest(employeeAuth) {
		val templates = new()

		val first = testCityTemplate(templates)
		val second = testIdentityTemplate(templates)
			.also { withContext(administratorAuth) { it.close().orThrow() } }

		templates.list(includeClosed = false) shouldSucceedAnd {
			it shouldContain first
			it shouldNotContain second
		}
		templates.list(includeClosed = true) shouldSucceedAnd {
			it shouldContain first
			it shouldContain second
		}
	}

	@Test
	@JsName("employeeCreate")
	fun `employees cannot create templates`() = runTest(employeeAuth) {
		val templates = new()

		shouldNotBeAuthorized(
			templates.create(
				"A new template",
				Template.Version(Clock.System.now(), "First version", label("field"))
			)
		)
	}

	@Test
	@JsName("employeeCreateVersion")
	fun `employees cannot create new versions of a template`() = runTest(employeeAuth) {
		val templates = new()

		val template = testCityTemplate(templates)

		shouldNotBeAuthorized(
			template.createVersion(
				Template.Version(
					Clock.System.now(),
					"Second version",
					label("field")
				)
			)
		)
	}

	@Test
	@JsName("employeeEdit")
	fun `employees cannot edit templates`() = runTest(employeeAuth) {
		val templates = new()

		val template = testCityTemplate(templates)

		shouldNotBeAuthorized(template.rename("New name"))
		shouldNotBeAuthorized(template.open())
		shouldNotBeAuthorized(template.close())
	}

	@Test
	@JsName("employeeGet")
	fun `employees can access templates`() = runTest(employeeAuth) {
		val templates = new()

		val template = testCityTemplate(templates)

		shouldSucceed(template.now())
	}

	//endregion
	//region Administrator access

	@Test
	@JsName("createTemplate")
	fun `create template`() = runTest(administratorAuth) {
		val templates = new()
		val testStart = currentInstant()
		advanceTimeBy(10)

		templates.create(
			"An example template",
			Template.Version(
				Instant.DISTANT_PAST,
				"First version",
				label("The field"),
			)
		).shouldSucceedAnd { ref ->
			ref.now().shouldSucceedAnd {
				assertEquals("An example template", it.name)
				assertTrue(it.open)
				assertEquals(1, it.versions.size)

				it.versions.first().now().shouldSucceedAnd { version ->
					assertEquals("First version", version.title)
					assertTrue(
						testStart < version.creationDate,
						"The date ${version.creationDate} should be in the past compared to the start of the test at $testStart"
					)
					assertTrue(
						version.creationDate > Instant.DISTANT_PAST,
						"The template creation shouldn't keep the date provided by the client"
					)

					assertEquals(label("The field"), version.field)
				}
			}
		}
	}

	@Test
	@JsName("createTemplateVersion")
	fun `create template version`() = runTest(administratorAuth) {
		val templates = new()
		val testStart = currentInstant()
		advanceTimeBy(10)

		val cities = testCityTemplate(templates)

		delay(10)

		cities.createVersion(
			Template.Version(
				Instant.DISTANT_PAST,
				"Second version",
				label("Other field")
			)
		).shouldSucceedAnd { versionRef ->
			versionRef.now().shouldSucceedAnd { version ->
				assertEquals("Second version", version.title)
				assertTrue(
					testStart < version.creationDate,
					"The date ${version.creationDate} should be in the past compared to the start of the test at $testStart"
				)
				assertTrue(
					version.creationDate > Instant.DISTANT_PAST,
					"The template creation shouldn't keep the date provided by the client"
				)

				assertEquals(label("Other field"), version.field)
			}

			cities.now().shouldSucceedAnd {
				assertEquals(2, it.versions.size, "Expected two versions, found ${it.versions}")
				assertContains(it.versions, versionRef, "The version we just created should be one of the two versions")
			}
		}
	}

	@Test
	@JsName("createTemplateInvalidTemplate")
	fun `cannot create a template with an invalid field import`() = runTest(administratorAuth) {
		val templates = new()

		val cities = testCityTemplate(templates).now()
			.map { it.versions.first() }
			.orThrow()

		shouldBeInvalid(
			templates.create(
				"Test",
				"Initial version",
				labelFrom(cities, "This field does not match the imported template at all")
			)
		)
	}

	@Test
	@JsName("createTemplateVersionInvalidTemplate")
	fun `cannot create a template version with an invalid field import`() = runTest(administratorAuth) {
		val templates = new()

		val cities = testCityTemplate(templates)
		val firstVersion = cities.now()
			.map { it.versions.first() }
			.orThrow()

		shouldBeInvalid(
			cities.createVersion(
				Template.Version(
					currentInstant(),
					"Second version",
					labelFrom(firstVersion, "This field does not match the imported template at all")
				)
			)
		)
	}

	@Test
	@JsName("closeTemplate")
	fun `close template`() = runTest(administratorAuth) {
		val templates = new()

		val cities = testCityTemplate(templates)

		shouldSucceed(cities.close())
		cities.now().shouldSucceedAnd { assertFalse(it.open) }
		templates.list(includeClosed = false).shouldSucceedAnd { it shouldNotContain cities }
		templates.list(includeClosed = true).shouldSucceedAnd { it shouldContain cities }

		shouldSucceed(cities.open())
		cities.now().shouldSucceedAnd { assertTrue(it.open) }
		templates.list(includeClosed = false).shouldSucceedAnd { it shouldContain cities }
		templates.list(includeClosed = true).shouldSucceedAnd { it shouldContain cities }
	}

	@Test
	@JsName("renameTemplate")
	fun `rename template`() = runTest(administratorAuth) {
		val templates = new()

		val cities = testCityTemplate(templates)

		shouldSucceed(cities.rename("Alternative cities"))
		cities.now().shouldSucceedAnd {
			assertEquals("Alternative cities", it.name)
		}
	}

	//endregion

}
