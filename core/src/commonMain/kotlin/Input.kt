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

	abstract suspend fun validateCompatibleWith(source: Input): Outcome<Unit>

	data class Text(
		val maxLength: UInt? = null,
	) : Input() {
		val effectiveMaxLength get() = maxLength ?: 4096u

		override suspend fun parse(value: String) = out {
			ensureValid(value.length <= effectiveMaxLength.toInt()) { "Le texte saisi fait plus de $effectiveMaxLength caractères : ${value.length}" }
			value
		}

		override suspend fun validateCompatibleWith(source: Input): Outcome<Unit> = out {
			ensureValid(source is Text) { "Impossible d'importer un texte à partir d'un ${source::class} (${this@Text} -> $source)" }

			ensureValid(effectiveMaxLength <= source.effectiveMaxLength) { "Un texte importé ne peut pas autoriser une longueur ($effectiveMaxLength) plus élevée que celle de sa source (${source.effectiveMaxLength})" }
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

		constructor(range: IntRange) : this(range.first.toLong(), range.last.toLong())
		constructor(range: LongRange) : this(range.first, range.last)

		val effectiveRange get() = effectiveMin..effectiveMax

		override suspend fun parse(value: String) = out {
			val long = value.toLongOrNull()
			ensureValid(long != null) { "'$value' n'est pas un nombre valide" }

			ensureValid(long >= effectiveMin) { "$value est inférieur à la valeur minimale autorisée, $effectiveMin" }
			ensureValid(long <= effectiveMax) { "$value est supérieur à la valeur maximale autorisée, $effectiveMax" }

			long
		}

		override suspend fun validateCompatibleWith(source: Input): Outcome<Unit> = out {
			ensureValid(source is Integer) { "Impossible d'importer un texte à partir d'un ${source::class} (${this@Integer} -> $source)" }

			ensureValid(effectiveMin >= source.effectiveMin) { "Un nombre importé ne peut pas autoriser une valeur minimale ($effectiveMin) plus petite que celle de sa source (${source.effectiveMin})" }
			ensureValid(effectiveMax <= source.effectiveMax) { "Un nombre importé ne peut pas autoriser une valeur maximale ($effectiveMax) plus grande que celle de sa source (${source.effectiveMax})" }
		}

		override fun toString() = "Nombre (de $effectiveMin à $effectiveMax)"
	}

	object Toggle : Input() {
		override suspend fun parse(value: String) = out {
			val bool = value.toBooleanStrictOrNull()
			ensureValid(bool != null) { "'$value' n'est pas un booléen" }

			bool
		}

		override suspend fun validateCompatibleWith(source: Input): Outcome<Unit> = out {
			ensureValid(source is Toggle) { "Impossible d'importer un booléen à partir d'un ${source::class} (${this@Toggle} -> $source)" }
		}

		override fun toString() = "Coche"
	}

	object Email : Input() {
		override suspend fun parse(value: String) = out {
			DataEmail(value)
		}

		override suspend fun validateCompatibleWith(source: Input): Outcome<Unit> = out {
			ensureValid(source is Email) { "Impossible d'importer une adresse email à partir d'un ${source::class} (${this@Email} -> $source)" }
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

		override suspend fun validateCompatibleWith(source: Input): Outcome<Unit> = out {
			ensureValid(source is Phone) { "Impossible d'importer un numéro de téléphone à partir d'un ${source::class} (${this@Phone} -> $source)" }
		}

		override fun toString() = "Numéro de téléphone"
	}

	object Date : Input() {
		override suspend fun parse(value: String) = out {
			LocalDate.parse(value)
		}

		override suspend fun validateCompatibleWith(source: Input): Outcome<Unit> = out {
			ensureValid(source is Date) { "Impossible d'importer une date à partir d'un ${source::class} (${this@Date} -> $source)" }
		}

		override fun toString() = "Date"
	}

	object Time : Input() {
		override suspend fun parse(value: String) = out {
			LocalTime.parse(value)
		}

		override suspend fun validateCompatibleWith(source: Input): Outcome<Unit> = out {
			ensureValid(source is Time) { "Impossible d'importer une heure à partir d'un ${source::class} (${this@Time} -> $source)" }
		}

		override fun toString() = "Heure"
	}

}
