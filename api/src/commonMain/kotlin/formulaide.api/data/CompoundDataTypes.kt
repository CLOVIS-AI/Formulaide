package formulaide.api.data

import kotlinx.serialization.Serializable

/**
 * Id of [CompoundData].
 */
typealias CompoundDataId = Int

/**
 * A data type built from the composition of other data types (record type, product type).
 *
 * Through [CompoundDataField.type], a compound data can include unions, other compound datas, or even be recursive.
 *
 * @property name The display name of this data type.
 * @property fields The different types that are part of this compound.
 * The list cannot be empty.
 * @see Data.compoundId
 */
@Serializable
data class CompoundData(
	val name: String,
	val id: CompoundDataId,
	val fields: List<CompoundDataField>,
)

/**
 * Id of [CompoundDataField].
 */
typealias CompoundDataFieldId = Int

/**
 * A datatype that takes part in a [CompoundData].
 *
 * @property id The ID of this field, guaranteed unique for a specific [CompoundData] (not globally unique).
 * @property name The display name of this field
 * @property type The type of this field. Can be any valid type (including a union, or itself).
 */
@Serializable
data class CompoundDataField(
	override val order: Int,
	override val minArity: UInt,
	override val maxArity: UInt,
	val id: CompoundDataFieldId,
	val name: String,
	val type: Data,
) : DataList, OrderedListElement
