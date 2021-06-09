package formulaide.api.data

import kotlinx.serialization.Serializable

typealias DataFieldId = Int

@Serializable
data class CompoundDataField(
	val name: String,
	val order: Int,
	val type: Data,
	val minArity: Int,
	val maxArity: Int,
	val id: DataFieldId,
) {
	val arity get() = minArity..maxArity
}

typealias CompoundDataId = Int

@Serializable
data class CompoundData(
	val name: String,
	val id: CompoundDataId,
	val fields: List<CompoundDataField>,
)
