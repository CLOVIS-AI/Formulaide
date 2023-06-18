package opensavvy.formulaide.remote.dto

import io.kotest.matchers.shouldBe
import opensavvy.formulaide.core.Input
import opensavvy.formulaide.core.integer
import opensavvy.formulaide.core.text
import opensavvy.formulaide.remote.dto.InputDto.Companion.toCore
import opensavvy.formulaide.remote.dto.InputDto.Companion.toDto
import opensavvy.formulaide.test.structure.Suite
import opensavvy.formulaide.test.structure.TestExecutor

class InputDtoTest : TestExecutor() {

	override fun Suite.register() {
		test("Base text") {
			val input = Input.text().bind()
			input.toDto().toCore() shouldBe input
		}

		test("Text with maximum length") {
			val input = Input.Text(maxLength = 5u)
			input.toDto().toCore() shouldBe input
		}

		test("Base integer") {
			val input = Input.integer().bind()
			input.toDto().toCore() shouldBe input
		}

		test("Integer with range") {
			val input = Input.integer(min = 2, max = 3).bind()
			input.toDto().toCore() shouldBe input
		}

		test("Toggle") {
			val input = Input.Toggle
			input.toDto().toCore() shouldBe input
		}

		test("Email") {
			val input = Input.Email
			input.toDto().toCore() shouldBe input
		}

		test("Phone") {
			val input = Input.Phone
			input.toDto().toCore() shouldBe input
		}

		test("Date") {
			val input = Input.Date
			input.toDto().toCore() shouldBe input
		}

		test("Time") {
			val input = Input.Time
			input.toDto().toCore() shouldBe input
		}
	}
}
