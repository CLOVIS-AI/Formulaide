package opensavvy.formulaide.remote.dto

import io.kotest.matchers.shouldBe
import opensavvy.formulaide.core.Field.Companion.arity
import opensavvy.formulaide.core.Field.Companion.choice
import opensavvy.formulaide.core.Field.Companion.group
import opensavvy.formulaide.core.Field.Companion.input
import opensavvy.formulaide.core.Field.Companion.label
import opensavvy.formulaide.core.Input
import opensavvy.formulaide.core.Template
import opensavvy.formulaide.core.text
import opensavvy.formulaide.remote.dto.FieldDto.Companion.toCore
import opensavvy.formulaide.remote.dto.FieldDto.Companion.toDto
import opensavvy.formulaide.test.structure.Suite
import opensavvy.formulaide.test.structure.TestExecutor

class FieldDtoTest : TestExecutor() {

	private fun decodeTemplate(): Template.Version.Ref? = null

	override fun Suite.register() {
		test("Label conversion") {
			val field = label("Label")
			field.toDto().toCore { decodeTemplate() } shouldBe field
		}

		test("Arity conversion") {
			val field = arity("Arity", 0u..2u, label("Label"))
			field.toDto().toCore { decodeTemplate() } shouldBe field
		}

		test("Choice conversion") {
			val field = choice(
				"Choice",
				0 to label("Label 1"),
				1 to label("Label 2"),
			)
			field.toDto().toCore { decodeTemplate() } shouldBe field
		}

		test("Group conversion") {
			val field = group(
				"Group",
				0 to label("Label 1"),
				1 to label("Label 2"),
			)
			field.toDto().toCore { decodeTemplate() } shouldBe field
		}

		test("Input conversion") {
			val field = input(
				"Input",
				Input.text().bind(),
			)
			field.toDto().toCore { decodeTemplate() } shouldBe field
		}
	}

}
