package opensavvy.formulaide.core

import arrow.core.NonEmptyList
import kotlinx.coroutines.withContext
import opensavvy.backbone.now
import opensavvy.formulaide.core.Field.Companion.arity
import opensavvy.formulaide.core.Field.Companion.arityFrom
import opensavvy.formulaide.core.Field.Companion.choice
import opensavvy.formulaide.core.Field.Companion.choiceFrom
import opensavvy.formulaide.core.Field.Companion.group
import opensavvy.formulaide.core.Field.Companion.groupFrom
import opensavvy.formulaide.core.Field.Companion.input
import opensavvy.formulaide.core.Field.Companion.inputFrom
import opensavvy.formulaide.core.Field.Companion.label
import opensavvy.formulaide.core.Field.Companion.labelFrom
import opensavvy.formulaide.core.Input.*
import opensavvy.formulaide.fake.FakeTemplates
import opensavvy.formulaide.test.assertions.shouldFailWithType
import opensavvy.formulaide.test.assertions.shouldSucceed
import opensavvy.formulaide.test.structure.Suite
import opensavvy.formulaide.test.structure.TestExecutor
import opensavvy.formulaide.test.structure.TestScope
import opensavvy.formulaide.test.structure.clock
import opensavvy.formulaide.test.utils.TestUsers.administratorAuth
import opensavvy.formulaide.test.utils.TestUsers.employeeAuth
import opensavvy.state.arrow.out
import opensavvy.state.outcome.failed
import opensavvy.state.outcome.success

class ImportedFieldTest : TestExecutor() {

	private data class CombinedFieldFailure(
		val failure: NonEmptyList<Field.Failures.Compatibility>,
	)

	private suspend fun TestScope.createTemplateForField(service: Template.Service, field: Field) =
		withContext(administratorAuth) {
			service.create(
				name = "Test",
				initialVersionTitle = "Initial version",
				field = field,
			).bind()
				.now().bind()
				.versions.first()
		}

	private suspend fun TestScope.import(field: Field, imported: suspend (Template.Version.Ref) -> Field) = out {
		val templates = FakeTemplates(clock)

		val template = createTemplateForField(templates, field)

		val new = imported(template)

		new.validate().fold(
			ifLeft = { CombinedFieldFailure(it).failed() },
			ifRight = { it.success() },
		).bind()
	}

	override fun Suite.register() {
		test("Identical label", employeeAuth) {
			shouldSucceed(import(
				label("Label")
			) {
				labelFrom(it, "Label")
			})
		}

		test("Label with a different name", employeeAuth) {
			shouldSucceed(import(
				label("Label")
			) {
				labelFrom(it, "Other name")
			})
		}

		test("Identical test input", employeeAuth) {
			shouldSucceed(import(
				input("Name", Input.text().bind())
			) {
				inputFrom(it, "Name", Input.text().bind())
			})
		}

		test("Text input with a different name", employeeAuth) {
			shouldSucceed(import(
				input("Name", Input.text().bind())
			) {
				inputFrom(it, "Other name", Input.text().bind())
			})
		}

		test("Text input with a greater maximum length", employeeAuth) {
			import(
				input("Name", Text(maxLength = 5u))
			) {
				inputFrom(it, "Name", Text(maxLength = 10u))
			} shouldFailWithType CombinedFieldFailure::class
		}

		test("Text input with lesser maximum length", employeeAuth) {
			shouldSucceed(import(
				input("Name", Text(maxLength = 5u))
			) {
				inputFrom(it, "Name", Text(maxLength = 4u))
			})
		}

		test("Identical int input", employeeAuth) {
			shouldSucceed(import(
				input("Name", Input.integer().bind())
			) {
				inputFrom(it, "Name", Input.integer().bind())
			})
		}

		test("Int input with a different name", employeeAuth) {
			shouldSucceed(import(
				input("Name", Input.integer().bind())
			) {
				inputFrom(it, "Other name", Input.integer().bind())
			})
		}

		test("Int input with a more restrictive range", employeeAuth) {
			shouldSucceed(import(
				input("Name", Integer(0..5))
			) {
				inputFrom(it, "Name", Integer(1..2))
			})

			shouldSucceed(import(
				input("Name", Integer(0..5))
			) {
				inputFrom(it, "Name", Integer(0..4))
			})

			shouldSucceed(import(
				input("Name", Integer(0..5))
			) {
				inputFrom(it, "Name", Integer(2..5))
			})
		}

		test("Int input with a less restrictive minimum", employeeAuth) {
			import(
				input("Name", Integer(0..5))
			) {
				inputFrom(it, "Name", Integer(-1..5))
			} shouldFailWithType CombinedFieldFailure::class

			import(
				input("Name", Integer(0..5))
			) {
				inputFrom(it, "Name", Integer(-1..3))
			} shouldFailWithType CombinedFieldFailure::class
		}

		test("Int input with a less restrictive maximum", employeeAuth) {
			import(
				input("Name", Integer(0..5))
			) {
				inputFrom(it, "Name", Integer(0..6))
			} shouldFailWithType CombinedFieldFailure::class

			import(
				input("Name", Integer(0..5))
			) {
				inputFrom(it, "Name", Integer(3..6))
			} shouldFailWithType CombinedFieldFailure::class
		}

		test("Identical toggle input", employeeAuth) {
			import(
				input("Name", Toggle)
			) {
				inputFrom(it, "Name", Toggle)
			}.bind()
		}

		test("Toggle input with a different name", employeeAuth) {
			import(
				input("Name", Toggle)
			) {
				inputFrom(it, "Other name", Toggle)
			}.bind()
		}

		test("Identical email input", employeeAuth) {
			import(
				input("Name", Email)
			) {
				inputFrom(it, "Name", Email)
			}.bind()
		}

		test("Email input with a different name", employeeAuth) {
			import(
				input("Name", Email)
			) {
				inputFrom(it, "Other name", Email)
			}.bind()
		}

		test("Identical phone input", employeeAuth) {
			shouldSucceed(import(
				input("Name", Phone)
			) {
				inputFrom(it, "Name", Phone)
			})
		}

		test("Phone input with a different name", employeeAuth) {
			shouldSucceed(import(
				input("Name", Phone)
			) {
				inputFrom(it, "Other name", Phone)
			})
		}

		test("Identical date input", employeeAuth) {
			shouldSucceed(import(
				input("Name", Date)
			) {
				inputFrom(it, "Name", Date)
			})
		}

		test("Date input with a different name", employeeAuth) {
			shouldSucceed(import(
				input("Name", Date)
			) {
				inputFrom(it, "Other name", Date)
			})
		}

		test("Identical time input", employeeAuth) {
			shouldSucceed(import(
				input("Name", Time)
			) {
				inputFrom(it, "Name", Time)
			})
		}

		test("Time input with a different name", employeeAuth) {
			shouldSucceed(import(
				input("Name", Time)
			) {
				inputFrom(it, "Other name", Time)
			})
		}

		test("Cannot import a different type of input", employeeAuth) {
			val types = listOf(
				Input.text().bind(),
				Input.integer().bind(),
				Toggle,
				Email,
				Phone,
				Date,
				Time,
			)

			for (original in types) {
				for (imported in types.filter { it != original }) {
					println("Checking that it is not possible to import $imported for the original input $original")
					import(
						input("Name", original)
					) {
						inputFrom(it, "Name", imported)
					} shouldFailWithType CombinedFieldFailure::class
				}
			}
		}

		test("Identical choice", employeeAuth) {
			shouldSucceed(import(
				choice(
					"Name",
					0 to label("First choice"),
					1 to label("Second choice"),
					2 to label("Third choice"),
				)
			) {
				choiceFrom(
					it,
					"Name",
					0 to label("First choice"),
					1 to label("Second choice"),
					2 to label("Third choice"),
				)
			})
		}

		test("Choice with a different name", employeeAuth) {
			shouldSucceed(import(
				choice(
					"Name",
					0 to label("First choice"),
					1 to label("Second choice"),
					2 to label("Third choice"),
				)
			) {
				choiceFrom(
					it,
					"Other name",
					0 to label("First choice renamed"),
					1 to label("Second choice renamed"),
					2 to label("Third choice renamed"),
				)
			})
		}

		test("Choice with less options", employeeAuth) {
			shouldSucceed(import(
				choice(
					"Name",
					0 to label("First choice"),
					1 to label("Second choice"),
					2 to label("Third choice"),
				)
			) {
				choiceFrom(
					it,
					"Name",
					0 to label("First choice"),
					2 to label("Third choice"),
				)
			})
		}

		test("Choice with more options", employeeAuth) {
			import(
				choice(
					"Name",
					0 to label("First choice"),
					1 to label("Second choice"),
					2 to label("Third choice"),
				)
			) {
				choiceFrom(
					it,
					"Name",
					0 to label("First choice"),
					1 to label("Second choice"),
					2 to label("Third choice"),
					3 to label("Fourth choice"),
				)
			} shouldFailWithType CombinedFieldFailure::class
		}

		test("Identical group", employeeAuth) {
			shouldSucceed(import(
				group(
					"Name",
					0 to label("First field"),
					1 to label("Second field"),
					2 to label("Third field"),
				)
			) {
				groupFrom(
					it,
					"Name",
					0 to label("First field"),
					1 to label("Second field"),
					2 to label("Third field"),
				)
			})
		}

		test("Group with a different name", employeeAuth) {
			shouldSucceed(import(
				group(
					"Name",
					0 to label("First field"),
					1 to label("Second field"),
					2 to label("Third field"),
				)
			) {
				groupFrom(
					it,
					"Other name",
					0 to label("First field renamed"),
					1 to label("Second field renamed"),
					2 to label("Third field renamed"),
				)
			})
		}

		test("Group with different fields", employeeAuth) {
			import(
				group(
					"Name",
					0 to label("First field"),
					1 to label("Second field"),
					2 to label("Third field"),
				)
			) {
				groupFrom(
					it,
					"Name",
					0 to label("First field"),
					1 to label("Second field"),
				)
			} shouldFailWithType CombinedFieldFailure::class

			import(
				group(
					"Name",
					0 to label("First field"),
					1 to label("Second field"),
					2 to label("Third field"),
				)
			) {
				groupFrom(
					it,
					"Name",
					0 to label("First field"),
					1 to label("Second field"),
					2 to label("Third field"),
					3 to label("Fourth field"),
				)
			} shouldFailWithType CombinedFieldFailure::class
		}

		test("Identical arity", employeeAuth) {
			shouldSucceed(import(
				arity(
					"Name",
					1u..5u,
					label("Field"),
				)
			) {
				arityFrom(
					it,
					"Name",
					1u..5u,
					label("Field"),
				)
			})
		}

		test("Arity with a different name", employeeAuth) {
			shouldSucceed(import(
				arity(
					"Name",
					1u..5u,
					label("Field"),
				)
			) {
				arityFrom(
					it,
					"Other name",
					1u..5u,
					label("Field"),
				)
			})
		}

		test("Import optional arity as mandatory", employeeAuth) {
			shouldSucceed(import(
				arity(
					"Name",
					0u..1u,
					label("Field"),
				)
			) {
				arityFrom(
					it,
					"Name",
					1u..1u,
					label("Field"),
				)
			})
		}

		test("Import mandatory arity as optional", employeeAuth) {
			import(
				arity(
					"Name",
					1u..1u,
					label("Field"),
				)
			) {
				arityFrom(
					it,
					"Name",
					0u..1u,
					label("Field"),
				)
			} shouldFailWithType CombinedFieldFailure::class
		}

		test("Arity with a lower minimum bound", employeeAuth) {
			import(
				arity(
					"Name",
					5u..6u,
					label("Field"),
				)
			) {
				arityFrom(
					it,
					"Name",
					4u..6u,
					label("Field"),
				)
			} shouldFailWithType CombinedFieldFailure::class
		}

		test("Arity with a higher minimum bound", employeeAuth) {
			shouldSucceed(import(
				arity(
					"Name",
					5u..6u,
					label("Field"),
				)
			) {
				arityFrom(
					it,
					"Name",
					6u..6u,
					label("Field"),
				)
			})
		}

		test("Arity with a higher maximum bound", employeeAuth) {
			import(
				arity(
					"Name",
					5u..6u,
					label("Field"),
				)
			) {
				arityFrom(
					it,
					"Name",
					5u..7u,
					label("Field"),
				)
			} shouldFailWithType CombinedFieldFailure::class
		}

		test("Arity with a lower maximum bound", employeeAuth) {
			shouldSucceed(import(
				arity(
					"Name",
					5u..6u,
					label("Field"),
				)
			) {
				arityFrom(
					it,
					"Name",
					5u..5u,
					label("Field"),
				)
			})
		}
	}
}
