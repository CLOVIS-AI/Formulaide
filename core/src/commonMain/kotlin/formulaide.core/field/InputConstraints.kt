package formulaide.core.field

import kotlinx.serialization.Serializable

@Serializable
sealed class InputConstraints {

	abstract fun requireCompatibleWith(source: InputConstraints)

	@Serializable
	data class Text(
		val maxLength: UInt? = null,
	) : InputConstraints() {
		val effectiveMaxLength get() = maxLength ?: UInt.MAX_VALUE
		override fun requireCompatibleWith(source: InputConstraints) {
			require(source is Text) { "Un champ texte n'est pas compatible avec $source" }
			require(effectiveMaxLength <= source.effectiveMaxLength) { "Le longueur maximale de ce champ ($effectiveMaxLength) doit être inférieure ou égale à celle de sa source (${source.effectiveMaxLength})" }
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
	}

	@Serializable
	object Boolean : InputConstraints() {
		override fun requireCompatibleWith(source: InputConstraints) {
			require(source is Boolean) { "Un champ booléen n'est pas compatible avec $source" }
		}
	}

	@Serializable
	object Email : InputConstraints() {
		override fun requireCompatibleWith(source: InputConstraints) {
			require(source is Email) { "Une adresse mail n'est pas compatible avec $source" }
		}
	}

	@Serializable
	object Phone : InputConstraints() {
		override fun requireCompatibleWith(source: InputConstraints) {
			require(source is Phone) { "Un numéro de téléphone n'est pas compatible avec $source" }
		}
	}

	@Serializable
	object Date : InputConstraints() {
		override fun requireCompatibleWith(source: InputConstraints) {
			require(source is Date) { "Une date n'est pas compatible avec $source" }
		}
	}

	@Serializable
	object Time : InputConstraints() {
		override fun requireCompatibleWith(source: InputConstraints) {
			require(source is Time) { "Une heure n'est pas compatible avec $source" }
		}
	}
}
