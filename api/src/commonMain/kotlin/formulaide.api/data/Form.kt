package formulaide.api.data

import kotlinx.serialization.Serializable

@Serializable
data class FormFieldComponent(
	val id: DataFieldId,
	val minArity: Int,
	val maxArity: Int,
	val components: List<FormFieldComponent>? = null,
)

typealias FormFieldId = Int

@Serializable
data class FormField(
	val name: String,
	val type: Data,
	val components: List<FormFieldComponent>? = null,
	val minArity: Int,
	val maxArity: Int,
	val order: Int,
	val id: FormFieldId,
)

typealias FormId = Int

@Serializable
data class Form(
	val name: String,
	val id: FormId,
	val public: Boolean,
	val fields: List<FormField>,
)
