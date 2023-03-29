package opensavvy.formulaide.test

import arrow.core.flatMap
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.withContext
import opensavvy.backbone.Ref.Companion.now
import opensavvy.formulaide.core.Department
import opensavvy.formulaide.core.Field.Companion.group
import opensavvy.formulaide.core.Field.Companion.groupFrom
import opensavvy.formulaide.core.Field.Companion.input
import opensavvy.formulaide.core.Field.Companion.label
import opensavvy.formulaide.core.Field.Companion.labelFrom
import opensavvy.formulaide.core.Form
import opensavvy.formulaide.core.Input
import opensavvy.formulaide.core.Template
import opensavvy.formulaide.test.assertions.*
import opensavvy.formulaide.test.execution.Setup
import opensavvy.formulaide.test.execution.Suite
import opensavvy.formulaide.test.execution.prepare
import opensavvy.formulaide.test.execution.prepared
import opensavvy.formulaide.test.utils.TestClock.Companion.currentInstant
import opensavvy.formulaide.test.utils.TestUsers.administratorAuth
import opensavvy.formulaide.test.utils.TestUsers.employeeAuth
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

//endregion

fun Suite.formTestSuite(
	testDepartments: Setup<Department.Service>,
	testTemplates: Setup<Template.Service>,
	testForms: Setup<Form.Service>,
) {
	val testDepartment by prepared {
		val departments = prepare(testDepartments)

		createDepartment(departments)
	}

	suite("Access forms") {
		val testPrivateForm by prepared(administratorAuth) {
			val forms = prepare(testForms)
			val department = prepare(testDepartment)

			forms.create(
				name = "Private form",
				firstVersionTitle = "Initial version",
				field = label("Field"),
				Form.Step(0, "Review", department, null),
			).orThrow()
		}

		val testPublicForm by prepared(administratorAuth) {
			val forms = prepare(testForms)
			val department = prepare(testDepartment)

			forms.create(
				name = "Public form",
				firstVersionTitle = "Initial version",
				field = label("Field"),
				Form.Step(0, "Review", department, null),
			).tap { it.publicize() }
				.orThrow()
		}

		suite("Guests can access open forms that are public") {
			test("Guests can list forms that are open and public") {
				val forms = prepare(testForms)
				val publicForm = prepare(testPublicForm)

				forms.list() shouldSucceedAnd {
					it shouldContain publicForm
				}
			}

			test("Guests can read forms that are open and public") {
				val publicForm = prepare(testPublicForm)

				publicForm.now().shouldSucceed()
			}

			test("Guests can read form versions that are open and public") {
				val publicForm = prepare(testPublicForm)
					.now().map { it.versionsSorted.first() }.orThrow()

				publicForm.now().shouldSucceed()
			}

			test("Private forms are not visible to guests") {
				val forms = prepare(testForms)
				val privateForm = prepare(testPrivateForm)

				forms.list() shouldSucceedAnd {
					it shouldNotContain privateForm
				}
			}

			test("Guests cannot read private forms") {
				val privateForm = prepare(testPrivateForm)

				shouldNotBeFound(privateForm.now())
			}

			test("Guests cannot list closed forms") {
				val forms = prepare(testForms)

				shouldNotBeAuthenticated(forms.list(includeClosed = true))
			}

			test("Guests cannot read closed forms") {
				val publicForm = prepare(testPublicForm)
					.also { withContext(administratorAuth) { it.close().orThrow() } }

				shouldNotBeFound(publicForm.now())
			}
		}

		suite("Employees can access all forms") {
			test("Employees can list forms that are open and public", employeeAuth) {
				val forms = prepare(testForms)
				val publicForm = prepare(testPublicForm)

				forms.list() shouldSucceedAnd {
					it shouldContain publicForm
				}
			}

			test("Employees can read forms that are open and public", employeeAuth) {
				val publicForm = prepare(testPublicForm)

				publicForm.now().shouldSucceed()
			}

			test("Employees can list forms that are open and private", employeeAuth) {
				val forms = prepare(testForms)
				val privateForm = prepare(testPrivateForm)

				forms.list() shouldSucceedAnd {
					it shouldContain privateForm
				}
			}

			test("Employees can read forms that are open and private", employeeAuth) {
				val privateForm = prepare(testPrivateForm)

				privateForm.now().shouldSucceed()
			}

			test("Employees can list closed forms", employeeAuth) {
				val forms = prepare(testForms)
				val publicForm =
					prepare(testPublicForm).also { withContext(administratorAuth) { it.close().orThrow() } }
				val privateForm =
					prepare(testPrivateForm).also { withContext(administratorAuth) { it.close().orThrow() } }

				forms.list(includeClosed = true) shouldSucceedAndSoftly {
					it shouldContain publicForm
					it shouldContain privateForm
				}
			}
		}
	}

	suite("Create forms") {
		suite("Access rights") {
			val createForm by prepared {
				val forms = prepare(testForms)
				val department = prepare(testDepartment)

				forms.create(
					"A new form",
					"Initial version",
					label("Field"),
					Form.Step(0, "Validation", department, null),
				)
			}

			test("Guests cannot create forms") {
				shouldNotBeAuthenticated(prepare(createForm))
			}

			test("Employees cannot create forms", employeeAuth) {
				shouldNotBeAuthorized(prepare(createForm))
			}

			test("Administrators can create forms", administratorAuth) {
				shouldSucceed(prepare(createForm))
			}
		}

		suite("The created form is correct") {
			val createSimpleForm by prepared {
				val form = prepare(testForms)
				val department = prepare(testDepartment)

				form.create(
					"An example form",
					"First version",
					label("The field"),
					Form.Step(0, "Validation", department, null),
				).orThrow()
			}

			test("The name of the form is correct", administratorAuth) {
				val form = prepare(createSimpleForm)

				form.now() shouldSucceedAnd {
					it.name shouldBe "An example form"
				}
			}

			test("The created form is open", administratorAuth) {
				val form = prepare(createSimpleForm)

				form.now() shouldSucceedAnd {
					it.open shouldBe true
				}
			}

			test("The created form is private", administratorAuth) {
				val form = prepare(createSimpleForm)

				form.now() shouldSucceedAnd {
					it.public shouldBe false
				}
			}

			test("The created form has a single version", administratorAuth) {
				val form = prepare(createSimpleForm)

				form.now() shouldSucceedAnd {
					it.versions shouldHaveSize 1
					it.versions.first().now() shouldSucceedAndSoftly { version ->
						version.title shouldBe "First version"
						version.field shouldBe label("The field")
					}
				}
			}

			test(
				"The version ignores the user-provided timestamp and replaces it with the current one",
				administratorAuth
			) {
				val testStart = currentInstant()
				advanceTimeBy(10)

				val form = prepare(createSimpleForm)

				advanceTimeBy(10)
				val testEnd = currentInstant()

				form.now()
					.flatMap { it.versions.first().now() }
					.shouldSucceedAndSoftly {
						it.creationDate shouldBeGreaterThan testStart
						it.creationDate shouldBeLessThan testEnd
					}
			}
		}

		suite("Creating a form which includes a template") {
			val createTemplate by prepared {
				val templates = prepare(testTemplates)

				templates.create(
					"Cities",
					"First version",
					group(
						"City",
						0 to input("Name", Input.Text(maxLength = 50u)),
						1 to input("Postal code", Input.Text(maxLength = 5u)),
					)
				).flatMap { it.now() }
					.map { it.versions.first() }
					.orThrow()
			}

			test("It is possible to import a template in a form", administratorAuth) {
				val forms = prepare(testForms)
				val department = prepare(testDepartment)
				val template = prepare(createTemplate)

				forms.create(
					"Test",
					"Initial version",
					groupFrom(
						template,
						"City",
						0 to input("Name", Input.Text(maxLength = 50u)),
						1 to input("Postal code", Input.Text(maxLength = 5u)),
					),
					Form.Step(0, "Validation", department, null),
				)
			}

			test("It is not possible to create a form which incorrectly imports a template", administratorAuth) {
				val forms = prepare(testForms)
				val department = prepare(testDepartment)
				val template = prepare(createTemplate)

				forms.create(
					"Test",
					"Initial version",
					labelFrom(template, "This field does not match the imported template at all"),
					Form.Step(0, "Validation", department, null),
				)
			}
		}
	}

	suite("Form versioning") {
		val testForm by prepared(administratorAuth) {
			val forms = prepare(testForms)
			val department = prepare(testDepartment)

			forms.create(
				"Form versioning test",
				"Initial version",
				label("The field"),
				Form.Step(0, "Validation", department, null),
			).orThrow()
		}

		val newVersion by prepared {
			val form = prepare(testForm)
			val department = prepare(testDepartment)

			delay(1000) // Ensure the new version has a different timestamp than the initial one

			form.createVersion(
				"Version 2",
				input("New field", Input.Text()),
				Form.Step(0, "Validation 2", department, null),
			)
		}

		test("Guests cannot create new versions") {
			shouldNotBeAuthenticated(prepare(newVersion))
		}

		test("Employees cannot create new versions", employeeAuth) {
			shouldNotBeAuthorized(prepare(newVersion))
		}

		test("Administrators can create new versions", administratorAuth) {
			shouldSucceed(prepare(newVersion))
		}

		suite("The created version is correct") {
			test("The title of the version is correct", administratorAuth) {
				val version = prepare(newVersion).orThrow()

				version.now() shouldSucceedAnd {
					it.title shouldBe "Version 2"
				}
			}

			test("The creation date is correct", administratorAuth) {
				val testStart = currentInstant()
				advanceTimeBy(10)

				val version = prepare(newVersion).orThrow()

				advanceTimeBy(10)
				val testEnd = currentInstant()

				version.now() shouldSucceedAndSoftly {
					it.creationDate shouldBeGreaterThan testStart
					it.creationDate shouldBeLessThan testEnd
				}
			}

			test("The field is correct", administratorAuth) {
				val version = prepare(newVersion).orThrow()

				version.now() shouldSucceedAnd {
					it.field shouldBe input("New field", Input.Text())
				}
			}

			test("The step is correct", administratorAuth) {
				val department = prepare(testDepartment)
				val version = prepare(newVersion).orThrow()

				version.now() shouldSucceedAnd {
					it.stepsSorted shouldBe listOf(Form.Step(0, "Validation 2", department, null))
				}
			}

			test("The resulting form has the correct number of versions", administratorAuth) {
				val form = prepare(newVersion)
					.flatMap { it.form.now() }
					.orThrow()

				form.versions shouldHaveSize 2
			}

			test("The previous versions are not modified", administratorAuth) {
				val department = prepare(testDepartment)
				val form = prepare(newVersion)
					.flatMap { it.form.now() }
					.orThrow()

				form.versionsSorted[0].now() shouldSucceedAndSoftly {
					it.title shouldBe "Initial version"
					it.field shouldBe label("The field")
					it.stepsSorted shouldBe listOf(Form.Step(0, "Validation", department, null))
				}
			}

			test("The version is correctly added to the list of versions", administratorAuth) {
				val department = prepare(testDepartment)
				val form = prepare(newVersion)
					.flatMap { it.form.now() }
					.orThrow()

				form.versionsSorted[1].now() shouldSucceedAnd {
					it.title shouldBe "Version 2"
					it.field shouldBe input("New field", Input.Text())
					it.stepsSorted shouldBe listOf(Form.Step(0, "Validation 2", department, null))
				}
			}

			test("The new version has a timestamp strictly superior to the previous one", administratorAuth) {
				val form = prepare(newVersion)
					.flatMap { it.form.now() }
					.orThrow()

				val firstVersion = form.versionsSorted[0].now().orThrow().creationDate
				val secondVersion = form.versionsSorted[1].now().orThrow().creationDate

				secondVersion shouldBeGreaterThan firstVersion
			}
		}

		suite("Creating a version which includes a template") {
			val testTemplate by prepared(administratorAuth) {
				val templates = prepare(testTemplates)

				templates.create(
					"Included template",
					"Initial version",
					group(
						"Root",
						0 to input("First name", Input.Text(maxLength = 20u)),
						1 to input("Last name", Input.Text(maxLength = 20u)),
					)
				).flatMap { it.now() }
					.map { it.versions.first() }
					.orThrow()
			}

			test("It is possible to import a template in a version", administratorAuth) {
				val form = prepare(testForm)
				val template = prepare(testTemplate)
				val department = prepare(testDepartment)

				delay(1000)

				shouldSucceed(
					form.createVersion(
						"Imported version",
						groupFrom(
							template,
							"Imported root",
							0 to input("First name", Input.Text(maxLength = 20u)),
							1 to input("Last name", Input.Text(maxLength = 20u)),
						),
						Form.Step(0, "Validation", department, null),
					)
				)
			}

			test("It is not possible to create a version which incorrectly imports a template", administratorAuth) {
				val form = prepare(testForm)
				val template = prepare(testTemplate)
				val department = prepare(testDepartment)

				delay(1000)

				shouldBeInvalid(
					form.createVersion(
						"Imported version",
						groupFrom(
							template,
							"Imported root",
							0 to input("First name", Input.Text(maxLength = 30u)), // larger than the imported value
							1 to input("Last name", Input.Text(maxLength = 30u)),
						),
						Form.Step(0, "Validation", department, null),
					)
				)
			}
		}
	}

	suite("Form editing") {
		fun prepareForm(name: String, open: Boolean, public: Boolean) = prepared(administratorAuth) {
			val forms = prepare(testForms)
			val department = prepare(testDepartment)

			val form = forms.create(
				"Form editing test ($name)",
				"Initial version",
				label("The field"),
				Form.Step(0, "Validation", department, null),
			).orThrow()

			if (!open) {
				form.close().orThrow()
			}

			if (public) {
				form.publicize().orThrow()
			}

			form.now().orThrow().also {
				check(it.open == open) { "The created form is not in the expected status, expected open=$open but found $it" }
				check(it.public == public) { "The created form is not in the expected status, expected public=$public but found $it" }
			}

			form
		}

		val privateClosedForm by prepareForm("private, closed", public = false, open = false)
		val privateOpenForm by prepareForm("private, open", public = false, open = true)
		val publicClosedForm by prepareForm("public, closed", public = true, open = false)
		val publicOpenForm by prepareForm("public, open", public = true, open = true)

		suspend fun examineAsAdmin(should: String, block: suspend () -> Unit) {
			withClue("Verification by the administrator: $should") {
				withContext(administratorAuth) {
					block()
				}
			}
		}

		suspend fun Form.Ref.examineAsAdmin(should: String, block: suspend (Form) -> Unit) {
			withClue("Form examination: $should") {
				withContext(administratorAuth) {
					now() shouldSucceedAnd {
						block(it)
					}
				}
			}
		}

		suite("Renaming") {
			val testForm = publicOpenForm

			test("Guests cannot rename forms") {
				val form = prepare(testForm)

				shouldNotBeAuthenticated(form.rename("New version"))

				form.examineAsAdmin("the name should not have changed") {
					it.name shouldStartWith "Form editing test"
				}
			}

			test("Employees cannot rename forms", employeeAuth) {
				val form = prepare(testForm)

				shouldNotBeAuthorized(form.rename("new version"))

				form.examineAsAdmin("the name should not have changed") {
					it.name shouldStartWith "Form editing test"
				}
			}

			test("Administrators can rename forms", administratorAuth) {
				val form = prepare(testForm)

				shouldSucceed(form.rename("New version"))

				form.examineAsAdmin("the name should have changed") {
					it.name shouldBe "New version"
				}
			}
		}

		suite("Archiving") {
			test("Guests cannot open a form") {
				val form = prepare(publicClosedForm)
				val forms = prepare(testForms)

				shouldNotBeAuthenticated(form.open())

				form.examineAsAdmin("should still be closed") {
					it.open shouldBe false
				}

				examineAsAdmin("the form shouldn't appear in the open form list") {
					forms.list(includeClosed = false) shouldSucceedAnd {
						it shouldNotContain form
					}
				}

				examineAsAdmin("the form should appear in the closed form list") {
					forms.list(includeClosed = true) shouldSucceedAnd {
						it shouldContain form
					}
				}
			}

			test("Guests cannot close a form") {
				val form = prepare(publicOpenForm)
				val forms = prepare(testForms)

				shouldNotBeAuthenticated(form.close())

				form.examineAsAdmin("should still be open") {
					it.open shouldBe true
				}

				examineAsAdmin("the form should appear in the open form list") {
					forms.list(includeClosed = false) shouldSucceedAnd {
						it shouldContain form
					}

					forms.list(includeClosed = true) shouldSucceedAnd {
						it shouldContain form
					}
				}
			}

			test("Employees cannot open a form", employeeAuth) {
				val form = prepare(publicClosedForm)
				val forms = prepare(testForms)

				shouldNotBeAuthorized(form.open())

				form.examineAsAdmin("should still be closed") {
					it.open shouldBe false
				}

				examineAsAdmin("the form shouldn't appear in the open form list") {
					forms.list(includeClosed = false) shouldSucceedAnd {
						it shouldNotContain form
					}
				}

				examineAsAdmin("the form should appear in the closed form list") {
					forms.list(includeClosed = true) shouldSucceedAnd {
						it shouldContain form
					}
				}
			}

			test("Employees cannot close a form", employeeAuth) {
				val form = prepare(publicOpenForm)
				val forms = prepare(testForms)

				shouldNotBeAuthorized(form.close())

				form.examineAsAdmin("should still be open") {
					it.open shouldBe true
				}

				examineAsAdmin("the form should appear in the open form list") {
					forms.list(includeClosed = false) shouldSucceedAnd {
						it shouldContain form
					}

					forms.list(includeClosed = true) shouldSucceedAnd {
						it shouldContain form
					}
				}
			}

			test("Administrators can open forms", administratorAuth) {
				val form = prepare(publicClosedForm)
				val forms = prepare(testForms)

				shouldSucceed(form.open())

				form.examineAsAdmin("should now be open") {
					it.open shouldBe true
				}

				examineAsAdmin("the form shouldn't appear in the open form list") {
					forms.list(includeClosed = false) shouldSucceedAnd {
						it shouldContain form
					}

					forms.list(includeClosed = true) shouldSucceedAnd {
						it shouldContain form
					}
				}
			}

			test("Administrators can close forms", administratorAuth) {
				val form = prepare(publicOpenForm)
				val forms = prepare(testForms)

				shouldSucceed(form.close())

				form.examineAsAdmin("should now be closed") {
					it.open shouldBe false
				}

				examineAsAdmin("the form shouldn't appear in the open form list") {
					forms.list(includeClosed = false) shouldSucceedAnd {
						it shouldNotContain form
					}
				}

				examineAsAdmin("the form should appear in the closed form list") {
					forms.list(includeClosed = true) shouldSucceedAnd {
						it shouldContain form
					}
				}
			}
		}

		suite("Visibility") {
			test("Guests cannot publish a form") {
				val form = prepare(privateOpenForm)

				shouldNotBeAuthenticated(form.publicize())

				form.examineAsAdmin("should still be private") {
					it.public shouldBe false
				}
			}

			test("Guests cannot privatize a form") {
				val form = prepare(publicOpenForm)

				shouldNotBeAuthenticated(form.privatize())

				form.examineAsAdmin("should still be public") {
					it.public shouldBe true
				}
			}

			test("Employees cannot publish a form", employeeAuth) {
				val form = prepare(privateOpenForm)

				shouldNotBeAuthorized(form.publicize())

				form.examineAsAdmin("should still be private") {
					it.public shouldBe false
				}
			}

			test("Employees cannot privatize a form", employeeAuth) {
				val form = prepare(publicOpenForm)

				shouldNotBeAuthorized(form.privatize())

				form.examineAsAdmin("should still be public") {
					it.public shouldBe true
				}
			}

			test("Administrators can publish a form", administratorAuth) {
				val form = prepare(privateClosedForm)

				shouldSucceed(form.publicize())

				form.examineAsAdmin("should now be public") {
					it.public shouldBe true
				}
			}

			test("Administrators can publish a form", administratorAuth) {
				val form = prepare(publicOpenForm)

				shouldSucceed(form.privatize())

				form.examineAsAdmin("should now be private") {
					it.public shouldBe false
				}
			}
		}
	}
}
