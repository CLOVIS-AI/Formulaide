package formulaide.api.data

import kotlinx.serialization.Serializable

@Serializable
data class UnionDataField(
	val type: Data,
	val id: Int,
	val name: String,
)
