package opensavvy.formulaide.test

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.withContext
import opensavvy.backbone.Ref.Companion.now
import opensavvy.formulaide.core.Field.Companion.arity
import opensavvy.formulaide.core.Field.Companion.group
import opensavvy.formulaide.core.Field.Companion.input
import opensavvy.formulaide.core.Field.Companion.label
import opensavvy.formulaide.core.Field.Companion.labelFrom
import opensavvy.formulaide.core.Input
import opensavvy.formulaide.core.Template
import opensavvy.formulaide.test.assertions.*
import opensavvy.formulaide.test.execution.Factory
import opensavvy.formulaide.test.execution.Suite
import opensavvy.formulaide.test.utils.TestClock.Companion.currentInstant
import opensavvy.formulaide.test.utils.TestUsers.administratorAuth
import opensavvy.formulaide.test.utils.TestUsers.employeeAuth
import opensavvy.state.outcome.orThrow

//region Test data

internal suspend fun createCityTemplate(templates: Template.Service) = withContext(administratorAuth) {
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

internal suspend fun createIdentityTemplate(templates: Template.Service) = withContext(administratorAuth) {
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

fun Suite.templateTestSuite(
	createTemplates: Factory<Template.Service>,
) {
	list(createTemplates)
	request(createTemplates)
	create(createTemplates)
	createVersion(createTemplates)
	edit(createTemplates)
}

private fun Suite.list(
	createTemplates: Factory<Template.Service>,
) = suite("List templates") {
	test("guests cannot list templates") {
		val templates = createTemplates()

		shouldNotBeAuthenticated(templates.list(includeClosed = false))
		shouldNotBeAuthenticated(templates.list(includeClosed = true))
	}

	test("employees can list open templates", employeeAuth) {
		val templates = createTemplates()

		val open = createCityTemplate(templates)
		val closed = createIdentityTemplate(templates)
			.also { withContext(administratorAuth) { it.close().orThrow() } }

		templates.list(includeClosed = false) shouldSucceedAnd {
			it shouldContain open
			it shouldNotContain closed
		}
	}

	test("employees can list private templates", employeeAuth) {
		val templates = createTemplates()

		val open = createCityTemplate(templates)
		val closed = createIdentityTemplate(templates)
			.also { withContext(administratorAuth) { it.close().orThrow() } }

		templates.list(includeClosed = true) shouldSucceedAnd {
			it shouldContain open
			it shouldContain closed
		}
	}
}

private fun Suite.request(
	createTemplates: Factory<Template.Service>,
) = suite("Access templates") {
	test("guests cannot access open templates") {
		val templates = createTemplates()

		val target = createCityTemplate(templates)
		shouldNotBeAuthenticated(target.now())
	}

	test("guests cannot access closed templates") {
		val templates = createTemplates()

		val target = createCityTemplate(templates)
			.also { withContext(administratorAuth) { it.close().orThrow() } }
		shouldNotBeAuthenticated(target.now())
	}

	test("employees can access templates", employeeAuth) {
		val templates = createTemplates()

		val target = createCityTemplate(templates)
		shouldSucceed(target.now())
	}

	test("employees can access closed templates", employeeAuth) {
		val templates = createTemplates()

		val target = createCityTemplate(templates)
			.also { withContext(administratorAuth) { it.close().orThrow() } }
		shouldSucceed(target.now())
	}
}

private fun Suite.create(
	createTemplates: Factory<Template.Service>,
) = suite("Create templates") {
	test("guests cannot create templates") {
		val templates = createTemplates()

		shouldNotBeAuthenticated(templates.create("A new template", "First version", label("field")))
	}

	test("employees cannot create templates", employeeAuth) {
		val templates = createTemplates()

		shouldNotBeAuthorized(templates.create("A new template", "First version", label("field")))
	}

	test("administrators can create a template", administratorAuth) {
		val templates = createTemplates()
		val testStart = currentInstant()
		advanceTimeBy(10)

		val template = templates.create(
			"An example template",
			"First version",
			label("The field"),
		).shouldSucceed()

		advanceTimeBy(10)
		val testEnd = currentInstant()

		template.now() shouldSucceedAnd {
			it.name shouldBe "An example template"
			it.open shouldBe true
			it.versions shouldHaveSize 1

			it.versions[0].now() shouldSucceedAnd { version ->
				version.title shouldBe "First version"
				version.creationDate shouldBeGreaterThan testStart
				version.creationDate shouldBeLessThan testEnd

				version.field shouldBe label("The field")
			}
		}
	}

	test("cannot create a template with an invalid field import", administratorAuth) {
		val templates = createTemplates()

		val cities = createCityTemplate(templates).now()
			.map { it.versions.first() }.orThrow()

		shouldBeInvalid(
			templates.create(
				"Test",
				"Initial version",
				labelFrom(cities, "This field does not match the imported template at all")
			)
		)
	}
}

private fun Suite.createVersion(
	createTemplates: Factory<Template.Service>,
) = suite("Create a new version of an existing template") {
	test("guests cannot create a new version of a template") {
		val templates = createTemplates()
		val target = createCityTemplate(templates)

		shouldNotBeAuthenticated(target.createVersion("Second version", label("field")))
	}

	test("employees cannot create new versions of a template", employeeAuth) {
		val templates = createTemplates()
		val target = createCityTemplate(templates)

		shouldNotBeAuthorized(target.createVersion("Second version", label("field")))
	}

	test("administrators can create new versions of a template", administratorAuth) {
		val templates = createTemplates()
		val testStart = currentInstant()
		advanceTimeBy(10)

		val template = createCityTemplate(templates)

		val versionRef = template.createVersion(
			"Second version",
			label("Other field"),
		).shouldSucceed()

		advanceTimeBy(10)
		val testEnd = currentInstant()

		versionRef.now() shouldSucceedAnd {
			it.title shouldBe "Second version"
			it.creationDate shouldBeGreaterThan testStart
			it.creationDate shouldBeLessThan testEnd

			it.field shouldBe label("Other field")
		}

		template.now() shouldSucceedAnd {
			it.versions shouldHaveSize 2
			it.versions[1] shouldBe versionRef
		}
	}

	test("cannot create a template version with an invalid field import", administratorAuth) {
		val templates = createTemplates()

		val cities = createCityTemplate(templates)
		val firstVersion = cities.now()
			.map { it.versions.first() }.orThrow()

		shouldBeInvalid(
			cities.createVersion(
				"Second version",
				labelFrom(
					firstVersion,
					"This field does not match the imported template at all"
				)
			)
		)
	}
}

private fun Suite.edit(
	createTemplates: Factory<Template.Service>,
) = suite("Edit a template") {
	test("guests cannot rename templates") {
		val templates = createTemplates()
		val target = createCityTemplate(templates)

		shouldNotBeAuthenticated(target.rename("New name"))

		withContext(administratorAuth) {
			target.now() shouldSucceedAnd {
				it.name shouldBe "Cities"
			}
		}
	}

	test("guests cannot open or close templates") {
		val templates = createTemplates()
		val target = createCityTemplate(templates)

		shouldNotBeAuthenticated(target.close())

		withContext(administratorAuth) {
			target.now() shouldSucceedAnd {
				it.open shouldBe true
			}
		}

		shouldNotBeAuthenticated(target.open())

		withContext(administratorAuth) {
			target.now() shouldSucceedAnd {
				it.open shouldBe true
			}
		}
	}

	test("employees cannot rename templates", employeeAuth) {
		val templates = createTemplates()
		val target = createCityTemplate(templates)

		shouldNotBeAuthorized(target.rename("New name"))

		withContext(administratorAuth) {
			target.now() shouldSucceedAnd {
				it.name shouldBe "Cities"
			}
		}
	}

	test("employees cannot open or close templates", employeeAuth) {
		val templates = createTemplates()
		val target = createCityTemplate(templates)

		shouldNotBeAuthorized(target.close())

		withContext(administratorAuth) {
			target.now() shouldSucceedAnd {
				it.open shouldBe true
			}
		}

		shouldNotBeAuthorized(target.open())

		withContext(administratorAuth) {
			target.now() shouldSucceedAnd {
				it.open shouldBe true
			}
		}
	}

	test("administrators can rename templates", administratorAuth) {
		val templates = createTemplates()
		val target = createCityTemplate(templates)

		shouldSucceed(target.rename("New name"))

		target.now() shouldSucceedAnd {
			it.name shouldBe "New name"
		}
	}

	test("administrators can open or close templates", administratorAuth) {
		val templates = createTemplates()
		val target = createCityTemplate(templates)

		shouldSucceed(target.close())

		target.now() shouldSucceedAnd {
			it.open shouldBe false
		}

		templates.list(includeClosed = false) shouldSucceedAnd {
			it shouldNotContain target
		}

		templates.list(includeClosed = true) shouldSucceedAnd {
			it shouldContain target
		}

		shouldSucceed(target.open())

		target.now() shouldSucceedAnd {
			it.open shouldBe true
		}

		templates.list(includeClosed = false) shouldSucceedAnd {
			it shouldContain target
		}

		templates.list(includeClosed = true) shouldSucceedAnd {
			it shouldContain target
		}
	}
}
