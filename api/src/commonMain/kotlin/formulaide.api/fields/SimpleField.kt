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

	/**
	 * Checks that a [value] provided by the user is compatible with this type.
	 */
	abstract fun validate(value: String?)

	/**
	 * Checks that [newer] is compatible with the current [SimpleField].
	 *
	 * Use this to check that a [FormField] corresponds to the matching [DataField], for example.
	 */
	open fun validateCompatibility(newer: SimpleField) {
		require(this::class == newer::class) { "Une donnée ne peut pas être compatible avec une donnée d'un autre type : la valeur d'origine est de type ${this::class.simpleName}, la nouvelle est de type ${newer::class.simpleName}" }

		require(newer.arity.min >= arity.min) { "Une donnée ne peut qu'augmenter l'arité minimale : la valeur d'origine ($arity) autorise un espace moins large que la nouvelle valeur (${newer.arity})" }
		require(newer.arity.max <= arity.max) { "Une donnée ne peut que diminuer l'arité maximale : la valeur d'origine ($arity) autorise un espace moins large que la nouvelle valeur (${newer.arity})" }
	}

	/**
	 * The user should input some text.
	 * @property maxLength The maximum number of characters allowed (`null` means 'no limit').
	 */
	@Serializable
	@SerialName("TEXT")
	data class Text(
		override val arity: Arity,
		val maxLength: Int? = null,
	) : SimpleField() {

		val effectiveMaxLength get() = maxLength ?: Int.MAX_VALUE

		override fun validate(value: String?) {
			requireNotNull(value) { "Un champ de texte doit être rempli : trouvé '$value'" }
			require(value.isNotBlank()) { "Un champ de texte ne peut pas être vide ou contenir uniquement des espaces : trouvé '$value'" }

			require(value.length <= effectiveMaxLength) { "La longueur maximale autorisée est de $maxLength caractères, mais ${value.length} ont été donnés" }
		}

		override fun validateCompatibility(newer: SimpleField) {
			super.validateCompatibility(newer)
			newer as Text

			require(newer.effectiveMaxLength <= effectiveMaxLength) { "La longueur maximale ne peut être que diminuée : la valeur d'origine est $effectiveMaxLength, la nouvelle valeur est ${newer.effectiveMaxLength}" }
		}
	}

	/**
	 * The user should input an integer (Kotlin's [Long]).
	 * @property min The minimum value of that integer (`null` means 'no minimum').
	 * @property max The maximum value of that integer (`null` means 'no maximum').
	 */
	@Serializable
	@SerialName("INTEGER")
	data class Integer(
		override val arity: Arity,
		val min: Long? = null,
		val max: Long? = null,
	) : SimpleField() {

		val effectiveMin get() = min ?: Long.MIN_VALUE
		val effectiveMax get() = max ?: Long.MAX_VALUE

		override fun validate(value: String?) {
			requireNotNull(value) { "Un entier ne peut pas être vide : trouvé $value" }
			val intVal = requireNotNull(value.toLongOrNull()) { "Cette donnée n'est pas un entier : $value" }

			require(intVal >= effectiveMin) { "La valeur minimale autorisée est $min, $intVal est trop petit" }
			require(intVal <= effectiveMax) { "La valeur maximale autorisée est $max, $intVal est trop grand" }
		}

		override fun validateCompatibility(newer: SimpleField) {
			super.validateCompatibility(newer)
			newer as Integer

			require(effectiveMin <= newer.effectiveMin) { "La valeur minimale ne peut pas être diminuée : la valeur d'origine est $effectiveMin, la nouvelle valeur est ${newer.effectiveMin}" }
			require(effectiveMax >= newer.effectiveMax) { "La valeur maximale ne peut pas être augmentée : la valeur d'origine est $effectiveMax, la nouvelle valeur est ${newer.effectiveMax}" }
		}
	}

	/**
	 * The user should input a decimal number (Kotlin's [Double]).
	 */
	@Serializable
	@SerialName("DECIMAL")
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
	@Serializable
	@SerialName("BOOLEAN")
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
	@Serializable
	@SerialName("MESSAGE")
	object Message : SimpleField() {
		override val arity get() = Arity.mandatory()
		override fun validate(value: String?) {} // whatever is always valid
	}

}
