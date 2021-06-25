package formulaide.api.fields

import formulaide.api.types.Arity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class DeepField : Field, Field.List, Field.Contained, Field.Named {

	@Serializable
	@SerialName("CONTAINER")
	data class Container(
		override val id: Int,
		override val order: Int,
		override val name: String,
		override val arity: Arity,
		val references: Field.Container.TopLevel<FlatField>,
		override val fields: List<DeeperField>,
	) : DeepField(), Field.Container<DeeperField>

	@Serializable
	@SerialName("UNION")
	data class Union(
		override val id: Int,
		override val order: Int,
		override val name: String,
		override val arity: Arity,
		override val options: List<DeepField>,
	) : DeepField(), Field.Union<DeepField>

	@Serializable
	@SerialName("SIMPLE")
	data class Simple(
		override val id: Int,
		override val order: Int,
		val field: SimpleField,
	) : DeepField() {
		override val arity get() = field.arity
		override val name get() = field.name
	}
}

@Serializable
sealed class DeeperField(
	val references: FlatField
) : Field, Field.List, Field.Contained, Field.Named {
	override val id get() = references.id
	override val name get() = references.name

	@Serializable
	@SerialName("CONTAINER")
	data class Container(
		override val id: Int,
		override val order: Int,
		override val arity: Arity,
		references: FlatField,
	) : DeeperField(references), Field.Container<DeeperField>

}
