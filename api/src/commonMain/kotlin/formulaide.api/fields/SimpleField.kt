package formulaide.api.fields

import formulaide.api.fields.SimpleField.Message.arity
import formulaide.api.types.Arity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A field that represents some specific data.
 *
 * This class does not implement [Field], because it is meant to be wrapped inside a [Field] implementation:
 * see [DataField.Simple], [FormField.Simple] and [DeepFormField.Simple].
 * For this reason, this class doesn't have an [id][Field.id] nor a [name][Field.name].
 */
@Serializable
sealed class SimpleField {
	abstract val arity: Arity

	/**
	 * The user should input some text.
	 * @property maxLength The maximum number of characters allowed (`null` means 'no limit').
	 */
	@SerialName("TEXT")
	data class Text(
		override val arity: Arity,
		val maxLength: Int? = null,
	) : SimpleField()

	/**
	 * The user should input an integer.
	 * @property min The minimum value of that integer (`null` means 'no minimum').
	 * @property max The maximum value of that integer (`null` means 'no maximum').
	 */
	@SerialName("INTEGER")
	data class Integer(
		override val arity: Arity,
		val min: Int? = null,
		val max: Int? = null,
	) : SimpleField()

	/**
	 * The user should input a decimal number.
	 */
	@SerialName("INTEGER")
	data class Decimal(
		override val arity: Arity,
	) : SimpleField()

	/**
	 * The user should check a box.
	 */
	@SerialName("INTEGER")
	data class Boolean(
		override val arity: Arity,
	) : SimpleField()

	/**
	 * A message should be displayed to the user, but they shouldn't have anything to fill in.
	 * The [arity] is always [Arity.mandatory].
	 */
	@SerialName("MESSAGE")
	object Message: SimpleField() {
		override val arity get() = Arity.mandatory()
	}

}
