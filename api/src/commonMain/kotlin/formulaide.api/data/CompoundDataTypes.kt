package formulaide.api.data

import formulaide.api.data.OrderedListElement.Companion.checkOrderValidity
import formulaide.api.types.Arity
import kotlinx.serialization.Serializable

/**
 * Id of [CompoundData].
 */
typealias CompoundDataId = String

/**
 * A data type built from the composition of other data types (record type, product type).
 *
 * Through [CompoundDataField.data], a compound data can include unions, other compound datas, or even be recursive.
 *
 * @property name The display name of this data type.
 * @property fields The different types that are part of this compound.
 * The list cannot be empty.
 * @see Data.Compound
 */
@Serializable
data class CompoundData(
	val name: String,
	val id: CompoundDataId,
	val fields: List<CompoundDataField>,
) {
	init {
		require(name.isNotBlank()) { "Le nom d'une donnée ne peut pas être vide : '$name'" }
		fields.checkOrderValidity()

		val ids = fields.distinctBy { it.checkValidity(allowRecursion = false); it.id }
		require(ids == fields) { "L'identité d'un champ ne doit pas apparaitre plusieurs fois dans une même donnée" }
	}
}

/**
 * Id of [CompoundDataField].
 */
typealias CompoundDataFieldId = Int

/**
 * A datatype that takes part in a [CompoundData].
 *
 * @property id The ID of this field, guaranteed unique for a specific [CompoundData] (not globally unique).
 * When creating a new data (see [NewCompoundData]), you are free to choose any ID value.
 * @property name The display name of this field
 * @property data The type of this field. Can be any valid type (including a union, or itself).
 */
@Serializable
data class CompoundDataField(
	override val order: Int,
	val arity: Arity,
	val id: CompoundDataFieldId,
	val name: String,
	val data: Data,
) : OrderedListElement {
	init {
		require(name.isNotBlank()) { "Le nom d'un champ ne peut pas être vide : '$name'" }
	}

	/**
	 * Internal function to check the validity of this object.
	 */
	internal fun checkValidity(allowRecursion: Boolean) {
		if (!allowRecursion && data is Data.Compound)
			require(data.id != SPECIAL_TOKEN_RECURSION) { "Un champ de donnée ne peut pas contenir SPECIAL_TOKEN_RECURSION, sauf s'il fait partie de NewCompoundData (mais ce n'est pas le cas ici)" }
	}
}

//region Modifications

/**
 * When creating a new compound data, [fields][CompoundDataField] are allowed to
 * use the special ID [SPECIAL_TOKEN_RECURSION] to refer to their own parent.
 * The server will replace those by the real ID of the data, once it is computed.
 */
@Serializable
data class NewCompoundData(
	val name: String,
	val fields: List<CompoundDataField>,
) {
	init {
		require(name.isNotBlank()) { "Le nom d'une donnée ne peut pas être vide : '$name'" }
		fields.checkOrderValidity()

		val ids = fields.distinctBy { it.checkValidity(allowRecursion = true); it.id }
		require(ids == fields) { "L'identité d'un champ ne doit pas apparaitre plusieurs fois dans une même donnée" }
	}
}

/**
 * Special ID that can be used for fields in data to refer to their parent data even before it was created (therefore doesn't have an ID yet).
 * See [NewCompoundData].
 *
 * To create a Data instance with this value, see [Data.recursiveCompound].
 */
// The semicolon is not allowed in normal IDs
const val SPECIAL_TOKEN_RECURSION: CompoundDataId = "special:myself"

//endregion
