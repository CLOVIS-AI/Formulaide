package formulaide.api.types

import formulaide.api.data.Action
import formulaide.api.data.Form
import kotlinx.serialization.Serializable

@Serializable
data class Upload(
	override val id: ReferenceId,
	val data: String,
	val uploadTimestamp: Long,
	val expirationTimestamp: Long,
	val mime: String,
) : Referencable {

	val bytes get() = data.encodeToByteArray()
}

@Serializable
data class UploadRequest(
	val form: Ref<Form>,
	val root: Ref<Action>?,
	val field: String,
)

@Serializable
data class DownloadRequest(
	val id: String,
)
