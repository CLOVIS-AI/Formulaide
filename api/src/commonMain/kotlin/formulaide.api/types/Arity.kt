package formulaide.api.types

import formulaide.api.types.Arity.Companion.forbidden
import formulaide.api.types.Arity.Companion.mandatory
import formulaide.api.types.Arity.Companion.optional
import kotlinx.serialization.Serializable

/**
 * A range for objects that can be given multiple times.
 *
 * This signifies that some data has some arity:
 * - `0..0` means the data cannot be given (see [forbidden]),
 * - `1..1` means the data is mandatory (see [mandatory]),
 * - `0..1` means the data is optional (the user can decide whether they want to give it or not, see [optional]),
 * - `1..5` means a list of minimum 1 element and maximum 5 elements,
 *
 * Any two positive integers can be given.
 *
 * @property min The minimal number of values given.
 * `0 <= min <= max`
 * @property max The maximal number of values given.
 * `0 <= min <= max`
 */
@Serializable
data class Arity(
	val min: Int,
	val max: Int,
) {
	init {
		require(0 <= min) { "L'arité minimale d'une donnée ne peut pas être négative : $min" }
		require(min <= max) { "L'arité maximale d'une donnée doit être supérieure ou égale à son arité minimale : min=$min, max=$max" }
	}

	/**
	 * Converts this [Arity] to a Kotlin [IntRange].
	 */
	val range get() = min..max

	override fun toString() = "$min..$max"

	companion object {

		/**
		 * Converts a Kotlin [IntRange] to an [Arity].
		 */
		fun IntRange.asArity(): Arity {
			require(step == 1) { "Les arités ne gèrent pas les pas différents de 1: step=$step" }
			return Arity(start, endInclusive)
		}

		/**
		 * Factory method to create a forbidden data.
		 *
		 * Thanks to the invariant, any forbidden arity must be [equal][Any.equals] to the one returned by this function.
		 */
		fun forbidden() = Arity(0, 0)

		/**
		 * Factory method to create a mandatory data.
		 */
		fun mandatory() = Arity(1, 1)

		/**
		 * Factory method to create an optional data.
		 */
		fun optional() = Arity(0, 1)

		/**
		 * Factory method to create a list of data.
		 */
		fun list(min: Int, max: Int) = Arity(min, max)
	}
}
