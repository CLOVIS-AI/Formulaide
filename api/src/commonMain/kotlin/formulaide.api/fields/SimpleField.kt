package formulaide.api.fields

import formulaide.api.types.Arity
import kotlinx.serialization.Serializable

@Serializable
sealed class SimpleField {
	abstract val name: String
	abstract val arity: Arity

	@Serializable
	data class Text(
		override val name: String,
		override val arity: Arity,
		val maxLength: Int? = null,
	) : SimpleField()

	@Serializable
	data class Integer(
		override val name: String,
		override val arity: Arity,
		val min: Int? = null,
		val max: Int? = null,
	) : SimpleField()

	@Serializable
	data class Message(
		override val name: String,
	) : SimpleField() {
		override val arity get() = Arity.mandatory()
	}

}
