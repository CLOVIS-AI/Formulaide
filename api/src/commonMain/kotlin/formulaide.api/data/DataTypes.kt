package formulaide.api.data

import formulaide.api.data.Data.Companion.compound
import formulaide.api.data.Data.Companion.simple
import formulaide.api.data.DataId.*
import kotlinx.serialization.Serializable

/**
 * A data type that can appear in a form or in storage.
 *
 * This enum represents the ID of the different types, see [Data] for the types themselves.
 *
 * There are 4 groups of data types:
 * - Simple types ([TEXT], [INTEGER]…)
 * - Compound types (created by the administrator, and stored for analysis and processing), represented by [COMPOUND]
 * - Union types (that appear inside compound types or inside forms), represented by [UNION]
 * - List types (that appear inside compound types or inside forms), implicit (see [DataList])
 */
@Serializable
enum class DataId {

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

	/**
	 * A compound type.
	 *
	 * @see Data
	 * @see CompoundData
	 */
	COMPOUND,

	/**
	 * A union type.
	 *
	 * @see Data
	 * @see UnionDataField
	 */
	UNION;
}

/**
 * A data type that can appear in a form or in storage.
 *
 * See [DataId] for an explanation of the different groups of types.
 *
 * To easily respect the attribute constraints, use the factory methods [simple], [compound] and [union].
 *
 * @property type The ID of the type represented by this class.
 * @property compoundId The ID of the compound type being represented,
 * only used when [type] is [COMPOUND] (`type == COMPOUND <=> compoundId != null`).
 * @property union The description of the types included in the union,
 * only used when [type] is [UNION] (`type == UNION <=> union != null`).
 * The list can never be empty (either `null` or a non-empty list).
 */
@Serializable
data class Data(
	val type: DataId,
	val compoundId: CompoundDataId? = null,
	val union: List<UnionDataField>? = null,
) {
	companion object {
		/**
		 * Factory method to reference a simple data.
		 * @see Data
		 * @see DataId
		 */
		fun simple(id: DataId) = Data(id)

		/**
		 * Factory method to reference a [compound data][CompoundData].
		 * @see CompoundData
		 * @see Data
		 * @see DataId
		 * @see COMPOUND
		 */
		fun compound(id: CompoundDataId) = Data(COMPOUND, compoundId = id)

		/**
		 * Factory method to reference a [union data][UnionDataField].
		 * @see UnionDataField
		 * @see Data
		 * @see DataId
		 * @see UNION
		 */
		fun union(ids: List<UnionDataField>) = Data(UNION, union = ids)
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
	val minArity: UInt

	/**
	 * The maximal arity of the data (inclusive).
	 *
	 * Cannot be strictly smaller than [minArity].
	 * @see arity
	 * @see DataList
	 */
	val maxArity: UInt

	/**
	 * The arity of this data point.
	 *
	 * Invariant: `0 <= minArity <= maxArity`
	 *
	 * @see DataList
	 */
	val arity get() = minArity..maxArity
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

}
