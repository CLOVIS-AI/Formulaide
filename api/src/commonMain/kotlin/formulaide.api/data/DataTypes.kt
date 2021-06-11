package formulaide.api.data

import formulaide.api.data.Data.*
import formulaide.api.data.Data.Simple.SimpleDataId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A data type that can appear in a form or in storage.
 *
 * There are 4 groups of data types:
 * - Simple types ([SimpleDataId.TEXT], [SimpleDataId.INTEGER]…), represented by [Simple]
 * - Compound types (created by the administrator, and stored for analysis and processing), represented by [Compound]
 * - Union types (that appear inside compound types or inside forms), represented by [Union]
 * - List types (that appear inside compound types or inside forms), implicit (see [DataList])
 *
 * When serialized, an additional field `type` is added, that corresponds to one of the three first categories.
 */
@Serializable
sealed class Data {
	companion object {

		/**
		 * Factory method to reference a simple data.
		 * @see Data
		 * @see Simple
		 * @see SimpleDataId
		 */
		fun simple(id: SimpleDataId) = Simple(id)

		/**
		 * Factory method to reference a [compound data][CompoundData].
		 * @see Data
		 * @see Compound
		 * @see CompoundData
		 */
		fun compound(compound: CompoundData) = Compound(compound.id)

		/**
		 * Factory method to reference a recursive [compound data][CompoundData].
		 * @see compound
		 * @see SPECIAL_TOKEN_RECURSION
		 */
		fun recursiveCompound() = Compound(SPECIAL_TOKEN_RECURSION)

		/**
		 * Factory method to reference a [union data][UnionDataField].
		 * @see Data
		 * @see Union
		 * @see UnionDataField
		 */
		fun union(ids: List<UnionDataField>) = Union(ids)
	}

	/**
	 * A reference to a [CompoundData].
	 */
	@Serializable
	@SerialName("COMPOUND")
	data class Compound(
		val id: CompoundDataId
	) : Data()

	/**
	 * A union of multiple different data types.
	 * @see UnionDataField
	 */
	@Serializable
	@SerialName("UNION")
	data class Union(
		val elements: List<UnionDataField>
	) : Data()

	/**
	 * A simple type.
	 * @see SimpleDataId
	 */
	@Serializable
	@SerialName("SIMPLE")
	data class Simple(
		val id: SimpleDataId,
	) : Data() {

		@Serializable
		enum class SimpleDataId {

			/**
			 * A simple text field.
			 *
			 * @see MESSAGE
			 */
			TEXT,

			/**
			 * An unmodifiable text field.
			 * This data type is used to display text in the form.
			 *
			 * When using this type, the name of the field ([CompoundDataField.name], [UnionDataField.name])
			 * contains the text that should be displayed to the user.
			 * When [submitting a form][FormSubmission], the expected value is `null` (since there is no meaningful user input).
			 *
			 * @see TEXT
			 */
			MESSAGE,

			/**
			 * The user should input an integer.
			 *
			 * The type corresponds to Kotlin's [Long] (a 64-bit signed integer).
			 */
			INTEGER,

			/**
			 * The user should input a decimal number.
			 *
			 * The type corresponds to Kotlin's [Double] (a 64-bit double precision floating point number).
			 */
			DECIMAL,

			/**
			 * A simple checkbox.
			 */
			BOOLEAN,

		}
	}
}

/**
 * Objects that can be given multiple times.
 *
 * This signifies that some data has some arity:
 * - `0..0` means the data cannot be given,
 * - `1..1` means the data is mandatory,
 * - `0..1` means the data is optional (the user can decide whether they want to give it or not),
 * - `1..5` means a list of minimum 1 element and maximum 5 elements,
 *
 * Any two positive integers can be given.
 */
interface DataList {

	/**
	 * The minimal arity of the data (inclusive).
	 *
	 * Cannot be smaller than 0. Cannot be strictly greater than [maxArity].
	 * @see arity
	 * @see DataList
	 */
	val minArity: Int

	/**
	 * The maximal arity of the data (inclusive).
	 *
	 * Cannot be strictly smaller than [minArity].
	 * @see arity
	 * @see DataList
	 */
	val maxArity: Int

	/**
	 * The arity of this data point.
	 *
	 * Invariant: `0 <= minArity <= maxArity`
	 *
	 * @see DataList
	 */
	val arity get() = minArity..maxArity

	/**
	 * Internal function to check the validity of a [DataList].
	 * Should be called by the constructor of each implementation.
	 */
	fun checkArityValidity() {
		require(0 <= minArity) { "L'arité minimale d'une donnée doit être 0, trouvé : $minArity" }
		require(minArity <= maxArity) { "L'arité maximale d'une donnée doit être supérieure ou égale à l'arité minimale : min=$minArity, max=$maxArity" }
	}
}

/**
 * Elements in a list that requires an order.
 *
 * Lists in JSON are unordered. Implementations of this interface are ordered.
 */
interface OrderedListElement {

	/**
	 * An arbitrary integer that represents the position of this element in the list.
	 * For two elements in the list, the one with smallest `order` appears first in the list, and the one with the greater `order` appears last.
	 *
	 * Can be used to reorder the list, for example with:
	 * ```
	 * someList.sortedBy { it.order }
	 * ```
	 */
	val order: Int

	companion object {
		/**
		 * Internal function to check the validity of a list of [OrderedListElement].
		 * Should be called by the constructor of each implementation.
		 */
		fun List<OrderedListElement>.checkOrderValidity() {
			val cleaned = distinct()
			require(cleaned == this) { "Tous les éléments devraient avoir un ordre différent" }
		}
	}
}
