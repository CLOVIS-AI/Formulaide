package formulaide.api.fields

import formulaide.api.types.Arity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FormFields(
	val fields: List<FormField>,
)

@Serializable
sealed class FormField : Field {

	@SerialName("FORM_SIMPLE")
	data class Simple(
		override val id: Int,
		override val name: String,
		override val simple: SimpleField,
	) : FormField(), Field.Simple

	@SerialName("FORM_UNION")
	data class Union(
		override val id: Int,
		override val name: String,
		override val arity: Arity,
		override val options: List<FormField>,
	) : FormField(), Field.Union<FormField>

	@SerialName("FORM_COMPOSITE")
	data class Composite(
		override val id: Int,
		override val name: String,
		override val arity: Arity,
		override val ref: DataField,
		override val fields: List<DeepFormFields>,
	) : FormField(), Field.Reference<DataField>, Field.Container<DeepFormFields>
}

@Serializable
sealed class DeepFormFields : Field, Field.Reference<DataField> {
	override val id get() = ref.id
	override val name get() = ref.name

	@SerialName("FORM_SIMPLE_DEEP")
	data class Simple(
		override val ref: DataField.Simple,
		override val simple: SimpleField,
	) : DeepFormFields(), Field.Simple

	@SerialName("FORM_UNION_DEEP")
	data class Union(
		override val ref: DataField.Union,
		override val arity: Arity,
		override val options: List<DeepFormFields>,
	) : DeepFormFields(), Field.Union<DeepFormFields>

	@SerialName("FORM_COMPOSITE_DEEP")
	data class Composite(
		override val ref: DataField.Composite,
		override val arity: Arity,
		override val fields: List<DeepFormFields>,
	) : DeepFormFields(), Field.Container<DeepFormFields>
}
