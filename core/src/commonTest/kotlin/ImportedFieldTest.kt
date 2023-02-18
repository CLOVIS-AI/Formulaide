package opensavvy.formulaide.core

import arrow.core.flatMap
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import opensavvy.backbone.Ref.Companion.now
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
import opensavvy.formulaide.test.assertions.shouldBeInvalid
import opensavvy.formulaide.test.assertions.shouldSucceed
import opensavvy.formulaide.test.utils.TestClock.Companion.testClock
import opensavvy.formulaide.test.utils.TestUsers.administratorAuth
import opensavvy.formulaide.test.utils.TestUsers.employeeAuth
import opensavvy.state.outcome.orThrow
import opensavvy.state.outcome.out
import kotlin.js.JsName
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ImportedFieldTest {

	private suspend fun Template.Service.of(field: Field) = withContext(administratorAuth) {
		create(
			name = "Test",
			initialVersionTitle = "Initial version",
			field = field,
		).flatMap { it.now() }
			.map { it.versions.first() }
			.orThrow()
	}

	private suspend fun import(field: Field, imported: suspend (Template.Version.Ref) -> Field) = out {
		val clock = testClock()
		val templates = FakeTemplates(clock)

		val template = templates.of(field)

		val new = imported(template)

		new.validate().bind()
	}

	@Test
	@JsName("importedLabelIdentical")
	fun `identical label`() = runTest(employeeAuth) {
		shouldSucceed(import(
			label("Label")
		) {
			labelFrom(it, "Label")
		})
	}

	@Test
	@JsName("importedLabelRenamed")
	fun `label with a different name`() = runTest(employeeAuth) {
		shouldSucceed(import(
			label("Label")
		) {
			labelFrom(it, "Other name")
		})
	}

	@Test
	@JsName("importedInputTextIdentical")
	fun `identical text input`() = runTest(employeeAuth) {
		shouldSucceed(import(
			input("Name", Text())
		) {
			inputFrom(it, "Name", Text())
		})
	}

	@Test
	@JsName("importedInputTextRenamed")
	fun `text input with a different name`() = runTest(employeeAuth) {
		shouldSucceed(import(
			input("Name", Text())
		) {
			inputFrom(it, "Other name", Text())
		})
	}

	@Test
	@JsName("importedInputTextGreaterMaxLength")
	fun `text input with greater maximum length`() = runTest(employeeAuth) {
		shouldBeInvalid(
			import(
				input("Name", Text(maxLength = 5u))
			) {
				inputFrom(it, "Name", Text(maxLength = 10u))
			}
		)
	}

	@Test
	@JsName("importedInputTextLesserMaxLength")
	fun `text input with lesser maximum length`() = runTest(employeeAuth) {
		shouldSucceed(import(
			input("Name", Text(maxLength = 5u))
		) {
			inputFrom(it, "Name", Text(maxLength = 4u))
		})
	}

	@Test
	@JsName("importedInputIntIdentical")
	fun `identical int input`() = runTest(employeeAuth) {
		shouldSucceed(import(
			input("Name", Integer())
		) {
			inputFrom(it, "Name", Integer())
		})
	}

	@Test
	@JsName("importedInputIntRenamed")
	fun `int input with a different name`() = runTest(employeeAuth) {
		shouldSucceed(import(
			input("Name", Integer())
		) {
			inputFrom(it, "Other name", Integer())
		})
	}

	@Test
	@JsName("importedInputIntMoreRestrictiveRange")
	fun `int input with more restrictive range`() = runTest(employeeAuth) {
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

	@Test
	@JsName("importedInputIntLessRestrictiveMin")
	fun `int input with less restrictive minimum`() = runTest(employeeAuth) {
		shouldBeInvalid(import(
			input("Name", Integer(0..5))
		) {
			inputFrom(it, "Name", Integer(-1..5))
		})

		shouldBeInvalid(import(
			input("Name", Integer(0..5))
		) {
			inputFrom(it, "Name", Integer(-1..3))
		})
	}

	@Test
	@JsName("importedInputIntLessRestrictiveMax")
	fun `int input with less restrictive maximum`() = runTest(employeeAuth) {
		shouldBeInvalid(import(
			input("Name", Integer(0..5))
		) {
			inputFrom(it, "Name", Integer(0..6))
		})

		shouldBeInvalid(import(
			input("Name", Integer(0..5))
		) {
			inputFrom(it, "Name", Integer(3..6))
		})
	}

	@Test
	@JsName("importedInputToggleIdentical")
	fun `identical toggle input`() = runTest(employeeAuth) {
		shouldSucceed(import(
			input("Name", Toggle)
		) {
			inputFrom(it, "Name", Toggle)
		})
	}

	@Test
	@JsName("importedInputToggleRenamed")
	fun `toggle input with a different name`() = runTest(employeeAuth) {
		shouldSucceed(import(
			input("Name", Toggle)
		) {
			inputFrom(it, "Other name", Toggle)
		})
	}

	@Test
	@JsName("importedInputEmailIdentical")
	fun `identical email input`() = runTest(employeeAuth) {
		shouldSucceed(import(
			input("Name", Email)
		) {
			inputFrom(it, "Name", Email)
		})
	}

	@Test
	@JsName("importedInputEmailRenamed")
	fun `email input with a different name`() = runTest(employeeAuth) {
		shouldSucceed(import(
			input("Name", Email)
		) {
			inputFrom(it, "Other name", Email)
		})
	}

	@Test
	@JsName("importedInputPhoneIdentical")
	fun `identical phone input`() = runTest(employeeAuth) {
		shouldSucceed(import(
			input("Name", Phone)
		) {
			inputFrom(it, "Name", Phone)
		})
	}

	@Test
	@JsName("importedInputPhoneRenamed")
	fun `phone input with a different name`() = runTest(employeeAuth) {
		shouldSucceed(import(
			input("Name", Phone)
		) {
			inputFrom(it, "Other name", Phone)
		})
	}

	@Test
	@JsName("importedInputDateIdentical")
	fun `identical date input`() = runTest(employeeAuth) {
		shouldSucceed(import(
			input("Name", Date)
		) {
			inputFrom(it, "Name", Date)
		})
	}

	@Test
	@JsName("importedInputDateRenamed")
	fun `date input with a different name`() = runTest(employeeAuth) {
		shouldSucceed(import(
			input("Name", Date)
		) {
			inputFrom(it, "Other name", Date)
		})
	}

	@Test
	@JsName("importedInputTimeIdentical")
	fun `identical time input`() = runTest(employeeAuth) {
		shouldSucceed(import(
			input("Name", Time)
		) {
			inputFrom(it, "Name", Time)
		})
	}

	@Test
	@JsName("importedInputTimeRenamed")
	fun `time input with a different name`() = runTest(employeeAuth) {
		shouldSucceed(import(
			input("Name", Time)
		) {
			inputFrom(it, "Other name", Time)
		})
	}

	@Test
	@JsName("cannotImportDifferentType")
	fun `cannot import a different type of input`() = runTest(employeeAuth) {
		val types = listOf(
			Text(),
			Integer(),
			Toggle,
			Email,
			Phone,
			Date,
			Time,
		)

		for (original in types) {
			for (imported in types.filter { it != original }) {
				println("Checking that it is not possible to import $imported for the original input $original")
				shouldBeInvalid(import(
					input("Name", original)
				) {
					inputFrom(it, "Name", imported)
				})
			}
		}
	}

	@Test
	@JsName("importedChoiceIdentical")
	fun `identical choice`() = runTest(employeeAuth) {
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

	@Test
	@JsName("importedChoiceRenamed")
	fun `choice with a different name`() = runTest(employeeAuth) {
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

	@Test
	@JsName("importedChoiceLessOptions")
	fun `choice with a less options`() = runTest(employeeAuth) {
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

	@Test
	@JsName("importedChoiceMoreOptions")
	fun `choice with more options`() = runTest(employeeAuth) {
		shouldBeInvalid(import(
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
		})
	}

	@Test
	@JsName("importedGroupIdentical")
	fun `identical group`() = runTest(employeeAuth) {
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

	@Test
	@JsName("importedGroupRenamed")
	fun `group with a different name`() = runTest(employeeAuth) {
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

	@Test
	@JsName("importedGroupDifferentFields")
	fun `group with different fields`() = runTest(employeeAuth) {
		shouldBeInvalid(import(
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
		})

		shouldBeInvalid(import(
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
		})
	}

	@Test
	@JsName("importedArityIdentical")
	fun `identical arity`() = runTest(employeeAuth) {
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

	@Test
	@JsName("importedArityRenamed")
	fun `arity with a different name`() = runTest(employeeAuth) {
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

	@Test
	@JsName("importedArityOptionalToMandatory")
	fun `import optional arity as mandatory`() = runTest(employeeAuth) {
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

	@Test
	@JsName("importedArityMandatoryToOptional")
	fun `import mandatory arity as optional`() = runTest(employeeAuth) {
		shouldBeInvalid(import(
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
		})
	}

	@Test
	@JsName("importedArityLesserMin")
	fun `arity with a lower minimum bound`() = runTest(employeeAuth) {
		shouldBeInvalid(import(
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
		})
	}

	@Test
	@JsName("importedArityGreaterMin")
	fun `arity with a higher minimum bound`() = runTest(employeeAuth) {
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

	@Test
	@JsName("importedArityGreaterMax")
	fun `arity with a higher maximum bound`() = runTest(employeeAuth) {
		shouldBeInvalid(import(
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
		})
	}

	@Test
	@JsName("importedArityLesserMax")
	fun `arity with a lower maximum bound`() = runTest(employeeAuth) {
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
