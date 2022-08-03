package formulaide.core.field

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable

@Serializable
sealed class InputConstraints {

	abstract fun requireCompatibleWith(source: InputConstraints)

	abstract fun parse(value: String): Any

	@Serializable
	data class Text(
		val maxLength: UInt? = null,
	) : InputConstraints() {
		val effectiveMaxLength get() = maxLength ?: UInt.MAX_VALUE
		override fun requireCompatibleWith(source: InputConstraints) {
			require(source is Text) { "Un champ texte n'est pas compatible avec $source" }
			require(effectiveMaxLength <= source.effectiveMaxLength) { "Le longueur maximale de ce champ ($effectiveMaxLength) doit être inférieure ou égale à celle de sa source (${source.effectiveMaxLength})" }
		}

		override fun parse(value: String): String {
			require(value.length <= effectiveMaxLength.toInt()) { "Le texte saisi fait plus de $effectiveMaxLength caractères" }
			return value
		}
	}

	@Serializable
	data class Integer(
		val min: Long? = null,
		val max: Long? = null,
	) : InputConstraints() {
		val effectiveMin get() = min ?: Long.MIN_VALUE
		val effectiveMax get() = max ?: Long.MAX_VALUE

		init {
			require(effectiveMax > effectiveMin) { "La valeur minimale ($effectiveMin) doit être plus petite que la value maximale ($effectiveMax)" }
		}

		override fun requireCompatibleWith(source: InputConstraints) {
			require(source is Integer) { "Un champ numérique n'est pas compatible avec $source" }
			require(effectiveMin >= source.effectiveMin) { "La valeur minimale de ce champ ($effectiveMin) doit être supérieure ou égale à celle de sa source (${source.effectiveMin})" }
			require(effectiveMax <= source.effectiveMax) { "La valeur maximale de ce champ ($effectiveMax) doit être inférieure ou égale à celle de sa source (${source.effectiveMax})" }
		}

		override fun parse(value: String): Long {
			val long = value.toLongOrNull()
			requireNotNull(long) { "$value n'est pas un nombre valide" }

			require(long >= effectiveMin) { "$value est trop petit, la valeur minimale autorisée est $effectiveMin" }
			require(long <= effectiveMax) { "$value est trop grand, la valeur maximale autorisée est $effectiveMax" }

			return long
		}
	}

	@Serializable
	object Boolean : InputConstraints() {
		override fun requireCompatibleWith(source: InputConstraints) {
			require(source is Boolean) { "Un champ booléen n'est pas compatible avec $source" }
		}

		override fun parse(value: String): kotlin.Boolean {
			val bool = value.toBooleanStrictOrNull()
			requireNotNull(bool) { "$value n'est pas un booléen" }

			return bool
		}
	}

	@Serializable
	object Email : InputConstraints() {
		override fun requireCompatibleWith(source: InputConstraints) {
			require(source is Email) { "Une adresse mail n'est pas compatible avec $source" }
		}

		override fun parse(value: String): String {
			require('@' in value) { "Une adresse mail doit contenir un arobas : $value" }
			return value
		}
	}

	@Serializable
	object Phone : InputConstraints() {
		override fun requireCompatibleWith(source: InputConstraints) {
			require(source is Phone) { "Un numéro de téléphone n'est pas compatible avec $source" }
		}

		override fun parse(value: String): String {
			require(value.length <= 20) { "Vous avez saisi un numéro de téléphone de ${value.length} caractères" }

			for (char in value) {
				require(char.isDigit() || char == '+') { "Le caractère $char n'est pas autorisé dans un numéro de téléphone (autorisés : chiffres et '+')" }
			}

			return value
		}
	}

	@Serializable
	object Date : InputConstraints() {
		override fun requireCompatibleWith(source: InputConstraints) {
			require(source is Date) { "Une date n'est pas compatible avec $source" }
		}

		override fun parse(value: String): LocalDate {
			return LocalDate.parse(value)
		}
	}

	@Serializable
	object Time : InputConstraints() {
		override fun requireCompatibleWith(source: InputConstraints) {
			require(source is Time) { "Une heure n'est pas compatible avec $source" }
		}

		override fun parse(value: String): LocalTime {
			return LocalTime.parse(value)
		}
	}
}
