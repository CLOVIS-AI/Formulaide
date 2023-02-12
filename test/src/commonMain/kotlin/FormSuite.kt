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
import opensavvy.formulaide.core.Department
import opensavvy.formulaide.core.Field.Companion.group
import opensavvy.formulaide.core.Field.Companion.input
import opensavvy.formulaide.core.Field.Companion.label
import opensavvy.formulaide.core.Field.Companion.labelFrom
import opensavvy.formulaide.core.Form
import opensavvy.formulaide.core.Input
import opensavvy.formulaide.core.Template
import opensavvy.formulaide.test.assertions.*
import opensavvy.formulaide.test.cases.TestUsers.administratorAuth
import opensavvy.formulaide.test.cases.TestUsers.employeeAuth
import opensavvy.formulaide.test.execution.Factory
import opensavvy.formulaide.test.execution.Suite
import opensavvy.formulaide.test.utils.TestClock.Companion.currentInstant
import opensavvy.state.outcome.orThrow

//region Test data

internal suspend fun createSimpleForm(
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

internal suspend fun createIdeaForm(
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

data class FormTestData(
	val departments: Department.Service,
	val templates: Template.Service,
	val forms: Form.Service,
)

fun Suite.formTestSuite(
	create: Factory<FormTestData>,
) {
	list(create)
	create(create)
	createVersion(create)
	edit(create)
}

private fun Suite.list(
	create: Factory<FormTestData>,
) = suite("List forms") {
	test("guests can list open forms, but will only see public forms") {
		val testData = create()
		val forms = testData.forms

		val dept = createDepartment(testData.departments)
		val private = createSimpleForm(forms, dept)
		val public = createIdeaForm(forms, dept, dept)

		forms.list() shouldSucceedAnd {
			it shouldContain public
			it shouldNotContain private
		}
	}

	test("guests cannot list closed forms") {
		val testData = create()
		val forms = testData.forms

		shouldNotBeAuthenticated(forms.list(includeClosed = true))
	}

	test("employees can list open forms, even if they are private", employeeAuth) {
		val testData = create()
		val forms = testData.forms

		val dept = createDepartment(testData.departments)
		val private = createSimpleForm(forms, dept)
		val public = createIdeaForm(forms, dept, dept)

		forms.list() shouldSucceedAnd {
			it shouldContain public
			it shouldContain private
		}
	}

	test("employees can list closed forms", employeeAuth) {
		val testData = create()
		val forms = testData.forms

		val dept = createDepartment(testData.departments)
		val private = createSimpleForm(forms, dept)
			.also { withContext(administratorAuth) { it.close().orThrow() } }
		val public = createIdeaForm(forms, dept, dept)
			.also { withContext(administratorAuth) { it.close().orThrow() } }

		forms.list(includeClosed = true) shouldSucceedAnd {
			it shouldContain public
			it shouldContain private
		}
	}
}

private fun Suite.create(
	create: Factory<FormTestData>,
) = suite("Create forms") {
	test("guests cannot create forms") {
		val testData = create()
		val forms = testData.forms
		val dept = createDepartment(testData.departments)

		shouldNotBeAuthenticated(
			forms.create(
				"A new form", "Initial version", label("Field"),
				Form.Step(0, "Validation", dept, null)
			)
		)
	}

	test("employees cannot create forms", employeeAuth) {
		val testData = create()
		val forms = testData.forms
		val dept = createDepartment(testData.departments)

		shouldNotBeAuthorized(
			forms.create(
				"A new form", "Initial version", label("Field"),
				Form.Step(0, "Validation", dept, null)
			)
		)
	}

	test("administrators can create forms", administratorAuth) {
		val testData = create()
		val forms = testData.forms
		val dept = createDepartment(testData.departments)

		val testStart = currentInstant()
		advanceTimeBy(10)

		val form = forms.create(
			"An example form",
			"First version",
			label("The field"),
			Form.Step(
				0,
				"Validation",
				dept,
				null,
			)
		).shouldSucceed()

		advanceTimeBy(10)
		val testEnd = currentInstant()

		form.now() shouldSucceedAnd {
			it.name shouldBe "An example form"
			it.open shouldBe true
			it.public shouldBe false

			it.versions shouldHaveSize 1
			it.versions.first().now() shouldSucceedAnd { version ->
				version.title shouldBe "First version"
				version.creationDate shouldBeGreaterThan testStart
				version.creationDate shouldBeLessThan testEnd

				version.field shouldBe label("The field")
			}
		}
	}

	test("cannot create a form with an invalid field import", administratorAuth) {
		val testData = create()
		val forms = testData.forms
		val templates = testData.templates
		val dept = createDepartment(testData.departments)

		val cities = createCityTemplate(templates).now()
			.map { it.versions.first() }
			.orThrow()

		shouldBeInvalid(
			forms.create(
				"Test",
				"Initial version",
				labelFrom(cities, "This field does not match the imported template at all"),
				Form.Step(0, "Validation", dept, null),
			)
		)
	}
}

private fun Suite.createVersion(
	create: Factory<FormTestData>,
) = suite("Create new versions of an existing form") {
	test("guests cannot create new versions") {
		val testData = create()
		val forms = testData.forms
		val dept = createDepartment(testData.departments)
		val form = createSimpleForm(forms, dept)

		shouldNotBeAuthenticated(
			form.createVersion(
				"Title",
				label("Field"),
				Form.Step(0, "Validation", dept, null)
			)
		)
	}

	test("employees cannot create new versions", employeeAuth) {
		val testData = create()
		val forms = testData.forms
		val dept = createDepartment(testData.departments)
		val form = createSimpleForm(forms, dept)

		shouldNotBeAuthorized(
			form.createVersion(
				"Title",
				label("Field"),
				Form.Step(0, "Validation", dept, null)
			)
		)
	}

	test("administrators can create new versions of a form", administratorAuth) {
		val testData = create()
		val forms = testData.forms
		val dept = createDepartment(testData.departments)

		val testStart = currentInstant()
		advanceTimeBy(10)

		val idea = createIdeaForm(forms, dept, dept)

		val versionRef = idea.createVersion(
			"Second version",
			label("Other field"),
			Form.Step(0, "Validation", dept, null),
		).shouldSucceed()

		advanceTimeBy(10)
		val testEnd = currentInstant()

		versionRef.now() shouldSucceedAnd {
			it.title shouldBe "Second version"
			it.creationDate shouldBeGreaterThan testStart
			it.creationDate shouldBeLessThan testEnd
			it.field shouldBe label("Other field")
		}

		idea.now() shouldSucceedAnd {
			it.versions shouldHaveSize 2
			it.versions[1] shouldBe versionRef
		}
	}

	test("cannot create a version of a form with an invalid field import", administratorAuth) {
		val testData = create()

		val forms = testData.forms
		val templates = testData.templates
		val dept = createDepartment(testData.departments)

		val cities = createCityTemplate(templates).now()
			.map { it.versions.first() }
			.orThrow()

		val idea = createIdeaForm(forms, dept, dept)

		shouldBeInvalid(
			idea.createVersion(
				"New version",
				labelFrom(cities, "This field does not match the imported template at all"),
				Form.Step(0, "Validation", dept, null)
			)
		)
	}
}

private fun Suite.edit(
	create: Factory<FormTestData>,
) = suite("Edit forms") {
	test("guests cannot rename forms") {
		val testData = create()

		val forms = testData.forms
		val dept = createDepartment(testData.departments)
		val form = createSimpleForm(forms, dept)

		shouldNotBeAuthenticated(form.rename("New name"))

		withContext(administratorAuth) {
			form.now() shouldSucceedAnd {
				it.name shouldBe "Simple"
			}
		}
	}

	test("guests cannot open or close forms") {
		val testData = create()

		val forms = testData.forms
		val dept = createDepartment(testData.departments)
		val form = createSimpleForm(forms, dept)

		shouldNotBeAuthenticated(form.close())

		withContext(administratorAuth) {
			form.now() shouldSucceedAnd {
				it.open shouldBe true
			}
		}

		shouldNotBeAuthenticated(form.open())

		withContext(administratorAuth) {
			form.now() shouldSucceedAnd {
				it.open shouldBe true
			}
		}
	}

	test("guests cannot publicize or privatize forms") {
		val testData = create()

		val forms = testData.forms
		val dept = createDepartment(testData.departments)
		val form = createIdeaForm(forms, dept, dept)

		shouldNotBeAuthenticated(form.privatize())

		withContext(administratorAuth) {
			form.now() shouldSucceedAnd {
				it.public shouldBe true
			}
		}

		shouldNotBeAuthenticated(form.publicize())

		withContext(administratorAuth) {
			form.now() shouldSucceedAnd {
				it.public shouldBe true
			}
		}
	}

	test("employees cannot rename forms", employeeAuth) {
		val testData = create()
		val forms = testData.forms
		val dept = createDepartment(testData.departments)
		val form = createSimpleForm(forms, dept)

		shouldNotBeAuthorized(form.rename("New name"))

		form.now() shouldSucceedAnd {
			it.name shouldBe "Simple"
		}
	}

	test("employees cannot open or close forms", employeeAuth) {
		val testData = create()
		val forms = testData.forms
		val dept = createDepartment(testData.departments)
		val form = createSimpleForm(forms, dept)

		shouldNotBeAuthorized(form.close())

		form.now() shouldSucceedAnd {
			it.open shouldBe true
		}

		shouldNotBeAuthorized(form.open())

		form.now() shouldSucceedAnd {
			it.open shouldBe true
		}
	}

	test("employees cannot publicize or privatize forms", employeeAuth) {
		val testData = create()
		val forms = testData.forms
		val dept = createDepartment(testData.departments)
		val form = createIdeaForm(forms, dept, dept)

		shouldNotBeAuthorized(form.privatize())

		form.now() shouldSucceedAnd {
			it.public shouldBe true
		}

		shouldNotBeAuthorized(form.publicize())

		form.now() shouldSucceedAnd {
			it.public shouldBe true
		}
	}

	test("administrators can close and open forms", administratorAuth) {
		val testData = create()
		val forms = testData.forms
		val dept = createDepartment(testData.departments)
		val idea = createIdeaForm(forms, dept, dept)

		idea.close().shouldSucceed()

		idea.now() shouldSucceedAnd {
			it.open shouldBe false
		}

		forms.list(includeClosed = false) shouldSucceedAnd { it shouldNotContain idea }
		forms.list(includeClosed = true) shouldSucceedAnd { it shouldContain idea }

		idea.open().shouldSucceed()

		idea.now() shouldSucceedAnd { it.open shouldBe true }

		forms.list(includeClosed = false) shouldSucceedAnd { it shouldContain idea }
		forms.list(includeClosed = true) shouldSucceedAnd { it shouldContain idea }
	}

	test("administrators can publish and unpublish forms", administratorAuth) {
		val testData = create()
		val forms = testData.forms
		val dept = createDepartment(testData.departments)
		val idea = createIdeaForm(forms, dept, dept)

		idea.privatize().shouldSucceed()

		idea.now() shouldSucceedAnd {
			it.public shouldBe false
		}

		forms.list(includeClosed = false) shouldSucceedAnd { it shouldContain idea }
		forms.list(includeClosed = true) shouldSucceedAnd { it shouldContain idea }

		idea.publicize().shouldSucceed()

		idea.now() shouldSucceedAnd {
			it.public shouldBe true
		}

		forms.list(includeClosed = false) shouldSucceedAnd { it shouldContain idea }
		forms.list(includeClosed = true) shouldSucceedAnd { it shouldContain idea }
	}

	test("administrators can rename forms", administratorAuth) {
		val testData = create()
		val forms = testData.forms
		val dept = createDepartment(testData.departments)
		val idea = createIdeaForm(forms, dept, dept)

		idea.rename("Alternative ideas").shouldSucceed()

		idea.now() shouldSucceedAnd {
			it.name shouldBe "Alternative ideas"
		}
	}
}
