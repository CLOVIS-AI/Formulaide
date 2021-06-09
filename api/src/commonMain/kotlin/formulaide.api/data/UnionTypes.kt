package formulaide.api.data

import kotlinx.serialization.Serializable

/**
 * A union data field.
 *
 * Can be any valid type. Multiple different types can form the same union (sum type).
 *
 * @property id The ID of this field in this union. This field is not globally unique.
 * @property name The display name of this union.
 * @see Data.union
 */
@Serializable
data class UnionDataField(
	val id: Int,
	val type: Data,
	val name: String,
)
