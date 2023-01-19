package opensavvy.formulaide.core

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import opensavvy.state.outcome.Outcome
import opensavvy.state.outcome.ensureValid
import opensavvy.state.outcome.out
import opensavvy.formulaide.core.data.Email as DataEmail

/**
 * A simple piece of information requested of the user filling in the form.
 */
sealed class Input {

	abstract suspend fun parse(value: String): Outcome<Any>

	data class Text(
		val maxLength: UInt? = null,
	) : Input() {
		val effectiveMaxLength get() = maxLength ?: 4096u

		override suspend fun parse(value: String) = out {
			ensureValid(value.length <= effectiveMaxLength.toInt()) { "Le texte saisi fait plus de $effectiveMaxLength caractères : ${value.length}" }
			value
		}

		override fun toString() = "Texte (longueur maximale : $effectiveMaxLength)"
	}

	data class Integer(
		val min: Long? = null,
		val max: Long? = null,
	) : Input() {
		val effectiveMin get() = min ?: Long.MIN_VALUE
		val effectiveMax get() = max ?: Long.MAX_VALUE

		init {
			require(effectiveMin < effectiveMax) { "La valeur minimale ($effectiveMin) doit être inférieure à la valeur maximale ($effectiveMax)" }
		}

		override suspend fun parse(value: String) = out {
			val long = value.toLongOrNull()
			ensureValid(long != null) { "'$value' n'est pas un nombre valide" }

			ensureValid(long >= effectiveMin) { "$value est inférieur à la valeur minimale autorisée, $effectiveMin" }
			ensureValid(long <= effectiveMax) { "$value est supérieur à la valeur maximale autorisée, $effectiveMax" }

			long
		}

		override fun toString() = "Nombre (de $effectiveMin à $effectiveMax)"
	}

	object Toggle : Input() {
		override suspend fun parse(value: String) = out {
			val bool = value.toBooleanStrictOrNull()
			ensureValid(bool != null) { "'$value' n'est pas un booléen" }

			bool
		}

		override fun toString() = "Coche"
	}

	object Email : Input() {
		override suspend fun parse(value: String) = out {
			DataEmail(value)
		}

		override fun toString() = "Adresse électronique"
	}

	object Phone : Input() {
		override suspend fun parse(value: String) = out {
			ensureValid(value.length <= 20) { "Un numéro de téléphone ne peut pas comporter ${value.length} caractères" }

			for (char in value) {
				ensureValid(char.isDigit() || char == '+') { "Le caractère '$char' n'est pas autorisé dans un numéro de téléphone" }
			}

			value
		}

		override fun toString() = "Numéro de téléphone"
	}

	object Date : Input() {
		override suspend fun parse(value: String) = out {
			LocalDate.parse(value)
		}

		override fun toString() = "Date"
	}

	object Time : Input() {
		override suspend fun parse(value: String) = out {
			LocalTime.parse(value)
		}

		override fun toString() = "Heure"
	}

}
