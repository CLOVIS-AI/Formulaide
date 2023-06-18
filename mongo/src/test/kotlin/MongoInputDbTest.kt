package opensavvy.formulaide.mongo

import io.kotest.matchers.shouldBe
import opensavvy.formulaide.core.Input
import opensavvy.formulaide.core.integer
import opensavvy.formulaide.core.text
import opensavvy.formulaide.mongo.MongoInputDto.Companion.toCore
import opensavvy.formulaide.mongo.MongoInputDto.Companion.toDto
import opensavvy.formulaide.test.structure.Suite
import opensavvy.formulaide.test.structure.TestExecutor

class MongoInputDbTest : TestExecutor() {

    override fun Suite.register() {
        test("Base text") {
            val input = Input.text().bind()
            input.toDto().toCore() shouldBe input
        }

        test("Text with maximum length") {
            val input = Input.text(maxLength = 5u).bind()
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
