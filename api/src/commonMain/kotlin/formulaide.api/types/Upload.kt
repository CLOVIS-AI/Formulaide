package formulaide.api.types

import formulaide.api.data.Action
import formulaide.api.data.Form
import kotlinx.serialization.Serializable

@Serializable
data class Upload(
	override val id: ReferenceId,
) : Referencable

@Serializable
data class UploadRequest(
	val form: Ref<Form>,
	val root: Ref<Action>?,
	val field: String,
)
