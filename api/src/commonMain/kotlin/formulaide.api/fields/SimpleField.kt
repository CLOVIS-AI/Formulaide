package formulaide.api.fields

import formulaide.api.types.Arity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class SimpleField {
	abstract val name: String
	abstract val arity: Arity

	@SerialName("TEXT")
	data class Text(
		override val name: String,
		override val arity: Arity,
		val maxLength: Int? = null,
	) : SimpleField()

	@SerialName("INTEGER")
	data class Integer(
		override val name: String,
		override val arity: Arity,
		val min: Int? = null,
		val max: Int? = null,
	) : SimpleField()

	@SerialName("MESSAGE")
	data class Message(
		override val name: String,
	) : SimpleField() {
		override val arity get() = Arity.mandatory()
	}

}
