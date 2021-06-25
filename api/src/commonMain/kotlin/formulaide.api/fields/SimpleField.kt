package formulaide.api.fields

import formulaide.api.fields.SimpleField.Message.arity
import formulaide.api.types.Arity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A field that represents some specific data.
 *
 * This class does not implement [Field], because it is meant to be wrapped inside a [Field] implementation:
 * see [DataField.Simple], [FormField.Shallow.Simple] and [FormField.Deep.Simple].
 * For this reason, this class doesn't have an [id][Field.id] nor a [name][Field.name].
 */
@Serializable
sealed class SimpleField {
	abstract val arity: Arity

	abstract fun validate(value: String?)

	/**
	 * The user should input some text.
	 * @property maxLength The maximum number of characters allowed (`null` means 'no limit').
	 */
	@SerialName("TEXT")
	data class Text(
		override val arity: Arity,
		val maxLength: Int? = null,
	) : SimpleField() {
		override fun validate(value: String?) {
			requireNotNull(value) { "Un champ de texte doit être rempli : trouvé '$value'" }
			require(value.isNotBlank()) { "Un champ de texte ne peut pas être vide ou contenir uniquement des espaces : trouvé '$value'" }

			if (maxLength != null)
				require(value.length <= maxLength) { "La longueur maximale autorisée est de $maxLength caractères, mais ${value.length} ont été donnés" }
		}
	}

	/**
	 * The user should input an integer (Kotlin's [Long]).
	 * @property min The minimum value of that integer (`null` means 'no minimum').
	 * @property max The maximum value of that integer (`null` means 'no maximum').
	 */
	@SerialName("INTEGER")
	data class Integer(
		override val arity: Arity,
		val min: Long? = null,
		val max: Long? = null,
	) : SimpleField() {
		override fun validate(value: String?) {
			requireNotNull(value) { "Un entier ne peut pas être vide : trouvé $value" }
			val intVal = requireNotNull(value.toLongOrNull()) { "Cette donnée n'est pas un entier : $value" }

			if (min != null)
				require(intVal >= min) { "La valeur minimale autorisée est $min, $intVal est trop petit" }

			if (max != null)
				require(intVal <= max) { "La valeur maximale autorisée est $max, $intVal est trop grand" }
		}
	}

	/**
	 * The user should input a decimal number (Kotlin's [Double]).
	 */
	@SerialName("INTEGER")
	data class Decimal(
		override val arity: Arity,
	) : SimpleField() {
		override fun validate(value: String?) {
			requireNotNull(value) { "Un réel ne peut pas être vide : trouvé $value" }
			requireNotNull(value.toDoubleOrNull()) { "Cette donnée n'est pas un réel : $value" }
		}
	}

	/**
	 * The user should check a box.
	 */
	@SerialName("INTEGER")
	data class Boolean(
		override val arity: Arity,
	) : SimpleField() {
		override fun validate(value: String?) {
			requireNotNull(value) { "Un booléen ne peut pas être vide : trouvé $value" }
			requireNotNull(value.toBooleanStrictOrNull()) { "Cette donnée n'est pas un booléen : $value" }
		}
	}

	/**
	 * A message should be displayed to the user, but they shouldn't have anything to fill in.
	 * The [arity] is always [Arity.mandatory].
	 */
	@SerialName("MESSAGE")
	object Message : SimpleField() {
		override val arity get() = Arity.mandatory()
		override fun validate(value: String?) {} // whatever is always valid
	}

}
