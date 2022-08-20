package formulaide.core.field

import formulaide.core.field.Field.Id.Companion.idOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class FieldTest {

	@Test
	fun basic() {
		val first = label("First field")
		val second = label("Second field")

		choice("Choice test", 0 to first, 1 to second)
		group("Group test", 0 to first, 1 to second)
		list("Optional test", 0u..1u, first)
	}

	@Test
	fun emptyName() {
		assertFails {
			label("")
		}

		assertFails {
			label("       ")
		}
	}

	@Test
	fun wrongSource() {
		val first = label("First field")
		val second = label("Second field")
		val original = group("Source", 0 to first, 1 to second)
		val container = Field.Container("0", original)

		groupFrom(idOf(), container, 0 to labelFrom(idOf(0), container), 1 to labelFrom(idOf(1), container))

		assertFails {
			groupFrom(idOf(), container, 0 to labelFrom(idOf(0), container))
		}

		assertFails {
			groupFrom(idOf(), container, 2 to labelFrom(idOf(0), container))
		}

		assertFails {
			groupFrom(idOf(), container, 0 to labelFrom(idOf(1), container), 1 to labelFrom(idOf(0), container))
		}
	}

	@Test
	fun id() {
		val first = label("Label")
		val second = choice("Choice", 0 to first)
		val third = group("Group", 0 to second)
		val fourth = list("List", 1u..1u, third)

		val fields = Field.Container("0", fourth)

		assertEquals(fourth, fields[idOf()])
		assertEquals(third, fields[idOf(0)])
		assertEquals(second, fields[idOf(0, 0)])
		assertEquals(first, fields[idOf(0, 0, 0)])
	}

	@Test
	fun inputCompatibility() {
		val text = input("Text", InputConstraints.Text(maxLength = 10u))
		val int = input("Int", InputConstraints.Integer(min = -10, max = 10))
		val boolean = input("Boolean", InputConstraints.Boolean)

		val fields = Field.Container(
			"0", group(
				"Group",
				0 to text,
				1 to int,
				2 to boolean,
			)
		)

		inputFrom(idOf(0), fields)
		inputFrom(idOf(0), fields, input = InputConstraints.Text(9u))
		inputFrom(idOf(0), fields, input = InputConstraints.Text(10u))

		assertFails {
			inputFrom(idOf(0), fields, input = InputConstraints.Boolean)
		}

		assertFails {
			inputFrom(idOf(0), fields, input = InputConstraints.Text(11u))
		}

		inputFrom(idOf(1), fields, input = InputConstraints.Integer(-10, 10))
		inputFrom(idOf(1), fields, input = InputConstraints.Integer(-9, 10))
		inputFrom(idOf(1), fields, input = InputConstraints.Integer(-10, 9))

		assertFails {
			inputFrom(idOf(1), fields, input = InputConstraints.Integer(-11, 10))
		}

		assertFails {
			inputFrom(idOf(1), fields, input = InputConstraints.Integer(-10, 11))
		}

		assertFails {
			inputFrom(idOf(1), fields, input = InputConstraints.Integer())
		}
	}
}
