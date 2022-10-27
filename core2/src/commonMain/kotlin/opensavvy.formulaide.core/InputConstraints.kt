package opensavvy.formulaide.core

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import opensavvy.state.Slice.Companion.successful
import opensavvy.state.State
import opensavvy.state.ensureValid
import opensavvy.state.state

sealed class InputConstraints {

	abstract fun parse(value: String): State<Any>

	data class Text(
		val maxLength: UInt? = null,
	) : InputConstraints() {
		val effectiveMaxLength get() = maxLength ?: 4096u

		override fun parse(value: String): State<String> = state {
			ensureValid(value.length <= effectiveMaxLength.toInt()) { "Le texte saisi fait plus de $effectiveMaxLength caractères : ${value.length} caractères" }
			emit(successful(value))
		}

		override fun toString() = "Text, maxLength=$effectiveMaxLength"
	}

	data class Integer(
		val min: Long? = null,
		val max: Long? = null,
	) : InputConstraints() {
		val effectiveMin get() = min ?: Long.MIN_VALUE
		val effectiveMax get() = max ?: Long.MAX_VALUE

		init {
			require(effectiveMax > effectiveMin) { "La valeur minimale ($effectiveMin) doit être inférieure à la valeur maximale ($effectiveMax)" }
		}

		override fun parse(value: String): State<Long> = state {
			val long = value.toLongOrNull()
			ensureValid(long != null) { "'$value' n'est pas un nombre valide" }

			ensureValid(long >= effectiveMin) { "$value est inférieur à la valeur minimale autorisée, $effectiveMin" }
			ensureValid(long <= effectiveMax) { "$value est supérieur à la valeur maximale autorisée, $effectiveMax" }

			emit(successful(long))
		}

		override fun toString() = "Integer, min=$effectiveMin, max=$effectiveMax"
	}

	object Boolean : InputConstraints() {
		override fun parse(value: String): State<kotlin.Boolean> = state {
			val bool = value.toBooleanStrictOrNull()
			ensureValid(bool != null) { "'$value' n'est pas un booléen" }

			emit(successful(bool))
		}

		override fun toString() = "Boolean"
	}

	object Email : InputConstraints() {
		override fun parse(value: String): State<String> = state {
			ensureValid('@' in value) { "Une adresse électronique doit contenir un arobase, trouvé '$value'" }
			emit(successful(value))
		}

		override fun toString() = "Email"
	}

	object Phone : InputConstraints() {
		override fun parse(value: String): State<String> = state {
			ensureValid(value.length <= 20) { "Un numéro de téléphone ne peut pas comporter ${value.length} caractères" }

			for (char in value) {
				ensureValid(char.isDigit() || char == '+') { "Le caractère '$char' n'est pas autorisé dans un numéro de téléphone" }
			}

			emit(successful(value))
		}

		override fun toString() = "Phone"
	}

	object Date : InputConstraints() {
		override fun parse(value: String): State<LocalDate> = state {
			emit(successful(LocalDate.parse(value)))
		}

		override fun toString() = "Date"
	}

	object Time : InputConstraints() {
		override fun parse(value: String): State<LocalTime> = state {
			emit(successful(LocalTime.parse(value)))
		}

		override fun toString() = "Time"
	}

}
