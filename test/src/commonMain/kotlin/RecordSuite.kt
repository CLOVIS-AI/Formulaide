package opensavvy.formulaide.test

import arrow.core.flatMap
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.withContext
import opensavvy.backbone.Ref.Companion.now
import opensavvy.formulaide.core.*
import opensavvy.formulaide.core.Field.Companion.choice
import opensavvy.formulaide.core.Field.Companion.group
import opensavvy.formulaide.core.Field.Companion.input
import opensavvy.formulaide.core.Field.Companion.label
import opensavvy.formulaide.test.assertions.*
import opensavvy.formulaide.test.structure.*
import opensavvy.formulaide.test.utils.TestUsers.administratorAuth
import opensavvy.formulaide.test.utils.TestUsers.employeeAuth
import opensavvy.state.outcome.orThrow

fun Suite.recordsTestSuite(
	testDepartments: Setup<Department.Service>,
	testForms: Setup<Form.Service>,
	testRecords: Setup<Record.Service>,
	testFiles: Setup<File.Service>,
) {
	val testDepartment by createDepartment(testDepartments)

	suite("Create a record") {
		val testPrivateForm by prepared(administratorAuth) {
			val forms = prepare(testForms)
			val department = prepare(testDepartment)

			forms.create(
				name = "Private form",
				firstVersionTitle = "Initial version",
				field = input("The illusion of choice", Input.Toggle),
				Form.Step(0, "Review", department, null),
			).tap { it.privatize().orThrow() }
				.flatMap { it.now() }
				.map { it.versionsSorted.first() }
				.orThrow()
		}

		test("Guests cannot create records to a private form") {
			val form = prepare(testPrivateForm)
			val records = prepare(testRecords)

			shouldNotBeFound(
				records.create(
					form,
					"" to "true",
				)
			)
		}

		test("Employees can create records to a private form", employeeAuth) {
			val form = prepare(testPrivateForm)
			val records = prepare(testRecords)

			shouldSucceed(
				records.create(
					form,
					"" to "true",
				)
			)
		}

		val testPublicForm by prepared(administratorAuth) {
			val forms = prepare(testForms)
			val department = prepare(testDepartment)

			forms.create(
				name = "Private form",
				firstVersionTitle = "Initial version",
				field = input("The illusion of choice", Input.Toggle),
				Form.Step(0, "Review", department, null),
			).tap { it.publicize().orThrow() }
				.flatMap { it.now() }
				.map { it.versionsSorted.first() }
				.orThrow()
		}

		test("Guests can create records to a public form") {
			val form = prepare(testPublicForm)
			val records = prepare(testRecords)

			records.create(
				form,
				"" to "true",
			).shouldSucceed()
		}

		suite("Created record validity") {
			val testRecordRef by prepared {
				val form = prepare(testPublicForm)
				val records = prepare(testRecords)

				records.create(
					form,
					"" to "true",
				).orThrow()
			}

			val testRecord by prepared {
				val recordRef = prepare(testRecordRef)

				withContext(employeeAuth) {
					recordRef.now().orThrow()
				}
			}

			val testRecordFirstDiff by prepared {
				val record = prepare(testRecord)

				record.historySorted.first()
			}

			test("The author should be a guest") {
				val record = prepare(testRecordFirstDiff)

				record.author shouldBe null
			}

			test("The source should be the initial submission") {
				val record = prepare(testRecordFirstDiff)

				record.source shouldBe Record.Status.Initial
			}

			test("The target should be the first step") {
				val record = prepare(testRecordFirstDiff)

				record.target shouldBe Record.Status.Step(0)
			}

			test("The reason should not be known") {
				val record = prepare(testRecordFirstDiff)

				record.reason shouldBe null
			}

			test("The submission should be known") {
				val record = prepare(testRecordFirstDiff)
				val files = prepare(testFiles)

				record.submission shouldNotBe null

				withContext(employeeAuth) {
					record.submission!!.now() shouldSucceedAnd {
						val parsed = it.parse(files).shouldSucceed()

						parsed[Field.Id.root] shouldBe true
					}
				}
			}

			test("Employees can search for the record", employeeAuth) {
				val records = prepare(testRecords)
				val record = prepare(testRecordRef)

				records.search() shouldSucceedAnd {
					it shouldContain record
				}
			}
		}

		test("Cannot create a record for another step than the initial one") {
			val form = prepare(testPublicForm)
			val records = prepare(testRecords)

			shouldBeInvalid(
				records.create(
					Submission(
						form,
						1, // not the correct step
						mapOf(
							Field.Id.root to "true",
						)
					)
				)
			)
		}

		test("Cannot create a record with an invalid submission") {
			val form = prepare(testPublicForm)
			val records = prepare(testRecords)

			shouldBeInvalid(
				records.create(
					form,
					"" to "this is not a boolean",
				)
			)
		}
	}

	val workflowForm by prepared(administratorAuth) {
		val forms = prepare(testForms)
		val departments = prepare(testDepartments)

		val first = departments.create("First department").orThrow()
		val second = departments.create("Second department").orThrow()
		val third = departments.create("Third department").orThrow()

		forms.create(
			"The workflow tester",
			"First version",
			group(
				"Initial field",
				0 to group(
					"Identity",
					0 to input("First name", Input.Text()),
					1 to input("Last name", Input.Text()),
				),
				1 to input("Idea", Input.Text()),
			),
			Form.Step(
				id = 0,
				name = "Pre-filtering",
				reviewer = first,
				field = choice(
					"Kind of request",
					0 to label("Ecological"),
					1 to label("Industrial"),
					2 to label("Administrative"),
				)
			),
			Form.Step(
				id = 1,
				name = "Decide whether the request is worth it",
				reviewer = second,
				field = input("Request ID", Input.Text()),
			),
			Form.Step(
				id = 2,
				name = "Completed requests",
				reviewer = third,
				field = null,
			)
		).flatMap { it.now() }
			.orThrow()
			.versionsSorted.first()
	}

	val testRecord by prepared(employeeAuth) {
		val records = prepare(testRecords)
		val form = prepare(workflowForm)

		records.create(
			form,
			"" to "",
			"0" to "",
			"0:0" to "My first name",
			"0:1" to "My last name",
			"1" to "Plant more trees!",
		).orThrow()
	}

	suite("Workflow") {
		test("Guests cannot access records") {
			val records = prepare(testRecords)
			prepare(testRecord)

			shouldNotBeAuthenticated(records.search())
		}

		suite("Accept a record") {
			val accept by prepared(employeeAuth) {
				val record = prepare(testRecord)
				val records = prepare(testRecords)

				records.accept(
					record,
					null,
					"" to "1",
				).orThrow()

				record
			}

			test("Should succeed", employeeAuth) {
				prepare(accept)
			}

			test("The status should be updated", employeeAuth) {
				val record = prepare(accept)

				record.now() shouldSucceedAnd {
					it.status shouldBe Record.Status.Step(1)
				}
			}

			test("The acceptance should be added to the history", employeeAuth) {
				val record = prepare(accept)

				record.now() shouldSucceedAndSoftly {
					it.historySorted shouldHaveSize 2
				}
			}

			val acceptDiff by prepared(employeeAuth) {
				val record = prepare(accept)

				record.now().orThrow()
					.historySorted.last()
			}

			test("The acceptance's type should be an acceptance", employeeAuth) {
				val diff = prepare(acceptDiff)

				diff::class shouldBe Record.Diff.Accept::class
			}

			test("The acceptance's author should be correct", employeeAuth) {
				val diff = prepare(acceptDiff)

				diff.author?.id shouldBe employeeAuth.user!!.id
			}

			test("The acceptance's date should be correct", employeeAuth) {
				val before = currentInstant
				advanceTimeBy(10)

				val diff = prepare(acceptDiff)

				advanceTimeBy(10)
				val after = currentInstant

				assertSoftly {
					diff.at shouldBeGreaterThan before
					diff.at shouldBeLessThan after
				}
			}

			test("The reason should be unknown", employeeAuth) {
				val diff = prepare(acceptDiff)

				diff.reason shouldBe null
			}

			test("The submission should be correct", employeeAuth) {
				val diff = prepare(acceptDiff)

				diff.submission shouldNotBe null
			}

			test("The previous and next steps should be correct", employeeAuth) {
				val diff = prepare(acceptDiff)

				assertSoftly {
					diff.source shouldBe Record.Status.Step(0)
					diff.target shouldBe Record.Status.Step(1)
				}
			}
		}

		suite("Refuse a record") {
			val refuse by prepared(employeeAuth) {
				val record = prepare(testRecord)
				val records = prepare(testRecords)

				records.refuse(
					record,
					"I don't like it",
				).orThrow()

				record
			}

			test("Should succeed", employeeAuth) {
				prepare(refuse)
			}

			test("The status should be updated", employeeAuth) {
				val record = prepare(refuse)

				record.now() shouldSucceedAnd {
					it.status shouldBe Record.Status.Refused
				}
			}

			test("The refusal should be added to the history", employeeAuth) {
				val record = prepare(refuse)

				record.now() shouldSucceedAndSoftly {
					it.historySorted shouldHaveSize 2
				}
			}

			val refuseDiff by prepared(employeeAuth) {
				val record = prepare(refuse)

				record.now().orThrow()
					.historySorted.last()
			}

			test("The refusal's type should be correct", employeeAuth) {
				val diff = prepare(refuseDiff)

				diff::class shouldBe Record.Diff.Refuse::class
			}

			test("The refusal's author should be correct", employeeAuth) {
				val diff = prepare(refuseDiff)

				diff.author?.id shouldBe employeeAuth.user!!.id
			}

			test("The refusal's date should be correct", employeeAuth) {
				val before = currentInstant
				advanceTimeBy(10)

				val diff = prepare(refuseDiff)

				advanceTimeBy(10)
				val after = currentInstant

				assertSoftly {
					diff.at shouldBeGreaterThan before
					diff.at shouldBeLessThan after
				}
			}

			test("The reason should be known", employeeAuth) {
				val diff = prepare(refuseDiff)

				diff.reason shouldBe "I don't like it"
			}

			test("There should be no submission", employeeAuth) {
				val diff = prepare(refuseDiff)

				diff.submission shouldBe null
			}

			test("The previous and next steps should be correct", employeeAuth) {
				val diff = prepare(refuseDiff)

				assertSoftly {
					diff.source shouldBe Record.Status.Step(0)
					diff.target shouldBe Record.Status.Refused
				}
			}
		}

		suite("Save additional data") {
			val editCurrent by prepared(employeeAuth) {
				val record = prepare(testRecord)
				val records = prepare(testRecords)

				records.editCurrent(
					record,
					null,
					"" to "0",
				).orThrow()

				record
			}

			test("Should succeed", employeeAuth) {
				prepare(editCurrent)
			}

			test("The status should be updated", employeeAuth) {
				val record = prepare(editCurrent)

				record.now() shouldSucceedAnd {
					it.status shouldBe Record.Status.Step(0)
				}
			}

			test("The edition should be added to the history", employeeAuth) {
				val record = prepare(editCurrent)

				record.now() shouldSucceedAndSoftly {
					it.historySorted shouldHaveSize 2
				}
			}

			val editCurrentDiff by prepared(employeeAuth) {
				val record = prepare(editCurrent)

				record.now().orThrow()
					.historySorted.last()
			}

			test("The edition's type should be correct", employeeAuth) {
				val diff = prepare(editCurrentDiff)

				diff::class shouldBe Record.Diff.EditCurrent::class
			}

			test("The edition's author should be correct", employeeAuth) {
				val diff = prepare(editCurrentDiff)

				diff.author?.id shouldBe employeeAuth.user!!.id
			}

			test("The edition's date should be correct", employeeAuth) {
				val before = currentInstant
				advanceTimeBy(10)

				val diff = prepare(editCurrentDiff)

				advanceTimeBy(10)
				val after = currentInstant

				assertSoftly {
					diff.at shouldBeGreaterThan before
					diff.at shouldBeLessThan after
				}
			}

			test("The reason should be unknown", employeeAuth) {
				val diff = prepare(editCurrentDiff)

				diff.reason shouldBe null
			}

			test("There should be a submission", employeeAuth) {
				val diff = prepare(editCurrentDiff)

				diff.submission shouldNotBe null
			}

			test("The previous and next steps should be correct", employeeAuth) {
				val diff = prepare(editCurrentDiff)

				assertSoftly {
					diff.source shouldBe Record.Status.Step(0)
					diff.target shouldBe Record.Status.Step(0)
				}
			}
		}

		suite("Edit the initial submission") {
			val editInitial by prepared(employeeAuth) {
				val record = prepare(testRecord)
				val records = prepare(testRecords)

				records.editInitial(
					record,
					"I didn't like it",
					"" to "0",
				).orThrow()

				record
			}

			test("Should succeed", employeeAuth) {
				prepare(editInitial)
			}

			test("The status should be updated", employeeAuth) {
				val record = prepare(editInitial)

				record.now() shouldSucceedAnd {
					it.status shouldBe Record.Status.Step(0)
				}
			}

			test("The edition should be added to the history", employeeAuth) {
				val record = prepare(editInitial)

				record.now() shouldSucceedAndSoftly {
					it.historySorted shouldHaveSize 2
				}
			}

			val editInitialDiff by prepared(employeeAuth) {
				val record = prepare(editInitial)

				record.now().orThrow()
					.historySorted.last()
			}

			test("The edition's type should be correct", employeeAuth) {
				val diff = prepare(editInitialDiff)

				diff::class shouldBe Record.Diff.EditInitial::class
			}

			test("The edition's author should be correct", employeeAuth) {
				val diff = prepare(editInitialDiff)

				diff.author?.id shouldBe employeeAuth.user!!.id
			}

			test("The edition's date should be correct", employeeAuth) {
				val before = currentInstant
				advanceTimeBy(10)

				val diff = prepare(editInitialDiff)

				advanceTimeBy(10)
				val after = currentInstant

				assertSoftly {
					diff.at shouldBeGreaterThan before
					diff.at shouldBeLessThan after
				}
			}

			test("The reason should be known", employeeAuth) {
				val diff = prepare(editInitialDiff)

				diff.reason shouldBe "I didn't like it"
			}

			test("There should be a submission", employeeAuth) {
				val diff = prepare(editInitialDiff)

				diff.submission shouldNotBe null
			}

			test("The previous and next steps should be correct", employeeAuth) {
				val diff = prepare(editInitialDiff)

				assertSoftly {
					diff.source shouldBe Record.Status.Step(0)
					diff.target shouldBe Record.Status.Step(0)
				}
			}
		}

		suite("Move back") {
			val moveBack by prepared(employeeAuth) {
				val record = prepare(testRecord)
				val records = prepare(testRecords)

				records.accept(
					record,
					null,
					"" to "0",
				).orThrow()

				records.moveBack(
					record,
					0,
					"I didn't like it",
				).orThrow()

				record
			}

			test("Should succeed", employeeAuth) {
				prepare(moveBack)
			}

			test("The status should be updated", employeeAuth) {
				val record = prepare(moveBack)

				record.now() shouldSucceedAnd {
					it.status shouldBe Record.Status.Step(0)
				}
			}

			test("The acceptance & the move should be added to the history", employeeAuth) {
				val record = prepare(moveBack)

				record.now() shouldSucceedAndSoftly {
					it.historySorted shouldHaveSize 3
				}
			}

			val moveBackDiff by prepared(employeeAuth) {
				val record = prepare(moveBack)

				record.now().orThrow()
					.historySorted.last()
			}

			test("The move's type should be correct", employeeAuth) {
				val diff = prepare(moveBackDiff)

				diff::class shouldBe Record.Diff.MoveBack::class
			}

			test("The move's author should be correct", employeeAuth) {
				val diff = prepare(moveBackDiff)

				diff.author?.id shouldBe employeeAuth.user!!.id
			}

			test("The move's date should be correct", employeeAuth) {
				val before = currentInstant
				advanceTimeBy(10)

				val diff = prepare(moveBackDiff)

				advanceTimeBy(10)
				val after = currentInstant

				assertSoftly {
					diff.at shouldBeGreaterThan before
					diff.at shouldBeLessThan after
				}
			}

			test("The reason should be known", employeeAuth) {
				val diff = prepare(moveBackDiff)

				diff.reason shouldBe "I didn't like it"
			}

			test("There should not be a submission", employeeAuth) {
				val diff = prepare(moveBackDiff)

				diff.submission shouldBe null
			}

			test("The previous and next steps should be correct", employeeAuth) {
				val diff = prepare(moveBackDiff)

				assertSoftly {
					diff.source shouldBe Record.Status.Step(1)
					diff.target shouldBe Record.Status.Step(0)
				}
			}
		}
	}
}
