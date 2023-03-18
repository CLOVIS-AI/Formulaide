package opensavvy.formulaide.mongo

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import opensavvy.formulaide.core.Field
import opensavvy.formulaide.core.Input
import opensavvy.formulaide.mongo.FieldDbDto.Companion.toCore
import opensavvy.formulaide.mongo.FieldDbDto.Companion.toDto
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class FieldDbTest {

    @Test
    fun `label conversion`() = runTest {
        val field = Field.label("Label")
        assertEquals(field, field.toDto().toCore { _, _ -> null })
    }

    @Test
    fun `arity conversion`() = runTest {
        val field = Field.arity("Arity", 0u..2u, Field.label("Label"))
        assertEquals(field, field.toDto().toCore { _, _ -> null })
    }

    @Test
    fun `choice conversion`() = runTest {
        val field = Field.choice(
            "Choice",
            0 to Field.label("Label 1"),
            1 to Field.label("Label 2"),
        )
        assertEquals(field, field.toDto().toCore { _, _ -> null })
    }

    @Test
    fun `group conversion`() = runTest {
        val field = Field.group(
            "Group",
            0 to Field.label("Label 1"),
            1 to Field.label("Label 2"),
        )
        assertEquals(field, field.toDto().toCore { _, _ -> null })
    }

    @Test
    fun `input conversion`() = runTest {
        val field = Field.input(
            "Input",
            Input.Text(),
        )
        assertEquals(field, field.toDto().toCore { _, _ -> null })
    }


}
