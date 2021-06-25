package formulaide.api.fields

import formulaide.api.types.Arity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class FlatField : Field, Field.List, Field.Named, Field.Contained {

	@Serializable
	@SerialName("CONTAINER")
	data class Container(
		override val id: Int,
		override val order: Int,
		override val name: String,
		override val arity: Arity,
		val ref: Field.Container.TopLevel<FlatField>,
	) : FlatField()

	@Serializable
	@SerialName("UNION")
	data class Union(
		override val id: Int,
		override val order: Int,
		override val name: String,
		override val arity: Arity,
		override val options: List<FlatField>,
	) : FlatField(), Field.Union<FlatField>

	@Serializable
	@SerialName("SIMPLE")
	data class Simple(
		override val id: Int,
		override val order: Int,
		val field: SimpleField,
	) : FlatField() {
		override val arity get() = field.arity
		override val name get() = field.name
	}

}
