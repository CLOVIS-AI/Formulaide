package formulaide.api.fields

import formulaide.api.types.Arity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DataFields(
	override val id: Int,
	val fields: List<DataField>,
) : Referencable

@Serializable
sealed class DataField : Field {

	@SerialName("DATA_SIMPLE")
	data class Simple(
		override val id: Int,
		override val name: String,
		override val simple: SimpleField,
	) : DataField(), Field.Simple

	@SerialName("DATA_UNION")
	data class Union(
		override val id: Int,
		override val name: String,
		override val arity: Arity,
		override val options: List<DataField>,
	) : DataField(), Field.Union<DataField>

	@SerialName("DATA_REFERENCE")
	data class Composite(
		override val id: Int,
		override val name: String,
		override val arity: Arity,
		override val ref: DataFields,
	) : DataField(), Field.Reference<DataFields>
}
