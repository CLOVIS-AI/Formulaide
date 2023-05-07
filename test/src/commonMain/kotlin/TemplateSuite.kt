package opensavvy.formulaide.test

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.withContext
import opensavvy.backbone.now
import opensavvy.formulaide.core.Field.Companion.arity
import opensavvy.formulaide.core.Field.Companion.group
import opensavvy.formulaide.core.Field.Companion.input
import opensavvy.formulaide.core.Field.Companion.label
import opensavvy.formulaide.core.Field.Companion.labelFrom
import opensavvy.formulaide.core.Input
import opensavvy.formulaide.core.Template
import opensavvy.formulaide.test.assertions.*
import opensavvy.formulaide.test.structure.*
import opensavvy.formulaide.test.utils.TestUsers.administratorAuth
import opensavvy.formulaide.test.utils.TestUsers.employeeAuth
import opensavvy.state.outcome.map

//region Test data

internal fun createCityTemplate(templates: Setup<Template.Service>) = prepared(administratorAuth) {
	prepare(templates).create(
		"Cities",
		"First version",
		group(
			"City",
			0 to input("Name", Input.Text(maxLength = 50u)),
			1 to input("Postal code", Input.Text(maxLength = 5u)),
		)
	).bind()
}

internal fun createIdentityTemplate(templates: Setup<Template.Service>) = prepared(administratorAuth) {
	prepare(templates).create(
		"Identities",
		"First version",
		group(
			"Identity",
			0 to arity("First name(s)", 1u..10u, input("First name", Input.Text(maxLength = 30u))),
			1 to input("Last name", Input.Text(maxLength = 30u)),
			// In the future, add the city here
		)
	).bind()
}

//endregion

fun Suite.templateTestSuite(
	createTemplates: Setup<Template.Service>,
) {
	list(createTemplates)
	request(createTemplates)
	create(createTemplates)
	createVersion(createTemplates)
	edit(createTemplates)
}

private fun Suite.list(
	createTemplates: Setup<Template.Service>,
) = suite("List templates") {
	val createCityTemplate by createCityTemplate(createTemplates)
	val createIdentityTemplate by createIdentityTemplate(createTemplates)

	test("guests cannot list templates") {
		val templates = prepare(createTemplates)

		shouldNotBeAuthenticated(templates.list(includeClosed = false))
		shouldNotBeAuthenticated(templates.list(includeClosed = true))
	}

	test("employees can list open templates", employeeAuth) {
		val templates = prepare(createTemplates)

		val open = prepare(createCityTemplate)
		val closed = prepare(createIdentityTemplate)
			.also { withContext(administratorAuth) { it.close().bind() } }

		templates.list(includeClosed = false) shouldSucceedAnd {
			it shouldContain open
			it shouldNotContain closed
		}
	}

	test("employees can list private templates", employeeAuth) {
		val templates = prepare(createTemplates)

		val open = prepare(createCityTemplate)
		val closed = prepare(createIdentityTemplate)
			.also { withContext(administratorAuth) { it.close().bind() } }

		templates.list(includeClosed = true) shouldSucceedAnd {
			it shouldContain open
			it shouldContain closed
		}
	}
}

private fun Suite.request(
	createTemplates: Setup<Template.Service>,
) = suite("Access templates") {
	val createCityTemplate by createCityTemplate(createTemplates)

	test("guests cannot access open templates") {
		prepare(createTemplates)

		val target = prepare(createCityTemplate)
		shouldNotBeAuthenticated(target.now())
	}

	test("guests cannot access closed templates") {
		prepare(createTemplates)

		val target = prepare(createCityTemplate)
			.also { withContext(administratorAuth) { it.close().bind() } }
		shouldNotBeAuthenticated(target.now())
	}

	test("employees can access templates", employeeAuth) {
		prepare(createTemplates)

		val target = prepare(createCityTemplate)
		shouldSucceed(target.now())
	}

	test("employees can access closed templates", employeeAuth) {
		prepare(createTemplates)

		val target = prepare(createCityTemplate)
			.also { withContext(administratorAuth) { it.close().bind() } }
		shouldSucceed(target.now())
	}
}

private fun Suite.create(
	createTemplates: Setup<Template.Service>,
) = suite("Create templates") {
	val createCityTemplate by createCityTemplate(createTemplates)

	test("guests cannot create templates") {
		val templates = prepare(createTemplates)

		shouldNotBeAuthenticated(templates.create("A new template", "First version", label("field")))
	}

	test("employees cannot create templates", employeeAuth) {
		val templates = prepare(createTemplates)

		shouldNotBeAuthorized(templates.create("A new template", "First version", label("field")))
	}

	test("administrators can create a template", administratorAuth) {
		val templates = prepare(createTemplates)
		val testStart = currentInstant
		advanceTimeBy(10)

		val template = templates.create(
			"An example template",
			"First version",
			label("The field"),
		).shouldSucceed()

		advanceTimeBy(10)
		val testEnd = currentInstant

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
		val templates = prepare(createTemplates)

		val cities = prepare(createCityTemplate).now()
			.map { it.versions.first() }.bind()

		templates.create(
			"Test",
			"Initial version",
			labelFrom(cities, "This field does not match the imported template at all")
		) shouldFailWithType Template.Failures.InvalidImport::class
	}
}

private fun Suite.createVersion(
	createTemplates: Setup<Template.Service>,
) = suite("Create a new version of an existing template") {
	val createCityTemplate by createCityTemplate(createTemplates)

	test("guests cannot create a new version of a template") {
		prepare(createTemplates)
		val target = prepare(createCityTemplate)

		shouldNotBeAuthenticated(target.createVersion("Second version", label("field")))
	}

	test("employees cannot create new versions of a template", employeeAuth) {
		prepare(createTemplates)
		val target = prepare(createCityTemplate)

		shouldNotBeAuthorized(target.createVersion("Second version", label("field")))
	}

	test("administrators can create new versions of a template", administratorAuth) {
		prepare(createTemplates)
		val testStart = currentInstant
		advanceTimeBy(10)

		val template = prepare(createCityTemplate)
		advanceTimeBy(10)

		val versionRef = template.createVersion(
			"Second version",
			label("Other field"),
		).shouldSucceed()

		advanceTimeBy(10)
		val testEnd = currentInstant

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
		prepare(createTemplates)

		val cities = prepare(createCityTemplate)
		advanceTimeBy(10)

		val firstVersion = cities.now()
			.map { it.versions.first() }.bind()

		cities.createVersion(
			"Second version",
			labelFrom(
				firstVersion,
				"This field does not match the imported template at all"
			)
		) shouldFailWithType Template.Failures.InvalidImport::class
	}
}

private fun Suite.edit(
	createTemplates: Setup<Template.Service>,
) = suite("Edit a template") {
	val createCityTemplate by createCityTemplate(createTemplates)

	test("guests cannot rename templates") {
		prepare(createTemplates)
		val target = prepare(createCityTemplate)

		shouldNotBeAuthenticated(target.rename("New name"))

		withContext(administratorAuth) {
			target.now() shouldSucceedAnd {
				it.name shouldBe "Cities"
			}
		}
	}

	test("guests cannot open or close templates") {
		prepare(createTemplates)
		val target = prepare(createCityTemplate)

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
		prepare(createTemplates)
		val target = prepare(createCityTemplate)

		shouldNotBeAuthorized(target.rename("New name"))

		withContext(administratorAuth) {
			target.now() shouldSucceedAnd {
				it.name shouldBe "Cities"
			}
		}
	}

	test("employees cannot open or close templates", employeeAuth) {
		prepare(createTemplates)
		val target = prepare(createCityTemplate)

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
		prepare(createTemplates)
		val target = prepare(createCityTemplate)

		shouldSucceed(target.rename("New name"))

		target.now() shouldSucceedAnd {
			it.name shouldBe "New name"
		}
	}

	test("administrators can open or close templates", administratorAuth) {
		val templates = prepare(createTemplates)
		val target = prepare(createCityTemplate)

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
