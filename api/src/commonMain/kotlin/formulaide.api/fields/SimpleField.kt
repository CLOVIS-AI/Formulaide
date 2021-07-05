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

	@Serializable
	@SerialName("EMAIL")
	data class Email(
		override val arity: Arity,
	) : SimpleField() {

		override fun validate(value: String?) {
			requireNotNull(value) { "Un email ne peut pas être vide : trouvé $value" }
			require('@' in value) { "Un email doit contenir une arobase : $value" }
			require(' ' !in value) { "Un email ne peut pas contenir d'espaces : $value" }
		}
	}

	/**
	 * A date, represented in the format `yyyy-mm-dd`.
	 *
	 * - year: `0..3000`
	 * - month: `1..12`
	 * - day: `1..31`
	 */
	@Serializable
	@SerialName("DATE")
	data class Date(
		override val arity: Arity,
	) : SimpleField() {

		override fun validate(value: String?) {
			requireNotNull(value) { "Une date ne peut pas être vide : trouvé $value" }

			val parts = value.split('-')
			require(parts.size == 3) { "Une date doit être composée de 3 parties, séparées par des tirets : trouvé $value" }

			val (year, month, day) = parts.map { it.toIntOrNull() }
			requireNotNull(year) { "L'année devrait être un entier : $year" }
			requireNotNull(month) { "Le mois devrait être un entier : $month" }
			requireNotNull(day) { "Le jour devrait être un entier : $day" }

			require(year in 0..3_000) { "L'année est invalide : $year" }
			require(month in 1..12) { "Le mois est invalide : $month" }
			require(day in 1..31) { "Le jour est invalide : $day" }
		}
	}

	/**
	 * A time, represented in `hh:mm`.
	 *
	 * - hour: `0..23`
	 * - minutes: `0..59`
	 */
	@Serializable
	@SerialName("TIME")
	data class Time(
		override val arity: Arity,
	) : SimpleField() {

		override fun validate(value: String?) {
			requireNotNull(value) { "Une heure ne peut pas être vide : trouvé $value" }

			val parts = value.split(':')
			require(parts.size == 2) { "Une heure doit être composée de deux parties, séparées par des ':' : trouvé $value" }

			val (hour, minutes) = parts.map { it.toIntOrNull() }
			requireNotNull(hour) { "L'heure devrait être un entier : $hour" }
			requireNotNull(minutes) { "Les minutes devraient être un entier : $minutes" }

			require(hour in 0..23) { "L'heure est invalide : $hour" }
			require(minutes in 0..59) { "Les minutes sont invalides : $minutes" }
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
