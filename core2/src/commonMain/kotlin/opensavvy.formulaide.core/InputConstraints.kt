package opensavvy.formulaide.core

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import opensavvy.state.slice.Slice
import opensavvy.state.slice.ensureValid
import opensavvy.state.slice.slice

sealed class InputConstraints {

	abstract suspend fun parse(value: String): Slice<Any>

	data class Text(
		val maxLength: UInt? = null,
	) : InputConstraints() {
		val effectiveMaxLength get() = maxLength ?: 4096u

		override suspend fun parse(value: String): Slice<String> = slice {
			ensureValid(value.length <= effectiveMaxLength.toInt()) { "Le texte saisi fait plus de $effectiveMaxLength caractères : ${value.length} caractères" }
			value
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

		override suspend fun parse(value: String): Slice<Long> = slice {
			val long = value.toLongOrNull()
			ensureValid(long != null) { "'$value' n'est pas un nombre valide" }

			ensureValid(long >= effectiveMin) { "$value est inférieur à la valeur minimale autorisée, $effectiveMin" }
			ensureValid(long <= effectiveMax) { "$value est supérieur à la valeur maximale autorisée, $effectiveMax" }

			long
		}

		override fun toString() = "Integer, min=$effectiveMin, max=$effectiveMax"
	}

	object Boolean : InputConstraints() {
		override suspend fun parse(value: String): Slice<kotlin.Boolean> = slice {
			val bool = value.toBooleanStrictOrNull()
			ensureValid(bool != null) { "'$value' n'est pas un booléen" }

			bool
		}

		override fun toString() = "Boolean"
	}

	object Email : InputConstraints() {
		override suspend fun parse(value: String): Slice<String> = slice {
			ensureValid('@' in value) { "Une adresse électronique doit contenir un arobase, trouvé '$value'" }
			value
		}

		override fun toString() = "Email"
	}

	object Phone : InputConstraints() {
		override suspend fun parse(value: String): Slice<String> = slice {
			ensureValid(value.length <= 20) { "Un numéro de téléphone ne peut pas comporter ${value.length} caractères" }

			for (char in value) {
				ensureValid(char.isDigit() || char == '+') { "Le caractère '$char' n'est pas autorisé dans un numéro de téléphone" }
			}

			value
		}

		override fun toString() = "Phone"
	}

	object Date : InputConstraints() {
		override suspend fun parse(value: String): Slice<LocalDate> = slice {
			LocalDate.parse(value)
		}

		override fun toString() = "Date"
	}

	object Time : InputConstraints() {
		override suspend fun parse(value: String): Slice<LocalTime> = slice {
			LocalTime.parse(value)
		}

		override fun toString() = "Time"
	}

}
