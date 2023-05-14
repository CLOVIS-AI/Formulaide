package opensavvy.formulaide.mongo

import io.kotest.matchers.shouldBe
import opensavvy.formulaide.core.Field
import opensavvy.formulaide.core.Input
import opensavvy.formulaide.core.text
import opensavvy.formulaide.fake.FakeTemplates
import opensavvy.formulaide.mongo.FieldDbDto.Companion.toCore
import opensavvy.formulaide.mongo.FieldDbDto.Companion.toDto
import opensavvy.formulaide.test.structure.Suite
import opensavvy.formulaide.test.structure.TestExecutor
import opensavvy.formulaide.test.structure.clock

class FieldDbTest : TestExecutor() {

    override fun Suite.register() {
        test("Label conversion") {
            val field = Field.label("Label")
            field.toDto().toCore(FakeTemplates(clock)) shouldBe field
        }

        test("Arity conversion") {
            val field = Field.arity("Arity", 0u..2u, Field.label("Label"))
            field.toDto().toCore(FakeTemplates(clock)) shouldBe field
        }

        test("Choice conversion") {
            val field = Field.choice(
                "Choice",
                0 to Field.label("Label 1"),
                1 to Field.label("Label 2"),
            )
            field.toDto().toCore(FakeTemplates(clock)) shouldBe field
        }

        test("Group conversion") {
            val field = Field.group(
                "Group",
                0 to Field.label("Label 1"),
                1 to Field.label("Label 2"),
            )
            field.toDto().toCore(FakeTemplates(clock)) shouldBe field
        }

        test("Input conversion") {
            val field = Field.input(
                "Input",
                Input.text().bind(),
            )
            field.toDto().toCore(FakeTemplates(clock)) shouldBe field
        }
    }
}
