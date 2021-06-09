package formulaide.api.data

import kotlinx.serialization.Serializable

@Serializable
enum class DataId {
	TEXT,
	TEXT_FIXED,
	NUMBER,
	BOOLEAN,

	COMPOUND,
	UNION;
}

@Serializable
data class Data(
	val type: DataId,
	val compoundId: CompoundDataId? = null,
	val union: List<UnionDataField>? = null,
)
